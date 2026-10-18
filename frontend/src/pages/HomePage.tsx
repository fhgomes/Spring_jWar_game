import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/useAuthStore';
import { Spinner } from '@/components/ui/Spinner';

export default function HomePage() {
  const status = useAuthStore((s) => s.status);

  if (status === 'loading') {
    return (
      <div className="grid min-h-screen place-items-center bg-table-900">
        <Spinner size="lg" />
      </div>
    );
  }

  return <Navigate to={status === 'signed-in' ? '/lobby' : '/login'} replace />;
}
