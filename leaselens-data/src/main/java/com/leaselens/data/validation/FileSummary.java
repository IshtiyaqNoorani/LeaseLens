package com.leaselens.data.validation;

/**
 * Per-file outcome.
 *
 * @param rows          data rows read (header excluded)
 * @param rowsAccepted  rows that parsed cleanly (they may still fail cross-file checks)
 */
public record FileSummary(String file, boolean present, int rows, int rowsAccepted, int errors, int warnings) {
}
