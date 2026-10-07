import React, { useState, useEffect } from 'react';
import { Table, Tag, Alert, Typography, Spin } from 'antd';
import { ClockCircleOutlined, WarningFilled, UserOutlined } from '@ant-design/icons';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import { formatUniqueId } from './TaskTable';
import moment from 'moment';

const { Title, Text } = Typography;

const slaStyles = {
  root: { padding: '4px 0' },
  summaryKpiBox: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
    gap: '12px',
    marginBottom: '20px'
  },
  kpiCard: {
    background: '#ffffff',
    border: '1px solid #cbd5e1',
    borderRadius: '8px',
    padding: '12px 16px',
    boxShadow: '0 1px 2px rgba(0,0,0,0.04)'
  },
  kpiLabel: {
    fontSize: '11px',
    fontWeight: 600,
    textTransform: 'uppercase',
    color: '#64748b',
    letterSpacing: '0.5px',
    marginBottom: '4px'
  },
  kpiValue: {
    fontSize: '16px',
    fontWeight: 700,
    color: '#0f172a'
  },
  headerRow: { display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' },
  headerIcon: { color: BRAND_COLORS.primary, fontSize: '18px' },
  headerTitle: { color: BRAND_COLORS.primary, margin: 0, fontSize: '15px' },
  loading: { textAlign: 'center', padding: '24px' },
  breachText: { fontSize: '13px', fontWeight: 600, color: '#991b1b', lineHeight: 1.6 },
  breachIcon: { marginRight: '8px', color: '#dc2626', fontSize: '16px' },
  breachAlert: {
    marginBottom: '16px',
    borderRadius: '8px',
    border: '1px solid #fca5a5',
    background: '#fef2f2',
    padding: '12px 16px'
  },
  tableWrap: {
    background: '#fff',
    padding: '16px 20px',
    borderRadius: '8px',
    border: '1px solid #e5e7eb',
    boxShadow: '0 1px 3px rgba(0,0,0,0.03)'
  },
  emptyWrap: { padding: '32px 16px', textAlign: 'center' },
  emptyIcon: { fontSize: '32px', color: BRAND_COLORS.primary, marginBottom: '12px' },
  emptyTitle: { fontSize: '15px', fontWeight: 600, color: '#374151', marginBottom: '4px' },
  emptyHint: { fontSize: '13px', color: '#6b7280', maxWidth: '480px', margin: '0 auto' },
  colTitle: { fontWeight: 700, color: '#1e293b', fontSize: '13px' },
  stageCell: { fontWeight: 600, color: BRAND_COLORS.primary, fontSize: '13px' },
  ownerCell: { fontWeight: 500, color: '#334155', fontSize: '12px' },
  ownerIcon: { color: BRAND_COLORS.primary, marginRight: '6px' },
  timeCell: { fontFamily: 'monospace', fontSize: '12px', color: '#475569' },
  durationCell: { fontWeight: 600, color: '#1e293b', fontSize: '12px' },
  statusTagBold: { fontWeight: 700, fontSize: '11px', borderRadius: '4px' },
  statusTag: { fontWeight: 600, fontSize: '11px', borderRadius: '4px' }
};

const formatStageName = (rawName, defKey) => {
  const key = ((rawName || '') + ' ' + (defKey || '')).toUpperCase().trim();
  if (key.includes('FORMTASK_57') || key.includes('TASK_57') || key.includes('WORK_UNIT')) {
    return 'Department / Work Unit Resolution';
  }
  if (key.includes('FORMTASK_48') || key.includes('INVESTIGATION') || key.includes('AUDIT')) {
    return 'Audit / Investigation';
  }
  if (key.includes('FORMTASK_CEX') || key.includes('CHIEF_EXPERIENCE') || key.includes('CXO')) {
    return 'Chief Experience Officer Review';
  }
  if (key.includes('SERVICETASK_65') || key.includes('SERVICE_QUALITY')) {
    return 'Service Quality Review';
  }
  if (key.includes('FORMTASK_43') || key.includes('CMD_SCREENING') || key.includes('SCREENING')) {
    return 'Customer Care Screening';
  }
  if (key.includes('SENIOR_MANAGER') || key.includes('MANAGER_ASSIGNMENT')) {
    return 'Customer Care Senior Manager Assignment';
  }
  if (key.includes('COMMITTEE')) {
    return 'Chief Committee Review';
  }
  if (key.includes('CONTACT_CENTER')) {
    return 'Contact Center Intake';
  }
  if (key.includes('BRANCH')) {
    return 'Branch Intake / Resolution';
  }
  if (key.includes('NOTIFICATION')) {
    return 'Customer Care Notification';
  }
  return rawName || defKey || 'Workflow Stage';
};

