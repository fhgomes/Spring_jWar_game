import { useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { createStompClient } from '@/lib/stomp';
import { useAuthStore } from '@/stores/useAuthStore';
import type { RoomSummary } from '@/types/api';

/**
 * Polls /api/rooms?status=open and subscribes to /topic/lobby for invalidation.
 */
export function useRooms() {
  const queryClient = useQueryClient();
  const getIdToken = useAuthStore((s) => s.getIdToken);

  const query = useQuery<RoomSummary[]>({
    queryKey: ['rooms', 'open'],
    queryFn: async () => {
      const { data } = await apiClient.get<RoomSummary[]>('/rooms', {
        params: { status: 'open' },
      });
      return data;
    },
    refetchInterval: 5000,
    staleTime: 2000,
  });

  useEffect(() => {
    const client = createStompClient({ getToken: getIdToken });

    client.onConnect = () => {
      client.subscribe('/topic/lobby', () => {
        queryClient.invalidateQueries({ queryKey: ['rooms', 'open'] });
      });
    };

    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [getIdToken, queryClient]);

  return query;
}
