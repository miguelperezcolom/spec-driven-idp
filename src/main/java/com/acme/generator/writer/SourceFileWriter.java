package com.acme.generator.writer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Writes Java source files respecting the generated / manual split:
 * <ul>
 *   <li><b>Generated</b> files are always overwritten.</li>
 *   <li><b>Manual</b> files are only written if they don't exist yet
 *       (they are stubs meant to be extended by the developer).</li>
 * </ul>
 */
public class SourceFileWriter {

    /**
     * Writes a generated file (always overwritten).
     */
    public void writeGenerated(Path dir, String className, String content) throws IOException {
        write(dir, className, content, true);
    }

    /**
     * Writes a manual stub file (skipped if already exists).
     */
    public void writeManualStub(Path dir, String className, String content) throws IOException {
        write(dir, className, content, false);
    }

    private void write(Path dir, String className, String content, boolean overwrite) throws IOException {
        Files.createDirectories(dir);
        Path file = dir.resolve(className + ".java");

        if (!overwrite && Files.exists(file)) {
            System.out.println("  [SKIP]  " + file + " (manual file already exists)");
            return;
        }

        Files.writeString(file, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println(overwrite
                ? "  [GEN]   " + file
                : "  [STUB]  " + file);
    }
}
