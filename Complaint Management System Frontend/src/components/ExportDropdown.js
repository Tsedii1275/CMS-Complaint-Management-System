import React, { useState } from 'react';
import { Button, Dropdown, message } from 'antd';
import { DownloadOutlined, DownOutlined, FileExcelOutlined, FilePdfOutlined, FileTextOutlined } from '@ant-design/icons';
import { BRAND_COLORS } from '../constants/theme';
import { exportDataset } from '../services/export/ExportService';

function hasRows(tables = []) {
  return tables.some(table => Array.isArray(table.rows) && table.rows.length > 0);
}

/**
 * System-wide export control. Use this everywhere instead of page-specific export buttons.
 */
function ExportDropdown({
  filename,
  documentTitle,
  subtitle,
  tables,
  getDataset,
  disabled = false,
  size,
  buttonText = 'Export'
}) {
  const [exporting, setExporting] = useState(false);

  const runExport = async (format) => {
    const dataset = typeof getDataset === 'function'
      ? getDataset()
      : { filename, documentTitle, subtitle, tables };

    if (!dataset || !hasRows(dataset.tables)) {
      message.warning('No records match the current filter criteria to export.');
      return;
    }

    try {
      setExporting(true);
      message.loading({ content: `Preparing ${format.toUpperCase()} export...`, key: 'cms-export' });
      await exportDataset({ ...dataset, format });
      message.success({ content: 'Export completed.', key: 'cms-export' });
    } catch (err) {
      console.error('Export failed:', err);
      message.error({ content: 'Export failed.', key: 'cms-export' });
    } finally {
      setExporting(false);
    }
  };

  return (
    <Dropdown
      trigger={['click']}
      disabled={disabled || exporting}
      menu={{
        items: [
          { key: 'xlsx', icon: <FileExcelOutlined />, label: 'Export Excel (.xlsx)', onClick: () => runExport('xlsx') },
          { key: 'csv', icon: <FileTextOutlined />, label: 'Export CSV (.csv)', onClick: () => runExport('csv') },
          { key: 'pdf', icon: <FilePdfOutlined />, label: 'Export PDF (.pdf)', onClick: () => runExport('pdf') }
        ]
      }}
    >
      <Button
        type="primary"
        size={size}
        loading={exporting}
        icon={<DownloadOutlined />}
        style={{
          backgroundColor: BRAND_COLORS.primary,
          borderColor: BRAND_COLORS.primary,
          fontWeight: 600,
          borderRadius: '6px'
        }}
      >
        {buttonText} <DownOutlined />
      </Button>
    </Dropdown>
  );
}

export default ExportDropdown;
