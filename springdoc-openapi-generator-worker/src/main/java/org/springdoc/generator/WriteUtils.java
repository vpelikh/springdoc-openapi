package org.springdoc.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * File-writing helpers for the generator workers.
 */
final class WriteUtils {

    private WriteUtils() {
    }

    /**
     * Writes {@code bytes} to {@code target} atomically where the underlying filesystem supports
     * it. The bytes are first written to a temporary sibling file, then moved over the target.
     * This guarantees the final output path only ever contains a complete document: if the fork is
     * killed mid-write (e.g. the plugin's fork timeout) or the write fails, no partial or corrupt
     * file is left at {@code target}.
     *
     * @throws IOException if the write or the move fails
     */
    static void writeAtomic(Path target, byte[] bytes) throws IOException {
        Path dir = target.getParent();
        if (dir == null) {
            dir = Path.of(".");
        }
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, target.getFileName().toString(), ".tmp");
        try {
            Files.write(tmp, bytes);
            try {
                // Atomic within the same directory when the (default) filesystem supports it.
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (java.nio.file.AtomicMoveNotSupportedException e) {
                // Fall back to a best-effort atomic (same-dir) move for non-atomic filesystems.
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally {
            // If anything failed before the move, leave no temp debris behind.
            Files.deleteIfExists(tmp);
        }
    }
}