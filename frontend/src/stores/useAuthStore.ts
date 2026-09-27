import { create } from 'zustand';
import { onAuthStateChanged, signOut as fbSignOut, type User as FirebaseUser } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import type { UserResponse } from '@/types/api';

export type AuthStatus = 'loading' | 'signed-in' | 'signed-out';

interface AuthState {
  status: AuthStatus;
  firebaseUser: FirebaseUser | null;
  /** Bootstrapped via GET /api/me. */
  currentUser: UserResponse | null;
  idToken: string | null;

  // actions
  setFirebaseUser(user: FirebaseUser | null): void;
  setCurrentUser(user: UserResponse | null): void;
  signOut(): Promise<void>;
  getIdToken(): Promise<string | null>;
  refreshIdToken(): Promise<string | null>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  status: 'loading',
  firebaseUser: null,
  currentUser: null,
  idToken: null,

  setFirebaseUser(user) {
    set({
      firebaseUser: user,
      status: user ? 'signed-in' : 'signed-out',
    });
  },

  setCurrentUser(user) {
    set({ currentUser: user });
  },

  async signOut() {
    await fbSignOut(auth);
    set({
      firebaseUser: null,
      currentUser: null,
      idToken: null,
      status: 'signed-out',
    });
  },

  async getIdToken() {
    const u = get().firebaseUser;
    if (!u) return null;
    const token = await u.getIdToken(/* forceRefresh */ false);
    set({ idToken: token });
    return token;
  },

  async refreshIdToken() {
    const u = get().firebaseUser;
    if (!u) return null;
    const token = await u.getIdToken(/* forceRefresh */ true);
    set({ idToken: token });
    return token;
  },
}));

// One-time wiring: listen to Firebase auth state changes.
// Imported by main.tsx so the store hydrates before React renders.
let initialized = false;
export function initAuthListener(): void {
  if (initialized) return;
  initialized = true;
  onAuthStateChanged(auth, (user) => {
    useAuthStore.getState().setFirebaseUser(user);
    if (!user) {
      useAuthStore.setState({ currentUser: null, idToken: null });
    }
  });
}
