package com.dashenbank.cms.integration.corebanking;

import com.dashenbank.cms.model.Customer;
import com.dashenbank.cms.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockCoreBankingClientTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private MockCoreBankingClient client;

    @Test
    void mapsLocalCustomerAsCoreBankingProfile() {
        Customer row = Customer.builder()
                .cifNumber("CIF10002")
                .accountNumber("5555666677778")
                .name("Abebe Bekele")
                .email("abebe.b@gmail.com")
                .phoneNumber("+251911223344")
                .homeBranch("Bole Branch")
                .district("Central District")
                .customerSegment("Retail")
                .customerSubSegment("Pensioner")
                .build();
        when(customerRepository.findByAccountNumber("5555666677778")).thenReturn(Optional.of(row));

        Optional<CoreBankingProfile> found = client.findByAccountNumber("5555666677778");

        assertTrue(found.isPresent());
        assertEquals("CIF10002", found.get().cifNumber());
        assertEquals("+251911223344", found.get().registeredPhone());
        assertEquals("Bole Branch", found.get().homeBranch());
        assertEquals("Central District", found.get().district());
    }

    @Test
    void returnsEmptyWhenAccountUnknown() {
        when(customerRepository.findByAccountNumber("0000000000000")).thenReturn(Optional.empty());

        assertTrue(client.findByAccountNumber("0000000000000").isEmpty());
        assertTrue(client.findByAccountNumber(" ").isEmpty());
    }
}
