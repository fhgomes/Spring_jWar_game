import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { LogOut, PlayCircle } from 'lucide-react';
import { useRoom } from '@/hooks/useRoom';
import { useAuthStore } from '@/stores/useAuthStore';
import { useToastStore } from '@/stores/useToastStore';
import { apiClient, isApiError } from '@/lib/api';
import { Container } from '@/components/ui/Container';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Spinner } from '@/components/ui/Spinner';
import { ColorPicker } from '@/components/rooms/ColorPicker';
import { RoomMemberList } from '@/components/rooms/RoomMemberList';
import { RoomChat } from '@/components/rooms/RoomChat';
import type { ArmyColor } from '@/types/api';

export default function RoomPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { roomId } = useParams<{ roomId: string }>();
  const toast = useToastStore((s) => s.push);
  const currentUser = useAuthStore((s) => s.currentUser);

  const { data: room, isLoading, isError, error } = useRoom(roomId);
  const [leaveOpen, setLeaveOpen] = useState(false);

  const meMember = useMemo(
    () => room?.members.find((m) => m.userId === currentUser?.id),
    [room, currentUser],
  );
  const isHost = !!meMember?.isHost || room?.hostUserId === currentUser?.id;
  const takenColors = useMemo(
    () =>
      (room?.members.flatMap((m) => (m.color ? [m.color] : [])) ?? []) as ArmyColor[],
    [room],
  );

  const colorMutation = useMutation({
    mutationFn: async (color: ArmyColor) => {
      await apiClient.patch(`/rooms/${roomId}/members/me`, { color });
    },
    onError: (err) => {
      if (isApiError(err) && err.httpStatus === 409) {
        toast({ variant: 'warning', title: t('room.color_taken_toast') });
      }
    },
  });

  const startMutation = useMutation({
    mutationFn: async () => {
      await apiClient.post(`/rooms/${roomId}/start`);
    },
  });

  const leaveMutation = useMutation({
    mutationFn: async () => {
      await apiClient.post(`/rooms/${roomId}/leave`);
    },
    onSuccess: () => navigate('/lobby'),
  });

  if (isLoading) {
    return (
      <div className="grid place-items-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isError || !room) {
    const httpStatus = isApiError(error) ? error.httpStatus : 0;
    const message =
      httpStatus === 403 ? t('room.no_access') : t('room.not_found_title');
    return (
      <Container className="py-20 text-center">
        <p className="mb-4 text-lg text-table-100">{message}</p>
        <Button onClick={() => navigate('/lobby')}>{t('room.not_found_back')}</Button>
      </Container>
    );
  }

  const distinctColored = new Set(
    room.members.flatMap((m) => (m.color ? [m.color] : [])),
  );
  const enoughPlayers = room.members.filter((m) => m.color).length >= 3;
  const noDupeColors = distinctColored.size === room.members.filter((m) => m.color).length;
  const canStart = enoughPlayers && noDupeColors;

  const startTooltip = !enoughPlayers
    ? t('room.start_match_disabled_min')
    : !noDupeColors
      ? t('room.start_match_disabled_dupe')
      : undefined;

  return (
    <Container size="full" className="py-4">
      <div className="grid gap-4 lg:grid-cols-3">
        <section className="lg:col-span-2">
          <Card>
            <CardHeader>
              <h1 className="font-serif text-2xl text-army-white">{room.name}</h1>
              <p className="mt-0.5 text-sm text-table-200">
                {t('lobby.players_count', {
                  count: room.members.length,
                  max: room.maxPlayers,
                })}
              </p>
            </CardHeader>

            <CardBody className="space-y-6">
              <div>
                <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-table-200">
                  {t('room.members')}
                </h2>
                <RoomMemberList
                  members={room.members}
                  currentUserId={currentUser?.id}
                  hostUserId={room.hostUserId}
                />
              </div>

              {meMember && (
                <div>
                  <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-table-200">
                    {t('room.color_picker_title')}
                  </h2>
                  <ColorPicker
                    selectedColor={meMember.color ?? null}
                    takenColors={takenColors}
                    onSelect={(c) => colorMutation.mutate(c)}
                    disabled={colorMutation.isPending}
                  />
                </div>
              )}

              <footer className="flex flex-wrap items-center gap-3 border-t border-table-700 pt-4">
                {isHost ? (
                  <Button
                    onClick={() => startMutation.mutate()}
                    disabled={!canStart}
                    loading={startMutation.isPending}
                    title={startTooltip}
                    leftIcon={<PlayCircle className="h-4 w-4" />}
                  >
                    {t('room.start_match')}
                  </Button>
                ) : (
                  <p className="text-sm text-table-200">{t('room.waiting_host')}</p>
                )}
                <Button
                  variant="danger"
                  onClick={() => setLeaveOpen(true)}
                  leftIcon={<LogOut className="h-4 w-4" />}
                >
                  {t('room.leave_room')}
                </Button>
              </footer>
            </CardBody>
          </Card>
        </section>

        <aside className="lg:col-span-1">
          <Card className="h-[70vh] overflow-hidden">
            {roomId && <RoomChat roomId={roomId} />}
          </Card>
        </aside>
      </div>

      <Modal
        open={leaveOpen}
        onClose={() => setLeaveOpen(false)}
        title={t('room.leave_confirm_title')}
        description={t('room.leave_confirm_message')}
        size="sm"
      >
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={() => setLeaveOpen(false)}>
            {t('common.cancel')}
          </Button>
          <Button
            variant="danger"
            onClick={() => leaveMutation.mutate()}
            loading={leaveMutation.isPending}
          >
            {t('room.leave_confirm_action')}
          </Button>
        </div>
      </Modal>
    </Container>
  );
}
