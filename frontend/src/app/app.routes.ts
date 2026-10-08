import { Component, inject } from '@angular/core';
import { Routes, RouterLink } from '@angular/router';
import { SessionService } from './core/session/session';
import { authGuard } from './core/guards/auth-guard';
import { roleGuard } from './core/guards/role-guard';
import { Shell } from './core/layout/shell/shell';
import { PageState } from './shared/ui/page-state/page-state';
@Component({
  imports: [PageState, RouterLink],
  template:
    '<app-page-state kind="error" title="Accès refusé" message="Votre compte ne dispose pas de cette section." /><p><a [routerLink]="session.hasDemandes() || session.hasAdministration() ? session.initialRoute() : \'/login\'">Revenir à un espace disponible</a></p>',
})
class AccessDenied {
  readonly session = inject(SessionService);
}
@Component({
  imports: [PageState, RouterLink],
  template:
    '<app-page-state kind="error" title="Page introuvable" message="Cette adresse ne correspond à aucune page disponible." /><p><a routerLink="/">Revenir à l’espace de travail</a></p>',
})
class NotFound {}
export const routes: Routes = [
  {
    path: 'login',
    title: 'Connexion · Safyron',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: () => inject(SessionService).initialRoute() },
      {
        path: 'demandes',
        title: 'Demandes · Safyron',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['RESPONSABLE_TECHNIQUE', 'AGENT_TECHNIQUE'] },
        loadComponent: () =>
          import('./features/demandes/demandes-list/demandes-list').then((m) => m.DemandesList),
      },
      {
        path: 'demandes/nouvelle',
        title: 'Nouvelle demande · Safyron',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['RESPONSABLE_TECHNIQUE'] },
        loadComponent: () =>
          import('./features/demandes/demande-create/demande-create').then((m) => m.DemandeCreate),
      },
      {
        path: 'demandes/:reference',
        title: 'Détail demande · Safyron',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['RESPONSABLE_TECHNIQUE', 'AGENT_TECHNIQUE'] },
        loadComponent: () =>
          import('./features/demandes/demande-detail/demande-detail').then((m) => m.DemandeDetail),
      },
      {
        path: 'administration/utilisateurs',
        title: 'Utilisateurs · Safyron',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['ADMINISTRATEUR'] },
        loadComponent: () =>
          import('./features/administration/utilisateurs-entry/utilisateurs-entry').then(
            (m) => m.UtilisateursEntry,
          ),
      },
      { path: 'acces-refuse', component: AccessDenied, title: 'Accès refusé · Safyron' },
      { path: '**', component: NotFound, title: 'Page introuvable · Safyron' },
    ],
  },
];
