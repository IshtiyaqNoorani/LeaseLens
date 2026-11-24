package com.leaselens.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.leaselens.api.service.DatasetService;

/** Loads the bundled sample at start-up (when enabled) so the app is usable immediately. */
@Component
public class SampleAutoloader {

    private static final Logger log = LoggerFactory.getLogger(SampleAutoloader.class);

    private final LeaseLensProperties properties;
    private final DatasetService datasets;

    public SampleAutoloader(LeaseLensProperties properties, DatasetService datasets) {
        this.properties = properties;
        this.datasets = datasets;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        if (properties.autoloadSample()) {
            var summary = datasets.loadBundled("sample");
            log.info("Loaded bundled sample: {} properties, {} obligations, horizon {} to {}",
                    summary.counts().properties(), summary.counts().obligations(),
                    summary.horizonStart(), summary.horizonEnd());
        }
    }
}
