#!/usr/bin/env python3
"""Export only reviewed tracked project files; never a workspace-wide archive."""
import hashlib
from pathlib import Path
import re
import subprocess
import zipfile
from project_version import VERSION_NAME

ROOT = Path(__file__).resolve().parents[1]
tracked = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).decode().split("\0")
tracked = [path for path in tracked if path]
allowed_roots = {"app", "docs", "gradle", "scripts", "tests", "third_party"}
allowed_files = {".gitignore", ".gitattributes", "README.md", "LICENSE", "NOTICE", "build.gradle", "settings.gradle",
                 "gradle.properties", "gradlew", "gradlew.bat", "version.properties"}
blocked_names = {"local.properties", "credentials", ".env", ".git", ".aws", ".codex", ".agents"}
blocked_suffix = {".jks", ".keystore", ".p12", ".pfx", ".pem", ".key", ".apk", ".aab", ".class", ".dex", ".so", ".pyc"}
if not tracked:
    raise SystemExit("FAIL: no reviewed tracked files")
for relative in tracked:
    path = Path(relative)
    if relative not in allowed_files and path.parts[0] not in allowed_roots:
        raise SystemExit("FAIL: unreviewed export root: " + relative)
    if any(part in blocked_names or part in {"build", ".build-cache", "verification", "dist"}
           for part in path.parts) or path.suffix in blocked_suffix:
        raise SystemExit("FAIL: private/generated export path: " + relative)
    if (ROOT / path).is_symlink():
        raise SystemExit("FAIL: export symlink: " + relative)
    data = (ROOT / path).read_bytes()
    if re.search(rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----\r?\n[A-Za-z0-9+/=\r\n]{64,}", data) or re.search(
            rb"(?:ghp_|github_pat_)[A-Za-z0-9_]{20,}", data):
        raise SystemExit("FAIL: suspected secret: " + relative)
output = ROOT / ("dist/offline-kana-keyboard-" + VERSION_NAME + "-source.zip")
output.parent.mkdir(exist_ok=True)
with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED) as archive:
    for relative in sorted(tracked):
        archive.write(ROOT / relative, "offline-kana-keyboard/" + relative)
print("PASS: source export:", len(tracked), "allowlisted files; SHA-256", hashlib.sha256(output.read_bytes()).hexdigest())
