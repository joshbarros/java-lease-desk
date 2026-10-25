package com.leasedesk.common;

public record VacateUnitCommand(String applicationId, String unitId, int feeCents) {
}
