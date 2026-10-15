#!/usr/bin/env bash
set -euo pipefail
previous_apk=$1
updated_apk=$2
expected_code=$3
report=$4
package=com.creditrepairai.v2
# Disposable emulator only. No user device is touched and no account is used.
adb wait-for-device
adb install "$previous_apk"
adb shell am start -W -n "$package/.MainActivity"
adb install -r "$updated_apk"
adb shell am force-stop "$package"
adb logcat -c
adb shell am start -W -n "$package/.MainActivity"
for attempt in $(seq 1 10); do
  if adb shell pidof "$package" >/dev/null; then break; fi
  sleep 1
done
sleep 3
adb shell pidof "$package" >/dev/null
adb shell dumpsys package "$package" | grep -F "versionCode=$expected_code "
if adb logcat -d -s AndroidRuntime:E | grep -F "Process: $package,"; then
  echo 'Release process crashed after installation.' >&2
  exit 1
fi
printf '{"previousInstall":"passed","inPlaceUpdate":"passed","releaseLaunch":"passed","package":"%s","versionCode":%s,"scope":"API-35 emulator; no live sign-in or purchases"}\n' "$package" "$expected_code" > "$report"
