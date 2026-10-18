import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { useAuthStore } from '@/stores/useAuthStore';
import type { UserResponse } from '@/types/api';

/**
 * Fetches the backend profile via GET /api/me as soon as Firebase is signed in.
 * Mirrors the result into useAuthStore.currentUser.
 */
export function useCurrentUser() {
  const status = useAuthStore((s) => s.status);
  const setCurrentUser = useAuthStore((s) => s.setCurrentUser);

  const query = useQuery<UserResponse>({
    queryKey: ['me'],
    queryFn: async () => {
      const { data } = await apiClient.get<UserResponse>('/me');
      return data;
    },
    enabled: status === 'signed-in',
    staleTime: 60_000,
    retry: 1,
  });

  useEffect(() => {
    if (query.data) {
      setCurrentUser(query.data);
    }
  }, [query.data, setCurrentUser]);

  return query;
}
