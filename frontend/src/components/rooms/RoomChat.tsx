import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowDown, Send, User } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { cn, formatRelative, uid } from '@/lib/utils';
import { apiClient } from '@/lib/api';
import { useAuthStore } from '@/stores/useAuthStore';
import type { RoomMessage } from '@/types/api';

interface Props {
  roomId: string;
}

export function RoomChat({ roomId }: Props) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const currentUser = useAuthStore((s) => s.currentUser);
  const scrollRef = useRef<HTMLDivElement>(null);
  const [text, setText] = useState('');
  const [showJumpToBottom, setShowJumpToBottom] = useState(false);

  const { data: messages = [] } = useQuery<RoomMessage[]>({
    queryKey: ['room-messages', roomId],
    queryFn: async () => {
      const { data } = await apiClient.get<RoomMessage[]>(`/rooms/${roomId}/messages`);
      return data;
    },
    staleTime: 1000,
  });

  const sendMutation = useMutation({
    mutationFn: async (input: { text: string; clientTempId: string }) => {
      const { data } = await apiClient.post<RoomMessage>(`/rooms/${roomId}/messages`, input);
      return data;
    },
    onMutate: (input) => {
      const optimistic: RoomMessage = {
        id: `local-${input.clientTempId}`,
        roomId,
        senderUserId: currentUser?.id ?? 'me',
        senderDisplayName: currentUser?.displayName ?? '',
        senderPhotoUrl: currentUser?.photoUrl,
        text: input.text,
        createdAt: new Date().toISOString(),
        clientTempId: input.clientTempId,
      };
      queryClient.setQueryData<RoomMessage[]>(['room-messages', roomId], (prev = []) => [
        ...prev,
        optimistic,
      ]);
    },
  });

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 80;
    if (nearBottom) {
      el.scrollTop = el.scrollHeight;
      setShowJumpToBottom(false);
    } else {
      setShowJumpToBottom(true);
    }
  }, [messages]);

  const send = () => {
    const trimmed = text.trim();
    if (!trimmed) return;
    sendMutation.mutate({ text: trimmed, clientTempId: uid() });
    setText('');
  };

  const onKey = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  return (
    <div className="flex h-full flex-col">
      <header className="border-b border-table-700 px-3 py-2 font-medium text-army-white">
        {t('chat.title')}
      </header>

      <div ref={scrollRef} className="relative flex-1 overflow-y-auto px-3 py-2">
        {messages.length === 0 ? (
          <p className="py-8 text-center text-sm text-table-300">{t('chat.empty')}</p>
        ) : (
          <ul className="space-y-3">
            {messages.map((m) => {
              const isMine = m.senderUserId === currentUser?.id;
              const pending = m.id.startsWith('local-');
              return (
                <li key={m.id} className={cn('flex gap-2', isMine && 'flex-row-reverse')}>
                  {m.senderPhotoUrl ? (
                    <img
                      src={m.senderPhotoUrl}
                      alt=""
                      className="h-7 w-7 shrink-0 rounded-full border border-table-600"
                    />
                  ) : (
                    <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-table-700">
                      <User className="h-3.5 w-3.5 text-table-200" aria-hidden="true" />
                    </span>
                  )}
                  <div className={cn('min-w-0 flex-1', isMine && 'text-right')}>
                    <p className="text-xs text-table-300">
                      <span className="font-medium text-army-white">{m.senderDisplayName}</span>{' '}
                      · {formatRelative(m.createdAt)}
                    </p>
                    <p
                      className={cn(
                        'mt-0.5 inline-block max-w-full break-words rounded-md px-3 py-1.5 text-sm',
                        isMine
                          ? 'bg-army-red/20 text-army-white'
                          : 'bg-table-700 text-army-white',
                        pending && 'opacity-60',
                      )}
                    >
                      {m.text}
                      {pending && (
                        <span className="ml-1 text-xs italic text-table-300">
                          ({t('common.sending')})
                        </span>
                      )}
                    </p>
                  </div>
                </li>
              );
            })}
          </ul>
        )}

        {showJumpToBottom && (
          <button
            type="button"
            onClick={() => {
              const el = scrollRef.current;
              if (el) el.scrollTop = el.scrollHeight;
            }}
            className="absolute bottom-2 right-3 flex items-center gap-1.5 rounded-full bg-army-red px-3 py-1.5 text-xs text-army-white shadow-strong hover:bg-red-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-army-white"
          >
            <ArrowDown className="h-3 w-3" /> {t('chat.new_messages')}
          </button>
        )}
      </div>

      <div className="border-t border-table-700 p-2">
        <div className="flex items-end gap-2">
          <textarea
            value={text}
            onChange={(e) => setText(e.target.value.slice(0, 500))}
            onKeyDown={onKey}
            rows={1}
            placeholder={t('chat.placeholder') ?? ''}
            aria-label={t('chat.placeholder') ?? ''}
            className="flex-1 resize-none rounded-md border border-table-600 bg-table-800 px-3 py-2 text-sm text-army-white focus:border-army-red focus:outline-none focus-visible:ring-2 focus-visible:ring-army-red"
          />
          <Button
            type="button"
            size="md"
            onClick={send}
            disabled={!text.trim()}
            aria-label={t('chat.send') ?? ''}
          >
            <Send className="h-4 w-4" />
          </Button>
        </div>
        <p className="mt-1 text-right text-xs text-table-300">
          {t('chat.char_counter', { count: text.length })}
        </p>
      </div>
    </div>
  );
}
