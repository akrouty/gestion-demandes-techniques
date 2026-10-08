import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { finalize } from 'rxjs';
import { AdministrationApi } from '../administration-api';
import { administrationError, businessRoles, nonBlank } from '../administration-feedback';
import { AdministrationError } from '../utilisateur-models';
@Component({
  selector: 'app-utilisateur-create',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
  ],
  templateUrl: './utilisateur-create.html',
  styleUrl: '../administration.scss',
})
export class UtilisateurCreate {
  private readonly api = inject(AdministrationApi);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  readonly loading = signal(false);
  readonly success = signal('');
  readonly error = signal<AdministrationError | null>(null);
  readonly form = this.fb.nonNullable.group({
    nom: ['', nonBlank],
    email: ['', [nonBlank, Validators.email]],
    actif: true,
    rolesMetier: this.fb.nonNullable.group({ responsable: false, agent: false }),
    password: ['', nonBlank],
  });
  submit(): void {
    if (this.loading()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const body = {
      nom: value.nom.trim(),
      email: value.email.trim(),
      actif: value.actif,
      rolesMetier: businessRoles(value.rolesMetier),
      password: value.password,
    };
    this.loading.set(true);
    this.error.set(null);
    this.success.set('');
    this.form.disable({ emitEvent: false });
    this.api
      .creer(body)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.loading.set(false);
          if (this.form.disabled) this.form.enable({ emitEvent: false });
        }),
      )
      .subscribe({
        next: (user) => {
          this.form.controls.password.reset();
          this.success.set('Utilisateur créé.');
          void this.router.navigate(['/administration/utilisateurs', user.id, 'modifier'], {
            state: { utilisateurCreated: user.id },
          });
        },
        error: (error) => {
          this.form.enable({ emitEvent: false });
          this.error.set(administrationError(error, this.form));
        },
      });
  }
}
