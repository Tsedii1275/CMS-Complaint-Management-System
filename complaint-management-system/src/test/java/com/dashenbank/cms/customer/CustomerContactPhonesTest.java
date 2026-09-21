package com.dashenbank.cms.customer;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomerContactPhonesTest {

    @Test
    void currentContactPrefersDedicatedFieldOverLegacyPhone() {
        Map<String, Object> customer = new HashMap<>();
        customer.put(CustomerContactPhones.CURRENT_CONTACT_PHONE, "+251944444444");
        customer.put(CustomerContactPhones.LEGACY_PHONE, "+251911111111");
        customer.put(CustomerContactPhones.CORE_BANKING_PHONE, "+251911111111");

        assertEquals("+251944444444", CustomerContactPhones.currentContact(customer));
    }

    @Test
    void currentContactFallsBackToLegacyPhoneForInFlightCases() {
        Map<String, Object> customer = Map.of(CustomerContactPhones.LEGACY_PHONE, "+251944444444");

        assertEquals("+251944444444", CustomerContactPhones.currentContact(customer));
    }

    @Test
    void currentContactDoesNotFallBackToCoreBankingPhone() {
        Map<String, Object> customer = Map.of(
                CustomerContactPhones.CORE_BANKING_PHONE, "+251911111111");

        assertNull(CustomerContactPhones.currentContact(customer));
        assertEquals("+251911111111", CustomerContactPhones.coreBanking(customer));
    }

    @Test
    void applyCurrentContactWritesCanonicalAndLegacyAlias() {
        Map<String, Object> customerVars = new HashMap<>();
        CustomerContactPhones.applyCurrentContact(customerVars, "+251944444444");

        assertEquals("+251944444444", customerVars.get(CustomerContactPhones.CURRENT_CONTACT_PHONE));
        assertEquals("+251944444444", customerVars.get(CustomerContactPhones.LEGACY_PHONE));
    }

    @Test
    void fromRequestAcceptsEitherPayloadKey() {
        assertEquals("+251944444444",
                CustomerContactPhones.fromRequest(Map.of(CustomerContactPhones.CURRENT_CONTACT_PHONE, "+251944444444")));
        assertEquals("+251944444444",
                CustomerContactPhones.fromRequest(Map.of(CustomerContactPhones.LEGACY_PHONE, "+251944444444")));
    }
}
