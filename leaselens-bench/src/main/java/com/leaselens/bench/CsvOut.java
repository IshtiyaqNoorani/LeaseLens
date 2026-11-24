package com.leaselens.bench;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Writes result CSVs. Lines starting with '#' record how and where the numbers were measured. */
public final class CsvOut {

    private CsvOut() {
    }

    public static void write(Path file, Map<String, String> metadata, List<String> header, List<List<String>> rows)
            throws IOException {
        Files.createDirectories(file.getParent());
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> m : metadata.entrySet()) {
                w.write("# " + m.getKey() + ": " + m.getValue().replace('\n', ' ') + "\n");
            }
            w.write(String.join(",", header) + "\n");
            for (List<String> row : rows) {
                w.write(String.join(",", row.stream().map(CsvOut::escape).toList()) + "\n");
            }
        }
    }

    private static String escape(String v) {
        return v.contains(",") || v.contains("\"") ? '"' + v.replace("\"", "\"\"") + '"' : v;
    }
}
