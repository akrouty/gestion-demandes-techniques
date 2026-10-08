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
describe('Routes F3 avec shell et guards F1', () => {
  let http: HttpTestingController;
  const base = environment.apiBaseUrl + '/utilisateurs';
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
  it('ADM seul accède liste réelle', async () => {
    login(['ADMINISTRATEUR']);
    const harness = await RouterTestingHarness.create('/administration/utilisateurs');
    http
      .expectOne((r) => r.url === base)
      .flush({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    harness.detectChanges();
    expect(TestBed.inject(Router).url).toBe('/administration/utilisateurs');
    expect(harness.routeNativeElement?.textContent).toContain('Nouvel utilisateur');
  });
  for (const role of ['RESPONSABLE_TECHNIQUE', 'AGENT_TECHNIQUE'] as const)
    it(role + ' seul écarté de toutes les routes F3 sans appels', async () => {
      login([role]);
      const harness = await RouterTestingHarness.create('/administration/utilisateurs');
      expect(TestBed.inject(Router).url).toBe('/acces-refuse');
      await harness.navigateByUrl('/administration/utilisateurs/nouveau');
      expect(TestBed.inject(Router).url).toBe('/acces-refuse');
      await harness.navigateByUrl('/administration/utilisateurs/7/modifier');
      expect(TestBed.inject(Router).url).toBe('/acces-refuse');
      http.expectNone(() => true);
    });
  for (const roles of [
    ['ADMINISTRATEUR', 'RESPONSABLE_TECHNIQUE'],
    ['ADMINISTRATEUR', 'AGENT_TECHNIQUE'],
    ['ADMINISTRATEUR', 'RESPONSABLE_TECHNIQUE', 'AGENT_TECHNIQUE'],
  ] as Role[][])
    it('union des rôles autorise modification pour ' + roles.join('+'), async () => {
      login(roles);
      await RouterTestingHarness.create('/administration/utilisateurs/7/modifier');
      http
        .expectOne(base + '/7')
        .flush({
          id: 7,
          nom: 'Test API',
          email: 'test@example.test',
          actif: true,
          roles: ['AGENT_TECHNIQUE'],
        });
      expect(TestBed.inject(Router).url).toBe('/administration/utilisateurs/7/modifier');
    });
  it('nouveau est une création réelle sans appel de détail inventé', async () => {
    login(['ADMINISTRATEUR']);
    const harness = await RouterTestingHarness.create('/administration/utilisateurs/nouveau');
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Nouvel utilisateur');
    expect(harness.routeNativeElement?.querySelector('input[type=password]')).not.toBeNull();
    http.expectNone(() => true);
  });
  it('sans session revient au login sans appel F3', async () => {
    await RouterTestingHarness.create('/administration/utilisateurs');
    expect(TestBed.inject(Router).url).toBe('/login');
    http.expectNone(() => true);
  });
  it('401 F3 utilise fin de session globale F1', async () => {
    login(['ADMINISTRATEUR']);
    const harness = await RouterTestingHarness.create('/administration/utilisateurs');
    http
      .expectOne((r) => r.url === base)
      .flush(
        { code: 'AUTHENTIFICATION_REQUISE', message: 'Authentification requise.' },
        { status: 401, statusText: 'Unauthorized' },
      );
    await harness.fixture.whenStable();
    expect(TestBed.inject(SessionService).user()).toBeNull();
    expect(TestBed.inject(Router).url).toBe('/login');
  });
});
