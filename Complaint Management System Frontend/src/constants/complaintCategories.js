export const STANDARD_COMPLAINT_CATEGORIES = [
  'Customer Service Issues',
  'Transaction Error',
  'Account Management',
  'Banking App Issues',
  'Credit/Financing Concerns',
  'Fraud & Security Risk',
  'Information Disclosure',
  'ATM & Card Banking Issues',
  'Policy & Compliance Disputes',
  'System Failure',
  'Branch Operation'
];

export function categoryNamesFromResponse(rows) {
  if (!Array.isArray(rows) || rows.length === 0) {
    return [...STANDARD_COMPLAINT_CATEGORIES];
  }
  const names = rows
    .map((row) => (typeof row === 'string' ? row : row?.name))
    .map((name) => String(name || '').trim())
    .filter(Boolean);
  return names.length > 0 ? names : [...STANDARD_COMPLAINT_CATEGORIES];
}
