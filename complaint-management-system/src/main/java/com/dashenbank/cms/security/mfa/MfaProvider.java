package com.dashenbank.cms.security.mfa;

/**
 * Extension point for a future second factor. Not used in the login path yet.
 * When {@code MFA_READY=true}, the security dashboard shows this contract as
 * ready for TOTP/SMS/push without enabling a challenge today.
 */
public interface MfaProvider {

    String id();

    boolean supports(String username);

    MfaChallenge begin(String username);

    boolean verify(String username, String code);

    record MfaChallenge(String challengeId, String deliveryHint) {
    }
}
