#!/usr/bin/env bash
adb logcat -c
./gradlew connectedDebugAndroidTest --stacktrace > gradle-ui.log 2>&1
status=$?
adb logcat -d -v threadtime > logcat-full.txt
exit $status
