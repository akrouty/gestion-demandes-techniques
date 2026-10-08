# Safyron — Frontend F1 et F2

F1 est implémenté et validé humainement (IMPLEMENTED + VALIDATED), intégré dans main.
F2 est en implémentation en cours, livré pour revue humaine ; aucune validation humaine F2 n’est prétendue.
F3 reste non implémenté. L’IA reste non implémentée et aucune API IA n’est appelée.

## Périmètre

SPA initialisée avec Angular CLI 22 (standalone, routing, SCSS, TypeScript strict, sans SSR).
Les composants F2, le service API et le fichier des modèles ont été créés avec le générateur Angular.
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
- features/administration : point d’entrée F1 ; F3 non implémenté.
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

Les validations navigateur réelles de F1 restent acquises. Aucune nouvelle validation
navigateur, Spring Boot/PostgreSQL ou visuelle F2 n’est prétendue pendant cette livraison.

Pour la revue humaine F2 après lancement des serveurs par l’utilisateur :

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

## Fichiers F2 de cette livraison

Préfixe des fichiers de la feature : src/app/features/demandes/.

| Changement                         | Fichiers                                                                                                              |
| ---------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| Création liste                     | demandes-list/demandes-list.ts, demandes-list.html, demandes-list.spec.ts                                             |
| Création formulaire                | demande-create/demande-create.ts, demande-create.html, demande-create.spec.ts                                         |
| Création détail                    | demande-detail/demande-detail.ts, demande-detail.html, demande-detail.spec.ts                                         |
| Création infrastructure de feature | demandes-api.ts, demandes-api.spec.ts, demande-models.ts, demande-feedback.ts, demandes.scss, demandes-routes.spec.ts |
| Modification                       | src/app/app.routes.ts, README.md, ../PROJECT_TRUTH.md                                                                 |
| Suppression du scaffolding         | demandes-entry/demandes-entry.ts, demandes-entry.html, demandes-entry.scss                                            |
