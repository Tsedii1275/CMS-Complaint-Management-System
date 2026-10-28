import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Table, Card, Empty, Alert, Button, Tooltip } from 'antd';
import { EyeOutlined } from '@ant-design/icons';
import ApiService from '../services/api';
import { formatUniqueId } from './TaskTable';
import ScreenedCaseDetailModal from './ScreenedCaseDetailModal';

function formatDate(value) {
  if (!value) return '-';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? '-' : d.toLocaleString();
}

function matchesSearch(record, query) {
  const q = String(query || '').toLowerCase().trim();
  if (!q) return true;
  return [
    formatUniqueId(record),
    record.customerName,
    record.currentContactPhone,
    record.accountNumber,
    record.complaintDescription,
  ].some((v) => String(v || '').toLowerCase().includes(q));
}

/**
 * Feedback items screened as "Other - Feedback" by Customer Care.
 * Read-only list for the Service Quality Director.
 */
function FeedbackPanel({
  searchQuery = '',
  loadItems,
  descriptionTitle = 'Feedback',
  detailsTitle = 'Feedback',
  detailLabel = 'Details',
  emptyDescription = 'No feedback received yet.',
  totalNoun = 'feedback items',
}) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const fetcher = loadItems || (() => ApiService.getFeedbackSlaMetrics());
      const data = await fetcher();
      setItems(Array.isArray(data) ? data : []);
      setError('');
    } catch (err) {
      setError(err?.message || 'Failed to load cases.');
    } finally {
      setLoading(false);
    }
  }, [loadItems]);

  useEffect(() => {
    load();
  }, [load]);

  const rows = useMemo(
    () => items.filter((r) => matchesSearch(r, searchQuery)),
    [items, searchQuery]
  );

  const columns = [
    {
      title: 'Unique ID No',
      key: 'id',
      render: (_, r) => formatUniqueId(r),
      sorter: (a, b) => String(formatUniqueId(a)).localeCompare(String(formatUniqueId(b)), undefined, { numeric: true }),
    },
    { title: 'Customer', dataIndex: 'customerName', key: 'customerName', render: (v) => v || '-' },
    { title: 'Phone', dataIndex: 'currentContactPhone', key: 'phone', render: (v) => v || '-' },
    {
      title: descriptionTitle,
      dataIndex: 'complaintDescription',
      key: 'description',
      ellipsis: true,
      render: (v) => v || '-',
    },
    {
      title: 'Received',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: formatDate,
      sorter: (a, b) => new Date(a.createdAt || 0) - new Date(b.createdAt || 0),
    },
    {
      title: 'Action',
      key: 'action',
      align: 'center',
      render: (_, r) => (
        <Tooltip title="View Details">
          <Button type="text" size="small" icon={<EyeOutlined />} onClick={() => setSelected(r)} />
        </Tooltip>
      ),
    },
  ];

  return (
    <Card style={{ borderRadius: '8px' }}>
      {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 12 }} />}
      <Table
        rowKey={(r) => r.id || r.processInstanceId || formatUniqueId(r)}
        loading={loading}
        dataSource={rows}
        columns={columns}
        locale={{ emptyText: <Empty description={emptyDescription} image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
        pagination={{
          defaultPageSize: 10,
          showSizeChanger: true,
          pageSizeOptions: [10, 25, 50, 100],
          showQuickJumper: true,
          showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} ${totalNoun}`,
        }}
        scroll={{ x: 'max-content' }}
      />
      <ScreenedCaseDetailModal
        open={Boolean(selected)}
        record={selected}
        title={detailsTitle}
        detailLabel={detailLabel}
        onClose={() => setSelected(null)}
      />
    </Card>
  );
}

export default FeedbackPanel;
