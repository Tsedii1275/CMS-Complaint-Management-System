import React, { useEffect, useState } from 'react';
import { Alert, Card, Col, Row, Tag, Typography } from 'antd';
import DashboardLayout from '../components/DashboardLayout';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';

const { Title, Text } = Typography;

function statusTag(status) {
  if (status === 'UP') return <Tag color="green">Up</Tag>;
  if (status === 'DISABLED') return <Tag>Disabled</Tag>;
  return <Tag color="red">Down</Tag>;
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

  return (
    <DashboardLayout userRole="system-admin">
      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '16px' }}>
        <Title level={2} style={{ margin: 0, color: BRAND_COLORS.primary }}>System Overview</Title>
        <Text type="secondary">Live application, database, and integration health</Text>
        {error && <Alert style={{ marginTop: 16 }} type="error" showIcon message={error} />}
        <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
          {cards.map(item => (
            <Col xs={24} md={12} lg={8} key={item.id || item.name}>
              <Card>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                  <Text strong>{item.name}</Text>
                  {statusTag(item.status)}
                </div>
                <div style={{ marginTop: 12, color: '#475569' }}>{item.detail || '—'}</div>
                {item.lastSuccessAt && (
                  <div style={{ marginTop: 8 }}>
                    <Text type="secondary">Last success: {String(item.lastSuccessAt).replace('T', ' ')}</Text>
                  </div>
                )}
              </Card>
            </Col>
          ))}
        </Row>
        {overview?.checkedAt && (
          <Text type="secondary" style={{ display: 'block', marginTop: 16 }}>
            Checked {String(overview.checkedAt).replace('T', ' ')}
          </Text>
        )}
      </div>
    </DashboardLayout>
  );
}

export default SystemAdminDashboard;
