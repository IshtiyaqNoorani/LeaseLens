package com.leaselens.data.model;

/** Anonymous tenant: an identifier and a segment only. */
public record Tenant(String id, TenantSegment segment) {
}
