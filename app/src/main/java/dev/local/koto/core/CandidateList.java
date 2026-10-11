/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Gate unsolicited candidates, learned lookup and phrase proposals before assembly. */
public final class CandidateList {
    private CandidateList() { }
    public static boolean allowsPhrases(InputSession session) {
        return session.hasCandidates() && !session.policy().explicitConversion;
    }
    public static List<ConversionCandidate> from(InputSession session,SkkDictionary dictionary,
                                                 Supplier<List<LearningData.Entry>> remembered,List<String> phrases) {
        if (!session.hasCandidates()) return Collections.emptyList();
        String reading=session.text();
        FieldPolicy policy=session.policy();
        // In NO_SUGGESTIONS/private/restricted fields the history supplier must
        // not run, even when a retained history exists from an earlier field.
        List<LearningData.Entry> learned=policy.learning && !session.incognito()
                ? remembered.get() : Collections.emptyList();
        List<String> generated=allowsPhrases(session) ? phrases : Collections.emptyList();
        return ConversionCandidate.combine(reading,dictionary==null ? Collections.emptyList() : dictionary.candidates(reading),
                learned,generated,dictionary==null ? Collections.emptyList() : dictionary.exactWords(reading));
    }
}
