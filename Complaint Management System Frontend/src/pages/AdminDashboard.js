import React, { useState, useEffect } from 'react';
import { Card, Typography, Table, Tag, DatePicker, Select, Input, Space, Button, Alert, Tooltip, Tabs, Modal, message, Popconfirm, Row, Col } from 'antd';
import { WarningOutlined, CheckCircleOutlined, ExclamationCircleOutlined, LineChartOutlined, PieChartOutlined, SettingOutlined, SyncOutlined, EditOutlined, DeleteOutlined, PaperClipOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import ExportDropdown from '../components/ExportDropdown';
import { formatUniqueId, formatIntakeId, matchesTicketSearch, compareTicketNumbersAsc } from '../components/TaskTable';
import Pagination from '../components/Pagination';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import { normalizeComplaintStatus, renderComplaintStatusTag, COMPLAINT_STATUS } from '../utils/statusUtils';
import moment from 'moment';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

const dashPlaceholder = <span style={{ color: '#94a3b8' }}>-</span>;

function renderCriEditingControl(type, val, dataIndex, options, onChange) {
  if (type === 'select') {
    return (
      <Select
        value={val}
        onChange={(v) => onChange(dataIndex, v)}
        size="small"
        style={{ width: '100%', minWidth: '130px' }}
      >
        {options.map(opt => (
          <Option key={opt} value={opt}>{opt}</Option>
        ))}
      </Select>
    );
  }
  if (type === 'date') {
    return (
      <DatePicker
        value={val && val !== '-' ? moment(val) : null}
        onChange={(d) => onChange(dataIndex, d ? d.format('YYYY-MM-DD') : '')}
        size="small"
        format="YYYY-MM-DD"
        style={{ width: '100%', minWidth: '130px' }}
      />
    );
  }
  return (
    <Input
      value={val === '-' ? '' : val}
      placeholder="-"
      onChange={(e) => onChange(dataIndex, e.target.value || '-')}
      size="small"
      style={{ width: '100%', minWidth: '120px' }}
    />
  );
}

function renderSupportingEvidenceCell(record, cellValue) {
  const link = record.evidenceUrl;
  if (link) {
    return (
      <a href={ApiService.getAttachmentUrl(link)} target="_blank" rel="noreferrer" style={{ color: '#2563eb', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
        <PaperClipOutlined /> Attached
      </a>
    );
  }
  if (cellValue === 'Attached') {
    return <Tag color="blue" style={{ fontWeight: 600, margin: 0 }}>Attached</Tag>;
  }
  return dashPlaceholder;
}

function renderCriReadOnlyCell(record, dataIndex, cellValue) {
  if (!cellValue || cellValue === '-') {
    return dashPlaceholder;
  }
  if (dataIndex === 'caseStatus') {
    return renderComplaintStatusTag(cellValue);
  }
  if (dataIndex === 'supportingEvidence') {
    return renderSupportingEvidenceCell(record, cellValue);
  }
  return <span style={{ color: '#1e293b', fontSize: '13px' }}>{cellValue}</span>;
}

// ─── Excel Standard Master Table Option Constants ───
const RECEIVED_BY_OPTIONS = [
  'Customer Care-Telephone',
  'Customer Care-In person',
  'Contact Center',
  'Contact Center-Email',
  'Digital Marketing',
  'HO',
  'Branch',
  'District',
  '-'
];
const CHANNEL_OPTIONS = ['Agent', 'ATM', 'Branch', 'District Office', 'HO', 'Super App', 'Merchant', '-'];
const DISTRICT_DEPARTMENT_OPTIONS = [
  'HO',
  'Corporate Center',
  'East Addis',
  'North Addis',
  'South Addis',
  'West Addis',
  'Adama',
  'Bahir Dar',
  'Dessie',
  'Dire Dawa',
  'Hawassa',
  'Jimma',
  'Mekelle',
  'Nekemte',
  'South-West',
  'Wolaita Soddo',
  '-'
];
const SERVICE_TYPE_OPTIONS = [
  'Deposit Account',
  'Credit/Financing',
  'Digital Banking',
  'ATM & Card Banking',
  'RTGs',
  'International Banking',
  'Vendor/Procurement',
  'Incoming Fund Transfer',
  'Outgoing Fund Transfer',
  'Merchant Service via Super App',
  'Interest Free Banking Services',
  'Other',
  '-'
];
const CLASSIFICATION_OPTIONS = ['General', 'Sensitive', 'Highly Sensitive', '-'];
const CATEGORY_OPTIONS = ['Customer Service Issues', 'Transaction Error', 'Account Management', 'Banking App Issues', 'Credit/Financing Concerns', 'Fraud & Security Risk', 'Information Disclosure', 'ATM & Card Banking Issues', 'Policy & Compliance Disputes', 'System Failure', 'Branch Operation', 'General', '-'];
const YES_NO_OPTIONS = ['Yes', 'No', '-'];
const FORWARDING_REASON_OPTIONS = [
  'For Justification',
  'For Resolution',
  'For Further Investigation',
  'For further Information and Evidence',
  'For Management Intervention',
  '-'
];
const RESOLUTION_PLAN_OPTIONS = [
  'Apology',
  'Clarification',
  'Justification',
  'Account Blockage Lifted',
  'Compensation',
  'Refund',
  'Correction',
  'Reversal of Pending Transactions',
  'Account Credited Back by Claimed Amount',
  "Customer's Request Fulfilled",
  'Pending Transaction Completed',
  '-'
];
const NOTIFICATION_MEANS_OPTIONS = [
  'Telephone',
  'email',
  'SMS',
  'Telegram Message',
  'Facebook Inbox',
  'LinkedIn Message',
  'In writing',
  'In Person',
  '-'
];
const REACTION_OPTIONS = ['Very Satisfied', 'Satisfied', 'Neutral', 'Dissatisfied', 'Extremely Dissatisfied', '-'];
const LIFETIME_VALUE_OPTIONS = ['High', 'Medium', 'Standard', '-'];

const CATEGORY_PIE_COLORS = [
  '#012169',
  '#FFCB70',
  '#0f766e',
  '#2563eb',
  '#dc2626',
  '#7c3aed',
  '#d97706',
  '#059669',
  '#0891b2',
  '#be185d'
];

function hasDisplayCategory(value) {
  const text = String(value || '').trim();
  return Boolean(text) && text !== '-' && text.toLowerCase() !== 'unspecified' && text.toLowerCase() !== 'null';
}

function polarPoint(cx, cy, r, angle) {
  return {
    x: cx + r * Math.cos(angle),
    y: cy + r * Math.sin(angle)
  };
}

function pieSlicePath(cx, cy, r, start, end) {
  const sweep = end - start;
  if (sweep >= Math.PI * 2 - 0.0001) {
    return `M ${cx} ${cy - r} A ${r} ${r} 0 1 1 ${cx} ${cy + r} A ${r} ${r} 0 1 1 ${cx} ${cy - r} Z`;
  }
  const from = polarPoint(cx, cy, r, start);
  const to = polarPoint(cx, cy, r, end);
  const largeArc = sweep > Math.PI ? 1 : 0;
  return `M ${cx} ${cy} L ${from.x} ${from.y} A ${r} ${r} 0 ${largeArc} 1 ${to.x} ${to.y} Z`;
}

function CategoryPieChart({ slices }) {
  const [hovered, setHovered] = useState(null);
  const size = 180;
  const cx = size / 2;
  const cy = size / 2;
  const r = 74;
  const total = slices.reduce((sum, item) => sum + item.value, 0);

  let angle = -Math.PI / 2;
  const drawn = slices.map((item) => {
    const sweep = total > 0 ? (item.value / total) * Math.PI * 2 : 0;
    const start = angle;
    const end = angle + sweep;
    angle = end;
    return { ...item, start, end, percent: total > 0 ? ((item.value / total) * 100).toFixed(1) : '0.0' };
  });

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 16, minHeight: 180 }}>
      <div style={{ position: 'relative', width: size, height: size, flexShrink: 0 }}>
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
          {drawn.length === 0 ? (
            <circle cx={cx} cy={cy} r={r} fill="#e2e8f0" />
          ) : drawn.map(slice => (
            <path
              key={slice.name}
              d={pieSlicePath(cx, cy, r, slice.start, slice.end)}
              fill={slice.color}
              stroke="#fff"
              strokeWidth="2"
              style={{ cursor: 'pointer', opacity: hovered && hovered.name !== slice.name ? 0.55 : 1 }}
              onMouseEnter={() => setHovered(slice)}
              onMouseLeave={() => setHovered(null)}
            />
          ))}
          <circle cx={cx} cy={cy} r={42} fill="#fff" />
        </svg>
        {hovered ? (
          <div
            style={{
              position: 'absolute',
              left: '50%',
              top: '50%',
              transform: 'translate(-50%, -50%)',
              background: '#0f172a',
              color: '#fff',
              padding: '6px 10px',
              borderRadius: 8,
              fontSize: 11,
              pointerEvents: 'none',
              minWidth: 120,
              textAlign: 'center'
            }}
          >
            <div style={{ fontWeight: 700 }}>{hovered.name}</div>
            <div>{hovered.value} ({hovered.percent}%)</div>
          </div>
        ) : (
          <div
            style={{
              position: 'absolute',
              left: '50%',
              top: '50%',
              transform: 'translate(-50%, -50%)',
              textAlign: 'center',
              pointerEvents: 'none'
            }}
          >
            <div style={{ fontSize: 20, fontWeight: 800, color: BRAND_COLORS.primary, lineHeight: 1 }}>{total}</div>
            <div style={{ fontSize: 10, color: '#8c8c8c', textTransform: 'uppercase' }}>Total</div>
          </div>
        )}
      </div>
      <div style={{ flex: 1, minWidth: 140, maxHeight: 180, overflowY: 'auto' }}>
        {drawn.length === 0 ? (
          <div style={{ color: '#888', fontSize: 13 }}>No category data available</div>
        ) : drawn.map(slice => (
          <div key={slice.name} style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
            <span style={{ width: 10, height: 10, borderRadius: 3, background: slice.color, flexShrink: 0 }} />
            <Text style={{ flex: 1, fontSize: 12 }}>{slice.name}</Text>
            <Text type="secondary" style={{ fontSize: 12 }}>{slice.value}</Text>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── SLA Status colors and icons ───
const SLA_STATUS_CONFIG = {
  ON_TIME: { color: '#52c41a', tag: 'green', icon: <CheckCircleOutlined />, label: 'On Time' },
  APPROACHING: { color: '#faad14', tag: 'orange', icon: <ExclamationCircleOutlined />, label: 'Approaching' },
  OVERDUE: { color: '#ff4d4f', tag: 'red', icon: <WarningOutlined />, label: 'Overdue' },
  BREACHED: { color: '#cf1322', tag: 'volcano', icon: <WarningOutlined />, label: 'Breached' },
};

function isClassifiedComplaint(item) {
  if (!item) return false;
  const cls = (item.classification || '').toUpperCase();
  const status = (item.status || '').toUpperCase();
  if (cls === 'OTHER' || cls === 'INTAKE') {
    return false;
  }
  if (status === 'OTHER' && cls !== 'COMPLAINT' && cls !== 'DECLINED') {
    return false;
  }
  const cId = item.dbcTicketId || item.variables?.dbcTicketId || item.complaintId || item.generalTicketId || '';
  const fcr = item.fcrStatus === true || item.variables?.fcrStatus === 'VERIFIED';
  return cls === 'COMPLAINT' || cls === 'DECLINED' || status === 'DECLINED' || String(cId).startsWith('DBC-') || String(cId).startsWith('FCR-') || fcr;
}

function buildAnalyticsFilters(filters) {
  const apiFilters = {};
  if (filters.complaintId) apiFilters.complaintId = filters.complaintId;
  if (filters.category) apiFilters.category = filters.category;
  if (filters.branch) apiFilters.branch = filters.branch;
  if (filters.district) apiFilters.district = filters.district;
  if (filters.channel) apiFilters.channel = filters.channel;
  if (filters.status) apiFilters.status = filters.status;
  if (filters.slaStatus) apiFilters.slaStatus = filters.slaStatus;
  if (filters.fcrStatus !== undefined && filters.fcrStatus !== null) apiFilters.fcrStatus = filters.fcrStatus;
  if (filters.dateRange?.length === 2) {
    apiFilters.startDate = filters.dateRange[0].startOf('day').toISOString();
    apiFilters.endDate = filters.dateRange[1].endOf('day').toISOString();
  }
  return apiFilters;
}

function formatOptionalDate(value) {
  return value ? moment(value).format('YYYY-MM-DD') : '-';
}

function toIsoDateTime(value) {
  if (!value || value === '-') {
    return null;
  }
  const parsed = moment(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DDTHH:mm:ss') : null;
}

function mapCriItemToMasterRow(item, idx) {
  return {
    id: item.id,
    key: item.uniqueIdNo || item.id || `CRI-${idx + 1}`,
    sNo: idx + 1,
    uniqueIdNo: item.uniqueIdNo || '-',
    dateOfComplaint: formatOptionalDate(item.dateOfComplaint),
    nameOfComplainant: item.nameOfComplainant || '-',
    accountNo: item.accountNo || '-',
    contactAddress: item.contactAddress || '-',
    customerSegment: item.customerSegment || '-',
    receivedBy: item.receivedBy || '-',
    complaintMadeOn: item.complaintMadeOnChannel || '-',
    complaintMadeOnChannel: item.complaintMadeOnChannel || '-',
    specificChannelName: item.specificChannelName || '-',
    districtDepartment: item.districtDepartment || '-',
    serviceType: item.serviceType || '-',
    detailsOfComplaint: item.detailsOfComplaint || '-',
    complaintClassification: item.complaintClassification || '-',
    supportingEvidence: item.supportingEvidence || '-',
    complainantAcknowledged: item.complainantAcknowledged || '-',
    isComplaintJustified: item.complaintJustified || '-',
    complaintJustified: item.complaintJustified || '-',
    validityReasonJustified: item.validityReasonForJustifiedComplaints || '-',
    validityReasonForJustifiedComplaints: item.validityReasonForJustifiedComplaints || '-',
    natureOfComplaints: item.natureOfComplaints || '-',
    complaintsCategory: item.complaintsCategory || '-',
    caseAssignedTo: item.caseAssignedTo || '-',
    expectedResolutionDate: formatOptionalDate(item.expectedResolutionDate),
    actualResolutionDate: formatOptionalDate(item.actualResolutionDate),
    resolutionTimeWorkingDays: item.resolutionTimeWorkingDays !== null && item.resolutionTimeWorkingDays !== undefined ? (item.resolutionTimeWorkingDays + ' working days') : '-',
    caseForwardedTo: item.caseForwardedTo || '-',
    dateCaseForwarded: formatOptionalDate(item.dateCaseForwarded),
    reasonForForwarding: item.reasonForForwarding || '-',
    caseStatus: normalizeComplaintStatus(item.overallStatus || item.caseStatus),
    escalatedTo: item.escalatedTo || '-',
    dateOfEscalation: formatOptionalDate(item.dateOfEscalation),
    reasonForEscalation: item.reasonForEscalation || '-',
    resolutionPlan: item.resolutionPlan || '-',
    isResolutionOutcomeNotified: item.resolutionOutcomeNotified || '-',
    resolutionOutcomeNotified: item.resolutionOutcomeNotified || '-',
    meansOfNotification: item.meansOfNotification || '-',
    wasComplainantAcknowledgeResolution: item.complainantAcknowledgedResolution || '-',
    complainantAcknowledgedResolution: item.complainantAcknowledgedResolution || '-',
    adviceGiven: item.adviceGivenToComplainant || '-',
    adviceGivenToComplainant: item.adviceGivenToComplainant || '-',
    customerReaction: item.customerReactionToHandlingProcess || '-',
    customerReactionToHandlingProcess: item.customerReactionToHandlingProcess || '-',
    customerLifetimeValue: item.customerLifetimeValue || '-',
    remarkAndSpecialNote: item.remarkAndSpecialNote || '-',
    requiresFollowUp: item.requiresFollowUp || '-',
    latestStatusAndRemark: item.latestStatusAndRemark || '-',
    isManuallyEdited: item.isManuallyEdited || false,
    lastEditedBy: item.lastEditedBy || null
  };
}

function AdminDashboard() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [masterData, setMasterData] = useState([]);
  const [editingKey, setEditingKey] = useState('');
  const [editingRowData, setEditingRowData] = useState({});
  const [savingRow, setSavingRow] = useState(false);

  // Pagination states
  const [masterPage, setMasterPage] = useState(1);
  const [masterPageSize, setMasterPageSize] = useState(10);
  const [drillPage, setDrillPage] = useState(1);
  const [drillPageSize, setDrillPageSize] = useState(10);
  const [slaModalPage, setSlaModalPage] = useState(1);
  const [slaModalPageSize, setSlaModalPageSize] = useState(10);



  // Trend and Reports list
  const [trendData, setTrendData] = useState([]);
  const [reportsData, setReportsData] = useState([]);
  const [trendInterval, setTrendInterval] = useState('daily');

  // Drill down modal state
  const [drillDownVisible, setDrillDownVisible] = useState(false);
  const [drillDownTitle, setDrillDownTitle] = useState('');
  const [drillDownData, setDrillDownData] = useState([]);

  // SLA Governance Config state
  const [isSlaConfigModalOpen, setIsSlaConfigModalOpen] = useState(false);
  const [slaConfigs, setSlaConfigs] = useState([]);
  const [holidays] = useState([]);
  const [editingSlaConfig, setEditingSlaConfig] = useState(null);
  const [newAllowedMins, setNewAllowedMins] = useState('');

  // Advanced Filters
  const [filters, setFilters] = useState({
    complaintId: '',
    category: undefined,
    branch: undefined,
    district: undefined,
    channel: undefined,
    status: undefined,
    complaintClassification: undefined,
    slaStatus: undefined,
    fcrStatus: undefined,
    dateRange: null
  });



  useEffect(() => {
    loadData();
  }, [filters, trendInterval]);

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');

      const apiFilters = buildAnalyticsFilters(filters);

      await ApiService.getAnalyticsStats(apiFilters);

      const trendRes = await ApiService.getAnalyticsTrend(trendInterval, apiFilters);
      setTrendData(trendRes || []);

      const reportsRes = await ApiService.getAnalyticsReports(apiFilters).catch(() => []);
      const allReports = (reportsRes || []).filter(isClassifiedComplaint)
        .filter(item => !filters.complaintId || matchesTicketSearch(item, filters.complaintId));
      setReportsData(allReports);

      // complainant_related_information is the single source of truth for this grid.
      // Never substitute another source, otherwise rows lose the CRI id and cannot be saved.
      let criRes = null;
      let criError = '';
      try {
        criRes = await ApiService.getComplainantRelatedInformation({
        search: filters.complaintId,
        status: filters.status,
        category: filters.category,
        district: filters.district
        });
      } catch (criErr) {
        criError = `Could not load Complainant Related Information: ${criErr.message}`;
        console.error(criErr);
      }

      const criRows = [...(criRes?.content || [])].sort((a, b) =>
        compareTicketNumbersAsc(a.uniqueIdNo, b.uniqueIdNo)
      );
      setMasterData(criRows.map((item, idx) => mapCriItemToMasterRow(item, idx)));

      if (criError) {
        setError(criError);
      }

      await ApiService.getAllSlaMetrics().catch(() => []);

      const auditRes = await ApiService.getAuditLogs(apiFilters).catch(() => []);
      setLogs(auditRes || []);
    } catch (err) {
      setError('Failed to load analytics dashboard data.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const clearFilters = () => {
    setFilters({
      complaintId: '',
      category: undefined,
      branch: undefined,
      district: undefined,
      channel: undefined,
      status: undefined,
      slaStatus: undefined,
      fcrStatus: undefined,
      dateRange: null
    });
  };

  const handleEditInlineRow = (record) => {
    setEditingKey(record.key);
    setEditingRowData({ ...record });
  };

  const handleCancelInlineRow = () => {
    setEditingKey('');
    setEditingRowData({});
  };

  const handleSaveInlineRow = async (key, record) => {
    const rowToSave = editingRowData;
    const targetId = record?.id ?? rowToSave?.id;
    if (!targetId) {
      message.error('Cannot save: this row has no Complainant Related Information record id. Please refresh and try again.');
      return;
    }

    setSavingRow(true);
    try {
      const saved = await ApiService.updateComplainantRelatedInformation(targetId, {
          nameOfComplainant: rowToSave.nameOfComplainant,
          accountNo: rowToSave.accountNo,
          contactAddress: rowToSave.contactAddress,
          customerSegment: rowToSave.customerSegment,
          receivedBy: rowToSave.receivedBy,
          complaintMadeOnChannel: rowToSave.complaintMadeOnChannel || rowToSave.complaintMadeOn,
          specificChannelName: rowToSave.specificChannelName,
          districtDepartment: rowToSave.districtDepartment,
          serviceType: rowToSave.serviceType,
          detailsOfComplaint: rowToSave.detailsOfComplaint,
          complaintClassification: rowToSave.complaintClassification,
          supportingEvidence: rowToSave.supportingEvidence,
          complainantAcknowledged: rowToSave.complainantAcknowledged,
          complaintJustified: rowToSave.complaintJustified || rowToSave.isComplaintJustified,
          validityReasonForJustifiedComplaints: rowToSave.validityReasonForJustifiedComplaints || rowToSave.validityReasonJustified,
          natureOfComplaints: rowToSave.natureOfComplaints,
          complaintsCategory: rowToSave.complaintsCategory,
          caseAssignedTo: rowToSave.caseAssignedTo,
          caseForwardedTo: rowToSave.caseForwardedTo,
          reasonForForwarding: rowToSave.reasonForForwarding,
          caseStatus: rowToSave.caseStatus,
        dateOfComplaint: toIsoDateTime(rowToSave.dateOfComplaint),
        expectedResolutionDate: toIsoDateTime(rowToSave.expectedResolutionDate),
        actualResolutionDate: toIsoDateTime(rowToSave.actualResolutionDate),
        dateCaseForwarded: toIsoDateTime(rowToSave.dateCaseForwarded),
        dateOfEscalation: toIsoDateTime(rowToSave.dateOfEscalation),
          escalatedTo: rowToSave.escalatedTo,
          reasonForEscalation: rowToSave.reasonForEscalation,
          resolutionPlan: rowToSave.resolutionPlan,
          resolutionOutcomeNotified: rowToSave.resolutionOutcomeNotified || rowToSave.isResolutionOutcomeNotified,
          meansOfNotification: rowToSave.meansOfNotification,
          complainantAcknowledgedResolution: rowToSave.complainantAcknowledgedResolution || rowToSave.wasComplainantAcknowledgeResolution,
          adviceGivenToComplainant: rowToSave.adviceGivenToComplainant || rowToSave.adviceGiven,
          customerReactionToHandlingProcess: rowToSave.customerReactionToHandlingProcess || rowToSave.customerReaction,
          customerLifetimeValue: rowToSave.customerLifetimeValue,
          remarkAndSpecialNote: rowToSave.remarkAndSpecialNote,
          requiresFollowUp: rowToSave.requiresFollowUp,
          latestStatusAndRemark: rowToSave.latestStatusAndRemark
        });

      // Render what the database actually stored, not the local edit buffer.
      setMasterData(prev => prev.map(row => (
        row.key === key ? { ...mapCriItemToMasterRow(saved, row.sNo - 1), sNo: row.sNo, key: row.key } : row
      )));
    setEditingKey('');
    setEditingRowData({});
      message.success('Complainant Related Information saved successfully.');
    } catch (err) {
      console.error('CRI update failed:', err);
      message.error(`Save failed: ${err.message}. No changes were stored.`);
    } finally {
      setSavingRow(false);
    }
  };

  const handleDeleteInlineRow = async (key, record) => {
    const targetId = record?.id;
    if (!targetId) {
      message.error('Cannot delete: this row has no Complainant Related Information record id. Please refresh and try again.');
      return;
    }

      try {
        await ApiService.deleteComplainantRelatedInformation(targetId);
      } catch (err) {
      console.error('CRI delete failed:', err);
      message.error(`Delete failed: ${err.message}. The record was not removed.`);
      return;
      }

    setMasterData(prev => prev.filter(row => row.key !== key));
    if (editingKey === key) {
      setEditingKey('');
      setEditingRowData({});
    }
    message.success('Complainant Related Information record deleted.');
  };

  const handleFieldChange = (dataIndex, value) => {
    setEditingRowData(prev => ({
      ...prev,
      [dataIndex]: value
    }));
  };

  const renderDataCell = (record, dataIndex, type = 'text', options = []) => {
    const isEditing = record.key === editingKey && type !== 'readonly';
    if (isEditing) {
      const val = editingRowData[dataIndex] !== undefined ? editingRowData[dataIndex] : record[dataIndex];
      return renderCriEditingControl(type, val, dataIndex, options, handleFieldChange);
    }
    return renderCriReadOnlyCell(record, dataIndex, record[dataIndex]);
  };

  const criExportColumns = [
    { header: 'S.No', accessor: (_, idx) => idx + 1 },
    { header: 'Unique ID No.', key: 'uniqueIdNo' },
    { header: 'Date of the Complaint', key: 'dateOfComplaint', type: 'date' },
    { header: 'Name of the Complainant', key: 'nameOfComplainant' },
    { header: 'Account No.', key: 'accountNo' },
    { header: 'Contact Address', key: 'contactAddress' },
    { header: 'Customer Segment', key: 'customerSegment' },
    { header: 'Received By', key: 'receivedBy' },
    { header: 'Complaint Made on', key: 'complaintMadeOn' },
    { header: 'Specific Channel Name', key: 'specificChannelName' },
    { header: 'District/Department', key: 'districtDepartment' },
    { header: 'Service Type', key: 'serviceType' },
    { header: 'Details of the Complaint', key: 'detailsOfComplaint' },
    { header: 'Complaint Classification', key: 'complaintClassification' },
    { header: 'Supporting Evidence', key: 'supportingEvidence' },
    { header: 'Did the Complainant Acknowledged?', key: 'complainantAcknowledged', type: 'boolean' },
    { header: 'Is the Complaint Justified', key: 'isComplaintJustified', type: 'boolean' },
    { header: 'Validity Reason For Justified Complaints', key: 'validityReasonJustified' },
    { header: 'Nature of Complaints', key: 'natureOfComplaints' },
    { header: 'Complaints Category', key: 'complaintsCategory' },
    { header: 'Case Assigned To', key: 'caseAssignedTo' },
    { header: 'Expected Resolution Date', key: 'expectedResolutionDate', type: 'date' },
    { header: 'Actual Resolution Date', key: 'actualResolutionDate', type: 'date' },
    { header: 'Resolution Time In Working-Days', key: 'resolutionTimeWorkingDays' },
    { header: 'Case Forwarded to', key: 'caseForwardedTo' },
    { header: 'Date the Case has Forwarded', key: 'dateCaseForwarded', type: 'date' },
    { header: 'Reason for Forwarding', key: 'reasonForForwarding' },
    { header: 'Case Status', key: 'caseStatus', type: 'status' },
    { header: 'Escalated to', key: 'escalatedTo' },
    { header: 'Date of Escalation', key: 'dateOfEscalation', type: 'date' },
    { header: 'Reason for Escalation', key: 'reasonForEscalation' },
    { header: 'Resolution Plan', key: 'resolutionPlan' },
    { header: 'Is Resolution Outcome Notified?', key: 'isResolutionOutcomeNotified', type: 'boolean' },
    { header: 'Means of Notification', key: 'meansOfNotification' },
    { header: 'Was Complainant Acknowledge Resolution?', key: 'wasComplainantAcknowledgeResolution', type: 'boolean' },
    { header: "Advice Given to the Complainant who didn't accept the resolution", key: 'adviceGiven' },
    { header: "Customer's Reaction to the Complaints Handling Process", key: 'customerReaction' },
    { header: 'Customer Lifetime Value', key: 'customerLifetimeValue' },
    { header: 'Remark and Special Note', key: 'remarkAndSpecialNote' },
    { header: 'Requires Follow Up', key: 'requiresFollowUp', type: 'boolean' },
    { header: 'Latest Status and Remark', key: 'latestStatusAndRemark' }
  ];

  const masterColumns = [
    { title: 'S.No', dataIndex: 'sNo', width: 60, fixed: 'left', render: (_, __, idx) => idx + 1 },
    {
      title: 'Actions',
      key: 'actions',
      width: 140,
      fixed: 'left',
      render: (_, record) => {
        const isEditingRow = record.key === editingKey;
        return isEditingRow ? (
          <Space size="middle">
            <Button
              type="link"
              size="small"
              loading={savingRow}
              style={{ color: '#2563eb', fontWeight: 700, padding: 0, fontSize: '13px' }}
              onClick={() => handleSaveInlineRow(record.key, record)}
            >
              Save
            </Button>
            <Button
              type="text"
              danger
              size="small"
              style={{ fontWeight: 500, padding: 0, fontSize: '13px' }}
              onClick={handleCancelInlineRow}
            >
              Cancel
            </Button>
          </Space>
        ) : (
          <Space size="middle">
            <Tooltip title="Edit Record">
              <EditOutlined
                className="grid-action-icon"
                style={{ color: '#2563eb', fontSize: '16px' }}
                onClick={() => handleEditInlineRow(record)}
              />
            </Tooltip>
            <Popconfirm
              title="Delete Record"
              description="Are you sure you want to delete this report record?"
              onConfirm={() => handleDeleteInlineRow(record.key, record)}
              okText="Yes"
              cancelText="No"
            >
              <Tooltip title="Delete Record">
                <DeleteOutlined className="grid-action-icon" style={{ color: '#dc2626', fontSize: '16px' }} />
              </Tooltip>
            </Popconfirm>
          </Space>
        );
      }
    },
    { title: 'Unique ID No.', dataIndex: 'uniqueIdNo', width: 160, fixed: 'left', render: v => <span style={{ fontFamily: 'monospace', fontWeight: 700, color: BRAND_COLORS.primary }}>{v}</span> },
    { title: 'Date of the Complaint', dataIndex: 'dateOfComplaint', width: 140, render: (_, r) => renderDataCell(r, 'dateOfComplaint', 'date') },
    { title: 'Name of the Complainant', dataIndex: 'nameOfComplainant', width: 180, render: (_, r) => renderDataCell(r, 'nameOfComplainant', 'text') },
    { title: 'Account No.', dataIndex: 'accountNo', width: 150, render: (_, r) => renderDataCell(r, 'accountNo', 'text') },
    { title: 'Contact Address', dataIndex: 'contactAddress', width: 180, render: (_, r) => renderDataCell(r, 'contactAddress', 'text') },
    { title: 'Customer Segment', dataIndex: 'customerSegment', width: 140, render: (_, r) => renderDataCell(r, 'customerSegment', 'text') },
    { title: 'Received By', dataIndex: 'receivedBy', width: 180, render: (_, r) => renderDataCell(r, 'receivedBy', 'select', RECEIVED_BY_OPTIONS) },
    { title: 'Complaint Made on', dataIndex: 'complaintMadeOn', width: 180, render: (_, r) => renderDataCell(r, 'complaintMadeOn', 'select', CHANNEL_OPTIONS) },
    { title: 'Specific Channel Name', dataIndex: 'specificChannelName', width: 160, render: (_, r) => renderDataCell(r, 'specificChannelName', 'text') },
    { title: 'District/Department', dataIndex: 'districtDepartment', width: 180, render: (_, r) => renderDataCell(r, 'districtDepartment', 'select', DISTRICT_DEPARTMENT_OPTIONS) },
    { title: 'Service Type', dataIndex: 'serviceType', width: 160, render: (_, r) => renderDataCell(r, 'serviceType', 'select', SERVICE_TYPE_OPTIONS) },
    { title: 'Details of the Complaint', dataIndex: 'detailsOfComplaint', width: 220, render: (_, r) => renderDataCell(r, 'detailsOfComplaint', 'text') },
    { title: 'Complaint Classification', dataIndex: 'complaintClassification', width: 170, render: (_, r) => renderDataCell(r, 'complaintClassification', 'select', CLASSIFICATION_OPTIONS) },
    { title: 'Supporting Evidence', dataIndex: 'supportingEvidence', width: 150, render: (_, r) => renderDataCell(r, 'supportingEvidence', 'text') },
    { title: 'Did the Complainant Acknowledged?', dataIndex: 'complainantAcknowledged', width: 180, render: (_, r) => renderDataCell(r, 'complainantAcknowledged', 'select', YES_NO_OPTIONS) },
    { title: 'Is the Complaint Justified', dataIndex: 'isComplaintJustified', width: 160, render: (_, r) => renderDataCell(r, 'isComplaintJustified', 'select', YES_NO_OPTIONS) },
    { title: 'Validity Reason For Justified Complaints', dataIndex: 'validityReasonJustified', width: 220, render: (_, r) => renderDataCell(r, 'validityReasonJustified', 'text') },
    { title: 'Nature of Complaints', dataIndex: 'natureOfComplaints', width: 180, render: (_, r) => renderDataCell(r, 'natureOfComplaints', 'text') },
    { title: 'Complaints Category', dataIndex: 'complaintsCategory', width: 190, render: (_, r) => renderDataCell(r, 'complaintsCategory', 'select', CATEGORY_OPTIONS) },
    { title: 'Case Assigned To', dataIndex: 'caseAssignedTo', width: 160, render: (_, r) => renderDataCell(r, 'caseAssignedTo', 'text') },
    { title: 'Expected Resolution Date', dataIndex: 'expectedResolutionDate', width: 160, render: (_, r) => renderDataCell(r, 'expectedResolutionDate', 'date') },
    { title: 'Actual Resolution Date', dataIndex: 'actualResolutionDate', width: 160, render: (_, r) => renderDataCell(r, 'actualResolutionDate', 'date') },
    { title: 'Resolution Time In Working-Days', dataIndex: 'resolutionTimeWorkingDays', width: 180, render: (_, r) => renderDataCell(r, 'resolutionTimeWorkingDays', 'readonly') },
    { title: 'Case Forwarded to', dataIndex: 'caseForwardedTo', width: 160, render: (_, r) => renderDataCell(r, 'caseForwardedTo', 'text') },
    { title: 'Date the Case has Forwarded', dataIndex: 'dateCaseForwarded', width: 160, render: (_, r) => renderDataCell(r, 'dateCaseForwarded', 'date') },
    { title: 'Reason for Forwarding', dataIndex: 'reasonForForwarding', width: 190, render: (_, r) => renderDataCell(r, 'reasonForForwarding', 'select', FORWARDING_REASON_OPTIONS) },
    { title: 'Case Status', dataIndex: 'caseStatus', width: 140, render: (_, r) => renderDataCell(r, 'caseStatus', 'select', Array.from(new Set(Object.values(COMPLAINT_STATUS)))) },
    { title: 'Escalated to', dataIndex: 'escalatedTo', width: 160, render: (_, r) => renderDataCell(r, 'escalatedTo', 'text') },
    { title: 'Date of Escalation', dataIndex: 'dateOfEscalation', width: 150, render: (_, r) => renderDataCell(r, 'dateOfEscalation', 'date') },
    { title: 'Reason for Escalation', dataIndex: 'reasonForEscalation', width: 190, render: (_, r) => renderDataCell(r, 'reasonForEscalation', 'text') },
    { title: 'Resolution Plan', dataIndex: 'resolutionPlan', width: 200, render: (_, r) => renderDataCell(r, 'resolutionPlan', 'select', RESOLUTION_PLAN_OPTIONS) },
    { title: 'Is Resolution Outcome Notified?', dataIndex: 'isResolutionOutcomeNotified', width: 180, render: (_, r) => renderDataCell(r, 'isResolutionOutcomeNotified', 'select', YES_NO_OPTIONS) },
    { title: 'Means of Notification', dataIndex: 'meansOfNotification', width: 160, render: (_, r) => renderDataCell(r, 'meansOfNotification', 'select', NOTIFICATION_MEANS_OPTIONS) },
    { title: 'Was Complainant Acknowledge Resolution?', dataIndex: 'wasComplainantAcknowledgeResolution', width: 220, render: (_, r) => renderDataCell(r, 'wasComplainantAcknowledgeResolution', 'select', YES_NO_OPTIONS) },
    { title: "Advice Given to the Complainant who didn't accept the resolution", dataIndex: 'adviceGiven', width: 240, render: (_, r) => renderDataCell(r, 'adviceGiven', 'text') },
    { title: "Customer's Reaction to the Complaints Handling Process", dataIndex: 'customerReaction', width: 220, render: (_, r) => renderDataCell(r, 'customerReaction', 'select', REACTION_OPTIONS) },
    { title: 'Customer Lifetime Value', dataIndex: 'customerLifetimeValue', width: 160, render: (_, r) => renderDataCell(r, 'customerLifetimeValue', 'select', LIFETIME_VALUE_OPTIONS) },
    { title: 'Remark and Special Note', dataIndex: 'remarkAndSpecialNote', width: 200, render: (_, r) => renderDataCell(r, 'remarkAndSpecialNote', 'text') },
    { title: 'Requires Follow Up', dataIndex: 'requiresFollowUp', width: 150, render: (_, r) => renderDataCell(r, 'requiresFollowUp', 'select', YES_NO_OPTIONS) },
    { title: 'Latest Status and Remark', dataIndex: 'latestStatusAndRemark', width: 220, render: (_, r) => renderDataCell(r, 'latestStatusAndRemark', 'text') }
  ];

  // Drill down logic
  const handleDrillDown = (dimension, value) => {
    let filtered = [];
    let title = '';
    if (dimension === 'category') {
      filtered = reportsData.filter(m => m.complaintCategory === value);
      title = `Complaints - Category: ${value.toUpperCase()}`;
    } else if (dimension === 'branch') {
      filtered = reportsData.filter(m => m.branch === value);
      title = `Complaints - Branch: ${value}`;
    } else if (dimension === 'district') {
      filtered = reportsData.filter(m => m.district === value);
      title = `Complaints - District: ${value}`;
    } else if (dimension === 'channel') {
      filtered = reportsData.filter(m => m.channel === value);
      title = `Complaints - Channel: ${value}`;
    } else if (dimension === 'chart') {
      filtered = reportsData.filter(m => {
        let periodStr = '';
        if (trendInterval === 'daily') periodStr = moment(m.createdAt).format('YYYY-MM-DD');
        else if (trendInterval === 'weekly') periodStr = moment(m.createdAt).format('YYYY-[W]ww');
        else if (trendInterval === 'monthly') periodStr = moment(m.createdAt).format('YYYY-MM');
        else periodStr = moment(m.createdAt).format('YYYY');
        return periodStr === value;
      });
      title = `Complaints - Period: ${value}`;
    }
    setDrillDownData(filtered);
    setDrillDownTitle(title);
    setDrillDownVisible(true);
  };





  // ─── Process logs to group by complaintId ───
  const groupedLogs = Object.values(logs.reduce((acc, log) => {
    const id = log.complaintId || 'unknown';
    if (!acc[id]) {
      acc[id] = {
        key: id,
        complaintId: id,
        processInstanceId: log.processInstanceId,
        latestAction: log.action,
        latestDate: log.createdAt,
        customerName: log.customerName || '',
        category: log.complaintCategory || '',
        description: log.complaintDescription || '',
        history: []
      };
    }
    acc[id].history.push(log);
    if (!acc[id].customerName && log.customerName) acc[id].customerName = log.customerName;
    if (!acc[id].category && log.complaintCategory) acc[id].category = log.complaintCategory;
    if (!acc[id].description && log.complaintDescription) acc[id].description = log.complaintDescription;
    if (moment(log.createdAt).isAfter(acc[id].latestDate)) {
      acc[id].latestAction = log.action;
      acc[id].latestDate = log.createdAt;
    }
    return acc;
  }, {}));

  groupedLogs.sort((a, b) => moment(b.latestDate).diff(moment(a.latestDate)));

  const getPriorityTagColor = (p) => {
    const priority = (p || '').toUpperCase();
    if (priority.includes('HIGH') || priority.includes('CRITICAL')) return 'red';
    if (priority.includes('SENSITIVE')) return 'purple';
    if (priority.includes('MEDIUM') || priority.includes('NORMAL')) return 'blue';
    return 'default';
  };

  // Render SVG Trend line chart
  const renderTrendChart = () => {
    if (trendData.length === 0) {
      return <div style={{ height: '160px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#888' }}>No data points available for trend</div>;
    }
    const maxVal = Math.max(...trendData.map(d => d.count), 5);
    const height = 150;
    const width = 800;
    const padding = 20;

    const points = trendData.map((d, i) => {
      const x = padding + (i * (width - 2 * padding)) / Math.max(trendData.length - 1, 1);
      const y = height - padding - (d.count * (height - 2 * padding)) / maxVal;
      return { x, y, label: d.period, val: d.count };
    });

    const dPath = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');

    return (
      <svg width="100%" height={height + 8} viewBox={`0 0 ${width} ${height}`} style={{ background: '#fcfcfc', border: '1px solid #f0f0f0', borderRadius: '8px' }}>
        {[0, 0.25, 0.5, 0.75, 1].map((r, idx) => {
          const y = padding + r * (height - 2 * padding);
          return (
            <line key={idx} x1={padding} y1={y} x2={width - padding} y2={y} stroke="#f0f0f0" strokeDasharray="3" />
          );
        })}
        <path d={dPath} fill="none" stroke={BRAND_COLORS.primary} strokeWidth="2.5" />
        {points.map((p, idx) => (
          <g key={`pt-${p.x}-${p.y}`} style={{ cursor: 'pointer' }} onClick={() => handleDrillDown('chart', p.label)}>
            <circle cx={p.x} cy={p.y} r="5" fill="#fff" stroke={BRAND_COLORS.primary} strokeWidth="2.5" />
            <title>{`Period: ${p.label}\nComplaints: ${p.val}`}</title>
            {(idx === 0 || idx === Math.floor(points.length / 2) || idx === points.length - 1) && (
              <text x={p.x} y={height - 4} fontSize="9px" textAnchor="middle" fill="#888">{p.label}</text>
            )}
          </g>
        ))}
      </svg>
    );
  };

  const categorySlices = Object.entries(
    masterData.reduce((acc, row) => {
      if (!hasDisplayCategory(row.complaintsCategory)) {
        return acc;
      }
      acc[row.complaintsCategory] = (acc[row.complaintsCategory] || 0) + 1;
      return acc;
    }, {})
  )
    .sort((a, b) => b[1] - a[1])
    .map(([name, value], index) => ({
      name,
      value,
      color: CATEGORY_PIE_COLORS[index % CATEGORY_PIE_COLORS.length]
    }));

  return (
    <DashboardLayout userRole="admin">
      <style>{printStyles}</style>
      <div style={{ padding: '12px 0', maxWidth: '100%', margin: '0' }} id="print-section">

        {/* Header Title */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <Title level={2} style={{ margin: 0, color: BRAND_COLORS.primary }}>
            Complaint Analytics & Reports
          </Title>
        </div>

        {error && <Alert message={error} type="error" showIcon style={{ marginBottom: 16 }} />}

        <Row gutter={[16, 16]} style={{ marginBottom: '24px' }} align="stretch">
          <Col xs={24} xl={14}>
            <Card
              title={<span style={{ fontWeight: 600, color: BRAND_COLORS.primary }}><LineChartOutlined /> Complaint Volume Trend Analysis</span>}
              style={{ borderRadius: '8px', height: '100%' }}
            >
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '16px' }}>
            <Select value={trendInterval} onChange={setTrendInterval} style={{ width: 120 }}>
              <Option value="daily">Daily</Option>
              <Option value="weekly">Weekly</Option>
              <Option value="monthly">Monthly</Option>
              <Option value="yearly">Yearly</Option>
            </Select>
          </div>
          {renderTrendChart()}
        </Card>
          </Col>
          <Col xs={24} xl={10}>
            <Card
              title={<span style={{ fontWeight: 600, color: BRAND_COLORS.primary }}><PieChartOutlined /> Complaints by Category</span>}
              style={{ borderRadius: '8px', height: '100%' }}
              loading={loading}
            >
              <CategoryPieChart slices={categorySlices} />
            </Card>
          </Col>
        </Row>

        {/* Advanced Filters */}
        <div style={{ marginBottom: '24px', padding: '12px 16px', background: 'transparent' }}>
          <Space wrap size="large">
            <div>
              <div style={{ marginBottom: 4, fontSize: '12px', color: '#666' }}>Unique ID No</div>
              <Input placeholder="Search Unique ID No or CM ticket" value={filters.complaintId} onChange={e => handleFilterChange('complaintId', e.target.value)} allowClear style={{ width: 200 }} />
            </div>
            <div>
              <div style={{ marginBottom: 4, fontSize: '12px', color: '#666' }}>Complaint Classification</div>
              <Select placeholder="All Classifications" value={filters.complaintClassification} onChange={val => handleFilterChange('complaintClassification', val)} allowClear style={{ width: 170 }}>
                <Option value="General">General</Option>
                <Option value="Sensitive">Sensitive</Option>
                <Option value="Highly Sensitive">Highly Sensitive</Option>
              </Select>
            </div>
            <div>
              <div style={{ marginBottom: 4, fontSize: '12px', color: '#666' }}>Date Range</div>
              <RangePicker value={filters.dateRange} onChange={val => handleFilterChange('dateRange', val)} />
            </div>
            <div style={{ marginTop: '22px' }}>
              <Button type="link" icon={<SyncOutlined />} onClick={clearFilters}>Reset Filters</Button>
            </div>
          </Space>
        </div>

        {/* Master Standardized Editable Complaint Ledger */}
        <Card
          title={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 700, color: BRAND_COLORS.primary, fontSize: '16px' }}>
                Complainant Related Information
              </span>
              <Space>
                <ExportDropdown
                  getDataset={() => ({
                    filename: `Master_Complaint_Ledger_${moment().format('YYYYMMDD_HHmmss')}`,
                    documentTitle: 'Complainant Related Information',
                    subtitle: `Exported ${moment().format('DD/MM/YYYY HH')}`,
                    tables: [{
                      title: 'Complaint Ledger',
                      sheetName: 'CRI Ledger',
                      columns: criExportColumns,
                      rows: masterData
                    }]
                  })}
                />
              </Space>
            </div>
          }
          bodyStyle={{ padding: '0' }}
          bordered={false}
          style={{ borderRadius: '12px', background: '#ffffff', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', overflow: 'hidden' }}
        >
          <Table
            className="data-management-grid"
            columns={masterColumns}
            dataSource={masterData.slice((masterPage - 1) * masterPageSize, masterPage * masterPageSize)}
            rowKey="key"
            loading={loading}
            rowClassName={(record) => (record.key === editingKey ? 'editing-row-highlight' : 'standard-table-row')}
            pagination={false}
            scroll={{ x: 5800, y: 550 }}
            bordered={false}
            size="middle"
          />
          <Pagination
            currentPage={masterPage}
            pageSize={masterPageSize}
            totalRecords={masterData.length}
            onPageChange={(page) => setMasterPage(page)}
            onPageSizeChange={(size) => { setMasterPageSize(size); setMasterPage(1); }}
            itemUnit="complaints"
          />
        </Card>
      </div>

      {/* Drill-down Modal */}
      <Modal
        title={<div style={{ color: BRAND_COLORS.primary, fontWeight: 600, fontSize: '18px' }}>{drillDownTitle}</div>}
        visible={drillDownVisible}
        onCancel={() => setDrillDownVisible(false)}
        footer={[
          <Button key="close" type="primary" onClick={() => setDrillDownVisible(false)}>
            Close
          </Button>
        ]}
        width="100%"
        style={{ maxWidth: 1200 }}
        bodyStyle={{ padding: '20px 24px' }}
      >
        <Table
          columns={[
            {
              title: 'Unique ID No',
              key: 'complaintId',
              width: 220,
              render: (_, r) => (
                <div>
                  <strong style={{ color: BRAND_COLORS.primary, fontFamily: 'monospace', fontSize: '12px' }}>{formatUniqueId(r)}</strong>
                  {formatIntakeId(r) && formatIntakeId(r) !== formatUniqueId(r) && (
                    <div style={{ fontSize: '10px', color: '#64748b' }}>Intake {formatIntakeId(r)}</div>
                  )}
                </div>
              )
            },
            {
              title: 'Customer Name',
              dataIndex: 'customerName',
              width: 180,
              render: name => name || <span style={{ color: '#aaa' }}>-</span>
            },
            {
              title: 'Category',
              dataIndex: 'complaintCategory',
              width: 180,
              render: c => c ? <Tag color="blue">{c.toUpperCase()}</Tag> : <span style={{ color: '#aaa' }}>-</span>
            },
            {
              title: 'Overall Status',
              dataIndex: 'status',
              width: 140,
              render: (_, r) => renderComplaintStatusTag(r)
            },
            {
              title: 'SLA Status',
              dataIndex: 'slaStatus',
              width: 140,
              render: sla => {
                const config = SLA_STATUS_CONFIG[sla] || { tag: 'default', label: sla || '-' };
                return <Tag color={config.tag}>{config.label}</Tag>;
              }
            }
          ]}
          dataSource={drillDownData.slice((drillPage - 1) * drillPageSize, drillPage * drillPageSize)}
          rowKey="complaintId"
          pagination={false}
          scroll={{ x: 'max-content' }}
        />
        <Pagination
          currentPage={drillPage}
          pageSize={drillPageSize}
          totalRecords={drillDownData.length}
          onPageChange={(page) => setDrillPage(page)}
          onPageSizeChange={(size) => { setDrillPageSize(size); setDrillPage(1); }}
          itemUnit="complaints"
        />
      </Modal>

      {/* SLA Governance Configuration Modal */}
      <Modal
        title={<div style={{ color: BRAND_COLORS.primary, fontWeight: 600, fontSize: '18px' }}><SettingOutlined /> Dashen Bank Centralized SLA Configuration & Governance</div>}
        open={isSlaConfigModalOpen}
        onCancel={() => setIsSlaConfigModalOpen(false)}
        width="100%"
        style={{ maxWidth: 1000 }}
        footer={[
          <Button key="reset" danger onClick={async () => {
            if (window.confirm('Reset all SLA configurations to Dashen Bank defaults?')) {
              try {
                await ApiService.resetSlaConfigs();
                message.success('SLA Configurations reset to Dashen Bank defaults.');
                const cfgs = await ApiService.getSlaConfigs();
                setSlaConfigs(cfgs);
              } catch (e) {
                console.error(e);
                message.error('Failed to reset SLA configs.');
              }
            }
          }}>
            Reset Defaults
          </Button>,
          <Button key="close" type="primary" onClick={() => setIsSlaConfigModalOpen(false)}>
            Close
          </Button>
        ]}
      >
        <Tabs defaultActiveKey="matrix">
          <Tabs.TabPane tab="Stage & Priority SLA Matrix" key="matrix">
            <Table
              dataSource={slaConfigs.slice((slaModalPage - 1) * slaModalPageSize, slaModalPage * slaModalPageSize)}
              rowKey="id"
              pagination={false}
              columns={[
                { title: 'Configuration Name', dataIndex: 'displayName', render: t => <strong style={{ color: BRAND_COLORS.primary }}>{t}</strong> },
                { title: 'Group', dataIndex: 'configGroup', render: g => <Tag color="blue">{g}</Tag> },
                { title: 'Priority', dataIndex: 'priority', render: p => <Tag color={getPriorityTagColor(p)}>{p}</Tag> },
                { title: 'Allowed Business Mins', dataIndex: 'allowedMinutes', render: m => <strong>{m} mins</strong> },
                {
                  title: 'Equivalent Time',
                  render: (_, r) => {
                    const mins = r.allowedMinutes || 0;
                    if (mins < 60) return `${mins} mins`;
                    if (mins < 480) return `${(mins / 60).toFixed(1)} Hours`;
                    return `${(mins / 480).toFixed(1)} Business Days (8h/day)`;
                  }
                },
                {
                  title: 'Action',
                  render: (_, r) => (
                    <Button size="small" type="primary" onClick={() => {
                      setEditingSlaConfig(r);
                      setNewAllowedMins(r.allowedMinutes);
                    }}>
                      Edit SLA
                    </Button>
                  )
                }
              ]}
            />
            <Pagination
              currentPage={slaModalPage}
              pageSize={slaModalPageSize}
              totalRecords={slaConfigs.length}
              onPageChange={(page) => setSlaModalPage(page)}
              onPageSizeChange={(size) => { setSlaModalPageSize(size); setSlaModalPage(1); }}
              itemUnit="governance rules"
            />
          </Tabs.TabPane>

          <Tabs.TabPane tab="SLA Alerts" key="alerts">
            <Card title="SLA Breach Alert Thresholds & Notification Rules" style={{ marginBottom: '16px' }}>
              <Table
                dataSource={[
                  { level: '80% Reminder (Approaching SLA)', threshold: '80% Business Time Elapsed', action: 'Automated notification & Dashboard SLA Warning indicator to assigned officer.' },
                  { level: '100% Breach Alert', threshold: '100% Business Time Elapsed', action: 'Automated breach alert dispatched to assigned Work Unit.' }
                ]}
                rowKey="level"
                pagination={false}
                columns={[
                  { title: 'Alert Level', dataIndex: 'level', render: l => <span style={{ fontWeight: 600, color: '#1e293b', fontSize: '13px' }}>{l}</span> },
                  { title: 'Trigger Threshold', dataIndex: 'threshold', render: t => <Tag color="default" style={{ fontWeight: 600, fontSize: '11px' }}>{t}</Tag> },
                  { title: 'System Notification Action', dataIndex: 'action', render: a => <span style={{ color: '#475569', fontSize: '12px', lineHeight: 1.4 }}>{a}</span> }
                ]}
              />
            </Card>
          </Tabs.TabPane>

          <Tabs.TabPane tab="Business Working Hours & Holidays" key="business_hours">
            <Card title="Dashen Bank Standard Working Hours Schedule" style={{ marginBottom: '16px' }}>
              <ul style={{ paddingLeft: '20px', lineHeight: 1.8 }}>
                <li><strong>Monday – Thursday:</strong> 08:00 AM – 12:00 PM & 01:00 PM – 05:00 PM (8 Hours / Day)</li>
                <li><strong>Friday:</strong> 08:00 AM – 11:30 AM & 01:00 PM – 05:00 PM (7.5 Hours / Day)</li>
                <li><strong>Saturday:</strong> 08:00 AM – 12:00 PM (4 Hours / Day)</li>
                <li><strong>Sunday & Holidays:</strong> Excluded (SLA timers pause automatically)</li>
              </ul>
            </Card>

            <Card title="Public & Bank Holidays Calendar">
              <Table
                dataSource={holidays}
                rowKey="id"
                pagination={false}
                columns={[
                  { title: 'Holiday Name', dataIndex: 'holidayName', render: n => <strong>{n}</strong> },
                  { title: 'Date', dataIndex: 'holidayDate', render: d => <Tag color="volcano">{d}</Tag> },
                  { title: 'Type', dataIndex: 'holidayType', render: t => <Tag color="purple">{t}</Tag> }
                ]}
              />
            </Card>
          </Tabs.TabPane>
        </Tabs>
      </Modal>

      {/* Edit Single SLA Modal */}
      <Modal
        title={`Edit SLA: ${editingSlaConfig?.displayName || ''}`}
        open={!!editingSlaConfig}
        onCancel={() => setEditingSlaConfig(null)}
        onOk={async () => {
          if (!editingSlaConfig) return;
          try {
            await ApiService.updateSlaConfig(editingSlaConfig.id, { allowedMinutes: Number.parseInt(newAllowedMins, 10) });
            message.success('SLA Configuration updated successfully.');
            setEditingSlaConfig(null);
            const cfgs = await ApiService.getSlaConfigs();
            setSlaConfigs(cfgs);
          } catch (e) {
            console.error(e);
            message.error('Failed to update SLA config.');
          }
        }}
      >
        {editingSlaConfig && (
          <div>
            <p><strong>Config Key:</strong> {editingSlaConfig.configKey}</p>
            <p><strong>Complaint Classification:</strong> {editingSlaConfig.priority}</p>
            <div style={{ marginTop: '16px' }}>
              <label htmlFor="allowed-mins-input">Allowed Business Minutes:</label>
              <Input
                id="allowed-mins-input"
                type="number"
                value={newAllowedMins}
                onChange={e => setNewAllowedMins(e.target.value)}
                style={{ marginTop: '8px' }}
              />
              <Text type="secondary" style={{ fontSize: '12px', marginTop: '4px', display: 'block' }}>
                Note: 240 mins = 4 Hours, 480 mins = 1 Business Day, 1440 mins = 3 Business Days.
              </Text>
            </div>
          </div>
        )}
      </Modal>
    </DashboardLayout>
  );
}

const printStyles = `
  @media print {
    body * {
      visibility: hidden !important;
    }
    #print-section, #print-section * {
      visibility: visible !important;
    }
    #print-section {
      position: absolute !important;
      left: 0 !important;
      top: 0 !important;
      width: 100% !important;
      margin: 0 !important;
      padding: 0 !important;
    }
    .ant-btn, .ant-select, .ant-picker-range, .ant-input, button, .ant-tabs-nav {
      display: none !important;
    }
  }
`;

export default AdminDashboard;
