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
  const allowed = item.totalAllowedMinutes;
  const elapsed = Number(item.totalElapsedMinutes || 0);
  if (allowed != null && elapsed > Number(allowed)) {
    return true;
  }
  const st = (item.slaStatus || '').toUpperCase();
  return st === 'RESOLVED_AFTER_SLA';
}

export function isCurrentStageSlaBreached(item) {
  const st = String(item?.ledgerStageStatus || item?.currentStageSlaStatus || '').toUpperCase();
  return st === 'BREACHED' || st === 'OVERDUE';
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
  const cls = String(item.classification || item.complaintClassification || '').toUpperCase();
  const status = String(item.status || item.caseStatus || item.overallStatus || '').toUpperCase();

  // Exclude "OTHER" inquiries (these belong to the Other tab)
  if (cls === 'OTHER' || status === 'OTHER') return false;

  // Always include DECLINED complaints
  if (status === 'DECLINED' || cls === 'DECLINED') return true;

  // Exclude raw unclassified INTAKE drafts starting with CM- without a DBC- ticket ID
  const dbcTicket = String(item.dbcTicketId || item.variables?.dbcTicketId || '');
  const compId = String(item.complaintId || item.uniqueIdNo || '');
  const hasDbcTicket = dbcTicket.startsWith('DBC-') || compId.startsWith('DBC-');
  const isFcr = item.fcrStatus === true || item.fcrStatus === 1 || String(item.fcrStatus).toUpperCase() === 'TRUE' || status === 'FCR_RESOLVED' || status === 'RESOLVED';

  if (cls === 'INTAKE' && !hasDbcTicket && !isFcr) {
    return false;
  }

  if (compId.startsWith('CM-') && !hasDbcTicket && !isFcr) {
    return false;
  }

  return true;
}
