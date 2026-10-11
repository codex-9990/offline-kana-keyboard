#!/usr/bin/env python3
"""Compile unmodified Settings/Setup/KeyView against a small, declared host UI model.

This model implements state, callbacks, and the documented filter-before-dispatch
path only. It omits Android WindowManager, Binder, rendering, Looper, and OEM UI.
It must never be described as an Android or tapjacking exploit runtime test.
"""
from pathlib import Path
import subprocess
import hashlib
import json

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / '.build-cache/android-ui-model'
SRC = OUT / 'src'
SOURCES = {
'android/util/DisplayMetrics.java': '''package android.util; public class DisplayMetrics { public float density=1,scaledDensity=1; public int widthPixels=400; }''',
'android/content/res/Resources.java': '''package android.content.res; public class Resources { public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();} }''',
'android/content/res/AssetManager.java': '''package android.content.res; public class AssetManager { public java.io.InputStream open(String name)throws java.io.IOException{return new java.io.FileInputStream("app/src/main/assets/"+name);} }''',
'android/content/Context.java': '''package android.content;
public class Context {
 public static final int MODE_PRIVATE=0; public static final String INPUT_METHOD_SERVICE="input_method";
 public static SharedPreferences preferences;
 public SharedPreferences getSharedPreferences(String n,int m){return preferences;}
 public android.content.res.Resources getResources(){return new android.content.res.Resources();}
 public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}
 public android.content.pm.PackageManager getPackageManager(){return null;}
 public Object getSystemService(String n){return null;} public void startActivity(Intent intent){}
}''',
'android/app/Activity.java': '''package android.app;
public class Activity extends android.content.Context {
 public android.view.View root; protected void onCreate(android.os.Bundle state){} protected void onResume(){} protected void onPause(){}
 public void setContentView(android.view.View v){root=v;}
}''',
'android/view/MotionEvent.java': '''package android.view;
public class MotionEvent {
 public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3;
 public static final int FLAG_WINDOW_IS_OBSCURED=1,FLAG_WINDOW_IS_PARTIALLY_OBSCURED=2;
 final int action,flags,pointers; final float x,y;
 public MotionEvent(int a,int f){this(a,f,1,20,20);} public MotionEvent(int a,int f,int n,float x,float y){action=a;flags=f;pointers=n;this.x=x;this.y=y;}
 public int getFlags(){return flags;} public int getActionMasked(){return action;} public int getPointerCount(){return pointers;}
 public float getX(){return x;} public float getY(){return y;}
}''',
'android/view/View.java': '''package android.view;
public class View {
 public static final int VISIBLE=0; static int serial=1; public interface OnClickListener{void onClick(View v);}
 protected final android.content.Context context; OnClickListener click; boolean enabled=true,filtered,down; int id;
 final java.util.ArrayDeque<Runnable> queue=new java.util.ArrayDeque<>();
 public View(android.content.Context c){context=c;} public android.content.res.Resources getResources(){return context.getResources();}
 public static int generateViewId(){return serial++;} public void setId(int n){id=n;} public int getId(){return id;}
 public void setEnabled(boolean b){enabled=b;} public boolean isEnabled(){return enabled;} public void setClickable(boolean b){} public void setFocusable(boolean b){}
 public void setPadding(int a,int b,int c,int d){} public void setContentDescription(CharSequence s){} public void invalidate(){}
 public void setFilterTouchesWhenObscured(boolean b){filtered=b;} public boolean getFilterTouchesWhenObscured(){return filtered;}
 public boolean onFilterTouchEventForSecurity(MotionEvent e){return !(filtered&&(e.getFlags()&1)!=0);}
 public boolean dispatchTouchEvent(MotionEvent e){if(!onFilterTouchEventForSecurity(e))return false; return onTouchEvent(e);}
 public boolean onTouchEvent(MotionEvent e){if(!enabled)return false;if(e.getActionMasked()==0)down=true;if(e.getActionMasked()==1&&down){down=false;return performClick();}if(e.getActionMasked()==3)down=false;return true;}
 public void setOnClickListener(OnClickListener l){click=l;} public boolean performClick(){if(click!=null)click.onClick(this);return true;}
 public int getWidth(){return 100;} public int getHeight(){return 60;} protected void onDraw(android.graphics.Canvas c){}
 public boolean postDelayed(Runnable r,long t){queue.add(r);return true;} public boolean removeCallbacks(Runnable r){return queue.removeIf(x->x==r);}
 public void runOnePosted(){if(!queue.isEmpty())queue.remove().run();} public int postedCount(){return queue.size();}
 public boolean performHapticFeedback(int i){return false;} protected void onDetachedFromWindow(){} protected void onWindowVisibilityChanged(int v){}
 public void simulateDetach(){onDetachedFromWindow();} public void simulateVisibility(int v){onWindowVisibilityChanged(v);}
 public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo i){}
 public boolean performAccessibilityAction(int a,android.os.Bundle b){return false;}
}''',
'android/view/ViewGroup.java': '''package android.view;
public class ViewGroup extends View {
 public final java.util.List<View> children=new java.util.ArrayList<>(); public ViewGroup(android.content.Context c){super(c);}
 public void addView(View v){children.add(v);} public void addView(View v,Object params){children.add(v);}
 public void setMotionEventSplittingEnabled(boolean b){}
 /** Narrow filter-before-child path; rejected UP deliberately has no synthesized CANCEL, as in AOSP API35. */
 public boolean dispatchTo(View child,MotionEvent e){if(!onFilterTouchEventForSecurity(e))return false;return child.dispatchTouchEvent(e);}
}''',
'android/widget/TextView.java': '''package android.widget;
public class TextView extends android.view.View {
 CharSequence text=""; public TextView(android.content.Context c){super(c);} public void setText(CharSequence s){text=s;} public CharSequence getText(){return text;}
 public void setTextSize(float s){} public void setTextColor(int c){} public void setGravity(int g){}
}''',
'android/widget/Button.java': '''package android.widget; public class Button extends TextView { public Button(android.content.Context c){super(c);} }''',
'android/widget/CheckBox.java': '''package android.widget;
public class CheckBox extends Button {
 public interface OnCheckedChangeListener{void onCheckedChanged(CheckBox v,boolean b);} boolean checked; OnCheckedChangeListener listener;
 public CheckBox(android.content.Context c){super(c);} public boolean isChecked(){return checked;}
 public void setChecked(boolean b){if(b!=checked){checked=b;if(listener!=null)listener.onCheckedChanged(this,b);}}
 public void setOnCheckedChangeListener(OnCheckedChangeListener l){listener=l;}
 public boolean performClick(){if(!isEnabled())return false;setChecked(!checked);return super.performClick();}
}''',
'android/widget/RadioButton.java': '''package android.widget; public class RadioButton extends CheckBox { public RadioButton(android.content.Context c){super(c);} }''',
'android/widget/LinearLayout.java': '''package android.widget;
public class LinearLayout extends android.view.ViewGroup {
 public static final int VERTICAL=1,HORIZONTAL=0; public LinearLayout(android.content.Context c){super(c);} public void setOrientation(int n){}
 public static class LayoutParams{public LayoutParams(int w,int h){}public LayoutParams(int w,int h,float t){}}
}''',
'android/widget/ScrollView.java': '''package android.widget; public class ScrollView extends android.view.ViewGroup { public ScrollView(android.content.Context c){super(c);} }''',
'android/widget/FrameLayout.java': '''package android.widget; public class FrameLayout extends android.view.ViewGroup { public FrameLayout(android.content.Context c){super(c);} }''',
'android/widget/RadioGroup.java': '''package android.widget;
public class RadioGroup extends LinearLayout {
 public RadioGroup(android.content.Context c){super(c);} public void check(int id){for(android.view.View v:children)((RadioButton)v).setChecked(v.getId()==id);}
}''',
'android/widget/SeekBar.java': '''package android.widget;
public class SeekBar extends android.view.View {
 public interface OnSeekBarChangeListener{void onProgressChanged(SeekBar v,int n,boolean user);void onStartTrackingTouch(SeekBar v);void onStopTrackingTouch(SeekBar v);}
 int value; OnSeekBarChangeListener listener; public SeekBar(android.content.Context c){super(c);} public void setMax(int n){}
 public void setProgress(int n){value=n;if(listener!=null)listener.onProgressChanged(this,n,false);} public int getProgress(){return value;}
 public void setOnSeekBarChangeListener(OnSeekBarChangeListener l){listener=l;}
}''',
'android/graphics/Canvas.java': '''package android.graphics;public class Canvas {public void drawRoundRect(RectF r,float a,float b,Paint p){}public void drawText(String s,float a,float b,Paint p){}}''',
'android/graphics/RectF.java': '''package android.graphics;public class RectF {public void set(float a,float b,float c,float d){}}''',
'android/graphics/Paint.java': '''package android.graphics;public class Paint {public static final int ANTI_ALIAS_FLAG=1;public enum Align{CENTER}public Paint(int f){}public void setColor(int c){}public void setTextAlign(Align a){}public void setTextSize(float s){}public float getTextSize(){return 20;}public float measureText(String s){return s.length()*10;}public float ascent(){return -10;}public float descent(){return 2;}}''',
'dev/local/koto/LearningStore.java': '''package dev.local.koto;
import dev.local.koto.core.*;
public final class LearningStore {
 static LearningCoordinator instance; public static LearningCoordinator get(android.content.Context context){
  if(instance==null){LearningRepository.Policy policy=new LearningRepository.Policy(){int days;boolean cleanup;public int days(){return days;}public boolean cleanupRequired(){return cleanup;}public boolean save(int d,boolean c){days=d;cleanup=c;return true;}};
  LearningRepository.Storage storage=new LearningRepository.Storage(){byte[] bytes;public byte[] read(){return bytes;}public void write(byte[] b){bytes=b.clone();}public void clear(){bytes=null;}};
  instance=new LearningCoordinator(new LearningRepository(storage,policy),Runnable::run,Runnable::run,System::currentTimeMillis,()->android.content.Context.preferences.getBoolean("incognito",false));}
  return instance;
 }
}''',
}

