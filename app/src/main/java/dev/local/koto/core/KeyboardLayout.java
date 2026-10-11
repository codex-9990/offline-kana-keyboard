/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

/** Familiar keypad rails and staggered QWERTY; original UI, no third-party branding. */
public final class KeyboardLayout {
    public enum Role { KANA,CHAR,SPACER,ENGLISH,SYMBOLS,PICKER,HIDE,MODIFY,DELETE,CONVERT,SPACE,ENTER,SHIFT }
    public static final class Key {
        public final Role role; public final String value; public final int kana; public final float weight;
        private Key(Role role,String value,int kana,float weight) { this.role=role; this.value=value; this.kana=kana; this.weight=weight; }
    }
    private static Key action(Role role,String value,float weight) { return new Key(role,value,-1,weight); }
    public static Key[] kanaRow(int row) {
        if (row<0 || row>3) throw new IllegalArgumentException("Keyboard row");
        Role[] left={Role.ENGLISH,Role.SYMBOLS,Role.PICKER,Role.HIDE};
        String[] labels={"ABC","?123","切替","隠す"};
        Role[] right={Role.DELETE,Role.CONVERT,Role.SPACE,Role.ENTER};
        String[] rightLabels={"⌫","変換","空白","↵"};
        Key[] keys=new Key[5]; keys[0]=action(left[row],labels[row],.7f);
        for (int i=0;i<3;i++) {
            int position=row*3+i;
            keys[i+1]=new Key(position==9 ? Role.MODIFY : Role.KANA,FlickLayout.kana(position,0),position,1);
        }
        keys[4]=action(right[row],rightLabels[row],.95f); return keys;
    }
    public static Key[] englishRow(int row) {
        if (row==3) return new Key[]{action(Role.SYMBOLS,"?123",1.5f),
                new Key(Role.KANA,"かな",-1,1),action(Role.CHAR,",",.8f),action(Role.SPACE,"English",4.4f),
                action(Role.CHAR,".",.8f),action(Role.ENTER,"↵",1.5f)};
        if (row<0 || row>2) throw new IllegalArgumentException("Keyboard row");
        String letters=SymbolLayout.englishRow(row); Key[] keys=new Key[letters.length()+(row==0 ? 0 : 2)];
        int offset=row==0 ? 0 : 1;
        if (row==1) { keys[0]=action(Role.SPACER,"",.5f); keys[keys.length-1]=action(Role.SPACER,"",.5f); }
        if (row==2) { keys[0]=action(Role.SHIFT,"⇧",1.5f); keys[keys.length-1]=action(Role.DELETE,"⌫",1.5f); }
        for (int i=0;i<letters.length();i++) keys[offset+i]=action(Role.CHAR,String.valueOf(letters.charAt(i)),1);
        return keys;
    }
    private KeyboardLayout() {}
}
