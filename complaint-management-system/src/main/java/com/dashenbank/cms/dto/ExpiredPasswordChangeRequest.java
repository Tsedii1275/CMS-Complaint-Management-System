package com.dashenbank.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ExpiredPasswordChangeRequest {

    @NotBlank
    private String passwordChangeToken;

    @NotBlank
    @Size(max = 128)
    private String currentPassword;

    @NotBlank
    @Size(min = 12, max = 128)
    private String newPassword;

    @NotBlank
    @Size(min = 12, max = 128)
    private String confirmPassword;

    public String getPasswordChangeToken() {
        return passwordChangeToken;
    }

    public void setPasswordChangeToken(String passwordChangeToken) {
        this.passwordChangeToken = passwordChangeToken;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
