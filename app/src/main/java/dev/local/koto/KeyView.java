/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import dev.local.koto.core.KeyGesture;
import dev.local.koto.core.FlickSensitivity;

/** A pointer gesture emits one character on release; cancelled gestures emit none. */
public final class KeyView extends View {
    public interface Listener { void press(int direction); }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private String label;
    private final String[] flick;
    private final Listener listener;
    private final boolean haptics, repeatable, accent;
    private final KeyGesture gesture;
    private int clickDirection;
    private final Runnable repeat = new Runnable() {
        @Override public void run() {
            if (!gesture.repeat()) return;
            if (!isEnabled() || !isShown()) { cancelGesture(); return; }
            listener.press(0);
            if (gesture.repeat()) postDelayed(this, 85);
        }
    };
    public KeyView(Context context, String label, String[] flick, Listener listener,
                   boolean haptics, boolean repeatable, boolean accent) {
        this(context,label,flick,listener,haptics,repeatable,accent,FlickSensitivity.DEFAULT_DP);
    }
    public KeyView(Context context, String label, String[] flick, Listener listener,
                   boolean haptics, boolean repeatable, boolean accent,int flickDistanceDp) {
        super(context);
        this.label = label; this.flick = flick; this.listener = listener;
        this.haptics = haptics; this.repeatable = repeatable; this.accent = accent;
        gesture = new KeyGesture(flick != null, repeatable,FlickSensitivity.pixels(flickDistanceDp,getResources().getDisplayMetrics().density));
        setClickable(true); setFocusable(true); setFilterTouchesWhenObscured(true);
        setContentDescription(flick == null ? label : label + "。左 " + flick[1]
                + "、上 " + flick[2] + "、右 " + flick[3] + "、下 " + flick[4]);
    }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    private float sp(float value) { return value * getResources().getDisplayMetrics().scaledDensity; }
    public void label(String value) { label = value; setContentDescription(value); invalidate(); }
    public void flickDistance(int distanceDp) {
        removeCallbacks(repeat); clickDirection=0;
        gesture.threshold(FlickSensitivity.pixels(distanceDp,getResources().getDisplayMetrics().density)); invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        rect.set(dp(2), dp(2), getWidth() - dp(2), getHeight() - dp(2));
        paint.setColor(gesture.active() ? 0xff467467 : accent ? 0xff8ddfc7 : 0xff2d3e49);
        canvas.drawRoundRect(rect, dp(9), dp(9), paint);
        paint.setColor(accent ? 0xff102b25 : 0xfff4f4ee);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(sp(flick == null && label.length() > 3 ? 13 : 21));
        String main = flick != null && gesture.active() ? flick[gesture.direction()] : label;
        String[] lines = main.split("\n",-1);
        if (lines.length > 1) paint.setTextSize(sp(13));
        for (String line : lines) {
            float measured = paint.measureText(line), width = Math.max(dp(8),getWidth()-dp(12));
            if (measured > width) paint.setTextSize(paint.getTextSize()*width/measured);
        }
        float lineHeight = paint.descent()-paint.ascent();
        for (int i=0;i<lines.length;i++) canvas.drawText(lines[i],getWidth()/2f,
                getHeight()/2f-(paint.ascent()+paint.descent())/2f+(i-(lines.length-1)/2f)*lineHeight,paint);
        if (flick != null && !gesture.active()) {
            paint.setTextSize(sp(10)); paint.setColor(0xffb5c8c9);
            float middle = getHeight() / 2f - (paint.ascent() + paint.descent()) / 2f;
            canvas.drawText(flick[1], dp(13), middle, paint);
            canvas.drawText(flick[2], getWidth() / 2f, dp(14), paint);
            canvas.drawText(flick[3], getWidth() - dp(13), middle, paint);
            canvas.drawText(flick[4], getWidth() / 2f, getHeight() - dp(7), paint);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        if (event.getPointerCount() > 1) { cancelGesture(); return true; }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                boolean emitNow = gesture.begin(event.getX(), event.getY());
                if (haptics) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if (emitNow) { clickDirection = 0; performClick(); }
                if (repeatable && gesture.active()) postDelayed(repeat, 450);
                invalidate(); return true;
            case MotionEvent.ACTION_MOVE:
                gesture.move(event.getX(), event.getY(), inside(event));
                if (!gesture.active()) removeCallbacks(repeat);
                invalidate(); return true;
            case MotionEvent.ACTION_UP:
                int releaseDirection = gesture.end(event.getX(), event.getY(), inside(event));
                removeCallbacks(repeat);
                if (releaseDirection >= 0) { clickDirection = releaseDirection; performClick(); }
                clickDirection = 0; invalidate(); return true;
            case MotionEvent.ACTION_CANCEL:
                cancelGesture(); return true;
            default: return true;
        }
    }
    private boolean inside(MotionEvent event) {
        return event.getX() >= 0 && event.getX() < getWidth() && event.getY() >= 0 && event.getY() < getHeight();
    }
    public void cancelGesture() {
        if (gesture != null) gesture.cancel();
        clickDirection = 0; removeCallbacks(repeat); invalidate();
    }
    @Override public boolean performClick() {
        if (!isEnabled()) return false;
        super.performClick(); listener.press(clickDirection); return true;
    }
    @Override public void setEnabled(boolean enabled) {
        if (!enabled && gesture!=null) cancelGesture();
        super.setEnabled(enabled);
    }
    @Override protected void onVisibilityChanged(View changedView,int visibility) {
        if (visibility!=VISIBLE && gesture!=null) cancelGesture();
        super.onVisibilityChanged(changedView,visibility);
    }
    @Override protected void onDetachedFromWindow() { cancelGesture(); super.onDetachedFromWindow(); }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        if (visibility != VISIBLE) cancelGesture();
        super.onWindowVisibilityChanged(visibility);
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.Button");
        if (flick != null) for (int i = 1; i <= 4; i++)
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(0x01000000 + i, flick[i]));
    }
    @Override public boolean performAccessibilityAction(int action, Bundle args) {
        if (!isEnabled()) return false;
        if (flick != null && action > 0x01000000 && action <= 0x01000004) {
            listener.press(action - 0x01000000); return true;
        }
        return super.performAccessibilityAction(action, args);
    }
}
