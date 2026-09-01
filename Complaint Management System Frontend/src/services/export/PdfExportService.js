import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import moment from 'moment';
import { tableToMatrix } from './formatters';
import { downloadBlob } from './download';
import { PDF_UNICODE_FONT, registerUnicodePdfFont } from './pdfFont';

const PRIMARY = [1, 33, 105];

function shouldUseLandscape(tables) {
  return tables.some(table => (table.columns || []).length > 7);
}

function drawHeader(doc, documentTitle, subtitle, pageWidth) {
  doc.setFillColor(...PRIMARY);
  doc.rect(0, 0, pageWidth, 18, 'F');
  doc.setTextColor(255, 255, 255);
  doc.setFont(PDF_UNICODE_FONT, 'normal');
  doc.setFontSize(12);
  doc.text('Dashen Bank S.C.', 12, 8);
  doc.setFontSize(9);
  doc.text('Complaint Management System', 12, 14);

  doc.setTextColor(15, 23, 42);
  doc.setFontSize(11);
  doc.text(documentTitle || 'Export Report', 12, 26);
  if (subtitle) {
    doc.setFontSize(9);
    doc.setTextColor(100, 116, 139);
    const lines = doc.splitTextToSize(String(subtitle), pageWidth - 24);
    doc.text(lines, 12, 32);
  }
}

export async function exportPdf({ filename, documentTitle, subtitle, tables = [], landscape }) {
  const orientation = landscape || shouldUseLandscape(tables) ? 'landscape' : 'portrait';
  const doc = new jsPDF({ orientation, unit: 'mm', format: 'a4' });
  await registerUnicodePdfFont(doc);

  const pageWidth = doc.internal.pageSize.getWidth();
  const pageHeight = doc.internal.pageSize.getHeight();
  const startY = subtitle ? 38 : 32;
  const tableFont = {
    font: PDF_UNICODE_FONT,
    fontStyle: 'normal',
    fontSize: 8,
    cellPadding: 2,
    overflow: 'linebreak',
    valign: 'middle',
    textColor: [15, 23, 42],
    lineColor: [226, 232, 240],
    lineWidth: 0.2
  };

  tables.forEach((table, index) => {
    const matrix = tableToMatrix(table);
    let y = index === 0 ? startY : (doc.lastAutoTable ? doc.lastAutoTable.finalY + 12 : startY);
    if (matrix.title) {
      doc.setFont(PDF_UNICODE_FONT, 'normal');
      doc.setFontSize(10);
      doc.setTextColor(...PRIMARY);
      doc.text(matrix.title, 12, y);
      y += 5;
    }
    autoTable(doc, {
      startY: y,
      head: [matrix.headers],
      body: matrix.rows.map(row => row.map(cell => String(cell ?? '-'))),
      theme: 'grid',
      styles: {
        ...tableFont,
        fontSize: matrix.headers.length > 10 ? 6 : 8,
        minCellWidth: 16
      },
      headStyles: {
        font: PDF_UNICODE_FONT,
        fontStyle: 'normal',
        fillColor: PRIMARY,
        textColor: 255,
        halign: 'left',
        valign: 'middle'
      },
      alternateRowStyles: { fillColor: [248, 250, 252] },
      margin: { top: 36, left: 10, right: 10, bottom: 16 },
      showHead: 'everyPage',
      horizontalPageBreak: matrix.headers.length > 8,
      horizontalPageBreakRepeat: matrix.headers.length > 8 ? [0, 1] : undefined,
      didDrawPage: () => {
        drawHeader(doc, documentTitle, subtitle, pageWidth);
      }
    });
  });

  const pageCount = doc.getNumberOfPages();
  for (let page = 1; page <= pageCount; page += 1) {
    doc.setPage(page);
    if (tables.length === 0) {
      drawHeader(doc, documentTitle, subtitle, pageWidth);
    }
    doc.setFont(PDF_UNICODE_FONT, 'normal');
    doc.setFontSize(8);
    doc.setTextColor(100, 116, 139);
    doc.text(`Generated ${moment().format('DD/MM/YYYY HH')}`, 12, pageHeight - 8);
    doc.text(`Page ${page} of ${pageCount}`, pageWidth - 12, pageHeight - 8, { align: 'right' });
  }

  const blob = doc.output('blob');
  downloadBlob(blob, `${filename}.pdf`, 'application/pdf');
}
