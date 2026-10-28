import React, { useState, useEffect } from 'react';
import { Card, Typography, Button, Form, Input, Select, Alert, Row, Col, Space, Checkbox, Table, Tag, Modal, Tooltip, Tabs, DatePicker, message as antMessage } from 'antd';
import { PlusCircleOutlined, PaperClipOutlined, EyeOutlined, AudioOutlined, SearchOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import { formatUniqueId, formatIntakeId, UniqueIdDisplay, compareTicketNumbersAsc, matchesTicketSearch } from '../components/TaskTable';
import Pagination from '../components/Pagination';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import { useAuth } from '../contexts/AuthContext';
import { renderComplaintStatusTag } from '../utils/statusUtils';
import { isResolvedAtFcr, classifyComplaintFilter } from '../utils/slaMetrics';
import { collectAttachments, isAudioAttachment } from '../utils/attachments';
import moment from 'moment';
import ExportDropdown from '../components/ExportDropdown';
import ScreenedCaseDetailModal from '../components/ScreenedCaseDetailModal';
import {
  getCustomerNameError,
  getDescriptionError,
  getAccountNumberError,
  getPlaceNameError,
  getEmailError,
  getStaffPhoneError,
  fieldRule
} from '../utils/complaintFieldValidation';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const PRIMARY = BRAND_COLORS.primary;
const EVIDENCE_FILE_ACCEPT = '.pdf,.jpg,.jpeg,.png,.docx,.xlsx';
const VOICE_FILE_ACCEPT = '.mp3,.wav,.m4a';

function registeredCaseText(value) {
  const text = value == null ? '' : String(value).trim();
  if (!text || text.toLowerCase() === 'null') {
    return 'N/A';
  }
  return text;
}

function firstRegisteredValue(...values) {
  for (const value of values) {
    const text = value == null ? '' : String(value).trim();
    if (text && text.toLowerCase() !== 'null') {
      return text;
    }
  }
  return '';
}

function complaintRecordDate(record) {
  return record?.createdAt || record?.complaintMadeOn || record?.date || record?.variables?.createdAt || null;
}

function matchesIntakeSearch(record, query, range) {
  const text = String(query || '').trim().toLowerCase();
  if (text) {
    const name = String(record?.customerName || record?.variables?.customerName || '').toLowerCase();
    if (!name.includes(text) && !matchesTicketSearch(record, text)) {
      return false;
    }
  }
  if (range?.length !== 2 || !range[0] || !range[1]) {
    return true;
  }
  const raw = complaintRecordDate(record);
  if (!raw) {
    return false;
  }
  const created = moment(raw);
  if (!created.isValid()) {
    return false;
  }
  const start = moment(range[0].toDate ? range[0].toDate() : range[0]).startOf('day');
  const end = moment(range[1].toDate ? range[1].toDate() : range[1]).endOf('day');
  return !created.isBefore(start) && !created.isAfter(end);
}

function registeredCaseKey(record) {
  return firstRegisteredValue(
    record?.processInstanceId,
    record?.complaintId,
    record?.dbcTicketId,
    record?.generalTicketId,
    record?.uniqueIdNo
  );
}

function mergeRegisteredCases(primaryRows, extraRows) {
  const seen = new Set();
  const merged = [];
  for (const row of [...primaryRows, ...extraRows]) {
    const key = registeredCaseKey(row);
    if (key) {
      if (seen.has(key)) {
        continue;
      }
      seen.add(key);
    }
    merged.push(row);
  }
  return merged;
}

function taskAsRegisteredCase(task) {
  const vars = task?.variables || {};
  const complaint = vars.complaint || {};
  const customer = vars.customer || {};
  return {
    ...task,
    customerName: task?.customerName || customer.name || '',
    complaintCategory: task?.complaintCategory || complaint.category || vars.complaintCategory || '',
    status: task?.status || task?.overallStatus || 'RECORDED',
    classification: task?.classification || vars.classification || 'INTAKE',
    createdAt: task?.createdAt || null
  };
}

function isOtherCase(record) {
  const classification = String(record?.classification || '').toUpperCase();
  const status = String(record?.status || '').toUpperCase();
  return classification === 'OTHER' || status === 'OTHER';
}

function screenedCaseTitle(record) {
  const type = String(record?.otherType || record?.variables?.otherType || '').toUpperCase();
  if (type === 'FEEDBACK') return 'Feedback';
  if (type === 'ETHICS' || type.includes('ETHICS')) return 'Ethics and Conduct';
  return 'Dispute';
}

function screenedCaseRecord(record) {
  if (!record) return null;
  const customer = record.variables?.customer || {};
  const complaint = record.variables?.complaint || {};
  return {
    ...record,
    customerName: firstRegisteredValue(record.customerName, customer.name) || '-',
    currentContactPhone: firstRegisteredValue(
      record.currentContactPhone,
      customer.currentContactPhone,
      customer.phone,
      record.phone
    ) || '-',
    accountNumber: firstRegisteredValue(
      record.accountNumber,
      customer.accountNumber,
      complaint.accountNumber
    ) || '-',
    complaintDescription: firstRegisteredValue(
      record.complaintDescription,
      complaint.description,
      record.description
    ) || '-',
    createdAt: complaintRecordDate(record)
  };
}

const pageStackStyle = { width: '100%', maxWidth: '1400px', margin: '0 auto' };
const headerRowStyle = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'flex-start',
  flexWrap: 'wrap',
  gap: '12px'
};
const pageTitleStyle = { margin: 0, color: PRIMARY, fontWeight: 700 };
const primaryButtonStyle = { backgroundColor: PRIMARY, borderColor: PRIMARY };
const overviewLabelStyle = { fontSize: '14px', color: PRIMARY, display: 'block', marginBottom: '12px' };
const searchBarStyle = { display: 'flex', flexWrap: 'wrap', gap: '12px', alignItems: 'center', marginTop: '16px' };
const searchInputStyle = { width: 360, maxWidth: '100%' };
const metricTitleStyle = { margin: '4px 0 0 0', color: PRIMARY };
const fcrMetricTitleStyle = { margin: '4px 0 0 0', color: '#16a34a' };
const cardRoundStyle = { borderRadius: '8px' };
const tabsCardBodyStyle = { padding: '8px 16px' };
const modalTitleStyle = { color: PRIMARY, fontWeight: 700 };
const detailModalTitleStyle = { color: PRIMARY, fontWeight: 700, fontSize: '16px' };
const sectionHeadingStyle = {
  color: PRIMARY,
  fontSize: '17px',
  fontWeight: 700,
  marginBottom: '20px',
  paddingBottom: '8px',
  borderBottom: '2px solid #f0f0f0'
};
const complaintSectionHeadingStyle = { ...sectionHeadingStyle, marginTop: '12px' };
const fieldLabelStyle = { fontWeight: 600, color: '#444', fontSize: '14px' };
const customerProfileBoxStyle = {
  background: '#f8fafc',
  border: '1px solid #e2e8f0',
  borderRadius: '8px',
  padding: '16px',
  marginBottom: '24px'
};
const customerProfileHeadingStyle = { ...fieldLabelStyle, display: 'block', marginBottom: '12px' };
const controlStyle = { height: '42px', borderRadius: '4px', fontSize: '15px', border: '1px solid #dcdcdc' };
const selectControlStyle = { height: '42px', borderRadius: '4px', fontSize: '15px' };
const datePickerStyle = {
  width: '100%',
  height: '42px',
  borderRadius: '4px',
  fontSize: '15px',
  border: '1px solid #dcdcdc'
};
const textareaStyle = { borderRadius: '4px', fontSize: '15px', border: '1px solid #dcdcdc' };
const uploadBoxStyle = {
  background: '#f8fafc',
  border: '1px solid #e2e8f0',
  borderRadius: '6px',
  padding: '16px 20px',
  marginBottom: '16px'
};
const uploadBoxLastStyle = { ...uploadBoxStyle, marginBottom: '20px' };
const uploadTitleStyle = { fontWeight: 600, color: PRIMARY, marginBottom: '10px', fontSize: '14px' };
const hiddenFileInputStyle = { display: 'none' };
const roundedButtonStyle = { borderRadius: '4px' };
const voicePreviewWrapStyle = { marginTop: '12px' };
const voicePreviewLabelStyle = { fontSize: '12px', display: 'block', marginBottom: '6px' };
const attachLabelStyle = { fontSize: '12px', display: 'block', marginBottom: '6px' };
const audioPreviewStyle = { width: '100%', height: '38px' };
const submitButtonStyle = { width: '100%', backgroundColor: PRIMARY };
const detailBoxStyle = {
  border: '1px solid #ddd',
  borderRadius: '6px',
  padding: '20px',
  marginTop: '10px',
  backgroundColor: '#f9f9f9'
};
const detailHeadingStyle = { color: PRIMARY, marginTop: 0, marginBottom: '16px' };
const ticketIdStyle = { color: PRIMARY, fontSize: '14px', fontFamily: 'monospace' };
const intakeIdStyle = { fontSize: '12px', color: '#64748b', marginTop: '2px' };
const mutedBlockLabelStyle = { fontSize: '12px', display: 'block' };
const sectionDividerStyle = { marginTop: '16px', paddingTop: '12px', borderTop: '1px solid #eee' };
const descriptionTextStyle = { fontStyle: 'italic', color: '#4b5563', margin: 0, fontSize: '13px' };
const audioDetailStyle = { width: '100%', height: '40px', marginTop: '4px' };
const voiceLinkStyle = { fontSize: '13px', fontWeight: 500, color: '#0284c7' };
const fcrTagStyle = { fontWeight: 700, fontSize: '11px', padding: '2px 8px' };
const fcrNotesBoxStyle = {
  backgroundColor: '#f0fdf4',
  border: '1px solid #bbf7d0',
  borderRadius: '6px',
  padding: '10px 12px',
  marginTop: '6px'
};
const fcrNotesLabelStyle = {
  fontSize: '12px',
  display: 'block',
  marginBottom: '2px',
  fontWeight: 600,
  color: '#166534'
};
const fcrNotesTextStyle = { fontStyle: 'italic', color: '#15803d', margin: 0, fontSize: '13px' };
const fcrBarStyle = { display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' };
const fcrWrapStyle = {
  background: '#fafafa',
  border: '1px solid #d9d9d9',
  borderRadius: '6px',
  padding: '14px 18px',
  marginBottom: '24px'
};
const fcrCheckedWrapStyle = { ...fcrWrapStyle, background: '#f6ffed' };
const fcrLabelStyle = { fontSize: '14px', color: '#334155' };
const fcrNotesItemStyle = { marginTop: '14px', marginBottom: 0 };
const formItemNoMarginStyle = { marginBottom: 0 };
const modalWideStyle = { maxWidth: 900 };
const modalDetailStyle = { maxWidth: 700 };
const alertMarginStyle = { marginBottom: '16px' };
const rowMargin16Style = { marginBottom: '16px' };
const rowMargin24Style = { marginBottom: '24px' };
const eyeIconStyle = { color: PRIMARY, fontSize: '16px' };
const audioIconStyle = { color: '#0284c7' };

function StaffFormLabel({ children }) {
  return <span style={fieldLabelStyle}>{children}</span>;
}
const linkGapStyle = { marginTop: '6px' };
const secondary13Style = { fontSize: '13px' };
const secondary12Style = { fontSize: '12px' };
const strong14Style = { fontSize: '14px' };
const successNameStyle = { fontWeight: 600 };
const fcrTextareaStyle = { borderRadius: '4px', fontSize: '15px' };

function BranchStaffDashboard() {
  const { user } = useAuth();
  const [form] = Form.useForm();

  const [otherComplaints, setOtherComplaints] = useState([]);
  const [scopedComplaints, setScopedComplaints] = useState([]);
  const [allRegisteredComplaints, setAllRegisteredComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('all');
  const [allPage, setAllPage] = useState(1);
  const [allPageSize, setAllPageSize] = useState(10);
  const [trackingPage, setTrackingPage] = useState(1);
  const [trackingPageSize, setTrackingPageSize] = useState(10);
  const [otherPage, setOtherPage] = useState(1);
  const [otherPageSize, setOtherPageSize] = useState(10);
  const [searchText, setSearchText] = useState('');
  const [dateRange, setDateRange] = useState(null);
  const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);
  const [selectedComplaint, setSelectedComplaint] = useState(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [detailFromOther, setDetailFromOther] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [fcrChecked, setFcrChecked] = useState(false);

  // Evidence Attachment States
  const [evidenceFiles, setEvidenceFiles] = useState([]);
  const [isUploadingEvidence, setIsUploadingEvidence] = useState(false);

  // Voice Attachment States
  const [voiceFiles, setVoiceFiles] = useState([]);
  const [isUploadingVoice, setIsUploadingVoice] = useState(false);
  const [cbsProfile, setCbsProfile] = useState(null);
  const [lookingUpAccount, setLookingUpAccount] = useState(false);

  const userRole = (user?.role || '').toUpperCase();
  const isManager = userRole.includes('MANAGER') || userRole.includes('DIRECTOR') || userRole.includes('LEADER') ||
    userRole === 'ROLE_BRANCH_MANAGER' || userRole === 'ROLE_CONTACT_CENTER_TEAM_LEADER' ||
    userRole === 'ROLE_CONTACT_CENTER_SENIOR_MANAGER' || userRole === 'ROLE_DIGITAL_MARKETING_SENIOR_MANAGER' ||
    userRole === 'ROLE_CUSTOMER_EXPERIENCE_PARTNERSHIP' ||
    userRole === 'ROLE_CUSTOMER_SERVICE_MANAGER' || userRole === 'ROLE_CHIEF_EXPERIENCE_OFFICER';

  const isBranchManager = userRole.startsWith('ROLE_BRANCH_MANAGER')
    || userRole === 'ROLE_CUSTOMER_SERVICE_MANAGER';
  const hideOtherTab = isBranchManager
    || userRole === 'ROLE_CUSTOMER_EXPERIENCE_PARTNERSHIP'
    || userRole.startsWith('ROLE_DIGITAL_MARKETING');
  const isContactCenterUser = !hideOtherTab && (
    userRole.includes('CONTACT_CENTER') ||
    userRole.includes('BRANCH') ||
    userRole.includes('STAFF') ||
    userRole.includes('ADMIN') ||
    userRole === 'ROLE_CONTACT_CENTER_AGENT' ||
    userRole === 'ROLE_CONTACT_CENTER_TEAM_LEADER' ||
    userRole === 'ROLE_CONTACT_CENTER_SENIOR_MANAGER');

  useEffect(() => {
    void loadComplaintsData();
    const interval = setInterval(() => {
      void loadComplaintsData();
    }, 6000);
    return () => clearInterval(interval);
  }, [user]);

  const loadComplaintsData = async () => {
    try {
      setLoading(true);
      const [slaData, otherData, registeredData, taskData] = await Promise.all([
        ApiService.getAllSlaMetrics().catch(() => []),
        ApiService.getOtherSlaMetrics().catch(() => []),
        ApiService.getRegisteredSlaMetrics().catch(() => null),
        ApiService.getEnrichedTasks().catch(() => [])
      ]);

      const byUniqueIdAsc = (rows) => [...rows].sort((a, b) =>
        compareTicketNumbersAsc(formatUniqueId(a), formatUniqueId(b)));

      const otherList = Array.isArray(otherData) ? otherData : [];
      setOtherComplaints(byUniqueIdAsc(otherList));

      const list = Array.isArray(slaData) ? slaData : [];
      const filtered = list.filter(classifyComplaintFilter);
      setScopedComplaints(byUniqueIdAsc(filtered));
      const registeredList = Array.isArray(registeredData) ? registeredData : mergeRegisteredCases(list, otherList);
      const openTasks = (Array.isArray(taskData) ? taskData : []).map(taskAsRegisteredCase);
      setAllRegisteredComplaints(byUniqueIdAsc(mergeRegisteredCases(registeredList, openTasks)));
    } catch (err) {
      console.error('Failed to load complaints data:', err);
    } finally {
      setLoading(false);
    }
  };

  // Metric Calculations stay on the full list. Search filters the tables below.
  const totalRegistered = scopedComplaints.length;
  const fcrResolvedCount = scopedComplaints.filter(isResolvedAtFcr).length;
  const filteredTracking = scopedComplaints.filter((record) => matchesIntakeSearch(record, searchText, dateRange));
  const filteredOther = otherComplaints.filter((record) => matchesIntakeSearch(record, searchText, dateRange));
  const filteredAll = allRegisteredComplaints.filter((record) => matchesIntakeSearch(record, searchText, dateRange));
  const safeAllPage = Math.min(allPage, Math.max(1, Math.ceil(filteredAll.length / allPageSize) || 1));
  const safeTrackingPage = Math.min(trackingPage, Math.max(1, Math.ceil(filteredTracking.length / trackingPageSize) || 1));
  const safeOtherPage = Math.min(otherPage, Math.max(1, Math.ceil(filteredOther.length / otherPageSize) || 1));
  const allSlice = filteredAll.slice(
    (safeAllPage - 1) * allPageSize,
    safeAllPage * allPageSize
  );
  const trackingSlice = filteredTracking.slice(
    (safeTrackingPage - 1) * trackingPageSize,
    safeTrackingPage * trackingPageSize
  );
  const otherSlice = filteredOther.slice(
    (safeOtherPage - 1) * otherPageSize,
    safeOtherPage * otherPageSize
  );

  const applySearchText = (value) => {
    setSearchText(value);
    setAllPage(1);
    setTrackingPage(1);
    setOtherPage(1);
  };

  const applyDateRange = (value) => {
    setDateRange(value);
    setAllPage(1);
    setTrackingPage(1);
    setOtherPage(1);
  };

  const intakeExportDataset = () => {
    const today = new Date().toISOString().slice(0, 10);
    const tabs = {
      all: { title: 'All Registered', rows: filteredAll, detailed: true },
      tracking: { title: 'Total Complaints', rows: filteredTracking, detailed: true },
      other: { title: 'Other', rows: filteredOther, detailed: false }
    };
    const current = tabs[activeTab] || tabs.all;
    const columns = current.detailed
      ? [
        { header: 'Unique ID No', accessor: (record) => formatUniqueId(record) },
        { header: 'Customer Name', key: 'customerName' },
        { header: 'Complaint Category', accessor: (record) => record.complaintCategory || 'General' },
        { header: 'Current Status', type: 'status', accessor: (record) => record }
      ]
      : [
        { header: 'Unique ID No', accessor: (record) => formatUniqueId(record) }
      ];
    return {
      filename: `${current.title.replace(/\s+/g, '_')}_${today}`,
      documentTitle: current.title,
      subtitle: searchText.trim() ? `Search: ${searchText.trim()}` : 'Complaint Registration',
      tables: [{
        title: current.title,
        sheetName: current.title,
        columns,
        rows: current.rows
      }]
    };
  };

  const handleEvidenceUpload = async (e) => {
    const selected = Array.from(e.target.files || []);
    e.target.value = '';
    if (selected.length === 0) return;

    setIsUploadingEvidence(true);
    try {
      const uploaded = await Promise.all(selected.map(async (file) => {
        const response = await ApiService.uploadEvidence(file);
        return { name: response.fileName || file.name, url: response.url };
      }));
      setEvidenceFiles((prev) => [...prev, ...uploaded]);
      antMessage.success(uploaded.length > 1 ? 'Supporting documents uploaded successfully!' : 'Supporting document uploaded successfully!');
    } catch (error) {
      console.error('Evidence upload failed:', error);
      antMessage.error(error.message || 'Failed to upload supporting document.');
    } finally {
      setIsUploadingEvidence(false);
    }
  };

  const handleVoiceUpload = async (e) => {
    const selected = Array.from(e.target.files || []);
    e.target.value = '';
    if (selected.length === 0) return;

    setIsUploadingVoice(true);
    try {
      const uploaded = await Promise.all(selected.map(async (file) => {
        const response = await ApiService.uploadAudio(file);
        return { name: response.fileName || file.name, url: response.url };
      }));
      setVoiceFiles((prev) => [...prev, ...uploaded]);
      antMessage.success(uploaded.length > 1 ? 'Voice attachments uploaded successfully!' : 'Voice attachment uploaded successfully!');
    } catch (error) {
      console.error('Voice upload failed:', error);
      antMessage.error(error.message || 'Failed to upload voice attachment file.');
    } finally {
      setIsUploadingVoice(false);
    }
  };

  const normalizePhone = (phone) => {
    if (!phone) return phone;
    let clean = phone.trim();
    if (clean.startsWith('0')) return '+251' + clean.substring(1);
    if (clean.startsWith('9') || clean.startsWith('7')) return '+251' + clean;
    return clean;
  };

  const handleAccountLookup = async () => {
    const accountNumber = String(form.getFieldValue('accountNumber') || '').trim();
    if (!/^\d{13}$/.test(accountNumber)) {
      form.validateFields(['accountNumber']).catch(() => {});
      return;
    }
    setLookingUpAccount(true);
    try {
      const profile = await ApiService.getCustomerByAccount(accountNumber);
      setCbsProfile(profile);
      const currentPhone = form.getFieldValue('phone');
      form.setFieldsValue({
        customerName: profile.customerName || form.getFieldValue('customerName'),
        phone: currentPhone || profile.phoneNumber
      });
      antMessage.success('Customer profile loaded from Core Banking');
    } catch (error) {
      setCbsProfile(null);
      antMessage.error(error.message || 'Customer not found for this account number');
    } finally {
      setLookingUpAccount(false);
    }
  };

  const handleSubmit = async (values) => {
    setIsSubmitting(true);
    setMessage('');

    if (values.date && moment(values.date).isAfter(moment().endOf('day'))) {
      antMessage.error('Complaint date cannot be in the future');
      setIsSubmitting(false);
      return;
    }

    try {
      const phone = normalizePhone(values.phone);

      const payload = {
        customer: {
          name: values.customerName,
          email: values.email || '',
          phone: phone,
          currentContactPhone: phone,
          accountNumber: values.accountNumber,
          preferredContactMethod: values.preferredContactMethod || 'Email',
          customerHomeBranch: cbsProfile?.homeBranch || '',
          customerHomeDistrict: cbsProfile?.homeDistrict || '',
          customerBranchCode: cbsProfile?.branchCode || '',
          customerSegment: cbsProfile?.customerSegment || '',
          coreBankingPhone: cbsProfile?.phoneNumber || ''
        },
        complaint: {
          channel: values.channel || 'branch',
          receivedBy: values.channel || 'Branch',
          complaintMadeOn: values.complaintMadeOn || 'Branch',
          serviceType: values.serviceType || 'Deposit Account',
          category: values.complaintCategory || 'Customer Service Issues',
          description: values.complaintDescription,
          complaintBranch: values.branch || user?.branch || '',
          complaintDistrict: values.district || '',
          date: values.date ? values.date.format('YYYY-MM-DD') : '',
          isFcr: fcrChecked,
          fcrNotes: fcrChecked ? values.resolutionNotes : '',
          resolutionNotes: values.resolutionNotes || '',
          evidenceUrl: evidenceFiles[0]?.url || voiceFiles[0]?.url || undefined,
          evidenceName: evidenceFiles[0]?.name || voiceFiles[0]?.name || undefined,
          evidenceFiles: evidenceFiles.length > 0 ? evidenceFiles : undefined,
          voiceUrl: voiceFiles[0]?.url || undefined,
          voiceName: voiceFiles[0]?.name || undefined,
          voiceFiles: voiceFiles.length > 0 ? voiceFiles : undefined
        }
      };

      const result = await ApiService.staffSubmitComplaint(payload);
      const ticketId = result.ticketId || result.complaintId || '';
      const successMsg = fcrChecked
        ? `FCR complaint registered with resolution notes and routed to Customer Care Officer. Ticket: ${ticketId}`
        : `Complaint registered successfully and submitted to Customer Care Officer workflow. Ticket: ${ticketId}`;
      antMessage.success(successMsg, 4);
      setMessage(`Success: ${successMsg}`);
      setTimeout(() => setMessage(''), 4000);

      form.resetFields();
      setFcrChecked(false);
      setEvidenceFiles([]);
      setVoiceFiles([]);
      setCbsProfile(null);
      setIsRegisterModalOpen(false);
      await loadComplaintsData();
    } catch (error) {
      console.error('Error submitting complaint:', error);
      const serverMsg = error.response?.data?.error || error.message || 'Failed to submit complaint. Please check fields and try again.';
      setMessage(`Error: ${serverMsg}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const isFcrSuccess = message.startsWith('FCR_SUCCESS');
  const isSuccess = message.startsWith('Success') || isFcrSuccess;

  let modalAlertTitle = 'Error';
  if (isFcrSuccess) {
    modalAlertTitle = 'Resolved at First Contact';
  } else if (isSuccess) {
    modalAlertTitle = 'Success';
  }

  // Table Columns for Complaint Tracking
  const trackingColumns = [
    {
      title: 'Unique ID No',
      dataIndex: 'complaintId',
      key: 'complaintId',
      defaultSortOrder: 'ascend',
      sorter: (a, b) => compareTicketNumbersAsc(formatUniqueId(a), formatUniqueId(b)),
      render: (id, r) => (
        <UniqueIdDisplay record={r} />
      )
    },
    {
      title: 'Customer Name',
      dataIndex: 'customerName',
      key: 'customerName'
    },
    {
      title: 'Complaint Category',
      dataIndex: 'complaintCategory',
      key: 'complaintCategory',
      render: (c) => <Tag color="blue">{(c || 'General').toUpperCase()}</Tag>
    },
    {
      title: 'Current Status',
      dataIndex: 'status',
      key: 'status',
      render: (s, r) => {
        const statusStr = (s || r.classification || 'RECORDED').toUpperCase();
        const isDeclined = statusStr === 'DECLINED' || r.classification === 'DECLINED' || r.variables?.decision === 'DECLINED';
        if (isDeclined) return renderComplaintStatusTag('DECLINED');
        return renderComplaintStatusTag(statusStr);
      }
    },
    {
      title: 'Action',
      key: 'action',
      align: 'center',
      render: (_, r) => (
        <Tooltip title="View Details">
          <Button
            type="text"
            size="small"
            icon={<EyeOutlined style={eyeIconStyle} />}
            onClick={() => {
              setSelectedComplaint(r);
              setDetailFromOther(isOtherCase(r));
              setIsDetailModalOpen(true);
            }}
          />
        </Tooltip>
      )
    }
  ];

  // Columns for Other Tab (omits Customer Name, Current Status, and Complaint Category)
  const otherColumns = [
    {
      title: 'Unique ID No',
      dataIndex: 'complaintId',
      key: 'complaintId',
      defaultSortOrder: 'ascend',
      sorter: (a, b) => compareTicketNumbersAsc(formatUniqueId(a), formatUniqueId(b)),
      render: (id, r) => (
        <UniqueIdDisplay record={r} />
      )
    },
    {
      title: 'Action',
      key: 'action',
      align: 'center',
      render: (_, r) => (
        <Tooltip title="View Details">
          <Button
            type="text"
            size="small"
            icon={<EyeOutlined style={eyeIconStyle} />}
            onClick={() => {
              setSelectedComplaint(r);
              setDetailFromOther(true);
              setIsDetailModalOpen(true);
            }}
          />
        </Tooltip>
      )
    }
  ];

  const fcrBoxStyle = fcrChecked ? fcrCheckedWrapStyle : fcrWrapStyle;
  const viewingOtherCase = Boolean(selectedComplaint) && (detailFromOther || isOtherCase(selectedComplaint));
  const closeDetail = () => {
    setIsDetailModalOpen(false);
    setDetailFromOther(false);
  };

  return (
    <DashboardLayout userRole="branch-staff">
      <Space direction="vertical" size="large" style={pageStackStyle}>
        <div style={headerRowStyle}>
          <div>
            <Title level={3} style={pageTitleStyle}>
              {isManager ? 'Operational Management Workspace' : 'Frontline Complaint Intake Workspace'}
            </Title>
            <Text type="secondary" style={secondary13Style}>
              Welcome, {user?.fullName || user?.username} &bull; Complaints Overview
            </Text>
          </div>
          <div>
            <Button
              type="primary"
              icon={<PlusCircleOutlined />}
              onClick={() => setIsRegisterModalOpen(true)}
              style={primaryButtonStyle}
              size="large"
            >
              Register New Complaint
            </Button>
          </div>
        </div>

        <div>
          <Text strong style={overviewLabelStyle}>
            Complaints Overview
          </Text>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12}>
              <Card size="small" style={cardRoundStyle}>
                <Text type="secondary" style={secondary12Style}>Total Complaints</Text>
                <Title level={3} style={metricTitleStyle}>{totalRegistered}</Title>
              </Card>
            </Col>
            <Col xs={24} sm={12}>
              <Card size="small" style={cardRoundStyle}>
                <Text type="secondary" style={secondary12Style}>Resolved at FCR</Text>
                <Title level={3} style={fcrMetricTitleStyle}>{fcrResolvedCount}</Title>
              </Card>
            </Col>
          </Row>
          <div style={searchBarStyle}>
            <Input
              allowClear
              prefix={<SearchOutlined style={{ color: '#475569' }} />}
              placeholder="Search by name or ticket"
              value={searchText}
              onChange={(e) => applySearchText(e.target.value)}
              style={searchInputStyle}
            />
            <RangePicker
              value={dateRange}
              format="DD/MM/YYYY"
              placeholder={['Start date', 'End date']}
              disabledDate={(current) => current?.isAfter(moment().endOf('day'))}
              onChange={applyDateRange}
            />
          </div>
        </div>

        <Card bodyStyle={tabsCardBodyStyle} style={cardRoundStyle}>
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            tabBarExtraContent={(
              <ExportDropdown size="small" getDataset={intakeExportDataset} />
            )}
            items={(() => {
              const tabItems = [
                {
                  key: 'all',
                  label: 'All Registered',
                  children: (
                    <div className="cms-table-shell" style={{ background: '#fff', padding: '12px 8px 4px', borderRadius: '8px' }}>
                      <Table
                        dataSource={allSlice}
                        columns={trackingColumns}
                        rowKey={(r, index) => registeredCaseKey(r) || index}
                        loading={loading}
                        pagination={false}
                        size="middle"
                        bordered={false}
                        locale={{ emptyText: 'No registered cases.' }}
                        scroll={{ x: 'max-content' }}
                      />
                      <Pagination
                        currentPage={allPage}
                        pageSize={allPageSize}
                        totalRecords={filteredAll.length}
                        onPageChange={setAllPage}
                        onPageSizeChange={(size) => { setAllPageSize(size); setAllPage(1); }}
                        itemUnit="cases"
                      />
                    </div>
                  )
                },
                {
                  key: 'tracking',
                  label: 'Total Complaints',
                  children: (
                    <div className="cms-table-shell" style={{ background: '#fff', padding: '12px 8px 4px', borderRadius: '8px' }}>
                      <Table
                        dataSource={trackingSlice}
                        columns={trackingColumns}
                        rowKey={(r, index) => r.complaintId || r.processInstanceId || index}
                        loading={loading}
                        pagination={false}
                        size="middle"
                        bordered={false}
                        scroll={{ x: 'max-content' }}
                      />
                      <Pagination
                        currentPage={trackingPage}
                        pageSize={trackingPageSize}
                        totalRecords={filteredTracking.length}
                        onPageChange={setTrackingPage}
                        onPageSizeChange={(size) => { setTrackingPageSize(size); setTrackingPage(1); }}
                        itemUnit="complaints"
                      />
                    </div>
                  )
                }
              ];
              if (isContactCenterUser) {
                tabItems.push({
                  key: 'other',
                  label: 'Other',
                  children: (
                    <div className="cms-table-shell" style={{ background: '#fff', padding: '12px 8px 4px', borderRadius: '8px' }}>
                      <Table
                        dataSource={otherSlice}
                        columns={otherColumns}
                        rowKey={(r, index) => r.complaintId || r.processInstanceId || r.generalTicketId || index}
                        loading={loading}
                        pagination={false}
                        size="middle"
                        bordered={false}
                        locale={{ emptyText: 'No items classified as Other.' }}
                        scroll={{ x: 'max-content' }}
                      />
                      <Pagination
                        currentPage={otherPage}
                        pageSize={otherPageSize}
                        totalRecords={filteredOther.length}
                        onPageChange={setOtherPage}
                        onPageSizeChange={(size) => { setOtherPageSize(size); setOtherPage(1); }}
                        itemUnit="items"
                      />
                    </div>
                  )
                });
              }

              return tabItems;
            })()}
          />
        </Card>

        <Modal
          title={<span style={modalTitleStyle}>Register Customer Complaint</span>}
          open={isRegisterModalOpen}
          onCancel={() => {
            setIsRegisterModalOpen(false);
            setCbsProfile(null);
          }}
          footer={null}
          width="100%"
          style={modalWideStyle}
        >
          {message && (
            <Alert
              message={modalAlertTitle}
              description={message.replace(/^(FCR_SUCCESS|Success|Error):\s*/, '')}
              type={isSuccess ? 'success' : 'error'}
              showIcon
              style={alertMarginStyle}
            />
          )}

          <Form
            form={form}
            layout="vertical"
            validateTrigger={['onChange', 'onBlur']}
            onFinish={handleSubmit}
            onFinishFailed={() => antMessage.error('Please fix the errors in the form before submitting.')}
            initialValues={{
              complaintCategory: 'Customer Service Issues',
              serviceType: 'Deposit Account',
              channel: 'Branch',
              preferredContactMethod: 'Email'
            }}
          >
            <div style={sectionHeadingStyle}>
              Customer Information
            </div>

            <Row gutter={32} style={rowMargin16Style}>
              <Col xs={24} md={12}>
                <Form.Item
                  name="customerName"
                  label={<StaffFormLabel>Customer Name</StaffFormLabel>}
                  rules={[fieldRule((value) => getCustomerNameError(value, 'english'))]}
                >
                  <Input maxLength={100} style={controlStyle} />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item
                  name="email"
                  label={<StaffFormLabel>Email Address</StaffFormLabel>}
                  rules={[fieldRule((value) => getEmailError(value, 'english'))]}
                >
                  <Input style={controlStyle} />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={32} style={rowMargin16Style}>
              <Col xs={24} md={12}>
                <Form.Item
                  name="phone"
                  label={<StaffFormLabel>Current Contact Phone</StaffFormLabel>}
                  getValueFromEvent={(e) => String(e.target.value || '').replace(/[^\d+]/g, '').slice(0, 13)}
                  rules={[fieldRule(getStaffPhoneError)]}
                >
                  <Input maxLength={13} placeholder="09... or +251..." style={controlStyle} />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item
                  name="date"
                  label={<StaffFormLabel>Complaint Date</StaffFormLabel>}
                  rules={[
                    { required: true, message: 'Please select complaint date' },
                    {
                      validator: (_, value) => {
                        if (value && moment(value).isAfter(moment().endOf('day'))) {
                          return Promise.reject(new Error('Complaint date cannot be in the future'));
                        }
                        return Promise.resolve();
                      }
                    }
                  ]}
                >
                  <DatePicker
                    format="DD/MM/YYYY"
                    placeholder="DD/MM/YYYY"
                    disabledDate={(current) => current?.isAfter(moment().endOf('day'))}
                    style={datePickerStyle}
                  />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={32} style={rowMargin24Style}>
              <Col xs={24} md={8}>
                <Form.Item
                  name="accountNumber"
                  label={<StaffFormLabel>Account Number</StaffFormLabel>}
                  getValueFromEvent={(e) => String(e.target.value || '').replace(/\D/g, '').slice(0, 13)}
                  rules={[fieldRule((value) => getAccountNumberError(value, 'english'))]}
                >
                  <Input.Search
                    maxLength={13}
                    style={controlStyle}
                    enterButton={<SearchOutlined />}
                    loading={lookingUpAccount}
                    onSearch={handleAccountLookup}
                    placeholder="13-digit account"
                  />
                </Form.Item>
              </Col>
              <Col xs={24} md={8}>
                <Form.Item
                  name="district"
                  label={<StaffFormLabel>Complaint District</StaffFormLabel>}
                  getValueFromEvent={(e) => String(e.target.value || '').replace(/[^\p{L} ]/gu, '').slice(0, 100)}
                  rules={[fieldRule((value) => getPlaceNameError(value, 'english', 'Complaint District'))]}
                >
                  <Input maxLength={100} placeholder="Enter Complaint District" style={controlStyle} />
                </Form.Item>
              </Col>
              <Col xs={24} md={8}>
                <Form.Item
                  name="branch"
                  label={<StaffFormLabel>Complaint Branch</StaffFormLabel>}
                  getValueFromEvent={(e) => String(e.target.value || '').replace(/[^\p{L} ]/gu, '').slice(0, 100)}
                  rules={[fieldRule((value) => getPlaceNameError(value, 'english', 'Complaint Branch'))]}
                >
                  <Input maxLength={100} placeholder="Enter Complaint Branch" style={controlStyle} />
                </Form.Item>
              </Col>
            </Row>

            {cbsProfile && (
              <div style={customerProfileBoxStyle}>
                <Text style={customerProfileHeadingStyle}>Customer Profile</Text>
                <Row gutter={[16, 12]}>
                  <Col xs={24} md={8}><Text type="secondary">Customer Name</Text><div><Text strong>{cbsProfile.customerName || 'N/A'}</Text></div></Col>
                  <Col xs={24} md={8}><Text type="secondary">Phone Number</Text><div><Text strong>{cbsProfile.phoneNumber || 'N/A'}</Text></div></Col>
                  <Col xs={24} md={8}><Text type="secondary">Branch Code</Text><div><Text strong>{cbsProfile.branchCode || 'N/A'}</Text></div></Col>
                  <Col xs={24} md={8}><Text type="secondary">Home Branch</Text><div><Text strong>{cbsProfile.homeBranch || 'N/A'}</Text></div></Col>
                  <Col xs={24} md={8}><Text type="secondary">Home District</Text><div><Text strong>{cbsProfile.homeDistrict || 'N/A'}</Text></div></Col>
                  <Col xs={24} md={8}><Text type="secondary">Customer Segment</Text><div><Text strong>{cbsProfile.customerSegment || 'N/A'}</Text></div></Col>
                </Row>
              </div>
            )}

            <div style={complaintSectionHeadingStyle}>
              Complaint Details
            </div>
            <Row gutter={[16, 16]} style={rowMargin16Style}>
              <Col xs={24} md={6}>
                <Form.Item name="complaintCategory" label={<StaffFormLabel>Category</StaffFormLabel>} rules={[{ required: true, message: 'Please select a category' }]}>
                  <Select style={selectControlStyle}>
                    <Select.Option value="Customer Service Issues">Customer Service Issues</Select.Option>
                    <Select.Option value="Transaction Error">Transaction Error</Select.Option>
                    <Select.Option value="Account Management">Account Management</Select.Option>
                    <Select.Option value="Banking App Issues">Banking App Issues</Select.Option>
                    <Select.Option value="Credit/Financing Concerns">Credit/Financing Concerns</Select.Option>
                    <Select.Option value="Fraud & Security Risk">Fraud & Security Risk</Select.Option>
                    <Select.Option value="Information Disclosure">Information Disclosure</Select.Option>
                    <Select.Option value="ATM & Card Banking Issues">ATM & Card Banking Issues</Select.Option>
                    <Select.Option value="Policy & Compliance Disputes">Policy & Compliance Disputes</Select.Option>
                    <Select.Option value="System Failure">System Failure</Select.Option>
                    <Select.Option value="Branch Operation">Branch Operation</Select.Option>
                  </Select>
                </Form.Item>
              </Col>
              <Col xs={24} md={6}>
                <Form.Item name="serviceType" label={<StaffFormLabel>Service Type</StaffFormLabel>} rules={[{ required: true, message: 'Please select service type' }]}>
                  <Select style={selectControlStyle}>
                    <Select.Option value="Deposit Account">Deposit Account</Select.Option>
                    <Select.Option value="Credit/Financing">Credit/Financing</Select.Option>
                    <Select.Option value="Digital Banking">Digital Banking</Select.Option>
                    <Select.Option value="ATM & Card Banking">ATM & Card Banking</Select.Option>
                    <Select.Option value="RTGs">RTGs</Select.Option>
                    <Select.Option value="International Banking">International Banking</Select.Option>
                    <Select.Option value="Vendor/Procurement">Vendor/Procurement</Select.Option>
                    <Select.Option value="Incoming Fund Transfer">Incoming Fund Transfer</Select.Option>
                    <Select.Option value="Outgoing Fund Transfer">Outgoing Fund Transfer</Select.Option>
                    <Select.Option value="Merchant Service via Super App">Merchant Service via Super App</Select.Option>
                    <Select.Option value="Interest Free Banking Services">Interest Free Banking Services</Select.Option>
                    <Select.Option value="Other">Other</Select.Option>
                  </Select>
                </Form.Item>
              </Col>
              <Col xs={24} md={6}>
                <Form.Item name="channel" label={<StaffFormLabel>Received By</StaffFormLabel>} rules={[{ required: true, message: 'Please select how the complaint was received' }]}>
                  <Select style={selectControlStyle}>
                    <Select.Option value="Customer Care-Telephone">Customer Care-Telephone</Select.Option>
                    <Select.Option value="Customer Care-In person">Customer Care-In person</Select.Option>
                    <Select.Option value="Contact Center">Contact Center</Select.Option>
                    <Select.Option value="Contact Center-Email">Contact Center-Email</Select.Option>
                    <Select.Option value="Digital Marketing">Digital Marketing</Select.Option>
                    <Select.Option value="HO">HO</Select.Option>
                    <Select.Option value="Branch">Branch</Select.Option>
                    <Select.Option value="District">District</Select.Option>
                  </Select>
                </Form.Item>
              </Col>
              <Col xs={24} md={6}>
                <Form.Item name="complaintMadeOn" label={<StaffFormLabel>Complaint Made on</StaffFormLabel>} rules={[{ required: true, message: 'Please select Complaint Made on' }]}>
                  <Select style={selectControlStyle}>
                    <Select.Option value="Agent">Agent</Select.Option>
                    <Select.Option value="ATM">ATM</Select.Option>
                    <Select.Option value="Branch">Branch</Select.Option>
                    <Select.Option value="District Office">District Office</Select.Option>
                    <Select.Option value="HO">HO</Select.Option>
                    <Select.Option value="Super App">Super App</Select.Option>
                    <Select.Option value="Merchant">Merchant</Select.Option>
                  </Select>
                </Form.Item>
              </Col>
            </Row>

            <Form.Item
              name="complaintDescription"
              label={<StaffFormLabel>Details of the Complaint</StaffFormLabel>}
              rules={[fieldRule((value) => getDescriptionError(value, 'english'))]}
            >
              <Input.TextArea rows={4} maxLength={5000} showCount style={textareaStyle} />
            </Form.Item>

            <div style={uploadBoxStyle}>
              <div style={uploadTitleStyle}>
                1. Supporting Evidence Document (PDF / Image / Doc)
              </div>
              <Space align="center">
                <input
                  type="file"
                  multiple
                  accept={EVIDENCE_FILE_ACCEPT}
                  onChange={handleEvidenceUpload}
                  style={hiddenFileInputStyle}
                  id="intake-evidence-file-manager"
                />
                <Button icon={<PaperClipOutlined />} onClick={() => document.getElementById('intake-evidence-file-manager').click()} loading={isUploadingEvidence} style={roundedButtonStyle}>
                  {evidenceFiles.length > 0 ? 'Add Documents' : 'Upload Supporting Documents'}
                </Button>
                {evidenceFiles.map((file) => (
                  <Text key={file.url} type="success" style={successNameStyle}>
                    ✓ {file.name}
                    <Button type="link" size="small" onClick={() => setEvidenceFiles((prev) => prev.filter((item) => item.url !== file.url))}>Remove</Button>
                  </Text>
                ))}
              </Space>
            </div>

            <div style={uploadBoxLastStyle}>
              <div style={uploadTitleStyle}>
                2. Voice / Call Recording Attachment (Audio File)
              </div>
              <Space align="center">
                <input
                  type="file"
                  multiple
                  accept={VOICE_FILE_ACCEPT}
                  onChange={handleVoiceUpload}
                  style={hiddenFileInputStyle}
                  id="intake-voice-file-manager"
                />
                <Button
                  icon={<PaperClipOutlined />}
                  onClick={() => document.getElementById('intake-voice-file-manager').click()}
                  loading={isUploadingVoice}
                  style={roundedButtonStyle}
                >
                  {voiceFiles.length > 0 ? 'Add Voice Attachments' : 'Upload Voice / Call Audio Files'}
                </Button>
                {voiceFiles.map((file) => (
                  <Text key={file.url} type="success" style={successNameStyle}>
                    ✓ {file.name}
                    <Button type="link" size="small" onClick={() => setVoiceFiles((prev) => prev.filter((item) => item.url !== file.url))}>Remove</Button>
                  </Text>
                ))}
              </Space>

              {voiceFiles.map((file) => (
                <div key={file.url} style={voicePreviewWrapStyle}>
                  <Text type="secondary" style={voicePreviewLabelStyle}>
                    Voice Recording Preview: {file.name}
                  </Text>
                  {/* eslint-disable-next-line jsx-a11y/media-has-caption -- call recording has no caption track */}
                  <audio controls src={ApiService.getAttachmentUrl(file.url)} style={audioPreviewStyle} aria-label="Voice recording preview" />
                </div>
              ))}
            </div>

            <div style={fcrBoxStyle}>
              <Checkbox checked={fcrChecked} onChange={(e) => setFcrChecked(e.target.checked)}>
                <strong style={fcrLabelStyle}>First Contact Resolution (FCR) Notes</strong>
              </Checkbox>

              {fcrChecked && (
                <Form.Item name="resolutionNotes" label={<StaffFormLabel>FCR Resolution Notes</StaffFormLabel>} rules={[{ required: true, message: 'Provide resolution notes' }]} style={fcrNotesItemStyle}>
                  <Input.TextArea rows={3} style={fcrTextareaStyle} />
                </Form.Item>
              )}
            </div>

            <Form.Item style={formItemNoMarginStyle}>
              <Button type="primary" htmlType="submit" loading={isSubmitting} size="large" style={submitButtonStyle}>
                Submit Complaint into Workflow
              </Button>
            </Form.Item>
          </Form>
        </Modal>

        <ScreenedCaseDetailModal
          open={isDetailModalOpen && viewingOtherCase}
          record={screenedCaseRecord(selectedComplaint)}
          title={screenedCaseTitle(selectedComplaint)}
          onClose={closeDetail}
        />

        <Modal
          title={<span style={detailModalTitleStyle}>Task Details</span>}
          open={isDetailModalOpen && !viewingOtherCase}
          onCancel={closeDetail}
          footer={null}
          width="100%"
          style={modalDetailStyle}
        >
          {selectedComplaint && (() => {
            const customer = selectedComplaint.variables?.customer || {};
            return (
            <div style={detailBoxStyle}>
              <Title level={5} style={detailHeadingStyle}>
                Complaint Information
              </Title>

              <Row gutter={[16, 12]}>
                <Col span={12}>
                  <Text type="secondary" style={mutedBlockLabelStyle}>Unique ID No</Text>
                  <strong style={ticketIdStyle}>
                    {formatUniqueId(selectedComplaint)}
                  </strong>
                  {formatIntakeId(selectedComplaint) && formatIntakeId(selectedComplaint) !== formatUniqueId(selectedComplaint) && (
                    <div style={intakeIdStyle}>
                      Intake {formatIntakeId(selectedComplaint)}
                    </div>
                  )}
                </Col>
                <Col span={12}>
                  <Text type="secondary" style={mutedBlockLabelStyle}>Customer Name</Text>
                  <strong style={strong14Style}>{registeredCaseText(selectedComplaint.customerName || customer.name)}</strong>
                </Col>
                <Col span={12}>
                  <Text type="secondary" style={mutedBlockLabelStyle}>Category</Text>
                  <Tag color="blue">{(selectedComplaint.complaintCategory || 'General').toUpperCase()}</Tag>
                </Col>
                <Col span={12}>
                  <Text type="secondary" style={mutedBlockLabelStyle}>Overall Status</Text>
                  {renderComplaintStatusTag(selectedComplaint)}
                </Col>
              </Row>

              {selectedComplaint.complaintDescription && (
                <div style={sectionDividerStyle}>
                  <Text type="secondary" style={voicePreviewLabelStyle}>Description</Text>
                  <p style={descriptionTextStyle}>
                    {selectedComplaint.complaintDescription}
                  </p>
                </div>
              )}

              {(() => {
                const complaintVars = selectedComplaint.variables?.complaint || {};
                const documents = collectAttachments(
                  complaintVars.evidenceFiles,
                  selectedComplaint.evidenceFiles,
                  { url: complaintVars.evidenceUrl || selectedComplaint.evidenceUrl || selectedComplaint.attachmentUrl, name: complaintVars.evidenceName || selectedComplaint.evidenceName }
                ).filter((file) => !isAudioAttachment(file));
                const voices = collectAttachments(
                  complaintVars.voiceFiles,
                  selectedComplaint.voiceFiles,
                  { url: complaintVars.voiceUrl || selectedComplaint.voiceUrl || selectedComplaint.variables?.voiceUrl, name: complaintVars.voiceName || selectedComplaint.voiceName }
                );
                return (
                  <>
                    {documents.length > 0 && (
                      <div style={sectionDividerStyle}>
                        <Text type="secondary" style={attachLabelStyle}>
                          <PaperClipOutlined /> Attached Evidence:
                        </Text>
                        {documents.map((file) => (
                          <div key={file.url} style={linkGapStyle}>
                            <a href={ApiService.getAttachmentUrl(file.url)} target="_blank" rel="noopener noreferrer">
                              <PaperClipOutlined /> View Document: {file.name}
                            </a>
                          </div>
                        ))}
                      </div>
                    )}
                    {voices.length > 0 && (
                      <div style={sectionDividerStyle}>
                        <Text type="secondary" style={attachLabelStyle}>
                          <AudioOutlined style={audioIconStyle} /> Voice Recording Attachment:
                        </Text>
                        {voices.map((file) => (
                          <div key={file.url} style={linkGapStyle}>
                            {/* eslint-disable-next-line jsx-a11y/media-has-caption -- voice recording has no caption track */}
                            <audio controls src={ApiService.getAttachmentUrl(file.url)} style={audioDetailStyle} aria-label="Voice recording attachment" />
                            <a href={ApiService.getAttachmentUrl(file.url)} target="_blank" rel="noopener noreferrer" style={voiceLinkStyle}>
                              <AudioOutlined /> Download Voice Clip ({file.name})
                            </a>
                          </div>
                        ))}
                      </div>
                    )}
                  </>
                );
              })()}

              {(selectedComplaint.fcrStatus || selectedComplaint.isFcr || selectedComplaint.variables?.complaint?.isFcr || selectedComplaint.fcrNotes || selectedComplaint.resolutionNotes || selectedComplaint.variables?.complaint?.fcrNotes) && (
                <div style={sectionDividerStyle}>
                  <div style={fcrBarStyle}>
                    <Tag color="cyan" style={fcrTagStyle}>
                      FIRST CONTACT RESOLUTION (FCR)
                    </Tag>
                  </div>
                  {(selectedComplaint.fcrNotes || selectedComplaint.resolutionNotes || selectedComplaint.variables?.complaint?.fcrNotes) && (
                    <div style={fcrNotesBoxStyle}>
                      <Text type="secondary" style={fcrNotesLabelStyle}>
                        FCR Resolution Notes:
                      </Text>
                      <p style={fcrNotesTextStyle}>
                        {selectedComplaint.fcrNotes || selectedComplaint.resolutionNotes || selectedComplaint.variables?.complaint?.fcrNotes}
                      </p>
                    </div>
                  )}
                </div>
              )}
            </div>
            );
          })()}
        </Modal>

      </Space>
    </DashboardLayout>
  );
}

export default BranchStaffDashboard;
