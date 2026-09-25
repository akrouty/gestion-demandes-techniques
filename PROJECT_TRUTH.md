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
- ADR-003 persistance V1 : DRAFT.
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
- Persistance V1 : décisions conceptuelles validées ; formalisation ADR-003 en cours.
- Entités persistées : `Utilisateur`, `Client`, `DemandeTechnique` et `HistoriqueDemande`, avec identifiants `Long` générés par séquences PostgreSQL.
- Les enums utilisent des codes textuels stables ; les rôles restent une collection d'enums.
- La référence métier d'une demande est obligatoire, unique, générée côté serveur et immuable ; l'email utilisateur est unique et normalisé.
- Les associations sont principalement `LAZY`, les cascades sont minimales et l'historique est fonctionnellement immuable.
- Les modifications métier et leur historique sont atomiques ; les instants utilisent `Instant` et `timestamptz`.
- Flyway gère les migrations SQL versionnées ; aucun soft-delete n'est ajouté à `DemandeTechnique`.
- Sécurité : Spring Security, JWT, RBAC et contrôles métier complémentaires dans la couche application/service.
- IA : frontière architecturale définie par un port abstrait réalisé par un adaptateur ; fonctionnement non bloquant et validation humaine obligatoire.

Ces décisions portent sur l'architecture générale, l'architecture backend et la persistance. Les classes techniques exactes et leurs dépendances détaillées ne sont pas encore décidées.

## État de l'implémentation

- Backend : non commencé.
- Frontend : non commencé.
- Base de données : non implémentée.
- Sécurité JWT/RBAC : prévue, non implémentée.
- Module IA : prévu dans le périmètre fonctionnel, non implémenté.
- Frontière architecturale IA : décidée ; fournisseur et modèle IA non encore choisis.
