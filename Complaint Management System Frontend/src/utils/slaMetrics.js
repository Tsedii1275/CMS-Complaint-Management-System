/**
 * Single frontend interpretation of SLA fields from complaint_sla_metrics.
 * Status values are calculated only by the Java SLA engine.
 */

export function isTerminalOverallStatus(status) {
  const s = (status || '').toUpperCase();
  return s === 'CLOSED' || s === 'RESOLVED' || s === 'DECLINED';
}

export function isClosedForCompliance(item) {
  const s = (item?.status || '').toUpperCase();
  return s === 'CLOSED' || s === 'RESOLVED';
}

export function isSlaBreached(item) {
  if (!item) return false;
  const st = (item.slaStatus || '').toUpperCase();
  return Boolean(item.breached) || st === 'BREACHED' || st === 'RESOLVED_AFTER_SLA';
}

export function isSlaApproaching(item) {
  return (item?.slaStatus || '').toUpperCase() === 'APPROACHING';
}

export function isResolvedAtFcr(item) {
  if (!item) return false;
  const fcr = item.fcrStatus;
  if (fcr === true || fcr === 1 || fcr === '1' || String(fcr).toUpperCase() === 'TRUE' || String(fcr).toUpperCase() === 'VERIFIED') {
    return true;
  }
  const status = String(item.status || item.overallStatus || '').toUpperCase();
  return status === 'FCR_RESOLVED';
}

export function isResolvedWithinSla(item) {
  return (item?.slaStatus || '').toUpperCase() === 'RESOLVED_WITHIN_SLA';
}

export function slaComplianceRate(metrics) {
  const list = metrics || [];
  const closed = list.filter(isClosedForCompliance);
  if (closed.length === 0) return null;
  const within = closed.filter(isResolvedWithinSla).length;
  return (within / closed.length) * 100;
}

export function classifyComplaintFilter(item) {
  if (!item) return false;
  const cls = String(item.classification || '').toUpperCase();
  const status = String(item.status || '').toUpperCase();
  if (cls === 'OTHER') return false;
  if (cls === 'INTAKE') {
    const cId = String(item.dbcTicketId || item.complaintId || item.generalTicketId || '').trim();
    return cId.startsWith('DBC-') || cId.startsWith('FCR-');
  }
  if (status === 'OTHER' && cls !== 'COMPLAINT' && cls !== 'DECLINED') return false;
  const cId = String(item.dbcTicketId || item.complaintId || item.generalTicketId || '').trim();
  const isDeclined = status === 'DECLINED' || cls === 'DECLINED';
  const fcr = item.fcrStatus === true || item.fcrStatus === 'VERIFIED';
  return cId.startsWith('DBC-') || cId.startsWith('FCR-') || isDeclined || cls === 'COMPLAINT' || fcr;
}
