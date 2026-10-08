import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { Subject, catchError, finalize, of, skip, startWith, switchMap, takeUntil } from 'rxjs';
import { PageState } from '../../../shared/ui/page-state/page-state';
import { AdministrationApi } from '../administration-api';
import { administrationError, businessRoles, nonBlank } from '../administration-feedback';
import { AdministrationError, UtilisateurDetailResponse, roleLabels } from '../utilisateur-models';
@Component({
  selector: 'app-utilisateur-edit',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    PageState,
  ],
  templateUrl: './utilisateur-edit.html',
  styleUrl: '../administration.scss',
})
export class UtilisateurEdit {
  private readonly api = inject(AdministrationApi);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly reloadRequest = new Subject<void>();
  readonly roleLabels = roleLabels;
  readonly user = signal<UtilisateurDetailResponse | null>(null);
  readonly loading = signal(true);
  readonly pageError = signal<AdministrationError | null>(null);
  readonly pageStatus = signal<number | null>(null);
  readonly pageSuccess = signal('');
  readonly identityBusy = signal(false);
  readonly stateBusy = signal(false);
  readonly rolesBusy = signal(false);
  readonly identityError = signal<AdministrationError | null>(null);
  readonly stateError = signal<AdministrationError | null>(null);
  readonly rolesError = signal<AdministrationError | null>(null);
  readonly identitySuccess = signal('');
  readonly stateSuccess = signal('');
  readonly rolesSuccess = signal('');
  readonly identityReload = signal(false);
  readonly stateReload = signal(false);
  readonly rolesReload = signal(false);
  readonly identity = this.fb.nonNullable.group({
    nom: ['', nonBlank],
    email: ['', [nonBlank, Validators.email]],
  });
  readonly rolesForm = this.fb.nonNullable.group({
    rolesMetier: this.fb.nonNullable.group({ responsable: false, agent: false }),
  });
  readonly confirmDeactivate = new FormControl(false, {
    nonNullable: true,
    validators: Validators.requiredTrue,
  });
  constructor() {
    let created = inject(Router).getCurrentNavigation()?.extras.state?.['utilisateurCreated'];
    this.route.paramMap
      .pipe(
        switchMap((params) =>
          this.reloadRequest.pipe(
            startWith(undefined),
            switchMap(() => {
              this.loading.set(true);
              this.user.set(null);
              this.pageError.set(null);
              this.pageStatus.set(null);
              this.pageSuccess.set('');
              this.identityError.set(null);
              this.stateError.set(null);
              this.rolesError.set(null);
              this.identitySuccess.set('');
              this.stateSuccess.set('');
              this.rolesSuccess.set('');
              this.identityReload.set(false);
              this.stateReload.set(false);
              this.rolesReload.set(false);
              const id = Number(params.get('id'));
              if (!Number.isSafeInteger(id) || id < 1) {
                this.pageStatus.set(404);
                this.pageError.set({ code: '', message: 'Utilisateur introuvable.' });
                return of(null);
              }
              return this.api.obtenir(id).pipe(
                catchError((error) => {
                  this.pageError.set(administrationError(error));
                  this.pageStatus.set(error instanceof HttpErrorResponse ? error.status : null);
                  return of(null);
                }),
              );
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((user) => {
        this.loading.set(false);
        if (!user) return;
        this.user.set(user);
        this.identity.reset({ nom: user.nom, email: user.email });
        this.resetRoles(user);
        this.confirmDeactivate.reset(false);
        if (created === user.id) {
          this.pageSuccess.set('Utilisateur créé.');
          created = undefined;
        }
      });
  }
  hasAdministrator(): boolean {
    return this.user()?.roles.includes('ADMINISTRATEUR') ?? false;
  }
  anyBusy(): boolean {
    return this.identityBusy() || this.stateBusy() || this.rolesBusy();
  }
  reload(): void {
    if (!this.anyBusy()) this.reloadRequest.next();
  }
  saveIdentity(): void {
    const user = this.user();
    if (!user || this.identityBusy() || this.identityReload()) return;
    if (this.identity.invalid) {
      this.identity.markAllAsTouched();
      return;
    }
    const value = this.identity.getRawValue();
    this.identityBusy.set(true);
    this.identityError.set(null);
    this.identitySuccess.set('');
    this.identity.disable({ emitEvent: false });
    this.api
      .modifier(user.id, { nom: value.nom.trim(), email: value.email.trim() })
      .pipe(
        takeUntil(this.route.paramMap.pipe(skip(1))),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.identityBusy.set(false);
          if (this.identity.disabled) this.identity.enable({ emitEvent: false });
        }),
      )
      .subscribe({
        next: (response) => {
          // Les réponses contiennent tout le DTO : n’appliquer que la section modifiée
          // évite d’écraser le résultat d’une autre sauvegarde concurrente.
          this.user.update((current) =>
            current ? { ...current, nom: response.nom, email: response.email } : current,
          );
          this.identity.reset({ nom: response.nom, email: response.email });
          this.identitySuccess.set('Nom et email enregistrés.');
        },
        error: (error) => {
          this.identity.enable({ emitEvent: false });
          this.identityError.set(administrationError(error, this.identity));
          this.identityReload.set(this.requiresReload(error));
          this.handleMissing(error);
        },
      });
  }
  changeState(): void {
    const user = this.user();
    if (!user || this.stateBusy() || this.stateReload()) return;
    if (user.actif && this.confirmDeactivate.invalid) {
      this.confirmDeactivate.markAsTouched();
      return;
    }
    this.stateBusy.set(true);
    this.stateError.set(null);
    this.stateSuccess.set('');
    this.confirmDeactivate.disable({ emitEvent: false });
    const request = user.actif ? this.api.desactiver(user.id) : this.api.activer(user.id);
    request
      .pipe(
        takeUntil(this.route.paramMap.pipe(skip(1))),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.stateBusy.set(false);
          this.confirmDeactivate.enable({ emitEvent: false });
        }),
      )
      .subscribe({
        next: (response) => {
          this.user.update((current) =>
            current ? { ...current, actif: response.actif } : current,
          );
          this.confirmDeactivate.reset(false);
          this.stateSuccess.set(response.actif ? 'Compte activé.' : 'Compte désactivé.');
        },
        error: (error) => {
          this.stateError.set(administrationError(error));
          this.stateReload.set(this.requiresReload(error));
          this.handleMissing(error);
        },
      });
  }
  saveRoles(): void {
    const user = this.user();
    if (!user || this.rolesBusy() || this.rolesReload() || this.rolesForm.invalid) return;
    const rolesMetier = businessRoles(this.rolesForm.getRawValue().rolesMetier);
    this.rolesBusy.set(true);
    this.rolesError.set(null);
    this.rolesSuccess.set('');
    this.rolesForm.disable({ emitEvent: false });
    this.api
      .remplacerRolesMetier(user.id, { rolesMetier })
      .pipe(
        takeUntil(this.route.paramMap.pipe(skip(1))),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.rolesBusy.set(false);
          if (this.rolesForm.disabled) this.rolesForm.enable({ emitEvent: false });
        }),
      )
      .subscribe({
        next: (response) => {
          this.user.update((current) =>
            current ? { ...current, roles: response.roles } : current,
          );
          this.resetRoles(response);
          this.rolesSuccess.set('Rôles métier enregistrés.');
        },
        error: (error) => {
          this.rolesForm.enable({ emitEvent: false });
          this.rolesError.set(administrationError(error, this.rolesForm));
          this.rolesReload.set(this.requiresReload(error));
          this.handleMissing(error);
        },
      });
  }
  private resetRoles(user: UtilisateurDetailResponse): void {
    this.rolesForm.reset({
      rolesMetier: {
        responsable: user.roles.includes('RESPONSABLE_TECHNIQUE'),
        agent: user.roles.includes('AGENT_TECHNIQUE'),
      },
    });
  }
  private requiresReload(error: unknown): boolean {
    return (
      error instanceof HttpErrorResponse &&
      (error.status === 0 || error.status >= 500 || error.error?.code === 'CONFLIT')
    );
  }
  private handleMissing(error: unknown): void {
    if (error instanceof HttpErrorResponse && error.status === 404) {
      this.user.set(null);
      this.pageStatus.set(404);
      this.pageError.set(administrationError(error));
    }
  }
}
