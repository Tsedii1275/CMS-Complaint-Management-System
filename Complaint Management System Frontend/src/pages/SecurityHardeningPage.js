import React, { useEffect, useState } from 'react';
import { Alert, Button, Card, Col, Descriptions, Row, Space, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';

const { Title, Text, Paragraph } = Typography;

const SECTIONS = [
  { id: 'system', title: '1. System Information' },
  { id: 'authentication', title: '2. Authentication' },
  { id: 'authorization', title: '3. Authorization' },
  { id: 'encryption', title: '4. Encryption' },
  { id: 'sessions', title: '5. Active Sessions' },
  { id: 'audit', title: '6. Audit Logs' },
  { id: 'ratelimits', title: '7. Rate Limits' },
  { id: 'headers', title: '8. HTTP Security Headers' },
  { id: 'tokens', title: '9. Token Security' },
  { id: 'keys', title: '10. Key Management' },
];

function boolTag(value, yes = 'Yes', no = 'No') {
  return value ? <Tag color="green">{yes}</Tag> : <Tag>{no}</Tag>;
}

function kvRows(obj) {
  return Object.entries(obj || {}).map(([key, value]) => ({
    key,
    value: value === null || value === undefined || value === '' ? '—' : String(value),
  }));
}

function SecurityHardeningPage() {
  const [loading, setLoading] = useState(true);
  const [dashboard, setDashboard] = useState(null);
  const [observedHeaders, setObservedHeaders] = useState({});
  const [authEvents, setAuthEvents] = useState([]);
  const [auditSummary, setAuditSummary] = useState([]);

  const load = async () => {
    setLoading(true);
    try {
      const [{ data, observedHeaders: headers }, events, summary] = await Promise.all([
        ApiService.getSecurityDashboard(),
        ApiService.getSecurityAuthEvents(),
        ApiService.getSecurityAuditSummary(),
      ]);
      setDashboard(data);
      setObservedHeaders(headers || {});
      setAuthEvents(events || []);
      setAuditSummary(summary || []);
    } catch (err) {
      message.error(err.message || 'Failed to load security evidence.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const info = dashboard?.systemInfo || {};
  const auth = dashboard?.authentication || {};
  const encryption = dashboard?.encryption || {};
  const token = dashboard?.tokenConfig || {};
  const keys = dashboard?.keyManagement || {};
  const httpSecurity = dashboard?.httpSecurity?.configuredValues || {};

  return (
    <DashboardLayout userRole="admin">
      <div style={{ padding: 24 }}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <div>
            <Title level={3} style={{ color: BRAND_COLORS.primary, marginBottom: 4 }}>
              <SafetyCertificateOutlined /> Security Hardening
            </Title>
            <Text type="secondary">
              UAT evidence dashboard. Each numbered section maps to the bank API Security Hardening Checklist.
              JWT values and secret material are never shown.
            </Text>
          </div>
          <Alert
            type="info"
            showIcon
            message="Screenshot guide"
            description="Sign in as ROLE_ADMIN, open this page, click Refresh, then capture each numbered card. For HTTP headers also open DevTools → Network → this API call and confirm Strict-Transport-Security, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Content-Security-Policy, and Permissions-Policy. For RBAC, open an operational account and confirm /admin/security returns 403."
          />
          <Space wrap>
            <Button icon={<ReloadOutlined />} onClick={load} loading={loading}>Refresh</Button>
            {SECTIONS.map((section) => (
              <Button key={section.id} size="small" href={`#${section.id}`}>{section.title}</Button>
            ))}
          </Space>

          <Card id="system" title="1. System Information" loading={loading}>
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="Application Version">{info.applicationVersion}</Descriptions.Item>
              <Descriptions.Item label="Build Version">{info.buildVersion}</Descriptions.Item>
              <Descriptions.Item label="Build Timestamp">{info.buildTimestamp}</Descriptions.Item>
              <Descriptions.Item label="Spring Boot Version">{info.springBootVersion}</Descriptions.Item>
              <Descriptions.Item label="Java Runtime">{info.javaRuntime}</Descriptions.Item>
              <Descriptions.Item label="Environment">{info.environment}</Descriptions.Item>
              <Descriptions.Item label="Database Version" span={2}>{info.databaseVersion}</Descriptions.Item>
            </Descriptions>
          </Card>

          <Card id="authentication" title="2. Authentication" loading={loading}>
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="LDAP authentication">{boolTag(auth.ldapAuthentication)}</Descriptions.Item>
              <Descriptions.Item label="Local fallback">{boolTag(auth.localFallbackAuthentication)}</Descriptions.Item>
              <Descriptions.Item label="JWT authentication">{boolTag(auth.jwtAuthentication)}</Descriptions.Item>
              <Descriptions.Item label="Account lockout">{boolTag(auth.accountLockoutEnabled)}</Descriptions.Item>
              <Descriptions.Item label="Lockout threshold">{auth.lockoutThreshold}</Descriptions.Item>
              <Descriptions.Item label="Lockout duration (min)">{auth.lockoutDurationMinutes}</Descriptions.Item>
              <Descriptions.Item label="Failed login counter">{boolTag(auth.failedLoginCounter)}</Descriptions.Item>
              <Descriptions.Item label="Login audit logging">{boolTag(auth.loginAuditLogging)}</Descriptions.Item>
              <Descriptions.Item label="MFA_READY">{boolTag(auth.mfaReady, 'true', 'false')}</Descriptions.Item>
              <Descriptions.Item label="MFA in login path">{boolTag(auth.mfaEnabledInLoginPath, 'enabled', 'extension only')}</Descriptions.Item>
              <Descriptions.Item label="MfaProvider">{auth.mfaProvider}</Descriptions.Item>
              <Descriptions.Item label="Extension point">{auth.mfaExtensionPoint}</Descriptions.Item>
            </Descriptions>
            <Title level={5} style={{ marginTop: 16 }}>Login events</Title>
            <Table
              size="small"
              rowKey={(row, index) => `${row.username}-${row.loginTime}-${index}`}
              pagination={{ pageSize: 8 }}
              dataSource={authEvents}
              columns={[
                { title: 'Username', dataIndex: 'username' },
                { title: 'Login time', dataIndex: 'loginTime' },
                {
                  title: 'Success / Failure',
                  dataIndex: 'outcome',
                  render: (value, row) => (
                    <Tag color={row.success ? 'green' : 'red'}>{value}</Tag>
                  ),
                },
                { title: 'Source IP', dataIndex: 'sourceIp' },
              ]}
            />
          </Card>

          <Card id="authorization" title="3. Authorization" loading={loading}>
            <Paragraph>
              Admin-only endpoints require ROLE_ADMIN. Operational endpoints require an authenticated staff role.
              This table is the live RBAC map used by Spring Security.
            </Paragraph>
            <Table
              size="small"
              rowKey="role"
              pagination={false}
              dataSource={dashboard?.rbac || []}
              columns={[
                { title: 'Role', dataIndex: 'role', width: 280 },
                { title: 'Assigned APIs', dataIndex: 'assignedApis' },
                { title: 'Permissions', dataIndex: 'permissions', width: 220 },
              ]}
            />
          </Card>

          <Card id="encryption" title="4. Encryption" loading={loading}>
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="Password Algorithm">{encryption.passwordAlgorithm}</Descriptions.Item>
              <Descriptions.Item label="Plaintext password storage">{boolTag(encryption.plaintextPasswordStorage, 'Yes', 'No')}</Descriptions.Item>
              <Descriptions.Item label="Encryption Algorithm">{encryption.encryptionAlgorithm}</Descriptions.Item>
              <Descriptions.Item label="Key Management Status">{encryption.keyManagementStatus}</Descriptions.Item>
              <Descriptions.Item label="AES-256 ready">{boolTag(encryption.aes256Ready)}</Descriptions.Item>
              <Descriptions.Item label="Field encryption enabled">{boolTag(encryption.fieldEncryptionEnabled)}</Descriptions.Item>
            </Descriptions>
            <Title level={5} style={{ marginTop: 16 }}>Sensitive field masking</Title>
            <Descriptions bordered size="small" column={1}>
              <Descriptions.Item label="0911223344">{encryption.maskingExamples?.phone}</Descriptions.Item>
              <Descriptions.Item label="customer@email.com">{encryption.maskingExamples?.email}</Descriptions.Item>
              <Descriptions.Item label="Account number">{encryption.maskingExamples?.accountNumber}</Descriptions.Item>
            </Descriptions>
          </Card>

          <Card id="sessions" title="5. Active Sessions" loading={loading}>
            <Paragraph>JWT values are not stored or displayed.</Paragraph>
            <Table
              size="small"
              rowKey={(row, index) => `${row.username}-${row.loginTime}-${index}`}
              pagination={false}
              dataSource={dashboard?.activeSessions || []}
              columns={[
                { title: 'Username', dataIndex: 'username' },
                { title: 'Role', dataIndex: 'role' },
                { title: 'Login Time', dataIndex: 'loginTime' },
                { title: 'Source IP', dataIndex: 'sourceIp' },
                { title: 'User Agent', dataIndex: 'userAgent' },
              ]}
            />
          </Card>

          <Card id="audit" title="6. Audit Logs" loading={loading}>
            <Table
              size="small"
              rowKey="eventType"
              pagination={false}
              dataSource={auditSummary}
              columns={[
                { title: 'Event Type', dataIndex: 'eventType' },
                { title: 'Count', dataIndex: 'count' },
                { title: 'Last Occurrence', dataIndex: 'lastOccurrence' },
              ]}
            />
          </Card>

          <Card id="ratelimits" title="7. Rate Limits" loading={loading}>
            <Table
              size="small"
              rowKey="protectedEndpoint"
              pagination={false}
              dataSource={dashboard?.rateLimits || []}
              columns={[
                { title: 'Protected Endpoint', dataIndex: 'protectedEndpoint' },
                { title: 'Limit', dataIndex: 'limit' },
                { title: 'Window (seconds)', dataIndex: 'windowSeconds' },
                { title: 'Exceeded status', dataIndex: 'exceededStatus' },
              ]}
            />
          </Card>

          <Card id="headers" title="8. HTTP Security Headers" loading={loading}>
            <Row gutter={16}>
              <Col xs={24} lg={12}>
                <Title level={5}>Configured values</Title>
                <Table
                  size="small"
                  pagination={false}
                  rowKey="key"
                  dataSource={kvRows(httpSecurity)}
                  columns={[
                    { title: 'Header', dataIndex: 'key' },
                    { title: 'Value', dataIndex: 'value' },
                  ]}
                />
              </Col>
              <Col xs={24} lg={12}>
                <Title level={5}>Observed on this API response</Title>
                <Table
                  size="small"
                  pagination={false}
                  rowKey="key"
                  dataSource={kvRows(observedHeaders)}
                  columns={[
                    { title: 'Header', dataIndex: 'key' },
                    { title: 'Value', dataIndex: 'value' },
                  ]}
                />
              </Col>
            </Row>
          </Card>

          <Card id="tokens" title="9. Token Security" loading={loading}>
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="Access Token TTL">{token.accessTokenTtlMinutes} minutes ({token.accessTokenTtlMs} ms)</Descriptions.Item>
              <Descriptions.Item label="Refresh Token TTL">{token.refreshTokenTtl}</Descriptions.Item>
              <Descriptions.Item label="Signing Algorithm">{token.signingAlgorithm}</Descriptions.Item>
              <Descriptions.Item label="Token Issuer">{token.tokenIssuer}</Descriptions.Item>
              <Descriptions.Item label="Expiration enabled">{boolTag(token.expirationEnabled)}</Descriptions.Item>
              <Descriptions.Item label="Expired tokens rejected">{boolTag(token.expiredTokensRejected)}</Descriptions.Item>
            </Descriptions>
          </Card>

          <Card id="keys" title="10. Key Management" loading={loading}>
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 12 }}
              message="Secret values are never returned. Sources are ENVIRONMENT, VAULT, CONFIG_SERVER, SECRET_STORE, NOT_REQUIRED, or NOT_CONFIGURED."
            />
            <Descriptions bordered size="small" column={1}>
              <Descriptions.Item label="JWT Secret Source">{keys.jwtSecretSource}</Descriptions.Item>
              <Descriptions.Item label="LDAP Credential Source">{keys.ldapCredentialSource}</Descriptions.Item>
              <Descriptions.Item label="SMTP Credential Source">{keys.smtpCredentialSource}</Descriptions.Item>
              <Descriptions.Item label="AES data key source">{keys.aesDataKeySource}</Descriptions.Item>
              <Descriptions.Item label="Hardcoded JWT secret">{boolTag(keys.hardcodedJwtSecret)}</Descriptions.Item>
              <Descriptions.Item label="Rotation ready">{boolTag(keys.rotationReady)}</Descriptions.Item>
              <Descriptions.Item label="Rotation procedure">{keys.rotationProcedure}</Descriptions.Item>
            </Descriptions>
          </Card>

          <Card title="Validation configuration summary" loading={loading}>
            <Table
              size="small"
              rowKey="surface"
              pagination={false}
              dataSource={dashboard?.validation || []}
              columns={[
                { title: 'Surface', dataIndex: 'surface' },
                { title: 'Fields', dataIndex: 'fields' },
                { title: 'Constraints', dataIndex: 'constraints' },
                { title: 'Failure status', dataIndex: 'failureStatus' },
              ]}
            />
          </Card>
        </Space>
      </div>
    </DashboardLayout>
  );
}

export default SecurityHardeningPage;
