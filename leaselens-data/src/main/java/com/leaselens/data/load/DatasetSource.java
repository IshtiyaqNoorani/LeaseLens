package com.leaselens.data.load;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/** Where the CSV files come from: a directory on disk or uploaded file contents. */
@FunctionalInterface
public interface DatasetSource {

    /** Opens the named file, or returns empty if it was not supplied. */
    Optional<Reader> open(String fileName) throws IOException;

    /** Files in a directory, read as UTF-8. */
    static DatasetSource directory(Path dir) {
        return fileName -> {
            Path file = dir.resolve(fileName);
            return Files.isRegularFile(file)
                    ? Optional.of(Files.newBufferedReader(file, StandardCharsets.UTF_8))
                    : Optional.empty();
        };
    }

    /** In-memory file contents keyed by file name (e.g. from an HTTP upload), decoded as UTF-8. */
    static DatasetSource contents(Map<String, byte[]> files) {
        return fileName -> {
            byte[] bytes = files.get(fileName);
            return bytes == null
                    ? Optional.empty()
                    : Optional.of(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
        };
    }
}
