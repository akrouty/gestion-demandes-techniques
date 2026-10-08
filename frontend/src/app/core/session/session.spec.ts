import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { SessionService } from './session';
import { LoginResponse, Role } from './session-models';
import { vi } from 'vitest';
const response = (roles: Role[] = ['RESPONSABLE_TECHNIQUE']): LoginResponse => ({
  accessToken: 'test-token',
  tokenType: 'Bearer',
  expiresAt: new Date(Date.now() + 60000).toISOString(),
  user: { id: 1, nom: 'Test', email: 'test@example.test', roles },
});
describe('Session mémoire', () => {
  let session: SessionService;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    session = TestBed.inject(SessionService);
  });
  afterEach(() => vi.restoreAllMocks());
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
