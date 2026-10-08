import { MatPaginatorIntl } from '@angular/material/paginator';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup, ValidationErrors } from '@angular/forms';
import { ApiError } from './demande-models';
export function nonBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && !control.value.trim() ? { required: true } : null;
}
export function apiError(error: unknown, form?: FormGroup): ApiError {
  const response = error instanceof HttpErrorResponse ? error : null;
  const body = response?.error;
  const result: ApiError =
    body && typeof body.code === 'string' && typeof body.message === 'string'
      ? {
          code: body.code,
          message: body.message,
          fieldErrors: Array.isArray(body.fieldErrors) ? body.fieldErrors : [],
        }
      : {
          code: '',
          message:
            response?.status === 403
              ? 'Accès refusé.'
              : response?.status === 404
                ? 'Demande introuvable.'
                : 'La requête n’a pas abouti. Vérifiez votre connexion puis réessayez.',
        };
  if (response?.status === 400 && form)
    for (const field of result.fieldErrors ?? []) {
      const control = form.get(field.field);
      if (control) {
        control.setErrors({ ...control.errors, server: field.message });
        control.markAsTouched();
      }
    }
  return result;
}
export function frenchPaginator(): MatPaginatorIntl {
  const paginator = new MatPaginatorIntl();
  paginator.itemsPerPageLabel = 'Résultats par page';
  paginator.nextPageLabel = 'Page suivante';
  paginator.previousPageLabel = 'Page précédente';
  paginator.firstPageLabel = 'Première page';
  paginator.lastPageLabel = 'Dernière page';
  paginator.getRangeLabel = (page, size, length) =>
    length === 0
      ? '0 résultat'
      : page * size + 1 + '–' + Math.min((page + 1) * size, length) + ' sur ' + length;
  return paginator;
}
