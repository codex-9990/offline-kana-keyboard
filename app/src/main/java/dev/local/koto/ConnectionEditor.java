/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.view.KeyEvent;
import android.view.KeyCharacterMap;
import android.os.SystemClock;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import dev.local.koto.core.InputSession;
import dev.local.koto.core.EditorAction;

/** The host editor is the only destination for typed text. Never reads surroundings. */
public final class ConnectionEditor implements InputSession.Editor {
    interface KeySender { void send(int code, int flags); }
    private final InputConnection connection;
    private final EditorAction action;
    private final KeySender keys;
    private boolean selectionActive;
    public ConnectionEditor(InputConnection connection, EditorInfo info) {
        this(connection, info, (code,flags) -> {
            if (connection == null) return;
            long time = SystemClock.uptimeMillis();
            connection.sendKeyEvent(new KeyEvent(time,time,KeyEvent.ACTION_DOWN,code,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,flags));
            connection.sendKeyEvent(new KeyEvent(time,time,KeyEvent.ACTION_UP,code,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,flags));
        });
    }
    ConnectionEditor(InputConnection connection, EditorInfo info, KeySender keys) {
        this.connection = connection;
        this.keys = keys;
        action = EditorAction.from(info.imeOptions,info.actionId,info.actionLabel);
        selection(info.initialSelStart, info.initialSelEnd);
    }
    public void selection(int start, int end) { selectionActive = start >= 0 && end >= 0 && start != end; }
    public EditorAction action() { return action; }
    @Override public boolean composing(String value) {
        boolean success = connection != null && connection.setComposingText(value, 1);
        if (success) selectionActive = false;
        return success;
    }
    @Override public boolean commit(String value) {
        boolean success = connection != null && connection.commitText(value, 1);
        if (success) selectionActive = false;
        return success;
    }
    @Override public void finish() { if (connection != null) connection.finishComposingText(); }
    private void key(int code) {
        if (connection == null) return;
        keys.send(code,KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE);
    }
    @Override public void delete() {
        if (connection == null) return;
        // Use selection metadata already supplied by Android; never read host text.
        if (selectionActive) {
            if (commit("")) return;
        } else {
            try { if (connection.deleteSurroundingTextInCodePoints(1, 0)) return; }
            catch (AbstractMethodError ignored) { /* Legacy editor: use its delete key path. */ }
        }
        key(KeyEvent.KEYCODE_DEL);
    }
    @Override public void cursor(int direction) { key(direction < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT); }
    @Override public void enter() {
        if (connection == null) return;
        if (action.enabled() && connection.performEditorAction(action.id())) return;
        connection.commitText("\n", 1);
    }
}
