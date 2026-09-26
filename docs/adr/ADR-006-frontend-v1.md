# ADR-006 — Architecture frontend V1

## Statut

**VALIDATED** — validation humaine enregistrée, sans implémentation.

## 1. Contexte

La SPA Angular doit permettre aux utilisateurs internes de gérer les demandes et aux Administrateurs de gérer les comptes. Le périmètre fonctionnel, les ADR-002 à ADR-005, le contrat API et la sécurité sont validés. Le frontend, le backend, la base et la sécurité JWT/RBAC ne sont pas implémentés.

Cet ADR applique, dans leur ordre de priorité, le périmètre fonctionnel, `PROJECT_TRUTH.md`, les conventions UML et techniques, les ADR validés, `API-CONTRACT-V1.md`, `SECURITY-DESIGN-V1.md` et les UML validés applicables. Le détail opérationnel figure dans [FRONTEND-DESIGN-V1](../frontend/FRONTEND-DESIGN-V1.md). Les orientations ci-dessous sont validées humainement ; les décisions de sécurité existantes restent validées.

## 2. Besoin et contraintes

Regrouper les responsabilités autour des parcours authentification, demandes et administration ; conserver une conception compréhensible et réalisable dans un stage court. Une même personne peut cumuler plusieurs rôles. Aucun portail Client, CRUD Client autonome, historique autonome ou dashboard supplémentaire n'est prévu.

Les routes, DTO et opérations métier suivent le contrat validé. Angular présente et transmet les intentions ; Spring Boot reste responsable du RBAC, des contrôles contextuels, transitions, invariants et validations métier. Aucun moteur de cycle de vie frontend ne doit dupliquer ces règles.

## 3. Options étudiées

| Option | Intérêt | Compromis et appréciation V1 |
|---|---|---|
| A — classement par type technique (`components`, `services`, `models`) | Repérage initial simple par nature de fichier. | Disperse un parcours entre plusieurs dossiers lorsque demandes, auth et administration grandissent ; rend leur périmètre moins lisible. |
| B — organisation feature-based avec `core`, `shared`, `features` | Regroupe pages, communication API et état local d'un même domaine fonctionnel. | Exige des frontières explicites pour éviter un `core` ou `shared` fourre-tout ; orientation retenue. |
| C — Clean Architecture frontend complète | Sépare systématiquement domaine, cas d'utilisation, repositories et adaptateurs. | Ajoute des couches et transformations sans besoin démontré pour une SPA cliente de cette API ; non retenue pour la V1. |

NgRx/Redux, repositories frontend, façades systématiques, CQRS, MVVM comme architecture officielle et ViewModels systématiques ne répondent pas à un besoin proportionné actuellement. Ils ne sont pas rejetés en général ; leur introduction ultérieure exigerait un besoin réel et une décision spécifique.

## 4. Décision

Retenir une seule SPA Angular organisée par feature, avec services API simples et état principalement local. Utiliser Reactive Forms pour les formulaires significatifs. Centraliser uniquement la session et les mécanismes transversaux nécessaires d'authentification/transport.

Cette organisation rapproche l'interface de ses parcours sans recopier les modules internes du backend. Aucun code, dépendance, arborescence exhaustive ou diagramme supplémentaire n'est créé par cette conception.

## 5. Organisation feature-based

L'organisation conceptuelle sous `src/app/` distingue :

- `core` : session mémoire, accès au token et à l'utilisateur courant, guards globaux UX, interceptor Authorization et gestion minimale de fin de session ;
- `shared` : éléments de présentation réellement réutilisés entre plusieurs features, sans logique métier propre ; aucune bibliothèque interne anticipée ;
- `features/auth` : page de connexion, appel login et établissement de la session ;
- `features/demandes` : liste, création, détail et actions métier, assistance IA, recherche Client pendant la création et sélection d'Agent ;
- `features/administration` : liste, création et modification des utilisateurs, activation et rôles métier.

Les services API et données propres à une feature restent dans celle-ci. Les éléments partagés ne dépendent pas des parcours métier. Les noms exacts de fichiers et classes ne sont pas fixés.

## 6. Gestion de la session/authentification

Le login public `POST /api/v1/auth/login` reçoit `email`, `password` et retourne `accessToken`, `tokenType` (`Bearer`), `expiresAt` (ISO-8601, même instant que `exp`) et `user` (`id`, `nom`, `email`, `roles`). La session conserve ces informations uniquement en mémoire. Aucun rôle n'est extrait du JWT comme source d'autorisation et aucun claim de rôle obligatoire n'est ajouté.

Ni `localStorage`, ni `sessionStorage`, ni cookie d'authentification, ni IndexedDB ne conservent cette session. Un rechargement complet impose une nouvelle connexion. La déconnexion efface la session mémoire et retourne au login, sans endpoint backend. Aucun refresh token, `/me`, `/auth/me`, synchronisation périodique ou websocket de rôles n'est ajouté.

Un `401` sur une API protégée termine la session et ramène au login sans boucle. Un échec du login reste affiché sur le formulaire. Un `403` indique un accès refusé et ne déconnecte pas automatiquement.

## 7. Routing et navigation

Prévoir `/login`, `/demandes`, `/demandes/nouvelle`, `/demandes/:reference`, `/administration/utilisateurs`, `/administration/utilisateurs/nouveau` et `/administration/utilisateurs/:id/modifier`. Ce sont des routes SPA, distinctes du contrat HTTP backend.

