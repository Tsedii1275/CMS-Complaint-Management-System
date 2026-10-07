package com.dashenbank.cms.notification.provider.datapower;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataPowerDestinationPolicyTest {

    @Test
    void defaultAllowListIsEthioTelecomOnly() {
        assertEquals(Set.of("09"), DataPowerDestinationPolicy.parseAllowedLocalPrefixes("09"));
        assertTrue(DataPowerDestinationPolicy.rejectIfUnsupported("0912345678", "09").isEmpty());
        assertTrue(DataPowerDestinationPolicy.rejectIfUnsupported("0712345678", "09").isPresent());
        assertTrue(DataPowerDestinationPolicy.rejectIfUnsupported("0712345678", "09").get().contains("safaricom"));
    }

    @Test
    void enablingSafaricomAllows07WithoutTreatingItAs09() {
        assertEquals(Set.of("09", "07"), DataPowerDestinationPolicy.parseAllowedLocalPrefixes("09,07"));
        assertTrue(DataPowerDestinationPolicy.rejectIfUnsupported("0712345678", "09, 07").isEmpty());
        assertTrue(DataPowerDestinationPolicy.rejectIfUnsupported("0912345678", "09,07").isEmpty());
    }

    @Test
    void rejectsUnknownPrefixEntries() {
        assertThrows(IllegalStateException.class, () -> DataPowerDestinationPolicy.parseAllowedLocalPrefixes("08"));
    }
}
