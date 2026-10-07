package com.dashenbank.cms.notification.provider.datapower;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataPowerPhoneNormalizerTest {

    @Test
    void convertsEthioTelecomToLocal09() {
        assertEquals(Optional.of("0912345678"), DataPowerPhoneNormalizer.toLocalFormat("0912345678"));
        assertEquals(Optional.of("0912345678"), DataPowerPhoneNormalizer.toLocalFormat("+251912345678"));
        assertEquals(Optional.of("0912345678"), DataPowerPhoneNormalizer.toLocalFormat("251912345678"));
        assertEquals(Optional.of("0912345678"), DataPowerPhoneNormalizer.toLocalFormat("0912 345 678"));
        assertEquals(Optional.of("0912345678"), DataPowerPhoneNormalizer.toLocalFormat("00251912345678"));
        assertEquals(Optional.of("0912811791"), DataPowerPhoneNormalizer.toLocalFormat("0912811791"));
        assertEquals(Optional.of(EthiopianMobileNetwork.ETHIO_TELECOM),
                DataPowerPhoneNormalizer.networkOf("0912345678"));
    }

    @Test
    void convertsSafaricomToLocal07WithoutRewritingTo09() {
        assertEquals(Optional.of("0712345678"), DataPowerPhoneNormalizer.toLocalFormat("0712345678"));
        assertEquals(Optional.of("0712345678"), DataPowerPhoneNormalizer.toLocalFormat("+251712345678"));
        assertEquals(Optional.of("0712345678"), DataPowerPhoneNormalizer.toLocalFormat("251712345678"));
        assertEquals(Optional.of("0712345678"), DataPowerPhoneNormalizer.toLocalFormat("07 123 45678"));
        assertEquals(Optional.of("0712345678"), DataPowerPhoneNormalizer.toLocalFormat("00251712345678"));
        assertEquals(Optional.of(EthiopianMobileNetwork.SAFARICOM),
                DataPowerPhoneNormalizer.networkOf("0712345678"));
    }

    @Test
    void rejectsNonEthiopianMobiles() {
        assertTrue(DataPowerPhoneNormalizer.toLocalFormat("0812345678").isEmpty());
        assertTrue(DataPowerPhoneNormalizer.toLocalFormat("+251812345678").isEmpty());
        assertTrue(DataPowerPhoneNormalizer.toLocalFormat("912345678").isEmpty());
        assertTrue(DataPowerPhoneNormalizer.toLocalFormat("712345678").isEmpty());
        assertTrue(DataPowerPhoneNormalizer.toLocalFormat("091234567").isEmpty());
        assertTrue(DataPowerPhoneNormalizer.toLocalFormat("abc").isEmpty());
        assertTrue(DataPowerPhoneNormalizer.toLocalFormat("").isEmpty());
        assertTrue(DataPowerPhoneNormalizer.toLocalFormat(null).isEmpty());
    }
}
