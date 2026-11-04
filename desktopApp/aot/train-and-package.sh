#!/usr/bin/env bash
# Builds the desktop download of this computer, with a JDK AOT cache, and checks it the way a user gets it.
#
# An AOT cache (JEP 483/514) holds classes already loaded and linked, and method profiles, from a training run, so the
# app starts faster. The JVM uses it only with the same JDK and the same jars (sizes and timestamps), and otherwise starts
# without it, silently. So the jars get their final form first, the app is trained on them, and the check at the end
# starts the installed or unzipped package and fails if the cache is not in use.
#
# Usage, from the repository root after `./gradlew :desktopApp:createReleaseDistributable`:
#   desktopApp/aot/train-and-package.sh <JDK 25 home> <output dir> [<version>]
# Into the output dir, as kormcrete-order-desk-compose-…: macos-<arch>.dmg (an ad-hoc signed app, /Applications while
# it trains) · linux-x64.deb (/opt) · windows-x64.zip (a folder that runs from anywhere).
set -euo pipefail

jdk=$1
out=$2
version=${3:-1.0.0}
mkdir -p "$out"
out=$(cd "$out" && pwd)
root=$PWD
name="Kormcrete Order Desk Compose"
asset=kormcrete-order-desk-compose
image=$(cd "$(find desktopApp/build/compose/binaries/main-release/app -maxdepth 1 -mindepth 1 | head -1)" && pwd)
# A time limit for app runs: coreutils timeout where there is one (Linux, Windows' Git Bash), perl on macOS.
if command -v timeout > /dev/null; then limit() { timeout "$@"; }; else limit() { perl -e 'alarm shift; exec @ARGV' "$@"; }; fi

sudo=""
case "$(uname -s)" in
  Linux)
    [ "$(id -u)" != 0 ] && sudo=sudo
    imageapp="$image/lib/app"
    install="/opt/$asset"; appdir="$install/lib/app"; launcher="$install/bin/$name"
    run() { limit 120 xvfb-run -a -s "-screen 0 1920x1080x24" "$@"; } ;;
  Darwin)
    imageapp="$image/Contents/app"
    install="/Applications/$name.app"; appdir="$install/Contents/app"; launcher="$install/Contents/MacOS/$name"
    # Started the way Finder starts apps; the bare launcher exits at once in a CI session.
    run() { limit 120 open -n -W --env "JAVA_TOOL_OPTIONS=$JAVA_TOOL_OPTIONS" --stdout "$out/mac-app.log" --stderr "$out/mac-app.log" "$install"; } ;;
  *)
    imageapp="$image/app"
    install="C:/Program Files/$name"; appdir="$install/app"; launcher="$install/$name.exe"
    out=$(cygpath -m "$out")
    run() { limit 120 "$@"; } ;;
esac

# Runs the app through its main screens with JAVA_TOOL_OPTIONS (quoted: paths may have spaces).
probe() { JAVA_TOOL_OPTIONS="$1" run "$launcher" || true; }

# The jars' final form: nothing may change them after training.
prepare() {
  if [ "$(uname -s)" = Darwin ]; then
    # The Gradle run's architecture picks Skia's library: an Intel JDK on Apple Silicon makes an app that does not fit.
    local arch
    case "$(uname -m)" in arm64) arch=arm64 ;; *) arch=x64 ;; esac
    [ -f "$imageapp/libskiko-macos-$arch.dylib" ] || {
      echo "No libskiko-macos-$arch.dylib in the app: Gradle ran on another architecture than this computer's." >&2
      exit 1
    }
  fi
  # A few JDK files come read-only, and xattr -dr com.apple.quarantine (the README) has to be able to change them.
  chmod -R u+w "$image"
  mkdir -p "$imageapp/legal"
  cp "$root/LICENSE" "$root/NOTICE" "$root/packaging/legal/INTER-OFL.txt" "$imageapp/legal/"
  # An even second: a zip's plain timestamps have two-second steps. The cache compares the jar's time in seconds.
  find "$imageapp" -name '*.jar' -exec touch -t 202601011200.00 {} +
}

train() {
  echo "== training at $install"
  # In two steps, both by the app's own launcher: the trimmed runtime has no java command for JDK 25's one-step
  # -XX:AOTCacheOutput. The first run records what the app does, the second writes the cache and exits.
  probe "\"-XX:AOTMode=record\" \"-XX:AOTConfiguration=$out/orderdesk.aotconf\" \"-Dorderdesk.probe=$out/training.txt\""
  probe "\"-XX:AOTMode=create\" \"-XX:AOTConfiguration=$out/orderdesk.aotconf\" \"-XX:AOTCache=$out/orderdesk.aot\""
  [ -s "$out/orderdesk.aot" ] || { echo "Training wrote no cache." >&2; exit 1; }
  $sudo cp "$out/orderdesk.aot" "$appdir/orderdesk.aot"
  ls -l "$appdir/orderdesk.aot"
  # The launcher reads its JVM options from the .cfg next to the jars and resolves $APPDIR itself.
  awk '{ print } /^\[JavaOptions\]/ { print "java-options=-XX:AOTCache=$APPDIR/orderdesk.aot" }' "$appdir/$name.cfg" > "$out/app.cfg"
  $sudo cp "$out/app.cfg" "$appdir/$name.cfg"
}

