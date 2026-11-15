#!/usr/bin/env bash
# Starts the headless emulator that scripts/e2e.sh plays on, installs the debug APK and opens the app. Run
# ./gradlew buildSmoke first. Safe to run again: an emulator already on the port is reused, and the APK reinstalled.
#   scripts/emu.sh         start (or reuse), install, launch
#   scripts/emu.sh stop    stop the emulator on that port, and no other
# Env: AVD (default Calendar_API34, which is only read: -read-only keeps its own data untouched), PORT (default 5584).
set -uo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/e2e-lib.sh"
APK="$HERE/../app/build/outputs/apk/debug/app-debug.apk"
AVD="${AVD:-Calendar_API34}"
LOG="$CACHE/emulator.log"

down() { ! up; }
booted() { [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; }

if [ "${1:-start}" = stop ]; then
  up || { echo "[emu] nothing on $SERIAL"; exit 0; }
  adb emu kill >/dev/null 2>&1
  poll 30 down || { echo "[emu] $SERIAL did not stop"; exit 1; }
  echo "[emu] stopped $SERIAL"; exit 0
fi

[ -f "$APK" ] || { echo "[emu] no debug APK at $APK: run ./gradlew buildSmoke first"; exit 1; }

if up; then
  echo "[emu] reusing $SERIAL"
else
  echo "[emu] starting $AVD on $SERIAL (log: $LOG)"
  # KVM is picked up by the emulator itself.
  nohup "$ANDROID_HOME/emulator/emulator" -avd "$AVD" -read-only -no-snapshot-save -no-window -no-boot-anim \
    -gpu swiftshader_indirect -port "$PORT" >"$LOG" 2>&1 &
fi

poll 300 booted || { echo "[emu] $SERIAL did not boot in time"; tail -20 "$LOG"; exit 1; }
adb shell input keyevent KEYCODE_WAKEUP >/dev/null
adb shell wm dismiss-keyguard >/dev/null 2>&1

echo "[emu] booted; installing $PKG"
adb install -r "$APK" || { echo "[emu] install failed"; exit 1; }
to_front && echo "[emu] $PKG started on $SERIAL"
