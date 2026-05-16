import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus } from 'lucide-react';
import { Container } from '@/components/ui/Container';
import { Button } from '@/components/ui/Button';
import { RoomList } from '@/components/rooms/RoomList';
import { CreateRoomModal } from '@/components/rooms/CreateRoomModal';
import { useRooms } from '@/hooks/useRooms';
import { useCurrentUser } from '@/hooks/useCurrentUser';

export default function LobbyPage() {
  const { t } = useTranslation();
  // Bootstrap profile via /api/me (mirrors into auth store)
  useCurrentUser();
  const { data: rooms, isLoading, isError } = useRooms();
  const [createOpen, setCreateOpen] = useState(false);

  return (
    <Container className="py-6">
      <header className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="font-serif text-3xl text-army-white">{t('lobby.title')}</h1>
          <p className="mt-1 text-sm text-table-200">{t('lobby.open_rooms')}</p>
        </div>
        <Button onClick={() => setCreateOpen(true)} leftIcon={<Plus className="h-4 w-4" />}>
          {t('lobby.create_room')}
        </Button>
      </header>

      {isError ? (
        <div className="py-12 text-center text-table-200">{t('lobby.load_error')}</div>
      ) : (
        <RoomList
          rooms={rooms ?? []}
          isLoading={isLoading}
          onCreate={() => setCreateOpen(true)}
        />
      )}

      <CreateRoomModal open={createOpen} onClose={() => setCreateOpen(false)} />
    </Container>
  );
}
