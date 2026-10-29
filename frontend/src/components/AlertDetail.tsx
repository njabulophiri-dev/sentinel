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
      <section>
        <button className="link" onClick={onBack}>← Back to alerts</button>
        <p className={message ? 'error' : 'muted'}>{message ?? 'Loading…'}</p>
      </section>
    );
  }

  const tx = alert.transaction;
  return (
    <section>
      <button className="link" onClick={onBack}>← Back to alerts</button>
      <h2>
        Alert #{alert.id} <span className={`badge ${tx.decision.toLowerCase()}`}>{decisionLabel(tx.decision)}</span>
      </h2>

      <dl className="facts">
        <dt>Amount</dt><dd>{formatMoney(tx.amount, tx.currency)}</dd>
        <dt>Account</dt><dd>{tx.accountId}</dd>
        <dt>Merchant</dt><dd>{tx.merchantId}</dd>
        <dt>Country</dt><dd>{tx.country}</dd>
        <dt>Happened</dt><dd>{formatDateTime(tx.occurredAt)}</dd>
        <dt>Risk score</dt><dd>{tx.riskScore} / 100</dd>
        <dt>Status</dt><dd>{statusLabel(alert.status)}</dd>
      </dl>

      <h3>Why it was flagged</h3>
      {tx.reasons.length === 0 ? (
        <p className="muted">No rules fired.</p>
      ) : (
        <ul className="reasons">
          {tx.reasons.map((r) => (
            <li key={r.ruleId}>
              <strong>{r.ruleId}</strong> (+{r.score}): {r.reason}
            </li>
          ))}
        </ul>
      )}

      {alert.status === 'OPEN' ? (
        <div className="resolve card">
          <h3>Your decision</h3>
          <label>
            Note (optional)
            <textarea value={note} onChange={(e) => setNote(e.target.value)} maxLength={500} rows={3} />
          </label>
          <div className="actions">
            <button disabled={busy} onClick={() => resolve('CONFIRMED_FRAUD')} className="danger">
              Confirm fraud
            </button>
            <button disabled={busy} onClick={() => resolve('FALSE_POSITIVE')}>
              Mark as false positive
            </button>
          </div>
        </div>
      ) : (
        <p className="muted">
          Resolved by {alert.resolvedBy}
          {alert.resolvedAt ? ` on ${formatDateTime(alert.resolvedAt)}` : ''}.{alert.note ? ` Note: ${alert.note}` : ''}
        </p>
      )}
      {message && (
        <p role="alert" className="error">
          {message}
        </p>
      )}
    </section>
  );
}
