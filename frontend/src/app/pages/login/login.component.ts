import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  readonly registerMode = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  constructor(private readonly auth: AuthService, private readonly router: Router) {}

  toggleMode(): void { this.registerMode.update((value) => !value); this.error.set(''); }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set('');
    const { email, password } = this.form.getRawValue();
    const request = this.registerMode() ? this.auth.register(email, password) : this.auth.login(email, password);
    request.subscribe({
      next: () => this.router.navigateByUrl('/dashboard'),
      error: (error) => { this.loading.set(false); this.error.set(error.error?.message ?? 'No se pudo completar la solicitud.'); },
      complete: () => this.loading.set(false)
    });
  }
}
