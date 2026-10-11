/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/** Latest request only: at most one active reading, one pending reading and one delivery. */
public final class PhraseRequests {
    public interface Listener { void converted(String reading,List<String> candidates); }
    private final PhraseConverter converter;
    private final Executor worker,delivery;
    private final Listener listener;
    private long generation;
    private String pending,resultReading;
    private List<String> result;
    private long resultGeneration;
    private boolean running,deliveryPending;
    public PhraseRequests(PhraseConverter converter,Executor worker,Executor delivery,Listener listener) {
        this.converter=converter; this.worker=worker; this.delivery=delivery; this.listener=listener;
    }
    public synchronized void request(String reading) {
        if (reading==null || reading.isEmpty() || reading.length()>PhraseConverter.MAX_INPUT) { cancel(); return; }
        generation++; pending=reading;
        if (!running) { running=true; worker.execute(this::drain); }
    }
    public synchronized void cancel() { generation++; pending=null; result=null; resultReading=null; }
    private synchronized boolean current(long token) { return token==generation; }
    private void drain() {
        while (true) {
            String reading; long token;
            synchronized (this) {
                if (pending==null) { running=false; return; }
                reading=pending; pending=null; token=generation;
            }
            List<String> words=converter.convert(reading,()->current(token));
            synchronized (this) {
                if (token!=generation) continue;
                resultReading=reading; result=words; resultGeneration=token;
                if (!deliveryPending) { deliveryPending=true; delivery.execute(this::publish); }
            }
        }
    }
    private void publish() {
        String reading; List<String> words;
        synchronized (this) {
            deliveryPending=false;
            if (result==null || resultGeneration!=generation) return;
            reading=resultReading; words=result; result=null; resultReading=null;
        }
        listener.converted(reading,Collections.unmodifiableList(words));
    }
}
