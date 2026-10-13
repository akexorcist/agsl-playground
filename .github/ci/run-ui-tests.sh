#!/usr/bin/env bash
exec > >(tee ci-script.log) 2>&1
echo "cwd=$(pwd)"; ls -la; ls -l /dev/kvm; adb devices
adb logcat -c
chmod +x gradlew
./gradlew connectedDebugAndroidTest --stacktrace > gradle-ui.log 2>&1
status=$?
adb logcat -d -v threadtime > logcat-full.txt
exit $status
