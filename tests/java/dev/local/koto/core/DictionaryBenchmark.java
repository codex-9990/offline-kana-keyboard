/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.io.FileInputStream;
import java.util.Locale;

/** Synthetic host measurements only. These do not predict device IME latency. */
public final class DictionaryBenchmark {
    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();
        SkkDictionary dictionary = SkkDictionary.load(new FileInputStream(args[0]));
        long loaded = System.nanoTime();
        String[] readings = {"にほん", "とうきょう", "かんじ", "がっこう", "はし", "たべる", "かく", "あぴぬめそ"};
        int checksum = 0;
        for (int i=0;i<1000;i++) checksum += dictionary.candidates(readings[i % readings.length]).size();
        long[] samples = new long[10000];
        for (int i=0;i<samples.length;i++) {
            long before = System.nanoTime();
            checksum += dictionary.candidates(readings[i % readings.length]).size();
            samples[i] = System.nanoTime() - before;
        }
        java.util.Arrays.sort(samples);
        System.out.printf(Locale.ROOT, "Synthetic JVM host benchmark: %d keys; load %.3f ms; lookup median %.3f ms; p95 %.3f ms; checksum %d%n",
                dictionary.size(), (loaded-start)/1000000d, samples[5000]/1000000d, samples[9500]/1000000d, checksum);
        System.out.println("Android touch-to-text, cold IME startup and S24 performance: NOT RUN");
    }
}
