#!/usr/bin/env bash
set -euo pipefail

# NEXUS Document Intelligence - Collabora/LibreOffice Android bootstrap
# Run this on Linux. Collabora's native Android build is not supported on Windows.

ROOT="${1:-$HOME/nexus-collabora}"
SRC="$ROOT/collabora-office"

mkdir -p "$ROOT"

require() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

require git
require java
require python3

if [ ! -d "$SRC/.git" ]; then
  echo "[1/5] Cloning current Collabora Office/Online monorepo..."
  git clone https://gerrit.collaboraoffice.com/online "$SRC"
else
  echo "[1/5] Existing source found; fetching updates..."
  git -C "$SRC" fetch --all --prune
fi

cd "$SRC"

echo "[2/5] Recording upstream revision..."
git rev-parse HEAD | tee "$ROOT/UPSTREAM_COMMIT.txt"

echo "[3/5] Creating local NEXUS development branch if needed..."
if ! git show-ref --verify --quiet refs/heads/nexus/document-intelligence; then
  git switch -c nexus/document-intelligence
else
  git switch nexus/document-intelligence
fi

echo "[4/5] Checking Android source tree..."
if [ ! -d android ]; then
  echo "Expected android/ directory not found. Upstream layout may have changed." >&2
  exit 1
fi
if [ ! -d engine ]; then
  echo "Expected engine/ directory not found. Upstream layout may have changed." >&2
  exit 1
fi

echo "[5/5] Setup complete."
echo
printf '%s\n' \
  "Source: $SRC" \
  "Branch: nexus/document-intelligence" \
  "Upstream commit: $(cat "$ROOT/UPSTREAM_COMMIT.txt")" \
  "Next: follow Collabora Android build prerequisites, then port the NEXUS intelligence adapter into android/."
