# ADR-004 — API REST V1

## Statut

**DRAFT**

## 1. Contexte

Le périmètre fonctionnel V1 et le cycle de vie des demandes sont validés. L'architecture retient une API REST Spring Boot au sein d'un monolithe modulaire, avec des DTO distincts des entités persistées. Il faut maintenant définir le contrat HTTP permettant à la future SPA Angular d'exécuter les cas d'utilisation sans contourner les règles métier.

Cet ADR fixe les principes de l'API V1. Le détail des routes, entrées, sorties, contrôles et erreurs est décrit dans `docs/api/API-CONTRACT-V1.md`.

## 2. Besoin et contraintes

L'API doit :

- exposer les cas d'utilisation validés ;
- préserver les transitions du cycle de vie ;
- appliquer les rôles et les contrôles contextuels côté backend ;
- identifier les demandes par leur référence métier unique et immuable ;
- conserver le statut, la référence, le créateur, l'auteur de l'historique et les dates métier sous contrôle du serveur ;
- rester simple et ne pas anticiper les choix de sécurité, d'IA ou d'implémentation encore différés.

Le Client reste une entité liée aux demandes et ne devient pas un module fonctionnel autonome. L'historisation demeure une conséquence interne des actions métier, sans API CRUD dédiée.

## 3. Options étudiées

### CRUD générique

Une API CRUD générique offrirait des opérations uniformes, mais permettrait facilement de modifier directement le statut ou d'exposer des ressources qui ne correspondent pas à des objectifs utilisateur. Elle rendrait les transitions et les autorisations métier moins explicites.

### API orientée actions métier

Une API orientée actions expose des opérations telles que l'affectation, le démarrage du traitement, la résolution, la clôture ou l'annulation. Chaque route exprime une intention métier et permet au backend de vérifier l'état courant, le rôle et le contexte avant d'appliquer la conséquence attendue.

Cette option est retenue parce qu'elle traduit directement les cas d'utilisation et protège le cycle de vie validé.

## 4. Décision

L'API V1 utilise le préfixe `/api/v1`. Les routes de demandes utilisent `DemandeTechnique.reference`, générée côté serveur, unique et immuable. Son format exact reste différé.

Les changements de statut résultent exclusivement d'actions métier explicites. Aucun endpoint n'accepte un statut choisi librement par le client. Les champs gérés par le serveur ne figurent pas dans les DTO d'entrée concernés.

Les endpoints protégés utilisent l'identité authentifiée fournie par la sécurité. Le mécanisme d'authentification et le cycle de vie des jetons ne sont pas définis ici.

## 5. Ressources principales

L'API couvre :

- les demandes techniques et leurs actions métier ;
- la recherche limitée des clients existants pendant l'enregistrement ;
- la consultation limitée des Agents techniques affectables ;
- l'administration des utilisateurs et de leurs rôles métier.

Aucun CRUD autonome n'est exposé pour `Client` ou `HistoriqueDemande`.

## 6. Actions métier sur les demandes

Les opérations explicites couvrent l'enregistrement, la qualification, l'analyse IA facultative, l'affectation ou la réaffectation, le démarrage et la saisie du traitement, la résolution, la clôture, le refus de résolution et l'annulation.

L'analyse IA reçoit les informations disponibles avant l'enregistrement et retourne uniquement des propositions de catégorie et de priorité. Elle ne crée ni ne modifie une demande et ne persiste aucune suggestion. Son indisponibilité n'empêche pas la qualification manuelle.

## 7. Administration

Les opérations d'administration permettent de consulter, créer et modifier les utilisateurs, d'activer ou désactiver un compte et de gérer les rôles métier `RESPONSABLE_TECHNIQUE` et `AGENT_TECHNIQUE`.

L'attribution du rôle `ADMINISTRATEUR` est refusée par l'opération de gestion des rôles métier. Aucun endpoint de suppression d'utilisateur n'est retenu.

## 8. DTO et validation

Les DTO REST sont distincts des entités persistées. Ils exposent uniquement les données utiles à l'opération concernée. Les entrées sont validées sur leur forme, puis les invariants et transitions sont contrôlés côté backend.

Aucune longueur maximale arbitraire n'est fixée dans cet ADR. Les limites exactes seront alignées avec la base et l'UX avant l'implémentation.

## 9. Erreurs et codes HTTP

L'API utilise un ensemble restreint de codes : `200`, `201`, `400`, `401`, `403`, `404`, `409` et, pour l'indisponibilité temporaire de l'analyse IA, `503`.

Les erreurs utilisent un format commun avec un code stable, un message et, si nécessaire, des erreurs de champs. Aucun détail interne, SQL, exception Java ou information de sécurité n'est exposé.

## 10. Pagination et lecture

Les listes de demandes et d'utilisateurs sont paginées. La recherche de clients peut également être paginée. Les filtres ne peuvent jamais élargir le périmètre autorisé de l'utilisateur.

La liste des demandes est triée par défaut par date de création décroissante. Le détail d'une demande n'embarque pas automatiquement tout son historique.

## 11. Autorisation

Les permissions effectives sont l'union des rôles de l'utilisateur. Le Responsable technique accède aux opérations de gestion des demandes, l'Agent technique uniquement aux demandes qui lui sont affectées et l'Administrateur aux opérations d'administration.

Le rôle `ADMINISTRATEUR` seul ne donne aucun accès métier aux demandes. Les contrôles contextuels, notamment l'identité de l'Agent affecté, restent obligatoires côté backend.

## 12. Conséquences et compromis

Les intentions métier et les transitions deviennent explicites dans le contrat HTTP, au prix d'un nombre d'endpoints d'action supérieur à celui d'un CRUD générique.

La séparation des DTO protège le contrat API du modèle de persistance, mais nécessite des transformations explicites. Les contrôles de rôle, de contexte et d'état devront être cohérents dans toutes les opérations.

Cette décision permet de poursuivre la conception de la sécurité et du frontend. Elle ne constitue pas une implémentation.

## 13. Décisions différées

Restent à définir :

- les endpoints de connexion, déconnexion et renouvellement éventuel ;
- la durée, la rotation, la révocation et le stockage des jetons ;
- le stockage et l'initialisation des mots de passe ou autres credentials ;
- les limites exactes de longueur et de pagination ;
- le format exact de la référence métier ;
- le fournisseur, le modèle et le protocole d'intégration IA ;
- les classes Java, signatures, annotations et contrats OpenAPI éventuels.
