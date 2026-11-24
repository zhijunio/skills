#!/bin/sh
set -eu
if [ "$#" -ne 1 ]; then
	printf '%s\n' 'Usage: sh verify.sh CANDIDATE_PROJECT' >&2
	exit 2
fi
here=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
candidate=$(CDPATH= cd -- "$1" && pwd)
work=$(mktemp -d)
printf 'Verification copy: %s\n' "$work"
rsync -a --exclude=target "$candidate/" "$work/"
cd "$work"
./mvnw -B verify > "$work/build.log" 2>&1 || { tail -50 "$work/build.log"; exit 1; }
jar=$(find target -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -print)
java_cmd=java
if [ -n "${JAVA_HOME:-}" ]; then java_cmd="$JAVA_HOME/bin/java"; fi
"$java_cmd" -jar "$jar" --server.port=0 > "$work/app.log" 2>&1 &
pid=$!
trap 'kill "$pid" 2>/dev/null || true; wait "$pid" 2>/dev/null || true' EXIT
tries=0
port=
while [ "$tries" -lt 30 ]; do
	if ! kill -0 "$pid" 2>/dev/null; then cat "$work/app.log"; exit 1; fi
	port=$(sed -n 's/.*Tomcat started on port \([0-9][0-9]*\).*/\1/p' "$work/app.log" | tail -n 1)
	if [ -n "$port" ] && grep -q 'Started PortalApplication' "$work/app.log"; then break; fi
	tries=$((tries + 1))
	sleep 1
done
if [ "$tries" -eq 30 ]; then cat "$work/app.log"; exit 1; fi
python3 "$here/acceptance.py" "http://127.0.0.1:$port"
