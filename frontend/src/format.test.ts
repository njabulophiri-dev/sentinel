import { describe, expect, it } from 'vitest';
import { decisionLabel, formatDateTime, formatMoney, statusLabel } from './format';

describe('format', () => {
  it('always shows two decimal places', () => {
    expect(formatMoney(7500, 'ZAR')).toBe('ZAR 7500.00');
    expect(formatMoney('199.9', 'ZAR')).toBe('ZAR 199.90');
  });

  it('labels every decision in plain words', () => {
    expect(decisionLabel('APPROVE')).toBe('Approved');
    expect(decisionLabel('REVIEW')).toBe('Needs review');
    expect(decisionLabel('DECLINE')).toBe('Declined');
  });

  it('labels every alert status', () => {
    expect(statusLabel('OPEN')).toBe('Open');
    expect(statusLabel('CONFIRMED_FRAUD')).toBe('Confirmed fraud');
    expect(statusLabel('FALSE_POSITIVE')).toBe('False positive');
  });

  it('falls back to the raw text for a date it cannot read', () => {
    expect(formatDateTime('not a date')).toBe('not a date');
  });
});
