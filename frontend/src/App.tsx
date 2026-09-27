import { Route, Routes } from 'react-router-dom';
import { AppShell } from '@/components/layout/AppShell';
import { ProtectedRoute, PublicOnlyRoute } from '@/components/auth/ProtectedRoute';
import HomePage from '@/pages/HomePage';
import LoginPage from '@/pages/LoginPage';
import SignupPage from '@/pages/SignupPage';
import LobbyPage from '@/pages/LobbyPage';
import RoomPage from '@/pages/RoomPage';
import MatchPage from '@/pages/MatchPage';
import AccountSettingsPage from '@/pages/AccountSettingsPage';
import GoodbyePage from '@/pages/GoodbyePage';
import NotFoundPage from '@/pages/NotFoundPage';

export function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route
        path="/login"
        element={
          <PublicOnlyRoute>
            <LoginPage />
          </PublicOnlyRoute>
        }
      />
      <Route
        path="/signup"
        element={
          <PublicOnlyRoute>
            <SignupPage />
          </PublicOnlyRoute>
        }
      />
      <Route path="/goodbye" element={<GoodbyePage />} />

      <Route
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route path="/lobby" element={<LobbyPage />} />
        <Route path="/rooms/:roomId" element={<RoomPage />} />
        <Route path="/matches/:matchId" element={<MatchPage />} />
        <Route path="/me/settings" element={<AccountSettingsPage />} />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
