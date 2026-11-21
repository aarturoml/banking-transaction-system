export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface Account {
  id: number;
  ownerEmail: string;
  currency: string;
  balance: number;
  createdAt: string;
}

export interface Transfer {
  id: number;
  sourceAccountId: number | null;
  destinationAccountId: number | null;
  amount: number;
  currency: string;
  status: string;
  createdAt: string;
}

export interface LedgerEntry {
  id: number;
  accountId: number;
  transferId: number | null;
  direction: 'CREDIT' | 'DEBIT';
  amount: number;
  createdAt: string;
}

export interface LedgerPage {
  content: LedgerEntry[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
