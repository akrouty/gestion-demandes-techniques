import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService } from '../session/session';
import { Role } from '../session/session-models';
/** Guard de navigation UX, sans décision d'autorisation contextuelle. */
export const roleGuard: CanActivateFn = (route) =>
  inject(SessionService).hasAnyRole(route.data['roles'] as Role[])
    ? true
    : inject(Router).parseUrl('/acces-refuse');
