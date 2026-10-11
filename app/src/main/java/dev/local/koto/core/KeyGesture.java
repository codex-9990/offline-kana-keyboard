/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

/** Framework-free pointer state. Retains coordinates only, never typed text. */
public final class KeyGesture {
    private final boolean flick, repeatable;
    private final float threshold;
    private boolean active;
    private float startX, startY;
    private int direction;
    public KeyGesture(boolean flick, boolean repeatable, float threshold) {
        this.flick = flick; this.repeatable = repeatable; this.threshold = threshold;
    }
    public boolean active() { return active; }
    public int direction() { return direction; }
    /** Repeat keys emit their first action immediately on down. */
    public boolean begin(float x, float y) {
        active = true; direction = FlickLayout.CENTER; startX = x; startY = y;
        return repeatable;
    }
    public void move(float x, float y, boolean inside) {
        if (!active) return;
        if (!flick && !inside) { cancel(); return; }
        if (flick) direction = FlickLayout.direction(x - startX, y - startY, threshold);
    }
    public boolean repeat() { return active && repeatable; }
    /** Returns -1 if cancelled or already emitted as a repeat key. */
    public int end(float x, float y, boolean inside) {
        move(x, y, inside);
        int result = active && !repeatable ? direction : -1;
        cancel(); return result;
    }
    public void cancel() { active = false; direction = FlickLayout.CENTER; }
}
