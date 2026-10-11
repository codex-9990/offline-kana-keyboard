/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import dev.local.koto.core.ObscuredGestureGuard;

/** Root dispatch protection, including cancellation before a release can be swallowed. */
public final class SecureTouchFrame extends FrameLayout {
    private final ObscuredGestureGuard guard=new ObscuredGestureGuard();
    private final Runnable cancelInteractions,blockedHint;
    public SecureTouchFrame(Context context,Runnable cancelInteractions,Runnable blockedHint) {
        super(context); this.cancelInteractions=cancelInteractions; this.blockedHint=blockedHint;
        setFilterTouchesWhenObscured(true);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        ObscuredGestureGuard.Decision decision=guard.event(event.getActionMasked(),event.getFlags());
        if (decision==ObscuredGestureGuard.Decision.ALLOW) return super.dispatchTouchEvent(event);
        cancelInteractions.run();
        if (decision==ObscuredGestureGuard.Decision.CANCEL_AND_REJECT) {
            deliverCleanCancel(event);
            if (blockedHint!=null) blockedHint.run();
        }
        return true;
    }
    /** A clean CANCEL reaches the active standard-control target through super dispatch. */
    private void deliverCleanCancel(MotionEvent original) {
        long now=original==null ? SystemClock.uptimeMillis() : original.getEventTime();
        MotionEvent cancel=MotionEvent.obtain(original==null ? now : original.getDownTime(),now,
                MotionEvent.ACTION_CANCEL,original==null ? 0 : original.getX(),original==null ? 0 : original.getY(),0);
        if (original!=null) cancel.setSource(original.getSource());
        try { super.dispatchTouchEvent(cancel); } finally { cancel.recycle(); }
    }
    public void resetInteraction() {
        cancelInteractions.run(); deliverCleanCancel(null); guard.reset();
    }
    @Override protected void onDetachedFromWindow() {
        resetInteraction(); super.onDetachedFromWindow();
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        if (visibility!=VISIBLE && guard!=null && cancelInteractions!=null) resetInteraction();
        super.onWindowVisibilityChanged(visibility);
    }
}
