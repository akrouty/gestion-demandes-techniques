import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe, KeyValuePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import {
  Observable,
  Subject,
  catchError,
  finalize,
  of,
  skip,
  startWith,
  switchMap,
  takeUntil,
} from 'rxjs';
import { SessionService } from '../../../core/session/session';
import { DemandesApi } from '../demandes-api';
import { apiError, nonBlank } from '../demande-feedback';
import {
  AgentAssignableResponse,
  ApiError,
  Categorie,
  DemandeDetailResponse,
  Priorite,
  TraitementDemandeRequest,
  categories,
  priorites,
  statuts,
} from '../demande-models';
@Component({
  selector: 'app-demande-detail',
  imports: [
    DatePipe,
    KeyValuePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
  ],
  templateUrl: './demande-detail.html',
  styleUrl: '../demandes.scss',
})
export class DemandeDetail {
  private readonly api = inject(DemandesApi);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly reloadRequest = new Subject<void>();
  readonly session = inject(SessionService);
  readonly categories = categories;
  readonly priorites = priorites;
  readonly statuts = statuts;
  readonly demande = signal<DemandeDetailResponse | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal<ApiError | null>(null);
  readonly success = signal('');
  readonly status = signal<number | null>(null);
  readonly reloadRequired = signal(false);
  readonly agents = signal<AgentAssignableResponse[] | null>(null);
  readonly agentsLoading = signal(false);
  readonly agentsError = signal<ApiError | null>(null);
  readonly qualification = this.fb.nonNullable.group({
    categorie: this.fb.control<Categorie | null>(null, Validators.required),
    priorite: this.fb.control<Priorite | null>(null, Validators.required),
  });
  readonly affectation = this.fb.group({
    agentId: this.fb.control<number | null>(null, Validators.required),
  });
  readonly traitement = this.fb.nonNullable.group({ descriptionTraitement: '', solution: '' });
  readonly annulation = this.fb.nonNullable.group({
    motif: ['', nonBlank],
    confirme: [false, Validators.requiredTrue],
  });
  constructor() {
    const created = inject(Router).getCurrentNavigation()?.extras.state?.['demandeCreated'];
    this.route.paramMap
      .pipe(
        switchMap((params) =>
          this.reloadRequest.pipe(
            startWith(undefined),
            switchMap(() => {
              this.loading.set(true);
              this.demande.set(null);
              this.error.set(null);
              this.status.set(null);
              this.reloadRequired.set(false);
              this.agents.set(null);
              this.agentsError.set(null);
              return this.api.detail(params.get('reference') ?? '').pipe(
                catchError((error) => {
                  this.error.set(apiError(error));
                  this.status.set(error instanceof HttpErrorResponse ? error.status : null);
                  return of(null);
                }),
              );
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((detail) => {
        this.loading.set(false);
        if (detail) {
          this.adopt(detail);
          if (created === detail.reference) this.success.set('Demande créée.');
        }
      });
  }
  // Indices de présentation uniquement. Le backend autorise les rôles actuels et l’Agent affecté.
  rt(): boolean {
    return this.session.hasAnyRole(['RESPONSABLE_TECHNIQUE']);
  }
  at(): boolean {
    return this.session.hasAnyRole(['AGENT_TECHNIQUE']);
  }
  active(): boolean {
    return ['NOUVELLE', 'ASSIGNEE', 'EN_COURS'].includes(this.demande()?.statut ?? '');
  }
  qualifiable(): boolean {
    return !!this.demande() && !['CLOTUREE', 'ANNULEE'].includes(this.demande()!.statut);
  }
  treatmentChanged(): boolean {
    const detail = this.demande();
    const value = this.traitement.getRawValue();
    return (
      !!detail &&
      (value.descriptionTraitement !== (detail.descriptionTraitement ?? '') ||
        value.solution !== (detail.solution ?? ''))
    );
  }
  canResolve(): boolean {
    return (
      this.at() &&
      this.demande()?.statut === 'EN_COURS' &&
      !!this.demande()?.solution?.trim() &&
      !this.treatmentChanged()
    );
  }
  qualify(): void {
    if (!this.rt() || !this.qualifiable() || this.qualification.invalid) {
      this.qualification.markAllAsTouched();
      return;
    }
    const { categorie, priorite } = this.qualification.getRawValue();
    if (categorie && priorite)
      this.mutate(
        this.api.qualify(this.demande()!.reference, { categorie, priorite }),
        'Qualification enregistrée.',
        this.qualification,
      );
  }
  loadAgents(): void {
    if (!this.rt() || !this.active() || this.agentsLoading()) return;
    this.agentsLoading.set(true);
    this.agentsError.set(null);
    this.api
      .agents()
      .pipe(
        takeUntil(this.route.paramMap.pipe(skip(1))),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.agentsLoading.set(false)),
      )
      .subscribe({
        next: (agents) => this.agents.set(agents),
        error: (error) => this.agentsError.set(apiError(error)),
      });
  }
  assign(): void {
    if (!this.rt() || !this.active() || this.affectation.invalid) {
      this.affectation.markAllAsTouched();
      return;
    }
    const id = this.affectation.getRawValue().agentId;
    if (id !== null)
      this.mutate(
        this.api.assign(this.demande()!.reference, id),
        'Affectation enregistrée.',
        this.affectation,
      );
  }
  start(): void {
    if (this.at() && this.demande()?.statut === 'ASSIGNEE')
      this.mutate(this.api.start(this.demande()!.reference), 'Traitement démarré.');
  }
  saveTreatment(): void {
    const detail = this.demande();
    if (!this.at() || detail?.statut !== 'EN_COURS') return;
    const value = this.traitement.getRawValue();
    const body: TraitementDemandeRequest = {};
    for (const field of ['descriptionTraitement', 'solution'] as const) {
      this.traitement.controls[field].setErrors(null);
      if (value[field] !== (detail[field] ?? '')) {
        if (!value[field].trim()) {
          this.traitement.controls[field].setErrors({ required: true });
          this.traitement.controls[field].markAsTouched();
        } else body[field] = value[field].trim();
      }
    }
    if (this.traitement.invalid) {
      this.error.set({
        code: '',
        message:
          'Un champ modifié doit être non vide. L’effacement du traitement ou de la solution n’est pas prévu.',
      });
      return;
    }
    if (!Object.keys(body).length) return;
    this.mutate(
      this.api.treatment(detail.reference, body),
      'Traitement enregistré.',
      this.traitement,
    );
  }
  resolve(): void {
    if (this.canResolve())
      this.mutate(this.api.resolve(this.demande()!.reference), 'Résolution enregistrée.');
  }
  close(): void {
    if (this.rt() && this.demande()?.statut === 'RESOLUE')
      this.mutate(this.api.close(this.demande()!.reference), 'Demande clôturée.');
  }
  refuse(): void {
    if (this.rt() && this.demande()?.statut === 'RESOLUE')
      this.mutate(this.api.refuse(this.demande()!.reference), 'Résolution refusée.');
  }
  cancel(): void {
    if (!this.rt() || !this.active() || this.annulation.invalid) {
      this.annulation.markAllAsTouched();
      return;
    }
    this.mutate(
      this.api.cancel(this.demande()!.reference, this.annulation.getRawValue().motif.trim()),
      'Demande annulée.',
      this.annulation,
    );
  }
  reload(): void {
    if (!this.busy()) {
      this.success.set('');
      this.reloadRequest.next();
    }
  }
  private adopt(detail: DemandeDetailResponse): void {
    this.demande.set(detail);
    this.qualification.reset({ categorie: detail.categorie, priorite: detail.priorite });
    this.affectation.reset({ agentId: detail.agentAffecte?.id ?? null });
    this.traitement.reset({
      descriptionTraitement: detail.descriptionTraitement ?? '',
      solution: detail.solution ?? '',
    });
    this.annulation.reset({ motif: '', confirme: false });
  }
  private mutate(
    request: Observable<DemandeDetailResponse>,
    message: string,
    form?: FormGroup,
  ): void {
    if (this.busy() || this.loading() || this.reloadRequired()) return;
    this.busy.set(true);
    this.error.set(null);
    this.success.set('');
    this.status.set(null);
    const forms = [this.qualification, this.affectation, this.traitement, this.annulation];
    forms.forEach((group) => group.disable({ emitEvent: false }));
    request
      .pipe(
        takeUntil(this.route.paramMap.pipe(skip(1))),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.busy.set(false);
          forms.forEach((group) => {
            if (group.disabled) group.enable({ emitEvent: false });
          });
        }),
      )
      .subscribe({
        next: (detail) => {
          this.adopt(detail);
          this.success.set(message);
        },
        error: (error) => {
          // Réactiver avant de rattacher les erreurs serveur : enable() recalculerait leur validité.
          forms.forEach((group) => group.enable({ emitEvent: false }));
          this.error.set(apiError(error, form));
          const status = error instanceof HttpErrorResponse ? error.status : 0;
          this.status.set(status);
          this.reloadRequired.set(status === 409 || status === 0 || status >= 500);
          if (status === 404) this.demande.set(null);
        },
      });
  }
}
