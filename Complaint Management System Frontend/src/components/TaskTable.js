import React, { useState, useEffect } from 'react';
import { Table, Tag, Button, Tooltip, Space } from 'antd';
import { ClockCircleOutlined, UserOutlined, EyeOutlined } from '@ant-design/icons';
import { BRAND_COLORS } from '../constants/theme';
import { formatDate, PRIORITY_CONFIG } from './TaskCard';
import Pagination from './Pagination';
import ExportDropdown from './ExportDropdown';
import { renderComplaintStatusTag } from '../utils/statusUtils';

/**
 * Resolves and formats the actual Complaint ID from backend data (no hardcoding or pseudo-hashing).
 */
export const formatUniqueId = (recordOrId) => {
  if (!recordOrId) return '—';

  if (typeof recordOrId === 'string') {
    return recordOrId;
  }

  if (typeof recordOrId === 'object') {
    // Priority 1: Check all candidate fields for an official DBC- ticket ID
    const candidates = [
      recordOrId.dbcTicketId,
      recordOrId.variables?.dbcTicketId,
      recordOrId.variables?.dbcTicketNo,
      recordOrId.complaintId,
      recordOrId.variables?.complaintId,
      recordOrId.ticketNumber,
      recordOrId.variables?.ticketNumber,
      recordOrId.uniqueIdNo,
      recordOrId.variables?.uniqueIdNo,
      recordOrId.generalTicketId,
      recordOrId.variables?.generalTicketId
    ];

    for (const c of candidates) {
      if (c && typeof c === 'string' && c.trim().startsWith('DBC-')) {
        return c.trim();
      }
    }

    // Priority 2: Fallback to general ticket fields
    const fallback = recordOrId.dbcTicketId ||
      recordOrId.variables?.dbcTicketId ||
      recordOrId.uniqueIdNo ||
      recordOrId.complaintId ||
      recordOrId.variables?.complaintId ||
      recordOrId.generalTicketId ||
      recordOrId.variables?.generalTicketId ||
      recordOrId.ticketNumber ||
      recordOrId.id;

    if (fallback && typeof fallback === 'string') {
      return fallback.trim();
    }

    return '—';
  }

  return '—';
};

export const compareTicketNumbersAsc = (left, right) => {
  const parse = (value) => {
    const raw = String(value || '').trim();
    if (!raw || raw === '-' || raw === '—') {
      return { prefixRank: 99, year: Number.MAX_SAFE_INTEGER, sequence: Number.MAX_SAFE_INTEGER, raw };
    }
    const match = raw.match(/^(DBC|FCR|CM)-(\d+)(?:\/(\d{4})-(\d{2}))?$/i);
    if (!match) {
      return { prefixRank: 50, year: Number.MAX_SAFE_INTEGER, sequence: Number.MAX_SAFE_INTEGER, raw };
    }
    const prefix = match[1].toUpperCase();
    let prefixRank = 50;
    if (prefix === 'DBC') prefixRank = 0;
    else if (prefix === 'FCR') prefixRank = 1;
    else if (prefix === 'CM') prefixRank = 2;
    return {
      prefixRank,
      year: match[3] ? Number(match[3]) : 0,
      sequence: Number(match[2]),
      raw
    };
  };

  const a = parse(left);
  const b = parse(right);
  if (a.prefixRank !== b.prefixRank) return a.prefixRank - b.prefixRank;
  if (a.year !== b.year) return a.year - b.year;
  if (a.sequence !== b.sequence) return a.sequence - b.sequence;
  return a.raw.localeCompare(b.raw, undefined, { sensitivity: 'base' });
};

/**
 * Original CM intake ticket, kept as a secondary lookup after a DBC number is issued.
 */
