import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Container } from '@/components/ui/Container';
import { Card, CardBody, CardFooter, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Modal } from '@/components/ui/Modal';
import { apiClient } from '@/lib/api';
import { useAuthStore } from '@/stores/useAuthStore';
import { useToastStore } from '@/stores/useToastStore';
import { useCurrentUser } from '@/hooks/useCurrentUser';
import { formatDate } from '@/lib/utils';
import type { UpdateUserRequest, UserResponse } from '@/types/api';

interface SettingsForm {
  displayName: string;
  photoUrl: string;
}

export default function AccountSettingsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToastStore((s) => s.push);
  const setCurrentUser = useAuthStore((s) => s.setCurrentUser);
  const firebaseUser = useAuthStore((s) => s.firebaseUser);
  const doSignOut = useAuthStore((s) => s.signOut);

  const { data: profile } = useCurrentUser();

  const {
    register,
    handleSubmit,
    reset,
    formState: { isDirty, isSubmitting },
  } = useForm<SettingsForm>({
    defaultValues: { displayName: '', photoUrl: '' },
  });

  useEffect(() => {
    if (profile) {
      reset({ displayName: profile.displayName, photoUrl: profile.photoUrl ?? '' });
    }
  }, [profile, reset]);

  const save = useMutation({
    mutationFn: async (input: UpdateUserRequest) => {
      const { data } = await apiClient.patch<UserResponse>('/me', input);
      return data;
    },
    onSuccess: (updated) => {
      setCurrentUser(updated);
      queryClient.setQueryData(['me'], updated);
      toast({ variant: 'success', title: t('account.save_success') });
    },
  });

  const onSubmit = async (values: SettingsForm) => {
    await save.mutateAsync({
      displayName: values.displayName,
      photoUrl: values.photoUrl || null,
    });
    reset(values);
  };

  // Delete account flow
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [confirmEmail, setConfirmEmail] = useState('');
  const deleteMatchesEmail = confirmEmail.trim().toLowerCase() === profile?.email.toLowerCase();

  const deleteMutation = useMutation({
    mutationFn: async () => {
      await apiClient.delete('/me');
      if (firebaseUser) {
        try {
          await firebaseUser.delete();
        } catch {
          // silent: backend deletion succeeded, Firebase may need re-auth
        }
      }
      await doSignOut();
    },
    onSuccess: () => {
      queryClient.clear();
      navigate('/goodbye');
    },
  });

  return (
    <Container size="md" className="py-8">
      <Card>
        <CardHeader>
          <h1 className="font-serif text-2xl text-army-white">{t('account.title')}</h1>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardBody className="space-y-4">
            <div>
              <Label htmlFor="account-display-name">{t('account.display_name')}</Label>
              <Input
                id="account-display-name"
                {...register('displayName', { required: true, minLength: 2, maxLength: 40 })}
              />
            </div>
            <div>
              <Label htmlFor="account-photo">{t('account.photo_url')}</Label>
              <Input
                id="account-photo"
                type="url"
                placeholder="https://..."
                {...register('photoUrl')}
              />
            </div>

            <dl className="grid grid-cols-2 gap-4 rounded-md border border-table-700 bg-table-900 p-3 text-sm">
              <div>
                <dt className="text-table-300">{t('account.email')}</dt>
                <dd className="text-army-white">{profile?.email}</dd>
              </div>
              <div>
                <dt className="text-table-300">{t('account.email_verified')}</dt>
                <dd
                  className={
                    firebaseUser?.emailVerified ? 'text-green-400' : 'text-yellow-400'
                  }
                >
                  {firebaseUser?.emailVerified
                    ? t('account.email_verified')
                    : t('account.email_unverified')}
                </dd>
              </div>
              <div>
                <dt className="text-table-300">{t('account.created_at')}</dt>
                <dd className="text-army-white">
                  {profile?.createdAt ? formatDate(profile.createdAt, { dateStyle: 'long' }) : '—'}
                </dd>
              </div>
            </dl>
          </CardBody>

          <CardFooter className="flex items-center justify-between">
            <Button type="submit" disabled={!isDirty} loading={isSubmitting || save.isPending}>
              {t('common.save')}
            </Button>
            <Button
              type="button"
              variant="danger"
              onClick={() => setDeleteOpen(true)}
            >
              {t('account.delete_account')}
            </Button>
          </CardFooter>
        </form>
      </Card>

      <Modal
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        title={t('account.delete_account_title')}
        description={t('account.delete_account_description')}
        size="md"
      >
        <div className="space-y-4">
          <Input
            type="email"
            placeholder={t('account.delete_account_placeholder') ?? ''}
            value={confirmEmail}
            onChange={(e) => setConfirmEmail(e.target.value)}
            aria-label={t('account.delete_account_placeholder') ?? ''}
          />
          {!deleteMatchesEmail && (
            <p className="text-xs text-table-300">{t('account.delete_account_helper')}</p>
          )}
          <div className="flex justify-end gap-2">
            <Button variant="ghost" onClick={() => setDeleteOpen(false)}>
              {t('common.cancel')}
            </Button>
            <Button
              variant="danger"
              disabled={!deleteMatchesEmail}
              loading={deleteMutation.isPending}
              onClick={() => deleteMutation.mutate()}
            >
              {t('account.delete_account_confirm')}
            </Button>
          </div>
        </div>
      </Modal>
    </Container>
  );
}
