package com.dashenbank.cms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Environment-specific CBS settings. Host, service, and credentials come from
 * {@code CBS_DATASOURCE_*} — never from Java source.
 */
@ConfigurationProperties(prefix = "cbs")
public class CbsProperties {

    private final Datasource datasource = new Datasource();
    private final Columns columns = new Columns();
    private int queryTimeoutSeconds = 8;
    private int connectTimeoutMs = 5000;

    public Datasource getDatasource() {
        return datasource;
    }

    public Columns getColumns() {
        return columns;
    }

    public int getQueryTimeoutSeconds() {
        return queryTimeoutSeconds;
    }

    public void setQueryTimeoutSeconds(int queryTimeoutSeconds) {
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public static class Datasource {
        private String url = "";
        private String username = "";
        private String password = "";
        private String driverClassName = "oracle.jdbc.OracleDriver";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }

    /**
     * Exact Oracle column names on {@code RTVSCBS_CUST_PROFILE}. Override with
     * {@code CBS_COL_*} after the CBS/DBA team confirms the live view.
     */
    public static class Columns {
        private String customerName = "CUSTOMER_NAME";
        private String phoneNumber = "PHONE_NUMBER";
        private String homeBranch = "HOME_BRANCH";
        private String homeDistrict = "HOME_DISTRICT";
        private String customerSegment = "CUSTOMER_SEGMENT";

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getHomeBranch() {
            return homeBranch;
        }

        public void setHomeBranch(String homeBranch) {
            this.homeBranch = homeBranch;
        }

        public String getHomeDistrict() {
            return homeDistrict;
        }

        public void setHomeDistrict(String homeDistrict) {
            this.homeDistrict = homeDistrict;
        }

        public String getCustomerSegment() {
            return customerSegment;
        }

        public void setCustomerSegment(String customerSegment) {
            this.customerSegment = customerSegment;
        }
    }
}
