package com.dashenbank.cms.security;

import com.dashenbank.cms.security.mfa.MfaProvider;
import org.springframework.stereotype.Component;

@Component
public class MfaCatalog {

    private final MfaProvider mfaProvider;

    public MfaCatalog(MfaProvider mfaProvider) {
        this.mfaProvider = mfaProvider;
    }

    public String providerId() {
        return mfaProvider.id();
    }
}
