package com.dashenbank.cms.integration.corebanking;

import java.util.Optional;

/**
 * Lookup of official customer KYC by account number. Phase 3 uses the local
 * {@code customers} table as a mock; a live CBS adapter can replace the
 * implementation later without changing callers.
 */
public interface CoreBankingClient {

    Optional<CoreBankingProfile> findByAccountNumber(String accountNumber);
}
