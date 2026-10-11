#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"
sdk_dir="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$sdk_dir" || ! -f "$sdk_dir/platforms/android-35/android.jar" || ! -x "$sdk_dir/build-tools/35.0.1/aapt2" ]]; then
  printf '%s\n' 'NOT RUN: SDK Platform 35 and Build Tools 35.0.1 must already be installed with user-approved terms.' 'This script will not install an SDK or accept agreements.' >&2
  exit 78
fi
python3 scripts/prepare_dictionary.py --check
export GRADLE_USER_HOME="$project_dir/.build-cache/gradle"
export ANDROID_USER_HOME="$project_dir/.build-cache/android-user"
./gradlew --no-daemon :app:assembleDebug :app:lintDebug --console=plain
printf '%s\n' 'APK: app/build/outputs/apk/debug/ (unsigned unless an explicitly supplied test keystore was configured)'
