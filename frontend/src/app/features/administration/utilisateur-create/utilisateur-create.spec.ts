import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';
import { UtilisateurCreate } from './utilisateur-create';
import { SessionService } from '../../../core/session/session';
import { authorizationInterceptor } from '../../../core/http/authorization-interceptor';
import { environment } from '../../../../environments/environment';
describe('Création utilisateur F3', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<UtilisateurCreate>;
  const base = environment.apiBaseUrl + '/utilisateurs';
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [UtilisateurCreate],
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
      user: { id: 1, nom: 'Admin test', email: 'adm@example.test', roles: ['ADMINISTRATEUR'] },
    });
    fixture = TestBed.createComponent(UtilisateurCreate);
    fixture.detectChanges();
    fixture.componentInstance.form.patchValue({
      nom: 'Utilisateur test',
      email: 'test@example.test',
      actif: false,
      password: 'test-only-password',
    });
  });
  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
  });
  const creation = () => http.expectOne(base);
  for (const [responsable, agent, expected] of [
    [false, false, []],
    [true, false, ['RESPONSABLE_TECHNIQUE']],
    [false, true, ['AGENT_TECHNIQUE']],
    [true, true, ['RESPONSABLE_TECHNIQUE', 'AGENT_TECHNIQUE']],
  ] as const)
    it('création avec rôles métier ' + JSON.stringify(expected), () => {
      const page = fixture.componentInstance;
      page.form.controls.rolesMetier.setValue({ responsable, agent });
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
      page.submit();
      const r = creation();
      expect(r.request.body).toEqual({
        nom: 'Utilisateur test',
        email: 'test@example.test',
        actif: false,
        password: 'test-only-password',
        rolesMetier: [...expected],
      });
      expect(r.request.body.rolesMetier).not.toContain('ADMINISTRATEUR');
      r.flush(
        {
          id: 42,
          nom: 'Utilisateur test',
          email: 'test@example.test',
          actif: false,
          roles: [...expected],
        },
        { status: 201, statusText: 'Created' },
      );
      expect(page.form.controls.password.value).toBe('');
      expect(page.success()).toBe('Utilisateur créé.');
      expect(navigate).toHaveBeenCalledWith(['/administration/utilisateurs', 42, 'modifier'], {
        state: { utilisateurCreated: 42 },
      });
      expect(TestBed.inject(SessionService).user()).not.toHaveProperty('password');
    });
  it('ADMINISTRATEUR jamais proposé comme contrôle sélectionnable', () => {
    const page = fixture.componentInstance;
    expect(Object.keys(page.form.controls.rolesMetier.controls)).toEqual(['responsable', 'agent']);
    expect(fixture.nativeElement.textContent).not.toContain('Administrateur');
    expect(
      fixture.nativeElement.querySelector('input[type=password]').getAttribute('autocomplete'),
    ).toBe('new-password');
  });
  it('bloque nom blanc, email invalide et password blanc', () => {
    const page = fixture.componentInstance;
    page.form.patchValue({ nom: '   ', email: 'invalide', password: '   ' });
    page.submit();
    http.expectNone(base);
    expect(page.form.touched).toBe(true);
    expect(page.form.invalid).toBe(true);
  });
  it('ne fixe aucune longueur minimale frontend arbitraire', () => {
    const page = fixture.componentInstance;
    page.form.controls.password.setValue('x');
    page.submit();
    const r = creation();
    expect(r.request.body.password).toBe('x');
    r.flush(
      {
        code: 'MOT_DE_PASSE_INVALIDE',
        message: 'Le mot de passe ne respecte pas la politique minimale.',
      },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(page.form.controls.password.getError('server')).toContain('politique minimale');
  });
  it('ne transforme pas le mot de passe saisi avant transmission', () => {
    const page = fixture.componentInstance;
    page.form.controls.password.setValue(' test-only-password ');
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    page.submit();
    const r = creation();
    expect(r.request.body.password).toBe(' test-only-password ');
    r.flush(
      { id: 42, nom: 'Test', email: 'test@example.test', actif: true, roles: [] },
      { status: 201, statusText: 'Created' },
    );
  });
  it('double soumission et saisies bloquées seulement pendant création', () => {
    const page = fixture.componentInstance;
    page.submit();
    page.submit();
    const r = creation();
    expect(page.loading()).toBe(true);
    expect(page.form.disabled).toBe(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    r.flush(
      { code: 'CONFLIT', message: 'Conflit de données.' },
      { status: 409, statusText: 'Conflict' },
    );
    expect(page.loading()).toBe(false);
    expect(page.form.enabled).toBe(true);
    expect(page.form.controls.nom.value).toBe('Utilisateur test');
  });
  it('EMAIL_DEJA_UTILISE rattache erreur près email et préserve autres champs', () => {
    const page = fixture.componentInstance;
    page.form.controls.rolesMetier.controls.agent.setValue(true);
    page.submit();
    creation().flush(
      { code: 'EMAIL_DEJA_UTILISE', message: 'Cet email est déjà utilisé.' },
      { status: 409, statusText: 'Conflict' },
    );
    expect(page.form.controls.email.getError('server')).toBe('Cet email est déjà utilisé.');
    expect(page.form.controls.nom.value).toBe('Utilisateur test');
    expect(page.form.controls.actif.value).toBe(false);
    expect(page.form.controls.rolesMetier.controls.agent.value).toBe(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Cet email est déjà utilisé.');
  });
  it('MOT_DE_PASSE_INVALIDE affiche message près password sans règle inventée', () => {
    const page = fixture.componentInstance;
    page.submit();
    creation().flush(
      {
        code: 'MOT_DE_PASSE_INVALIDE',
        message: 'Le mot de passe ne respecte pas la politique minimale.',
      },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(page.form.controls.password.getError('server')).toBe(
      'Le mot de passe ne respecte pas la politique minimale.',
    );
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('politique minimale');
  });
  it('400 VALIDATION rattache fieldErrors et garde champs inconnus visibles', () => {
    const page = fixture.componentInstance;
    page.submit();
    creation().flush(
      {
        code: 'VALIDATION',
        message: 'Requête invalide.',
        fieldErrors: [
          { field: 'nom', code: 'OBLIGATOIRE', message: 'Nom requis' },
          { field: 'autre', code: 'INVALIDE', message: 'Autre erreur' },
        ],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(page.form.controls.nom.getError('server')).toBe('Nom requis');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Autre erreur');
  });
  it('ROLE_METIER_INVALIDE conserve choix et message compréhensible', () => {
    const page = fixture.componentInstance;
    page.submit();
    creation().flush(
      { code: 'ROLE_METIER_INVALIDE', message: 'Rôle incorrect' },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(page.error()?.message).toBe('Les rôles sélectionnés ne sont pas valides.');
  });
  it('403 conserve session', () => {
    const page = fixture.componentInstance;
    page.submit();
    creation().flush(
      { code: 'ACCES_INTERDIT', message: 'Accès refusé.' },
      { status: 403, statusText: 'Forbidden' },
    );
    expect(TestBed.inject(SessionService).token()).toBe('test');
    expect(page.error()?.code).toBe('ACCES_INTERDIT');
  });
  it('destruction de page annule requête en attente', () => {
    fixture.componentInstance.submit();
    const r = creation();
    fixture.destroy();
    expect(r.cancelled).toBe(true);
  });
});
