# Safyron — Frontend F1, F2 et F3

F1 est implémenté et validé humainement (IMPLEMENTED + VALIDATED), intégré dans main.
F2 est implémenté et validé humainement (IMPLEMENTED + VALIDATED), selon la confirmation explicite de l’utilisateur.
F3 est implémenté techniquement, en attente de validation humaine navigateur/backend. F4 et l’IA restent non implémentés ; aucune API IA n’est appelée.

## Périmètre

SPA initialisée avec Angular CLI 22 (standalone, routing, SCSS, TypeScript strict, sans SSR).
Les composants, services API et fichiers des modèles F2/F3 ont été créés avec le générateur Angular.
Angular Material 22 et le thème Safyron de F1 sont conservés.

F1 : login réel, session uniquement en mémoire, expiration, logout, guards UX,
Bearer limité à notre API, fin de session sur 401 protégé, conservation sur 403.
Les rôles proviennent du snapshot de login ; les autorisations restent backend.
Aucun stockage navigateur de session, refresh token ou endpoint /me.

F2 : liste paginée et filtrée côté serveur, création avec client existant ou nouveau,
détail centralisant qualification, affectation, traitement, résolution, refus, clôture et annulation.
Les écrans consomment uniquement les données HTTP du backend ; les fixtures restent dans les tests.
Le tableau desktop devient une liste de cartes sur petit écran.

## Développement

Environnement de réalisation : Node 24.21.0, npm 11.19.0.
Depuis frontend :

```powershell
npm ci
npm run build
npm test -- --watch=false
```

Le lancement des serveurs reste à la charge de l’utilisateur.
L’environnement de développement utilise http://localhost:8080/api/v1 ;
le backend doit autoriser http://localhost:4200 via sa configuration CORS existante.
L’environnement de production utilise /api/v1 sur la même origine.
Aucun secret n’est stocké dans le frontend.

## Organisation

- core/session, guards, http et layout : fondation F1 conservée.
- features/auth : formulaire et service de connexion F1.
- features/demandes : service HTTP simple, DTO, libellés, feedback, styles et trois pages F2.
- features/administration : API utilisateurs, DTO et trois pages F3 (liste, création, modification).
- shared/ui/page-state et src/styles.scss : présentation et thème F1 conservés.

Le logo source images/logo.png et sa copie public/safyron-logo.png sont inchangés.
SHA256 : FAAA78EFF30225ADB508208D84CDF86D69607E12D5892811B7F84DD4395D891C.

## Appels HTTP F2

Toutes les routes suivantes utilisent le préfixe /api/v1 :

| Méthode | Endpoint                                   | Usage                                                      |
| ------- | ------------------------------------------ | ---------------------------------------------------------- |
| GET     | /demandes                                  | Liste ; page 0 par défaut, size 20, tri et filtres serveur |
| POST    | /demandes                                  | Création ; exactement clientId ou nouveauClient            |
| GET     | /demandes/{reference}                      | Détail                                                     |
| GET     | /clients                                   | Recherche RT pendant création, pages de 10 résultats       |
| GET     | /agents                                    | Sélection RT, chargement explicite                         |
| PUT     | /demandes/{reference}/qualification        | Catégorie et priorité                                      |
| PUT     | /demandes/{reference}/affectation          | agentId                                                    |
| POST    | /demandes/{reference}/demarrage-traitement | Démarrage AT                                               |
| PATCH   | /demandes/{reference}/traitement           | Champs modifiés non vides uniquement                       |
| POST    | /demandes/{reference}/resolution           | Après solution réellement enregistrée                      |
| POST    | /demandes/{reference}/refus-resolution     | Refus RT sans motif inventé                                |
| POST    | /demandes/{reference}/cloture              | Clôture RT                                                 |
| POST    | /demandes/{reference}/annulation           | Motif obligatoire avec confirmation                        |

Après succès, le détail affiché vient du serveur ; aucun statut n’est calculé localement.
La visibilité des actions selon le snapshot et l’état reçu sert uniquement à l’UX.
Le backend vérifie l’Agent affecté et les rôles actuels.
Le filtre, le tri et la pagination sont conservés dans les query parameters.
Une nouvelle consultation annule la précédente ; aucune ancienne réponse ne remplace la vue actuelle.

