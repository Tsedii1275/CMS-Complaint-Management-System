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

export function isAudioAttachment(file) {
  const label = `${file?.name || ''} ${file?.url || ''}`.toLowerCase();
  return ['.mp3', '.wav', '.m4a', '.webm', '.ogg'].some((extension) => label.includes(extension));
}
