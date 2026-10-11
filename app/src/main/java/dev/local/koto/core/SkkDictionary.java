/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only SKK word lookup, including simple okurigana. Not a sentence engine. */
public final class SkkDictionary {
    private final Map<String, List<String>> entries;
    private SkkDictionary(Map<String, List<String>> entries) {
        this.entries = Collections.unmodifiableMap(entries);
    }
    public static SkkDictionary load(InputStream source) throws IOException {
        Map<String, List<String>> entries = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) throw new IOException("Loading cancelled");
                if (line.startsWith("#") || line.isEmpty()) continue;
                String[] fields = line.split("\t", -1);
                if (fields.length < 2) throw new IOException("Invalid dictionary format");
                List<String> values = new ArrayList<>();
                for (int i = 1; i < fields.length; i++) {
                    String word = fields[i].split(";", 2)[0];
                    // SKK Lisp expressions are data; never evaluate or commit them.
                    if (!word.isEmpty() && !word.startsWith("(") && !word.contains("#")
                            && !word.contains("\uFFFD")) values.add(word);
                }
                if (!values.isEmpty()) entries.put(fields[0], Collections.unmodifiableList(values));
            }
        }
        if (entries.isEmpty()) throw new IOException("Empty dictionary");
        return new SkkDictionary(entries);
    }
    public int size() { return entries.size(); }
    public List<String> candidates(String reading) {
        if (reading.isEmpty()) return Collections.emptyList();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        List<String> exact = entries.get(reading);
        if (exact != null) result.addAll(exact);
        // Longest matching stem first. SKK's okuri key ends in a Roman initial.
        for (int split = reading.length() - 1; split > 0 && result.size() < 64; split--) {
            String suffix = reading.substring(split);
            String initial = okuriInitial(suffix.charAt(0));
            if (initial.isEmpty()) continue;
            List<String> stems = entries.get(reading.substring(0, split) + initial);
            if (stems != null) for (String stem : stems) result.add(stem + suffix);
        }
        result.add(reading);
        result.add(FlickLayout.katakana(reading));
        List<String> limited = new ArrayList<>(result);
        return Collections.unmodifiableList(limited.subList(0, Math.min(64, limited.size())));
    }
    private static String okuriInitial(char kana) {
        String[] rows = {"ぁあ", "ぃい", "ぅう", "ぇえ", "ぉお", "かきくけこ", "がぎぐげご",
            "さしすせそ", "ざじずぜぞ", "たちつてと", "だぢづでど", "なにぬねの",
            "はひふへほ", "ばびぶべぼ", "ぱぴぷぺぽ", "まみむめも", "やゆよ", "らりるれろ", "わを"};
        String[] initial = {"a", "i", "u", "e", "o", "k", "g", "s", "z", "t", "d", "n", "h", "b", "p", "m", "y", "r", "w"};
        for (int i = 0; i < rows.length; i++) if (rows[i].indexOf(kana) >= 0) return initial[i];
        return "";
    }
}
