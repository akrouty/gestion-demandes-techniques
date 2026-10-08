import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SessionService } from '../session/session';

export const authorizationInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionService);
  const router = inject(Router);
  const base = new URL(environment.apiBaseUrl, globalThis.location.origin);
  const url = new URL(request.url, globalThis.location.origin);
  const prefix = base.pathname.replace(/\/$/, '');
  const protectedApi =
    url.origin === base.origin &&
    (url.pathname === prefix || url.pathname.startsWith(prefix + '/')) &&
    url.pathname !== prefix + '/auth/login';
  if (!protectedApi) return next(request);
  const hadSession = session.user() !== null;
  const token = session.token();
  if (hadSession && !token) void router.navigateByUrl('/login');
  const authorized = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;
  return next(authorized).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) session.invalidate(token);
      return throwError(() => error);
    }),
  );
};
