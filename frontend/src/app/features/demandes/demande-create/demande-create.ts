import { Component, DestroyRef, inject, signal } from '@angular/core';
import { KeyValuePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import {
  Subject,
  catchError,
  debounceTime,
  distinctUntilChanged,
  finalize,
  of,
  switchMap,
} from 'rxjs';
import { SessionService } from '../../../core/session/session';
import { DemandesApi } from '../demandes-api';
import { apiError, frenchPaginator, nonBlank } from '../demande-feedback';
import {
  ApiError,
  Categorie,
  ClientSummaryResponse,
  CreationDemandeRequest,
  PageResponse,
  Priorite,
  categories,
  priorites,
} from '../demande-models';
@Component({
  selector: 'app-demande-create',
  providers: [{ provide: MatPaginatorIntl, useFactory: frenchPaginator }],
  imports: [
    KeyValuePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatRadioModule,
    MatPaginatorModule,
  ],
  templateUrl: './demande-create.html',
  styleUrl: '../demandes.scss',
})
export class DemandeCreate {
  private readonly api = inject(DemandesApi);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly session = inject(SessionService);
  private readonly fb = inject(FormBuilder);
  private readonly clientRequests = new Subject<{ recherche: string; page: number } | null>();
  readonly categories = categories;
  readonly priorites = priorites;
  readonly loading = signal(false);
  readonly error = signal<ApiError | null>(null);
  readonly clientsLoading = signal(false);
  readonly clientsError = signal<ApiError | null>(null);
  readonly clients = signal<PageResponse<ClientSummaryResponse> | null>(null);
  readonly selectedClient = signal<ClientSummaryResponse | null>(null);
  readonly search = this.fb.nonNullable.control('');
  readonly form = this.fb.nonNullable.group({
    titre: ['', nonBlank],
    description: ['', nonBlank],
    categorie: this.fb.control<Categorie | null>(null, Validators.required),
    priorite: this.fb.control<Priorite | null>(null, Validators.required),
    clientMode: 'existing',
    clientId: this.fb.control<number | null>(null, Validators.required),
    nouveauClient: this.fb.nonNullable.group({
      nom: ['', nonBlank],
      email: ['', [nonBlank, Validators.email]],
      telephone: ['', nonBlank],
    }),
  });
  constructor() {
    this.form.controls.nouveauClient.disable();
    this.clientRequests
      .pipe(
        switchMap((query) => {
          this.clientsLoading.set(!!query);
          this.clientsError.set(null);
          this.clients.set(null);
          return query && this.session.hasAnyRole(['RESPONSABLE_TECHNIQUE'])
            ? this.api.clients(query.recherche, query.page).pipe(
                catchError((error) => {
                  this.clientsError.set(apiError(error));
                  return of(null);
                }),
              )
            : of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((page) => {
        this.clients.set(page);
        this.clientsLoading.set(false);
      });
    this.search.valueChanges
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.searchClients());
    this.form.controls.clientMode.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((mode) => {
        if (mode === 'new') {
          this.form.controls.clientId.disable();
          this.form.controls.nouveauClient.enable();
          this.clientRequests.next(null);
        } else {
          this.form.controls.nouveauClient.disable();
          this.form.controls.clientId.enable();
          this.searchClients();
        }
      });
    this.searchClients();
  }
  searchClients(page = 0): void {
    if (
      this.form.controls.clientMode.value === 'existing' &&
      this.session.hasAnyRole(['RESPONSABLE_TECHNIQUE'])
    )
      this.clientRequests.next({ recherche: this.search.value.trim(), page });
  }
  paginateClients(event: PageEvent): void {
    this.searchClients(event.pageIndex);
  }
  selectClient(client: ClientSummaryResponse): void {
    this.selectedClient.set(client);
    this.form.controls.clientId.setValue(client.id);
  }
  private enableForm(): void {
    this.form.enable({ emitEvent: false });
    this.search.enable({ emitEvent: false });
    if (this.form.controls.clientMode.value === 'new')
      this.form.controls.clientId.disable({ emitEvent: false });
    else this.form.controls.nouveauClient.disable({ emitEvent: false });
  }
  submit(): void {
    if (this.loading() || !this.session.hasAnyRole(['RESPONSABLE_TECHNIQUE'])) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    if (!value.categorie || !value.priorite) return;
    const common = {
      titre: value.titre.trim(),
      description: value.description.trim(),
      categorie: value.categorie,
      priorite: value.priorite,
    };
    let body: CreationDemandeRequest;
    if (value.clientMode === 'new')
      body = {
        ...common,
        nouveauClient: {
          nom: value.nouveauClient.nom.trim(),
          email: value.nouveauClient.email.trim(),
          telephone: value.nouveauClient.telephone.trim(),
        },
      };
    else if (value.clientId !== null) body = { ...common, clientId: value.clientId };
    else return;
    this.loading.set(true);
    this.error.set(null);
    this.form.disable({ emitEvent: false });
    this.search.disable({ emitEvent: false });
    this.api
      .create(body)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.loading.set(false);
          if (this.form.disabled) this.enableForm();
        }),
      )
      .subscribe({
        next: (detail) => {
          void this.router.navigate(['/demandes', detail.reference], {
            state: { demandeCreated: detail.reference },
          });
        },
        error: (error) => {
          this.enableForm();
          this.error.set(apiError(error, this.form));
        },
      });
  }
}
