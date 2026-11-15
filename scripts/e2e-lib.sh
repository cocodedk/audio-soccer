# Sourced by emu.sh and e2e.sh: where the SDK is, which emulator they use, and adb pinned to it. A real phone may be on USB,
# so every adb call goes through adb() below, which names the emulator with -s.
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_HOME ANDROID_SDK_ROOT="$ANDROID_HOME"
ADB_BIN="$ANDROID_HOME/platform-tools/adb"
SERIAL="${SERIAL:-emulator-${PORT:-5584}}" # the one variable that names the device (PORT picks the default one)
PKG=dk.cocode.soccer.debug                  # the debug build's application id (applicationIdSuffix)
ACTIVITY=dk.cocode.soccer.ui.MainActivity
CACHE="${XDG_CACHE_HOME:-$HOME/.cache}/audio-soccer-e2e" # downloads and logs live here, never in the repo
mkdir -p "$CACHE"

case "$SERIAL" in
  emulator-[0-9]*) PORT="${SERIAL#emulator-}" ;;
  *) echo "refusing to run: serial '$SERIAL' is not an emulator (emulator-<port>)" >&2; exit 2 ;;
esac

adb() { "$ADB_BIN" -s "$SERIAL" "$@"; }
up() { adb get-state >/dev/null 2>&1; }

# poll SECONDS CMD...: runs the command until it succeeds, or fails once SECONDS have passed.
poll() {
  local end=$((SECONDS + $1))
  shift
  until "$@"; do
    [ "$SECONDS" -ge "$end" ] && return 1
    sleep 0.3
  done
}

# Brings the app to the front the way the launcher does (a plain am start would stack a second copy on top).
to_front() { adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$PKG/$ACTIVITY" >/dev/null 2>&1; }
