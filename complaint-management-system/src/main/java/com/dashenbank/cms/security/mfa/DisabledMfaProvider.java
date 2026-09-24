package com.dashenbank.cms.security.mfa;

import org.springframework.stereotype.Component;

/**
 * Placeholder so the MFA contract is wired. Login does not call this yet.
 */
@Component
public class DisabledMfaProvider implements MfaProvider {

    @Override
    public String id() {
        return "none";
    }

    @Override
    public boolean supports(String username) {
        return false;
    }

    @Override
    public MfaChallenge begin(String username) {
        throw new UnsupportedOperationException("MFA challenge is not enabled");
    }

    @Override
    public boolean verify(String username, String code) {
        return false;
    }
}
