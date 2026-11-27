#!/usr/bin/env bash
# Runtime smoke test invoked by reactivecircus/android-emulator-runner.
# Keep the GitHub Action's "script" input to one line and keep shell logic here.
set -Eeuo pipefail

api_level="${1:-}"
case "$api_level" in
  26) expected_sdk=26; expected_page_size=4096 ;;
  37) expected_sdk=37; expected_page_size=16384 ;;
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
  adb -s "$serial" shell 'LD_SHOW_AUXV=1 /system/bin/ls /' 2>&1 | grep -m1 '^AT_PAGESZ' || true
  adb -s "$serial" shell getprop ro.boot.hardware.cpu.pagesize || true
  adb -s "$serial" shell getprop ro.product.page_size || true
  adb -s "$serial" shell getprop ro.product.cpu.pagesize.max || true
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

if [[ "$api_level" == "37" ]]; then
  # Read the page size exposed to applications by the dynamic linker.
  # The x86_64 16 KB emulator uses a 4 KB host kernel but exposes 16 KB
  # application pages, so KernelPageSize in smaps is deliberately not used.
  page_size_line="$(adb -s "$serial" shell 'LD_SHOW_AUXV=1 /system/bin/ls /' 2>&1 | grep -m1 '^AT_PAGESZ' || true)"
  actual_page_size="$(awk '{print $2}' <<< "$page_size_line")"
  [[ "$actual_page_size" =~ ^[0-9]+$ ]] || fail "could not read AT_PAGESZ from the Android auxiliary vector: $page_size_line"
  [[ "$actual_page_size" == "$expected_page_size" ]] || fail "expected application page size $expected_page_size bytes, got $actual_page_size"
else
  echo "API 26 page-size assertion skipped; 16 KB page-size validation is performed on API 37."
fi

adb -s "$serial" install -r "$apk" || fail "APK installation failed"
# Some API 26 images deny clearing the main log buffer; this is optional.
if ! adb -s "$serial" logcat -c; then
  echo "Warning: API $api_level log buffers could not be cleared; continuing with launch checks."
fi
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

# Restrict fatal-error scanning to Klang's own process IDs. Android 17
# Google API emulator images emit unrelated native crashes and JNI errors
# from background Google/system services; scanning all of logcat makes those
# platform issues look like application failures.
read -r -a app_pids <<< "$app_pid $app_pid_after"
checked_pids=()
fatal_log=""
for pid in "${app_pids[@]}"; do
  [[ "$pid" =~ ^[0-9]+$ ]] || continue

  case " ${checked_pids[*]} " in
    *" $pid "*) continue ;;
  esac
  checked_pids+=("$pid")

  if ! pid_log="$(adb -s "$serial" logcat -d -v brief --pid="$pid" 2>&1)"; then
    fail "could not inspect logcat for Klang process PID $pid: $pid_log"
  fi

  pid_fatal="$(printf '%s\\n' "$pid_log" | grep -E 'FATAL EXCEPTION|UnsatisfiedLinkError|NoClassDefFoundError|Fatal signal' || true)"
  if [[ -n "$pid_fatal" ]]; then
    fatal_log+=

echo "API $api_level launched Klang successfully (pid $app_pid_after)."
\\n'"Klang PID $pid:"

echo "API $api_level launched Klang successfully (pid $app_pid_after)."
\\n'"$pid_fatal"
  fi
done

echo "Checked Klang process logcat for PID(s): ${checked_pids[*]}"
if [[ -n "$fatal_log" ]]; then
  echo "$fatal_log" >&2
  fail "fatal Java or native-library error detected in Klang process"
fi

echo "API $api_level launched Klang successfully (pid $app_pid_after)."
