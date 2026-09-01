import ExcelJS from 'exceljs/dist/exceljs.min.js';
import { BRAND_COLORS } from '../../constants/theme';
import { sanitizeSheetName, tableToMatrix } from './formatters';
import { downloadBlob } from './download';

function hexToArgb(hex) {
  const cleaned = hex.replace('#', '');
  return `FF${cleaned.toUpperCase()}`;
}

function autoFitColumns(sheet) {
  sheet.columns.forEach((column) => {
    let max = 12;
    column.eachCell({ includeEmpty: true }, (cell) => {
      const value = cell.value == null ? '' : String(cell.value);
      const longestLine = value.split(/\r?\n/).reduce((acc, line) => Math.max(acc, line.length), 0);
      max = Math.max(max, longestLine);
    });
    column.width = Math.min(45, Math.max(14, max + 2));
  });
}

function styleHeader(row) {
  row.font = { bold: true, color: { argb: 'FFFFFFFF' }, name: 'Calibri', size: 11 };
  row.fill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: hexToArgb(BRAND_COLORS.primary) }
  };
  row.alignment = { vertical: 'middle', horizontal: 'center', wrapText: true };
  row.height = 24;
}

function styleBodyCell(cell) {
  cell.alignment = { vertical: 'middle', horizontal: 'left', wrapText: true };
  cell.font = { name: 'Calibri', size: 10 };
  cell.border = {
    top: { style: 'thin', color: { argb: 'FFE2E8F0' } },
    left: { style: 'thin', color: { argb: 'FFE2E8F0' } },
    bottom: { style: 'thin', color: { argb: 'FFE2E8F0' } },
    right: { style: 'thin', color: { argb: 'FFE2E8F0' } }
  };
}

export async function exportExcel({ filename, documentTitle, subtitle, tables = [] }) {
  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'Complaint Management System';
  workbook.company = 'Dashen Bank S.C.';
  workbook.created = new Date();
  workbook.modified = new Date();

  const usedNames = new Set();
  tables.forEach((table, tableIndex) => {
    const matrix = tableToMatrix(table);
    let sheetName = sanitizeSheetName(matrix.sheetName || `Sheet${tableIndex + 1}`);
    if (usedNames.has(sheetName)) {
      sheetName = sanitizeSheetName(`${sheetName.slice(0, 28)}_${tableIndex + 1}`);
    }
    usedNames.add(sheetName);

    const sheet = workbook.addWorksheet(sheetName, {
      views: [{ state: 'frozen', ySplit: 1 }]
    });

    const headerCount = matrix.headers.length || 1;
    if (documentTitle) {
      sheet.mergeCells(1, 1, 1, headerCount);
      const titleCell = sheet.getCell(1, 1);
      titleCell.value = documentTitle;
      titleCell.font = { bold: true, size: 14, color: { argb: hexToArgb(BRAND_COLORS.primary) }, name: 'Calibri' };
      titleCell.alignment = { vertical: 'middle', horizontal: 'left' };
    }
    if (subtitle) {
      const rowIndex = documentTitle ? 2 : 1;
      sheet.mergeCells(rowIndex, 1, rowIndex, headerCount);
      const subCell = sheet.getCell(rowIndex, 1);
      subCell.value = subtitle;
      subCell.font = { size: 10, color: { argb: 'FF64748B' }, name: 'Calibri' };
    }

    const headerRowIndex = (documentTitle ? 1 : 0) + (subtitle ? 1 : 0) + 1;
    if (documentTitle || subtitle) {
      sheet.views = [{ state: 'frozen', ySplit: headerRowIndex }];
    }

    matrix.headers.forEach((header, colIndex) => {
      const cell = sheet.getCell(headerRowIndex, colIndex + 1);
      cell.value = header;
    });
    styleHeader(sheet.getRow(headerRowIndex));

    matrix.rows.forEach((row, rowIndex) => {
      const excelRow = sheet.getRow(headerRowIndex + 1 + rowIndex);
      row.forEach((value, colIndex) => {
        const cell = excelRow.getCell(colIndex + 1);
        cell.value = value;
        styleBodyCell(cell);
      });
    });

    autoFitColumns(sheet);
    sheet.pageSetup = {
      orientation: headerCount > 8 ? 'landscape' : 'portrait',
      fitToPage: true,
      fitToWidth: 1,
      fitToHeight: 0,
      paperSize: 9
    };
  });

  if (workbook.worksheets.length === 0) {
    workbook.addWorksheet('Sheet1');
  }

  const buffer = await workbook.xlsx.writeBuffer();
  downloadBlob(
    buffer,
    `${filename}.xlsx`,
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  );
}
