package com.leasedesk.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LeaseRulesTest {

    @Test
    void monthsStayOnTheBound() {
        assertTrue(LeaseRules.emptyMonths(0));
        assertFalse(LeaseRules.emptyMonths(1));
        assertFalse(LeaseRules.overMonths(24));
        assertTrue(LeaseRules.overMonths(25));
    }

    @Test
    void feeStaysInsideTheIntegerRange() {
        assertEquals(1200, LeaseRules.fee(12, 100));
        assertEquals(2400, LeaseRules.fee(24, 100));
        assertTrue(LeaseRules.hasSlot(1));
        assertFalse(LeaseRules.hasSlot(0));
        assertThrows(ArithmeticException.class, () -> LeaseRules.fee(2, Integer.MAX_VALUE));
    }

    @Test
    void lienedTenantsMatchTheCode() {
        assertTrue(LeaseRules.lienedTenant("P-lien"));
        assertTrue(LeaseRules.lienedTenant("unliened"));
        assertFalse(LeaseRules.lienedTenant("P-1"));
    }
}
