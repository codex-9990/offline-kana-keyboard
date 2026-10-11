/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Host tests use synthetic input exclusively; no Android/device credentials. */
public final class CoreTests {
    private static int count;
    private interface Test { void run() throws Exception; }
    private static void test(String name, Test body) throws Exception {
        body.run(); count++; System.out.println("PASS: " + name);
    }
    private static void eq(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
    private static void yes(boolean condition) { if (!condition) throw new AssertionError("Expected true"); }
    private static void no(boolean condition) { yes(!condition); }
    private static final class Editor implements InputSession.Editor {
        String committed = "", composition = "";
        int deletes, cursor, enters;
        boolean accepting = true;
        @Override public boolean composing(String value) { if (accepting) composition = value; return accepting; }
        @Override public boolean commit(String value) {
            if (accepting) { committed += value; composition = ""; } return accepting;
        }
        @Override public void finish() { committed += composition; composition = ""; }
        @Override public void delete() { deletes++; }
        @Override public void cursor(int direction) { cursor += direction; }
        @Override public void enter() { enters++; }
    }
    private static InputSession start(Editor editor, int type, int options) {
        InputSession s = new InputSession(); s.start(editor, FieldPolicy.from(type, options)); return s;
    }
    public static void main(String[] args) throws Exception {
        test("all 12 keys and five flick directions", () -> {
            String[] expected = {"あいうえお","かきくけこ","さしすせそ","たちつてと","なにぬねの","はひふへほ",
                "まみむめも","や（ゆ）よ","らりるれろ",null,"わをんー〜","、。？！…"};
            for (int k = 0; k < 12; k++) for (int d = 0; d < 5; d++)
                eq(k == 9 ? "゛小" : String.valueOf(expected[k].charAt(d)), FlickLayout.kana(k, d));
            String[] clone = FlickLayout.key(0); clone[0] = "bad"; eq("あ", FlickLayout.kana(0, 0));
        });
        test("gesture threshold, axis, diagonal and negative directions", () -> {
            eq(0,FlickLayout.direction(21.99f,0,22)); eq(1,FlickLayout.direction(-22,0,22));
            eq(2,FlickLayout.direction(0,-22,22)); eq(3,FlickLayout.direction(22,0,22));
            eq(4,FlickLayout.direction(0,22,22)); eq(3,FlickLayout.direction(25,-25,22));
            eq(2,FlickLayout.direction(3,-30,22)); eq(4,FlickLayout.direction(-4,30,22));
        });
        test("dakuten, handakuten, small kana, unsupported suffix", () -> {
            eq("ぱ",FlickLayout.modifyLast("ば")); eq("は",FlickLayout.modifyLast("ぱ"));
            eq("にっ",FlickLayout.modifyLast("につ")); eq("づ",FlickLayout.modifyLast("っ"));
            eq("ぁ",FlickLayout.modifyLast("あ")); eq("ゔ",FlickLayout.modifyLast("ぅ"));
            eq("😀",FlickLayout.modifyLast("😀")); eq("",FlickLayout.modifyLast(""));
        });
        test("kana composition and candidate replace", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0);
            s.type("に"); s.type("ほ"); s.type("ん"); eq("にほん",e.composition);
            eq("",e.committed); yes(s.hasCandidates()); s.select("日本");
            eq("日本",e.committed); eq("",s.text()); no(s.hasCandidates());
            s.type("あ"); s.enter(); eq("日本あ",e.committed); eq(0,e.enters);
            s.enter(); eq(1,e.enters);
        });
        test("composition deletion is Unicode codepoint safe", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0);
            s.type("あ"); s.type("😀"); s.backspace(); eq("あ",s.text());
            s.backspace(); eq("",s.text()); eq("",e.committed);
            s.backspace(); eq(1,e.deletes);
        });
        test("mode change commits once; shift is one-shot", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0);
            s.type("か"); s.mode(InputSession.Mode.ENGLISH); eq("か",e.committed);
            s.shift(); s.type("a"); s.type("b"); eq("かAb",e.committed); no(s.shifted());
            s.mode(InputSession.Mode.SYMBOLS); s.type("@"); eq("かAb@",e.committed);
        });
        test("all Android password variations: direct input, no candidate or buffer", () -> {
            int[] types = {0x81,0x91,0xe1,0x12};
            for (int type : types) {
                Editor e = new Editor(); InputSession s = start(e,type,0);
                yes(s.policy().password); s.type("SyntheticOnly42");
                eq("",s.text()); eq("",e.composition); no(s.hasCandidates());
                eq("SyntheticOnly42",e.committed); s.mode(InputSession.Mode.KANA); s.type("あ");
                eq("",s.text()); no(s.hasCandidates());
            }
        });
        test("NO_PERSONALIZED_LEARNING: immutable dictionary allowed, no storage API", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0x1000000);
            yes(s.policy().noPersonalizedLearning); s.type("にほん"); yes(s.hasCandidates());
            s.finish(); eq("",s.text());
        });
        test("numeric, phone, date, and NO_SUGGESTIONS policy", () -> {
            for (int type : new int[]{2,3,4}) {
                Editor e = new Editor(); InputSession s = start(e,type,0);
                yes(s.policy().numeric); eq(InputSession.Mode.SYMBOLS,s.mode()); s.type("123"); no(s.hasCandidates());
            }
            Editor e = new Editor(); InputSession s = start(e,0x80001,0);
            s.type("かな"); eq("かな",e.committed); no(s.hasCandidates()); eq("",s.text());
        });
        test("email, web email and URI fields default to English", () -> {
            for (int type : new int[]{0x11,0x21,0xd1}) {
                Editor e = new Editor(); InputSession s = start(e,type,0);
                eq(InputSession.Mode.ENGLISH,s.mode()); s.type("x"); eq("x",e.committed);
            }
        });
        test("cursor action commits current composition", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0);
            s.type("ねこ"); s.cursor(-1); eq("ねこ",e.committed); eq(-1,e.cursor); eq("",s.text());
        });
        test("space commits a word and inserts a direct space without leftover composition", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0);
            s.space(); eq(" ",e.committed); eq("",s.text()); no(s.hasCandidates());
            s.type("かな"); s.space(); eq(" かな ",e.committed); eq("",s.text());
        });
        test("outside selection and hide keep editor text, clear transient state", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0);
            s.type("かな"); s.selectionMovedOutsideComposition(); eq("かな",e.committed); eq("",s.text());
            s.type("にほん"); s.finish(); eq("かなにほん",e.committed); eq("",s.text());
            s.type("bad"); eq("かなにほん",e.committed);
        });
        test("lifecycle restart model does not copy text across fields", () -> {
            Editor first = new Editor(), second = new Editor(); InputSession s = start(first,1,0);
            s.type("かな"); s.finish(); s.start(second,FieldPolicy.from(0x81,0)); s.type("x");
            eq("かな",first.committed); eq("x",second.committed); eq("",s.text());
        });
        test("rotation model: finish span then restart yields no duplicate", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0);
            s.type("かな"); s.selectionMovedOutsideComposition(); s.finish();
            s.start(e,FieldPolicy.from(1,0)); s.type("は"); s.commitComposition(); eq("かなは",e.committed);
        });
        test("failed host writes retain existing composing text", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0);
            s.type("あ"); e.accepting = false; s.type("い"); eq("あ",s.text());
            s.backspace(); eq("あ",s.text()); s.select("亜"); eq("あ",s.text());
            s.commitComposition(); eq("あ",s.text());
            s.mode(InputSession.Mode.ENGLISH); eq(InputSession.Mode.KANA,s.mode());
            s.cursor(1); eq(0,e.cursor);
        });
        test("bounded composition protects typing responsiveness", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0);
            for (int i=0;i<65;i++) s.type("あ"); eq(64,e.committed.length()); eq("あ",s.text());
        });
        test("dictionary parser strips annotations; never evaluates SKK expressions", () -> {
            String data = "ねこ\t猫;animal\nたべr\t食べ\nあ\t(concat evil)\t#1\t亜\n";
            SkkDictionary d = SkkDictionary.load(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
            yes(d.candidates("ねこ").contains("猫")); yes(d.candidates("たべる").contains("食べる"));
            no(d.candidates("あ").contains("(concat evil)")); no(d.candidates("あ").contains("#1"));
            eq("アイウヴー",FlickLayout.katakana("あいうゔー"));
            try { d.candidates("あ").add("bad"); throw new AssertionError("mutable candidates"); }
            catch (UnsupportedOperationException expected) {}
        });
        test("bundled full SKK-ML data provides real word and okuri candidates", () -> {
            SkkDictionary d = SkkDictionary.load(new FileInputStream(args[0]));
            yes(d.size() > 40000);
            for (String[] pair : new String[][]{{"にほん","日本"},{"とうきょう","東京"},{"かんじ","漢字"},
                    {"がっこう","学校"},{"はし","橋"},{"たべる","食べる"},{"かく","書く"}})
                yes(d.candidates(pair[0]).contains(pair[1]));
            List<String> unknown = d.candidates("あぴぬめそ"); yes(unknown.contains("あぴぬめそ"));
            yes(unknown.contains("アピヌメソ"));
            no(d.candidates("きょう").contains("PersistentSyntheticMarker"));
        });
        System.out.println("PASS: " + count + " host test groups; Android runtime coverage NOT RUN");
    }
}
