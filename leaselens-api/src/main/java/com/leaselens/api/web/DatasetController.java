package com.leaselens.api.web;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.leaselens.api.service.DatasetService;
import com.leaselens.api.service.DatasetService.DatasetSummary;
import com.leaselens.data.load.DatasetFile;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    private final DatasetService datasets;

    public DatasetController(DatasetService datasets) {
        this.datasets = datasets;
    }

    /** Loads a dataset bundled with the application: {@code sample} (default) or {@code golden}. */
    @PostMapping("/sample")
    public DatasetSummary loadSample(@RequestParam(defaultValue = "sample") String name) {
        return datasets.loadBundled(name);
    }

    /**
     * Uploads the six CSV files. Each part is matched by its file name (e.g. {@code leases.csv}) or by
     * its form field name (e.g. {@code leases} or {@code leases.csv}). Missing files are reported by the
     * validator, not silently ignored.
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DatasetSummary upload(MultipartHttpServletRequest request) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        for (Map.Entry<String, java.util.List<MultipartFile>> part : request.getMultiFileMap().entrySet()) {
            for (MultipartFile file : part.getValue()) {
                String name = datasetName(file.getOriginalFilename());
                if (name == null) {
                    name = datasetName(part.getKey());
                }
                if (name == null) {
                    throw new IllegalArgumentException("Uploaded file '" + file.getOriginalFilename()
                            + "' is not one of the expected files: properties.csv, tenants.csv, leases.csv, "
                            + "rent_obligations.csv, rent_transactions.csv, expenses.csv.");
                }
                if (files.containsKey(name)) {
                    throw new IllegalArgumentException(name + " was uploaded more than once.");
                }
                files.put(name, file.getBytes());
            }
        }
        return datasets.upload(files);
    }

    @GetMapping("/current")
    public DatasetSummary current() {
        return datasets.current();
    }

    public record AnalysisDateRequest(LocalDate analysisDate) {
    }

    @PutMapping("/current/analysis-date")
    public DatasetSummary setAnalysisDate(@RequestBody AnalysisDateRequest request) {
        return datasets.setAnalysisDate(request == null ? null : request.analysisDate());
    }

    private static String datasetName(String raw) {
        if (raw == null) {
            return null;
        }
        String base = raw.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1).strip().toLowerCase(java.util.Locale.ROOT);
        for (DatasetFile f : DatasetFile.values()) {
            if (base.equals(f.fileName()) || (base + ".csv").equals(f.fileName())) {
                return f.fileName();
            }
        }
        return null;
    }
}
