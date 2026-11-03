import React from 'react';
import { Tag, Tooltip } from 'antd';
import { returnTagOf } from '../utils/investigationWorkflow';

const tagStyle = { fontWeight: 700, fontSize: '11px', borderRadius: '4px', margin: 0, whiteSpace: 'nowrap' };

/**
 * Marks a case row that was sent back, so it stands out from cases arriving for the first time.
 * Renders nothing for every other case.
 */
function InvestigationReturnTag({ record }) {
  const info = returnTagOf(record);
  if (!info) return null;
  const tooltip = info.reason
    ? `${info.by ? `${info.by}: ` : ''}${info.reason}`
    : info.label;
  return (
    <Tooltip title={tooltip}>
      <Tag color={info.color} style={tagStyle}>{info.label}</Tag>
    </Tooltip>
  );
}

export default InvestigationReturnTag;
