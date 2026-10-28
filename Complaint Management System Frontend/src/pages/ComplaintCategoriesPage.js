import React, { useCallback, useEffect, useState } from 'react';
import { Button, Card, Table, Tag, Typography, message } from 'antd';
import DashboardLayout from '../components/DashboardLayout';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';

const { Title, Text } = Typography;

function ComplaintCategoriesPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await ApiService.getAllComplaintCategories();
      setRows(Array.isArray(data) ? data : []);
    } catch (err) {
      message.error(err?.message || 'Could not load categories.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const setActive = async (row, active) => {
    setUpdatingId(row.id);
    try {
      await ApiService.setComplaintCategoryActive(row.id, active);
      message.success(active ? 'Category turned on.' : 'Category turned off.');
      await load();
    } catch (err) {
      message.error(err?.message || 'Could not update the category.');
    } finally {
      setUpdatingId(null);
    }
  };

  const columns = [
    { title: 'Category', dataIndex: 'name', key: 'name' },
    {
      title: 'Type',
      key: 'builtin',
      render: (_, row) => (row.builtin ? <Tag>Standard</Tag> : <Tag color="blue">Added</Tag>)
    },
    {
      title: 'Status',
      key: 'active',
      render: (_, row) => (row.active ? <Tag color="green">On</Tag> : <Tag>Off</Tag>)
    },
    {
      title: 'Action',
      key: 'action',
      render: (_, row) => {
        if (row.builtin) {
          return <Text type="secondary">Stays available</Text>;
        }
        if (row.active) {
          return (
            <Button size="small" loading={updatingId === row.id} onClick={() => setActive(row, false)}>
              Turn off
            </Button>
          );
        }
        return (
          <Button size="small" type="primary" loading={updatingId === row.id} onClick={() => setActive(row, true)}>
            Turn on
          </Button>
        );
      }
    }
  ];

  return (
    <DashboardLayout>
      <div style={{ maxWidth: 900, margin: '0 auto' }}>
        <Title level={3} style={{ margin: 0, color: BRAND_COLORS.primary }}>Complaint Categories</Title>
        <Text type="secondary" style={{ display: 'block', margin: '8px 0 16px' }}>
          Standard categories stay on the registration form. Added categories can be turned off for new complaints.
        </Text>
        <Card style={{ borderRadius: 8 }}>
          <Table
            rowKey={(row) => row.id || row.name}
            loading={loading}
            dataSource={rows}
            columns={columns}
            pagination={false}
          />
        </Card>
      </div>
    </DashboardLayout>
  );
}

export default ComplaintCategoriesPage;