# The model intentionally covers Java state/cancellation only, not native layout,
# WindowManager trust classification, Binder, accessibility transport or timing.
# Production Settings, IME, learning, editor, touch-root and key classes are compiled unchanged.
SOURCES.pop('dev/local/koto/LearningStore.java')
SOURCES.update({
'android/os/SystemClock.java': '''package android.os;public class SystemClock{public static long uptimeMillis(){return System.nanoTime()/1000000;}}''',
'android/os/Looper.java': '''package android.os;public class Looper{private static final Looper MAIN=new Looper();public static Looper getMainLooper(){return MAIN;}}''',
'android/os/Handler.java': '''package android.os;
public class Handler{static final java.util.concurrent.ConcurrentLinkedQueue<Runnable> queue=new java.util.concurrent.ConcurrentLinkedQueue<>();
public Handler(Looper l){}public boolean post(Runnable r){queue.add(r);return true;}public void removeCallbacksAndMessages(Object o){}
public static void drain(){int n=0;Runnable r;while((r=queue.poll())!=null){if(++n>10000)throw new AssertionError("Unbounded model queue");r.run();}}}
''',
'android/content/res/Configuration.java': '''package android.content.res;public class Configuration{public static final int ORIENTATION_PORTRAIT=1,ORIENTATION_LANDSCAPE=2;public int orientation=1;}''',
'android/content/res/Resources.java': '''package android.content.res;public class Resources{public static final Configuration configuration=new Configuration();public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();}public Configuration getConfiguration(){return configuration;}}''',
'android/content/Context.java': '''package android.content;
public class Context{
public static final int MODE_PRIVATE=0;public static final String INPUT_METHOD_SERVICE="input_method";public static SharedPreferences preferences;
static final Context application=new Context();public static java.io.File storage;
public Context getApplicationContext(){return application;}public java.io.File getNoBackupFilesDir(){return storage;}
public SharedPreferences getSharedPreferences(String n,int m){return preferences;}public String getPackageName(){return "dev.local.koto";}
public android.content.res.Resources getResources(){return new android.content.res.Resources();}
public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}
public android.content.pm.PackageManager getPackageManager(){return null;}
public Object getSystemService(String n){return new android.view.inputmethod.InputMethodManager();}public void startActivity(Intent i){}
}''',
'android/view/MotionEvent.java': '''package android.view;
public class MotionEvent{public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3,FLAG_WINDOW_IS_OBSCURED=1,FLAG_WINDOW_IS_PARTIALLY_OBSCURED=2;
int action,flags,pointers,source;final float x,y;long downTime,eventTime;public MotionEvent(int a,int f){this(a,f,1,20,20);}
public MotionEvent(int a,int f,int n,float x,float y){action=a;flags=f;pointers=n;this.x=x;this.y=y;}
public static MotionEvent obtain(long d,long t,int a,float x,float y,int m){MotionEvent e=new MotionEvent(a,0,1,x,y);e.downTime=d;e.eventTime=t;return e;}
public int getFlags(){return flags;}public int getActionMasked(){return action;}public int getPointerCount(){return pointers;}
public float getX(){return x;}public float getY(){return y;}public long getDownTime(){return downTime;}public long getEventTime(){return eventTime;}
public int getSource(){return source;}public void setSource(int s){source=s;}public void recycle(){}
}''',
'android/view/View.java': '''package android.view;
public class View{
public static final int VISIBLE=0,INVISIBLE=4,GONE=8,LAYOUT_DIRECTION_LTR=0;static int serial=1;public interface OnClickListener{void onClick(View v);}
protected final android.content.Context context;OnClickListener click;boolean enabled=true,filtered,down;int id,visibility=VISIBLE,windowVisibility=VISIBLE;CharSequence description="";
final java.util.ArrayDeque<Runnable> queue=new java.util.ArrayDeque<>();public View(android.content.Context c){context=c;}
public android.content.res.Resources getResources(){return context.getResources();}public static int generateViewId(){return serial++;}
public void setId(int i){id=i;}public int getId(){return id;}public void setEnabled(boolean b){enabled=b;}public boolean isEnabled(){return enabled;}
public void setClickable(boolean b){}public void setFocusable(boolean b){}public void setPadding(int a,int b,int c,int d){}
public void setContentDescription(CharSequence s){description=s;}public CharSequence getContentDescription(){return description;}public void invalidate(){}
public void setFilterTouchesWhenObscured(boolean b){filtered=b;}public boolean getFilterTouchesWhenObscured(){return filtered;}
public boolean onFilterTouchEventForSecurity(MotionEvent e){return !(filtered&&(e.getFlags()&1)!=0);}
public boolean dispatchTouchEvent(MotionEvent e){if(!onFilterTouchEventForSecurity(e))return false;return onTouchEvent(e);}
public boolean onTouchEvent(MotionEvent e){if(!enabled)return false;if(e.getActionMasked()==0)down=true;if(e.getActionMasked()==1&&down){down=false;return performClick();}if(e.getActionMasked()==3)down=false;return true;}
public void setOnClickListener(OnClickListener l){click=l;}public boolean performClick(){if(click!=null)click.onClick(this);return true;}
public int getWidth(){return 100;}public int getHeight(){return 60;}public int getLeft(){return 0;}protected void onDraw(android.graphics.Canvas c){}
public boolean postDelayed(Runnable r,long t){queue.add(r);return true;}public boolean post(Runnable r){r.run();return true;}
public boolean removeCallbacks(Runnable r){return queue.removeIf(x->x==r);}public void runOnePosted(){if(!queue.isEmpty())queue.remove().run();}public int postedCount(){return queue.size();}
public boolean performHapticFeedback(int i){return false;}protected void onDetachedFromWindow(){down=false;}
protected void onWindowVisibilityChanged(int v){}protected void onVisibilityChanged(View changed,int v){}
public void simulateDetach(){onDetachedFromWindow();}public void simulateVisibility(int v){windowVisibility=v;onWindowVisibilityChanged(v);}
public boolean isShown(){return visibility==VISIBLE&&windowVisibility==VISIBLE;}public void setVisibility(int v){visibility=v;onVisibilityChanged(this,v);}public int getVisibility(){return visibility;}
public void setLayoutDirection(int d){}public void setBackgroundColor(int c){}public void scrollTo(int x,int y){}
public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo i){}
public boolean performAccessibilityAction(int a,android.os.Bundle b){return false;}
}''',
'android/view/ViewGroup.java': '''package android.view;
public class ViewGroup extends View{
public final java.util.List<View> children=new java.util.ArrayList<>();View forcedTarget,activeTarget;
public ViewGroup(android.content.Context c){super(c);}public void addView(View v){children.add(v);}public void addView(View v,Object p){children.add(v);}
public void setMotionEventSplittingEnabled(boolean b){}public int getChildCount(){return children.size();}public View getChildAt(int n){return children.get(n);}
public void removeAllViews(){for(View v:new java.util.ArrayList<View>(children))v.simulateDetach();children.clear();}
public void forceNextTarget(View v){forcedTarget=v;}
/** Deliberately modeled, not native: security filter before child target, CANCEL releases it, DOWN begins a fresh target. */
public boolean dispatchTouchEvent(MotionEvent e){if(!onFilterTouchEventForSecurity(e))return false;
if(e.getActionMasked()==0){if(activeTarget!=null)activeTarget.dispatchTouchEvent(MotionEvent.obtain(0,0,3,0,0,0));activeTarget=forcedTarget;}
boolean r=activeTarget==null?super.onTouchEvent(e):activeTarget.dispatchTouchEvent(e);
if(e.getActionMasked()==1||e.getActionMasked()==3)activeTarget=null;return r;}
protected void onDetachedFromWindow(){for(View child:children)child.simulateDetach();super.onDetachedFromWindow();}
}''',
'android/widget/FrameLayout.java': '''package android.widget;public class FrameLayout extends android.view.ViewGroup{public FrameLayout(android.content.Context c){super(c);}public static class LayoutParams{public LayoutParams(int w,int h){}public LayoutParams(int w,int h,int g){}}}''',
'android/widget/TextView.java': '''package android.widget;public class TextView extends android.view.View{
CharSequence text="";public TextView(android.content.Context c){super(c);}public void setText(CharSequence s){text=s;}public CharSequence getText(){return text;}
public void setTextSize(float s){}public void setTextColor(int c){}public void setGravity(int g){}public void setMaxLines(int n){}
public void setVerticalScrollBarEnabled(boolean b){}public void setMovementMethod(android.text.method.ScrollingMovementMethod m){}
public android.text.Layout getLayout(){return null;}public void setTypeface(android.graphics.Typeface t,int s){}
}''',
'android/widget/Button.java': '''package android.widget;public class Button extends TextView{public Button(android.content.Context c){super(c);}public void setAllCaps(boolean b){}public void setMinWidth(int n){}public void setMinimumWidth(int n){}}''',
'android/widget/HorizontalScrollView.java': '''package android.widget;public class HorizontalScrollView extends android.view.ViewGroup{public HorizontalScrollView(android.content.Context c){super(c);}public void setHorizontalScrollBarEnabled(boolean b){}public void smoothScrollTo(int x,int y){}public static class LayoutParams{public LayoutParams(int w,int h){}}}''',
'android/widget/Toast.java': '''package android.widget;public class Toast{public static final int LENGTH_SHORT=0,LENGTH_LONG=1;public static int shown;public static Toast makeText(android.content.Context c,CharSequence t,int d){return new Toast();}public void show(){shown++;}}''',
'android/graphics/Typeface.java': '''package android.graphics;public class Typeface{public static final Typeface DEFAULT=new Typeface();public static final int BOLD=1,NORMAL=0;}''',
'android/text/method/ScrollingMovementMethod.java': '''package android.text.method;public class ScrollingMovementMethod{public static ScrollingMovementMethod getInstance(){return new ScrollingMovementMethod();}}''',
'android/view/inputmethod/InputMethodManager.java': '''package android.view.inputmethod;public class InputMethodManager{public void showInputMethodPicker(){}}''',
'android/view/KeyEvent.java': '''package android.view;public class KeyEvent{public static final int ACTION_DOWN=0,ACTION_UP=1,KEYCODE_DEL=67,KEYCODE_DPAD_LEFT=21,KEYCODE_DPAD_RIGHT=22,FLAG_SOFT_KEYBOARD=2,FLAG_KEEP_TOUCH_MODE=4;final int action,code,flags;public KeyEvent(long d,long t,int a,int c,int r,int m,int dev,int scan,int f){action=a;code=c;flags=f;}public int getAction(){return action;}public int getKeyCode(){return code;}public int getFlags(){return flags;}}''',
'android/inputmethodservice/InputMethodService.java': '''package android.inputmethodservice;
public class InputMethodService extends android.content.Context{
public android.view.inputmethod.InputConnection connection;public void onCreate(){}public android.view.View onCreateInputView(){return null;}public boolean onEvaluateFullscreenMode(){return false;}
public android.view.inputmethod.InputConnection getCurrentInputConnection(){return connection;}
public void onStartInput(android.view.inputmethod.EditorInfo i,boolean r){}public void onStartInputView(android.view.inputmethod.EditorInfo i,boolean r){}
public void onFinishInputView(boolean f){}public void onFinishInput(){}public void onConfigurationChanged(android.content.res.Configuration c){}
public void onUpdateSelection(int a,int b,int c,int d,int e,int f){}public void requestHideSelf(int f){}public void onDestroy(){}
}''',
})

SRC.mkdir(parents=True, exist_ok=True)
for name, text in SOURCES.items():
    file = SRC / name
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(text + '\n')
CLASSES = OUT / 'classes'
CLASSES.mkdir(exist_ok=True)
jar = ROOT / '.build-cache/aosp-tools/android-all.jar'
assert hashlib.sha256(jar.read_bytes()).hexdigest() == json.loads((ROOT/'docs/aosp-toolchain.lock.json').read_text())['android-all']['sha256']
core = ROOT / '.build-cache/host-tests'
java = Path('/workspace/gpd-toolchains/jdk-17.0.20.1+1/bin')
sources = sorted(SRC.rglob('*.java')) + sorted((ROOT/'app/src/main/java/dev/local/koto').glob('*.java'))
sources.append(ROOT / 'tests/ui-model/dev/local/koto/HardeningUiModelTests.java')
subprocess.run([str(java/'javac'),'--release','8','-encoding','UTF-8','-cp',f'{jar}:{core}','-d',str(CLASSES),*map(str,sources)],check=True)
subprocess.run([str(java/'java'),'-ea','-cp',f'{CLASSES}:{core}:{jar}','dev.local.koto.HardeningUiModelTests'],check=True)
