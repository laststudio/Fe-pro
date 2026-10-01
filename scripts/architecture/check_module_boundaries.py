#!/usr/bin/env python3
"""Validate Gradle module dependency direction and detect cycles."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


MODULE_PATTERN = re.compile(r'include\("(:[^"]+)"\)')
DEPENDENCY_PATTERN = re.compile(r'project\("(:[^"]+)"\)')


def project_dir(root: Path, module: str) -> Path:
    name = module.removeprefix(":")
    return root / name.replace(":", "/")


def module_kind(module: str) -> str:
    if module == ":app":
        return "app"
    parts = module.removeprefix(":").split(":")
    return parts[0] if parts else "unknown"


def parse_modules(root: Path) -> list[str]:
    settings = (root / "settings.gradle.kts").read_text(encoding="utf-8")
    return MODULE_PATTERN.findall(settings)


def parse_dependencies(root: Path, modules: list[str]) -> dict[str, set[str]]:
    graph = {module: set() for module in modules}
    for module in modules:
        build = project_dir(root, module) / "build.gradle.kts"
        if not build.exists():
            continue
        dependencies = DEPENDENCY_PATTERN.findall(build.read_text(encoding="utf-8"))
        graph[module].update(dep for dep in dependencies if dep in graph)
    return graph


def find_cycles(graph: dict[str, set[str]]) -> list[list[str]]:
    cycles: list[list[str]] = []
    visiting: list[str] = []
    visited: set[str] = set()

    def visit(module: str) -> None:
        if module in visiting:
            start = visiting.index(module)
            cycles.append(visiting[start:] + [module])
            return
        if module in visited:
            return
        visiting.append(module)
        for dependency in graph.get(module, ()):
            visit(dependency)
        visiting.pop()
        visited.add(module)

    for module in graph:
        visit(module)
    return cycles


def violations(graph: dict[str, set[str]]) -> list[str]:
    errors: list[str] = []
    for module, dependencies in graph.items():
        source_kind = module_kind(module)
        for dependency in dependencies:
            target_kind = module_kind(dependency)
            # :feature:shared is a sanctioned shared-UI layer that other features
            # may depend on; feature-to-feature dependencies are otherwise forbidden.
            if source_kind == "feature" and target_kind == "feature" and dependency != ":feature:shared":
                errors.append(f"{module} must not depend on feature {dependency}")
            if source_kind == "data" and target_kind == "feature":
                errors.append(f"{module} must not depend on feature {dependency}")
            if source_kind == "core" and target_kind in {"data", "feature"}:
                errors.append(f"{module} must not depend on {target_kind} {dependency}")
    return errors


MAX_PRODUCTION_LINES = 800
MAX_SCREEN_LINES = 500


def check_file_sizes(root: Path) -> list[str]:
    """Largest production Kotlin files must stay within the size budget."""
    errors: list[str] = []
    search_roots = ["app/src", "core", "data", "feature"]
    for relative_root in search_roots:
        base = root / relative_root
        if not base.exists():
            continue
        for path in base.rglob("*.kt"):
            if "/build/" in path.as_posix():
                continue
            lines = len(path.read_text(encoding="utf-8").splitlines())
            rel = path.relative_to(root).as_posix()
            if lines > MAX_PRODUCTION_LINES:
                errors.append(f"{rel} has {lines} lines (limit {MAX_PRODUCTION_LINES})")
            if path.name.endswith("Screen.kt") and lines > MAX_SCREEN_LINES:
                errors.append(f"{rel} has {lines} lines (UI limit {MAX_SCREEN_LINES})")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path.cwd())
    args = parser.parse_args()
    root = args.root.resolve()

    modules = parse_modules(root)
    graph = parse_dependencies(root, modules)
    errors = (
        violations(graph)
        + ["dependency cycle: " + " -> ".join(cycle) for cycle in find_cycles(graph)]
        + check_file_sizes(root)
    )

    print("Architecture modules:")
    for module in modules:
        deps = ", ".join(sorted(graph[module])) or "(none)"
        print(f"  {module}: {deps}")

    if errors:
        print("\nBoundary violations:", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        return 1
    print("\nModule boundary check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
