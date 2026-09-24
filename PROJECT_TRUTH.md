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
- Diagramme de composants V1 : DRAFT (essai non validé).
- Diagramme de classes de conception backend V1 : DRAFT (essai non validé).

## Décisions d'architecture retenues

- Frontend : Angular SPA.
- Backend : API REST Spring Boot sous forme de monolithe modulaire organisé par domaines fonctionnels et responsabilités internes.
- Persistance : PostgreSQL avec JPA/Hibernate et Spring Data JPA ; DTO séparés des entités JPA.
- Sécurité : Spring Security, JWT, RBAC et contrôles métier complémentaires dans la couche application/service.
- IA : frontière architecturale définie par un port abstrait réalisé par un adaptateur ; fonctionnement non bloquant et validation humaine obligatoire.

Ces décisions portent sur l'architecture générale. Les classes techniques exactes et leurs dépendances détaillées ne sont pas encore décidées.

## État de l'implémentation

- Backend : non commencé.
- Frontend : non commencé.
- Base de données : non implémentée.
- Sécurité JWT/RBAC : prévue, non implémentée.
- Module IA : prévu dans le périmètre fonctionnel, non implémenté.
- Frontière architecturale IA : décidée ; fournisseur et modèle IA non encore choisis.
