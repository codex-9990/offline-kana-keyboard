#!/usr/bin/env python3
"""Deterministic offline derivation. Never fetches or executes dictionary content."""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "third_party/skk/SKK-JISYO.ML.json"
SOURCE_SHA256 = "885ed8e2aa38c0249c79fe428aed9c419133984ddff7fcef69035ea886773887"
SOURCE_BLOB = "89dba326fd271d250b550fc46f3fe3f2df667ea4"
SOURCE_COMMIT = "0a164e6b990c5eb5b59eb7d8789f08865dc2f644"


def artifacts():
    raw = SOURCE.read_bytes()
    if hashlib.sha256(raw).hexdigest() != SOURCE_SHA256:
        raise SystemExit("FAIL: pinned dictionary source SHA-256 mismatch")
    blob = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
    if blob != SOURCE_BLOB:
        raise SystemExit("FAIL: dictionary Git blob mismatch")
    data = json.loads(raw)
    entries = {}
    removed = 0
    for section in ("okuri_nasi", "okuri_ari"):
        for reading, values in data[section].items():
            if not reading or any(c in reading for c in "\t\r\n#"):
                removed += len(values)
                continue
            words = []
            for value in values:
                value = value.split(";", 1)[0]
                if (not value or value.startswith("(") or "#" in value
                        or any(c in value for c in "\t\r\n\ufffd")):
                    removed += 1
                    continue
                if value not in words:
                    words.append(value)
            if words:
                entries[reading] = words
    tsv = ("# SKK-JISYO.ML; GPL-2.0-or-later; see NOTICE.txt and GPL-2.txt\n"
           + "# Read-only word dictionary; no learning; no Lisp evaluation\n"
           + "".join(key + "\t" + "\t".join(entries[key]) + "\n" for key in sorted(entries)))
    notice = ("Koto Offline Kana Keyboard\nCopyright (C) 2026 Koto contributors\n"
              "Application code: GPL-2.0-or-later. See GPL-2.txt.\n\n"
              "Bundled SKK-JISYO.ML dictionary\n" + data["description"] + "\n\n"
              + data["copyright"] + "\n\n" + data["license"] + "\n\n"
              "Upstream: https://github.com/skk-dev/dict\n"
              f"Commit: {SOURCE_COMMIT}\n"
              "Source: json/SKK-JISYO.ML.json\n"
              "Derivation: scripts/prepare_dictionary.py, strips annotations and "
              "unsupported Lisp/numeric expansions. Original source is included.\n"
              "This is SKK word lookup, not Mozc.\n")
    lock = {
        "upstream": "https://github.com/skk-dev/dict", "commit": SOURCE_COMMIT,
        "path": "json/SKK-JISYO.ML.json", "git_blob": SOURCE_BLOB,
        "sha256": SOURCE_SHA256, "license": "GPL-2.0-or-later",
        "raw_readings": len(data["okuri_nasi"]), "raw_okuri_keys": len(data["okuri_ari"]),
        "bundled_keys": len(entries), "bundled_candidates": sum(map(len, entries.values())),
        "removed_unsupported_candidates": removed,
        "asset_sha256": hashlib.sha256(tsv.encode()).hexdigest(),
    }
    return {
        ROOT / "app/src/main/assets/skk.tsv": tsv.encode(),
        ROOT / "app/src/main/assets/NOTICE.txt": notice.encode(),
        ROOT / "NOTICE": notice.encode(),
        ROOT / "third_party/skk/dictionary.lock.json": (json.dumps(lock, indent=2) + "\n").encode(),
        ROOT / "app/src/main/assets/GPL-2.txt": (ROOT / "LICENSE").read_bytes(),
    }, lock


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files, lock = artifacts()
    for path, payload in files.items():
        if args.check:
            if not path.is_file() or path.read_bytes() != payload:
                raise SystemExit("FAIL: derived artifact differs: " + str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(payload)
    print("PASS: pinned SKK-ML source and " + str(len(files)) + " deterministic artifacts; "
          + str(lock["bundled_keys"]) + " keys, " + str(lock["bundled_candidates"]) + " candidates")


if __name__ == "__main__":
    main()
