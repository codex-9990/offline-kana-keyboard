/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import dev.local.koto.core.InputSession;

/** The host editor is the only destination for typed text. Never reads surroundings. */
public final class ConnectionEditor implements InputSession.Editor {
    private final InputConnection connection;
    private final int options, actionId;
    public ConnectionEditor(InputConnection connection, EditorInfo info) {
        this.connection = connection;
        options = info.imeOptions;
        actionId = info.actionLabel != null && info.actionId != 0 ? info.actionId : options & EditorInfo.IME_MASK_ACTION;
    }
    @Override public boolean composing(String value) { return connection != null && connection.setComposingText(value, 1); }
    @Override public boolean commit(String value) { return connection != null && connection.commitText(value, 1); }
    @Override public void finish() { if (connection != null) connection.finishComposingText(); }
    private void key(int code) {
        if (connection == null) return;
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }
    @Override public void delete() { key(KeyEvent.KEYCODE_DEL); }
    @Override public void cursor(int direction) { key(direction < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT); }
    @Override public void enter() {
        if (connection == null) return;
        if ((options & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0
                && actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED
                && connection.performEditorAction(actionId)) return;
        connection.commitText("\n", 1);
    }
}
