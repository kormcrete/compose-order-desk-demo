#!/usr/bin/env bash
# Adds a JDK AOT cache to the packaged desktop app, builds the installer from it, installs it, and checks it.
#
# An AOT cache (JEP 483/514) holds classes already loaded and linked, and method profiles, from a training run,
# so the app starts faster. The JVM only uses it with the same JDK and the same class path, jars at the same
# paths: so the app is trained where its installer puts it, the cache goes inside the app, and the installer is
# built from that app.
#
# Usage, from the repository root after `./gradlew :desktopApp:createReleaseDistributable`:
#   desktopApp/aot/train-and-package.sh <JDK 25 home> <output dir>
# Linux: /opt/order-desk, .deb · macOS: /Applications/Order Desk.app, .dmg · Windows: C:\Program Files\Order Desk, .msi
set -euo pipefail

jdk=$1
out=$2
mkdir -p "$out"
out=$(cd "$out" && pwd)
image=$(find desktopApp/build/compose/binaries/main-release/app -maxdepth 1 -mindepth 1 | head -1)
name="Order Desk"
version=1.0.0
sudo=""
# macOS has no `timeout`; perl's alarm does the same everywhere.
limit() { perl -e 'alarm shift; exec @ARGV' "$@"; }
[ "$(id -u 2>/dev/null || echo 0)" != 0 ] && command -v sudo > /dev/null && sudo=sudo

case "$(uname -s)" in
  Linux)
    install="/opt/order-desk"
    appdir="$install/lib/app"
    launcher="$install/bin/$name"
    runtime="$install/lib/runtime"
    type=deb
    run() { limit 120 xvfb-run -a -s "-screen 0 1920x1080x24" "$@"; }
    ;;
  Darwin)
    install="/Applications/$name.app"
    appdir="$install/Contents/app"
    launcher="$install/Contents/MacOS/$name"
    runtime="$install/Contents/runtime/Contents/Home"
    type=dmg
    run() { limit 120 "$@"; }
    ;;
  *)
    install="C:/Program Files/$name"
    appdir="$install/app"
    launcher="$install/$name.exe"
    runtime="$install/runtime"
    out=$(cygpath -m "$out")
    type=none # building an .msi needs the WiX toolset; the trained app is checked where it was trained
    run() { limit 120 "$@"; }
    ;;
esac
cfg="$appdir/$name.cfg"

echo "== training at $install"
$sudo rm -rf "$install"
$sudo mkdir -p "$(dirname "$install")"
$sudo cp -a "$image" "$install"
$sudo chmod -R a+rwX "$install"
# JDK 25 writes the cache from a second java process; the trimmed runtime has no java command, so the
# matching JDK's launcher stands in during training (same JDK build as the runtime) and is removed after.
exe=""
[ -f "$jdk/bin/java.exe" ] && exe=.exe
had_java=no
[ -f "$runtime/bin/java$exe" ] && had_java=yes
$sudo mkdir -p "$runtime/bin"
[ $had_java = no ] && $sudo cp "$jdk/bin/java$exe" "$runtime/bin/java$exe"
# Quoted: the install path may contain spaces.
JAVA_TOOL_OPTIONS="\"-XX:AOTCacheOutput=$appdir/orderdesk.aot\" \"-Dorderdesk.probe=$out/training.txt\"" run "$launcher" || true
[ $had_java = no ] && $sudo rm -f "$runtime/bin/java$exe"
rm -f "$appdir/orderdesk.aot.config"
ls -l "$appdir/orderdesk.aot"

# The launcher reads its JVM options from the .cfg next to the jars; $APPDIR is resolved by the launcher.
awk '{ print } /^\[JavaOptions\]/ { print "java-options=-XX:AOTCache=$APPDIR/orderdesk.aot" }' "$cfg" > "$out/app.cfg"
cp "$out/app.cfg" "$cfg"
grep -n 'AOTCache' "$cfg"

if [ "$type" != none ]; then
echo "== packaging the trained app as $type"
trained="$out/image"
rm -rf "$trained"
mkdir -p "$trained"
cp -a "$install" "$trained/"
"$jdk/bin/jpackage" --type "$type" --app-image "$trained/$(basename "$install")" --name "$name" \
  --app-version "$version" --vendor Kormcrete --dest "$out"
ls -l "$out"

echo "== installing the $type and checking the cache"
$sudo rm -rf "$install"
case "$type" in
  deb) $sudo dpkg -i "$out"/*.deb > /dev/null ;;
  dmg)
    mount=$(hdiutil attach -nobrowse "$out"/*.dmg | tail -1 | cut -f3-)
    cp -a "$mount/$name.app" /Applications/
    hdiutil detach "$mount" > /dev/null
    ;;
esac
fi
JAVA_TOOL_OPTIONS="\"-Dorderdesk.probe=$out/memory-aot.txt\" \"-Xlog:class+load:file=$out/classload.txt\"" run "$launcher" || true
total=$(grep -c 'source:' "$out/classload.txt" || true)
cached=$(grep -c 'source: shared objects file' "$out/classload.txt" || true)
echo "classes loaded: $total, from the AOT cache: $cached" | tee "$out/aot.txt"
