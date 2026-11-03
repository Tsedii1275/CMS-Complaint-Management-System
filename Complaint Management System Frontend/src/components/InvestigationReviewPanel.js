import React, { useState } from 'react';
import { Modal, Button, Input, Upload, Typography, Empty, Alert, message as antMessage } from 'antd';
import { UploadOutlined, SendOutlined, RollbackOutlined } from '@ant-design/icons';
import TaskTable from './TaskTable';
import InvestigationCaseSummary from './InvestigationCaseSummary';
import ApiService from '../services/api';
import {
  INVESTIGATION_STEPS,
  TASK_SQ_REVIEW,
  TASK_SQ_RETURN,
  TASK_SM_CLOSURE,
  SQ_RETURN_TO_SENIOR_MANAGER,
  SQ_SEND_TO_CXO,
  SM_CLOSE,
  SM_SEND_TO_SERVICE_QUALITY,
  buildReviewPayload,
  buildSqDecisionPayload,
  buildSmDecisionPayload,
  returnTagOf
} from '../utils/investigationWorkflow';

const { Text } = Typography;

const PRIMARY_ACTION = 'primary';
const RETURN_ACTION = 'return';

function stepOf(task) {
  return INVESTIGATION_STEPS[task?.definitionKey] || null;
}

function buildPayload(task, action, { remarks, reason, files }) {
  switch (task.definitionKey) {
    case TASK_SQ_REVIEW:
      return buildSqDecisionPayload(action === RETURN_ACTION ? SQ_RETURN_TO_SENIOR_MANAGER : SQ_SEND_TO_CXO,
        { remarks, reason, files });
    case TASK_SM_CLOSURE:
      return buildSmDecisionPayload(action === RETURN_ACTION ? SM_SEND_TO_SERVICE_QUALITY : SM_CLOSE,
        { summary: remarks, notes: reason, files });
    case TASK_SQ_RETURN:
      return { reviewFiles: [] };
    default:
      return buildReviewPayload(remarks, files);
  }
}

function ReturnedBanner({ task }) {
  const info = returnTagOf(task);
  if (!info) return null;
  return (
    <Alert
      type="warning"
      showIcon
      style={{ marginBottom: '16px', borderRadius: '8px' }}
      message={info.label}
      description={info.reason ? `${info.by ? `${info.by}: ` : ''}${info.reason}` : undefined}
    />
  );
}

/**
 * Work queue for the Audit Investigation review steps owned by the Customer Care Senior Manager
 * (first review and returned cases) and the Service Quality Department (review and returned cases).
 */
