export const ROLE_ROUTES = {
  // Shared Operational Dashboard Roles
  'ROLE_CONTACT_CENTER_AGENT': '/branch-staff',
  'ROLE_CONTACT_CENTER_TEAM_LEADER': '/branch-staff',
  'ROLE_CONTACT_CENTER_SENIOR_MANAGER': '/branch-staff',
  'ROLE_CUSTOMER_CARE_OFFICER': '/branch-staff',
  'ROLE_CUSTOMER_CARE_SENIOR_MANAGER': '/cmd',
  'ROLE_CUSTOMER_CARE_TEAM_LEADER': '/branch-staff',
  'ROLE_DIGITAL_MARKETING_OFFICER': '/branch-staff',
  'ROLE_DIGITAL_MARKETING_SENIOR_MANAGER': '/branch-staff',
  'ROLE_CUSTOMER_EXPERIENCE_PARTNERSHIP': '/branch-staff',
  'ROLE_CHIEF_EXPERIENCE_OFFICER': '/executive',
  'ROLE_BRANCH_MANAGER': '/branch-staff',
  'ROLE_CUSTOMER_SERVICE_MANAGER': '/branch-staff',
  'ROLE_SERVICE_QUALITY_DIRECTOR': '/cmd',

  // Audit Roles
  'ROLE_AUDIT_INVESTIGATION_TEAM': '/audit',
  'ROLE_OPERATIONAL_AUDIT_SENIOR_MANAGER': '/audit',
  'ROLE_OPERATIONAL_AUDIT_DIRECTOR': '/audit',

  // Committee Role
  'ROLE_COMMITTEE_SECRETARY': '/chief-committee',
  'ROLE_CHIEF_COMMITTEE': '/chief-committee',

  // Other Existing Roles
  'ROLE_DEPARTMENT_WORKUNIT': '/work-unit',
  'ROLE_ADMIN': '/admin'
};

export const getRouteForRole = (role) => {
  if (!role) return '/staff-login';
  const key = typeof role === 'string' ? role.toUpperCase() : '';
  if (key === 'ROLE_CONTACT_CENTER_MANAGER') {
    return ROLE_ROUTES.ROLE_CONTACT_CENTER_SENIOR_MANAGER;
  }
  if (key === 'ROLE_BRANCH_STAFF') {
    return ROLE_ROUTES.ROLE_CONTACT_CENTER_AGENT;
  }
  if (key === 'ROLE_SERVICE_QUALITY') {
    return ROLE_ROUTES.ROLE_CUSTOMER_CARE_OFFICER;
  }
  if (key === 'ROLE_CHIEF_COMMITTEE') {
    return ROLE_ROUTES.ROLE_COMMITTEE_SECRETARY;
  }
  return ROLE_ROUTES[role] || ROLE_ROUTES[key] || '/staff-login';
};

/**
 * Path access derived from ROLE_ROUTES landing pages and DashboardLayout menus.
 * Does not invent new workspace assignments.
 */
export function canAccessPath(pathname, role) {
  const r = (role || '').toUpperCase();
  if (!pathname) return false;

  if (pathname.startsWith('/admin')) {
    return r === 'ROLE_ADMIN';
  }
  if (pathname === '/audit') {
    return r.includes('AUDIT');
  }
  if (pathname === '/executive') {
    return r.includes('CHIEF_EXPERIENCE');
  }
  if (pathname === '/chief-committee') {
    return r.includes('COMMITTEE');
  }
  if (pathname === '/work-unit') {
    return r === 'ROLE_DEPARTMENT_WORKUNIT';
  }
  if (pathname === '/cmd') {
    return r === 'ROLE_CUSTOMER_CARE_OFFICER'
      || r === 'ROLE_CUSTOMER_CARE_TEAM_LEADER'
      || r.includes('CUSTOMER_CARE_OFFICER')
      || r === 'ROLE_CUSTOMER_CARE_SENIOR_MANAGER'
      || r === 'ROLE_SERVICE_QUALITY_DIRECTOR'
      || r.startsWith('ROLE_BRANCH_MANAGER')
      || r.startsWith('ROLE_CONTACT_CENTER')
      || r.startsWith('ROLE_DIGITAL_MARKETING')
      || r === 'ROLE_CUSTOMER_EXPERIENCE_PARTNERSHIP'
      || r.startsWith('ROLE_CUSTOMER_SERVICE')
      || r.startsWith('ROLE_CUSTOMER_CARE');
  }
  if (pathname === '/branch-staff') {
    if (r === 'ROLE_CONTACT_CENTER_MANAGER') {
      return true;
    }
    if (r === 'ROLE_SERVICE_QUALITY') {
      return true;
    }
    if (r === 'ROLE_CUSTOMER_CARE_SENIOR_MANAGER' || r === 'ROLE_SERVICE_QUALITY_DIRECTOR') {
      return true;
    }
    const landing = ROLE_ROUTES[r];
    return landing === '/branch-staff';
  }
  return false;
}
