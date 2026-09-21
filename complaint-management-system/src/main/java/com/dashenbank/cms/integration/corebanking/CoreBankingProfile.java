package com.dashenbank.cms.integration.corebanking;

/**
 * Official customer profile retrieved from Core Banking (or the local mock).
 * {@code registeredPhone} is the CBS number and is never an SMS destination.
 */
public record CoreBankingProfile(
        String cifNumber,
        String accountNumber,
        String name,
        String email,
        String registeredPhone,
        String homeBranch,
        String district,
        String customerSegment,
        String customerSubSegment) {
}
