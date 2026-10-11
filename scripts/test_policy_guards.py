#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-2.0-or-later
"""Negative controls for source/packaged-help release guards, using in-memory mutations."""
from pathlib import Path
from compatibility_policy import compatibility_checks
from help_policy import SECTIONS, RETIRED_CLAIMS, help_checks, java_text_values

ROOT = Path(__file__).resolve().parents[1]
sources = {p.name: p.read_text() for p in (ROOT / "app/src/main/java").rglob("*.java")}
values = java_text_values(sources["SettingsActivity.java"])
assert all(ok for _, ok in help_checks(values))
assert all(ok for _, ok in compatibility_checks(sources))
count = 0
for _, prefix, required in SECTIONS:
    section = next(value for value in values if value.startswith(prefix))
    for part in required:
        altered = [value.replace(part, "", 1) if value == section else value for value in values]
        assert not all(ok for _, ok in help_checks(altered)), part
        count += 1
    for altered in ([value for value in values if value != section], values + [section]):
        assert not all(ok for _, ok in help_checks(altered)), prefix
        count += 1
for claim in RETIRED_CLAIMS:
    assert not all(ok for _, ok in help_checks(values + [claim])), claim
    count += 1
print(f"PASS: {count} negative help mutations rejected (missing behavior/section, duplicate section, retired claims)")
mutations = (
    ("FieldPolicy.java", "learning=candidates && !noSuggestions", "learning=candidates"),
    ("FieldPolicy.java", "variation <= 0xe0 && !password", "variation <= 0xff0"),
    ("KotoImeService.java", "if (!session.requestConversion()) return;", "if (candidates.isEmpty()) return; if (!session.requestConversion()) return;"),
    ("CandidateList.java", "? remembered.get() : Collections.emptyList()", "? remembered.get() : remembered.get()"),
    ("KotoImeService.java", "CandidateList.allowsPhrases(session)", "session.hasCandidates()"),
    ("KotoImeService.java", "(!candidate.learned || stateAtRender==learning.state() && currentLearned(candidate,snapshot))", "stateAtRender==learning.state()"),
)
for index, (file, before, after) in enumerate(mutations):
    assert before in sources[file]
    altered = dict(sources)
    altered[file] = sources[file].replace(before, after, 1)
    checks = list(compatibility_checks(altered))
    assert not checks[index][1], checks[index][0]
print(f"PASS: {len(mutations)} compatibility policy mutations rejected")
