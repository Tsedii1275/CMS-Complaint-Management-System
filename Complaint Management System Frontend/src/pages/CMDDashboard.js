import React, { useState, useEffect, useRef } from 'react';
import { Card, Typography, Button, Tag, Empty, Alert, Form, Input, Checkbox, Row, Col, Select, Radio, Progress, Tooltip, Tabs, Table, Space, Modal, Upload, Badge, message as antMessage } from 'antd';
import { DeleteOutlined, ArrowLeftOutlined, DashboardOutlined, SearchOutlined, UserOutlined, WarningOutlined, EditOutlined, CheckOutlined, CloseOutlined, UploadOutlined, PaperClipOutlined, CheckCircleOutlined, CheckCircleFilled, CloseCircleOutlined, SaveOutlined, SwapOutlined, AudioOutlined, EyeOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import TaskTable, { formatUniqueId, formatIntakeId, matchesTicketSearch } from '../components/TaskTable';
import { useAuth } from '../contexts/AuthContext';
import { renderComplaintStatusTag } from '../utils/statusUtils';

const { Title, Text } = Typography;
const { Option } = Select;

function followupStatusTagColor(status) {
  if (status === 'CLOSED') return 'success';
  if (status === 'IN_PROGRESS') return 'processing';
  return 'error';
}

function isClosedCmdTask(t) {
  return t.status === 'CLOSED' ||
    t.variables?.status === 'CLOSED' ||
    t.variables?.stage === 'CLOSED' ||
    t.variables?.currentStage === 'CLOSED' ||
    t.variables?.resolutionAccepted === true ||
    Boolean(t.deleteReason) ||
    Boolean(t.ended);
}

function isDeclinedCmdTask(t) {
  return t.status === 'DECLINED' ||
    t.classification === 'DECLINED' ||
    t.variables?.status === 'DECLINED' ||
    t.variables?.decision === 'DECLINED' ||
    t.variables?.classification === 'DECLINED' ||
    t.variables?.targetTab === 'Declined' ||
    t.definitionKey === 'DECLINED_HISTORIC';
}

function isOtherCmdTask(t) {
  return t.status === 'OTHER' ||
    t.classification === 'OTHER' ||
    t.variables?.status === 'OTHER' ||
    t.variables?.classification === 'OTHER' ||
    t.variables?.targetTab === 'Other';
}

function buildUnresolvedTicketIds(followups) {
  const unresolvedTicketIds = new Set();
  (followups || []).forEach(f => {
    if (f.ticketNumber) unresolvedTicketIds.add(String(f.ticketNumber).toLowerCase().trim());
    if (f.ticketId) unresolvedTicketIds.add(String(f.ticketId).toLowerCase().trim());
    if (f.complaintId) unresolvedTicketIds.add(String(f.complaintId).toLowerCase().trim());
  });
  return unresolvedTicketIds;
}

function isUnresolvedCmdTask(t, unresolvedTicketIds) {
  if (t.variables?.isUnresolvedFollowup === true ||
    t.variables?.targetTab === 'Unresolved Follow-Up' ||
    t.variables?.targetTab === 'unresolved_followups' ||
    Boolean(t.variables?.followupStatus)) {
    return true;
  }
  const id1 = String(t.dbcTicketId || '').toLowerCase().trim();
  const id2 = String(t.variables?.dbcTicketId || '').toLowerCase().trim();
  const id3 = String(t.complaintId || '').toLowerCase().trim();
  const id4 = String(t.generalTicketId || '').toLowerCase().trim();
  const id5 = String(t.variables?.uniqueIdNo || '').toLowerCase().trim();
  return Boolean((id1 && unresolvedTicketIds.has(id1)) ||
    (id2 && unresolvedTicketIds.has(id2)) ||
    (id3 && unresolvedTicketIds.has(id3)) ||
    (id4 && unresolvedTicketIds.has(id4)) ||
    (id5 && unresolvedTicketIds.has(id5)));
}

const CMD_SCREENING_RESET = {
    classification: 'COMPLAINT',
  declineReason: '',
  additionalRemarks: '',
  complaintCategory: 'Customer Service Issues',
  serviceType: 'Digital Banking',
  complaintMadeOn: 'Branch',
  receivedBy: 'Customer Care-Telephone',
          complaintClassification: 'General',
          requiresInvestigation: false,
          notes: '',
          district: '',
          branch: '',
          department: '',
  manager: '',
  isDeclining: false
};

function resolveFinalClassification(formData) {
  const isDeclined = formData.classification === 'DECLINED' || Boolean(formData.isDeclining);
  if (isDeclined) {
    return { isDeclined: true, finalClassification: 'DECLINED' };
  }
  if (formData.classification === 'OTHER') {
    return { isDeclined: false, finalClassification: 'OTHER' };
  }
  return { isDeclined: false, finalClassification: 'COMPLAINT' };
}

function cmdSubmitValidationError(formData, isDeclined, finalClassification) {
  if (isDeclined) {
    return formData.declineReason?.trim() ? null : 'Decline Reason is mandatory when declining a complaint.';
  }
  if (finalClassification !== 'COMPLAINT' || formData.requiresInvestigation) {
    return null;
  }
  const mode = formData.assignmentType || 'DISTRICT_BRANCH';
  if (mode === 'DISTRICT_DEPARTMENT') return assignmentDistrictDepartmentError(formData);
  if (mode === 'DISTRICT_BRANCH') return assignmentDistrictBranchError(formData);
  if (mode === 'HQ_DEPARTMENT') return assignmentHqDepartmentError(formData);
  return null;
}

function assignmentDistrictDepartmentError(formData) {
  if (!formData.district?.trim()) return 'District is required for District Department assignment.';
  if (!formData.department?.trim()) return 'District Department is required.';
  return null;
}

function assignmentDistrictBranchError(formData) {
  if (!formData.district?.trim()) return 'District is required for District Branch assignment.';
  if (!formData.branch?.trim()) return 'Branch is required for District Branch assignment.';
  return null;
}

function assignmentHqDepartmentError(formData) {
  if (!formData.department?.trim()) return 'Head Office Department is required.';
  return null;
}

function applyWorkUnitAssignmentVars(variables, formData) {
  const mode = formData.assignmentType || 'DISTRICT_BRANCH';
  variables.assignmentType = mode;
  if (mode === 'HQ_DEPARTMENT') {
    variables.district = 'Head Office';
    variables.branch = '';
    variables.department = formData.department;
    variables.manager = formData.manager || `Department Director & Manager (${formData.department})`;
        return;
      }
      if (mode === 'DISTRICT_DEPARTMENT') {
    variables.district = formData.district;
    variables.branch = '';
    variables.department = formData.department;
    variables.manager = formData.manager || `District Director & Manager (${formData.department})`;
          return;
        }
  variables.district = formData.district;
  variables.branch = formData.branch;
  variables.department = formData.department || '';
  variables.manager = formData.manager || 'Branch Manager & CSM';
}

function hasWorkUnitResolution(selectedTask) {
  return Boolean(selectedTask.variables?.resolutionDetails || selectedTask.variables?.actionTaken
    || selectedTask.variables?.workUnitResolutionGiven || selectedTask.variables?.resolutionProvided);
}

function buildCmdCompleteVariables(formData, finalClassification, selectedTask, investigationFileList) {
      const variables = {
        classification: finalClassification,
        decision: finalClassification,
        isComplaint: finalClassification === 'COMPLAINT' || finalClassification === 'DECLINED',
        declineReason: formData.declineReason || '',
        additionalRemarks: formData.additionalRemarks || formData.notes || '',
        complaintCategory: formData.complaintCategory || 'Customer Service Issues',
        serviceType: formData.serviceType || 'Digital Banking',
        complaintMadeOn: formData.complaintMadeOn || 'Branch',
        receivedBy: formData.receivedBy || 'Customer Care-Telephone',
        complaintClassification: formData.complaintClassification || 'General',
        priorityLevel: formData.complaintClassification || 'General',
        requiresInvestigation: Boolean(formData.requiresInvestigation),
        notes: formData.notes || ''
      };

  const workUnitRes = hasWorkUnitResolution(selectedTask);
  const isClosingAfterRes = workUnitRes && (formData.ccoActionChoice === 'close_complaint' || !formData.ccoActionChoice);

      if (finalClassification === 'DECLINED') {
        variables.targetTab = 'Declined';
        variables.status = 'DECLINED';
      } else if (finalClassification === 'OTHER') {
        variables.targetTab = 'Other';
      } else if (isClosingAfterRes) {
        const summaryText = (formData.resolutionSummary || formData.notes || '').trim();
        if (!summaryText) {
      return { error: 'Resolution Summary is mandatory before closing the case and notifying the customer.' };
        }
        variables.status = 'CLOSED';
        variables.currentStage = 'CLOSED';
        variables.stage = 'CLOSED';
        variables.decision = 'RESOLVED';
        variables.resolutionAccepted = true;
        variables.requiresInvestigation = false;
        variables.resolutionSummary = summaryText;
        variables.notes = summaryText;
        variables.additionalRemarks = summaryText;
      } else if (formData.requiresInvestigation) {
        variables.requiresInvestigation = true;
        variables.currentStage = 'CHIEF_EXPERIENCE_REVIEW';
        variables.stage = 'CHIEF_EXPERIENCE';
        variables.assignedToRole = 'ROLE_CHIEF_EXPERIENCE_OFFICER';
        variables.nextStage = 'CHIEF_OPERATION_AUDIT';
        variables.investigationFiles = investigationFileList.map(f => ({
          name: f.name,
          url: f.url,
          size: f.size,
          type: f.type
        }));
      } else {
    applyWorkUnitAssignmentVars(variables, formData);
  }

  return { variables, hasWorkUnitRes: workUnitRes, isClosingAfterRes };
}

function cmdSubmitSuccessText(finalClassification, isClosingAfterRes, isReassigning, variables, requiresInvestigation) {
      if (finalClassification === 'DECLINED') {
    return 'Complaint declined and recorded in audit log. Case moved to Declined tab.';
  }
  if (finalClassification === 'OTHER') {
    return 'Item classified as OTHER and routed directly to the Contact Center queue.';
  }
  if (isClosingAfterRes) {
    return 'Complaint resolution accepted, case CLOSED, and customer notified successfully!';
  }
  if (isReassigning) {
        const targetUnit = variables.department || variables.branch || variables.district || 'Work Unit';
    return `Complaint successfully reassigned to ${targetUnit} for additional resolution.`;
  }
  if (requiresInvestigation) {
    return 'Complaint marked for Investigation and escalated to Chief Experience Officer for review.';
  }
  return 'Task completed! Complaint classified and case routed successfully.';
}

function applyCmdRoleScope(filtered, user) {
      const role = user?.role || '';
      if (role === 'ROLE_BRANCH_MANAGER' || role === 'ROLE_CUSTOMER_SERVICE_MANAGER') {
        const userBranch = user?.branch || 'Bole Branch';
    return filtered.filter(t => {
          const b = t.variables?.complaint?.branch || t.branch;
          return !b || b.toLowerCase().includes(userBranch.split(' ')[0].toLowerCase());
        });
  }
  if (role === 'ROLE_DIGITAL_MARKETING_SENIOR_MANAGER') {
    return filtered.filter(t => {
          const ch = (t.variables?.complaint?.channel || t.channel || '').toLowerCase();
          return ['digital', 'social_media', 'web', 'mobile', 'internet_banking', 'super_app', 'portal'].includes(ch);
        });
  }
  return filtered;
}

function mapDeclinedMetricToTask(m) {
  const dbcFromTicket = m.dbcTicketId && String(m.dbcTicketId).startsWith('DBC-') ? m.dbcTicketId : null;
  const dbcFromComplaint = m.complaintId && String(m.complaintId).startsWith('DBC-') ? m.complaintId : null;
  const formalDbc = dbcFromTicket || dbcFromComplaint || m.dbcTicketId || m.complaintId;
        const intakeId = m.generalTicketId || m.complaintId;
  return {
          id: `declined-${m.id || formalDbc}`,
          complaintId: formalDbc,
          dbcTicketId: formalDbc,
          generalTicketId: intakeId,
          customerName: m.customerName || 'N/A',
          status: 'DECLINED',
          classification: 'DECLINED',
          priority: m.priority || 'Normal',
          createdAt: m.createdAt,
          definitionKey: 'DECLINED_HISTORIC',
          variables: {
            customer: { name: m.customerName, phone: m.phone || 'N/A' },
            complaint: {
              description: m.breachReason || 'Declined Complaint',
              branch: m.branch,
              district: m.district,
              category: m.complaintCategory
            },
            declineReason: m.breachReason || 'Declined',
            classification: 'DECLINED',
            status: 'DECLINED',
            targetTab: 'Declined',
            dbcTicketId: formalDbc,
            complaintId: formalDbc,
            generalTicketId: intakeId
          }
  };
}

function buildCmdTaskList(tasksData, metricsData, user) {
  let filtered = (tasksData || []).filter(task =>
    task.definitionKey === 'FormTask_43' || task.definitionKey === 'UserTask_WorkUnit' || task.definitionKey === 'UserTask_Audit'
  );
  filtered = applyCmdRoleScope(filtered, user);
  (metricsData || []).filter(m => m.status === 'DECLINED' || m.classification === 'DECLINED')
    .forEach(m => filtered.push(mapDeclinedMetricToTask(m)));
      filtered.sort((a, b) => {
        const aRejected = !!a.variables?.customerFeedbackComment || a.variables?.isSatisfied === false;
        const bRejected = !!b.variables?.customerFeedbackComment || b.variables?.isSatisfied === false;
        if (aRejected && !bRejected) return -1;
        if (!aRejected && bRejected) return 1;
        return 0;
      });
      return filtered;
}

function resolveAssignmentTypeFromTask(task) {
  if (task.variables?.assignmentType) return task.variables.assignmentType;
  if (task.variables?.district === 'Head Office') return 'HQ_DEPARTMENT';
  if (task.variables?.department && !task.variables?.branch) return 'DISTRICT_DEPARTMENT';
  return 'DISTRICT_BRANCH';
}

function initialCmdFormFromTask(task) {
    const rawClass = task.classification || task.variables?.classification;
    const initialClass = (rawClass && rawClass !== 'INTAKE') ? rawClass : 'COMPLAINT';
  return {
      classification: initialClass,
        declineReason: '',
        additionalRemarks: '',
      resolutionSummary: '',
      complaintCategory: task.variables?.complaintCategory || task.variables?.complaint?.category || task.complaintCategory || 'Customer Service Issues',
      serviceType: task.variables?.serviceType || task.variables?.complaint?.serviceType || 'Digital Banking',
      complaintMadeOn: task.variables?.complaintMadeOn || task.variables?.channel || task.variables?.complaint?.complaintMadeOn || 'Branch',
      receivedBy: task.variables?.receivedBy || task.variables?.complaint?.receivedBy || 'Customer Care-Telephone',
      complaintClassification: task.variables?.complaintClassification || task.variables?.priorityLevel || task.priority || 'General',
      requiresInvestigation: Boolean(task.variables?.requiresInvestigation),
        notes: '',
    assignmentType: resolveAssignmentTypeFromTask(task),
      district: task.variables?.district || task.variables?.complaint?.district || '',
      branch: task.variables?.branch || task.variables?.complaint?.branch || '',
      department: task.variables?.department || '',
      manager: task.variables?.manager || '',
      accountNumber: task.variables?.customer?.accountNumber || task.variables?.accountNumber || '',
      complaintDescription: task.variables?.complaint?.description || task.variables?.description || '',
      ccoActionChoice: 'close_complaint'
  };
}

const FCR_APPROVE_VARS = {
          fcrAction: 'approve',
          fcrStatus: 'VERIFIED',
          requiresInvestigation: false,
          status: 'RESOLVED',
          currentStage: 'RESOLVED',
          stage: 'RESOLVED',
          decision: 'FCR_APPROVED',
          notes: 'FCR resolution verified and approved by Customer Care Officer.'
        };

          const STANDARDIZED_CATEGORY_MAP = [
            { key: 'Customer Service Issues', label: 'Customer Service Issues', color: '#fa8c16' },
            { key: 'Transaction Error', label: 'Transaction Error', color: '#cf1322' },
            { key: 'Account Management', label: 'Account Management', color: '#52c41a' },
            { key: 'Banking App Issues', label: 'Banking App Issues', color: '#722ed1' },
            { key: 'Credit/Financing Concerns', label: 'Credit/Financing Concerns', color: '#13c2c2' },
            { key: 'Fraud & Security Risk', label: 'Fraud & Security Risk', color: '#f5222d' },
            { key: 'Information Disclosure', label: 'Information Disclosure', color: '#eb2f96' },
            { key: 'ATM & Card Banking Issues', label: 'ATM & Card Banking Issues', color: '#1890ff' },
            { key: 'Policy & Compliance Disputes', label: 'Policy & Compliance Disputes', color: '#fa541c' },
            { key: 'System Failure', label: 'System Failure', color: '#2f54eb' },
            { key: 'Branch Operation', label: 'Branch Operation', color: '#faad14' },
            { key: 'General', label: 'General / Other', color: '#8c8c8c' }
          ];

function getTaskCategoryLabel(taskItem, slaMetrics) {
            const ticketId = taskItem.complaintId || taskItem.variables?.complaintId || taskItem.id;
            const metric = slaMetrics.find(m => m.complaintId === ticketId);
            const raw = taskItem.variables?.complaintCategory ||
              taskItem.variables?.complaint?.complaintCategory ||
              taskItem.variables?.complaint?.category ||
              taskItem.variables?.category ||
              taskItem.variables?.serviceType ||
              metric?.complaintCategory ||
              taskItem.complaintCategory ||
              'General';
            const str = String(raw).trim();
            const matched = STANDARDIZED_CATEGORY_MAP.find(c => c.key.toLowerCase() === str.toLowerCase());
            if (matched) return matched.key;
            const lower = str.toLowerCase();
            if (lower.includes('customer service') || lower.includes('behaviour')) return 'Customer Service Issues';
            if (lower.includes('transaction')) return 'Transaction Error';
            if (lower.includes('account')) return 'Account Management';
            if (lower.includes('app') || lower.includes('mobile') || lower.includes('super')) return 'Banking App Issues';
            if (lower.includes('credit') || lower.includes('loan') || lower.includes('financing')) return 'Credit/Financing Concerns';
            if (lower.includes('fraud') || lower.includes('security')) return 'Fraud & Security Risk';
            if (lower.includes('disclosure') || lower.includes('information')) return 'Information Disclosure';
            if (lower.includes('atm') || lower.includes('card')) return 'ATM & Card Banking Issues';
            if (lower.includes('policy') || lower.includes('compliance')) return 'Policy & Compliance Disputes';
            if (lower.includes('system') || lower.includes('technical')) return 'System Failure';
            if (lower.includes('branch')) return 'Branch Operation';
            return 'General';
}

function isSeniorCmdManager(role) {
  return role === 'ROLE_CUSTOMER_CARE_SENIOR_MANAGER'
    || role === 'ROLE_SERVICE_QUALITY_DIRECTOR'
    || role === 'ROLE_ADMIN';
}

function isCmdOfficerWorkspace(role) {
  return role === 'ROLE_CUSTOMER_CARE_OFFICER' || role === 'ROLE_CUSTOMER_CARE_TEAM_LEADER';
}

function isUnassignedAssignee(assignee) {
  return !assignee || assignee === 'Unassigned' || assignee === 'null';
}

function isFcrPendingCco(t) {
  const pending = t.isFcr || t.variables?.isFcr || t.variables?.complaint?.isFcr
    || t.variables?.fcrStatus === 'PENDING_CCO_VERIFICATION' || t.fcrStatus === 'PENDING_CCO_VERIFICATION';
  return pending && t.variables?.fcrStatus !== 'REJECTED' && t.fcrStatus !== 'REJECTED' && t.variables?.isFcr !== false;
}

function isMyCmdTask(t, user) {
  return t.assignee === user?.username || t.assignee === user?.name || t.claimedBy === user?.username;
}

function isActiveCmdQueueTask(t, unresolvedTicketIds) {
  return !isClosedCmdTask(t) && !isDeclinedCmdTask(t) && !isOtherCmdTask(t) && !isUnresolvedCmdTask(t, unresolvedTicketIds);
}

function followupActionStatus(status) {
  if (status === 'IN_PROGRESS') return { color: 'orange', label: 'IN PROGRESS' };
  if (status === 'CLOSED') return { color: 'green', label: 'COMPLETED' };
  return { color: 'red', label: 'PENDING' };
}

function cmdAlertFromMessage(message) {
  const lowerMsg = message.toLowerCase();
  if (lowerMsg.includes('success')) return { type: 'success', title: 'Success' };
  if (lowerMsg.includes('error')) return { type: 'error', title: 'Error' };
  return { type: 'info', title: 'Information' };
}

function CmdMessageBanner({ message, onClose }) {
  if (!message) return null;
  const { type, title } = cmdAlertFromMessage(message);
  return (
    <Alert
      message={title}
      description={message.replace(/^(success|error):\s*/i, '')}
      type={type}
      showIcon
      closable
      style={{ marginBottom: '24px' }}
      onClose={onClose}
    />
  );
}

function CmdCategoryAnalytics({ tasks, slaMetrics, unresolvedFollowups, loading }) {
  const unresolvedTicketIds = buildUnresolvedTicketIds(unresolvedFollowups);
  const activeCategoryTasks = tasks.filter(t => isActiveCmdQueueTask(t, unresolvedTicketIds));
          const totalComplaints = activeCategoryTasks.length;
          const categoryCounts = {};
          activeCategoryTasks.forEach(t => {
    const cat = getTaskCategoryLabel(t, slaMetrics);
            categoryCounts[cat] = (categoryCounts[cat] || 0) + 1;
          });
          const categoryData = STANDARDIZED_CATEGORY_MAP.map(c => {
            const count = categoryCounts[c.key] || 0;
            return {
              key: c.key,
              name: c.label,
              color: c.color,
              count,
              percent: totalComplaints > 0 ? (count / totalComplaints) * 100 : 0
            };
          }).sort((a, b) => b.count - a.count);
          const radius = 48;
          const circumference = 2 * Math.PI * radius;
          let currentRotation = -90;
  const slices = categoryData.filter(c => c.count > 0);

          return (
            <div style={{ marginBottom: '24px' }}>
              <Card
                title={<span style={{ fontWeight: 600, color: BRAND_COLORS.primary, display: 'flex', alignItems: 'center', gap: '8px' }}><DashboardOutlined /> Complaint Category Analytics</span>}
                bordered={true}
                loading={loading}
                style={{ borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.02)' }}
              >
                <Row gutter={[32, 24]} align="middle">
                  <Col xs={24} md={12} style={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                    <div style={{ position: 'relative', width: '140px', height: '140px', flexShrink: 0 }}>
                      <svg width="100%" height="100%" viewBox="0 0 120 120">
                <circle cx="60" cy="60" r={radius} fill="transparent" stroke="#f0f0f0" strokeWidth="12" />
                {totalComplaints > 0 && slices.map((cat) => {
                          const strokeDashoffset = circumference - (cat.percent / 100) * circumference;
                          const rotation = currentRotation;
                          currentRotation += (cat.percent / 100) * 360;
                          return (
                            <Tooltip
                              key={cat.key}
                              title={<div style={{ textAlign: 'center' }}><strong>{cat.name}</strong><br />{cat.count} complaints ({Math.round(cat.percent)}%)</div>}
                              placement="top"
                            >
                              <circle
                                cx="60"
                                cy="60"
                                r={radius}
                                fill="transparent"
                                stroke={cat.color}
                                strokeWidth="12"
                                strokeDasharray={circumference}
                                strokeDashoffset={strokeDashoffset}
                                transform={`rotate(${rotation} 60 60)`}
                                strokeLinecap="round"
                                style={{
                                  transition: 'stroke-dashoffset 0.8s ease, transform 0.8s ease, stroke-width 0.2s ease',
                                  cursor: 'pointer'
                                }}
                                onMouseEnter={(e) => {
                                  e.target.setAttribute('stroke-width', '15');
                                }}
                                onMouseLeave={(e) => {
                                  e.target.setAttribute('stroke-width', '12');
                                }}
                              />
                            </Tooltip>
                          );
                        })}
                      </svg>
                      <div style={{
                        position: 'absolute',
                        top: '50%',
                        left: '50%',
                        transform: 'translate(-50%, -50%)',
                        textAlign: 'center'
                      }}>
                        <div style={{ fontSize: '26px', fontWeight: '800', color: BRAND_COLORS.primary, lineHeight: 1 }}>
                          {totalComplaints}
                        </div>
                        <div style={{ fontSize: '10px', color: '#8c8c8c', marginTop: '2px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Total</div>
                      </div>
                    </div>
                  </Col>
                  <Col xs={24} md={12} className="card-vertical-divider">
                    <div style={{ maxHeight: '180px', overflowY: 'auto', paddingRight: '8px' }}>
              {slices.length === 0 ? (
                        <div style={{ textAlign: 'center', padding: '24px 0', color: '#bfbfbf' }}>No active complaints in queue</div>
                      ) : (
                slices.map(cat => (
                          <div key={cat.key} style={{ marginBottom: '10px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '2px' }}>
                              <span style={{ fontWeight: 500, display: 'flex', alignItems: 'center', gap: '6px' }}>
                                <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: cat.color, display: 'inline-block' }}></span>
                                {cat.name}
                              </span>
                              <span style={{ color: '#8c8c8c' }}>{cat.count} ({Math.round(cat.percent)}%)</span>
                            </div>
                    <Progress percent={Math.round(cat.percent)} size="small" showInfo={false} strokeColor={cat.color} />
                          </div>
                        ))
                      )}
                    </div>
                  </Col>
                </Row>
              </Card>
            </div>
          );
}

function CmdQueueToolbar({
  user,
  tasks,
  unresolvedFollowups,
  searchQuery,
  setSearchQuery,
  activeTab,
  setActiveTab,
  setSelectedTask
}) {
              const userRole = user?.role || '';
  const isSeniorManager = isSeniorCmdManager(userRole);
  const unresolvedTicketIds = buildUnresolvedTicketIds(unresolvedFollowups);
  const activeTasks = tasks.filter(t => isActiveCmdQueueTask(t, unresolvedTicketIds));
  const unassignedTasks = activeTasks.filter(t => isUnassignedAssignee(t.assignee));
  const assignedTasks = activeTasks.filter(t => !isUnassignedAssignee(t.assignee));
  const openFollowupCount = unresolvedFollowups.reduce((n, f) => (f.followupStatus !== 'CLOSED' ? n + 1 : n), 0);

              return (
                <div>
                  {isSeniorManager && (
                    <Row gutter={[16, 16]} style={{ marginBottom: '20px' }}>
                      <Col xs={24} sm={8}>
                        <Card size="small" style={{ borderRadius: '8px', borderLeft: `4px solid ${BRAND_COLORS.primary}` }}>
                          <Text type="secondary" style={{ fontSize: '11px', fontWeight: 600 }}>Total Queue Volume</Text>
                          <Title level={3} style={{ margin: '2px 0 0 0', color: BRAND_COLORS.primary }}>{activeTasks.length}</Title>
                        </Card>
                      </Col>
                      <Col xs={24} sm={8}>
                        <Card size="small" style={{ borderRadius: '8px', borderLeft: '4px solid #2563eb' }}>
                          <Text type="secondary" style={{ fontSize: '11px', fontWeight: 600 }}>Assigned complaints</Text>
                          <Title level={3} style={{ margin: '2px 0 0 0', color: '#2563eb' }}>{assignedTasks.length}</Title>
                        </Card>
                      </Col>
                      <Col xs={24} sm={8}>
                        <Card size="small" style={{ borderRadius: '8px', borderLeft: '4px solid #ea580c' }}>
                          <Text type="secondary" style={{ fontSize: '11px', fontWeight: 600 }}>Unassigned Complaints</Text>
                          <Title level={3} style={{ margin: '2px 0 0 0', color: '#ea580c' }}>{unassignedTasks.length}</Title>
                        </Card>
                      </Col>
                    </Row>
                  )}
      {openFollowupCount > 0 && (
                    <Alert
                      message="Unresolved Customer Complaint Alert"
          description={`There are ${openFollowupCount} unresolved customer complaint survey response(s) requiring follow-up investigation.`}
                      type="warning"
                      showIcon
                      icon={<WarningOutlined style={{ color: '#ef4444' }} />}
                      style={{ marginBottom: '16px', borderRadius: '8px', borderLeft: '4px solid #ef4444', backgroundColor: '#fff5f5' }}
                      closable
                    />
                  )}
                  <div style={{ marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Input
                      className="pill-search-input"
          placeholder="Search by Unique ID No, CM ticket, Customer, Description..."
                      prefix={<SearchOutlined style={{ color: '#475569', fontSize: '16px' }} />}
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      allowClear
          style={{ width: '100%', maxWidth: '280px' }}
                    />
                  </div>
                  <Tabs
                    activeKey={activeTab}
                    onChange={(key) => {
                      setActiveTab(key);
                      setSelectedTask(null);
                    }}
                    style={{ marginBottom: '16px' }}
                  >
                    <Tabs.TabPane tab="All Tasks" key="all_tasks" />
                    {isSeniorManager && (
                      <Tabs.TabPane tab="Assigned" key="assigned_tasks" />
                    )}
                    {isSeniorManager && (
                      <Tabs.TabPane tab="Unassigned" key="unassigned_tasks" />
                    )}
                    {isCmdOfficerWorkspace(userRole) && (
                      <Tabs.TabPane tab="My Tasks" key="my_tasks" />
                    )}
                    <Tabs.TabPane tab="FCR Verification" key="fcr_tasks" />
                    <Tabs.TabPane tab="Declined" key="declined_tasks" />
                    <Tabs.TabPane
          tab={(
                        <span>
                          Unresolved Follow-Up
              {openFollowupCount > 0 && (
                <Badge count={openFollowupCount} overflowCount={99} style={{ marginLeft: 6, backgroundColor: '#ef4444' }} />
                          )}
                        </span>
          )}
                      key="unresolved_followups"
                    />
                  </Tabs>
                </div>
              );
}

function UnresolvedFollowupsPanel({
  unresolvedFollowups,
  searchQuery,
  setSelectedFollowup,
  setIsFollowupModalOpen,
  handleStartFollowup,
  handleCloseFollowup
}) {
                const unresolvedColumns = [
                  {
                    title: 'Unique ID No',
                    dataIndex: 'ticketNumber',
                    key: 'ticketNumber',
                    render: (text, record) => (
                      <span style={{
                        fontFamily: 'monospace',
                        fontSize: '11px',
                        color: '#111827',
                        fontWeight: 600,
                        background: '#f3f4f6',
                        padding: '3px 8px',
                        borderRadius: '4px',
                        whiteSpace: 'nowrap',
                        display: 'inline-block'
                      }}>
                        {text || record.ticketId || '-'}
                      </span>
                    )
                  },
                  {
                    title: 'Submission Date',
                    dataIndex: 'submittedAt',
                    key: 'submittedAt',
                    render: (text) => text ? new Date(text).toLocaleString() : '-'
                  },
                  {
                    title: 'Satisfaction Answer',
                    key: 'satisfaction',
                    render: () => <Tag color="error" style={{ fontWeight: 600 }}>No (Not Resolved)</Tag>
                  },
                  {
                    title: 'Follow-Up Status',
                    dataIndex: 'followupStatus',
                    key: 'followupStatus',
                    render: (status) => {
        const { color, label } = followupActionStatus(status);
                      return <Tag color={color} style={{ fontWeight: 600 }}>{label}</Tag>;
                    }
                  },
                  {
                    title: 'Actions',
                    key: 'actions',
                    render: (_, record) => (
                      <Space size="small">
                        <Tooltip title="View Complaint Details">
                          <Button
                            type="text"
                            icon={<EyeOutlined style={{ color: BRAND_COLORS.primary, fontSize: '18px' }} />}
                            onClick={() => {
                              setSelectedFollowup(record);
                              setIsFollowupModalOpen(true);
                            }}
                          />
                        </Tooltip>
                        {record.followupStatus !== 'IN_PROGRESS' && record.followupStatus !== 'CLOSED' && (
                          <Button
                            size="small"
                            type="primary"
                            style={{ borderRadius: '4px', background: '#2563eb', borderColor: '#2563eb', fontWeight: 600 }}
                            onClick={() => handleStartFollowup(record.id)}
                          >
                            Start Follow-Up
                          </Button>
                        )}
                        {record.followupStatus !== 'CLOSED' && (
                          <Button
                            size="small"
                            type="primary"
                            style={{ borderRadius: '4px', background: '#10b981', borderColor: '#10b981', fontWeight: 600 }}
                            onClick={() => handleCloseFollowup(record.id)}
                          >
                            Close Follow-Up
                          </Button>
                        )}
                      </Space>
                    )
                  }
                ];
                const query = searchQuery.toLowerCase().trim();
                const filteredUnresolved = unresolvedFollowups.filter(f => {
                  const comm = (f.additionalComments || '').toLowerCase();
    return !query || matchesTicketSearch(f, query) || comm.includes(query);
                });
                if (filteredUnresolved.length === 0) {
                  return (
                    <Empty
                      description="No unresolved complaint survey follow-ups found."
                      image={Empty.PRESENTED_IMAGE_SIMPLE}
                      style={{ marginTop: '60px' }}
                    />
                  );
                }
                return (
                  <Table
                    dataSource={filteredUnresolved}
                    columns={unresolvedColumns}
                    rowKey="id"
                    pagination={{ pageSize: 10 }}
                    style={{ background: '#fff', borderRadius: '8px' }}
      scroll={{ x: 'max-content' }}
                  />
                );
              }

function getCmdTabFilteredTasks({ tasks, unresolvedFollowups, user, activeTab, searchQuery }) {
  const unresolvedTicketIds = buildUnresolvedTicketIds(unresolvedFollowups);
  const activeTasks = tasks.filter(t => isActiveCmdQueueTask(t, unresolvedTicketIds));
  const tabLists = {
    all_tasks: activeTasks,
    unassigned_tasks: activeTasks.filter(t => isUnassignedAssignee(t.assignee)),
    assigned_tasks: activeTasks.filter(t => !isUnassignedAssignee(t.assignee)),
    my_tasks: activeTasks.filter(t => isMyCmdTask(t, user)),
    fcr_tasks: activeTasks.filter(isFcrPendingCco),
    declined_tasks: tasks.filter(isDeclinedCmdTask)
  };
  const tabFilteredTasks = tabLists[activeTab] || activeTasks;
              const query = searchQuery.toLowerCase().trim();
  return tabFilteredTasks.filter(task => {
                const customerName = task.customerName || '';
                const desc = task.variables?.complaint?.description || '';
                const cat = task.variables?.complaint?.category || '';
                const officer = task.assignee || task.claimedBy || '';
                return !query ||
      matchesTicketSearch(task, query) ||
                  customerName.toLowerCase().includes(query) ||
                  desc.toLowerCase().includes(query) ||
                  cat.toLowerCase().includes(query) ||
                  officer.toLowerCase().includes(query);
              });
}

function CmdTaskQueue({
  user,
  tasks,
  unresolvedFollowups,
  activeTab,
  searchQuery,
  handleTaskSelect,
  handleOpenAssignModal
}) {
  const isSeniorManager = isSeniorCmdManager(user?.role || '');
  const filtered = getCmdTabFilteredTasks({ tasks, unresolvedFollowups, user, activeTab, searchQuery });
              if (filtered.length === 0) {
                return (
                  <Empty
                    description="No matching tasks found for selected view"
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    style={{ marginTop: '60px' }}
                  />
                );
              }
              const isDeclinedTab = activeTab === 'declined_tasks';
              return (
                <TaskTable
                  tasks={filtered}
                  onSelectTask={handleTaskSelect}
                  onAssignTask={isSeniorManager && !isDeclinedTab ? handleOpenAssignModal : null}
                  showPriority={false}
                  showAssignedOfficer={!isDeclinedTab}
                  showAssignedDate={!isDeclinedTab}
      showOverallStatus={false}
    />
  );
}

function sget(ref) {
  return ref.current;
}

async function loadUnresolvedFollowupsCmd(ref) {
  try {
    const data = await ApiService.getUnresolvedFollowups();
    sget(ref).setUnresolvedFollowups(data || []);
  } catch (err) {
    console.error('Failed to load unresolved followups:', err);
  }
}

async function loadActiveOfficersCmd(ref) {
  try {
    const officers = await ApiService.getCustomerCareOfficers();
    sget(ref).setActiveOfficers(officers || []);
  } catch (err) {
    console.error('Failed to fetch active officers:', err);
  }
}

async function loadHierarchyCmd(ref) {
  try {
    const data = await ApiService.getHierarchy();
    sget(ref).setDistrictsList(data);
  } catch (err) {
    console.error('Failed to load hierarchy:', err);
  }
}

async function fetchSlaMetricsCmd(ref) {
  try {
    const data = await ApiService.getAllSlaMetrics().catch(() => []);
    sget(ref).setSlaMetrics(data || []);
  } catch (err) {
    console.error('Failed to load SLA metrics:', err);
  }
}

async function loadCmdTasks(ref, showLoading = false) {
  const s = sget(ref);
  try {
    if (showLoading === true) s.setLoading(true);
    const [tasksData, metricsData] = await Promise.all([
      ApiService.getEnrichedTasks().catch(() => []),
      ApiService.getAllSlaMetrics().catch(() => [])
    ]);
    s.setSlaMetrics(metricsData || []);
    const filtered = buildCmdTaskList(tasksData, metricsData, s.user);
    s.setTasks(filtered);
    s.setError('');
    if (s.selectedTask) {
      const updatedSelected = filtered.find(t => t.id === s.selectedTask.id);
      if (updatedSelected) {
        s.setSelectedTask(updatedSelected);
      }
    }
    return filtered;
  } catch (error) {
    s.setError('Failed to load tasks. Please try again later.');
    console.error('Error loading tasks:', error);
  } finally {
    s.setLoading(false);
  }
}

async function startCmdFollowup(ref, id) {
  const s = sget(ref);
  try {
    await ApiService.startUnresolvedFollowup(id, s.user?.username || 'team_leader');
    antMessage.success('Follow-up investigation started successfully.');
    await loadUnresolvedFollowupsCmd(ref);
  } catch (err) {
    antMessage.error(err.message || 'Failed to start follow-up.');
  }
}

async function closeCmdFollowup(ref, id) {
  const s = sget(ref);
  try {
    await ApiService.closeUnresolvedFollowup(id, s.user?.username || 'team_leader');
    antMessage.success('Follow-up investigation closed successfully.');
    await loadUnresolvedFollowupsCmd(ref);
  } catch (err) {
    antMessage.error(err.message || 'Failed to close follow-up.');
  }
}

function openCmdAssignModal(ref, task) {
  const s = sget(ref);
  s.setTaskToAssign(task);
  const existingAssignee = task.assignee || task.claimedBy || task.assignedUser;
  s.setSelectedOfficerUsername(existingAssignee && existingAssignee !== 'Unassigned' && existingAssignee !== 'null' ? existingAssignee : '');
  s.setIsAssignModalOpen(true);
  loadActiveOfficersCmd(ref);
}

async function confirmCmdTaskAssignment(ref) {
  const s = sget(ref);
  if (!s.taskToAssign || !s.selectedOfficerUsername) {
    antMessage.warning('Please select a Customer Care Officer or Team Leader.');
    return;
  }
  s.setIsAssigning(true);
  try {
    await ApiService.assignTask(s.taskToAssign.id, s.selectedOfficerUsername);
    const officer = s.activeOfficers.find(o => o.username === s.selectedOfficerUsername);
    const officerName = officer ? (officer.fullName || officer.username) : s.selectedOfficerUsername;
    antMessage.success(`Complaint ${s.taskToAssign.complaintId || s.taskToAssign.id} assigned to ${officerName}`);
    s.setIsAssignModalOpen(false);
    s.setTaskToAssign(null);
    s.setSelectedOfficerUsername('');
    await loadCmdTasks(ref, false);
  } catch (err) {
    console.error('Assignment error:', err);
    antMessage.error(err.response?.data?.error || err.message || 'Failed to assign complaint.');
  } finally {
    s.setIsAssigning(false);
  }
}

async function clearAllCmdTasks(ref) {
  if (!window.confirm('Are you sure you want to clear all tasks? This action cannot be undone.')) {
    return;
  }
  const s = sget(ref);
  s.setClearingTasks(true);
  try {
    await ApiService.clearAllTasks();
    antMessage.success('All tasks and SLA records cleared successfully!');
    s.setMessage('All tasks and SLA records cleared successfully!');
    s.setSelectedTask(null);
    s.setFormData({
      complaintClassification: 'General',
      requiresInvestigation: false,
      notes: '',
      district: '',
      branch: '',
      department: '',
      manager: ''
    });
    await loadCmdTasks(ref, true);
    await fetchSlaMetricsCmd(ref);
    setTimeout(() => s.setMessage(''), 4000);
  } catch (error) {
    const detail = error?.message || 'Failed to clear all tasks';
    antMessage.error(detail);
    s.setMessage(`error: ${detail}`);
    console.error('Error clearing tasks:', error);
  } finally {
    s.setClearingTasks(false);
  }
}

async function selectCmdTask(ref, task) {
  const s = sget(ref);
  s.setSelectedTask(task);
  s.setMessage('');
  s.setEditingFields({ district: false, branch: false, description: false, accountNumber: false });
  s.setFormData(initialCmdFormFromTask(task));
  try {
    await loadCmdTasks(ref, false);
  } catch (error) {
    s.setMessage('Failed to select task');
    console.error('Error selecting task:', error);
  }
}

function mergeSavedComplaintDetails(prev, formData) {
  if (!prev) return prev;
  const updatedVars = {
    ...prev.variables,
    branch: formData.branch,
    district: formData.district,
    description: formData.complaintDescription,
    complaintCategory: formData.complaintCategory,
    serviceType: formData.serviceType,
    channel: formData.complaintMadeOn,
    complaintMadeOn: formData.complaintMadeOn,
    receivedBy: formData.receivedBy,
    accountNumber: formData.accountNumber,
    complaint: {
      ...prev.variables?.complaint,
      branch: formData.branch,
      district: formData.district,
      description: formData.complaintDescription,
      category: formData.complaintCategory,
      serviceType: formData.serviceType,
      channel: formData.complaintMadeOn,
      receivedBy: formData.receivedBy,
      accountNumber: formData.accountNumber
    },
    customer: {
      ...prev.variables?.customer,
      accountNumber: formData.accountNumber
    }
  };
  return {
    ...prev,
    branch: formData.branch,
    district: formData.district,
    complaintCategory: formData.complaintCategory,
    channel: formData.complaintMadeOn,
    variables: updatedVars
  };
}

async function saveAllCmdChanges(ref) {
  const s = sget(ref);
  if (!s.selectedTask) return;
  s.setIsSubmitting(true);
  try {
    const payload = {
      branch: s.formData.branch,
      district: s.formData.district,
      complaintDescription: s.formData.complaintDescription,
      description: s.formData.complaintDescription,
      complaintCategory: s.formData.complaintCategory,
      category: s.formData.complaintCategory,
      serviceType: s.formData.serviceType,
      complaintMadeOn: s.formData.complaintMadeOn,
      channel: s.formData.complaintMadeOn,
      receivedBy: s.formData.receivedBy,
      accountNumber: s.formData.accountNumber
    };
    await ApiService.updateComplaintDetails(s.selectedTask.id, payload);
    const formData = s.formData;
    s.setSelectedTask(prev => mergeSavedComplaintDetails(prev, formData));
    antMessage.success('All complaint details saved and updated as official record across the system.');
    s.setMessage('success: All complaint details saved and updated as official record across the system.');
    await loadCmdTasks(ref, true);
    await fetchSlaMetricsCmd(ref);
  } catch (err) {
    antMessage.error('Failed to save all changes: ' + (err.message || 'Unknown error'));
  } finally {
    s.setIsSubmitting(false);
  }
}

function changeCmdBranch(ref, value) {
  const s = sget(ref);
  const distObj = s.districtsList.find(d => d.name === s.formData.district);
  const branchObj = distObj?.branches.find(b => b.name === value);
  const deptObj = branchObj?.departments.find(d => d.name === s.formData.department);
  let manager = '';
  if (deptObj) {
    manager = `${deptObj.managerName} (${deptObj.name})`;
  } else if (branchObj) {
    manager = branchObj.managerName;
  }
  s.setFormData(prev => ({
    ...prev,
    branch: value,
    manager
  }));
}

async function saveCmdClassification(ref) {
  const s = sget(ref);
  try {
    if (s.selectedTask?.id) {
      await ApiService.updateComplaintDetails(s.selectedTask.id, {
        classification: s.formData.classification || 'COMPLAINT'
      });
    }
    antMessage.success('Classification saved successfully.');
  } catch (err) {
    antMessage.error(err?.message || 'Failed to save classification.');
  }
}

async function submitCmdScreening(ref) {
  const s = sget(ref);
  if (!s.selectedTask) return;
  const { isDeclined, finalClassification } = resolveFinalClassification(s.formData);
  const validationError = cmdSubmitValidationError(s.formData, isDeclined, finalClassification);
  if (validationError) {
    antMessage.error(validationError);
    s.setMessage(`error: ${validationError}`);
    return;
  }
  s.setIsSubmitting(true);
  s.setMessage('');
  try {
    const built = buildCmdCompleteVariables(s.formData, finalClassification, s.selectedTask, s.investigationFileList);
    if (built.error) {
      antMessage.warning(built.error);
      s.setIsSubmitting(false);
      return;
    }
    const { variables, hasWorkUnitRes, isClosingAfterRes } = built;
    const isReassigning = hasWorkUnitRes && s.formData.ccoActionChoice === 'reassign_work_unit';
    await ApiService.completeTask(s.selectedTask.id, variables);
    const successText = cmdSubmitSuccessText(
      finalClassification,
      isClosingAfterRes,
      isReassigning,
      variables,
      s.formData.requiresInvestigation
    );
    if (finalClassification === 'DECLINED') {
      s.setActiveTab('declined_tasks');
    } else if (finalClassification === 'OTHER') {
      s.setActiveTab('all_tasks');
    }
    antMessage.success(successText);
    s.setMessage(`success: ${successText}`);
    setTimeout(() => s.setMessage(''), 4000);
    s.setFormData(CMD_SCREENING_RESET);
    s.setSelectedTask(null);
    await loadCmdTasks(ref, true);
    await fetchSlaMetricsCmd(ref);
  } catch (err) {
    const backendError = err?.message || err?.data?.error || String(err);
    const errorText = `Failed to submit screening decision: ${backendError}`;
    antMessage.error(errorText);
    s.setMessage(errorText);
    console.error('Submit error:', err);
  } finally {
    s.setIsSubmitting(false);
  }
}

async function approveCmdFcr(ref) {
  const s = sget(ref);
  await ApiService.completeTask(s.selectedTask.id, FCR_APPROVE_VARS);
  antMessage.success('FCR Resolution verified and complaint marked as RESOLVED!');
  s.setMessage('success: FCR Resolution verified and complaint marked as RESOLVED!');
  s.setSelectedTask(null);
  await loadCmdTasks(ref, true);
  await fetchSlaMetricsCmd(ref);
}

async function rejectCmdFcr(ref) {
  const s = sget(ref);
  const currentTaskId = s.selectedTask.id;
  const processInstanceId = s.selectedTask.processInstanceId;
  await ApiService.rejectFcr(currentTaskId);
  antMessage.info('FCR rejected. Complaint updated to standard screening.');
  s.setMessage('info: FCR rejected. Complaint updated to standard screening.');
  const freshTasks = await loadCmdTasks(ref, true);
  await fetchSlaMetricsCmd(ref);
  const updatedTask = (freshTasks || []).find(t => t.id === currentTaskId || t.processInstanceId === processInstanceId);
  s.setActiveTab('my_tasks');
  if (updatedTask) {
    await selectCmdTask(ref, updatedTask);
  }
}

async function verifyCmdFcr(ref, fcrAction) {
  const s = sget(ref);
  if (!s.selectedTask) return;
  s.setIsSubmitting(true);
  try {
    if (fcrAction === 'approve') {
      await approveCmdFcr(ref);
    } else {
      await rejectCmdFcr(ref);
    }
    setTimeout(() => s.setMessage(''), 4000);
  } catch (err) {
    console.error('FCR verification error:', err);
    antMessage.error(err.response?.data?.error || err.message || 'Failed to update FCR status.');
  } finally {
    s.setIsSubmitting(false);
  }
}

async function closeCmdCommitteeCase(ref, isAccepted) {
  const s = sget(ref);
  if (!s.selectedTask) return;
  const summaryText = (s.formData.resolutionSummary || s.formData.notes || '').trim();
  if (!summaryText) {
    antMessage.warning('Resolution Summary is mandatory before closing the case and notifying the customer.');
    return;
  }
  s.setIsSubmitting(true);
  try {
    const vars = {
      classification: 'COMPLAINT',
      decision: 'RESOLVED',
      status: 'CLOSED',
      currentStage: 'CLOSED',
      fcrStatus: isAccepted,
      fcrAction: isAccepted ? 'approve' : 'close',
      resolutionSummary: summaryText,
      notes: summaryText,
      additionalRemarks: summaryText
    };
    await ApiService.completeTask(s.selectedTask.id, vars);
    antMessage.success('Complaint closed and customer notified of final committee decision.');
    s.setMessage('success: Complaint closed and customer notified of final committee decision.');
    s.setSelectedTask(null);
    await loadCmdTasks(ref, true);
    await fetchSlaMetricsCmd(ref);
  } catch (err) {
    antMessage.error('Failed to close case: ' + (err.message || 'Unknown error'));
  } finally {
    s.setIsSubmitting(false);
  }
}

async function closeCmdAfterWorkUnitResolution(ref) {
  const s = sget(ref);
  if (!s.selectedTask) return;
  const summaryText = (s.formData.resolutionSummary || s.formData.notes || '').trim();
  if (!summaryText) {
    antMessage.warning('Please enter a Resolution Summary before closing the complaint.');
    return;
  }
  s.setIsSubmitting(true);
  try {
    const vars = {
      status: 'CLOSED',
      currentStage: 'CLOSED',
      stage: 'CLOSED',
      decision: 'RESOLVED',
      resolutionAccepted: true,
      requiresInvestigation: false,
      resolutionSummary: summaryText,
      notes: summaryText,
      additionalRemarks: summaryText
    };
    await ApiService.completeTask(s.selectedTask.id, vars);
    antMessage.success('Complaint resolution accepted, case CLOSED, and customer notified successfully!');
    s.setSelectedTask(null);
    await loadCmdTasks(ref, true);
    await fetchSlaMetricsCmd(ref);
  } catch (err) {
    antMessage.error(err?.message || 'Failed to close complaint');
  } finally {
    s.setIsSubmitting(false);
  }
}

function bindCmdHandlers(stateRef) {
  return {
    handleStartFollowup: (id) => startCmdFollowup(stateRef, id),
    handleCloseFollowup: (id) => closeCmdFollowup(stateRef, id),
    handleOpenAssignModal: (task) => openCmdAssignModal(stateRef, task),
    confirmTaskAssignment: () => confirmCmdTaskAssignment(stateRef),
    handleTaskSelect: (task) => selectCmdTask(stateRef, task),
    handleSaveAllChanges: () => saveAllCmdChanges(stateRef),
    handleSelectChange: (name, value) => sget(stateRef).setFormData(prev => ({ ...prev, [name]: value })),
    handleDistrictChange: (value) => sget(stateRef).setFormData(prev => ({ ...prev, district: value, branch: '', manager: '' })),
    handleBranchChange: (value) => changeCmdBranch(stateRef, value),
    handleSaveClassification: () => saveCmdClassification(stateRef),
    handleSubmit: () => submitCmdScreening(stateRef),
    handleFcrVerification: (action) => verifyCmdFcr(stateRef, action),
    handleCloseCommitteeDecisionCase: (accepted) => closeCmdCommitteeCase(stateRef, accepted),
    handleCloseAfterWorkUnit: () => closeCmdAfterWorkUnitResolution(stateRef),
    clearAllTasks: () => clearAllCmdTasks(stateRef)
  };
}

function useCmdDashboardState() {
  const { user } = useAuth();
  const stateRef = useRef(null);
  const [tasks, setTasks] = useState([]);
  const [selectedTask, setSelectedTask] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [districtsList, setDistrictsList] = useState([]);
  const [slaMetrics, setSlaMetrics] = useState([]);
  const [activeTab, setActiveTab] = useState(
    isCmdOfficerWorkspace(user?.role || '') ? 'my_tasks' : 'all_tasks'
  );
  const [formData, setFormData] = useState({
    classification: 'COMPLAINT',
    complaintClassification: 'General',
    requiresInvestigation: false,
    notes: '',
    assignmentType: 'DISTRICT_BRANCH',
    district: '',
    branch: '',
    department: '',
    manager: ''
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [clearingTasks, setClearingTasks] = useState(false);
  const [investigationFileList, setInvestigationFileList] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [taskToAssign, setTaskToAssign] = useState(null);
  const [activeOfficers, setActiveOfficers] = useState([]);
  const [selectedOfficerUsername, setSelectedOfficerUsername] = useState('');
  const [isAssigning, setIsAssigning] = useState(false);
  const [unresolvedFollowups, setUnresolvedFollowups] = useState([]);
  const [selectedFollowup, setSelectedFollowup] = useState(null);
  const [isFollowupModalOpen, setIsFollowupModalOpen] = useState(false);
  const [editingFields, setEditingFields] = useState({ district: false, branch: false, description: false, accountNumber: false });

  const bag = {
    user,
    tasks,
    setTasks,
    selectedTask,
    setSelectedTask,
    loading,
    setLoading,
    error,
    setError,
    districtsList,
    setDistrictsList,
    slaMetrics,
    setSlaMetrics,
    activeTab,
    setActiveTab,
    formData,
    setFormData,
    isSubmitting,
    setIsSubmitting,
    message,
    setMessage,
    clearingTasks,
    setClearingTasks,
    investigationFileList,
    setInvestigationFileList,
    searchQuery,
    setSearchQuery,
    isAssignModalOpen,
    setIsAssignModalOpen,
    taskToAssign,
    setTaskToAssign,
    activeOfficers,
    setActiveOfficers,
    selectedOfficerUsername,
    setSelectedOfficerUsername,
    isAssigning,
    setIsAssigning,
    unresolvedFollowups,
    setUnresolvedFollowups,
    selectedFollowup,
    setSelectedFollowup,
    isFollowupModalOpen,
    setIsFollowupModalOpen,
    editingFields,
    setEditingFields,
    stateRef
  };
  stateRef.current = bag;

  useEffect(() => {
    loadCmdTasks(stateRef, true);
    fetchSlaMetricsCmd(stateRef);
    loadHierarchyCmd(stateRef);
    loadActiveOfficersCmd(stateRef);
    loadUnresolvedFollowupsCmd(stateRef);
    const interval = setInterval(() => {
      loadCmdTasks(stateRef, false);
      fetchSlaMetricsCmd(stateRef);
      loadUnresolvedFollowupsCmd(stateRef);
    }, 5000);
    return () => clearInterval(interval);
  }, []);

  return bag;
}


function getCmdVoiceAttachment(task) {
  const vUrl =
    task.variables?.complaint?.voiceAttachmentUrl ||
    task.variables?.complaint?.voiceUrl ||
    task.variables?.voiceAttachmentUrl ||
    task.variables?.voiceUrl ||
    task.voiceAttachmentUrl ||
    task.voiceUrl;
  const vName =
    task.variables?.complaint?.voiceAttachmentName ||
    task.variables?.complaint?.voiceName ||
    task.variables?.voiceAttachmentName ||
    task.variables?.voiceName ||
    task.voiceAttachmentName ||
    task.voiceName ||
    'Voice_Recording.webm';
  return vUrl ? { vUrl, vName } : null;
}

function otherCmdSubmitButton(selectedTask, formData, isSubmitting) {
  const hasWorkUnitRes = hasWorkUnitResolution(selectedTask);
  const isClosing = hasWorkUnitRes && formData.ccoActionChoice !== 'reassign_work_unit';
  let submitLabel = 'Submit';
  if (isSubmitting) {
    submitLabel = 'Processing...';
  } else if (isClosing) {
    submitLabel = 'Close Complaint & Notify Customer';
  } else if (hasWorkUnitRes) {
    submitLabel = 'Reassign Complaint to Work Unit';
  }
  return (
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
        backgroundColor: isClosing ? '#16a34a' : '#fa8c16',
        borderColor: isClosing ? '#16a34a' : '#fa8c16'
      }}
    >
      {submitLabel}
              </Button>
  );
}

function CmdVoiceRecordingCard({ task }) {
  const voice = getCmdVoiceAttachment(task);
  if (!voice) return null;
  const { vUrl, vName } = voice;
  return (
                    <div style={{
      background: '#f0f9ff',
      border: '1px solid #bae6fd',
      borderRadius: '8px',
      padding: '12px 16px',
      marginTop: '8px',
      display: 'flex',
      flexDirection: 'column',
      gap: '8px'
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Space align="center">
          <AudioOutlined style={{ color: '#0284c7', fontSize: '18px' }} />
          <Text style={{ fontWeight: 600, color: '#0369a1', fontSize: '14px' }}>
            Voice / Call Recording Attachment
                            </Text>
                        </Space>
        <Button
          href={ApiService.getAttachmentUrl(vUrl)}
          download={vName}
          target="_blank"
          rel="noopener noreferrer"
          size="small"
          type="primary"
          style={{ backgroundColor: '#0284c7', borderColor: '#0284c7', borderRadius: '4px' }}
        >
          Download Audio ({vName})
        </Button>
                      </div>
      <audio src={ApiService.getAttachmentUrl(vUrl)} controls style={{ width: '100%', height: '36px', marginTop: '4px' }}>
        <track kind="captions" />
      </audio>
    </div>
  );
}

function getCmdEvidenceAttachment(task) {
  const eUrl =
    task.variables?.complaint?.evidenceUrl ||
    task.variables?.complaint?.attachmentUrl ||
    task.variables?.evidenceUrl ||
    task.variables?.attachmentUrl ||
    task.evidenceUrl ||
    task.attachmentUrl;
  const eName =
    task.variables?.complaint?.evidenceName ||
    task.variables?.complaint?.attachmentName ||
    task.variables?.evidenceName ||
    task.variables?.attachmentName ||
    task.evidenceName ||
    'Supporting_Evidence_Document';
  return eUrl ? { eUrl, eName } : null;
}

function CmdEvidenceDocumentCard({ task }) {
  const evidence = getCmdEvidenceAttachment(task);
  if (!evidence) return null;
  const { eUrl, eName } = evidence;
  return (
                      <div style={{
                        background: '#f8fafc',
                        border: '1px solid #e2e8f0',
      borderRadius: '8px',
      padding: '12px 16px',
      marginTop: '8px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between'
    }}>
      <Space align="center">
        <PaperClipOutlined style={{ color: BRAND_COLORS.primary, fontSize: '18px' }} />
        <div>
          <Text style={{ fontWeight: 600, color: '#334155', fontSize: '14px', display: 'block' }}>
            Supporting Evidence Document
                        </Text>
          <Text type="secondary" style={{ fontSize: '12px' }}>{eName}</Text>
                      </div>
                      </Space>
      <a
        href={ApiService.getAttachmentUrl(eUrl)}
        download={eName}
        target="_blank"
        rel="noopener noreferrer"
        style={{
          fontSize: '13px',
          fontWeight: 600,
          color: BRAND_COLORS.primary,
          background: '#eff6ff',
          padding: '6px 12px',
          borderRadius: '4px',
          border: '1px solid #bfdbfe'
        }}
      >
        Download Document
      </a>
                    </div>
  );
}

function CmdResolutionNotesField({ selectedTask, formData, setFormData }) {
  const hasWorkUnitRes = hasWorkUnitResolution(selectedTask);
  const isClosingAfterRes = hasWorkUnitRes && (formData.ccoActionChoice === 'close_complaint' || !formData.ccoActionChoice);
  const labelText = isClosingAfterRes ? 'Resolution Summary' : 'Notes / Comments';
  const placeholderText = isClosingAfterRes
    ? 'Provide a concise summary of the final outcome communicated to the customer.'
    : 'Enter screening notes or comments...';
  return (
    <Form.Item
      label={(
        <span style={{ color: '#475569', fontSize: '14px', fontWeight: isClosingAfterRes ? 600 : 500 }}>
          {labelText} {isClosingAfterRes && <span style={{ color: '#ef4444' }}>*</span>}
        </span>
      )}
      required={isClosingAfterRes}
      style={{ marginBottom: '24px' }}
    >
      <Input.TextArea
        name="resolutionSummary"
        value={formData.resolutionSummary || formData.notes || ''}
        onChange={(e) => {
          const val = e.target.value;
          setFormData(prev => ({ ...prev, resolutionSummary: val, notes: val, additionalRemarks: val }));
        }}
        rows={3}
        placeholder={placeholderText}
        style={{ borderRadius: '8px' }}
      />
    </Form.Item>
  );
}

function isCommitteeReturnedToCco(task) {
  const v = task?.variables || {};
  return v.committeeStatus === 'ACCEPTED' ||
    v.currentStage === 'COMMITTEE_ACCEPTED' ||
    v.isCommitteeAccepted === true ||
    v.committeeStatus === 'REJECTED' ||
    v.currentStage === 'COMMITTEE_REJECTED' ||
    v.committeeDecision === 'rejected' ||
    v.isCommitteeRejected === true;
}

function isCommitteeAcceptedDecision(task) {
  const v = task?.variables || {};
  return v.committeeStatus === 'ACCEPTED' ||
    v.currentStage === 'COMMITTEE_ACCEPTED' ||
    v.isCommitteeAccepted === true ||
    v.committeeDecision === 'approved' ||
    v.committeeDecision === 'accepted';
}

function CmdCommitteeDecisionCard({ selectedTask, formData, setFormData, isSubmitting, handleCloseCommitteeDecisionCase }) {
  if (!isCommitteeReturnedToCco(selectedTask)) return null;
  const isAccepted = isCommitteeAcceptedDecision(selectedTask);
                  return (
                    <div style={{
                      background: '#ffffff',
                      borderRadius: '12px',
                      padding: '20px 24px',
                      marginBottom: '20px',
                      border: isAccepted ? '1px solid #bbf7d0' : '1px solid #fee2e2',
                      borderLeft: isAccepted ? '4px solid #16a34a' : '4px solid #ef4444',
                      boxShadow: '0 1px 3px rgba(0,0,0,0.04)'
                    }}>
                      <div style={{ marginBottom: '14px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <Space size="middle" align="start">
                          {isAccepted ? (
                            <CheckCircleOutlined style={{ color: '#16a34a', fontSize: '22px', marginTop: '2px' }} />
                          ) : (
                            <CloseCircleOutlined style={{ color: '#ef4444', fontSize: '22px', marginTop: '2px' }} />
                          )}
                          <div>
                            <Space align="center" style={{ marginBottom: '2px' }}>
                              <Text style={{ fontWeight: 700, fontSize: '16px', color: '#0f172a' }}>
                                Chief Committee Decision:
                              </Text>
                              <Tag color={isAccepted ? 'green' : 'red'} style={{ fontWeight: 700, padding: '2px 8px', borderRadius: '4px' }}>
                                {isAccepted ? 'Committee Accepted' : 'Committee Rejected'}
                              </Tag>
                            </Space>
                            <Text style={{ fontSize: '13px', color: '#64748b', display: 'block' }}>
                              Case returned to Customer Care Officer to perform final case closure &amp; customer notification.
                            </Text>
                          </div>
                        </Space>
                      </div>
                      {(selectedTask.variables?.committeeExplanation || selectedTask.variables?.decisionSummary) && (
                        <div style={{
                          background: isAccepted ? '#f0fdf4' : '#fff5f5',
                          padding: '14px 16px',
                          borderRadius: '8px',
                          border: isAccepted ? '1px solid #bbf7d0' : '1px solid #fecaca',
                          marginBottom: '16px'
                        }}>
                          <Text style={{ fontSize: '12px', display: 'block', fontWeight: 600, color: isAccepted ? '#166534' : '#991b1b', marginBottom: '4px' }}>
                            {isAccepted ? 'Committee Resolution Directions:' : 'Reason for Committee Rejection:'}
                          </Text>
                          <Text style={{ fontSize: '14px', color: isAccepted ? '#14532d' : '#7f1d1d', fontWeight: 500, lineHeight: '1.5', display: 'block' }}>
                            {selectedTask.variables?.committeeExplanation || selectedTask.variables?.decisionSummary}
                          </Text>
                        </div>
                      )}
                      <Form.Item
                        label={<span style={{ fontWeight: 600, color: '#1e293b', fontSize: '13px' }}>Resolution Summary <span style={{ color: '#ef4444' }}>*</span></span>}
                        required
                        style={{ marginBottom: '16px' }}
                      >
                        <Input.TextArea
                          rows={3}
                          placeholder="Provide a concise summary of the final outcome communicated to the customer."
                          value={formData.resolutionSummary || formData.notes || ''}
                          onChange={(e) => {
                            const val = e.target.value;
                            setFormData(prev => ({ ...prev, resolutionSummary: val, notes: val, additionalRemarks: val }));
                          }}
                          style={{ borderRadius: '6px' }}
                        />
                      </Form.Item>
                      <Button
                        type="primary"
                        danger={!isAccepted}
                        icon={isAccepted ? <CheckOutlined /> : <CloseCircleOutlined />}
                        style={{
                          backgroundColor: isAccepted ? '#16a34a' : undefined,
                          borderColor: isAccepted ? '#16a34a' : undefined,
                          borderRadius: '6px',
                          fontWeight: 600,
                          height: '38px'
                        }}
                        loading={isSubmitting}
                        onClick={() => handleCloseCommitteeDecisionCase(isAccepted)}
                      >
                        Close Case &amp; Notify Customer
                      </Button>
                    </div>
                  );
}

function CmdFcrReviewBanner({ selectedTask, isSubmitting, handleFcrVerification }) {
  if (!isFcrPendingCco(selectedTask)) {
    return null;
  }
  return (
                    <div style={{
                      background: '#ffffff',
                      borderRadius: '12px',
                      padding: '20px 24px',
                      marginBottom: '20px',
                      border: '1px solid #e2e8f0',
                      borderLeft: '4px solid #16a34a',
                      boxShadow: '0 1px 3px rgba(0,0,0,0.04)'
                    }}>
                      <div style={{ marginBottom: '14px' }}>
                        <Space size="middle" align="start">
                          <CheckCircleOutlined style={{ color: '#16a34a', fontSize: '20px', marginTop: '2px' }} />
                          <div>
                            <Text style={{ fontWeight: 700, fontSize: '16px', color: '#0f172a', display: 'block' }}>
                              First Contact Resolution (FCR) Review
                            </Text>
                            <Text style={{ fontSize: '13px', color: '#64748b' }}>
                              Resolved at intake level by Branch Staff
                            </Text>
                          </div>
                        </Space>
                      </div>

                      <div style={{
                        background: '#f8fafc',
                        padding: '14px 16px',
                        borderRadius: '8px',
                        border: '1px solid #e2e8f0',
                        marginBottom: '16px'
                      }}>
                        <Text style={{ fontSize: '12px', display: 'block', fontWeight: 600, color: '#475569', marginBottom: '4px' }}>
                          Resolution Notes recorded:
                        </Text>
                        <Text style={{ fontSize: '14px', color: '#1e293b', fontWeight: 500, lineHeight: '1.5', display: 'block' }}>
                          {selectedTask.variables?.fcrNotes || selectedTask.variables?.resolutionNotes || selectedTask.variables?.complaint?.fcrNotes || selectedTask.variables?.complaint?.resolutionNotes || 'FCR notes'}
                        </Text>
                      </div>

                      <Space size="middle" wrap>
                        <Button
                          type="primary"
                          icon={<CheckOutlined />}
                          style={{ backgroundColor: '#16a34a', borderColor: '#16a34a', borderRadius: '6px', fontWeight: 600, height: '36px' }}
                          loading={isSubmitting}
                          onClick={() => handleFcrVerification('approve')}
                        >
                          Approve FCR &amp; Close Case
                        </Button>

                        <Button
                          danger
                          icon={<CloseOutlined />}
                          style={{ borderRadius: '6px', fontWeight: 600, height: '36px' }}
                          loading={isSubmitting}
                          onClick={() => handleFcrVerification('reject')}
                        >
                          Reject FCR &amp; Route to Screening
                        </Button>
                      </Space>
                    </div>
  );
}

function CmdWorkUnitResolutionBanner({ selectedTask, formData, setFormData, isSubmitting, handleCloseAfterWorkUnit }) {
  if (!hasWorkUnitResolution(selectedTask) || formData.ccoActionChoice === 'reassign_work_unit') {
    return null;
  }
  return (
                    <div style={{
                      background: '#ffffff',
                      borderRadius: '12px',
                      padding: '20px 24px',
                      marginBottom: '20px',
                      border: '1px solid #bbf7d0',
                      borderLeft: '4px solid #16a34a',
                      boxShadow: '0 1px 3px rgba(0,0,0,0.04)'
                    }}>
                      <div style={{ marginBottom: '14px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Space size="middle" align="center">
                          <CheckCircleOutlined style={{ color: '#16a34a', fontSize: '22px' }} />
                          <div>
                            <Space align="center">
                              <Text style={{ fontWeight: 700, fontSize: '16px', color: '#0f172a' }}>
                                Work Unit Resolution Submitted
                              </Text>
                            </Space>
                            <Text style={{ fontSize: '13px', color: '#64748b', display: 'block' }}>
                              Review the resolution provided by the Work Unit and choose an action below.
                            </Text>
                          </div>
                        </Space>
                      </div>

                      <div style={{
                        background: '#f0fdf4',
                        padding: '14px 16px',
                        borderRadius: '8px',
                        border: '1px solid #bbf7d0',
                        marginBottom: '16px'
                      }}>
                        {selectedTask.variables?.resolutionDetails && (
                          <div style={{ marginBottom: '8px' }}>
                            <Text style={{ fontSize: '12px', display: 'block', fontWeight: 600, color: '#166534', marginBottom: '2px' }}>
                              Resolution Details:
                            </Text>
                            <Text style={{ fontSize: '14px', color: '#1e293b', fontWeight: 500, lineHeight: '1.5', display: 'block' }}>
                              {selectedTask.variables.resolutionDetails}
                            </Text>
                          </div>
                        )}
                        {selectedTask.variables?.actionTaken && (
                          <div>
                            <Text style={{ fontSize: '12px', display: 'block', fontWeight: 600, color: '#166534', marginBottom: '2px' }}>
                              Action Taken:
                            </Text>
                            <Text style={{ fontSize: '14px', color: '#1e293b', fontWeight: 500, lineHeight: '1.5', display: 'block' }}>
                              {selectedTask.variables.actionTaken}
                            </Text>
                          </div>
                        )}
                      </div>

                      <Form.Item
                        label={<span style={{ fontWeight: 600, color: '#1e293b', fontSize: '13px' }}>Resolution Summary <span style={{ color: '#ef4444' }}>*</span></span>}
                        required
                        style={{ marginBottom: '16px' }}
                      >
                        <Input.TextArea
                          rows={3}
                          placeholder="Provide a concise summary of the final outcome communicated to the customer."
                          value={formData.resolutionSummary || formData.notes || ''}
                          onChange={(e) => {
                            const val = e.target.value;
                            setFormData(prev => ({ ...prev, resolutionSummary: val, notes: val, additionalRemarks: val }));
                          }}
                          style={{ borderRadius: '6px' }}
                        />
                      </Form.Item>

                      {/* Direct Action Buttons */}
                      <Space size="middle" style={{ marginTop: '4px' }}>
                        <Button
                          type="primary"
                          icon={<CheckOutlined />}
                          style={{ backgroundColor: '#16a34a', borderColor: '#16a34a', borderRadius: '6px', fontWeight: 600, height: '38px' }}
                          loading={isSubmitting}
                          onClick={handleCloseAfterWorkUnit}
                        >
                          Close Complaint &amp; Notify Customer
                        </Button>

                        <Button
                          type="default"
                          icon={<SwapOutlined />}
                          style={{ borderColor: '#2563eb', color: '#2563eb', borderRadius: '6px', fontWeight: 600, height: '38px' }}
                          onClick={() => {
                            setFormData(prev => ({ ...prev, ccoActionChoice: 'reassign_work_unit' }));
                            antMessage.info('Please select target assignment details in the right panel to reassign.');
                          }}
                        >
                          Reassign Complaint
                        </Button>
                      </Space>
                    </div>
  );
}

function CmdComplaintDetailsCard({
  selectedTask, activeTab, formData, setFormData, editingFields, setEditingFields,
  handleSaveAllChanges, isSubmitting, user, handleOpenAssignModal, handleBranchChange,
  handleDistrictChange, districtsList
}) {
  return (
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
                    <Space align="center">
                      {(isSeniorCmdManager(user?.role) || user?.role === 'ROLE_ADMIN') &&
                        activeTab !== 'declined_tasks' &&
                        !isDeclinedCmdTask(selectedTask) && (
                        <Button
                          type="primary"
                          size="small"
                          icon={<UserOutlined />}
                          onClick={() => handleOpenAssignModal(selectedTask)}
                          style={{ backgroundColor: BRAND_COLORS.primary, borderColor: BRAND_COLORS.primary, borderRadius: '6px', fontWeight: 600 }}
                        >
                          {(selectedTask.assignee && String(selectedTask.assignee).toLowerCase() !== 'unassigned' && String(selectedTask.assignee).toLowerCase() !== 'null' && String(selectedTask.assignee).toLowerCase() !== 'initiator') ? 'Reassign Officer' : 'Assign Officer'}
                        </Button>
                      )}
                    </Space>
                  </div>

                  <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: '16px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Text style={{ color: '#64748b', fontSize: '14px' }}>Unique ID No</Text>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>
                          {formatUniqueId(selectedTask)}
                        </Text>
                        {formatIntakeId(selectedTask) && formatIntakeId(selectedTask) !== formatUniqueId(selectedTask) && (
                          <Text type="secondary" style={{ fontSize: '12px' }}>
                            Intake {formatIntakeId(selectedTask)}
                          </Text>
                        )}
                        {(selectedTask.variables?.committeeDecision === 'rejected' ||
                          selectedTask.variables?.committeeStatus === 'REJECTED' ||
                          selectedTask.variables?.currentStage === 'COMMITTEE_REJECTED' ||
                          selectedTask.variables?.isCommitteeRejected === true ||
                          selectedTask.committeeDecision === 'rejected') && (
                            <Tag color="magenta" style={{ fontWeight: 700, fontSize: '11px', borderRadius: '4px', margin: 0 }}>
                              Committee Rejected
                            </Tag>
                          )}
                        {(selectedTask.variables?.committeeDecision === 'approved' ||
                          selectedTask.variables?.committeeDecision === 'accepted' ||
                          selectedTask.variables?.committeeDecision === 'accept' ||
                          selectedTask.variables?.committeeStatus === 'ACCEPTED' ||
                          selectedTask.variables?.currentStage === 'COMMITTEE_ACCEPTED' ||
                          selectedTask.variables?.isCommitteeAccepted === true ||
                          selectedTask.committeeDecision === 'approved' ||
                          selectedTask.committeeDecision === 'accepted') && (
                            <Tag color="green" style={{ fontWeight: 700, fontSize: '11px', borderRadius: '4px', margin: 0 }}>
                              Committee Approved
                            </Tag>
                          )}
                      </div>
                    </div>

                    {/* 1. Customer Name (No badge, no edit icon) */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Text style={{ color: '#64748b', fontSize: '14px' }}>Customer Name</Text>
                      <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>{selectedTask.customerName || 'N/A'}</Text>
                    </div>

                    {/* 2. Preferred Contact Number (No badge, no edit icon) */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Text style={{ color: '#64748b', fontSize: '14px' }}>Preferred Contact Number</Text>
                      <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>
                        {selectedTask.variables?.customer?.phone || selectedTask.variables?.phone || 'N/A'}
                      </Text>
                    </div>

                    {/* 3. Preferred Contact Method (No edit icon) */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Text style={{ color: '#64748b', fontSize: '14px' }}>Preferred Contact Method</Text>
                      <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>
                        {selectedTask.variables?.customer?.preferredContactMethod || selectedTask.variables?.preferredContactMethod || 'Email'}
                      </Text>
                    </div>

                    {/* 4. Complaint Branch (Editable with Edit Icon in My Tasks) */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Text style={{ color: '#64748b', fontSize: '14px' }}>Complaint Branch</Text>
                      {activeTab === 'my_tasks' && editingFields.branch ? (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <Select
                            placeholder="Select Branch"
                            value={formData.branch || undefined}
                            onChange={handleBranchChange}
                            style={{ minWidth: '180px' }}
                            size="small"
                          >
                            {districtsList.flatMap(d => d.branches).map(br => (
                              <Option key={br.id || br.name} value={br.name}>{br.name}</Option>
                            ))}
                          </Select>
                          <Button
                            type="text"
                            size="small"
                            icon={<CheckOutlined style={{ color: '#52c41a' }} />}
                            onClick={() => setEditingFields(p => ({ ...p, branch: false }))}
                          />
                        </div>
                      ) : (
                        <div style={{ display: 'flex', alignItems: 'center' }}>
                          <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>
                            {formData.branch || selectedTask.variables?.complaint?.branch || selectedTask.variables?.branch || 'N/A'}
                          </Text>
                          {activeTab === 'my_tasks' && (
                            <Tooltip title="Edit Complaint Branch">
                              <EditOutlined
                                style={{ color: BRAND_COLORS.primary, cursor: 'pointer', marginLeft: '8px', fontSize: '14px' }}
                                onClick={() => setEditingFields(p => ({ ...p, branch: true }))}
                              />
                            </Tooltip>
                          )}
                        </div>
                      )}
                    </div>

                    {/* 5. District (Editable with Edit Icon in My Tasks) */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Text style={{ color: '#64748b', fontSize: '14px' }}>District</Text>
                      {activeTab === 'my_tasks' && editingFields.district ? (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <Select
                            placeholder="Select District"
                            value={formData.district || undefined}
                            onChange={handleDistrictChange}
                            style={{ minWidth: '180px' }}
                            size="small"
                          >
                            {districtsList.map(dist => (
                              <Option key={dist.id || dist.name} value={dist.name}>{dist.name}</Option>
                            ))}
                          </Select>
                          <Button
                            type="text"
                            size="small"
                            icon={<CheckOutlined style={{ color: '#52c41a' }} />}
                            onClick={() => setEditingFields(p => ({ ...p, district: false }))}
                          />
                        </div>
                      ) : (
                        <div style={{ display: 'flex', alignItems: 'center' }}>
                          <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>
                            {formData.district || selectedTask.variables?.district || selectedTask.variables?.complaint?.district || 'N/A'}
                          </Text>
                          {activeTab === 'my_tasks' && (
                            <Tooltip title="Edit District">
                              <EditOutlined
                                style={{ color: BRAND_COLORS.primary, cursor: 'pointer', marginLeft: '8px', fontSize: '14px' }}
                                onClick={() => setEditingFields(p => ({ ...p, district: true }))}
                              />
                            </Tooltip>
                          )}
                        </div>
                      )}
                    </div>

                    {/* 6. Details of the Complaint (Editable with Edit Icon in My Tasks) */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <Text style={{ color: '#64748b', fontSize: '14px' }}>Details of the Complaint</Text>
                      {activeTab === 'my_tasks' && editingFields.description ? (
                        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '6px' }}>
                          <Input.TextArea
                            rows={3}
                            value={formData.complaintDescription}
                            onChange={(e) => setFormData(p => ({ ...p, complaintDescription: e.target.value }))}
                            style={{ width: '280px', borderRadius: '6px' }}
                          />
                          <Button
                            type="primary"
                            size="small"
                            icon={<CheckOutlined />}
                            onClick={() => setEditingFields(p => ({ ...p, description: false }))}
                            style={{ borderRadius: '4px' }}
                          >
                            Done
                          </Button>
                        </div>
                      ) : (
                        <div style={{ display: 'flex', alignItems: 'flex-start' }}>
                          <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 500, textAlign: 'right', maxWidth: '260px' }}>
                            {formData.complaintDescription || selectedTask.variables?.complaint?.description || selectedTask.variables?.description || 'N/A'}
                          </Text>
                          {activeTab === 'my_tasks' && (
                            <Tooltip title="Edit Complaint Details">
                              <EditOutlined
                                style={{ color: BRAND_COLORS.primary, cursor: 'pointer', marginLeft: '8px', marginTop: '3px', fontSize: '14px' }}
                                onClick={() => setEditingFields(p => ({ ...p, description: true }))}
                              />
                            </Tooltip>
                          )}
                        </div>
                      )}
                    </div>

                    <CmdVoiceRecordingCard task={selectedTask} />
                    <CmdEvidenceDocumentCard task={selectedTask} />

                    {/* Work Unit Resolution & Action Taken Section */}
                    {/* Separator Line & Complaint Information Section */}
                    {activeTab !== 'declined_tasks' && selectedTask?.variables?.targetTab !== 'Declined' && selectedTask?.status !== 'DECLINED' && selectedTask?.classification !== 'DECLINED' && (
                      <div style={{ borderTop: '1px solid #e2e8f0', marginTop: '16px', paddingTop: '16px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
                        <Text style={{ fontSize: '15px', fontWeight: 700, color: BRAND_COLORS.primary, display: 'block', marginBottom: '2px' }}>
                          Complaint Information
                        </Text>

                        {/* Complaint Category */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Text style={{ color: '#64748b', fontSize: '14px' }}>Complaint Category</Text>
                          {activeTab === 'my_tasks' ? (
                            <Select
                              value={formData.complaintCategory || 'Customer Service Issues'}
                              onChange={(value) => setFormData(prev => ({ ...prev, complaintCategory: value }))}
                              style={{ minWidth: '220px' }}
                            >
                              <Option value="Customer Service Issues">Customer Service Issues</Option>
                              <Option value="Transaction Error">Transaction Error</Option>
                              <Option value="Account Management">Account Management</Option>
                              <Option value="Banking App Issues">Banking App Issues</Option>
                              <Option value="Credit/Financing Concerns">Credit/Financing Concerns</Option>
                              <Option value="Fraud & Security Risk">Fraud & Security Risk</Option>
                              <Option value="Information Disclosure">Information Disclosure</Option>
                              <Option value="ATM & Card Banking Issues">ATM & Card Banking Issues</Option>
                              <Option value="Policy & Compliance Disputes">Policy & Compliance Disputes</Option>
                              <Option value="System Failure">System Failure</Option>
                              <Option value="Branch Operation">Branch Operation</Option>
                            </Select>
                          ) : (
                            <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>
                              {formData.complaintCategory || selectedTask.variables?.complaintCategory || selectedTask.variables?.complaint?.category || 'Customer Service Issues'}
                            </Text>
                          )}
                        </div>

                        {/* Service Type */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Text style={{ color: '#64748b', fontSize: '14px' }}>Service Type</Text>
                          {activeTab === 'my_tasks' ? (
                            <Select
                              value={formData.serviceType || 'Digital Banking'}
                              onChange={(value) => setFormData(prev => ({ ...prev, serviceType: value }))}
                              style={{ minWidth: '220px' }}
                            >
                              <Option value="Deposit Account">Deposit Account</Option>
                              <Option value="Credit/Financing">Credit/Financing</Option>
                              <Option value="Digital Banking">Digital Banking</Option>
                              <Option value="ATM & Card Banking">ATM & Card Banking</Option>
                              <Option value="RTGs">RTGs</Option>
                              <Option value="International Banking">International Banking</Option>
                              <Option value="Vendor/Procurement">Vendor/Procurement</Option>
                              <Option value="Incoming Fund Transfer">Incoming Fund Transfer</Option>
                              <Option value="Outgoing Fund Transfer">Outgoing Fund Transfer</Option>
                              <Option value="Merchant Service via Super App">Merchant Service via Super App</Option>
                              <Option value="Interest Free Banking Services">Interest Free Banking Services</Option>
                              <Option value="Other">Other</Option>
                            </Select>
                          ) : (
                            <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>
                              {formData.serviceType || selectedTask.variables?.serviceType || selectedTask.variables?.complaint?.serviceType || 'Digital Banking'}
                            </Text>
                          )}
                        </div>

                        {/* Complaint Made On */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Text style={{ color: '#64748b', fontSize: '14px' }}>Complaint Made On</Text>
                          {activeTab === 'my_tasks' ? (
                            <Select
                              value={formData.complaintMadeOn || 'Branch'}
                              onChange={(value) => setFormData(prev => ({ ...prev, complaintMadeOn: value }))}
                              style={{ minWidth: '220px' }}
                            >
                              <Option value="Agent">Agent</Option>
                              <Option value="ATM">ATM</Option>
                              <Option value="Branch">Branch</Option>
                              <Option value="District Office">District Office</Option>
                              <Option value="HO">HO</Option>
                              <Option value="Super App">Super App</Option>
                              <Option value="Merchant">Merchant</Option>
                            </Select>
                          ) : (
                            <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>
                              {formData.complaintMadeOn || selectedTask.variables?.complaintMadeOn || selectedTask.variables?.channel || 'Branch'}
                            </Text>
                          )}
                        </div>

                        {/* Received By */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Text style={{ color: '#64748b', fontSize: '14px' }}>Received By</Text>
                          {activeTab === 'my_tasks' ? (
                            <Select
                              value={formData.receivedBy || 'Customer Care-Telephone'}
                              onChange={(value) => setFormData(prev => ({ ...prev, receivedBy: value }))}
                              style={{ minWidth: '220px' }}
                            >
                              <Option value="Customer Care-Telephone">Customer Care-Telephone</Option>
                              <Option value="Customer Care-In person">Customer Care-In person</Option>
                              <Option value="Contact Center">Contact Center</Option>
                              <Option value="Contact Center-Email">Contact Center-Email</Option>
                              <Option value="Digital Marketing">Digital Marketing</Option>
                              <Option value="HO">HO</Option>
                              <Option value="Branch">Branch</Option>
                              <Option value="District">District</Option>
                            </Select>
                          ) : (
                            <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600 }}>
                              {formData.receivedBy || selectedTask.variables?.receivedBy || 'Customer Care-Telephone'}
                            </Text>
                          )}
                        </div>

                        {activeTab === 'my_tasks' && (
                          <div style={{ paddingTop: '16px', borderTop: '1px solid #e2e8f0', marginTop: '16px', display: 'flex', justifyContent: 'flex-end' }}>
                            <Button
                              type="primary"
                              icon={<SaveOutlined />}
                              loading={isSubmitting}
                              onClick={handleSaveAllChanges}
                              style={{
                                backgroundColor: BRAND_COLORS.primary,
                                borderColor: BRAND_COLORS.primary,
                                borderRadius: '6px',
                                fontWeight: 600,
                                height: '40px',
                                padding: '0 24px',
                                fontSize: '14px'
                              }}
                            >
                              Save All Changes
                            </Button>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
  );
}

function CmdAuditInvestigationCard({ selectedTask }) {
  if (!selectedTask.variables?.auditFindings) {
    return null;
  }
  return (
                  <div style={{
                    background: '#f8fafc',
                    borderRadius: '16px',
                    padding: '24px',
                    marginBottom: '20px',
                    border: '1px solid #f1f5f9'
                  }}>
                    <div style={{ marginBottom: '16px' }}>
                      <Text style={{ fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
                        Audit Investigation Details
                      </Text>
                    </div>

                    <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: '16px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <Text style={{ color: '#64748b', fontSize: '14px' }}>Justification</Text>
                        <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 500, textAlign: 'right', maxWidth: '280px' }}>
                          {selectedTask.variables.auditJustification || 'N/A'}
                        </Text>
                      </div>

                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <Text style={{ color: '#64748b', fontSize: '14px' }}>Findings</Text>
                        <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 500, textAlign: 'right', maxWidth: '280px' }}>
                          {selectedTask.variables.auditFindings || 'N/A'}
                        </Text>
                      </div>

                      {(selectedTask.variables?.auditAttachment || selectedTask.variables?.auditAttachmentName) && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '8px' }}>
                          <Text style={{ color: '#64748b', fontSize: '14px' }}>Investigation Attachment</Text>
                          <a
                            href={ApiService.getAttachmentUrl(
                              selectedTask.variables.auditAttachment ||
                              `/api/complaints/attachments/${selectedTask.variables.auditAttachmentName}`
                            )}
                            download={selectedTask.variables.auditAttachmentName || 'audit_attachment'}
                            target="_blank"
                            rel="noopener noreferrer"
                            style={{ fontSize: '14px', fontWeight: 600, color: BRAND_COLORS.primary }}
                          >
                            {selectedTask.variables.auditAttachmentName || 'Download Attachment'}
                          </a>
                        </div>
                      )}
                    </div>
                  </div>
  );
}

function CmdCustomerProfileCard({ selectedTask, activeTab, editingFields, setEditingFields, formData, setFormData }) {
  if (!selectedTask.variables?.customer) {
    return null;
  }
  return (
                    <div style={{
                      background: '#f8fafc',
                      borderRadius: '16px',
                      padding: '24px',
                      marginBottom: '20px',
                      border: '1px solid #e2e8f0',
                      boxShadow: '0 1px 3px rgba(0,0,0,0.03)'
                    }}>
                      <div style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <UserOutlined style={{ color: BRAND_COLORS.primary, fontSize: '18px' }} />
                        <Text style={{ fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
                          Customer Profile
                        </Text>
                      </div>

                      <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: '16px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Text style={{ color: '#64748b', fontSize: '14px' }}>Customer Segment</Text>
                          <Tag color={selectedTask.variables.customer.customerSegment === 'Corporate' ? 'gold' : 'blue'} style={{ fontWeight: 600, margin: 0 }}>
                            {selectedTask.variables.customer.customerSegment || 'Retail'}
                          </Tag>
                        </div>

                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Text style={{ color: '#64748b', fontSize: '14px' }}>Account Number</Text>
                          {activeTab === 'my_tasks' && editingFields.accountNumber ? (
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                              <Input
                                value={formData.accountNumber}
                                onChange={(e) => setFormData(p => ({ ...p, accountNumber: e.target.value }))}
                                style={{ width: '160px', fontFamily: 'monospace', borderRadius: '4px' }}
                                size="small"
                              />
                              <Button
                                type="text"
                                size="small"
                                icon={<CheckOutlined style={{ color: '#52c41a' }} />}
                                onClick={() => setEditingFields(p => ({ ...p, accountNumber: false }))}
                              />
                            </div>
                          ) : (
                            <div style={{ display: 'flex', alignItems: 'center' }}>
                              <Text style={{ color: '#0f172a', fontSize: '14px', fontWeight: 600, fontFamily: 'monospace' }}>
                                {formData.accountNumber || selectedTask.variables?.customer?.accountNumber || selectedTask.variables?.accountNumber || 'N/A'}
                              </Text>
                              {activeTab === 'my_tasks' && (
                                <Tooltip title="Edit Account Number">
                                  <EditOutlined
                                    style={{ color: BRAND_COLORS.primary, cursor: 'pointer', marginLeft: '8px', fontSize: '14px' }}
                                    onClick={() => setEditingFields(p => ({ ...p, accountNumber: true }))}
                                  />
                                </Tooltip>
                              )}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
  );
}

function shouldShowCmdScreening(activeTab, formData, selectedTask) {
  return (activeTab === 'my_tasks' || formData.ccoActionChoice === 'reassign_work_unit')
    && !isFcrPendingCco(selectedTask)
    && selectedTask?.variables?.status !== 'CLOSED'
    && selectedTask?.variables?.status !== 'DECLINED';
}

function CmdDeclineReasonSection({ formData, setFormData }) {
  if (!formData.isDeclining) {
    return null;
  }
  return (
                              <div style={{ background: '#fff1f0', border: '1px solid #ffa39e', borderRadius: '8px', padding: '16px', marginBottom: '16px' }}>
                                <Text strong style={{ color: '#cf1322', fontSize: '14px', display: 'block', marginBottom: '8px' }}>
                                  Decline Complaint
                                </Text>
                                <Form.Item label={<span style={{ color: '#475569', fontSize: '13px', fontWeight: 600 }}>Decline Reason <span style={{ color: '#ef4444' }}>*</span></span>} required style={{ marginBottom: '12px' }}>
                                  <Input.TextArea
                                    rows={3}
                                    placeholder="Enter mandatory reason for declining this complaint..."
                                    value={formData.declineReason}
                                    onChange={(e) => setFormData(prev => ({ ...prev, declineReason: e.target.value }))}
                                    style={{ borderRadius: '6px' }}
                                  />
                                </Form.Item>

                                <Form.Item label={<span style={{ color: '#475569', fontSize: '13px', fontWeight: 500 }}>Additional Remarks (Optional)</span>} style={{ marginBottom: 0 }}>
                                  <Input.TextArea
                                    rows={2}
                                    placeholder="Enter any additional remarks..."
                                    value={formData.additionalRemarks}
                                    onChange={(e) => setFormData(prev => ({ ...prev, additionalRemarks: e.target.value }))}
                                    style={{ borderRadius: '6px' }}
                                  />
                                </Form.Item>
                              </div>
  );
}

function CmdScreeningSubmitActions({ selectedTask, formData, setFormData, isSubmitting }) {
  let actions;
  if (formData.classification === 'OTHER') {
    actions = otherCmdSubmitButton(selectedTask, formData, isSubmitting);
  } else if (formData.isDeclining) {
    actions = (
                                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    <Button
                                      type="primary"
                                      danger
                                      htmlType="submit"
                                      loading={isSubmitting}
                                      size="large"
                                      style={{ width: '100%', borderRadius: '8px', height: '44px', fontWeight: 600 }}
                                    >
                                      {isSubmitting ? 'Processing...' : 'Submit'}
                                    </Button>
                                    <Button
                                      type="default"
                                      size="small"
                                      onClick={() => setFormData(prev => ({ ...prev, isDeclining: false, declineReason: '' }))}
                                      style={{ borderRadius: '6px' }}
                                    >
                                      Cancel Decline
                                    </Button>
                                  </div>
    );
  } else {
    actions = (
                                  <div style={{ display: 'flex', gap: '12px' }}>
                                    <Button
                                      type="primary"
                                      htmlType="submit"
                                      loading={isSubmitting}
                                      size="large"
                                      style={{ flex: 1, borderRadius: '8px', height: '44px', fontWeight: 600, backgroundColor: BRAND_COLORS.primary, borderColor: BRAND_COLORS.primary }}
                                    >
                                      {isSubmitting ? 'Processing...' : 'Submit'}
                                    </Button>
                                    <Button
                                      type="primary"
                                      danger
                                      size="large"
                                      icon={<WarningOutlined />}
                                      onClick={() => setFormData(prev => ({ ...prev, isDeclining: true }))}
                                      style={{ borderRadius: '8px', height: '44px', fontWeight: 600 }}
                                    >
                                      Decline Complaint
                                    </Button>
                                  </div>
    );
  }
  return (
                            <div style={{ marginTop: '24px' }}>
                              {actions}
                            </div>
  );
}

function CmdScreeningFormCard(props) {
  const {
    selectedTask, activeTab, formData, setFormData, isSubmitting, districtsList,
    investigationFileList, setInvestigationFileList, handleSelectChange, handleDistrictChange,
    handleBranchChange, handleSaveClassification, handleSubmit
  } = props;
  if (!shouldShowCmdScreening(activeTab, formData, selectedTask)) {
    return null;
  }
  return (
                      <div style={{
                        background: '#f8fafc',
                        borderRadius: '16px',
                        padding: '24px',
                        border: '1px solid #f1f5f9'
                      }}>
                        <div style={{ marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Text style={{ fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
                            {formData.ccoActionChoice === 'reassign_work_unit' ? 'Reassign Complaint to Work Unit' : 'CMD Screening'}
                          </Text>
                          {formData.ccoActionChoice === 'reassign_work_unit' && (
                            <Button
                              type="link"
                              size="small"
                              onClick={() => setFormData(prev => ({ ...prev, ccoActionChoice: 'close_complaint' }))}
                              style={{ color: '#2563eb', fontWeight: 600, padding: 0 }}
                            >
                              ← Back to Resolution Review
                            </Button>
                          )}
                        </div>

                        <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: '16px' }}>
                          <Form onFinish={handleSubmit} layout="vertical">
                            {/* Case Classification Section - Hidden when reassigning to Work Unit */}
                            {formData.ccoActionChoice !== 'reassign_work_unit' && (
                              <Form.Item label={<span style={{ color: '#475569', fontSize: '14px', fontWeight: 600 }}>Case Classification <span style={{ color: '#ef4444' }}>*</span></span>} required style={{ marginBottom: '16px' }}>
                                <Radio.Group
                                  value={formData.classification || 'COMPLAINT'}
                                  onChange={(e) => setFormData(prev => ({ ...prev, classification: e.target.value, isDeclining: false, declineReason: '' }))}
                                  style={{ width: '100%' }}
                                >
                                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', width: '100%' }}>
                                    <button
                                      type="button"
                                      onClick={() => setFormData(prev => ({ ...prev, classification: 'COMPLAINT', isDeclining: false, declineReason: '' }))}
                                      style={{
                                        border: (formData.classification || 'COMPLAINT') === 'COMPLAINT' ? '2px solid #2563eb' : '1px solid #cbd5e1',
                                        background: (formData.classification || 'COMPLAINT') === 'COMPLAINT' ? '#eff6ff' : '#ffffff',
                                        borderRadius: '10px',
                                        padding: '12px 16px',
                                        cursor: 'pointer',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                        transition: 'all 0.2s ease',
                                        boxShadow: (formData.classification || 'COMPLAINT') === 'COMPLAINT' ? '0 2px 8px rgba(37,99,235,0.12)' : 'none',
                                        width: '100%',
                                        textAlign: 'left',
                                        font: 'inherit'
                                      }}
                                    >
                                      <Radio value="COMPLAINT" style={{ fontWeight: 600, fontSize: '14px', color: (formData.classification || 'COMPLAINT') === 'COMPLAINT' ? '#1e40af' : '#334155' }}>
                                        Complaint
                                      </Radio>
                                      {(formData.classification || 'COMPLAINT') === 'COMPLAINT' && (
                                        <CheckCircleFilled style={{ color: '#2563eb', fontSize: '16px' }} />
                                      )}
                                    </button>

                                    <button
                                      type="button"
                                      onClick={() => setFormData(prev => ({ ...prev, classification: 'OTHER', isDeclining: false, declineReason: '' }))}
                                      style={{
                                        border: formData.classification === 'OTHER' ? '2px solid #2563eb' : '1px solid #cbd5e1',
                                        background: formData.classification === 'OTHER' ? '#eff6ff' : '#ffffff',
                                        borderRadius: '10px',
                                        padding: '12px 16px',
                                        cursor: 'pointer',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                        transition: 'all 0.2s ease',
                                        boxShadow: formData.classification === 'OTHER' ? '0 2px 8px rgba(37,99,235,0.12)' : 'none',
                                        width: '100%',
                                        textAlign: 'left',
                                        font: 'inherit'
                                      }}
                                    >
                                      <Radio value="OTHER" style={{ fontWeight: 600, fontSize: '14px', color: formData.classification === 'OTHER' ? '#1e40af' : '#334155' }}>
                                        Other
                                      </Radio>
                                      {formData.classification === 'OTHER' && (
                                        <CheckCircleFilled style={{ color: '#2563eb', fontSize: '16px' }} />
                                      )}
                                    </button>
                                  </div>
                                </Radio.Group>

                                <div style={{ marginTop: '12px' }}>
                                  <Button
                                    type="default"
                                    size="small"
                                    onClick={handleSaveClassification}
                                    style={{
                                      borderColor: '#2563eb',
                                      color: '#2563eb',
                                      fontWeight: 600,
                                      borderRadius: '6px',
                                      fontSize: '13px',
                                      padding: '2px 16px',
                                      height: '30px'
                                    }}
                                  >
                                    Save
                                  </Button>
                                </div>
                              </Form.Item>
                            )}

                            <CmdDeclineReasonSection formData={formData} setFormData={setFormData} />

                            {/* Routing & Details options if NOT declining and classification is NOT OTHER */}
                            {!formData.isDeclining && formData.classification !== 'OTHER' && (
                              <>
                                <div style={{ borderTop: '1px solid #e2e8f0', margin: '28px 0' }} />

                                {/* Work Unit Resolution Review Options for Customer Care Officer */}
                                {Boolean(selectedTask.variables?.resolutionDetails || selectedTask.variables?.actionTaken || selectedTask.variables?.workUnitResolutionGiven || selectedTask.variables?.resolutionProvided) && (
                                  <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '8px', padding: '16px', marginBottom: '20px' }}>
                                    <Text strong style={{ color: '#166534', fontSize: '14px', display: 'block', marginBottom: '8px' }}>
                                      Review Work Unit Resolution &amp; Choose Action:
                                    </Text>
                                    <Radio.Group
                                      value={formData.ccoActionChoice || 'close_complaint'}
                                      onChange={(e) => setFormData(prev => ({ ...prev, ccoActionChoice: e.target.value }))}
                                      style={{ width: '100%' }}
                                    >
                                      <Space direction="vertical" style={{ width: '100%' }}>
                                        <Radio value="close_complaint" style={{ fontSize: '13px', fontWeight: 600, color: '#15803d' }}>
                                          Close Complaint &amp; Notify Customer
                                        </Radio>
                                        <Radio value="reassign_work_unit" style={{ fontSize: '13px', fontWeight: 600, color: '#1e40af' }}>
                                          Reassign Complaint to Work Unit for Additional Investigation / Resolution
                                        </Radio>
                                      </Space>
                                    </Radio.Group>
                                  </div>
                                )}

                                {selectedTask?.variables?.committeeDecision !== 'approved' && !(selectedTask.variables?.resolutionDetails || selectedTask.variables?.workUnitResolutionGiven) && (
                                  <Form.Item style={{ marginBottom: '16px' }}>
                                    <Checkbox
                                      name="requiresInvestigation"
                                      checked={formData.requiresInvestigation}
                                      onChange={(e) => {
                                        const checked = e.target.checked;
                                        setFormData(prev => ({
                                          ...prev,
                                          requiresInvestigation: checked,
                                          ...(checked ? {
                                            district: '',
                                            branch: '',
                                            department: '',
                                            manager: ''
                                          } : {})
                                        }));
                                      }}
                                    >
                                      <span style={{ fontWeight: 600, color: '#0f172a', fontSize: '14px' }}>Requires Investigation</span>
                                    </Checkbox>
                                  </Form.Item>
                                )}

                                {/* Upload Multiple Investigation Evidence Files */}
                                {formData.requiresInvestigation && (
                                  <div style={{ marginTop: '8px', marginBottom: '24px', backgroundColor: '#f8fafc', padding: '16px', borderRadius: '8px', border: '1px dashed #cbd5e1' }}>
                                    <Text strong style={{ color: '#0f172a', fontSize: '14px', display: 'block', marginBottom: '4px' }}>
                                      Upload Investigation Evidence &amp; Documents (Multiple Files)
                                    </Text>
                                    <Text style={{ color: '#64748b', fontSize: '12px', display: 'block', marginBottom: '12px' }}>
                                      Attach multiple evidence files, documents, screenshots, or receipts for the Chief Experience Officer review.
                                    </Text>
                                    <Upload
                                      multiple
                                      beforeUpload={(file) => {
                                        const reader = new FileReader();
                                        reader.onload = () => {
                                          const fileObj = {
                                            uid: file.uid || `${Date.now()}-${file.name}`,
                                            name: file.name,
                                            size: file.size,
                                            type: file.type,
                                            url: reader.result,
                                            status: 'done'
                                          };
                                          setInvestigationFileList(prev => [...prev, fileObj]);
                                        };
                                        reader.readAsDataURL(file);
                                        return false; // Prevent auto HTTP upload
                                      }}
                                      onRemove={(file) => {
                                        setInvestigationFileList(prev => prev.filter(f => f.uid !== file.uid && f.name !== file.name));
                                      }}
                                      fileList={investigationFileList}
                                    >
                                      <Button icon={<UploadOutlined />} style={{ borderRadius: '6px' }}>
                                        Select Multiple Evidence Files
                                      </Button>
                                    </Upload>
                                  </div>
                                )}

                                {!formData.requiresInvestigation && (
                                  (!(selectedTask.variables?.resolutionDetails || selectedTask.variables?.actionTaken || selectedTask.variables?.workUnitResolutionGiven || selectedTask.variables?.resolutionProvided)
                                    || formData.ccoActionChoice === 'reassign_work_unit')
                                ) && (
                                    <>
                                      <div style={{ borderTop: '1px solid #e2e8f0', margin: '28px 0' }} />

                                      <div style={{ marginBottom: '20px' }}>
                                        <Text strong style={{ color: '#0f172a', fontSize: '15px', letterSpacing: '0.3px', display: 'block', marginBottom: '4px' }}>
                                          Assignment Details
                                        </Text>
                                        <Text style={{ color: '#64748b', fontSize: '12px' }}>
                                          Select the target organizational unit responsible for handling this complaint.
                                        </Text>
                                      </div>

                                      {/* Assignment Scope Radio Buttons */}
                                      <Form.Item label={<span style={{ color: '#475569', fontSize: '14px', fontWeight: 600 }}>Assignment Scope</span>} required style={{ marginBottom: '24px' }}>
                                        <Radio.Group
                                          value={formData.assignmentType || 'DISTRICT_BRANCH'}
                                          onChange={(e) => {
                                            const val = e.target.value;
                                            setFormData(prev => ({
                                              ...prev,
                                              assignmentType: val,
                                              district: '',
                                              branch: '',
                                              department: '',
                                              manager: ''
                                            }));
                                          }}
                                          style={{ width: '100%' }}
                                        >
                                          <Space direction="vertical" style={{ width: '100%' }} size="small">
                                            <Radio value="DISTRICT_DEPARTMENT" style={{ fontSize: '14px', color: '#1e293b', fontWeight: 500 }}>
                                              District Department
                                            </Radio>
                                            <Radio value="DISTRICT_BRANCH" style={{ fontSize: '14px', color: '#1e293b', fontWeight: 500 }}>
                                              Branch
                                            </Radio>
                                            <Radio value="HQ_DEPARTMENT" style={{ fontSize: '14px', color: '#1e293b', fontWeight: 500 }}>
                                              Head Office Department
                                            </Radio>
                                          </Space>
                                        </Radio.Group>
                                      </Form.Item>

                                      {/* District Department Option */}
                                      {formData.assignmentType === 'DISTRICT_DEPARTMENT' && (
                                        <>
                                          <Form.Item label={<span style={{ color: '#475569', fontSize: '14px', fontWeight: 500 }}>District <span style={{ color: '#ef4444' }}>*</span></span>} required style={{ marginBottom: '24px' }}>
                                            <Select
                                              placeholder="Select District"
                                              value={formData.district || undefined}
                                              onChange={(val) => {
                                                setFormData(prev => ({
                                                  ...prev,
                                                  district: val,
                                                  department: '',
                                                  manager: 'District Director & Department Manager'
                                                }));
                                              }}
                                              style={{ width: '100%' }}
                                            >
                                              {districtsList.map(dist => (
                                                <Option key={dist.id} value={dist.name}>{dist.name}</Option>
                                              ))}
                                            </Select>
                                          </Form.Item>

                                          <Form.Item label={<span style={{ color: '#475569', fontSize: '14px', fontWeight: 500 }}>Department <span style={{ color: '#ef4444' }}>*</span></span>} required style={{ marginBottom: '24px' }}>
                                            <Select
                                              placeholder={formData.district ? "Select Department" : "Select District first"}
                                              value={formData.department || undefined}
                                              onChange={(val) => {
                                                setFormData(prev => ({
                                                  ...prev,
                                                  department: val,
                                                  manager: `District Director & Manager (${val})`
                                                }));
                                              }}
                                              disabled={!formData.district}
                                              style={{ width: '100%' }}
                                            >
                                              {['Operations Department', 'District Audit', 'ATM Operations', 'Digital Banking', 'Credit Department', 'Customer Service', 'Fraud Investigation'].map(dept => (
                                                <Option key={dept} value={dept}>{dept}</Option>
                                              ))}
                                            </Select>
                                          </Form.Item>
                                        </>
                                      )}

                                      {/* Branch Option */}
                                      {(formData.assignmentType === 'DISTRICT_BRANCH' || !formData.assignmentType) && (
                                        <>
                                          <Form.Item label={<span style={{ color: '#475569', fontSize: '14px', fontWeight: 500 }}>District <span style={{ color: '#ef4444' }}>*</span></span>} required style={{ marginBottom: '24px' }}>
                                            <Select
                                              placeholder="Select District"
                                              value={formData.district || undefined}
                                              onChange={handleDistrictChange}
                                              style={{ width: '100%' }}
                                            >
                                              {districtsList.map(dist => (
                                                <Option key={dist.id} value={dist.name}>{dist.name}</Option>
                                              ))}
                                            </Select>
                                          </Form.Item>

                                          <Form.Item label={<span style={{ color: '#475569', fontSize: '14px', fontWeight: 500 }}>Branch <span style={{ color: '#ef4444' }}>*</span></span>} required style={{ marginBottom: '24px' }}>
                                            <Select
                                              placeholder={formData.district ? "Select Branch" : "Select District first"}
                                              value={formData.branch || undefined}
                                              onChange={handleBranchChange}
                                              disabled={!formData.district}
                                              style={{ width: '100%' }}
                                            >
                                              {formData.district && districtsList.find(d => d.name === formData.district)?.branches.map(br => (
                                                <Option key={br.id} value={br.name}>{br.name} ({br.code})</Option>
                                              ))}
                                            </Select>
                                          </Form.Item>
                                        </>
                                      )}

                                      {/* Head Office Department Option */}
                                      {formData.assignmentType === 'HQ_DEPARTMENT' && (
                                          <Form.Item label={<span style={{ color: '#475569', fontSize: '14px', fontWeight: 500 }}>Department <span style={{ color: '#ef4444' }}>*</span></span>} required style={{ marginBottom: '24px' }}>
                                            <Select
                                              placeholder="Select Department"
                                              value={formData.department || undefined}
                                              onChange={(val) => {
                                                setFormData(prev => ({
                                                  ...prev,
                                                  district: 'Head Office',
                                                  department: val,
                                                  manager: `Department Director & Manager (${val})`
                                                }));
                                              }}
                                              style={{ width: '100%' }}
                                            >
                                              {['ATM Operations', 'Digital Banking', 'Card Operations', 'Credit Department', 'Operations Department', 'Customer Experience', 'Fraud Investigation', 'Trade Finance & Foreign Exchange', 'Internal Audit & Compliance'].map(dept => (
                                                <Option key={dept} value={dept}>{dept}</Option>
                                              ))}
                                            </Select>
                                          </Form.Item>
                                      )}
                                    </>
                                  )}

                                <div style={{ borderTop: '1px solid #e2e8f0', margin: '28px 0' }} />

                                <Form.Item label={<span style={{ color: '#475569', fontSize: '14px', fontWeight: 500 }}>Complaint Classification</span>} required style={{ marginBottom: '24px' }}>
                                  <Select
                                    value={formData.complaintClassification}
                                    onChange={(value) => handleSelectChange('complaintClassification', value)}
                                    style={{ width: '100%' }}
                                  >
                                    <Option value="General">General</Option>
                                    <Option value="Sensitive">Sensitive</Option>
                                    <Option value="Highly Sensitive">Highly Sensitive</Option>
                                  </Select>
                                </Form.Item>

                                <CmdResolutionNotesField selectedTask={selectedTask} formData={formData} setFormData={setFormData} />
                              </>
                            )}

                            <CmdScreeningSubmitActions
                              selectedTask={selectedTask}
                              formData={formData}
                              setFormData={setFormData}
                              isSubmitting={isSubmitting}
                            />
                          </Form>
                        </div>
                      </div>
  );
}

function CmdComplaintInfoColumn(props) {
  const {
    selectedTask, activeTab, formData, setFormData, editingFields, setEditingFields,
    handleSaveAllChanges, isSubmitting, handleFcrVerification, handleCloseCommitteeDecisionCase,
    handleCloseAfterWorkUnit, user, handleOpenAssignModal, handleBranchChange, handleDistrictChange,
    districtsList
  } = props;
  const wideColumn = activeTab === 'my_tasks' || activeTab === 'all_tasks' || activeTab === 'fcr_tasks';
  return (
    <Col xs={24} lg={wideColumn ? 13 : 24}>
      <CmdFcrReviewBanner selectedTask={selectedTask} isSubmitting={isSubmitting} handleFcrVerification={handleFcrVerification} />
      <CmdCommitteeDecisionCard
        selectedTask={selectedTask}
        formData={formData}
        setFormData={setFormData}
        isSubmitting={isSubmitting}
        handleCloseCommitteeDecisionCase={handleCloseCommitteeDecisionCase}
      />
      <CmdWorkUnitResolutionBanner
        selectedTask={selectedTask}
        formData={formData}
        setFormData={setFormData}
        isSubmitting={isSubmitting}
        handleCloseAfterWorkUnit={handleCloseAfterWorkUnit}
      />
      <CmdComplaintDetailsCard
        selectedTask={selectedTask}
        activeTab={activeTab}
        formData={formData}
        setFormData={setFormData}
        editingFields={editingFields}
        setEditingFields={setEditingFields}
        handleSaveAllChanges={handleSaveAllChanges}
        isSubmitting={isSubmitting}
        user={user}
        handleOpenAssignModal={handleOpenAssignModal}
        handleBranchChange={handleBranchChange}
        handleDistrictChange={handleDistrictChange}
        districtsList={districtsList}
      />
      <CmdAuditInvestigationCard selectedTask={selectedTask} />
    </Col>
  );
}

function CmdScreeningSideColumn(props) {
  return (
    <Col xs={24} lg={11}>
      <CmdCustomerProfileCard {...props} />
      <CmdScreeningFormCard {...props} />
    </Col>
  );
}

function CmdSelectedTaskWorkspace(props) {
  const { setSelectedTask, activeTab } = props;
                                  return (
    <div>
      <div style={{ marginBottom: '20px' }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => setSelectedTask(null)} aria-label="Back to task queue" style={{ borderRadius: '6px' }} />
      </div>
      <Row gutter={[24, 24]}>
        <CmdComplaintInfoColumn {...props} />
        {(activeTab === 'my_tasks' || activeTab === 'all_tasks' || activeTab === 'fcr_tasks') && (
          <CmdScreeningSideColumn {...props} />
        )}
      </Row>
    </div>
  );
}

function CmdDashboardView(props) {
  const {
  user,
  tasks,
  selectedTask,
  setSelectedTask,
  loading,
  error,
  slaMetrics,
  activeTab,
  setActiveTab,
  message,
  setMessage,
  clearingTasks,
  searchQuery,
  setSearchQuery,
  isAssignModalOpen,
  setIsAssignModalOpen,
  setTaskToAssign,
  taskToAssign,
  activeOfficers,
  selectedOfficerUsername,
  setSelectedOfficerUsername,
  isAssigning,
  unresolvedFollowups,
  selectedFollowup,
  setSelectedFollowup,
  isFollowupModalOpen,
  setIsFollowupModalOpen,
  handleStartFollowup,
  handleCloseFollowup,
  handleOpenAssignModal,
  confirmTaskAssignment,
  handleTaskSelect,
  clearAllTasks
  } = props;
  if (loading) {
    return (
      <DashboardLayout userRole="cmd">
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '400px' }}>
          <Card loading style={{ width: '100%', maxWidth: '600px' }} />
        </div>
      </DashboardLayout>
    );
  }

  if (error) {
    return (
      <DashboardLayout userRole="cmd">
        <Alert
          message="Error"
          description={error}
          type="error"
          showIcon
          style={{ margin: '20px' }}
        />
      </DashboardLayout>
    );
  }


  return (
    <DashboardLayout userRole="cmd">
      <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '32px', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <Title level={2} style={{ margin: 0, color: BRAND_COLORS.primary }}>
              Hello, {user?.fullName || user?.username || ''}
            </Title>
            <Text type="secondary" style={{ fontSize: '15px' }}>
              Screen &amp; categorize complaints
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


        <CmdMessageBanner message={message} onClose={() => setMessage('')} />

        {!selectedTask && (
          <CmdCategoryAnalytics
            tasks={tasks}
            slaMetrics={slaMetrics}
            unresolvedFollowups={unresolvedFollowups}
            loading={loading}
          />
        )}



        {!selectedTask ? (
          <div>
            <CmdQueueToolbar
              user={user}
              tasks={tasks}
              unresolvedFollowups={unresolvedFollowups}
              searchQuery={searchQuery}
              setSearchQuery={setSearchQuery}
              activeTab={activeTab}
              setActiveTab={setActiveTab}
              setSelectedTask={setSelectedTask}
            />
            {activeTab === 'unresolved_followups' ? (
              <UnresolvedFollowupsPanel
                unresolvedFollowups={unresolvedFollowups}
                searchQuery={searchQuery}
                setSelectedFollowup={setSelectedFollowup}
                setIsFollowupModalOpen={setIsFollowupModalOpen}
                handleStartFollowup={handleStartFollowup}
                handleCloseFollowup={handleCloseFollowup}
              />
            ) : (
              <CmdTaskQueue
                user={user}
                tasks={tasks}
                unresolvedFollowups={unresolvedFollowups}
                activeTab={activeTab}
                searchQuery={searchQuery}
                handleTaskSelect={handleTaskSelect}
                handleOpenAssignModal={handleOpenAssignModal}
              />
            )}
          </div>
        ) : (
          <CmdSelectedTaskWorkspace {...props} />
        )}
      </div>

      {/* Task Assignment Modal for Customer Care Senior Manager */}
      <Modal
        title={
          <span style={{ fontWeight: 700, color: BRAND_COLORS.primary, fontSize: '16px' }}>
            <UserOutlined style={{ marginRight: '8px' }} />
            Assign / Reassign Complaint
          </span>
        }
        open={isAssignModalOpen}
        onCancel={() => { setIsAssignModalOpen(false); setTaskToAssign(null); }}
        onOk={confirmTaskAssignment}
        confirmLoading={isAssigning}
        okText="Confirm Assignment"
        cancelText="Cancel"
        destroyOnClose
        style={{ borderRadius: '12px' }}
      >
        {taskToAssign && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', padding: '8px 0' }}>
            <div style={{ background: '#f8fafc', padding: '12px 16px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
              <Text type="secondary" style={{ fontSize: '12px', display: 'block' }}>Unique ID No</Text>
              <Text strong style={{ fontSize: '14px', color: BRAND_COLORS.primary }}>
                {formatUniqueId(taskToAssign)}
              </Text>
              {formatIntakeId(taskToAssign) && formatIntakeId(taskToAssign) !== formatUniqueId(taskToAssign) && (
                <Text type="secondary" style={{ fontSize: '12px', display: 'block' }}>
                  Intake {formatIntakeId(taskToAssign)}
                </Text>
              )}
              <div style={{ marginTop: '8px', display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
                <div>
                  <Text type="secondary" style={{ fontSize: '11px', display: 'block' }}>Customer</Text>
                  <Text style={{ fontSize: '12px', fontWeight: 600 }}>{taskToAssign.customerName || 'N/A'}</Text>
                </div>
                <div>
                  <Text type="secondary" style={{ fontSize: '11px', display: 'block' }}>Overall Status</Text>
                  {renderComplaintStatusTag(taskToAssign)}
                </div>
                <div>
                  <Text type="secondary" style={{ fontSize: '11px', display: 'block' }}>Current Officer</Text>
                  <Tag color={(taskToAssign.assignee && String(taskToAssign.assignee).toLowerCase() !== 'unassigned' && String(taskToAssign.assignee).toLowerCase() !== 'null' && String(taskToAssign.assignee).toLowerCase() !== 'initiator') ? 'green' : 'orange'} style={{ margin: 0, fontSize: '11px' }}>
                    {(taskToAssign.assignee && String(taskToAssign.assignee).toLowerCase() !== 'unassigned' && String(taskToAssign.assignee).toLowerCase() !== 'null' && String(taskToAssign.assignee).toLowerCase() !== 'initiator') ? taskToAssign.assignee : 'Unassigned'}
                  </Tag>
                </div>
              </div>
            </div>

            <div>
              <Text strong style={{ fontSize: '13px', display: 'block', marginBottom: '6px', color: '#1e293b' }}>
                Select Customer Care Officer or Team Leader <span style={{ color: '#ef4444' }}>*</span>
              </Text>
              <Select
                showSearch
                placeholder="Search and select officer or team leader..."
                optionFilterProp="children"
                value={selectedOfficerUsername || undefined}
                onChange={setSelectedOfficerUsername}
                style={{ width: '100%', borderRadius: '6px' }}
                size="large"
              >
                {activeOfficers.map(off => {
                  const assigneeRole = off.role === 'ROLE_CUSTOMER_CARE_TEAM_LEADER' ? 'Team Leader' : 'Officer';
                  return (
                  <Option key={off.username} value={off.username}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span style={{ fontWeight: 600, color: '#0f172a' }}>{off.fullName || off.username}</span>
                      <span style={{ fontSize: '11px', color: '#64748b', marginLeft: '8px' }}>
                        {assigneeRole} · @{off.username} {off.branch ? `(${off.branch})` : ''}
                      </span>
                    </div>
                  </Option>
                  );
                })}
              </Select>
            </div>
          </div>
        )}
      </Modal>

      {/* Unresolved Follow-Up Detail Modal */}
      <Modal
        title={<Title level={4} style={{ margin: 0, color: BRAND_COLORS.primary }}>Unresolved Complaint Follow-Up Details</Title>}
        open={isFollowupModalOpen}
        onCancel={() => {
          setIsFollowupModalOpen(false);
          setSelectedFollowup(null);
        }}
        footer={[
          <Button key="close" onClick={() => setIsFollowupModalOpen(false)}>
            Close
          </Button>,
          selectedFollowup?.followupStatus !== 'IN_PROGRESS' && selectedFollowup?.followupStatus !== 'CLOSED' && (
            <Button
              key="start"
              type="primary"
              style={{ background: '#2563eb', borderColor: '#2563eb' }}
              onClick={async () => {
                await handleStartFollowup(selectedFollowup.id);
                setIsFollowupModalOpen(false);
              }}
            >
              Start Follow-Up
            </Button>
          ),
          selectedFollowup?.followupStatus !== 'CLOSED' && (
            <Button
              key="complete"
              type="primary"
              style={{ background: '#10b981', borderColor: '#10b981' }}
              onClick={async () => {
                await handleCloseFollowup(selectedFollowup.id);
                setIsFollowupModalOpen(false);
              }}
            >
              Close Follow-Up
            </Button>
          )
        ].filter(Boolean)}
        width="100%"
        style={{ maxWidth: 600 }}
      >
        {selectedFollowup && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <Card size="small" style={{ background: '#f8fafc', borderRadius: '8px' }}>
              <Row gutter={[16, 16]}>
                <Col xs={24} sm={12}>
                  <Text type="secondary" style={{ fontSize: '12px', display: 'block', marginBottom: '4px' }}>Unique ID No</Text>
                  <span style={{
                    fontFamily: 'monospace',
                    fontSize: '12px',
                    color: '#111827',
                    fontWeight: 600,
                    background: '#f3f4f6',
                    padding: '3px 8px',
                    borderRadius: '4px',
                    whiteSpace: 'nowrap',
                    display: 'inline-block'
                  }}>
                    {selectedFollowup.ticketNumber || selectedFollowup.ticketId || '-'}
                  </span>
                </Col>
                <Col xs={24} sm={12}>
                  <Text type="secondary" style={{ fontSize: '12px', display: 'block', marginBottom: '4px' }}>Complaint Business Status</Text>
                  <Tag color="default" style={{ fontWeight: 600 }}>Closed</Tag>
                </Col>
                <Col xs={24} sm={12}>
                  <Text type="secondary" style={{ fontSize: '12px', display: 'block', marginBottom: '4px' }}>Survey Resolution Answer</Text>
                  <Tag color="error" style={{ fontWeight: 600 }}>No (Not Resolved)</Tag>
                </Col>
                <Col xs={24} sm={12}>
                  <Text type="secondary" style={{ fontSize: '12px', display: 'block', marginBottom: '4px' }}>Follow-Up Status</Text>
                  <Tag color={followupStatusTagColor(selectedFollowup.followupStatus)}>
                    {selectedFollowup.followupStatus || 'PENDING'}
                  </Tag>
                </Col>
              </Row>
            </Card>

            <Card size="small" title="Customer Survey Response" style={{ borderRadius: '8px' }}>
              <Text type="secondary" style={{ fontSize: '12px', display: 'block', marginBottom: '4px' }}>Submission Time</Text>
              <Text>
                {selectedFollowup.submittedAt
                  ? new Date(selectedFollowup.submittedAt).toLocaleString('en-GB', {
                      day: '2-digit',
                      month: '2-digit',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                      second: '2-digit',
                      hour12: false
                    }).replace(',', '')
                  : '-'}
              </Text>
            </Card>
          </div>
        )}
      </Modal>
    </DashboardLayout>
  );
}

function CMDDashboard() {
  const vm = useCmdDashboardState();
  const handlers = bindCmdHandlers(vm.stateRef);
  return <CmdDashboardView {...vm} {...handlers} />;
}

export default CMDDashboard;
