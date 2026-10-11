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

/** Actual Android API signatures with injected key sender; not a native Android event loop. */
public final class ConnectionEditorHostTests {
    private static int count;
    private interface Test { void run(); }
    private static void test(String name,Test body) { body.run();count++;System.out.println("PASS: "+name); }
    private static void eq(Object e,Object a) { if(!e.equals(a)) throw new AssertionError("Expected "+e+"; got "+a); }
    private static void yes(boolean b) { if(!b) throw new AssertionError("Expected true"); }
    private static final class Host {
        String text=""; int start,end,spanStart=-1,spanEnd=-1,lastAction=-1;
        boolean accept=true,finishSupported=true,actionSupported=true;
        final List<String> calls=new ArrayList<>(); final List<Integer> keys=new ArrayList<>(),flags=new ArrayList<>();
        final InputConnection connection=(InputConnection)Proxy.newProxyInstance(InputConnection.class.getClassLoader(),
            new Class<?>[]{InputConnection.class},(p,m,a)->{
                String name=m.getName();calls.add(name);
                if(name.startsWith("get") || name.startsWith("setComposing") || name.equals("setSelection"))
                    throw new AssertionError("Host text/span access forbidden: "+name);
                if(name.equals("finishComposingText")) { if(finishSupported) spanStart=spanEnd=-1;return finishSupported; }
                if(name.equals("commitText")) {
                    eq(1,a[1]); yes(spanStart<0);
                    if(accept) { int first=Math.min(start,end);text=text.substring(0,first)+a[0]+text.substring(Math.max(start,end));start=end=first+a[0].toString().length(); }
                    return accept;
                }
                if(name.equals("performEditorAction")) { lastAction=(Integer)a[0];return actionSupported; }
                if(name.equals("sendKeyEvent")) throw new AssertionError("Native KeyEvent runtime NOT RUN");
                return m.getReturnType()==boolean.class ? false : null;
            });
        EditorInfo info(int options) { EditorInfo i=new EditorInfo();i.inputType=1;i.imeOptions=options;i.initialSelStart=start;i.initialSelEnd=end;return i; }
        ConnectionEditor editor(int options) { return new ConnectionEditor(connection,info(options),(code,f)->{
            keys.add(code);flags.add(f);
            int first=Math.min(start,end),last=Math.max(start,end);
            if(code==KeyEvent.KEYCODE_DEL) {
                if(first==last && first>0) first=text.offsetByCodePoints(first,-1);
                text=text.substring(0,first)+text.substring(last);start=end=first;
            } else if(code==KeyEvent.KEYCODE_DPAD_LEFT) start=end=Math.max(0,first-1);
            else start=end=Math.min(text.length(),last+1);
        }); }
    }
    public static void main(String[] args) {
        test("native delete key follows actual Unicode caret without host reads",()->{
            Host h=new Host();h.text="あ😀";h.start=h.end=h.text.length();ConnectionEditor e=h.editor(0);
            e.delete();eq("あ",h.text);e.delete();eq("",h.text);eq(2,h.keys.size());eq(KeyEvent.KEYCODE_DEL,h.keys.get(0));
        });
        test("native delete key acts on actual selected range",()->{
            Host h=new Host();h.text="a😀b";h.start=1;h.end=3;h.editor(0).delete();eq("ab",h.text);eq(1,h.start);
        });
        test("reversed selection and stale callbacks never choose a guessed delete range",()->{
            Host h=new Host();h.text="abcd";h.start=3;h.end=1;ConnectionEditor e=h.editor(0);
            e.updateSelection(4,4,4,4,0,4);e.delete();eq("ad",h.text);eq(1,h.start);
        });
        test("explicit commit first ends host-owned span without replacing its text",()->{
            Host h=new Host();h.text="old";h.start=h.end=3;h.spanStart=0;h.spanEnd=3;
            yes(h.editor(0).commit("日本"));eq("old日本",h.text);eq(java.util.Arrays.asList("finishComposingText","commitText"),h.calls);
        });
        test("rejected commit reports failure then permits explicit retry",()->{
            Host h=new Host();ConnectionEditor e=h.editor(0);h.accept=false;yes(!e.commit("日本"));yes(e.commitRejected());
            eq("",h.text);h.accept=true;yes(e.commit("日本"));eq("日本",h.text);yes(!e.commitRejected());
        });
        test("unsupported finish rejects replacement rather than reclaiming host span",()->{
            Host h=new Host();h.text="old";h.spanStart=0;h.spanEnd=3;h.start=h.end=3;h.finishSupported=false;
            ConnectionEditor e=h.editor(0);yes(!e.commit("日本"));eq("old",h.text);eq(1,h.calls.size());
            h.finishSupported=true;yes(e.commit("日本"));eq("old日本",h.text);
        });
        test("editor actions honor standard ID and no-enter flag",()->{
            Host h=new Host();h.editor(EditorInfo.IME_ACTION_SEARCH).enter();eq(EditorInfo.IME_ACTION_SEARCH,h.lastAction);eq("",h.text);
            h=new Host();h.editor(EditorInfo.IME_ACTION_DONE|EditorInfo.IME_FLAG_NO_ENTER_ACTION).enter();eq("\n",h.text);
        });
        test("rejected action falls back to explicit newline and labelled custom action works",()->{
            Host h=new Host();h.actionSupported=false;h.editor(EditorInfo.IME_ACTION_SEND).enter();eq("\n",h.text);
            h=new Host();EditorInfo i=h.info(0);i.actionId=42;i.actionLabel="保存";
            new ConnectionEditor(h.connection,i,(c,f)->{}).enter();eq(42,h.lastAction);
        });
        test("cursor keys use actual host caret and no surrounding reads",()->{
            Host h=new Host();h.text="abc";h.start=h.end=2;ConnectionEditor e=h.editor(0);e.cursor(-1);eq(1,h.start);e.cursor(1);eq(2,h.start);
        });
        test("null connection rejects confirmation without blocking local draft editing",()->{
            ConnectionEditor e=new ConnectionEditor(null,new EditorInfo(),(c,f)->{throw new AssertionError();});
            InputSession s=new InputSession();s.start(e,FieldPolicy.from(1,0));yes(s.type("に"));yes(!s.confirm());yes(s.type("ほ"));eq("にほ",s.text());
            e.delete();e.cursor(1);e.enter();e.finish();
        });
        test("reading and preview make zero host calls until explicit confirmation",()->{
            Host h=new Host();InputSession s=new InputSession();s.start(h.editor(0),FieldPolicy.from(1,0));
            yes(s.type("にほん"));yes(s.preview("日本",s.revision()));eq("",h.text);eq(0,h.calls.size());
            yes(s.confirm());eq("日本",h.text);yes(s.type("ご"));eq("日本",h.text);eq("ご",s.text());yes(s.confirm());eq("日本ご",h.text);
        });
        test("no-enter suppresses custom execution",()->{
            Host h=new Host();EditorInfo i=h.info(EditorInfo.IME_FLAG_NO_ENTER_ACTION);i.actionId=42;i.actionLabel="保存";
            new ConnectionEditor(h.connection,i,(c,f)->{}).enter();eq(-1,h.lastAction);eq("\n",h.text);
        });
        test("unlabelled custom IDs do not silently execute",()->{
            for(String caption:new String[]{null,""," \t\n"}) {
                Host h=new Host();EditorInfo i=h.info(0);i.actionId=42;i.actionLabel=caption;
                new ConnectionEditor(h.connection,i,(c,f)->{}).enter();eq(-1,h.lastAction);eq("\n",h.text);
            }
        });
        test("native delete and cursor keys carry software keyboard flags",()->{
            Host h=new Host();ConnectionEditor e=h.editor(0);e.delete();e.cursor(-1);e.cursor(1);eq(3,h.flags.size());
            for(int f:h.flags) eq(KeyEvent.FLAG_SOFT_KEYBOARD|KeyEvent.FLAG_KEEP_TOUCH_MODE,f);
        });
        System.out.println("PASS: "+count+" Android API adapter host groups; Android native event loop NOT RUN");
    }
}
