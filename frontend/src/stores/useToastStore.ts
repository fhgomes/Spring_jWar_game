import { create } from 'zustand';
import { uid } from '@/lib/utils';

export type ToastVariant = 'info' | 'success' | 'warning' | 'error';

export interface ToastMessage {
  id: string;
  variant: ToastVariant;
  title: string;
  description?: string;
  durationMs: number;
}

interface ToastState {
  toasts: ToastMessage[];
  push(toast: Omit<ToastMessage, 'id' | 'durationMs'> & { durationMs?: number }): string;
  dismiss(id: string): void;
}

export const useToastStore = create<ToastState>((set, get) => ({
  toasts: [],

  push(input) {
    const id = uid();
    const toast: ToastMessage = {
      id,
      variant: input.variant,
      title: input.title,
      description: input.description,
      durationMs: input.durationMs ?? 5000,
    };
    set({ toasts: [...get().toasts, toast] });
    if (toast.durationMs > 0) {
      setTimeout(() => get().dismiss(id), toast.durationMs);
    }
    return id;
  },

  dismiss(id) {
    set({ toasts: get().toasts.filter((t) => t.id !== id) });
  },
}));
