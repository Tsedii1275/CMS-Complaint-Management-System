import { isVisibleOnWorkUnitInbox } from './workUnitInbox';

function wuTask(variables) {
  return { definitionKey: 'FormTask_57', variables };
}

const boleBm = {
  username: 'bole.bm',
  role: 'ROLE_BRANCH_MANAGER',
  branch: 'Bole Branch',
};

test('branch manager sees complaint CCO assigned to their AD username and branch', () => {
  const task = wuTask({
    assignmentType: 'BRANCH',
    assignedOfficerUsername: 'Bole.BM',
    complaintBranch: 'Bole Branch',
  });
  expect(isVisibleOnWorkUnitInbox(task, boleBm)).toBe(true);
});

test('branch manager does not see another branch manager assignment', () => {
  const task = wuTask({
    assignmentType: 'BRANCH',
    assignedOfficerUsername: 'adama.bm',
    complaintBranch: 'Adama Branch',
  });
  expect(isVisibleOnWorkUnitInbox(task, boleBm)).toBe(false);
});

test('branch manager does not see a ticket assigned to them at a different branch', () => {
  const task = wuTask({
    assignmentType: 'BRANCH',
    assignedOfficerUsername: 'bole.bm',
    complaintBranch: 'Adama Branch',
  });
  expect(isVisibleOnWorkUnitInbox(task, boleBm)).toBe(false);
});

test('AD branch manager with no CMS branch still sees own assignee tickets', () => {
  const task = wuTask({
    assignmentType: 'BRANCH',
    assignedOfficerUsername: 'bole.bm',
    complaintBranch: 'Bole Branch',
  });
  expect(isVisibleOnWorkUnitInbox(task, {
    username: 'bole.bm',
    role: 'ROLE_BRANCH_MANAGER',
  })).toBe(true);
});

test('district work-unit user only sees their assignee tickets', () => {
  const task = wuTask({
    assignmentType: 'DISTRICT',
    assignedOfficerUsername: 'east.dd',
    complaintDistrict: 'East District',
  });
  expect(isVisibleOnWorkUnitInbox(task, {
    username: 'east.dd',
    role: 'ROLE_DEPARTMENT_WORKUNIT',
  })).toBe(true);
  expect(isVisibleOnWorkUnitInbox(task, {
    username: 'west.dd',
    role: 'ROLE_DEPARTMENT_WORKUNIT',
  })).toBe(false);
});
