import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';
import { DemandeCreate } from './demande-create';
import { SessionService } from '../../../core/session/session';
import { authorizationInterceptor } from '../../../core/http/authorization-interceptor';
import { environment } from '../../../../environments/environment';
describe('Création de demande', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<DemandeCreate>;
  const base = environment.apiBaseUrl;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DemandeCreate],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authorizationInterceptor])),
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
  afterEach(() => {
    http.verify({ ignoreCancelled: true });
    vi.restoreAllMocks();
  });
  function setup() {
    fixture = TestBed.createComponent(DemandeCreate);
    fixture.detectChanges();
    http
      .expectOne((r) => r.url === base + '/clients')
      .flush({ items: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
    fixture.componentInstance.form.patchValue({
      titre: 'Titre',
      description: 'Description',
      categorie: 'AUTRE',
      priorite: 'MOYENNE',
    });
    return fixture.componentInstance;
  }
  function creation() {
    return http.expectOne(base + '/demandes');
  }
  it('client existant : envoie clientId exclusivement et suit référence serveur', () => {
    const page = setup();
    page.selectClient({ id: 7, nom: 'Fixture client', email: 'c@example.test', telephone: '123' });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    page.submit();
    const r = creation();
    expect(r.request.body).toEqual({
      titre: 'Titre',
      description: 'Description',
      categorie: 'AUTRE',
      priorite: 'MOYENNE',
      clientId: 7,
    });
    r.flush({ reference: 'DT-SERVEUR' }, { status: 201, statusText: 'Created' });
    expect(navigate).toHaveBeenCalledWith(['/demandes', 'DT-SERVEUR'], {
      state: { demandeCreated: 'DT-SERVEUR' },
    });
  });
  it('nouveau client : omet clientId même si un client avait été sélectionné', () => {
    const page = setup();
    page.form.controls.clientId.setValue(7);
    page.form.controls.clientMode.setValue('new');
    page.form.controls.nouveauClient.setValue({
      nom: 'Nouveau',
      email: 'new@example.test',
      telephone: '+216 123',
    });
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    page.submit();
    const r = creation();
    expect(r.request.body.nouveauClient).toEqual({
      nom: 'Nouveau',
      email: 'new@example.test',
      telephone: '+216 123',
    });
    expect(r.request.body).not.toHaveProperty('clientId');
    r.flush({ reference: 'DT-NOUVEAU' }, { status: 201, statusText: 'Created' });
  });
  it('oblige la sélection du client et rejette les espaces seuls', () => {
    const page = setup();
    page.form.controls.titre.setValue('   ');
    page.submit();
    http.expectNone(base + '/demandes');
    expect(page.form.touched).toBe(true);
    expect(page.form.controls.clientId.invalid).toBe(true);
    expect(page.form.controls.titre.invalid).toBe(true);
  });
  it('valide email du nouveau client sans inventer une règle téléphone', () => {
    const page = setup();
    page.form.controls.clientMode.setValue('new');
    page.form.controls.nouveauClient.setValue({
      nom: 'Nouveau',
      email: 'invalide',
      telephone: 'extension 12',
    });
    page.submit();
    http.expectNone(base + '/demandes');
    expect(page.form.controls.nouveauClient.controls.email.invalid).toBe(true);
    expect(page.form.controls.nouveauClient.controls.telephone.valid).toBe(true);
  });
  it('loading bloque double envoi et les champs puis conserve saisie après erreur', () => {
    const page = setup();
    page.form.controls.clientId.setValue(7);
    page.submit();
    page.submit();
    const r = creation();
    expect(page.loading()).toBe(true);
    expect(page.form.disabled).toBe(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    r.flush(
      { code: 'REQUETE_INVALIDE', message: 'Erreur API' },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(page.loading()).toBe(false);
    expect(page.form.controls.titre.value).toBe('Titre');
    expect(page.form.controls.nouveauClient.disabled).toBe(true);
    expect(page.form.controls.clientId.enabled).toBe(true);
  });
  it('rattache fieldErrors aux contrôles et garde les erreurs non reconnues visibles', () => {
    const page = setup();
    page.form.controls.clientMode.setValue('new');
    page.form.controls.nouveauClient.setValue({
      nom: 'Nouveau',
      email: 'new@example.test',
      telephone: '123',
    });
    page.submit();
    creation().flush(
      {
        code: 'VALIDATION',
        message: 'Saisie invalide',
        fieldErrors: [
          { field: 'nouveauClient.email', code: 'EMAIL_INVALIDE', message: 'Email refusé' },
          { field: 'autre', code: 'INVALIDE', message: 'Autre problème' },
        ],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(page.form.controls.nouveauClient.controls.email.getError('server')).toBe('Email refusé');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Autre problème');
  });
  it('403 conserve la session', () => {
    const page = setup();
    page.form.controls.clientId.setValue(7);
    page.submit();
    creation().flush(
      { code: 'ACCES_INTERDIT', message: 'Création interdite' },
      { status: 403, statusText: 'Forbidden' },
    );
    expect(TestBed.inject(SessionService).token()).toBe('test');
    expect(page.error()?.code).toBe('ACCES_INTERDIT');
  });
  it('recherche client attend le debounce et reste paginée', async () => {
    const page = setup();
    page.search.setValue('Dupont');
    http.expectNone((r) => r.url === base + '/clients');
    await new Promise((resolve) => setTimeout(resolve, 400));
    const r = http.expectOne((r) => r.url === base + '/clients');
    expect(r.request.params.get('recherche')).toBe('Dupont');
    expect(r.request.params.get('size')).toBe('10');
    expect(r.request.params.get('page')).toBe('0');
    r.flush({ items: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucun client trouvé');
  });
  it('annule la recherche en passant à nouveau client', () => {
    fixture = TestBed.createComponent(DemandeCreate);
    const old = http.expectOne((r) => r.url === base + '/clients');
    fixture.componentInstance.form.controls.clientMode.setValue('new');
    expect(old.cancelled).toBe(true);
    expect(fixture.componentInstance.clientsLoading()).toBe(false);
  });
  it('erreur de recherche affiche une reprise explicite', () => {
    fixture = TestBed.createComponent(DemandeCreate);
    http
      .expectOne((r) => r.url === base + '/clients')
      .flush(
        { code: 'ERREUR', message: 'Recherche indisponible' },
        { status: 500, statusText: 'Error' },
      );
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Recherche indisponible');
    fixture.componentInstance.searchClients();
    http
      .expectOne((r) => r.url === base + '/clients')
      .flush({ items: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  });
  it('ne charge ni clients ni IA pour AT', () => {
    TestBed.inject(SessionService).establish({
      accessToken: 'test',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: { id: 2, nom: 'Agent', email: 'a@example.test', roles: ['AGENT_TECHNIQUE'] },
    });
    fixture = TestBed.createComponent(DemandeCreate);
    fixture.componentInstance.submit();
    http.expectNone(() => true);
  });
});
