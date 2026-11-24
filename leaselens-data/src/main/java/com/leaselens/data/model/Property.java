package com.leaselens.data.model;

/**
 * A property in the portfolio.
 *
 * @param rentableAreaSqft      total lettable area in square feet
 * @param acquisitionValuePaise purchase price in paise (used only for the cash-yield ratio)
 */
public record Property(String id, String name, PropertyType type, String zone,
                       long rentableAreaSqft, long acquisitionValuePaise, int totalUnits) {
}
