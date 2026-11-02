#!/usr/bin/env bash
# Takes Order Desk's screenshots on a running Android emulator: ci/android-shots.sh <out dir> <scene>...
set -euo pipefail
out=$1; shift
mkdir -p "$out"
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
# A clean status bar: demo mode, 9:41, full battery and signal, no notifications.
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" > /dev/null; }
demo enter
demo clock -e hhmm 0941
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e level 4 -e datatype none
demo notifications -e visible false
for scene in "$@"; do
  adb shell am start -S -W -n io.github.kormcrete.orderdesk/io.github.kormcrete.orderdesk.android.MainActivity --es scene "$scene" > /dev/null
  sleep 8
  adb exec-out screencap -p > "$out/android-$scene.png"
  echo "android-$scene.png: $(wc -c < "$out/android-$scene.png") bytes"
done
