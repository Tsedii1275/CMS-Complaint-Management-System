package com.dashenbank.cms.customer;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocationKeysTest {

    @Test
    void customerHomePrefersExplicitKeysAndFallsBackToLegacyDistrictFromPriorSnapshot() {
        Map<String, Object> customer = new HashMap<>();
        customer.put(LocationKeys.CUSTOMER_HOME_BRANCH, "Bole Branch");
        customer.put(LocationKeys.CUSTOMER_HOME_DISTRICT, "Central District");
        customer.put(LocationKeys.LEGACY_DISTRICT, "ShouldNotWin");

        assertEquals("Bole Branch", LocationKeys.customerHomeBranch(customer));
        assertEquals("Central District", LocationKeys.customerHomeDistrict(customer));
        assertEquals("Central District",
                LocationKeys.customerHomeDistrict(Map.of(LocationKeys.LEGACY_DISTRICT, "Central District")));
    }

    @Test
    void customerHomeBranchFallsBackToLegacyHomeBranchOnly() {
        Map<String, Object> customer = Map.of(LocationKeys.LEGACY_HOME_BRANCH, "Bole Branch");
        assertEquals("Bole Branch", LocationKeys.customerHomeBranch(customer));
    }

    @Test
    void complaintLocationPrefersExplicitKeysOverLegacy() {
        Map<String, Object> complaint = new HashMap<>();
        complaint.put(LocationKeys.COMPLAINT_BRANCH, "Hawassa Branch");
        complaint.put(LocationKeys.COMPLAINT_DISTRICT, "Southern District");
        complaint.put(LocationKeys.LEGACY_BRANCH, "Bole Branch");
        complaint.put(LocationKeys.LEGACY_DISTRICT, "Central District");

        assertEquals("Hawassa Branch", LocationKeys.complaintBranch(Map.of(), complaint));
        assertEquals("Southern District", LocationKeys.complaintDistrict(Map.of(), complaint));
    }

    @Test
    void complaintLocationDoesNotUseCustomerHomeBranch() {
        Map<String, Object> complaint = Map.of(LocationKeys.LEGACY_HOME_BRANCH, "Bole Branch");
        assertNull(LocationKeys.complaintBranch(Map.of(), complaint));
    }

    @Test
    void applyWritesOnlyExplicitKeys() {
        Map<String, Object> customer = new HashMap<>();
        LocationKeys.applyCustomerHome(customer, "Bole Branch", "Central District");
        assertEquals("Bole Branch", customer.get(LocationKeys.CUSTOMER_HOME_BRANCH));
        assertEquals("Central District", customer.get(LocationKeys.CUSTOMER_HOME_DISTRICT));
        assertNull(customer.get(LocationKeys.LEGACY_BRANCH));
        assertNull(customer.get(LocationKeys.LEGACY_DISTRICT));
        assertNull(customer.get(LocationKeys.LEGACY_HOME_BRANCH));

        Map<String, Object> complaint = new HashMap<>();
        LocationKeys.applyComplaintLocation(complaint, "Hawassa Branch", "Southern District");
        assertEquals("Hawassa Branch", complaint.get(LocationKeys.COMPLAINT_BRANCH));
        assertEquals("Southern District", complaint.get(LocationKeys.COMPLAINT_DISTRICT));
        assertNull(complaint.get(LocationKeys.LEGACY_BRANCH));
        assertNull(complaint.get(LocationKeys.LEGACY_DISTRICT));
    }
}
