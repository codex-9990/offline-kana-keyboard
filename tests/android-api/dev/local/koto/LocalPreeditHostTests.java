/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import dev.local.koto.core.FieldPolicy;
import dev.local.koto.core.FlickLayout;
import dev.local.koto.core.InputSession;
import dev.local.koto.core.KeyGesture;
import dev.local.koto.core.SkkDictionary;
import java.io.FileInputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Physical synthetic editor plus independent local-preedit oracle. Never uses actual user text. */
public final class LocalPreeditHostTests {
    private static int count;
    private interface Test { void run(); }
    private static void test(String name,Test body) { body.run();count++;System.out.println("PASS: "+name); }
    private static void eq(Object e,Object a) { if(!e.equals(a)) throw new AssertionError("Expected "+e+"; got "+a); }
    private static void yes(boolean b) { if(!b) throw new AssertionError("Expected true"); }
    private static final class Host {
        String text; int cursor,end,spanStart=-1,spanEnd=-1,commits,finishes,keys,reported;
        boolean accept=true,finishSupported=true,omitRange;
        final ArrayDeque<int[]> updates=new ArrayDeque<>();
        final List<String> payloads=new ArrayList<>(),learned=new ArrayList<>();
        final InputSession session=new InputSession(); final ConnectionEditor editor;
        final InputConnection connection=(InputConnection)Proxy.newProxyInstance(InputConnection.class.getClassLoader(),
            new Class<?>[]{InputConnection.class},(p,m,a)->{
                String name=m.getName();
                if(name.startsWith("get") || name.startsWith("setComposing") || name.equals("setSelection"))
                    throw new AssertionError("Surrounding reads/span ownership forbidden: "+name);
                if(name.equals("finishComposingText")) {
                    finishes++;if(!finishSupported) return false;
                    spanStart=spanEnd=-1;updates.add(new int[]{cursor,end,cursor,end,-1,-1});return true;
                }
                if(name.equals("commitText")) {
                    eq(1,a[1]);yes(spanStart<0);if(!accept)return false;
                    String value=a[0].toString();int old=cursor,oldEnd=end,first=Math.min(cursor,end),last=Math.max(cursor,end);
                    text=text.substring(0,first)+value+text.substring(last);cursor=end=first+value.length();
                    commits++;payloads.add(value);updates.add(new int[]{old,oldEnd,cursor,end,-1,-1});return true;
                }
                if(name.equals("performEditorAction")) return true;
                if(name.equals("sendKeyEvent")) throw new AssertionError("Native key dispatch NOT RUN");
                return m.getReturnType()==boolean.class ? false : null;
            });
        Host(int type,int flags,String initial,boolean unknown) {
            text=initial;cursor=end=reported=initial.length();
            EditorInfo i=new EditorInfo();i.inputType=type;i.imeOptions=flags;
            i.initialSelStart=i.initialSelEnd=unknown ? -1 : cursor;
            editor=new ConnectionEditor(connection,i,(code,keyFlags)->{
                eq(KeyEvent.FLAG_SOFT_KEYBOARD|KeyEvent.FLAG_KEEP_TOUCH_MODE,keyFlags);keys++;
                int old=cursor,oldEnd=end,first=Math.min(cursor,end),last=Math.max(cursor,end);
                if(code==KeyEvent.KEYCODE_DEL) {
                    if(first==last && first>0)first=text.offsetByCodePoints(first,-1);
                    text=text.substring(0,first)+text.substring(last);cursor=end=first;
                } else if(code==KeyEvent.KEYCODE_DPAD_LEFT)cursor=end=Math.max(0,first-1);
                else cursor=end=Math.min(text.length(),last+1);
                updates.add(new int[]{old,oldEnd,cursor,end,omitRange ? -1 : spanStart,omitRange ? -1 : spanEnd});
            });
            session.start(editor,FieldPolicy.from(type,flags));session.choices((r,w,policy)->learned.add(r+":"+w));
        }
        void flush() {
            int guard=0;while(!updates.isEmpty()) {
                yes(++guard<10000);int[] u=updates.remove();reported=u[2];
                editor.updateSelection(u[0],u[1],u[2],u[3],u[4],u[5]);
            }
        }
        void coalesce(boolean noMotion) {
            int old=noMotion ? cursor : reported;updates.clear();
            updates.add(new int[]{old,old,cursor,end,omitRange ? -1 : spanStart,omitRange ? -1 : spanEnd});flush();
        }
        void move(int first,int last) {
            int old=cursor,oldEnd=end;cursor=first;end=last;
            updates.add(new int[]{old,oldEnd,cursor,end,omitRange ? -1 : spanStart,omitRange ? -1 : spanEnd});
        }
        void kana(int key,int direction) {
            KeyGesture gesture=new KeyGesture(true,false,22);yes(!gesture.begin(50,50));
            float x=50,y=50;if(direction==1)x-=30;if(direction==2)y-=30;if(direction==3)x+=30;if(direction==4)y+=30;
            int d=gesture.end(x,y,true);yes(session.type(FlickLayout.kana(key,d)));eq(-1,gesture.end(x,y,true));
        }
    }
    private static Host normal() { return new Host(1,0,"",false); }
    private static String insert(String text,int first,int last,String value) {
        int a=Math.min(first,last),b=Math.max(first,last);return text.substring(0,a)+value+text.substring(b);
    }
    public static void main(String[] args) throws Exception {
        SkkDictionary dictionary=SkkDictionary.load(new FileInputStream(args[0]));
        test("three flicks are accepted once locally and 日本 commits only on explicit choice",()->{
            Host h=normal();h.kana(4,1);h.kana(5,4);h.kana(10,2);
            eq("にほん",h.session.text());eq("",h.text);eq(0,h.finishes);yes(dictionary.candidates(h.session.text()).contains("日本"));
            yes(h.session.select("日本",h.session.revision()));eq("日本",h.text);eq(1,h.commits);h.flush();
        });
        test("exact shorter conversion then ご before callbacks never loses or blocks kana",()->{
            for(boolean omit:new boolean[]{false,true}) {
                Host h=normal();h.omitRange=omit;yes(h.session.type("にほん"));yes(h.session.select("日本",h.session.revision()));
                yes(h.session.type("ご"));h.flush();eq("ご",h.session.text());eq("日本",h.text);
                yes(h.session.confirm());eq("日本ご",h.text);eq(2,h.commits);yes(h.session.type("あ"));h.coalesce(true);
                eq("あ",h.session.text());yes(h.session.confirm());eq("日本ごあ",h.text);
            }
        });
        test("continuous one thousand conversions with delayed or coalesced callbacks account for every key",()->{
            Host h=normal();StringBuilder expected=new StringBuilder();int accepted=0;
            for(int i=0;i<1000;i++) {
                for(String key:new String[]{"に","ほ","ん"}) {yes(h.session.type(key));accepted++;}
                yes(dictionary.candidates(h.session.text()).contains("日本"));yes(h.session.preview("日本",h.session.revision()));
                yes(h.session.confirm());expected.append("日本");yes(h.session.type("ご"));accepted++;
                if(i%3==0)h.flush();else if(i%3==1)h.coalesce(true);
                eq("ご",h.session.text());yes(h.session.confirm());expected.append("ご");
            }
            h.flush();eq(4000,accepted);eq(expected.toString(),h.text);eq(2000,h.commits);eq("",h.session.text());
        });
        test("coalesced preview cancel and old 3 to 2 callbacks do not change the local draft",()->{
            Host h=normal();yes(h.session.type("にほん"));yes(h.session.preview("日本",h.session.revision()));yes(h.session.cancelPreview());
            h.editor.updateSelection(3,3,3,3,-1,-1);h.editor.updateSelection(3,3,2,2,-1,-1);
            eq("にほん",h.session.text());eq("",h.text);yes(h.session.type("ご"));eq("にほんご",h.session.text());
            yes(h.session.confirm());eq("にほんご",h.text);
        });
        test("missing stripped positive conflicting and unknown bounds cannot duplicate or discard reading",()->{
            Host h=new Host(1,0,"prefix:",true);yes(h.session.type("に"));
            for(int[] u:new int[][]{{0,0,1,1,-1,-1},{1,1,2,2,0,1},{2,2,0,0,8,9},{-1,-1,-1,-1,-1,-1}})
                h.editor.updateSelection(u[0],u[1],u[2],u[3],u[4],u[5]);
            yes(h.session.type("ほ"));yes(h.session.type("ん"));eq("にほん",h.session.text());eq("prefix:",h.text);
            yes(h.session.select("日本",h.session.revision()));eq("prefix:日本",h.text);
        });
        test("host-owned composing span is left untouched until finish then explicit insertion",()->{
            Host h=new Host(1,0,"old",false);h.spanStart=0;h.spanEnd=3;
            yes(h.session.type("にほん"));yes(h.session.preview("日本",h.session.revision()));eq("old",h.text);eq(0,h.finishes);
            yes(h.session.confirm());eq("old日本",h.text);eq(-1,h.spanStart);
        });
        test("local preview cancel deletion and modifier never send or edit host text",()->{
            Host h=normal();h.session.type("にほん");h.session.preview("日本",h.session.revision());h.session.backspace();
            eq("にほん",h.session.text());eq("",h.session.preview());h.session.backspace();eq("にほ",h.session.text());
            h.session.modify();eq("にぼ",h.session.text());eq("",h.text);eq(0,h.commits);eq(0,h.keys);eq(0,h.finishes);
            h.session.discard();eq("",h.session.text());eq("",h.text);
        });
        test("typing after preview cancels only local preview and includes each appended key",()->{
            Host h=normal();h.session.type("にほん");h.session.preview("日本",h.session.revision());yes(h.session.type("ご"));
            eq("にほんご",h.session.text());eq("",h.session.preview());eq("",h.text);yes(h.session.confirm());eq("にほんご",h.text);
        });
        test("actual cursor or selected range receives confirmation without restoring a prior span",()->{
            for(boolean selection:new boolean[]{false,true}) {
                Host h=new Host(1,0,"にほん",false);h.session.type("あ");h.move(1,selection ? 2 : 1);h.flush();
                eq("あ",h.session.text());yes(h.session.confirm());eq(selection ? "にあん" : "にあほん",h.text);
            }
        });
        test("keyboard cursor keys preserve the draft and confirm at actual new caret",()->{
            Host h=new Host(1,0,"abcd",false);h.session.type("かな");h.session.cursor(-1);h.flush();
            eq("かな",h.session.text());eq("abcd",h.text);yes(h.session.confirm());eq("abcかなd",h.text);
        });
        test("layout changes and spaces keep an unsent mixed draft until confirmed",()->{
            Host h=normal();h.session.type("か");h.session.mode(InputSession.Mode.ENGLISH);h.session.shift();yes(h.session.type("a"));
            h.session.space();h.session.mode(InputSession.Mode.SYMBOLS);yes(h.session.type("$"));eq("かA $",h.session.text());eq("",h.text);
            yes(h.session.confirm());eq("かA $",h.text);
        });
        test("failed confirmation retains every accepted key and local edits work before explicit retry",()->{
            Host h=normal();h.session.type("にほん");h.accept=false;yes(!h.session.confirm());eq("にほん",h.session.text());
            yes(h.session.type("ご"));h.session.backspace();yes(h.session.preview("日本",h.session.revision()));
            h.accept=true;yes(h.session.confirm());eq("日本",h.text);eq(1,h.commits);
        });
        test("unsupported finish rejects commit without blocking local typing or cancellation",()->{
            Host h=new Host(1,0,"old",false);h.spanStart=0;h.spanEnd=3;h.finishSupported=false;
            yes(h.session.type("に"));yes(!h.session.confirm());yes(h.session.type("ほ"));eq("にほ",h.session.text());eq("old",h.text);
            h.session.discard();yes(h.session.type("新"));h.finishSupported=true;yes(h.session.confirm());eq("old新",h.text);
        });
        test("explicit local cancellation sends nothing and immediately permits more input",()->{
            Host h=normal();h.session.type("にほん");h.session.preview("日本",h.session.revision());h.session.discard();
            eq("",h.text);yes(h.session.type("あ"));yes(h.session.confirm());eq("あ",h.text);
        });
        test("focus change cancels old local text and late callbacks cannot transfer it",()->{
            Host a=normal(),b=normal();a.session.type("にほん");a.session.preview("日本",a.session.revision());
            a.session.start(b.editor,FieldPolicy.from(1,0));b.editor.updateSelection(0,0,3,3,-1,-1);
            eq("",a.session.text());eq("",a.text);yes(a.session.type("新"));yes(a.session.confirm());eq("新",b.text);
        });
        test("relayout within the same session preserves draft while hide or fresh restart cancels it",()->{
            Host h=normal();h.session.type("にほん");h.session.preview("日本",h.session.revision());long old=h.session.revision();h.session.relayout();
            eq("にほん",h.session.text());eq("日本",h.session.preview());yes(!h.session.select("wrong",old));eq("",h.text);
            h.session.discard();h.session.start(h.editor,FieldPolicy.from(1,0));eq("",h.session.text());eq("",h.text);
            yes(h.session.type("新"));yes(h.session.confirm());eq("新",h.text);
        });
        test("password nontext and unknown fields have no local draft preview conversion or learning",()->{
            for(int type:new int[]{0x81,0x91,0xe1,0x12,2,3,4,0,5,15,0xf1,0xff1}) {
                Host h=new Host(type,0,"",false);h.session.mode(InputSession.Mode.KANA);
                yes(h.session.type("に"));yes(h.session.type("ほ"));yes(h.session.type("ん"));h.flush();
                eq("にほん",h.text);eq("",h.session.text());eq("",h.session.preview());yes(!h.session.hasCandidates());yes(h.learned.isEmpty());eq(3,h.commits);
                yes(!h.session.requestConversion());yes(!h.session.preedit(true).visible);
            }
        });
        test("NO_SUGGESTIONS body and web body convert on first explicit request without personalized learning",()->{
            for(int type:new int[]{0x80001,0x800a1}) for(int flags:new int[]{0,EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING}) {
                Host h=new Host(type,flags,"prefix:",false);h.kana(4,1);h.kana(5,4);h.kana(10,2);
                eq("にほん",h.session.text());eq("prefix:",h.text);yes(!h.session.hasCandidates());
                yes(!h.session.preview("日本",h.session.revision()));yes(!h.session.select("日本",h.session.revision()));
                yes(h.session.requestConversion());yes(h.session.hasCandidates());
                yes(dictionary.candidates(h.session.text()).contains("日本"));yes(h.session.preview("日本",h.session.revision()));
                eq("prefix:",h.text);yes(h.session.confirm());eq("prefix:日本",h.text);h.coalesce(true);
                yes(h.session.type("ご"));yes(!h.session.hasCandidates());yes(h.session.confirm());eq("prefix:日本ご",h.text);
                eq(2,h.commits);yes(h.learned.isEmpty());
            }
        });
        test("ordinary web notepad URL and FORCE_ASCII combinations keep field-dependent policies",()->{
            for(int type:new int[]{1,0xa1,0x11,0x21,0xd1}) for(int noSuggestions:new int[]{0,0x80000})
                for(int flags:new int[]{0,0x1000000,0x80000000,0x81000000}) {
                    Host h=new Host(type|noSuggestions,flags,"",false);
                    boolean ascii=type==0x11 || type==0x21 || type==0xd1 || (flags&0x80000000)!=0;
                    eq(ascii ? InputSession.Mode.ENGLISH : InputSession.Mode.KANA,h.session.mode());
                    h.session.mode(InputSession.Mode.ENGLISH);yes(h.session.type("https://x.example/a?b=1"));yes(h.session.confirm());
                    h.session.mode(InputSession.Mode.KANA);yes(h.session.type("にほん"));
                    eq(!(ascii || noSuggestions!=0),h.session.hasCandidates());yes(h.session.requestConversion());
                    yes(h.session.select("日本",h.session.revision()));eq("https://x.example/a?b=1日本",h.text);
                    if(ascii || noSuggestions!=0 || (flags&0x1000000)!=0)yes(h.learned.isEmpty());
                }
        });
        test("manual field rejected confirmation no span support and capacity never lock out local editing",()->{
            Host h=new Host(0x80011,0x81000000,"old",false);h.session.mode(InputSession.Mode.KANA);h.session.type("にほん");
            h.session.requestConversion();h.session.preview("日本",h.session.revision());h.accept=false;yes(!h.session.confirm());
            eq("日本",h.session.preview());yes(h.session.type("ご"));eq("にほんご",h.session.text());yes(!h.session.hasCandidates());
            h.session.backspace();h.session.requestConversion();h.accept=true;h.finishSupported=false;yes(!h.session.confirm());
            yes(h.session.cancelPreview());h.finishSupported=true;yes(h.session.confirm());eq("oldにほん",h.text);
            for(int i=0;i<256;i++)yes(h.session.type("あ"));yes(!h.session.type("い"));h.session.backspace();yes(h.session.type("い"));
            yes(h.session.confirm());yes(h.session.type("う"));yes(h.session.confirm());eq(3,h.commits);yes(h.learned.isEmpty());
        });
        test("explicit request lifecycle clears on focus hide restart and preserves same-session relayout only",()->{
            Host h=new Host(0x80001,0,"",false);h.session.type("にほん");h.session.requestConversion();h.session.preview("日本",h.session.revision());
            long old=h.session.revision();h.session.relayout();yes(h.session.hasCandidates());eq("日本",h.session.preview());
            yes(!h.session.select("wrong",old));h.session.discard();yes(!h.session.conversionRequested());eq("",h.text);
            h.session.type("にほん");h.session.requestConversion();old=h.session.revision();
            Host next=new Host(0x800a1,0,"",false);h.session.start(next.editor,FieldPolicy.from(0x800a1,0));h.session.type("にほん");
            h.editor.updateSelection(0,0,2,2,-1,-1);yes(!h.session.hasCandidates());yes(!h.session.select("日本",old));eq("",next.text);
            yes(h.session.requestConversion());yes(h.session.select("日本",h.session.revision()));eq("日本",next.text);
            h.session.finish();yes(!h.session.conversionRequested());yes(!h.session.type("ignored"));
        });
        test("two thousand manual conversion cycles account for all accepted keys and confirmations with no history",()->{
            int accepted=0,confirmations=0;
            for(int flags:new int[]{0,0x1000000}) {
                Host h=new Host(0x800a1,flags,"",false);StringBuilder expected=new StringBuilder();
                for(int i=0;i<1000;i++) {
                    for(String key:new String[]{"に","ほ","ん"}){yes(h.session.type(key));accepted++;}
                    yes(!h.session.hasCandidates());yes(h.session.requestConversion());yes(h.session.preview("日本",h.session.revision()));
                    yes(h.session.confirm());confirmations++;expected.append("日本");yes(h.session.type("ご"));accepted++;
                    if(i%3==0)h.flush();else if(i%3==1)h.coalesce(true);
                    eq("ご",h.session.text());yes(!h.session.hasCandidates());yes(h.session.confirm());confirmations++;expected.append("ご");
                }
                h.flush();eq(expected.toString(),h.text);eq(2000,h.commits);yes(h.learned.isEmpty());
            }
            eq(8000,accepted);eq(4000,confirmations);
            System.out.println("PASS: 2000 explicit-only cycles; 8000 accepted keys; 4000 exact confirmations; no learned records");
        });
        test("no personalized learning and manual private mode retain continuous static conversion without records",()->{
            for(boolean manual:new boolean[]{false,true}) {
                Host h=new Host(1,manual ? 0 : EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING,"",false);if(manual)h.session.incognito(true);
                for(int i=0;i<25;i++) {
                    yes(h.session.type("にほん"));yes(dictionary.candidates(h.session.text()).contains("日本"));yes(h.session.select("日本",h.session.revision()));
                    yes(h.session.type("ご"));h.coalesce(true);yes(h.session.confirm());
                }
                eq(50,h.commits);yes(h.learned.isEmpty());
            }
        });
        test("private preference change cancels local learned preview even when native commit rejects",()->{
            Host h=normal();h.session.type("にほん");h.session.preview("日本",h.session.revision());long old=h.session.revision();h.accept=false;
            yes(h.session.syncIncognito(()->true));eq("",h.session.preview());yes(!h.session.select("日本",old));
            yes(h.session.type("ご"));eq("にほんご",h.session.text());h.accept=true;yes(h.session.confirm());yes(h.learned.isEmpty());
        });
        test("raw confirmation and cancellation never learn while explicit candidate success can",()->{
            Host h=normal();h.session.type("にほん");h.session.preview("日本",h.session.revision());h.session.cancelPreview();h.session.confirm();yes(h.learned.isEmpty());
            h.session.type("にほん");h.session.preview("日本",h.session.revision());h.accept=false;yes(!h.session.confirm());yes(h.learned.isEmpty());
            h.accept=true;yes(h.session.confirm());eq(java.util.Arrays.asList("にほん:日本"),h.learned);
        });
        test("bounded draft capacity reports rejection explicitly and confirmation resumes accepted typing",()->{
            Host h=normal();for(int i=0;i<256;i++)yes(h.session.type("あ"));yes(!h.session.type("い"));eq(0,h.commits);
            yes(h.session.limitReached());yes(h.session.confirm());eq(256,h.text.length());yes(h.session.type("い"));yes(h.session.confirm());eq(257,h.text.length());
        });
        test("old candidate actions cannot commit after a new field or a repeated reading",()->{
            Host h=normal();h.session.type("にほん");long old=h.session.revision();yes(h.session.select("日本",old));h.session.type("にほん");
            yes(!h.session.select("日本",old));eq("にほん",h.session.text());h.session.start(h.editor,FieldPolicy.from(1,0));
            yes(!h.session.select("日本",old));eq("日本",h.text);
        });
        test("bounded sequence oracle requires every accepted key and confirmation exactly once",()->{
            char[] alphabet={'T','P','C','D','A','X','F','M','N'};int sequences=0,accepted=0,confirmations=0;
            for(int length=0,power=1;length<=4;length++,power*=alphabet.length)for(int code=0;code<power;code++)for(int options=0;options<8;options++) {
                StringBuilder trace=new StringBuilder();int encoding=code;
                for(int i=0;i<length;i++,encoding/=alphabet.length)trace.append(alphabet[encoding%alphabet.length]);
                try {
                    Host h=new Host(1,0,"prefix:seed",false);h.omitRange=(options&1)!=0;
                    if((options&2)!=0){h.spanStart=7;h.spanEnd=11;}
                    String draft="にほん",preview="",expected=h.text;yes(h.session.type(draft));int count=0;
                    for(int step=0;step<length;step++) {
                        char op=trace.charAt(step);int first=h.cursor,last=h.end;
                        if(op=='T'){yes(h.session.type("ご"));accepted++;draft+="ご";preview="";}
                        else if(op=='P'){if(!draft.isEmpty()){yes(h.session.preview("日本",h.session.revision()));preview="日本";}}
                        else if(op=='C'){yes(h.session.cancelPreview());preview="";}
                        else if(op=='D') {
                            h.session.backspace();if(!preview.isEmpty())preview="";
                            else if(!draft.isEmpty())draft=draft.substring(0,draft.offsetByCodePoints(draft.length(),-1));
                            else {int a=Math.min(first,last),b=Math.max(first,last);if(a==b && a>0)a=expected.offsetByCodePoints(a,-1);expected=insert(expected,a,b,"");}
                        } else if(op=='A')h.session.cursor(-1);
                        else if(op=='X') {
                            if(!draft.isEmpty()){yes(h.session.confirm());expected=insert(expected,first,last,preview.isEmpty()?draft:preview);draft="";preview="";count++;confirmations++;}
                            else yes(!h.session.confirm());
                        } else if(op=='F')h.flush();else h.coalesce(op=='N');
                        if((options&4)!=0)h.flush();
                        eq(draft,h.session.text());eq(preview,h.session.preview());eq(expected,h.text);eq(count,h.commits);
                    }
                    if(!draft.isEmpty()) {
                        int first=h.cursor,last=h.end;yes(h.session.confirm());expected=insert(expected,first,last,preview.isEmpty()?draft:preview);count++;
                    }
                    h.flush();eq(expected,h.text);eq(count,h.commits);eq("",h.session.text());
                    yes(h.session.type("続"));int first=h.cursor,last=h.end;h.coalesce(true);eq("続",h.session.text());
                    yes(h.session.confirm());eq(insert(expected,first,last,"続"),h.text);eq(count+1,h.commits);sequences++;
                } catch(AssertionError failure){throw new AssertionError("Sequence="+trace+" options="+options,failure);}
            }
            eq(59048,sequences);yes(accepted>0);yes(confirmations>0);
            System.out.println("PASS: 59048 bounded sequences (depth 0-4; typed keys/local preview/cancel/delete/cursor/confirm/ordered/two coalescing forms; no ignored accepted keys or lockout allowed)");
        });
        test("manual conversion bounded oracle requires an explicit request and accounts for every accepted input",()->{
            char[] alphabet={'T','R','P','C','D','A','X','F','M','N'};int sequences=0,accepted=0,confirmations=0;
            for(int flags:new int[]{0,0x1000000})for(int length=0,power=1;length<=4;length++,power*=alphabet.length)
                for(int code=0;code<power;code++)for(int options=0;options<8;options++) {
                    StringBuilder trace=new StringBuilder();int encoding=code;
                    for(int i=0;i<length;i++,encoding/=alphabet.length)trace.append(alphabet[encoding%alphabet.length]);
                    try {
                        Host h=new Host(0x800a1,flags,"prefix:seed",false);h.omitRange=(options&1)!=0;
                        if((options&2)!=0){h.spanStart=7;h.spanEnd=11;}
                        String draft="にほん",preview="",expected=h.text;boolean requested=false;yes(h.session.type(draft));int commits=0;
                        for(int step=0;step<length;step++) {
                            char op=trace.charAt(step);int first=h.cursor,last=h.end;
                            if(op=='T'){yes(h.session.type("ご"));accepted++;draft+="ご";preview="";requested=false;}
                            else if(op=='R'){eq(!draft.isEmpty(),h.session.requestConversion());requested=!draft.isEmpty();}
                            else if(op=='P'){
                                boolean allowed=requested && !draft.isEmpty();eq(allowed,h.session.preview("日本",h.session.revision()));if(allowed)preview="日本";
                            } else if(op=='C'){yes(h.session.cancelPreview());preview="";}
                            else if(op=='D') {
                                h.session.backspace();if(!preview.isEmpty())preview="";
                                else {requested=false;
                                    if(!draft.isEmpty())draft=draft.substring(0,draft.offsetByCodePoints(draft.length(),-1));
                                    else {int a=Math.min(first,last),b=Math.max(first,last);if(a==b && a>0)a=expected.offsetByCodePoints(a,-1);expected=insert(expected,a,b,"");}
                                }
                            } else if(op=='A')h.session.cursor(-1);
                            else if(op=='X') {
                                if(!draft.isEmpty()){yes(h.session.confirm());expected=insert(expected,first,last,preview.isEmpty()?draft:preview);draft="";preview="";requested=false;commits++;confirmations++;}
                                else yes(!h.session.confirm());
                            } else if(op=='F')h.flush();else h.coalesce(op=='N');
                            if((options&4)!=0)h.flush();
                            eq(draft,h.session.text());eq(preview,h.session.preview());eq(requested,h.session.conversionRequested());
                            eq(requested && !draft.isEmpty(),h.session.hasCandidates());eq(expected,h.text);eq(commits,h.commits);yes(h.learned.isEmpty());
                        }
                        if(!draft.isEmpty()) {
                            int first=h.cursor,last=h.end;yes(h.session.confirm());expected=insert(expected,first,last,preview.isEmpty()?draft:preview);commits++;
                        }
                        h.flush();eq(expected,h.text);eq(commits,h.commits);eq("",h.session.text());yes(!h.session.conversionRequested());
                        yes(h.session.type("続"));int first=h.cursor,last=h.end;h.coalesce(true);eq("続",h.session.text());yes(!h.session.hasCandidates());
                        yes(h.session.confirm());eq(insert(expected,first,last,"続"),h.text);eq(commits+1,h.commits);yes(h.learned.isEmpty());sequences++;
                    } catch(AssertionError failure){throw new AssertionError("Manual sequence="+trace+" options="+options+" flags="+flags,failure);}
                }
            eq(177776,sequences);yes(accepted>0);yes(confirmations>0);
            System.out.println("PASS: 177776 explicit-only bounded sequences (depth 0-4; request/typed keys/local preview/cancel/delete/cursor/confirm/ordered/two coalescing forms; NO_PERSONALIZED_LEARNING both states)");
        });
        System.out.println("PASS: "+count+" local-preedit Android API host groups; native Android Binder/Looper/View/S24 NOT RUN");
    }
}
