import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Eye, EyeOff } from 'lucide-react';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { apiClient, isApiError } from '@/lib/api';
import { useToastStore } from '@/stores/useToastStore';
import type { CreateRoomRequest, RoomDetail } from '@/types/api';

const schema = z.object({
  name: z.string().min(3).max(40),
  maxPlayers: z.coerce.number().int().min(3).max(6),
  password: z
    .string()
    .optional()
    .refine((v) => !v || (v.length >= 4 && v.length <= 32), { message: 'length' }),
});

type CreateRoomValues = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onClose(): void;
}

export function CreateRoomModal({ open, onClose }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToastStore((s) => s.push);
  const [showPassword, setShowPassword] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateRoomValues>({
    defaultValues: { name: '', maxPlayers: 6, password: '' },
  });

  const createMutation = useMutation({
    mutationFn: async (input: CreateRoomRequest) => {
      const { data } = await apiClient.post<RoomDetail>('/rooms', input);
      return data;
    },
  });

  const onSubmit = async (values: CreateRoomValues) => {
    const parsed = schema.safeParse(values);
    if (!parsed.success) return;
    try {
      const created = await createMutation.mutateAsync({
        name: parsed.data.name,
        maxPlayers: parsed.data.maxPlayers,
        password: parsed.data.password || undefined,
      });
      queryClient.invalidateQueries({ queryKey: ['rooms', 'open'] });
      reset();
      onClose();
      navigate(`/rooms/${created.id}`);
    } catch (err) {
      if (isApiError(err)) {
        toast({ variant: 'error', title: t('create_room.errors.generic') });
      }
    }
  };

  return (
    <Modal open={open} onClose={onClose} title={t('create_room.title')} size="md">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <Label htmlFor="room-name" required>
            {t('create_room.name_label')}
          </Label>
          <Input
            id="room-name"
            placeholder={t('create_room.name_placeholder') ?? ''}
            error={!!errors.name}
            aria-describedby={errors.name ? 'room-name-err' : undefined}
            {...register('name')}
          />
          {errors.name && (
            <p id="room-name-err" className="mt-1 text-xs text-army-red">
              {t('create_room.errors.name_min')}
            </p>
          )}
        </div>

        <div>
          <Label htmlFor="room-max">{t('create_room.max_players_label')}</Label>
          <select
            id="room-max"
            className="block w-full rounded-md border border-table-600 bg-table-800 px-3 py-2 text-army-white focus:border-army-red focus:outline-none focus-visible:ring-2 focus-visible:ring-army-red"
            {...register('maxPlayers')}
          >
            {[3, 4, 5, 6].map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </div>

        <div>
          <Label htmlFor="room-password">{t('create_room.password_label')}</Label>
          <div className="relative">
            <Input
              id="room-password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="off"
              placeholder={t('create_room.password_placeholder') ?? ''}
              error={!!errors.password}
              {...register('password')}
            />
            <button
              type="button"
              onClick={() => setShowPassword((v) => !v)}
              className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-table-300 hover:bg-table-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-army-red"
              aria-label={
                showPassword
                  ? t('create_room.hide_password') ?? 'Esconder'
                  : t('create_room.show_password') ?? 'Mostrar'
              }
            >
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
          {errors.password && (
            <p className="mt-1 text-xs text-army-red">
              {t('create_room.errors.password_length')}
            </p>
          )}
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="ghost" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button type="submit" loading={isSubmitting}>
            {isSubmitting ? t('create_room.creating') : t('create_room.submit')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
