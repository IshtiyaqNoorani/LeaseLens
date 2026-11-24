package com.leaselens.data.model;

/**
 * Declared lease status. Informational only: analytics use the lease dates and the analysis date,
 * so a status that disagrees with the dates never changes a result.
 */
public enum LeaseStatus { ACTIVE, EXPIRED, TERMINATED }
