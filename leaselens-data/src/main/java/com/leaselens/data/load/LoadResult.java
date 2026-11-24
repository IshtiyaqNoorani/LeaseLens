package com.leaselens.data.load;

import java.util.Optional;

import com.leaselens.data.model.PortfolioDataset;
import com.leaselens.data.validation.ValidationReport;

/**
 * Outcome of loading. The dataset is present only when the report has no errors; a partially
 * valid dataset is never returned.
 */
public record LoadResult(ValidationReport report, Optional<PortfolioDataset> dataset) {
}
