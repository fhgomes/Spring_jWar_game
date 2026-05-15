import { type ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/useAuthStore';
import { Spinner } from '@/components/ui/Spinner';

interface Props {
  children: ReactNode;
}

export function ProtectedRoute({ children }: Props) {
  const status = useAuthStore((s) => s.status);
  const location = useLocation();

  if (status === 'loading') {
    return (
      <div className="grid min-h-screen place-items-center bg-table-900">
        <Spinner size="lg" />
      </div>
    );
  }

  if (status === 'signed-out') {
    const next = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`/login?next=${next}`} replace />;
  }

  return <>{children}</>;
}

export function PublicOnlyRoute({ children }: Props) {
  const status = useAuthStore((s) => s.status);

  if (status === 'loading') {
    return (
      <div className="grid min-h-screen place-items-center bg-table-900">
        <Spinner size="lg" />
      </div>
    );
  }

  if (status === 'signed-in') {
    return <Navigate to="/lobby" replace />;
  }

  return <>{children}</>;
}
