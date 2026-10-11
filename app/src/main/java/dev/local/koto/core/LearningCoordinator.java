/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.LongSupplier;

/** One worker task, at most 32 pending choices, and immutable memory-only reads. */
public final class LearningCoordinator {
    public static final int MAX_PENDING=32;
    public static final class State {
        public final int days;
        public final boolean busy,failed;
        public final long generation;
        public final List<LearningData.Entry> entries;
        private State(int days,boolean busy,boolean failed,long generation,List<LearningData.Entry> entries) {
            this.days=days; this.busy=busy; this.failed=failed; this.generation=generation;
            this.entries=Collections.unmodifiableList(new ArrayList<>(entries));
        }
        public List<LearningData.Entry> suggest(String reading,FieldPolicy policy,boolean incognito,long now) {
            return busy || failed || incognito || !policy.learning ? Collections.emptyList()
                    : LearningData.suggest(entries,reading,days,now);
        }
        public int count(long now) {
            int result=0; for (LearningData.Entry e:entries) if (LearningData.alive(e,days,now)) result++;
            return result;
        }
    }
    private final LearningRepository repository;
    private final Executor worker,notifications;
    private final LongSupplier clock;
    private final List<Runnable> listeners=new ArrayList<>();
    private final LinkedHashMap<String,LearningData.Entry> pending=new LinkedHashMap<>();
    private volatile State state=new State(0,true,false,0,Collections.emptyList());
    private boolean running,maintenance,notificationPending;
    private int control=1,requestedDays; // 1 load, 2 configure, 3 clear
    private long epoch,generation;
    public LearningCoordinator(LearningRepository repository,Executor worker,Executor notifications,LongSupplier clock) {
        this.repository=repository; this.worker=worker; this.notifications=notifications; this.clock=clock;
        schedule();
    }
    public State state() { return state; }
    public synchronized void addListener(Runnable listener) { if (!listeners.contains(listener)) listeners.add(listener); }
    public synchronized void removeListener(Runnable listener) { listeners.remove(listener); }
    public synchronized boolean configure(int days) { return request(2,days); }
    public synchronized boolean clear() { return request(3,0); }
    private boolean request(int operation,int days) {
        if (state.busy) return false;
        epoch++; pending.clear(); control=operation; requestedDays=days;
        state=new State(0,true,false,++generation,Collections.emptyList());
        notifyLater(); schedule(); return true;
    }
    public synchronized void maintain() { maintenance=true; schedule(); }
    public synchronized boolean record(String reading,String word,FieldPolicy policy,boolean incognito,boolean explicit,long expectedGeneration) {
        LearningData.Entry e=new LearningData.Entry(reading,word,clock.getAsLong());
        if (expectedGeneration!=state.generation || !explicit || incognito || !policy.learning
                || state.busy || state.failed || state.days==0 || !LearningData.valid(e))
            return false;
        String key=reading+'\0'+word;
        pending.remove(key); pending.put(key,e);
        while (pending.size()>MAX_PENDING) pending.remove(pending.keySet().iterator().next());
        schedule(); return true;
    }
    public synchronized int pendingCount() { return pending.size(); }
    private synchronized void schedule() {
        if (!running) { running=true; worker.execute(this::drain); }
    }
    private void drain() {
        while (true) {
            int operation,days; long taskEpoch;
            List<LearningData.Entry> choices;
            synchronized (this) {
                if (control==0 && !maintenance && pending.isEmpty()) { running=false; return; }
                operation=control; control=0; days=requestedDays; taskEpoch=epoch;
                choices=new ArrayList<>(pending.values()); pending.clear(); maintenance=false;
            }
            long now=clock.getAsLong();
            if (operation==1) repository.load(now);
            else if (operation==2) repository.configure(days,now);
            else if (operation==3) repository.clear();
            else if (!choices.isEmpty()) repository.record(choices,now);
            else repository.prune(now);
            synchronized (this) {
                // A clear/disable request invalidates any older publication and queued choice.
                if (taskEpoch==epoch) {
                    state=new State(repository.days(),false,repository.failed(),++generation,repository.entries());
                    notifyLater();
                }
            }
        }
    }
    private synchronized void notifyLater() {
        if (notificationPending) return;
        notificationPending=true;
        notifications.execute(()-> {
            List<Runnable> callbacks;
            synchronized (LearningCoordinator.this) { notificationPending=false; callbacks=new ArrayList<>(listeners); }
            for (Runnable callback:callbacks) callback.run();
        });
    }
}
