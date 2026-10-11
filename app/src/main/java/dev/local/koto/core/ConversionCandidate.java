/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public final class ConversionCandidate {
    public final String word,learningReading;
    public final boolean learned,eligible,phrase;
    private ConversionCandidate(String word,String reading,boolean learned,boolean eligible) {
        this(word,reading,learned,eligible,false);
    }
    private ConversionCandidate(String word,String reading,boolean learned,boolean eligible,boolean phrase) {
        this.word=word; learningReading=reading; this.learned=learned; this.eligible=eligible; this.phrase=phrase;
    }
    public static List<ConversionCandidate> combine(String reading,List<String> dictionary,List<LearningData.Entry> learned) {
        return combine(reading,dictionary,learned,Collections.emptyList(),dictionary);
    }
    public static List<ConversionCandidate> combine(String reading,List<String> dictionary,List<LearningData.Entry> learned,
                                                     List<String> phrases,List<String> exact) {
        String kana=FlickLayout.katakana(reading);
        int wordLimit=reading.equals(kana) ? 63 : 62;
        LinkedHashMap<String,ConversionCandidate> words=new LinkedHashMap<>();
        for (LearningData.Entry e:learned) if (!e.word.equals(reading) && !e.word.equals(kana))
            words.putIfAbsent(e.word,new ConversionCandidate(e.word,e.reading,true,true));
        for (String word:exact) if (!word.equals(reading) && !word.equals(kana) && words.size()<wordLimit)
            words.putIfAbsent(word,new ConversionCandidate(word,reading,false,true));
        for (String word:phrases) if (!word.equals(reading) && !word.equals(kana) && words.size()<wordLimit)
            words.putIfAbsent(word,new ConversionCandidate(word,reading,false,false,true));
        for (String word:dictionary) if (!word.equals(reading) && !word.equals(kana) && words.size()<wordLimit)
            words.putIfAbsent(word,new ConversionCandidate(word,reading,false,false));
        List<ConversionCandidate> result=new ArrayList<>(words.values());
        if (result.size()>wordLimit) result=new ArrayList<>(result.subList(0,wordLimit));
        result.add(new ConversionCandidate(reading,reading,false,false));
        if (!kana.equals(reading)) result.add(new ConversionCandidate(kana,reading,false,false));
        return Collections.unmodifiableList(result);
    }
}
