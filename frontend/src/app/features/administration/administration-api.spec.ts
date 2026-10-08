import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AdministrationApi } from './administration-api';
import { environment } from '../../../environments/environment';
describe('Administration API — contrat HTTP', () => {
  let api: AdministrationApi;
  let http: HttpTestingController;
  const base = environment.apiBaseUrl + '/utilisateurs';
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AdministrationApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('GET liste page zéro sans tri ni recherche implicites', () => {
    api.lister({ page: 0, size: 20 }).subscribe();
    const r = http.expectOne((request) => request.url === base);
    expect(r.request.method).toBe('GET');
    expect(r.request.params.keys().sort()).toEqual(['page', 'size']);
    expect(r.request.params.get('page')).toBe('0');
    expect(r.request.params.get('size')).toBe('20');
    r.flush({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });
  for (const sort of ['nom,asc', 'email,desc'])
    it('GET tri serveur ' + sort, () => {
      api.lister({ page: 2, size: 10, sort }).subscribe();
      const r = http.expectOne((request) => request.url === base);
      expect(r.request.params.get('sort')).toBe(sort);
      expect(r.request.params.get('page')).toBe('2');
      r.flush({ items: [], page: 2, size: 10, totalElements: 0, totalPages: 0 });
    });
  it('GET détail retourne uniquement DTO de lecture', () => {
    let response: unknown;
    api.obtenir(7).subscribe((value) => (response = value));
    const user = {
      id: 7,
      nom: 'Test',
      email: 't@example.test',
      actif: true,
      roles: ['AGENT_TECHNIQUE'],
    };
    http.expectOne(base + '/7').flush(user);
    expect(response).toEqual(user);
    expect(response).not.toHaveProperty('password');
    expect(response).not.toHaveProperty('passwordHash');
  });
  it('POST création avec corps exact et réponse 201', () => {
    const body = {
      nom: 'Test',
      email: 't@example.test',
      actif: false,
      rolesMetier: ['AGENT_TECHNIQUE' as const],
      password: 'test-only-password',
    };
    api.creer(body).subscribe();
    const r = http.expectOne(base);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(body);
    r.flush(
      { id: 7, nom: body.nom, email: body.email, actif: false, roles: body.rolesMetier },
      { status: 201, statusText: 'Created' },
    );
  });
  it('PUT général contient nom/email exclusivement', () => {
    api.modifier(7, { nom: 'Nouveau', email: 'n@example.test' }).subscribe();
    const r = http.expectOne(base + '/7');
    expect(r.request.method).toBe('PUT');
    expect(r.request.body).toEqual({ nom: 'Nouveau', email: 'n@example.test' });
    r.flush({});
  });
  for (const method of ['activer', 'desactiver'] as const)
    it('POST ' + method + ' via endpoint dédié sans corps', () => {
      api[method](7).subscribe();
      const r = http.expectOne(
        base + '/7/' + (method === 'activer' ? 'activation' : 'desactivation'),
      );
      expect(r.request.method).toBe('POST');
      expect(r.request.body).toBeNull();
      r.flush({});
    });
  it('remplacement complet RT/AT avec PUT rôles métier', () => {
    api
      .remplacerRolesMetier(7, { rolesMetier: ['RESPONSABLE_TECHNIQUE', 'AGENT_TECHNIQUE'] })
      .subscribe();
    const r = http.expectOne(base + '/7/roles-metier');
    expect(r.request.method).toBe('PUT');
    expect(r.request.body).toEqual({ rolesMetier: ['RESPONSABLE_TECHNIQUE', 'AGENT_TECHNIQUE'] });
    r.flush({});
  });
  it('accepte le remplacement par tableau vide', () => {
    api.remplacerRolesMetier(7, { rolesMetier: [] }).subscribe();
    const r = http.expectOne(base + '/7/roles-metier');
    expect(r.request.body).toEqual({ rolesMetier: [] });
    r.flush({});
  });
});
