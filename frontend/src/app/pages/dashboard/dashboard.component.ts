import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BankingApiService } from '../../core/banking-api.service';
import { AuthService } from '../../core/auth.service';
import { Account, LedgerEntry } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  private readonly fb = inject(FormBuilder);
  readonly accounts = signal<Account[]>([]);
  readonly selectedAccount = signal<Account | null>(null);
  readonly entries = signal<LedgerEntry[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  readonly accountForm = this.fb.nonNullable.group({ currency: ['USD', Validators.required] });
  readonly transferForm = this.fb.nonNullable.group({
    sourceAccountId: [0, [Validators.required, Validators.min(1)]],
    destinationAccountId: [0, [Validators.required, Validators.min(1)]],
    amount: [25, [Validators.required, Validators.min(0.0001)]],
    currency: ['USD', Validators.required]
  });
  readonly depositForm = this.fb.nonNullable.group({
    accountId: [0, [Validators.required, Validators.min(1)]],
    amount: [100, [Validators.required, Validators.min(0.0001)]]
  });

  constructor(private readonly api: BankingApiService, readonly auth: AuthService) { this.refresh(); }
  get isAdmin(): boolean { return this.auth.role() === 'ADMIN'; }
  get currencyTotals(): { currency: string; balance: number }[] {
    const totals = new Map<string, number>();
    for (const account of this.accounts()) totals.set(account.currency, (totals.get(account.currency) ?? 0) + account.balance);
    return [...totals.entries()].map(([currency, balance]) => ({ currency, balance }));
  }
  get primaryBalance(): { currency: string; balance: number } | null {
    return this.currencyTotals[0] ?? null;
  }
  formatCurrency(currency: string): string {
    return currency === 'USD' ? '$' : currency === 'EUR' ? '€' : '£';
  }

  refresh(): void {
    this.loading.set(true);
    this.api.accounts().subscribe({
      next: (accounts) => { this.accounts.set(accounts); if (!this.selectedAccount() && accounts.length) this.selectAccount(accounts[0]); this.loading.set(false); },
      error: (error) => this.showError(error)
    });
  }
  selectAccount(account: Account): void {
    this.selectedAccount.set(account);
    this.depositForm.controls.accountId.setValue(account.id);
    this.api.ledger(account.id).subscribe({ next: (page) => this.entries.set(page.content), error: (error) => this.showError(error) });
  }
  createAccount(): void {
    if (this.accountForm.invalid) return;
    this.busy.set(true);
    this.api.createAccount(this.accountForm.controls.currency.value).subscribe({
      next: () => { this.showMessage('Cuenta creada correctamente.'); this.refresh(); },
      error: (error) => this.showError(error),
      complete: () => this.busy.set(false)
    });
  }
  deposit(): void {
    if (this.depositForm.invalid) return;
    this.busy.set(true);
    const { accountId, amount } = this.depositForm.getRawValue();
    this.api.deposit(accountId, amount).subscribe({
      next: () => { this.showMessage('Depósito simulado aplicado.'); this.refresh(); },
      error: (error) => this.showError(error),
      complete: () => this.busy.set(false)
    });
  }
  transfer(): void {
    if (this.transferForm.invalid) return;
    this.busy.set(true);
    const value = this.transferForm.getRawValue();
    this.api.transfer(value.sourceAccountId, value.destinationAccountId, value.amount, value.currency, `frontend-${Date.now()}`).subscribe({
      next: () => { this.showMessage('Transferencia completada.'); this.refresh(); },
      error: (error) => this.showError(error),
      complete: () => this.busy.set(false)
    });
  }
  logout(): void { this.auth.logout(); }
  private showMessage(message: string): void { this.error.set(''); this.message.set(message); setTimeout(() => this.message.set(''), 4000); }
  private showError(error: any): void { this.loading.set(false); this.busy.set(false); this.error.set(error?.error?.message ?? 'No se pudo completar la operación.'); }
}
