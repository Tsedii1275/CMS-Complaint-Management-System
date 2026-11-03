import React from 'react';
import { Timeline } from 'antd';
import { formatUniqueId, formatIntakeId } from './TaskTable';
import CaseAttachments from './CaseAttachments';
import { TRAIL_ACTION_LABELS, reviewTrailOf } from '../utils/investigationWorkflow';

function classificationLabel(task) {
  const raw = String(task?.variables?.priorityLevel || task?.priority || 'General').toUpperCase();
  if (raw.includes('HIGH')) return 'Highly Sensitive';
  if (raw.includes('SENSITIVE')) return 'Sensitive';
  return 'General';
}

function Fact({ label, children }) {
  return (
    <div style={{ minWidth: 0 }}>
      <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '2px' }}>{label}</div>
      <div style={{ fontSize: '14px', color: '#0f172a', fontWeight: 600, wordBreak: 'break-word' }}>{children}</div>
    </div>
  );
}

function formatTrailTime(value) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
}

function ReviewTrail({ task }) {
  const trail = reviewTrailOf(task);
  if (trail.length === 0) return null;
  return (
    <div style={{ marginTop: '16px' }}>
      <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '8px' }}>Investigation Review History</div>
      <Timeline
        items={trail.map((entry, index) => ({
          key: `${entry.step || 'step'}-${index}`,
          children: (
            <div>
              <div style={{ fontSize: '13px', fontWeight: 600, color: '#0f172a' }}>
                {TRAIL_ACTION_LABELS[entry.action] || entry.action}
              </div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>
                {entry.actor || 'unknown'} &bull; {formatTrailTime(entry.at)}
                {entry.fileCount ? ` \u2022 ${entry.fileCount} file(s)` : ''}
              </div>
              {entry.remarks ? (
                <div style={{ marginTop: '2px', fontSize: '13px', color: '#334155', whiteSpace: 'pre-wrap' }}>
                  {entry.remarks}
                </div>
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
  const uniqueId = formatUniqueId(task);
  const intakeId = formatIntakeId(task);
  const notes = vars.notes || vars.additionalRemarks || task?.description;
  return (
    <div style={{
      background: '#f8fafc',
      border: '1px solid #e2e8f0',
      borderRadius: '10px',
      padding: '16px',
      marginBottom: '20px'
    }}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '14px 24px' }}>
        <Fact label="Unique ID No">
          {uniqueId}
          {intakeId && intakeId !== uniqueId ? (
            <span style={{ display: 'block', fontWeight: 500, color: '#64748b', fontSize: '12px' }}>Intake {intakeId}</span>
          ) : null}
        </Fact>
        <Fact label="Customer Name">{vars.customerName || task?.customerName || 'N/A'}</Fact>
        <Fact label="Classification">{classificationLabel(task)}</Fact>
        <Fact label="Category">{vars.complaintCategory || vars.complaint?.category || 'N/A'}</Fact>
      </div>
      {notes ? (
        <div style={{ marginTop: '16px' }}>
          <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '4px' }}>Customer Care Officer notes</div>
          <div style={{ fontSize: '14px', color: '#334155', whiteSpace: 'pre-wrap' }}>{notes}</div>
        </div>
      ) : null}
      <CaseAttachments variables={task?.variables} />
      <ReviewTrail task={task} />
    </div>
  );
}

export default InvestigationCaseSummary;