export const formatIntakeId = (recordOrId) => {
  if (!recordOrId || typeof recordOrId !== 'object') return '';

  const candidates = [
    recordOrId.generalTicketId,
    recordOrId.variables?.generalTicketId,
    recordOrId.intakeTicketId,
    recordOrId.variables?.intakeTicketId
  ];

  for (const c of candidates) {
    if (c && typeof c === 'string' && c.trim().startsWith('CM-')) {
      return c.trim();
    }
  }

  return '';
};

const recordVariables = (record) => record?.variables || {};

const assignedUserOf = (record) =>
  record.assignee || record.claimedBy || record.assignedUser || recordVariables(record).assignedUser;

const isAssignedOfficer = (assignedUser) =>
  Boolean(
    assignedUser &&
    String(assignedUser).toLowerCase() !== 'null' &&
    String(assignedUser).toLowerCase() !== 'unassigned' &&
    String(assignedUser).toLowerCase() !== 'initiator'
  );

const complaintPriorityConfig = (record) => {
  const rawPriority = (record.priority || recordVariables(record).complaintPriority || 'general')
    .toString()
    .toLowerCase()
    .replaceAll(' ', '_');
  return PRIORITY_CONFIG[rawPriority] || PRIORITY_CONFIG.general;
};

export const matchesTicketSearch = (record, query) => {
  if (!query) return true;
  const q = String(query).toLowerCase().trim();
  if (!q) return true;
  if (!record) return false;

  const uniqueId = formatUniqueId(record);
  const intakeId = formatIntakeId(record);
  const extra = [
    record.complaintId,
    record.dbcTicketId,
    record.generalTicketId,
    record.ticketNumber,
    record.uniqueIdNo,
    record.variables?.complaintId,
    record.variables?.dbcTicketId,
    record.variables?.generalTicketId,
    record.variables?.ticketNumber,
    record.variables?.uniqueIdNo
  ];

  if (uniqueId?.toLowerCase().includes(q)) return true;
  if (intakeId?.toLowerCase().includes(q)) return true;
  return extra.some(v => typeof v === 'string' && v.toLowerCase().includes(q));
};

export const UniqueIdDisplay = ({ record, showIntake = true }) => {
  const displayId = formatUniqueId(record);
  const intakeId = formatIntakeId(record);
  const showSecondary = showIntake && intakeId && intakeId !== displayId;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', minWidth: 0 }}>
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
        {displayId}
      </span>
      {showSecondary && (
        <span style={{ fontSize: '10px', color: '#64748b', whiteSpace: 'nowrap' }}>
          Intake {intakeId}
        </span>
      )}
    </div>
  );
};

/**
 * Neat, modern, high-density Table View for staff task queues.
 * Displays standard task card information in a clean, space-efficient single-row table format.
 */
