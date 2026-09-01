import React, { useState, useEffect } from 'react';
import { Card, Typography, Button, Tag, Empty, Alert, Form, Input, Space, Row, Col, message as antdMessage } from 'antd';
import { DeleteOutlined, ArrowLeftOutlined, SearchOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import TaskTable, { formatUniqueId, formatIntakeId, matchesTicketSearch } from '../components/TaskTable';
import { useAuth } from '../contexts/AuthContext';

const { Title, Text, Paragraph } = Typography;

function WorkUnitDashboard() {
  const { user } = useAuth();
  const [tasks, setTasks] = useState([]);
  const [selectedTask, setSelectedTask] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [formData, setFormData] = useState({
    resolutionDetails: '',
    actionTaken: '',
    isSensitive: false
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [clearingTasks, setClearingTasks] = useState(false);

  useEffect(() => {
    loadTasks();

    const interval = setInterval(() => {
      loadTasks();
    }, 5000);

    return () => clearInterval(interval);
  }, []);

  const loadTasks = async (showLoading = false) => {
    try {
      if (showLoading === true) setLoading(true);
      const tasksData = await ApiService.getEnrichedTasks();
      const workUnitTasks = tasksData.filter(task => {
        const currentStage = task.variables?.currentStage || task.variables?.stage;
        const isWorkUnitTask = task.definitionKey === 'FormTask_57'
          || task.definitionKey === 'UserTask_WorkUnit'
          || task.definitionKey === 'SecondaryResolutionReview'
          || task.name?.includes('Secondary Resolution Review');
        if (!isWorkUnitTask && ['COMMITTEE_ACCEPTED', 'COMMITTEE_REJECTED', 'COMMITTEE_REVIEW', 'CHIEF_EXPERIENCE_REVIEW', 'CHIEF_OPERATION_AUDIT', 'CMD_SCREENING', 'COMPLETED', 'RESOLVED', 'CLOSED'].includes(currentStage)) {
          return false;
        }

        // Exclude Non-Complaint / "Other" / "Declined" cases
        const isComplaintVar = task.variables?.isComplaint;
        if (isComplaintVar === false || isComplaintVar === 'false') {
          return false;
        }

        const classification = (task.variables?.classification || task.variables?.complaintClassification || '').toUpperCase();
        if (['OTHER', 'DECLINED', 'INQUIRY', 'REQUEST'].includes(classification)) {
          return false;
        }

        const targetTab = task.variables?.targetTab;
        if (['Other', 'Declined'].includes(targetTab)) {
          return false;
        }

        // Non-complaints carry CM- ticket ID without a valid DBC- ticket ID
        const dbcTicketId = task.variables?.dbcTicketId;
        const complaintId = task.complaintId || task.variables?.complaintId || task.variables?.ticketId || task.variables?.uniqueIdNo || '';
        if (String(complaintId).startsWith('CM-') && (!dbcTicketId || !String(dbcTicketId).startsWith('DBC-'))) {
          return false;
        }

        return (
          task.definitionKey !== 'FormTask_43' &&
          task.definitionKey !== 'FormTask_ChiefCommittee' &&
          task.definitionKey !== 'FormTask_CEX' &&
          task.definitionKey !== 'FormTask_48' &&
          (
            task.definitionKey === 'FormTask_57' ||
            task.definitionKey === 'UserTask_WorkUnit' ||
            task.definitionKey === 'SecondaryResolutionReview' ||
            task.name?.includes('Secondary Resolution Review')
          )
        );
      });
      setTasks(workUnitTasks);
    } catch (err) {
      setError('Failed to load tasks');
      console.error('Error loading tasks:', err);
    } finally {
      setLoading(false);
    }
  };

  const clearAllTasks = async () => {
    if (window.confirm('Are you sure you want to clear all tasks? This action cannot be undone.')) {
      setClearingTasks(true);
      try {
        await ApiService.clearAllTasks();
        antdMessage.success('All tasks and SLA records cleared successfully!');
        setMessage('All tasks and SLA records cleared successfully!');
        setSelectedTask(null);
        setFormData({ resolutionDetails: '', actionTaken: '', isSensitive: false });
        await loadTasks();

        setTimeout(() => setMessage(''), 4000);
      } catch (err) {
        const detail = err?.message || 'Failed to clear all tasks';
        antdMessage.error(detail);
        setMessage(`error: ${detail}`);
        console.error('Error clearing tasks:', err);
      } finally {
        setClearingTasks(false);
      }
    }
  };

  const handleTaskSelect = async (task) => {
    setSelectedTask(task);
    setMessage('');
    try {
      await loadTasks();
    } catch (err) {
      setMessage('Failed to select task');
      console.error('Error selecting task:', err);
    }
  };

  const handleFormChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async () => {
    if (!selectedTask) return;

    setIsSubmitting(true);
    setMessage('');

    try {
      const variables = {
        resolutionDetails: formData.resolutionDetails,
        actionTaken: formData.actionTaken,
        workUnitResolutionGiven: true,
        currentStage: 'CCO_RESOLUTION_REVIEW',
        stage: 'CCO_RESOLUTION_REVIEW',
        status: 'RESOLUTION_GIVEN',
        requiresCcoReview: true
      };

      await ApiService.completeTask(selectedTask.id, variables);
      antdMessage.success('Resolution submitted successfully! Complaint returned to Customer Care Officer for review.');
      setMessage('Resolution submitted successfully! Complaint returned to Customer Care Officer for review.');
      setTimeout(() => setMessage(''), 4000);

      setFormData({ resolutionDetails: '', actionTaken: '' });
      setSelectedTask(null);
      await loadTasks();
    } catch (err) {
      antdMessage.error('Failed to complete task');
      setMessage('Failed to complete task');
      console.error('Error completing task:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '400px' }}>
      <Card loading={true} style={{ width: '100%', maxWidth: '600px' }} />
    </div>
  );

  if (error) return (
    <Alert
      message="Error"
      description={error}
      type="error"
      showIcon
      style={{ margin: '20px' }}
    />
  );

  const getPriorityColor = (priority) => {
    const p = priority?.toUpperCase() || '';
    if (p.includes('HIGH') || p.includes('CRITICAL')) return 'red';
    if (p.includes('SENSITIVE')) return 'orange';
    return 'green';
  };

  const getPriorityLabel = (priority) => {
    const p = priority?.toUpperCase() || '';
    if (p.includes('HIGH') || p.includes('CRITICAL')) return 'High Sensitive';
    if (p.includes('SENSITIVE')) return 'Sensitive';
    return 'Normal';
  };

  return (
    <DashboardLayout userRole="work-unit">
      <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
          <div>
            <Title level={2} style={{ margin: 0, color: BRAND_COLORS.primary }}>
              Hello, {user?.fullName || user?.username || ''}
            </Title>
            <Text type="secondary" style={{ fontSize: '15px' }}>
              Resolve assigned complaints
            </Text>
          </div>
          <div>
            <Button
              danger
              type="primary"
              icon={<DeleteOutlined />}
              onClick={clearAllTasks}
              loading={clearingTasks}
              style={{ borderRadius: '6px' }}
            >
              Clear All Tasks
            </Button>
          </div>
        </div>

        {message && (
          <Alert
            message={message.includes('success') ? 'Success' : 'Information'}
            description={message}
            type={message.includes('success') ? 'success' : 'info'}
            showIcon
            closable
            style={{ marginBottom: '24px' }}
            onClose={() => setMessage('')}
          />
        )}



        {!selectedTask ? (
          <div>
            {/* Search Input Bar (Above Tabs) */}
            <div style={{ marginBottom: '16px' }}>
              <Input
                className="pill-search-input"
                placeholder="Search by Unique ID No, CM ticket, Customer, Description..."
                prefix={<SearchOutlined style={{ color: '#475569', fontSize: '16px' }} />}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                allowClear
                style={{ width: '280px' }}
              />
            </div>

            {(() => {
              const query = searchQuery.toLowerCase().trim();
              const filtered = tasks.filter(task => {
                const customerName = task.customerName || '';
                const desc = task.variables?.complaint?.description || '';
                const cat = task.variables?.complaint?.category || '';
                return !query ||
                  matchesTicketSearch(task, query) ||
                  customerName.toLowerCase().includes(query) ||
                  desc.toLowerCase().includes(query) ||
                  cat.toLowerCase().includes(query);
              });

              if (filtered.length === 0) {
                return (
                  <Empty
                    description="No matching tasks found for selected view"
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    style={{ marginTop: '60px' }}
                  />
                );
              }

              return (
                <TaskTable
                  tasks={filtered}
                  onSelectTask={handleTaskSelect}
                  showAssignedOfficer={false}
                  showAssignedDate={false}
                  showOverallStatus={false}
                />
              );
            })()}
          </div>
        ) : (() => {
          const isSecondaryReview = Boolean(selectedTask.name?.includes('Secondary Resolution Review'));

          let submitButtonText = 'Resolve Complaint';
          if (isSubmitting) {
            submitButtonText = 'Processing...';
          } else if (isSecondaryReview) {
            submitButtonText = 'Submit Secondary Resolution';
          }

          let formTitle = "Resolution Action";
          if (isSecondaryReview) {
            formTitle = "Secondary Resolution Review & Action";
          } else if (user?.fullName) {
            formTitle = `${user.fullName}'s Resolution`;
          }

          return (
            <div>
              <Button icon={<ArrowLeftOutlined />} onClick={() => setSelectedTask(null)} style={{ marginBottom: '20px', borderRadius: '6px' }} />

              {isSecondaryReview && (
                <Alert
                  message="Resolution Disputed - Secondary Review Required"
                  description="The customer has rejected the previously provided resolution. Please investigate their feedback details below and formulate a corrective secondary resolution."
                  type="warning"
                  showIcon
                  closable={false}
                  style={{ marginBottom: '24px' }}
                />
              )}

              <Row gutter={[24, 24]}>
                <Col xs={24} lg={13}>
                  <div style={{
                    background: '#f8fafc',
                    borderRadius: '16px',
                    padding: '24px',
                    marginBottom: '20px',
                    border: '1px solid #f1f5f9'
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                      <Text style={{ fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
                        Complaint details
                      </Text>
                      <Tag color="blue" style={{ borderRadius: '6px', fontWeight: 600, margin: 0 }}>
                        {selectedTask.dbcTicketId || selectedTask.variables?.dbcTicketId || selectedTask.complaintId}
                      </Tag>
                    </div>

                    <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: '16px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Text style={{ color: '#64748b', fontSize: '14px' }}>Unique ID No</Text>
                        <div style={{ textAlign: 'right' }}>
                          <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>{formatUniqueId(selectedTask)}</Text>
                          {formatIntakeId(selectedTask) && formatIntakeId(selectedTask) !== formatUniqueId(selectedTask) && (
                            <Text type="secondary" style={{ fontSize: '12px', display: 'block' }}>
                              Intake {formatIntakeId(selectedTask)}
                            </Text>
                          )}
                        </div>
                      </div>

                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Text style={{ color: '#64748b', fontSize: '14px' }}>Customer Name</Text>
                        <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>{selectedTask.customerName}</Text>
                      </div>

                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Text style={{ color: '#64748b', fontSize: '14px' }}>Complaint Classification</Text>
                        <Tag color={getPriorityColor(selectedTask.priority)} style={{ margin: 0, fontWeight: 600 }}>
                          {getPriorityLabel(selectedTask.priority)}
                        </Tag>
                      </div>

                      {selectedTask.variables?.branch && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Text style={{ color: '#64748b', fontSize: '14px' }}>Branch</Text>
                          <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>{selectedTask.variables.branch}</Text>
                        </div>
                      )}

                      {selectedTask.variables?.department && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Text style={{ color: '#64748b', fontSize: '14px' }}>Department</Text>
                          <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>{selectedTask.variables.department}</Text>
                        </div>
                      )}

                      {(selectedTask.variables?.customer?.accountNumber || selectedTask.variables?.complaint?.accountNumber) && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Text style={{ color: '#64748b', fontSize: '14px' }}>Account Number</Text>
                          <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>
                            {selectedTask.variables?.customer?.accountNumber || selectedTask.variables?.complaint?.accountNumber}
                          </Text>
                        </div>
                      )}

                      {selectedTask.variables?.complaint && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                          <Text style={{ color: '#64748b', fontSize: '14px' }}>Details of the Complaint</Text>
                          <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 500, textAlign: 'right', maxWidth: '280px' }}>
                            {selectedTask.variables.complaint.description}
                          </Text>
                        </div>
                      )}

                      {selectedTask.variables?.complaint?.voiceAttachmentUrl && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px', paddingTop: '8px' }}>
                          <Text style={{ color: '#64748b', fontSize: '14px' }}>Voice Call Recording</Text>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <audio
                              src={ApiService.getAttachmentUrl(selectedTask.variables.complaint.voiceAttachmentUrl)}
                              controls
                              style={{ height: '32px', maxWidth: '200px' }}
                            >
                              <track kind="captions" />
                            </audio>
                            <Button
                              href={ApiService.getAttachmentUrl(selectedTask.variables.complaint.voiceAttachmentUrl)}
                              download={selectedTask.variables.complaint.voiceAttachmentName || 'voice_recording.mp3'}
                              target="_blank"
                              rel="noopener noreferrer"
                              size="small"
                            >
                              Download
                            </Button>
                          </div>
                        </div>
                      )}

                      {selectedTask.variables?.complaint?.evidenceUrl && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '8px' }}>
                          <Text style={{ color: '#64748b', fontSize: '14px' }}>Evidence Attachment</Text>
                          <a
                            href={ApiService.getAttachmentUrl(selectedTask.variables.complaint.evidenceUrl)}
                            download={selectedTask.variables.complaint.evidenceName || 'evidence_attachment'}
                            target="_blank"
                            rel="noopener noreferrer"
                            style={{ fontSize: '14px', fontWeight: 600, color: BRAND_COLORS.primary }}
                          >
                            {selectedTask.variables.complaint.evidenceName || 'Download Evidence'}
                          </a>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Customer Feedback Details */}
                  {isSecondaryReview && (
                    <div style={{ marginTop: '20px', paddingTop: '20px', borderTop: '1px solid #f0f0f0' }}>
                      <Text strong style={{ display: 'block', marginBottom: '12px', color: '#b78700' }}>
                        Customer Survey Feedback Details:
                      </Text>
                      <Space direction="vertical" size="small" style={{ width: '100%' }}>
                        <div>
                          <Text strong>Satisfaction Score (CSAT):</Text>{' '}
                          <Tag color="red">{selectedTask.variables?.csatScore || 'N/A'} / 5</Tag>
                        </div>
                        <div>
                          <Text strong>Net Promoter Score (NPS):</Text>{' '}
                          <Tag color="volcano">{selectedTask.variables?.npsScore || 'N/A'} / 10</Tag>
                        </div>
                        {selectedTask.variables?.npsComment && (
                          <div style={{ paddingLeft: '12px', borderLeft: '2px solid #d9d9d9' }}>
                            <Text type="secondary">NPS Comment:</Text>{' '}
                            <Text italic>"{selectedTask.variables.npsComment}"</Text>
                          </div>
                        )}
                        <div>
                          <Text strong>Customer Effort Score (CES):</Text>{' '}
                          <Tag color="orange">{selectedTask.variables?.cesScore || 'N/A'} / 7</Tag>
                        </div>
                        {selectedTask.variables?.cesComment && (
                          <div style={{ paddingLeft: '12px', borderLeft: '2px solid #d9d9d9' }}>
                            <Text type="secondary">CES Comment:</Text>{' '}
                            <Text italic>"{selectedTask.variables.cesComment}"</Text>
                          </div>
                        )}
                        {selectedTask.variables?.customerFeedbackComment && (
                          <div>
                            <Text strong>Customer Comments:</Text>
                            <Paragraph style={{ background: '#f5f5f5', padding: '8px 12px', borderRadius: '4px', marginTop: '4px' }}>
                              {selectedTask.variables.customerFeedbackComment}
                            </Paragraph>
                          </div>
                        )}
                      </Space>
                    </div>
                  )}
                </Col>

                <Col xs={24} lg={11}>
                  <div style={{
                    background: '#f8fafc',
                    borderRadius: '16px',
                    padding: '24px',
                    border: '1px solid #f1f5f9'
                  }}>
                    <div style={{ marginBottom: '16px' }}>
                      <Text style={{ fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
                        {formTitle}
                      </Text>
                    </div>

                    <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: '16px' }}>
                      <Form onFinish={handleSubmit} layout="vertical">
                        <Form.Item
                          label={<span style={{ color: '#475569', fontSize: '14px', fontWeight: 500 }}>{isSecondaryReview ? "Secondary Investigation / Corrective Action Findings" : "Resolution Details"}</span>}
                          required
                        >
                          <Input.TextArea
                            name="resolutionDetails"
                            value={formData.resolutionDetails}
                            onChange={(e) => handleFormChange(e)}
                            rows={5}
                            placeholder={isSecondaryReview ? "Enter details of manager secondary resolution findings..." : "Enter detailed resolution details..."}
                            style={{ borderRadius: '8px', border: '1px solid #cbd5e1' }}
                          />
                        </Form.Item>

                        <Form.Item
                          label={<span style={{ color: '#475569', fontSize: '14px', fontWeight: 500 }}>{isSecondaryReview ? "Action Taken (Optional)" : "Action Taken"}</span>}
                          required={!isSecondaryReview}
                        >
                          <Input.TextArea
                            name="actionTaken"
                            value={formData.actionTaken}
                            onChange={(e) => handleFormChange(e)}
                            rows={3}
                            placeholder="Describe actions taken to resolve the complaint..."
                            style={{ borderRadius: '8px', border: '1px solid #cbd5e1' }}
                          />
                        </Form.Item>



                        <div style={{ marginTop: '24px' }}>
                          <Button
                            type="primary"
                            htmlType="submit"
                            loading={isSubmitting}
                            size="large"
                            style={{
                              width: '100%',
                              borderRadius: '8px',
                              height: '44px',
                              fontWeight: 600,
                              background: isSecondaryReview ? '#faad14' : undefined,
                              borderColor: isSecondaryReview ? '#faad14' : undefined
                            }}
                          >
                            {submitButtonText}
                          </Button>
                        </div>
                      </Form>
                    </div>
                  </div>
                </Col>
              </Row>
            </div>
          );
        })()}
      </div>
    </DashboardLayout>
  );
}

export default WorkUnitDashboard;
