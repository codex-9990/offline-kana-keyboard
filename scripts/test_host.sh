#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
javac_bin="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
command -v "$javac_bin" >/dev/null || { printf '%s\n' 'JDK with javac is required.' >&2; exit 78; }
python3 scripts/prepare_dictionary.py --check
mkdir -p .build-cache/host-tests
if command -v rg >/dev/null; then
  mapfile -t java_sources < <(rg --files app/src/main/java/dev/local/koto/core tests/java -g '*.java' | sort)
else
  mapfile -t java_sources < <(find app/src/main/java/dev/local/koto/core tests/java -name '*.java' -print | sort)
fi
"$javac_bin" --release 8 -encoding UTF-8 -d .build-cache/host-tests "${java_sources[@]}"
"$java_bin" -ea -cp .build-cache/host-tests dev.local.koto.core.CoreTests app/src/main/assets/skk.tsv
"$java_bin" -ea -cp .build-cache/host-tests dev.local.koto.core.FeatureTests app/src/main/assets/skk.tsv
python3 scripts/test_policy_guards.py
python3 scripts/audit_source.py
