export function resolveCurrentContactPhone(customer, fallback) {
  if (!customer && !fallback) {
    return '';
  }
  return customer?.currentContactPhone
    || customer?.phone
    || customer?.preferredContactNumber
    || fallback
    || '';
}

export function resolveCoreBankingPhone(customer) {
  return customer?.coreBankingPhone || customer?.registeredPhone || '';
}
