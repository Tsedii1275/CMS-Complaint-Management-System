import unicodeFontUrl from '../../assets/fonts/AbyssinicaSIL-Regular.ttf';

const FONT_FILE = 'AbyssinicaSIL-Regular.ttf';
export const PDF_UNICODE_FONT = 'AbyssinicaSIL';

let cachedBase64 = null;

function arrayBufferToBase64(buffer) {
  const bytes = new Uint8Array(buffer);
  const chunks = [];
  const chunkSize = 0x2000;
  for (let i = 0; i < bytes.length; i += chunkSize) {
    chunks.push(String.fromCharCode.apply(null, bytes.subarray(i, i + chunkSize)));
  }
  return btoa(chunks.join(''));
}

async function fetchFont(url) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Unable to load PDF font (${response.status})`);
  }
  return arrayBufferToBase64(await response.arrayBuffer());
}

async function loadUnicodeFontBase64() {
  if (cachedBase64) {
    return cachedBase64;
  }

  const candidates = [
    unicodeFontUrl,
    `${process.env.PUBLIC_URL || ''}/fonts/${FONT_FILE}`
  ].filter(Boolean);

  let lastError = null;
  for (const url of candidates) {
    try {
      cachedBase64 = await fetchFont(url);
      return cachedBase64;
    } catch (err) {
      lastError = err;
    }
  }
  throw lastError || new Error('Unable to load PDF font');
}

export async function registerUnicodePdfFont(doc) {
  const base64 = await loadUnicodeFontBase64();
  doc.addFileToVFS(FONT_FILE, base64);
  doc.addFont(FONT_FILE, PDF_UNICODE_FONT, 'normal');
  doc.addFont(FONT_FILE, PDF_UNICODE_FONT, 'bold');
  doc.setFont(PDF_UNICODE_FONT, 'normal');
}
