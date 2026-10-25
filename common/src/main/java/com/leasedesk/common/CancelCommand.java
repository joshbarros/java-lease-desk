package com.leasedesk.common;

public record CancelCommand(String applicationId, String unitId, int feeCents) {
}
