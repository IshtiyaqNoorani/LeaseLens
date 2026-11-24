package com.leaselens.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.leaselens.data.load.DatasetFile;
import com.leaselens.data.load.DatasetLoader;
import com.leaselens.data.load.DatasetSource;
import com.leaselens.data.load.LoadResult;
import com.leaselens.data.validation.IssueCollector;
import com.leaselens.data.validation.Rule;
import com.leaselens.data.validation.Severity;

/** Validation behaviour not covered by the one-defect fixtures. */
class LoaderEdgeCasesTest {

    /** The golden files as editable in-memory text. */
    private static Map<String, String> golden() throws Exception {
        Map<String, String> files = new HashMap<>();
        for (DatasetFile f : DatasetFile.values()) {
            files.put(f.fileName(), Files.readString(TestData.dir("golden").resolve(f.fileName()), StandardCharsets.UTF_8));
        }
        return files;
    }

    private static LoadResult load(Map<String, String> files) throws Exception {
        Map<String, byte[]> bytes = new HashMap<>();
        files.forEach((k, v) -> bytes.put(k, v.getBytes(StandardCharsets.UTF_8)));
        return new DatasetLoader().load(DatasetSource.contents(bytes));
    }

    @Test
    @DisplayName("Uploaded contents load exactly like files on disk")
    void uploadedContents() throws Exception {
        assertTrue(load(golden()).report().valid());
    }

    @Test
    @DisplayName("Amounts with more than two decimals, thousands separators or text are rejected")
    void badAmounts() throws Exception {
        for (String bad : new String[] {"25000.001", "\"25,000.00\"", "twenty"}) {
            Map<String, String> files = golden();
            files.put("leases.csv", files.get("leases.csv").replace("25000.00", bad));
            LoadResult r = load(files);
            assertFalse(r.report().valid(), bad);
            assertTrue(r.report().has(Rule.INVALID_NUMBER), bad + " -> " + r.report().issues());
        }
    }

    @Test
    @DisplayName("Header matching ignores case; extra columns give a warning, not an error")
    void headerCaseAndExtraColumns() throws Exception {
        Map<String, String> files = golden();
        files.put("tenants.csv", files.get("tenants.csv")
                .replace("tenant_id,tenant_segment", "TENANT_ID,Tenant_Segment,notes")
                .replace("T001,INDIVIDUAL", "T001,individual,first")
                .replace("T002,SME", "T002,SME,")
                .replace("T003,CORPORATE", "T003,CORPORATE,"));
        LoadResult r = load(files);
        assertTrue(r.report().valid(), () -> r.report().issues().toString());
        assertTrue(r.report().issues().stream()
                .anyMatch(i -> i.rule() == Rule.UNKNOWN_COLUMN && i.severity() == Severity.WARNING));
    }

    @Test
    @DisplayName("An empty expenses file is only a warning; the dataset still loads")
    void emptyOptionalFile() throws Exception {
        Map<String, String> files = golden();
        files.put("expenses.csv", "expense_id,property_id,expense_date,expense_category,amount\n");
        LoadResult r = load(files);
        assertTrue(r.report().valid());
        assertTrue(r.report().has(Rule.EMPTY_FILE));
        assertEquals(0, r.dataset().orElseThrow().expenses().size());
    }

    @Test
    @DisplayName("Many bad rows: every error is counted but the stored list is capped")
    void issueCap() throws Exception {
        Map<String, String> files = golden();
        StringBuilder expenses = new StringBuilder("expense_id,property_id,expense_date,expense_category,amount\n");
        for (int i = 0; i < 2_500; i++) {
            expenses.append("X").append(i).append(",P001,not-a-date,REPAIRS,10.00\n");
        }
        files.put("expenses.csv", expenses.toString());
        LoadResult r = load(files);
        assertEquals(2_500, r.report().errorCount());
        assertEquals(IssueCollector.MAX_STORED_ISSUES, r.report().issues().size());
        assertTrue(r.report().truncated());
    }

    @Test
    @DisplayName("A lease may start the day after another ends on the same area (no false overlap)")
    void backToBackLeasesAreNotOverlap() throws Exception {
        Map<String, String> files = golden();
        // L001 ends 2026-06-30; give P001 a back-to-back lease on the same 1,000 sq ft from 2026-07-01.
        files.put("leases.csv", files.get("leases.csv") + "L004,P001,T001,2026-07-01,2027-06-30,26000.00,1000,1,ACTIVE\n");
        assertTrue(load(files).report().valid());
    }

    @Test
    @DisplayName("A missing file is reported by name and loading continues to report other problems")
    void missingFileStillReportsOthers() throws Exception {
        Map<String, String> files = golden();
        files.remove("tenants.csv");
        files.put("expenses.csv", files.get("expenses.csv").replace("P002,2026-03-25", "P777,2026-03-25"));
        LoadResult r = load(files);
        assertTrue(r.report().has(Rule.MISSING_FILE));
        assertTrue(r.report().has(Rule.UNKNOWN_REFERENCE));
    }

}
