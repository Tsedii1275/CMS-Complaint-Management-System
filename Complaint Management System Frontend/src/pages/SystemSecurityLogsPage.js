import React, { useEffect, useState } from 'react';
import { Alert, Card, Input, Select, Space, Table, Typography } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';

const { Title, Text } = Typography;

const EVENT_OPTIONS = [
  'ALL',
  'SUCCESSFUL_LOGIN',
  'FAILED_LOGIN',
  'LOGOUT',
  'ACCOUNT_LOCKED',
  'ACCOUNT_UNLOCKED',
  'PASSWORD_CHANGED',
  'PASSWORD_RESET',
  'PASSWORD_EXPIRED',
  'SESSION_TIMEOUT',
  'ROLE_CHANGED',
  'LDAP_SYNC',
  'NOTIFICATION_SENT'
];

function SystemSecurityLogsPage() {
  const [rows, setRows] = useState([]);
  const [query, setQuery] = useState('');
  const [eventType, setEventType] = useState('ALL');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    ApiService.getSystemSecurityLogs(query, eventType)
      .then(data => {
        if (active) setRows(Array.isArray(data) ? data : []);
      })
      .catch(err => {
        if (active) setError(err.message || 'Failed to load security logs.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [query, eventType]);

  return (
    <DashboardLayout userRole="system-admin">
      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '16px' }}>
        <Title level={2} style={{ margin: 0, color: BRAND_COLORS.primary }}>Security & Audit Logs</Title>
        <Text type="secondary">Read-only authentication, role, access, and configuration events</Text>
        {error && <Alert style={{ marginTop: 16 }} type="error" showIcon message={error} />}
        <Card style={{ marginTop: 24 }}>
          <Space wrap style={{ marginBottom: 16 }}>
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="Search user, event, or IP"
              value={query}
              onChange={e => setQuery(e.target.value)}
              style={{ width: 280 }}
            />
            <Select value={eventType} onChange={setEventType} style={{ width: 220 }}>
              {EVENT_OPTIONS.map(option => (
                <Select.Option key={option} value={option}>{option === 'ALL' ? 'All events' : option}</Select.Option>
              ))}
            </Select>
          </Space>
          <Table
            rowKey="id"
            loading={loading}
            dataSource={rows}
            pagination={{ pageSize: 20 }}
            columns={[
              { title: 'When', dataIndex: 'createdAt', render: value => String(value || '').replace('T', ' ') },
              { title: 'Event', dataIndex: 'eventType' },
              { title: 'User', dataIndex: 'username' },
              { title: 'Source IP', dataIndex: 'ipAddress', render: value => value || '—' }
            ]}
          />
        </Card>
      </div>
    </DashboardLayout>
  );
}

export default SystemSecurityLogsPage;
