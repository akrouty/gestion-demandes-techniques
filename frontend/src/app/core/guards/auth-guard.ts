import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService } from '../session/session';
export const authGuard: CanActivateFn = () =>
  inject(SessionService).token() ? true : inject(Router).parseUrl('/login');
