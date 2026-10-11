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
import dev.local.koto.core.FlickLayout;

/** A pointer gesture emits one character on release; cancelled gestures emit none. */
public final class KeyView extends View {
    public interface Listener { void press(int direction); }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private String label;
    private final String[] flick;
    private final Listener listener;
    private final boolean haptics, repeatable, accent;
    private boolean down, repeated;
    private float startX, startY;
    private int direction, clickDirection;
    private final Runnable repeat = new Runnable() {
        @Override public void run() {
            if (!down) return;
            repeated = true; listener.press(0); postDelayed(this, 85);
        }
    };
    public KeyView(Context context, String label, String[] flick, Listener listener,
                   boolean haptics, boolean repeatable, boolean accent) {
        super(context);
        this.label = label; this.flick = flick; this.listener = listener;
        this.haptics = haptics; this.repeatable = repeatable; this.accent = accent;
        setClickable(true); setFocusable(true); setFilterTouchesWhenObscured(true);
        setContentDescription(flick == null ? label : label + "。左 " + flick[1]
                + "、上 " + flick[2] + "、右 " + flick[3] + "、下 " + flick[4]);
    }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    private float sp(float value) { return value * getResources().getDisplayMetrics().scaledDensity; }
    public void label(String value) { label = value; setContentDescription(value); invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        rect.set(dp(2), dp(2), getWidth() - dp(2), getHeight() - dp(2));
        paint.setColor(down ? 0xff467467 : accent ? 0xff8ddfc7 : 0xff2d3e49);
        canvas.drawRoundRect(rect, dp(9), dp(9), paint);
        paint.setColor(accent ? 0xff102b25 : 0xfff4f4ee);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(sp(flick == null && label.length() > 3 ? 13 : 21));
        String main = flick != null && down ? flick[direction] : label;
        canvas.drawText(main, getWidth() / 2f, getHeight() / 2f - (paint.ascent() + paint.descent()) / 2f, paint);
        if (flick != null && !down) {
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
        if (event.getPointerCount() > 1) { cancel(); return true; }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                down = true; repeated = false; direction = 0;
                startX = event.getX(); startY = event.getY();
                if (haptics) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if (repeatable) postDelayed(repeat, 450);
                invalidate(); return true;
            case MotionEvent.ACTION_MOVE:
                if (down && flick != null) direction = FlickLayout.direction(event.getX() - startX,
                        event.getY() - startY, dp(22));
                invalidate(); return true;
            case MotionEvent.ACTION_UP:
                if (!down) return true;
                if (flick != null) direction = FlickLayout.direction(event.getX() - startX,
                        event.getY() - startY, dp(22));
                clickDirection = direction;
                if (!repeated) performClick();
                clickDirection = 0; cancel(); return true;
            case MotionEvent.ACTION_CANCEL:
                cancel(); return true;
            default: return true;
        }
    }
    private void cancel() { down = false; removeCallbacks(repeat); invalidate(); }
    @Override public boolean performClick() {
        super.performClick(); listener.press(clickDirection); return true;
    }
    @Override protected void onDetachedFromWindow() { cancel(); super.onDetachedFromWindow(); }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.Button");
        if (flick != null) for (int i = 1; i <= 4; i++)
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(0x01000000 + i, flick[i]));
    }
    @Override public boolean performAccessibilityAction(int action, Bundle args) {
        if (flick != null && action > 0x01000000 && action <= 0x01000004) {
            listener.press(action - 0x01000000); return true;
        }
        return super.performAccessibilityAction(action, args);
    }
}
