import { useEffect, useState } from 'react';
import { ApiError, getAlert, resolveAlert } from '../api';
import { decisionLabel, formatDateTime, formatMoney, statusLabel } from '../format';
import type { AlertStatus, AlertView } from '../types';

export function AlertDetail({ id, onBack, onError }: { id: number; onBack: () => void; onError: (e: unknown) => void }) {
  const [alert, setAlert] = useState<AlertView | null>(null);
  const [note, setNote] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let current = true;
    getAlert(id)
      .then((result) => current && setAlert(result))
      .catch((e) => {
        if (current) {
          setMessage('Could not load this alert.');
          onError(e);
        }
      });
    return () => {
      current = false;
    };
  }, [id, onError]);

  async function resolve(outcome: Exclude<AlertStatus, 'OPEN'>) {
    setBusy(true);
    setMessage(null);
    try {
      setAlert(await resolveAlert(id, outcome, note));
    } catch (e) {
      onError(e);
      setMessage(
        e instanceof ApiError && e.status === 409
          ? 'This alert was already resolved by someone else. Reload to see the result.'
          : 'Could not save the decision. Try again.',
      );
    } finally {
      setBusy(false);
    }
  }

  if (!alert) {
    return (
      <section className="page-content detail-page">
        <button className="back-link" onClick={onBack}><span aria-hidden="true">←</span> Back to queue</button>
        {message ? (
          <div className="state-panel error-panel" role="alert">
            <span className="state-icon">!</span>
            <div><strong>Alert unavailable</strong><p>{message}</p></div>
          </div>
        ) : (
          <div className="loading-panel" role="status"><span className="spinner" /> Loading alert…</div>
        )}
      </section>
    );
  }

  const tx = alert.transaction;
  const isOpen = alert.status === 'OPEN';
  return (
    <section className="page-content detail-page">
      <button className="back-link" onClick={onBack}><span aria-hidden="true">←</span> Back to queue</button>
      <div className="detail-heading">
        <div>
          <div className="eyebrow"><span className="eyebrow-line" /> ALERT REVIEW · #{alert.id}</div>
          <h1>Transaction investigation</h1>
          <p>Review the risk signals and record an analyst outcome.</p>
        </div>
        <span className={`badge ${tx.decision.toLowerCase()}`}><span className="badge-dot" />{decisionLabel(tx.decision)}</span>
      </div>

      <div className="detail-layout">
        <div className="detail-main">
          <section className="detail-card transaction-summary">
            <div className="card-heading">
              <div><span className="section-kicker">TRANSACTION OVERVIEW</span><h2>{tx.accountId}</h2></div>
              <span className={`status-label ${alert.status.toLowerCase()}`}>{statusLabel(alert.status)}</span>
            </div>
            <div className="transaction-stats">
              <div className="transaction-stat"><span>AMOUNT</span><strong>{formatMoney(tx.amount, tx.currency)}</strong></div>
              <div className="transaction-stat"><span>MERCHANT</span><strong>{tx.merchantId}</strong></div>
              <div className="transaction-stat"><span>COUNTRY</span><strong>{tx.country}</strong></div>
              <div className="transaction-stat"><span>RECEIVED</span><strong>{formatDateTime(tx.receivedAt)}</strong></div>
            </div>
            <div className="transaction-reference">
              <span>Transaction ID</span><code>{tx.id}</code>
            </div>
          </section>

          <section className="detail-card signals-card">
            <div className="card-heading">
              <div><span className="section-kicker">DECISION CONTEXT</span><h2>Why this was flagged</h2></div>
              <span className="signal-count">{tx.reasons.length} {tx.reasons.length === 1 ? 'signal' : 'signals'}</span>
            </div>
            {tx.reasons.length === 0 ? (
              <div className="inline-empty"><span className="empty-check">✓</span><span>No rules fired for this transaction.</span></div>
            ) : (
              <ul className="reasons">
                {tx.reasons.map((reason, index) => (
                  <li key={reason.ruleId}>
                    <span className="reason-index">{String(index + 1).padStart(2, '0')}</span>
                    <span className="reason-copy"><strong>{reason.ruleId.replace(/_/g, ' ')}</strong><span>{reason.reason}</span></span>
                    <span className="reason-score">+{reason.score}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {message && <div role="alert" className="inline-error">{message}</div>}
          {!isOpen && (
            <section className="resolution-note">
              <span className="resolution-check">✓</span>
              <div>
                <strong>Case resolved by {alert.resolvedBy}</strong>
                <p>{alert.resolvedAt ? formatDateTime(alert.resolvedAt) : 'Resolution time unavailable'}
                  {alert.note ? ` · “${alert.note}”` : ''}
                </p>
              </div>
            </section>
          )}
        </div>

        <aside className="detail-side">
          <section className="risk-card">
            <span className="section-kicker">RISK SCORE</span>
            <div className="risk-number">{tx.riskScore}<span>/100</span></div>
            <div className="risk-meter"><span style={{ width: `${tx.riskScore}%` }} /></div>
            <div className="risk-scale"><span>LOW</span><span>REVIEW</span><span>HIGH</span></div>
            <p>{tx.riskScore >= 80 ? 'High risk · decline recommended' : tx.riskScore >= 40 ? 'Elevated risk · analyst review' : 'Low risk · within normal range'}</p>
          </section>

          {isOpen && (
            <section className="resolve-card">
              <span className="section-kicker">ANALYST ACTION</span>
              <h2>Record an outcome</h2>
              <p>Your decision will be added to the audit trail.</p>
              <label className="note-label" htmlFor="resolution-note">Case note <span>OPTIONAL</span></label>
              <textarea
                id="resolution-note"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                maxLength={500}
                rows={4}
                placeholder="Add context for the next analyst…"
              />
              <div className="character-count">{note.length} / 500</div>
              <button disabled={busy} onClick={() => resolve('CONFIRMED_FRAUD')} className="button-danger">
                {busy ? 'Saving decision…' : 'Confirm as fraud'}
              </button>
              <button disabled={busy} onClick={() => resolve('FALSE_POSITIVE')} className="button-secondary button-full">
                Mark as false positive
              </button>
            </section>
          )}
        </aside>
      </div>
    </section>
  );
}
