#!/usr/bin/env bash
# Prints VERSION=<x.y.z> for the release workflow: from a vX.Y.Z tag, else 1.0.0 (a pull request or a manual run; a
# macOS app's version cannot start with 0).
set -euo pipefail
if [ "${GITHUB_REF_TYPE:-}" = tag ]; then
  VERSION="${GITHUB_REF_NAME#v}"
  if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "release tags look like v1.2.3, not $GITHUB_REF_NAME" >&2
    exit 1
  fi
else
  VERSION=1.0.0
fi
echo "VERSION=$VERSION"