Le détail concentre qualification, affectation, traitement, résolution, examen et annulation : l'utilisateur conserve le contexte et le nombre d'écrans reste limité. Aucun écran par action ni dashboard n'est nécessaire.

Après login, RT ou AT est orienté vers les demandes ; ADM seul vers les utilisateurs. Le cumul d'ADM avec un rôle métier rend les deux sections visibles. Cette orientation ne crée aucune hiérarchie de sécurité. Les query parameters peuvent conserver simplement pagination et filtres pour le retour arrière.

## 8. Communication API

Flux conceptuel : page/component → service API de la feature → HttpClient → API Spring Boot. Retour : API → DTO → service → page/component → rendu.

Les services encapsulent HTTP et les adaptations techniques utiles, sans repositories frontend ni reproduction des services métier backend. Les DTO représentent le contrat REST, pas les entités JPA. Un composant de présentation réutilisable reçoit des données et remonte des interactions.

L'interceptor ajoute `Authorization: Bearer <token>` aux seules requêtes protégées destinées à l'API de l'application, pas au login ni à un tiers. Il traite la fin de session ; les erreurs de validation et conflits restent gérés dans le contexte de l'écran. Après une mutation réussie, l'interface utilise la réponse serveur sans inventer localement un statut.

## 9. Gestion des rôles et autorisation UX

Les menus, guards et actions visibles utilisent l'union des rôles du snapshot. L'auth guard vérifie la présence d'une session utilisable ; le role guard écarte une section manifestement incompatible. Aucun guard ne décide si l'Agent est affecté à une ressource : l'API décide à sa consultation puis à chaque action.

**Visibilité frontend ≠ autorisation backend.** Le backend relit à chaque requête protégée l'utilisateur existant et actif ainsi que ses rôles actuels depuis Identity, applique le RBAC puis les contrôles contextuels. Un snapshot ancien peut laisser une action visible après retrait de rôle ; le `403` reste autoritatif.

## 10. Formulaires

Reactive Forms structure les champs, validations de forme et erreurs proches du contexte. Les contrôles client facilitent la saisie sans remplacer le backend ni inventer des contraintes de longueur ou de mot de passe.

La création de demande envoie exactement `clientId` ou `nouveauClient`. Le PATCH traitement omet les champs inchangés, transmet uniquement des valeurs non vides et exige au moins un champ. L'administration sépare nom/email, activation et rôles métier ; seuls RT et AT sont attribuables. ADM déjà présent est affichable mais jamais ajouté ou retiré par cette interface.

L'analyse IA est facultative : suggestions temporaires de catégorie/priorité validées ou modifiées humainement. Un `503` laisse le formulaire utilisable manuellement ; aucun résultat IA fictif n'est substitué.

## 11. Gestion d'état

État d'écran local dans les pages/components, HTTP dans les services, session comme état transversal minimal. Pas de store global de demandes ou utilisateurs, NgRx/Redux ou cache avancé. Un rechargement des données en revenant sur un écran est acceptable pour le volume V1.

Les écrans distinguent chargement, succès, vide et erreur ; les actions distinguent attente, soumission, succès et erreur. Empêcher les doubles soumissions et afficher une confirmation discrète après succès ne nécessite aucune machine à états supplémentaire.

## 12. UX/responsive

Interface interne sobre : navigation claire, référence identifiable, états/priorités lisibles, formulaires avec labels, actions explicites et confirmations raisonnables pour annulation ou désactivation. Desktop principal, tablette utilisable, mobile adapté à la consultation et aux actions raisonnables ; formulaires verticaux sur petit écran et tableaux adaptables ou à défilement contrôlé.

Prévoir clavier, focus cohérent et erreurs lisibles ; ne pas communiquer uniquement par couleur. Aucun effet décoratif, bibliothèque UI ou framework CSS n'est choisi.

## 13. Conséquences et compromis

| Choix | Gain | Limite et mesure |
|---|---|---|
| Session mémoire | Aucun token persistant après fermeture/rechargement. | Nouvelle connexion après refresh ; compromis sécurité déjà validé. |
| Snapshot de rôles | Navigation possible sans `/me`. | Peut devenir ancien ; backend autoritatif et traitement normal du `403`. |
| Feature-based | Parcours regroupés et compréhensibles. | Risque de dossiers globaux fourre-tout ; responsabilités strictes. |
| Absence de store global | Peu d'état transversal à synchroniser. | Certains écrans rechargent les données ; acceptable en V1. |
| Détail central et état serveur | Moins d'écrans et cohérence des actions. | Gérer les chargements et conflits sans écraser silencieusement la saisie. |

## 14. Décisions différées

Version exacte Angular à choisir lors de l'initialisation selon la version stable/LTS appropriée et l'environnement ; organisation standalone exacte, syntaxe du router, Signals et forme des interceptors restent ouverts. Bibliothèque UI (Material, Bootstrap, Tailwind, PrimeNG ou autre), CSS, design system, fichiers/classes exacts et stratégie complète de tests sont différés.

Déploiement, serveur web, Docker, CI/CD, SSR, PWA et internationalisation sont hors décision. NgRx, cache avancé et websocket ne sont pas introduits pour la V1. `/me` et refresh token restent exclus par le cadre validé ; les évoquer comme évolution ne les autorise pas. Les précisions HTTP non fixées par le contrat (base des pages, sérialisation exacte du tri, limites) devront être clarifiées avant implémentation, sans choix implicite ici.
