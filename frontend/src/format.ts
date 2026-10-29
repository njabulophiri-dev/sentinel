import type { AlertStatus, Decision } from './types';

export function formatMoney(amount: number | string, currency: string): string {
  return `${currency} ${Number(amount).toFixed(2)}`;
}

export function decisionLabel(decision: Decision): string {
  switch (decision) {
    case 'APPROVE':
      return 'Approved';
    case 'REVIEW':
      return 'Needs review';
    case 'DECLINE':
      return 'Declined';
  }
}

export function statusLabel(status: AlertStatus): string {
  switch (status) {
    case 'OPEN':
      return 'Open';
    case 'CONFIRMED_FRAUD':
      return 'Confirmed fraud';
    case 'FALSE_POSITIVE':
      return 'False positive';
  }
}

export function formatDateTime(iso: string): string {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('en-ZA');
}
