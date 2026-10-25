package com.leasedesk.common;

public record VetCommand(String applicationId, String tenantCode, int feeCents) {
}
