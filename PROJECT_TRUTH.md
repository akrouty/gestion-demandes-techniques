# PROJECT_TRUTH

## Phase actuelle

Conception technique en cours.

## État des livrables

- Périmètre fonctionnel V1 : validé.
- Diagramme Use Case global : validé.
- Diagramme d'activité du cycle de vie : validé.
- Fiches détaillées des cas d'utilisation : DRAFT.
- Diagrammes de séquence : validés.
- Diagramme de classes métier : validé.
- Conventions de conception technique : DRAFT.
- ADR-001 architecture applicative V1 : DRAFT.
- ADR-002 architecture backend V1 : validé.
- ADR-003 persistance V1 : validé.
- ADR-004 API REST V1 : validé.
- Contrat API REST V1 : validé.
- ADR-005 sécurité V1 : validé.
- Conception sécurité V1 : validée.
- ADR-006 architecture frontend V1 : validé.
- Conception frontend V1 : validée.
- Diagramme de composants V1 : validé.
- Diagramme de classes de conception backend V1 : DRAFT (préliminaire, non validable tant que les conceptions backend, persistance, API REST et sécurité ne sont pas suffisamment définies).

## Décisions d'architecture retenues

- Frontend : Angular SPA.
- Backend : API REST Spring Boot sous forme de monolithe modulaire organisé module-first autour de `request`, `identity / administration`, `security` et `ai`.
- Responsabilités internes du backend : `presentation`, `application`, `domain` et `persistence`, sans figer les packages Java exacts.
- La responsabilité `application` orchestre les cas d'utilisation et porte les transactions ; le `domain` porte les invariants intrinsèques.
- Les dépendances inter-modules passent par les responsabilités publiques des modules fournisseurs, sans accès direct à leurs repositories internes.
- Une modification métier et son historisation associée sont atomiques.
- Le cycle de vie utilise un enum de statut, des opérations métier explicites et des validations explicites, sans State Pattern.
- Persistance : PostgreSQL avec JPA/Hibernate et Spring Data JPA ; DTO séparés des entités JPA.
- Persistance V1 : validée et formalisée dans ADR-003.
- Entités persistées : `Utilisateur`, `Client`, `DemandeTechnique` et `HistoriqueDemande`, avec identifiants `Long` générés par séquences PostgreSQL.
- Les enums utilisent des codes textuels stables ; les rôles restent une collection d'enums.
- La référence métier d'une demande est obligatoire, unique, générée côté serveur et immuable ; l'email utilisateur est unique et normalisé.
- Les associations sont principalement `LAZY`, les cascades sont minimales et l'historique est fonctionnellement immuable.
- Les modifications métier et leur historique sont atomiques ; les instants utilisent `Instant` et `timestamptz`.
- Flyway gère les migrations SQL versionnées ; aucun soft-delete n'est ajouté à `DemandeTechnique`.
- API REST V1 : validée et formalisée dans ADR-004 et API-CONTRACT-V1.
- Sécurité V1 : validée et formalisée dans ADR-005 et SECURITY-DESIGN-V1.
- Authentification : API stateless avec JWT d'accès de durée limitée ; le login retourne avec le JWT un snapshot minimal de l'utilisateur connecté (`id`, `nom`, `email`, `roles`) au moment du login, destiné uniquement à l'UX Angular et jamais utilisé comme source d'autorisation. Les rôles actuels et l'état actif faisant autorité restent relus côté backend depuis `identity / administration` à chaque requête protégée.
- Mots de passe : BCrypt via Spring Security `PasswordEncoder` ; le mot de passe initial est fourni par l'Administrateur à la création, puis immédiatement haché et jamais persisté en clair.
- Token côté SPA : JWT conservé uniquement en mémoire ; aucun refresh token, blacklist ou Redis en V1.
- Secrets : configuration externe, jamais codée en dur ni versionnée.
- Autorisation : RBAC complété par les contrôles métier contextuels.
- IA : frontière architecturale définie par un port abstrait réalisé par un adaptateur ; fonctionnement non bloquant et validation humaine obligatoire.

Ces décisions portent sur l'architecture générale, l'architecture backend, la persistance, l'API REST, la sécurité et le frontend. Les classes techniques exactes et leurs dépendances détaillées ne sont pas encore décidées.

## Conception frontend validée

ADR-006 et FRONTEND-DESIGN-V1 retiennent une organisation Angular SPA feature-based avec responsabilités `core`, `shared` et `features` (authentification, demandes, administration). Ces décisions sont validées humainement et ne constituent pas une implémentation.

- Session : JWT, expiration et utilisateur courant conservés uniquement en mémoire, conformément à la sécurité validée ; snapshot de rôles réservé à l'UX.
- Guards comme aides à la navigation, interceptor pour le transport du token et la fin de session ; backend autoritatif pour RBAC et contrôles contextuels.
- État principalement local, services API par feature, Reactive Forms et détail de demande centralisant les actions ; navigation par union des rôles.
- Bibliothèque UI, framework CSS, version exacte Angular et détails d'implémentation différés ; aucun NgRx/Redux ou endpoint supplémentaire introduit.

## État de l'implémentation

- Backend : non commencé.
- Frontend : non commencé.
- Base de données : non implémentée.
- Sécurité JWT/RBAC : prévue, non implémentée.
- Module IA : prévu dans le périmètre fonctionnel, non implémenté.
- Frontière architecturale IA : décidée ; fournisseur et modèle IA non encore choisis.
