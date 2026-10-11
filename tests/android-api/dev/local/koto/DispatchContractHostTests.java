/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.view.inputmethod.*;
import dev.local.koto.core.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.Objects;

/** Actual cached Android remote-wrapper Java code, synthetic remote endpoint; no native Binder. */
public final class DispatchContractHostTests {
    interface Test { void run()throws Exception; }
    static int count;static Object keepService;
    static void yes(boolean b){if(!b)throw new AssertionError();}
    static void eq(Object e,Object a){if(!Objects.equals(e,a))throw new AssertionError("Expected "+e+"; got "+a);}
    static void test(String name,Test test)throws Exception{test.run();count++;System.out.println("PASS: "+name);keepService=null;}
    static final class Remote {
        int commands;boolean fail;final InputConnection connection;
        Remote()throws Exception{
            Class<?> remoteInterface=Class.forName("com.android.internal.inputmethod.IRemoteInputConnection");
            Object remote=Proxy.newProxyInstance(remoteInterface.getClassLoader(),new Class<?>[]{remoteInterface},(p,m,a)->{
                if(m.getName().startsWith("get"))throw new AssertionError("Surrounding-text reads forbidden");
                if(m.getName().equals("commitText")){commands++;if(fail)throw new android.os.RemoteException("Synthetic dispatch failure");}
                return null;
            });
            Class<?> serviceInterface=Class.forName("android.inputmethodservice.InputMethodServiceInternal");
            Object service=Proxy.newProxyInstance(serviceInterface.getClassLoader(),new Class<?>[]{serviceInterface},(p,m,a)->m.getReturnType()==boolean.class?false:null);
            keepService=service;
            Class<?> group=Class.forName("com.android.internal.inputmethod.CancellationGroup");
            Constructor<?> constructor=Class.forName("android.inputmethodservice.RemoteInputConnection").getDeclaredConstructor(WeakReference.class,remoteInterface,group);
            constructor.setAccessible(true);connection=(InputConnection)constructor.newInstance(new WeakReference<Object>(service),remote,group.getConstructor().newInstance());
        }
        InputSession session(int type,int flags){EditorInfo info=new EditorInfo();info.inputType=type;info.imeOptions=flags;InputSession s=new InputSession();s.start(new ConnectionEditor(connection,info,(c,f)->{}),FieldPolicy.from(type,flags));return s;}
    }
    public static void main(String[] args)throws Exception{
        test("actual remote wrapper acknowledges a void commit command without host-application evidence",()->{
            Remote r=new Remote();yes(r.connection.commitText("日本",1));eq(1,r.commands);
        });
        test("dispatch contract sends once and retains no post-confirmation text for automatic retry",()->{
            Remote r=new Remote();InputSession s=r.session(1,0);int[] choices={0};s.choices((reading,word,p)->choices[0]++);
            s.type("にほん");s.preview("日本",s.revision());yes(s.confirm());eq(1,r.commands);eq(1,choices[0]);eq("",s.text());eq("",s.preview());
            s.relayout();yes(!s.confirm());s.finish();eq(1,r.commands);
        });
        test("observable remote dispatch failure preserves local draft and permits only an explicit retry",()->{
            Remote r=new Remote();r.fail=true;InputSession s=r.session(1,0);s.type("にほん");yes(!s.confirm());eq("にほん",s.text());eq(1,r.commands);
            s.relayout();eq(1,r.commands);r.fail=false;yes(s.confirm());eq(2,r.commands);eq("",s.text());
        });
        test("remote dispatch in protected private and no-learning policies does not notify learning",()->{
            for(int type:new int[]{1,0x81,0x91,0xe1,0,0xf}){
                Remote r=new Remote();InputSession s=r.session(type,0x1000000);s.mode(InputSession.Mode.KANA);s.incognito(true);int[] choices={0};s.choices((a,b,p)->choices[0]++);
                yes(s.type("ば"));if(s.policy().candidates)yes(s.confirm());eq(1,r.commands);eq("",s.text());eq(0,choices[0]);
            }
        });
        System.out.println("PASS: "+count+" Android remote-dispatch contract host groups; native Binder and host acceptance NOT RUN");
    }
}
