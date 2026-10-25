import { useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { apiClient } from '@/lib/api';
import { createStompClient } from '@/lib/stomp';
import { useAuthStore } from '@/stores/useAuthStore';
import { useToastStore } from '@/stores/useToastStore';
import type { GameEvent, RoomDetail, RoomMessage } from '@/types/api';

/**
 * Fetches /api/rooms/{id}, subscribes to /topic/rooms/{id}, and handles
 * room-level events (member joined/left, color changed, host changed,
 * room message, match started).
 */
export function useRoom(roomId: string | undefined) {
  const queryClient = useQueryClient();
  const getIdToken = useAuthStore((s) => s.getIdToken);
  const toast = useToastStore((s) => s.push);
  const navigate = useNavigate();
  const { t } = useTranslation();

  const query = useQuery<RoomDetail>({
    queryKey: ['room', roomId],
    queryFn: async () => {
      const { data } = await apiClient.get<RoomDetail>(`/rooms/${roomId}`);
      return data;
    },
    enabled: !!roomId,
    refetchInterval: 3000,
    staleTime: 1000,
  });

  useEffect(() => {
    if (!roomId) return;

    const client = createStompClient({ getToken: getIdToken });

    client.onConnect = () => {
      client.subscribe(`/topic/rooms/${roomId}`, (msg) => {
        try {
          const event = JSON.parse(msg.body) as GameEvent<unknown>;
          handleRoomEvent(event);
        } catch (err) {
          // eslint-disable-next-line no-console
          console.warn('[stomp] failed to parse room event', err);
        }
      });
    };

    function handleRoomEvent(event: GameEvent<unknown>) {
      switch (event.type) {
        case 'ROOM_MEMBER_JOINED':
        case 'ROOM_MEMBER_LEFT':
        case 'ROOM_MEMBER_COLOR_CHANGED':
        case 'ROOM_HOST_CHANGED':
        case 'ROOM_UPDATED':
          queryClient.invalidateQueries({ queryKey: ['room', roomId] });
          if (event.type === 'ROOM_MEMBER_JOINED') {
            const p = event.payload as { displayName?: string } | undefined;
            if (p?.displayName) {
              toast({
                variant: 'info',
                title: t('room.member_joined_toast', { name: p.displayName }),
              });
            }
          }
          break;
        case 'ROOM_MESSAGE': {
          const msg = event.payload as RoomMessage;
          queryClient.setQueryData<RoomMessage[]>(['room-messages', roomId], (prev = []) => {
            // de-dupe by id (server echo) or clientTempId
            const filtered = prev.filter(
              (m) => m.id !== msg.id && (!msg.clientTempId || m.clientTempId !== msg.clientTempId),
            );
            return [...filtered, msg];
          });
          break;
        }
        case 'MATCH_STARTED': {
          const p = event.payload as { matchId: string };
          if (p?.matchId) navigate(`/matches/${p.matchId}`);
          break;
        }
        default:
          break;
      }
    }

    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [roomId, getIdToken, queryClient, toast, navigate, t]);

  return query;
}