Un 400 conserve la saisie et rattache les fieldErrors connus au formulaire.
Un 403 conserve la session. Un 404 affiche la demande absente.
Après 409 ou résultat de mutation incertain (réseau/5xx), le rechargement est explicite,
remplace les saisies par l’état serveur et conditionne les prochaines mutations.

## Vérifications et revue humaine

Les tests automatisés F2 couvrent les requêtes HTTP, les pages, les rôles,
les corps de création exclusifs, le PATCH partiel, les actions et les erreurs.
Les comptes/demandes/clients utilisés comme fixtures sont limités aux fichiers .spec.ts.
Le nombre final et les résultats de build/tests sont consignés dans PROJECT_TRUTH.

Les validations de F1 restent acquises. La validation fonctionnelle réelle de F2 est explicitement confirmée par l’utilisateur (intégration Spring Boot, navigateur, workflow RT/AT). La finition visuelle globale reste prévue pour F4.

Parcours de référence F2 pour les vérifications manuelles :

1. RT : chercher, filtrer, trier, paginer ; revenir avec le navigateur et vérifier la vue.
2. RT : créer avec chacun des deux modes client ; vérifier le détail retourné.
3. RT : qualifier et affecter/réaffecter à un Agent retourné par l’API.
4. AT : démarrer, enregistrer traitement/solution, puis résoudre.
5. RT : examiner la solution ; tester refus ou clôture ; annuler une demande active avec motif.
6. Vérifier clavier, mobile, badges textuels et feedback avec les données locales réelles.
7. Vérifier les refus backend et un conflit concurrent ; recharger explicitement l’état.

## Audit backend pour F2

Aucune modification backend nécessaire pour F2.
L’inspection des contrôleurs, DTO, SecurityConfig, pagination et chargements des listes
n’a révélé aucun défaut concret bloquant l’intégration. Les associations de liste sont
chargées en groupe après pagination, avec consultation groupée des rôles utilisateurs.

Les ADR et documents de conception VALIDATED restent inchangés. Leurs phrases historiques
« sans implémentation » décrivent la conception ; PROJECT_TRUTH donne l’état actuel.
L’endpoint IA est prévu au contrat mais absent du backend actuel : il reste exclu de F2.

## Fichiers F2 — livraison précédente

Préfixe des fichiers de la feature : src/app/features/demandes/.

| Changement                         | Fichiers                                                                                                              |
| ---------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| Création liste                     | demandes-list/demandes-list.ts, demandes-list.html, demandes-list.spec.ts                                             |
| Création formulaire                | demande-create/demande-create.ts, demande-create.html, demande-create.spec.ts                                         |
| Création détail                    | demande-detail/demande-detail.ts, demande-detail.html, demande-detail.spec.ts                                         |
| Création infrastructure de feature | demandes-api.ts, demandes-api.spec.ts, demande-models.ts, demande-feedback.ts, demandes.scss, demandes-routes.spec.ts |
| Modification                       | src/app/app.routes.ts, README.md, ../PROJECT_TRUTH.md                                                                 |
| Suppression du scaffolding         | demandes-entry/demandes-entry.ts, demandes-entry.html, demandes-entry.scss                                            |

## F3 — Administration

Routes SPA protégées par les guards UX F1 et le rôle ADMINISTRATEUR :

- /administration/utilisateurs
- /administration/utilisateurs/nouveau
- /administration/utilisateurs/:id/modifier

Le cumul ADM + RT/AT conserve l’accès à l’administration.
Le backend reste l’unique autorité sur les rôles actuels et l’état actif.

La liste utilise MatTable sur desktop et des cartes sur mobile, avec MatPaginator.
Les pages commencent à zéro, avec size 20 par défaut. Les seuls tris proposés
sont id, nom, email et actif, côté serveur. Aucun tri par défaut n’est imposé,
aucun filtre ou paramètre recherche n’est ajouté.

La création utilise un Reactive Form avec nom, email, actif, rôles métier et
mot de passe initial. Seuls RT et AT sont sélectionnables, ensemble ou séparément ;
un tableau vide est accepté. Le mot de passe n’a aucune longueur minimale
arbitraire côté frontend, reste une donnée d’entrée et est effacé après succès.

