export function customerHomeBranch(customer) {
  return customer?.customerHomeBranch || customer?.homeBranch || '';
}

export function customerHomeDistrict(customer) {
  return customer?.customerHomeDistrict || customer?.district || '';
}

export function complaintBranch(variables, complaint) {
  const vars = variables || {};
  const c = complaint || vars.complaint || {};
  return c.complaintBranch || vars.complaintBranch || c.branch || vars.branch || '';
}

export function complaintDistrict(variables, complaint) {
  const vars = variables || {};
  const c = complaint || vars.complaint || {};
  return c.complaintDistrict || vars.complaintDistrict || c.district || vars.district || '';
}
