#!/usr/bin/env bash
# Usage: run-ui-tests.sh <skiagl|skiavk>
renderer=${1:-skiagl}
exec > >(tee ci-script.log) 2>&1
echo "cwd=$(pwd) renderer=$renderer"; adb devices

# Landscape, like a tablet held normally.
adb shell settings put system accelerometer_rotation 0
for r in 1 0 3 2; do
  adb shell settings put system user_rotation $r; sleep 2
  dims=$(adb shell dumpsys window displays | grep -m1 -o 'cur=[0-9]*x[0-9]*' | sed 's/cur=//')
  w=${dims%x*}; h=${dims#*x}
  echo "rotation $r -> $dims"
  if [ -n "$w" ] && [ -n "$h" ] && [ "$w" -gt "$h" ]; then break; fi
done

# Choose the HWUI graphics backend: OpenGL (skiagl) or Vulkan (skiavk).
adb shell setprop debug.hwui.renderer "$renderer"
echo "debug.hwui.renderer=$(adb shell getprop debug.hwui.renderer)"

adb logcat -c
chmod +x gradlew
./gradlew connectedDebugAndroidTest --stacktrace > gradle-ui.log 2>&1
status=$?
adb logcat -d -v threadtime > logcat-full.txt
exit $status
