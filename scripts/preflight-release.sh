#!/bin/bash
# Pre-release checks for Maven Central deploy. Exits non-zero on any failure
# so subsequent steps don't proceed if anything looks off.
#
# Usage: scripts/preflight-release.sh [expected-version]
#
# If expected-version is given, fails when pom.xml's version doesn't match.
set -e

EXPECTED_VERSION="${1:-}"
PROJECT_NAME=$(basename "$(pwd)")
FAIL=0

red()    { printf "\033[0;31m%s\033[0m\n" "$*"; }
green()  { printf "\033[0;32m%s\033[0m\n" "$*"; }
yellow() { printf "\033[0;33m%s\033[0m\n" "$*"; }

echo "=== ${PROJECT_NAME} pre-release preflight ==="
echo

# 1. Off corporate zscaler / network reaches Sonatype Central Portal
echo "[1/7] Network connectivity to Sonatype Central Portal..."
if curl -sS -o /dev/null -m 8 -w "%{http_code}" "https://central.sonatype.com/" 2>/dev/null | grep -qE "^(200|301|302)$"; then
    green "      ✓ central.sonatype.com reachable"
else
    red "      ✗ central.sonatype.com NOT reachable (zscaler enabled? VPN?)"
    FAIL=$((FAIL + 1))
fi

# 2. Working tree clean
echo "[2/7] Working tree clean..."
if [ -z "$(git status --porcelain)" ]; then
    green "      ✓ no uncommitted changes"
else
    red "      ✗ uncommitted changes detected:"
    git status --porcelain | sed 's/^/        /'
    FAIL=$((FAIL + 1))
fi

# 3. On master branch
echo "[3/7] On master branch..."
BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$BRANCH" = "master" ]; then
    green "      ✓ on master"
else
    red "      ✗ on '${BRANCH}' (expected master)"
    FAIL=$((FAIL + 1))
fi

# 4. In sync with origin (not behind; ahead is OK — those will get released)
echo "[4/7] In sync with origin/master..."
git fetch origin --quiet 2>/dev/null || true
BEHIND=$(git rev-list HEAD..origin/master --count 2>/dev/null || echo 0)
AHEAD=$(git rev-list origin/master..HEAD --count 2>/dev/null || echo 0)
if [ "$BEHIND" -eq 0 ]; then
    if [ "$AHEAD" -eq 0 ]; then
        green "      ✓ identical to origin/master"
    else
        green "      ✓ ${AHEAD} commit(s) ahead of origin/master (these will be in the release)"
    fi
else
    red "      ✗ ${BEHIND} commit(s) behind origin/master — pull before releasing"
    FAIL=$((FAIL + 1))
fi

# 5. Version in pom.xml
echo "[5/7] Version check..."
VERSION=$(grep -m1 '<version>' pom.xml | sed -E 's|.*<version>([^<]+)</version>.*|\1|')
if [ -n "$EXPECTED_VERSION" ]; then
    if [ "$VERSION" = "$EXPECTED_VERSION" ]; then
        green "      ✓ pom.xml version is ${VERSION}"
    else
        red "      ✗ pom.xml version is ${VERSION} (expected ${EXPECTED_VERSION})"
        FAIL=$((FAIL + 1))
    fi
else
    green "      ✓ pom.xml version is ${VERSION} (no expected version supplied)"
fi
if echo "$VERSION" | grep -q "SNAPSHOT"; then
    red "      ✗ pom.xml is a SNAPSHOT — release versions must not contain SNAPSHOT"
    FAIL=$((FAIL + 1))
fi

# 6. GPG key available
echo "[6/7] GPG key..."
if gpg --list-secret-keys --keyid-format=long 2>/dev/null | grep -q '^sec'; then
    KEYID=$(gpg --list-secret-keys --keyid-format=long 2>/dev/null | grep '^sec' | head -1 | awk '{print $2}')
    green "      ✓ GPG secret key available (${KEYID})"
else
    red "      ✗ no GPG secret key found — gpg signing will fail"
    FAIL=$((FAIL + 1))
fi

# 7. Maven settings.xml has Sonatype credentials
echo "[7/7] Maven settings.xml has Sonatype credentials..."
SETTINGS_FILE="${HOME}/.m2/settings.xml"
if [ -f "$SETTINGS_FILE" ]; then
    if grep -qE 'central|sonatype|ossrh' "$SETTINGS_FILE" 2>/dev/null; then
        green "      ✓ settings.xml has central/sonatype/ossrh server entry"
    else
        yellow "      ⚠ settings.xml exists but no central/sonatype/ossrh entry — verify credentials"
    fi
else
    red "      ✗ ~/.m2/settings.xml not found"
    FAIL=$((FAIL + 1))
fi

echo
if [ "$FAIL" -eq 0 ]; then
    green "=== preflight: PASS — ready to deploy ==="
    exit 0
else
    red "=== preflight: ${FAIL} check(s) FAILED — fix before deploying ==="
    exit 1
fi
