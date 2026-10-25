package com.leasedesk.common;

public record InvoiceEvent(String applicationId, int feeCents, String status) {
}
