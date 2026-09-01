import React, { useState, useEffect } from 'react';
import { Card, Typography, Row, Col, Table, Tag, Button, Modal, Space, Input, Empty, Tooltip, Upload, message as antMessage } from 'antd';
import { EyeOutlined, SendOutlined, PaperClipOutlined, UploadOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import { formatUniqueId, formatIntakeId } from '../components/TaskTable';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import { useAuth } from '../contexts/AuthContext';

const { Title, Text, Paragraph } = Typography;

const recordVars = (record) => record?.variables || {};

function isInvestigationReviewTask(task) {
  if (task.definitionKey === 'FormTask_43') {
    return false;
  }
  const vars = recordVars(task);
  const currentStage = vars.currentStage || vars.stage;
  if (['CHIEF_OPERATION_AUDIT', 'AUDIT_INVESTIGATION', 'COMMITTEE_REVIEW', 'COMPLETED', 'RESOLVED', 'CLOSED'].includes(currentStage)) {
    return false;
  }
  const isInvestigationRequired = vars.requiresInvestigation === true;
  return isInvestigationRequired && (
    currentStage === 'CHIEF_EXPERIENCE_REVIEW' ||
    currentStage === 'CHIEF_EXPERIENCE' ||
    task.definitionKey === 'FormTask_CEX' ||
    !currentStage
  );
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

function InvestigationInspectionBody({ selectedTask, cxRemarks, setCxRemarks, cxFileList, setCxFileList }) {
  const vars = recordVars(selectedTask);
  const uniqueId = formatUniqueId(selectedTask);
  const intakeId = formatIntakeId(selectedTask);
  const showIntake = Boolean(intakeId && intakeId !== uniqueId);
  const officerNotes = vars.notes || vars.additionalRemarks || selectedTask.description;
  const investigationFiles = vars.investigationFiles || [];
  const hasFiles = investigationFiles.length > 0;

  return (
    <div style={{ paddingTop: '8px' }}>
      <Row gutter={[16, 16]} style={{ marginBottom: '16px', backgroundColor: '#f8fafc', padding: '16px', borderRadius: '8px', border: '1px solid #f1f5f9' }}>
        <Col span={12}>
          <Text type="secondary" style={{ fontSize: '12px', display: 'block', marginBottom: '2px' }}>Unique ID No</Text>
          <Text code style={{ color: BRAND_COLORS.primary, fontWeight: 600, fontSize: '13px' }}>
            {uniqueId}
          </Text>
          {showIntake ? (
            <Text type="secondary" style={{ fontSize: '11px', display: 'block', marginTop: '2px' }}>
              Intake {intakeId}
            </Text>
          ) : null}
        </Col>
        <Col span={12}>
          <Text type="secondary" style={{ fontSize: '12px', display: 'block', marginBottom: '2px' }}>Customer Name</Text>
          <Text strong style={{ fontSize: '13px', color: '#0f172a' }}>
            {customerNameOf(selectedTask)}
          </Text>
        </Col>
        <Col span={12}>
          <Text type="secondary" style={{ fontSize: '12px', display: 'block', marginBottom: '2px' }}>Complaint Classification</Text>
          <div>
            <ClassificationTag record={selectedTask} />
          </div>
        </Col>
        <Col span={12}>
          <Text type="secondary" style={{ fontSize: '12px', display: 'block', marginBottom: '2px' }}>Complaint Category</Text>
          <Text style={{ fontSize: '13px', color: '#334155' }}>
            {vars.complaintCategory || 'CUSTOMER SERVICE ISSUES'}
          </Text>
        </Col>
      </Row>

      {officerNotes ? (
        <div style={{ marginBottom: '16px', padding: '12px', backgroundColor: '#fff', border: '1px solid #e2e8f0', borderRadius: '8px' }}>
          <Text strong style={{ color: '#0f172a', fontSize: '13px', display: 'block', marginBottom: '4px' }}>
            Customer Care Officer Notes &amp; Narrative:
          </Text>
          <Paragraph style={{ margin: 0, color: '#475569', fontSize: '13px' }}>
            {officerNotes}
          </Paragraph>
        </div>
      ) : null}

      <div style={{ marginBottom: '20px', padding: '14px', backgroundColor: '#f0f9ff', border: '1px solid #bae6fd', borderRadius: '8px' }}>
        <Text strong style={{ color: '#0369a1', fontSize: '13px', display: 'block', marginBottom: '8px' }}>
          <PaperClipOutlined style={{ marginRight: '6px' }} />
          Attached Evidence &amp; Documents ({investigationFiles.length} Attached):
        </Text>
        {hasFiles ? (
          <Space direction="vertical" style={{ width: '100%' }}>
            {investigationFiles.map((file, idx) => (
              <div key={file.uid || file.name || idx} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#fff', padding: '8px 12px', borderRadius: '6px', border: '1px solid #e0f2fe' }}>
                <Space>
                  <PaperClipOutlined style={{ color: '#0284c7' }} />
                  <Text strong style={{ fontSize: '13px', color: '#0f172a' }}>{file.name}</Text>
                  {file.size ? <Text type="secondary" style={{ fontSize: '11px' }}>({Math.round(file.size / 1024)} KB)</Text> : null}
                </Space>
                {file.url ? (
                  <a href={file.url} download={file.name} target="_blank" rel="noreferrer">
                    <Button size="small" type="link" style={{ padding: 0 }}>
                      View / Download
                    </Button>
                  </a>
                ) : null}
              </div>
            ))}
          </Space>
        ) : (
          <Text type="secondary" style={{ fontSize: '12px', fontStyle: 'italic' }}>
            No physical file attachments provided by Customer Care Officer.
          </Text>
        )}
      </div>

      <div style={{ marginBottom: '16px' }}>
        <Text strong style={{ color: '#0f172a', fontSize: '14px', display: 'block', marginBottom: '8px' }}>
          Chief Experience Officer Remarks / Instructions:
        </Text>
        <Input.TextArea
          rows={3}
          value={cxRemarks}
          onChange={(e) => setCxRemarks(e.target.value)}
          placeholder="Enter investigation assessment or instructions for Chief Operation Audit..."
          style={{ borderRadius: '6px', padding: '10px' }}
        />
      </div>

      <div style={{ marginBottom: '8px' }}>
        <Text strong style={{ color: '#0f172a', fontSize: '14px', display: 'block', marginBottom: '8px' }}>
          <PaperClipOutlined style={{ marginRight: '6px', color: '#0284c7' }} />
          Attach Supporting Documents / Files:
        </Text>
        <Upload
          fileList={cxFileList}
          beforeUpload={() => false}
          onChange={({ fileList }) => setCxFileList(fileList)}
          multiple
        >
          <Button icon={<UploadOutlined />} style={{ borderRadius: '6px' }}>
            Select Supporting Files to Attach
          </Button>
        </Upload>
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
  const [isEscalating, setIsEscalating] = useState(false);

  useEffect(() => {
    loadData();
    const interval = setInterval(loadData, 5000);
    return () => clearInterval(interval);
  }, []);

  const loadData = async () => {
    try {
      const enrichedTasks = await ApiService.getEnrichedTasks().catch(() => []);

      // Filter tasks sent from screening requiring investigation for Chief Experience Officer review
      const tasksToReview = (enrichedTasks || []).filter(isInvestigationReviewTask);
      setInvestigationTasks(tasksToReview);
    } catch (err) {
      console.error('Failed to load executive metrics or investigation tasks:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleEscalateToAudit = async (task) => {
    if (!task) return;
    setIsEscalating(true);
    try {
      const serializableCxFiles = cxFileList.map(f => ({
        uid: f.uid || f.name,
        name: f.name,
        size: f.size,
        type: f.type,
        url: f.thumbUrl || f.url || null
      }));

      const existingFiles = recordVars(task).investigationFiles || [];
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
        <Tooltip title="Inspect Case">
          <Button
            type="text"
            size="small"
            icon={<EyeOutlined style={{ color: BRAND_COLORS.primary, fontSize: '18px' }} />}
            onClick={() => {
              setSelectedTask(r);
              setCxRemarks('');
              setCxFileList([]);
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
            Welcome, {user?.fullName || executiveTitle} &bull; Executive Process Oversight &amp; Investigation Escalation
          </Text>
        </div>

        {/* SECTION: INVESTIGATION ESCALATIONS QUEUE (TASKS FROM CUSTOMER CARE OFFICER) */}
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
          {investigationTasks.length === 0 ? (
            <Empty description="No complaints currently pending Chief Experience Officer investigation review." />
          ) : (
            <Table
              dataSource={investigationTasks}
              columns={investigationColumns}
              rowKey={(r) => r.id || recordVars(r).complaintId}
              loading={loading}
              pagination={{ pageSize: 5 }}
              size="small"
            />
          )}
        </Card>

        {/* Comprehensive Complaint Inspection & Escalation Modal */}
        <Modal
          title={
            <span style={{ fontSize: '16px', fontWeight: 600, color: '#0f172a' }}>
              Complaint Investigation Inspection
            </span>
          }
          open={Boolean(selectedTask)}
          onCancel={() => {
            setSelectedTask(null);
            setCxFileList([]);
          }}
          footer={[
            <Button key="cancel" onClick={() => { setSelectedTask(null); setCxFileList([]); }}>
              Cancel
            </Button>,
            <Button
              key="submit"
              type="primary"
              loading={isEscalating}
              icon={<SendOutlined />}
              style={{ backgroundColor: '#0284c7', borderColor: '#0284c7', borderRadius: '6px' }}
              onClick={() => handleEscalateToAudit(selectedTask)}
            >
              Escalate to Chief Operation Audit
            </Button>
          ]}
          width="100%"
          style={{ maxWidth: 750 }}
        >
          {selectedTask ? (
            <InvestigationInspectionBody
              selectedTask={selectedTask}
              cxRemarks={cxRemarks}
              setCxRemarks={setCxRemarks}
              cxFileList={cxFileList}
              setCxFileList={setCxFileList}
            />
          ) : null}

        </Modal>

      </Space>
    </DashboardLayout>
  );
}

export default ExecutiveDashboard;
