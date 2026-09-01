import moment from 'moment';
import { STATUS_CONFIG, resolveOverallComplaintStatus, normalizeComplaintStatus } from '../../utils/statusUtils';

export const EMPTY_VALUE = '-';

function isBlank(value) {
  return value === null
    || value === undefined
    || value === ''
    || value === 'null'
    || value === 'undefined'
    || value === 'N/A'
    || value === '—';
}

export function formatBoolean(value) {
  if (isBlank(value)) return EMPTY_VALUE;
  if (typeof value === 'boolean') return value ? 'Yes' : 'No';
  const text = String(value).trim().toLowerCase();
  if (['true', 'yes', 'y', '1'].includes(text)) return 'Yes';
  if (['false', 'no', 'n', '0'].includes(text)) return 'No';
  return String(value);
}

export function formatExportDate(value) {
  if (isBlank(value)) return EMPTY_VALUE;
  const parsed = moment(value);
  if (!parsed.isValid()) return String(value);
  const asString = String(value).trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(asString) || (parsed.hours() === 0 && parsed.minutes() === 0 && parsed.seconds() === 0 && parsed.milliseconds() === 0)) {
    return parsed.format('DD/MM/YYYY');
  }
  return parsed.format('DD/MM/YYYY HH');
}

export function formatPercent(value) {
  if (isBlank(value)) return EMPTY_VALUE;
  if (typeof value === 'string' && value.includes('%')) {
    const numeric = Number(String(value).replace('%', '').trim());
    if (Number.isNaN(numeric)) return value;
    return `${numeric.toFixed(2)}%`;
  }
  const numeric = Number(value);
  if (Number.isNaN(numeric)) return String(value);
  const percent = Math.abs(numeric) <= 1 && numeric !== 0 ? numeric * 100 : numeric;
  return `${percent.toFixed(2)}%`;
}

export function formatCurrency(value) {
  if (isBlank(value)) return EMPTY_VALUE;
  const numeric = Number(String(value).replace(/[^\d.-]/g, ''));
  if (Number.isNaN(numeric)) return String(value);
  return `ETB ${numeric.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function humanizeLabel(value) {
  return String(value)
    .replaceAll('_', ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase()
    .replace(/\b\w/g, char => char.toUpperCase());
}

export function formatStatus(value, row) {
  if (row && typeof row === 'object' && !Array.isArray(row)) {
    const resolved = resolveOverallComplaintStatus(row);
    if (STATUS_CONFIG[resolved]?.label) {
      return humanizeLabel(STATUS_CONFIG[resolved].label);
    }
  }
  if (isBlank(value)) return EMPTY_VALUE;
  if (typeof value === 'object') {
    const resolved = resolveOverallComplaintStatus(value);
    return STATUS_CONFIG[resolved]?.label ? humanizeLabel(STATUS_CONFIG[resolved].label) : EMPTY_VALUE;
  }
  const normalized = normalizeComplaintStatus(value, null);
  if (normalized && STATUS_CONFIG[normalized]) {
    return humanizeLabel(STATUS_CONFIG[normalized].label);
  }
  return humanizeLabel(value);
}

export function formatExportValue(value, type, row) {
  if (type === 'boolean' || typeof value === 'boolean') {
    return formatBoolean(value);
  }
  if (type === 'date') {
    return formatExportDate(value);
  }
  if (type === 'percent') {
    return formatPercent(value);
  }
  if (type === 'currency') {
    return formatCurrency(value);
  }
  if (type === 'status') {
    return formatStatus(value, row);
  }
  if (type === 'label') {
    if (isBlank(value)) return EMPTY_VALUE;
    return humanizeLabel(value);
  }
  if (isBlank(value)) {
    return EMPTY_VALUE;
  }
  return String(value);
}

export function cellFromColumn(column, row, index) {
  if (typeof column.accessor === 'function') {
    return formatExportValue(column.accessor(row, index), column.type, row);
  }
  const key = column.key;
  const raw = key === undefined ? undefined : row?.[key];
  return formatExportValue(raw, column.type, row);
}

export function tableToMatrix(table) {
  const headers = (table.columns || []).map(column => column.header);
  const rows = (table.rows || []).map((row, index) =>
    (table.columns || []).map(column => cellFromColumn(column, row, index))
  );
  return { title: table.title || '', sheetName: table.sheetName || table.title || 'Sheet1', headers, rows };
}

export function sanitizeSheetName(name) {
  const cleaned = String(name || 'Sheet1').replace(/[\\/?*[\]:]/g, ' ').trim();
  return (cleaned || 'Sheet1').slice(0, 31);
}
