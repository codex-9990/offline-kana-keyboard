/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

/** Android's stable input-type bit masks, kept framework-free for host tests. */
public final class FieldPolicy {
    public final boolean password, noPersonalizedLearning, noSuggestions, numeric, candidates, ascii, learning, explicitConversion;
    private FieldPolicy(boolean password, boolean noLearning, boolean noSuggestions, boolean numeric,
                        boolean candidates, boolean ascii, boolean learning, boolean explicitConversion) {
        this.password = password; this.noPersonalizedLearning = noLearning;
        this.noSuggestions = noSuggestions;
        this.numeric = numeric; this.candidates = candidates;
        this.ascii = ascii;
        this.learning = learning;
        this.explicitConversion = explicitConversion;
    }
    public static FieldPolicy from(int inputType, int imeOptions) {
        int type = inputType & 0xf, variation = inputType & 0xff0;
        boolean password = (type == 1 && (variation == 0x80 || variation == 0x90 || variation == 0xe0))
                || (type == 2 && variation == 0x10);
        boolean numeric = type == 2 || type == 3 || type == 4;
        boolean noLearning = (imeOptions & 0x1000000) != 0;
        boolean noSuggestions = type == 1 && (inputType & 0x80000) != 0;
        // Recognized non-password text supports local editing and explicit
        // dictionary conversion. Unknown classes/variations stay direct.
        boolean candidates = type == 1 && variation <= 0xe0 && !password;
        boolean ascii = password || (imeOptions & 0x80000000) != 0
                || (type == 1 && (variation == 0x10 || variation == 0x20 || variation == 0xd0));
        boolean explicitConversion = candidates && (noSuggestions || ascii);
        boolean learning=candidates && !noSuggestions && !noLearning && !ascii && variation!=0x60 && variation!=0x70;
        return new FieldPolicy(password, noLearning, noSuggestions, numeric, candidates, ascii, learning, explicitConversion);
    }
}
