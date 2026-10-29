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
  }, [filter, page, onError]);

  function changeFilter(next: Filter) {
    setFilter(next);
    setPage(0);
  }

  return (
    <section>
      <div className="toolbar">
        <h2>Alerts</h2>
        <label>
          Show{' '}
          <select value={filter} onChange={(e) => changeFilter(e.target.value as Filter)}>
            <option value="OPEN">Open</option>
            <option value="CONFIRMED_FRAUD">Confirmed fraud</option>
            <option value="FALSE_POSITIVE">False positives</option>
            <option value="ALL">All</option>
          </select>
        </label>
      </div>

      {failed && <p className="error">Could not load alerts.</p>}
      {!data && !failed && <p className="muted">Loading…</p>}
      {data && data.items.length === 0 && <p className="muted">No alerts here. Nothing to review.</p>}

      {data && data.items.length > 0 && (
        <>
          <table>
            <thead>
              <tr>
                <th>Received</th>
                <th>Account</th>
                <th>Amount</th>
                <th>Decision</th>
                <th>Score</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((alert) => (
                <tr key={alert.id} onClick={() => onOpen(alert.id)} tabIndex={0}
                    onKeyDown={(e) => e.key === 'Enter' && onOpen(alert.id)}>
                  <td>{formatDateTime(alert.transaction.receivedAt)}</td>
                  <td>{alert.transaction.accountId}</td>
                  <td>{formatMoney(alert.transaction.amount, alert.transaction.currency)}</td>
                  <td>
                    <span className={`badge ${alert.transaction.decision.toLowerCase()}`}>
                      {decisionLabel(alert.transaction.decision)}
                    </span>
                  </td>
                  <td>{alert.transaction.riskScore}</td>
                  <td>{statusLabel(alert.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pager">
            <button disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </button>
            <span>
              Page {data.page + 1} of {Math.max(1, data.totalPages)}
            </span>
            <button disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>
              Next
            </button>
          </div>
        </>
      )}
    </section>
  );
}
