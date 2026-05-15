import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { createStompClient } from '@/lib/stomp';
import { useAuthStore } from '@/stores/useAuthStore';
import type {
  ActionLogEntry,
  GameEvent,
  GameStateSnapshot,
  MatchFinishedPayload,
} from '@/types/api';

/**
 * Subscribes to the match snapshot and applies STOMP patches.
 * Returns both the snapshot and a chronological action log (UI-only).
 */
export function useMatch(matchId: string | undefined) {
  const queryClient = useQueryClient();
  const getIdToken = useAuthStore((s) => s.getIdToken);
  const [actionLog, setActionLog] = useState<ActionLogEntry[]>([]);
  const [matchFinished, setMatchFinished] = useState<MatchFinishedPayload | null>(null);

  const query = useQuery<GameStateSnapshot>({
    queryKey: ['match', matchId],
    queryFn: async () => {
      const { data } = await apiClient.get<GameStateSnapshot>(`/matches/${matchId}/state`);
      return data;
    },
    enabled: !!matchId,
    staleTime: 1000,
  });

  useEffect(() => {
    if (!matchId) return;

    const client = createStompClient({ getToken: getIdToken });

    client.onConnect = () => {
      client.subscribe(`/topic/matches/${matchId}`, (msg) => {
        try {
          const event = JSON.parse(msg.body) as GameEvent<unknown>;
          handleMatchEvent(event);
        } catch (err) {
          // eslint-disable-next-line no-console
          console.warn('[stomp] failed to parse match event', err);
        }
      });
    };

    function handleMatchEvent(event: GameEvent<unknown>) {
      switch (event.type) {
        case 'STATE_PATCH':
        case 'TROOPS_ADDED':
        case 'TROOPS_MOVED':
        case 'ATTACK_RESULT':
        case 'PHASE_ENDED':
        case 'TURN_CHANGED':
          // FR-029 — on patch fail, fall back to a full refetch.
          queryClient.invalidateQueries({ queryKey: ['match', matchId] });
          appendLog({
            type: event.type,
            text: '',
            timestamp: event.timestamp,
          });
          break;
        case 'MATCH_FINISHED':
          setMatchFinished(event.payload as MatchFinishedPayload);
          queryClient.invalidateQueries({ queryKey: ['match', matchId] });
          break;
        default:
          break;
      }
    }

    function appendLog(entry: Omit<ActionLogEntry, 'id'>) {
      setActionLog((prev) => [
        ...prev,
        { ...entry, id: `${entry.type}-${entry.timestamp}-${prev.length}` },
      ]);
    }

    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [matchId, getIdToken, queryClient]);

  return { ...query, actionLog, matchFinished };
}
