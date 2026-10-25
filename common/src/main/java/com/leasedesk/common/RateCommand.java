package com.leasedesk.common;

public record RateCommand(String applicationId, int months, int rentCents) {
}
