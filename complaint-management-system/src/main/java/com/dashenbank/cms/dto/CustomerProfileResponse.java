package com.dashenbank.cms.dto;

import com.dashenbank.cms.integration.corebanking.CoreBankingProfile;

/**
 * Live CBS customer profile returned by {@code GET /api/customer-profile/{accountNumber}}.
 * Account number is the lookup key only and is not part of this payload.
 */
public class CustomerProfileResponse {

    private final String customerName;
    private final String phoneNumber;
    private final String homeBranch;
    private final String homeDistrict;
    private final String customerSegment;

    public CustomerProfileResponse(String customerName, String phoneNumber, String homeBranch, String homeDistrict,
            String customerSegment) {
        this.customerName = nullToEmpty(customerName);
        this.phoneNumber = nullToEmpty(phoneNumber);
        this.homeBranch = nullToEmpty(homeBranch);
        this.homeDistrict = nullToEmpty(homeDistrict);
        this.customerSegment = nullToEmpty(customerSegment);
    }

    public static CustomerProfileResponse from(CoreBankingProfile profile) {
        return new CustomerProfileResponse(
                profile.customerName(),
                profile.phoneNumber(),
                profile.homeBranch(),
                profile.homeDistrict(),
                profile.customerSegment());
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getHomeBranch() {
        return homeBranch;
    }

    public String getHomeDistrict() {
        return homeDistrict;
    }

    public String getCustomerSegment() {
        return customerSegment;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
