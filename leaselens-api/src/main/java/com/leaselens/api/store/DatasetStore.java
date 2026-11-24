package com.leaselens.api.store;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.leaselens.api.service.ApiExceptions.NoDatasetException;

/** Holds the current snapshot. Swapped atomically so readers never see a half-loaded dataset. */
@Component
public class DatasetStore {

    private final AtomicReference<Snapshot> current = new AtomicReference<>();

    public Snapshot require() {
        Snapshot s = current.get();
        if (s == null) {
            throw new NoDatasetException();
        }
        return s;
    }

    public Optional<Snapshot> current() {
        return Optional.ofNullable(current.get());
    }

    public void replace(Snapshot snapshot) {
        current.set(snapshot);
    }
}
