import { useTranslation } from 'react-i18next';
import { Crown, User } from 'lucide-react';
import { cn } from '@/lib/utils';
import { ARMY_COLOR_HEX } from '@/types/game';
import type { RoomMemberDto } from '@/types/api';

interface Props {
  members: RoomMemberDto[];
  currentUserId: string | undefined;
  hostUserId: string;
}

export function RoomMemberList({ members, currentUserId, hostUserId }: Props) {
  const { t } = useTranslation();

  return (
    <ul className="divide-y divide-table-700 rounded-md border border-table-700 bg-table-800">
      {members.map((m) => {
        const isMe = m.userId === currentUserId;
        const isHost = m.userId === hostUserId || m.isHost;

        return (
          <li
            key={m.userId}
            className={cn(
              'flex items-center gap-3 p-3',
              isMe && 'bg-table-700/40',
            )}
          >
            {m.photoUrl ? (
              <img
                src={m.photoUrl}
                alt=""
                className="h-9 w-9 rounded-full border border-table-600 object-cover"
              />
            ) : (
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-table-700 text-table-200">
                <User className="h-4 w-4" aria-hidden="true" />
              </span>
            )}

            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-army-white">{m.displayName}</p>
              <div className="mt-0.5 flex items-center gap-2 text-xs">
                {isHost && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-army-yellow/20 px-2 py-0.5 text-yellow-200">
                    <Crown className="h-3 w-3" aria-hidden="true" />
                    {t('common.host')}
                  </span>
                )}
                {isMe && (
                  <span className="inline-flex items-center rounded-full bg-army-red/20 px-2 py-0.5 text-red-200">
                    {t('common.you')}
                  </span>
                )}
              </div>
            </div>

            <div className="shrink-0">
              {m.color ? (
                <span
                  className="block h-5 w-5 rounded-full border border-table-600"
                  style={{ backgroundColor: ARMY_COLOR_HEX[m.color] }}
                  title={t(`colors.${m.color}`) ?? m.color}
                  aria-label={t(`colors.${m.color}`) ?? m.color}
                />
              ) : (
                <span className="text-xs text-table-300">{t('room.no_color')}</span>
              )}
            </div>
          </li>
        );
      })}
    </ul>
  );
}
