#!/usr/bin/env bash
# Puts a folder of PNGs into <subfolder>/ of the `screenshots` branch, keeping the branch's other folders.
# ci/publish-screens.sh <source dir> <subfolder>   (GITHUB_TOKEN, GITHUB_REPOSITORY and GITHUB_SHA from Actions)
set -euo pipefail
src=$(cd "$1" && pwd)
sub=$2
url="https://x-access-token:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"
work=$(mktemp -d)
cd "$work"
if ! git clone -q --depth 1 -b screenshots "$url" pub 2> /dev/null; then
  git init -q -b screenshots pub
  git -C pub remote add origin "$url"
fi
cd pub
git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
rm -rf "$sub"
mkdir -p "$sub"
cp "$src"/*.png "$sub"/
git add -A
git commit -q -m "$sub: screens of ${GITHUB_SHA::7}" || { echo "no changes"; exit 0; }
for attempt in 1 2 3; do
  git push -q origin HEAD:screenshots && exit 0
  git pull -q --rebase origin screenshots
done
exit 1
