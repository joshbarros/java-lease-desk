package com.leasedesk.common;

import java.util.Locale;

public final class LeaseRules {
    public static final int OVER_MONTHS = 24;

    private LeaseRules() {
    }

    public static boolean emptyMonths(int months) {
        return months == 0;
    }

    public static boolean overMonths(int months) {
        return months > OVER_MONTHS;
    }

    public static int fee(int months, int rentCents) {
        return Math.multiplyExact(months, rentCents);
    }

    public static boolean hasSlot(int openSlots) {
        return openSlots > 0;
    }

    public static boolean lienedTenant(String tenantCode) {
        return tenantCode != null && tenantCode.toLowerCase(Locale.ROOT).contains("lien");
    }
}
