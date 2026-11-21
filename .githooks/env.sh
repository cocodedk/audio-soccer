# Sourced by pre-commit and pre-push: the owner's env file first, then JDK 17 if JAVA_HOME is still unset, because the
# host default may be newer than Gradle supports (CLAUDE.md, Building).
# shellcheck source=/dev/null
[ -f "$HOME/.soccer-env.sh" ] && . "$HOME/.soccer-env.sh"
if [ -z "${JAVA_HOME:-}" ] && [ -d /usr/lib/jvm/java-17-openjdk-amd64 ]; then
  JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
  export JAVA_HOME
fi
