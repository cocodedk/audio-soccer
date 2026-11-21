#!/usr/bin/env bash
# scripts/setup-repo.sh - apply the GitHub settings of cocodedk/audio-soccer.
#
# A person runs this, with gh signed in as the owner (admin rights on the repo), after the
# stacked pull requests have landed on main. Every call sets a value rather than adding one,
# so it is safe to run again. It changes:
#   - description, homepage, topics
#   - merge settings: squash and merge commit allowed, head branches deleted on merge
#   - private vulnerability reporting: on
#   - the five labels that the issue forms may use
#   - protection of main: the check "verify" must pass, a pull request is required with no
#     approvals (the owner can merge their own), no force-push, no deletion, admins may bypass
#
# Branch protection names the check "verify", the job in .github/workflows/ci.yml. If ci.yml
# is not on main yet the script leaves protection alone and says so, because a required check
# that never reports would block every pull request.
set -euo pipefail

REPO="cocodedk/audio-soccer"
BRANCH="main"
DESCRIPTION="Audio Soccer: a football game you play by ear, made for blind and low-vision players. Android app and browser trial."
HOMEPAGE="https://soccer.cocode.dk/"
TOPICS=(audio-game accessibility blind low-vision visually-impaired screen-reader android football fdroid kotlin)

command -v gh >/dev/null 2>&1 || { echo "ERROR: gh not found" >&2; exit 1; }
perm=$(gh repo view "$REPO" --json viewerPermission -q .viewerPermission 2>/dev/null) || perm=
if [ "$perm" != ADMIN ]; then
  echo "ERROR: gh is not signed in with admin rights on $REPO (permission: ${perm:-none})." >&2
  exit 1
fi

echo "== $REPO =="

gh api -X PATCH "repos/$REPO" \
  -f description="$DESCRIPTION" \
  -f homepage="$HOMEPAGE" \
  -F allow_squash_merge=true \
  -F allow_merge_commit=true \
  -F delete_branch_on_merge=true \
  -F allow_update_branch=true >/dev/null
echo "ok  description, homepage, merge settings (squash and merge commit, delete head branches)"

topic_args=()
for topic in "${TOPICS[@]}"; do topic_args+=(-f "names[]=$topic"); done
gh api -X PUT "repos/$REPO/topics" "${topic_args[@]}" >/dev/null
echo "ok  topics: ${TOPICS[*]}"

gh api -X PUT "repos/$REPO/private-vulnerability-reporting" >/dev/null
echo "ok  private vulnerability reporting on"

# name|color|description. --force updates a label that already exists.
labels=(
  "bug|d73a4a|Something in the app or the browser trial does not work as it should"
  "accessibility|0e8a16|A barrier for blind and low-vision players: sound, speech, gestures, contrast"
  "screen-reader|5319e7|How the game works with a screen reader on, or the step of turning it off"
  "idea|a2eeef|A suggestion for something new in the game"
  "translation|1d76db|English or Persian wording: menus, spoken lines, store text"
)
names=()
for entry in "${labels[@]}"; do
  IFS='|' read -r name color desc <<< "$entry"
  gh label create "$name" --repo "$REPO" --color "$color" --description "$desc" --force >/dev/null
  names+=("$name")
done
echo "ok  labels: ${names[*]}"

if gh api "repos/$REPO/contents/.github/workflows/ci.yml?ref=$BRANCH" >/dev/null 2>&1; then
  gh api -X PUT "repos/$REPO/branches/$BRANCH/protection" --input - >/dev/null <<'JSON'
{
  "required_status_checks": { "strict": true, "contexts": ["verify"] },
  "enforce_admins": false,
  "required_pull_request_reviews": {
    "required_approving_review_count": 0,
    "dismiss_stale_reviews": false,
    "require_code_owner_reviews": false
  },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "required_linear_history": false,
  "required_conversation_resolution": false
}
JSON
  echo "ok  protection of $BRANCH: check verify, pull request required (0 approvals), no force-push, no deletion, admins may bypass"
else
  echo "skipped  protection of $BRANCH: .github/workflows/ci.yml is not on $BRANCH yet. Run this again once it is."
fi
