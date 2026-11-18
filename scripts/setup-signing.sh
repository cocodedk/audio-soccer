#!/usr/bin/env bash
# scripts/setup-signing.sh - create the release keystore and store the four signing secrets.
#
# Run it yourself, in your own terminal: it asks for the keystore password, which must not
# go through a chat, a script argument or a log. Safe to run again: it never overwrites an
# existing keystore, and the secrets are simply set again.
#
#   scripts/setup-signing.sh KEYSTORE_FILE KEY_ALIAS
#   for example: scripts/setup-signing.sh ~/keystores/audio-soccer.keystore soccer
#
# There is deliberately no default path. The first signed release fixes the key for good:
# F-Droid pins its certificate, and every update must be signed with it. Choose on purpose
# between a key made for this app (for example ~/keystores/audio-soccer.keystore) and a
# key you already share between apps.
#
# The keystore is PKCS12. That format has one password for the store and the key and ignores
# a separate key password, so this script asks for one password and uses it for both.
set -euo pipefail

REPO="cocodedk/audio-soccer"

die() { echo "ERROR: $*" >&2; exit 1; }
umask 077

[ "$#" -eq 2 ] || die "usage: $0 KEYSTORE_FILE KEY_ALIAS. Both are required, and have no default: see the top of this script."
KEYSTORE_FILE=$1
KEY_ALIAS=$2

for tool in keytool gh base64; do
  command -v "$tool" >/dev/null 2>&1 || die "$tool not found"
done
perm=$(gh repo view "$REPO" --json viewerPermission -q .viewerPermission 2>/dev/null) || perm=
[ "$perm" = ADMIN ] || die "gh is not signed in with admin rights on $REPO (permission: ${perm:-none}). Nothing was changed."

# An existing keystore is reused and never touched. Say which one, because this choice is final.
if [ -e "$KEYSTORE_FILE" ]; then
  [ -f "$KEYSTORE_FILE" ] || die "$KEYSTORE_FILE exists and is not a file"
  echo "The keystore already exists and will be reused as it is:"
  echo "  $KEYSTORE_FILE -> $(readlink -f "$KEYSTORE_FILE")"
  printf 'It will sign every release of this app, and F-Droid will pin it. Use it? [y/N] '
  read -r answer || answer=
  case "$answer" in [yY]|[yY][eE][sS]) ;; *) die "Stopped. Nothing was changed." ;; esac
fi

IFS= read -rsp 'Keystore password, also used for the key (at least 6 characters): ' PASS || PASS=
echo
[ "${#PASS}" -ge 6 ] || die "The password must be at least 6 characters. Nothing was changed."

# The password reaches keytool through the environment, never through its arguments.
export KS_PASS="$PASS"
if [ ! -e "$KEYSTORE_FILE" ]; then
  IFS= read -rsp 'The same password again: ' AGAIN || AGAIN=
  echo
  [ "$PASS" = "$AGAIN" ] || die "The two passwords differ. Nothing was changed."
  mkdir -p "$(dirname "$KEYSTORE_FILE")"
  keytool -genkeypair -keystore "$KEYSTORE_FILE" -storetype PKCS12 -alias "$KEY_ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Audio Soccer, O=Cocode" \
    -storepass:env KS_PASS -keypass:env KS_PASS >/dev/null 2>&1 || die "keytool could not create $KEYSTORE_FILE"
fi

# Without -alias, keytool prints the keystore type; with it, the certificate fingerprint.
kind=$(keytool -list -keystore "$KEYSTORE_FILE" -storepass:env KS_PASS 2>/dev/null) ||
  die "Wrong password, or not a keystore. Nothing was uploaded."
case "$kind" in
  *"Keystore type: PKCS12"*) ;;
  *) die "Not a PKCS12 keystore, so the key may have its own password. Nothing was uploaded." ;;
esac
info=$(keytool -list -v -keystore "$KEYSTORE_FILE" -alias "$KEY_ALIAS" -storepass:env KS_PASS 2>/dev/null) ||
  die "No key with the alias '$KEY_ALIAS' in the keystore. Nothing was uploaded."
digest=$(printf '%s\n' "$info" | sed -n 's/^[[:space:]]*SHA256:[[:space:]]*//p' | head -1 | tr -d ':' | tr 'A-F' 'a-f')
[ -n "$digest" ] || die "Could not read the certificate SHA-256. Nothing was uploaded."

# Values go in through stdin, so they are never on a command line or in the terminal.
base64 < "$KEYSTORE_FILE" | tr -d '\n' | gh secret set KEYSTORE_BASE64 --repo "$REPO" >/dev/null
printf '%s' "$PASS" | gh secret set KEYSTORE_PASSWORD --repo "$REPO" >/dev/null
printf '%s' "$KEY_ALIAS" | gh secret set KEY_ALIAS --repo "$REPO" >/dev/null
printf '%s' "$PASS" | gh secret set KEY_PASSWORD --repo "$REPO" >/dev/null

echo
echo "Signing certificate SHA-256 (what F-Droid pins; the release log shows the same):"
echo "  $digest"
echo
echo "Back up now, somewhere off this machine such as an encrypted USB stick or your"
echo "password manager: the keystore file ($KEYSTORE_FILE) and its password."
echo "GitHub keeps the secrets write-only and cannot give them back. Lose either one and"
echo "the app can never be updated again."
