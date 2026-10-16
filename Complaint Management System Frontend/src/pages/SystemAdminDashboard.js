import React, { useEffect, useState } from 'react';
import { Alert, Col, Row, Typography } from 'antd';
import DashboardLayout from '../components/DashboardLayout';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';

const { Title, Text } = Typography;

const STATUS = {
  UP: { label: 'Up', color: '#157347', background: '#e8f6ee' },
  DISABLED: { label: 'Disabled', color: '#64748b', background: '#f1f5f9' },
  DOWN: { label: 'Down', color: '#b42318', background: '#fdeceb' },
};

function formatWhen(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').replace(/\.\d+/, '').replace(/Z$/, '');
}

function statusStyle(status) {
  return STATUS[status] || STATUS.DOWN;
}

function SystemAdminDashboard() {
  const [overview, setOverview] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    ApiService.getSystemOverview()
      .then(data => {
        if (active) setOverview(data);
      })
      .catch(err => {
        if (active) setError(err.message || 'Failed to load system overview.');
      });
    return () => {
      active = false;
    };
  }, []);

  const cards = [
    overview?.application,
    overview?.database,
    ...(overview?.integrations || [])
  ].filter(Boolean);
  const upCount = cards.filter(item => item.status === 'UP').length;
  const checkedAt = formatWhen(overview?.checkedAt);

  return (
    <DashboardLayout userRole="system-admin">
      <div style={{ maxWidth: 1120, margin: '0 auto', padding: '8px 8px 24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', gap: 16, flexWrap: 'wrap' }}>
          <div>
            <Title level={3} style={{ margin: 0, color: BRAND_COLORS.primary, fontWeight: 650 }}>
              System Overview
            </Title>
            <Text style={{ color: '#64748b' }}>Application, database, and integration health</Text>
          </div>
          {cards.length > 0 && (
            <Text style={{ color: '#64748b' }}>
              {upCount} of {cards.length} up{checkedAt ? ` · Checked ${checkedAt}` : ''}
            </Text>
          )}
        </div>

        {error && <Alert style={{ marginTop: 16 }} type="error" showIcon message={error} />}

        <Row gutter={[16, 16]} style={{ marginTop: 20 }}>
          {cards.map(item => {
            const tone = statusStyle(item.status);
            return (
              <Col xs={24} md={12} lg={8} key={item.id || item.name}>
                <div
                  style={{
                    height: '100%',
                    minHeight: 148,
                    padding: '18px 18px 16px',
                    background: '#fff',
                    border: '1px solid #e7ebf0',
                    borderRadius: 12,
                    boxShadow: '0 1px 2px rgba(15, 23, 42, 0.04)',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
                    <Text strong style={{ color: '#0f172a', fontSize: 15 }}>{item.name}</Text>
                    <span
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        padding: '2px 8px',
                        borderRadius: 999,
                        background: tone.background,
                        color: tone.color,
                        fontSize: 12,
                        fontWeight: 600,
                        lineHeight: '20px',
                      }}
                    >
                      <span style={{ width: 6, height: 6, borderRadius: '50%', background: tone.color }} />
                      {tone.label}
                    </span>
                  </div>
                  <div style={{ marginTop: 14, color: '#475569', fontSize: 14, lineHeight: 1.5 }}>
                    {item.detail || '—'}
                  </div>
                  <div style={{ marginTop: 16, color: '#94a3b8', fontSize: 12 }}>
                    {item.lastSuccessAt ? `Last success ${formatWhen(item.lastSuccessAt)}` : 'No successful check yet'}
                  </div>
                </div>
              </Col>
            );
          })}
        </Row>
      </div>
    </DashboardLayout>
  );
}

export default SystemAdminDashboard;
