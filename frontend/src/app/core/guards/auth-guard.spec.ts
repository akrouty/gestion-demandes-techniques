import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { authGuard } from './auth-guard';
import { roleGuard } from './role-guard';
import { SessionService } from '../session/session';
describe('Guards UX', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter([])] }));
  const check = (guard: typeof authGuard, roles: string[] = []) =>
    TestBed.runInInjectionContext(() =>
      guard({ data: { roles } } as unknown as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  const login = () =>
    TestBed.inject(SessionService).establish({
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: { id: 1, nom: 'Test', email: 'test@example.test', roles: ['AGENT_TECHNIQUE'] },
    });
  it('session absente → login', () =>
    expect((check(authGuard) as UrlTree).toString()).toBe('/login'));
  it('session utilisable → accès', () => {
    login();
    expect(check(authGuard)).toBe(true);
  });
  it('rôle incompatible → accès refusé en conservant la session', () => {
    login();
    expect((check(roleGuard, ['ADMINISTRATEUR']) as UrlTree).toString()).toBe('/acces-refuse');
    expect(TestBed.inject(SessionService).token()).toBe('token');
  });
  it('un rôle compatible dans l’union suffit', () => {
    login();
    expect(check(roleGuard, ['RESPONSABLE_TECHNIQUE', 'AGENT_TECHNIQUE'])).toBe(true);
  });
});
