package com.leasedesk.common;

public record DeedEvent(String applicationId, int feeCents, String status) {
}
