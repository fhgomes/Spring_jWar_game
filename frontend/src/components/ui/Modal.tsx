import { Fragment, type ReactNode } from 'react';
import { Dialog, Transition } from '@headlessui/react';
import { X } from 'lucide-react';
import { cn } from '@/lib/utils';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: ReactNode;
  description?: ReactNode;
  children: ReactNode;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  /** When true, clicking outside or pressing Esc does not close. */
  isStatic?: boolean;
  /** Hide the close (X) icon. */
  hideCloseButton?: boolean;
}

const SIZE: Record<NonNullable<ModalProps['size']>, string> = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
  xl: 'max-w-2xl',
};

export function Modal({
  open,
  onClose,
  title,
  description,
  children,
  size = 'md',
  isStatic = false,
  hideCloseButton = false,
}: ModalProps) {
  return (
    <Transition show={open} as={Fragment}>
      <Dialog
        as="div"
        className="relative z-50"
        onClose={isStatic ? () => undefined : onClose}
        static={isStatic ? true : undefined}
      >
        <Transition.Child
          as={Fragment}
          enter="ease-out duration-200"
          enterFrom="opacity-0"
          enterTo="opacity-100"
          leave="ease-in duration-150"
          leaveFrom="opacity-100"
          leaveTo="opacity-0"
        >
          <div className="fixed inset-0 bg-black/70 backdrop-blur-sm" aria-hidden="true" />
        </Transition.Child>

        <div className="fixed inset-0 overflow-y-auto">
          <div className="flex min-h-full items-center justify-center p-4">
            <Transition.Child
              as={Fragment}
              enter="ease-out duration-200"
              enterFrom="opacity-0 scale-95"
              enterTo="opacity-100 scale-100"
              leave="ease-in duration-150"
              leaveFrom="opacity-100 scale-100"
              leaveTo="opacity-0 scale-95"
            >
              <Dialog.Panel
                className={cn(
                  'relative w-full rounded-lg border border-table-700 bg-table-800 p-6 shadow-strong',
                  SIZE[size],
                )}
              >
                {!hideCloseButton && !isStatic && (
                  <button
                    type="button"
                    aria-label="Fechar"
                    onClick={onClose}
                    className="absolute right-3 top-3 rounded-md p-1 text-table-300 transition-colors hover:bg-table-700 hover:text-army-white focus:outline-none focus-visible:ring-2 focus-visible:ring-army-red"
                  >
                    <X className="h-5 w-5" />
                  </button>
                )}
                {title && (
                  <Dialog.Title className="mb-2 pr-8 text-lg font-semibold text-army-white">
                    {title}
                  </Dialog.Title>
                )}
                {description && (
                  <Dialog.Description className="mb-4 text-sm text-table-200">
                    {description}
                  </Dialog.Description>
                )}
                {children}
              </Dialog.Panel>
            </Transition.Child>
          </div>
        </div>
      </Dialog>
    </Transition>
  );
}
