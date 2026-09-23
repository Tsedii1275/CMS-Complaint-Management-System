package com.dashenbank.cms.integration.corebanking;

/**
 * Official customer profile retrieved in real time from Core Banking.
 * {@code phoneNumber} is the CBS account-opening number and is never an SMS destination.
 */
public record CoreBankingProfile(
        String customerName,
        String phoneNumber,
        String homeBranch,
        String homeDistrict,
        String customerSegment) {
}
