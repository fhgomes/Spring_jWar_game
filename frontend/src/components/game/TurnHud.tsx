import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/Button';
import { ARMY_COLOR_HEX } from '@/types/game';
import type { GamePhase, GameStateSnapshot, PlayerSnapshot } from '@/types/api';

interface Props {
  snapshot: GameStateSnapshot;
  currentUserId: string | undefined;
  onEndPhase(): void;
  endingPhase?: boolean;
}

export function TurnHud({ snapshot, currentUserId, onEndPhase, endingPhase }: Props) {
  const { t } = useTranslation();
  const currentPlayer = snapshot.players.find((p) => p.userId === snapshot.currentTurnUserId);
  const isViewerTurn = snapshot.currentTurnUserId === currentUserId;
  const isViewerInMatch = snapshot.players.some((p) => p.userId === currentUserId);

  const phaseLabel = t(`phases.${snapshot.currentPhase}`);
  const endLabel = snapshot.currentPhase === 'MOVE' ? t('match.end_turn') : t('match.end_phase');

  return (
    <header className="flex items-center justify-between gap-4 border-b border-table-700 bg-table-900/95 px-4 py-2 backdrop-blur">
      <div className="flex items-center gap-3">
        {currentPlayer && <PlayerChip player={currentPlayer} />}
        <span className="rounded-md bg-army-red/20 px-2 py-1 text-xs font-medium uppercase tracking-wider text-red-200">
          {phaseLabel}
        </span>

        {isViewerTurn && snapshot.currentPhase === 'ADD' && snapshot.troopsToDeploy != null && (
          <span className="text-sm text-table-100">
            {t('match.troops_to_deploy', { count: snapshot.troopsToDeploy })}
          </span>
        )}
      </div>

      <div className="flex items-center gap-3">
        {!isViewerInMatch && (
          <span className="rounded-md bg-table-700 px-2 py-1 text-xs text-table-100">
            {t('match.spectator_mode')}
          </span>
        )}
        {!isViewerTurn && currentPlayer && isViewerInMatch && (
          <span className="hidden text-sm text-table-200 md:inline">
            {t('match.watching', { name: currentPlayer.displayName })}
          </span>
        )}
        {isViewerTurn && (
          <Button onClick={onEndPhase} loading={endingPhase} variant="primary">
            {endLabel}
          </Button>
        )}
      </div>
    </header>
  );
}

function PlayerChip({ player }: { player: PlayerSnapshot }) {
  return (
    <div className="flex items-center gap-2">
      <span
        className="inline-block h-4 w-4 rounded-full border border-table-600"
        style={{ backgroundColor: ARMY_COLOR_HEX[player.color] }}
        aria-hidden="true"
      />
      <span className="text-sm font-medium text-army-white">{player.displayName}</span>
    </div>
  );
}

export function ContinentBonusBanner({
  snapshot,
}: {
  snapshot: GameStateSnapshot;
}) {
  const { t } = useTranslation();
  const ctx = snapshot.continentBonusContext;
  if (!ctx) return null;

  return (
    <div className="border-b border-yellow-700 bg-yellow-900/40 px-4 py-2 text-sm text-yellow-100">
      {t('match.continent_bonus_banner', {
        count: ctx.remaining,
        continent: t(`continents.${ctx.continentKey}`),
      })}
    </div>
  );
}

export function phaseHasTodo(snapshot: GameStateSnapshot): string | null {
  if (snapshot.currentPhase === 'ADD' && (snapshot.troopsToDeploy ?? 0) > 0) {
    return `Posicione os exércitos restantes (${snapshot.troopsToDeploy})`;
  }
  return null;
}

export function _phaseTypeNarrow(p: GamePhase): GamePhase {
  return p;
}
