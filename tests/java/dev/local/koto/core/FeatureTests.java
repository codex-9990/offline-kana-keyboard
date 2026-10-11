/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.io.IOException;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/** Synthetic fixtures only; production core and real bounded file adapter. */
public final class FeatureTests {
    private static int count;
    private interface Test { void run() throws Exception; }
    private static void test(String name,Test body) throws Exception { body.run(); count++; System.out.println("PASS: "+name); }
    private static void yes(boolean b) { if (!b) throw new AssertionError("Expected true"); }
    private static void no(boolean b) { yes(!b); }
    private static void eq(Object a,Object b) { if (!a.equals(b)) throw new AssertionError("Expected "+a+"; got "+b); }
    private static final FieldPolicy NORMAL=FieldPolicy.from(1,0);
    private static String reading(int i) {
        String chars="あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん";
        return "か"+chars.charAt(i%chars.length())+chars.charAt(i/chars.length()%chars.length());
    }
    private static String repeat(String s,int n) { StringBuilder b=new StringBuilder(); for (int i=0;i<n;i++) b.append(s); return b.toString(); }
    private static final class Queue implements Executor {
        final ArrayDeque<Runnable> tasks=new ArrayDeque<>();
        @Override public void execute(Runnable task) { tasks.add(task); }
        void run() { int n=0; while (!tasks.isEmpty()) { if (++n>1000) throw new AssertionError("Unbounded tasks"); tasks.remove().run(); } }
    }
    private static final class Policy implements LearningRepository.Policy {
        int days; boolean cleanup,accept=true;
        @Override public int days() { return days; }
        @Override public boolean cleanupRequired() { return cleanup; }
        @Override public boolean save(int d,boolean c) { if (!accept) return false; days=d; cleanup=c; return true; }
    }
    private static final class Storage implements LearningRepository.Storage {
        byte[] bytes; int reads,writes,clears; boolean writeFail,clearFail; Runnable onWrite;
        @Override public byte[] read() { reads++; return bytes==null ? null : bytes.clone(); }
        @Override public void write(byte[] value) throws IOException {
            writes++; if (onWrite!=null) { Runnable task=onWrite; onWrite=null; task.run(); }
            if (writeFail) throw new IOException("Synthetic write failure"); bytes=value.clone();
        }
        @Override public void clear() throws IOException { clears++; if (clearFail) throw new IOException("Synthetic delete failure"); bytes=null; }
    }
    private static final class Fixture {
        final Policy policy=new Policy(); final Storage storage=new Storage();
        final Queue worker=new Queue(),ui=new Queue(); long now=100*LearningData.DAY;
        boolean privateMode;
        final LearningRepository repository=new LearningRepository(storage,policy);
        final LearningCoordinator coordinator=new LearningCoordinator(repository,worker,ui,()->now,()->privateMode);
        void run() { worker.run(); ui.run(); }
        void enable(int d) { run(); yes(coordinator.configure(d)); run(); eq(d,coordinator.state().days); }
        boolean pick(String r,String w) { return coordinator.record(r,w,NORMAL,false,true,coordinator.state().generation); }
        List<LearningData.Entry> suggest(String r) { return coordinator.state().suggest(r,NORMAL,false,now); }
    }
    private static final class Editor implements InputSession.Editor {
        boolean accepting=true; String committed="";
        @Override public boolean commit(String s) { if (accepting) committed+=s; return accepting; }
        @Override public void finish() { }
        @Override public void delete() {}
        @Override public void cursor(int d) {}
        @Override public void enter() {}
    }
    private static InputSession session(Editor e,List<String> choices,int type,int flags) {
        InputSession s=new InputSession(); s.choices((r,w,p)->choices.add(r+":"+w)); s.start(e,FieldPolicy.from(type,flags)); return s;
    }
    public static void main(String[] args) throws Exception {
        test("learning defaults off; reads and rejected choices perform no file I/O",()->{
            Fixture f=new Fixture(); f.run(); eq(0,f.coordinator.state().days); no(f.pick("にほん","日本"));
            int operations=f.storage.reads+f.storage.writes+f.storage.clears;
            for (int i=0;i<1000;i++) { yes(f.suggest("に").isEmpty()); no(f.pick("にほん","日本")); }
            eq(operations,f.storage.reads+f.storage.writes+f.storage.clears); eq(0,f.worker.tasks.size());
        });
        test("retention options are explicit and invalid policies fail closed",()->{
            for (int d:new int[]{0,1,7,30,90}) eq(d,LearningData.days(d));
            for (int d:new int[]{-1,2,365,Integer.MAX_VALUE}) eq(0,LearningData.days(d));
        });
        test("only explicit successful candidate confirmation notifies learning",()->{
            List<String> choices=new ArrayList<>(); Editor e=new Editor(); InputSession s=session(e,choices,1,0);
            s.type("にほん"); s.preview("日本",s.revision()); yes(choices.isEmpty()); s.enter(); eq(1,choices.size());
            s.type("とうきょう"); s.select("東京",s.revision()); eq(2,choices.size());
            s.type("かんじ"); s.preview("漢字",s.revision()); s.confirm(); eq(3,choices.size());
            s.type("かな"); s.enter(); eq(3,choices.size());
        });
        test("typing modes cursor hide and lifecycle never learn implicit commits",()->{
            for (int action=0;action<6;action++) {
                List<String> choices=new ArrayList<>(); Editor e=new Editor(); InputSession s=session(e,choices,1,0);
                s.type("にほん"); s.preview("日本",s.revision());
                switch(action) { case 0:s.type("あ");break; case 1:s.mode(InputSession.Mode.ENGLISH);break;
                    case 2:s.cursor(1);break; case 3:s.discard();break;
                    case 4:s.finish();break; default:s.start(new Editor(),NORMAL); }
                yes(choices.isEmpty());
            }
        });
        test("failed writes stale choices cancellation and raw kana cannot learn",()->{
            List<String> choices=new ArrayList<>(); Editor e=new Editor(); InputSession s=session(e,choices,1,0);
            s.type("にほん"); long old=s.revision(); s.preview("日本",old); e.accepting=false;
            s.enter(); yes(choices.isEmpty()); no(s.select("日本",s.revision())); e.accepting=true;
            no(s.select("日本",old)); s.backspace(); s.enter(); yes(choices.isEmpty());
        });
        test("password restricted email URL name address numeric and private fields suppress learning",()->{
            for (int type:new int[]{0x81,0x91,0xe1,0x12,0x21,0x11,0x61,0x71,2,3,4,0x80001})
                no(FieldPolicy.from(type,0).learning);
            no(FieldPolicy.from(1,0x1000000).learning); no(FieldPolicy.from(1,0x80000000).learning);
            for (int flags:new int[]{0,0x1000000}) {
                List<String> choices=new ArrayList<>(); InputSession s=session(new Editor(),choices,1,flags);
                if (flags==0) s.incognito(true);
                s.type("にほん"); s.preview("日本",s.revision()); s.enter(); yes(choices.isEmpty());
            }
            Fixture f=new Fixture(); f.enable(7);
            for (int type:new int[]{0x81,0x21,0x61,0x71}) no(f.coordinator.record("にほん","日本",FieldPolicy.from(type,0),false,true,f.coordinator.state().generation));
            no(f.coordinator.record("にほん","日本",NORMAL,true,true,f.coordinator.state().generation));
            no(f.coordinator.record("にほん","日本",NORMAL,false,false,f.coordinator.state().generation));
            eq(0,f.storage.writes);
        });
        test("lookup never exposes learned entries in restricted or private fields",()->{
            Fixture f=new Fixture(); f.enable(7); yes(f.pick("にほん","日本")); f.run();
            eq(1,f.suggest("に").size());
            yes(f.coordinator.state().suggest("に",NORMAL,true,f.now).isEmpty());
            yes(f.coordinator.state().suggest("に",FieldPolicy.from(1,0x1000000),false,f.now).isEmpty());
            yes(f.coordinator.state().suggest("に",FieldPolicy.from(0x81,0),false,f.now).isEmpty());
        });
        test("settings privacy toggle returning to the same editor invalidates preview and old taps",()->{
            List<String> choices=new ArrayList<>(); Editor e=new Editor(); InputSession s=session(e,choices,1,0);
            boolean[] setting={false}; s.type("にほん"); s.preview("日本",s.revision()); long old=s.revision();
            setting[0]=true; yes(s.syncIncognito(()->setting[0])); yes(s.incognito()); eq("",s.preview());
            eq("にほん",s.text()); no(s.select("日本",old)); s.select("日本",s.revision()); yes(choices.isEmpty());
            setting[0]=false; yes(s.syncIncognito(()->setting[0])); no(s.incognito());
            s.type("とうきょう"); s.select("東京",s.revision()); eq(1,choices.size());
        });
        test("privacy read failure cancels local preview even when the host rejects confirmation",()->{
            List<String> choices=new ArrayList<>(); Editor e=new Editor(); InputSession s=session(e,choices,1,0);
            s.type("にほん"); s.preview("日本",s.revision()); e.accepting=false;
            yes(s.syncIncognito(()->{ throw new IllegalStateException("Synthetic preference failure"); }));
            yes(s.incognito()); eq("",s.preview()); eq("にほん",s.text()); eq("",e.committed);
            s.enter(); yes(choices.isEmpty()); yes(s.type("ご")); eq("にほんご",s.text());
            e.accepting=true; yes(s.confirm()); eq("にほんご",e.committed); yes(choices.isEmpty());
        });
        test("current privacy gates retained learned snapshots and recording before listeners run",()->{
            Fixture f=new Fixture(); f.enable(7); f.pick("にほん","日本"); f.run();
            LearningCoordinator.State old=f.coordinator.state(); f.privateMode=true;
            yes(old.suggest("に",NORMAL,false,f.now).isEmpty()); no(f.pick("とうきょう","東京"));
            eq(0,f.coordinator.pendingCount());
        });
        test("expired learned preview projects reading and accessibility before the same text is confirmed",()->{
            Fixture f=new Fixture(); f.enable(1); yes(f.pick("にほん","日本")); f.run();
            List<String> choices=new ArrayList<>(); Editor e=new Editor(); InputSession s=session(e,choices,1,0);
            yes(s.type("にほん")); yes(s.preview("日本",s.revision())); eq("日本",s.preedit(true).text);
            long old=s.revision(); f.now+=LearningData.DAY; eq(1,f.coordinator.state().entries.size());
            s.relayout(); eq("日本",s.preview());
            InputSession.Preedit shown=s.preedit(!f.suggest("にほん").isEmpty());
            yes(shown.visible); eq("にほん",shown.text); eq("未送信の読み: にほん",shown.description); eq("",s.preview());
            no(s.select("日本",old)); yes(s.confirm()); eq(shown.text,e.committed); yes(choices.isEmpty());
            yes(s.type("ご")); eq("ご",s.preedit(true).text);
        });
        test("cleared or disabled learned preview projects the exact next confirmation before or after worker completion",()->{
            for (int action=0;action<2;action++) for (boolean completed:new boolean[]{false,true}) {
                Fixture f=new Fixture(); f.enable(7); yes(f.pick("にほん","日本")); f.run();
                List<String> choices=new ArrayList<>(); Editor e=new Editor(); InputSession s=session(e,choices,1,0);
                yes(s.type("にほん")); yes(s.preview("日本",s.revision())); eq("日本",s.preedit(true).text);
                long old=s.revision(); yes(action==0 ? f.coordinator.clear() : f.coordinator.configure(0));
                if (completed) f.run();
                InputSession.Preedit shown=s.preedit(!f.suggest("にほん").isEmpty());
                yes(shown.visible); eq("にほん",shown.text); eq("未送信の読み: にほん",shown.description);
                no(s.select("日本",old)); yes(s.confirm()); eq(shown.text,e.committed); yes(choices.isEmpty());
                f.run(); yes(f.suggest("にほん").isEmpty());
            }
        });
        test("preedit projection keeps valid preview text exact and has no restricted-field echo",()->{
            Editor e=new Editor(); InputSession s=session(e,new ArrayList<>(),1,0);
            InputSession.Preedit empty=s.preedit(true); yes(empty.visible); eq("",empty.text);
            eq("キーボード内の未確定入力はありません",empty.description);
            s.type("にほん"); s.preview("日本",s.revision()); long revision=s.revision();
            InputSession.Preedit shown=s.preedit(true); eq(revision,s.revision()); eq("日本",shown.text);
            eq("仮選択: 日本、読み: にほん",shown.description); yes(s.confirm()); eq(shown.text,e.committed);
            for (int type:new int[]{0x81,0x91,0xe1,0x12,2,0x80001}) {
                s=session(new Editor(),new ArrayList<>(),type,0); yes(s.type("5"));
                InputSession.Preedit hidden=s.preedit(true); no(hidden.visible); eq("",hidden.text); eq("",hidden.description);
            }
        });
        test("privacy transitions discard queued choices and never revive them when switched off",()->{
            Fixture f=new Fixture(); f.enable(7); f.pick("にほん","日本"); f.run(); int writes=f.storage.writes;
            LearningCoordinator.State stale=f.coordinator.state(); yes(f.pick("とうきょう","東京"));
            f.privateMode=true; f.coordinator.privateModeChanged(); eq(0,f.coordinator.pendingCount());
            yes(f.suggest("に").isEmpty()); f.privateMode=false; f.coordinator.privateModeChanged(); f.run();
            eq(writes,f.storage.writes); eq(1,f.coordinator.state().entries.size());
            no(f.coordinator.record("とうきょう","東京",NORMAL,false,true,stale.generation));
            yes(f.pick("とうきょう","東京")); f.run(); eq(2,f.coordinator.state().entries.size());
        });
        test("queued worker independently rechecks current privacy without a delivered callback",()->{
            Fixture f=new Fixture(); f.enable(7); yes(f.pick("にほん","日本")); f.privateMode=true; f.run();
            eq(0,f.storage.writes); eq(0,f.coordinator.state().entries.size());
        });
        test("privacy changes before persistence restore the previous bounded memory snapshot",()->{
            Fixture f=new Fixture(); f.enable(7); f.pick("にほん","日本"); f.run(); int writes=f.storage.writes;
            int[] calls={0}; f.repository.record(Collections.singletonList(new LearningData.Entry("とうきょう","東京",f.now)),
                    f.now,()->++calls[0]==1);
            eq(2,calls[0]); eq(writes,f.storage.writes); eq(1,f.repository.entries().size());
        });
        test("prefix prediction retains the complete original reading",()->{
            Fixture f=new Fixture(); f.enable(7); f.pick("にほん","日本"); f.run();
            List<ConversionCandidate> candidates=ConversionCandidate.combine("に",Collections.emptyList(),f.suggest("に"));
            eq("日本",candidates.get(0).word); eq("にほん",candidates.get(0).learningReading); yes(candidates.get(0).learned);
        });
        test("expiry is enforced in memory reads without renewing time or doing disk I/O",()->{
            Fixture f=new Fixture(); f.enable(1); f.pick("にほん","日本"); f.run(); long selected=f.now;
            int writes=f.storage.writes,clears=f.storage.clears;
            f.now=selected+LearningData.DAY-1; eq(1,f.suggest("に").size());
            for (int i=0;i<100;i++) f.suggest("に"); eq(selected,f.coordinator.state().entries.get(0).selectedAt);
            f.now++; yes(f.suggest("に").isEmpty()); eq(writes,f.storage.writes); eq(clears,f.storage.clears);
            f.coordinator.maintain(); f.run(); yes(f.storage.bytes==null); eq(0,f.coordinator.state().entries.size());
        });
        test("retention shortening prunes old choices and preserves recent ones",()->{
            Fixture f=new Fixture(); f.enable(7); f.pick("にほん","日本"); f.run(); f.now+=2*LearningData.DAY;
            f.pick("とうきょう","東京"); f.run(); yes(f.coordinator.configure(1)); f.run();
            yes(f.suggest("に").isEmpty()); eq(1,f.suggest("とう").size());
        });
        test("period extension cannot revive an expired or future-dated choice",()->{
            Fixture f=new Fixture(); f.enable(1); f.pick("にほん","日本"); f.run(); f.now+=2*LearningData.DAY;
            yes(f.coordinator.configure(90)); f.run(); yes(f.suggest("に").isEmpty()); yes(f.storage.bytes==null);
            f.pick("とうきょう","東京"); f.run(); f.now-=LearningData.DAY; f.coordinator.maintain(); f.run();
            yes(f.suggest("とう").isEmpty()); yes(f.storage.bytes==null);
        });
        test("repeated learning coalesces one pair and one queued worker",()->{
            Fixture f=new Fixture(); f.enable(7); int before=f.storage.writes;
            for (int i=0;i<10000;i++) { f.now++; yes(f.pick("にほん","日本")); }
            eq(1,f.coordinator.pendingCount()); eq(1,f.worker.tasks.size()); f.run();
            eq(before+1,f.storage.writes); eq(1,f.coordinator.state().entries.size()); eq(f.now,f.coordinator.state().entries.get(0).selectedAt);
        });
        test("pending choices and notifications remain bounded during a burst",()->{
            Fixture f=new Fixture(); f.enable(7);
            for (int i=0;i<1000;i++) { f.now++; f.pick(reading(i),"語"+i); }
            eq(LearningCoordinator.MAX_PENDING,f.coordinator.pendingCount()); eq(1,f.worker.tasks.size());
            f.worker.run(); eq(1,f.ui.tasks.size()); f.ui.run(); eq(32,f.coordinator.state().entries.size());
        });
        test("entry quota evicts oldest records deterministically",()->{
            LearningData d=new LearningData();
            for (int i=0;i<1000;i++) d.record(new LearningData.Entry(reading(i),"語"+i,1000+i));
            eq(128,d.snapshot().size()); eq(1872L,d.snapshot().get(0).selectedAt); yes(d.encode().length<=LearningData.MAX_BYTES);
            LearningData a=new LearningData(),b=new LearningData();
            for (int i=0;i<200;i++) a.record(new LearningData.Entry(reading(i),"語"+i,1));
            for (int i=199;i>=0;i--) b.record(new LearningData.Entry(reading(i),"語"+i,1));
            yes(Arrays.equals(a.encode(),b.encode()));
        });
        test("serialized byte quota can evict before the entry quota",()->{
            LearningData d=new LearningData();
            for (int i=0;i<150;i++) d.record(new LearningData.Entry(repeat("あ",28)+reading(i),repeat("\ud840\udc00",47)+(char)(0x4e00+i),1000+i));
            yes(d.snapshot().size()<128); yes(d.snapshot().size()>1); yes(d.encode().length<=32768);
        });
        test("reload keeps valid choices without additional history or cache",()->{
            Fixture f=new Fixture(); f.enable(7); f.pick("にほん","日本"); f.run(); byte[] saved=f.storage.bytes.clone();
            LearningRepository loaded=new LearningRepository(f.storage,f.policy); loaded.load(f.now);
            eq(1,loaded.entries().size()); eq("日本",loaded.entries().get(0).word); yes(Arrays.equals(saved,f.storage.bytes));
        });
        test("malformed oversized duplicated and truncated data fail closed",()->{
            for (byte[] bytes:new byte[][]{new byte[0],new byte[]{1,2,3},new byte[LearningData.MAX_BYTES+1]}) {
                Fixture f=new Fixture(); f.policy.days=7; f.storage.bytes=bytes; f.run();
                yes(f.coordinator.state().failed); eq(0,f.coordinator.state().days); yes(f.suggest("に").isEmpty());
                yes(f.storage.bytes==null); yes(f.policy.cleanup);
            }
            LearningData d=new LearningData(); d.record(new LearningData.Entry("にほん","日本",1)); byte[] bytes=d.encode();
            for (byte[] bad:new byte[][]{Arrays.copyOf(bytes,bytes.length-1),Arrays.copyOf(bytes,bytes.length+1)}) {
                try { new LearningData().decode(bad); throw new AssertionError("Corruption accepted"); } catch (IOException expected) {}
            }
            java.io.ByteArrayOutputStream duplicate=new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out=new java.io.DataOutputStream(duplicate);
            out.writeInt(0x4b4f5431); out.writeInt(2);
            for (int i=0;i<2;i++) { out.writeUTF("にほん"); out.writeUTF("日本"); out.writeLong(1); }
            out.close();
            try { new LearningData().decode(duplicate.toByteArray()); throw new AssertionError("Duplicate accepted"); }
            catch (IOException expected) {}
        });
        test("clear invalidates pending choices and stale generations before worker completion",()->{
            Fixture f=new Fixture(); f.enable(7); f.pick("にほん","日本"); long stale=f.coordinator.state().generation;
            yes(f.coordinator.clear()); yes(f.suggest("に").isEmpty()); eq(0,f.coordinator.pendingCount());
            no(f.coordinator.record("にほん","日本",NORMAL,false,true,stale)); f.run();
            eq(0,f.coordinator.state().entries.size()); yes(f.storage.bytes==null); eq(7,f.coordinator.state().days);
            no(f.coordinator.record("にほん","日本",NORMAL,false,true,stale)); f.run(); yes(f.storage.bytes==null);
        });
        test("clear requested during an active write cannot repopulate cleared memory",()->{
            Fixture f=new Fixture(); f.enable(7); f.storage.onWrite=()->yes(f.coordinator.clear());
            f.pick("にほん","日本"); f.run(); eq(0,f.coordinator.state().entries.size()); yes(f.storage.bytes==null);
        });
        test("off removes data and startup cleanup prevents accidental restoration",()->{
            Fixture f=new Fixture(); f.enable(7); f.pick("にほん","日本"); f.run(); yes(f.coordinator.configure(0)); f.run();
            yes(f.storage.bytes==null); no(f.pick("にほん","日本"));
            LearningData old=new LearningData(); old.record(new LearningData.Entry("にほん","日本",f.now)); f.storage.bytes=old.encode();
            f.repository.load(f.now); yes(f.storage.bytes==null); yes(f.repository.entries().isEmpty());
        });
        test("delete failure is reported truthfully and blocks reuse until successful retry",()->{
            Fixture f=new Fixture(); f.enable(7); f.pick("にほん","日本"); f.run(); f.storage.clearFail=true;
            yes(f.coordinator.clear()); f.run(); yes(f.coordinator.state().failed); eq(0,f.coordinator.state().days);
            yes(f.storage.bytes!=null); yes(f.policy.cleanup); no(f.pick("にほん","日本"));
            f.storage.clearFail=false; yes(f.coordinator.clear()); f.run(); no(f.coordinator.state().failed); yes(f.storage.bytes==null);
        });
        test("write and policy failures stop learning without reporting successful persistence",()->{
            Fixture f=new Fixture(); f.enable(7); f.storage.writeFail=true; f.pick("にほん","日本"); f.run();
            yes(f.coordinator.state().failed); yes(f.suggest("に").isEmpty()); no(f.pick("にほん","日本"));
            Fixture p=new Fixture(); p.run(); p.policy.accept=false; yes(p.coordinator.configure(7)); p.run();
            yes(p.coordinator.state().failed); eq(0,p.coordinator.state().days);
        });
        test("persisted cleanup guard discards old data before re-enabling",()->{
            Fixture f=new Fixture(); f.policy.days=7; f.policy.cleanup=true;
            LearningData d=new LearningData(); d.record(new LearningData.Entry("にほん","日本",f.now)); f.storage.bytes=d.encode(); f.run();
            yes(f.storage.bytes==null); eq(0,f.coordinator.state().entries.size()); no(f.policy.cleanup);
        });
        test("corrupt policy metadata cannot crash the worker or expose saved choices",()->{
            Storage storage=new Storage();
            LearningRepository.Policy broken=new LearningRepository.Policy() {
                @Override public int days() { throw new ClassCastException("Synthetic policy corruption"); }
                @Override public boolean cleanupRequired() { throw new ClassCastException(); }
                @Override public boolean save(int days,boolean cleanup) { throw new IllegalStateException(); }
            };
            LearningRepository repository=new LearningRepository(storage,broken); repository.load(1000);
            yes(repository.failed()); eq(0,repository.days()); yes(repository.entries().isEmpty());
            no(repository.configure(7,1000)); no(repository.clear());
        });
        test("invalid lexical values raw kana controls and oversized words cannot be saved",()->{
            Fixture f=new Fixture(); f.enable(7);
            for (String[] pair:new String[][]{{"","日本"},{"password","秘密"},{"にほん","にほん"},{"にほん","ニホン"},
                    {"にほん","日\n本"},{"にほん","\ud800"},{repeat("あ",33),"語"},{"あ",repeat("語",49)}}) no(f.pick(pair[0],pair[1]));
            eq(0,f.storage.writes);
        });
        Path base=java.nio.file.Paths.get(".build-cache/feature-files"); Files.createDirectories(base);
        test("real atomic file replacement has one data file and cleans interrupted temporary writes",()->{
            Path dir=Files.createTempDirectory(base,"storage-"); LearningFileStorage storage=new LearningFileStorage(dir);
            LearningData data=new LearningData(); data.record(new LearningData.Entry("にほん","日本",1)); byte[] expected=data.encode();
            storage.write(expected); yes(Arrays.equals(expected,storage.read()));
            Files.write(dir.resolve("learning-v1.tmp"),new byte[]{9,9,9}); yes(Arrays.equals(expected,storage.read()));
            no(Files.exists(dir.resolve("learning-v1.tmp"))); eq(1L,Files.list(dir).count()); storage.clear(); eq(0L,Files.list(dir).count());
        });
        test("real oversized file is rejected and clear leaves dictionary and controls untouched",()->{
            Path dir=Files.createTempDirectory(base,"limits-"); LearningFileStorage storage=new LearningFileStorage(dir);
            Files.write(dir.resolve("learning-v1.bin"),new byte[32769]); Files.write(dir.resolve("learning-v1.tmp"),new byte[]{1});
            Files.write(dir.resolve("skk.tsv"),new byte[]{7}); Files.write(dir.resolve("controls.xml"),new byte[]{8});
            try { storage.read(); throw new AssertionError("Oversize accepted"); } catch (IOException expected) {}
            storage.clear(); yes(Arrays.equals(new byte[]{7},Files.readAllBytes(dir.resolve("skk.tsv"))));
            yes(Arrays.equals(new byte[]{8},Files.readAllBytes(dir.resolve("controls.xml")))); eq(2L,Files.list(dir).count());
        });
        test("density-independent flick limits reset and invalid density are deterministic",()->{
            eq(8,FlickSensitivity.clamp(-10)); eq(40,FlickSensitivity.clamp(100)); eq(22,FlickSensitivity.DEFAULT_DP);
            for (float density:new float[]{1,2,3.5f}) eq(22*density,FlickSensitivity.pixels(22,density));
            eq(22f,FlickSensitivity.pixels(22,Float.NaN)); eq(22f,FlickSensitivity.pixels(22,0));
        });
        test("tap versus flick edges diagonal ties and out-of-key origins honor chosen distance",()->{
            for (int dp:new int[]{8,22,40}) for (float density:new float[]{1,3}) {
                float distance=FlickSensitivity.pixels(dp,density);
                KeyGesture g=new KeyGesture(true,false,distance); g.begin(0,0); eq(0,g.end(distance-.01f,0,false));
                g.begin(0,0); eq(1,g.end(-distance,0,false)); g.begin(0,0); eq(2,g.end(0,-distance,false));
                g.begin(0,0); eq(3,g.end(distance,0,false)); g.begin(0,0); eq(4,g.end(0,distance,false));
                g.begin(0,0); eq(3,g.end(distance,-distance,false));
            }
        });
        test("sensitivity changes configuration cancellation and held keys never duplicate release",()->{
            KeyGesture g=new KeyGesture(true,false,22); g.begin(0,0); g.move(-30,0,false); g.threshold(8); eq(-1,g.end(-30,0,false));
            KeyGesture held=new KeyGesture(false,true,22); yes(held.begin(0,0)); yes(held.repeat());
            held.move(-1,0,false); no(held.repeat()); eq(-1,held.end(0,0,true));
            yes(held.begin(0,0)); held.cancel(); no(held.repeat()); eq(-1,held.end(0,0,true));
        });
        test("Japanese keypad has mode rails all twelve positions and familiar right controls",()->{
            KeyboardLayout.Role[] left={KeyboardLayout.Role.ENGLISH,KeyboardLayout.Role.SYMBOLS,KeyboardLayout.Role.PICKER,KeyboardLayout.Role.HIDE};
            KeyboardLayout.Role[] right={KeyboardLayout.Role.DELETE,KeyboardLayout.Role.CONVERT,KeyboardLayout.Role.SPACE,KeyboardLayout.Role.ENTER};
            for (int r=0;r<4;r++) {
                KeyboardLayout.Key[] row=KeyboardLayout.kanaRow(r); eq(5,row.length); eq(left[r],row[0].role); eq(right[r],row[4].role);
                for (int c=0;c<3;c++) eq(r*3+c,row[c+1].kana);
            }
            eq(KeyboardLayout.Role.MODIFY,KeyboardLayout.kanaRow(3)[1].role);
        });
        test("English rows are staggered with wider shift delete and space and accessible mode exits",()->{
            for (int r=0;r<4;r++) { float units=0; for (KeyboardLayout.Key key:KeyboardLayout.englishRow(r)) units+=key.weight; yes(Math.abs(10-units)<.001); }
            eq(KeyboardLayout.Role.SPACER,KeyboardLayout.englishRow(1)[0].role);
            eq(.5f,KeyboardLayout.englishRow(1)[0].weight); eq(1.5f,KeyboardLayout.englishRow(2)[0].weight);
            yes(Arrays.stream(KeyboardLayout.englishRow(3)).anyMatch(k->k.role==KeyboardLayout.Role.SPACE && k.weight>=4));
        });
        SkkDictionary dictionary=SkkDictionary.load(new FileInputStream(args[0])); PhraseConverter converter=new PhraseConverter(dictionary);
        test("offline segmented phrase candidates cover real dictionary and polite endings",()->{
            List<String> a=converter.convert("にほんにいきます",()->true); yes(a.contains("日本に行きます"));
            List<String> b=converter.convert("きょうはとうきょうにいきます",()->true); yes(b.contains("今日は東京に行きます"));
            List<String> c=converter.convert("すしをたべます",()->true); yes(c.stream().anyMatch(w->w.endsWith("を食べます")));
        });
        test("phrase conversion is bounded cancelled and independent of learning storage",()->{
            yes(converter.convert(repeat("あ",65),()->true).isEmpty()); yes(converter.convert("にほん",()->false).isEmpty());
            yes(converter.convert("\ud840\udc00",()->true).isEmpty());
            Fixture f=new Fixture(); f.run(); int ops=f.storage.reads+f.storage.writes+f.storage.clears;
            yes(converter.convert("にほんにいきます",()->true).contains("日本に行きます")); eq(ops,f.storage.reads+f.storage.writes+f.storage.clears);
        });
        test("generated phrases are never learnable and kana fallbacks survive the candidate cap",()->{
            List<String> phrases=converter.convert("にほんにいきます",()->true);
            List<ConversionCandidate> c=ConversionCandidate.combine("にほんにいきます",dictionary.candidates("にほんにいきます"),Collections.emptyList(),phrases,Collections.emptyList());
            yes(c.stream().anyMatch(v->v.phrase && v.word.equals("日本に行きます") && !v.eligible));
            c=ConversionCandidate.combine("こう",dictionary.candidates("こう"),Collections.emptyList(),Arrays.asList("交付申請"),dictionary.exactWords("こう"));
            yes(c.size()<=64); yes(c.stream().anyMatch(v->v.word.equals("こう"))); yes(c.stream().anyMatch(v->v.word.equals("コウ")));
        });
        test("phrase worker coalesces bursts and suppresses cancelled or stale field results",()->{
            Queue worker=new Queue(),ui=new Queue(); List<String> delivered=new ArrayList<>();
            PhraseRequests requests=new PhraseRequests(converter,worker,ui,(r,c)->delivered.add(r));
            for (int i=0;i<500;i++) requests.request("とうきょう"); requests.request("にほん"); eq(1,worker.tasks.size());
            worker.run(); eq(1,ui.tasks.size()); requests.cancel(); ui.run(); yes(delivered.isEmpty());
            requests.request("にほん"); worker.run(); requests.request("かんじ"); ui.run(); yes(delivered.isEmpty());
            worker.run(); ui.run(); eq(Collections.singletonList("かんじ"),delivered);
        });
        System.out.println("PASS: "+count+" feature groups; Android UI/event-loop/private-storage runtime NOT RUN");
    }
}
