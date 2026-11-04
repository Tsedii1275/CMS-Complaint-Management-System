function saveType(mimeType) {
  const type = mimeType || 'application/octet-stream';
  // Firefox opens text/csv itself. That open starts another download, so the file list never stops.
  if (/^text\/csv\b/i.test(type)) return 'application/csv';
  return type;
}

export function downloadBlob(data, filename, mimeType) {
  const type = saveType(mimeType);
  const blob = data instanceof Blob ? data : new Blob([data], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename || 'export';
  link.rel = 'noopener';
  link.style.display = 'none';
  document.body.appendChild(link);
  // Firefox drops the file when the click happens in the same moment the address is created.
  window.setTimeout(() => link.click(), 0);
  window.setTimeout(() => {
    link.remove();
    URL.revokeObjectURL(url);
  }, 40000);
}
