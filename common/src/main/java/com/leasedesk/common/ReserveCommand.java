package com.leasedesk.common;

public record ReserveCommand(String applicationId, String unitId, int feeCents) {
}
