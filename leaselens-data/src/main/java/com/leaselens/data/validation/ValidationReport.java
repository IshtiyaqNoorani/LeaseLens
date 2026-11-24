package com.leaselens.data.validation;

import java.util.List;

/**
 * Result of validating a dataset.
 *
 * @param issues        issues in the order found, capped at {@link IssueCollector#MAX_STORED_ISSUES}
 * @param errorCount    total errors found (including any not stored because of the cap)
 * @param warningCount  total warnings found (including any not stored because of the cap)
 * @param truncated     true if more issues were found than are listed
 */
public record ValidationReport(List<ValidationIssue> issues, List<FileSummary> files,
                               int errorCount, int warningCount, boolean truncated) {

    /** True when there are no errors. Warnings do not make a dataset invalid. */
    public boolean valid() {
        return errorCount == 0;
    }

    /** True if at least one issue with this rule was recorded. */
    public boolean has(Rule rule) {
        return issues.stream().anyMatch(i -> i.rule() == rule);
    }
}
