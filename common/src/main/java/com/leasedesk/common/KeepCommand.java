package com.leasedesk.common;

public record KeepCommand(String applicationId, String unitId, int feeCents) {
}
