import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
  Router,
} from '@angular/router';
import { SessionService } from './session';
import { LoginResponse, Role } from './session-models';
import { vi } from 'vitest';
import { authGuard } from '../guards/auth-guard';
import { roleGuard } from '../guards/role-guard';
const storageKey = 'safyron.session.v1';
const jwt = (exp = Math.floor(Date.now() / 1000) + 60) =>
  `${btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).replace(/=/g, '')}.${btoa(JSON.stringify({ exp })).replace(/=/g, '')}.c2lnbmF0dXJl`;
const response = (roles: Role[] = ['RESPONSABLE_TECHNIQUE']): LoginResponse => ({
  accessToken: 'test-token',
  tokenType: 'Bearer',
  expiresAt: new Date(Date.now() + 60000).toISOString(),
  user: { id: 1, nom: 'Test', email: 'test@example.test', roles },
});
describe('Session avec persistance par onglet', () => {
  let session: SessionService;
  beforeEach(() => {
    sessionStorage.removeItem(storageKey);
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    session = TestBed.inject(SessionService);
  });
  afterEach(() => {
    vi.restoreAllMocks();
    sessionStorage.removeItem(storageKey);
  });
  const restart = () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    session = TestBed.inject(SessionService);
  };
  const check = (guard: typeof authGuard, roles: string[] = []) =>
    TestBed.runInInjectionContext(() =>
      guard({ data: { roles } } as unknown as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  it('restaure token et rôles avant les guards après redémarrage Angular (F5)', () => {
    const data = { ...response(), accessToken: jwt() };
    const localWrite = vi.spyOn(Storage.prototype, 'setItem');
    session.establish(data);
    restart();
    expect(session.token()).toBe(data.accessToken);
    expect(session.user()).toEqual(data.user);
    expect(check(authGuard)).toBe(true);
    expect(check(roleGuard, ['RESPONSABLE_TECHNIQUE'])).toBe(true);
    expect(localWrite.mock.contexts.every((storage) => storage === sessionStorage)).toBe(true);
  });
  it.each([
    ['JSON malformé', '{'],
    ['snapshot absent', JSON.stringify({ accessToken: jwt() })],
    ['JWT malformé', JSON.stringify(response())],
    [
      'JWT expiré malgré expiresAt futur',
      JSON.stringify({ ...response(), accessToken: jwt(Math.floor(Date.now() / 1000) - 1) }),
    ],
    [
      'expiration de session passée',
      JSON.stringify({ ...response(), accessToken: jwt(), expiresAt: new Date(0).toISOString() }),
    ],
    [
      'rôle inconnu',
      JSON.stringify({
        ...response(),
        accessToken: jwt(),
        user: { ...response().user, roles: ['ROOT'] },
      }),
    ],
  ])('rejette %s et interdit la navigation protégée', (_, stored) => {
    sessionStorage.setItem(storageKey, stored);
    restart();
    expect(session.user()).toBeNull();
    expect((check(authGuard) as UrlTree).toString()).toBe('/login');
    expect(sessionStorage.getItem(storageKey)).toBeNull();
  });
  it('utilise aussi exp du JWT après restauration', () => {
    session.establish({ ...response(), accessToken: jwt(Math.floor(Date.now() / 1000) + 5) });
    restart();
    vi.spyOn(Date, 'now').mockReturnValue(Date.now() + 10000);
    expect((check(authGuard) as UrlTree).toString()).toBe('/login');
    expect(sessionStorage.getItem(storageKey)).toBeNull();
  });
  it('reste utilisable en mémoire si le stockage est interdit', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('SecurityError');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('SecurityError');
    });
    vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
      throw new Error('SecurityError');
    });
    restart();
    session.establish(response());
    expect(session.token()).toBe('test-token');
    session.clear();
    expect(session.user()).toBeNull();
  });
  it('établit ensemble le token et le snapshot sans décoder le JWT', () => {
    const data = response();
    session.establish(data);
    expect(session.token()).toBe('test-token');
    expect(session.user()).toEqual(data.user);
    expect(session.initialRoute()).toBe('/demandes');
  });
  it('déconnecte et retourne au login', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    session.establish(response());
    session.logout();
    expect(session.token()).toBeNull();
    expect(session.user()).toBeNull();
    expect(navigate).toHaveBeenCalledWith('/login');
    expect(sessionStorage.getItem(storageKey)).toBeNull();
    restart();
    expect(session.token()).toBeNull();
  });
  it('expire au premier usage selon expiresAt', () => {
    session.establish(response());
    vi.spyOn(Date, 'now').mockReturnValue(Date.now() + 120000);
    expect(session.token()).toBeNull();
    expect(session.user()).toBeNull();
  });
  it('refuse une expiration invalide', () => {
    expect(() => session.establish({ ...response(), expiresAt: 'invalide' })).toThrow();
    expect(session.user()).toBeNull();
  });
  it('oriente ADM seul vers utilisateurs', () => {
    session.establish(response(['ADMINISTRATEUR']));
    expect(session.initialRoute()).toBe('/administration/utilisateurs');
    expect(session.hasDemandes()).toBe(false);
  });
  it('cumule les sections pour un compte multi-rôles', () => {
    session.establish(response(['AGENT_TECHNIQUE', 'ADMINISTRATEUR']));
    expect(session.hasDemandes()).toBe(true);
    expect(session.hasAdministration()).toBe(true);
    expect(session.initialRoute()).toBe('/demandes');
  });
  it('une erreur d’une ancienne session ne détruit pas la nouvelle', () => {
    session.establish(response());
    session.establish({ ...response(), accessToken: 'new-token' });
    session.invalidate('test-token');
    expect(session.token()).toBe('new-token');
  });
});
