// Backend DTO contract — keep in sync with Java DTOs.
// See specs/003-server-rest-foundation, 004-auth-and-users,
// 005-game-rooms-and-matches, 006-realtime-gameplay.

export type RoomStatus = 'OPEN' | 'FULL' | 'IN_PROGRESS' | 'FINISHED' | 'CLOSED';
export type MatchStatus = 'LOBBY' | 'IN_PROGRESS' | 'FINISHED';
export type GamePhase = 'ADD' | 'ATTACK' | 'MOVE';

export type ArmyColor = 'RED' | 'BLUE' | 'GREEN' | 'YELLOW' | 'BLACK' | 'WHITE' | 'GRAY' | 'PURPLE';

export type GameEventType =
  | 'TROOPS_ADDED'
  | 'ATTACK_RESULT'
  | 'TROOPS_MOVED'
  | 'PHASE_ENDED'
  | 'TURN_CHANGED'
  | 'MATCH_FINISHED'
  | 'ROOM_MEMBER_JOINED'
  | 'ROOM_MEMBER_LEFT'
  | 'ROOM_MEMBER_COLOR_CHANGED'
  | 'ROOM_HOST_CHANGED'
  | 'ROOM_MESSAGE'
  | 'ROOM_UPDATED'
  | 'LOBBY_UPDATED'
  | 'MATCH_STARTED'
  | 'STATE_PATCH';

/** User profile returned by GET /api/me. */
export interface UserResponse {
  id: string;
  firebaseUid: string;
  email: string;
  displayName: string;
  photoUrl?: string;
  provider: 'email' | 'google';
  createdAt: string;
}

export interface UpdateUserRequest {
  displayName?: string;
  photoUrl?: string | null;
}

/** Lightweight row used by GET /api/rooms. */
export interface RoomSummary {
  id: string;
  name: string;
  hostNickname: string;
  memberCount: number;
  maxPlayers: number;
  status: RoomStatus;
  hasPassword: boolean;
  createdAt: string;
  /** Optional convenience field set by the backend with claimed colors. */
  claimedColors?: ArmyColor[];
}

export interface RoomMemberDto {
  userId: string;
  displayName: string;
  photoUrl?: string;
  color: ArmyColor | null;
  isHost: boolean;
  joinedAt: string;
}

export interface RoomDetail {
  id: string;
  name: string;
  hostUserId: string;
  status: RoomStatus;
  maxPlayers: number;
  members: RoomMemberDto[];
  hasPassword: boolean;
  createdAt: string;
}

export interface CreateRoomRequest {
  name: string;
  maxPlayers: number;
  password?: string;
}

export interface JoinRoomRequest {
  password?: string;
}

export interface RoomMessage {
  id: string;
  roomId: string;
  senderUserId: string;
  senderDisplayName: string;
  senderPhotoUrl?: string;
  text: string;
  createdAt: string;
  /** Echo of the client-generated temp id for optimistic reconciliation. */
  clientTempId?: string;
}

export interface SendRoomMessageRequest {
  text: string;
  clientTempId?: string;
}

// ---------------- Match / Game ---------------------------------------

export interface PlayerSnapshot {
  userId: string;
  displayName: string;
  color: ArmyColor;
  troopsAvailable: number;
  cardCount: number;
  eliminated: boolean;
}

export interface CountrySnapshot {
  code: number;
  /** Backend enum key — e.g., "BRA", "ARG". */
  key: string;
  name: string;
  continentCode: number;
  ownerUserId: string | null;
  troops: number;
  /** Per-territory remaining movable troops for the current Movement phase. */
  movableTroops?: number;
}

export interface ContinentSnapshot {
  code: number;
  key: string;
  name: string;
  ownerUserId: string | null;
  bonusTroops: number;
}

export interface CardSnapshot {
  id: string;
  countryKey: string | null; // null for jokers
  shape: 'CIRCLE' | 'TRIANGLE' | 'SQUARE' | 'JOKER';
}

export interface GameStateSnapshot {
  matchId: string;
  status: MatchStatus;
  currentTurnUserId: string;
  currentPhase: GamePhase;
  players: PlayerSnapshot[];
  countries: CountrySnapshot[];
  continents: ContinentSnapshot[];
  turnNumber: number;
  /** Only included for the viewer. */
  myCards?: CardSnapshot[];
  /** Only included for the viewer. */
  myObjective?: string;
  /** Continent-bonus placement state, only for current player during ADD phase. */
  continentBonusContext?: {
    continentKey: string;
    remaining: number;
  } | null;
  exchangeRound?: number;
  /** Number of troops still to place for the current player in ADD phase. */
  troopsToDeploy?: number;
}

export interface AttackResultDto {
  srcCountry: number;
  targetCountry: number;
  attackers: number[];
  defense: number[];
  srcCountryLoss: number;
  targetCountryLoss: number;
  conquered: boolean;
  playerDestroyed: boolean;
}

export type GameActionRequest =
  | { type: 'DEPLOY'; country: number; count: number }
  | { type: 'ATTACK'; source: number; target: number; attackerDice: number }
  | { type: 'MOVE'; source: number; target: number; count: number }
  | { type: 'MOVE_AFTER_CONQUEST'; source: number; target: number; count: number }
  | { type: 'EXCHANGE_CARDS'; cardIds: string[] }
  | { type: 'END_PHASE' }
  | { type: 'END_TURN' };

export interface GameEvent<T = unknown> {
  type: GameEventType;
  matchId?: string;
  roomId?: string;
  payload: T;
  timestamp: string;
}

export interface MatchFinishedPayload {
  winnerUserId: string;
  winnerDisplayName: string;
  objectiveText: string;
  turnNumber: number;
}

export interface DiceRolledPayload extends AttackResultDto {
  actorUserId: string;
}
