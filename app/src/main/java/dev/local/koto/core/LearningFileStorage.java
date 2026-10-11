/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

/** Fixed filenames below Android's app-private no-backup directory; no journal. */
public final class LearningFileStorage implements LearningRepository.Storage {
    private final Supplier<Path> directory;
    private Path base,temp;
    public LearningFileStorage(Path noBackupDirectory) {
        this(()->noBackupDirectory);
    }
    public LearningFileStorage(Supplier<Path> directory) { this.directory=directory; }
    private void initialize() {
        if (base==null) {
            Path root=directory.get(); base=root.resolve("learning-v1.bin"); temp=root.resolve("learning-v1.tmp");
        }
    }
    private void removeTemp() throws IOException { Files.deleteIfExists(temp); }
    @Override public byte[] read() throws IOException {
        initialize();
        removeTemp(); // A crash may leave an incomplete newer file. Never restore it.
        if (!Files.exists(base,LinkOption.NOFOLLOW_LINKS)) return null;
        if (!Files.isRegularFile(base,LinkOption.NOFOLLOW_LINKS) || Files.size(base)>LearningData.MAX_BYTES)
            throw new IOException("Invalid learning file");
        try (FileChannel input=FileChannel.open(base,StandardOpenOption.READ)) {
            ByteBuffer bytes=ByteBuffer.allocate(LearningData.MAX_BYTES+1);
            while (input.read(bytes)>0 && bytes.hasRemaining()) {}
            if (bytes.position()>LearningData.MAX_BYTES) throw new IOException("Learning size limit");
            byte[] result=new byte[bytes.position()]; bytes.flip(); bytes.get(result); return result;
        }
    }
    @Override public void write(byte[] data) throws IOException {
        initialize();
        if (data.length>LearningData.MAX_BYTES) throw new IOException("Learning size limit");
        Files.createDirectories(base.getParent()); removeTemp();
        try {
            try (FileChannel output=FileChannel.open(temp,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {
                ByteBuffer bytes=ByteBuffer.wrap(data); while (bytes.hasRemaining()) output.write(bytes);
                output.force(true);
            }
            // Fail closed on filesystems without atomic replacement; no fallback journal/backup.
            Files.move(temp,base,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally { removeTemp(); }
    }
    @Override public void clear() throws IOException {
        initialize();
        IOException failure=null;
        try { removeTemp(); } catch (IOException e) { failure=e; }
        try { Files.deleteIfExists(base); } catch (IOException e) { failure=e; }
        if (Files.exists(base,LinkOption.NOFOLLOW_LINKS) || Files.exists(temp,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Learning deletion incomplete");
        if (failure!=null) throw failure;
    }
}
