import { tableToMatrix } from './formatters';
import { downloadBlob } from './download';

function csvEscape(value) {
  const text = value === null || value === undefined ? '' : String(value);
  if (/[",\r\n]/.test(text)) {
    return `"${text.replaceAll('"', '""')}"`;
  }
  return text;
}

function matrixToCsv(headers, rows) {
  const lines = [headers.map(csvEscape).join(',')];
  rows.forEach(row => {
    lines.push(row.map(csvEscape).join(','));
  });
  return lines.join('\r\n');
}

export function exportCsv({ filename, documentTitle, subtitle, tables = [] }) {
  const sections = [];
  if (documentTitle) sections.push(csvEscape(documentTitle));
  if (subtitle) sections.push(csvEscape(subtitle));
  if (documentTitle || subtitle) sections.push('');

  tables.forEach((table, index) => {
    const matrix = tableToMatrix(table);
    if (matrix.title) {
      sections.push(csvEscape(matrix.title));
    }
    sections.push(matrixToCsv(matrix.headers, matrix.rows));
    if (index < tables.length - 1) {
      sections.push('');
    }
  });

  const csv = `\uFEFF${sections.join('\r\n')}`;
  downloadBlob(csv, `${filename}.csv`, 'text/csv;charset=utf-8;');
}
