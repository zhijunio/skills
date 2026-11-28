#!/bin/sh
# Local CRaC experiment only. No automatic checkpoint promotion or restore.
# Use an isolated directory per artifact/runtime/configuration/platform.
set -eu
umask 077

CRAC_CHECKPOINT_DIR="${CRAC_CHECKPOINT_DIR:-/opt/crac/checkpoint}"
APP_JAR="${APP_JAR:-/app/app.jar}"
JAVA_OPTS="${JAVA_OPTS:-}"
CRAC_MODE="${CRAC_MODE:-}"
# Operator-supplied identity for the reviewed artifact/runtime/configuration/platform.
# This is a compatibility label, not a signature or integrity proof.
: "${CRAC_COMPATIBILITY_ID:?Set the reviewed checkpoint compatibility identity}"
: "${CRAC_MODE:?Select create or restore explicitly}"

case "$CRAC_MODE" in
  create)
    mkdir -p "$CRAC_CHECKPOINT_DIR"
    if [ -n "$(ls -A "$CRAC_CHECKPOINT_DIR")" ]; then
      echo "[crac] Refusing to overwrite a nonempty checkpoint directory" >&2
      exit 1
    fi
    echo "[crac] Creating checkpoint; inspect the result before explicitly restoring"
    # The checkpoint operation may terminate this JVM with a nonzero status.
    # Do not infer success from the presence of files or automatically restore.
    # JAVA_OPTS is operator-controlled whitespace-separated JVM flags.
    set +e
    java $JAVA_OPTS -XX:CRaCCheckpointTo="$CRAC_CHECKPOINT_DIR" \
      -Dspring.context.checkpoint=onRefresh -jar "$APP_JAR"
    status=$?
    set -e
    echo "[crac] JVM exited with status $status; checkpoint completeness is unverified" >&2
    echo "[crac] Inspect checkpoint using the selected runtime documentation and record the reviewed identity before restore" >&2
    # No automatic success marker: require explicit operator review.
    exit "$status"
    ;;
  restore)
    : "${CRAC_REVIEWED_ID:?Set only after checkpoint completeness and compatibility review}"
    if [ "$CRAC_REVIEWED_ID" != "$CRAC_COMPATIBILITY_ID" ] || \
       [ ! -d "$CRAC_CHECKPOINT_DIR" ] || [ -z "$(ls -A "$CRAC_CHECKPOINT_DIR")" ]; then
      echo "[crac] Missing or incompatible reviewed checkpoint" >&2
      exit 1
    fi
    exec java -XX:CRaCRestoreFrom="$CRAC_CHECKPOINT_DIR"
    ;;
  *)
    echo "[crac] Unsupported mode; select create or restore" >&2
    exit 1
    ;;
esac
