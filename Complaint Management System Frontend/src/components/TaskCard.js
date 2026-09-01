export const PRIORITY_CONFIG = {
  high_sensitive:    { label: 'Highly Sensitive', bg: '#fff1f0', border: '#ffa39e', dot: '#ff4d4f', text: '#cf1322' },
  highly_sensitive:  { label: 'Highly Sensitive', bg: '#fff1f0', border: '#ffa39e', dot: '#ff4d4f', text: '#cf1322' },
  sensitive:         { label: 'Sensitive',        bg: '#fff7e6', border: '#ffd591', dot: '#fa8c16', text: '#ad6800' },
  normal:            { label: 'General',          bg: '#f6ffed', border: '#b7eb8f', dot: '#52c41a', text: '#389e0d' },
  general:           { label: 'General',          bg: '#f6ffed', border: '#b7eb8f', dot: '#52c41a', text: '#389e0d' },
};

const TASK_TITLE_MAP = {
  // CMD
  'Screen and categorize complaints for proper routing': 'Screening',
  'screens the complaint categorize and set its priority level': 'Categorize & Route',
  // Audit
  'Investigate the complaint': 'Investigate',
  'conduct an investigation': 'Investigate',
  // WorkUnit
  'Investigate and resolve the complaint': 'Resolve',
  'resolve the complaint': 'Resolve',
  // Chief Committee
  'Review investigation findings and make a decision': 'Review & Decide',
  'Chief Committee Review': 'Review & Decide',
  // Service Quality
  'Notify Customer of Resolution': 'Notify Customer',
  'Send resolution notification': 'Notify Customer',
};

export function getShortTitle(name) {
  if (!name) return 'Task';
  const key = Object.keys(TASK_TITLE_MAP).find(k =>
    name.toLowerCase().includes(k.toLowerCase())
  );
  if (key) return TASK_TITLE_MAP[key];
  return name.length > 22 ? name.substring(0, 22) + '…' : name;
}

export function formatTime(isoStr) {
  if (!isoStr) return '—';
  try {
    const d = new Date(isoStr);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } catch { return '—'; }
}

export function formatDate(isoStr) {
  if (!isoStr) return '—';
  try {
    const d = new Date(isoStr);
    const today = new Date();
    const isToday = d.toDateString() === today.toDateString();
    if (isToday) return `Today ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
    return d.toLocaleDateString([], { month: 'short', day: 'numeric' }) + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } catch { return '—'; }
}

