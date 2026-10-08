import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort, SortDirection } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { Subject, catchError, of, startWith, switchMap, tap } from 'rxjs';
import { PageState } from '../../../shared/ui/page-state/page-state';
import { AdministrationApi } from '../administration-api';
import { administrationError, frenchPaginator } from '../administration-feedback';
import {
  AdministrationError,
  PageUtilisateursResponse,
  UtilisateurQuery,
  roleLabels,
} from '../utilisateur-models';
@Component({
  selector: 'app-utilisateurs-list',
  providers: [{ provide: MatPaginatorIntl, useFactory: frenchPaginator }],
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatPaginatorModule,
    MatSortModule,
    MatTableModule,
    PageState,
  ],
  templateUrl: './utilisateurs-list.html',
  styleUrl: '../administration.scss',
})
export class UtilisateursList {
  private readonly api = inject(AdministrationApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reloadRequest = new Subject<void>();
  readonly roleLabels = roleLabels;
  readonly loading = signal(true);
  readonly error = signal<AdministrationError | null>(null);
  readonly result = signal<PageUtilisateursResponse | null>(null);
  readonly sort = new FormControl('', { nonNullable: true });
  readonly columns = ['nom', 'email', 'actif', 'roles', 'action'];
  readonly sortOptions = [
    { value: '', label: 'Ordre du serveur' },
    { value: 'id,asc', label: 'Identifiant croissant' },
    { value: 'id,desc', label: 'Identifiant décroissant' },
    { value: 'nom,asc', label: 'Nom : A à Z' },
    { value: 'nom,desc', label: 'Nom : Z à A' },
    { value: 'email,asc', label: 'Email : A à Z' },
    { value: 'email,desc', label: 'Email : Z à A' },
    { value: 'actif,desc', label: 'Actifs en premier' },
    { value: 'actif,asc', label: 'Inactifs en premier' },
  ];
  query: UtilisateurQuery = { page: 0, size: 20 };
  constructor() {
    this.route.queryParamMap
      .pipe(
        tap((params) => {
          const natural = (key: string, fallback: number, minimum: number) => {
            const raw = params.get(key);
            const value = raw === null ? fallback : Number(raw);
            return Number.isSafeInteger(value) && value >= minimum ? value : fallback;
          };
          const sort = params.get('sort') ?? '';
          this.query = {
            page: natural('page', 0, 0),
            size: natural('size', 20, 1),
            sort: /^(id|nom|email|actif),(asc|desc)$/.test(sort) ? sort : undefined,
          };
          this.sort.setValue(this.query.sort ?? '', { emitEvent: false });
        }),
        switchMap(() =>
          this.reloadRequest.pipe(
            startWith(undefined),
            switchMap(() => {
              this.loading.set(true);
              this.error.set(null);
              this.result.set(null);
              return this.api.lister(this.query).pipe(
                catchError((error) => {
                  this.error.set(administrationError(error));
                  return of(null);
                }),
              );
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((page) => {
        this.result.set(page);
        this.loading.set(false);
      });
    this.sort.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((sort) => this.navigate({ ...this.query, page: 0, sort: sort || undefined }));
  }
  roleLabel(role: keyof typeof roleLabels): string {
    return roleLabels[role];
  }
  sortActive(): string {
    return this.sort.value.split(',')[0];
  }
  sortDirection(): SortDirection {
    return (this.sort.value.split(',')[1] ?? '') as SortDirection;
  }
  sortChanged(event: Sort): void {
    this.sort.setValue(event.direction ? event.active + ',' + event.direction : '');
  }
  paginate(event: PageEvent): void {
    this.navigate({ ...this.query, page: event.pageIndex, size: event.pageSize });
  }
  retry(): void {
    this.reloadRequest.next();
  }
  private navigate(query: UtilisateurQuery): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        page: query.page,
        size: query.size,
        ...(query.sort ? { sort: query.sort } : {}),
      },
    });
  }
}
