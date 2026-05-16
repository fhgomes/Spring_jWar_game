import { useTranslation } from 'react-i18next';
import { Lock, Users } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardFooter, CardHeader } from '@/components/ui/Card';
import { ARMY_COLOR_HEX } from '@/types/game';
import type { RoomSummary } from '@/types/api';

interface Props {
  room: RoomSummary;
  onJoin(room: RoomSummary): void;
  joining?: boolean;
}

export function RoomCard({ room, onJoin, joining }: Props) {
  const { t } = useTranslation();
  const isFull = room.memberCount >= room.maxPlayers || room.status === 'FULL';

  return (
    <Card className="flex flex-col">
      <CardHeader className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <h3
            className="truncate text-base font-semibold text-army-white"
            title={room.name}
          >
            {room.name}
          </h3>
          <p className="mt-0.5 truncate text-xs text-table-200">
            {t('common.host')}: {room.hostNickname}
          </p>
        </div>
        {room.hasPassword && (
          <span
            className="rounded-full bg-table-700 p-1.5 text-table-200"
            aria-label={t('lobby.password_required') ?? ''}
            title={t('lobby.password_required') ?? ''}
          >
            <Lock className="h-3.5 w-3.5" />
          </span>
        )}
      </CardHeader>

      <CardBody className="flex-1">
        <div className="flex items-center gap-2 text-sm text-table-100">
          <Users className="h-4 w-4 text-table-300" />
          <span>
            {t('lobby.players_count', { count: room.memberCount, max: room.maxPlayers })}
          </span>
        </div>

        {room.claimedColors && room.claimedColors.length > 0 && (
          <div className="mt-3 flex gap-1.5">
            {room.claimedColors.map((c) => (
              <span
                key={c}
                title={t(`colors.${c}`) ?? c}
                aria-label={t(`colors.${c}`) ?? c}
                className="h-3.5 w-3.5 rounded-full border border-table-600"
                style={{ backgroundColor: ARMY_COLOR_HEX[c] }}
              />
            ))}
          </div>
        )}
      </CardBody>

      <CardFooter>
        <Button
          variant="primary"
          size="sm"
          fullWidth
          disabled={isFull}
          loading={joining}
          onClick={() => onJoin(room)}
        >
          {isFull ? t('lobby.full') : t('lobby.join')}
        </Button>
      </CardFooter>
    </Card>
  );
}
