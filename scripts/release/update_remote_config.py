#!/usr/bin/env python3
"""Publish release config to Lastudio, then mirror it to public config repos."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import sys
import tempfile
import time
import uuid
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, urlopen


if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")


DEFAULT_LOCAL_CONFIG_FILE = "config/config.json"
DEFAULT_CONFIG_FILE = "config.json"
DEFAULT_CHANGELOG_FILE = "update.md"
DEFAULT_DOWNLOAD_URL = "https://lastudio.cc/dl/Fe/latest"
PUBLISHER_CONFIG_URL = "https://api.lastudio.cc/publisher/software/fe/config"
READ_ONLY_FIELDS = {"key", "releaseId", "changelogUrl"}
MIRROR_EXCLUDED_FIELDS = {
    "key",
    "releaseId",
    "version",
    "configRevision",
    "changelogContent",
}
RETRY_ATTEMPTS = 6

DEFAULT_GITHUB_REPO = "laststudio/Fe_config"
DEFAULT_GITHUB_BRANCH = "main"
DEFAULT_GITEE_REPO = "qiuqiqiuqid/fe_config"
DEFAULT_GITEE_BRANCH = "master"


def run_command(command: list[str], cwd: Path | None = None) -> None:
    subprocess.run(command, cwd=cwd, check=True, text=True)


def clone_url(provider: str, repo: str, username: str | None, token: str) -> str:
    if provider == "github":
        return f"https://x-access-token:{quote(token, safe='')}@github.com/{repo}.git"
    if provider == "gitee":
        if not username:
            raise ValueError("Gitee sync requires a username.")
        return f"https://{quote(username, safe='')}:{quote(token, safe='')}@gitee.com/{repo}.git"
    raise ValueError(f"Unknown provider: {provider}")


def clone_repo(provider: str, repo: str, branch: str, username: str | None, token: str, target_dir: Path) -> Path:
    repo_dir = target_dir / f"{provider}-config"
    run_command([
        "git",
        "clone",
        "--branch",
        branch,
        "--depth",
        "1",
        clone_url(provider, repo, username, token),
        str(repo_dir),
    ])
    return repo_dir


def load_json_file(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"{path} must contain a JSON object.")
    return data


def read_text(path: Path | None) -> str:
    if path is None:
        return ""
    return path.read_text(encoding="utf-8").strip()


def read_exact_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def raw_url(provider: str, repo: str, branch: str, file_path: str) -> str:
    normalized_path = file_path.strip().lstrip("/")
    if provider == "github":
        return f"https://raw.githubusercontent.com/{repo}/{branch}/{normalized_path}"
    if provider == "gitee":
        return f"https://raw.giteeusercontent.com/{repo}/raw/{branch}/{normalized_path}"
    raise ValueError(f"Unknown provider: {provider}")


def build_release_fields(
    local_config_file: Path,
    version_code: int,
    version_name: str,
    apk_url: str,
    update_message: str,
    changelog_content: str,
) -> dict:
    config = load_json_file(local_config_file)
    return {
        "version": version_name,
        "latestVersionCode": version_code,
        "updateUrl": (config.get("updateUrl") or apk_url or DEFAULT_DOWNLOAD_URL).strip(),
        "isForce": config.get("isForce", False),
        "updateMessage": update_message,
        "changelogSummary": update_message,
        "changelogContent": changelog_content,
    }


def request_publisher_config(
    method: str,
    token: str,
    payload: dict | None = None,
    idempotency_key: str | None = None,
) -> dict:
    body = None if payload is None else json.dumps(payload, ensure_ascii=False, sort_keys=True).encode("utf-8")
    for attempt in range(RETRY_ATTEMPTS):
        headers = {
            "Accept": "application/json",
            "Authorization": f"Publisher {token}",
        }
        if body is not None:
            if not idempotency_key:
                raise ValueError("Publisher PUT requires an Idempotency-Key.")
            headers.update({
                "Content-Type": "application/json; charset=utf-8",
                "Idempotency-Key": idempotency_key,
                "X-Request-Timestamp": str(int(time.time())),
                "X-Request-Nonce": uuid.uuid4().hex,
            })
        request = Request(
            PUBLISHER_CONFIG_URL,
            data=body,
            method=method,
            headers=headers,
        )
        try:
            with urlopen(request, timeout=15) as response:
                result = json.loads(response.read().decode("utf-8"))
            if not isinstance(result, dict):
                raise ValueError(f"Publisher {method} response must be a flat JSON object.")
            return result
        except HTTPError as exc:
            retryable = exc.code == 429 or 500 <= exc.code < 600
            if not retryable or attempt == RETRY_ATTEMPTS - 1:
                raise
        except (TimeoutError, URLError):
            if attempt == RETRY_ATTEMPTS - 1:
                raise
        delay = 2 ** attempt
        print(f"Publisher {method} request failed; retrying in {delay}s.", file=sys.stderr)
        time.sleep(delay)
    raise RuntimeError("Publisher request retry loop exited unexpectedly.")


def publish_config(token: str, release_fields: dict, version_code: int, version_name: str) -> dict:
    identity = f"fe:{version_code}:{version_name}".encode("utf-8")
    base_key = f"fe-config-{hashlib.sha256(identity).hexdigest()}"
    for attempt in range(RETRY_ATTEMPTS):
        current_config = request_publisher_config("GET", token)
        if "configRevision" not in current_config:
            raise ValueError("Publisher GET response is missing configRevision.")
        if not isinstance(current_config.get("changelogContent"), str):
            raise ValueError("Publisher GET response is missing changelogContent.")

        updated_config = {
            key: value
            for key, value in current_config.items()
            if key not in READ_ONLY_FIELDS
        }
        updated_config.update(release_fields)

        # 幂等键必须跟随 revision：重试时 payload 已变，复用旧键会被后端判定为
        # "Idempotency-Key 已用于不同请求" 并返回 409，形成永远失败的死循环。
        idempotency_key = f"{base_key}-r{current_config['configRevision']}"
        try:
            published_config = request_publisher_config(
                "PUT",
                token,
                updated_config,
                idempotency_key=idempotency_key,
            )
            if "configRevision" not in published_config:
                raise ValueError("Publisher PUT response is missing configRevision.")
            if not isinstance(published_config.get("changelogContent"), str):
                raise ValueError("Publisher PUT response is missing changelogContent.")
            return published_config
        except HTTPError as exc:
            if exc.code != 409 or attempt == RETRY_ATTEMPTS - 1:
                raise
            delay = 2 ** attempt
            print(f"Publisher config revision changed; retrying in {delay}s.", file=sys.stderr)
            time.sleep(delay)
    raise RuntimeError("Publisher config publish retry loop exited unexpectedly.")


def mirror_config(published_config: dict, provider: str, repo: str, branch: str, changelog_file: str) -> dict:
    config = {
        key: value
        for key, value in published_config.items()
        if key not in MIRROR_EXCLUDED_FIELDS
    }
    config["changelogUrl"] = raw_url(provider, repo, branch, changelog_file)
    return config


def write_if_changed(path: Path, content: str) -> bool:
    old_content = path.read_text(encoding="utf-8") if path.exists() else None
    if old_content == content:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return True


def sync_files(
    provider: str,
    repo: str,
    branch: str,
    username: str | None,
    token: str,
    version_name: str,
    config_file: str,
    changelog_file: str,
    generated_config: dict,
    changelog_content: str,
) -> None:
    config_content = json.dumps(generated_config, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    for attempt in range(RETRY_ATTEMPTS):
        with tempfile.TemporaryDirectory(prefix=f"fe-{provider}-config-") as temp_dir:
            repo_dir = clone_repo(provider, repo, branch, username, token, Path(temp_dir))
            changed_files: list[str] = []
            if write_if_changed(repo_dir / config_file, config_content):
                changed_files.append(config_file)
            if write_if_changed(repo_dir / changelog_file, changelog_content):
                changed_files.append(changelog_file)
            if not changed_files:
                print(f"{provider} config repository has no changes.")
                return
            run_command(["git", "config", "user.name", "github-actions[bot]"], cwd=repo_dir)
            run_command(["git", "config", "user.email", "github-actions[bot]@users.noreply.github.com"], cwd=repo_dir)
            run_command(["git", "add", *changed_files], cwd=repo_dir)
            run_command(["git", "commit", "-m", f"Update Fe v{version_name} config"], cwd=repo_dir)
            try:
                run_command(["git", "push"], cwd=repo_dir)
            except subprocess.CalledProcessError:
                if attempt == RETRY_ATTEMPTS - 1:
                    raise
                delay = 2 ** attempt
                print(f"{provider} config push failed; recloning in {delay}s.", file=sys.stderr)
                time.sleep(delay)
                continue
            print(f"{provider} config repository updated: {', '.join(changed_files)}")
            return


def main() -> int:
    parser = argparse.ArgumentParser(description="Publish release config and mirror it to remote config repositories")
    parser.add_argument("--version-code", required=True, type=int, help="Android versionCode")
    parser.add_argument("--version-name", required=True, help="Android versionName")
    parser.add_argument("--apk-url", default=DEFAULT_DOWNLOAD_URL, help="Default APK download URL")
    parser.add_argument("--message-file", default="current_changelog.md", help="Current changelog text file")
    parser.add_argument("--local-config-file", default=os.getenv("LOCAL_CONFIG_FILE", DEFAULT_LOCAL_CONFIG_FILE))
    parser.add_argument("--source-changelog-file", default=os.getenv("SOURCE_CHANGELOG_FILE", DEFAULT_CHANGELOG_FILE))
    parser.add_argument("--config-file", default=os.getenv("REMOTE_CONFIG_FILE", DEFAULT_CONFIG_FILE))
    parser.add_argument("--changelog-file", default=os.getenv("REMOTE_CHANGELOG_FILE", DEFAULT_CHANGELOG_FILE))

    parser.add_argument("--github-repo", default=os.getenv("CONFIG_GITHUB_REPO", DEFAULT_GITHUB_REPO))
    parser.add_argument("--github-branch", default=os.getenv("CONFIG_GITHUB_BRANCH", DEFAULT_GITHUB_BRANCH))
    parser.add_argument("--github-token", default=os.getenv("CONFIG_GITHUB_TOKEN") or os.getenv("GH_CONFIG_TOKEN"))

    parser.add_argument("--gitee-repo", default=os.getenv("GITEE_CONFIG_REPO", DEFAULT_GITEE_REPO))
    parser.add_argument("--gitee-branch", default=os.getenv("GITEE_CONFIG_BRANCH", DEFAULT_GITEE_BRANCH))
    parser.add_argument("--gitee-username", default=os.getenv("GITEE_USERNAME"))
    parser.add_argument("--gitee-token", default=os.getenv("GITEE_TOKEN"))
    parser.add_argument("--publisher-token", default=os.getenv("LASTUDIO_PUBLISHER_TOKEN"))
    args = parser.parse_args()

    try:
        local_config_file = Path(args.local_config_file)
        source_changelog_file = Path(args.source_changelog_file)
        if not local_config_file.exists():
            raise FileNotFoundError(f"Local config file not found: {local_config_file}")
        if not source_changelog_file.exists():
            raise FileNotFoundError(f"Source changelog file not found: {source_changelog_file}")
        if not args.publisher_token:
            raise ValueError("LASTUDIO_PUBLISHER_TOKEN is not configured.")

        update_message = read_text(Path(args.message_file) if args.message_file else None)
        release_fields = build_release_fields(
            local_config_file=local_config_file,
            version_code=args.version_code,
            version_name=args.version_name,
            apk_url=args.apk_url,
            update_message=update_message,
            changelog_content=read_exact_text(source_changelog_file),
        )
        published_config = publish_config(
            args.publisher_token,
            release_fields,
            args.version_code,
            args.version_name,
        )

        synced_any = False
        if args.github_token:
            sync_files(
                provider="github",
                repo=args.github_repo,
                branch=args.github_branch,
                username=None,
                token=args.github_token,
                version_name=args.version_name,
                config_file=args.config_file,
                changelog_file=args.changelog_file,
                generated_config=mirror_config(
                    published_config,
                    "github",
                    args.github_repo,
                    args.github_branch,
                    args.changelog_file,
                ),
                changelog_content=published_config["changelogContent"],
            )
            synced_any = True
        else:
            print("CONFIG_GITHUB_TOKEN is not configured; skipping GitHub config repository sync.")

        if args.gitee_username and args.gitee_token:
            sync_files(
                provider="gitee",
                repo=args.gitee_repo,
                branch=args.gitee_branch,
                username=args.gitee_username,
                token=args.gitee_token,
                version_name=args.version_name,
                config_file=args.config_file,
                changelog_file=args.changelog_file,
                generated_config=mirror_config(
                    published_config,
                    "gitee",
                    args.gitee_repo,
                    args.gitee_branch,
                    args.changelog_file,
                ),
                changelog_content=published_config["changelogContent"],
            )
            synced_any = True
        else:
            print("GITEE_USERNAME or GITEE_TOKEN is not configured; skipping Gitee config repository sync.")

        if not synced_any:
            raise ValueError("No config repository credentials are configured.")
    except subprocess.CalledProcessError as exc:
        print(f"Command failed with exit code {exc.returncode}", file=sys.stderr)
        return exc.returncode or 1
    except Exception as exc:
        print(f"Config sync failed: {exc}", file=sys.stderr)
        return 1

    print("Remote config sync completed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
