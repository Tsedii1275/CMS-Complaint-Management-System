import React, { useState, useEffect } from 'react';
import { Card, Typography, Button, Alert, Form, Input, Space, Row, Col } from 'antd';
import { ArrowLeftOutlined, SearchOutlined, PaperClipOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import TaskTable, { formatUniqueId, formatIntakeId, matchesTicketSearch } from '../components/TaskTable';
import { useAuth } from '../contexts/AuthContext';
import { renderComplaintStatusTag } from '../utils/statusUtils';

const { Title, Text } = Typography;

const PRIMARY = BRAND_COLORS.primary;

const pageStackStyle = { width: '100%', maxWidth: '1300px', margin: '0 auto' };
const headerRowStyle = { display: 'flex', justifyContent: 'space-between', alignItems: 'center' };
const pageTitleStyle = { margin: 0, color: PRIMARY, fontWeight: 700 };
const welcomeStyle = { fontSize: '13px' };
const cardRoundStyle = { borderRadius: '8px' };
const metricLabelStyle = { fontSize: '12px' };
const metricValueStyle = { margin: '4px 0 0 0', color: PRIMARY };
const cardTitleStyle = { fontWeight: 600, color: PRIMARY, fontSize: '15px' };
const searchWrapStyle = { marginBottom: '16px' };
const searchIconStyle = { color: '#475569' };
const searchInputStyle = { width: '320px', borderRadius: '6px' };
const backWrapStyle = { marginBottom: '16px' };
const warningAlertStyle = { marginBottom: '16px', borderRadius: '6px' };
const infoPanelStyle = {
  border: '1px solid #e2e8f0',
  borderRadius: '8px',
  padding: '20px',
  backgroundColor: '#f8fafc'
};
const panelHeadingStyle = { color: PRIMARY, marginTop: 0, marginBottom: '16px', fontWeight: 700 };
const fieldCaptionStyle = {
  fontSize: '12px',
  color: '#64748b',
  fontWeight: 500,
  display: 'block',
  marginBottom: '2px'
};
const ticketIdStyle = { color: PRIMARY, fontSize: '13px' };
const intakeIdStyle = { fontSize: '11px', display: 'block', marginTop: '2px' };
const fieldValueStyle = { fontSize: '13px', color: '#0f172a', fontWeight: 600 };
const cxBoxStyle = {
  marginTop: '8px',
  padding: '12px',
  backgroundColor: '#f0f9ff',
  border: '1px solid #bae6fd',
  borderRadius: '6px'
};
const cxHeadingStyle = { color: '#0369a1', fontSize: '12px', display: 'block' };
const cxTextStyle = { fontSize: '13px', color: '#0f172a' };
const cxAttachHeadingStyle = { color: '#0369a1', fontSize: '12px', display: 'block', marginBottom: '4px' };
const cxRemarkWrapStyle = { marginBottom: '8px' };
const paperClipIconStyle = { marginRight: '4px' };
const fullWidthStyle = { width: '100%' };
const fileRowStyle = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  backgroundColor: '#fff',
  padding: '4px 8px',
  borderRadius: '4px',
  border: '1px solid #e0f2fe'
};
const fileNameStyle = { fontSize: '12px', color: '#0f172a' };
const downloadLinkStyle = { padding: 0, fontSize: '11px' };
const textareaStyle = { borderRadius: '6px' };
const uploadBoxStyle = {
  background: '#ffffff',
  border: '1px dashed #cbd5e1',
  borderRadius: '6px',
  padding: '12px 16px',
  marginBottom: '16px'
};
const uploadLabelStyle = { fontSize: '13px', color: '#334155' };
const hiddenFileInputStyle = { display: 'none' };
const roundedButtonStyle = { borderRadius: '6px' };
const submitButtonStyle = {
  width: '100%',
  backgroundColor: PRIMARY,
  borderColor: PRIMARY,
  borderRadius: '6px',
  fontWeight: 600
};

