import React, { useState, useEffect } from 'react';
import { Card, Typography, Button, Form, Input, Select, Alert, Row, Col, Space, Checkbox, Table, Tag, Modal, Tooltip, Tabs, DatePicker, message as antMessage } from 'antd';
import { PlusCircleOutlined, PaperClipOutlined, EyeOutlined, AudioOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import { formatUniqueId, formatIntakeId, UniqueIdDisplay } from '../components/TaskTable';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import { useAuth } from '../contexts/AuthContext';
import { renderComplaintStatusTag } from '../utils/statusUtils';
import { isResolvedAtFcr } from '../utils/slaMetrics';

const { Title, Text } = Typography;

const PRIMARY = BRAND_COLORS.primary;
const EVIDENCE_FILE_ACCEPT = '.pdf,.png,.jpg,.jpeg,.doc,.docx';
const VOICE_FILE_ACCEPT = '.mp3,.wav,.m4a,.ogg,.webm,.aac,.flac,.wma,.amr';

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
  const [hierarchy, setHierarchy] = useState([]);
  const [selectedDistrict, setSelectedDistrict] = useState('');
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('tracking');
  const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);
  const [selectedComplaint, setSelectedComplaint] = useState(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [fcrChecked, setFcrChecked] = useState(false);

  // Evidence Attachment States
  const [evidenceUrl, setEvidenceUrl] = useState('');
  const [evidenceName, setEvidenceName] = useState('');
  const [isUploadingEvidence, setIsUploadingEvidence] = useState(false);

  // Voice Attachment States
  const [voiceUrl, setVoiceUrl] = useState('');
  const [voiceName, setVoiceName] = useState('');
  const [isUploadingVoice, setIsUploadingVoice] = useState(false);

  const userRole = (user?.role || '').toUpperCase();
  const isManager = userRole.includes('MANAGER') || userRole.includes('DIRECTOR') || userRole.includes('LEADER') ||
    userRole === 'ROLE_BRANCH_MANAGER' || userRole === 'ROLE_CONTACT_CENTER_TEAM_LEADER' ||
    userRole === 'ROLE_CONTACT_CENTER_SENIOR_MANAGER' || userRole === 'ROLE_DIGITAL_MARKETING_SENIOR_MANAGER' ||
    userRole === 'ROLE_CUSTOMER_EXPERIENCE_PARTNERSHIP' ||
    userRole === 'ROLE_CUSTOMER_SERVICE_MANAGER' || userRole === 'ROLE_CHIEF_EXPERIENCE_OFFICER';

  const isContactCenterUser = userRole.includes('CONTACT_CENTER') ||
    userRole.includes('BRANCH') ||
    userRole.includes('STAFF') ||
    userRole.includes('ADMIN') ||
    userRole === 'ROLE_CONTACT_CENTER_AGENT' ||
    userRole === 'ROLE_CONTACT_CENTER_TEAM_LEADER' ||
    userRole === 'ROLE_CONTACT_CENTER_SENIOR_MANAGER' ||
    userRole === 'ROLE_CUSTOMER_EXPERIENCE_PARTNERSHIP';

  useEffect(() => {
    loadComplaintsData();
    const interval = setInterval(loadComplaintsData, 6000);
    return () => clearInterval(interval);
  }, [user]);

  const loadComplaintsData = async () => {
    try {
      const [slaData, otherData, hierarchyData] = await Promise.all([
        ApiService.getAllSlaMetrics().catch(() => []),
        ApiService.getOtherSlaMetrics().catch(() => []),
        ApiService.getHierarchy().catch(() => [])
      ]);

      setOtherComplaints(Array.isArray(otherData) ? otherData : []);
      if (Array.isArray(hierarchyData)) {
        setHierarchy(hierarchyData);
      }

      // Fetch ALL registered complaints EXCLUDING OTHER and INTAKE-only records.
      const filtered = (slaData || []).filter(item => {
        if (!item) return false;
        const cls = (item.classification || '').toUpperCase();
        const st = (item.status || '').toUpperCase();
        if (cls === 'OTHER' || st === 'OTHER') {
          return false;
        }
        if (cls === 'INTAKE') {
          const cId = String(item.dbcTicketId || item.complaintId || item.generalTicketId || '');
          return cId.startsWith('DBC-') || cId.startsWith('FCR-');
        }
        return cls === 'COMPLAINT' || cls === 'DECLINED' || st === 'DECLINED'
          || String(item.dbcTicketId || item.complaintId || '').startsWith('DBC-')
          || String(item.complaintId || '').startsWith('FCR-')
          || item.fcrStatus === true;
      });

      setScopedComplaints(filtered);
    } catch (err) {
      console.error('Failed to load complaints data:', err);
    } finally {
      setLoading(false);
    }
  };

  const availableBranchesForDistrict = (distName) => {
    if (!distName) {
      const allBranches = [];
      (hierarchy || []).forEach(d => {
        if (d.branches) allBranches.push(...d.branches);
      });
      return allBranches;
    }
    const distObj = (hierarchy || []).find(d => d.name === distName);
    return distObj ? (distObj.branches || []) : [];
  };

  // Metric Calculations
  const totalRegistered = scopedComplaints.length;
  const fcrResolvedCount = scopedComplaints.filter(isResolvedAtFcr).length;

  const handleEvidenceUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setIsUploadingEvidence(true);
    try {
      const response = await ApiService.uploadEvidence(file);
      setEvidenceUrl(response.url);
      setEvidenceName(response.fileName);
      antMessage.success('Supporting document uploaded successfully!');
    } catch (error) {
      console.error('Evidence upload failed:', error);
      antMessage.error('Failed to upload supporting document.');
    } finally {
      setIsUploadingEvidence(false);
    }
  };

  const handleVoiceUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setIsUploadingVoice(true);
    try {
      const response = await ApiService.uploadEvidence(file);
      setVoiceUrl(response.url);
      setVoiceName(response.fileName);
      antMessage.success('Voice attachment uploaded successfully!');
    } catch (error) {
      console.error('Voice upload failed:', error);
      antMessage.error('Failed to upload voice attachment file.');
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

  const handleSubmit = async (values) => {
    setIsSubmitting(true);
    setMessage('');

    try {
      const phone = normalizePhone(values.phone);

      const payload = {
        customer: {
          name: values.customerName,
          email: values.email || '',
          phone: phone,
          accountNumber: values.accountNumber,
          preferredContactMethod: values.preferredContactMethod || 'Email'
        },
        complaint: {
          channel: values.channel || 'branch',
          receivedBy: values.channel || 'Branch',
          complaintMadeOn: values.complaintMadeOn || 'Branch',
          serviceType: values.serviceType || 'Deposit Account',
          category: values.complaintCategory || 'Customer Service Issues',
          description: values.complaintDescription,
          branch: values.branch || user?.branch || '',
          district: values.district || '',
          date: values.date ? values.date.format('YYYY-MM-DD') : '',
          isFcr: fcrChecked,
          fcrNotes: fcrChecked ? values.resolutionNotes : '',
          resolutionNotes: values.resolutionNotes || '',
          evidenceUrl: evidenceUrl || voiceUrl || undefined,
          evidenceName: evidenceName || voiceName || undefined,
          voiceUrl: voiceUrl || undefined,
          voiceName: voiceName || undefined
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
      setSelectedDistrict('');
      setFcrChecked(false);
      setEvidenceUrl('');
      setEvidenceName('');
      setVoiceUrl('');
      setVoiceName('');
      setIsRegisterModalOpen(false);
      loadComplaintsData();
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
              setIsDetailModalOpen(true);
            }}
          />
        </Tooltip>
      )
    }
  ];

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
        </div>

        <Card bodyStyle={tabsCardBodyStyle} style={cardRoundStyle}>
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            items={(() => {
              const tabItems = [
                {
                  key: 'tracking',
                  label: 'Total Complaint',
                  children: (
                    <Table
                      dataSource={scopedComplaints}
                      columns={trackingColumns}
                      rowKey={(r, index) => r.complaintId || r.processInstanceId || index}
                      loading={loading}
                      pagination={{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'] }}
                      size="small"
                      scroll={{ x: 'max-content' }}
                    />
                  )
                }
              ];
              if (isContactCenterUser) {
                tabItems.push({
                  key: 'other',
                  label: 'Other',
                  children: (
                    <Table
                      dataSource={otherComplaints}
                      columns={otherColumns}
                      rowKey={(r, index) => r.complaintId || r.processInstanceId || r.generalTicketId || index}
                      loading={loading}
                      pagination={{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'] }}
                      size="small"
                      locale={{ emptyText: 'No items classified as Other.' }}
                      scroll={{ x: 'max-content' }}
                    />
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
          onCancel={() => setIsRegisterModalOpen(false)}
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
            onFinish={handleSubmit}
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
                <Form.Item name="customerName" label={<span style={fieldLabelStyle}>Customer Name</span>} rules={[{ required: true, message: 'Please enter customer name' }]}>
                  <Input style={controlStyle} />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="email" label={<span style={fieldLabelStyle}>Email Address</span>} rules={[{ type: 'email', message: 'Valid email required' }]}>
                  <Input style={controlStyle} />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={32} style={rowMargin16Style}>
              <Col xs={24} md={12}>
                <Form.Item
                  name="phone"
                  label={<span style={fieldLabelStyle}>Contact Phone Number</span>}
                  rules={[
                    { required: true, message: 'Please enter contact number' },
                    { pattern: /^(\+?2510?[79]\d{8}|0?[79]\d{8})$/, message: 'Valid phone number required' }
                  ]}
                >
                  <Input style={controlStyle} />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="date" label={<span style={fieldLabelStyle}>Complaint Date</span>} rules={[{ required: true, message: 'Please select complaint date' }]}>
                  <DatePicker format="DD/MM/YYYY" placeholder="DD/MM/YYYY" style={datePickerStyle} />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={32} style={rowMargin24Style}>
              <Col xs={24} md={8}>
                <Form.Item
                  name="accountNumber"
                  label={<span style={fieldLabelStyle}>Account Number</span>}
                  rules={[
                    { required: true, message: 'Please enter account number' },
                    { len: 13, message: 'Account Number must be 13 digits' }
                  ]}
                >
                  <Input maxLength={13} style={controlStyle} />
                </Form.Item>
              </Col>
              <Col xs={24} md={8}>
                <Form.Item
                  name="district"
                  label={<span style={fieldLabelStyle}>District</span>}
                >
                  <Select
                    placeholder="— Select District —"
                    allowClear
                    onChange={(val) => {
                      setSelectedDistrict(val || '');
                      form.setFieldsValue({ branch: undefined });
                    }}
                    style={selectControlStyle}
                  >
                    {(hierarchy || []).map((dist) => (
                      <Select.Option key={dist.id || dist.name} value={dist.name}>
                        {dist.name}
                      </Select.Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              <Col xs={24} md={8}>
                <Form.Item
                  name="branch"
                  label={<span style={fieldLabelStyle}>Branch</span>}
                >
                  <Select
                    placeholder={selectedDistrict ? "— Select Branch —" : "— Select District or Choose Branch —"}
                    allowClear
                    showSearch
                    filterOption={(input, option) =>
                      (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                    }
                    style={selectControlStyle}
                  >
                    {availableBranchesForDistrict(selectedDistrict).map((br) => (
                      <Select.Option key={br.id || br.name} value={br.name}>
                        {br.name}
                      </Select.Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
            </Row>

            <div style={complaintSectionHeadingStyle}>
              Complaint Details
            </div>
            <Row gutter={[16, 16]} style={rowMargin16Style}>
              <Col xs={24} md={6}>
                <Form.Item name="complaintCategory" label={<span style={fieldLabelStyle}>Category</span>} rules={[{ required: true }]}>
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
                <Form.Item name="serviceType" label={<span style={fieldLabelStyle}>Service Type</span>} rules={[{ required: true, message: 'Please select service type' }]}>
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
                <Form.Item name="channel" label={<span style={fieldLabelStyle}>Received By</span>} rules={[{ required: true }]}>
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
                <Form.Item name="complaintMadeOn" label={<span style={fieldLabelStyle}>Complaint Made on</span>} rules={[{ required: true, message: 'Please select Complaint Made on' }]}>
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

            <Form.Item name="complaintDescription" label={<span style={fieldLabelStyle}>Details of the Complaint</span>} rules={[{ required: true, message: 'Please enter complaint details' }]}>
              <Input.TextArea rows={4} style={textareaStyle} />
            </Form.Item>

            <div style={uploadBoxStyle}>
              <div style={uploadTitleStyle}>
                1. Supporting Evidence Document (PDF / Image / Doc)
              </div>
              <Space align="center">
                <input
                  type="file"
                  accept={EVIDENCE_FILE_ACCEPT}
                  onChange={handleEvidenceUpload}
                  style={hiddenFileInputStyle}
                  id="intake-evidence-file-manager"
                />
                <Button icon={<PaperClipOutlined />} onClick={() => document.getElementById('intake-evidence-file-manager').click()} loading={isUploadingEvidence} style={roundedButtonStyle}>
                  {evidenceUrl ? 'Change Document' : 'Upload Supporting Document'}
                </Button>
                {evidenceName && <Text type="success" style={successNameStyle}>✓ {evidenceName}</Text>}
              </Space>
            </div>

            <div style={uploadBoxLastStyle}>
              <div style={uploadTitleStyle}>
                2. Voice / Call Recording Attachment (Audio File)
              </div>
              <Space align="center">
                <input
                  type="file"
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
                  {voiceUrl ? 'Change Voice Attachment' : 'Upload Voice / Call Audio File'}
                </Button>
                {voiceName && <Text type="success" style={successNameStyle}>✓ {voiceName}</Text>}
              </Space>

              {voiceUrl && (
                <div style={voicePreviewWrapStyle}>
                  <Text type="secondary" style={voicePreviewLabelStyle}>
                    Voice Recording Preview:
                  </Text>
                  {/* eslint-disable-next-line jsx-a11y/media-has-caption -- call recording has no caption track */}
                  <audio controls src={voiceUrl} style={audioPreviewStyle} aria-label="Voice recording preview" />
                </div>
              )}
            </div>

            <div style={fcrChecked ? fcrCheckedWrapStyle : fcrWrapStyle}>
              <Checkbox checked={fcrChecked} onChange={(e) => setFcrChecked(e.target.checked)}>
                <strong style={fcrLabelStyle}>First Contact Resolution (FCR) Notes</strong>
              </Checkbox>

              {fcrChecked && (
                <Form.Item name="resolutionNotes" label={<span style={fieldLabelStyle}>FCR Resolution Notes</span>} rules={[{ required: true, message: 'Provide resolution notes' }]} style={fcrNotesItemStyle}>
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

        <Modal
          title={<span style={detailModalTitleStyle}>Task Details</span>}
          open={isDetailModalOpen}
          onCancel={() => setIsDetailModalOpen(false)}
          footer={null}
          width="100%"
          style={modalDetailStyle}
        >
          {selectedComplaint && (
            <div style={detailBoxStyle}>
              {!(selectedComplaint.classification === 'OTHER' || selectedComplaint.status === 'OTHER') && (
                <Title level={5} style={detailHeadingStyle}>
                  Complaint Information
                </Title>
              )}

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
                  <strong style={strong14Style}>{selectedComplaint.customerName}</strong>
                </Col>

                {!(selectedComplaint.classification === 'OTHER' || selectedComplaint.status === 'OTHER') && (
                  <>
                    <Col span={12}>
                      <Text type="secondary" style={mutedBlockLabelStyle}>Category</Text>
                      <Tag color="blue">{(selectedComplaint.complaintCategory || 'General').toUpperCase()}</Tag>
                    </Col>
                    <Col span={12}>
                      <Text type="secondary" style={mutedBlockLabelStyle}>Overall Status</Text>
                      {renderComplaintStatusTag(selectedComplaint)}
                    </Col>
                  </>
                )}
              </Row>

              {selectedComplaint.complaintDescription && (
                <div style={sectionDividerStyle}>
                  <Text type="secondary" style={voicePreviewLabelStyle}>Description</Text>
                  <p style={descriptionTextStyle}>
                    {selectedComplaint.complaintDescription}
                  </p>
                </div>
              )}

              {(selectedComplaint.evidenceUrl || selectedComplaint.attachmentUrl) && (
                <div style={sectionDividerStyle}>
                  <Text type="secondary" style={attachLabelStyle}>
                    <PaperClipOutlined /> Attached Evidence:
                  </Text>
                  {((selectedComplaint.evidenceUrl || selectedComplaint.attachmentUrl || '').toLowerCase().includes('.webm') ||
                    (selectedComplaint.evidenceUrl || selectedComplaint.attachmentUrl || '').toLowerCase().includes('.mp3') ||
                    (selectedComplaint.evidenceUrl || selectedComplaint.attachmentUrl || '').toLowerCase().includes('.wav') ||
                    (selectedComplaint.evidenceUrl || selectedComplaint.attachmentUrl || '').toLowerCase().includes('.m4a') ||
                    (selectedComplaint.evidenceUrl || selectedComplaint.attachmentUrl || '').toLowerCase().includes('.ogg') ||
                    (selectedComplaint.evidenceName || '').toLowerCase().includes('voice')) ? (
                    <div>
                      {/* eslint-disable-next-line jsx-a11y/media-has-caption -- audio evidence has no caption track */}
                      <audio
                        controls
                        src={ApiService.getAttachmentUrl(selectedComplaint.evidenceUrl || selectedComplaint.attachmentUrl)}
                        style={audioDetailStyle}
                        aria-label="Audio evidence"
                      />
                      <div style={linkGapStyle}>
                        <a href={ApiService.getAttachmentUrl(selectedComplaint.evidenceUrl || selectedComplaint.attachmentUrl)} target="_blank" rel="noopener noreferrer">
                          <PaperClipOutlined /> Download / Listen Audio Evidence ({selectedComplaint.evidenceName || 'Audio_Clip'})
                        </a>
                      </div>
                    </div>
                  ) : (
                    <a href={ApiService.getAttachmentUrl(selectedComplaint.evidenceUrl || selectedComplaint.attachmentUrl)} target="_blank" rel="noopener noreferrer">
                      <PaperClipOutlined /> View Document: {selectedComplaint.evidenceName || 'Attached Evidence'}
                    </a>
                  )}
                </div>
              )}

              {(selectedComplaint.voiceUrl || selectedComplaint.variables?.voiceUrl || selectedComplaint.variables?.complaint?.voiceUrl) && (
                <div style={sectionDividerStyle}>
                  <Text type="secondary" style={attachLabelStyle}>
                    <AudioOutlined style={audioIconStyle} /> Voice Recording Attachment:
                  </Text>
                  {/* eslint-disable-next-line jsx-a11y/media-has-caption -- voice recording has no caption track */}
                  <audio
                    controls
                    src={ApiService.getAttachmentUrl(
                      selectedComplaint.voiceUrl ||
                      selectedComplaint.variables?.voiceUrl ||
                      selectedComplaint.variables?.complaint?.voiceUrl
                    )}
                    style={audioDetailStyle}
                    aria-label="Voice recording attachment"
                  />
                  <div style={linkGapStyle}>
                    <a
                      href={ApiService.getAttachmentUrl(
                        selectedComplaint.voiceUrl ||
                        selectedComplaint.variables?.voiceUrl ||
                        selectedComplaint.variables?.complaint?.voiceUrl
                      )}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={voiceLinkStyle}
                    >
                      <AudioOutlined /> Download Voice Clip ({selectedComplaint.voiceName || selectedComplaint.variables?.voiceName || 'Voice_Clip.webm'})
                    </a>
                  </div>
                </div>
              )}

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
          )}
        </Modal>

      </Space>
    </DashboardLayout>
  );
}

export default BranchStaffDashboard;
