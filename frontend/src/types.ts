export type Decision = 'APPROVE' | 'REVIEW' | 'DECLINE';
export type AlertStatus = 'OPEN' | 'CONFIRMED_FRAUD' | 'FALSE_POSITIVE';

export interface Reason {
  ruleId: string;
  score: number;
  reason: string;
}

export interface TransactionView {
  id: string;
  externalId: string;
  accountId: string;
  merchantId: string;
  amount: number;
  currency: string;
  country: string;
  decision: Decision;
  riskScore: number;
  occurredAt: string;
  receivedAt: string;
  reasons: Reason[];
}

export interface AlertView {
  id: number;
  status: AlertStatus;
  createdAt: string;
  resolvedBy?: string;
  resolvedAt?: string;
  note?: string;
  transaction: TransactionView;
}

export interface PageView<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  role: string;
}
