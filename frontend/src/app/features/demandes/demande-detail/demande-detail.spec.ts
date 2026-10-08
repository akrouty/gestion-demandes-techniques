import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { DemandeDetail } from './demande-detail';
import { SessionService } from '../../../core/session/session';
import { Role } from '../../../core/session/session-models';
import { authorizationInterceptor } from '../../../core/http/authorization-interceptor';
import { DemandeDetailResponse } from '../demande-models';
import { environment } from '../../../../environments/environment';
describe('Détail et actions de demande', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;
  let page: DemandeDetail;
  const base = environment.apiBaseUrl + '/demandes/DT-42';
  const fixtureData = (changes: Partial<DemandeDetailResponse> = {}): DemandeDetailResponse => ({
    reference: 'DT-42',
    titre: 'Demande de test',
    description: 'Description API',
    categorie: 'AUTRE',
    priorite: 'MOYENNE',
    statut: 'EN_COURS',
    client: { id: 3, nom: 'Client test', email: 'client@example.test', telephone: '123' },
    createur: {
      id: 1,
      nom: 'Responsable test',
      email: 'rt@example.test',
      actif: true,
      roles: ['RESPONSABLE_TECHNIQUE'],
    },
    agentAffecte: {
      id: 2,
      nom: 'Agent test',
      email: 'at@example.test',
      actif: true,
      roles: ['AGENT_TECHNIQUE'],
    },
    descriptionTraitement: 'Diagnostic enregistré',
    solution: null,
    motifAnnulation: null,
    dateCreation: '2026-10-08T10:00:00Z',
    dateModification: '2026-10-08T11:00:00Z',
    dateResolution: null,
    dateCloture: null,
    dateAnnulation: null,
    ...changes,
  });
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'demandes/:reference', component: DemandeDetail }]),
        provideHttpClient(withInterceptors([authorizationInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.expectNone((r) => r.url.includes('analyse-ia'));
    http.verify({ ignoreCancelled: true });
  });
  const login = (roles: Role[] = ['RESPONSABLE_TECHNIQUE', 'AGENT_TECHNIQUE']) =>
    TestBed.inject(SessionService).establish({
      accessToken: 'test-token',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: { id: 2, nom: 'Test', email: 'test@example.test', roles },
    });
  async function setup(changes: Partial<DemandeDetailResponse> = {}, roles?: Role[]) {
    login(roles);
    harness = await RouterTestingHarness.create('/demandes/DT-42');
    page = harness.routeDebugElement!.componentInstance as DemandeDetail;
    http.expectOne(base).flush(fixtureData(changes));
    harness.detectChanges();
  }
  const action = (suffix: string) => http.expectOne(base + '/' + suffix);
  it('affiche les sections et omet les dates absentes', async () => {
    await setup();
    const text = harness.routeNativeElement!.textContent!;
    for (const value of [
      'DT-42',
      'Description API',
      'Client test',
      'Diagnostic enregistré',
      'Informations générales',
      'Affectation',
    ])
      expect(text).toContain(value);
    expect(text).not.toContain('null');
    expect(text).not.toContain('Invalid Date');
  });
  it('loading distingue le chargement du contenu', async () => {
    login();
    harness = await RouterTestingHarness.create('/demandes/DT-42');
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Chargement de la demande');
    http.expectOne(base).flush(fixtureData());
  });
  it('RT voit qualification et affectation mais pas traitement AT', async () => {
    await setup({}, ['RESPONSABLE_TECHNIQUE']);
    const text = harness.routeNativeElement!.textContent!;
    expect(text).toContain('Enregistrer la qualification');
    expect(text).toContain('Afficher les agents disponibles');
    expect(text).not.toContain('Enregistrer le traitement');
  });
  it('AT voit le traitement sans charger clients ou agents RT', async () => {
    await setup({}, ['AGENT_TECHNIQUE']);
    const text = harness.routeNativeElement!.textContent!;
    expect(text).toContain('Enregistrer le traitement');
    expect(text).not.toContain('Enregistrer la qualification');
    expect(text).not.toContain('Afficher les agents disponibles');
    page.loadAgents();
    http.expectNone((r) => r.url.endsWith('/clients') || r.url.endsWith('/agents'));
  });
  for (const statut of ['CLOTUREE', 'ANNULEE'] as const)
    it('état terminal ' + statut + ' sans édition', async () => {
      await setup({ statut });
      const text = harness.routeNativeElement!.textContent!;
      expect(text).not.toContain('Enregistrer la qualification');
      expect(text).not.toContain('Enregistrer le traitement');
      expect(text).not.toContain('Annuler la demande');
    });
  it('qualification utilise la réponse sans changer un statut localement', async () => {
    await setup();
    page.qualification.setValue({ categorie: 'NOTE_CALCUL', priorite: 'HAUTE' });
    page.qualify();
    const r = action('qualification');
    expect(r.request.body).toEqual({ categorie: 'NOTE_CALCUL', priorite: 'HAUTE' });
    expect(page.demande()?.categorie).toBe('AUTRE');
    expect(page.busy()).toBe(true);
    r.flush(fixtureData({ categorie: 'NOTE_CALCUL', priorite: 'HAUTE' }));
    expect(page.demande()?.categorie).toBe('NOTE_CALCUL');
    expect(page.success()).toContain('Qualification');
    expect(page.busy()).toBe(false);
  });
  it('affectation charge agents réels à la demande et envoie id seulement', async () => {
    await setup({ statut: 'NOUVELLE', agentAffecte: null });
    page.loadAgents();
    http
      .expectOne(environment.apiBaseUrl + '/agents')
      .flush([{ id: 8, nom: 'Agent disponible', email: 'a@example.test', actif: true }]);
    page.affectation.controls.agentId.setValue(8);
    page.assign();
    const r = action('affectation');
    expect(r.request.body).toEqual({ agentId: 8 });
    expect(page.demande()?.statut).toBe('NOUVELLE');
    r.flush(
      fixtureData({
        statut: 'ASSIGNEE',
        agentAffecte: {
          id: 8,
          nom: 'Agent disponible',
          email: 'a@example.test',
          actif: true,
          roles: ['AGENT_TECHNIQUE'],
        },
      }),
    );
    expect(page.demande()?.statut).toBe('ASSIGNEE');
  });
  it('agents vides et échec restent explicites', async () => {
    await setup();
    page.loadAgents();
    http
      .expectOne(environment.apiBaseUrl + '/agents')
      .flush(
        { code: 'ERREUR', message: 'Liste indisponible' },
        { status: 500, statusText: 'Error' },
      );
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Liste indisponible');
    page.loadAgents();
    http.expectOne(environment.apiBaseUrl + '/agents').flush([]);
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Aucun agent actif disponible');
  });
  it('démarrage attend la réponse serveur', async () => {
    await setup({ statut: 'ASSIGNEE' }, ['AGENT_TECHNIQUE']);
    page.start();
    const r = action('demarrage-traitement');
    expect(r.request.body).toBeNull();
    expect(page.demande()?.statut).toBe('ASSIGNEE');
    r.flush(fixtureData({ statut: 'EN_COURS' }));
    expect(page.demande()?.statut).toBe('EN_COURS');
  });
  it('PATCH partiel omet le traitement inchangé', async () => {
    await setup({}, ['AGENT_TECHNIQUE']);
    page.traitement.controls.solution.setValue('Solution saisie');
    page.saveTreatment();
    const r = action('traitement');
    expect(r.request.body).toEqual({ solution: 'Solution saisie' });
    expect(page.demande()?.solution).toBeNull();
    expect(page.canResolve()).toBe(false);
    r.flush(fixtureData({ solution: 'Solution enregistrée serveur' }));
    expect(page.traitement.controls.solution.value).toBe('Solution enregistrée serveur');
    expect(page.canResolve()).toBe(true);
  });
  it('PATCH description seule laisse solution absente', async () => {
    await setup({}, ['AGENT_TECHNIQUE']);
    page.traitement.controls.descriptionTraitement.setValue('Nouveau diagnostic');
    page.saveTreatment();
    const r = action('traitement');
    expect(r.request.body).toEqual({ descriptionTraitement: 'Nouveau diagnostic' });
    r.flush(fixtureData({ descriptionTraitement: 'Nouveau diagnostic' }));
  });
  it('PATCH vide ou inchangé ne part pas', async () => {
    await setup({}, ['AGENT_TECHNIQUE']);
    page.saveTreatment();
    http.expectNone(base + '/traitement');
    page.traitement.controls.descriptionTraitement.setValue('   ');
    page.saveTreatment();
    http.expectNone(base + '/traitement');
    expect(page.traitement.controls.descriptionTraitement.invalid).toBe(true);
    expect(page.error()?.message).toContain('non vide');
  });
  it('résolution bloquée pour solution non enregistrée ou traitement modifié', async () => {
    await setup({}, ['AGENT_TECHNIQUE']);
    page.traitement.controls.solution.setValue('Saisie locale');
    page.resolve();
    http.expectNone(base + '/resolution');
    page.reload();
    http.expectOne(base).flush(fixtureData({ solution: 'Solution enregistrée' }));
    page.traitement.controls.descriptionTraitement.setValue('Modification');
    expect(page.canResolve()).toBe(false);
    page.resolve();
    http.expectNone(base + '/resolution');
  });
  it('résolution avec solution backend attend le résultat API', async () => {
    await setup({ solution: 'Solution enregistrée' }, ['AGENT_TECHNIQUE']);
    expect(page.canResolve()).toBe(true);
    page.resolve();
    const r = action('resolution');
    expect(r.request.body).toBeNull();
    expect(page.demande()?.statut).toBe('EN_COURS');
    r.flush(
      fixtureData({
        statut: 'RESOLUE',
        solution: 'Solution enregistrée',
        dateResolution: '2026-10-08T12:00:00Z',
      }),
    );
    expect(page.demande()?.statut).toBe('RESOLUE');
  });
  it('clôture de RESOLUE utilise endpoint dédié', async () => {
    await setup({ statut: 'RESOLUE', solution: 'Solution' }, ['RESPONSABLE_TECHNIQUE']);
    page.close();
    action('cloture').flush(fixtureData({ statut: 'CLOTUREE' }));
    expect(page.demande()?.statut).toBe('CLOTUREE');
  });
  it('refus de résolution sans motif inventé', async () => {
    await setup({ statut: 'RESOLUE', solution: 'Solution' }, ['RESPONSABLE_TECHNIQUE']);
    page.refuse();
    const r = action('refus-resolution');
    expect(r.request.body).toBeNull();
    r.flush(fixtureData({ statut: 'EN_COURS' }));
    expect(page.demande()?.statut).toBe('EN_COURS');
  });
  it('annulation exige motif non vide et confirmation', async () => {
    await setup({}, ['RESPONSABLE_TECHNIQUE']);
    page.cancel();
    http.expectNone(base + '/annulation');
    page.annulation.setValue({ motif: 'Motif', confirme: false });
    page.cancel();
    http.expectNone(base + '/annulation');
    page.annulation.setValue({ motif: '   ', confirme: true });
    page.cancel();
    http.expectNone(base + '/annulation');
    page.annulation.setValue({ motif: 'Motif réel', confirme: true });
    page.cancel();
    const r = action('annulation');
    expect(r.request.body).toEqual({ motif: 'Motif réel' });
    r.flush(fixtureData({ statut: 'ANNULEE', motifAnnulation: 'Motif réel' }));
    expect(page.demande()?.statut).toBe('ANNULEE');
  });
  it('double action est bloquée tant que mutation charge', async () => {
    await setup({ statut: 'ASSIGNEE' }, ['AGENT_TECHNIQUE']);
    page.start();
    page.start();
    const r = action('demarrage-traitement');
    expect(page.busy()).toBe(true);
    harness.detectChanges();
    const buttons = Array.from(
      harness.routeNativeElement!.querySelectorAll<HTMLButtonElement>('button'),
    );
    expect(buttons.find((button) => button.textContent?.includes('Démarrer'))?.disabled).toBe(true);
    r.flush(fixtureData());
    expect(page.busy()).toBe(false);
  });
  it('403 action conserve session et accepte autorité backend pour Agent non affecté', async () => {
    await setup(
      {
        statut: 'ASSIGNEE',
        agentAffecte: {
          id: 99,
          nom: 'Autre agent',
          email: 'autre@example.test',
          actif: true,
          roles: ['AGENT_TECHNIQUE'],
        },
      },
      ['AGENT_TECHNIQUE'],
    );
    page.start();
    action('demarrage-traitement').flush(
      { code: 'ACCES_INTERDIT', message: 'Agent non affecté' },
      { status: 403, statusText: 'Forbidden' },
    );
    expect(TestBed.inject(SessionService).token()).toBe('test-token');
    expect(page.demande()?.statut).toBe('ASSIGNEE');
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Accès refusé');
  });
  for (const status of [403, 404])
    it('GET ' + status + ' remplace contenu et préserve session', async () => {
      login();
      harness = await RouterTestingHarness.create('/demandes/DT-42');
      http
        .expectOne(base)
        .flush(
          {
            code: status === 404 ? 'DEMANDE_INTROUVABLE' : 'ACCES_INTERDIT',
            message: status === 404 ? 'Demande absente' : 'Accès refusé',
          },
          { status, statusText: 'Error' },
        );
      harness.detectChanges();
      expect(harness.routeNativeElement?.textContent).toContain(
        status === 404 ? 'Demande introuvable' : 'Accès refusé',
      );
      expect(harness.routeNativeElement?.textContent).not.toContain('Informations générales');
      expect(TestBed.inject(SessionService).token()).toBe('test-token');
    });
  it('409 conserve saisie et bloque actions jusqu’au rechargement explicite', async () => {
    await setup({}, ['AGENT_TECHNIQUE']);
    page.traitement.controls.solution.setValue('Solution locale');
    page.saveTreatment();
    action('traitement').flush(
      { code: 'TRANSITION_INVALIDE', message: 'État modifié' },
      { status: 409, statusText: 'Conflict' },
    );
    expect(page.reloadRequired()).toBe(true);
    expect(page.traitement.controls.solution.value).toBe('Solution locale');
    http.expectNone(base);
    page.saveTreatment();
    http.expectNone(base + '/traitement');
    page.reload();
    http.expectOne(base).flush(fixtureData({ statut: 'RESOLUE', solution: 'Solution serveur' }));
    expect(page.reloadRequired()).toBe(false);
    expect(page.traitement.controls.solution.value).toBe('Solution serveur');
    expect(page.demande()?.statut).toBe('RESOLUE');
  });
  it('réseau incertain ne rejoue pas mutation et impose consultation serveur', async () => {
    await setup({ statut: 'ASSIGNEE' }, ['AGENT_TECHNIQUE']);
    page.start();
    action('demarrage-traitement').error(new ProgressEvent('error'));
    expect(page.reloadRequired()).toBe(true);
    page.start();
    http.expectNone(base + '/demarrage-traitement');
    expect(page.busy()).toBe(false);
  });
  it('400 rattache erreur de champ après la fin du loading', async () => {
    await setup();
    page.qualify();
    action('qualification').flush(
      {
        code: 'VALIDATION',
        message: 'Erreur saisie',
        fieldErrors: [{ field: 'categorie', code: 'INVALIDE', message: 'Catégorie refusée' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(page.qualification.controls.categorie.getError('server')).toBe('Catégorie refusée');
    expect(page.busy()).toBe(false);
  });
  it('une réponse d’action ne remplace pas une nouvelle référence', async () => {
    await setup({ statut: 'ASSIGNEE' }, ['AGENT_TECHNIQUE']);
    page.start();
    const pending = action('demarrage-traitement');
    await TestBed.inject(Router).navigateByUrl('/demandes/DT-43');
    expect(pending.cancelled).toBe(true);
    http
      .expectOne(environment.apiBaseUrl + '/demandes/DT-43')
      .flush(fixtureData({ reference: 'DT-43' }));
    expect(page.demande()?.reference).toBe('DT-43');
  });
});
