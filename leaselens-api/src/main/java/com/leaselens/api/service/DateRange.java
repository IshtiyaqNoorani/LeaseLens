package com.leaselens.api.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.leaselens.api.store.Snapshot;

/** An inclusive date range validated against the loaded dataset. */
public record DateRange(LocalDate from, LocalDate to) {

    public int days() {
        return (int) ChronoUnit.DAYS.between(from, to) + 1;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    /**
     * Defaults: {@code from} = horizon start, {@code to} = analysis date. The range must lie within
     * the horizon and must not go past the analysis date, because nothing after it is "known".
     */
    public static DateRange resolve(LocalDate from, LocalDate to, Snapshot s) {
        LocalDate start = from != null ? from : s.dataset().horizonStart();
        LocalDate end = to != null ? to : s.analysisDate();
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("from (" + start + ") is after to (" + end + ").");
        }
        if (start.isBefore(s.dataset().horizonStart())) {
            throw new IllegalArgumentException("from (" + start + ") is before the data begins ("
                    + s.dataset().horizonStart() + ").");
        }
        if (end.isAfter(s.analysisDate())) {
            throw new IllegalArgumentException("to (" + end + ") is after the analysis date (" + s.analysisDate()
                    + "). Move the analysis date or choose an earlier end date.");
        }
        return new DateRange(start, end);
    }
}
