package com.leasedesk.common;

public record RestoreUnitCommand(String applicationId, String unitId, int feeCents) {
}