function toTimelineList(data) {
  if (Array.isArray(data)) {
    return data;
  }
  if (data && Array.isArray(data.data)) {
    return data.data;
  }
  if (data && Array.isArray(data.timeline)) {
    return data.timeline;
  }
  if (data && Array.isArray(data.content)) {
    return data.content;
  }
  return [];
}

function timelineRowKey(record, idx) {
  if (record.id) {
    return `tt-${record.id}`;
  }
  if (record.taskId) {
    return `task-${record.taskId}`;
  }
  const stageId = record.complaintId || 'cmp';
  const stageName = record.taskDefinitionKey || record.taskName || 'stage';
  return `stage-${stageId}-${stageName}-${idx}`;
}

function displayAssignedOwner(rawOwner, loggedInUser) {
  if (!rawOwner) return 'Unassigned';
  const lowerOwner = rawOwner.toString().trim().toLowerCase();
  if (loggedInUser && (lowerOwner === loggedInUser.username?.toLowerCase() || lowerOwner === 'admin')) {
    return loggedInUser.fullName || loggedInUser.username || rawOwner;
  }
  if (lowerOwner === 'admin') {
    return loggedInUser?.fullName || 'Tseday Teka';
  }
  return rawOwner;
}

function formatClock(value) {
  if (!value) {
    return '—';
  }
  return moment(value).format('DD/MM/YYYY HH:mm');
}

function formatStartedTimestamp(record) {
  if (record.startedAt) {
    return formatClock(record.startedAt);
  }
  return formatClock(record.assignedAt);
}

function parseStoredUser() {
  const userStr = localStorage.getItem('user');
  if (!userStr) {
    return null;
  }
  try {
    return JSON.parse(userStr);
  } catch {
    return null;
  }
}

