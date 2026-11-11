import { useEffect, useState } from 'react';
import { listAlerts } from '../api';
import { decisionLabel, formatDateTime, formatMoney, statusLabel } from '../format';
import type { AlertStatus, AlertView, PageView } from '../types';

type Filter = AlertStatus | 'ALL';

export function AlertList({ onOpen, onError }: { onOpen: (id: number) => void; onError: (e: unknown) => void }) {
  const [filter, setFilter] = useState<Filter>('OPEN');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageView<AlertView> | null>(null);
  const [failed, setFailed] = useState(false);
  const [refresh, setRefresh] = useState(0);

  useEffect(() => {
    let current = true;
    setData(null);
    setFailed(false);
    listAlerts(filter, page)
      .then((result) => current && setData(result))
      .catch((e) => {
        if (current) {
          setFailed(true);
          onError(e);
        }
      });
    return () => {
      current = false; // ignore a slow response that arrives after the user changed the filter
    };
  }, [filter, page, refresh, onError]);

  function changeFilter(next: Filter) {
    setFilter(next);
    setPage(0);
  }

  const pageItems = data?.items ?? [];
  const highRiskCount = pageItems.filter((alert) => alert.transaction.riskScore >= 80).length;
  const averageRisk = pageItems.length
    ? Math.round(pageItems.reduce((total, alert) => total + alert.transaction.riskScore, 0) / pageItems.length)
    : 0;
  const firstItem = data && data.totalElements > 0 ? data.page * data.size + 1 : 0;
  const lastItem = data ? Math.min((data.page + 1) * data.size, data.totalElements) : 0;

  return (
    <section className="page-content">
      <div className="page-heading">
        <div>
          <div className="eyebrow"><span className="eyebrow-line" /> FRAUD OPERATIONS</div>
          <h1>Risk inbox</h1>
          <p>Review flagged transactions and make confident decisions.</p>
        </div>
        <div className="live-indicator"><span className="status-dot" /> LIVE QUEUE</div>
      </div>

      <div className="metric-grid">
        <article className="metric-card">
          <div className="metric-label">ALERTS IN THIS VIEW</div>
          <div className="metric-value">{data ? data.totalElements : '—'}</div>
          <div className="metric-note">Filtered queue total</div>
        </article>
        <article className="metric-card metric-card-warm">
          <div className="metric-label">HIGH RISK · THIS PAGE</div>
          <div className="metric-value">{data ? highRiskCount.toString().padStart(2, '0') : '—'}</div>
          <div className="metric-note">Score of 80 or above</div>
        </article>
        <article className="metric-card">
          <div className="metric-label">AVERAGE RISK · THIS PAGE</div>
          <div className="metric-value">{data ? averageRisk : '—'}{data && <span className="metric-out-of"> / 100</span>}</div>
          <div className="metric-note">Across visible alerts</div>
        </article>
      </div>

      <div className="queue-panel">
        <div className="queue-toolbar">
          <div>
            <h2>Transaction alerts</h2>
            <p>Prioritised for analyst review</p>
          </div>
          <div className="filter-tabs" role="group" aria-label="Filter alerts">
            {([
              ['OPEN', 'Open'],
              ['CONFIRMED_FRAUD', 'Fraud'],
              ['FALSE_POSITIVE', 'Cleared'],
              ['ALL', 'All alerts'],
            ] as const).map(([value, label]) => (
              <button
                key={value}
                className={`filter-tab ${filter === value ? 'selected' : ''}`}
                aria-pressed={filter === value}
                onClick={() => changeFilter(value)}
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        {failed && (
          <div className="state-panel error-panel" role="alert">
            <span className="state-icon">!</span>
            <div><strong>We couldn’t load the queue</strong><p>Check your connection and try again.</p></div>
            <button className="button-secondary" onClick={() => setRefresh((value) => value + 1)}>Try again</button>
          </div>
        )}
        {!data && !failed && (
          <div className="loading-panel" role="status"><span className="spinner" /> Loading alerts…</div>
        )}
        {data && data.items.length === 0 && (
          <div className="state-panel empty-panel">
            <span className="empty-check" aria-hidden="true">✓</span>
            <div><strong>You’re all caught up</strong><p>No alerts in this view right now.</p></div>
          </div>
        )}

        {data && data.items.length > 0 && (
          <>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>TRANSACTION</th>
                    <th>RECEIVED</th>
                    <th>AMOUNT</th>
                    <th>DECISION</th>
                    <th>RISK SCORE</th>
                    <th>STATUS</th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((alert) => (
                    <tr key={alert.id}>
                      <td>
                        <div className="transaction-cell">
                          <button
                            className="transaction-icon"
                            onClick={() => onOpen(alert.id)}
                            aria-label={`Open alert ${alert.id} for account ${alert.transaction.accountId}`}
                          >
                            ↗
                          </button>
                          <span>
                            <span className="transaction-name">{alert.transaction.accountId}</span>
                            <small>Alert #{alert.id}</small>
                          </span>
                        </div>
                      </td>
                      <td><span className="date-cell">{formatDateTime(alert.transaction.receivedAt)}</span></td>
                      <td><strong className="amount-cell">{formatMoney(alert.transaction.amount, alert.transaction.currency)}</strong></td>
                      <td>
                        <span className={`badge ${alert.transaction.decision.toLowerCase()}`}>
                          <span className="badge-dot" />{decisionLabel(alert.transaction.decision)}
                        </span>
                      </td>
                      <td>
                        <div className="score-cell">
                          <span>{alert.transaction.riskScore}</span>
                          <span className="score-track"><span style={{ width: `${alert.transaction.riskScore}%` }} /></span>
                        </div>
                      </td>
                      <td><span className={`status-label ${alert.status.toLowerCase()}`}>{statusLabel(alert.status)}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="pager">
              <span className="pager-count">Showing <strong>{firstItem}–{lastItem}</strong> of <strong>{data.totalElements}</strong></span>
              <div className="pager-controls">
                <button className="button-secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
                <span>Page {data.page + 1} of {Math.max(1, data.totalPages)}</span>
                <button className="button-secondary" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</button>
              </div>
            </div>
          </>
        )}
      </div>
    </section>
  );
}