prepare
rm -f "$out"/training.txt "$out"/memory-aot.txt "$out"/classload.txt "$out"/aot-warnings.txt "$out"/orderdesk.aot*
case "$(uname -s)" in
  Linux)
    echo "== packaging and installing the .deb"
    rm -rf "$out/plain"
    "$jdk/bin/jpackage" --type deb --app-image "$image" --name "$name" --app-version "$version" --vendor Kormcrete \
      --description "Order Desk in Compose Multiplatform" --license-file LICENSE \
      --linux-package-name "$asset" --linux-shortcut --linux-menu-group Office --dest "$out/plain"
    plain=$(ls "$out"/plain/*.deb)
    $sudo apt-get install -y -qq "$plain" > /dev/null
    train
    echo "== adding the cache to the .deb"
    rm -rf "$out/deb"
    dpkg-deb -R "$plain" "$out/deb"
    cp "$appdir/orderdesk.aot" "$out/deb$appdir/"
    cp "$appdir/$name.cfg" "$out/deb$appdir/"
    file="$out/$asset-linux-x64.deb"
    dpkg-deb -b -Zxz -z9 --root-owner-group "$out/deb" "$file" > /dev/null
    $sudo dpkg -r "$asset" > /dev/null
    $sudo apt-get install -y -qq "$file" > /dev/null
    rm -rf "$out/deb" "$out/plain"
    ;;
  Darwin)
    rm -rf "$install"
    cp -a "$image" "$install"
    train
    # Adding the cache broke the signature: without a valid one macOS reports the download as damaged.
    codesign --force --deep --sign - "$install"
    codesign --verify --deep --strict "$install"
    echo "== building the .dmg from the trained app"
    case "$(uname -m)" in arm64) arch=arm64 ;; *) arch=x64 ;; esac
    file="$out/$asset-macos-$arch.dmg"
    rm -rf "$out/dmg" "$file"
    mkdir -p "$out/dmg"
    cp -a "$install" "$out/dmg/"
    ln -s /Applications "$out/dmg/Applications"
    # LZMA: the smallest of the formats macOS reads (10.15 and later).
    hdiutil create -quiet -volname "$name" -srcfolder "$out/dmg" -fs HFS+ -format ULMO "$file"
    rm -rf "$install" "$out/dmg"
    mount=$(hdiutil attach -nobrowse -readonly "$file" | tail -1 | awk -F'\t' '{print $NF}')
    cp -a "$mount/$name.app" /Applications/
    hdiutil detach -quiet "$mount"
    ;;
  *)
    rm -rf "$install"
    cp -a "$image" "$install"
    train
    echo "== building the .zip"
    file="$out/$asset-windows-x64.zip"
    rm -f "$file"
    # 7-Zip also stores the exact UTC times: a zip's plain times are local, and would shift in another time zone.
    (cd "$install/.." && 7z a -tzip -bd -mx=9 "$file" "$name" > /dev/null)
    rm -rf "$install"
    # Unzipped as a user would, by Explorer's own code, and in another time zone than the machine that built it.
    tzutil //s "W. Europe Standard Time"
    unzipped=$(cygpath -w "$out/unzipped")
    rm -rf "$out/unzipped"
    mkdir -p "$out/unzipped"
    powershell -NoProfile -Command "\$s = New-Object -ComObject Shell.Application; \$s.NameSpace('$unzipped').CopyHere(\$s.NameSpace('$(cygpath -w "$file")').Items(), 0x14)"
    install="$out/unzipped/$name"; appdir="$install/app"; launcher="$install/$name.exe"
    for _ in $(seq 1 120); do [ -f "$appdir/orderdesk.aot" ] && [ -f "$launcher" ] && break; sleep 1; done
    ;;
esac

echo "== checking the download"
probe "\"-Dorderdesk.probe=$out/memory-aot.txt\" \"-Xlog:class+load:file=$out/classload.txt\" \"-Xlog:aot=warning:file=$out/aot-warnings.txt\""
total=$(grep -c 'source:' "$out/classload.txt" || true)
cached=$(grep -c 'source: shared objects file' "$out/classload.txt" || true)
echo "classes loaded: $total, from the AOT cache: $cached" | tee "$out/aot.txt"
[ -s "$out/aot-warnings.txt" ] && head -5 "$out/aot-warnings.txt" | tee -a "$out/aot.txt"
[ -s "$out/memory-aot.txt" ] || { echo "The app did not finish its walk through the screens." >&2; exit 1; }
cat "$out/memory-aot.txt"
grep -q '^splash on screen' "$out/memory-aot.txt" || { echo "The download starts without its splash." >&2; exit 1; }
[ "$total" -gt 0 ] && [ $((cached * 100 / total)) -ge 90 ] || { echo "The download does not use its AOT cache." >&2; exit 1; }
du -h "$file"
