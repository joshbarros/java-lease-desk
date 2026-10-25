package com.leasedesk.common;

public record QuoteEvent(String applicationId, int feeCents, String status, String reason) {
}
