package com.leasedesk.common;

public record UnitOpened(String unitId, String ownerId, String buildingId, String unitCode, int slots, int rentCents) {
}
