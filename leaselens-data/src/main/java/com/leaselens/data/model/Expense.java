package com.leaselens.data.model;

import java.time.LocalDate;

/** An operating expense paid for a property (cash basis). */
public record Expense(String id, String propertyId, LocalDate expenseDate, ExpenseCategory category, long amountPaise) {
}
