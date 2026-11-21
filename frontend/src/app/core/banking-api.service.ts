import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Account, LedgerPage, Transfer } from './models';

@Injectable({ providedIn: 'root' })
export class BankingApiService {
  constructor(private readonly http: HttpClient) {}

  accounts() {
    return this.http.get<Account[]>('/api/v1/accounts');
  }

  createAccount(currency: string) {
    return this.http.post<Account>('/api/v1/accounts', { currency });
  }

  deposit(accountId: number, amount: number) {
    return this.http.post<Transfer>('/api/v1/simulations/deposits', { accountId, amount });
  }

  transfer(sourceAccountId: number, destinationAccountId: number, amount: number, currency: string, key: string) {
    return this.http.post<Transfer>(
      '/api/v1/transfers',
      { sourceAccountId, destinationAccountId, amount, currency },
      { headers: { 'Idempotency-Key': key } }
    );
  }

  ledger(accountId: number, page = 0, size = 20) {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');
    return this.http.get<LedgerPage>(`/api/v1/accounts/${accountId}/transactions`, { params });
  }
}
