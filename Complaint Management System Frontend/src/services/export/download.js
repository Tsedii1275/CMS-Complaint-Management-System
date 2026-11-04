function saveType(mimeType) {
  const type = mimeType || 'application/octet-stream';
  // Firefox remembers a CSV as a file it should open itself. Clicking that finished
  // download opens Firefox again, which starts another CSV download.
  if (/^text\/csv\b/i.test(type)) return 'application/octet-stream';
  return type;
}

export function downloadBlob(data, filename, mimeType) {
  const type = saveType(mimeType);
  const blob = data instanceof Blob ? data : new Blob([data], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename || 'export';
  link.type = type;
  link.rel = 'noopener';
  link.style.display = 'none';
  document.body.appendChild(link);
  link.click();
  // Firefox reads the file after click() returns. Removing the address immediately aborts the download.
  window.setTimeout(() => {
    link.remove();
    URL.revokeObjectURL(url);
  }, 15000);
}
