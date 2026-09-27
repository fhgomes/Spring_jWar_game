import { useState, type FormEvent } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useTranslation } from 'react-i18next';
import {
  signInWithEmailAndPassword,
  sendPasswordResetEmail,
  type AuthError,
} from 'firebase/auth';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { auth } from '@/lib/firebase';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Modal } from '@/components/ui/Modal';
import { GoogleSignInButton } from './GoogleSignInButton';
import { mapFirebaseAuthError } from './errorMessages';
import { useToastStore } from '@/stores/useToastStore';

const schema = z.object({
  email: z.string().min(1).email(),
  password: z.string().min(1),
});

type LoginValues = z.infer<typeof schema>;

export function LoginForm() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [search] = useSearchParams();
  const next = search.get('next') ?? '/lobby';
  const toast = useToastStore((s) => s.push);
  const [forgotOpen, setForgotOpen] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    watch,
  } = useForm<LoginValues>({
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = async (values: LoginValues) => {
    const parsed = schema.safeParse(values);
    if (!parsed.success) return;

    try {
      await signInWithEmailAndPassword(auth, parsed.data.email, parsed.data.password);
      navigate(next, { replace: true });
    } catch (err) {
      const e = err as AuthError;
      const msg = mapFirebaseAuthError(e.code, t);
      toast({ variant: 'error', title: msg });
    }
  };

  return (
    <>
      <form noValidate onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <GoogleSignInButton />

        <div className="relative my-4 flex items-center">
          <div className="flex-grow border-t border-table-700" />
          <span className="mx-4 text-xs uppercase text-table-300">{t('auth.or')}</span>
          <div className="flex-grow border-t border-table-700" />
        </div>

        <div>
          <Label htmlFor="email" required>
            {t('auth.email')}
          </Label>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            aria-describedby={errors.email ? 'email-err' : undefined}
            error={!!errors.email}
            {...register('email', { required: true })}
          />
          {errors.email && (
            <p id="email-err" className="mt-1 text-xs text-army-red">
              {t('auth.validation.email_required')}
            </p>
          )}
        </div>

        <div>
          <Label htmlFor="password" required>
            {t('auth.password')}
          </Label>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            error={!!errors.password}
            {...register('password', { required: true })}
          />
        </div>

        <div className="flex items-center justify-between text-sm">
          <button
            type="button"
            onClick={() => setForgotOpen(true)}
            className="text-army-red hover:underline focus:outline-none focus-visible:underline"
          >
            {t('auth.forgot_password')}
          </button>
          <Link to="/signup" className="text-table-200 hover:text-army-white hover:underline">
            {t('auth.no_account')}
          </Link>
        </div>

        <Button type="submit" loading={isSubmitting} fullWidth size="lg">
          {isSubmitting ? t('auth.signing_in') : t('auth.sign_in')}
        </Button>
      </form>

      <ForgotPasswordModal
        open={forgotOpen}
        onClose={() => setForgotOpen(false)}
        defaultEmail={watch('email')}
      />
    </>
  );
}

interface ForgotProps {
  open: boolean;
  onClose: () => void;
  defaultEmail: string;
}

function ForgotPasswordModal({ open, onClose, defaultEmail }: ForgotProps) {
  const { t } = useTranslation();
  const [email, setEmail] = useState(defaultEmail);
  const [loading, setLoading] = useState(false);
  const toast = useToastStore((s) => s.push);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await sendPasswordResetEmail(auth, email);
      toast({ variant: 'success', title: t('auth.reset_password_success') });
      onClose();
    } catch (err) {
      const code = (err as AuthError)?.code;
      toast({ variant: 'error', title: mapFirebaseAuthError(code, t) });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={t('auth.reset_password_title')}
      description={t('auth.reset_password_description')}
      size="sm"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <Label htmlFor="reset-email">{t('auth.email')}</Label>
          <Input
            id="reset-email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button type="submit" loading={loading}>
            {t('auth.reset_password_submit')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
