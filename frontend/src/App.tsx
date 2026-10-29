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
      <header className="topbar">
        <h1>Sentinel</h1>
        <div className="who">
          <span>
            {session.username} ({session.role.toLowerCase()})
          </span>
          <button className="link" onClick={signOut}>
            Sign out
          </button>
        </div>
      </header>
      <main>
        {view.name === 'list' ? (
          <AlertList onOpen={(id) => setView({ name: 'detail', id })} onError={onError} />
        ) : (
          <AlertDetail id={view.id} onBack={() => setView({ name: 'list' })} onError={onError} />
        )}
      </main>
    </div>
  );
}
