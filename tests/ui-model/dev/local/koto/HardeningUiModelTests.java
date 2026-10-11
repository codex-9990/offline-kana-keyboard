/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.content.*;
import android.os.Handler;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import dev.local.koto.core.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Actual production Activities/IME/editor/learning/root/key code; declared host framework models. */
public final class HardeningUiModelTests {
    interface Test { void run() throws Exception; }
    static int count;static volatile boolean failPrivateRead;
    static final Map<String,Object> values=new ConcurrentHashMap<>();
    static final Set<SharedPreferences.OnSharedPreferenceChangeListener> listeners=new CopyOnWriteArraySet<>();
    static int privateWrites;
    static void yes(boolean b){if(!b)throw new AssertionError();}
    static void eq(Object e,Object a){if(!Objects.equals(e,a))throw new AssertionError("Expected "+e+"; got "+a);}
    static void test(String name,Test body)throws Exception{body.run();count++;System.out.println("PASS: "+name);}
    static final SharedPreferences prefs=(SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class<?>[]{SharedPreferences.class},(p,m,a)->{
        String name=m.getName();
        if(name.equals("getBoolean")||name.equals("getInt")){
            if(failPrivateRead&&a[0].equals("incognito"))throw new ClassCastException("Synthetic private-setting read failure");
            return values.getOrDefault(a[0],a[1]);
        }
        if(name.equals("registerOnSharedPreferenceChangeListener")){listeners.add((SharedPreferences.OnSharedPreferenceChangeListener)a[0]);return null;}
        if(name.equals("unregisterOnSharedPreferenceChangeListener")){listeners.remove(a[0]);return null;}
        if(name.equals("edit")){
            Map<String,Object> pending=new HashMap<>();
            return Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),new Class<?>[]{SharedPreferences.Editor.class},(e,n,b)->{
                if(n.getName().startsWith("put")){pending.put((String)b[0],b[1]);return e;}
                if(n.getName().equals("apply")||n.getName().equals("commit")){
                    List<String> changed=new ArrayList<>();
                    for(Map.Entry<String,Object> item:pending.entrySet()){
                        if(item.getKey().equals("incognito"))privateWrites++;
                        if(!Objects.equals(values.put(item.getKey(),item.getValue()),item.getValue()))changed.add(item.getKey());
                    }
                    for(String key:changed)for(SharedPreferences.OnSharedPreferenceChangeListener l:listeners)
                        new Handler(android.os.Looper.getMainLooper()).post(()->l.onSharedPreferenceChanged((SharedPreferences)p,key));
                    return n.getReturnType()==boolean.class?true:null;
                }
                return null;
            });
        }
        return null;
    });
    static void pump(){Handler.drain();}
    static void waitFor(BooleanSupplier condition)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(!condition.getAsBoolean()){pump();if(System.nanoTime()>end)throw new AssertionError("Model worker deadline");Thread.sleep(2);}
        pump();
    }
    static List<View> descendants(View view){
        List<View> out=new ArrayList<>();if(view==null)return out;out.add(view);
        if(view instanceof ViewGroup)for(View child:((ViewGroup)view).children)out.addAll(descendants(child));return out;
    }
    static CheckBox privateBox(SettingsActivity a){for(View v:descendants(a.root))if(v instanceof CheckBox&&((TextView)v).getText().toString().startsWith("私用モード"))return (CheckBox)v;throw new AssertionError();}
    static View named(SettingsActivity a,String label){for(View v:descendants(a.root))if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;throw new AssertionError(label);}
    static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    static InputSession session(KotoImeService ime)throws Exception{return (InputSession)field(ime,"session");}
    static KeyView key(KotoImeService ime,String label)throws Exception{
        Field labelField=KeyView.class.getDeclaredField("label");labelField.setAccessible(true);
        for(View v:descendants((View)field(ime,"board")))if(v instanceof KeyView&&label.equals(labelField.get(v)))return (KeyView)v;
        throw new AssertionError("Missing fixed key: "+label);
    }
    static void press(KotoImeService ime,String label)throws Exception{yes(key(ime,label).performClick());pump();}
    static void tap(SecureTouchFrame root,View target,int downFlags,int moveFlags,int upFlags){
        root.forceNextTarget(target);root.dispatchTouchEvent(new MotionEvent(0,downFlags));
        if(moveFlags>=0)root.dispatchTouchEvent(new MotionEvent(2,moveFlags));root.dispatchTouchEvent(new MotionEvent(1,upFlags));pump();
    }
    static class Host {
        String text="";int commits,keyDowns;boolean drop;
        final InputConnection connection=(InputConnection)Proxy.newProxyInstance(InputConnection.class.getClassLoader(),new Class<?>[]{InputConnection.class},(p,m,a)->{
            String name=m.getName();
            if(name.startsWith("get")||name.startsWith("setComposing")||name.equals("setSelection"))throw new AssertionError("Forbidden host read/span mutation");
            if(name.equals("finishComposingText"))return true;
            if(name.equals("commitText")){commits++;if(!drop)text+=a[0];return true;}
            if(name.equals("sendKeyEvent")){KeyEvent e=(KeyEvent)a[0];if(e.getAction()==0){keyDowns++;if(e.getKeyCode()==67&&!text.isEmpty())text=text.substring(0,text.offsetByCodePoints(text.length(),-1));}return true;}
            return m.getReturnType()==boolean.class?false:null;
        });
    }
    static void start(KotoImeService ime,Host host,int type,int flags){EditorInfo info=new EditorInfo();info.inputType=type;info.imeOptions=flags;ime.connection=host.connection;ime.onStartInput(info,false);ime.onStartInputView(info,false);pump();}
    public static void main(String[] args)throws Exception{
        Context.preferences=prefs;Context.storage=Files.createTempDirectory("koto-ui-model-storage-").toFile();
        try{run();System.out.println("PASS: "+count+" production UI integration model groups; native Android WindowManager/Binder/Looper/layout/S24 NOT RUN");System.exit(0);}
        catch(Throwable failure){failure.printStackTrace();System.exit(1);}
    }
    static void run()throws Exception{
        prefs.edit().putBoolean("incognito",true).apply();
        SettingsActivity settings=new SettingsActivity();settings.onCreate(null);settings.onResume();
        LearningCoordinator learning=LearningStore.get(settings);waitFor(()->!learning.state().busy);
        test("private control synchronizes both resume directions without writing current preferences",()->{
            settings.onPause();prefs.edit().putBoolean("incognito",false).apply();pump();int writes=privateWrites;
            settings.onResume();yes(!privateBox(settings).isChecked());eq(writes,privateWrites);
            settings.onPause();prefs.edit().putBoolean("incognito",true).apply();pump();writes=privateWrites;
            settings.onResume();yes(privateBox(settings).isChecked());eq(writes,privateWrites);
        });
        test("foreground preference callbacks refresh height haptics sensitivity and private control",()->{
            prefs.edit().putInt("row_height",72).putBoolean("haptics",true).putInt("flick_distance",12).putBoolean("incognito",false).apply();int writes=privateWrites;pump();
            eq(28,((SeekBar)field(settings,"heightSlider")).getProgress());eq(4,((SeekBar)field(settings,"sensitivitySlider")).getProgress());
            yes(((CheckBox)field(settings,"hapticToggle")).isChecked());yes(!privateBox(settings).isChecked());eq(writes,privateWrites);
        });
        test("private preference read failure displays ON and does not persist a false setting",()->{
            settings.onPause();failPrivateRead=true;int writes=privateWrites;settings.onResume();yes(privateBox(settings).isChecked());eq(writes,privateWrites);
            failPrivateRead=false;settings.onPause();settings.onResume();yes(!privateBox(settings).isChecked());
        });
        test("Settings and exported launcher reject full partial and mixed gesture tails",()->{
            SetupActivity setup=new SetupActivity();setup.onCreate(null);setup.onResume();
            for(SettingsActivity a:Arrays.asList(settings,setup))for(int[] events:new int[][]{{0,-1,1},{0,2,0},{1,-1,0},{0,-1,2},{2,0,0},{0,3,0}}){
                CheckBox box=privateBox(a);boolean previous=prefs.getBoolean("incognito",false);int writes=privateWrites;
                tap((SecureTouchFrame)a.root,box,events[0],events[1],events[2]);eq(previous,prefs.getBoolean("incognito",false));eq(writes,privateWrites);
                tap((SecureTouchFrame)a.root,box,0,-1,0);eq(!previous,prefs.getBoolean("incognito",false));
            }
            setup.onPause();
        });
        test("obscured retention/delete controls cannot change storage policy or delete records",()->{
            yes(learning.configure(7));waitFor(()->!learning.state().busy);eq(7,learning.state().days);
            yes(learning.record("にほん","日本",FieldPolicy.from(1,0),false,true,learning.state().generation));waitFor(()->learning.state().entries.size()==1);
            for(String label:new String[]{"オフ（学習データを削除）","1 日","学習データをすべて削除"}){
                View control=named(settings,label);tap((SecureTouchFrame)settings.root,control,0,2,0);eq(7,learning.state().days);eq(1,learning.state().entries.size());
            }
            yes(learning.clear());waitFor(()->!learning.state().busy);eq(0,learning.state().entries.size());
        });
        test("accessibility performClick remains usable after the touch guard blocks a gesture",()->{
            CheckBox box=privateBox(settings);boolean before=prefs.getBoolean("incognito",false);
            tap((SecureTouchFrame)settings.root,box,1,-1,0);box.performClick();pump();eq(!before,prefs.getBoolean("incognito",false));
        });
        prefs.edit().putBoolean("incognito",false).apply();pump();settings.onPause();
        test("root clean cancellation stops held repeats on obscured UP MOVE or CANCEL",()->{
            for(int action:new int[]{1,2,3})for(int flags:new int[]{1,2,3}){
                int[] n={0};KeyView key=new KeyView(settings,"delete",null,d->n[0]++,false,true,false);
                SecureTouchFrame root=new SecureTouchFrame(settings,key::cancelGesture,null);root.forceNextTarget(key);
                root.dispatchTouchEvent(new MotionEvent(0,0));eq(1,n[0]);root.dispatchTouchEvent(new MotionEvent(action,flags));
                for(int i=0;i<10;i++)key.runOnePosted();eq(1,n[0]);eq(0,key.postedCount());
                root.dispatchTouchEvent(new MotionEvent(1,0));eq(1,n[0]);
                root.dispatchTouchEvent(new MotionEvent(0,0));root.dispatchTouchEvent(new MotionEvent(1,0));eq(2,n[0]);
            }
        });
        test("ordinary button DOWN partial MOVE clean UP is cancelled before its listener",()->{
            int[] clicks={0};Button button=new Button(settings);button.setOnClickListener(v->clicks[0]++);
            SecureTouchFrame root=new SecureTouchFrame(settings,()->{},null);tap(root,button,0,2,0);eq(0,clicks[0]);tap(root,button,0,-1,0);eq(1,clicks[0]);
        });
        test("held repeats stop when disabled hidden detached or lifecycle-reset",()->{
            for(int operation=0;operation<4;operation++){
                int[] n={0};KeyView key=new KeyView(settings,"delete",null,d->n[0]++,false,true,false);SecureTouchFrame root=new SecureTouchFrame(settings,key::cancelGesture,null);
                root.forceNextTarget(key);root.dispatchTouchEvent(new MotionEvent(0,0));
                if(operation==0)key.setEnabled(false);if(operation==1)key.setVisibility(View.GONE);if(operation==2)key.simulateDetach();if(operation==3)root.resetInteraction();
                key.runOnePosted();eq(1,n[0]);eq(0,key.postedCount());
            }
        });
        test("accessibility flick action remains usable while touch gesture is blocked",()->{
            int[] direction={-1};KeyView key=new KeyView(settings,"あ",FlickLayout.key(0),d->direction[0]=d,false,false,false);
            SecureTouchFrame root=new SecureTouchFrame(settings,key::cancelGesture,null);tap(root,key,1,-1,0);
            yes(key.performAccessibilityAction(0x01000001,null));eq(1,direction[0]);
        });
        KotoImeService ime=new KotoImeService();ime.onCreate();ime.onCreateInputView();
        try{
            waitFor(()->{try{return field(ime,"dictionary")!=null;}catch(Exception e){throw new RuntimeException(e);}});
            test("actual IME protected-field palette dispatches full voiced/small kana with no echo or learning",()->{
                int before=learning.state().entries.size();
                for(int type:new int[]{0x81,0x91,0xe1,0,0xf,0xf1}){
                    Host h=new Host();start(ime,h,type,0x1000000);if(session(ime).mode()!=InputSession.Mode.KANA)press(ime,"かな");
                    press(ime,"濁・小\n選択");press(ime,"ば");press(ime,"濁・小\n選択");press(ime,"ぱ");
                    press(ime,"濁・小\n選択");press(ime,"小かな");press(ime,"ゃ");
                    press(ime,"濁・小\n選択");press(ime,"カタカナ");press(ime,"ヴ");
                    press(ime,"濁・小\n選択");press(ime,"小かな");press(ime,"ッ");press(ime,"カ");
                    eq("ばぱゃヴッカ",h.text);eq("",session(ime).text());eq("",session(ime).preview());
                    TextView preedit=(TextView)field(ime,"preedit");eq(View.GONE,preedit.getVisibility());eq("",preedit.getText());eq("",preedit.getContentDescription());
                }
                pump();eq(before,learning.state().entries.size());
            });
            test("protected palette appends a complete character and never reconstructs prior secret input",()->{
                Host h=new Host();start(ime,h,0x81,0);press(ime,"かな");press(ime,"は");press(ime,"濁・小\n選択");press(ime,"ば");eq("はば",h.text);eq(2,h.commits);eq("",session(ime).text());
            });
            test("IME root rejects obscured character click and permits the next clean gesture",()->{
                Host h=new Host();start(ime,h,0x81,0);SecureTouchFrame root=(SecureTouchFrame)field(ime,"frame");
                tap(root,key(ime,"a"),0,2,0);eq("",h.text);tap(root,key(ime,"a"),0,-1,0);eq("a",h.text);
            });
            test("ordinary IME modifier remains a local voiced preview with no implicit host commit",()->{
                Host h=new Host();start(ime,h,1,0);press(ime,"は");press(ime,"゛小");eq("ば",session(ime).text());eq("",h.text);press(ime,"確定");eq("ば",h.text);
            });
            test("documented true-dispatch host drop clears once without auto-resend or retry retention",()->{
                Host h=new Host();h.drop=true;start(ime,h,1,0);press(ime,"は");press(ime,"確定");eq(1,h.commits);eq("",h.text);eq("",session(ime).text());
                ime.onConfigurationChanged(new android.content.res.Configuration());ime.onFinishInputView(false);pump();eq(1,h.commits);eq("",session(ime).text());
            });
            test("field restart/hide clears ordinary draft and protected palette state is reset on new field",()->{
                Host h=new Host();start(ime,h,1,0);press(ime,"は");ime.onFinishInputView(false);eq("",session(ime).text());eq("",h.text);
                start(ime,h,0x81,0);press(ime,"かな");press(ime,"濁・小\n選択");yes((Boolean)field(ime,"directKanaPalette"));
                start(ime,new Host(),0x81,0);yes(!(Boolean)field(ime,"directKanaPalette"));yes(!(Boolean)field(ime,"directKatakana"));
            });
            test("same-session rotation preserves local reading and invalidates old candidate listeners",()->{
                Host h=new Host();start(ime,h,1,0);
                key(ime,"な").performAccessibilityAction(0x01000001,null);key(ime,"は").performAccessibilityAction(0x01000004,null);key(ime,"わ").performAccessibilityAction(0x01000002,null);pump();
                eq("にほん",session(ime).text());View old=null;
                for(View v:descendants((View)field(ime,"candidateRow")))if(v instanceof Button){old=v;break;}
                yes(old!=null);android.content.res.Resources.configuration.orientation=2;
                ime.onConfigurationChanged(android.content.res.Resources.configuration);old.performClick();pump();
                eq("にほん",session(ime).text());eq("",h.text);eq(44,field(ime,"rowHeight"));
                android.content.res.Resources.configuration.orientation=1;
            });
            test("actual IME no-suggestions web field opens fixed conversion only on the first explicit request",()->{
                Host h=new Host();start(ime,h,0x800a1,0x1000000);
                key(ime,"な").performAccessibilityAction(0x01000001,null);key(ime,"は").performAccessibilityAction(0x01000004,null);key(ime,"わ").performAccessibilityAction(0x01000002,null);pump();
                eq("にほん",session(ime).text());yes(!session(ime).hasCandidates());press(ime,"変換");yes(session(ime).hasCandidates());yes(!session(ime).preview().isEmpty());
                press(ime,"確定");yes(!h.text.isEmpty());eq(1,h.commits);eq("",session(ime).text());
            });
        }finally{ime.onDestroy();}
        yes(learning.configure(0));waitFor(()->!learning.state().busy);settings.onPause();
    }
}
