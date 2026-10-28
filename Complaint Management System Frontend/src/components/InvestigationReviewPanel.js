import React, { useState } from 'react';
import { Modal, Button, Input, Upload, Typography, Empty, message as antMessage } from 'antd';
import { UploadOutlined, SendOutlined } from '@ant-design/icons';
import TaskTable from './TaskTable';
import InvestigationCaseSummary from './InvestigationCaseSummary';
import ApiService from '../services/api';
import { INVESTIGATION_STEPS, buildReviewPayload } from '../utils/investigationWorkflow';

const { Text } = Typography;

function stepOf(task) {
  return INVESTIGATION_STEPS[task?.definitionKey] || null;
}

/**
 * Work queue for the Audit Investigation review steps owned by the Customer Care Senior Manager
 * (review and closure) and the Service Quality Department (review).
 */
function InvestigationReviewPanel({ tasks = [], searchQuery = '', onCompleted }) {
  const [selectedTask, setSelectedTask] = useState(null);
  const [remarks, setRemarks] = useState('');
  const [fileList, setFileList] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [remarksError, setRemarksError] = useState('');

  const query = searchQuery.trim().toLowerCase();
  const visibleTasks = tasks.filter((task) => {
    if (!query) return true;
    const vars = task.variables || {};
    return [task.complaintId, task.dbcTicketId, task.generalTicketId, task.customerName, vars.customerName]
      .some((value) => typeof value === 'string' && value.toLowerCase().includes(query));
  });

  const step = stepOf(selectedTask);

  const closeModal = () => {
    setSelectedTask(null);
    setRemarks('');
    setFileList([]);
    setRemarksError('');
  };

  const handleSubmit = async () => {
    if (!selectedTask || !step) return;
    if (step.remarksRequired && !remarks.trim()) {
      setRemarksError(`${step.remarksLabel} is required.`);
      return;
    }
    setSubmitting(true);
    try {
      const uploaded = await ApiService.uploadAttachmentList(fileList, ApiService.ticketFromTask(selectedTask));
      await ApiService.completeTask(selectedTask.id, buildReviewPayload(remarks, uploaded));
      antMessage.success(step.successText);
      closeModal();
      if (onCompleted) {
        await onCompleted();
      }
    } catch (err) {
      antMessage.error(err?.message || 'Failed to complete this review step.');
    } finally {
      setSubmitting(false);
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

  return (
    <>
      <TaskTable
        tasks={visibleTasks}
        listKey={`investigation|${query}`}
        onSelectTask={(task) => {
          setSelectedTask(task);
          setRemarks('');
          setFileList([]);
          setRemarksError('');
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
        footer={[
          <Button key="cancel" onClick={closeModal}>Cancel</Button>,
          <Button
            key="submit"
            type="primary"
            icon={<SendOutlined />}
            loading={submitting}
            onClick={handleSubmit}
          >
            {step?.actionLabel || 'Submit'}
          </Button>
        ]}
      >
        {selectedTask && step ? (
          <div>
            <Text type="secondary" style={{ display: 'block', marginBottom: '12px', fontSize: '13px' }}>
              Next: {step.nextOwner}
            </Text>
            <InvestigationCaseSummary task={selectedTask} />
            <div style={{ marginBottom: '16px' }}>
              <Text style={{ display: 'block', marginBottom: '6px', color: '#334155' }}>
                {step.remarksLabel}
                {step.remarksRequired ? <span style={{ color: '#dc2626' }}> *</span> : null}
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
          </div>
        ) : null}
      </Modal>
    </>
  );
}

export default InvestigationReviewPanel;
