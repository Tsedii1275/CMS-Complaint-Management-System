import { isSecondaryReviewTask, validateResolutionForm } from './workUnitResolution';

describe('work unit resolution form', () => {
  test('an empty form is rejected for both mandatory fields', () => {
    const errors = validateResolutionForm({ resolutionDetails: '', actionTaken: '' }, false);
    expect(Object.keys(errors).sort()).toEqual(['actionTaken', 'resolutionDetails']);
  });

  test('blank spaces do not count as an answer', () => {
    const errors = validateResolutionForm({ resolutionDetails: '   ', actionTaken: ' \n ' }, false);
    expect(errors.resolutionDetails).toBeTruthy();
    expect(errors.actionTaken).toBeTruthy();
  });

  test('a filled form passes', () => {
    expect(validateResolutionForm({ resolutionDetails: 'Fixed', actionTaken: 'Refunded' }, false)).toEqual({});
  });

  test('secondary review only needs the findings', () => {
    expect(validateResolutionForm({ resolutionDetails: 'Findings', actionTaken: '' }, true)).toEqual({});
    expect(validateResolutionForm({ resolutionDetails: '', actionTaken: 'x' }, true).resolutionDetails).toBeTruthy();
  });

  test('missing form data is treated as empty', () => {
    expect(Object.keys(validateResolutionForm(undefined, false))).toHaveLength(2);
  });

  test('secondary review is detected from the task name', () => {
    expect(isSecondaryReviewTask({ name: 'Secondary Resolution Review' })).toBe(true);
    expect(isSecondaryReviewTask({ name: 'Work Unit Resolution' })).toBe(false);
    expect(isSecondaryReviewTask(null)).toBe(false);
  });
});
