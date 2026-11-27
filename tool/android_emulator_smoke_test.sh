#!/usr/bin/env bash
# Runtime smoke test invoked by reactivecircus/android-emulator-runner.
# Keep the GitHub Action's "script" input to one line and keep shell logic here.
set -Eeuo pipefail

api_level="${1:-}"
case "$api_level" in
  26) expected_sdk=26 ;;
  37) expected_sdk=37 ;;
  *)
    echo "Usage: $0 {26|37}" >&2
    exit 2
    ;;
esac

apk="build/app/outputs/flutter-apk/app-github-release.apk"
package_name="dev.klangretro.player"
activity_name="com.looper.player.MainActivity"
serial="${ANDROID_SERIAL:-emulator-5554}"

dump_diagnostics() {
  echo "::group::Emulator diagnostics (API $api_level)"
  adb -s "$serial" devices -l || true
  adb -s "$serial" shell getprop ro.build.version.sdk || true
  adb -s "$serial" shell dumpsys activity activities 2>/dev/null | tail -n 80 || true
  adb -s "$serial" logcat -d -v threadtime 2>/dev/null | tail -n 250 || true
  echo "::endgroup::"
}

fail() {
  echo "API $api_level runtime smoke test failed: $*" >&2
  dump_diagnostics
  exit 1
}

if [[ ! -s "$apk" ]]; then
  fail "release APK is missing or empty: $apk"
fi

adb -s "$serial" wait-for-device
booted=""
for ((attempt = 0; attempt < 180; attempt++)); do
  booted="$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  [[ "$booted" == "1" ]] && break
  sleep 2
done
[[ "$booted" == "1" ]] || fail "emulator did not finish booting"

actual_sdk="$(adb -s "$serial" shell getprop ro.build.version.sdk | tr -d '\r')"
[[ "$actual_sdk" == "$expected_sdk" ]] || fail "expected Android API $expected_sdk, got API $actual_sdk"

adb -s "$serial" install -r "$apk" || fail "APK installation failed"
adb -s "$serial" logcat -c
adb -s "$serial" shell am force-stop "$package_name" || true
adb -s "$serial" shell am start -W -n "$package_name/$activity_name" || fail "activity launch failed"

app_pid=""
for ((attempt = 0; attempt < 45; attempt++)); do
  app_pid="$(adb -s "$serial" shell pidof "$package_name" 2>/dev/null | tr -d '\r' || true)"
  [[ -n "$app_pid" ]] && break
  sleep 1
done
[[ -n "$app_pid" ]] || fail "Klang process did not appear after launch"

sleep 10
app_pid_after="$(adb -s "$serial" shell pidof "$package_name" 2>/dev/null | tr -d '\r' || true)"
[[ -n "$app_pid_after" ]] || fail "Klang process exited shortly after launch"

fatal_log="$(adb -s "$serial" logcat -d -v brief | grep -E 'FATAL EXCEPTION|UnsatisfiedLinkError|NoClassDefFoundError|Fatal signal' || true)"
if [[ -n "$fatal_log" ]]; then
  echo "$fatal_log" >&2
  fail "fatal Java or native-library error detected"
fi

echo "API $api_level launched Klang successfully (pid $app_pid_after)."
