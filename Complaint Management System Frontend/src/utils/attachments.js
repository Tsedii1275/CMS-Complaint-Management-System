function cleanText(value) {
  const text = value == null ? '' : String(value).trim();
  if (!text || text.toLowerCase() === 'null') {
    return '';
  }
  return text;
}

function fileNameFromUrl(url) {
  const path = cleanText(url).split('?')[0];
  const name = path.slice(path.lastIndexOf('/') + 1);
  return name || 'Attachment';
}

function addAttachment(files, seen, url, name) {
  const cleanUrl = cleanText(url);
  if (!cleanUrl || seen.has(cleanUrl)) {
    return;
  }
  seen.add(cleanUrl);
  files.push({ url: cleanUrl, name: cleanText(name) || fileNameFromUrl(cleanUrl) });
}

function addAttachmentSource(files, seen, source) {
  if (!source) {
    return;
  }
  if (Array.isArray(source)) {
    source.forEach((item) => addAttachmentSource(files, seen, item));
    return;
  }
  if (typeof source === 'string') {
    addAttachment(files, seen, source, '');
    return;
  }
  addAttachment(
    files,
    seen,
    source.url || source.evidenceUrl || source.voiceUrl || source.attachmentUrl,
    source.name || source.fileName || source.evidenceName || source.voiceName
  );
}

export function collectAttachments(...sources) {
  const files = [];
  const seen = new Set();
  sources.forEach((source) => addAttachmentSource(files, seen, source));
  return files;
}

function textOf(...values) {
  for (const value of values) {
    const text = cleanText(value);
    if (text) return text;
  }
  return '';
}

/**
 * Every document that belongs to one complaint, grouped by where it came from.
 * A file is listed once, under the first group that holds it, so the same case shows the same
 * documents to the Senior Manager, Service Quality, CXO, Audit team and Committee alike.
 */
export function collectAttachmentGroups(variables) {
  const vars = variables || {};
  const complaint = vars.complaint || {};
  const defs = [
    {
      key: 'complaint',
      label: 'Complaint Evidence',
      sources: [
        complaint.evidenceFiles,
        vars.evidenceFiles,
        {
          url: textOf(complaint.evidenceUrl, complaint.attachmentUrl, vars.evidenceUrl, vars.attachmentUrl),
          name: textOf(complaint.evidenceName, complaint.attachmentName, vars.evidenceName, vars.attachmentName)
        }
      ]
    },
    {
      key: 'voice',
      label: 'Voice Recordings',
      sources: [
        complaint.voiceFiles,
        vars.voiceFiles,
        {
          url: textOf(complaint.voiceAttachmentUrl, complaint.voiceUrl, vars.voiceAttachmentUrl, vars.voiceUrl),
          name: textOf(complaint.voiceAttachmentName, complaint.voiceName, vars.voiceAttachmentName, vars.voiceName)
        }
      ]
    },
    { key: 'workUnit', label: 'Work Unit Documents', sources: [vars.workUnitFiles] },
    {
      key: 'investigation',
      label: 'Investigation Review Documents',
      sources: [vars.investigationFiles, vars.reviewFiles, vars.cxFiles]
    },
    {
      key: 'audit',
      label: 'Audit Documents',
      sources: [vars.auditAttachments, { url: vars.auditAttachment, name: vars.auditAttachmentName }]
    },
    {
      key: 'committee',
      label: 'Committee Documents',
      sources: [vars.committeeAttachments, { url: vars.committeeAttachment, name: vars.committeeAttachmentName }]
    }
  ];

  const seen = new Set();
  return defs
    .map((def) => ({
      key: def.key,
      label: def.label,
      files: collectAttachments(...def.sources).filter((file) => {
        if (seen.has(file.url)) return false;
        seen.add(file.url);
        return true;
      })
    }))
    .filter((group) => group.files.length > 0);
}

export function isAudioAttachment(file) {
  const label = `${file?.name || ''} ${file?.url || ''}`.toLowerCase();
  return ['.mp3', '.wav', '.m4a', '.webm', '.ogg'].some((extension) => label.includes(extension));
}
