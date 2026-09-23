package com.dashenbank.cms.integration.corebanking;

import com.dashenbank.cms.config.CbsProperties;
import com.dashenbank.cms.exception.CbsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CbsCustomerProfileServiceTest {

    @Mock
    private CbsCustomerProfileRepository repository;

    @Test
    void returnsMappedProfile() {
        CoreBankingProfile row = new CoreBankingProfile(
                "Abebe Kebede", "0912345678", "Bole Branch", "Addis District", "Retail");
        when(repository.isConfigured()).thenReturn(true);
        when(repository.findByAccountNumber("1001234567890")).thenReturn(List.of(row));

        Optional<CoreBankingProfile> found = new CbsCustomerProfileService(repository)
                .findByAccountNumber("1001234567890");

        assertTrue(found.isPresent());
        assertEquals("Abebe Kebede", found.get().customerName());
        assertEquals("0912345678", found.get().phoneNumber());
        assertEquals("Bole Branch", found.get().homeBranch());
        assertEquals("Addis District", found.get().homeDistrict());
        assertEquals("Retail", found.get().customerSegment());
    }

    @Test
    void returnsEmptyWhenAccountUnknown() {
        when(repository.isConfigured()).thenReturn(true);
        when(repository.findByAccountNumber("0000000000000")).thenReturn(List.of());

        assertTrue(new CbsCustomerProfileService(repository).findByAccountNumber("0000000000000").isEmpty());
        assertTrue(new CbsCustomerProfileService(repository).findByAccountNumber(" ").isEmpty());
    }

    @Test
    void unavailableWhenDatasourceMissing() {
        when(repository.isConfigured()).thenReturn(false);
        CbsException ex = assertThrows(CbsException.class,
                () -> new CbsCustomerProfileService(repository).findByAccountNumber("1001234567890"));
        assertEquals("CBS_UNAVAILABLE", ex.getCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
    }

    @Test
    void unavailableWhenOracleUnreachable() {
        when(repository.isConfigured()).thenReturn(true);
        when(repository.findByAccountNumber("1001234567890"))
                .thenThrow(new CannotGetJdbcConnectionException("CBS down"));

        CbsException ex = assertThrows(CbsException.class,
                () -> new CbsCustomerProfileService(repository).findByAccountNumber("1001234567890"));
        assertEquals("CBS_UNAVAILABLE", ex.getCode());
    }

    @Test
    void timeoutWhenQueryExceedsLimit() {
        when(repository.isConfigured()).thenReturn(true);
        when(repository.findByAccountNumber("1001234567890"))
                .thenThrow(new QueryTimeoutException("ORA-01013"));

        CbsException ex = assertThrows(CbsException.class,
                () -> new CbsCustomerProfileService(repository).findByAccountNumber("1001234567890"));
        assertEquals("CBS_TIMEOUT", ex.getCode());
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, ex.getStatus());
    }

    @Test
    void columnDefaultsMatchConfirmedViewStyle() {
        CbsProperties.Columns columns = new CbsProperties.Columns();
        assertEquals("CUSTOMER_NAME", columns.getCustomerName());
        assertEquals("PHONE_NUMBER", columns.getPhoneNumber());
        assertEquals("HOME_BRANCH", columns.getHomeBranch());
        assertEquals("HOME_DISTRICT", columns.getHomeDistrict());
        assertEquals("CUSTOMER_SEGMENT", columns.getCustomerSegment());
    }
}
