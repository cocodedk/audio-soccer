#!/usr/bin/env bash
# Plays the debug build on the emulator with no eyes and no ears: fingers go in through adb input, and every verdict comes from
# the app's own logcat lines (SoccerSpeech, SoccerEvent). Start the emulator first: scripts/emu.sh. Each check prints PASS or
# FAIL, and the exit status is non-zero if any failed. The full logcat of the run is kept in $CACHE/e2e-run.log. eSpeak NG,
# the second engine, is fetched from F-Droid into $CACHE (the APK found there is used again; delete it to fetch the current one).
# Env: PORT (default 5584), the emulator's port. Needs bash, curl, awk and a GNU grep with -P.
set -uo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/e2e-lib.sh"

up || { echo "no emulator on $SERIAL: run scripts/emu.sh first" >&2; exit 2; }
read -r W H < <(adb shell wm size | sed -En 's/.*: *([0-9]+)x([0-9]+).*/\1 \2/p' | tail -1)
X=$((W / 2))
ESPEAK=com.reecedunn.espeak
FULL_LOG="$CACHE/e2e-run.log" # all of logcat, kept step by step: the checks of the last step read it
FAILS=0
: >"$FULL_LOG"
adb logcat -c # earlier use of the emulator left lines in it that are not this run's

# --- logs: SoccerSpeech has every line the speech engine accepted, SoccerEvent the screen changes, game events and the
# player's position. They are read as message text only. keep_log files what logcat holds and empties it, so that a check
# reads only the lines since the step (or the gesture) began.
log_of() { adb logcat -d -v raw -s "$1"; }
keep_log() {
  adb logcat -d -v threadtime >>"$FULL_LOG"
  adb logcat -c
}
log_has() { # TAG REGEX: a line of TAG's log matches the Perl regex (in a variable: grep -q closing a pipe early fails under pipefail)
  local out
  out="$(log_of "$1")"
  grep -qP -- "$2" <<<"$out"
}
wait_for() { poll "$3" log_has "$1" "$2"; } # TAG REGEX SECONDS
player_x() { log_of SoccerEvent | sed -n 's/^player x=\([0-9.]*\) .*/\1/p'; }
step() { keep_log; echo "== $*"; }

# --- checks
pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1"; FAILS=$((FAILS + 1)); }
show_logs() { { log_of SoccerSpeech | tail -6; log_of SoccerEvent | tail -6; } | sed 's/^/        /'; }
# expect TITLE TAG REGEX [SECONDS]: PASS when TAG's log has a matching line within SECONDS (10), else FAIL with the log's tail.
expect() {
  if wait_for "$2" "$3" "${4:-10}"; then pass "$1"; else fail "$1 (no line like '$3' in $2)"; show_logs; return 1; fi
}

# reports TITLE EN|FA [SECONDS]: the app says it speaks that language through eSpeak ("voice engine=<package> lang=<EN|FA>").
reports() { expect "$1" SoccerEvent "^voice engine=\Q$ESPEAK\E lang=$2\$" "${3:-10}"; }

# --- fingers. The menu is equal bands down the whole height of the screen: the middle of band I (from 0) of N is at y.
# Each gesture starts a fresh log, so what a check finds after it is what the gesture caused.
band_y() { echo $((H * (2 * $1 + 1) / (2 * $2))); }
# A finger held 600 ms on band I of N (a hold is anything over 300 ms): the game speaks the band.
hold() {
  keep_log
  adb shell input swipe "$X" "$(band_y "$@")" "$X" "$(band_y "$@")" 600
}
# Two quick taps in one shell, so the second lands inside the 350 ms a double-tap allows: it chooses what was spoken.
double_tap() {
  local y
  y="$(band_y "$@")"
  keep_log
  adb shell "input tap $X $y; input tap $X $y"
}

# --- the app and the second engine
# The app, fresh: no settings kept from an earlier run. pm clear takes the old task away behind our back, so wait it out.
launch() {
  adb shell "am force-stop $PKG; pm clear $PKG" >/dev/null
  sleep 2
  keep_log
  to_front
}

engine_count() { adb shell cmd package query-services --brief -a android.intent.action.TTS_SERVICE | grep -cE '^ +[a-z][a-z0-9_.]*/'; }
on_top() { [ "$(adb shell "dumpsys activity activities | grep -c 'topResumedActivity=.*$PKG'" | tr -d '\r')" != 0 ]; }

# eSpeak NG from F-Droid, on the emulator only. Its voice data is unpacked by its own screen, which a person opens once.
ESPEAK_READY=
install_espeak() {
  [ -n "$ESPEAK_READY" ] && return 0
  if ! adb shell pm list packages "$ESPEAK" | grep -q "$ESPEAK"; then
    local apk code
    apk="$(ls -t "$CACHE/$ESPEAK"_*.apk 2>/dev/null | head -1)"
    if [ -z "$apk" ]; then
      code="$(curl -fsS --max-time 30 "https://f-droid.org/api/v1/packages/$ESPEAK" | grep -oP '"suggestedVersionCode":\K[0-9]+')" || return 1
      apk="$CACHE/${ESPEAK}_$code.apk"
      curl -fsSL --max-time 300 -o "$apk.part" "https://f-droid.org/repo/${ESPEAK}_$code.apk" && mv "$apk.part" "$apk" || return 1
    fi
    adb install -r "$apk" >/dev/null || return 1
  fi
  # The data screen unpacks the voices and closes itself, which puts the game back on top.
  adb shell am start -W -n "$ESPEAK/.DownloadVoiceData" >/dev/null
  poll 30 on_top
  to_front
  ESPEAK_READY=1
}

