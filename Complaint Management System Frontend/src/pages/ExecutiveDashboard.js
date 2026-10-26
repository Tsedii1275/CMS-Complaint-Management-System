import React, { useState, useEffect } from 'react';
import { Card, Typography, Table, Tag, Button, Modal, Space, Input, Empty, Tooltip, Upload, Alert, message as antMessage } from 'antd';
import { EyeOutlined, SendOutlined, PaperClipOutlined, UploadOutlined, RollbackOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import InvestigationCaseSummary from '../components/InvestigationCaseSummary';
import { formatUniqueId } from '../components/TaskTable';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import { useAuth } from '../contexts/AuthContext';
import {
  TASK_CXO,
  CXO_SEND_TO_AUDIT,
  CXO_RETURN_TO_SENIOR_MANAGER,
  buildCxoDecisionPayload
} from '../utils/investigationWorkflow';

const { Title, Text } = Typography;

const recordVars = (record) => record?.variables || {};

// The Chief Experience Officer only works the FormTask_CEX decision. By the time a case gets here it
// has already been reviewed by the Customer Care Senior Manager and the Service Quality Department.
function isChiefExperienceReviewTask(task) {
  return task?.definitionKey === TASK_CXO;
}

function customerNameOf(record) {
  return recordVars(record).customerName || record.customerName || 'N/A';
}

function ClassificationTag({ record }) {
  const p = recordVars(record).priorityLevel || record.priority || 'General';
  const str = (p || '').toUpperCase();
  if (str.includes('HIGH')) return <Tag color="red">Highly Sensitive</Tag>;
  if (str.includes('SENSITIVE')) return <Tag color="orange">Sensitive</Tag>;
  return <Tag color="green">General</Tag>;
}

function CxoDecisionBody({
  selectedTask, cxRemarks, setCxRemarks, cxFileList, setCxFileList, returnReason, setReturnReason, returnError
}) {
  return (
    <div style={{ paddingTop: '8px' }}>
      <InvestigationCaseSummary task={selectedTask} />

      <div style={{ marginBottom: '16px' }}>
        <Text strong style={{ color: '#0f172a', fontSize: '14px', display: 'block', marginBottom: '8px' }}>
          Chief Experience Officer Remarks / Instructions (for Send to Audit Investigation):
        </Text>
        <Input.TextArea
          rows={3}
          value={cxRemarks}
          onChange={(e) => setCxRemarks(e.target.value)}
          placeholder="Enter investigation assessment or instructions for the Audit Investigation Team..."
          style={{ borderRadius: '6px', padding: '10px' }}
        />
      </div>

      <div style={{ marginBottom: '16px' }}>
        <Text strong style={{ color: '#0f172a', fontSize: '14px', display: 'block', marginBottom: '8px' }}>
          <PaperClipOutlined style={{ marginRight: '6px', color: '#0284c7' }} />
          Attach Supporting Documents / Files:
        </Text>
        <Upload
          fileList={cxFileList}
          beforeUpload={() => false}
          accept=".pdf,.jpg,.jpeg,.png,.docx,.xlsx,.mp3,.wav,.m4a"
          onChange={({ fileList }) => setCxFileList(fileList)}
          multiple
        >
          <Button icon={<UploadOutlined />} style={{ borderRadius: '6px' }}>
            Select Supporting Files to Attach
          </Button>
        </Upload>
      </div>

      <div style={{ padding: '12px', border: '1px solid #fecaca', background: '#fef2f2', borderRadius: '8px' }}>
        <Text strong style={{ color: '#991b1b', fontSize: '14px', display: 'block', marginBottom: '8px' }}>
          Reason for returning to the Customer Care Senior Manager (required only when returning):
        </Text>
        <Input.TextArea
          rows={2}
          value={returnReason}
          status={returnError ? 'error' : undefined}
          onChange={(e) => setReturnReason(e.target.value)}
          placeholder="Explain why this case does not need an audit investigation and can be closed..."
          style={{ borderRadius: '6px' }}
        />
        {returnError ? (
          <Text type="danger" style={{ fontSize: '12px' }}>{returnError}</Text>
        ) : null}
      </div>
    </div>
  );
}

function ExecutiveDashboard() {
  const { user } = useAuth();
  const [investigationTasks, setInvestigationTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedTask, setSelectedTask] = useState(null);
  const [cxRemarks, setCxRemarks] = useState('');
  const [cxFileList, setCxFileList] = useState([]);
  const [returnReason, setReturnReason] = useState('');
  const [returnError, setReturnError] = useState('');
  const [submittingDecision, setSubmittingDecision] = useState('');

  useEffect(() => {
    loadData();
    const interval = setInterval(loadData, 5000);
    return () => clearInterval(interval);
  }, []);

  const loadData = async () => {
    try {
      const enrichedTasks = await ApiService.getEnrichedTasks().catch(() => []);
      setInvestigationTasks((enrichedTasks || []).filter(isChiefExperienceReviewTask));
    } catch (err) {
      console.error('Failed to load executive investigation tasks:', err);
    } finally {
      setLoading(false);
    }
  };

  const resetDecisionForm = () => {
    setSelectedTask(null);
    setCxRemarks('');
    setCxFileList([]);
    setReturnReason('');
    setReturnError('');
  };

  const submitDecision = async (task, decision) => {
    if (!task) return;
    if (decision === CXO_RETURN_TO_SENIOR_MANAGER && !returnReason.trim()) {
      setReturnError('A reason is required to return the case to the Customer Care Senior Manager.');
      return;
    }
    setReturnError('');
    setSubmittingDecision(decision);
    try {
      const uploadedFiles = await ApiService.uploadAttachmentList(
        cxFileList,
        ApiService.ticketFromTask(task)
      );
      const payload = buildCxoDecisionPayload(decision, {
        remarks: cxRemarks,
        reason: returnReason,
        files: uploadedFiles,
        existingFiles: recordVars(task).investigationFiles || []
      });
      await ApiService.completeTask(task.id, payload);
      antMessage.success(decision === CXO_RETURN_TO_SENIOR_MANAGER
        ? 'Case returned to the Customer Care Senior Manager for closure without audit.'
        : 'Case sent to the Audit Investigation Team.');
      resetDecisionForm();
      await loadData();
    } catch (err) {
      antMessage.error('Failed to submit decision: ' + (err.message || 'Unknown error'));
    } finally {
      setSubmittingDecision('');
    }
  };

  const executiveTitle = (() => {
    const r = user?.role || '';
    if (r.includes('CHIEF_EXPERIENCE') || r.includes('ROLE_CHIEF_EXPERIENCE_OFFICER')) return 'Chief Experience Officer';
    return user?.fullName || 'Executive Leader';
  })();

  const investigationColumns = [
    {
      title: 'Unique ID No',
      key: 'complaintId',
      render: (_, r) => {
        return <strong style={{ color: BRAND_COLORS.primary, fontFamily: 'monospace' }}>{formatUniqueId(r)}</strong>;
      }
    },
    {
      title: 'Customer Name',
      key: 'customerName',
      render: (_, r) => customerNameOf(r)
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
        <Tooltip title="Review Case">
          <Button
            type="text"
            size="small"
            icon={<EyeOutlined style={{ color: BRAND_COLORS.primary, fontSize: '18px' }} />}
            onClick={() => {
              setSelectedTask(r);
              setCxRemarks('');
              setCxFileList([]);
              setReturnReason('');
              setReturnError('');
            }}
          />
        </Tooltip>
      )
    }
  ];

  return (
    <DashboardLayout userRole="executive-dashboard">
      <Space direction="vertical" size="large" style={{ width: '100%', maxWidth: '1400px', margin: '0 auto' }}>

        {/* Page Header */}
        <div>
          <Title level={3} style={{ color: BRAND_COLORS.primary, margin: 0, fontWeight: 700 }}>
            Executive Monitoring Workspace
          </Title>
          <Text type="secondary" style={{ fontSize: '13px' }}>
            Welcome, {user?.fullName || executiveTitle} &bull; Executive Process Oversight &amp; Investigation Decisions
          </Text>
        </div>

        <Card
          title={
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ fontWeight: 600, color: BRAND_COLORS.primary, fontSize: '15px' }}>
                Complaints Requiring Investigation
              </span>
              {investigationTasks.length > 0 && (
                <Tag color="red" style={{ borderRadius: '10px' }}>{investigationTasks.length} Pending Review</Tag>
              )}
            </div>
          }
          size="small"
          style={{ borderRadius: '8px' }}
        >
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: '12px' }}
            message="These cases were reviewed by the Customer Care Senior Manager and the Service Quality Department. Send each case to the Audit Investigation Team, or return it to the Customer Care Senior Manager for closure without audit."
          />
          {investigationTasks.length === 0 ? (
            <Empty description="No complaints currently pending Chief Experience Officer review." />
          ) : (
            <Table
              dataSource={investigationTasks}
              columns={investigationColumns}
              rowKey={(r) => r.id || recordVars(r).complaintId}
              loading={loading}
              pagination={{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50'] }}
              size="small"
            />
          )}
        </Card>

        <Modal
          title={
            <span style={{ fontSize: '16px', fontWeight: 600, color: '#0f172a' }}>
              Chief Experience Officer Review
            </span>
          }
          open={Boolean(selectedTask)}
          onCancel={resetDecisionForm}
          footer={[
            <Button key="cancel" onClick={resetDecisionForm} disabled={Boolean(submittingDecision)}>
              Cancel
            </Button>,
            <Button
              key="return"
              danger
              icon={<RollbackOutlined />}
              loading={submittingDecision === CXO_RETURN_TO_SENIOR_MANAGER}
              disabled={Boolean(submittingDecision) && submittingDecision !== CXO_RETURN_TO_SENIOR_MANAGER}
              style={{ borderRadius: '6px' }}
              onClick={() => submitDecision(selectedTask, CXO_RETURN_TO_SENIOR_MANAGER)}
            >
              Return to Customer Care Senior Manager
            </Button>,
            <Button
              key="audit"
              type="primary"
              icon={<SendOutlined />}
              loading={submittingDecision === CXO_SEND_TO_AUDIT}
              disabled={Boolean(submittingDecision) && submittingDecision !== CXO_SEND_TO_AUDIT}
              style={{ backgroundColor: '#0284c7', borderColor: '#0284c7', borderRadius: '6px' }}
              onClick={() => submitDecision(selectedTask, CXO_SEND_TO_AUDIT)}
            >
              Send to Audit Investigation
            </Button>
          ]}
          width="100%"
          style={{ maxWidth: 800 }}
        >
          {selectedTask ? (
            <CxoDecisionBody
              selectedTask={selectedTask}
              cxRemarks={cxRemarks}
              setCxRemarks={setCxRemarks}
              cxFileList={cxFileList}
              setCxFileList={setCxFileList}
              returnReason={returnReason}
              setReturnReason={setReturnReason}
              returnError={returnError}
            />
          ) : null}
        </Modal>

      </Space>
    </DashboardLayout>
  );
}

export default ExecutiveDashboard;
