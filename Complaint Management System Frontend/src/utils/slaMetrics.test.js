import { isSlaBreached, isCurrentStageSlaBreached } from './slaMetrics';

test('sticky breached flag with elapsed under allowed is not an overall clock breach', () => {
  expect(isSlaBreached({
    breached: true,
    slaStatus: 'BREACHED',
    status: 'ESCALATED',
    totalAllowedMinutes: 1440,
    totalElapsedMinutes: 12,
  })).toBe(false);
});

test('elapsed over allowed is an overall clock breach', () => {
  expect(isSlaBreached({
    breached: false,
    slaStatus: 'ON_TRACK',
    totalAllowedMinutes: 30,
    totalElapsedMinutes: 45,
  })).toBe(true);
});

test('current stage SLA uses ledger status not case ESCALATED', () => {
  expect(isCurrentStageSlaBreached({ status: 'ESCALATED', ledgerStageStatus: 'IN_PROGRESS' })).toBe(false);
  expect(isCurrentStageSlaBreached({ status: 'ON_TRACK', ledgerStageStatus: 'BREACHED' })).toBe(true);
});
