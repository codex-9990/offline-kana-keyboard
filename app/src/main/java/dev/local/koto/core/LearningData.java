/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Only explicit lexical choices, not an input history or a language model. */
public final class LearningData {
    public static final int MAX_ENTRIES = 128, MAX_BYTES = 32768;
    public static final long DAY = 86400000L;
    public static final class Entry {
        public final String reading, word;
        public final long selectedAt;
        public Entry(String reading, String word, long selectedAt) {
            this.reading=reading; this.word=word; this.selectedAt=selectedAt;
        }
    }
    private static final Comparator<Entry> ORDER = Comparator.comparingLong((Entry e)->e.selectedAt)
            .thenComparing(e->e.reading).thenComparing(e->e.word);
    private final List<Entry> entries = new ArrayList<>();
    public static int days(int value) { return value==1 || value==7 || value==30 || value==90 ? value : 0; }
    public static boolean valid(Entry e) {
        if (e==null || e.reading==null || e.word==null || e.selectedAt<=0) return false;
        int length=e.reading.codePointCount(0,e.reading.length());
        if (length<1 || length>32 || e.word.isEmpty() || e.word.codePointCount(0,e.word.length())>48) return false;
        for (int i=0;i<e.reading.length();i++) {
            char c=e.reading.charAt(i); if ((c<'ぁ' || c>'ゖ') && c!='ー') return false;
        }
        for (int i=0;i<e.word.length();) {
            int c=e.word.codePointAt(i); i+=Character.charCount(c);
            if (Character.isISOControl(c) || Character.getType(c)==Character.FORMAT
                    || c>=0xd800 && c<=0xdfff) return false;
        }
        return !e.word.equals(e.reading) && !e.word.equals(FlickLayout.katakana(e.reading));
    }
    public static boolean alive(Entry e,int days,long now) {
        return days>0 && valid(e) && e.selectedAt<=now && now-e.selectedAt<days*DAY;
    }
    public boolean prune(int days,long now) {
        return entries.removeIf(e->!alive(e,days,now));
    }
    public void clear() { entries.clear(); }
    public List<Entry> snapshot() { return Collections.unmodifiableList(new ArrayList<>(entries)); }
    public void record(Entry value) throws IOException {
        if (!valid(value)) return;
        entries.removeIf(e->e.reading.equals(value.reading) && e.word.equals(value.word));
        entries.add(value); entries.sort(ORDER);
        while (entries.size()>MAX_ENTRIES || encode().length>MAX_BYTES) entries.remove(0);
    }
    public byte[] encode() throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try (DataOutputStream out=new DataOutputStream(bytes)) {
            out.writeInt(0x4b4f5431); out.writeInt(entries.size());
            for (Entry e:entries) { out.writeUTF(e.reading); out.writeUTF(e.word); out.writeLong(e.selectedAt); }
        }
        return bytes.toByteArray();
    }
    public void decode(byte[] bytes) throws IOException {
        clear();
        if (bytes==null) return;
        if (bytes.length>MAX_BYTES) throw new IOException("Learning size limit");
        List<Entry> restored=new ArrayList<>();
        try (DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt()!=0x4b4f5431) throw new IOException("Learning format");
            int count=in.readInt(); if (count<0 || count>MAX_ENTRIES) throw new IOException("Learning count limit");
            for (int i=0;i<count;i++) {
                Entry e=new Entry(in.readUTF(),in.readUTF(),in.readLong());
                if (!valid(e)) throw new IOException("Invalid learning record");
                for (Entry prior:restored) if (prior.reading.equals(e.reading) && prior.word.equals(e.word))
                    throw new IOException("Duplicate learning record");
                restored.add(e);
            }
            if (in.read()!=-1) throw new IOException("Trailing learning data");
        }
        entries.addAll(restored); entries.sort(ORDER);
    }
    public static List<Entry> suggest(List<Entry> entries,String prefix,int days,long now) {
        if (prefix==null || prefix.isEmpty()) return Collections.emptyList();
        List<Entry> result=new ArrayList<>();
        for (Entry e:entries) if (e.reading.startsWith(prefix) && alive(e,days,now)) result.add(e);
        result.sort(Comparator.comparing((Entry e)->!e.reading.equals(prefix))
                .thenComparing(ORDER.reversed()));
        return Collections.unmodifiableList(result.subList(0,Math.min(8,result.size())));
    }
}
