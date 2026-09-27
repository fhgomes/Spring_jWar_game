import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useTranslation } from 'react-i18next';
import {
  createUserWithEmailAndPassword,
  sendEmailVerification,
  updateProfile,
  type AuthError,
} from 'firebase/auth';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { auth } from '@/lib/firebase';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { GoogleSignInButton } from './GoogleSignInButton';
import { mapFirebaseAuthError } from './errorMessages';
import { useToastStore } from '@/stores/useToastStore';

interface FieldErrors {
  email?: string;
  displayName?: string;
  password?: string;
  passwordConfirm?: string;
}

const schema = z
  .object({
    displayName: z.string().min(2).max(40),
    email: z.string().email(),
    password: z
      .string()
      .min(8)
      .regex(/[A-Za-z]/)
      .regex(/[0-9]/),
    passwordConfirm: z.string(),
  })
  .refine((d) => d.password === d.passwordConfirm, {
    path: ['passwordConfirm'],
    message: 'mismatch',
  });

type SignupValues = z.infer<typeof schema>;

export function SignupForm() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [search] = useSearchParams();
  const next = search.get('next') ?? '/lobby';
  const toast = useToastStore((s) => s.push);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<SignupValues>({
    defaultValues: { displayName: '', email: '', password: '', passwordConfirm: '' },
  });

  const onSubmit = async (values: SignupValues) => {
    const parsed = schema.safeParse(values);
    if (!parsed.success) {
      const fieldErrs: FieldErrors = {};
      parsed.error.issues.forEach((issue) => {
        const field = issue.path[0] as keyof FieldErrors;
        if (!fieldErrs[field]) {
          fieldErrs[field] = issue.message;
        }
      });
      Object.entries(fieldErrs).forEach(([field, msg]) => {
        setError(field as keyof SignupValues, { message: msg });
      });
      return;
    }

    try {
      const cred = await createUserWithEmailAndPassword(
        auth,
        parsed.data.email,
        parsed.data.password,
      );
      await updateProfile(cred.user, { displayName: parsed.data.displayName });
      // sendEmailVerification is fire-and-forget — failures don't block sign-up.
      void sendEmailVerification(cred.user).catch(() => undefined);
      navigate(next, { replace: true });
    } catch (err) {
      const e = err as AuthError;
      if (e.code === 'auth/email-already-in-use') {
        setError('email', { message: t('auth.errors.email_in_use') });
      }
      toast({ variant: 'error', title: mapFirebaseAuthError(e.code, t) });
    }
  };

  return (
    <form noValidate onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <GoogleSignInButton />

      <div className="relative my-4 flex items-center">
        <div className="flex-grow border-t border-table-700" />
        <span className="mx-4 text-xs uppercase text-table-300">{t('auth.or')}</span>
        <div className="flex-grow border-t border-table-700" />
      </div>

      <div>
        <Label htmlFor="displayName" required>
          {t('auth.display_name')}
        </Label>
        <Input
          id="displayName"
          autoComplete="nickname"
          error={!!errors.displayName}
          aria-describedby={errors.displayName ? 'displayName-err' : undefined}
          {...register('displayName')}
        />
        {errors.displayName && (
          <p id="displayName-err" className="mt-1 text-xs text-army-red">
            {t('auth.validation.display_name_min')}
          </p>
        )}
      </div>

      <div>
        <Label htmlFor="email" required>
          {t('auth.email')}
        </Label>
        <Input
          id="email"
          type="email"
          autoComplete="email"
          error={!!errors.email}
          aria-describedby={errors.email ? 'email-err' : undefined}
          {...register('email')}
        />
        {errors.email && (
          <p id="email-err" className="mt-1 text-xs text-army-red">
            {errors.email.message ?? t('auth.validation.email_invalid')}
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
          autoComplete="new-password"
          error={!!errors.password}
          aria-describedby={errors.password ? 'password-err' : undefined}
          {...register('password')}
        />
        {errors.password && (
          <p id="password-err" className="mt-1 text-xs text-army-red">
            {t('auth.validation.password_complexity')}
          </p>
        )}
      </div>

      <div>
        <Label htmlFor="passwordConfirm" required>
          {t('auth.password_confirm')}
        </Label>
        <Input
          id="passwordConfirm"
          type="password"
          autoComplete="new-password"
          error={!!errors.passwordConfirm}
          aria-describedby={errors.passwordConfirm ? 'passwordConfirm-err' : undefined}
          {...register('passwordConfirm')}
        />
        {errors.passwordConfirm && (
          <p id="passwordConfirm-err" className="mt-1 text-xs text-army-red">
            {t('auth.validation.password_match')}
          </p>
        )}
      </div>

      <div className="flex items-center justify-end text-sm">
        <Link to="/login" className="text-table-200 hover:text-army-white hover:underline">
          {t('auth.has_account')}
        </Link>
      </div>

      <Button type="submit" loading={isSubmitting} fullWidth size="lg">
        {isSubmitting ? t('auth.creating_account') : t('auth.sign_up')}
      </Button>
    </form>
  );
}
