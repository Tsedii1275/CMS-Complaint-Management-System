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

export const AMHARIC_CATEGORY_LABELS = {
  'Customer Service Issues': 'የደንበኞች አገልግሎት ችግኝ',
  'Transaction Error': 'የግብይት ስህተት',
  'Account Management': 'የሂሳብ አስተዳደር',
  'Banking App Issues': 'የባንክ መተግበሪያ ችግኝ',
  'Credit/Financing Concerns': 'የብድር/ፋይናንስ ጉዳዮች',
  'Fraud & Security Risk': 'የማጭበርበር እና የደህንነት ስጋት',
  'Information Disclosure': 'የመረጃ ይፋ መደረግ',
  'ATM & Card Banking Issues': 'የኤቲኤም እና የካርድ ባንክ ችግኝ',
  'Policy & Compliance Disputes': 'የፖሊሲ እና የተገዢነት ክርክሮች',
  'System Failure': 'የስርዓት መቋረጥ',
  'Branch Operation': 'የቅርንጫፍ አሰራር',
  'Other': 'ሌላ',
  'Others': 'ሌሎች'
};

const AMHARIC_BY_LOWERCASE = Object.fromEntries(
  Object.entries(AMHARIC_CATEGORY_LABELS).map(([english, amharic]) => [english.toLowerCase(), amharic])
);

export function categoryLabel(name, language) {
  if (language !== 'amharic') return name;
  const key = String(name || '').trim().toLowerCase();
  return AMHARIC_BY_LOWERCASE[key] || name;
}

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
