package com.leasedesk.common;

public record VacateCommand(String applicationId, String unitId, int feeCents) {
}
