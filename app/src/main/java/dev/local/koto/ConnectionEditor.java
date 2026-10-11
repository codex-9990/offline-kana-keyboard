/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.os.SystemClock;
import android.view.KeyCharacterMap;
import dev.local.koto.core.InputSession;
import dev.local.koto.core.EditorAction;

/** Explicit host operations only. No composing span ownership, cursor cache or surrounding reads. */
public final class ConnectionEditor implements InputSession.Editor {
    interface KeySender { void send(int code,int flags); }
    private final InputConnection connection;
    private final EditorAction action;
    private final KeySender keys;
    private boolean commitRejected;
    public ConnectionEditor(InputConnection connection,EditorInfo info) {
        this(connection,info,(code,flags)->{
            if (connection==null) return;
            long time=SystemClock.uptimeMillis();
            connection.sendKeyEvent(new KeyEvent(time,time,KeyEvent.ACTION_DOWN,code,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,flags));
            connection.sendKeyEvent(new KeyEvent(time,time,KeyEvent.ACTION_UP,code,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,flags));
        });
    }
    ConnectionEditor(InputConnection connection,EditorInfo info,KeySender keys) {
        this.connection=connection; this.keys=keys; action=EditorAction.from(info.imeOptions,info.actionId,info.actionLabel);
    }
    /** Untagged selection updates never discard, send or lock the keyboard-local draft. */
    public void updateSelection(int oldStart,int oldEnd,int newStart,int newEnd,int candidatesStart,int candidatesEnd) { }
    public EditorAction action() { return action; }
    public boolean commitRejected() { return commitRejected; }
    @Override public boolean commit(String value) {
        if (connection==null) { commitRejected=true; return false; }
        try {
            // End any host-owned composition without replacing it. Then insert at
            // the host's actual selection, never an inferred or restored range.
            if (!connection.finishComposingText()) { commitRejected=true; return false; }
            boolean accepted=connection.commitText(value,1); commitRejected=!accepted; return accepted;
        } catch (AbstractMethodError | RuntimeException ignored) { commitRejected=true; return false; }
    }
    @Override public void finish() {
        if (connection!=null) try { connection.finishComposingText(); }
        catch (AbstractMethodError | RuntimeException ignored) { /* No input logging. */ }
    }
    private void key(int code) {
        if (connection==null) return;
        finish(); keys.send(code,KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE);
    }
    @Override public void delete() { key(KeyEvent.KEYCODE_DEL); }
    @Override public void cursor(int direction) { key(direction<0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT); }
    @Override public void enter() {
        if (connection==null) return;
        try { if (action.enabled() && connection.performEditorAction(action.id())) return; }
        catch (AbstractMethodError | RuntimeException ignored) { /* Explicit newline fallback. */ }
        commit("\n");
    }
}