function InvestigationReviewPanel({ tasks = [], searchQuery = '', onCompleted }) {
  const [selectedTask, setSelectedTask] = useState(null);
  const [remarks, setRemarks] = useState('');
  const [reason, setReason] = useState('');
  const [fileList, setFileList] = useState([]);
  const [submitting, setSubmitting] = useState('');
  const [remarksError, setRemarksError] = useState('');
  const [reasonError, setReasonError] = useState('');

  const query = searchQuery.trim().toLowerCase();
  const visibleTasks = tasks.filter((task) => {
    if (!query) return true;
    const vars = task.variables || {};
    return [task.complaintId, task.dbcTicketId, task.generalTicketId, task.customerName, vars.customerName]
      .some((value) => typeof value === 'string' && value.toLowerCase().includes(query));
  });

  const step = stepOf(selectedTask);
  const hasReturnAction = Boolean(step?.returnLabel);

  const resetForm = () => {
    setRemarks('');
    setReason('');
    setFileList([]);
    setRemarksError('');
    setReasonError('');
  };

  const closeModal = () => {
    setSelectedTask(null);
    resetForm();
  };

  const handleSubmit = async (action) => {
    if (!selectedTask || !step) return;
    const returning = action === RETURN_ACTION;
    const closureNeedsSummary = step.closureSummaryRequired && !returning;
    if ((step.remarksRequired || closureNeedsSummary) && !returning && !remarks.trim()) {
      setRemarksError(`${step.remarksLabel} is required.`);
      return;
    }
    if (returning && !reason.trim()) {
      setReasonError(`${step.returnReasonLabel} is required.`);
      return;
    }
    setSubmitting(action);
    try {
      const uploaded = step.noInput
        ? []
        : await ApiService.uploadAttachmentList(fileList, ApiService.ticketFromTask(selectedTask));
      await ApiService.completeTask(selectedTask.id,
        buildPayload(selectedTask, action, { remarks, reason, files: uploaded }));
      antMessage.success(returning ? step.returnSuccessText : step.successText);
      closeModal();
      if (onCompleted) {
        await onCompleted();
      }
    } catch (err) {
      antMessage.error(err?.message || 'Failed to complete this review step.');
    } finally {
      setSubmitting('');
    }
  };

  if (visibleTasks.length === 0) {
    return (
      <Empty
        description="No cases are waiting for your investigation review."
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        style={{ marginTop: '60px' }}
      />
    );
  }

  const busy = Boolean(submitting);
  const footer = [
    <Button key="cancel" onClick={closeModal} disabled={busy}>Cancel</Button>
  ];
  if (hasReturnAction) {
    footer.push(
      <Button
        key="return"
        danger
        icon={<RollbackOutlined />}
        loading={submitting === RETURN_ACTION}
        disabled={busy && submitting !== RETURN_ACTION}
        style={{ borderRadius: '6px' }}
        onClick={() => handleSubmit(RETURN_ACTION)}
      >
        {step.returnLabel}
      </Button>
    );
  }
  footer.push(
    <Button
      key="submit"
      type="primary"
      icon={<SendOutlined />}
      loading={submitting === PRIMARY_ACTION}
      disabled={busy && submitting !== PRIMARY_ACTION}
      onClick={() => handleSubmit(PRIMARY_ACTION)}
    >
      {step?.actionLabel || 'Submit'}
    </Button>
  );

  return (
    <>
      <TaskTable
        tasks={visibleTasks}
        listKey={`investigation|${query}`}
        onSelectTask={(task) => {
          setSelectedTask(task);
          resetForm();
        }}
        showPriority
        showAssignedOfficer={false}
        showAssignedDate={false}
        showOverallStatus={false}
      />
      <Modal
        title={<span style={{ fontWeight: 600 }}>{step?.modalTitle || 'Investigation Review'}</span>}
        open={Boolean(selectedTask)}
        onCancel={closeModal}
        destroyOnClose
        width="100%"
        style={{ maxWidth: 760 }}
        footer={footer}
      >
        {selectedTask && step ? (
          <div>
            <Text type="secondary" style={{ display: 'block', marginBottom: '12px', fontSize: '13px' }}>
              Next: {step.nextOwner}
            </Text>
            <ReturnedBanner task={selectedTask} />
            <InvestigationCaseSummary task={selectedTask} />
            {step.noInput ? null : (
              <>
                <div style={{ marginBottom: '16px' }}>
                  <Text style={{ display: 'block', marginBottom: '6px', color: '#334155' }}>
                    {step.remarksLabel}
                    {step.remarksRequired ? <span style={{ color: '#dc2626' }}> *</span> : null}
                    {step.closureSummaryRequired ? (
                      <Text type="secondary"> (required only when closing)</Text>
                    ) : null}
                  </Text>
                  <Input.TextArea
                    rows={3}
                    value={remarks}
                    status={remarksError ? 'error' : undefined}
                    onChange={(e) => {
                      setRemarks(e.target.value);
                      if (remarksError) setRemarksError('');
                    }}
                    placeholder={step.remarksPlaceholder}
                    style={{ borderRadius: '8px' }}
                  />
                  {remarksError ? (
                    <Text type="danger" style={{ fontSize: '12px' }}>{remarksError}</Text>
                  ) : null}
                </div>
                {hasReturnAction ? (
                  <div style={{ marginBottom: '16px' }}>
                    <Text style={{ display: 'block', marginBottom: '6px', color: '#334155' }}>
                      {step.returnReasonLabel} <Text type="secondary">{step.returnReasonHint}</Text>
                    </Text>
                    <Input.TextArea
                      rows={2}
                      value={reason}
                      status={reasonError ? 'error' : undefined}
                      onChange={(e) => {
                        setReason(e.target.value);
                        if (reasonError) setReasonError('');
                      }}
                      placeholder={step.returnReasonPlaceholder}
                      style={{ borderRadius: '8px' }}
                    />
                    {reasonError ? (
                      <Text type="danger" style={{ fontSize: '12px' }}>{reasonError}</Text>
                    ) : null}
                  </div>
                ) : null}
                <div>
                  <Text style={{ display: 'block', marginBottom: '6px', color: '#334155' }}>
                    Attach documents (optional)
                  </Text>
                  <Upload
                    multiple
                    fileList={fileList}
                    beforeUpload={() => false}
                    accept=".pdf,.jpg,.jpeg,.png,.docx,.xlsx,.mp3,.wav,.m4a"
                    onChange={({ fileList: next }) => setFileList(next)}
                  >
                    <Button icon={<UploadOutlined />}>Select files</Button>
                  </Upload>
                </div>
              </>
            )}
          </div>
        ) : null}
      </Modal>
    </>
  );
}

export default InvestigationReviewPanel;
