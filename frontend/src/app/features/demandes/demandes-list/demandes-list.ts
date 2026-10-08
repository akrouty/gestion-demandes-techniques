import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe, KeyValuePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { Subject, catchError, of, skip, startWith, switchMap, takeUntil, tap, timer } from 'rxjs';
import { SessionService } from '../../../core/session/session';
import { DemandesApi } from '../demandes-api';
import { apiError, frenchPaginator } from '../demande-feedback';
import {
  ApiError,
  categories,
  priorites,
  statuts,
  DemandeQuery,
  DemandeSummaryResponse,
  PageResponse,
} from '../demande-models';
@Component({
  selector: 'app-demandes-list',
  providers: [{ provide: MatPaginatorIntl, useFactory: frenchPaginator }],
  imports: [
    DatePipe,
    KeyValuePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatPaginatorModule,
  ],
  templateUrl: './demandes-list.html',
  styleUrl: '../demandes.scss',
})
export class DemandesList {
  private readonly api = inject(DemandesApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reloadRequest = new Subject<void>();
  readonly session = inject(SessionService);
  readonly categories = categories;
  readonly priorites = priorites;
  readonly statuts = statuts;
  readonly loading = signal(true);
  readonly error = signal<ApiError | null>(null);
  readonly result = signal<PageResponse<DemandeSummaryResponse> | null>(null);
  readonly filters = inject(FormBuilder).nonNullable.group({
    recherche: '',
    statut: '',
    priorite: '',
    categorie: '',
    sort: 'dateCreation,desc',
  });
  readonly sortOptions = [
    { value: 'dateCreation,desc', label: 'Plus récentes' },
    { value: 'dateCreation,asc', label: 'Plus anciennes' },
    { value: 'dateModification,desc', label: 'Dernières modifications' },
    { value: 'reference,asc', label: 'Référence' },
    { value: 'titre,asc', label: 'Titre' },
    { value: 'statut,asc', label: 'Statut' },
    { value: 'categorie,asc', label: 'Catégorie' },
    { value: 'priorite,desc', label: 'Priorité' },
  ];
  query: DemandeQuery = { page: 0, size: 20, sort: 'dateCreation,desc' };
  constructor() {
    this.route.queryParamMap
      .pipe(
        tap((params) => {
          const natural = (key: string, fallback: number, minimum: number) => {
            const raw = params.get(key);
            const number = raw === null ? fallback : Number(raw);
            return Number.isSafeInteger(number) && number >= minimum ? number : fallback;
          };
          const statut = params.get('statut') ?? '';
          const priorite = params.get('priorite') ?? '';
          const categorie = params.get('categorie') ?? '';
          const sort = params.get('sort') ?? 'dateCreation,desc';
          this.query = {
            page: natural('page', 0, 0),
            size: natural('size', 20, 1),
            sort: /^(dateCreation|dateModification|reference|titre|statut|categorie|priorite),(asc|desc)$/.test(
              sort,
            )
              ? sort
              : 'dateCreation,desc',
            recherche: params.get('recherche')?.trim() || undefined,
            statut: Object.hasOwn(statuts, statut) ? (statut as DemandeQuery['statut']) : undefined,
            priorite: Object.hasOwn(priorites, priorite)
              ? (priorite as DemandeQuery['priorite'])
              : undefined,
            categorie: Object.hasOwn(categories, categorie)
              ? (categorie as DemandeQuery['categorie'])
              : undefined,
            clientId: params.has('clientId') ? natural('clientId', 0, 1) || undefined : undefined,
            agentId: params.has('agentId') ? natural('agentId', 0, 1) || undefined : undefined,
          };
          this.filters.patchValue(
            {
              recherche: this.query.recherche ?? '',
              statut: this.query.statut ?? '',
              priorite: this.query.priorite ?? '',
              categorie: this.query.categorie ?? '',
              sort: this.query.sort,
            },
            { emitEvent: false },
          );
        }),
        switchMap(() =>
          this.reloadRequest.pipe(
            startWith(undefined),
            switchMap(() => {
              this.loading.set(true);
              this.error.set(null);
              this.result.set(null);
              return this.api.list(this.query).pipe(
                catchError((error) => {
                  this.error.set(apiError(error));
                  return of(null);
                }),
              );
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((result) => {
        this.result.set(result);
        this.loading.set(false);
      });
    this.filters.controls.recherche.valueChanges
      .pipe(
        switchMap(() => timer(350).pipe(takeUntil(this.route.queryParamMap.pipe(skip(1))))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.applyFilters());
    for (const name of ['statut', 'priorite', 'categorie', 'sort'] as const)
      this.filters.controls[name].valueChanges
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => this.applyFilters());
  }
  applyFilters(): void {
    const value = this.filters.getRawValue();
    this.navigate({ ...this.query, ...value, page: 0 });
  }
  paginate(event: PageEvent): void {
    this.navigate({ ...this.query, page: event.pageIndex, size: event.pageSize });
  }
  reset(): void {
    this.navigate({ page: 0, size: 20, sort: 'dateCreation,desc' });
  }
  retry(): void {
    this.reloadRequest.next();
  }
  private navigate(query: Record<string, unknown>): void {
    const queryParams = Object.fromEntries(
      Object.entries(query).filter(([, value]) => value !== undefined && value !== ''),
    );
    void this.router.navigate([], { relativeTo: this.route, queryParams });
  }
}
