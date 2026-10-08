import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { LoginResponse, Role } from './session-models';

/** Snapshot pour l'UX uniquement : les autorisations restent côté backend. */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly router = inject(Router);
  private readonly current = signal<LoginResponse | null>(null);
  private revision = 0;
  readonly user = computed(() => this.current()?.user ?? null);
  readonly roles = computed(() => this.user()?.roles ?? []);
  readonly hasDemandes = computed(() =>
    this.hasAnyRole(['RESPONSABLE_TECHNIQUE', 'AGENT_TECHNIQUE']),
  );
  readonly hasAdministration = computed(() => this.hasAnyRole(['ADMINISTRATEUR']));
  readonly initialRoute = computed(() =>
    this.hasDemandes()
      ? '/demandes'
      : this.hasAdministration()
        ? '/administration/utilisateurs'
        : '/acces-refuse',
  );
  get generation(): number {
    return this.revision;
  }
  establish(response: LoginResponse): void {
    if (
      !response.accessToken ||
      response.tokenType !== 'Bearer' ||
      !Number.isFinite(Date.parse(response.expiresAt)) ||
      Date.parse(response.expiresAt) <= Date.now()
    ) {
      throw new Error('Réponse de connexion inutilisable');
    }
    this.revision++;
    this.current.set({ ...response, user: { ...response.user, roles: [...response.user.roles] } });
  }
  token(): string | null {
    const session = this.current();
    if (!session) return null;
    if (Date.parse(session.expiresAt) <= Date.now()) {
      this.clear();
      return null;
    }
    return session.accessToken;
  }
  hasAnyRole(roles: readonly Role[]): boolean {
    return roles.some((role) => this.roles().includes(role));
  }
  clear(): void {
    this.revision++;
    this.current.set(null);
  }
  logout(): void {
    this.clear();
    void this.router.navigateByUrl('/login');
  }
  invalidate(token: string | null): void {
    // Une ancienne requête ne doit pas déconnecter une nouvelle session.
    if (token && this.current()?.accessToken !== token) return;
    if (!this.current()) return;
    this.logout();
  }
}
