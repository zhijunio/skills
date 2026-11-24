#!/bin/sh
set -eu
if [ "$#" -ne 2 ]; then
	printf '%s\n' 'Usage: sh verify.sh BASELINE_PROJECT CANDIDATE_PROJECT' >&2
	exit 2
fi
here=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
base=$(CDPATH= cd -- "$1" && pwd)
candidate=$(CDPATH= cd -- "$2" && pwd)
work=$(mktemp -d)
printf 'Verification copy: %s\n' "$work"
rsync -a --exclude=target "$candidate/" "$work/"
migrations=src/main/resources/db/migration
fixtures="$work/src/test/resources/legacy-migrations"
mkdir -p "$fixtures"
for original in "$base/$migrations/"*.sql; do
	name=$(basename -- "$original")
	cmp "$original" "$work/$migrations/$name"
	cp "$original" "$fixtures/"
	printf 'Original migration matches: %s\n' "$name"
done
cp "$here/LegacyOrderMigrationIT.java" "$work/src/test/java/com/example/orders/"
cd "$work"
./mvnw -B verify
