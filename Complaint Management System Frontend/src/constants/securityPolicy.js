export const LOCKOUT_THRESHOLD = 6;
export const LOCKOUT_DURATION_MINUTES = 30;
export const SESSION_TIMEOUT_MINUTES = 15;
export const SESSION_TIMEOUT_MS = SESSION_TIMEOUT_MINUTES * 60 * 1000;
export const PASSWORD_MIN_LENGTH = 12;

export const PASSWORD_POLICY = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{12,}$/;

export const PASSWORD_REQUIREMENTS_MESSAGE =
  'Password must be at least 12 characters and contain uppercase, lowercase, and numeric characters.';

export const PASSWORD_HISTORY_MESSAGE = 'You cannot reuse any of your last 4 passwords.';

export const ACCOUNT_LOCKED_MESSAGE =
  'Your account has been locked due to multiple failed login attempts. Please try again after 30 minutes or contact the system administrator.';

export const PASSWORD_EXPIRED_MESSAGE =
  'Your password has expired. Please change your password to continue.';

export const SESSION_TIMEOUT_MESSAGE =
  'Your session has expired due to inactivity. Please login again.';

export const SESSION_MESSAGE_KEY = 'cms.sessionMessage';

export function passwordExpiryBannerText(daysRemaining) {
  if (daysRemaining <= 1) {
    return 'Your password will expire tomorrow.';
  }
  return `Your password will expire in ${daysRemaining} days.`;
}
