package com.dashenbank.cms.integration.corebanking;

import com.dashenbank.cms.exception.CbsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.SocketTimeoutException;
import java.sql.SQLTimeoutException;
import java.util.List;
import java.util.Optional;

/**
 * CBS customer profile lookup. Does not persist a CMS customer master and does
 * not fall back to any local customer table.
 */
@Service
public class CbsCustomerProfileService implements CoreBankingClient {

    private static final Logger log = LoggerFactory.getLogger(CbsCustomerProfileService.class);

    private final CbsCustomerProfileRepository repository;

    public CbsCustomerProfileService(CbsCustomerProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CoreBankingProfile> findByAccountNumber(String accountNumber) {
        if (!StringUtils.hasText(accountNumber)) {
            return Optional.empty();
        }
        if (!repository.isConfigured()) {
            log.warn("CBS customer profile lookup skipped: Oracle datasource is not configured.");
            throw CbsException.unavailable();
        }
        try {
            List<CoreBankingProfile> rows = repository.findByAccountNumber(accountNumber.trim());
            if (rows.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(rows.get(0));
        } catch (DataAccessException e) {
            if (isTimeout(e)) {
                log.warn("CBS customer profile lookup timed out.");
                throw CbsException.timeout();
            }
            log.warn("CBS customer profile lookup unavailable: {}", rootMessage(e));
            throw CbsException.unavailable();
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        String message = error.getMessage();
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        if (message == null) {
            return "oracle-error";
        }
        String lower = message.toLowerCase();
        if (lower.contains("password") || lower.contains("jdbc:oracle")) {
            return "oracle-error";
        }
        return message;
    }

    private static boolean isTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof QueryTimeoutException
                    || current instanceof SQLTimeoutException
                    || current instanceof SocketTimeoutException) {
                return true;
            }
            if (!(current instanceof CannotGetJdbcConnectionException)) {
                String message = current.getMessage();
                if (message != null && (message.contains("ORA-01013") || message.contains("ORA-12170"))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
