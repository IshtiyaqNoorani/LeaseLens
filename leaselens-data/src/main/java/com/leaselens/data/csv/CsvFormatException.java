package com.leaselens.data.csv;

/** The file is not well-formed CSV (for example, an unterminated quoted field). */
public final class CsvFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int lineNumber;

    public CsvFormatException(int lineNumber, String message) {
        super(message);
        this.lineNumber = lineNumber;
    }

    public int lineNumber() {
        return lineNumber;
    }
}
