import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';

export interface StompClientOptions {
  /**
   * Returns the current Firebase ID token. Called on every (re)connect attempt
   * so that an expired token isn't replayed.
   */
  getToken: () => Promise<string | null>;
  /**
   * Base WS URL — defaults to `/ws` (proxied to backend by Vite in dev).
   */
  wsUrl?: string;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (err: unknown) => void;
}

/**
 * Builds a configured STOMP client.
 *
 * Usage:
 *   const client = createStompClient({ getToken: () => auth.getIdToken() });
 *   client.activate();
 *   const sub = client.subscribe('/topic/lobby', (msg) => ...);
 *   client.deactivate();
 */
export function createStompClient(opts: StompClientOptions): Client {
  const wsUrl = opts.wsUrl ?? `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`;

  const client = new Client({
    brokerURL: wsUrl,
    reconnectDelay: 3000,
    heartbeatIncoming: 10_000,
    heartbeatOutgoing: 10_000,
    beforeConnect: async () => {
      const token = await opts.getToken();
      if (token) {
        client.connectHeaders = { Authorization: `Bearer ${token}` };
      } else {
        client.connectHeaders = {};
      }
    },
    onConnect: () => opts.onConnect?.(),
    onDisconnect: () => opts.onDisconnect?.(),
    onStompError: (frame) => opts.onError?.(frame),
    onWebSocketError: (event) => opts.onError?.(event),
  });

  return client;
}

export type { IMessage, StompSubscription };
