import { useState, type FormEvent } from 'react';
import { ApiError, login, type Session } from '../api';

export function Login({ onLoggedIn }: { onLoggedIn: (session: Session) => void }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      onLoggedIn(await login(username, password));
    } catch (e) {
      setError(
        e instanceof ApiError && e.status === 401
          ? 'Wrong username or password.'
          : e instanceof ApiError && e.status === 429
            ? 'Too many attempts. Wait a little before trying again.'
            : 'Could not sign in. Try again.',
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login">
      <section className="login-story">
        <a className="brand login-brand" href="/" aria-label="Sentinel">
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" fill="none">
              <path d="M16 3 27 7v8c0 7.3-4.7 12.1-11 14C9.7 27.1 5 22.3 5 15V7l11-4Z" />
              <path d="m11 16 3.2 3.2L21.5 12" />
            </svg>
          </span>
          <span><strong>sentinel</strong><small>RISK INTELLIGENCE</small></span>
        </a>
        <div className="login-story-copy">
          <div className="eyebrow"><span className="eyebrow-line" /> FRAUD OPERATIONS PLATFORM</div>
          <h1>See risk.<br /><span>Make the call.</span></h1>
          <p>One clear view of every signal, every decision, and every transaction that needs a closer look.</p>
        </div>
        <div className="login-preview" aria-hidden="true">
          <div className="preview-top"><span>RISK SIGNALS</span><span className="preview-live"><i /> LIVE</span></div>
          <div className="preview-row"><span className="preview-symbol">↗</span><span>Unusual transaction velocity</span><strong>+35</strong></div>
          <div className="preview-row"><span className="preview-symbol">⌖</span><span>Country change detected</span><strong>+45</strong></div>
          <div className="preview-total"><span>COMPOSITE RISK</span><strong>80 <small>/ 100</small></strong></div>
          <div className="preview-meter"><span /></div>
        </div>
        <div className="login-story-footer"><span>BUILT FOR THE MOMENT THAT MATTERS</span><span>01 — 03</span></div>
      </section>
      <section className="login-panel">
        <form onSubmit={submit} className="login-form">
          <div className="login-mobile-brand">SENTINEL <span>·</span> ANALYST</div>
          <span className="section-kicker">ANALYST PORTAL</span>
          <h2>Welcome back</h2>
          <p className="login-subtitle">Sign in to review your risk queue.</p>
          <label htmlFor="username">Username</label>
          <input id="username" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" placeholder="Enter your username" required />
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            placeholder="Enter your password"
            required
          />
          {error && <p role="alert" className="login-error">{error}</p>}
          <button type="submit" disabled={busy} className="login-submit">
            {busy ? 'Signing in…' : 'Sign in to Sentinel'} <span aria-hidden="true">→</span>
          </button>
          <div className="login-security"><span className="lock-icon" aria-hidden="true">◆</span> Secured analyst access</div>
        </form>
        <footer className="login-panel-footer">SENTINEL RISK INTELLIGENCE <span>·</span> SOUTH AFRICA</footer>
      </section>
    </div>
  );
}
