/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

public final class FlickSensitivity {
    public static final int MIN_DP=8, MAX_DP=40, DEFAULT_DP=22;
    private FlickSensitivity() {}
    public static int clamp(int value) { return Math.max(MIN_DP,Math.min(MAX_DP,value)); }
    public static float pixels(int value,float density) {
        if (!Float.isFinite(density) || density<=0) density=1;
        return clamp(value)*density;
    }
}
