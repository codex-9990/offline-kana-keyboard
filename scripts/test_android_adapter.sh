#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"
api_jar=".build-cache/aosp-tools/android-all.jar"
if [[ ! -f "$api_jar" ]]; then
  printf '%s\n' 'NOT RUN: cached Android API jar unavailable; this test does not download tools.' >&2
  exit 78
fi
python3 - <<'PY'
import hashlib,json
from pathlib import Path
expected=json.loads(Path('docs/aosp-toolchain.lock.json').read_text())['android-all']['sha256']
assert hashlib.sha256(Path('.build-cache/aosp-tools/android-all.jar').read_bytes()).hexdigest()==expected
PY
mkdir -p .build-cache/android-api-tests
mapfile -t core_sources < <(rg --files app/src/main/java/dev/local/koto/core -g '*.java' | sort)
"${JAVA_HOME:?set JAVA_HOME to a complete JDK}/bin/javac" --release 8 -encoding UTF-8 -cp "$api_jar" \
  -d .build-cache/android-api-tests "${core_sources[@]}" app/src/main/java/dev/local/koto/ConnectionEditor.java \
  tests/android-api/dev/local/koto/ConnectionEditorHostTests.java \
  tests/android-api/dev/local/koto/CompositionCallbackHostTests.java
"$JAVA_HOME/bin/java" -ea -cp "$api_jar:.build-cache/android-api-tests" dev.local.koto.ConnectionEditorHostTests
"$JAVA_HOME/bin/java" -ea -cp "$api_jar:.build-cache/android-api-tests" dev.local.koto.CompositionCallbackHostTests app/src/main/assets/skk.tsv