La modification comporte trois sauvegardes indépendantes : nom/email, activation
ou désactivation, remplacement complet des rôles métier. Une réponse s’applique
uniquement aux champs de sa section afin de ne pas écraser une autre réponse
concurrente ou les saisies encore présentes dans une autre section.
La désactivation requiert une confirmation explicite.

ADMINISTRATEUR existant est affiché en lecture seule et n’est jamais envoyé dans
rolesMetier. C’est le backend qui le conserve. Aucune suppression ou modification
de mot de passe existant n’est proposée.

| Méthode | Endpoint relatif à /api/v1       | Usage                        |
| ------- | -------------------------------- | ---------------------------- |
| GET     | /utilisateurs                    | Pagination et tri serveur    |
| GET     | /utilisateurs/{id}               | Lecture du compte            |
| POST    | /utilisateurs                    | Création, réponse 201        |
| PUT     | /utilisateurs/{id}               | Nom/email uniquement         |
| POST    | /utilisateurs/{id}/activation    | Activation sans corps        |
| POST    | /utilisateurs/{id}/desactivation | Désactivation sans corps     |
| PUT     | /utilisateurs/{id}/roles-metier  | Remplacement des rôles RT/AT |

Les erreurs connues sont affichées dans leur section : fieldErrors, email déjà
utilisé, politique minimale du mot de passe, rôles invalides, paramètres invalides,
accès refusé, utilisateur absent et conflit. Un 403 conserve la session.
Les 401 utilisent l’interceptor F1. Après résultat de mutation incertain ou CONFLIT,
la section attend un rechargement explicite ; les saisies ne sont pas écrasées silencieusement.

### Audit et validation F3

Aucune modification backend nécessaire pour F3.
Les contrôleurs, DTO, erreurs, RBAC, pagination, tris, unicité email et préservation
du rôle ADM sont compatibles avec les parcours. La liste charge les rôles en groupe
après pagination. Aucune modification des ADR, contrats validés ou du backend.

Validation technique : npm ci et build production réussis ; 161 tests frontend réussis (24 F1, 66 F2 et 71 F3), dans 15 fichiers. Aucun serveur démarré.

Les tests F3 couvrent les sept endpoints, les rôles cumulés, la pagination/tri,
les quatre combinaisons de rôles métier, les opérations indépendantes/concurrentes,
les erreurs, la confirmation de désactivation et la fin de session globale F1.
Les fixtures de test restent dans les fichiers .spec.ts ; aucune donnée métier fictive
n’est incluse dans les écrans.

La validation humaine F3 reste à réaliser par l’utilisateur après lancement des serveurs :

1. ADM : paginer, trier et consulter les comptes réels.
2. Créer un utilisateur avec chacun des ensembles de rôles métier, y compris aucun.
3. Vérifier email dupliqué, politique de mot de passe et effacement après création.
4. Modifier nom/email, désactiver avec confirmation puis réactiver.
5. Modifier les rôles RT/AT d’un compte ADM et vérifier sa conservation en lecture seule.
6. Vérifier les accès multi-rôles, le mobile, les labels, le clavier et les messages d’erreur.

### Fichiers F3 de cette livraison

Préfixe de la feature : src/app/features/administration/.

| Changement                | Fichiers                                                                                                                                                 |
| ------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Liste                     | utilisateurs-list/utilisateurs-list.ts, utilisateurs-list.html, utilisateurs-list.spec.ts                                                                |
| Création                  | utilisateur-create/utilisateur-create.ts, utilisateur-create.html, utilisateur-create.spec.ts                                                            |
| Modification              | utilisateur-edit/utilisateur-edit.ts, utilisateur-edit.html, utilisateur-edit.spec.ts                                                                    |
| Infrastructure de feature | administration-api.ts, administration-api.spec.ts, utilisateur-models.ts, administration-feedback.ts, administration.scss, administration-routes.spec.ts |
| Fichiers modifiés         | src/app/app.routes.ts, README.md, ../PROJECT_TRUTH.md                                                                                                    |
| Scaffolding supprimé      | utilisateurs-entry/utilisateurs-entry.ts, utilisateurs-entry.html, utilisateurs-entry.scss                                                               |
