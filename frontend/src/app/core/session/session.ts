import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { LoginResponse, Role } from './session-models';

/** Snapshot pour l'UX uniquement : les autorisations restent côté backend. */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly router = inject(Router);
  private readonly current = signal<LoginResponse | null>(null);
  private revision = 0;
  private readonly storageKey = 'safyron.session.v1';
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
  constructor() {
    this.restore();
  }
  private restore(): void {
    try {
      const stored = sessionStorage.getItem(this.storageKey);
      if (!stored) return;
      const data = JSON.parse(stored);
      const parts = typeof data?.accessToken === 'string' ? data.accessToken.split('.') : [];
      if (parts.length !== 3 || parts.some((part: string) => !/^[A-Za-z0-9_-]+$/.test(part))) {
        throw new Error('Session invalide');
      }
      const decode = (part: string) => JSON.parse(atob(part.replace(/-/g, '+').replace(/_/g, '/')));
      const header = decode(parts[0]);
      const claims = decode(parts[1]);
      const roles: readonly Role[] = ['RESPONSABLE_TECHNIQUE', 'AGENT_TECHNIQUE', 'ADMINISTRATEUR'];
      if (
        typeof header?.alg !== 'string' ||
        header.alg.toLowerCase() === 'none' ||
        !Number.isFinite(claims?.exp) ||
        claims.exp * 1000 <= Date.now() ||
        (claims.nbf !== undefined &&
          (!Number.isFinite(claims.nbf) || claims.nbf * 1000 > Date.now())) ||
        !Number.isSafeInteger(data.user?.id) ||
        data.user.id <= 0 ||
        typeof data.user.nom !== 'string' ||
        typeof data.user.email !== 'string' ||
        !Array.isArray(data.user.roles) ||
        !data.user.roles.every((role: Role) => roles.includes(role)) ||
        typeof data.expiresAt !== 'string' ||
        !Number.isFinite(Date.parse(data.expiresAt))
      ) {
        throw new Error('Session invalide');
      }
      // Contrôles locaux uniquement : la signature et les droits restent vérifiés par l'API.
      this.establish({
        ...data,
        expiresAt: new Date(Math.min(Date.parse(data.expiresAt), claims.exp * 1000)).toISOString(),
      });
    } catch {
      this.clear();
    }
  }
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
    this.current.set({
      accessToken: response.accessToken,
      tokenType: response.tokenType,
      expiresAt: response.expiresAt,
      user: {
        id: response.user.id,
        nom: response.user.nom,
        email: response.user.email,
        roles: [...response.user.roles],
      },
    });
    try {
      sessionStorage.setItem(this.storageKey, JSON.stringify(this.current()));
    } catch {
      // Stockage désactivé ou indisponible : la session reste en mémoire.
      this.removeStoredSession();
    }
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
  private removeStoredSession(): void {
    try {
      sessionStorage.removeItem(this.storageKey);
    } catch {
      // Le navigateur peut interdire l'accès au stockage.
    }
  }
  clear(): void {
    this.revision++;
    this.current.set(null);
    this.removeStoredSession();
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
