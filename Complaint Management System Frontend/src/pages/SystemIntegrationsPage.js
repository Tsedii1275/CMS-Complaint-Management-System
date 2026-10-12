import React, { useEffect, useState } from 'react';
import { Alert, Card, Col, Row, Statistic, Table, Tag, Typography } from 'antd';
import DashboardLayout from '../components/DashboardLayout';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';

const { Title, Text } = Typography;

function statusTag(status) {
  if (status === 'UP') return <Tag color="green">Up</Tag>;
  if (status === 'DISABLED') return <Tag>Disabled</Tag>;
  return <Tag color="red">Down</Tag>;
}

function SystemIntegrationsPage() {
  const [report, setReport] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    ApiService.getSystemIntegrations()
      .then(data => {
        if (active) setReport(data);
      })
      .catch(err => {
        if (active) setError(err.message || 'Failed to load integration diagnostics.');
      });
    return () => {
      active = false;
    };
  }, []);

  const queues = report?.queues || {};

  return (
    <DashboardLayout userRole="system-admin">
      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '16px' }}>
        <Title level={2} style={{ margin: 0, color: BRAND_COLORS.primary }}>Integration & Notification Diagnostics</Title>
        <Text type="secondary">AD, CBS, email, SMS, workflow, and notification queues</Text>
        {error && <Alert style={{ marginTop: 16 }} type="error" showIcon message={error} />}
        <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
          <Col xs={12} md={6}><Card><Statistic title="Email waiting" value={queues.emailPending ?? '—'} /></Card></Col>
          <Col xs={12} md={6}><Card><Statistic title="Email failed" value={queues.emailFailed ?? '—'} /></Card></Col>
          <Col xs={12} md={6}><Card><Statistic title="SMS waiting" value={queues.smsPending ?? '—'} /></Card></Col>
          <Col xs={12} md={6}><Card><Statistic title="SMS failed" value={queues.smsFailed ?? '—'} /></Card></Col>
        </Row>
        <Card style={{ marginTop: 16 }}>
          <Table
            rowKey="id"
            pagination={false}
            dataSource={report?.components || []}
            columns={[
              { title: 'Integration', dataIndex: 'name' },
              { title: 'Status', dataIndex: 'status', render: statusTag },
              { title: 'Detail', dataIndex: 'detail' },
              {
                title: 'Last success',
                dataIndex: 'lastSuccessAt',
                render: value => value ? String(value).replace('T', ' ') : '—'
              },
              {
                title: 'Last error',
                dataIndex: 'lastError',
                render: value => value || '—'
              }
            ]}
          />
        </Card>
      </div>
    </DashboardLayout>
  );
}

export default SystemIntegrationsPage;
