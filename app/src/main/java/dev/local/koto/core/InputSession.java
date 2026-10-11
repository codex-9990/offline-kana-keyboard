/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

/** Composition only lives in RAM. A successful explicit choice can notify local learning. */
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
    public interface ChoiceListener { void committed(String reading,String word,FieldPolicy policy); }
    private ChoiceListener choices;
    private boolean incognito;
    private Editor editor;
    private String composing = "", preview = "";
    private long revision;
    private FieldPolicy policy = FieldPolicy.from(1, 0);
    private Mode mode = Mode.KANA;
    private boolean shifted;
    public void start(Editor editor, FieldPolicy policy) {
        // Never move a previous editor's composition to a newly focused editor.
        if (this.editor != null) this.editor.finish();
        composing = ""; preview = ""; shifted = false; this.editor = editor; this.policy = policy;
        mode = policy.numeric ? Mode.SYMBOLS : policy.ascii ? Mode.ENGLISH : Mode.KANA;
        revision++;
    }
    public String text() { return composing; }
    public String preview() { return preview; }
    public long revision() { return revision; }
    public FieldPolicy policy() { return policy; }
    public Mode mode() { return mode; }
    public boolean shifted() { return shifted; }
    public boolean incognito() { return incognito; }
    public void choices(ChoiceListener listener) { choices=listener; }
    public boolean incognito(boolean value) {
        if (incognito==value) return true;
        incognito=value; boolean restored=cancelPreview(); revision++; return restored;
    }
    /** Re-read current settings even when Android returns to the same active editor. */
    public boolean syncIncognito(java.util.function.BooleanSupplier currentSetting) {
        boolean value;
        try { value=currentSetting.getAsBoolean(); } catch (RuntimeException ignored) { value=true; }
        return incognito(value);
    }
    public boolean hasCandidates() { return policy.candidates && mode == Mode.KANA && !composing.isEmpty(); }
    public void shift() { shifted = !shifted; revision++; }
    public void mode(Mode next) {
        commitComposition();
        if (!composing.isEmpty()) return;
        mode = next; shifted = false; revision++;
    }
    public void type(String value) {
        if (editor == null || value == null || value.isEmpty()) return;
        // Continuing to type confirms the visibly selected word before a new reading.
        if (!preview.isEmpty()) {
            commitComposition();
            if (!composing.isEmpty()) return;
        }
        if (mode == Mode.KANA && policy.candidates) {
            if (composing.codePointCount(0, composing.length()) >= 64) {
                commitComposition();
                if (!composing.isEmpty()) return;
            }
            String next = composing + value;
            if (editor.composing(next)) { composing = next; revision++; }
        } else {
            String next = mode == Mode.ENGLISH && shifted ? value.toUpperCase(java.util.Locale.ROOT) : value;
            if (editor.commit(next)) { shifted = false; revision++; }
        }
    }
    public boolean preview(String value, long expectedRevision) {
        if (expectedRevision != revision || !hasCandidates() || value == null || value.isEmpty()) return false;
        if (value.equals(preview)) return true;
        if (!editor.composing(value)) return false;
        preview = value; revision++; return true;
    }
    public boolean cancelPreview() {
        if (preview.isEmpty()) return true;
        if (!editor.composing(composing)) return false;
        preview = ""; revision++; return true;
    }
    public void modify() {
        if (editor == null || composing.isEmpty()) return;
        if (!cancelPreview()) return;
        String next = FlickLayout.modifyLast(composing);
        if (!next.equals(composing) && editor.composing(next)) { composing = next; revision++; }
    }
    public void backspace() {
        if (editor == null) return;
        // First backspace cancels conversion; the next deletes a reading code point.
        if (!preview.isEmpty()) { cancelPreview(); return; }
        if (composing.isEmpty()) { editor.delete(); revision++; return; }
        String next = composing.substring(0, composing.offsetByCodePoints(composing.length(), -1));
        if (editor.composing(next)) {
            composing = next; revision++;
            if (next.isEmpty()) editor.finish();
        }
    }
    public boolean select(String value, long expectedRevision) {
        return expectedRevision == revision && select(value);
    }
    public boolean select(String value) {
        if (editor == null || !hasCandidates() || value == null || value.isEmpty()) return false;
        if (!editor.commit(value)) return false;
        String reading=composing;
        composing = ""; preview = ""; revision++; editor.finish();
        notifyChoice(reading,value); return true;
    }
    public void commitComposition() {
        commitComposition(false);
    }
    private void commitComposition(boolean explicit) {
        if (editor == null || composing.isEmpty()) return;
        String reading=composing,choice=preview;
        if (editor.commit(choice.isEmpty() ? composing : choice)) {
            composing = ""; preview = ""; revision++; editor.finish();
            if (explicit && !choice.isEmpty()) notifyChoice(reading,choice);
        }
    }
    private void notifyChoice(String reading,String word) {
        if (choices!=null && policy.learning && !incognito) choices.committed(reading,word,policy);
    }
    public void space() {
        if (editor == null) return;
        commitComposition(true);
        if (composing.isEmpty()) editor.commit(" ");
    }
    public void enter() {
        if (editor == null) return;
        if (!composing.isEmpty()) commitComposition(true); else editor.enter();
    }
    public void cursor(int direction) {
        if (editor == null) return;
        commitComposition();
        if (composing.isEmpty()) editor.cursor(direction);
    }
    public void selectionMovedOutsideComposition() {
        if (editor != null && !composing.isEmpty()) editor.finish();
        composing = ""; preview = ""; shifted = false; revision++;
    }
    public void finish() {
        if (editor != null) editor.finish();
        composing = ""; preview = ""; shifted = false; editor = null; revision++;
    }
}
