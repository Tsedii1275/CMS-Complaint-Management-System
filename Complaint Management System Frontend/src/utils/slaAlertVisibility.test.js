import { isSlaAlertForUser } from './slaAlertVisibility';

test('branch manager does not see customer care screening or audit alerts', () => {
  const user = { username: 'bole.bm', role: 'ROLE_BRANCH_MANAGER', branch: 'Bole Branch' };
  expect(isSlaAlertForUser({
    currentStage: 'CMD_SCREENING',
    currentStageLabel: 'Customer Care Screening',
  }, user)).toBe(false);
  expect(isSlaAlertForUser({
    currentStage: 'INVESTIGATION',
    currentStageLabel: 'Audit / Investigation',
  }, user)).toBe(false);
});

test('customer care sees screening only', () => {
  const user = { username: 'cco', role: 'ROLE_CUSTOMER_CARE_OFFICER' };
  expect(isSlaAlertForUser({ currentStage: 'CMD_SCREENING' }, user)).toBe(true);
  expect(isSlaAlertForUser({ currentStage: 'INVESTIGATION' }, user)).toBe(false);
  expect(isSlaAlertForUser({ currentStage: 'WORK_UNIT_RESOLUTION' }, user)).toBe(false);
});

test('cxo sees executive review including CXO_REVIEW alias', () => {
  const user = { username: 'cxo', role: 'ROLE_CHIEF_EXPERIENCE_OFFICER' };
  expect(isSlaAlertForUser({ currentStage: 'CHIEF_EXPERIENCE_REVIEW' }, user)).toBe(true);
  expect(isSlaAlertForUser({ currentStage: 'CXO_REVIEW' }, user)).toBe(true);
  expect(isSlaAlertForUser({ currentStage: 'CMD_SCREENING' }, user)).toBe(false);
});

test('work unit sees only assigned or same-department work-unit alerts', () => {
  const user = { username: 'loans.lead', role: 'ROLE_DEPARTMENT_WORKUNIT', department: 'Loans' };
  expect(isSlaAlertForUser({
    currentStage: 'WORK_UNIT_RESOLUTION',
    assignedOfficerUsername: 'loans.lead',
  }, user)).toBe(true);
  expect(isSlaAlertForUser({
    currentStage: 'CMD_SCREENING',
    assignedOfficerUsername: 'loans.lead',
  }, user)).toBe(false);
  expect(isSlaAlertForUser({
    currentStage: 'WORK_UNIT_RESOLUTION',
    assignedOfficerUsername: 'other.lead',
    department: 'Trade',
  }, user)).toBe(false);
});
