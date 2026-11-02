#!/usr/bin/env bash
# Adds a JDK AOT cache to the desktop app's installer, installs it, and checks that the JVM uses the cache.
#
# An AOT cache (JEP 483/514) holds classes already loaded and linked, and method profiles, from a training run,
# so the app starts faster. The JVM uses it only with the same JDK and the same jars: same paths, same sizes,
# same timestamps. So the app is trained where its installer puts it, on the very files the installer installs,
# and the cache is added to the package without touching the jars.
#
# Usage, from the repository root after `./gradlew :desktopApp:createReleaseDistributable`:
#   desktopApp/aot/train-and-package.sh <JDK 25 home> <output dir>
# Linux: .deb into /opt/order-desk · macOS: .dmg with /Applications/Order Desk.app ·
# Windows: trained and checked in C:\Program Files\Order Desk (an .msi needs the WiX toolset; not built here).
set -euo pipefail

jdk=$1
out=$2
mkdir -p "$out"
out=$(cd "$out" && pwd)
image=$(cd "$(find desktopApp/build/compose/binaries/main-release/app -maxdepth 1 -mindepth 1 | head -1)" && pwd)
name="Order Desk"
version=1.0.0
sudo=""
[ "$(id -u 2>/dev/null || echo 0)" != 0 ] && command -v sudo > /dev/null && sudo=sudo
# A time limit for app runs: coreutils timeout where there is one (Linux, Windows' Git Bash), perl on macOS.
if command -v timeout > /dev/null; then limit() { timeout "$@"; }; else limit() { perl -e 'alarm shift; exec @ARGV' "$@"; }; fi

case "$(uname -s)" in
  Linux)
    install=/opt/order-desk; appdir="$install/lib/app"; launcher="$install/bin/$name"; runtime="$install/lib/runtime"
    run() { limit 120 xvfb-run -a -s "-screen 0 1920x1080x24" "$@"; } ;;
  Darwin)
    install="/Applications/$name.app"; appdir="$install/Contents/app"; launcher="$install/Contents/MacOS/$name"
    runtime="$install/Contents/runtime/Contents/Home"
    # Started the way Finder starts apps; the bare launcher exits at once in a CI session.
    run() { limit 120 open -n -W --env "JAVA_TOOL_OPTIONS=$JAVA_TOOL_OPTIONS" --stdout "$out/mac-app.log" --stderr "$out/mac-app.log" "$install"; } ;;
  *)
    install="C:/Program Files/$name"; appdir="$install/app"; launcher="$install/$name.exe"; runtime="$install/runtime"
    out=$(cygpath -m "$out")
    run() { limit 120 "$@"; } ;;
esac
cfg="$appdir/$name.cfg"
exe=""
[ -f "$jdk/bin/java.exe" ] && exe=.exe

# Runs the installed app through its main screens with JAVA_TOOL_OPTIONS (quoted: paths may have spaces).
probe() { JAVA_TOOL_OPTIONS="$1" run "$launcher" || true; }

train() {
  echo "== training at $install"
  # JDK 25 writes the cache from a second java process; the trimmed runtime has no java command, so the
  # launcher of the same JDK build stands in for the training run and is removed after.
  local added=no
  if [ ! -f "$runtime/bin/java$exe" ]; then
    $sudo mkdir -p "$runtime/bin"
    $sudo cp "$jdk/bin/java$exe" "$runtime/bin/java$exe"
    added=yes
  fi
  $sudo chmod -R a+rwX "$install"
  probe "\"-XX:AOTCacheOutput=$appdir/orderdesk.aot\" \"-Dorderdesk.probe=$out/training.txt\""
  [ $added = yes ] && $sudo rm -f "$runtime/bin/java$exe"
  ls -l "$appdir/orderdesk.aot"
  # The launcher reads its JVM options from the .cfg next to the jars and resolves $APPDIR itself.
  awk '{ print } /^\[JavaOptions\]/ { print "java-options=-XX:AOTCache=$APPDIR/orderdesk.aot" }' "$cfg" > "$out/app.cfg"
  $sudo cp "$out/app.cfg" "$cfg"
}

case "$(uname -s)" in
  Linux)
    echo "== packaging and installing the .deb"
    "$jdk/bin/jpackage" --type deb --app-image "$image" --name "$name" --app-version "$version" --vendor Kormcrete --dest "$out/plain"
    plain=$(ls "$out"/plain/*.deb)
    $sudo dpkg -i "$plain" > /dev/null
    train
    echo "== adding the cache to the .deb"
    rm -rf "$out/deb"
    dpkg-deb -R "$plain" "$out/deb"
    cp "$appdir/orderdesk.aot" "$out/deb$appdir/"
    cp "$cfg" "$out/deb$cfg"
    dpkg-deb -b --root-owner-group "$out/deb" "$out/$(basename "$plain")" > /dev/null
    $sudo dpkg -r order-desk > /dev/null
    $sudo dpkg -i "$out/$(basename "$plain")" > /dev/null
    ;;
  Darwin)
    $sudo rm -rf "$install"
    $sudo cp -a "$image" "$install"
    train
    echo "== building the .dmg from the trained app"
    rm -rf "$out/dmg"
    mkdir -p "$out/dmg"
    cp -a "$install" "$out/dmg/"
    ln -s /Applications "$out/dmg/Applications"
    hdiutil create -quiet -volname "$name" -srcfolder "$out/dmg" -ov -format UDZO "$out/$name-$version.dmg"
    $sudo rm -rf "$install"
    mount=$(hdiutil attach -nobrowse "$out/$name-$version.dmg" | tail -1 | cut -f3-)
    cp -a "$mount/$name.app" /Applications/
    hdiutil detach -quiet "$mount"
    ;;
  *)
    rm -rf "$install"
    cp -a "$image" "$install"
    train
    ;;
esac

echo "== checking the installed app"
probe "\"-Dorderdesk.probe=$out/memory-aot.txt\" \"-Xlog:class+load:file=$out/classload.txt\" \"-Xlog:aot=warning:file=$out/aot-warnings.txt\""
total=$(grep -c 'source:' "$out/classload.txt" || true)
cached=$(grep -c 'source: shared objects file' "$out/classload.txt" || true)
echo "classes loaded: $total, from the AOT cache: $cached" | tee "$out/aot.txt"
[ -s "$out/aot-warnings.txt" ] && head -5 "$out/aot-warnings.txt" | tee -a "$out/aot.txt"
ls -l "$out" | grep -E '\.(deb|dmg)$' || true