step "1. launch"
launch
if [ "$(engine_count)" -eq 0 ]; then
  echo "   this image has no speech engine"
  expect "with no speech engine the app says so" SoccerEvent '^screen NoEngine$' 30
  install_espeak || fail "eSpeak NG could not be installed (BLOCKED)"
  # No default engine is set (Android makes only a system engine the default): the app has to open the installed one itself.
  launch
  reports "the game opened the engine just installed, which is no default" EN 40
fi
expect "launch: the app speaks its greeting" SoccerSpeech '^Audio Soccer\. Touch the screen to hear the options' 40

step "2. a held finger speaks a band"
hold 1 6
expect "600 ms on band 2 of 6 speaks 'Score a goal'" SoccerSpeech '^Score a goal$'

step "3. two quick taps choose it"
double_tap 1 6
expect "the level starts" SoccerEvent '^screen Play\(level=2\)$'
# The first level of a run begins once the drag intro (two lines) has been said; eSpeak takes some 12 s over them.
expect "the drag intro is spoken first" SoccerSpeech '^Put a finger anywhere on the screen and drag it' 20
expect "the level 2 round intro is spoken once the drag intro has ended" SoccerSpeech '^Round 1\. Score a goal\.$' 40

step "4. a drag up the screen runs toward the goal"
if expect "the round reports where the player stands" SoccerEvent '^player x=' 15; then
  x0="$(player_x | tail -1)"
  # A third of the screen's height in 3 s. A finger has to leave the 24 dp dead zone before the player runs, so a short slow
  # drag spends much of the 3 s standing still: 100 px in 3 s moved the player only 4 m on this 440 dpi screen.
  y1=$((H * 7 / 10))
  adb shell input swipe "$X" "$y1" "$X" $((y1 - H / 3)) 3000
  keep_log
  wait_for SoccerEvent '^player x=' 3 # the player stands still once the finger is up: the next report is where
  x1="$(player_x | head -1)"
  msg="a 3 s drag up moved the player from x=$x0 to x=${x1:-none}"
  if awk -v a="${x1:-0}" -v b="$x0" 'BEGIN { exit !(a - b >= 8) }'; then pass "$msg (at least 8 m)"; else fail "$msg, less than 8 m"; fi
fi

step "5. a tap kicks"
adb shell input tap "$X" $((H / 2))
expect "the tap kicked the ball" SoccerEvent '^event Kick$' 5

step "6. back pauses; the pause menu leads home"
adb shell input keyevent KEYCODE_BACK
expect "back paused the round" SoccerEvent '^screen Paused\(level=2\)$'
expect "the pause menu speaks 'Resume'" SoccerSpeech '^Resume$'
hold 2 3
expect "band 3 of 3 is 'Back to menu'" SoccerSpeech '^Back to menu$'
double_tap 2 3
expect "choosing it goes back to the menu" SoccerEvent '^screen Home$'

step "7. a second speech engine, and Persian"
if install_espeak; then
  hold 5 6
  expect "the sixth band is Settings" SoccerSpeech '^Settings$'
  double_tap 5 6
  expect "Settings opens" SoccerEvent '^screen Settings$'
  hold 1 3
  expect "the voice option names the engine in use" SoccerSpeech '^Voice: '
  engines="$(engine_count)"
  if [ "$engines" -eq 2 ]; then
    first="$(log_of SoccerSpeech | grep '^Voice: ' | tail -1)"
    voice_becomes() { double_tap 1 3; expect "$1" SoccerSpeech "$2" 20; }
    to_other() { voice_becomes "$1" "^(?!\Q$first\E\$)Voice: " && reports "...which the app says is eSpeak" EN; }
    to_other "the voice option moves on to the other engine"
    voice_becomes "...and, with two engines, back to the first" "^\Q$first\E\$"
    to_other "...and on to the other again"
  else
    echo "   skipped: $engines speech engine(s) installed, so cycling the voice option is not checked"
  fi
  hold 0 3
  double_tap 0 3
  expect "with Persian chosen, Persian lines are spoken" SoccerSpeech '(*UTF8)[\x{0600}-\x{06FF}]' 20
  reports "...through eSpeak, as the app says" FA
  # Extra evidence only: eSpeak NG's own log of the voice it was given. Its wording is not ours, so it never fails the run.
  if wait_for eSpeakService 'language=fa' 2; then echo "info  eSpeak's own log shows its Persian voice (language=fa)"
  else echo "info  eSpeak's own log has no language=fa line (its log wording may have changed)"; fi
  if wait_for SoccerSpeech 'This voice cannot speak Persian' 2; then fail "the app fell back to English for Persian"; else pass "the app did not fall back to English"; fi
else
  fail "BLOCKED: eSpeak NG could not be fetched or installed"
fi

step "8. the whole run is clean"
if grep -A3 'FATAL EXCEPTION' "$FULL_LOG" | grep -q "Process: $PKG"; then
  fail "the app crashed"; grep -A8 'FATAL EXCEPTION' "$FULL_LOG" | head -12 | sed 's/^/        /'
else
  pass "no FATAL EXCEPTION for the app"
fi
pids="$(awk '$6 == "SoccerEvent:" { print $3 }' "$FULL_LOG" | sort -u | paste -sd'|')"
under="$(awk -v p="^($pids)$" '$3 ~ p && $6 == "AudioTrack:" && tolower($0) ~ /underrun|underflow/' "$FULL_LOG")"
if [ -z "$under" ]; then pass "no AudioTrack underrun from the app"; else fail "AudioTrack underruns from the app"; echo "$under" | head -5 | sed 's/^/        /'; fi

echo
[ "$FAILS" -eq 0 ] && echo "e2e: all checks passed" || echo "e2e: $FAILS check(s) FAILED (log: $FULL_LOG)"
[ "$FAILS" -eq 0 ]
