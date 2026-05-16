import type { ReactNode } from 'react';
import { Outlet } from 'react-router-dom';
import { TopNav } from './TopNav';
import { VerifyEmailBanner } from '@/components/auth/VerifyEmailBanner';

interface AppShellProps {
  children?: ReactNode;
}

export function AppShell({ children }: AppShellProps) {
  return (
    <div className="flex min-h-screen flex-col bg-table-900">
      <TopNav />
      <VerifyEmailBanner />
      <main className="flex-1">{children ?? <Outlet />}</main>
    </div>
  );
}
