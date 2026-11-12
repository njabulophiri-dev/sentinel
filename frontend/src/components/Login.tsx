import { useState, type FormEvent } from 'react';
import { ApiError, login, type Session } from '../api';
import { ArrowRightIcon } from './Icons';

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
        <svg className="login-illustration" viewBox="0 0 1000 1600" preserveAspectRatio="xMidYMin slice" aria-hidden="true">
          <defs>
            <linearGradient id="story-background" x1="0" y1="0" x2="1" y2="1">
              <stop stopColor="#0b211f" />
              <stop offset=".52" stopColor="#0d3430" />
              <stop offset="1" stopColor="#09201e" />
            </linearGradient>
            <radialGradient id="story-light">
              <stop stopColor="#d2a74e" stopOpacity=".62" />
              <stop offset="1" stopColor="#d2a74e" stopOpacity="0" />
            </radialGradient>
            <filter id="soft-blur"><feGaussianBlur stdDeviation="30" /></filter>
            <filter id="phone-glow"><feGaussianBlur stdDeviation="11" /></filter>
          </defs>
          <path fill="url(#story-background)" d="M0 0h1000v1600H0z" />
          <g filter="url(#soft-blur)" opacity=".8">
            <circle cx="158" cy="320" r="95" fill="url(#story-light)" />
            <circle cx="800" cy="260" r="135" fill="#08786b" opacity=".34" />
            <circle cx="90" cy="800" r="82" fill="#8b8b39" opacity=".28" />
            <circle cx="684" cy="1040" r="180" fill="#1d8b8a" opacity=".25" />
          </g>
          <g transform="rotate(-12 750 1190)">
            <rect x="630" y="945" width="205" height="515" rx="38" fill="#53c2b9" opacity=".38" filter="url(#phone-glow)" />
            <rect x="640" y="950" width="190" height="495" rx="36" fill="#142326" stroke="#334343" strokeWidth="8" />
            <rect x="657" y="970" width="156" height="452" rx="23" fill="#1c2a2b" />
            <circle cx="735" cy="1000" r="18" fill="#263538" />
          </g>
        </svg>
        <div className="login-hero-overlay" />
        <a className="brand login-brand" href="/" aria-label="Sentinel">
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" fill="none">
              <path d="M16 3 27 7v8c0 7.3-4.7 12.1-11 14C9.7 27.1 5 22.3 5 15V7l11-4Z" />
              <path d="m11 16 3.2 3.2L21.5 12" />
            </svg>
          </span>
          <span><strong>sentinel</strong><small>RISK INTELLIGENCE</small></span>
        </a>
        <div className="login-notifications" aria-hidden="true">
          <article className="notification-card">
            <span className="notification-icon"><svg viewBox="0 0 24 24" fill="none"><path d="M12 3 19 6v5c0 4.7-3 7.8-7 9-4-1.2-7-4.3-7-9V6l7-3Z" /><path d="m9 12 2 2 4-4" /></svg></span>
            <span className="notification-copy"><strong>SCAM ALERT</strong><span>Transaction declined: R 8 200,00 at MER-ONLINE. Risk score 85/100.</span></span>
            <time>now</time>
          </article>
          <article className="notification-card">
            <span className="notification-icon"><svg viewBox="0 0 24 24" fill="none"><path d="M12 3 19 6v5c0 4.7-3 7.8-7 9-4-1.2-7-4.3-7-9V6l7-3Z" /><path d="m9 12 2 2 4-4" /></svg></span>
            <span className="notification-copy"><strong>COUNTRY CHANGE DETECTED</strong><span>Card used in GB minutes after Johannesburg.</span></span>
            <time>2m ago</time>
          </article>
          <article className="notification-card">
            <span className="notification-icon"><svg viewBox="0 0 24 24" fill="none"><path d="M12 3 19 6v5c0 4.7-3 7.8-7 9-4-1.2-7-4.3-7-9V6l7-3Z" /><path d="m9 12 2 2 4-4" /></svg></span>
            <span className="notification-copy"><strong>UNUSUAL TRANSACTION VELOCITY</strong><span>6 payments in 2 minutes. Review recommended.</span></span>
            <time>5m ago</time>
          </article>
        </div>
      </section>
      <section className="login-panel">
        <form onSubmit={submit} className="login-form">
          <div className="login-mobile-brand">SENTINEL <span>·</span> ANALYST</div>
          <span className="section-kicker">ANALYST PORTAL</span>
          <h2>Welcome back</h2>
          <p className="login-subtitle">Sign in to review your risk queue.</p>
          <label htmlFor="username">Username</label>
          <input id="username" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" placeholder="analyst" required />
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
            {busy ? 'Signing in…' : 'Sign In'} <span className="login-submit-icon"><ArrowRightIcon /></span>
          </button>
        </form>
      </section>
    </div>
  );
}
