function normalize(value) {
  return String(value || '').trim().toLowerCase();
}

function stageOf(alert) {
  return String(alert?.currentStage || alert?.currentStageLabel || '').toUpperCase();
}

function assigneeOf(alert) {
  return normalize(alert?.assignedOfficerUsername || alert?.assigneeUsername);
}

function branchOf(alert) {
  return normalize(alert?.branch || alert?.complaintBranch);
}

function departmentOf(alert) {
  return normalize(alert?.department);
}

function matchesAssignee(alert, user) {
  const username = normalize(user?.username);
  return Boolean(username && assigneeOf(alert) === username);
}

function matchesBranch(alert, user) {
  const userBranch = normalize(user?.branch);
  const ticketBranch = branchOf(alert);
  if (!userBranch || !ticketBranch) {
    return false;
  }
  return userBranch === ticketBranch || ticketBranch.includes(userBranch.split(' ')[0]);
}

function matchesDepartment(alert, user) {
  const userDept = normalize(user?.department);
  const ticketDept = departmentOf(alert);
  if (!userDept || !ticketDept) {
    return false;
  }
  return userDept === ticketDept;
}

/**
 * Stage SLA bell: only the caller's workflow lane and unit.
 */
export function isSlaAlertForUser(alert, user) {
  const role = String(user?.role || '').toUpperCase();
  if (!alert || !role || role.includes('ADMIN')) {
    return false;
  }
  const stage = stageOf(alert);

  if (role.includes('AUDIT')) {
    return stage.includes('INVESTIGATION');
  }
  if (role.includes('COMMITTEE')) {
    return stage.includes('COMMITTEE');
  }
  if (role.includes('CHIEF_EXPERIENCE')) {
    return stage.includes('CHIEF_EXPERIENCE') || stage.includes('CXO_REVIEW') || stage.includes('CXO');
  }
  if (role === 'ROLE_SERVICE_QUALITY_DIRECTOR' || role.includes('SERVICE_QUALITY_DIRECTOR')) {
    return stage.includes('SERVICE_QUALITY');
  }
  if (role === 'ROLE_CUSTOMER_CARE_OFFICER'
      || role === 'ROLE_CUSTOMER_CARE_TEAM_LEADER'
      || role === 'ROLE_CUSTOMER_CARE_SENIOR_MANAGER'
      || role.includes('CUSTOMER_CARE_OFFICER')) {
    return stage.includes('CMD_SCREENING') || stage === 'SCREENING'
      || stage.includes('CUSTOMER_NOTIFICATION');
  }
  if (role.includes('DEPARTMENT_WORKUNIT')) {
    if (!stage.includes('WORK_UNIT')) {
      return false;
    }
    return matchesAssignee(alert, user) || matchesDepartment(alert, user) || matchesBranch(alert, user);
  }
  if (role.startsWith('ROLE_BRANCH_MANAGER')) {
    const workUnit = stage.includes('WORK_UNIT');
    const branchIntake = stage.includes('BRANCH') && !stage.includes('WORK_UNIT');
    if (!workUnit && !branchIntake) {
      return false;
    }
    return matchesAssignee(alert, user) || matchesBranch(alert, user);
  }
  if (role.startsWith('ROLE_CUSTOMER_SERVICE') || role === 'ROLE_CUSTOMER_EXPERIENCE_PARTNERSHIP') {
    return stage.includes('BRANCH') && (matchesBranch(alert, user) || matchesAssignee(alert, user));
  }
  if (role.startsWith('ROLE_CONTACT_CENTER') || role.startsWith('ROLE_DIGITAL_MARKETING')) {
    return stage.includes('CONTACT_CENTER') || stage.includes('BRANCH')
      || stage.includes('CUSTOMER_NOTIFICATION');
  }
  return false;
}
