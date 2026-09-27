import { Fragment } from 'react';
import { Menu, Transition } from '@headlessui/react';
import { Link, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { LogOut, Settings, User } from 'lucide-react';
import { useAuthStore } from '@/stores/useAuthStore';
import { useToastStore } from '@/stores/useToastStore';
import { cn } from '@/lib/utils';

export function UserMenu() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.currentUser);
  const firebaseUser = useAuthStore((s) => s.firebaseUser);
  const doSignOut = useAuthStore((s) => s.signOut);
  const toast = useToastStore((s) => s.push);

  const displayName = user?.displayName ?? firebaseUser?.displayName ?? firebaseUser?.email ?? '';
  const photoUrl = user?.photoUrl ?? firebaseUser?.photoURL ?? undefined;

  const handleSignOut = async () => {
    await doSignOut();
    queryClient.clear();
    toast({ variant: 'info', title: t('common.ok') ?? '' });
    navigate('/login');
  };

  if (!firebaseUser) return null;

  return (
    <Menu as="div" className="relative">
      <Menu.Button
        className="flex items-center gap-2 rounded-md p-1 transition-colors hover:bg-table-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-army-red"
        aria-label={t('nav.account') ?? ''}
      >
        {photoUrl ? (
          <img
            src={photoUrl}
            alt=""
            className="h-8 w-8 rounded-full border border-table-600 object-cover"
          />
        ) : (
          <span className="flex h-8 w-8 items-center justify-center rounded-full bg-table-700">
            <User className="h-4 w-4 text-army-white" />
          </span>
        )}
        <span className="hidden text-sm text-army-white sm:inline">{displayName}</span>
      </Menu.Button>

      <Transition
        as={Fragment}
        enter="transition ease-out duration-100"
        enterFrom="opacity-0 scale-95"
        enterTo="opacity-100 scale-100"
        leave="transition ease-in duration-75"
        leaveFrom="opacity-100 scale-100"
        leaveTo="opacity-0 scale-95"
      >
        <Menu.Items className="absolute right-0 mt-2 w-56 origin-top-right rounded-md border border-table-700 bg-table-800 shadow-strong focus:outline-none">
          <div className="border-b border-table-700 p-3">
            <p className="text-sm font-medium text-army-white">{displayName}</p>
            <p className="truncate text-xs text-table-200">{user?.email ?? firebaseUser.email}</p>
          </div>
          <div className="p-1">
            <Menu.Item>
              {({ active }) => (
                <Link
                  to="/me/settings"
                  className={cn(
                    'flex w-full items-center gap-2 rounded px-3 py-2 text-sm text-army-white',
                    active && 'bg-table-700',
                  )}
                >
                  <Settings className="h-4 w-4" />
                  {t('nav.account')}
                </Link>
              )}
            </Menu.Item>
            <Menu.Item>
              {({ active }) => (
                <button
                  type="button"
                  onClick={handleSignOut}
                  className={cn(
                    'flex w-full items-center gap-2 rounded px-3 py-2 text-sm text-army-white',
                    active && 'bg-table-700',
                  )}
                >
                  <LogOut className="h-4 w-4" />
                  {t('nav.logout')}
                </button>
              )}
            </Menu.Item>
          </div>
        </Menu.Items>
      </Transition>
    </Menu>
  );
}
