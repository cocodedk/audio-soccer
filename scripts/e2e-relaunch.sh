# Sourced by e2e.sh, whose checks and helpers its functions use (they are only called once those exist): the app relaunched with
# Persian and eSpeak saved.
#
# An engine changes its language only when idle: set while it is still on a line, eSpeak NG kept its English voice for the lines after
# it. The first line an engine is given is the app's silent probe (TtsVoice), so the app stops the engine before it sets the language.
# Each relaunch checks, from the app's own logcat lines, that it speaks Persian through eSpeak and says its greeting in Persian, and
# from eSpeak's own log, the only witness of the voice it used, that it set a Persian voice (language=fa) and never an English one
# after the app had said so. eSpeak's log tag is eSpeakService, and it logs nativeSetVoiceByProperties(language=...) for each
# synthesis. RELAUNCHES (default 5) is how many in a row.

# The voice languages eSpeak set since the app said it speaks Persian through eSpeak, one a line.
espeak_voices_after_app_says_fa() {
  log_of SoccerEvent eSpeakService |
    awk -v say="^voice engine=$ESPEAK lang=FA$" '
      $0 ~ say { seen = 1; next }
      seen && /nativeSetVoiceByProperties\(language=/ { voice = $0; sub(/.*language=/, "", voice); sub(/,.*/, "", voice); print voice }'
}

espeak_has_set_a_voice() { [ -n "$(espeak_voices_after_app_says_fa)" ]; }

relaunch_persian() {
  local i voices
  for i in $(seq 1 "${RELAUNCHES:-5}"); do
    adb shell am force-stop "$PKG" # the settings are kept: no pm clear
    sleep 1
    keep_log
    to_front
    reports "relaunch $i: the app speaks Persian through eSpeak" FA 40 || continue
    expect "relaunch $i: the greeting is logged in Persian" SoccerSpeech '(*UTF8)^\x{0641}\x{0648}\x{062A}\x{0628}\x{0627}\x{0644} \x{0635}\x{0648}\x{062A}\x{06CC}\.' 20 || continue
    poll 10 espeak_has_set_a_voice
    voices="$(espeak_voices_after_app_says_fa | tr '\n' ' ')" # once: the verdict and the failure message come from the same read
    if [ "$(tr ' ' '\n' <<<"$voices" | sort -u | grep .)" = fa ]; then pass "relaunch $i: eSpeak set only its Persian voice (language=fa) for the greeting"
    else fail "relaunch $i: eSpeak set these voices after the app said Persian: ${voices:-none}"; fi
  done
}
