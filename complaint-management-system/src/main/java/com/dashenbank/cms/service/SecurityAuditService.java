package com.dashenbank.cms.service;

import com.dashenbank.cms.model.SecurityAuditEvent;
import com.dashenbank.cms.model.SecurityAuditLog;
import com.dashenbank.cms.repository.SecurityAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityAuditService {

    private final SecurityAuditLogRepository securityAuditLogRepository;

    public SecurityAuditService(SecurityAuditLogRepository securityAuditLogRepository) {
        this.securityAuditLogRepository = securityAuditLogRepository;
    }

    @Transactional
    public void log(String username, SecurityAuditEvent eventType, String ipAddress) {
        String actor = username == null || username.isBlank() ? "unknown" : username.trim();
        securityAuditLogRepository.save(SecurityAuditLog.builder()
                .username(actor)
                .eventType(eventType)
                .ipAddress(ipAddress)
                .build());
    }
}
