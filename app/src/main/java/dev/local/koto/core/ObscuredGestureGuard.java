/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

/** No text or coordinates: reject an obscured gesture until a fresh DOWN or lifecycle reset. */
public final class ObscuredGestureGuard {
    public enum Decision { ALLOW, CANCEL_AND_REJECT, REJECT }
    private boolean blocked;
    public Decision event(int actionMasked,int flags) {
        if (actionMasked==0) blocked=false;
        if ((flags & 3)!=0) {
            boolean first=!blocked; blocked=true;
            return first ? Decision.CANCEL_AND_REJECT : Decision.REJECT;
        }
        return blocked ? Decision.REJECT : Decision.ALLOW;
    }
    public void reset() { blocked=false; }
}
