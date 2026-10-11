/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** Bounded dictionary segmentation. Heuristic costs, not contextual linguistic conversion. */
public final class PhraseConverter {
    public static final int MAX_INPUT=64,MAX_SEGMENT=16,BEAM=4,MAX_STEPS=8192;
    private static final List<String> PARTICLES=Arrays.asList("は","が","を","に","へ","と","で","も","の","や",
            "から","まで","です","でした","ね","よ","か");
    private static final String[] POLITE={"ます","ました","ません","ましょう"};
    private static final class Path {
        final String text; final int cost,converted;
        Path(String text,int cost,int converted) { this.text=text; this.cost=cost; this.converted=converted; }
    }
    private static final Comparator<Path> ORDER=Comparator.comparingInt((Path p)->p.cost).thenComparing(p->p.text);
    private final SkkDictionary dictionary;
    public PhraseConverter(SkkDictionary dictionary) { this.dictionary=dictionary; }
    public List<String> convert(String reading,BooleanSupplier current) {
        if (reading==null || reading.isEmpty() || reading.length()>MAX_INPUT) return Collections.emptyList();
        for (int i=0;i<reading.length();i++) if (Character.isSurrogate(reading.charAt(i))) return Collections.emptyList();
        List<List<Path>> paths=new ArrayList<>();
        for (int i=0;i<=reading.length();i++) paths.add(new ArrayList<>());
        paths.get(0).add(new Path("",0,0));
        Map<String,List<String>> cache=new LinkedHashMap<>();
        long deadline=System.nanoTime()+80000000L; int steps=0;
        for (int start=0;start<reading.length();start++) {
            if (!current.getAsBoolean() || Thread.currentThread().isInterrupted()) return Collections.emptyList();
            for (int end=start+1;end<=Math.min(reading.length(),start+MAX_SEGMENT);end++) {
                if (++steps>MAX_STEPS || System.nanoTime()>deadline) return Collections.emptyList();
                String part=reading.substring(start,end);
                List<String> words=cache.computeIfAbsent(part,this::words);
                for (Path before:new ArrayList<>(paths.get(start))) {
                    if (end==start+1) add(paths.get(end),new Path(before.text+part,before.cost+18,before.converted));
                    if (PARTICLES.contains(part)) add(paths.get(end),new Path(before.text+part,before.cost+2,before.converted));
                    for (int rank=0;rank<words.size();rank++) {
                        if (++steps>MAX_STEPS) return Collections.emptyList();
                        int cost=8+(part.length()==1 ? 12 : 0)+rank+(dictionary.exactChoice(part,words.get(rank)) ? 0 : 6);
                        // Prefer a known noun plus a particle over a homophone that absorbs the particle.
                        if (part.length()>1 && PARTICLES.contains(part.substring(part.length()-1))
                                && !dictionary.exactWords(part.substring(0,part.length()-1)).isEmpty()) cost+=4;
                        add(paths.get(end),new Path(before.text+words.get(rank),before.cost+cost,before.converted+1));
                    }
                }
            }
        }
        LinkedHashSet<String> result=new LinkedHashSet<>();
        for (Path path:paths.get(reading.length())) if (path.converted>0 && !path.text.equals(reading)) result.add(path.text);
        return Collections.unmodifiableList(new ArrayList<>(result));
    }
    private static void add(List<Path> paths,Path next) {
        for (Path p:paths) if (p.text.equals(next.text) && p.cost<=next.cost) return;
        paths.removeIf(p->p.text.equals(next.text)); paths.add(next); paths.sort(ORDER);
        while (paths.size()>BEAM) paths.remove(paths.size()-1);
    }
    private List<String> words(String reading) {
        LinkedHashSet<String> result=new LinkedHashSet<>();
        addWords(result,reading);
        for (String ending:POLITE) if (reading.endsWith(ending) && reading.length()>ending.length()) {
            String stem=reading.substring(0,reading.length()-ending.length());
            for (String word:dictionary.phraseWords(stem+"る")) if (word.endsWith("る") && !word.equals(stem+"る")
                    && !word.equals(FlickLayout.katakana(stem+"る"))) result.add(word.substring(0,word.length()-1)+ending);
            String i="いきぎしちにびみり",u="うくぐすつぬぶむる";
            int p=i.indexOf(stem.charAt(stem.length()-1));
            if (p>=0) {
                String base=stem.substring(0,stem.length()-1)+u.charAt(p);
                for (String word:dictionary.phraseWords(base)) if (word.endsWith(String.valueOf(u.charAt(p)))
                        && !word.equals(base) && !word.equals(FlickLayout.katakana(base)))
                    result.add(word.substring(0,word.length()-1)+stem.charAt(stem.length()-1)+ending);
            }
        }
        List<String> limited=new ArrayList<>(result); return limited.subList(0,Math.min(3,limited.size()));
    }
    private void addWords(LinkedHashSet<String> result,String reading) {
        for (String word:dictionary.phraseWords(reading)) {
            if (!word.equals(reading) && !word.equals(FlickLayout.katakana(reading))) result.add(word);
            if (result.size()>=3) break;
        }
    }
}
