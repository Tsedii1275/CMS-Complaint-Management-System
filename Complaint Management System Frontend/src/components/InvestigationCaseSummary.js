import React from 'react';
import { Row, Col, Typography, Space, Tag, Button, Timeline } from 'antd';
import { PaperClipOutlined } from '@ant-design/icons';
import { formatUniqueId, formatIntakeId } from './TaskTable';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import { collectAttachments } from '../utils/attachments';
import { TRAIL_ACTION_LABELS, reviewTrailOf } from '../utils/investigationWorkflow';

const { Text, Paragraph } = Typography;

const sectionStyle = {
  marginBottom: '16px',
  padding: '12px',
  backgroundColor: '#fff',
  border: '1px solid #e2e8f0',
  borderRadius: '8px'
};

function classificationTag(task) {
  const raw = String(task?.variables?.priorityLevel || task?.priority || 'General').toUpperCase();
  if (raw.includes('HIGH')) return <Tag color="red">Highly Sensitive</Tag>;
  if (raw.includes('SENSITIVE')) return <Tag color="orange">Sensitive</Tag>;
  return <Tag color="green">General</Tag>;
}

function formatTrailTime(value) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
}

function CaseFacts({ task }) {
  const vars = task?.variables || {};
  const uniqueId = formatUniqueId(task);
  const intakeId = formatIntakeId(task);
  return (
    <Row gutter={[16, 12]} style={{ ...sectionStyle, backgroundColor: '#f8fafc' }}>
      <Col xs={24} sm={12}>
        <Text type="secondary" style={{ fontSize: '12px', display: 'block' }}>Unique ID No</Text>
        <Text code style={{ color: BRAND_COLORS.primary, fontWeight: 600 }}>{uniqueId}</Text>
        {intakeId && intakeId !== uniqueId ? (
          <Text type="secondary" style={{ fontSize: '11px', display: 'block' }}>Intake {intakeId}</Text>
        ) : null}
      </Col>
      <Col xs={24} sm={12}>
        <Text type="secondary" style={{ fontSize: '12px', display: 'block' }}>Customer Name</Text>
        <Text strong>{vars.customerName || task?.customerName || 'N/A'}</Text>
      </Col>
      <Col xs={24} sm={12}>
        <Text type="secondary" style={{ fontSize: '12px', display: 'block' }}>Complaint Classification</Text>
        {classificationTag(task)}
      </Col>
      <Col xs={24} sm={12}>
        <Text type="secondary" style={{ fontSize: '12px', display: 'block' }}>Complaint Category</Text>
        <Text>{vars.complaintCategory || vars.complaint?.category || 'N/A'}</Text>
      </Col>
    </Row>
  );
}

function CaseDocuments({ task }) {
  const vars = task?.variables || {};
  const files = collectAttachments(vars.investigationFiles, vars.complaint?.evidenceFiles, vars.evidenceFiles);
  return (
    <div style={{ ...sectionStyle, backgroundColor: '#f0f9ff', borderColor: '#bae6fd' }}>
      <Text strong style={{ color: '#0369a1', display: 'block', marginBottom: '8px' }}>
        <PaperClipOutlined style={{ marginRight: '6px' }} />
        Attached Evidence &amp; Documents ({files.length})
      </Text>
      {files.length === 0 ? (
        <Text type="secondary" style={{ fontStyle: 'italic', fontSize: '12px' }}>No documents attached.</Text>
      ) : (
        <Space direction="vertical" style={{ width: '100%' }}>
          {files.map((file) => (
            <div key={file.url} style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', background: '#fff', padding: '6px 10px', borderRadius: '6px', border: '1px solid #e0f2fe' }}>
              <Text style={{ fontSize: '13px' }}>{file.name}</Text>
              <a href={ApiService.getAttachmentUrl(file.url)} download={file.name} target="_blank" rel="noopener noreferrer">
                <Button size="small" type="link" style={{ padding: 0 }}>View / Download</Button>
              </a>
            </div>
          ))}
        </Space>
      )}
    </div>
  );
}

function ReviewTrail({ task }) {
  const trail = reviewTrailOf(task);
  if (trail.length === 0) return null;
  return (
    <div style={sectionStyle}>
      <Text strong style={{ display: 'block', marginBottom: '10px', color: '#0f172a' }}>
        Investigation Review History
      </Text>
      <Timeline
        items={trail.map((entry, index) => ({
          key: `${entry.step}-${index}`,
          children: (
            <div>
              <Text strong style={{ fontSize: '13px' }}>
                {TRAIL_ACTION_LABELS[entry.action] || entry.action}
              </Text>
              <div style={{ fontSize: '11px', color: '#64748b' }}>
                {entry.actor || 'unknown'} &bull; {formatTrailTime(entry.at)}
                {entry.fileCount ? ` \u2022 ${entry.fileCount} file(s)` : ''}
              </div>
              {entry.remarks ? (
                <Paragraph style={{ margin: '4px 0 0 0', fontSize: '13px', color: '#475569' }}>
                  {entry.remarks}
                </Paragraph>
              ) : null}
            </div>
          )
        }))}
      />
    </div>
  );
}

function InvestigationCaseSummary({ task }) {
  const vars = task?.variables || {};
  const ccoNotes = vars.notes || vars.additionalRemarks || task?.description;
  return (
    <div>
      <CaseFacts task={task} />
      {ccoNotes ? (
        <div style={sectionStyle}>
          <Text strong style={{ display: 'block', marginBottom: '4px', color: '#0f172a' }}>
            Customer Care Officer Notes &amp; Narrative
          </Text>
          <Paragraph style={{ margin: 0, color: '#475569', fontSize: '13px' }}>{ccoNotes}</Paragraph>
        </div>
      ) : null}
      <CaseDocuments task={task} />
      <ReviewTrail task={task} />
    </div>
  );
}

export default InvestigationCaseSummary;
