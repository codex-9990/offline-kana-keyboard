/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

/** Fixed complete characters for direct input; never inspects or reconstructs host/password text. */
public final class DirectKanaLayout {
    private static final String[][] VOICED={
        {"が","ぎ","ぐ","げ","ご","ざ","じ","ず","ぜ"},
        {"ぞ","だ","ぢ","づ","で","ど","ば","び","ぶ"},
        {"べ","ぼ","ぱ","ぴ","ぷ","ぺ","ぽ","ゔ"}
    };
    private static final String[][] SMALL={
        {"ぁ","ぃ","ぅ","ぇ","ぉ","っ"}, {"ゃ","ゅ","ょ","ゎ","ゕ","ゖ"}
    };
    public static String[][] rows(boolean small,boolean katakana) {
        String[][] source=small ? SMALL : VOICED, copy=new String[source.length][];
        for (int row=0;row<source.length;row++) {
            copy[row]=new String[source[row].length];
            for (int key=0;key<source[row].length;key++) copy[row][key]=script(source[row][key],katakana);
        }
        return copy;
    }
    public static String script(String text,boolean katakana) { return katakana ? FlickLayout.katakana(text) : text; }
    private DirectKanaLayout() { }
}
