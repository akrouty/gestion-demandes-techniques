import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { DemandesList } from './demandes-list';
import { SessionService } from '../../../core/session/session';
import { environment } from '../../../../environments/environment';
describe('Liste des demandes', () => {
  let http: HttpTestingController;
  const base = environment.apiBaseUrl;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'demandes', component: DemandesList }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(SessionService).establish({
      accessToken: 'test',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: { id: 1, nom: 'Test', email: 'test@example.test', roles: ['RESPONSABLE_TECHNIQUE'] },
    });
  });
  afterEach(() => http.verify({ ignoreCancelled: true }));
  const request = () => http.expectOne((r) => r.url === base + '/demandes');
  const flushEmpty = (page = 0, size = 20) =>
    request().flush({ items: [], page, size, totalElements: 0, totalPages: 0 });
  it('commence à page zéro et rend un état vide', async () => {
    const harness = await RouterTestingHarness.create('/demandes');
    const r = request();
    expect(r.request.params.get('page')).toBe('0');
    r.flush({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Aucune demande');
  });
  it('affiche les données backend et les liens de détail', async () => {
    const harness = await RouterTestingHarness.create('/demandes');
    request().flush({
      items: [
        {
          reference: 'DT-42',
          titre: 'Pompe à inspecter',
          categorie: 'NOTE_CALCUL',
          priorite: 'HAUTE',
          statut: 'EN_COURS',
          client: { id: 4, nom: 'Client API', email: 'c@example.test', telephone: '123' },
          agentAffecte: null,
          dateCreation: '2026-10-08T10:00:00Z',
          dateModification: '2026-10-08T10:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Pompe à inspecter');
    expect(harness.routeNativeElement?.textContent).toContain('Client API');
    expect(harness.routeNativeElement?.querySelector('a.reference')?.getAttribute('href')).toBe(
      '/demandes/DT-42',
    );
    http.expectNone(
      (r) =>
        r.url.includes('analyse-ia') || r.url.endsWith('/clients') || r.url.endsWith('/agents'),
    );
  });
  it('restaure les filtres URL et page', async () => {
    await RouterTestingHarness.create(
      '/demandes?page=3&size=10&statut=EN_COURS&priorite=HAUTE&categorie=AUTRE&recherche=pompe&sort=titre,asc',
    );
    const r = request();
    expect(r.request.params.get('page')).toBe('3');
    expect(r.request.params.get('sort')).toBe('titre,asc');
    expect(r.request.params.get('recherche')).toBe('pompe');
    expect(r.request.params.get('statut')).toBe('EN_COURS');
    expect(r.request.params.get('priorite')).toBe('HAUTE');
    expect(r.request.params.get('categorie')).toBe('AUTRE');
    r.flush({ items: [], page: 3, size: 10, totalElements: 0, totalPages: 0 });
  });
  it('un filtre remet page à zéro et synchronise URL', async () => {
    const harness = await RouterTestingHarness.create('/demandes?page=4');
    flushEmpty(4);
    const component = harness.routeDebugElement!.componentInstance as DemandesList;
    component.filters.controls.statut.setValue('ASSIGNEE');
    await harness.fixture.whenStable();
    const r = request();
    expect(r.request.params.get('page')).toBe('0');
    expect(r.request.params.get('statut')).toBe('ASSIGNEE');
    expect(TestBed.inject(Router).url).toContain('statut=ASSIGNEE');
    r.flush({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });
  it('le paginator garde les index backend sans ajouter un', async () => {
    const harness = await RouterTestingHarness.create('/demandes');
    flushEmpty();
    (harness.routeDebugElement!.componentInstance as DemandesList).paginate({
      pageIndex: 2,
      pageSize: 10,
      length: 60,
    });
    await harness.fixture.whenStable();
    const r = request();
    expect(r.request.params.get('page')).toBe('2');
    expect(r.request.params.get('size')).toBe('10');
    r.flush({ items: [], page: 2, size: 10, totalElements: 60, totalPages: 6 });
  });
  it('annule une réponse ancienne quand URL change', async () => {
    const harness = await RouterTestingHarness.create('/demandes');
    const old = request();
    await TestBed.inject(Router).navigateByUrl('/demandes?statut=RESOLUE');
    expect(old.cancelled).toBe(true);
    flushEmpty();
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Aucune demande');
  });
  it('le retour à une URL précédente restaure sa vue serveur', async () => {
    await RouterTestingHarness.create('/demandes?statut=EN_COURS');
    flushEmpty();
    await TestBed.inject(Router).navigateByUrl('/demandes?statut=RESOLUE');
    flushEmpty();
    await TestBed.inject(Router).navigateByUrl('/demandes?statut=EN_COURS');
    const r = request();
    expect(r.request.params.get('statut')).toBe('EN_COURS');
    r.flush({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });
  it('AT ne voit pas création et ne charge aucun référentiel RT', async () => {
    const session = TestBed.inject(SessionService);
    session.establish({
      accessToken: 'test',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: { id: 2, nom: 'Agent', email: 'a@example.test', roles: ['AGENT_TECHNIQUE'] },
    });
    const harness = await RouterTestingHarness.create('/demandes');
    flushEmpty();
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).not.toContain('Nouvelle demande');
    http.expectNone((r) => r.url.endsWith('/clients') || r.url.endsWith('/agents'));
  });
  it('loading puis erreur 403 conserve la session et permet une reprise sans polluer URL', async () => {
    const harness = await RouterTestingHarness.create('/demandes');
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Chargement');
    request().flush(
      { code: 'ACCES_INTERDIT', message: 'Accès refusé par API' },
      { status: 403, statusText: 'Forbidden' },
    );
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Accès refusé par API');
    expect(TestBed.inject(SessionService).token()).toBe('test');
    (harness.routeDebugElement!.componentInstance as DemandesList).retry();
    flushEmpty();
    expect(TestBed.inject(Router).url).toBe('/demandes');
  });
  it('normalise les paramètres invalides sans requête invalide', async () => {
    await RouterTestingHarness.create(
      '/demandes?page=-1&size=0&statut=INVALIDE&sort=client.nom,asc',
    );
    const r = request();
    expect(r.request.params.get('page')).toBe('0');
    expect(r.request.params.get('size')).toBe('20');
    expect(r.request.params.has('statut')).toBe(false);
    expect(r.request.params.get('sort')).toBe('dateCreation,desc');
    r.flush({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });
  it('une navigation annule aussi une recherche encore en debounce', async () => {
    const harness = await RouterTestingHarness.create('/demandes?page=3');
    flushEmpty(3);
    const page = harness.routeDebugElement!.componentInstance as DemandesList;
    page.filters.controls.recherche.setValue('Recherche en attente');
    await TestBed.inject(Router).navigateByUrl('/demandes?page=2&statut=RESOLUE');
    flushEmpty(2);
    await new Promise((resolve) => setTimeout(resolve, 400));
    http.expectNone((r) => r.url === base + '/demandes');
    expect(TestBed.inject(Router).url).toContain('page=2');
    expect(page.filters.controls.recherche.value).toBe('');
  });
  it('changer le tri repart à zéro avec tri serveur', async () => {
    const harness = await RouterTestingHarness.create('/demandes?page=3');
    flushEmpty(3);
    (harness.routeDebugElement!.componentInstance as DemandesList).filters.controls.sort.setValue(
      'titre,asc',
    );
    await harness.fixture.whenStable();
    const r = request();
    expect(r.request.params.get('page')).toBe('0');
    expect(r.request.params.get('sort')).toBe('titre,asc');
    r.flush({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });
});
