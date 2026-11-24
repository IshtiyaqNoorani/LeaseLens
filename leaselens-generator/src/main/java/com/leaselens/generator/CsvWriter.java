package com.leaselens.generator;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes CSV with LF line endings and UTF-8, so output is byte-identical on every OS. */
final class CsvWriter {

    private CsvWriter() {
    }

    static void write(Path file, List<String> header, List<List<String>> rows) throws IOException {
        Files.createDirectories(file.getParent());
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeRow(w, header);
            for (List<String> row : rows) {
                writeRow(w, row);
            }
        }
    }

    private static void writeRow(Writer w, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                w.write(',');
            }
            w.write(escape(values.get(i)));
        }
        w.write('\n');
    }

    private static String escape(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /** Paise to a rupee string with exactly two decimals, e.g. 2500000 -> "25000.00". */
    static String rupees(long paise) {
        String sign = paise < 0 ? "-" : "";
        long abs = Math.abs(paise);
        return sign + (abs / 100) + "." + String.format(java.util.Locale.ROOT, "%02d", abs % 100);
    }
}
