import React, { useState, useEffect } from 'react';
import { Card, Typography, Button, Tag, Alert, Form, Input, Space, Row, Col, Table, Select, Tooltip } from 'antd';
import { ArrowLeftOutlined, SearchOutlined, PaperClipOutlined, EyeOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import { formatUniqueId, formatIntakeId, matchesTicketSearch } from '../components/TaskTable';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import { useAuth } from '../contexts/AuthContext';
import { complaintBranch } from '../utils/locationKeys';
import { collectAttachments } from '../utils/attachments';

const { Title, Text } = Typography;

const recordVars = (record) => record?.variables || {};
const customerOf = (record) => recordVars(record).customer || {};
const complaintOf = (record) => recordVars(record).complaint || {};

function customerDisplayName(record) {
  return record.customerName || recordVars(record).customerName || customerOf(record).fullName || 'N/A';
}

function complaintCategoryLabel(record) {
  return (complaintOf(record).category || recordVars(record).complaintCategory || record.category || 'General').toUpperCase();
}

function complaintPriorityLabel(record) {
  return (complaintOf(record).priority || recordVars(record).priorityLevel || record.priority || '').toUpperCase();
}

function matchesCommitteeSearch(task, searchQuery) {
  const q = searchQuery.toLowerCase();
  const custName = (task.customerName || recordVars(task).customerName || customerOf(task).fullName || '').toLowerCase();
  return !q || matchesTicketSearch(task, q) || custName.includes(q);
}

function ClassificationTag({ record }) {
  const p = complaintPriorityLabel(record);
  if (p.includes('HIGH')) return <Tag color="magenta">High Sensitive</Tag>;
  if (p.includes('SENSITIVE')) return <Tag color="orange">Sensitive</Tag>;
  return <Tag color="blue">Normal</Tag>;
}

function FieldLabel({ children }) {
  return (
    <Text style={{ fontSize: '12px', color: '#64748b', fontWeight: 500, display: 'block', marginBottom: '2px' }}>
      {children}
    </Text>
  );
}

function FieldValue({ children }) {
  return (
    <Text style={{ fontSize: '13px', color: '#0f172a', fontWeight: 600 }}>{children}</Text>
  );
}

function UniqueIdBadge({ value, fontSize = '12px', padding = '4px 10px' }) {
  return (
    <span style={{
      fontFamily: 'monospace',
      fontSize,
      color: '#111827',
      fontWeight: 600,
      background: '#f3f4f6',
      padding,
      borderRadius: '4px',
      border: '1px solid #e5e7eb',
      display: 'inline-block'
    }}>
      {value}
    </span>
  );
}

function CommitteeDecisionWorkspace({
  selectedTask,
  formData,
  setFormData,
  handleFormChange,
  handleFileChange,
  handleDecisionSubmit,
  selectedFiles,
  isSubmitting,
  onBack
}) {
  const vars = recordVars(selectedTask);
  const customer = customerOf(selectedTask);
  const complaint = complaintOf(selectedTask);
  const accountNumber = customer.accountNumber || complaint.accountNumber;
  const uniqueId = formatUniqueId(selectedTask);
  const intakeId = formatIntakeId(selectedTask);
  const showIntake = Boolean(intakeId && intakeId !== uniqueId);
  const auditSummary = vars.auditFindings || vars.auditJustification;
  const auditFiles = collectAttachments(
    vars.auditAttachments,
    { url: vars.auditAttachment || vars.evidenceUrl, name: vars.auditAttachmentName || vars.evidenceName }
  );

  return (
    <div>
      <div style={{ marginBottom: '16px' }}>
        <Button icon={<ArrowLeftOutlined />} onClick={onBack} />
      </div>
      <Row gutter={[24, 24]}>
        <Col xs={24} lg={12}>
          <div style={{
            border: '1px solid #e2e8f0',
            borderRadius: '8px',
            padding: '20px',
            backgroundColor: '#f8fafc'
          }}>
            <Title level={5} style={{ color: BRAND_COLORS.primary, marginTop: 0, marginBottom: '16px', fontWeight: 700 }}>
              Complaint Information
            </Title>
            <Row gutter={[16, 14]}>
              <Col span={12}>
                <FieldLabel>Unique ID No</FieldLabel>
                <UniqueIdBadge value={uniqueId} />
                {showIntake ? (
                  <Text type="secondary" style={{ fontSize: '11px', display: 'block', marginTop: '4px' }}>
                    Intake {intakeId}
                  </Text>
                ) : null}
              </Col>
              <Col span={12}>
                <FieldLabel>Customer Name</FieldLabel>
                <FieldValue>{selectedTask.customerName}</FieldValue>
              </Col>
              <Col span={12}>
                <FieldLabel>Category</FieldLabel>
                <Tag color="blue">{(complaint.category || 'General').toUpperCase()}</Tag>
              </Col>
              <Col span={12}>
                <FieldLabel>Complaint Branch</FieldLabel>
                <FieldValue>{complaintBranch(recordVars(selectedTask), complaint) || 'Main Branch'}</FieldValue>
              </Col>
              {accountNumber ? (
                <Col span={12}>
                  <FieldLabel>Account Number</FieldLabel>
                  <FieldValue>{accountNumber}</FieldValue>
                </Col>
              ) : null}
              <Col span={24}>
                <FieldLabel>Details of the Complaint</FieldLabel>
                <FieldValue>{complaint.description || 'N/A'}</FieldValue>
              </Col>
            </Row>
            {auditSummary ? (
              <div style={{ marginTop: '14px', paddingTop: '12px', borderTop: '1px solid #e2e8f0' }}>
                <Text style={{ fontSize: '12px', color: '#64748b', fontWeight: 500, display: 'block', marginBottom: '6px' }}>
                  Audit Findings & Recommendation Summary
                </Text>
                <p style={{ color: '#334155', margin: 0, fontSize: '13px', lineHeight: '1.5', background: '#ffffff', padding: '10px 12px', borderRadius: '6px', border: '1px solid #e2e8f0' }}>
                  {auditSummary}
                </p>
              </div>
            ) : null}
            {auditFiles.length > 0 ? (
              <div style={{ marginTop: '14px', paddingTop: '10px', borderTop: '1px solid #e2e8f0' }}>
                <Text style={{ fontSize: '12px', color: '#64748b', fontWeight: 500, display: 'block', marginBottom: '6px' }}>
                  Audit Supporting Attachment
                </Text>
                {auditFiles.map((file) => (
                  <a
                    key={file.url}
                    href={ApiService.getAttachmentUrl(file.url)}
                    download={file.name}
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{ fontSize: '13px', fontWeight: 600, color: BRAND_COLORS.primary, display: 'flex', alignItems: 'center', marginBottom: '4px' }}
                  >
                    <PaperClipOutlined style={{ marginRight: '6px' }} />
                    {file.name}
                  </a>
                ))}
              </div>
            ) : null}
          </div>
        </Col>
        <Col xs={24} lg={12}>
          <div style={{
            border: '1px solid #e2e8f0',
            borderRadius: '8px',
            padding: '20px',
            backgroundColor: '#f8fafc'
          }}>
            <Title level={5} style={{ color: BRAND_COLORS.primary, marginTop: 0, marginBottom: '16px', fontWeight: 700 }}>
              Record Committee Decision
            </Title>
            <Form layout="vertical" onFinish={() => handleDecisionSubmit()}>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item label="Committee Decision" required>
                    <Select
                      value={formData.committeeDecision}
                      onChange={(val) => setFormData(prev => ({ ...prev, committeeDecision: val }))}
                      style={{ borderRadius: '6px' }}
                    >
                      <Select.Option value="approved">Accept</Select.Option>
                      <Select.Option value="rejected">Rejected</Select.Option>
                      <Select.Option value="further_review">Refer for Further Review</Select.Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item label="Decision Date">
                    <Input
                      type="date"
                      name="decisionDate"
                      max={new Date().toISOString().split('T')[0]}
                      value={formData.decisionDate}
                      onChange={handleFormChange}
                      style={{ borderRadius: '6px' }}
                    />
                  </Form.Item>
                </Col>
              </Row>
              <Form.Item label="Approving Authority">
                <Input
                  name="approvingAuthority"
                  value={formData.approvingAuthority}
                  onChange={handleFormChange}
                  placeholder="Authority name or committee chair title..."
                  style={{ borderRadius: '6px' }}
                />
              </Form.Item>
              <Form.Item label="Decision Summary & Directions Passed" required>
                <Input.TextArea
                  name="decisionSummary"
                  value={formData.decisionSummary}
                  onChange={handleFormChange}
                  rows={4}
                  placeholder="Provide details on committee decision and directions passed..."
                  style={{ borderRadius: '6px' }}
                />
              </Form.Item>
              <div style={{ background: '#ffffff', border: '1px dashed #cbd5e1', borderRadius: '6px', padding: '12px 16px', marginBottom: '16px' }}>
                <Space direction="vertical" style={{ width: '100%' }}>
                  <Text strong style={{ fontSize: '13px', color: '#334155' }}>Upload Decision Documents</Text>
                  <Space wrap>
                    <input
                      type="file"
                      multiple
                      accept=".pdf,.jpg,.jpeg,.png,.docx,.xlsx,.mp3,.wav,.m4a"
                      onChange={handleFileChange}
                      style={{ display: 'none' }}
                      id="committee-document-upload"
                    />
                    <Button
                      type="button"
                      icon={<PaperClipOutlined />}
                      onClick={() => document.getElementById('committee-document-upload').click()}
                      style={{ borderRadius: '6px' }}
                    >
                      {selectedFiles.length > 0 ? 'Add Documents' : 'Choose Documents'}
                    </Button>
                    {selectedFiles.map((file) => (
                      <Text key={`${file.name}-${file.size}`} type="success">✓ {file.name}</Text>
                    ))}
                  </Space>
                </Space>
              </div>
              <Button
                type="primary"
                htmlType="submit"
                loading={isSubmitting}
                size="large"
                style={{
                  width: '100%',
                  backgroundColor: BRAND_COLORS.primary,
                  borderColor: BRAND_COLORS.primary,
                  borderRadius: '6px',
                  fontWeight: 600
                }}
              >
                Submit
              </Button>
            </Form>
          </div>
        </Col>
      </Row>
    </div>
  );
}

