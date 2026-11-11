import { useCallback, useState } from 'react';
import { ApiError, loadSession, logout, type Session } from './api';
import { AlertDetail } from './components/AlertDetail';
import { AlertList } from './components/AlertList';
import { Login } from './components/Login';

type View = { name: 'list' } | { name: 'detail'; id: number };

export function App() {
  const [session, setSession] = useState<Session | null>(loadSession);
  const [view, setView] = useState<View>({ name: 'list' });

  const signOut = useCallback(() => {
    logout();
    setSession(null);
    setView({ name: 'list' });
  }, []);

  // Any 401 means the token expired or was rejected: go back to the login screen.
  const onError = useCallback(
    (error: unknown) => {
      if (error instanceof ApiError && error.status === 401) {
        signOut();
      }
    },
    [signOut],
  );

  if (!session) {
    return <Login onLoggedIn={setSession} />;
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <a className="brand" href="/" aria-label="Sentinel home">
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" fill="none">
              <path d="M16 3 27 7v8c0 7.3-4.7 12.1-11 14C9.7 27.1 5 22.3 5 15V7l11-4Z" />
              <path d="m11 16 3.2 3.2L21.5 12" />
            </svg>
          </span>
          <span>
            <strong>sentinel</strong>
            <small>RISK INTELLIGENCE</small>
          </span>
        </a>

        <div className="sidebar-label">WORKSPACE</div>
        <div className="sidebar-item active">
          <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
            <path d="M4 5.5h12M4 10h12M4 14.5h8" />
            <circle cx="15" cy="14.5" r="2" />
          </svg>
          <span>Alert queue</span>
        </div>

        <div className="sidebar-spacer" />
        <div className="sidebar-footer">
          <span className="status-dot" />
          <span>
            <strong>Analyst workspace</strong>
            <small>Protected session</small>
          </span>
        </div>
      </aside>

      <div className="workspace">
        <header className="topbar">
          <div className="breadcrumbs">
            <span>Workspace</span>
            <span className="breadcrumb-divider">/</span>
            <strong>{view.name === 'list' ? 'Alert queue' : `Alert #${view.id}`}</strong>
          </div>
          <div className="who">
            <span className="avatar" aria-hidden="true">{session.username.slice(0, 1).toUpperCase()}</span>
            <span className="user-copy">
              <strong>{session.username}</strong>
              <small>{session.role.toLowerCase()}</small>
            </span>
            <button className="sign-out" onClick={signOut}>Sign out</button>
          </div>
        </header>
        <main>
          {view.name === 'list' ? (
            <AlertList onOpen={(id) => setView({ name: 'detail', id })} onError={onError} />
          ) : (
            <AlertDetail id={view.id} onBack={() => setView({ name: 'list' })} onError={onError} />
          )}
          <footer className="page-footer">
            <span>Sentinel Risk Intelligence</span>
            <span>Fraud operations · South Africa</span>
          </footer>
        </main>
      </div>
    </div>
  );
}
