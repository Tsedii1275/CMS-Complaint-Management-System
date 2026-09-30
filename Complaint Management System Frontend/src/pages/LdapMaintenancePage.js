import React, { useEffect, useState } from 'react';
import { Alert, Button, Card, Col, Descriptions, Row, Space, Table, Tag, Typography, message } from 'antd';
import { CloudServerOutlined, ReloadOutlined, SyncOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';

const { Title, Text } = Typography;

function boolTag(value) {
  return value ? <Tag color="green">Yes</Tag> : <Tag>No</Tag>;
}

function mappingRows(map) {
  return Object.entries(map || {}).map(([key, role]) => ({ key, role }));
}

function LdapMaintenancePage() {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const data = await ApiService.getLdapStatus();
      setStatus(data);
    } catch (err) {
      message.error(err.message || 'Failed to load LDAP status.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const healthCheck = async () => {
    try {
      const result = await ApiService.checkLdapHealth();
      message.info(result.reachable ? 'Directory bind succeeded.' : (result.detail || 'Directory unreachable.'));
      load();
    } catch (err) {
      message.error(err.message || 'Health check failed.');
    }
  };

  const runSync = async () => {
    try {
      const result = await ApiService.runLdapSync();
      message.success(`Sync finished. Users: ${result.usersSynced}, failed: ${result.failedSynchronizations}`);
      load();
    } catch (err) {
      message.error(err.message || 'Sync failed.');
    }
  };

  return (
    <DashboardLayout userRole="admin">
      <div style={{ padding: 24 }}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <div>
            <Title level={3} style={{ color: BRAND_COLORS.navy, marginBottom: 4 }}>
              <CloudServerOutlined /> LDAP / Active Directory
            </Title>
            <Text type="secondary">
              Maintenance view for hybrid AD login. User &amp; Role Management is unchanged.
            </Text>
          </div>
          <Alert
            type="info"
            showIcon
            message="Active Directory Authentication & Approval Workflow"
            description="Active Directory is strictly used for identity verification. Automatic role assignment based on Job Title, Group, or OU is disabled. All new AD users are initialized with PENDING_APPROVAL status until an administrator assigns a role in User Management."
          />
          <Space>
            <Button icon={<ReloadOutlined />} onClick={load} loading={loading}>Refresh</Button>
            <Button onClick={healthCheck}>Health check</Button>
            <Button type="primary" icon={<SyncOutlined />} onClick={runSync}>Run sync now</Button>
          </Space>
          <Row gutter={16}>
            <Col xs={24} lg={12}>
              <Card title="Connection" loading={loading}>
                <Descriptions column={1} size="small">
                  <Descriptions.Item label="LDAP enabled">{boolTag(status?.ldapEnabled)}</Descriptions.Item>
                  <Descriptions.Item label="AD authentication">{boolTag(status?.adAuthenticationEnabled)}</Descriptions.Item>
                  <Descriptions.Item label="Directory reachable">{boolTag(status?.directoryReachable)}</Descriptions.Item>
                  <Descriptions.Item label="URL">{status?.url || '—'}</Descriptions.Item>
                  <Descriptions.Item label="Fallback URL">{status?.fallbackUrl || '—'}</Descriptions.Item>
                  <Descriptions.Item label="Base DN">{status?.baseDn || '—'}</Descriptions.Item>
                  <Descriptions.Item label="TLS peer name">{status?.sslPeerName || '—'}</Descriptions.Item>
                  <Descriptions.Item label="Last health check">{status?.lastHealthCheckAt || '—'}</Descriptions.Item>
                  <Descriptions.Item label="Last LDAP error">{status?.lastLdapError || '—'}</Descriptions.Item>
                </Descriptions>
              </Card>
            </Col>
            <Col xs={24} lg={12}>
              <Card title="Synchronization" loading={loading}>
                <Descriptions column={1} size="small">
                  <Descriptions.Item label="Scheduled sync">{boolTag(status?.scheduledSyncEnabled)}</Descriptions.Item>
                  <Descriptions.Item label="Last result">{status?.lastSyncResult || '—'}</Descriptions.Item>
                  <Descriptions.Item label="Last successful sync">{status?.lastSuccessfulSyncAt || '—'}</Descriptions.Item>
                  <Descriptions.Item label="Users synchronized">{status?.usersSynced ?? 0}</Descriptions.Item>
                  <Descriptions.Item label="Failed synchronizations">{status?.failedSynchronizations ?? 0}</Descriptions.Item>
                  <Descriptions.Item label="AD users in CMS">{status?.adUsersInCms ?? 0}</Descriptions.Item>
                  <Descriptions.Item label="Role assignment model"><Tag color="gold">Admin Approval Required</Tag></Descriptions.Item>
                </Descriptions>
              </Card>
            </Col>
          </Row>
        </Space>
      </div>
    </DashboardLayout>
  );
}

export default LdapMaintenancePage;
