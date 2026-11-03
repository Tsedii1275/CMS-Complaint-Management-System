import React from 'react';
import { Button, Typography } from 'antd';
import { PaperClipOutlined } from '@ant-design/icons';
import ApiService from '../services/api';
import { collectAttachmentGroups, isAudioAttachment } from '../utils/attachments';

const { Text } = Typography;

/**
 * Lists every document and recording attached to the complaint at any stage, so each team in the
 * investigation loop (and Audit and the Committee) sees the full set.
 */
function CaseAttachments({ variables, style }) {
  const groups = collectAttachmentGroups(variables);
  const total = groups.reduce((sum, group) => sum + group.files.length, 0);

  return (
    <div style={{ marginTop: '16px', ...style }}>
      <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '6px' }}>
        <PaperClipOutlined style={{ marginRight: '4px' }} />
        Documents{total > 0 ? ` (${total})` : ''}
      </div>
      {total === 0 ? (
        <Text type="secondary" style={{ fontSize: '13px' }}>No documents attached.</Text>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {groups.map((group) => (
            <div key={group.key}>
              <div style={{ fontSize: '11px', fontWeight: 700, color: '#475569', textTransform: 'uppercase', marginBottom: '4px' }}>
                {group.label}
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {group.files.map((file) => (
                  <div key={file.url} style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
                    <Text style={{ fontSize: '13px', color: '#0f172a', wordBreak: 'break-word' }}>{file.name}</Text>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
                      {isAudioAttachment(file) ? (
                        <audio
                          src={ApiService.getAttachmentUrl(file.url)}
                          controls
                          style={{ height: '28px', maxWidth: '180px' }}
                        >
                          <track kind="captions" />
                        </audio>
                      ) : null}
                      <a href={ApiService.getAttachmentUrl(file.url)} download={file.name} target="_blank" rel="noopener noreferrer">
                        <Button size="small" type="link" style={{ padding: 0 }}>Download</Button>
                      </a>
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default CaseAttachments;
