package com.leasedesk.common;

public record HoldEvent(String applicationId, String unitId, int feeCents, String status) {
}
