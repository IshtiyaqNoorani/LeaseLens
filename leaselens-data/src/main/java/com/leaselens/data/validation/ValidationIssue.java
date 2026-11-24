package com.leaselens.data.validation;

/**
 * One problem found in the input.
 *
 * @param line   1-based line number in the file (header = line 1); null for file-level issues
 * @param column column name; null when not tied to one column
 * @param value  the offending raw value; null when not applicable
 */
public record ValidationIssue(Severity severity, Rule rule, String file, Integer line,
                              String column, String value, String message) {
}
