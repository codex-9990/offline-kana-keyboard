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
        String committed = "";
        int deletes, cursor, enters;
        boolean accepting = true;
        @Override public boolean commit(String value) {
            if (accepting) committed += value; return accepting;
        }
        @Override public void finish() { }
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
        test("kana preedit remains local until explicit candidate confirmation", () -> {
            Editor e=new Editor(); InputSession s=start(e,1,0);
            yes(s.type("に")); yes(s.type("ほ")); yes(s.type("ん")); eq("にほん",s.text()); eq("",e.committed);
            yes(s.hasCandidates()); yes(s.select("日本")); eq("日本",e.committed); eq("",s.text());
            yes(s.type("あ")); s.enter(); eq("日本あ",e.committed); eq(0,e.enters); s.enter(); eq(1,e.enters);
        });
        test("composition deletion is Unicode codepoint safe", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0);
            s.type("あ"); s.type("😀"); s.backspace(); eq("あ",s.text());
            s.backspace(); eq("",s.text()); eq("",e.committed);
            s.backspace(); eq(1,e.deletes);
        });
        test("mode changes preserve local reading and shift is one-shot", () -> {
            Editor e=new Editor(); InputSession s=start(e,1,0); yes(s.type("か")); s.mode(InputSession.Mode.ENGLISH);
            s.shift(); yes(s.type("a")); yes(s.type("b")); s.mode(InputSession.Mode.SYMBOLS); yes(s.type("@"));
            eq("かAb@",s.text()); eq("",e.committed); no(s.shifted()); yes(s.confirm()); eq("かAb@",e.committed);
        });
        test("all Android password variations: direct input, no candidate or buffer", () -> {
            int[] types = {0x81,0x91,0xe1,0x12};
            for (int type : types) {
                Editor e = new Editor(); InputSession s = start(e,type,0);
                yes(s.policy().password); s.type("SyntheticOnly42");
                eq("",s.text()); eq("",s.preview()); no(s.hasCandidates());
                eq("SyntheticOnly42",e.committed); s.mode(InputSession.Mode.KANA); s.type("あ");
                eq("",s.text()); no(s.hasCandidates());
            }
        });
        test("NO_PERSONALIZED_LEARNING: immutable dictionary allowed, no storage API", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0x1000000);
            yes(s.policy().noPersonalizedLearning); s.type("にほん"); yes(s.hasCandidates());
            s.finish(); eq("",s.text());
        });
        test("numeric phone date null and unknown fields stay direct", () -> {
            for (int type : new int[]{2,3,4}) {
                Editor e = new Editor(); InputSession s = start(e,type,0);
                yes(s.policy().numeric); eq(InputSession.Mode.SYMBOLS,s.mode()); s.type("123"); no(s.hasCandidates());
            }
            for(int type:new int[]{0,5,15,0xf1,0xff1}) {
                Editor e=new Editor(); InputSession s=start(e,type,0); yes(s.type("かな"));
                eq("かな",e.committed); no(s.hasCandidates()); no(s.requestConversion()); eq("",s.text());
            }
        });
        test("NO_SUGGESTIONS body and web body accept local text but require explicit conversion",()->{
            for(int type:new int[]{0x80001,0x800a1}) for(int flags:new int[]{0,0x1000000}) {
                Editor e=new Editor(); InputSession s=start(e,type,flags);
                yes(s.policy().noSuggestions); yes(s.policy().candidates); yes(s.policy().explicitConversion); no(s.policy().learning);
                yes(s.type("にほん")); eq("にほん",s.text()); eq("",e.committed); no(s.hasCandidates());
                no(s.preview("日本",s.revision())); no(s.select("日本",s.revision()));
                yes(s.preedit(true).visible); eq("にほん",s.preedit(true).text);
                yes(s.requestConversion()); yes(s.hasCandidates()); yes(s.preview("日本",s.revision()));
                eq("",e.committed); yes(s.confirm()); eq("日本",e.committed); no(s.conversionRequested());
            }
        });
        test("URI email and FORCE_ASCII keep ASCII access and explicit manual kana conversion",()->{
            for(int variation:new int[]{1,0xa1,0x11,0x21,0xd1}) for(int noSuggestions:new int[]{0,0x80000})
                for(int flags:new int[]{0,0x1000000,0x80000000,0x81000000}) {
                    Editor e=new Editor(); InputSession s=start(e,variation|noSuggestions,flags);
                    boolean ascii=variation==0x11 || variation==0x21 || variation==0xd1 || (flags&0x80000000)!=0;
                    boolean manual=ascii || noSuggestions!=0;
                    eq(ascii ? InputSession.Mode.ENGLISH : InputSession.Mode.KANA,s.mode());
                    eq(manual,s.policy().explicitConversion); if(manual)no(s.policy().learning);
                    yes(s.type("a")); eq("",e.committed); yes(s.confirm()); eq("a",e.committed);
                    s.mode(InputSession.Mode.KANA); yes(s.type("にほん")); eq(!manual,s.hasCandidates());
                    yes(s.requestConversion()); yes(s.preview("日本",s.revision())); yes(s.confirm()); eq("a日本",e.committed);
                }
        });
        test("editing closes explicit candidates while unchanged-reading navigation and relayout preserve them",()->{
            Editor e=new Editor(); InputSession s=start(e,0x80001,0); s.type("にほん"); s.requestConversion();
            yes(s.preview("日本",s.revision())); long old=s.revision(); s.cursor(-1); s.relayout();
            yes(s.conversionRequested()); eq("日本",s.preview()); no(s.select("wrong",old));
            s.backspace(); eq("にほん",s.text()); yes(s.hasCandidates()); s.backspace(); eq("にほ",s.text()); no(s.hasCandidates());
            s.requestConversion(); s.modify(); eq("にぼ",s.text()); no(s.hasCandidates());
            s.requestConversion(); s.space(); no(s.hasCandidates()); s.requestConversion();
            s.mode(InputSession.Mode.ENGLISH); eq("にぼ ",s.text()); no(s.conversionRequested());
            s.mode(InputSession.Mode.KANA); no(s.hasCandidates()); s.requestConversion(); s.incognito(true); no(s.hasCandidates());
            s.requestConversion(); s.discard(); no(s.conversionRequested()); eq("",e.committed);
        });
        test("explicit conversion never relaxes any password nontext or unknown restriction",()->{
            for(int type:new int[]{0x81,0x91,0xe1,0x12,2,3,4,0,5,15,0xf1,0xff1})
                for(int noSuggestions:new int[]{0,0x80000}) for(int flags:new int[]{0,0x1000000,0x80000000,0x81000000}) {
                    Editor e=new Editor(); InputSession s=start(e,type|noSuggestions,flags); s.mode(InputSession.Mode.KANA);
                    yes(s.type("かな")); eq("かな",e.committed); eq("",s.text()); no(s.policy().learning);
                    no(s.policy().candidates); no(s.requestConversion()); no(s.hasCandidates()); no(s.preedit(true).visible);
                }
        });
        test("rejected manual Unicode confirmation retains draft and permits editing and retry",()->{
            Editor e=new Editor(); InputSession s=start(e,0x80011,0x80000000); s.mode(InputSession.Mode.KANA); s.type("にほん");
            s.requestConversion(); s.preview("日本",s.revision()); e.accepting=false; no(s.confirm());
            eq("日本",s.preview()); yes(s.hasCandidates()); yes(s.type("ご")); no(s.hasCandidates()); eq("にほんご",s.text());
            s.backspace(); s.requestConversion(); e.accepting=true; yes(s.select("日本",s.revision())); eq("日本",e.committed);
        });
        test("manual conversion capacity rejection preserves requested preview until an actual edit",()->{
            Editor e=new Editor(); InputSession s=start(e,0x80001,0);
            yes(s.type(new String(new char[256]).replace('\0','あ'))); yes(s.requestConversion()); yes(s.preview("仮",s.revision()));
            no(s.type("い")); yes(s.hasCandidates()); eq("仮",s.preview()); eq("",e.committed);
            s.backspace(); s.backspace(); no(s.hasCandidates()); yes(s.type("い")); yes(s.confirm()); eq(256,e.committed.length());
            yes(s.type("う")); yes(s.confirm()); eq(257,e.committed.length());
        });
        test("email web email and URI fields use local English preedit", () -> {
            for (int type:new int[]{0x11,0x21,0xd1}) {
                Editor e=new Editor(); InputSession s=start(e,type,0); eq(InputSession.Mode.ENGLISH,s.mode());
                yes(s.type("x")); eq("x",s.text()); eq("",e.committed); yes(s.confirm()); eq("x",e.committed);
            }
        });
        test("cursor action preserves local draft without sending it", () -> {
            Editor e=new Editor(); InputSession s=start(e,1,0); s.type("ねこ"); s.cursor(-1);
            eq("",e.committed); eq(-1,e.cursor); eq("ねこ",s.text()); s.confirm(); eq("ねこ",e.committed);
        });
        test("space appends locally and only confirmation sends it", () -> {
            Editor e=new Editor(); InputSession s=start(e,1,0); s.space(); s.type("かな"); s.space();
            eq(" かな ",s.text()); eq("",e.committed); yes(s.confirm()); eq(" かな ",e.committed);
        });
        test("cancel hide and finished input never send an unconfirmed draft", () -> {
            Editor e=new Editor(); InputSession s=start(e,1,0); s.type("かな"); s.discard();
            eq("",e.committed); eq("",s.text()); s.type("にほん"); s.finish(); eq("",e.committed); eq("",s.text());
            no(s.type("bad")); eq("",e.committed);
        });
        test("lifecycle restart model does not copy text across fields", () -> {
            Editor first = new Editor(), second = new Editor(); InputSession s = start(first,1,0);
            s.type("かな"); s.finish(); s.start(second,FieldPolicy.from(0x81,0)); s.type("x");
            eq("",first.committed); eq("x",second.committed); eq("",s.text());
        });
        test("same-session relayout preserves draft and fresh restart discards it", () -> {
            Editor e=new Editor(); InputSession s=start(e,1,0); s.type("かな"); long old=s.revision(); s.relayout();
            eq("かな",s.text()); no(s.preview("仮名",old)); eq("",e.committed);
            s.start(e,FieldPolicy.from(1,0)); eq("",s.text()); s.type("は"); s.confirm(); eq("は",e.committed);
        });
        test("failed confirmation keeps draft editable without lockout or implicit commit", () -> {
            Editor e=new Editor(); InputSession s=start(e,1,0); s.type("あ"); e.accepting=false;
            yes(s.type("い")); eq("あい",s.text()); no(s.confirm()); eq("あい",s.text());
            s.backspace(); eq("あ",s.text()); s.mode(InputSession.Mode.ENGLISH); yes(s.type("b")); s.cursor(1);
            eq("あb",s.text()); eq(1,e.cursor); eq("",e.committed); e.accepting=true; yes(s.confirm()); eq("あb",e.committed);
        });
        test("explicit preedit limit never auto commits and confirmation allows continuous entry", () -> {
            Editor e=new Editor(); InputSession s=start(e,1,0);
            for(int i=0;i<InputSession.MAX_PREEDIT_CODE_POINTS;i++) yes(s.type("😀"));
            no(s.type("あ")); yes(s.limitReached()); eq(256,s.text().codePointCount(0,s.text().length())); eq("",e.committed);
            s.backspace(); yes(s.type("あ")); no(s.limitReached()); yes(s.confirm()); yes(s.type("い")); eq("い",s.text());
        });
        test("typing after local preview restores reading and accepts the next key once", () -> {
            Editor e=new Editor(); InputSession s=start(e,1,0); s.type("にほん"); yes(s.preview("日本",s.revision()));
            eq("日本",s.displayed()); eq("",e.committed); yes(s.type("ご")); eq("にほんご",s.text()); eq("",s.preview());
            s.enter(); eq("にほんご",e.committed); s.enter(); eq(1,e.enters);
        });
        test("backspace cancels preview before deleting reading", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0);
            s.type("にほん"); yes(s.preview("日本",s.revision())); s.backspace();
            eq("にほん",s.text()); eq("にほん",s.text()); eq("",s.preview()); eq(0,e.deletes);
            s.backspace(); eq("にほ",s.text()); eq("",e.committed);
            yes(s.preview("二歩",s.revision())); s.modify(); eq("にぼ",s.text()); eq("",s.preview());
        });
        test("only explicit confirmation sends a preview mode and cursor do not", () -> {
            for(int operation=0;operation<4;operation++) {
                Editor e=new Editor(); InputSession s=start(e,1,0); s.type("はし"); s.preview("橋",s.revision());
                if(operation==0) s.mode(InputSession.Mode.ENGLISH);
                if(operation==1) s.cursor(-1);
                if(operation==2) s.space();
                if(operation==3) s.enter();
                if(operation==3) { eq("橋",e.committed); eq("",s.text()); no(s.confirm()); }
                else { eq("",e.committed); yes(s.confirm()); eq(operation==0 ? "はし" : operation==1 ? "橋" : "はし ",e.committed); }
            }
        });
        test("local preview cancellation and editing work when host confirmation fails", () -> {
            Editor e=new Editor(); InputSession s=start(e,1,0); s.type("にほん"); e.accepting=false;
            yes(s.preview("日本",s.revision())); no(s.confirm()); eq("日本",s.preview());
            yes(s.cancelPreview()); s.backspace(); eq("にほ",s.text()); yes(s.type("ん"));
            e.accepting=true; yes(s.confirm()); eq("にほん",e.committed);
        });
        test("stale candidate actions cannot replace repeated readings or new fields", () -> {
            Editor first = new Editor(); InputSession s = start(first,1,0); s.type("にほん");
            long stale=s.revision(); yes(s.select("日本",stale)); s.type("にほん");
            no(s.select("日本",stale)); no(s.preview("日本",stale)); eq("にほん",s.text());
            Editor second = new Editor(); s.start(second,FieldPolicy.from(1,0)); s.type("にほん");
            no(s.select("日本",stale)); eq("",second.committed);
            no(s.select(null)); no(s.select("")); eq("にほん",s.text());
        });
        test("unconfirmed preview is discarded on field change without leakage", () -> {
            Editor first=new Editor(),second=new Editor(); InputSession s=start(first,1,0);
            s.type("にほん"); s.preview("日本",s.revision()); s.discard(); eq("",first.committed); eq("",s.text());
            s.start(second,FieldPolicy.from(0x81,0)); s.type("x"); eq("x",second.committed); no(s.hasCandidates());
        });
        test("restricted fields refuse candidate preview; FORCE_ASCII starts English", () -> {
            for (int type : new int[]{0x81,0x91,0xe1,0x12,0,5,0xf1}) {
                Editor e = new Editor(); InputSession s = start(e,type,0); s.mode(InputSession.Mode.KANA); s.type("かな");
                no(s.preview("仮名",s.revision())); eq("",s.preview()); eq("",s.text()); eq("かな",e.committed);
            }
            Editor e = new Editor(); InputSession s = start(e,1,0x80000000);
            eq(InputSession.Mode.ENGLISH,s.mode()); s.type("a"); eq("",e.committed); eq("a",s.text()); s.confirm(); eq("a",e.committed);
            s = start(new Editor(),1,0x1000000); s.type("にほん"); yes(s.preview("日本",s.revision()));
            yes(s.policy().noPersonalizedLearning);
        });
        test("failed direct input keeps one-shot Shift until a successful write", () -> {
            Editor e = new Editor(); InputSession s = start(e,0x81,0); s.shift(); e.accepting=false;
            s.type("a"); yes(s.shifted()); eq("",e.committed);
            e.accepting=true; s.type("a"); no(s.shifted()); s.type("b"); eq("Ab",e.committed); eq("",s.text());
        });
        test("sustained repeated conversion produces no duplicate or stale commits", () -> {
            Editor e = new Editor(); InputSession s = start(e,1,0); StringBuilder expected=new StringBuilder();
            for(int i=0;i<100;i++) {
                s.type("に"); s.type("ほ"); s.type("ん"); long revision=s.revision();
                yes(s.preview("日本",revision)); no(s.select("wrong",revision)); s.enter(); expected.append("日本");
            }
            eq(expected.toString(),e.committed); eq("",s.text()); eq("",s.preview());
        });
        test("gesture release emits once; cancelled and outside button drags emit none", () -> {
            KeyGesture flick = new KeyGesture(true,false,22); no(flick.begin(50,50));
            flick.move(-10,50,false); eq(FlickLayout.LEFT,flick.end(-10,50,false)); eq(-1,flick.end(-10,50,false));
            flick.begin(50,50); flick.cancel(); eq(-1,flick.end(50,20,true));
            KeyGesture button = new KeyGesture(false,false,22); button.begin(50,50);
            button.move(120,50,false); eq(-1,button.end(50,50,true));
            button.begin(50,50); eq(FlickLayout.CENTER,button.end(50,50,true));
        });
        test("repeat begins immediately and stops on release, cancel or drag outside", () -> {
            KeyGesture key = new KeyGesture(false,true,22); yes(key.begin(50,50));
            for(int i=0;i<5;i++) yes(key.repeat());
            eq(-1,key.end(50,50,true)); no(key.repeat());
            yes(key.begin(50,50)); key.cancel(); no(key.repeat());
            yes(key.begin(50,50)); key.move(150,50,false); no(key.repeat());
            eq(-1,key.end(50,50,true));
        });
        test("common readings always retain bounded kana and katakana choices", () -> {
            SkkDictionary d = SkkDictionary.load(new FileInputStream(args[0]));
            for(String reading : new String[]{"こう","しょう","か","あ","きょう"}) {
                List<String> choices=d.candidates(reading); yes(choices.size()<=64);
                yes(choices.contains(reading)); yes(choices.contains(FlickLayout.katakana(reading)));
                eq(choices.size(),new java.util.HashSet<>(choices).size());
            }
        });
        test("every printable ASCII character is available through the production layouts", () -> {
            java.util.Set<String> available=new java.util.HashSet<>(); available.add(" ");
            for(int r=0;r<3;r++) for(char c:SymbolLayout.englishRow(r).toCharArray()) {
                available.add(String.valueOf(c)); available.add(String.valueOf(Character.toUpperCase(c)));
            }
            for(int page=0;page<SymbolLayout.pages();page++) for(int r=0;r<3;r++)
                available.addAll(java.util.Arrays.asList(SymbolLayout.row(page,r)));
            available.addAll(java.util.Arrays.asList(SymbolLayout.bottom()));
            for(int c=32;c<=126;c++) yes(available.contains(String.valueOf((char)c)));
            yes(available.contains("~")); eq("〜",FlickLayout.kana(10,FlickLayout.DOWN));
            String[] clone=SymbolLayout.row(0,0); clone[0]="bad"; eq("1",SymbolLayout.row(0,0)[0]);
        });
        test("synthetic A-dollar-1 password can be entered without composition or candidates", () -> {
            Editor e=new Editor(); InputSession s=start(e,0x81,0); s.shift(); s.type("a");
            s.mode(InputSession.Mode.SYMBOLS); yes(java.util.Arrays.asList(SymbolLayout.bottom()).contains("$"));
            s.type("$"); s.type(SymbolLayout.row(0,0)[0]); eq("A$1",e.committed); eq("",s.text()); no(s.hasCandidates());
            for(String value:new String[]{"[","]","{","}","<",">","^","`","|","~"}) s.type(value);
            eq("",s.text()); eq("",s.preview()); no(s.hasCandidates());
        });
        test("standard editor labels correspond to executable action IDs", () -> {
            String[] labels={"移動","検索","送信","次へ","完了","前へ"};
            for(int id=2;id<=7;id++) {
                EditorAction action=EditorAction.from(id,0,null); yes(action.enabled()); eq(id,action.id()); eq(labels[id-2],action.label());
            }
            no(EditorAction.from(0,0,null).enabled()); no(EditorAction.from(255,0,null).enabled());
        });
        test("custom actions share an explicit label and safely handle suppressed or missing captions", () -> {
            EditorAction custom=EditorAction.from(0,42,"保存"); yes(custom.enabled()); eq(42,custom.id()); eq("実行\n保存",custom.label());
            eq("実行\n↵",EditorAction.from(0,42,"↵").label());
            no(EditorAction.from(0x40000000,42,"保存").enabled()); eq("↵",EditorAction.from(0x40000000,42,"保存").label());
            no(EditorAction.from(0,42," \t\n").enabled());
        });
        test("host action captions are bounded and stripped of control and bidi formatting", () -> {
            EditorAction action=EditorAction.from(0,42,"\u202E保\u200B存\n"); eq("実行\n保存",action.label());
            StringBuilder huge=new StringBuilder(); for(int i=0;i<5000;i++) huge.append("あ");
            action=EditorAction.from(0,42,huge); yes(action.label().length()<16); yes(action.description().length()<50);
            no(EditorAction.from(0,42,"\ud800").enabled());
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
