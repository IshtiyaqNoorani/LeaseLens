package com.leaselens.data.model;

import java.time.LocalDate;

/** A payment received against a rent obligation. */
public record RentTransaction(String id, String obligationId, LocalDate transactionDate, long amountPaise) {
}