function SlaTimelineComponent({ complaintId }) {
  const [timeline, setTimeline] = useState([]);
  const [loading, setLoading] = useState(true);
  const [now, setNow] = useState(moment());

  useEffect(() => {
    const timer = setInterval(() => setNow(moment()), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    if (complaintId) {
      loadTimeline();
    }
  }, [complaintId]);

  const resolveTargetId = (idOrRecord) => {
    if (!idOrRecord) return '';
    if (typeof idOrRecord === 'string') return idOrRecord;
    return idOrRecord.dbcTicketId || idOrRecord.complaintId || idOrRecord.generalTicketId || idOrRecord.processInstanceId || formatUniqueId(idOrRecord);
  };

  const loadTimeline = async () => {
    const targetId = resolveTargetId(complaintId);
    if (!targetId) return;
    try {
      setLoading(true);
      const data = await ApiService.getComplaintSlaTimeline(targetId).catch((err) => {
        console.error('Failed to load complaint SLA timeline endpoint:', err);
        return [];
      });
      setTimeline(toTimelineList(data));
    } catch (err) {
      console.error('Failed to load SLA timeline:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatDuration = (mins) => {
    if (mins === null || mins === undefined) return '—';
    const num = Number(mins);
    if (Number.isNaN(num)) return '—';
    if (num <= 0) return '0 sec';
    if (num < 1) {
      const secs = Math.round(num * 60);
      if (secs > 0) {
        return `${secs} sec`;
      }
      return '< 1 sec';
    }
    if (num < 60) {
      const minsRound = Math.round(num);
      return `${minsRound} min${minsRound === 1 ? '' : 's'}`;
    }
    const hours = Math.floor(num / 60);
    const remMins = Math.round(num % 60);
    if (hours > 0 && remMins > 0) {
      return `${hours}h ${remMins}m`;
    }
    if (hours > 0) {
      return `${hours}h`;
    }
    const finalMinsRound = Math.round(num);
    return `${finalMinsRound} mins`;
  };

  const liveElapsedMins = (startedAt, completedAt) => {
    if (!startedAt) return 0;
    const end = completedAt ? moment(completedAt) : now;
    return Math.max(0, end.diff(moment(startedAt), 'seconds') / 60);
  };

  const getLiveResponseTimeMins = (r) => {
    if (r.responseTimeMinutes && r.responseTimeMinutes > 0) return r.responseTimeMinutes;
    return liveElapsedMins(r.startedAt, r.completedAt);
  };

  const getLiveResolutionTimeMins = (r) => {
    if (r.completedAt) {
      if (r.resolutionTimeMinutes && r.resolutionTimeMinutes > 0) return r.resolutionTimeMinutes;
      if (r.durationMinutes && r.durationMinutes > 0) return r.durationMinutes;
    }
    return liveElapsedMins(r.startedAt, r.completedAt);
  };

  const getStageBreachInfo = (t) => {
    if (!t) return { isBreached: false };
    const targetMins = t.resolutionSlaTargetMinutes;
    if (targetMins == null) {
      const breached = t.resolutionSlaStatus === 'BREACHED' || t.resolutionSlaStatus === 'OVERDUE';
      return { isBreached: breached, overBy: 0, targetMins: null, actualMins: 0 };
    }
    const actualMins = (t.durationMinutes !== undefined && t.durationMinutes !== null)
      ? t.durationMinutes
      : getLiveResolutionTimeMins(t);

    const isResolutionBreached = t.resolutionSlaStatus === 'BREACHED'
      || t.resolutionSlaStatus === 'OVERDUE'
      || (actualMins > targetMins && actualMins > 0 && t.resolutionSlaStatus !== 'ON_TIME' && t.resolutionSlaStatus !== 'RESOLVED_WITHIN_SLA');
    const isResponseBreached = t.responseSlaStatus === 'BREACHED' || t.responseSlaStatus === 'OVERDUE';

    if (isResolutionBreached) {
      const overBy = Math.max(0, actualMins - targetMins);
      return {
        type: 'Time To Resolution Exceeded',
        overBy,
        targetMins,
        actualMins,
        isBreached: true
      };
    }

    if (isResponseBreached) {
      const respTargetMins = t.responseSlaTargetMinutes || 30;
      const respActualMins = getLiveResponseTimeMins(t);
      const overBy = Math.max(0, respActualMins - respTargetMins);
      return {
        type: 'Time To Respond Exceeded',
        overBy,
        targetMins: respTargetMins,
        actualMins: respActualMins,
        isBreached: true
      };
    }

    return { isBreached: false };
  };

  if (loading) {
    return (
      <div style={slaStyles.loading}>
        <Spin size="medium" tip="Loading Stage SLA Timeline..." />
      </div>
    );
  }

  const breachStage = timeline.find(t => getStageBreachInfo(t)?.isBreached);
  const breachInfo = breachStage ? getStageBreachInfo(breachStage) : null;

  // Summary Metrics Calculation
  const totalStages = timeline.length;
  const breachedCount = timeline.filter(t => getStageBreachInfo(t)?.isBreached).length;
  const activeTask = timeline.find(t => !t.completedAt) || timeline[timeline.length - 1];
  const currentStageName = activeTask ? formatStageName(activeTask.taskName, activeTask.taskDefinitionKey) : 'Closed';

  return (
    <div style={slaStyles.root}>
      {/* Summary KPI Cards Section */}
      <div style={slaStyles.summaryKpiBox}>
        <div style={slaStyles.kpiCard}>
          <div style={slaStyles.kpiLabel}>Total Stages</div>
          <div style={slaStyles.kpiValue}>{totalStages}</div>
        </div>
        <div style={slaStyles.kpiCard}>
          <div style={slaStyles.kpiLabel}>Breached Stages</div>
          <div style={{ ...slaStyles.kpiValue, color: breachedCount > 0 ? '#dc2626' : '#16a34a' }}>
            {breachedCount}
          </div>
        </div>
        <div style={slaStyles.kpiCard}>
          <div style={slaStyles.kpiLabel}>Current Stage</div>
          <div style={{ ...slaStyles.kpiValue, fontSize: '13px', color: BRAND_COLORS.primary }}>
            {currentStageName}
          </div>
        </div>
      </div>

      <div style={slaStyles.headerRow}>
        <ClockCircleOutlined style={slaStyles.headerIcon} />
        <Title level={4} style={slaStyles.headerTitle}>
          Stage SLA Timeline
        </Title>
      </div>

      <div>
        {/* Highlight Breach Location */}
        {breachStage && breachInfo && (
          <Alert
            message={
              <div style={slaStyles.breachText}>
                <WarningFilled style={slaStyles.breachIcon} />
                <strong>SLA Breach Location Identified:</strong> {formatStageName(breachStage.taskName, breachStage.taskDefinitionKey)} Stage (Assigned To: {displayAssignedOwner(breachStage.assignedUser, parseStoredUser())}) — Time Taken: {formatDuration(breachInfo.actualMins)} vs Target: {formatDuration(breachInfo.targetMins)} — Exceeded by <Text type="danger" strong>+{formatDuration(breachInfo.overBy)}</Text>
              </div>
            }
            type="error"
            showIcon={false}
            style={slaStyles.breachAlert}
          />
        )}

        <div style={slaStyles.tableWrap}>
          <Table
            dataSource={timeline}
            rowKey={timelineRowKey}
            pagination={false}
            size="middle"
            bordered={false}
            locale={{
              emptyText: (
                <div style={slaStyles.emptyWrap}>
                  <ClockCircleOutlined style={slaStyles.emptyIcon} />
                  <div style={slaStyles.emptyTitle}>
                    No SLA stage history is currently available for this complaint.
                  </div>
                  <div style={slaStyles.emptyHint}>
                    SLA stage history is populated as the complaint transitions through workflow stages.
                  </div>
                </div>
              )
            }}
            columns={[
              {
                title: <span style={slaStyles.colTitle}>Stage</span>,
                key: 'stage',
                render: (_, r) => (
                  <span style={slaStyles.stageCell}>
                    {formatStageName(r.taskName, r.taskDefinitionKey)}
                  </span>
                )
              },
              {
                title: <span style={slaStyles.colTitle}>Assigned To</span>,
                key: 'assignedTo',
                render: (_, r) => {
                  const rawOwner = r.assignedUser || r.claimedBy || 'Unassigned';
                  const owner = displayAssignedOwner(rawOwner, parseStoredUser());
                  return (
                    <span style={slaStyles.ownerCell}>
                      <UserOutlined style={slaStyles.ownerIcon} />
                      {owner}
                    </span>
                  );
                }
              },
              {
                title: <span style={slaStyles.colTitle}>Started</span>,
                key: 'started',
                render: (_, r) => (
                  <span style={slaStyles.timeCell}>
                    {formatStartedTimestamp(r)}
                  </span>
                )
              },
              {
                title: <span style={slaStyles.colTitle}>Completed</span>,
                key: 'completed',
                render: (_, r) => (
                  <span style={slaStyles.timeCell}>
                    {r.completedAt ? formatClock(r.completedAt) : '—'}
                  </span>
                )
              },
              {
                title: <span style={slaStyles.colTitle}>Duration</span>,
                key: 'duration',
                render: (_, r) => (
                  <span style={slaStyles.durationCell}>
                    {formatDuration(getLiveResolutionTimeMins(r))}
                  </span>
                )
              },
              {
                title: <span style={slaStyles.colTitle}>SLA Status</span>,
                key: 'slaStatus',
                render: (_, r) => {
                  const stageInfo = getStageBreachInfo(r);
                  if (stageInfo?.isBreached) {
                    return (
                      <Tag color="red" style={slaStyles.statusTagBold}>
                        ❌ Breached (+{formatDuration(stageInfo.overBy)})
                      </Tag>
                    );
                  }
                  if (!r.completedAt) {
                    return (
                      <Tag color="processing" style={slaStyles.statusTag}>
                        🔄 In Progress
                      </Tag>
                    );
                  }
                  return (
                    <Tag color="green" style={slaStyles.statusTag}>
                      ✅ Met
                    </Tag>
                  );
                }
              }
            ]}
          />
        </div>
      </div>
    </div>
  );
};

export default SlaTimelineComponent;
