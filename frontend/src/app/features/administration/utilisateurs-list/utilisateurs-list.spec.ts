import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { UtilisateursList } from './utilisateurs-list';
import { SessionService } from '../../../core/session/session';
import { authorizationInterceptor } from '../../../core/http/authorization-interceptor';
import { environment } from '../../../../environments/environment';
describe('Liste utilisateurs F3', () => {
  let http: HttpTestingController;
  const base = environment.apiBaseUrl + '/utilisateurs';
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'administration/utilisateurs', component: UtilisateursList }]),
        provideHttpClient(withInterceptors([authorizationInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(SessionService).establish({
      accessToken: 'test',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: { id: 1, nom: 'Test', email: 'a@example.test', roles: ['ADMINISTRATEUR'] },
    });
  });
  afterEach(() => http.verify({ ignoreCancelled: true }));
  const request = () => http.expectOne((r) => r.url === base);
  const flush = (page = 0, size = 20) =>
    request().flush({ items: [], page, size, totalElements: 0, totalPages: 0 });
  it('loading puis page zéro et empty', async () => {
    const harness = await RouterTestingHarness.create('/administration/utilisateurs');
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Chargement des utilisateurs');
    const r = request();
    expect(r.request.params.get('page')).toBe('0');
    expect(r.request.params.get('size')).toBe('20');
    expect(r.request.params.has('sort')).toBe(false);
    r.flush({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Aucun utilisateur');
  });
  it('rend noms, emails, états et tous les rôles retournés', async () => {
    const harness = await RouterTestingHarness.create('/administration/utilisateurs');
    request().flush({
      items: [
        {
          id: 7,
          nom: 'Test API',
          email: 'test@example.test',
          actif: false,
          roles: ['ADMINISTRATEUR', 'AGENT_TECHNIQUE'],
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    harness.detectChanges();
    const text = harness.routeNativeElement!.textContent!;
    expect(text).toContain('Test API');
    expect(text).toContain('test@example.test');
    expect(text).toContain('Inactif');
    expect(text).toContain('Administrateur');
    expect(text).toContain('Agent technique');
    expect(
      harness.routeNativeElement
        ?.querySelector('a[aria-label="Modifier Test API"]')
        ?.getAttribute('href'),
    ).toBe('/administration/utilisateurs/7/modifier');
    expect(harness.routeNativeElement?.querySelector('input')).toBeNull();
  });
  it('MatPaginator envoie index backend sans décalage', async () => {
    const harness = await RouterTestingHarness.create('/administration/utilisateurs');
    flush();
    (harness.routeDebugElement!.componentInstance as UtilisateursList).paginate({
      pageIndex: 3,
      pageSize: 10,
      length: 80,
    });
    await harness.fixture.whenStable();
    const r = request();
    expect(r.request.params.get('page')).toBe('3');
    expect(r.request.params.get('size')).toBe('10');
    r.flush({ items: [], page: 3, size: 10, totalElements: 80, totalPages: 8 });
  });
  for (const active of ['nom', 'email'])
    it('tri ' + active + ' serveur remet page zéro', async () => {
      const harness = await RouterTestingHarness.create('/administration/utilisateurs?page=3');
      flush(3);
      (harness.routeDebugElement!.componentInstance as UtilisateursList).sortChanged({
        active,
        direction: 'desc',
      });
      await harness.fixture.whenStable();
      const r = request();
      expect(r.request.params.get('sort')).toBe(active + ',desc');
      expect(r.request.params.get('page')).toBe('0');
      expect(TestBed.inject(Router).url).toContain('sort=' + active + ',desc');
      r.flush({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    });
  it('restaure pagination et tri URL', async () => {
    const harness = await RouterTestingHarness.create(
      '/administration/utilisateurs?page=2&size=10&sort=email,asc',
    );
    const r = request();
    expect(r.request.params.get('page')).toBe('2');
    expect(r.request.params.get('sort')).toBe('email,asc');
    r.flush({ items: [], page: 2, size: 10, totalElements: 0, totalPages: 0 });
    expect((harness.routeDebugElement!.componentInstance as UtilisateursList).sort.value).toBe(
      'email,asc',
    );
  });
  it('nouvelle vue annule une requête précédente', async () => {
    await RouterTestingHarness.create('/administration/utilisateurs');
    const old = request();
    await TestBed.inject(Router).navigateByUrl('/administration/utilisateurs?page=1');
    expect(old.cancelled).toBe(true);
    flush(1);
  });
  it('400 PARAMETRE_INVALIDE donne erreur et reprise explicite', async () => {
    const harness = await RouterTestingHarness.create('/administration/utilisateurs');
    request().flush(
      { code: 'PARAMETRE_INVALIDE', message: 'Paramètre de requête invalide.' },
      { status: 400, statusText: 'Bad Request' },
    );
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('PARAMETRE_INVALIDE');
    (harness.routeDebugElement!.componentInstance as UtilisateursList).retry();
    flush();
  });
  it('403 conserve session sans redirection', async () => {
    const harness = await RouterTestingHarness.create('/administration/utilisateurs');
    request().flush(
      { code: 'ACCES_INTERDIT', message: 'Accès refusé.' },
      { status: 403, statusText: 'Forbidden' },
    );
    expect(TestBed.inject(SessionService).token()).toBe('test');
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Accès refusé');
  });
  it('ne transmet aucune recherche ou tri de rôle inventé', async () => {
    await RouterTestingHarness.create(
      '/administration/utilisateurs?recherche=test&sort=roles,asc&page=-2',
    );
    const r = request();
    expect(r.request.params.keys().sort()).toEqual(['page', 'size']);
    expect(r.request.params.get('page')).toBe('0');
    r.flush({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });
});
