package com.leaselens.data.csv;

import java.util.List;

/**
 * A parsed CSV file: a header and data rows.
 *
 * @param header column names as written, trimmed
 * @param rows   data rows; each row remembers its line number in the file
 */
public record CsvTable(String fileName, List<String> header, List<Row> rows) {

    /**
     * One data row.
     *
     * @param lineNumber 1-based line number in the file (the header is line 1)
     */
    public record Row(int lineNumber, List<String> fields) {
    }

    /** Index of a column, or -1 if absent. Matching ignores case and surrounding spaces. */
    public int columnIndex(String column) {
        for (int i = 0; i < header.size(); i++) {
            if (header.get(i).equalsIgnoreCase(column)) {
                return i;
            }
        }
        return -1;
    }
}
