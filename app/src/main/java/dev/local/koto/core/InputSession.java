/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

/** Keyboard-local preedit. Host text changes only on explicit confirmation or restricted direct entry. */
public final class InputSession {
    public static final int MAX_PREEDIT_CODE_POINTS=256;
    public enum Mode { KANA, ENGLISH, SYMBOLS }
    public interface Editor {
        boolean commit(String value);
        void finish();
        void delete();
        void cursor(int direction);
        void enter();
    }
    public interface ChoiceListener { void committed(String reading,String word,FieldPolicy policy); }
    private ChoiceListener choices;
    private boolean incognito,shifted,limitReached,conversionRequested;
    private Editor editor;
    private String reading="",preview="";
    private long revision;
    private FieldPolicy policy=FieldPolicy.from(1,0);
    private Mode mode=Mode.KANA;
    public void start(Editor editor,FieldPolicy policy) {
        if (this.editor!=null) this.editor.finish();
        discard(); this.editor=editor; this.policy=policy;
        mode=policy.numeric ? Mode.SYMBOLS : policy.ascii ? Mode.ENGLISH : Mode.KANA;
    }
    public String text() { return reading; }
    public String preview() { return preview; }
    public String displayed() { return preview.isEmpty() ? reading : preview; }
    /** One immutable projection, captured only after stale learned preview cancellation. */
    public static final class Preedit {
        public final boolean visible;
        public final String text,description;
        private Preedit(boolean visible,String text,String description) {
            this.visible=visible; this.text=text; this.description=description;
        }
    }
    public Preedit preedit(boolean previewCurrent) {
        if (!previewCurrent) cancelPreview();
        if (!policy.candidates) return new Preedit(false,"","");
        String value=displayed();
        String description=value.isEmpty() ? "キーボード内の未確定入力はありません"
            : preview.isEmpty() ? "未送信の読み: "+value : "仮選択: "+value+"、読み: "+reading;
        return new Preedit(true,value,description);
    }
    public long revision() { return revision; }
    public FieldPolicy policy() { return policy; }
    public Mode mode() { return mode; }
    public boolean shifted() { return shifted; }
    public boolean incognito() { return incognito; }
    public boolean limitReached() { return limitReached; }
    public boolean conversionRequested() { return conversionRequested; }
    public void choices(ChoiceListener listener) { choices=listener; }
    public boolean incognito(boolean value) {
        if (incognito!=value) { incognito=value; cancelPreview(); conversionRequested=false; revision++; }
        return true;
    }
    public boolean syncIncognito(java.util.function.BooleanSupplier currentSetting) {
        boolean value;
        try { value=currentSetting.getAsBoolean(); } catch (RuntimeException ignored) { value=true; }
        return incognito(value);
    }
    public boolean canRequestConversion() { return policy.candidates && mode==Mode.KANA && !reading.isEmpty(); }
    public boolean hasCandidates() { return canRequestConversion() && (!policy.explicitConversion || conversionRequested); }
    /** Only the user's conversion control opens candidates in explicit-only fields. */
    public boolean requestConversion() {
        if (!canRequestConversion()) return false;
        if (policy.explicitConversion && !conversionRequested) { conversionRequested=true; revision++; }
        return true;
    }
    public void shift() { shifted=!shifted; revision++; }
    public void mode(Mode next) { cancelPreview(); conversionRequested=false; mode=next; shifted=false; revision++; }
    /** True means this key was accepted exactly once into preedit or the direct-entry host. */
    public boolean type(String value) {
        if (editor==null || value==null || value.isEmpty()) return false;
        String next=mode==Mode.ENGLISH && shifted ? value.toUpperCase(java.util.Locale.ROOT) : value;
        if (!policy.candidates) {
            if (!editor.commit(next)) return false;
            shifted=false; revision++; return true;
        }
        if (reading.codePointCount(0,reading.length())+next.codePointCount(0,next.length())>MAX_PREEDIT_CODE_POINTS) {
            limitReached=true; return false;
        }
        preview=""; reading+=next; conversionRequested=false; shifted=false; limitReached=false; revision++; return true;
    }
    public boolean preview(String value,long expectedRevision) {
        if (editor==null || expectedRevision!=revision || !hasCandidates() || value==null || value.isEmpty()) return false;
        if (!value.equals(preview)) { preview=value; revision++; }
        return true;
    }
    public boolean cancelPreview() {
        if (!preview.isEmpty()) { preview=""; revision++; }
        return true;
    }
    public void modify() {
        if (editor==null || reading.isEmpty()) return;
        cancelPreview(); String next=FlickLayout.modifyLast(reading);
        if (!next.equals(reading)) { reading=next; conversionRequested=false; revision++; }
    }
    public void backspace() {
        if (editor==null) return;
        if (!preview.isEmpty()) { cancelPreview(); return; }
        if (reading.isEmpty()) editor.delete();
        else reading=reading.substring(0,reading.offsetByCodePoints(reading.length(),-1));
        conversionRequested=false; limitReached=false; revision++;
    }
    public boolean select(String value,long expectedRevision) { return expectedRevision==revision && select(value); }
    public boolean select(String value) {
        if (editor==null || !hasCandidates() || value==null || value.isEmpty()) return false;
        if (!editor.commit(value)) return false;
        String original=reading; discard(); notifyChoice(original,value); return true;
    }
    /** Called only by an explicit confirmation control. No typing/mode/cursor/lifecycle caller. */
    public boolean confirm() {
        if (editor==null || reading.isEmpty()) return false;
        String original=reading,choice=preview;
        if (!editor.commit(displayed())) return false;
        discard(); if (!choice.isEmpty()) notifyChoice(original,choice); return true;
    }
    private void notifyChoice(String original,String word) {
        if (choices!=null && policy.learning && !incognito) choices.committed(original,word,policy);
    }
    public void space() { type(" "); }
    public void enter() { if (editor!=null) { if (!reading.isEmpty()) confirm(); else editor.enter(); } }
    public void cursor(int direction) { if (editor!=null) { editor.cursor(direction); revision++; } }
    /** Explicit cancel, hide or field end discards only unconfirmed local text. */
    public void discard() { reading=""; preview=""; conversionRequested=false; shifted=false; limitReached=false; revision++; }
    /** A relayout within the same active input preserves the draft and invalidates old buttons. */
    public void relayout() { shifted=false; revision++; }
    public void finish() { if (editor!=null) editor.finish(); discard(); editor=null; }
}
