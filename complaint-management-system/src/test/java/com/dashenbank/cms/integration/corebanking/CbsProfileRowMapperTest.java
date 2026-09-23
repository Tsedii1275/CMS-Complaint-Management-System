package com.dashenbank.cms.integration.corebanking;

import com.dashenbank.cms.config.CbsProperties;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CbsProfileRowMapperTest {

    @Test
    void mapsConfiguredViewColumnsOnly() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(6);
        stubColumn(meta, rs, 1, "ACCOUNT_NO", "1001234567890");
        stubColumn(meta, rs, 2, "CUSTOMER_NAME", "Abebe Kebede");
        stubColumn(meta, rs, 3, "PHONE_NUMBER", "0912345678");
        stubColumn(meta, rs, 4, "HOME_BRANCH", "Bole Branch");
        stubColumn(meta, rs, 5, "HOME_DISTRICT", "Addis District");
        stubColumn(meta, rs, 6, "CUSTOMER_SEGMENT", "Retail");

        CoreBankingProfile profile = new CbsProfileRowMapper(new CbsProperties.Columns()).mapRow(rs, 0);

        assertEquals("Abebe Kebede", profile.customerName());
        assertEquals("0912345678", profile.phoneNumber());
        assertEquals("Bole Branch", profile.homeBranch());
        assertEquals("Addis District", profile.homeDistrict());
        assertEquals("Retail", profile.customerSegment());
    }

    @Test
    void ignoresUnconfiguredCbsColumns() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(3);
        stubColumn(meta, rs, 1, "CIF_NUMBER", "CIF1");
        stubColumn(meta, rs, 2, "EMAIL", "hidden@bank.test");
        stubColumn(meta, rs, 3, "CUSTOMER_NAME", "Abebe Kebede");

        CoreBankingProfile profile = new CbsProfileRowMapper(new CbsProperties.Columns()).mapRow(rs, 0);

        assertEquals("Abebe Kebede", profile.customerName());
        assertNull(profile.phoneNumber());
        assertNull(profile.homeBranch());
        assertNull(profile.homeDistrict());
        assertNull(profile.customerSegment());
    }

    private static void stubColumn(ResultSetMetaData meta, ResultSet rs, int index, String label, String value)
            throws SQLException {
        when(meta.getColumnLabel(index)).thenReturn(label);
        when(rs.getObject(index)).thenReturn(value);
    }
}
