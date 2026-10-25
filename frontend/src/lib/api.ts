import axios, { type AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/stores/useAuthStore';
import { useToastStore } from '@/stores/useToastStore';

/**
 * Typed error from the backend envelope.
 *   { code: string; message: string; details?: unknown }
 */
export class ApiError extends Error {
  public readonly code: string;
  public readonly httpStatus: number;
  public readonly details: unknown;

  constructor(code: string, message: string, httpStatus: number, details: unknown = null) {
    super(message);
    this.code = code;
    this.httpStatus = httpStatus;
    this.details = details;
    this.name = 'ApiError';
  }
}

interface BackendErrorEnvelope {
  code: string;
  message: string;
  details?: unknown;
}

const pkgVersion = '0.1.0';

export const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 15_000,
  headers: {
    'X-Client-Version': pkgVersion,
  },
});

// Request interceptor — attach Firebase ID token
apiClient.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  const token = await useAuthStore.getState().getIdToken();
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

// Response interceptor — convert errors to ApiError, handle 401 silent refresh once.
let refreshAttempted = false;
apiClient.interceptors.response.use(
  (resp) => {
    refreshAttempted = false;
    return resp;
  },
  async (error: AxiosError<BackendErrorEnvelope>) => {
    // Network / timeout — no `response` object
    if (!error.response) {
      useToastStore.getState().push({
        variant: 'error',
        title: 'Sem conexão com o servidor.',
      });
      return Promise.reject(new ApiError('NETWORK_ERROR', 'Sem conexão com o servidor.', 0));
    }

    const status = error.response.status;
    const data = error.response.data;

    // Silent token refresh on first 401
    if (status === 401 && !refreshAttempted) {
      refreshAttempted = true;
      try {
        await useAuthStore.getState().refreshIdToken();
        // Retry original request once
        if (error.config) {
          return apiClient.request(error.config);
        }
      } catch {
        // fallthrough — sign out + redirect
      }
      await useAuthStore.getState().signOut();
      useToastStore.getState().push({
        variant: 'warning',
        title: 'Sua sessão expirou. Entre novamente.',
      });
    }

    const apiErr = new ApiError(
      data?.code ?? 'UNKNOWN_ERROR',
      data?.message ?? error.message ?? 'Erro desconhecido.',
      status,
      data?.details ?? null,
    );

    return Promise.reject(apiErr);
  },
);

export function isApiError(err: unknown): err is ApiError {
  return err instanceof ApiError;
}
