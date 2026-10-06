#!/usr/bin/env bash
# 用生产 keystore 出**正式签名**的 release 包，并核验签名指纹。
#
#   FE_KS_PASS='口令' [FE_KEY_ALIAS='别名'] [FE_KEY_PASS='密钥口令'] \
#       ./tools/build-release-signed.sh [--install]
#
# 需要：FE_KS_PASS（keystore 口令）。别名可留空 —— 脚本会用口令列出别名，
#       只有一个时自动采用。
# 产出：app/build/outputs/apk/release/app-release.apk（生产签名）
# 顺带：
#   1. keystore 复制到 ASCII 临时路径 —— 路径含中文时 keytool 在本机会报
#      "Invalid file path"（实测），复制后规避；
#   2. 构建后打印并校验签名证书 SHA-256 是否等于生产指纹
#      （官方 Fe_2.1.0.apk 的 Signer #1 = 95fdebb3682f7c52…）；
#   3. 传 --install 时自动 adb install -r。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KS_SRC="${FE_KS_PATH:-$ROOT/../Fe签名/Fe}"
PROD_SHA="${FE_PROD_SHA256:-95fdebb3682f7c529584cfe11f27b73557426d211d53688beaa18434fdd84cfe}"
: "${FE_KS_PASS:?请通过 FE_KS_PASS 提供 keystore 口令}"
KEYTOOL="${KEYTOOL:-$(command -v keytool)}"
[ -f "$KS_SRC" ] || { echo "找不到 keystore: $KS_SRC"; exit 1; }

WORK="$(mktemp -d)"; KS="$WORK/fe_prod.p12"
cp "$KS_SRC" "$KS"

ALIAS="${FE_KEY_ALIAS:-}"
if [ -z "$ALIAS" ]; then
  ALIASES=$(LANG=C "$KEYTOOL" -list -keystore "$KS" -storetype PKCS12 -storepass "$FE_KS_PASS" 2>/dev/null \
            | sed -n 's/^\(.*\),[^,]*$/\1/p' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | grep -v '^$' || true)
  COUNT=$(echo "$ALIASES" | grep -c . || true)
  if [ "$COUNT" != "1" ]; then echo "别名需要显式指定（候选：$(echo $ALIASES | tr '\n' ' ')）"; exit 1; fi
  ALIAS="$ALIASES"
fi
echo "① keystore: $KS_SRC（已复制到 ASCII 临时路径）  别名: $ALIAS"

ANDROID_KEYSTORE_PATH="$KS" \
ANDROID_KEYSTORE_PASSWORD="$FE_KS_PASS" \
ANDROID_KEY_ALIAS="$ALIAS" \
ANDROID_KEY_PASSWORD="${FE_KEY_PASS:-$FE_KS_PASS}" \
EXPECTED_SIGNATURE_SHA256="$PROD_SHA" \
  "$ROOT/gradlew" -p "$ROOT" :app:assembleRelease

APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
APKSIGNER="$(ls "${ANDROID_HOME:-$HOME/Android/Sdk}"/build-tools/*/apksigner 2>/dev/null | tail -1)"
echo "② 产物: $APK"
if [ -n "$APKSIGNER" ]; then
  SHA=$("$APKSIGNER" verify --print-certs "$APK" 2>/dev/null | sed -n 's/.*SHA-256 digest: *//p' | head -1 | tr -d '\r')
  echo "   签名证书 SHA-256: $SHA"
  if [ "$(echo "$SHA" | tr 'A-Z' 'a-z')" = "$PROD_SHA" ]; then echo "   ✅ 与生产签名一致"; else echo "   ❌ 与生产签名不一致（期望 $PROD_SHA）"; exit 1; fi
fi
if [ "${1:-}" = "--install" ]; then
  adb install -r -d "$APK"
fi
