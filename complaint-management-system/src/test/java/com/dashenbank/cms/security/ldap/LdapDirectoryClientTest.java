package com.dashenbank.cms.security.ldap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LdapDirectoryClientTest {

    @Test
    void formatsMicrosoftObjectGuid() {
        byte[] guid = new byte[] {
                0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10
        };
        assertEquals("04030201-0605-0807-090a-0b0c0d0e0f10", LdapDirectoryClient.objectGuidToString(guid));
    }

    @Test
    void escapesLdapFilterMeta() {
        assertEquals("a\\2a\\28b\\29", LdapDirectoryClient.escapeFilter("a*(b)"));
    }
}
