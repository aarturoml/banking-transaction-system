import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse } from './models';

const TOKEN_KEY = 'banking_access_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly currentToken = signal<string | null>(localStorage.getItem(TOKEN_KEY));

  constructor(private readonly http: HttpClient, private readonly router: Router) {}

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/login', { email, password }).pipe(
      tap((response) => {
        localStorage.setItem(TOKEN_KEY, response.accessToken);
        this.currentToken.set(response.accessToken);
      })
    );
  }

  register(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/register', { email, password }).pipe(
      tap((response) => {
        localStorage.setItem(TOKEN_KEY, response.accessToken);
        this.currentToken.set(response.accessToken);
      })
    );
  }

  token(): string | null {
    return this.currentToken();
  }

  role(): string | null {
    const token = this.token();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.role ?? null;
    } catch {
      return null;
    }
  }

  email(): string | null {
    const token = this.token();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.sub ?? null;
    } catch {
      return null;
    }
  }

  isAuthenticated(): boolean {
    return !!this.token();
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.currentToken.set(null);
    this.router.navigateByUrl('/login');
  }
}
