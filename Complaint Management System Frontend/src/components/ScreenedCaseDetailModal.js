import React from 'react';
import { Button, Descriptions, Modal } from 'antd';
import { formatUniqueId } from './TaskTable';

function formatReceived(value) {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString();
}

function ScreenedCaseDetailModal({
  open,
  record,
  title,
  detailLabel = 'Details',
  onClose,
}) {
  return (
    <Modal
      title={title}
      open={open}
      onCancel={onClose}
      footer={<Button onClick={onClose}>Close</Button>}
      destroyOnClose
    >
      {record && (
        <Descriptions column={1} size="small" bordered>
          <Descriptions.Item label="Unique ID No">{formatUniqueId(record)}</Descriptions.Item>
          <Descriptions.Item label="Customer">{record.customerName || '-'}</Descriptions.Item>
          <Descriptions.Item label="Phone">{record.currentContactPhone || '-'}</Descriptions.Item>
          <Descriptions.Item label="Account Number">{record.accountNumber || '-'}</Descriptions.Item>
          <Descriptions.Item label="Received">{formatReceived(record.createdAt)}</Descriptions.Item>
          <Descriptions.Item label={detailLabel}>
            <div style={{ whiteSpace: 'pre-wrap' }}>{record.complaintDescription || '-'}</div>
          </Descriptions.Item>
        </Descriptions>
      )}
    </Modal>
  );
}

export default ScreenedCaseDetailModal;
