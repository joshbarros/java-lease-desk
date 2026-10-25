package com.leasedesk.common;

public final class Channels {
    public static final String OWNERS = "lease.owners";
    public static final String BUILDINGS = "lease.buildings";
    public static final String TENANTS = "lease.tenants";
    public static final String FLOORS = "lease.floors";
    public static final String UNITS = "lease.units";
    public static final String APPLICATIONS = "lease.applications";
    public static final String QUOTES = "lease.quotes";
    public static final String VETS = "lease.vets";
    public static final String HOLDS = "lease.holds";
    public static final String DEEDS = "lease.deeds";
    public static final String INVOICES = "lease.invoices";
    public static final String SIGNS = "lease.signs";
    public static final String VACATES = "lease.vacates";

    public static final String RATE = "lease.quote.rate";
    public static final String RATE_DLQ = "lease.quote.rate.dlq";
    public static final String RESERVE = "lease.unit.reserve";
    public static final String RESERVE_DLQ = "lease.unit.reserve.dlq";
    public static final String DECIDE = "lease.vet.decide";
    public static final String DECIDE_DLQ = "lease.vet.decide.dlq";
    public static final String VACATE = "lease.vacate.free";
    public static final String VACATE_DLQ = "lease.vacate.free.dlq";
    public static final String RELEASE_UNIT = "lease.unit.release";
    public static final String RELEASE_UNIT_DLQ = "lease.unit.release.dlq";
    public static final String DEED = "lease.deed.issue";
    public static final String DEED_DLQ = "lease.deed.issue.dlq";
    public static final String KEEP = "lease.unit.keep";
    public static final String KEEP_DLQ = "lease.unit.keep.dlq";
    public static final String CHARGE = "lease.invoice.charge";
    public static final String CHARGE_DLQ = "lease.invoice.charge.dlq";
    public static final String SIGN = "lease.sign.issue";
    public static final String SIGN_DLQ = "lease.sign.issue.dlq";
    public static final String BREAK = "lease.vacate.break";
    public static final String BREAK_DLQ = "lease.vacate.break.dlq";
    public static final String RESTORE = "lease.unit.restore";
    public static final String RESTORE_DLQ = "lease.unit.restore.dlq";

    private Channels() {
    }
}
