import React, { useState, useEffect } from 'react';
import { Card, Typography, Table, Tag, Select, Input, Space, Button, Alert, Row, Col, Tabs, Modal, Form, message, Spin, Tooltip, Popconfirm } from 'antd';
import { PrinterOutlined, EditOutlined, SearchOutlined, DeleteOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import ExportDropdown from '../components/ExportDropdown';
import Pagination from '../components/Pagination';
import { formatUniqueId, matchesTicketSearch, compareTicketNumbersAsc } from '../components/TaskTable';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import moment from 'moment';

const { Title, Text } = Typography;
const { Option } = Select;

const NBE_STATUS_COLORS = {
  RESOLVED: 'green',
  CLOSED: 'default',
  ESCALATED: 'orange',
  ON_TRACK: 'blue',
  DECLINED: 'red',
  RECORDED: 'purple'
};

function resolveAnnex1Status(row) {
  const stored = row?.reportStatus;
  if (stored && stored !== '-' && String(stored).trim() !== '') {
    return String(stored).trim();
  }
  const current = row?.overallStatus || row?.status;
  if (current && current !== '-' && String(current).trim() !== '') {
    return String(current).trim();
  }
  return '';
}

function printCellValue(col, row, index) {
  if (col.key === 'index') {
    return index + 1;
  }
  if (col.key === 'lodgedDate' || col.key === 'resolvedDate') {
    return row[col.dataIndex] ? moment(row[col.dataIndex]).format('YYYY-MM-DD') : '-';
  }
  if (col.key === 'complaintId') {
    return formatUniqueId(row);
  }
  if (col.key === 'reportStatus') {
    return resolveAnnex1Status(row) || '-';
  }
  if (col.key === 'daysOpen') {
    const days = row.daysOpen;
    if (days === 0 || days === '0') {
      return '0 Days';
    }
    if (days === null || days === undefined || days === '' || days === '-') {
      return '-';
    }
    return `${days} Days`;
  }
  return row[col.dataIndex] || '-';
}

function parseDaysOpenValue(value) {
  if (value === null || value === undefined || value === '' || value === '-') {
    return null;
  }
  const parsed = Number.parseInt(String(value).replace(/days?/i, '').trim(), 10);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : null;
}

// nbe_compliance_reports is keyed by ticket number so that stored report values
// survive completion of the underlying process instance.
function resolveTicketNumber(record) {
  const candidate = record?.dbcTicketId || record?.complaintId;
  return candidate && candidate !== 'N/A' ? String(candidate) : '';
}

const NBE_EDITABLE_FIELDS = new Set([
  'staffHandling',
  'reasonForNonResolution',
  'additionalComments',
  'reportStatus',
  'daysOpen'
]);

function nbeStatusColor(statusValue) {
  const val = String(statusValue || '').toUpperCase();
  if (NBE_STATUS_COLORS[val]) {
    return NBE_STATUS_COLORS[val];
  }
  if (val.includes('NBE')) {
    return 'magenta';
  }
  return 'gold';
}

function phoneOnly(value) {
  return String(value || '')
    .split(/[,;/]/)
    .map(part => part.trim())
    .filter(part => part && part !== '-' && part !== 'N/A' && !part.includes('@'))
    .join(', ');
}

function NBEReports() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [reportsData, setReportsData] = useState([]);
  const [editingKey, setEditingKey] = useState('');
  const [editingRowData, setEditingRowData] = useState({});
  const [savingRow, setSavingRow] = useState(false);

  // Filters
  const [annex1Month, setAnnex1Month] = useState('ALL');
  const [annex1Year, setAnnex1Year] = useState(moment().format('YYYY'));
  const [searchText, setSearchText] = useState('');

  // Pagination states
  const [annex1Page, setAnnex1Page] = useState(1);
  const [annex1PageSize, setAnnex1PageSize] = useState(10);
  const [annex2Page, setAnnex2Page] = useState(1);
  const [annex2PageSize, setAnnex2PageSize] = useState(10);

  // Editing comments modal
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingRecord] = useState(null);
  const [form] = Form.useForm();
  const [savingComments, setSavingComments] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await ApiService.getNbeReportsData();
      const classifiedOnly = (data || []).filter(r => {
        if (!r) return false;
        const cls = (r.classification || '').toUpperCase();
        const status = (r.status || r.overallStatus || '').toUpperCase();
        if (cls === 'OTHER' || cls === 'INTAKE') {
          return false;
        }
        if (status === 'OTHER' && cls !== 'COMPLAINT' && cls !== 'DECLINED') {
          return false;
        }
        const cId = String(r.dbcTicketId || r.complaintId || '');
        return cls === 'COMPLAINT' || cls === 'DECLINED' || status === 'DECLINED'
          || cId.startsWith('DBC-') || cId.startsWith('FCR-');
      }).sort((a, b) => compareTicketNumbersAsc(
        a.dbcTicketId || a.complaintId,
        b.dbcTicketId || b.complaintId
      ));
      setReportsData(classifiedOnly);
    } catch (err) {
      setError('Failed to fetch NBE report data.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // --- Filtering for Annex 1 ---
  const filteredAnnex1 = reportsData.filter(row => {
    // Classified complaints stay in Annex 1 even without a lodged date.
    // Date filters apply only when a lodgement timestamp exists.
    let dateMatch = true;
    if (row.lodgedDate) {
      const lodged = moment(row.lodgedDate);
      const monthMatch = annex1Month === 'ALL' || lodged.format('MM') === annex1Month;
      const yearMatch = lodged.format('YYYY') === annex1Year;
      dateMatch = monthMatch && yearMatch;
    } else if (annex1Month !== 'ALL') {
      dateMatch = false;
    }

    const searchLower = searchText.toLowerCase();
    const searchMatch = !searchText ||
      matchesTicketSearch(row, searchLower) ||
      (row.complainantName || '').toLowerCase().includes(searchLower);

    return dateMatch && searchMatch;
  }).sort((a, b) => compareTicketNumbersAsc(a.dbcTicketId || a.complaintId, b.dbcTicketId || b.complaintId));

  // --- Filtering for Annex 2 ---
  // Only include unresolved, open for more than 10 business days, SLA = OVERDUE or BREACHED
  const filteredAnnex2 = reportsData.filter(row => {
    const isUnresolved = !row.resolvedDate;
    const isOverdue = row.slaStatus === 'OVERDUE' || row.slaStatus === 'BREACHED' || row.slaStatus === 'RESOLVED_AFTER_SLA' || row.breached === true;
    const isLongStanding = row.daysOpen >= 10;

    const searchLower = searchText.toLowerCase();
    const searchMatch = !searchText ||
      matchesTicketSearch(row, searchLower) ||
      (row.complainantName || '').toLowerCase().includes(searchLower);

    return isUnresolved && isOverdue && isLongStanding && searchMatch;
  }).sort((a, b) => compareTicketNumbersAsc(a.dbcTicketId || a.complaintId, b.dbcTicketId || b.complaintId));

  // --- Handle Edit Comments ---
  const handleSaveComments = async () => {
    const ticketNumber = resolveTicketNumber(editingRecord);
    if (!ticketNumber) {
      message.error('Cannot save: this record has no ticket number.');
      return;
    }

    try {
      setSavingComments(true);
      const values = await form.validateFields();

      await ApiService.saveNbeComplianceReport({
        ticketNumber,
        processInstanceId: editingRecord?.processInstanceId || null,
        reasonForNonResolution: values.reasonForNonResolution,
        additionalComments: values.additionalComments
      });

      message.success('NBE report comments saved.');
      setIsEditModalOpen(false);
      await fetchData();
    } catch (err) {
      message.error(err.message || 'Failed to save NBE comments.');
    } finally {
      setSavingComments(false);
    }
  };

  const annex1ExportColumns = [
    { header: 'No.', accessor: (_, i) => i + 1 },
    { header: 'Date Complaint Lodged', key: 'lodgedDate', type: 'date' },
    { header: 'Unique ID No', accessor: (row) => formatUniqueId(row) },
    { header: 'Complainant Name', key: 'complainantName' },
    { header: 'Mobile', accessor: (row) => phoneOnly(row.mobile) },
    { header: 'Email', key: 'email' },
    { header: 'Issues Raised', key: 'issuesRaised' },
    { header: 'Status', accessor: (row) => resolveAnnex1Status(row), type: 'label' },
    { header: 'No. of Days Issue Takes', accessor: (row) => (row.daysOpen === 0 || row.daysOpen ? `${row.daysOpen} Days` : '-') },
    { header: 'Name of Staff Handling', key: 'staffHandling' }
  ];

  const annex2ExportColumns = [
    { header: 'No.', accessor: (_, i) => i + 1 },
    { header: 'Date Complaint Lodged', key: 'lodgedDate', type: 'date' },
    { header: 'Complainant Name', key: 'complainantName' },
    { header: 'Unique ID No', accessor: (row) => formatUniqueId(row) },
    { header: 'Staff Handling Complaint', key: 'staffHandling' },
    { header: 'Complaint Details', key: 'issuesRaised' },
    { header: 'Number of Days Open', accessor: (row) => (row.daysOpen === 0 || row.daysOpen ? `${row.daysOpen} Days` : '-') },
    { header: 'Reason for Non-Resolution', key: 'reasonForNonResolution' },
    { header: 'Additional Comments', key: 'additionalComments' }
  ];

  // --- Print helpers ---
  const buildPrintTable = (columns, data) => {
    let html = '<table style="border-collapse: collapse; width: 100%; font-size: 11px; margin-top: 15px; margin-bottom: 30px;">';

    // Header row
    html += '<thead><tr style="background-color: #f2f2f2;">';
    columns.forEach(col => {
      if (col.key !== 'actions') {
        html += `<th style="border: 1px solid #000; padding: 6px; text-align: left;">${col.title}</th>`;
      }
    });
    html += '</tr></thead>';

    // Body rows
    html += '<tbody>';
    if (data.length === 0) {
      const colSpan = columns.filter(c => c.key !== 'actions').length;
      html += `<tr><td colspan="${colSpan}" style="border: 1px solid #000; padding: 12px; text-align: center; color: #888; font-style: italic;">No complaints found for the selected period.</td></tr>`;
    } else {
      data.forEach((row, i) => {
        html += '<tr>';
        columns.forEach(col => {
          if (col.key !== 'actions') {
            html += `<td style="border: 1px solid #000; padding: 6px; text-align: left;">${printCellValue(col, row, i)}</td>`;
          }
        });
        html += '</tr>';
      });
    }
    html += '</tbody>';
    html += '</table>';
    return html;
  };

  // --- Print/PDF function ---
  const handlePrint = (annexType) => {
    let titleHtml = '';
    let tableHtml = '';

    if (annexType === 'annex1') {
      const monthLabel = annex1Month === 'ALL' ? 'All Months (One Year)' : moment(`${annex1Year}-${annex1Month}-01`).format('MMMM');
      titleHtml = `
        <div style="text-align: center; margin-bottom: 24px;">
          <h2 style="margin: 0 0 5px 0; font-size: 18px; font-weight: bold; color: #012169;">Annex 1 – Complaints Handling Report</h2>
          <h3 style="margin: 0 0 5px 0; font-size: 16px; font-weight: bold;">Dashen Bank S.C.</h3>
          <h4 style="margin: 0; font-size: 14px; font-weight: 500;">
            For: <span style="border-bottom: 1px solid #000; padding: 0 12px; font-weight: bold;">${monthLabel}, ${annex1Year}</span>
          </h4>
        </div>
      `;
      tableHtml = buildPrintTable(columnsAnnex1, filteredAnnex1);
    } else {
      titleHtml = `
        <div style="text-align: center; margin-bottom: 24px;">
          <h2 style="margin: 0 0 5px 0; font-size: 18px; font-weight: bold; color: #012169;">Annex 2 – Notification Letter to NBE</h2>
          <h3 style="margin: 0 0 5px 0; font-size: 15px; font-weight: bold; color: #555;">For Complaints Not Resolved Within 10 Business Days</h3>
          <h4 style="margin: 0; font-size: 13px; font-weight: 400;">Reporting Entity: Dashen Bank S.C.</h4>
        </div>
      `;
      tableHtml = buildPrintTable(columnsAnnex2, filteredAnnex2);
    }

    const printHtml = `
      <html>
        <head>
          <title>NBE Compliance Report</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 20px; color: #000; }
            table { border-collapse: collapse; width: 100%; margin-top: 15px; margin-bottom: 30px; font-size: 11px; }
            th, td { border: 1px solid #000; padding: 6px; text-align: left; }
            th { background-color: #f2f2f2; font-weight: bold; }
            h1, h2, h3 { text-align: center; margin: 5px 0; color: #012169; }
            .footer-section { margin-top: 50px; font-size: 12px; page-break-inside: avoid; }
            .footer-row { display: flex; justify-content: space-between; margin-top: 40px; }
            .footer-col { width: 30%; border-top: 1px solid #000; text-align: center; padding-top: 5px; }
          </style>
        </head>
        <body>
          ${titleHtml}
          ${tableHtml}
        </body>
      </html>
    `;

    const printWindow = window.open('', '_blank');
    printWindow.document.documentElement.innerHTML = printHtml;
    printWindow.focus();
    setTimeout(() => {
      printWindow.print();
      printWindow.close();
    }, 250);
  };

  const getRecordKey = (record) => record.processInstanceId || record.complaintId || record.id || record.dbcTicketId;

  const handleEditInlineRow = (record) => {
    setEditingKey(getRecordKey(record));
    setEditingRowData({
      ...record,
      reportStatus: resolveAnnex1Status(record)
    });
  };

  const handleCancelInlineRow = () => {
    setEditingKey('');
    setEditingRowData({});
  };

  const handleSaveInlineRow = async (record) => {
    const ticketNumber = resolveTicketNumber(record);
    if (!ticketNumber) {
      message.error('Cannot save: this row has no ticket number to key the NBE compliance report on.');
      return;
    }

    setSavingRow(true);
    try {
      const saved = await ApiService.saveNbeComplianceReport({
        ticketNumber,
        processInstanceId: record.processInstanceId || null,
        staffHandling: editingRowData.staffHandling ?? '',
        reportStatus: editingRowData.reportStatus ?? '',
        daysOpen: parseDaysOpenValue(editingRowData.daysOpen),
        reasonForNonResolution: editingRowData.reasonForNonResolution ?? '',
        additionalComments: editingRowData.additionalComments ?? ''
      });

      setEditingKey('');
      setEditingRowData({});
      message.success(`NBE compliance report saved for ${saved.ticketNumber}.`);
      // Reload from nbe_compliance_reports so the grid shows the stored values.
      await fetchData();
    } catch (err) {
      console.error('NBE compliance report save failed:', err);
      message.error(`Save failed: ${err.message}. No changes were stored.`);
    } finally {
      setSavingRow(false);
    }
  };

  const handleDeleteInlineRow = async (record) => {
    const ticketNumber = resolveTicketNumber(record);
    if (!ticketNumber) {
      message.error('Cannot clear: this row has no ticket number.');
      return;
    }

    try {
      await ApiService.deleteNbeComplianceReport(ticketNumber);
    } catch (err) {
      console.error('NBE compliance report delete failed:', err);
      message.error(`Could not clear stored values: ${err.message}`);
      return;
    }

    if (editingKey === getRecordKey(record)) {
      setEditingKey('');
      setEditingRowData({});
    }
    message.success(`Stored NBE report values cleared for ${ticketNumber}.`);
    await fetchData();
  };

  const handleFieldChange = (dataIndex, value) => {
    setEditingRowData(prev => ({
      ...prev,
      [dataIndex]: value
    }));
  };

  const renderNbeEditCell = (record, dataIndex, type, options) => {
    const val = editingRowData[dataIndex] !== undefined ? editingRowData[dataIndex] : record[dataIndex];
    if (type === 'select') {
      return (
        <Select
          value={val}
          onChange={(v) => handleFieldChange(dataIndex, v)}
          size="small"
          style={{ width: '100%', minWidth: '120px' }}
        >
          {options.map(opt => (
            <Option key={opt} value={opt}>{opt}</Option>
          ))}
        </Select>
      );
    }
    if (type === 'number') {
      const numeric = val === '-' || val === undefined || val === null ? '' : val;
      return (
        <Input
          type="number"
          min={0}
          value={numeric}
          placeholder="Days"
          onChange={(e) => handleFieldChange(dataIndex, e.target.value)}
          size="small"
          style={{ width: '100%', minWidth: '90px' }}
        />
      );
    }
    return (
      <Input
        value={val === '-' ? '' : val}
        placeholder="-"
        onChange={(e) => handleFieldChange(dataIndex, e.target.value || '-')}
        size="small"
        style={{ width: '100%', minWidth: '110px' }}
      />
    );
  };

  const renderNbeDisplayCell = (record, dataIndex) => {
    const cellValue = record[dataIndex];
    if (dataIndex === 'daysOpen') {
      if (cellValue === 0 || cellValue === '0') {
        return '0 Days';
      }
      if (cellValue === null || cellValue === undefined || cellValue === '' || cellValue === '-') {
        return <span style={{ color: '#94a3b8' }}>-</span>;
      }
      return `${cellValue} Days`;
    }
    if (!cellValue || cellValue === '-') {
      return <span style={{ color: '#94a3b8' }}>-</span>;
    }
    if (dataIndex === 'status' || dataIndex === 'overallStatus') {
      const overall = record.overallStatus || cellValue;
      return <Tag color={nbeStatusColor(overall)}>{overall}</Tag>;
    }
    if (dataIndex === 'reportStatus') {
      const displayed = resolveAnnex1Status(record);
      return displayed
        ? <Tag color={nbeStatusColor(displayed)}>{displayed}</Tag>
        : <span style={{ color: '#94a3b8' }}>-</span>;
    }
    if (dataIndex === 'lodgedDate') {
      return cellValue ? moment(cellValue).format('DD/MM/YY') : '-';
    }
    return <span style={{ color: '#1e293b', fontSize: '13px' }}>{cellValue}</span>;
  };

  const renderNbeDataCell = (record, dataIndex, type = 'text', options = []) => {
    const isEditing = getRecordKey(record) === editingKey && NBE_EDITABLE_FIELDS.has(dataIndex);
    if (isEditing) {
      return renderNbeEditCell(record, dataIndex, type, options);
    }
    return renderNbeDisplayCell(record, dataIndex);
  };

  const renderActionsColumn = {
    title: 'Actions',
    key: 'actions',
    width: 140,
    fixed: 'left',
    render: (_, record) => {
      const key = getRecordKey(record);
      const isEditingRow = key === editingKey;
      return isEditingRow ? (
        <Space size="middle">
          <Button
            type="link"
            size="small"
            loading={savingRow}
            style={{ color: '#2563eb', fontWeight: 700, padding: 0, fontSize: '13px' }}
            onClick={() => handleSaveInlineRow(record)}
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
            title="Clear Stored Report Values"
            description="Remove the saved NBE report values for this ticket and fall back to system-derived data? The complaint itself is not affected."
            onConfirm={() => handleDeleteInlineRow(record)}
            okText="Yes"
            cancelText="No"
          >
            <Tooltip title="Clear Stored Report Values">
              <DeleteOutlined className="grid-action-icon" style={{ color: '#dc2626', fontSize: '16px' }} />
            </Tooltip>
          </Popconfirm>
        </Space>
      );
    }
  };

  const columnsAnnex1 = [
    { title: 'No.', dataIndex: 'index', key: 'index', width: 50, fixed: 'left', render: (_, __, i) => i + 1 },
    renderActionsColumn,
    { title: 'Date Complaint Lodged', dataIndex: 'lodgedDate', key: 'lodgedDate', render: (_, r) => renderNbeDataCell(r, 'lodgedDate') },
    { title: 'Unique ID No', dataIndex: 'complaintId', key: 'complaintId', render: (val, record) => <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>{formatUniqueId(record || val)}</span> },
    { title: 'Complainant Name', dataIndex: 'complainantName', key: 'complainantName', render: (_, r) => renderNbeDataCell(r, 'complainantName') },
    { title: 'Mobile', dataIndex: 'mobile', key: 'mobile', render: (_, r) => renderNbeDataCell({ ...r, mobile: phoneOnly(r.mobile) }, 'mobile') },
    { title: 'Email', dataIndex: 'email', key: 'email', render: (_, r) => renderNbeDataCell(r, 'email') },
    { title: 'Issues Raised', dataIndex: 'issuesRaised', key: 'issuesRaised', ellipsis: true, render: (_, r) => renderNbeDataCell(r, 'issuesRaised') },
    { title: 'Status', dataIndex: 'reportStatus', key: 'reportStatus', render: (_, r) => renderNbeDataCell(r, 'reportStatus', 'select', ['', 'RECORDED', 'ESCALATED', 'ON_TRACK', 'RESOLVED', 'CLOSED', 'DECLINED', 'Referred to NBE']) },
    { title: 'No. of Days Issue Takes', dataIndex: 'daysOpen', key: 'daysOpen', render: (_, r) => renderNbeDataCell(r, 'daysOpen', 'number') },
    { title: 'Name of Staff Handling', dataIndex: 'staffHandling', key: 'staffHandling', render: (_, r) => renderNbeDataCell(r, 'staffHandling') }
  ];

  const columnsAnnex2 = [
    { title: 'No.', dataIndex: 'index', key: 'index', width: 50, fixed: 'left', render: (_, __, i) => i + 1 },
    renderActionsColumn,
    { title: 'Date Complaint Lodged', dataIndex: 'lodgedDate', key: 'lodgedDate', render: (_, r) => renderNbeDataCell(r, 'lodgedDate') },
    { title: 'Complainant Name', dataIndex: 'complainantName', key: 'complainantName', render: (_, r) => renderNbeDataCell(r, 'complainantName') },
    { title: 'Unique ID No', dataIndex: 'complaintId', key: 'complaintId', render: (val, record) => <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>{formatUniqueId(record || val)}</span> },
    { title: 'Staff Handling Complaint', dataIndex: 'staffHandling', key: 'staffHandling', render: (_, r) => renderNbeDataCell(r, 'staffHandling') },
    { title: 'Complaint Details', dataIndex: 'issuesRaised', key: 'issuesRaised', ellipsis: true, render: (_, r) => renderNbeDataCell(r, 'issuesRaised') },
    { title: 'Number of Days Open', dataIndex: 'daysOpen', key: 'daysOpen', render: (_, r) => renderNbeDataCell(r, 'daysOpen', 'number') },
    { title: 'Reason for Non-Resolution', dataIndex: 'reasonForNonResolution', key: 'reasonForNonResolution', render: (_, r) => renderNbeDataCell(r, 'reasonForNonResolution') },
    { title: 'Additional Comments', dataIndex: 'additionalComments', key: 'additionalComments', render: (_, r) => renderNbeDataCell(r, 'additionalComments') }
  ];

  return (
    <DashboardLayout userRole="admin">
      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '12px 0' }}>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <Title level={2} style={{ margin: 0, color: BRAND_COLORS.primary }}>NBE Compliance Reports</Title>
            <Text type="secondary">National Bank of Ethiopia compliance reporting dashboard</Text>
          </div>
        </div>

        {error && <Alert message={error} type="error" showIcon style={{ marginBottom: 16 }} />}

        {/* Global Toolbar Filters */}
        <Card style={{ marginBottom: '24px', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.02)' }}>
          <Row gutter={[16, 16]} align="middle">
            <Col xs={24} sm={8}>
              <Text type="secondary" style={{ display: 'block', marginBottom: '4px' }}>Search Report</Text>
              <Input
                className="pill-search-input"
                placeholder="Search Unique ID No, CM ticket or Complainant..."
                prefix={<SearchOutlined style={{ color: '#475569', fontSize: '16px' }} />}
                value={searchText}
                onChange={e => setSearchText(e.target.value)}
                allowClear
              />
            </Col>

            <Col xs={12} sm={6}>
              <Text type="secondary" style={{ display: 'block', marginBottom: '4px' }}>Reporting Month</Text>
              <Select value={annex1Month} onChange={setAnnex1Month} style={{ width: '100%' }}>
                <Option value="ALL">All Months (One Year)</Option>
                {moment.monthsShort().map((m, index) => {
                  const val = String(index + 1).padStart(2, '0');
                  return <Option key={val} value={val}>{m}</Option>;
                })}
              </Select>
            </Col>

            <Col xs={12} sm={6}>
              <Text type="secondary" style={{ display: 'block', marginBottom: '4px' }}>Reporting Year</Text>
              <Select value={annex1Year} onChange={setAnnex1Year} style={{ width: '100%' }}>
                {['2024', '2025', '2026', '2027'].map(y => (
                  <Option key={y} value={y}>{y}</Option>
                ))}
              </Select>
            </Col>
          </Row>
        </Card>

        {/* Tabs for Annex 1 and Annex 2 */}
        <Tabs type="card" defaultActiveKey="annex1" style={{ background: '#fff', padding: '16px', borderRadius: '8px', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>

          {/* ANNEX 1 TAB */}
          <Tabs.TabPane tab="Annex 1 - Monthly Report" key="annex1">
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginBottom: '16px' }}>
              <ExportDropdown
                getDataset={() => {
                  const periodLabel = annex1Month === 'ALL'
                    ? `Full Year ${annex1Year}`
                    : moment(`${annex1Year}-${annex1Month}-01`).format('MMMM, YYYY');
                  return {
                    filename: `NBE_Annex1_${annex1Year}_${annex1Month}_${moment().format('YYYYMMDD')}`,
                    documentTitle: 'Annex 1 – Complaints Handling Report',
                    subtitle: `Dashen Bank S.C.  |  ${periodLabel}`,
                    tables: [{
                      title: 'Annex 1',
                      sheetName: 'Annex 1',
                      columns: annex1ExportColumns,
                      rows: filteredAnnex1
                    }]
                  };
                }}
              />
              <Button
                type="primary"
                icon={<PrinterOutlined />}
                onClick={() => handlePrint('annex1')}
              >
                Print / Save to PDF
              </Button>
            </div>

            {loading ? (
              <div style={{ textAlign: 'center', padding: '50px 0' }}><Spin size="large" /></div>
            ) : (
              <div id="nbe-annex1-report" style={{ padding: '10px', backgroundColor: '#fff' }}>
                <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                  <h2 style={{ margin: '0 0 5px 0', fontSize: '18px', fontWeight: 'bold', color: '#012169' }}>Annex 1 – Complaints Handling Report</h2>
                  <h3 style={{ margin: '0 0 5px 0', fontSize: '16px', fontWeight: 'bold' }}>Dashen Bank S.C.</h3>
                  <h4 style={{ margin: '0', fontSize: '14px', fontWeight: 500 }}>
                    For: <span style={{ borderBottom: '1px solid #000', padding: '0 12px', fontWeight: 'bold' }}>
                      {annex1Month === 'ALL' ? `Full Year ${annex1Year}` : moment(`${annex1Year}-${annex1Month}-01`).format('MMMM, YYYY')}
                    </span>
                  </h4>
                </div>

                <Table
                  id="nbe-annex1-table"
                  className="data-management-grid"
                  columns={columnsAnnex1}
                  dataSource={filteredAnnex1.slice((annex1Page - 1) * annex1PageSize, annex1Page * annex1PageSize)}
                  rowKey={(record) => getRecordKey(record)}
                  rowClassName={(record) => (getRecordKey(record) === editingKey ? 'editing-row-highlight' : 'standard-table-row')}
                  pagination={false}
                  scroll={{ x: 'max-content' }}
                />
                <Pagination
                  currentPage={annex1Page}
                  pageSize={annex1PageSize}
                  totalRecords={filteredAnnex1.length}
                  onPageChange={(page) => setAnnex1Page(page)}
                  onPageSizeChange={(size) => { setAnnex1PageSize(size); setAnnex1Page(1); }}
                  itemUnit="records"
                />
              </div>
            )}
          </Tabs.TabPane>

          {/* ANNEX 2 TAB */}
          <Tabs.TabPane tab="Annex 2 - Unresolved Escalations" key="annex2">
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginBottom: '16px' }}>
              <ExportDropdown
                getDataset={() => ({
                  filename: `NBE_Annex2_Escalations_${moment().format('YYYYMMDD')}`,
                  documentTitle: 'Annex 2 – Notification Letter to NBE',
                  subtitle: 'Complaints Not Resolved Within 10 Business Days  |  Dashen Bank S.C.',
                  tables: [{
                    title: 'Annex 2',
                    sheetName: 'Annex 2',
                    columns: annex2ExportColumns,
                    rows: filteredAnnex2
                  }]
                })}
              />
              <Button
                type="primary"
                icon={<PrinterOutlined />}
                onClick={() => handlePrint('annex2')}
              >
                Print / Save to PDF
              </Button>
            </div>

            {loading ? (
              <div style={{ textAlign: 'center', padding: '50px 0' }}><Spin size="large" /></div>
            ) : (
              <div id="nbe-annex2-report" style={{ padding: '10px', backgroundColor: '#fff' }}>
                <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                  <h2 style={{ margin: '0 0 5px 0', fontSize: '18px', fontWeight: 'bold', color: '#012169' }}>Annex 2 – Notification Letter to NBE</h2>
                  <h3 style={{ margin: '0 0 5px 0', fontSize: '15px', fontWeight: 'bold', color: '#555' }}>For Complaints Not Resolved Within 10 Business Days</h3>
                  <h4 style={{ margin: '0', fontSize: '13px', fontWeight: '400' }}>Reporting Entity: Dashen Bank S.C.</h4>
                </div>

                <Table
                  id="nbe-annex2-table"
                  className="data-management-grid"
                  columns={columnsAnnex2}
                  dataSource={filteredAnnex2.slice((annex2Page - 1) * annex2PageSize, annex2Page * annex2PageSize)}
                  rowKey={(record) => getRecordKey(record)}
                  rowClassName={(record) => (getRecordKey(record) === editingKey ? 'editing-row-highlight' : 'standard-table-row')}
                  pagination={false}
                  scroll={{ x: 'max-content' }}
                />
                <Pagination
                  currentPage={annex2Page}
                  pageSize={annex2PageSize}
                  totalRecords={filteredAnnex2.length}
                  onPageChange={(page) => setAnnex2Page(page)}
                  onPageSizeChange={(size) => { setAnnex2PageSize(size); setAnnex2Page(1); }}
                  itemUnit="records"
                />
              </div>
            )}
          </Tabs.TabPane>
        </Tabs>

        {/* Modal for editing Reason for Non-Resolution & Comments */}
        <Modal
          title={<span style={{ color: BRAND_COLORS.primary, fontWeight: 'bold' }}>Edit NBE Comments</span>}
          open={isEditModalOpen}
          onOk={handleSaveComments}
          onCancel={() => setIsEditModalOpen(false)}
          okText="Save Comments"
          confirmLoading={savingComments}
          destroyOnClose
        >
          <Form form={form} layout="vertical" style={{ marginTop: '16px' }}>
            <Form.Item
              name="reasonForNonResolution"
              label="Reason for Non-Resolution"
              rules={[{ required: true, message: 'Please provide the reason for non-resolution.' }]}
            >
              <Input.TextArea rows={4} placeholder="Describe the reason why this complaint could not be resolved within 10 business days..." />
            </Form.Item>
            <Form.Item
              name="additionalComments"
              label="Additional Comments / Action Plan"
            >
              <Input.TextArea rows={3} placeholder="Provide any additional comments, mitigations, or next steps..." />
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </DashboardLayout>
  );
}

export default NBEReports;
