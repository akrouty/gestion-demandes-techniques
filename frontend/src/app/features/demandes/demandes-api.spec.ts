import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { DemandesApi } from './demandes-api';
import { environment } from '../../../environments/environment';
describe('Demandes API — contrat HTTP réel', () => {
  let api: DemandesApi;
  let http: HttpTestingController;
  const base = environment.apiBaseUrl;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(DemandesApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('liste paginée à partir de zéro avec tous les filtres serveur', () => {
    api
      .list({
        page: 0,
        size: 20,
        sort: 'dateCreation,desc',
        statut: 'EN_COURS',
        priorite: 'HAUTE',
        categorie: 'NOTE_CALCUL',
        recherche: 'pompe',
        clientId: 4,
        agentId: 8,
      })
      .subscribe();
    const request = http.expectOne((r) => r.url === base + '/demandes');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');
    for (const [key, value] of Object.entries({
      sort: 'dateCreation,desc',
      statut: 'EN_COURS',
      priorite: 'HAUTE',
      categorie: 'NOTE_CALCUL',
      recherche: 'pompe',
      clientId: '4',
      agentId: '8',
    }))
      expect(request.request.params.get(key)).toBe(value);
    request.flush({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });
  it('recherche les clients par pages limitées', () => {
    api.clients('Dupont', 2).subscribe();
    const request = http.expectOne((r) => r.url === base + '/clients');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.get('sort')).toBe('nom,asc');
    expect(request.request.params.get('recherche')).toBe('Dupont');
    request.flush({ items: [], page: 2, size: 10, totalElements: 0, totalPages: 0 });
  });
  it('charge seulement les agents retournés par le backend', () => {
    api.agents().subscribe();
    http.expectOne(base + '/agents').flush([]);
  });
  it('encode la référence du détail', () => {
    api.detail('DT/42').subscribe();
    http.expectOne(base + '/demandes/DT%2F42').flush({ reference: 'DT/42' });
  });
  for (const mode of ['existing', 'new'] as const)
    it('POST création avec client ' + mode, () => {
      const common = {
        titre: 'Besoin',
        description: 'Description',
        categorie: 'AUTRE' as const,
        priorite: 'BASSE' as const,
      };
      const body =
        mode === 'existing'
          ? { ...common, clientId: 7 }
          : {
              ...common,
              nouveauClient: { nom: 'Client test', email: 'c@example.test', telephone: '123' },
            };
      api.create(body).subscribe();
      const request = http.expectOne(base + '/demandes');
      expect(request.request.method).toBe('POST');
      expect(request.request.body).toEqual(body);
      request.flush({ reference: 'DT-SERVEUR' }, { status: 201, statusText: 'Created' });
    });
  it('PUT qualification sans statut', () => {
    api.qualify('DT-1', { categorie: 'NOTE_CALCUL', priorite: 'HAUTE' }).subscribe();
    const request = http.expectOne(base + '/demandes/DT-1/qualification');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ categorie: 'NOTE_CALCUL', priorite: 'HAUTE' });
    request.flush({});
  });
  it('PUT affectation avec agentId uniquement', () => {
    api.assign('DT-1', 9).subscribe();
    const request = http.expectOne(base + '/demandes/DT-1/affectation');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ agentId: 9 });
    request.flush({});
  });
  it('PATCH conserve les champs absents', () => {
    api.treatment('DT-1', { solution: 'Solution' }).subscribe();
    const request = http.expectOne(base + '/demandes/DT-1/traitement');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ solution: 'Solution' });
    request.flush({});
  });
  for (const [method, endpoint] of [
    ['start', 'demarrage-traitement'],
    ['resolve', 'resolution'],
    ['close', 'cloture'],
    ['refuse', 'refus-resolution'],
  ] as const)
    it('POST ' + endpoint + ' sans corps métier', () => {
      api[method]('DT-1').subscribe();
      const request = http.expectOne(base + '/demandes/DT-1/' + endpoint);
      expect(request.request.method).toBe('POST');
      expect(request.request.body).toBeNull();
      request.flush({});
    });
  it('POST annulation avec motif uniquement', () => {
    api.cancel('DT-1', 'Motif').subscribe();
    const request = http.expectOne(base + '/demandes/DT-1/annulation');
    expect(request.request.body).toEqual({ motif: 'Motif' });
    request.flush({});
  });
});
