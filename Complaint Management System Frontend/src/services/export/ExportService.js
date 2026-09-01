import { exportCsv } from './CsvExportService';
import { exportExcel } from './ExcelExportService';
import { exportPdf } from './PdfExportService';

export const EXPORT_FORMATS = {
  xlsx: 'xlsx',
  csv: 'csv',
  pdf: 'pdf'
};

/**
 * Shared export entry point for every module.
 * @param {{
 *   format: 'xlsx'|'csv'|'pdf',
 *   filename: string,
 *   documentTitle?: string,
 *   subtitle?: string,
 *   tables: Array<{ title?: string, sheetName?: string, columns: Array<{header:string,key?:string,type?:string,accessor?:Function}>, rows: object[] }>,
 *   landscape?: boolean
 * }} options
 */
export async function exportDataset(options) {
  const format = options.format === 'excel' ? 'xlsx' : options.format;
  const payload = { ...options, filename: options.filename.replace(/\.(xlsx|xls|csv|pdf)$/i, '') };

  if (format === 'csv') {
    exportCsv(payload);
    return;
  }
  if (format === 'pdf') {
    await exportPdf(payload);
    return;
  }
  await exportExcel(payload);
}

export default { exportDataset, EXPORT_FORMATS };
