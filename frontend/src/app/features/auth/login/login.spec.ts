import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Login } from './login';
import { AuthApi } from '../auth-api';
import { SessionService } from '../../../core/session/session';
import { LoginResponse } from '../../../core/session/session-models';
describe('Login', () => {
  const response: LoginResponse = {
    accessToken: 'test-token',
    tokenType: 'Bearer',
    expiresAt: new Date(Date.now() + 60000).toISOString(),
    user: { id: 1, nom: 'Test', email: 'test@example.test', roles: ['RESPONSABLE_TECHNIQUE'] },
  };
  let api: { login: ReturnType<typeof vi.fn> };
  beforeEach(() => {
    api = { login: vi.fn() };
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), { provide: AuthApi, useValue: api }],
    });
  });
  afterEach(() => vi.restoreAllMocks());
  const setup = () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({
      email: 'test@example.test',
      password: 'test-password',
    });
    return fixture;
  };
  it('soumet email/password et redirige après succès', () => {
    api.login.mockReturnValue(of(response));
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = setup();
    fixture.nativeElement
      .querySelector('form')
      .dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(api.login).toHaveBeenCalledWith({
      email: 'test@example.test',
      password: 'test-password',
    });
    expect(TestBed.inject(SessionService).token()).toBe('test-token');
    expect(navigate).toHaveBeenCalledWith('/demandes');
    expect(fixture.componentInstance.form.controls.password.value).toBe('');
  });
  it('affiche une erreur générique sans reprendre les détails backend', () => {
    api.login.mockReturnValue(
      throwError(() => ({ status: 401, error: { message: 'Compte inconnu' } })),
    );
    const fixture = setup();
    fixture.componentInstance.submit();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain(
      'Connexion impossible',
    );
    expect(fixture.nativeElement.textContent).not.toContain('Compte inconnu');
    expect(TestBed.inject(SessionService).user()).toBeNull();
    expect(fixture.componentInstance.loading()).toBe(false);
  });
  it('bloque les doubles soumissions pendant loading', () => {
    const pending = new Subject<LoginResponse>();
    api.login.mockReturnValue(pending);
    const fixture = setup();
    fixture.componentInstance.submit();
    fixture.detectChanges();
    fixture.componentInstance.submit();
    expect(api.login).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    pending.complete();
    expect(fixture.componentInstance.loading()).toBe(false);
  });
  it('ne soumet pas un formulaire invalide', () => {
    const fixture = TestBed.createComponent(Login);
    fixture.componentInstance.submit();
    expect(api.login).not.toHaveBeenCalled();
    expect(fixture.componentInstance.form.touched).toBe(true);
  });
  it('une réponse tardive après logout ne rétablit pas la session', () => {
    const pending = new Subject<LoginResponse>();
    api.login.mockReturnValue(pending);
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = setup();
    fixture.componentInstance.submit();
    TestBed.inject(SessionService).logout();
    pending.next(response);
    expect(TestBed.inject(SessionService).user()).toBeNull();
    pending.complete();
  });
  it('ne traite plus une réponse après destruction de la page', () => {
    const pending = new Subject<LoginResponse>();
    api.login.mockReturnValue(pending);
    const fixture = setup();
    fixture.componentInstance.submit();
    fixture.destroy();
    pending.next(response);
    expect(TestBed.inject(SessionService).user()).toBeNull();
  });
});
