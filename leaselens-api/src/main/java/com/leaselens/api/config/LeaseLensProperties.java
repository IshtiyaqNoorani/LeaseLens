package com.leaselens.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application settings (prefix {@code leaselens}).
 *
 * @param autoloadSample load the bundled sample dataset at start-up
 * @param attention      thresholds for the rule-based "requires attention" list
 */
@ConfigurationProperties(prefix = "leaselens")
public record LeaseLensProperties(boolean autoloadSample, Attention attention) {

    public LeaseLensProperties {
        if (attention == null) {
            attention = new Attention(90.0, 75.0, 45.0, 90, 1.0);
        }
    }

    /**
     * Transparent attention rules. A property is flagged when any rule fires.
     *
     * @param collectionEfficiencyBelowPct flag if cohort collection efficiency is below this
     * @param occupancyBelowPct            flag if time-weighted occupancy is below this
     * @param expenseRatioAbovePct         flag if operating expenses / rent collected is above this
     * @param overdueDays                  balances overdue for more than this many days count as long overdue
     * @param overdueShareAbovePct         flag if long-overdue balances exceed this % of rent billed in the period
     */
    public record Attention(double collectionEfficiencyBelowPct, double occupancyBelowPct,
                            double expenseRatioAbovePct, int overdueDays, double overdueShareAbovePct) {
    }
}
