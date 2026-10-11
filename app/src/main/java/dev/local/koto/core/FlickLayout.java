/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

/** Standard Japanese 12-key layout: center, left, up, right, down. */
public final class FlickLayout {
    public static final int CENTER = 0, LEFT = 1, UP = 2, RIGHT = 3, DOWN = 4;
    private static final String[][] KEYS = {
        {"あ", "い", "う", "え", "お"}, {"か", "き", "く", "け", "こ"},
        {"さ", "し", "す", "せ", "そ"}, {"た", "ち", "つ", "て", "と"},
        {"な", "に", "ぬ", "ね", "の"}, {"は", "ひ", "ふ", "へ", "ほ"},
        {"ま", "み", "む", "め", "も"}, {"や", "（", "ゆ", "）", "よ"},
        {"ら", "り", "る", "れ", "ろ"}, {"゛小", "゛小", "゛小", "゛小", "゛小"},
        {"わ", "を", "ん", "ー", "〜"}, {"、", "。", "？", "！", "…"}
    };
    private FlickLayout() {}
    public static String[] key(int position) { return KEYS[position].clone(); }
    public static String kana(int position, int direction) { return KEYS[position][direction]; }
    public static int direction(float dx, float dy, float threshold) {
        if (Math.hypot(dx, dy) < threshold) return CENTER;
        if (Math.abs(dx) >= Math.abs(dy)) return dx < 0 ? LEFT : RIGHT;
        return dy < 0 ? UP : DOWN;
    }
    public static String modifyLast(String text) {
        if (text.isEmpty()) return text;
        int index = text.offsetByCodePoints(text.length(), -1);
        String last = text.substring(index);
        String[] cycles = {"あぁ", "いぃ", "うぅゔ", "えぇ", "おぉ", "やゃ", "ゆゅ", "よょ", "わゎ",
            "かが", "きぎ", "くぐ", "けげ", "こご", "さざ", "しじ", "すず", "せぜ", "そぞ",
            "ただ", "ちぢ", "つっづ", "てで", "とど", "はばぱ", "ひびぴ", "ふぶぷ", "へべぺ", "ほぼぽ"};
        for (String cycle : cycles) {
            int p = cycle.indexOf(last);
            if (p >= 0) return text.substring(0, index) + cycle.charAt((p + 1) % cycle.length());
        }
        return text;
    }
    public static String katakana(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            result.append(c >= 'ぁ' && c <= 'ゖ' ? (char) (c + 0x60) : c);
        }
        return result.toString();
    }
}
