package com.dashenbank.cms.integration.corebanking;

import com.dashenbank.cms.config.CbsDataSourceConfig;
import com.dashenbank.cms.config.CbsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Query layer for Oracle CBS. Isolated from the complaint domain.
 */
@Repository
public class CbsCustomerProfileRepository {

    private final NamedParameterJdbcTemplate cbsJdbc;
    private final CbsProfileRowMapper rowMapper;
    private final String profileSql;

    public CbsCustomerProfileRepository(
            @Autowired(required = false) @Qualifier(CbsDataSourceConfig.CBS_JDBC_TEMPLATE) NamedParameterJdbcTemplate cbsJdbc,
            CbsProperties properties) {
        this.cbsJdbc = cbsJdbc;
        this.rowMapper = new CbsProfileRowMapper(properties.getColumns());
        String tableName = properties.getTableName() != null && !properties.getTableName().isBlank()
                ? properties.getTableName().trim()
                : "FCUBSLIVE.RTVSCBS_CUST_PROFILE";
        String accountCol = properties.getColumns().getAccountNumber() != null
                && !properties.getColumns().getAccountNumber().isBlank()
                        ? properties.getColumns().getAccountNumber().trim()
                        : "ACCOUNT_NUMBER";
        this.profileSql = "SELECT * FROM " + tableName + " WHERE " + accountCol + " = :accountNumber";
    }

    public boolean isConfigured() {
        return cbsJdbc != null;
    }

    @SuppressWarnings("null")
    public List<CoreBankingProfile> findByAccountNumber(String accountNumber) {
        String cleanAccount = accountNumber == null ? "" : accountNumber.trim();
        return cbsJdbc.query(profileSql, Map.of("accountNumber", cleanAccount), rowMapper);
    }
}
