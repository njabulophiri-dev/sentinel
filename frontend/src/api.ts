import type { AlertStatus, AlertView, PageView, TokenResponse } from './types';

/** An error the API reported on purpose (RFC 7807 problem JSON). */
export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors?: Record<string, string>;

  constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

// The token lives in sessionStorage: it survives a refresh but not closing the tab, and it expires in 15 minutes.
const TOKEN_KEY = 'sentinel.session';

export interface Session {
  token: string;
  role: string;
  username: string;
}

export function loadSession(): Session | null {
  try {
    const raw = sessionStorage.getItem(TOKEN_KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

function saveSession(session: Session | null): void {
  if (session) {
    sessionStorage.setItem(TOKEN_KEY, JSON.stringify(session));
  } else {
    sessionStorage.removeItem(TOKEN_KEY);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const session = loadSession();
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (session) {
    headers.set('Authorization', `Bearer ${session.token}`);
  }

  const response = await fetch(`/api/v1${path}`, { ...init, headers });
  if (!response.ok) {
    let detail = `Request failed (${response.status})`;
    let fieldErrors: Record<string, string> | undefined;
    try {
      const problem = await response.json();
      detail = problem.detail ?? problem.title ?? detail;
      fieldErrors = problem.errors;
    } catch {
      // The body was empty or not JSON; keep the generic message.
    }
    throw new ApiError(response.status, detail, fieldErrors);
  }
  return (await response.json()) as T;
}

export async function login(username: string, password: string): Promise<Session> {
  const token = await request<TokenResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  const session = { token: token.accessToken, role: token.role, username };
  saveSession(session);
  return session;
}

export function logout(): void {
  saveSession(null);
}

export function listAlerts(status: AlertStatus | 'ALL', page: number): Promise<PageView<AlertView>> {
  const query = new URLSearchParams({ page: String(page), size: '20' });
  if (status !== 'ALL') {
    query.set('status', status);
  }
  return request<PageView<AlertView>>(`/alerts?${query}`);
}

export function getAlert(id: number): Promise<AlertView> {
  return request<AlertView>(`/alerts/${id}`);
}

export function resolveAlert(id: number, status: Exclude<AlertStatus, 'OPEN'>, note: string): Promise<AlertView> {
  return request<AlertView>(`/alerts/${id}/resolution`, {
    method: 'POST',
    body: JSON.stringify({ status, note: note.trim() || undefined }),
  });
}
