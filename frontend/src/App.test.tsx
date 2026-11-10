// Runtime validation coverage for the analyst sign-in and alert-review flow.
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';

const pendingAlert = {
  id: 41,
  status: 'OPEN',
  createdAt: '2026-11-10T12:00:00Z',
  transaction: {
    id: 'tx-41',
    externalId: 'external-41',
    accountId: 'AC-42',
    merchantId: 'MER-7',
    amount: 12500,
    currency: 'ZAR',
    country: 'ZA',
    decision: 'REVIEW',
    riskScore: 55,
    occurredAt: '2026-11-10T11:59:00Z',
    receivedAt: '2026-11-10T12:00:00Z',
    reasons: [{ ruleId: 'HIGH_AMOUNT', score: 25, reason: 'Amount is above the review threshold.' }],
  },
};

const resolvedAlert = {
  ...pendingAlert,
  status: 'CONFIRMED_FRAUD',
  resolvedBy: 'analyst',
  resolvedAt: '2026-11-10T12:05:00Z',
  note: 'Verified with the cardholder.',
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function page(status: string) {
  const alert = status === 'CONFIRMED_FRAUD' ? resolvedAlert : pendingAlert;
  return { items: [alert], page: 0, size: 20, totalElements: 1, totalPages: 1 };
}

describe('analyst workflow', () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    sessionStorage.clear();
    fetchMock.mockReset();
    fetchMock.mockImplementation(async (input, init) => {
      const url = new URL(String(input), window.location.origin);
      if (url.pathname === '/api/v1/auth/login') {
        const credentials = JSON.parse(String(init?.body)) as { username: string; password: string };
        return credentials.password === 'wrong'
          ? jsonResponse({ detail: 'Too many attempts' }, 429)
          : jsonResponse({ accessToken: 'test-token', tokenType: 'Bearer', expiresInSeconds: 900, role: 'ANALYST' });
      }
      if (url.pathname === '/api/v1/alerts/41/resolution' && init?.method === 'POST') {
        return jsonResponse(resolvedAlert);
      }
      if (url.pathname === '/api/v1/alerts/41') {
        return jsonResponse(pendingAlert);
      }
      if (url.pathname === '/api/v1/alerts') {
        const status = url.searchParams.get('status') ?? 'ALL';
        return jsonResponse(page(status));
      }
      return jsonResponse({ detail: 'Not found' }, 404);
    });
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it('signs in, filters the queue, opens an alert, and records an outcome', async () => {
    render(<App />);

    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'analyst' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'analyst-pass' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in to sentinel/i }));

    expect(await screen.findByRole('heading', { name: 'Risk inbox' })).toBeTruthy();
    expect(await screen.findByRole('button', { name: 'Open alert 41 for account AC-42' })).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Fraud' }));
    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input]) => String(input).includes('status=CONFIRMED_FRAUD'))).toBe(true);
    });

    fireEvent.click(screen.getByRole('button', { name: 'Open alert 41 for account AC-42' }));
    expect(await screen.findByRole('heading', { name: 'Transaction investigation' })).toBeTruthy();
    expect(await screen.findByText('Amount is above the review threshold.')).toBeTruthy();

    fireEvent.change(screen.getByLabelText(/Case note/i), { target: { value: 'Verified with the cardholder.' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm as fraud' }));

    expect(await screen.findByText('Case resolved by analyst')).toBeTruthy();
    const resolutionCall = fetchMock.mock.calls.find(([input, init]) =>
      String(input).includes('/alerts/41/resolution') && init?.method === 'POST',
    );
    expect(resolutionCall).toBeTruthy();
    expect(JSON.parse(String(resolutionCall?.[1]?.body))).toEqual({
      status: 'CONFIRMED_FRAUD',
      note: 'Verified with the cardholder.',
    });
  });

  it('shows a clear message when login is throttled', async () => {
    render(<App />);

    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'analyst' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in to sentinel/i }));

    expect((await screen.findByRole('alert')).textContent).toContain(
      'Too many attempts. Wait a little before trying again.',
    );
  });
});
