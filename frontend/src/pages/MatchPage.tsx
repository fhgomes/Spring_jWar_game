import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useMatch } from '@/hooks/useMatch';
import { useAuthStore } from '@/stores/useAuthStore';
import { useToastStore } from '@/stores/useToastStore';
import { apiClient, isApiError } from '@/lib/api';
import { Spinner } from '@/components/ui/Spinner';
import { BoardSvg } from '@/components/game/BoardSvg';
import { TurnHud, ContinentBonusBanner } from '@/components/game/TurnHud';
import { ActionPanel } from '@/components/game/ActionPanel';
import { CardHandSidebar } from '@/components/game/CardHandSidebar';
import { ObjectivePanel } from '@/components/game/ObjectivePanel';
import { ActionFeed } from '@/components/game/ActionFeed';
import { AttackModal } from '@/components/game/AttackModal';
import { MoveTroopsControls } from '@/components/game/MoveTroopsControls';
import { EndGameModal } from '@/components/game/EndGameModal';
import type {
  AttackResultDto,
  CountrySnapshot,
  GameActionRequest,
} from '@/types/api';

interface SelectedSrc {
  country: CountrySnapshot;
}

export default function MatchPage() {
  const { t } = useTranslation();
  const { matchId } = useParams<{ matchId: string }>();
  const currentUser = useAuthStore((s) => s.currentUser);
  const toast = useToastStore((s) => s.push);
  const queryClient = useQueryClient();

  const { data: snapshot, isLoading, isError, actionLog, matchFinished } = useMatch(matchId);

  const [selectedSrc, setSelectedSrc] = useState<SelectedSrc | null>(null);
  const [attackTarget, setAttackTarget] = useState<CountrySnapshot | null>(null);
  const [attackResult, setAttackResult] = useState<AttackResultDto | null>(null);
  const [moveTarget, setMoveTarget] = useState<CountrySnapshot | null>(null);
  const [finalMapMode, setFinalMapMode] = useState(false);

  const action = useMutation({
    mutationFn: async (req: GameActionRequest) => {
      const { data } = await apiClient.post(`/matches/${matchId}/actions`, req);
      return data as AttackResultDto | undefined;
    },
    onError: (err) => {
      if (isApiError(err)) {
        if (err.code === 'NOT_YOUR_TURN') {
          toast({ variant: 'warning', title: t('match.errors.not_your_turn') });
        } else if (err.code === 'INVALID_ACTION') {
          toast({ variant: 'warning', title: t('match.errors.invalid_action') });
        } else {
          toast({ variant: 'error', title: t('match.errors.send_failed') });
        }
      }
      queryClient.invalidateQueries({ queryKey: ['match', matchId] });
    },
  });

  const isViewerTurn = snapshot?.currentTurnUserId === currentUser?.id;
  const isInMatch = snapshot?.players.some((p) => p.userId === currentUser?.id) ?? false;
  const isSpectator = !isInMatch;

  const ownedCountryKeys = useMemo(() => {
    if (!snapshot || !currentUser) return [];
    return snapshot.countries.filter((c) => c.ownerUserId === currentUser.id).map((c) => c.key);
  }, [snapshot, currentUser]);

  const handleTerritoryClick = (country: CountrySnapshot) => {
    if (!snapshot || !isViewerTurn || finalMapMode) return;

    if (snapshot.currentPhase === 'ADD') {
      // Quick +1 deploy on own territories
      if (country.ownerUserId !== currentUser?.id) return;
      action.mutate({ type: 'DEPLOY', country: country.code, count: 1 });
      return;
    }

    if (snapshot.currentPhase === 'ATTACK') {
      if (!selectedSrc) {
        if (country.ownerUserId === currentUser?.id && country.troops > 1) {
          setSelectedSrc({ country });
        }
        return;
      }
      // Already have a source — clicking an enemy = attack target
      if (country.ownerUserId !== currentUser?.id) {
        setAttackTarget(country);
        setAttackResult(null);
      } else if (country.code !== selectedSrc.country.code) {
        // Re-select source
        setSelectedSrc({ country });
      } else {
        setSelectedSrc(null);
      }
      return;
    }

    if (snapshot.currentPhase === 'MOVE') {
      if (!selectedSrc) {
        if (country.ownerUserId === currentUser?.id && country.troops > 1) {
          setSelectedSrc({ country });
        }
        return;
      }
      if (country.ownerUserId === currentUser?.id && country.code !== selectedSrc.country.code) {
        setMoveTarget(country);
      } else {
        setSelectedSrc(null);
      }
      return;
    }
  };

  const handleAttack = async (dice: number) => {
    if (!selectedSrc || !attackTarget) return;
    const res = await action.mutateAsync({
      type: 'ATTACK',
      source: selectedSrc.country.code,
      target: attackTarget.code,
      attackerDice: dice,
    });
    if (res) setAttackResult(res);
  };

  const handleAttackClose = () => {
    setAttackTarget(null);
    setAttackResult(null);
    setSelectedSrc(null);
  };

  const handleMoveConfirm = async (count: number) => {
    if (!selectedSrc || !moveTarget) return;
    await action.mutateAsync({
      type: 'MOVE',
      source: selectedSrc.country.code,
      target: moveTarget.code,
      count,
    });
    setMoveTarget(null);
    setSelectedSrc(null);
  };

  const handleEndPhase = () => {
    if (!snapshot) return;
    action.mutate({
      type: snapshot.currentPhase === 'MOVE' ? 'END_TURN' : 'END_PHASE',
    });
  };

  const handleExchange = async (cardIds: string[]) => {
    await action.mutateAsync({ type: 'EXCHANGE_CARDS', cardIds });
  };

  if (isLoading) {
    return (
      <div className="grid place-items-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isError || !snapshot) {
    return (
      <div className="grid place-items-center py-20 text-table-100">
        <p>{t('common.error')}</p>
      </div>
    );
  }

  // TODO: backend should include adjacency (or `validAttackTargets` / `validMoveTargets`)
  // on the snapshot or on the selected source. For now we permissively highlight
  // every non-owned (attack) or owned (move) territory. See spec 010 FR-011.
  const attackTargets =
    snapshot.currentPhase === 'ATTACK' && selectedSrc
      ? snapshot.countries
          .filter((c) => c.ownerUserId !== currentUser?.id)
          .map((c) => c.code)
      : [];

  const moveTargets =
    snapshot.currentPhase === 'MOVE' && selectedSrc
      ? snapshot.countries
          .filter((c) => c.ownerUserId === currentUser?.id && c.code !== selectedSrc.country.code)
          .map((c) => c.code)
      : [];

  const sourceName = selectedSrc
    ? t(`countries.${selectedSrc.country.key}`) ?? selectedSrc.country.name
    : '';
  const attackTargetName = attackTarget
    ? t(`countries.${attackTarget.key}`) ?? attackTarget.name
    : '';
  const moveTargetName = moveTarget
    ? t(`countries.${moveTarget.key}`) ?? moveTarget.name
    : '';

  return (
    <div className="flex h-[calc(100vh-3.5rem)] flex-col bg-table-900">
      <TurnHud
        snapshot={snapshot}
        currentUserId={currentUser?.id}
        onEndPhase={handleEndPhase}
        endingPhase={action.isPending}
      />
      <ContinentBonusBanner snapshot={snapshot} />

      <div className="flex flex-1 overflow-hidden">
        <section className="flex flex-1 flex-col">
          <div className="flex-1 overflow-hidden">
            <BoardSvg
              countries={snapshot.countries}
              players={snapshot.players}
              selectedCountry={selectedSrc?.country.code ?? null}
              attackTargets={attackTargets}
              moveTargets={moveTargets}
              disabled={!isViewerTurn || isSpectator || finalMapMode}
              onTerritoryClick={handleTerritoryClick}
            />
          </div>

          <div className="space-y-2 p-2">
            {isViewerTurn && !finalMapMode && (
              <ActionPanel phase={snapshot.currentPhase} />
            )}
            <ObjectivePanel objectiveText={snapshot.myObjective} isSpectator={isSpectator} />
            <ActionFeed entries={actionLog} />
          </div>
        </section>

        {!isSpectator && (
          <aside className="hidden w-80 shrink-0 md:block">
            <CardHandSidebar
              cards={snapshot.myCards ?? []}
              ownedCountryKeys={ownedCountryKeys}
              exchangeRoundAward={
                snapshot.exchangeRound != null
                  ? Math.max(4, 4 + 2 * snapshot.exchangeRound)
                  : 4
              }
              onExchange={handleExchange}
              loading={action.isPending}
              countries={snapshot.countries}
            />
          </aside>
        )}
      </div>

      {/* Attack modal: opens when an enemy target is picked */}
      {attackTarget && selectedSrc && (
        <AttackModal
          open={!!attackTarget}
          onClose={handleAttackClose}
          onAttack={handleAttack}
          sourceTroops={selectedSrc.country.troops}
          sourceName={sourceName}
          targetName={attackTargetName}
          result={attackResult}
          loading={action.isPending}
        />
      )}

      {/* Move modal */}
      {moveTarget && selectedSrc && (
        <MoveTroopsControls
          open={!!moveTarget}
          onClose={() => setMoveTarget(null)}
          onConfirm={handleMoveConfirm}
          sourceName={sourceName}
          targetName={moveTargetName}
          minMove={1}
          maxMove={selectedSrc.country.troops - 1}
          loading={action.isPending}
        />
      )}

      <EndGameModal
        open={!!matchFinished && !finalMapMode}
        payload={matchFinished}
        onViewFinalMap={() => setFinalMapMode(true)}
      />

      {finalMapMode && (
        <div className="pointer-events-none absolute left-1/2 top-20 -translate-x-1/2 rounded-full bg-army-yellow px-4 py-1 text-sm font-semibold text-table-900 shadow-strong">
          {t('match.match_ended')}
        </div>
      )}
    </div>
  );
}

