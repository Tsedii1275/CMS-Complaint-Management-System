import {
  TASK_SM_REVIEW,
  TASK_SQ_REVIEW,
  TASK_SM_CLOSURE,
  TASK_SQ_RETURN,
  TASK_CXO,
  CXO_RETURN_TO_SENIOR_MANAGER,
  CXO_RETURN_TO_SERVICE_QUALITY,
  CXO_SEND_TO_AUDIT,
  SQ_SEND_TO_CXO,
  SQ_RETURN_TO_SENIOR_MANAGER,
  SM_CLOSE,
  SM_SEND_TO_SERVICE_QUALITY,
  buildCxoDecisionPayload,
  buildReviewPayload,
  buildSqDecisionPayload,
  buildSmDecisionPayload,
  returnTagOf,
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

describe('returned case tag', () => {
  const tagged = (definitionKey, tag, extra = {}) => ({
    definitionKey,
    variables: { investigationReturnTag: tag, investigationReturnReason: 'why', investigationReturnedBy: 'Someone', ...extra }
  });

  test('a new case has no tag', () => {
    expect(returnTagOf({ definitionKey: TASK_SM_REVIEW, variables: {} })).toBeNull();
    expect(returnTagOf({ definitionKey: TASK_SQ_REVIEW, variables: { investigationReturnTag: '' } })).toBeNull();
  });

  test('each way of sending a case back has its own tag with the reason', () => {
    expect(returnTagOf(tagged(TASK_SM_CLOSURE, 'RETURNED_BY_SERVICE_QUALITY')).label).toBe('Returned by Service Quality');
    expect(returnTagOf(tagged(TASK_SQ_RETURN, 'RETURNED_BY_CXO')).label).toBe('Returned by CXO');
    expect(returnTagOf(tagged(TASK_SM_CLOSURE, 'RETURNED_TO_SENIOR_MANAGER')).label).toBe('Returned to Senior Manager');
    const resent = returnTagOf(tagged(TASK_SQ_REVIEW, 'RESENT_BY_SENIOR_MANAGER'));
    expect(resent.label).toBe('Resent by Senior Manager');
    expect(resent.reason).toBe('why');
    expect(resent.by).toBe('Someone');
  });

  test('tasks outside the review chain are never tagged', () => {
    expect(returnTagOf(tagged('FormTask_43', 'RETURNED_BY_CXO'))).toBeNull();
    expect(returnTagOf(tagged(TASK_SQ_REVIEW, 'SOMETHING_ELSE'))).toBeNull();
    expect(returnTagOf(null)).toBeNull();
  });

  test('service quality sees the returned-case step in its queue', () => {
    const queue = investigationTasksForRole([{ id: 'a', definitionKey: TASK_SQ_RETURN }], 'ROLE_SERVICE_QUALITY_DIRECTOR');
    expect(queue.map((t) => t.id)).toEqual(['a']);
    expect(investigationTasksForRole([{ id: 'a', definitionKey: TASK_SQ_RETURN }], 'ROLE_CUSTOMER_CARE_SENIOR_MANAGER')).toEqual([]);
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

  test('service quality payloads carry the decision and only the return needs a reason', () => {
    const send = buildSqDecisionPayload(SQ_SEND_TO_CXO, { remarks: ' fine ' });
    expect(send).toEqual({ sqDecision: SQ_SEND_TO_CXO, reviewRemarks: 'fine', reviewFiles: [] });
    const back = buildSqDecisionPayload(SQ_RETURN_TO_SENIOR_MANAGER, { reason: ' missing statement ' });
    expect(back.sqDecision).toBe(SQ_RETURN_TO_SENIOR_MANAGER);
    expect(back.sqReturnReason).toBe('missing statement');
  });

  test('senior manager can close or send the case back with notes', () => {
    const close = buildSmDecisionPayload(SM_CLOSE, { summary: ' resolved ', notes: 'ignored' });
    expect(close).toEqual({ smDecision: SM_CLOSE, reviewRemarks: 'resolved', reviewFiles: [] });
    const resend = buildSmDecisionPayload(SM_SEND_TO_SERVICE_QUALITY, { summary: 'ignored', notes: ' new facts ' });
    expect(resend.smDecision).toBe(SM_SEND_TO_SERVICE_QUALITY);
    expect(resend.smResendReason).toBe('new facts');
    expect(resend.reviewRemarks).toBeUndefined();
  });

  test('cxo return goes to service quality with a reason', () => {
    const payload = buildCxoDecisionPayload(CXO_RETURN_TO_SERVICE_QUALITY, { reason: ' dig deeper ' });
    expect(payload.cxoDecision).toBe(CXO_RETURN_TO_SERVICE_QUALITY);
    expect(payload.cxoReturnReason).toBe('dig deeper');
    expect(payload.escalatedBy).toBeUndefined();
  });

  test('review trail defaults to an empty list', () => {
    expect(reviewTrailOf(null)).toEqual([]);
    expect(reviewTrailOf({ variables: { investigationReviewTrail: [{ action: 'x' }] } })).toHaveLength(1);
  });
});
