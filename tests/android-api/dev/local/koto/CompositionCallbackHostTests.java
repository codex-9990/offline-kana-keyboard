/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import dev.local.koto.core.FieldPolicy;
import dev.local.koto.core.FlickLayout;
import dev.local.koto.core.InputSession;
import dev.local.koto.core.KeyGesture;
import dev.local.koto.core.EditorSelection;
import dev.local.koto.core.SkkDictionary;
import java.io.FileInputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Replaces composing spans in a visible buffer and delivers realistic queued selection callbacks. */
public final class CompositionCallbackHostTests {
    private static int count;
    private static String filter="";
    private interface Test { void run() throws Exception; }
    private static void test(String name,Test body) throws Exception {
        if (!filter.isEmpty() && !name.contains(filter)) return;
        body.run(); count++; System.out.println("PASS: "+name);
    }
    private static void eq(Object expected,Object actual) { if (!expected.equals(actual)) throw new AssertionError("Expected "+expected+"; got "+actual); }
    private static void yes(boolean b) { if (!b) throw new AssertionError("Expected true"); }
    private static final class Host {
        String text="";
        int cursor,selectionEnd,spanStart=-1,spanEnd=-1,finishes,commits,regions,batches,reportedCursor;
        boolean omitRange,stripSpan,accept=true,regionSupported=true,regionUpdates,batchSupported=true;
        final ArrayDeque<int[]> updates=new ArrayDeque<>();
        final List<String> learned=new ArrayList<>();
        InputSession session=new InputSession(); ConnectionEditor editor;
        final InputConnection connection=(InputConnection)Proxy.newProxyInstance(InputConnection.class.getClassLoader(),
                new Class<?>[]{InputConnection.class},(proxy,method,args)->{
                    String name=method.getName();
                    if (name.startsWith("get")) throw new AssertionError("Surrounding/host text reads forbidden: "+name);
                    if (name.equals("beginBatchEdit")) { batches++; return batchSupported; }
                    if (name.equals("endBatchEdit")) { batches--; return true; }
                    if (name.equals("setComposingRegion")) {
                        regions++; if (!regionSupported || !accept) return false;
                        spanStart=(Integer)args[0]; spanEnd=(Integer)args[1];
                        yes(spanStart>=0 && spanEnd>=spanStart && spanEnd<=text.length());
                        if (regionUpdates) updates.add(new int[]{cursor,selectionEnd,cursor,selectionEnd,omitRange ? -1 : spanStart,omitRange ? -1 : spanEnd});
                        return true;
                    }
                    if (name.equals("setComposingText") || name.equals("commitText")) {
                        if (!accept) return false;
                        int old=cursor,oldEnd=selectionEnd;
                        int first=spanStart>=0 ? spanStart : Math.min(cursor,selectionEnd);
                        int last=spanStart>=0 ? spanEnd : Math.max(cursor,selectionEnd);
                        String value=args[0].toString(); eq(1,args[1]);
                        text=text.substring(0,first)+value+text.substring(last);
                        cursor=selectionEnd=first+value.length();
                        if (name.equals("setComposingText")) { spanStart=first; spanEnd=cursor; }
                        else { commits++; spanStart=spanEnd=-1; }
                        if (stripSpan) spanStart=spanEnd=-1;
                        updates.add(new int[]{old,oldEnd,cursor,selectionEnd,omitRange ? -1 : spanStart,omitRange ? -1 : spanEnd});
                        return true;
                    }
                    if (name.equals("finishComposingText")) {
                        finishes++; spanStart=spanEnd=-1;
                        updates.add(new int[]{cursor,selectionEnd,cursor,selectionEnd,-1,-1}); return true;
                    }
                    if (name.equals("performEditorAction")) return true;
                    return method.getReturnType()==boolean.class ? false : null;
                });
        Host(int type,int flags) {
            this(type,flags,"",false);
        }
        Host(int type,int flags,String prefix,boolean unknown) {
            text=prefix; cursor=selectionEnd=prefix.length();
            reportedCursor=cursor;
            EditorInfo info=new EditorInfo(); info.inputType=type; info.imeOptions=flags;
            info.initialSelStart=info.initialSelEnd=unknown ? -1 : cursor;
            editor=new ConnectionEditor(connection,info,(code,keyFlags)->{});
            session.start(editor,FieldPolicy.from(type,flags)); session.choices((r,w,p)->learned.add(r+":"+w));
        }
        void flush() {
            int guard=0;
            while (!updates.isEmpty()) {
                yes(++guard<1000); int[] u=updates.remove();
                reportedCursor=u[2];
                editor.updateSelection(session,u[0],u[1],u[2],u[3],u[4],u[5]);
            }
        }
        void restart() {
            session.finish(); flush();
            EditorInfo info=new EditorInfo(); info.inputType=1;
            info.initialSelStart=cursor; info.initialSelEnd=selectionEnd;
            editor=new ConnectionEditor(connection,info,(code,keyFlags)->{});
            session.start(editor,FieldPolicy.from(1,0));
        }
        void coalesce(boolean noMotion) {
            if (updates.isEmpty()) return;
            updates.clear();
            int old=noMotion ? cursor : reportedCursor;
            updates.add(new int[]{old,old,cursor,selectionEnd,omitRange ? -1 : spanStart,omitRange ? -1 : spanEnd});
            flush();
        }
        void kana(int key,int direction) {
            KeyGesture gesture=new KeyGesture(true,false,22);
            yes(!gesture.begin(50,50)); float x=50,y=50;
            if (direction==1) x-=30; if (direction==2) y-=30; if (direction==3) x+=30; if (direction==4) y+=30;
            int d=gesture.end(x,y,true); session.type(FlickLayout.kana(key,d));
        }
    }
    private static void sequenceStep(Host h,char operation,int step) {
        String before=h.text,reading=h.session.text(),preview=h.session.preview();
        String displayed=preview.isEmpty() ? reading : preview;
        if (operation=='T') {
            String value=step%2==0 ? "ご" : "あ";
            h.session.type(value);
            if (!h.session.text().equals(reading) || !h.session.preview().equals(preview)) eq(before+value,h.text);
            else eq(before,h.text);
        } else if (operation=='P') {
            String value=new String[]{"日本","日","日本語"}[step%3];
            boolean success=h.session.preview(value,h.session.revision());
            if (success) {
                yes(before.endsWith(displayed)); eq(before.substring(0,before.length()-displayed.length())+value,h.text);
            } else eq(before,h.text);
        } else if (operation=='C') {
            boolean success=h.session.cancelPreview();
            if (success && !preview.isEmpty()) {
                yes(before.endsWith(preview)); eq(before.substring(0,before.length()-preview.length())+reading,h.text);
            } else eq(before,h.text);
        } else {
            if (operation=='F') h.flush(); else h.coalesce(operation=='N');
            eq(before,h.text); // Selection callbacks may finish a span, never rewrite its text.
        }
        yes(h.text.startsWith("prefix:")); eq(0,h.batches); yes(h.learned.isEmpty());
    }
    private static void sequenceCursor(Host h,int position,boolean drainFirst) {
        if (drainFirst) h.flush();
        String before=h.text; int old=h.cursor;
        int next=7+Math.min(position,before.length()-8);
        yes(next>=7 && next<old);
        h.cursor=h.selectionEnd=next;
        h.updates.add(new int[]{old,old,next,next,h.omitRange ? -1 : h.spanStart,h.omitRange ? -1 : h.spanEnd});
        h.flush(); eq("",h.session.text()); eq(before,h.text);
        int regions=h.regions; h.session.type("あ"); h.flush();
        String inserted=before.substring(0,next)+"あ"+before.substring(next);
        if (h.editor.compositionNeedsRestart()) {
            eq(before,h.text); eq("",h.session.text()); eq(regions,h.regions); yes(h.editor.compositionRejected());
            // A new adapter receives a trusted EditorInfo selection; the old epoch stays fenced.
            h.restart(); h.session.type("あ"); h.flush(); eq(inserted,h.text); eq("あ",h.session.text());
        } else eq(inserted,h.text);
        yes(h.text.startsWith("prefix:")); eq(0,h.batches); yes(h.learned.isEmpty());
    }
    public static void main(String[] args) throws Exception {
        filter=args.length>1 ? args[1] : "";
        final SkkDictionary dictionary=SkkDictionary.load(new FileInputStream(args[0]));
        test("sequential kana survives omitted composing-range callbacks and converts as one word",()->{
            Host h=new Host(1,0); h.omitRange=true;
            h.kana(4,1); h.flush(); eq("に",h.session.text()); eq(0,h.finishes);
            h.kana(5,4); h.flush(); eq("にほ",h.session.text());
            h.kana(10,2); h.flush(); eq("にほん",h.session.text()); eq("にほん",h.text);
            yes(dictionary.candidates(h.session.text()).contains("日本"));
            yes(h.session.select("日本",h.session.revision())); h.flush(); eq("日本",h.text); eq(1,h.commits);
        });
        test("standard reported spans retain every flick until explicit conversion",()->{
            Host h=new Host(1,0); h.kana(4,1); h.flush(); h.kana(5,4); h.flush(); h.kana(10,2); h.flush();
            eq("にほん",h.text); eq("にほん",h.session.text()); eq(0,h.finishes); eq(0,h.regions);
            yes(h.session.select("日本",h.session.revision())); h.flush(); eq("日本",h.text);
        });
        test("dropped composing spans are restored without duplicating kana or preceding host text",()->{
            Host h=new Host(1,0,"prefix:",false); h.omitRange=true; h.stripSpan=true;
            h.kana(4,1); h.flush(); h.kana(5,4); h.flush(); h.kana(10,2); h.flush();
            eq("prefix:にほん",h.text); eq("にほん",h.session.text()); eq(0,h.commits);
            yes(h.session.select("日本",h.session.revision())); h.flush();
            eq("prefix:日本",h.text); eq(3,h.regions); eq(0,h.batches);
        });
        test("delayed self selection callbacks cannot clear the newest composition",()->{
            for (boolean omit:new boolean[]{false,true}) {
                Host h=new Host(1,0); h.omitRange=omit;
                h.kana(4,1); h.kana(5,4); h.kana(10,2); h.flush();
                eq("にほん",h.session.text()); eq("にほん",h.text); eq(0,h.finishes);
                h.session.preview("日本",h.session.revision()); h.flush(); eq("日本",h.session.preview());
                h.session.enter(); h.flush(); eq("日本",h.text); eq(1,h.commits);
            }
        });
        test("coalesced callbacks acknowledge a burst without changing current kana",()->{
            Host h=new Host(1,0); h.omitRange=true;
            for (int i=0;i<32;i++) h.session.type("あ");
            h.updates.clear(); h.updates.add(new int[]{0,0,32,32,-1,-1}); h.flush();
            eq(32,h.session.text().length()); eq(32,h.text.length()); eq(0,h.finishes);
        });
        test("preview cancellation deletion and length-changing candidates survive callbacks",()->{
            Host h=new Host(1,0); h.omitRange=true; h.stripSpan=true;
            h.session.type("にほん"); h.flush(); yes(h.session.preview("日本",h.session.revision())); h.flush();
            eq("日本",h.text); h.session.backspace(); h.flush(); eq("にほん",h.text); eq("",h.session.preview());
            h.session.backspace(); h.flush(); eq("にほ",h.text); eq("にほ",h.session.text());
            h.session.backspace(); h.flush(); h.session.backspace(); h.flush(); eq("",h.text); eq("",h.session.text()); eq(0,h.batches);
        });
        test("real cursor moves and noncollapsed selections end composition without replacement",()->{
            for (boolean range:new boolean[]{false,true}) {
                Host h=new Host(1,0); h.omitRange=!range; h.session.type("にほん"); h.flush();
                h.cursor=1; h.selectionEnd=range ? 2 : 1;
                h.updates.add(new int[]{3,3,h.cursor,h.selectionEnd,range ? 0 : -1,range ? 3 : -1}); h.flush();
                eq("",h.session.text()); eq("にほん",h.text); eq(0,h.commits);
                h.session.type("あ"); h.flush(); eq(range ? "にあん" : "にあほん",h.text);
            }
        });
        test("no personalized learning and manual private mode retain dictionary conversion",()->{
            for (boolean manual:new boolean[]{false,true}) {
                Host h=new Host(1,manual ? 0 : EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING); h.omitRange=true;
                if (manual) h.session.incognito(true);
                h.kana(4,1); h.flush(); h.kana(5,4); h.flush(); h.kana(10,2); h.flush();
                eq("にほん",h.session.text()); yes(dictionary.candidates(h.session.text()).contains("日本"));
                yes(h.session.select("日本",h.session.revision())); h.flush(); eq("日本",h.text); yes(h.learned.isEmpty());
            }
        });
        test("password and no-suggestions fields remain direct with no buffer or learning",()->{
            for (int type:new int[]{0x81,0x91,0xe1,0x12,0x80001}) {
                Host h=new Host(type,0); h.session.mode(InputSession.Mode.KANA); h.omitRange=true;
                h.kana(4,1); h.flush(); h.kana(5,4); h.flush(); h.kana(10,2); h.flush();
                eq("",h.session.text()); eq("にほん",h.text); eq(3,h.commits); yes(!h.session.hasCandidates()); yes(h.learned.isEmpty());
            }
        });
        test("rejected writes and unsupported region recovery do not silently commit or duplicate",()->{
            Host h=new Host(1,0); h.accept=false; h.session.type("に"); eq("",h.text); eq("",h.session.text());
            yes(h.editor.compositionRejected()); h.accept=true; h.omitRange=true; h.session.type("に"); h.flush();
            h.regionSupported=false; h.session.type("ほ"); h.flush(); eq("に",h.text); eq("に",h.session.text());
            eq(0,h.commits); eq(0,h.finishes); eq(0,h.batches); yes(h.editor.compositionRejected());
            h.regionSupported=true; h.session.type("ほ"); h.flush(); eq("にほ",h.text); yes(!h.editor.compositionRejected());
        });
        test("unknown initial selection uses supplied span or one acknowledged write without surrounding reads",()->{
            for (boolean omit:new boolean[]{false,true}) {
                Host h=new Host(1,0,"prefix:",true); h.omitRange=omit; h.stripSpan=omit;
                h.session.type("に"); h.flush(); h.session.type("ほ"); h.flush(); h.session.type("ん"); h.flush();
                eq("prefix:にほん",h.text); eq("にほん",h.session.text()); yes(h.session.select("日本",h.session.revision()));
                h.flush(); eq("prefix:日本",h.text);
            }
        });
        test("no usable initial or callback anchor rejects unsafe region guessing",()->{
            Host h=new Host(1,0,"prefix:",true); h.omitRange=true; h.session.type("に"); h.updates.clear();
            h.updates.add(new int[]{-1,-1,h.cursor,h.cursor,-1,-1}); h.flush(); h.session.type("ほ");
            eq("prefix:に",h.text); eq("に",h.session.text()); eq(0,h.regions); yes(h.editor.compositionRejected());
        });
        test("acknowledged commits and later finish duplicates retain the next composing word",()->{
            Host h=new Host(1,0); h.omitRange=true; h.session.type("にほん"); h.flush();
            h.session.select("日本",h.session.revision()); h.flush(); h.session.type("ご"); h.flush();
            eq("日本ご",h.text); eq("ご",h.session.text()); eq(1,h.commits);
            h.session.enter(); h.flush(); eq("日本ご",h.text); eq(2,h.commits);
        });
        test("selection expectation metadata stays bounded and contains no text history",()->{
            EditorSelection s=new EditorSelection(0,0);
            for (int i=0;i<10000;i++) s.expect(1,true);
            eq(EditorSelection.MAX_EXPECTED,s.pendingMetadataCount());
            s.select(0,0); eq(0,s.pendingMetadataCount());
        });
        test("region restoration callbacks are recognized even when batching is unsupported",()->{
            for (boolean batch:new boolean[]{false,true}) {
                Host h=new Host(1,0); h.omitRange=true; h.stripSpan=true; h.regionUpdates=true; h.batchSupported=batch;
                h.kana(4,1); h.flush(); h.kana(5,4); h.flush(); h.kana(10,2); h.flush();
                eq("にほん",h.session.text()); eq("にほん",h.text); eq(0,h.finishes); eq(0,h.batches);
                yes(h.session.select("日本",h.session.revision())); h.flush(); eq("日本",h.text);
            }
        });
        test("conflicting positive composing ranges end local state instead of overwriting unrelated spans",()->{
            Host h=new Host(1,0); h.session.type("にほん"); h.flush();
            h.updates.add(new int[]{3,3,3,3,1,3}); h.flush(); eq("",h.session.text()); eq("にほん",h.text); eq(0,h.regions);
        });
        test("incremental cursor move to an acknowledged old position ends composition",()->{
            Host h=new Host(1,0); h.omitRange=true;
            h.kana(4,1); h.flush(); h.kana(5,4); h.flush(); h.kana(10,2); h.flush();
            h.cursor=h.selectionEnd=1; h.updates.add(new int[]{3,3,1,1,-1,-1}); h.flush();
            eq("",h.session.text()); eq("にほん",h.text);
            h.session.type("あ"); h.flush(); eq("にあほん",h.text); eq("あ",h.session.text());
        });
        test("stripped spans with delayed callbacks do not append accumulated readings",()->{
            Host h=new Host(1,0); h.omitRange=true; h.stripSpan=true;
            h.kana(4,1); h.kana(5,4); h.kana(10,2); eq("にほん",h.text); h.flush();
            eq("にほん",h.session.text()); yes(h.session.select("日本",h.session.revision())); h.flush(); eq("日本",h.text);
        });
        test("incremental cursor targets preserve prefix text across reported and omitted spans",()->{
            for (boolean omit:new boolean[]{false,true}) for (int position:new int[]{0,1,2}) {
                Host h=new Host(1,EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING,"prefix:",false); h.omitRange=omit;
                h.kana(4,1); h.flush(); h.kana(5,4); h.flush(); h.kana(10,2); h.flush();
                int next=7+position; h.cursor=h.selectionEnd=next;
                h.updates.add(new int[]{10,10,next,next,omit ? -1 : 7,omit ? -1 : 10}); h.flush();
                eq("",h.session.text()); h.session.type("あ"); h.flush();
                eq("prefix:"+"にほん".substring(0,position)+"あ"+"にほん".substring(position),h.text); yes(h.learned.isEmpty());
            }
        });
        test("stripped delayed bursts and acknowledged preview changes preserve prefix",()->{
            for (boolean regionCallbacks:new boolean[]{false,true}) for (boolean coalesce:new boolean[]{false,true}) {
                Host h=new Host(1,0,"prefix:",false); h.omitRange=true; h.stripSpan=true; h.regionUpdates=regionCallbacks;
                h.kana(4,1); h.kana(5,4); h.kana(10,2); eq("prefix:にほん",h.text);
                if (coalesce) { h.updates.clear(); h.updates.add(new int[]{7,7,10,10,-1,-1}); }
                h.flush(); eq("にほん",h.session.text()); eq(0,h.finishes); eq(0,h.batches);
                h.session.preview("日本",h.session.revision()); h.flush(); h.session.cancelPreview(); h.flush();
                eq("prefix:にほん",h.text); yes(h.session.select("日本",h.session.revision())); h.flush(); eq("prefix:日本",h.text);
            }
        });
        test("acknowledged metadata is consumed while outstanding delayed writes remain eligible",()->{
            EditorSelection s=new EditorSelection(0,0); s.expect(1,true); s.expect(2,true); s.expect(3,true);
            yes(!s.update(0,0,1,1,-1,-1)); eq(2,s.pendingMetadataCount());
            yes(!s.update(1,1,2,2,-1,-1)); eq(1,s.pendingMetadataCount());
            yes(!s.update(2,2,3,3,-1,-1)); eq(0,s.pendingMetadataCount());
            yes(s.update(3,3,1,1,-1,-1)); yes(!s.recoverRegion());
            s=new EditorSelection(0,0); s.expect(1,true); s.expect(2,true); s.expect(3,true);
            yes(!s.update(0,0,3,3,-1,-1)); eq(0,s.pendingMetadataCount()); yes(s.update(3,3,2,2,-1,-1));
        });
        test("unknown unacknowledged anchors stop before duplicating an accumulated reading",()->{
            Host h=new Host(1,0,"prefix:",true); h.omitRange=true; h.stripSpan=true;
            h.kana(4,1); h.kana(5,4); eq("prefix:に",h.text); eq("に",h.session.text()); yes(h.editor.compositionRejected());
            h.flush(); h.kana(5,4); h.flush(); eq("prefix:にほ",h.text);
        });
        test("cursor and selection after delayed stripped-span burst start fresh at actual location",()->{
            for (boolean selection:new boolean[]{false,true}) {
                Host h=new Host(1,0); h.omitRange=true; h.stripSpan=true;
                h.kana(4,1); h.kana(5,4); h.kana(10,2); h.flush();
                h.cursor=1; h.selectionEnd=selection ? 2 : 1; h.updates.add(new int[]{3,3,h.cursor,h.selectionEnd,-1,-1}); h.flush();
                eq("",h.session.text()); h.session.type("あ"); h.flush(); eq(selection ? "にあん" : "にあほん",h.text);
            }
        });
        test("current-coordinate callbacks retire queued writes even without old-coordinate progression",()->{
            EditorSelection s=new EditorSelection(0,0); s.expect(1,true); s.expect(2,true); s.expect(3,true);
            yes(!s.update(3,3,3,3,-1,-1)); eq(0,s.pendingMetadataCount()); yes(s.update(3,3,1,1,-1,-1));
        });
        test("coalesced preview cancel cannot hide a subsequent real cursor move",()->{
            Host h=new Host(1,0); h.omitRange=true;
            h.session.type("にほん"); h.flush();
            yes(h.session.preview("日本",h.session.revision())); yes(h.session.cancelPreview());
            h.updates.clear(); h.updates.add(new int[]{3,3,3,3,-1,-1}); h.flush();
            eq("にほん",h.text);
            h.cursor=h.selectionEnd=2; h.updates.add(new int[]{3,3,2,2,-1,-1}); h.flush();
            eq("",h.session.text()); eq("にほん",h.text);
            int regions=h.regions; h.session.type("あ"); h.flush();
            eq("にほん",h.text); eq("",h.session.text()); eq(regions,h.regions); yes(h.editor.compositionRejected());
            h.restart(); h.session.type("あ"); h.flush(); eq("にほあん",h.text); eq("あ",h.session.text());
        });
        test("ambiguous delayed reversal stops without reanchoring from older callbacks",()->{
            for (boolean strip:new boolean[]{false,true}) for (boolean region:new boolean[]{false,true}) {
                Host h=new Host(1,0,"prefix:",false); h.omitRange=true; h.stripSpan=strip; h.regionUpdates=region;
                h.session.type("にほん"); h.flush();
                yes(h.session.preview("日本",h.session.revision())); yes(h.session.cancelPreview());
                int[] first=h.updates.remove();
                h.editor.updateSelection(h.session,first[0],first[1],first[2],first[3],first[4],first[5]);
                if (!h.editor.compositionNeedsRestart() && !h.updates.isEmpty()) {
                    int[] second=h.updates.remove();
                    h.editor.updateSelection(h.session,second[0],second[1],second[2],second[3],second[4],second[5]);
                }
                yes(h.editor.compositionNeedsRestart()); eq("",h.session.text());
                int regions=h.regions; h.session.type("あ"); eq("prefix:にほん",h.text); eq(regions,h.regions);
                h.flush(); yes(h.editor.compositionNeedsRestart());
                h.session.type("あ"); eq("prefix:にほん",h.text); eq(regions,h.regions);
                h.restart(); h.session.type("あ"); h.flush(); eq("prefix:にほんあ",h.text);
            }
        });
        test("ambiguous shorter commit then next word preserves text and fences further conversion",()->{
            Host h=new Host(1,0); h.omitRange=true; h.session.type("にほん"); h.flush();
            yes(h.session.select("日本",h.session.revision())); h.session.type("ご"); h.flush();
            eq("日本ご",h.text); eq("",h.session.text()); yes(h.editor.compositionNeedsRestart());
            int regions=h.regions; h.session.type("あ"); eq("日本ご",h.text); eq(regions,h.regions);
            h.restart(); h.session.type("あ"); h.flush(); eq("日本ごあ",h.text);
        });
        test("fence survives private toggles unknown resets and positive aliases but allows explicit direct mode",()->{
            for (boolean omit:new boolean[]{false,true}) {
                Host h=new Host(1,EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING); h.omitRange=omit;
                h.session.type("にほん"); h.flush();
                yes(h.session.preview("日本",h.session.revision())); yes(h.session.cancelPreview());
                h.coalesce(true); h.cursor=h.selectionEnd=2;
                h.updates.add(new int[]{3,3,2,2,omit ? -1 : 0,omit ? -1 : 3}); h.flush();
                yes(h.editor.compositionNeedsRestart()); eq("",h.session.text());
                h.session.incognito(true); h.editor.selection(-1,-1); h.session.type("あ");
                eq("にほん",h.text); yes(h.editor.compositionNeedsRestart());
                h.session.mode(InputSession.Mode.ENGLISH); int regions=h.regions;
                h.session.type("a"); h.flush(); eq("にほaん",h.text); eq(regions,h.regions);
                yes(h.editor.compositionNeedsRestart()); yes(h.learned.isEmpty());
                h.session.mode(InputSession.Mode.KANA); h.session.type("あ"); eq("にほaん",h.text);
                h.restart(); h.session.type("あ"); h.flush(); eq("にほaあん",h.text);
            }
        });
        test("overflowed ordering cannot be revived as a region anchor",()->{
            EditorSelection s=new EditorSelection(0,0);
            for (int i=0;i<100;i++) s.expect(i+1,true);
            eq(64,s.pendingMetadataCount()); yes(!s.update(100,100,100,100,-1,-1)); eq(0,s.pendingMetadataCount());
            yes(s.update(0,0,1,1,-1,-1)); yes(s.anchorBlocked()); yes(!s.recoverRegion());
            s.select(-1,-1); yes(s.anchorBlocked()); s.select(1,1); yes(!s.anchorBlocked());
        });
        test("bounded operation sequences preserve host text and fence ambiguous cursor histories",()->{
            char[] alphabet={'T','P','C','F','M','N'}; int sequences=0;
            for (int length=0,power=1;length<=4;length++,power*=alphabet.length) {
                for (int code=0;code<power;code++) {
                    StringBuilder name=new StringBuilder(); int value=code;
                    for (int step=0;step<length;step++,value/=alphabet.length) name.append(alphabet[value%alphabet.length]);
                    for (int options=0;options<32;options++) {
                        try {
                            Host h=new Host(1,0,"prefix:",false);
                            h.omitRange=(options&1)!=0; h.stripSpan=(options&2)!=0;
                            h.regionUpdates=(options&4)!=0; h.batchSupported=(options&8)!=0;
                            h.session.type("にほん"); h.flush();
                            for (int step=0;step<length;step++) sequenceStep(h,name.charAt(step),step);
                            sequenceCursor(h,code%3,(options&16)!=0); sequences++;
                        } catch (AssertionError failure) {
                            throw new AssertionError("Sequence="+name+" options="+options+" length="+length,failure);
                        }
                    }
                }
            }
            eq(49760,sequences);
            System.out.println("PASS: 49760 bounded sequences (depth 0-4; type/preview/cancel/ordered/coalesced/no-motion callbacks; range/span/region/batch/drain options; cursor and fresh-adapter recovery)");
        });
        System.out.println("PASS: "+count+" composition-callback host groups; Android runtime reproduction NOT RUN");
    }
}
