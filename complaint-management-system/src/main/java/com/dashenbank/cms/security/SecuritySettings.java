package com.dashenbank.cms.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security")
public class SecuritySettings {

    private int lockoutThreshold = SecurityPolicy.LOCKOUT_THRESHOLD;
    private int lockoutDurationMinutes = SecurityPolicy.LOCKOUT_DURATION_MINUTES;
    private boolean mfaReady = true;
    private String jwtIssuer = "dashenbank-cms";

    public int getLockoutThreshold() {
        return lockoutThreshold;
    }

    public void setLockoutThreshold(int lockoutThreshold) {
        this.lockoutThreshold = lockoutThreshold;
    }

    public int getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }

    public void setLockoutDurationMinutes(int lockoutDurationMinutes) {
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    public boolean isMfaReady() {
        return mfaReady;
    }

    public void setMfaReady(boolean mfaReady) {
        this.mfaReady = mfaReady;
    }

    public String getJwtIssuer() {
        return jwtIssuer;
    }

    public void setJwtIssuer(String jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }
}
