#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-2.0-or-later
"""Source release guards for explicit fixed-dictionary conversion; not UI execution."""


def compatibility_checks(sources):
    policy = sources["FieldPolicy.java"]
    session = sources["InputSession.java"]
    service = sources["KotoImeService.java"]
    candidates = sources["CandidateList.java"]
    yield "NO_SUGGESTIONS excludes learning independently of conversion eligibility", (
        "type == 1 && (inputType & 0x80000) != 0" in policy
        and "learning=candidates && !noSuggestions && !noLearning && !ascii" in policy)
    yield "recognized text permits explicit conversion with ASCII defaults and restricted types retained", (
        "candidates = type == 1 && variation <= 0xe0 && !password" in policy
        and "explicitConversion = candidates && (noSuggestions || ascii)" in policy
        and "policy.numeric ? Mode.SYMBOLS : policy.ascii ? Mode.ENGLISH : Mode.KANA" in session)
    cycle = service.split("private void cycleCandidate() {", 1)[-1].split("private void enter()", 1)[0]
    yield "first conversion press requests candidates before checking an empty list", (
        "session.canRequestConversion()" in cycle
        and 0 <= cycle.find("session.requestConversion()") < cycle.find("refreshCandidates()") < cycle.find("candidates.isEmpty()")
        and service.count("session.requestConversion()") == 1)
    yield "candidate assembly gates unsolicited output before dictionary and private history lookup", (
        "if (!session.hasCandidates()) return Collections.emptyList();" in candidates
        and "policy.learning && !session.incognito()" in candidates
        and "? remembered.get() : Collections.emptyList()" in candidates
        and "allowsPhrases(session) ? phrases : Collections.emptyList()" in candidates)
    yield "segmented callbacks and requests exclude explicit-only fields and cancel across lifecycle", (
        "CandidateList.allowsPhrases(session)" in service
        and "if (session.policy().explicitConversion)" in service
        and "phraseReading=\"\"; phraseCandidates=Collections.emptyList();" in service
        and service.count("phraseRequests.cancel()") >= 2
        and "reading.equals(session.text()) && reading.equals(phraseReading)" in service)
    yield "static candidate taps remain independent of learned-history generation", (
        "(!candidate.learned || stateAtRender==learning.state() && currentLearned(candidate,snapshot))" in service
        and "if (!policy.learning || LearningStore.privateMode(controls)) return;" in service
        and "if (!session.policy().learning || session.incognito() || LearningStore.privateMode(controls)) return false;" in service)
