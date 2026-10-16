import React, { useEffect, useState } from 'react';
import { Alert, Input, Select, Table, Typography } from 'antd';
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

const EVENT_TONE = {
  SUCCESSFUL_LOGIN: { color: '#157347', background: '#e8f6ee' },
  FAILED_LOGIN: { color: '#b42318', background: '#fdeceb' },
  ACCOUNT_LOCKED: { color: '#b42318', background: '#fdeceb' },
  ACCOUNT_UNLOCKED: { color: '#157347', background: '#e8f6ee' },
  PASSWORD_EXPIRED: { color: '#b54708', background: '#fef4e6' },
  SESSION_TIMEOUT: { color: '#b54708', background: '#fef4e6' },
};

function formatWhen(value) {
  if (!value) return '—';
  return String(value).replace('T', ' ').replace(/\.\d+/, '').replace(/Z$/, '');
}

function eventLabel(value) {
  if (!value || value === 'ALL') return 'All events';
  return String(value)
    .toLowerCase()
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

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
      <div style={{ maxWidth: 1120, margin: '0 auto', padding: '8px 8px 24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', gap: 16, flexWrap: 'wrap' }}>
          <div>
            <Title level={3} style={{ margin: 0, color: BRAND_COLORS.primary, fontWeight: 650 }}>
              Security & Audit Logs
            </Title>
            <Text style={{ color: '#64748b' }}>Authentication, role, access, and configuration events</Text>
          </div>
          <Text style={{ color: '#64748b' }}>{loading ? 'Loading…' : `${rows.length} events`}</Text>
        </div>

        {error && <Alert style={{ marginTop: 16 }} type="error" showIcon message={error} />}

        <div
          style={{
            marginTop: 20,
            background: '#fff',
            border: '1px solid #e7ebf0',
            borderRadius: 12,
            boxShadow: '0 1px 2px rgba(15, 23, 42, 0.04)',
            overflow: 'hidden',
          }}
        >
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', padding: 16 }}>
            <Input
              allowClear
              prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
              placeholder="Search user, event, or IP"
              value={query}
              onChange={e => setQuery(e.target.value)}
              style={{ width: 280 }}
            />
            <Select
              value={eventType}
              onChange={setEventType}
              style={{ width: 220 }}
              options={EVENT_OPTIONS.map(option => ({ value: option, label: eventLabel(option) }))}
            />
          </div>
          <Table
            rowKey="id"
            loading={loading}
            dataSource={rows}
            pagination={{ pageSize: 20, showSizeChanger: false }}
            columns={[
              {
                title: 'When',
                dataIndex: 'createdAt',
                width: 190,
                render: value => <span style={{ color: '#475569' }}>{formatWhen(value)}</span>,
              },
              {
                title: 'Event',
                dataIndex: 'eventType',
                render: value => {
                  const tone = EVENT_TONE[value] || { color: '#334155', background: '#f1f5f9' };
                  return (
                    <span
                      style={{
                        display: 'inline-flex',
                        padding: '2px 8px',
                        borderRadius: 999,
                        background: tone.background,
                        color: tone.color,
                        fontSize: 12,
                        fontWeight: 600,
                        lineHeight: '20px',
                      }}
                    >
                      {eventLabel(value)}
                    </span>
                  );
                },
              },
              {
                title: 'User',
                dataIndex: 'username',
                render: value => value || '—',
              },
              {
                title: 'Source IP',
                dataIndex: 'ipAddress',
                render: value => value || '—',
              },
            ]}
          />
        </div>
      </div>
    </DashboardLayout>
  );
}

export default SystemSecurityLogsPage;