function TaskTable({ tasks = [], onSelectTask, onAssignTask, showPriority = false, showAssignedOfficer = true, showAssignedDate = true, showOverallStatus = true }) {
  const [pageSize, setPageSize] = useState(10);
  const [currentPage, setCurrentPage] = useState(1);

  useEffect(() => {
    setCurrentPage(1);
  }, [tasks.length]);

  const columns = [
    {
      title: 'Unique ID No',
      dataIndex: 'complaintId',
      key: 'complaintId',
      width: 250,
      render: (id, r) => {
        const vars = recordVariables(r);
        const isCommitteeRejected = vars.committeeDecision === 'rejected' ||
          vars.committeeStatus === 'REJECTED' ||
          vars.currentStage === 'COMMITTEE_REJECTED' ||
          vars.isCommitteeRejected === true ||
          r.committeeDecision === 'rejected' ||
          r.isCommitteeRejected === true;

        const isCommitteeAccepted = vars.committeeDecision === 'approved' ||
          vars.committeeDecision === 'accepted' ||
          vars.committeeDecision === 'accept' ||
          vars.committeeStatus === 'ACCEPTED' ||
          vars.currentStage === 'COMMITTEE_ACCEPTED' ||
          vars.isCommitteeAccepted === true ||
          r.committeeDecision === 'approved' ||
          r.committeeDecision === 'accepted' ||
          r.isCommitteeAccepted === true;

        const isResolutionGiven = r.definitionKey === 'FormTask_43' && (
          vars.status === 'RESOLUTION_GIVEN' ||
          vars.currentStage === 'CCO_RESOLUTION_REVIEW' ||
          vars.workUnitResolutionGiven === true ||
          vars.requiresCcoReview === true ||
          r.status === 'RESOLUTION_GIVEN'
        );

        return (
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', flexWrap: 'nowrap' }}>
            <UniqueIdDisplay record={r} />
            {isCommitteeRejected && (
              <Tag color="magenta" style={{ fontWeight: 700, fontSize: '11px', borderRadius: '4px', margin: 0, whiteSpace: 'nowrap' }}>
                Committee Rejected
              </Tag>
            )}
            {isCommitteeAccepted && (
              <Tag color="green" style={{ fontWeight: 700, fontSize: '11px', borderRadius: '4px', margin: 0, whiteSpace: 'nowrap' }}>
                Committee Approved
              </Tag>
            )}
            {isResolutionGiven && (
              <Tag color="cyan" style={{ fontWeight: 700, fontSize: '11px', borderRadius: '4px', margin: 0, whiteSpace: 'nowrap' }}>
                Resolution Given
              </Tag>
            )}
          </div>
        );
      }
    },
    ...(showOverallStatus ? [{
      title: 'Overall Status',
      key: 'overallStatus',
      width: 120,
      render: (_, r) => renderComplaintStatusTag(r)
    }] : []),
    {
      title: 'Customer Name',
      dataIndex: 'customerName',
      key: 'customerName',
      width: 220,
      render: (name) => (
        <span style={{ fontSize: '12px', color: '#374151', fontWeight: 500, whiteSpace: 'nowrap' }}>
          <UserOutlined style={{ color: '#9ca3af', marginRight: '6px' }} />
          {name || '—'}
        </span>
      )
    },
    ...(showPriority ? [{
      title: 'Complaint Classification',
      key: 'priority',
      width: 120,
      render: (_, r) => {
        const pConfig = complaintPriorityConfig(r);
        return (
          <span style={{ fontSize: '11px', background: pConfig.bg, color: pConfig.text, border: `1px solid ${pConfig.border}`, padding: '2px 6px', borderRadius: '4px', fontWeight: 600, whiteSpace: 'nowrap' }}>
            {pConfig.label}
          </span>
        );
      }
    }] : []),
    ...(showAssignedOfficer ? [{
      title: 'Assigned Officer',
      key: 'assignedOfficer',
      width: 180,
      render: (_, r) => {
        const assignedUser = assignedUserOf(r);
        const isAssigned = isAssignedOfficer(assignedUser);
        return isAssigned ? (
          <Tag color="blue" style={{ fontSize: '11px', fontWeight: 600, borderRadius: '4px', whiteSpace: 'nowrap' }}>
            <UserOutlined style={{ marginRight: '4px' }} />
            {assignedUser}
          </Tag>
        ) : (
          <Tag color="orange" style={{ fontSize: '11px', fontWeight: 600, borderRadius: '4px', whiteSpace: 'nowrap' }}>
            Unassigned
          </Tag>
        );
      }
    }] : []),
    ...(showAssignedDate ? [{
      title: 'Assigned Date',
      key: 'assignedDate',
      width: 150,
      render: (_, r) => {
        const assignedUser = assignedUserOf(r);
        const isAssigned = isAssignedOfficer(assignedUser);
        const vars = recordVariables(r);

        const assignDateVal = r.assignedDate || r.assignedAt || r.claimedAt || vars.assignedDate || vars.assignedAt || vars.claimedAt;

        if (!isAssigned || !assignDateVal) {
          return <span style={{ fontSize: '13px', color: '#9ca3af', fontWeight: 600 }}>—</span>;
        }

        return (
          <span style={{ fontSize: '11px', color: '#4b5563', whiteSpace: 'nowrap' }}>
            <ClockCircleOutlined style={{ marginRight: '4px', color: '#6b7280' }} />
            {formatDate(assignDateVal)}
          </span>
        );
      }
    }] : []),
    {
      title: 'Action',
      key: 'action',
      width: onAssignTask ? 140 : 80,
      align: 'center',
      render: (_, r) => {
        const isAssigned = isAssignedOfficer(assignedUserOf(r));
        return (
          <Space size="small">
            <Tooltip title="View Task Details">
              <Button
                type="text"
                size="small"
                icon={<EyeOutlined style={{ fontSize: '15px', color: BRAND_COLORS.primary }} />}
                onClick={(e) => { e.stopPropagation(); onSelectTask(r); }}
                style={{ padding: '2px 4px', height: '24px', display: 'flex', alignItems: 'center' }}
              />
            </Tooltip>
            {onAssignTask && (
              <Button
                type="primary"
                size="small"
                onClick={(e) => { e.stopPropagation(); onAssignTask(r); }}
                style={{
                  fontSize: '11px',
                  height: '24px',
                  padding: '0 8px',
                  borderRadius: '4px',
                  fontWeight: 600,
                  backgroundColor: isAssigned ? '#475569' : BRAND_COLORS.primary,
                  borderColor: isAssigned ? '#475569' : BRAND_COLORS.primary
                }}
              >
                {isAssigned ? 'Reassign' : 'Assign'}
              </Button>
            )}
          </Space>
        );
      }
    }
  ];

  const paginatedTasks = tasks.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const exportColumns = [
    { header: 'Unique ID No', accessor: (r) => formatUniqueId(r) },
    ...(showOverallStatus ? [{
      header: 'Overall Status',
      type: 'status',
      accessor: (r) => r
    }] : []),
    { header: 'Customer Name', key: 'customerName' },
    ...(showPriority ? [{
      header: 'Complaint Classification',
      accessor: (r) => complaintPriorityConfig(r).label
    }] : []),
    ...(showAssignedOfficer ? [{
      header: 'Assigned Officer',
      accessor: (r) => {
        const assignedUser = assignedUserOf(r);
        return isAssignedOfficer(assignedUser) ? assignedUser : 'Unassigned';
      }
    }] : []),
    ...(showAssignedDate ? [{
      header: 'Assigned Date',
      type: 'date',
      accessor: (r) => {
        const assignedUser = assignedUserOf(r);
        if (!isAssignedOfficer(assignedUser)) return null;
        const vars = recordVariables(r);
        return r.assignedDate || r.assignedAt || r.claimedAt || vars.assignedDate || vars.assignedAt || vars.claimedAt;
      }
    }] : [])
  ];

  return (
    <div className="cms-table-shell" style={{ background: '#fff', padding: '12px 20px', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12 }}>
        <ExportDropdown
          size="small"
          getDataset={() => ({
            filename: `Task_Queue_${new Date().toISOString().slice(0, 10)}`,
            documentTitle: 'Task Queue',
            tables: [{
              title: 'Tasks',
              sheetName: 'Tasks',
              columns: exportColumns,
              rows: tasks
            }]
          })}
        />
      </div>
      <Table
        dataSource={paginatedTasks}
        columns={columns}
        rowKey="id"
        size="middle"
        pagination={false}
        bordered={false}
        scroll={{ x: 'max-content' }}
      />
      <Pagination
        currentPage={currentPage}
        pageSize={pageSize}
        totalRecords={tasks.length}
        onPageChange={(page) => setCurrentPage(page)}
        onPageSizeChange={(size) => { setPageSize(size); setCurrentPage(1); }}
        itemUnit="tasks"
      />
    </div>
  );
}

export default TaskTable;
