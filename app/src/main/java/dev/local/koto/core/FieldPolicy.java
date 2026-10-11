/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

/** Android's stable input-type bit masks, kept framework-free for host tests. */
public final class FieldPolicy {
    public final boolean password, noPersonalizedLearning, numeric, candidates, ascii;
    private FieldPolicy(boolean password, boolean noLearning, boolean numeric, boolean candidates, boolean ascii) {
        this.password = password; this.noPersonalizedLearning = noLearning;
        this.numeric = numeric; this.candidates = candidates;
        this.ascii = ascii;
    }
    public static FieldPolicy from(int inputType, int imeOptions) {
        int type = inputType & 0xf, variation = inputType & 0xff0;
        boolean password = (type == 1 && (variation == 0x80 || variation == 0x90 || variation == 0xe0))
                || (type == 2 && variation == 0x10);
        boolean numeric = type == 2 || type == 3 || type == 4;
        boolean noLearning = (imeOptions & 0x1000000) != 0;
        boolean candidates = type == 1 && !password && (inputType & 0x80000) == 0;
        boolean ascii = password || (imeOptions & 0x80000000) != 0
                || (type == 1 && (variation == 0x10 || variation == 0x20 || variation == 0xd0));
        return new FieldPolicy(password, noLearning, numeric, candidates, ascii);
    }
}
