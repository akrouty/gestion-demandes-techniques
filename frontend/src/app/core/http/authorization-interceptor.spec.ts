import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';
import { authorizationInterceptor } from './authorization-interceptor';
import { SessionService } from '../session/session';
import { environment } from '../../../environments/environment';
describe('Authorization interceptor', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  let session: SessionService;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authorizationInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    requests = TestBed.inject(HttpTestingController);
    session = TestBed.inject(SessionService);
    session.establish({
      accessToken: 'test-token',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: { id: 1, nom: 'Test', email: 'test@example.test', roles: ['AGENT_TECHNIQUE'] },
    });
  });
  afterEach(() => {
    requests.verify();
    vi.restoreAllMocks();
  });
  it('ajoute Bearer uniquement à notre API protégée', () => {
    const url = environment.apiBaseUrl + '/demandes';
    http.get(url).subscribe();
    const r = requests.expectOne(url);
    expect(r.request.headers.get('Authorization')).toBe('Bearer test-token');
    r.flush({});
  });
  it('ne joint pas de token au login et conserve la session sur 401 login', () => {
    const url = environment.apiBaseUrl + '/auth/login';
    http.post(url, {}).subscribe({ error: () => {} });
    const r = requests.expectOne(url);
    expect(r.request.headers.has('Authorization')).toBe(false);
    r.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(session.token()).toBe('test-token');
  });
  it('n’expose pas le token à une autre origine', () => {
    const url = 'https://external.example/api/v1/demandes';
    http.get(url).subscribe();
    const r = requests.expectOne(url);
    expect(r.request.headers.has('Authorization')).toBe(false);
    r.flush({});
  });
  it('ne joint pas le token aux ressources hors préfixe API', () => {
    const url = new URL(environment.apiBaseUrl, location.origin).origin + '/api/v10/demandes';
    http.get(url).subscribe();
    const r = requests.expectOne(url);
    expect(r.request.headers.has('Authorization')).toBe(false);
    r.flush({});
  });
  it('401 simultanés nettoient la session et redirigent une seule fois', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const url = environment.apiBaseUrl + '/demandes';
    http.get(url).subscribe({ error: () => {} });
    http.get(url).subscribe({ error: () => {} });
    for (const r of requests.match(url)) r.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(session.user()).toBeNull();
    expect(navigate).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith('/login');
  });
  it('403 conserve la session sans redirection', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl');
    const url = environment.apiBaseUrl + '/demandes';
    http.get(url).subscribe({ error: () => {} });
    requests.expectOne(url).flush({}, { status: 403, statusText: 'Forbidden' });
    expect(session.token()).toBe('test-token');
    expect(navigate).not.toHaveBeenCalled();
  });
});
