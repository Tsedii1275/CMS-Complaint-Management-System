import React, { useState, useEffect, useMemo } from 'react';
import {
  Card, Typography, Row, Col, Table, Input, Empty, Alert, Spin, Tag, Button,
  Modal, Upload, message as antMessage, Select
} from 'antd';
import {
  SearchOutlined, SendOutlined, AuditOutlined, PaperClipOutlined, UploadOutlined
} from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import ExportDropdown from '../components/ExportDropdown';
import ApiService from '../services/api';
import { matchesTicketSearch } from '../components/TaskTable';
import { BRAND_COLORS } from '../constants/theme';

const { Title, Text, Paragraph } = Typography;

const PAGE_STYLE = { background: '#f8fafc', minHeight: '100vh', padding: '24px' };
const PAGE_INNER_STYLE = { maxWidth: '1400px', margin: '0 auto' };
const ACCENT_BAR_STYLE = { width: '100px', height: '4px', background: BRAND_COLORS.primary, borderRadius: '2px', marginBottom: '16px' };
const HEADER_BLOCK_STYLE = { marginBottom: '28px' };
const HEADER_TEXT_BLOCK_STYLE = { marginBottom: '16px' };
const PAGE_TITLE_STYLE = { margin: 0, color: '#334155', fontWeight: 600, letterSpacing: '-0.3px' };
const PAGE_SUBTITLE_STYLE = { fontSize: '14px', color: '#64748b', fontWeight: 400 };
const TIME_FILTER_SELECT_STYLE = { width: 150 };
const ALERT_STYLE = { marginBottom: '24px', borderRadius: '12px' };
const LOADING_WRAP_STYLE = { textAlign: 'center', padding: '100px 0' };
const KPI_ROW_STYLE = { marginBottom: '28px' };
const SECTION_HEAD_STYLE = { marginBottom: '16px' };
const SECTION_TITLE_STYLE = { margin: 0, color: '#334155', fontWeight: 600, fontSize: '18px' };
const SECTION_SUBTITLE_STYLE = { fontSize: '13px', color: '#64748b', fontWeight: 400 };
const CHART_ROW_STYLE = { marginBottom: '32px' };
const TABLE_HEADER_ROW_STYLE = { display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' };
const TABLE_TITLE_STYLE = { fontSize: '16px', fontWeight: 700, color: '#0f172a' };
const TABLE_HEADER_ACTIONS_STYLE = { display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' };
const SEARCH_INPUT_STYLE = { width: 320, borderRadius: '8px' };
const SEARCH_ICON_STYLE = { color: '#94a3b8' };
const TABLE_CARD_STYLE = {
  borderRadius: '16px',
  background: '#ffffff',
  border: '1px solid #e2e8f0',
  boxShadow: '0 1px 3px rgba(15, 23, 42, 0.03)',
  marginBottom: '32px'
};
const TICKET_ID_STYLE = {
  fontFamily: 'monospace',
  fontSize: '11px',
  color: '#111827',
  fontWeight: 600,
  background: '#f3f4f6',
  padding: '3px 8px',
  borderRadius: '4px',
  whiteSpace: 'nowrap',
  display: 'inline-block'
};
const TAG_SEMIBOLD_STYLE = { fontWeight: 600 };
const TAG_BOLD_STYLE = { fontWeight: 700 };
const CES_VALUE_STYLE = { fontSize: '13px' };
const COMMENT_STYLE = { margin: 0, fontSize: '12px' };
const KPI_CARD_STYLE = {
  borderRadius: '12px',
  background: '#ffffff',
  border: '1px solid #e2e8f0',
  boxShadow: '0 1px 3px rgba(15, 23, 42, 0.03)',
  height: '100%'
};
const KPI_CARD_BODY_STYLE = { padding: '20px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', height: '100%' };
const KPI_LABEL_STYLE = { fontSize: '13px', fontWeight: 600, color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.5px', display: 'block', marginBottom: '8px' };
const KPI_VALUE_STYLE = { fontSize: '26px', fontWeight: 700, color: '#0f172a', letterSpacing: '-0.3px', lineHeight: 1.2 };
const KPI_SUBTITLE_STYLE = { fontSize: '12px', color: '#64748b', marginTop: '10px', fontWeight: 500 };
const CHART_CARD_STYLE = {
  borderRadius: '16px',
  background: '#ffffff',
  border: '1px solid #e2e8f0',
  boxShadow: '0 1px 3px rgba(15, 23, 42, 0.03)',
  height: '100%'
};
const CHART_CARD_BODY_STYLE = { padding: '24px' };
const CHART_TITLE_STYLE = { fontSize: '16px', fontWeight: 600, color: '#334155' };
const CHART_STACK_STYLE = { display: 'flex', flexDirection: 'column', gap: '14px' };
const CHART_ROW_ITEM_STYLE = { display: 'flex', alignItems: 'center', gap: '16px' };
const CHART_LABEL_STYLE = { minWidth: '120px', fontSize: '12px', fontWeight: 600, color: '#475569', display: 'flex', alignItems: 'center', gap: '6px' };
const CHART_TRACK_STYLE = { flexGrow: 1, background: '#f8fafc', borderRadius: '6px', height: '10px', overflow: 'hidden', border: '1px solid #f1f5f9' };
const CHART_COUNT_STYLE = { minWidth: '80px', textAlign: 'right', fontSize: '12px', fontWeight: 600, color: '#334155' };
const CHART_PERCENT_STYLE = { color: '#94a3b8', fontWeight: 500 };
const MODAL_TITLE_ROW_STYLE = { display: 'flex', alignItems: 'center', gap: '8px' };
const PRIMARY_ICON_STYLE = { color: BRAND_COLORS.primary };
const MODAL_TITLE_TEXT_STYLE = { fontSize: '16px', fontWeight: 600, color: '#0f172a' };
const PRIMARY_BUTTON_STYLE = { backgroundColor: BRAND_COLORS.primary, borderColor: BRAND_COLORS.primary, borderRadius: '6px' };
const MODAL_WRAP_STYLE = { maxWidth: 650 };
const MODAL_BODY_STACK_STYLE = { display: 'flex', flexDirection: 'column', gap: '16px' };
const TICKET_BOX_STYLE = { background: '#f8fafc', padding: '16px', borderRadius: '8px', border: '1px solid #e2e8f0' };
const TICKET_LABEL_STYLE = { fontSize: '12px' };
const TICKET_VALUE_STYLE = { fontSize: '16px', color: BRAND_COLORS.primary };
const FIELD_LABEL_STYLE = { color: '#0f172a', fontSize: '14px', display: 'block', marginBottom: '8px' };
const TEXTAREA_STYLE = { borderRadius: '8px' };
const CLIP_ICON_STYLE = { marginRight: '6px', color: BRAND_COLORS.primary };
const UPLOAD_BUTTON_STYLE = { borderRadius: '6px' };

function npsTagColor(score) {
  if (score >= 9) {
    return 'green';
  }
  if (score <= 6) {
    return 'red';
  }
  return 'orange';
}

function CustomerExperienceDashboard() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [rawFeedbackList, setRawFeedbackList] = useState([]);
  const [, setInvestigationTasks] = useState([]);
  const [selectedTask, setSelectedTask] = useState(null);
  const [cxRemarks, setCxRemarks] = useState('');
  const [cxFileList, setCxFileList] = useState([]);
  const [isEscalating, setIsEscalating] = useState(false);

  // Filters
  const [timePeriod, setTimePeriod] = useState('All Time'); // 'All Time' | 'Year' | 'Month' | 'Week' | 'Day'
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');

      const [listData, enrichedTasks] = await Promise.all([
        ApiService.getCustomerFeedbackList().catch(() => []),
        ApiService.getEnrichedTasks().catch(() => [])
      ]);

      setRawFeedbackList(listData || []);

      // Filter tasks requiring Chief Experience review before escalation to Chief Operation Audit
      const cxTasks = (enrichedTasks || []).filter(task => {
        if (task.definitionKey === 'FormTask_43') return false;
        const currentStage = task.variables?.currentStage || task.variables?.stage;
        if (['CHIEF_OPERATION_AUDIT', 'AUDIT_INVESTIGATION', 'COMMITTEE_REVIEW', 'COMPLETED', 'RESOLVED', 'CLOSED'].includes(currentStage)) {
          return false;
        }
        const isInvestigationRequired = task.variables?.requiresInvestigation === true;
        return (
          isInvestigationRequired && (
            currentStage === 'CHIEF_EXPERIENCE_REVIEW' ||
            currentStage === 'CHIEF_EXPERIENCE' ||
            task.definitionKey === 'FormTask_CEX' ||
            !currentStage
          )
        );
      });
      setInvestigationTasks(cxTasks);
    } catch (err) {
      setError(err.message || 'Failed to load customer experience metrics.');
      console.error('Error loading CX dashboard data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleEscalateToAudit = async (task) => {
    setIsEscalating(true);
    try {
      const serializableCxFiles = cxFileList.map(f => ({
        uid: f.uid || f.name,
        name: f.name,
        size: f.size,
        type: f.type,
        url: f.thumbUrl || f.url || null
      }));

      const existingFiles = task.variables?.investigationFiles || [];
      const updatedFiles = [...existingFiles, ...serializableCxFiles];

      await ApiService.completeTask(task.id, {
        requiresInvestigation: true,
        currentStage: 'CHIEF_OPERATION_AUDIT',
        stage: 'AUDIT_INVESTIGATION',
        escalatedBy: 'Chief Experience Officer',
        cxRemarks: cxRemarks || 'Escalated to Chief Operation Audit for operational investigation.',
        cxFiles: serializableCxFiles,
        investigationFiles: updatedFiles,
        nextStage: 'COMMITTEE_REVIEW',
        auditAccessRoles: ['ROLE_AUDIT_INVESTIGATION_TEAM', 'ROLE_OPERATIONAL_AUDIT_SENIOR_MANAGER', 'ROLE_OPERATIONAL_AUDIT_DIRECTOR']
      });
      antMessage.success('Complaint successfully escalated to Chief Operation Audit with supporting attachments.');
      setSelectedTask(null);
      setCxRemarks('');
      setCxFileList([]);
      await loadData();
    } catch (err) {
      antMessage.error('Failed to escalate complaint to Chief Operation Audit: ' + (err.message || 'Unknown error'));
    } finally {
      setIsEscalating(false);
    }
  };

  // Filter rawFeedbackList based on selected timePeriod & searchQuery
  const filteredList = useMemo(() => {
    let list = [...rawFeedbackList];
    const now = new Date();

    // Time filter
    if (timePeriod && timePeriod !== 'All Time') {
      list = list.filter(item => {
        const dateStr = item.feedbackSubmittedAt || item.submittedAt;
        if (!dateStr) return false;
        const d = new Date(dateStr);
        if (Number.isNaN(d.getTime())) return false;
        const diffMs = now - d;
        const diffDays = diffMs / (1000 * 60 * 60 * 24);

        if (timePeriod === 'Day') return diffDays <= 1;
        if (timePeriod === 'Week') return diffDays <= 7;
        if (timePeriod === 'Month') return diffDays <= 30;
        if (timePeriod === 'Year') return diffDays <= 365;
        return true;
      });
    }

    // Search query filter
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      list = list.filter(item => {
        const comments = (item.additionalComments || item.npsComment || item.cesComment || '').toLowerCase();
        return matchesTicketSearch(item, q) || comments.includes(q);
      });
    }

    return list;
  }, [rawFeedbackList, timePeriod, searchQuery]);

  // Derived Analytics & Calculations directly from filtered survey responses
  const analytics = useMemo(() => {
    const surveyRequestsSent = filteredList.length;

    // Completed responses (where customer completed survey / token expired)
    const completedResponses = filteredList.filter(f =>
      f.tokenExpired === true || Boolean(f.feedbackSubmittedAt || f.submittedAt)
    );
    const totalResponses = completedResponses.length;

    // Question 1: Has your complaint been resolved?
    const q1NoList = completedResponses.filter(f => f.resolutionConfirmed === false);
    const unresolvedComplaintsCount = q1NoList.length;

    // Question 2: CSAT (If Q1 = Yes)
    // Options: Very Satisfied (5), Satisfied (4) -> Positive | Dissatisfied (2), Very Dissatisfied (1) -> Negative
    const csatResponses = completedResponses.filter(f => f.csatScore !== null && f.csatScore !== undefined);

    let verySatisfiedCount = 0;
    let satisfiedCount = 0;
    let dissatisfiedCount = 0;
    let veryDissatisfiedCount = 0;

    csatResponses.forEach(f => {
      const score = f.csatScore;
      if (score === 5) verySatisfiedCount++;
      else if (score === 4) satisfiedCount++;
      else if (score === 2) dissatisfiedCount++;
      else if (score === 1) veryDissatisfiedCount++;
      else if (score >= 4) satisfiedCount++;
      else if (score < 4) dissatisfiedCount++;
    });

    const positiveCsatCount = verySatisfiedCount + satisfiedCount;
    const totalCsatResponses = csatResponses.length;
    const csatPercentage = totalCsatResponses > 0 ? (positiveCsatCount / totalCsatResponses) * 100 : 0;

    // Question 4: CES (If Q1 = Yes)
    // Mapping: Very Easy (4), Easy (3), Difficult (2), Very Difficult (1)
    const parseCesScore = (f) => {
      if (typeof f.cesScore === 'number' && f.cesScore >= 1 && f.cesScore <= 4) return f.cesScore;
      const r = (f.easeRating || '').toLowerCase();
      if (r.includes('very easy')) return 4;
      if (r.includes('easy')) return 3;
      if (r.includes('very difficult')) return 1;
      if (r.includes('difficult')) return 2;
      return null;
    };

    const cesResponses = completedResponses
      .map(f => parseCesScore(f))
      .filter(score => score !== null);

    let veryEasyCount = 0;
    let easyCount = 0;
    let difficultCount = 0;
    let veryDifficultCount = 0;

    cesResponses.forEach(score => {
      if (score === 4) veryEasyCount++;
      else if (score === 3) easyCount++;
      else if (score === 2) difficultCount++;
      else if (score === 1) veryDifficultCount++;
    });

    const totalCesResponses = cesResponses.length;
    const sumCes = cesResponses.reduce((acc, val) => acc + val, 0);
    const avgCes = totalCesResponses > 0 ? sumCes / totalCesResponses : 0;

    // Question 5: NPS (If Q1 = Yes)
    // Scale 0-10: Promoters (9-10), Passives (7-8), Detractors (0-6)
    const npsResponses = completedResponses.filter(f => f.npsScore !== null && f.npsScore !== undefined);
    const totalNpsResponses = npsResponses.length;

    let promotersCount = 0;
    let passivesCount = 0;
    let detractorsCount = 0;

    const npsScoreCounts = new Array(11).fill(0);

    npsResponses.forEach(f => {
      const score = Math.max(0, Math.min(10, Math.round(f.npsScore)));
      npsScoreCounts[score]++;
      if (score >= 9) promotersCount++;
      else if (score >= 7) passivesCount++;
      else detractorsCount++;
    });

    const promotersPercent = totalNpsResponses > 0 ? (promotersCount / totalNpsResponses) * 100 : 0;
    const passivesPercent = totalNpsResponses > 0 ? (passivesCount / totalNpsResponses) * 100 : 0;
    const detractorsPercent = totalNpsResponses > 0 ? (detractorsCount / totalNpsResponses) * 100 : 0;
    const npsScore = promotersPercent - detractorsPercent;

    return {
      surveyRequestsSent,
      totalResponses,
      unresolvedComplaintsCount,

      // CSAT
      csatPercentage,
      positiveCsatCount,
      totalCsatResponses,
      csatDistribution: [
        { label: 'Very Satisfied', count: verySatisfiedCount, color: '#10b981' },
        { label: 'Satisfied', count: satisfiedCount, color: '#3b82f6' },
        { label: 'Dissatisfied', count: dissatisfiedCount, color: '#f59e0b' },
        { label: 'Very Dissatisfied', count: veryDissatisfiedCount, color: '#ef4444' },
      ],

      // CES
      avgCes,
      totalCesResponses,
      cesDistribution: [
        { label: 'Very Easy', count: veryEasyCount, color: '#10b981', score: 4 },
        { label: 'Easy', count: easyCount, color: '#3b82f6', score: 3 },
        { label: 'Difficult', count: difficultCount, color: '#f59e0b', score: 2 },
        { label: 'Very Difficult', count: veryDifficultCount, color: '#ef4444', score: 1 },
      ],

      // NPS
      npsScore,
      promotersCount,
      passivesCount,
      detractorsCount,
      promotersPercent,
      passivesPercent,
      detractorsPercent,
      totalNpsResponses,
      npsDistribution: npsScoreCounts.map((count, score) => {
        let category = 'Detractor';
        let color = '#ef4444';
        if (score >= 9) { category = 'Promoter'; color = '#10b981'; }
        else if (score >= 7) { category = 'Passive'; color = '#f59e0b'; }
        return { score: `${score}`, count, category, color };
      })
    };
  }, [filteredList]);

  const cxExportDataset = () => ({
    filename: `CX_Feedback_Report_${timePeriod}_${new Date().toISOString().slice(0, 10)}`,
    documentTitle: 'Customer Experience Feedback Report',
    subtitle: `Time period: ${timePeriod}`,
    tables: [{
      title: 'Survey Responses',
      sheetName: 'Feedback',
      columns: [
        { header: 'Unique ID No', accessor: (item) => item.dbcTicketId || item.ticketNumber || item.ticketId || '-' },
        {
          header: 'Q1 Complaint Resolved',
          accessor: (item) => {
            if (item.resolutionConfirmed === true) return 'Yes';
            if (item.resolutionConfirmed === false) return 'No';
            return 'Pending';
          }
        },
        { header: 'CSAT Score (1-5)', key: 'csatScore' },
        { header: 'Speed Rating', key: 'resolutionSpeedRating' },
        { header: 'CES Rating', accessor: (item) => item.easeRating || item.cesScore },
        { header: 'NPS Score (0-10)', accessor: (item) => (item.npsScore === null || item.npsScore === undefined ? '-' : item.npsScore) },
        { header: 'Additional Comments', key: 'additionalComments' },
        { header: 'Submission Date', accessor: (item) => item.feedbackSubmittedAt || item.submittedAt, type: 'date' }
      ],
      rows: filteredList
    }]
  });

  const columns = [
    {
      title: 'Unique ID No',
      dataIndex: 'ticketNumber',
      key: 'ticketNumber',
      render: (text, record) => {
        const ticket = record.dbcTicketId || text || record.ticketId || '-';
        return (
          <span style={TICKET_ID_STYLE}>
            {ticket}
          </span>
        );
      }
    },
    {
      title: 'Q1: Resolved?',
      dataIndex: 'resolutionConfirmed',
      key: 'resolutionConfirmed',
      render: (confirmed, record) => {
        if (!record.feedbackSubmittedAt && !record.submittedAt) {
          return <Tag color="blue">Pending Response</Tag>;
        }
        return (
          <Tag color={confirmed === true ? 'green' : 'red'} style={TAG_SEMIBOLD_STYLE}>
            {confirmed === true ? 'Yes (Satisfied)' : 'No (Unresolved)'}
          </Tag>
        );
      }
    },
    {
      title: 'CSAT Rating (Q2)',
      dataIndex: 'csatScore',
      key: 'csatScore',
      render: (score) => {
        if (score === 5) return <Tag color="green">Very Satisfied (5/5)</Tag>;
        if (score === 4) return <Tag color="blue">Satisfied (4/5)</Tag>;
        if (score === 2) return <Tag color="orange">Dissatisfied (2/5)</Tag>;
        if (score === 1) return <Tag color="red">Very Dissatisfied (1/5)</Tag>;
        if (score) return <Text strong>{score} / 5</Text>;
        return <Text type="secondary">-</Text>;
      }
    },
    {
      title: 'NPS (Q5)',
      dataIndex: 'npsScore',
      key: 'npsScore',
      render: (score) => score !== null && score !== undefined ? (
        <Tag color={npsTagColor(score)} style={TAG_BOLD_STYLE}>
          {score} / 10
        </Tag>
      ) : <Text type="secondary">-</Text>
    },
    {
      title: 'CES Rating (Q4)',
      dataIndex: 'easeRating',
      key: 'easeRating',
      render: (text, record) => {
        const val = text || (record.cesScore ? `Score ${record.cesScore}` : null);
        return val ? <Text strong style={CES_VALUE_STYLE}>{val}</Text> : <Text type="secondary">-</Text>;
      }
    },
    {
      title: 'Customer Comments (Q6)',
      dataIndex: 'additionalComments',
      key: 'additionalComments',
      width: '280px',
      render: (text, record) => {
        const comment = text || record.npsComment || record.cesComment;
        return comment ? (
          <Paragraph ellipsis={{ rows: 2, expandable: true }} style={COMMENT_STYLE}>
            {comment}
          </Paragraph>
        ) : <Text type="secondary" italic>No additional comments provided.</Text>;
      }
    },
    {
      title: 'Submission Date',
      dataIndex: 'feedbackSubmittedAt',
      key: 'feedbackSubmittedAt',
      render: (date, record) => {
        const d = date || record.submittedAt;
        return d ? new Date(d).toLocaleString() : '-';
      }
    }
  ];

  // Helper for KPI Cards
  const renderKpiCard = ({ title, value, subtitle }) => (
    <Card
      bordered={false}
      style={KPI_CARD_STYLE}
      bodyStyle={KPI_CARD_BODY_STYLE}
    >
      <div>
        <Text type="secondary" style={KPI_LABEL_STYLE}>
          {title}
        </Text>
        <div style={KPI_VALUE_STYLE}>
          {value}
        </div>
      </div>
      {subtitle && (
        <div style={KPI_SUBTITLE_STYLE}>
          {subtitle}
        </div>
      )}
    </Card>
  );

  // Helper for Chart Cards
  const renderChartCard = ({ title, items, total, showCategoryTags = false }) => {
    return (
      <Card
        bordered={false}
        style={CHART_CARD_STYLE}
        bodyStyle={CHART_CARD_BODY_STYLE}
        title={<span style={CHART_TITLE_STYLE}>{title}</span>}
      >
        <div style={CHART_STACK_STYLE}>
          {items.map((item) => {
            const percentage = total > 0 ? Math.round(((item.count || 0) * 100) / total) : 0;
            const colorToUse = item.color || BRAND_COLORS.primary;
            const categoryTagStyle = {
              fontSize: '10px',
              padding: '1px 5px',
              borderRadius: '4px',
              background: `${colorToUse}15`,
              color: colorToUse,
              fontWeight: 700
            };
            const barFillStyle = {
              width: `${percentage}%`,
              minWidth: item.count > 0 ? '6px' : '0px',
              height: '100%',
              background: colorToUse,
              borderRadius: '6px',
              transition: 'width 0.4s ease'
            };
            return (
              <div key={item.label || item.score} style={CHART_ROW_ITEM_STYLE}>
                <span style={CHART_LABEL_STYLE}>
                  {item.label || `Score ${item.score}`}
                  {showCategoryTags && item.category && (
                    <span style={categoryTagStyle}>
                      {item.category}
                    </span>
                  )}
                </span>
                <div style={CHART_TRACK_STYLE}>
                  <div style={barFillStyle} />
                </div>
                <span style={CHART_COUNT_STYLE}>
                  {item.count} <span style={CHART_PERCENT_STYLE}>({percentage}%)</span>
                </span>
              </div>
            );
          })}
        </div>
      </Card>
    );
  };

  return (
    <DashboardLayout userRole="admin">
      <div style={PAGE_STYLE}>
        <div style={PAGE_INNER_STYLE}>

          {/* Top Accent Bar */}
          <div style={ACCENT_BAR_STYLE} />

          {/* Header & Controls */}
          <div style={HEADER_BLOCK_STYLE}>
            <div style={HEADER_TEXT_BLOCK_STYLE}>
              <Title level={2} style={PAGE_TITLE_STYLE}>
                Customer Experience (CX) Dashboard
              </Title>
              <Text type="secondary" style={PAGE_SUBTITLE_STYLE}>
                Real-time satisfaction measurement, CSAT, NPS, and CES analytics derived directly from customer survey responses.
              </Text>
            </div>

            <Select
              value={timePeriod}
              onChange={(val) => setTimePeriod(val)}
              style={TIME_FILTER_SELECT_STYLE}
            >
              <Select.Option value="All Time">All Time</Select.Option>
              <Select.Option value="Year">Year</Select.Option>
              <Select.Option value="Month">Month</Select.Option>
              <Select.Option value="Week">Week</Select.Option>
              <Select.Option value="Day">Day</Select.Option>
            </Select>
          </div>

          {error && (
            <Alert message="Error Loading CX Data" description={error} type="error" showIcon closable style={ALERT_STYLE} />
          )}

          {loading ? (
            <div style={LOADING_WRAP_STYLE}>
              <Spin size="large" tip="Calculating survey analytics..." />
            </div>
          ) : (
            <div>

              {/* 1. Core Dashboard Metrics (KPI Cards in 2 clean 4-card rows) */}
              <Row gutter={[16, 16]} style={KPI_ROW_STYLE}>
                <Col xs={24} sm={12} md={12} lg={6}>
                  {renderKpiCard({
                    title: 'Survey Requests Sent',
                    value: analytics.surveyRequestsSent,
                    subtitle: 'Total invitations sent'
                  })}
                </Col>
                <Col xs={24} sm={12} md={12} lg={6}>
                  {renderKpiCard({
                    title: 'Total Responses',
                    value: analytics.totalResponses,
                    subtitle: 'Completed surveys'
                  })}
                </Col>
                <Col xs={24} sm={12} md={12} lg={6}>
                  {renderKpiCard({
                    title: 'Unresolved Complaints',
                    value: analytics.unresolvedComplaintsCount,
                    subtitle: 'Q1 Answered No'
                  })}
                </Col>
                <Col xs={24} sm={12} md={12} lg={6}>
                  {renderKpiCard({
                    title: 'CSAT (Q2)',
                    value: `${analytics.csatPercentage.toFixed(1)}%`,
                    subtitle: `Positive: ${analytics.positiveCsatCount} / ${analytics.totalCsatResponses}`
                  })}
                </Col>
                <Col xs={24} sm={12} md={12} lg={6}>
                  {renderKpiCard({
                    title: 'NPS Score (Q5)',
                    value: Math.round(analytics.npsScore),
                    subtitle: `P: ${Math.round(analytics.promotersPercent)}% · D: ${Math.round(analytics.detractorsPercent)}%`
                  })}
                </Col>
                <Col xs={24} sm={12} md={12} lg={6}>
                  {renderKpiCard({
                    title: 'CES Score (Q4)',
                    value: `${analytics.avgCes.toFixed(1)} / 4`,
                    subtitle: 'Avg Effort Score'
                  })}
                </Col>
              </Row>

              {/* 2. Charts & Distributions Row */}
              <div style={SECTION_HEAD_STYLE}>
                <Title level={4} style={SECTION_TITLE_STYLE}>
                  Survey Response Distribution Analytics
                </Title>
                <Text type="secondary" style={SECTION_SUBTITLE_STYLE}>
                  Detailed category and score distribution breakdowns for CSAT (Q2), NPS (Q5), and CES (Q4)
                </Text>
              </div>

              <Row gutter={[24, 24]} style={CHART_ROW_STYLE}>
                <Col xs={24} lg={8}>
                  {renderChartCard({
                    title: 'CSAT Distribution (Q2)',
                    items: analytics.csatDistribution,
                    total: analytics.totalCsatResponses
                  })}
                </Col>
                <Col xs={24} lg={8}>
                  {renderChartCard({
                    title: 'CES Distribution (Q4)',
                    items: analytics.cesDistribution,
                    total: analytics.totalCesResponses
                  })}
                </Col>
                <Col xs={24} lg={8}>
                  {renderChartCard({
                    title: 'NPS Score Distribution (Q5: 0-10)',
                    items: analytics.npsDistribution,
                    total: analytics.totalNpsResponses,
                    showCategoryTags: true
                  })}
                </Col>
              </Row>

              {/* 3. Customer Submissions & Comments Table */}
              <Card
                title={
                  <div style={TABLE_HEADER_ROW_STYLE}>
                    <span style={TABLE_TITLE_STYLE}>
                      Customer Feedback Submissions
                    </span>
                    <div style={TABLE_HEADER_ACTIONS_STYLE}>
                      <Input
                        placeholder="Search by Ticket ID or Feedback Comments..."
                        prefix={<SearchOutlined style={SEARCH_ICON_STYLE} />}
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        style={SEARCH_INPUT_STYLE}
                        allowClear
                      />
                      <ExportDropdown getDataset={cxExportDataset} />
                    </div>
                  </div>
                }
                bordered={false}
                style={TABLE_CARD_STYLE}
              >
                <Table
                  columns={columns}
                  dataSource={filteredList}
                  rowKey={(record) => record.id || record.ticketId || record.ticketNumber}
                  pagination={{
                    pageSize: 10,
                    showSizeChanger: true,
                    pageSizeOptions: [10, 25, 50, 100],
                    showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} survey records`
                  }}
                  locale={{ emptyText: <Empty description="No customer feedback survey responses found for the selected filter" /> }}
                />
              </Card>

              {/* Modal to Escalate to Chief Operation Audit */}
              <Modal
                title={
                  <div style={MODAL_TITLE_ROW_STYLE}>
                    <AuditOutlined style={PRIMARY_ICON_STYLE} />
                    <span style={MODAL_TITLE_TEXT_STYLE}>Escalate Complaint to Chief Operation Audit</span>
                  </div>
                }
                open={Boolean(selectedTask)}
                onCancel={() => setSelectedTask(null)}
                footer={[
                  <Button key="cancel" onClick={() => setSelectedTask(null)}>Cancel</Button>,
                  <Button
                    key="submit"
                    type="primary"
                    loading={isEscalating}
                    icon={<SendOutlined />}
                    style={PRIMARY_BUTTON_STYLE}
                    onClick={() => handleEscalateToAudit(selectedTask)}
                  >
                    Escalate to Chief Operation Audit
                  </Button>
                ]}
                width="100%"
                style={MODAL_WRAP_STYLE}
              >
                {selectedTask && (
                  <div style={MODAL_BODY_STACK_STYLE}>
                    <div style={TICKET_BOX_STYLE}>
                      <Text type="secondary" style={TICKET_LABEL_STYLE}>Complaint Ticket ID:</Text>
                      <br />
                      <Text strong style={TICKET_VALUE_STYLE}>
                        {selectedTask.complaintId || selectedTask.dbcTicketId || selectedTask.variables?.dbcTicketId || '-'}
                      </Text>
                    </div>

                    <div>
                      <Text strong style={FIELD_LABEL_STYLE}>
                        Chief Experience Officer Remarks / Audit Escalation Notes:
                      </Text>
                      <Input.TextArea
                        rows={4}
                        placeholder="Provide detailed remarks and instructions for the Chief Operation Audit team..."
                        value={cxRemarks}
                        onChange={(e) => setCxRemarks(e.target.value)}
                        style={TEXTAREA_STYLE}
                      />
                    </div>

                    <div>
                      <Text strong style={FIELD_LABEL_STYLE}>
                        <PaperClipOutlined style={CLIP_ICON_STYLE} />
                        Attach Supporting Documents / Files:
                      </Text>
                      <Upload
                        fileList={cxFileList}
                        onChange={({ fileList }) => setCxFileList(fileList)}
                        beforeUpload={() => false}
                        multiple
                      >
                        <Button icon={<UploadOutlined />} style={UPLOAD_BUTTON_STYLE}>
                          Select Files
                        </Button>
                      </Upload>
                    </div>
                  </div>
                )}
              </Modal>

            </div>
          )}

        </div>
      </div>
    </DashboardLayout>
  );
}

export default CustomerExperienceDashboard;
