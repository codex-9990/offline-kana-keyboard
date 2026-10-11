/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.view.KeyEvent;
import android.view.KeyCharacterMap;
import android.os.SystemClock;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import dev.local.koto.core.InputSession;
import dev.local.koto.core.EditorAction;
import dev.local.koto.core.EditorSelection;

/** The host editor is the only destination for typed text. Never reads surroundings. */
public final class ConnectionEditor implements InputSession.Editor {
    interface KeySender { void send(int code, int flags); }
    private final InputConnection connection;
    private final EditorAction action;
    private final KeySender keys;
    private boolean selectionActive;
    private final EditorSelection selections;
    private boolean compositionRejected;
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
        selections=new EditorSelection(info.initialSelStart,info.initialSelEnd);
        selection(info.initialSelStart, info.initialSelEnd);
    }
    public void selection(int start, int end) {
        selections.select(start,end); selectionActive=selections.hasSelection();
    }
    /** Service callback path, also exercised with a real-signature synthetic editor. */
    public boolean updateSelection(InputSession session,int oldStart,int oldEnd,int newStart,int newEnd,int start,int end) {
        boolean outside=selections.update(oldStart,oldEnd,newStart,newEnd,start,end);
        selectionActive=selections.hasSelection();
        if (!session.text().isEmpty() && outside) {
            session.selectionMovedOutsideComposition(); return true;
        }
        return false;
    }
    public EditorAction action() { return action; }
    public boolean compositionRejected() { return compositionRejected; }
    @Override public boolean composing(String value) {
        return write(value,true);
    }
    @Override public boolean commit(String value) {
        return write(value,false);
    }
    private boolean write(String value,boolean composing) {
        if (connection==null) { compositionRejected=composing; return false; }
        boolean recover=selections.recoverRegion();
        if (selections.unknownMissingRegion()) { compositionRejected=true; return false; }
        boolean batch=false;
        EditorSelection.Write expected=null;
        EditorSelection.Write regionExpected=null;
        try {
            if (recover) {
                batch=true; connection.beginBatchEdit();
                int first=selections.spanStart(),last=selections.spanEnd();
                regionExpected=selections.expect(last-first,true);
                if (!connection.setComposingRegion(first,last)) {
                    selections.reject(regionExpected); regionExpected=null;
                    compositionRejected=true; return false;
                }
                regionExpected=null; // Successful restoration is a real expected update.
            }
            expected=selections.expect(value.length(),composing);
            boolean success=composing ? connection.setComposingText(value,1) : connection.commitText(value,1);
            if (success) { selectionActive=false; compositionRejected=false; }
            else { selections.reject(expected); compositionRejected=composing || recover; }
            return success;
        } catch (AbstractMethodError | RuntimeException ignored) {
            if (expected!=null) selections.reject(expected);
            if (regionExpected!=null) selections.reject(regionExpected);
            compositionRejected=true; return false;
        } finally {
            if (batch) {
                try { connection.endBatchEdit(); } catch (AbstractMethodError | RuntimeException ignored) { /* No input log. */ }
            }
        }
    }
    @Override public void finish() {
        selections.finish();
        if (connection != null) connection.finishComposingText();
    }
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
            try { if (connection.deleteSurroundingTextInCodePoints(1, 0)) { selections.select(-1,-1); return; } }
            catch (AbstractMethodError ignored) { /* Legacy editor: use its delete key path. */ }
        }
        selections.select(-1,-1);
        key(KeyEvent.KEYCODE_DEL);
    }
    @Override public void cursor(int direction) {
        selections.select(-1,-1); key(direction < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
    }
    @Override public void enter() {
        if (connection == null) return;
        if (action.enabled() && connection.performEditorAction(action.id())) return;
        commit("\n");
    }
}
