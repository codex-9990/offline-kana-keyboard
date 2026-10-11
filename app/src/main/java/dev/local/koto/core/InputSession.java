/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

/** Input state only lives in RAM. No logging, history, persistence or learning. */
public final class InputSession {
    public enum Mode { KANA, ENGLISH, SYMBOLS }
    public interface Editor {
        boolean composing(String value);
        boolean commit(String value);
        void finish();
        void delete();
        void cursor(int direction);
        void enter();
    }
    private Editor editor;
    private String composing = "";
    private FieldPolicy policy = FieldPolicy.from(1, 0);
    private Mode mode = Mode.KANA;
    private boolean shifted;
    public void start(Editor editor, FieldPolicy policy) {
        // Never move a previous editor's composition to a newly focused editor.
        composing = ""; shifted = false; this.editor = editor; this.policy = policy;
        mode = policy.numeric ? Mode.SYMBOLS : policy.ascii ? Mode.ENGLISH : Mode.KANA;
    }
    public String text() { return composing; }
    public FieldPolicy policy() { return policy; }
    public Mode mode() { return mode; }
    public boolean shifted() { return shifted; }
    public boolean hasCandidates() { return policy.candidates && mode == Mode.KANA && !composing.isEmpty(); }
    public void shift() { shifted = !shifted; }
    public void mode(Mode next) {
        commitComposition();
        if (!composing.isEmpty()) return;
        mode = next; shifted = false;
    }
    public void type(String value) {
        if (editor == null || value == null || value.isEmpty()) return;
        if (mode == Mode.KANA && policy.candidates) {
            if (composing.codePointCount(0, composing.length()) >= 64) {
                commitComposition();
                if (!composing.isEmpty()) return;
            }
            String next = composing + value;
            if (editor.composing(next)) composing = next;
        } else {
            String next = mode == Mode.ENGLISH && shifted ? value.toUpperCase(java.util.Locale.ROOT) : value;
            editor.commit(next); shifted = false;
        }
    }
    public void modify() {
        if (editor == null || composing.isEmpty()) return;
        String next = FlickLayout.modifyLast(composing);
        if (editor.composing(next)) composing = next;
    }
    public void backspace() {
        if (editor == null) return;
        if (composing.isEmpty()) { editor.delete(); return; }
        String next = composing.substring(0, composing.offsetByCodePoints(composing.length(), -1));
        if (editor.composing(next)) {
            composing = next;
            if (next.isEmpty()) editor.finish();
        }
    }
    public void select(String value) {
        if (editor == null || composing.isEmpty() || !hasCandidates()) return;
        if (editor.commit(value)) { composing = ""; editor.finish(); }
    }
    public void commitComposition() {
        if (editor == null || composing.isEmpty()) return;
        if (editor.commit(composing)) { composing = ""; editor.finish(); }
    }
    public void space() {
        if (editor == null) return;
        commitComposition();
        if (composing.isEmpty()) editor.commit(" ");
    }
    public void enter() {
        if (editor == null) return;
        if (!composing.isEmpty()) commitComposition(); else editor.enter();
    }
    public void cursor(int direction) {
        if (editor == null) return;
        commitComposition();
        if (composing.isEmpty()) editor.cursor(direction);
    }
    public void selectionMovedOutsideComposition() {
        if (editor != null && !composing.isEmpty()) editor.finish();
        composing = "";
    }
    public void finish() {
        if (editor != null) editor.finish();
        composing = ""; shifted = false; editor = null;
    }
}
