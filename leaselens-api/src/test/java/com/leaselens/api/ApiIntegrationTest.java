package com.leaselens.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.leaselens.data.load.DatasetFile;

/**
 * End-to-end HTTP tests on the golden dataset: every endpoint's happy path, the error statuses,
 * uploads, and the "as of analysis date" behaviour. Expected numbers come from data/golden/EXPECTED.md.
 */
@SpringBootTest(properties = "leaselens.autoload-sample=false")
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String PROBLEM = "application/problem+json";

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void loadGolden() throws Exception {
        mvc.perform(post("/api/datasets/sample").param("name", "golden"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("golden"))
                .andExpect(jsonPath("$.days").value(90));
    }

    private static Path data(String child) {
        String dir = System.getProperty("leaselens.dataDir", "../data");
        return Path.of(dir).resolve(child);
    }

    private MockMultipartHttpServletRequestBuilder uploadFrom(Path dir) throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/datasets/upload");
        for (DatasetFile f : DatasetFile.values()) {
            Path file = dir.resolve(f.fileName());
            if (Files.exists(file)) {
                req.file(new MockMultipartFile("files", f.fileName(), "text/csv", Files.readAllBytes(file)));
            }
        }
        return req;
    }

    // ------------------------------------------------------------------ datasets

    @Test
    @DisplayName("Bundled sample loads: 50 properties over 1,096 days")
    void loadSample() throws Exception {
        mvc.perform(post("/api/datasets/sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.properties").value(50))
                .andExpect(jsonPath("$.days").value(1096))
                .andExpect(jsonPath("$.analysisDate").value("2026-10-31"));
    }

    @Test
    @DisplayName("Unknown bundled dataset gives 404")
    void unknownBundled() throws Exception {
        mvc.perform(post("/api/datasets/sample").param("name", "nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    @Test
    @DisplayName("Valid CSV upload replaces the dataset")
    void uploadValid() throws Exception {
        mvc.perform(uploadFrom(data("golden")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("upload"))
                .andExpect(jsonPath("$.counts.obligations").value(8));
    }

    @Test
    @DisplayName("Invalid upload gives 422 with the validation report and keeps the previous dataset")
    void uploadInvalidKeepsPrevious() throws Exception {
        mvc.perform(uploadFrom(data("invalid/15-overpayment")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.validation.errorCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.validation.issues[*].rule").value(hasItem("OVERPAYMENT")));
        mvc.perform(get("/api/datasets/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("golden"));
    }

    @Test
    @DisplayName("Upload with a missing file is rejected with MISSING_FILE")
    void uploadMissingFile() throws Exception {
        mvc.perform(uploadFrom(data("invalid/01-missing-file")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.validation.issues[*].rule").value(hasItem("MISSING_FILE")));
    }

    @Test
    @DisplayName("Upload of an unexpected file name gives 400")
    void uploadUnexpectedName() throws Exception {
        mvc.perform(multipart("/api/datasets/upload")
                        .file(new MockMultipartFile("files", "notes.txt", "text/plain", "x".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("not one of the expected files")));
    }

    @Test
    @DisplayName("Moving the analysis date changes every 'as of' figure")
    void analysisDate() throws Exception {
        mvc.perform(put("/api/datasets/current/analysis-date").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"analysisDate\":\"2026-02-28\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisDate").value("2026-02-28"));
        // Due by Feb 28: O001, O002, O004, O005, O007, all settled by then.
        mvc.perform(get("/api/portfolio/overview"))
                .andExpect(jsonPath("$.to").value("2026-02-28"))
                .andExpect(jsonPath("$.outstandingPaise").value(0))
                .andExpect(jsonPath("$.occupancyPct").value(80.0));
        mvc.perform(get("/api/payments/percentiles").param("p", "50").param("seed", "3"))
                .andExpect(jsonPath("$.population.completed").value(5))
                .andExpect(jsonPath("$.population.openExcluded").value(0))
                .andExpect(jsonPath("$.percentiles[0].daysLate").value(2));
        mvc.perform(get("/api/portfolio/overview").param("to", "2026-03-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("after the analysis date")));
    }

    @Test
    @DisplayName("Analysis date outside the data gives 400")
    void analysisDateOutOfRange() throws Exception {
        mvc.perform(put("/api/datasets/current/analysis-date").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"analysisDate\":\"2027-01-01\"}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ portfolio and properties

    @Test
    @DisplayName("Overview returns the golden KPIs")
    void overview() throws Exception {
        mvc.perform(get("/api/portfolio/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rentBilledPaise").value(49_500_000))
                .andExpect(jsonPath("$.rentCollectedPaise").value(40_500_000))
                .andExpect(jsonPath("$.collectionEfficiencyPct").value(81.82))
                .andExpect(jsonPath("$.netOperatingCashSurplusPaise").value(33_500_000))
                .andExpect(jsonPath("$.monthlyTrend.length()").value(3));
    }

    @Test
    @DisplayName("League table sorts and rejects unknown sort keys")
    void performance() throws Exception {
        mvc.perform(get("/api/properties/performance").param("sort", "collectionEfficiencyPct").param("order", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.rows[0].propertyId").value("P002"));
        mvc.perform(get("/api/properties/performance").param("sort", "shoeSize"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    @Test
    @DisplayName("Property detail, and 404 for an unknown property")
    void propertyDetail() throws Exception {
        mvc.perform(get("/api/properties/P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.netOperatingCashSurplusPaise").value(8_500_000))
                .andExpect(jsonPath("$.leases.length()").value(2));
        mvc.perform(get("/api/properties/P999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("P999")));
    }

    // ------------------------------------------------------------------ cash flow (segment tree)

    @Test
    @DisplayName("Range query is answered by the Segment Tree")
    void cashFlowRange() throws Exception {
        mvc.perform(get("/api/cashflow/range").param("scope", "portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netCashFlowPaise").value(33_500_000))
                .andExpect(jsonPath("$.maxDate").value("2026-01-04"))
                .andExpect(jsonPath("$.algorithm.name").value("SegmentTree"))
                .andExpect(jsonPath("$.algorithm.syllabusUnit").value(3))
                .andExpect(jsonPath("$.algorithm.operations").value(greaterThan(0)));
    }

    @Test
    @DisplayName("Correction workflow: add, query reflects it, list, undo, reset")
    void adjustmentWorkflow() throws Exception {
        mvc.perform(post("/api/cashflow/adjustments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"propertyId\":\"P002\",\"date\":\"2026-01-20\",\"amountPaise\":4000000,"
                                + "\"reason\":\"Reversed tax charge\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adjustment.id").value("ADJ-0001"))
                .andExpect(jsonPath("$.algorithm.operation").value(containsString("pointUpdate")));
        mvc.perform(get("/api/cashflow/range"))
                .andExpect(jsonPath("$.netCashFlowPaise").value(37_500_000))
                .andExpect(jsonPath("$.adjustmentsApplied").value(1));
        mvc.perform(get("/api/cashflow/adjustments")).andExpect(jsonPath("$.adjustments.length()").value(1));
        mvc.perform(delete("/api/cashflow/adjustments/ADJ-0001")).andExpect(status().isOk());
        mvc.perform(get("/api/cashflow/range")).andExpect(jsonPath("$.netCashFlowPaise").value(33_500_000));
        mvc.perform(delete("/api/cashflow/adjustments/ADJ-0001")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/cashflow/adjustments")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Adjustments are validated: property, date, non-zero amount, reason")
    void adjustmentValidation() throws Exception {
        String[] bad = {
            "{\"date\":\"2026-01-20\",\"amountPaise\":100,\"reason\":\"x\"}",
            "{\"propertyId\":\"P001\",\"date\":\"2030-01-01\",\"amountPaise\":100,\"reason\":\"x\"}",
            "{\"propertyId\":\"P001\",\"date\":\"2026-01-20\",\"amountPaise\":0,\"reason\":\"x\"}",
            "{\"propertyId\":\"P001\",\"date\":\"2026-01-20\",\"amountPaise\":100,\"reason\":\"  \"}",
            "{\"propertyId\":\"P001\",\"date\":\"not-a-date\",\"amountPaise\":100,\"reason\":\"x\"}"
        };
        for (String body : bad) {
            mvc.perform(post("/api/cashflow/adjustments").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM));
        }
    }

    @Test
    @DisplayName("Periods and series endpoints")
    void periodsAndSeries() throws Exception {
        mvc.perform(get("/api/cashflow/periods").param("granularity", "QUARTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periods.length()").value(1))
                .andExpect(jsonPath("$.periods[0].label").value("2026-Q1"))
                .andExpect(jsonPath("$.periods[0].netCashFlowPaise").value(33_500_000));
        mvc.perform(get("/api/cashflow/series").param("from", "2026-01-01").param("to", "2026-01-05"))
                .andExpect(jsonPath("$.points.length()").value(5))
                .andExpect(jsonPath("$.points[3].netPaise").value(12_000_000));
        mvc.perform(get("/api/cashflow/periods").param("granularity", "WEEK")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Input errors: bad date, reversed range, unknown scope")
    void inputErrors() throws Exception {
        mvc.perform(get("/api/cashflow/range").param("from", "2026-13-01"))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(PROBLEM));
        mvc.perform(get("/api/cashflow/range").param("from", "2026-03-01").param("to", "2026-02-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("is after")));
        mvc.perform(get("/api/cashflow/range").param("from", "2025-12-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/cashflow/range").param("scope", "P404"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ payments (quickselect)

    @Test
    @DisplayName("Percentiles come from Randomized Quickselect and report the seed")
    void percentiles() throws Exception {
        mvc.perform(get("/api/payments/percentiles").param("p", "50,75,90").param("seed", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentiles[1].daysLate").value(15))
                .andExpect(jsonPath("$.algorithm.name").value("RandomizedQuickselect"))
                .andExpect(jsonPath("$.algorithm.syllabusUnit").value(6))
                .andExpect(jsonPath("$.algorithm.seed").value(42))
                .andExpect(jsonPath("$.censoringNote").value(containsString("2 obligation(s)")));
        // Without a seed the server chooses one and returns it, so the run can be repeated.
        mvc.perform(get("/api/payments/percentiles"))
                .andExpect(jsonPath("$.algorithm.seed").isNumber());
    }

    @Test
    @DisplayName("Invalid percentiles and filters give 400 or 404")
    void percentileErrors() throws Exception {
        for (String p : new String[] {"0", "101", "abc", "99.999"}) {
            mvc.perform(get("/api/payments/percentiles").param("p", p)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/payments/percentiles").param("propertyType", "CASTLE")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/payments/percentiles").param("propertyId", "P999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("No matching completed payments gives an explicit message, not a made-up value")
    void emptySelection() throws Exception {
        mvc.perform(get("/api/payments/percentiles").param("propertyType", "OFFICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.population.completed").value(0))
                .andExpect(jsonPath("$.percentiles.length()").value(0))
                .andExpect(jsonPath("$.message").value(containsString("No completed payments")));
    }

    @Test
    @DisplayName("Threshold records, histogram and outstanding balances")
    void thresholdAndOutstanding() throws Exception {
        mvc.perform(get("/api/payments/threshold").param("p", "75").param("seed", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdDaysLate").value(15))
                .andExpect(jsonPath("$.countAtOrAbove").value(2))
                .andExpect(jsonPath("$.histogram.length()").value(8));
        mvc.perform(get("/api/payments/outstanding"))
                .andExpect(jsonPath("$.totalOutstandingPaise").value(9_000_000))
                .andExpect(jsonPath("$.openObligations").value(2));
    }

    // ------------------------------------------------------------------ expenses

    @Test
    @DisplayName("Expense summary and unusual-record analysis")
    void expenses() throws Exception {
        mvc.perform(get("/api/expenses/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPaise").value(7_000_000))
                .andExpect(jsonPath("$.byCategory[0].category").value("PROPERTY_TAX"));
        // Golden groups are too small for quartiles: all are skipped and nothing is flagged.
        mvc.perform(get("/api/expenses/unusual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupsAnalysed").value(0))
                .andExpect(jsonPath("$.records.length()").value(0));
    }
}
