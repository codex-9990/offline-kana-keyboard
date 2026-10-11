/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.util.*;

/** Regressions for the reviewed obscured-event and password-safe character paths. */
public final class HardeningTests {
    static int count;
    static void yes(boolean value) { if(!value)throw new AssertionError(); }
    static void eq(Object e,Object a) { if(!Objects.equals(e,a))throw new AssertionError("Expected "+e+"; got "+a); }
    static void test(String name,Runnable test) { test.run();count++;System.out.println("PASS: "+name); }
    public static void main(String[] args) {
        test("both obscuration flags reject DOWN MOVE UP CANCEL and their combined value",()->{
            for(int flags:new int[]{1,2,3})for(int action:new int[]{0,1,2,3}) {
                ObscuredGestureGuard g=new ObscuredGestureGuard();
                eq(ObscuredGestureGuard.Decision.ALLOW,g.event(0,0));
                eq(ObscuredGestureGuard.Decision.CANCEL_AND_REJECT,g.event(action,flags));
                eq(ObscuredGestureGuard.Decision.REJECT,g.event(1,0));
            }
        });
        test("clean release after partial MOVE cannot complete a rejected gesture",()->{
            ObscuredGestureGuard g=new ObscuredGestureGuard();g.event(0,0);g.event(2,2);
            for(int action:new int[]{2,1,3,1})eq(ObscuredGestureGuard.Decision.REJECT,g.event(action,0));
            eq(ObscuredGestureGuard.Decision.ALLOW,g.event(0,0));eq(ObscuredGestureGuard.Decision.ALLOW,g.event(1,0));
        });
        test("a rejected DOWN cannot accept its clean tail and each new flagged gesture cancels once",()->{
            ObscuredGestureGuard g=new ObscuredGestureGuard();eq(ObscuredGestureGuard.Decision.CANCEL_AND_REJECT,g.event(0,1));
            eq(ObscuredGestureGuard.Decision.REJECT,g.event(2,2));eq(ObscuredGestureGuard.Decision.REJECT,g.event(1,0));
            eq(ObscuredGestureGuard.Decision.CANCEL_AND_REJECT,g.event(0,2));
        });
        test("lifecycle reset permits the next clean gesture without treating unrelated flags as obscuration",()->{
            ObscuredGestureGuard g=new ObscuredGestureGuard();g.event(0,1);g.reset();
            eq(ObscuredGestureGuard.Decision.ALLOW,g.event(0,4));eq(ObscuredGestureGuard.Decision.ALLOW,g.event(1,4));
        });
        test("complete direct choices contain every voiced semivoiced and small character in both scripts",()->{
            String voiced="がぎぐげござじずぜぞだぢづでどばびぶべぼぱぴぷぺぽゔ",small="ぁぃぅぇぉっゃゅょゎゕゖ";
            for(boolean tiny:new boolean[]{false,true})for(boolean kata:new boolean[]{false,true}) {
                String expected=DirectKanaLayout.script(tiny ? small : voiced,kata);Set<String> seen=new HashSet<>();
                for(String[] row:DirectKanaLayout.rows(tiny,kata)) {
                    yes(row.length<=9);
                    for(String value:row) {yes(value.codePointCount(0,value.length())==1);yes(seen.add(value));yes(expected.contains(value));yes(Character.getType(value.charAt(0))!=Character.NON_SPACING_MARK);}
                }
                eq(expected.codePointCount(0,expected.length()),seen.size());
            }
        });
        test("direct layout callers cannot mutate the fixed character table or original flick mappings",()->{
            String[][] rows=DirectKanaLayout.rows(false,false);rows[0][0]="x";eq("が",DirectKanaLayout.rows(false,false)[0][0]);
            for(int key=0;key<12;key++){String[] kana=FlickLayout.key(key);for(int d=0;d<5;d++){String original=kana[d];DirectKanaLayout.script(original,true);eq(original,FlickLayout.kana(key,d));}}
        });
        test("complete kana dispatch in all direct field types creates no local draft echo or learning",()->{
            for(int type:new int[]{0x81,0x91,0xe1,0x12,0,0xf,0xf1,2,3,4}) {
                final StringBuilder host=new StringBuilder();final int[] choices={0};
                InputSession.Editor editor=new InputSession.Editor(){public boolean commit(String s){host.append(s);return true;}public void finish(){}public void delete(){}public void cursor(int n){}public void enter(){}};
                InputSession s=new InputSession();s.start(editor,FieldPolicy.from(type,0x1000000));s.mode(InputSession.Mode.KANA);s.choices((r,w,p)->choices[0]++);
                for(String value:new String[]{"ば","ぱ","ゃ","ヴ","ッ","ヵ"}){yes(s.type(value));eq("",s.text());eq("",s.preview());yes(!s.preedit(true).visible);}
                eq("ばぱゃヴッヵ",host.toString());eq(0,choices[0]);yes(!s.hasCandidates());
            }
        });
        test("reported direct dispatch failure stores no password draft for automatic retry",()->{
            final int[] attempts={0};InputSession s=new InputSession();
            s.start(new InputSession.Editor(){public boolean commit(String v){attempts[0]++;return false;}public void finish(){}public void delete(){}public void cursor(int n){}public void enter(){}},FieldPolicy.from(0x81,0));
            s.mode(InputSession.Mode.KANA);yes(!s.type("ば"));eq("",s.text());eq(1,attempts[0]);s.relayout();s.confirm();s.finish();eq(1,attempts[0]);
        });
        test("true dispatch clears ordinary draft exactly once without retaining text or auto-resending",()->{
            final int[] attempts={0};InputSession s=new InputSession();
            s.start(new InputSession.Editor(){public boolean commit(String v){attempts[0]++;return true;}public void finish(){}public void delete(){}public void cursor(int n){}public void enter(){}},FieldPolicy.from(1,0));
            s.type("にほん");yes(s.confirm());eq("",s.text());eq("",s.preview());s.relayout();yes(!s.confirm());s.finish();eq(1,attempts[0]);
        });
        System.out.println("PASS: "+count+" hardening core groups; native Android dispatch/IME/password widgets NOT RUN");
    }
}
