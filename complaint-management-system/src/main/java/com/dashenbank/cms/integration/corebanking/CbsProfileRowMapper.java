package com.dashenbank.cms.integration.corebanking;

import com.dashenbank.cms.config.CbsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Maps {@code SELECT *} from {@code RTVSCBS_CUST_PROFILE} using the exact
 * column
 * names configured for this environment. Extra CBS columns are ignored.
 */
public final class CbsProfileRowMapper implements RowMapper<CoreBankingProfile> {

    private static final Logger log = LoggerFactory.getLogger(CbsProfileRowMapper.class);
    private static final AtomicBoolean COLUMNS_LOGGED = new AtomicBoolean(false);

    private final String customerNameColumn;
    private final String phoneNumberColumn;
    private final String homeBranchColumn;
    private final String branchCodeColumn;
    private final String homeDistrictColumn;
    private final String customerSegmentColumn;

    public CbsProfileRowMapper(CbsProperties.Columns columns) {
        this.customerNameColumn = columns.getCustomerName();
        this.phoneNumberColumn = columns.getPhoneNumber();
        this.homeBranchColumn = columns.getHomeBranch();
        this.branchCodeColumn = columns.getBranchCode();
        this.homeDistrictColumn = columns.getHomeDistrict();
        this.customerSegmentColumn = columns.getCustomerSegment();
    }

    @Override
    public CoreBankingProfile mapRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, String> columns = readColumns(rs);
        logViewColumnsOnce(columns);
        String branchName = value(columns, homeBranchColumn, "BRANCH_NAME", "HOME_BRANCH");
        String code = value(columns, branchCodeColumn, "BRANCH_CODE");
        return new CoreBankingProfile(
                value(columns, customerNameColumn, "CUSTOMER_NAME", "FULL_NAME"),
                value(columns, phoneNumberColumn, "REGISTERED_PHONE", "PHONE_NUMBER", "TELEPHONE", "MOBILE"),
                branchName,
                value(columns, homeDistrictColumn, "DISTRICT_NAME", "HOME_DISTRICT", "DISTRICT"),
                value(columns, customerSegmentColumn, "CUST_SEGMENT", "CUSTOMER_SEGMENT", "SEGMENT"),
                code);
    }

    static Map<String, String> readColumns(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, String> columns = new LinkedHashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String label = meta.getColumnLabel(i);
            if (label == null || label.isBlank()) {
                label = meta.getColumnName(i);
            }
            Object raw = rs.getObject(i);
            String text = raw == null ? null : raw.toString().trim();
            if (text != null && text.isEmpty()) {
                text = null;
            }
            columns.put(normalize(label), text);
        }
        return columns;
    }

    private void logViewColumnsOnce(Map<String, String> columns) {
        if (COLUMNS_LOGGED.compareAndSet(false, true)) {
            log.info("CBS RTVSCBS_CUST_PROFILE columns detected: {}", columns.keySet());
        }
    }

    private static String value(Map<String, String> columns, String primaryName, String... fallbacks) {
        String val = columns.get(normalize(primaryName));
        if (val != null && !val.isBlank()) {
            return val;
        }
        for (String fallback : fallbacks) {
            val = columns.get(normalize(fallback));
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return null;
    }

    static String normalize(String name) {
        return name == null ? "" : name.trim().toUpperCase(Locale.ROOT);
    }
}
