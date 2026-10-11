/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import dev.local.koto.core.FieldPolicy;
import dev.local.koto.core.InputSession;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/** Actual Android API classes with a synthetic editor proxy; not device runtime QA. */
public final class ConnectionEditorHostTests {
    private static int count;
    private interface Test { void run() throws Exception; }
    private static void test(String name, Test body) throws Exception {
        body.run(); count++; System.out.println("PASS: " + name);
    }
    private static void eq(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
    private static void yes(boolean value) { if (!value) throw new AssertionError("Expected true"); }
    private static final class Host {
        String text = "", composition = "";
        int start, end, lastAction = -1;
        boolean accept = true, deleteSupported = true, actionSupported = true, legacyDelete;
        final List<String> calls = new ArrayList<>();
        final List<Integer> keys = new ArrayList<>();
        final List<Integer> keyFlags = new ArrayList<>();
        final InputConnection connection = (InputConnection) Proxy.newProxyInstance(
                InputConnection.class.getClassLoader(), new Class<?>[]{InputConnection.class}, (proxy, method, args) -> {
                    String name = method.getName(); calls.add(name);
                    if (name.startsWith("get")) throw new AssertionError("Host text read forbidden: " + name);
                    if (name.equals("setComposingText")) {
                        if (accept) composition = args[0].toString(); return accept;
                    }
                    if (name.equals("commitText")) {
                        if (accept) {
                            text = text.substring(0,Math.min(start,end)) + args[0] + text.substring(Math.max(start,end));
                            start = Math.min(start,end) + args[0].toString().length(); end = start; composition = "";
                        }
                        return accept;
                    }
                    if (name.equals("deleteSurroundingTextInCodePoints")) {
                        eq(1,args[0]); eq(0,args[1]);
                        if (legacyDelete) throw new AbstractMethodError("Synthetic legacy editor");
                        if (accept && deleteSupported && start==end && start>0) {
                            int before = text.offsetByCodePoints(start,-1);
                            text = text.substring(0,before) + text.substring(end); start = end = before;
                        }
                        return accept && deleteSupported;
                    }
                    if (name.equals("finishComposingText")) { composition=""; return true; }
                    if (name.equals("performEditorAction")) { lastAction=(Integer)args[0]; return accept && actionSupported; }
                    if (name.equals("sendKeyEvent")) throw new AssertionError("Native KeyEvent creation is not a host runtime test");
                    return method.getReturnType()==boolean.class ? false : null;
                });
        EditorInfo info(int options) {
            EditorInfo info = new EditorInfo(); info.inputType=1; info.imeOptions=options;
            info.initialSelStart=start; info.initialSelEnd=end; return info;
        }
        ConnectionEditor.KeySender sender() { return (code,flags)->{ keys.add(code); keyFlags.add(flags); }; }
        ConnectionEditor editor(int options) { return new ConnectionEditor(connection,info(options),sender()); }
    }
    public static void main(String[] args) throws Exception {
        test("committed emoji deletion uses code points without reading host text", () -> {
            Host h=new Host(); h.text="あ😀"; h.start=h.end=h.text.length(); ConnectionEditor e=h.editor(0);
            e.delete(); eq("あ",h.text); eq(0,h.keys.size()); eq("deleteSurroundingTextInCodePoints",h.calls.get(0));
            e.delete(); eq("",h.text); e.delete(); eq("",h.text);
        });
        test("selected text deletion replaces only the supplied selection", () -> {
            Host h=new Host(); h.text="a😀b"; h.start=1; h.end=3; ConnectionEditor e=h.editor(0);
            e.delete(); eq("ab",h.text); eq(1,h.start); eq(0,h.keys.size());
            e.delete(); eq("b",h.text);
        });
        test("selection callbacks handle reversed ranges and collapsed selections", () -> {
            Host h=new Host(); h.text="abcd"; h.start=h.end=4; ConnectionEditor e=h.editor(0);
            h.start=3; h.end=1; e.selection(3,1); e.delete(); eq("ad",h.text);
            h.start=h.end=2; e.selection(2,2); e.delete(); eq("a",h.text);
        });
        test("unsupported and legacy delete APIs route to one delete key", () -> {
            Host h=new Host(); h.deleteSupported=false; ConnectionEditor e=h.editor(0);
            e.delete(); eq(1,h.keys.size()); eq(KeyEvent.KEYCODE_DEL,h.keys.get(0));
            h.legacyDelete=true; e.delete(); eq(2,h.keys.size());
        });
        test("rejected selection replacement uses the legacy delete fallback", () -> {
            Host h=new Host(); h.text="abc"; h.start=0; h.end=3; h.accept=false; ConnectionEditor e=h.editor(0);
            e.delete(); eq("abc",h.text); eq(1,h.keys.size());
            eq("commitText",h.calls.get(0));
        });
        test("successful text writes consume selection metadata; failed writes retain it", () -> {
            Host h=new Host(); h.text="abc"; h.start=0; h.end=3; ConnectionEditor e=h.editor(0);
            h.accept=false; yes(!e.commit("x")); h.accept=true; e.delete(); eq("",h.text);
            h.text="abc"; h.start=0; h.end=3; e.selection(0,3); yes(e.commit("x")); e.delete(); eq("",h.text);
        });
        test("editor action honors action type and no-enter flag", () -> {
            Host h=new Host(); ConnectionEditor e=h.editor(EditorInfo.IME_ACTION_SEARCH); e.enter();
            eq("performEditorAction",h.calls.get(0)); eq(EditorInfo.IME_ACTION_SEARCH,h.lastAction); eq("",h.text);
            h=new Host(); e=h.editor(EditorInfo.IME_ACTION_DONE|EditorInfo.IME_FLAG_NO_ENTER_ACTION); e.enter();
            eq("\n",h.text); eq("commitText",h.calls.get(0));
        });
        test("unsupported editor action falls back to newline; custom action ID is used", () -> {
            Host h=new Host(); h.actionSupported=false; h.editor(EditorInfo.IME_ACTION_SEND).enter(); eq("\n",h.text);
            h=new Host(); EditorInfo info=h.info(EditorInfo.IME_ACTION_GO); info.actionLabel="Synthetic action"; info.actionId=42;
            ConnectionEditor custom = new ConnectionEditor(h.connection,info,h.sender());
            yes(custom.action().label().startsWith("実行\n")); custom.enter(); eq("performEditorAction",h.calls.get(0)); eq(42,h.lastAction);
        });
        test("cursor port uses left and right keys without host text reads", () -> {
            Host h=new Host(); ConnectionEditor e=h.editor(0); e.cursor(-1); e.cursor(1);
            eq(KeyEvent.KEYCODE_DPAD_LEFT,h.keys.get(0)); eq(KeyEvent.KEYCODE_DPAD_RIGHT,h.keys.get(1)); eq(0,h.calls.size());
        });
        test("null connection safely rejects writes and all actions", () -> {
            EditorInfo info=new EditorInfo(); ConnectionEditor e=new ConnectionEditor(null,info,(code,flags)->{ throw new AssertionError("No connection"); });
            yes(!e.composing("かな")); yes(!e.commit("x")); e.delete(); e.cursor(1); e.enter(); e.finish();
        });
        test("core preview commits through actual InputConnection API signatures", () -> {
            Host h=new Host(); ConnectionEditor e=h.editor(0); InputSession s=new InputSession();
            s.start(e,FieldPolicy.from(1,0)); s.type("にほん"); yes(s.preview("日本",s.revision()));
            s.type("ご"); eq("日本",h.text); eq("ご",h.composition); s.enter(); eq("日本ご",h.text);
        });
        test("custom action is labelled and NO_ENTER_ACTION suppresses custom execution", () -> {
            Host h=new Host(); EditorInfo info=h.info(EditorInfo.IME_ACTION_UNSPECIFIED);
            info.actionId=42; info.actionLabel="保存";
            ConnectionEditor e=new ConnectionEditor(h.connection,info,h.sender());
            eq("実行\n保存",e.action().label()); e.enter(); eq(42,h.lastAction);
            h=new Host(); info=h.info(EditorInfo.IME_FLAG_NO_ENTER_ACTION); info.actionId=42; info.actionLabel="保存";
            e=new ConnectionEditor(h.connection,info,h.sender()); eq("↵",e.action().label()); e.enter(); eq(-1,h.lastAction); eq("\n",h.text);
        });
        test("unlabelled custom action cannot silently execute as return", () -> {
            for(String label:new String[]{null,""," \t\n"}) {
                Host h=new Host(); EditorInfo info=h.info(EditorInfo.IME_ACTION_UNSPECIFIED); info.actionId=42; info.actionLabel=label;
                ConnectionEditor e=new ConnectionEditor(h.connection,info,h.sender());
                eq("↵",e.action().label()); e.enter(); eq(-1,h.lastAction); eq("\n",h.text);
            }
        });
        test("delete and cursor fallbacks mark events as software keyboard input", () -> {
            Host h=new Host(); h.deleteSupported=false; ConnectionEditor e=h.editor(0);
            e.delete(); e.cursor(-1); e.cursor(1); eq(3,h.keyFlags.size());
            for(int flags:h.keyFlags) eq(KeyEvent.FLAG_SOFT_KEYBOARD|KeyEvent.FLAG_KEEP_TOUCH_MODE,flags);
        });
        System.out.println("PASS: "+count+" host Android API adapter groups; Android event-loop/device runtime NOT RUN");
    }
}
