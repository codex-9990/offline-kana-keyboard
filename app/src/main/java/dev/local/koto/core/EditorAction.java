/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

/** One action plan for the displayed Enter key and the command sent to the host. */
public final class EditorAction {
    private final int id;
    private final String label, description;
    private EditorAction(int id, String label, String description) {
        this.id = id; this.label = label; this.description = description;
    }
    public int id() { return id; }
    public boolean enabled() { return id > 1; }
    public String label() { return label; }
    public String description() { return description; }
    public static EditorAction from(int options, int customId, CharSequence customLabel) {
        if ((options & 0x40000000) != 0) return new EditorAction(1,"↵","改行");
        String name = clean(customLabel);
        if (customId > 1 && !name.isEmpty()) {
            String visible = name;
            if (name.codePointCount(0,name.length()) > 6)
                visible = name.substring(0,name.offsetByCodePoints(0,6)) + "…";
            return new EditorAction(customId,"実行\n" + visible,"入力先アプリの操作: " + name);
        }
        int action = options & 0xff;
        String[] labels = {"↵","↵","移動","検索","送信","次へ","完了","前へ"};
        if (action < 2 || action >= labels.length) return new EditorAction(1,"↵","改行");
        return new EditorAction(action,labels[action],labels[action]);
    }
    private static String clean(CharSequence input) {
        if (input == null) return "";
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (int i=0;i<Math.min(input.length(),128) && count<32;) {
            int c = Character.codePointAt(input,i); i += Character.charCount(c);
            if (Character.isISOControl(c) || (c>=0x200b && c<=0x200f) || (c>=0x202a && c<=0x202e)
                    || (c>=0x2060 && c<=0x206f) || c==0xfeff || (c>=0xd800 && c<=0xdfff)) continue;
            result.appendCodePoint(c); count++;
        }
        return result.toString().trim();
    }
}
