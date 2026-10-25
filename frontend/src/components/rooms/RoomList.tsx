import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { Castle } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { RoomCard } from './RoomCard';
import { apiClient, isApiError } from '@/lib/api';
import { useToastStore } from '@/stores/useToastStore';
import type { RoomSummary } from '@/types/api';

interface Props {
  rooms: RoomSummary[];
  onCreate(): void;
  isLoading?: boolean;
}

export function RoomList({ rooms, onCreate, isLoading }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const toast = useToastStore((s) => s.push);
  const [joiningId, setJoiningId] = useState<string | null>(null);

  const joinMutation = useMutation({
    mutationFn: async ({ id, password }: { id: string; password?: string }) => {
      await apiClient.post(`/rooms/${id}/join`, { password });
    },
  });

  const handleJoin = async (room: RoomSummary) => {
    setJoiningId(room.id);
    try {
      if (room.hasPassword) {
        const password = window.prompt(t('lobby.password_required') ?? '');
        if (!password) {
          setJoiningId(null);
          return;
        }
        await joinMutation.mutateAsync({ id: room.id, password });
      } else {
        await joinMutation.mutateAsync({ id: room.id });
      }
      navigate(`/rooms/${room.id}`);
    } catch (err) {
      if (isApiError(err)) {
        if (err.httpStatus === 409) {
          toast({ variant: 'warning', title: t('lobby.room_full_toast') });
        } else if (err.httpStatus === 401) {
          toast({ variant: 'error', title: t('lobby.password_wrong') });
        } else {
          toast({ variant: 'error', title: err.message });
        }
      }
    } finally {
      setJoiningId(null);
    }
  };

  if (isLoading) {
    return (
      <div className="grid place-items-center py-20 text-table-200">
        <p>{t('lobby.loading')}</p>
      </div>
    );
  }

  if (rooms.length === 0) {
    return (
      <div className="grid place-items-center gap-4 py-20 text-center">
        <Castle className="h-16 w-16 text-table-500" aria-hidden="true" />
        <p className="text-lg text-table-100">{t('lobby.empty_title')}</p>
        <Button onClick={onCreate} size="lg">
          {t('lobby.create_room')}
        </Button>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
      {rooms.map((room) => (
        <RoomCard
          key={room.id}
          room={room}
          onJoin={handleJoin}
          joining={joiningId === room.id}
        />
      ))}
    </div>
  );
}
