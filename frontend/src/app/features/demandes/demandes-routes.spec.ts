import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../../app.routes';
import { SessionService } from '../../core/session/session';
import { Role } from '../../core/session/session-models';
import { authorizationInterceptor } from '../../core/http/authorization-interceptor';
import { environment } from '../../../environments/environment';
describe('Routes F2 avec guards et shell F1', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(withInterceptors([authorizationInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify({ ignoreCancelled: true }));
  function login(roles: Role[]) {
    TestBed.inject(SessionService).establish({
      accessToken: 'test',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: { id: 1, nom: 'Test', email: 'test@example.test', roles },
    });
  }
  it('nouvelle est reconnue avant reference et appelle clients, pas detail', async () => {
    login(['RESPONSABLE_TECHNIQUE']);
    const harness = await RouterTestingHarness.create('/demandes/nouvelle');
    http
      .expectOne((r) => r.url === environment.apiBaseUrl + '/clients')
      .flush({ items: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
    http.expectNone(environment.apiBaseUrl + '/demandes/nouvelle');
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Nouvelle demande');
  });
  it('AT est écarté de création sans appel HTTP réservé au RT', async () => {
    login(['AGENT_TECHNIQUE']);
    await RouterTestingHarness.create('/demandes/nouvelle');
    expect(TestBed.inject(Router).url).toBe('/acces-refuse');
    http.expectNone(() => true);
  });
  it('ADM seul est écarté des demandes', async () => {
    login(['ADMINISTRATEUR']);
    await RouterTestingHarness.create('/demandes');
    expect(TestBed.inject(Router).url).toBe('/acces-refuse');
    http.expectNone(() => true);
  });
});
