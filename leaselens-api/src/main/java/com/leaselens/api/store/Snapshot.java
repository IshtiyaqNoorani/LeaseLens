package com.leaselens.api.store;

import java.time.LocalDate;

import com.leaselens.data.model.PortfolioDataset;

/**
 * The loaded dataset plus everything derived from it. Replaced as a whole when a new dataset is
 * loaded or the analysis date changes. The cash-flow index (and its adjustments) is shared between
 * snapshots of the same dataset.
 *
 * @param source       where the data came from: "sample", "golden" or "upload"
 * @param analysisDate the "as of" date for balances, occupancy and settled payments
 */
public record Snapshot(PortfolioDataset dataset, String source, LocalDate analysisDate, Ledger ledger,
                       CashFlowIndex cashFlow) {

    public static Snapshot of(PortfolioDataset dataset, String source) {
        return new Snapshot(dataset, source, dataset.horizonEnd(), new Ledger(dataset), new CashFlowIndex(dataset));
    }

    public Snapshot withAnalysisDate(LocalDate date) {
        return new Snapshot(dataset, source, date, ledger, cashFlow);
    }
}
