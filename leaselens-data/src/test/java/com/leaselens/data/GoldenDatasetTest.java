package com.leaselens.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.leaselens.data.load.DatasetLoader;
import com.leaselens.data.load.DatasetSource;
import com.leaselens.data.load.LoadResult;
import com.leaselens.data.model.ObligationStatus;
import com.leaselens.data.model.PortfolioDataset;
import com.leaselens.data.model.RentObligation;

/** The golden dataset (data/golden, see EXPECTED.md) must load cleanly with the documented values. */
class GoldenDatasetTest {

    private static PortfolioDataset load() throws Exception {
        LoadResult result = new DatasetLoader().load(DatasetSource.directory(TestData.dir("golden")));
        assertTrue(result.report().valid(), () -> "golden dataset should be valid: " + result.report().issues());
        assertEquals(0, result.report().warningCount(), () -> "unexpected warnings: " + result.report().issues());
        return result.dataset().orElseThrow();
    }

    @Test
    @DisplayName("Loads with no errors or warnings and the expected row counts")
    void loadsCleanly() throws Exception {
        PortfolioDataset d = load();
        assertEquals(2, d.properties().size());
        assertEquals(3, d.tenants().size());
        assertEquals(3, d.leases().size());
        assertEquals(8, d.obligations().size());
        assertEquals(8, d.transactions().size());
        assertEquals(6, d.expenses().size());
    }

    @Test
    @DisplayName("Horizon is derived as 2026-01-01 to 2026-03-31 (90 days)")
    void horizon() throws Exception {
        PortfolioDataset d = load();
        assertEquals(LocalDate.of(2026, 1, 1), d.horizonStart());
        assertEquals(LocalDate.of(2026, 3, 31), d.horizonEnd());
        assertEquals(90, d.horizonDays());
    }

    @Test
    @DisplayName("Money is parsed exactly into paise and statuses reconcile")
    void moneyAndStatuses() throws Exception {
        PortfolioDataset d = load();
        assertEquals(12_000_000L, d.lease("L002").monthlyRentPaise());           // ₹1,20,000.00
        assertEquals(1_000_000_000L, d.property("P001").acquisitionValuePaise()); // ₹1,00,00,000.00

        RentObligation partial = d.obligation("O006");
        assertEquals(ObligationStatus.PARTIAL, partial.status());
        assertEquals(6_000_000L, partial.balancePaise());
        assertNull(partial.settledDate());
        assertEquals(LocalDate.of(2026, 3, 20), d.obligation("O003").settledDate()); // settled by 2nd instalment
        assertEquals(ObligationStatus.UNPAID, d.obligation("O008").status());
    }
}