function AuditDashboard() {
  const { user } = useAuth();
  const [tasks, setTasks] = useState([]);
  const [selectedTask, setSelectedTask] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  const [formData, setFormData] = useState({
    auditJustification: '',
    auditFindings: '',
    auditAttachment: '',
    auditAttachmentName: ''
  });
  const [selectedFile, setSelectedFile] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    loadTasks();

    const interval = setInterval(() => {
      loadTasks();
    }, 5000);

    return () => clearInterval(interval);
  }, []);

  const loadTasks = async () => {
    try {
      const tasksData = await ApiService.getEnrichedTasks();
      const auditTasks = (tasksData || []).filter(task => {
        const currentStage = task.variables?.currentStage || task.variables?.stage || '';
        const defKey = task.definitionKey || task.taskDefinitionKey || task.id || '';

        if (defKey === 'FormTask_ChiefCommittee') {
          return false;
        }

        if (['COMPLETED', 'RESOLVED', 'CLOSED', 'COMMITTEE_REVIEW', 'COMMITTEE_DECISION'].includes(currentStage)) {
          return false;
        }

        return (
          defKey === 'FormTask_48' ||
          defKey === 'UserTask_Audit' ||
          currentStage === 'CHIEF_OPERATION_AUDIT' ||
          currentStage === 'AUDIT_INVESTIGATION' ||
          currentStage === 'INVESTIGATION'
        );
      });
      setTasks(auditTasks);
    } catch (err) {
      console.error('Error loading tasks:', err);
    }
  };

  const handleTaskSelect = async (task) => {
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
    const file = e.target.files[0];
    if (file) {
      setSelectedFile(file);
      setFormData(prev => ({
        ...prev,
        auditAttachmentName: file.name
      }));
    }
  };

  const handleSubmit = async () => {
    if (!selectedTask) return;

    if (!formData.auditJustification.trim()) {
      setMessage('Error: Investigation Justification is required.');
      return;
    }

    setIsSubmitting(true);
    setMessage('');

    try {
      let uploadedUrl = formData.auditAttachment;
      let uploadedName = formData.auditAttachmentName;

      if (selectedFile) {
        try {
          const uploadResult = await ApiService.uploadEvidence(selectedFile);
          uploadedUrl = uploadResult.url;
          uploadedName = uploadResult.fileName;
        } catch (uploadError) {
          setMessage('Failed to upload attachment: ' + uploadError.message);
          setIsSubmitting(false);
          return;
        }
      }

      await ApiService.submitAuditResult(selectedTask.id, {
        auditJustification: formData.auditJustification,
        auditFindings: formData.auditFindings,
        auditAttachment: uploadedUrl,
        auditAttachmentName: uploadedName,
        currentStage: 'COMMITTEE_REVIEW',
        stage: 'COMMITTEE_DECISION',
        auditCompleted: true,
        escalatedToCommittee: true
      });

      setMessage('Audit investigation completed and case escalated to Complaints Review & Decision Committee!');
      setSelectedTask(null);
      setFormData({ auditJustification: '', auditFindings: '', auditAttachment: '', auditAttachmentName: '' });
      await loadTasks();

      setTimeout(() => setMessage(''), 5000);
    } catch (error) {
      setMessage('Failed to submit audit investigation: ' + (error.message || 'Unknown error'));
    } finally {
      setIsSubmitting(false);
    }
  };

  // METRIC CALCULATION
  const openInvestigationsCount = tasks.length;

  return (
    <DashboardLayout userRole="audit">
      <Space direction="vertical" size="large" style={pageStackStyle}>
        <div style={headerRowStyle}>
          <div>
            <Title level={3} style={pageTitleStyle}>
              Audit Workspace
            </Title>
            <Text type="secondary" style={welcomeStyle}>
              Welcome, {user?.fullName || user?.username} &bull; Operational Compliance & Investigation Oversight
            </Text>
          </div>
        </div>

        {message && (
          <Alert
            message={message.includes('success') || message.includes('completed') ? 'Success' : 'Information'}
            description={message}
            type={message.includes('success') || message.includes('completed') ? 'success' : 'info'}
            showIcon
            closable
            onClose={() => setMessage('')}
          />
        )}

        {!selectedTask && (
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} md={6}>
              <Card size="small" style={cardRoundStyle}>
                <Text type="secondary" style={metricLabelStyle}>Open</Text>
                <Title level={3} style={metricValueStyle}>{openInvestigationsCount}</Title>
              </Card>
            </Col>
          </Row>
        )}

        {!selectedTask ? (
          <Card
            title={
              <span style={cardTitleStyle}>
                Complaints Under Audit Review ({tasks.length})
              </span>
            }
            size="small"
            style={cardRoundStyle}
          >
            <div style={searchWrapStyle}>
              <Input
                placeholder="Search by Unique ID No, CM ticket, Customer, Description..."
                prefix={<SearchOutlined style={searchIconStyle} />}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                allowClear
                style={searchInputStyle}
              />
            </div>
            <TaskTable
              tasks={tasks.filter(t => {
                const q = searchQuery.toLowerCase();
                return !q || matchesTicketSearch(t, q) || (t.customerName || '').toLowerCase().includes(q);
              })}
              showAssignedOfficer={false}
              showAssignedDate={false}
              showOverallStatus={false}
              onSelectTask={handleTaskSelect}
            />
          </Card>
        ) : (
          <div>
            <div style={backWrapStyle}>
              <Button icon={<ArrowLeftOutlined />} onClick={() => setSelectedTask(null)} />
            </div>

            {((selectedTask?.variables?.committeeDecision === 'further_review' && selectedTask?.variables?.escalatedBy !== 'Chief Experience Officer') || selectedTask?.variables?.furtherReview === true || selectedTask?.variables?.referredByCommittee === true) && (
              <Alert
                message="Re-routed for Further Investigation"
                description={selectedTask.variables?.decisionSummary || selectedTask.variables?.committeeExplanation || "This complaint was reviewed by the Complaints Review & Decision Committee and referred back to Audit for further investigation."}
                type="warning"
                showIcon
                style={warningAlertStyle}
              />
            )}

            <Row gutter={[24, 24]}>
              <Col xs={24} lg={12}>
                <div style={infoPanelStyle}>
                  <Title level={5} style={panelHeadingStyle}>
                    Complaint Information
                  </Title>

                  <Row gutter={[16, 14]}>
                    <Col span={12}>
                      <Text style={fieldCaptionStyle}>Unique ID No</Text>
                      <strong style={ticketIdStyle}>
                        {formatUniqueId(selectedTask)}
                      </strong>
                      {formatIntakeId(selectedTask) && formatIntakeId(selectedTask) !== formatUniqueId(selectedTask) && (
                        <Text type="secondary" style={intakeIdStyle}>
                          Intake {formatIntakeId(selectedTask)}
                        </Text>
                      )}
                    </Col>
                    <Col span={12}>
                      <Text style={fieldCaptionStyle}>Customer Name</Text>
                      <Text style={fieldValueStyle}>{selectedTask.customerName}</Text>
                    </Col>

                    <Col span={12}>
                      <Text style={fieldCaptionStyle}>Contact Phone</Text>
                      <Text style={fieldValueStyle}>{selectedTask.variables?.customer?.phone || 'N/A'}</Text>
                    </Col>
                    <Col span={12}>
                      <Text style={fieldCaptionStyle}>Branch / Unit</Text>
                      <Text style={fieldValueStyle}>{selectedTask.variables?.complaint?.branch || 'Main Branch'}</Text>
                    </Col>
                    <Col span={12}>
                      <Text style={fieldCaptionStyle}>Overall Status</Text>
                      {renderComplaintStatusTag(selectedTask)}
                    </Col>

                    {(selectedTask.variables?.customer?.accountNumber || selectedTask.variables?.complaint?.accountNumber) && (
                      <Col span={12}>
                        <Text style={fieldCaptionStyle}>Account Number</Text>
                        <Text style={fieldValueStyle}>{selectedTask.variables?.customer?.accountNumber || selectedTask.variables?.complaint?.accountNumber}</Text>
                      </Col>
                    )}

                    <Col span={24}>
                      <Text style={fieldCaptionStyle}>Details of the Complaint</Text>
                      <Text style={fieldValueStyle}>{selectedTask.variables?.complaint?.description || 'N/A'}</Text>
                    </Col>

                    {(selectedTask.variables?.cxRemarks || (selectedTask.variables?.investigationFiles || []).length > 0) && (
                      <Col span={24}>
                        <div style={cxBoxStyle}>
                          {selectedTask.variables?.cxRemarks && (
                            <div style={cxRemarkWrapStyle}>
                              <Text strong style={cxHeadingStyle}>Chief Experience Officer Instructions:</Text>
                              <Text style={cxTextStyle}>{selectedTask.variables.cxRemarks}</Text>
                            </div>
                          )}
                          {(selectedTask.variables?.investigationFiles || []).length > 0 && (
                            <div>
                              <Text strong style={cxAttachHeadingStyle}>
                                <PaperClipOutlined style={paperClipIconStyle} />
                                Attached Evidence &amp; Documents ({selectedTask.variables.investigationFiles.length}):
                              </Text>
                              <Space direction="vertical" style={fullWidthStyle}>
                                {selectedTask.variables.investigationFiles.map((file, idx) => (
                                  <div key={file.uid || idx} style={fileRowStyle}>
                                    <Text style={fileNameStyle}>{file.name}</Text>
                                    {file.url && (
                                      <a href={file.url} download={file.name} target="_blank" rel="noreferrer">
                                        <Button size="small" type="link" style={downloadLinkStyle}>Download</Button>
                                      </a>
                                    )}
                                  </div>
                                ))}
                              </Space>
                            </div>
                          )}
                        </div>
                      </Col>
                    )}
                  </Row>
                </div>
              </Col>

              <Col xs={24} lg={12}>
                <div style={infoPanelStyle}>
                  <Title level={5} style={panelHeadingStyle}>
                    Audit Findings
                  </Title>

                  <Form layout="vertical" onFinish={handleSubmit}>
                    <Form.Item label="Findings" required>
                      <Input.TextArea
                        name="auditJustification"
                        value={formData.auditJustification}
                        onChange={handleFormChange}
                        rows={3}
                        placeholder="Enter compliance justification / recommendation details..."
                        style={textareaStyle}
                      />
                    </Form.Item>

                    <div style={uploadBoxStyle}>
                      <Space direction="vertical" style={fullWidthStyle}>
                        <Text strong style={uploadLabelStyle}>Upload Supporting Document</Text>
                        <Space>
                          <input
                            type="file"
                            onChange={handleFileChange}
                            style={hiddenFileInputStyle}
                            id="audit-document-upload"
                          />
                          <Button
                            icon={<PaperClipOutlined />}
                            onClick={() => document.getElementById('audit-document-upload').click()}
                            style={roundedButtonStyle}
                          >
                            {selectedFile ? 'Change Document' : 'Choose Document'}
                          </Button>
                          {selectedFile && <Text type="success">✓ {selectedFile.name}</Text>}
                        </Space>
                      </Space>
                    </div>

                    <Button type="primary" htmlType="submit" loading={isSubmitting} size="large" style={submitButtonStyle}>
                      Submit
                    </Button>
                  </Form>
                </div>
              </Col>
            </Row>
          </div>
        )}

      </Space>
    </DashboardLayout>
  );
}

export default AuditDashboard;
