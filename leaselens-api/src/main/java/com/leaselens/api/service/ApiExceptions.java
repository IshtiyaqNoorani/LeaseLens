package com.leaselens.api.service;

import com.leaselens.data.validation.ValidationReport;

/** Exceptions that map to specific HTTP statuses in {@code ApiExceptionHandler}. */
public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** 409: an analytics endpoint was called before any dataset was loaded. */
    public static final class NoDatasetException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public NoDatasetException() {
            super("No dataset is loaded. Load the sample (POST /api/datasets/sample) or upload CSV files first.");
        }
    }

    /** 404: an identifier in the URL or a parameter does not exist. */
    public static final class NotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public NotFoundException(String message) {
            super(message);
        }
    }

    /** 422: an uploaded dataset failed validation. The previously loaded dataset is kept. */
    public static final class DatasetRejectedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final transient ValidationReport report;

        public DatasetRejectedException(ValidationReport report) {
            super("The dataset was rejected: " + report.errorCount() + " error(s) found. "
                    + "The previously loaded dataset is still in use.");
            this.report = report;
        }

        public ValidationReport report() {
            return report;
        }
    }
}