function isCommitteeReviewTask(task) {
  const isCommitteeTask = task.definitionKey === 'FormTask_ChiefCommittee' ||
    task.taskName?.toLowerCase().includes('committee') ||
    task.name?.toLowerCase().includes('committee');
  if (!isCommitteeTask) {
    return false;
  }
  const vars = recordVars(task);
  const currentStage = vars.currentStage || vars.stage;
  if (['CHIEF_EXPERIENCE_REVIEW', 'CHIEF_OPERATION_AUDIT', 'CMD_SCREENING', 'WORKUNIT_ASSIGNMENT', 'COMPLETED', 'RESOLVED', 'CLOSED'].includes(currentStage)) {
    return false;
  }
  return currentStage === 'COMMITTEE_REVIEW' || isCommitteeTask;
}

function ChiefCommitteeDashboard() {
  const { user } = useAuth();
  const [tasks, setTasks] = useState([]);
  const [selectedTask, setSelectedTask] = useState(null);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  const [formData, setFormData] = useState({
    committeeDecision: 'approved',
    decisionDate: '',
    approvingAuthority: 'Complaints Review & Decision Committee',
    decisionSummary: '',
    committeeAttachment: '',
    committeeAttachmentName: ''
  });
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    loadTasks(true);

    const interval = setInterval(() => {
      loadTasks(false);
    }, 5000);

    return () => clearInterval(interval);
  }, []);

  const loadTasks = async (showLoading = false) => {
    try {
      if (showLoading) setLoading(true);
      const tasksData = await ApiService.getEnrichedTasks();
      const committeeTasks = tasksData.filter(isCommitteeReviewTask);
      setTasks(committeeTasks);
    } catch (err) {
      console.error('Error loading committee tasks:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleTaskSelect = (task) => {
    setSelectedTask(task);
    setMessage('');
  };

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleFileChange = (e) => {
    const chosen = Array.from(e.target.files || []);
    e.target.value = '';
    if (chosen.length === 0) {
      return;
    }
    setSelectedFiles((prev) => [...prev, ...chosen]);
  };

  const handleDecisionSubmit = async (decisionOverride) => {
    if (!selectedTask) return;

    const finalDecision = decisionOverride || formData.committeeDecision || 'approved';

    if (!formData.decisionSummary.trim()) {
      setMessage('Error: Decision Summary is required.');
      return;
    }

    setIsSubmitting(true);
    setMessage('');

    try {
      const uploadedFiles = [];
      if (selectedFiles.length > 0) {
        try {
          for (const file of selectedFiles) {
            const uploadResult = await ApiService.uploadAttachment(file, ApiService.ticketFromTask(selectedTask));
            uploadedFiles.push({ name: uploadResult.fileName || file.name, url: uploadResult.url });
          }
        } catch (uploadError) {
          setMessage('Failed to upload decision document: ' + uploadError.message);
          setIsSubmitting(false);
          return;
        }
      }

      const payload = {
        committeeDecision: finalDecision,
        furtherReview: finalDecision === 'further_review',
        decisionDate: formData.decisionDate || new Date().toISOString().split('T')[0],
        approvingAuthority: formData.approvingAuthority,
        committeeExplanation: formData.decisionSummary,
        decisionSummary: formData.decisionSummary,
        committeeAttachment: uploadedFiles[0]?.url || '',
        committeeAttachmentName: uploadedFiles[0]?.name || '',
        committeeAttachments: uploadedFiles
      };

      await ApiService.completeTask(selectedTask.id, payload);

      const decisionMsg = finalDecision === 'further_review'
        ? 'Referred for further review and routed back for Audit Investigation!'
        : `Committee decision (${finalDecision.toUpperCase()}) recorded and decision documents uploaded successfully!`;

      setMessage(decisionMsg);
      setFormData({
        committeeDecision: 'approved',
        decisionDate: '',
        approvingAuthority: 'Complaints Review & Decision Committee',
        decisionSummary: '',
        committeeAttachment: '',
        committeeAttachmentName: ''
      });
      setSelectedFiles([]);
      setSelectedTask(null);
      await loadTasks(true);

      setTimeout(() => setMessage(''), 5000);
    } catch (error) {
      setMessage('Failed to submit committee decision: ' + (error.message || 'Unknown error'));
    } finally {
      setIsSubmitting(false);
    }
  };

  // Committee Review Queue Columns
  const reviewQueueColumns = [
    {
      title: 'Unique ID No',
      dataIndex: 'complaintId',
      key: 'complaintId',
      render: (text, record) => <UniqueIdBadge value={formatUniqueId(record || text)} fontSize="11px" padding="3px 8px" />
    },
    {
      title: 'Customer Name',
      dataIndex: 'customerName',
      key: 'customerName',
      render: (text, record) => customerDisplayName(record)
    },
    {
      title: 'Complaint Category',
      key: 'category',
      render: (_, r) => complaintCategoryLabel(r)
    },
    {
      title: 'Complaint Classification',
      key: 'priority',
      render: (_, r) => <ClassificationTag record={r} />
    },
    {
      title: 'Action',
      key: 'action',
      align: 'center',
      render: (_, r) => (
        <Tooltip title="Inspect & Record Decision">
          <Button
            type="text"
            size="small"
            icon={<EyeOutlined style={{ color: BRAND_COLORS.primary, fontSize: '16px' }} />}
            onClick={() => handleTaskSelect(r)}
          />
        </Tooltip>
      )
    }
  ];

  const filteredTasks = tasks.filter(t => matchesCommitteeSearch(t, searchQuery));
  const pendingReviewsCount = tasks.length;
  const alertIsSuccess = Boolean(message && (message.includes('recorded') || message.includes('success')));

  return (
    <DashboardLayout userRole="chief-committee">
      <Space direction="vertical" size="large" style={{ width: '100%', maxWidth: '1400px', margin: '0 auto' }}>

        {/* Page Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Title level={3} style={{ margin: 0, color: BRAND_COLORS.primary, fontWeight: 700 }}>
              Complaints Review & Decision Committee Workspace
            </Title>
            <Text type="secondary" style={{ fontSize: '13px' }}>
              Welcome, {user?.fullName || user?.username} (Committee Secretary) &bull; Decision Tracking & Policy Directives
            </Text>
          </div>
        </div>

        {message ? (
          <Alert
            message={alertIsSuccess ? 'Success' : 'Information'}
            description={message}
            type={alertIsSuccess ? 'success' : 'info'}
            showIcon
            closable
            onClose={() => setMessage('')}
          />
        ) : null}

        {/* SECTION 1: PENDING COMMITTEE REVIEWS SUMMARY CARD */}
        {!selectedTask && (
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} md={8}>
              <Card size="small" style={{ borderRadius: '8px' }}>
                <Text type="secondary" style={{ fontSize: '12px' }}>Pending Committee Reviews</Text>
                <Title level={3} style={{ margin: '4px 0 0 0', color: BRAND_COLORS.primary }}>{pendingReviewsCount}</Title>
              </Card>
            </Col>
          </Row>
        )}

        {/* SECTION 2: COMMITTEE REVIEWS LIST */}
        {!selectedTask ? (
          <Card size="small" style={{ borderRadius: '8px' }}>
            <div style={{ marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Title level={5} style={{ margin: 0, color: '#334155', fontWeight: 600 }}>
                Pending Reviews ({tasks.length})
              </Title>
              <Input
                placeholder="Search by Unique ID No, CM ticket, Customer Name..."
                prefix={<SearchOutlined style={{ color: '#475569' }} />}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                allowClear
                style={{ width: '320px', borderRadius: '6px' }}
              />
            </div>
            <Table
              dataSource={filteredTasks}
              columns={reviewQueueColumns}
              rowKey="id"
              pagination={{ pageSize: 8 }}
              size="small"
              loading={loading}
            />
          </Card>
        ) : (
          <CommitteeDecisionWorkspace
            selectedTask={selectedTask}
            formData={formData}
            setFormData={setFormData}
            handleFormChange={handleFormChange}
            handleFileChange={handleFileChange}
            handleDecisionSubmit={handleDecisionSubmit}
            selectedFiles={selectedFiles}
            isSubmitting={isSubmitting}
            onBack={() => setSelectedTask(null)}
          />
        )}

      </Space>
    </DashboardLayout>
  );
}

export default ChiefCommitteeDashboard;
