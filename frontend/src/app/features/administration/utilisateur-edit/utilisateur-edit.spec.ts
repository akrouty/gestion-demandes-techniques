import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { UtilisateurEdit } from './utilisateur-edit';
import { SessionService } from '../../../core/session/session';
import { authorizationInterceptor } from '../../../core/http/authorization-interceptor';
import { UtilisateurDetailResponse } from '../utilisateur-models';
import { environment } from '../../../../environments/environment';
describe('Modification utilisateur F3', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;
  let page: UtilisateurEdit;
  const base = environment.apiBaseUrl + '/utilisateurs/7';
  const data = (changes: Partial<UtilisateurDetailResponse> = {}): UtilisateurDetailResponse => ({
    id: 7,
    nom: 'Utilisateur API',
    email: 'test@example.test',
    actif: true,
    roles: ['ADMINISTRATEUR', 'RESPONSABLE_TECHNIQUE'],
    ...changes,
  });
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'administration/utilisateurs/:id/modifier', component: UtilisateurEdit },
        ]),
        provideHttpClient(withInterceptors([authorizationInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(SessionService).establish({
      accessToken: 'test',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: { id: 1, nom: 'Admin test', email: 'admin@example.test', roles: ['ADMINISTRATEUR'] },
    });
  });
  afterEach(() => {
    http.expectNone(
      (r) => r.method === 'DELETE' || /password|mot-de-passe|administrateur/.test(r.url),
    );
    http.verify({ ignoreCancelled: true });
  });
  async function setup(changes: Partial<UtilisateurDetailResponse> = {}) {
    harness = await RouterTestingHarness.create('/administration/utilisateurs/7/modifier');
    page = harness.routeDebugElement!.componentInstance as UtilisateurEdit;
    http.expectOne(base).flush(data(changes));
    harness.detectChanges();
  }
  const action = (suffix: string) => http.expectOne(base + '/' + suffix);
  it('GET détail initialise uniquement champs de lecture et les sections', async () => {
    await setup();
    expect(page.identity.getRawValue()).toEqual({
      nom: 'Utilisateur API',
      email: 'test@example.test',
    });
    expect(page.rolesForm.getRawValue()).toEqual({
      rolesMetier: { responsable: true, agent: false },
    });
    const text = harness.routeNativeElement!.textContent!;
    for (const label of [
      'Informations générales',
      'État du compte',
      'Rôles',
      'Informations de lecture',
    ])
      expect(text).toContain(label);
    expect(harness.routeNativeElement?.querySelector('input[type=password]')).toBeNull();
    expect(text).not.toContain('mot de passe');
    expect(text).not.toContain('Supprimer');
    expect(page.user()).not.toHaveProperty('password');
    expect(page.user()).not.toHaveProperty('passwordHash');
  });
  it('PUT sauvegarde nom/email exclusivement et attend réponse serveur', async () => {
    await setup();
    page.identity.setValue({ nom: 'Nom saisi', email: 'nouveau@example.test' });
    page.saveIdentity();
    const r = http.expectOne(base);
    expect(r.request.method).toBe('PUT');
    expect(r.request.body).toEqual({ nom: 'Nom saisi', email: 'nouveau@example.test' });
    expect(page.user()?.nom).toBe('Utilisateur API');
    expect(page.identityBusy()).toBe(true);
    r.flush(data({ nom: 'Nom serveur', email: 'nouveau@example.test' }));
    expect(page.user()?.nom).toBe('Nom serveur');
    expect(page.identity.controls.nom.value).toBe('Nom serveur');
    expect(page.identitySuccess()).toBe('Nom et email enregistrés.');
  });
  it('nom/email invalides ne déclenchent pas PUT', async () => {
    await setup();
    page.identity.patchValue({ nom: '   ', email: 'invalide' });
    page.saveIdentity();
    http.expectNone(base);
    expect(page.identity.touched).toBe(true);
  });
  it('email dupliqué conserve saisie et rattache erreur email', async () => {
    await setup();
    page.identity.controls.email.setValue('duplique@example.test');
    page.saveIdentity();
    http
      .expectOne(base)
      .flush(
        { code: 'EMAIL_DEJA_UTILISE', message: 'Cet email est déjà utilisé.' },
        { status: 409, statusText: 'Conflict' },
      );
    expect(page.identity.controls.email.getError('server')).toBe('Cet email est déjà utilisé.');
    expect(page.identity.controls.email.value).toBe('duplique@example.test');
    expect(page.user()?.email).toBe('test@example.test');
  });
  it('fieldErrors restent rattachées après fin du loading', async () => {
    await setup();
    page.saveIdentity();
    http.expectOne(base).flush(
      {
        code: 'VALIDATION',
        message: 'Requête invalide.',
        fieldErrors: [{ field: 'nom', code: 'OBLIGATOIRE', message: 'Nom requis' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(page.identity.controls.nom.getError('server')).toBe('Nom requis');
    expect(page.identityBusy()).toBe(false);
    expect(page.identity.enabled).toBe(true);
  });
  it('activation utilise son endpoint et état reçu seulement', async () => {
    await setup({ actif: false });
    expect(harness.routeNativeElement?.textContent).toContain('Activer le compte');
    expect(harness.routeNativeElement?.textContent).not.toContain('Désactiver le compte');
    page.changeState();
    const r = action('activation');
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toBeNull();
    expect(page.user()?.actif).toBe(false);
    r.flush(data({ actif: true }));
    expect(page.user()?.actif).toBe(true);
    expect(page.stateSuccess()).toBe('Compte activé.');
  });
  it('désactivation exige confirmation puis utilise endpoint dédié', async () => {
    await setup();
    page.changeState();
    http.expectNone(base + '/desactivation');
    expect(page.confirmDeactivate.touched).toBe(true);
    page.confirmDeactivate.setValue(true);
    page.changeState();
    const r = action('desactivation');
    expect(r.request.body).toBeNull();
    expect(page.user()?.actif).toBe(true);
    r.flush(data({ actif: false }));
    expect(page.user()?.actif).toBe(false);
    expect(page.stateSuccess()).toBe('Compte désactivé.');
  });
  for (const [responsable, agent, expected] of [
    [false, false, []],
    [true, false, ['RESPONSABLE_TECHNIQUE']],
    [false, true, ['AGENT_TECHNIQUE']],
    [true, true, ['RESPONSABLE_TECHNIQUE', 'AGENT_TECHNIQUE']],
  ] as const)
    it('remplace entièrement les rôles métier par ' + JSON.stringify(expected), async () => {
      await setup();
      page.rolesForm.controls.rolesMetier.setValue({ responsable, agent });
      page.saveRoles();
      const r = action('roles-metier');
      expect(r.request.method).toBe('PUT');
      expect(r.request.body).toEqual({ rolesMetier: [...expected] });
      expect(r.request.body.rolesMetier).not.toContain('ADMINISTRATEUR');
      r.flush(data({ roles: ['ADMINISTRATEUR', ...expected] }));
      expect(page.user()?.roles).toEqual(['ADMINISTRATEUR', ...expected]);
      expect(page.hasAdministrator()).toBe(true);
      expect(page.rolesSuccess()).toBe('Rôles métier enregistrés.');
    });
  it('ADMINISTRATEUR apparaît en lecture seule sans checkbox ni action pour le retirer', async () => {
    await setup();
    const text = harness.routeNativeElement!.textContent!;
    expect(text).toContain('Administrateur');
    expect(text).toContain('lecture seule');
    const checkboxes = Array.from(harness.routeNativeElement!.querySelectorAll('mat-checkbox'));
    expect(checkboxes.some((box) => box.textContent?.includes('Administrateur'))).toBe(false);
    expect(Object.keys(page.rolesForm.controls.rolesMetier.controls)).toEqual([
      'responsable',
      'agent',
    ]);
  });
  it('identité en cours ne bloque ni rôles ni état', async () => {
    await setup();
    page.saveIdentity();
    const identityRequest = http.expectOne(base);
    expect(page.identity.disabled).toBe(true);
    expect(page.rolesForm.enabled).toBe(true);
    expect(page.confirmDeactivate.enabled).toBe(true);
    page.rolesForm.controls.rolesMetier.controls.agent.setValue(true);
    page.saveRoles();
    const rolesRequest = action('roles-metier');
    expect(page.identityBusy()).toBe(true);
    expect(page.rolesBusy()).toBe(true);
    expect(page.stateBusy()).toBe(false);
    rolesRequest.flush(
      data({ roles: ['ADMINISTRATEUR', 'RESPONSABLE_TECHNIQUE', 'AGENT_TECHNIQUE'] }),
    );
    identityRequest.flush(data());
    expect(page.rolesSuccess()).toBe('Rôles métier enregistrés.');
    expect(page.identitySuccess()).toBe('Nom et email enregistrés.');
  });
  it('réponses concurrentes ne rétablissent pas anciennes valeurs des autres sections', async () => {
    await setup();
    page.identity.controls.nom.setValue('Nouveau nom');
    page.saveIdentity();
    const identityRequest = http.expectOne(base);
    page.rolesForm.controls.rolesMetier.setValue({ responsable: false, agent: true });
    page.saveRoles();
    const rolesRequest = action('roles-metier');
    rolesRequest.flush(data({ roles: ['ADMINISTRATEUR', 'AGENT_TECHNIQUE'] }));
    identityRequest.flush(
      data({ nom: 'Nouveau nom', roles: ['ADMINISTRATEUR', 'RESPONSABLE_TECHNIQUE'] }),
    );
    expect(page.user()?.nom).toBe('Nouveau nom');
    expect(page.user()?.roles).toEqual(['ADMINISTRATEUR', 'AGENT_TECHNIQUE']);
  });
  it('une action état préserve les saisies non enregistrées des autres sections', async () => {
    await setup({ actif: false });
    page.identity.controls.nom.setValue('Saisie locale');
    page.rolesForm.controls.rolesMetier.controls.agent.setValue(true);
    page.changeState();
    action('activation').flush(data({ actif: true }));
    expect(page.identity.controls.nom.value).toBe('Saisie locale');
    expect(page.rolesForm.controls.rolesMetier.controls.agent.value).toBe(true);
    expect(page.identitySuccess()).toBe('');
    expect(page.rolesSuccess()).toBe('');
  });
  it('double sauvegarde identité ne déclenche qu’une requête', async () => {
    await setup();
    page.saveIdentity();
    page.saveIdentity();
    http.expectOne(base).flush(data());
    expect(page.identityBusy()).toBe(false);
  });
  it('double remplacement rôles ne déclenche qu’une requête', async () => {
    await setup();
    page.saveRoles();
    page.saveRoles();
    action('roles-metier').flush(data());
    expect(page.rolesBusy()).toBe(false);
  });
  it('double activation ne déclenche qu’une requête', async () => {
    await setup({ actif: false });
    page.changeState();
    page.changeState();
    action('activation').flush(data());
    expect(page.stateBusy()).toBe(false);
  });
  for (const status of [403, 404])
    it('GET ' + status + ' montre état explicite et conserve session', async () => {
      harness = await RouterTestingHarness.create('/administration/utilisateurs/7/modifier');
      http.expectOne(base).flush(
        {
          code: status === 404 ? 'UTILISATEUR_INTROUVABLE' : 'ACCES_INTERDIT',
          message: status === 404 ? 'Utilisateur introuvable.' : 'Accès refusé.',
        },
        { status, statusText: 'Error' },
      );
      harness.detectChanges();
      expect(harness.routeNativeElement?.textContent).toContain(
        status === 404 ? 'Utilisateur introuvable' : 'Accès refusé',
      );
      expect(harness.routeNativeElement?.querySelector('form')).toBeNull();
      expect(TestBed.inject(SessionService).token()).toBe('test');
    });
  it('403 sur mutation conserve session et saisie', async () => {
    await setup();
    page.identity.controls.nom.setValue('Nom local');
    page.saveIdentity();
    http
      .expectOne(base)
      .flush(
        { code: 'ACCES_INTERDIT', message: 'Accès refusé.' },
        { status: 403, statusText: 'Forbidden' },
      );
    expect(TestBed.inject(SessionService).token()).toBe('test');
    expect(page.identity.controls.nom.value).toBe('Nom local');
    expect(page.identityError()?.message).toBe('Accès refusé.');
  });
  it('404 sur mutation remplace détail par utilisateur introuvable', async () => {
    await setup();
    page.saveRoles();
    action('roles-metier').flush(
      { code: 'UTILISATEUR_INTROUVABLE', message: 'Utilisateur introuvable.' },
      { status: 404, statusText: 'Not Found' },
    );
    expect(page.user()).toBeNull();
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Utilisateur introuvable');
  });
  it('409 CONFLIT conserve saisie et propose consultation contrôlée', async () => {
    await setup();
    page.identity.controls.nom.setValue('Nom local');
    page.saveIdentity();
    http
      .expectOne(base)
      .flush(
        { code: 'CONFLIT', message: 'Conflit avec l’état actuel des données.' },
        { status: 409, statusText: 'Conflict' },
      );
    expect(page.identityError()?.message).toContain('Conflit');
    expect(page.identityReload()).toBe(true);
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('doit être vérifié sur le serveur');
    expect(page.identity.controls.nom.value).toBe('Nom local');
    http.expectNone(base);
    page.saveIdentity();
    http.expectNone(base);
    page.reload();
    http.expectOne(base).flush(data({ nom: 'Nom serveur' }));
    expect(page.identity.controls.nom.value).toBe('Nom serveur');
    expect(page.identityReload()).toBe(false);
  });
  it('ROLE_METIER_INVALIDE affiche erreur sans changer rôles reçus', async () => {
    await setup();
    page.saveRoles();
    action('roles-metier').flush(
      { code: 'ROLE_METIER_INVALIDE', message: 'Rôle interdit' },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(page.rolesError()?.message).toBe('Les rôles sélectionnés ne sont pas valides.');
    expect(page.user()?.roles).toEqual(['ADMINISTRATEUR', 'RESPONSABLE_TECHNIQUE']);
  });
  it('réseau incertain ne rejoue pas une activation automatiquement', async () => {
    await setup({ actif: false });
    page.changeState();
    action('activation').error(new ProgressEvent('error'));
    expect(page.stateReload()).toBe(true);
    page.changeState();
    http.expectNone(base + '/activation');
    expect(page.user()?.actif).toBe(false);
  });
  it('nouvelle ressource annule ancienne mutation', async () => {
    await setup();
    page.saveIdentity();
    const old = http.expectOne(base);
    await TestBed.inject(Router).navigateByUrl('/administration/utilisateurs/8/modifier');
    expect(old.cancelled).toBe(true);
    http
      .expectOne(environment.apiBaseUrl + '/utilisateurs/8')
      .flush(data({ id: 8, nom: 'Autre utilisateur' }));
    expect(page.user()?.id).toBe(8);
  });
  it('id de route invalide n’invente aucune requête', async () => {
    harness = await RouterTestingHarness.create('/administration/utilisateurs/invalide/modifier');
    http.expectNone(() => true);
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Utilisateur introuvable');
  });
});
