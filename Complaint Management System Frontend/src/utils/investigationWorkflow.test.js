import {
  TASK_SM_REVIEW,
  TASK_SQ_REVIEW,
  TASK_SM_CLOSURE,
  TASK_CXO,
  CXO_RETURN_TO_SENIOR_MANAGER,
  CXO_SEND_TO_AUDIT,
  buildCxoDecisionPayload,
  buildReviewPayload,
  hasInvestigationTab,
  investigationTabLabel,
  investigationTasksForRole,
  isInvestigationReviewTask,
  reviewTrailOf
} from './investigationWorkflow';

const tasks = [
  { id: '1', definitionKey: TASK_SM_REVIEW },
  { id: '2', definitionKey: TASK_SQ_REVIEW },
  { id: '3', definitionKey: TASK_SM_CLOSURE },
  { id: '4', definitionKey: 'FormTask_43' },
  { id: '5', definitionKey: TASK_CXO }
];

describe('investigation workflow tabs', () => {
  test('senior manager sees review and closure tasks only', () => {
    const ids = investigationTasksForRole(tasks, 'ROLE_CUSTOMER_CARE_SENIOR_MANAGER').map((t) => t.id);
    expect(ids).toEqual(['1', '3']);
  });

  test('service quality sees only the service quality review', () => {
    const ids = investigationTasksForRole(tasks, 'ROLE_SERVICE_QUALITY_DIRECTOR').map((t) => t.id);
    expect(ids).toEqual(['2']);
  });

  test('other roles get no investigation tab', () => {
    expect(investigationTasksForRole(tasks, 'ROLE_CUSTOMER_CARE_OFFICER')).toEqual([]);
    expect(investigationTasksForRole(tasks, 'ROLE_CHIEF_EXPERIENCE_OFFICER')).toEqual([]);
    expect(hasInvestigationTab('ROLE_CUSTOMER_CARE_OFFICER')).toBe(false);
    expect(hasInvestigationTab('ROLE_SERVICE_QUALITY_DIRECTOR')).toBe(true);
  });

  test('tab label depends on role', () => {
    expect(investigationTabLabel('ROLE_SERVICE_QUALITY_DIRECTOR')).toBe('Service Quality Review');
    expect(investigationTabLabel('ROLE_CUSTOMER_CARE_SENIOR_MANAGER')).toBe('Investigation Review');
  });

  test('only the new review tasks are review tasks', () => {
    expect(isInvestigationReviewTask({ definitionKey: TASK_SM_REVIEW })).toBe(true);
    expect(isInvestigationReviewTask({ definitionKey: TASK_CXO })).toBe(false);
    expect(isInvestigationReviewTask(null)).toBe(false);
  });
});

describe('investigation payloads', () => {
  test('review payload trims remarks and defaults files', () => {
    expect(buildReviewPayload('  ok  ')).toEqual({ reviewRemarks: 'ok', reviewFiles: [] });
  });

  test('return decision carries the reason and no audit escalation fields', () => {
    const payload = buildCxoDecisionPayload(CXO_RETURN_TO_SENIOR_MANAGER, { reason: ' Not audit ' });
    expect(payload.cxoDecision).toBe(CXO_RETURN_TO_SENIOR_MANAGER);
    expect(payload.cxoReturnReason).toBe('Not audit');
    expect(payload.currentStage).toBeUndefined();
    expect(payload.escalatedBy).toBeUndefined();
  });

  test('send to audit keeps the audit escalation fields', () => {
    const files = [{ name: 'a.pdf' }];
    const payload = buildCxoDecisionPayload(CXO_SEND_TO_AUDIT, {
      remarks: 'go',
      files,
      existingFiles: [{ name: 'old.pdf' }]
    });
    expect(payload.cxoDecision).toBe(CXO_SEND_TO_AUDIT);
    expect(payload.currentStage).toBe('CHIEF_OPERATION_AUDIT');
    expect(payload.stage).toBe('AUDIT_INVESTIGATION');
    expect(payload.investigationFiles).toHaveLength(2);
  });

  test('review trail defaults to an empty list', () => {
    expect(reviewTrailOf(null)).toEqual([]);
    expect(reviewTrailOf({ variables: { investigationReviewTrail: [{ action: 'x' }] } })).toHaveLength(1);
  });
});
