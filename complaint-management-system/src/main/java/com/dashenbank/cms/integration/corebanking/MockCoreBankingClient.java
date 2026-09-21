package com.dashenbank.cms.integration.corebanking;

import com.dashenbank.cms.model.Customer;
import com.dashenbank.cms.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class MockCoreBankingClient implements CoreBankingClient {

    private final CustomerRepository customerRepository;

    public MockCoreBankingClient(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Optional<CoreBankingProfile> findByAccountNumber(String accountNumber) {
        if (!StringUtils.hasText(accountNumber)) {
            return Optional.empty();
        }
        return customerRepository.findByAccountNumber(accountNumber.trim())
                .map(MockCoreBankingClient::toProfile);
    }

    private static CoreBankingProfile toProfile(Customer customer) {
        return new CoreBankingProfile(
                customer.getCifNumber(),
                customer.getAccountNumber(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getHomeBranch(),
                customer.getDistrict(),
                customer.getCustomerSegment(),
                customer.getCustomerSubSegment());
    }
}
