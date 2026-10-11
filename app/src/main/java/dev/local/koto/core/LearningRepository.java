/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/** Blocking operations are owned exclusively by the coordinator's worker. */
public final class LearningRepository {
    public interface Storage {
        byte[] read() throws IOException;
        void write(byte[] data) throws IOException;
        void clear() throws IOException;
    }
    public interface Policy {
        int days(); boolean cleanupRequired();
        boolean save(int days,boolean cleanupRequired);
    }
    private final Storage storage;
    private final Policy policy;
    private final LearningData data=new LearningData();
    private int days;
    private boolean failed;
    public LearningRepository(Storage storage,Policy policy) { this.storage=storage; this.policy=policy; }
    public int days() { return failed ? 0 : days; }
    public boolean failed() { return failed; }
    public List<LearningData.Entry> entries() { return failed ? Collections.emptyList() : data.snapshot(); }
    public void load(long now) {
        days=0; failed=false;
        try {
            days=LearningData.days(policy.days());
            if (days==0 || policy.cleanupRequired()) {
                storage.clear(); data.clear();
                if (!savePolicy(days,false)) throw new IOException("Learning policy unavailable");
            } else { data.decode(storage.read()); prune(now); }
        } catch (IOException | RuntimeException ignored) { failClosed(); }
    }
    public void prune(long now) {
        if (failed) return;
        if (data.prune(days,now)) persist();
    }
    public boolean configure(int requested,long now) {
        int next=LearningData.days(requested);
        if (!failed) data.prune(days,now); // Expired entries cannot revive on period extension.
        if (!savePolicy(next,true)) { failClosed(); return false; }
        days=next; failed=false; data.prune(days,now);
        try {
            if (days==0 || data.snapshot().isEmpty()) storage.clear(); else storage.write(data.encode());
            if (!savePolicy(days,false)) throw new IOException("Learning policy unavailable");
            return true;
        } catch (IOException | RuntimeException ignored) { failClosed(); return false; }
    }
    public boolean clear() {
        data.clear();
        int retainedDays=safeDays();
        if (!savePolicy(retainedDays,true)) { failClosed(); return false; }
        try {
            storage.clear();
            if (!savePolicy(retainedDays,false)) throw new IOException("Learning policy unavailable");
            days=retainedDays; failed=false; return true;
        } catch (IOException | RuntimeException ignored) { failClosed(); return false; }
    }
    public void record(List<LearningData.Entry> values,long now) {
        if (days()==0) return;
        boolean changed=data.prune(days,now);
        try {
            for (LearningData.Entry e:values) if (LearningData.alive(e,days,now)) {
                data.record(e); changed=true;
            }
            if (changed) persist();
        } catch (IOException | RuntimeException ignored) { failClosed(); }
    }
    private void persist() {
        try {
            if (data.snapshot().isEmpty()) storage.clear(); else storage.write(data.encode());
        } catch (IOException | RuntimeException ignored) { failClosed(); }
    }
    private void failClosed() {
        failed=true; data.clear();
        // A persisted guard prevents an interrupted/failed cleanup from restoring old memory.
        savePolicy(safeDays(),true);
        try { storage.clear(); } catch (IOException | RuntimeException ignored) { /* Retry explicitly or next start. */ }
    }
    private int safeDays() {
        try { return LearningData.days(policy.days()); } catch (RuntimeException ignored) { return 0; }
    }
    private boolean savePolicy(int days,boolean cleanup) {
        try { return policy.save(days,cleanup); } catch (RuntimeException ignored) { return false; }
    }
}
