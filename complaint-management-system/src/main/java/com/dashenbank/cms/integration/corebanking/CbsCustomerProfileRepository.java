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

    static final String PROFILE_SQL = "SELECT * FROM RTVSCBS_CUST_PROFILE WHERE ACCOUNT_NO = :accountNumber";

    private final NamedParameterJdbcTemplate cbsJdbc;
    private final CbsProfileRowMapper rowMapper;

    public CbsCustomerProfileRepository(
            @Autowired(required = false) @Qualifier(CbsDataSourceConfig.CBS_JDBC_TEMPLATE) NamedParameterJdbcTemplate cbsJdbc,
            CbsProperties properties) {
        this.cbsJdbc = cbsJdbc;
        this.rowMapper = new CbsProfileRowMapper(properties.getColumns());
    }

    public boolean isConfigured() {
        return cbsJdbc != null;
    }

    public List<CoreBankingProfile> findByAccountNumber(String accountNumber) {
        return cbsJdbc.query(PROFILE_SQL, Map.of("accountNumber", accountNumber), rowMapper);
    }
}
