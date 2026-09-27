import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { sendEmailVerification, type AuthError } from 'firebase/auth';
import { AlertTriangle, X } from 'lucide-react';
import { useAuthStore } from '@/stores/useAuthStore';
import { useToastStore } from '@/stores/useToastStore';
import { Button } from '@/components/ui/Button';
import { mapFirebaseAuthError } from './errorMessages';

export function VerifyEmailBanner() {
  const { t } = useTranslation();
  const firebaseUser = useAuthStore((s) => s.firebaseUser);
  const [dismissed, setDismissed] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [sending, setSending] = useState(false);
  const toast = useToastStore((s) => s.push);

  if (!firebaseUser || firebaseUser.emailVerified || dismissed) return null;

  const handleResend = async () => {
    if (!firebaseUser) return;
    setSending(true);
    try {
      await sendEmailVerification(firebaseUser);
      toast({ variant: 'success', title: t('auth.verify_email_sent') });
      setCooldown(60);
      const interval = setInterval(() => {
        setCooldown((c) => {
          if (c <= 1) {
            clearInterval(interval);
            return 0;
          }
          return c - 1;
        });
      }, 1000);
    } catch (err) {
      const e = err as AuthError;
      toast({ variant: 'error', title: mapFirebaseAuthError(e.code, t) });
    } finally {
      setSending(false);
    }
  };

  return (
    <div
      role="alert"
      className="flex items-center gap-3 bg-yellow-900/80 px-4 py-2 text-sm text-yellow-50"
    >
      <AlertTriangle className="h-4 w-4 shrink-0" aria-hidden="true" />
      <span className="flex-1">{t('auth.verify_email_banner')}</span>
      <Button
        size="sm"
        variant="ghost"
        onClick={handleResend}
        loading={sending}
        disabled={cooldown > 0}
        className="text-yellow-50 hover:bg-yellow-800/60"
      >
        {cooldown > 0
          ? t('auth.verify_email_countdown', { seconds: cooldown })
          : t('auth.verify_email_resend')}
      </Button>
      <button
        type="button"
        onClick={() => setDismissed(true)}
        aria-label={t('auth.verify_email_dismiss') ?? ''}
        className="rounded p-1 transition-colors hover:bg-yellow-800/60 focus:outline-none focus-visible:ring-2 focus-visible:ring-yellow-300"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}
