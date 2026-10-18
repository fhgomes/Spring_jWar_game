import type { TFunction } from 'i18next';

/**
 * Map Firebase Auth error codes to PT-BR copy keys (FR-024 of spec 008).
 */
export function mapFirebaseAuthError(code: string | undefined, t: TFunction): string {
  switch (code) {
    case 'auth/email-already-in-use':
      return t('auth.errors.email_in_use');
    case 'auth/invalid-email':
      return t('auth.errors.invalid_email');
    case 'auth/weak-password':
      return t('auth.errors.weak_password');
    case 'auth/wrong-password':
    case 'auth/user-not-found':
    case 'auth/invalid-credential':
      return t('auth.errors.wrong_credentials');
    case 'auth/too-many-requests':
      return t('auth.errors.too_many_requests');
    case 'auth/network-request-failed':
      return t('auth.errors.network');
    default:
      return t('auth.errors.unknown');
  }
}
