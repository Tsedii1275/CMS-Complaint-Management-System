import React from 'react';
import { Tag } from 'antd';

export const COMPLAINT_STATUS = {
    RECORDED: 'RECORDED',
    ON_TRACK: 'ON_TRACK',
    ESCALATED: 'ESCALATED',
    RESOLVED: 'RESOLVED',
    CLOSED: 'CLOSED',
    DECLINED: 'DECLINED'
};

export const STATUS_CONFIG = {
    RECORDED: {
        label: 'RECORDED',
        color: 'purple',
        description: 'Newly registered complaint.'
    },
    ON_TRACK: {
        label: 'ON_TRACK',
        color: 'blue',
        description: 'Complaint assigned to a Work Unit and being handled.'
    },
    ESCALATED: {
        label: 'ESCALATED',
        color: 'orange',
        description: 'Complaint requiring investigation / CEX / Audit / Committee.'
    },
    RESOLVED: {
        label: 'RESOLVED',
        color: 'green',
        description: 'Customer Care Officer closed the case after reviewing the resolution.'
    },
    CLOSED: {
        label: 'CLOSED',
        color: 'default',
        description: 'Customer submitted the post-resolution feedback survey.'
    },
    DECLINED: {
        label: 'DECLINED',
        color: 'red',
        description: 'Customer Care Officer declined the complaint.'
    }
};

const isOfficial = (value) => Object.values(COMPLAINT_STATUS).includes(String(value || '').toUpperCase());

const blob = (...parts) => parts.filter(Boolean).join(' ').toUpperCase();

/**
 * Maps a task/metrics record onto the six official overall statuses.
 * Prefers backend overallStatus; never treats SLA/stage/decision as overall status.
 */
export const resolveOverallComplaintStatus = (record = {}) => {
    if (isOfficial(record.overallStatus)) {
        return String(record.overallStatus).toUpperCase();
    }

    const vars = record.variables || {};
    const classification = vars.classification || record.classification || '';
    const decision = vars.decision || record.decision || '';
    const status = vars.status || record.status || '';
    const currentStage = vars.currentStage || vars.stage || record.currentStage || '';
    const committeeStatus = vars.committeeStatus || '';
    const feedbackSubmitted = Boolean(
        record.customerFeedbackSubmitted
        || vars.customerFeedbackSubmitted
        || vars.customerFeedbackSubmittedAt
        || record.feedbackSubmittedAt
    );

    if (blob(classification, decision, status).includes('DECLINE')) {
        return COMPLAINT_STATUS.DECLINED;
    }
    if (feedbackSubmitted) {
        return COMPLAINT_STATUS.CLOSED;
    }
    if (
        vars.resolutionAccepted === true
        || String(status).toUpperCase() === 'CLOSED'
        || String(currentStage).toUpperCase() === 'CLOSED'
        || String(vars.stage || '').toUpperCase() === 'CLOSED'
        || String(vars.fcrStatus || record.fcrStatus || '').toUpperCase() === 'VERIFIED'
        || String(decision).toUpperCase() === 'FCR_APPROVED'
        || (String(vars.fcrAction || '').toLowerCase() === 'approve'
            && (String(status).toUpperCase() === 'RESOLVED' || String(currentStage).toUpperCase() === 'RESOLVED'))
    ) {
        return COMPLAINT_STATUS.RESOLVED;
    }

    const stageBlob = blob(currentStage, vars.stage, committeeStatus, status);
    if (
        vars.requiresInvestigation === true
        || record.requiresInvestigation === true
        || stageBlob.includes('INVESTIGAT')
        || stageBlob.includes('AUDIT')
        || stageBlob.includes('COMMITTEE')
        || stageBlob.includes('CHIEF_EXPERIENCE')
        || stageBlob.includes('CHIEF_OPERATION')
        || stageBlob.includes('ESCALAT')
    ) {
        return COMPLAINT_STATUS.ESCALATED;
    }

    if (
        stageBlob.includes('WORK_UNIT')
        || stageBlob.includes('RESOLUTION_GIVEN')
        || vars.assignedDepartment
        || vars.department
        || record.department
    ) {
        return COMPLAINT_STATUS.ON_TRACK;
    }

    if (stageBlob.includes('CMD_SCREENING') || stageBlob.includes('INTAKE') || stageBlob.includes('RECORD')) {
        return COMPLAINT_STATUS.RECORDED;
    }

    return normalizeComplaintStatus(status || currentStage, COMPLAINT_STATUS.RECORDED);
};

export const normalizeComplaintStatus = (rawStatus, fallback = COMPLAINT_STATUS.RECORDED) => {
    if (!rawStatus) return fallback;
    const statusUpper = String(rawStatus).toUpperCase().trim().replace(/ /g, '_');

    if (statusUpper === 'ONTRACK') return COMPLAINT_STATUS.ON_TRACK;
    if (isOfficial(statusUpper)) return statusUpper;
    if (statusUpper === 'CASE_CLOSED_BY_CUSTOMER') return COMPLAINT_STATUS.CLOSED;
    if (['NEW', 'SUBMITTED', 'CREATED', 'REGISTERED', 'TICKET_GENERATED', 'COMPLAINT_CREATED', 'INTAKE', 'CMD_SCREENING'].includes(statusUpper)) {
        return COMPLAINT_STATUS.RECORDED;
    }
    if (['IN_PROGRESS', 'OPEN', 'PENDING', 'ASSIGNED', 'UNDER_REVIEW', 'ACTIVE', 'RESOLUTION_GIVEN'].includes(statusUpper)
        || statusUpper.includes('WORK_UNIT')) {
        return COMPLAINT_STATUS.ON_TRACK;
    }
    if (statusUpper.includes('COMMITTEE') || statusUpper.includes('INVESTIG') || statusUpper.includes('AUDIT')
        || statusUpper.includes('CHIEF_EXPERIENCE') || statusUpper.includes('CHIEF_OPERATION')
        || statusUpper === 'ESCALATED_FOR_INVESTIGATION') {
        return COMPLAINT_STATUS.ESCALATED;
    }
    if (['RESOLUTION_PENDING', 'COMPLETED', 'CLOSED_PENDING', 'CASE_CLOSED'].includes(statusUpper)) {
        return COMPLAINT_STATUS.RESOLVED;
    }
    if (statusUpper === 'REJECTED' || statusUpper.includes('DECLINE')) {
        return COMPLAINT_STATUS.DECLINED;
    }
    return fallback;
};

export const renderComplaintStatusTag = (rawStatusOrRecord, style = {}) => {
    const normalized = rawStatusOrRecord && typeof rawStatusOrRecord === 'object'
        ? resolveOverallComplaintStatus(rawStatusOrRecord)
        : normalizeComplaintStatus(rawStatusOrRecord);
    const cfg = STATUS_CONFIG[normalized] || STATUS_CONFIG.RECORDED;

    return (
        <Tag
            color={cfg.color}
            style={{
                fontWeight: 600,
                borderRadius: '4px',
                margin: 0,
                fontSize: '11px',
                letterSpacing: '0.3px',
                padding: '2px 8px',
                ...style
            }}
        >
            {cfg.label}
        </Tag>
    );
};
