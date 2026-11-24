package com.leaselens.data.csv;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC 4180 CSV parser: comma separator, optional double-quoted fields (which may contain
 * commas and line breaks), {@code ""} for a literal quote inside a quoted field, and LF or CRLF
 * line endings. A leading UTF-8 byte-order mark is ignored, blank lines are skipped, and unquoted
 * values are trimmed.
 *
 * <p>Written by hand to keep the data module free of third-party dependencies. It is
 * infrastructure, not an assessed algorithm.
 */
public final class CsvParser {

    private CsvParser() {
    }

    /**
     * Parses a whole file. A file with no content gives a table with an empty header and no rows.
     *
     * @throws CsvFormatException if a quoted field is not closed before the end of the file
     */
    public static CsvTable parse(String fileName, Reader reader) throws IOException, CsvFormatException {
        String text = readAll(reader);
        if (!text.isEmpty() && text.charAt(0) == '﻿') {
            text = text.substring(1);
        }
        return new Parser(fileName, text).run();
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[8192];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, read);
        }
        return sb.toString();
    }

    /** Single-use state machine over the file text. */
    private static final class Parser {
        private final String fileName;
        private final String text;

        private final List<List<String>> records = new ArrayList<>();
        private final List<Integer> recordLines = new ArrayList<>();
        private List<String> record = new ArrayList<>();
        private final StringBuilder field = new StringBuilder();
        private boolean inQuotes;
        private boolean fieldQuoted;
        private boolean recordHasContent;
        private int line = 1;
        private int recordLine = 1;

        Parser(String fileName, String text) {
            this.fileName = fileName;
            this.text = text;
        }

        CsvTable run() throws CsvFormatException {
            int i = 0;
            while (i < text.length()) {
                char ch = text.charAt(i);
                if (inQuotes) {
                    if (ch == '"') {
                        if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                            field.append('"'); // escaped quote
                            i += 2;
                            continue;
                        }
                        inQuotes = false; // closing quote
                    } else {
                        if (ch == '\n') {
                            line++;
                        }
                        field.append(ch);
                    }
                    i++;
                    continue;
                }
                switch (ch) {
                    case '"' -> {
                        if (!fieldQuoted && field.toString().isBlank()) {
                            inQuotes = true;
                            fieldQuoted = true;
                            field.setLength(0);
                            recordHasContent = true;
                        } else {
                            field.append(ch); // stray quote inside an unquoted value: keep it literally
                        }
                    }
                    case ',' -> {
                        endField();
                        recordHasContent = true;
                    }
                    case '\r' -> {
                        // ignored; the following '\n' ends the line
                    }
                    case '\n' -> {
                        endField();
                        endRecord();
                        line++;
                        recordLine = line;
                    }
                    default -> {
                        if (fieldQuoted && Character.isWhitespace(ch)) {
                            break; // spaces after a closing quote
                        }
                        field.append(ch);
                        if (!Character.isWhitespace(ch)) {
                            recordHasContent = true;
                        }
                    }
                }
                i++;
            }
            if (inQuotes) {
                throw new CsvFormatException(recordLine,
                        fileName + " line " + recordLine + ": a quoted value is never closed.");
            }
            endField();
            endRecord();
            return toTable();
        }

        private void endField() {
            record.add(fieldQuoted ? field.toString() : field.toString().trim());
            field.setLength(0);
            fieldQuoted = false;
        }

        private void endRecord() {
            if (recordHasContent) {
                records.add(record);
                recordLines.add(recordLine);
            }
            record = new ArrayList<>();
            recordHasContent = false;
        }

        private CsvTable toTable() {
            if (records.isEmpty()) {
                return new CsvTable(fileName, List.of(), List.of());
            }
            List<String> header = records.get(0).stream().map(String::trim).toList();
            List<CsvTable.Row> rows = new ArrayList<>(records.size() - 1);
            for (int r = 1; r < records.size(); r++) {
                rows.add(new CsvTable.Row(recordLines.get(r), List.copyOf(records.get(r))));
            }
            return new CsvTable(fileName, header, List.copyOf(rows));
        }
    }
}
