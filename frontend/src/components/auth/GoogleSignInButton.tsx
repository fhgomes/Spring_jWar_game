import { useState } from 'react';
import {
  signInWithPopup,
  signInWithRedirect,
  type AuthError,
} from 'firebase/auth';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/Button';
import { auth, googleProvider } from '@/lib/firebase';
import { useToastStore } from '@/stores/useToastStore';
import { mapFirebaseAuthError } from './errorMessages';

interface Props {
  onSuccess?: () => void;
  className?: string;
  fullWidth?: boolean;
}

export function GoogleSignInButton({ onSuccess, className, fullWidth = true }: Props) {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const toast = useToastStore((s) => s.push);

  const handleClick = async () => {
    setLoading(true);
    try {
      await signInWithPopup(auth, googleProvider);
      onSuccess?.();
    } catch (err) {
      const e = err as AuthError;
      if (e?.code === 'auth/popup-closed-by-user') {
        // intentional cancel — silent
      } else if (e?.code === 'auth/popup-blocked') {
        await signInWithRedirect(auth, googleProvider);
      } else {
        const msg = mapFirebaseAuthError(e?.code, t);
        toast({ variant: 'error', title: msg });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Button
      type="button"
      variant="secondary"
      onClick={handleClick}
      loading={loading}
      fullWidth={fullWidth}
      className={className}
      leftIcon={<GoogleGlyph />}
    >
      {t('auth.sign_in_with_google')}
    </Button>
  );
}

function GoogleGlyph() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
      <path
        fill="#4285F4"
        d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615z"
      />
      <path
        fill="#34A853"
        d="M9 18c2.43 0 4.467-.806 5.957-2.18l-2.908-2.26c-.806.54-1.838.86-3.05.86-2.342 0-4.326-1.582-5.034-3.71H.957v2.332A8.997 8.997 0 0 0 9 18z"
      />
      <path
        fill="#FBBC05"
        d="M3.966 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.284-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.01-2.332z"
      />
      <path
        fill="#EA4335"
        d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.464.891 11.427 0 9 0A8.997 8.997 0 0 0 .957 4.958L3.965 7.29C4.673 5.163 6.657 3.58 9 3.58z"
      />
    </svg>
  );
}
