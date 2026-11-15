#!/usr/bin/env bash
# Starts the headless emulator that scripts/e2e.sh plays on, installs the debug APK and opens the app. Run
# ./gradlew buildSmoke first. Safe to run again: an emulator already on the port is reused, and the APK reinstalled.
#   scripts/emu.sh         start (or reuse), install, launch
#   scripts/emu.sh stop    stop the emulator on that port, and no other
# Env: AVD (default soccer: made from IMG if avdmanager is there, otherwise name an AVD that exists; the emulator runs it
# -read-only, so its own data is never changed), IMG (default system-images;android-34;google_apis;x86_64), PORT (default 5584).
set -uo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/e2e-lib.sh"
APK="$HERE/../app/build/outputs/apk/debug/app-debug.apk"
AVD="${AVD:-soccer}"
IMG="${IMG:-system-images;android-34;google_apis;x86_64}"
LOG="$CACHE/emulator.log"
EMULATOR="$ANDROID_HOME/emulator/emulator"
# avdmanager and the emulator have to look for AVDs in the same place.
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$HOME/.android}"
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$ANDROID_USER_HOME/avd}"

down() { ! up; }
booted() { [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; }

# The AVD is made from IMG if it is missing and avdmanager can do it; if not, say which AVDs there are and stop.
ensure_avd() {
  local avds mgr why
  avds="$("$EMULATOR" -list-avds)" # in a variable: grep -q closing a pipe early fails it under pipefail
  grep -qx "$AVD" <<<"$avds" && return 0
  mgr="$(command -v avdmanager || ls "$ANDROID_HOME"/cmdline-tools/*/bin/avdmanager 2>/dev/null | head -1)"
  if [ -z "$mgr" ]; then
    why="there is no AVD named $AVD, and no avdmanager here to create it"
  else
    echo "[emu] creating AVD $AVD from $IMG"
    if why="$(echo no | "$mgr" create avd -n "$AVD" -k "$IMG" 2>&1)"; then
      avds="$("$EMULATOR" -list-avds)"
      grep -qx "$AVD" <<<"$avds" && return 0
      why="avdmanager made $AVD, but the emulator does not list it (is ANDROID_AVD_HOME $ANDROID_AVD_HOME right?)"
    else
      why="avdmanager could not make $AVD from $IMG (install the image with sdkmanager): $(tail -3 <<<"$why")"
    fi
  fi
  echo "[emu] $why"
  echo "[emu] installed AVDs:"
  sed 's/^/        /' <<<"$avds"
  echo "[emu] run with AVD=<name> to use one of them"
  exit 1
}

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
  [ -x "$EMULATOR" ] || { echo "[emu] no emulator at $EMULATOR: install it with sdkmanager, or set ANDROID_HOME"; exit 1; }
  ensure_avd
  echo "[emu] starting $AVD on $SERIAL (log: $LOG)"
  # KVM is picked up by the emulator itself.
  nohup "$EMULATOR" -avd "$AVD" -read-only -no-snapshot-save -no-window -no-boot-anim \
    -gpu swiftshader_indirect -port "$PORT" >"$LOG" 2>&1 &
fi

poll 300 booted || { echo "[emu] $SERIAL did not boot in time"; tail -20 "$LOG"; exit 1; }
adb shell input keyevent KEYCODE_WAKEUP >/dev/null
adb shell wm dismiss-keyguard >/dev/null 2>&1

echo "[emu] booted; installing $PKG"
adb install -r "$APK" || { echo "[emu] install failed"; exit 1; }
to_front && echo "[emu] $PKG started on $SERIAL"
