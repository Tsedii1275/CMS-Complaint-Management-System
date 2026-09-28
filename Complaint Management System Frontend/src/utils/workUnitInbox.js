import { complaintBranch, complaintDistrict } from './locationKeys';

function normalizeKey(value) {
  return String(value || '').trim().replace(/\s+/g, ' ').toLowerCase();
}

function isWorkUnitDefinition(task) {
  return task?.definitionKey === 'FormTask_57'
    || task?.definitionKey === 'UserTask_WorkUnit'
    || task?.definitionKey === 'SecondaryResolutionReview'
    || Boolean(task?.name?.includes('Secondary Resolution Review'));
}

export function isWorkUnitStageTask(task) {
  const currentStage = task?.variables?.currentStage || task?.variables?.stage;
  if (!isWorkUnitDefinition(task) && ['COMMITTEE_ACCEPTED', 'COMMITTEE_REJECTED', 'COMMITTEE_REVIEW',
    'CHIEF_EXPERIENCE_REVIEW', 'CHIEF_OPERATION_AUDIT', 'CMD_SCREENING', 'COMPLETED', 'RESOLVED', 'CLOSED']
    .includes(currentStage)) {
    return false;
  }

  const isComplaintVar = task?.variables?.isComplaint;
  if (isComplaintVar === false || isComplaintVar === 'false') {
    return false;
  }

  const classification = (task?.variables?.classification || task?.variables?.complaintClassification || '')
    .toUpperCase();
  if (['OTHER', 'DECLINED', 'INQUIRY', 'REQUEST'].includes(classification)) {
    return false;
  }

  const targetTab = task?.variables?.targetTab;
  if (['Other', 'Declined'].includes(targetTab)) {
    return false;
  }

  const dbcTicketId = task?.variables?.dbcTicketId;
  const complaintId = task?.complaintId || task?.variables?.complaintId
    || task?.variables?.ticketId || task?.variables?.uniqueIdNo || '';
  if (String(complaintId).startsWith('CM-') && (!dbcTicketId || !String(dbcTicketId).startsWith('DBC-'))) {
    return false;
  }

  return task?.definitionKey !== 'FormTask_43'
    && task?.definitionKey !== 'FormTask_ChiefCommittee'
    && task?.definitionKey !== 'FormTask_CEX'
    && task?.definitionKey !== 'FormTask_48'
    && isWorkUnitDefinition(task);
}

export function assignedOfficerUsername(task) {
  const vars = task?.variables || {};
  return vars.assignedOfficerUsername || vars.assigneeUsername || '';
}

export function assignmentScope(task) {
  const stored = String(task?.variables?.assignmentType || '').toUpperCase();
  if (stored === 'DISTRICT_DEPARTMENT' || stored === 'DISTRICT') return 'DISTRICT';
  if (stored === 'HQ_DEPARTMENT') return 'HQ_DEPARTMENT';
  if (stored === 'BRANCH' || stored === 'DISTRICT_BRANCH') return 'BRANCH';
  return '';
}

function branchMatches(user, task) {
  const userBranch = normalizeKey(user?.branch);
  const taskBranch = normalizeKey(complaintBranch(task?.variables));
  if (!userBranch || !taskBranch) {
    return true;
  }
  return userBranch === taskBranch;
}

function unitFallbackMatches(user, task) {
  const role = String(user?.role || '').toUpperCase();
  const scope = assignmentScope(task);
  if (role.startsWith('ROLE_BRANCH_MANAGER')) {
    const userBranch = normalizeKey(user?.branch);
    const taskBranch = normalizeKey(complaintBranch(task?.variables));
    return Boolean(userBranch && taskBranch && userBranch === taskBranch
      && (scope === 'BRANCH' || !scope));
  }
  const userDept = normalizeKey(user?.department);
  const userDistrict = normalizeKey(user?.district);
  if (scope === 'HQ_DEPARTMENT' || (!scope && userDept)) {
    const dept = normalizeKey(task?.variables?.department);
    return Boolean(userDept && dept && userDept === dept);
  }
  if (scope === 'DISTRICT' || userDistrict) {
    const district = normalizeKey(complaintDistrict(task?.variables));
    return Boolean(userDistrict && district && userDistrict === district);
  }
  return false;
}

/**
 * Work Unit inbox: tickets assigned to this AD user.
 * Branch Managers also stay on BRANCH-scope tickets for their branch when branch is known.
 */
export function isVisibleOnWorkUnitInbox(task, user) {
  if (!isWorkUnitStageTask(task)) {
    return false;
  }
  const username = normalizeKey(user?.username);
  if (!username) {
    return false;
  }
  const assignee = normalizeKey(assignedOfficerUsername(task));
  if (assignee) {
    if (assignee !== username) {
      return false;
    }
    const role = String(user?.role || '').toUpperCase();
    if (role.startsWith('ROLE_BRANCH_MANAGER')) {
      const scope = assignmentScope(task);
      if (scope && scope !== 'BRANCH') {
        return false;
      }
      return branchMatches(user, task);
    }
    return true;
  }
  return unitFallbackMatches(user, task);
}
