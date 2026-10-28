export function sanitizePublicPhone(formPhone, countryCode) {
  let rawPhone = String(formPhone || '').trim();
  if (rawPhone.startsWith('0')) {
    rawPhone = rawPhone.substring(1);
  }
  if (rawPhone.startsWith(countryCode)) {
    rawPhone = rawPhone.substring(countryCode.length);
  } else if (rawPhone.startsWith('+')) {
    rawPhone = rawPhone.replace(/^\+\d+/, '');
  }
  return countryCode + rawPhone;
}

export function getCustomerNameError(name, language) {
  if (!name?.trim()) {
    return language === 'english' ? 'Customer name is required' : 'የደንበኛው ሙሉ ስም ግዴታ ነው';
  }
  const trimmed = name.trim();
  if (trimmed.length < 2 || trimmed.length > 100) {
    return language === 'english' ? 'Customer name must be between 2 and 100 characters' : 'ስም ከ2 እስከ 100 ፊደላት መሆን አለበት';
  }
  if (!/^[a-zA-Z\u00C0-\u024F\u1200-\u137F\s'-]+$/.test(trimmed)) {
    return language === 'english' ? 'Customer name contains invalid characters' : 'ስም የተከለከሉ ፊደላትን ይዟል';
  }
  return '';
}

export function getDescriptionError(desc, language) {
  if (!desc?.trim()) {
    return language === 'english' ? 'Complaint description is required' : 'የቅሬታው ዝርዝር መግለጫ ግዴታ ነው';
  }
  const trimmed = desc.trim();
  if (trimmed.length < 10) {
    return language === 'english' ? 'Complaint description must be at least 10 characters long' : 'የቅሬታ መግለጫ ቢያንስ 10 ፊደላት መሆን አለበት';
  }
  if (trimmed.length > 5000) {
    return language === 'english' ? 'Complaint description cannot exceed 5000 characters' : 'የቅሬታ መግለጫ ከ5000 ፊደላት መብለጥ የለበትም';
  }
  if (/<\s*script[\s/>]|on\w+\s*=|javascript:/i.test(trimmed)) {
    return language === 'english' ? 'Description contains invalid script or HTML tags' : 'መግለጫው የተከለከሉ ኮዶችን ይዟል';
  }
  return '';
}

export function getAccountNumberError(accountNumber, language) {
  if (!accountNumber?.trim()) {
    return language === 'english' ? 'Account Number is mandatory' : 'የአካውንት ቁጥር ግዴታ ነው';
  }
  if (accountNumber.length !== 13 || !/^\d+$/.test(accountNumber)) {
    return language === 'english' ? 'Account Number must be 13 digits' : 'አካውንት ቁጥር 13 አሃዝ መሆን አለበት';
  }
  return '';
}

export function lettersOnlyPlaceName(value) {
  return String(value || '').replace(/[^\p{L} ]/gu, '').slice(0, 100);
}

export function getPlaceNameError(value, language, label) {
  const trimmed = String(value || '').trim();
  if (!trimmed) return '';
  if (!/^[\p{L} ]{1,100}$/u.test(trimmed)) {
    return language === 'english'
      ? `${label} may contain letters and spaces only`
      : `${label} ፊደላት እና ክፍተት ብቻ መያዝ አለበት`;
  }
  return '';
}

export function getEmailError(email, language) {
  if (!email?.trim()) return '';
  const trimmed = email.trim();
  const at = trimmed.indexOf('@');
  const domain = at > 0 ? trimmed.slice(at + 1) : '';
  const dot = domain.lastIndexOf('.');
  const looksValid = at > 0 && dot > 0 && dot < domain.length - 1 && !/\s/.test(trimmed);
  if (!looksValid) {
    return language === 'english' ? 'Please enter a valid email address' : 'እባክዎን ትክክለኛ የኢሜይል አድራሻ ያስገቡ';
  }
  return '';
}

export const PUBLIC_COUNTRY_PHONES = [
  { code: '+251', flag: '🇪🇹', name: 'Ethiopia', minDigits: 9, maxDigits: 10, national: /^(0?[79]\d{8})$/ },
  { code: '+1', flag: '🇺🇸', name: 'USA', minDigits: 10, maxDigits: 10, national: /^\d{10}$/ },
  { code: '+44', flag: '🇬🇧', name: 'UK', minDigits: 10, maxDigits: 11, national: /^(0\d{10}|\d{10})$/ },
  { code: '+254', flag: '🇰🇪', name: 'Kenya', minDigits: 9, maxDigits: 10, national: /^(0\d{9}|\d{9})$/ },
  { code: '+253', flag: '🇩🇯', name: 'Djibouti', minDigits: 8, maxDigits: 8, national: /^\d{8}$/ },
  { code: '+971', flag: '🇦🇪', name: 'UAE', minDigits: 8, maxDigits: 10, national: /^(0\d{8,9}|\d{8,9})$/ },
  { code: '+966', flag: '🇸🇦', name: 'Saudi Arabia', minDigits: 9, maxDigits: 10, national: /^(0\d{9}|\d{9})$/ }
];

export function publicPhoneRule(countryCode) {
  return PUBLIC_COUNTRY_PHONES.find((country) => country.code === countryCode) || PUBLIC_COUNTRY_PHONES[0];
}

function phoneLengthMessage(rule, language) {
  const count = rule.minDigits === rule.maxDigits
    ? String(rule.maxDigits)
    : `${rule.minDigits}-${rule.maxDigits}`;
  return language === 'english'
    ? `Enter ${count} digits for ${rule.name}`
    : `ለ${rule.name} ${count} አሃዝ ያስገቡ`;
}

export function getPhoneError(phone, countryCode, language) {
  if (!phone?.trim()) {
    return language === 'english' ? 'Phone number is required' : 'የስልክ ቁጥር ግዴታ ነው';
  }
  const digits = phone.trim().replace(/\D/g, '');
  const rule = publicPhoneRule(countryCode);
  if (digits.length < rule.minDigits || digits.length > rule.maxDigits) {
    return phoneLengthMessage(rule, language);
  }
  if (!rule.national.test(digits)) {
    return language === 'english'
      ? `Invalid ${rule.name} phone number`
      : `ትክክለኛ የ${rule.name} ስልክ ቁጥር ያስገቡ`;
  }
  return '';
}

/** Staff registration accepts 09..., 9... and +251... and checks them as an Ethiopian number. */
export function getStaffPhoneError(phone) {
  if (!phone?.trim()) {
    return 'Phone number is required';
  }
  let digits = phone.trim().replace(/\D/g, '');
  if (digits.startsWith('251')) {
    digits = digits.slice(3);
  }
  return getPhoneError(digits, '+251', 'english');
}

export function fieldRule(check) {
  return {
    validator: (_, value) => {
      const message = check(value);
      return message ? Promise.reject(new Error(message)) : Promise.resolve();
    }
  };
}
