package com.dashenbank.cms.integration.corebanking;

import java.util.Optional;

/**
 * Live lookup of official customer KYC by account number from Oracle CBS
 * {@code RTVSCBS_CUST_PROFILE}. Results are not persisted as a CMS customer master.
 */
public interface CoreBankingClient {

    Optional<CoreBankingProfile> findByAccountNumber(String accountNumber);
}
