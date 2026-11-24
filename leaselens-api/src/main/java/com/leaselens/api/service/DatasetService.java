package com.leaselens.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.leaselens.api.service.ApiExceptions.DatasetRejectedException;
import com.leaselens.api.service.ApiExceptions.NotFoundException;
import com.leaselens.api.store.DatasetStore;
import com.leaselens.api.store.Snapshot;
import com.leaselens.data.load.DatasetLoader;
import com.leaselens.data.load.DatasetSource;
import com.leaselens.data.load.LoadResult;
import com.leaselens.data.model.PortfolioDataset;
import com.leaselens.data.validation.FileSummary;
import com.leaselens.data.validation.Severity;
import com.leaselens.data.validation.ValidationIssue;

/** Loads datasets (bundled or uploaded), validates them and manages the analysis date. */
@Service
public class DatasetService {

    /** Datasets packaged inside the application jar (copied from the repository's data folder at build time). */
    public static final List<String> BUNDLED = List.of("sample", "golden");

    private final DatasetStore store;
    private final DatasetLoader loader = new DatasetLoader();

    public DatasetService(DatasetStore store) {
        this.store = store;
    }

    public record Counts(int properties, int tenants, int leases, int obligations, int transactions, int expenses) {
    }

    public record DatasetSummary(String source, LocalDate horizonStart, LocalDate horizonEnd, int days,
                                 LocalDate analysisDate, Counts counts, List<ValidationIssue> warnings,
                                 List<FileSummary> files, int adjustments) {
    }

    public DatasetSummary loadBundled(String name) {
        String n = name == null || name.isBlank() ? "sample" : name.strip();
        if (!BUNDLED.contains(n)) {
            throw new NotFoundException("No bundled dataset called '" + name + "'. Available: " + BUNDLED + ".");
        }
        DatasetSource source = fileName -> {
            InputStream in = DatasetService.class.getResourceAsStream("/datasets/" + n + "/" + fileName);
            return Optional.ofNullable(in).map(s -> (Reader) new InputStreamReader(s, StandardCharsets.UTF_8));
        };
        return accept(load(source), n);
    }

    /** Validates uploaded files. On any error the current dataset is kept and a 422 is raised. */
    public DatasetSummary upload(Map<String, byte[]> files) {
        return accept(load(DatasetSource.contents(files)), "upload");
    }

    public DatasetSummary current() {
        return summary(store.require(), List.of(), List.of());
    }

    public DatasetSummary setAnalysisDate(LocalDate date) {
        Snapshot s = store.require();
        if (date == null) {
            throw new IllegalArgumentException("analysisDate is required (YYYY-MM-DD).");
        }
        PortfolioDataset d = s.dataset();
        if (date.isBefore(d.horizonStart()) || date.isAfter(d.horizonEnd())) {
            throw new IllegalArgumentException("analysisDate " + date + " must be within the data ("
                    + d.horizonStart() + " to " + d.horizonEnd() + ").");
        }
        Snapshot updated = s.withAnalysisDate(date);
        store.replace(updated);
        return summary(updated, List.of(), List.of());
    }

    private LoadResult load(DatasetSource source) {
        try {
            return loader.load(source);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the dataset files.", e);
        }
    }

    private DatasetSummary accept(LoadResult result, String sourceName) {
        if (result.dataset().isEmpty()) {
            throw new DatasetRejectedException(result.report());
        }
        Snapshot snapshot = Snapshot.of(result.dataset().get(), sourceName);
        store.replace(snapshot);
        List<ValidationIssue> warnings = result.report().issues().stream()
                .filter(i -> i.severity() == Severity.WARNING).toList();
        return summary(snapshot, warnings, result.report().files());
    }

    private static DatasetSummary summary(Snapshot s, List<ValidationIssue> warnings, List<FileSummary> files) {
        PortfolioDataset d = s.dataset();
        return new DatasetSummary(s.source(), d.horizonStart(), d.horizonEnd(), d.horizonDays(), s.analysisDate(),
                new Counts(d.properties().size(), d.tenants().size(), d.leases().size(), d.obligations().size(),
                        d.transactions().size(), d.expenses().size()),
                warnings, files, s.cashFlow().adjustments().size());
    }
}
