import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup, ValidationErrors } from '@angular/forms';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { AdministrationError, RoleMetier } from './utilisateur-models';
export function nonBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && !control.value.trim() ? { required: true } : null;
}
export function businessRoles(value: { responsable: boolean; agent: boolean }): RoleMetier[] {
  const roles: RoleMetier[] = [];
  if (value.responsable) roles.push('RESPONSABLE_TECHNIQUE');
  if (value.agent) roles.push('AGENT_TECHNIQUE');
  return roles;
}
export function administrationError(error: unknown, form?: FormGroup): AdministrationError {
  const response = error instanceof HttpErrorResponse ? error : null;
  const body = response?.error;
  const result: AdministrationError =
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
                ? 'Utilisateur introuvable.'
                : 'La requête n’a pas abouti. Vérifiez votre connexion puis réessayez.',
        };
  if (result.code === 'EMAIL_DEJA_UTILISE') {
    result.message = 'Cet email est déjà utilisé.';
    form?.get('email')?.setErrors({ server: result.message });
    form?.get('email')?.markAsTouched();
  }
  if (result.code === 'MOT_DE_PASSE_INVALIDE') {
    result.message = 'Le mot de passe ne respecte pas la politique minimale.';
    form?.get('password')?.setErrors({ server: result.message });
    form?.get('password')?.markAsTouched();
  }
  if (result.code === 'ROLE_METIER_INVALIDE')
    result.message = 'Les rôles sélectionnés ne sont pas valides.';
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
  paginator.itemsPerPageLabel = 'Utilisateurs par page';
  paginator.nextPageLabel = 'Page suivante';
  paginator.previousPageLabel = 'Page précédente';
  paginator.firstPageLabel = 'Première page';
  paginator.lastPageLabel = 'Dernière page';
  paginator.getRangeLabel = (page, size, length) =>
    length === 0
      ? '0 utilisateur'
      : page * size + 1 + '–' + Math.min((page + 1) * size, length) + ' sur ' + length;
  return paginator;
}
