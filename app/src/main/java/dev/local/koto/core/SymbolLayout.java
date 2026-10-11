/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

/** Both symbol pages and QWERTY rows are shared by rendering and host coverage. */
public final class SymbolLayout {
    private static final String[] ENGLISH = {"qwertyuiop","asdfghjkl","zxcvbnm"};
    private static final String[] DIGITS = {"1","2","3","4","5","6","7","8","9","0"};
    private static final String[][][] PAGES = {
        {{"!","?","#","@","%","&","*","(",")","⌫"},
         {"-","_","+","=","/","\\",":",";","'","\""}},
        {{"`","~","[","]","{","}","<",">","^","⌫"},
         {"|","$","¥","😀","、","。","「","」",",","."}}
    };
    private static final String[] BOTTOM = {"$",",","."};
    private SymbolLayout() {}
    public static int pages() { return PAGES.length; }
    public static String englishRow(int row) { return ENGLISH[row]; }
    public static String[] row(int page, int row) { return row==0 ? DIGITS.clone() : PAGES[page][row-1].clone(); }
    public static String[] bottom() { return BOTTOM.clone(); }
}
