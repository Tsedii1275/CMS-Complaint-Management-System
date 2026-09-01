import React, { useState, useEffect } from 'react';
import { Table, Tag, Alert, Typography, Spin } from 'antd';
import { ClockCircleOutlined, WarningFilled, UserOutlined } from '@ant-design/icons';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import moment from 'moment';

const { Title, Text } = Typography;

const slaStyles = {
  root: { padding: '4px 0' },
  summaryBox: {
    background: '#f8fafc',
    padding: '16px 20px',
    borderRadius: '8px',
    border: '1px solid #e2e8f0',
    marginBottom: '20px'
  },
  summaryRow: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '16px',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  label: { fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.5px' },
  ticketId: { fontWeight: 700, fontSize: '16px', color: BRAND_COLORS.primary },
  summaryValue: { fontWeight: 600, fontSize: '14px', color: '#1e293b' },
  classTag: { fontWeight: 600, fontSize: '12px', margin: 0 },
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
  if (key.includes('CMD_SCREENING') || key.includes('SCREENING') || key.includes('TASK_12')) {
    return 'Customer Care Senior Manager Assignment';
  }
  if (key.includes('CMD_OFFICER') || key.includes('TASK_24') || key.includes('TRIAGE')) {
    return 'Customer Care Officer';
  }
  if (key.includes('AUDIT') || key.includes('INVESTIGATION') || key.includes('TASK_57')) {
    return 'Investigation Team';
  }
  if (key.includes('WORKUNIT') || key.includes('WORK_UNIT') || key.includes('DEPARTMENT')) {
    return 'Work Unit Resolution';
  }
  if (key.includes('COMMITTEE') || key.includes('CHIEF_COMMITTEE')) {
    return 'Committee Review';
  }
  if (key.includes('INTAKE') || key.includes('RECORD')) {
    return 'Customer Care Intake';
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
  return [];
}

function timelineRowKey(record) {
  if (record.id) {
    return `tt-${record.id}`;
  }
  if (record.taskId) {
    return `task-${record.taskId}`;
  }
  const stageId = record.complaintId || 'cmp';
  const stageName = record.taskDefinitionKey || record.taskName || 'stage';
  return `stage-${stageId}-${stageName}`;
}

function displayAssignedOwner(rawOwner, loggedInUser) {
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
  } catch (error) {
    void error;
    return null;
  }
}

function classificationTagProps(complaintSummary) {
  const val = complaintSummary.complaintClassification
    || complaintSummary.variables?.complaintClassification
    || complaintSummary.priority;
  const isMissing = !val || String(val).trim() === '' || String(val).trim() === 'null';
  if (isMissing) {
    return { color: 'default', text: 'Not Classified' };
  }
  const raw = String(val).toUpperCase().trim();
  let text = String(val).trim();
  let color = 'blue';
  if (raw.includes('HIGH') || raw.includes('CRITICAL')) {
    text = 'Highly Sensitive';
    color = 'red';
  } else if (raw.includes('SENSITIVE')) {
    text = 'Sensitive';
    color = 'orange';
  } else if (raw.includes('GENERAL') || raw.includes('NORMAL')) {
    text = 'General';
    color = 'green';
  }
  return { color, text };
}

function SlaTimelineComponent({ complaintId }) {
  const [timeline, setTimeline] = useState([]);
  const [complaintSummary, setComplaintSummary] = useState(null);
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

  const loadTimeline = async () => {
    try {
      setLoading(true);
      const [data, summaryData] = await Promise.all([
        ApiService.getComplaintSlaTimeline(complaintId),
        ApiService.getSlaByComplaintId(complaintId).catch(() => null)
      ]);
      const list = toTimelineList(data);
      setTimeline(list);
      setComplaintSummary(summaryData || null);
    } catch (err) {
      console.error('Failed to load SLA timeline:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatDuration = (mins) => {
    if (mins === null || mins === undefined) return '—';
    const num = Number(mins);
    if (num !== num) return '—';
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
    if (num < 480) {
      const hours = (num / 60).toFixed(1);
      return `${hours} hrs`;
    }
    const days = (num / 480).toFixed(1);
    return `${days} day${Number(days) === 1 ? '' : 's'}`;
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
    if (r.resolutionTimeMinutes && r.resolutionTimeMinutes > 0) return r.resolutionTimeMinutes;
    if (r.durationMinutes && r.durationMinutes > 0) return r.durationMinutes;
    return liveElapsedMins(r.startedAt, r.completedAt);
  };

  const getStageBreachInfo = (t) => {
    if (!t) return { isBreached: false };
    const targetMins = t.resolutionSlaTargetMinutes || 240;
    const actualMins = getLiveResolutionTimeMins(t);

    const respTargetMins = t.responseSlaTargetMinutes || 30;
    const respActualMins = getLiveResponseTimeMins(t);

    const isResolutionBreached = t.resolutionSlaStatus === 'BREACHED' || (actualMins > targetMins && actualMins > 0);
    const isResponseBreached = t.responseSlaStatus === 'BREACHED' || (respActualMins > respTargetMins && respActualMins > 0);

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
      const overBy = Math.max(0, respActualMins - respTargetMins);
      return {
        type: 'Time To Respond Exceeded',
        overBy,
        targetMins: respTargetMins,
        actualMins: respActualMins,
        isBreached: true
      };
    }

    return { isBreached: false, actualMins, targetMins };
  };

  const breachStage = timeline.find(t => getStageBreachInfo(t)?.isBreached);
  const breachInfo = breachStage ? getStageBreachInfo(breachStage) : null;
  const classification = complaintSummary ? classificationTagProps(complaintSummary) : null;

  return (
    <div style={slaStyles.root}>
      {complaintSummary && (
        <div style={slaStyles.summaryBox}>
          <div style={slaStyles.summaryRow}>
            <div>
              <Text type="secondary" style={slaStyles.label}>Ticket Identifier</Text>
              <div style={slaStyles.ticketId}>
                {complaintSummary.dbcTicketId || complaintSummary.complaintId || complaintId}
              </div>
            </div>
            <div>
              <Text type="secondary" style={slaStyles.label}>Customer Name</Text>
              <div style={slaStyles.summaryValue}>
                {complaintSummary.customerName || 'Recorded Complainant'}
              </div>
            </div>
            <div>
              <Text type="secondary" style={slaStyles.label}>Category</Text>
              <div style={slaStyles.summaryValue}>
                {complaintSummary.complaintCategory || 'General Issue'}
              </div>
            </div>
            <div>
              <Text type="secondary" style={slaStyles.label}>Classification</Text>
              <div>
                <Tag color={classification.color} style={slaStyles.classTag}>
                  {classification.text}
                </Tag>
              </div>
            </div>
            <div>
              <Text type="secondary" style={slaStyles.label}>Branch / Unit</Text>
              <div style={slaStyles.summaryValue}>
                {complaintSummary.branch || complaintSummary.department || 'Main Branch'}
              </div>
            </div>
          </div>
        </div>
      )}

      <div style={slaStyles.headerRow}>
        <ClockCircleOutlined style={slaStyles.headerIcon} />
        <Title level={4} style={slaStyles.headerTitle}>
          Stage SLA Timeline
        </Title>
      </div>

      {loading ? (
        <div style={slaStyles.loading}>
          <Spin size="medium" tip="Loading SLA Timeline..." />
        </div>
      ) : (
        <div>
          {breachStage && breachInfo && (
            <Alert
              message={
                <div style={slaStyles.breachText}>
                  <WarningFilled style={slaStyles.breachIcon} />
                  <strong>SLA Breach Location Identified:</strong> {formatStageName(breachStage.taskName, breachStage.taskDefinitionKey)} Stage (Time Taken: {formatDuration(breachInfo.actualMins)} vs Target: {formatDuration(breachInfo.targetMins)} — Exceeded by <Text type="danger" strong>+{formatDuration(breachInfo.overBy)}</Text>)
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
                      {formatClock(r.completedAt)}
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
                        ✅ Within SLA
                      </Tag>
                    );
                  }
                }
              ]}
            />
          </div>
        </div>
      )}
    </div>
  );
}

export default SlaTimelineComponent;
