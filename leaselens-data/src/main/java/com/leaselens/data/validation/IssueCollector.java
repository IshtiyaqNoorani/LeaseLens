package com.leaselens.data.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Accumulates issues while a dataset is validated. */
public final class IssueCollector {

    /** Upper bound on issues kept in a report, so a badly broken large file cannot flood the UI. */
    public static final int MAX_STORED_ISSUES = 1_000;

    private final List<ValidationIssue> issues = new ArrayList<>();
    private final Map<String, int[]> perFile = new LinkedHashMap<>(); // file -> {errors, warnings}
    private int errors;
    private int warnings;

    public void error(Rule rule, String file, Integer line, String column, String value, String message) {
        add(new ValidationIssue(Severity.ERROR, rule, file, line, column, value, message));
    }

    public void warning(Rule rule, String file, Integer line, String column, String value, String message) {
        add(new ValidationIssue(Severity.WARNING, rule, file, line, column, value, message));
    }

    private void add(ValidationIssue issue) {
        int[] counts = perFile.computeIfAbsent(issue.file(), f -> new int[2]);
        if (issue.severity() == Severity.ERROR) {
            errors++;
            counts[0]++;
        } else {
            warnings++;
            counts[1]++;
        }
        if (issues.size() < MAX_STORED_ISSUES) {
            issues.add(issue);
        }
    }

    public int errorCount() {
        return errors;
    }

    public int errorCount(String file) {
        int[] counts = perFile.get(file);
        return counts == null ? 0 : counts[0];
    }

    public int warningCount(String file) {
        int[] counts = perFile.get(file);
        return counts == null ? 0 : counts[1];
    }

    public ValidationReport report(List<FileSummary> files) {
        return new ValidationReport(List.copyOf(issues), List.copyOf(files), errors, warnings,
                errors + warnings > issues.size());
    }
}
