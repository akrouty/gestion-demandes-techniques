# Safyron — Frontend F1

F1 implémenté et validé humainement (IMPLEMENTED + VALIDATED), intégré dans `main`.

## Périmètre

SPA initialisée avec Angular CLI 22 (`ng new`, standalone, routing, SCSS, TypeScript strict, sans SSR).
Composants, services, guards, interceptor et environnements générés avec les schematics CLI.
Angular Material 22 : thème Material 3 via `mat.theme`, `mat.theme-overrides` et overrides de composants officiels ; aucune API M2.

Login réel, session uniquement en mémoire, expiration selon `expiresAt`, logout local,
guards UX, Bearer limité à notre API, fin de session sur 401 protégé, conservation sur 403.
Les rôles proviennent uniquement du snapshot du LoginResponse. Les autorisations restent backend.
Le rechargement complet perd la session. Aucun `/me`, refresh token ou stockage navigateur.

Les routes `/demandes` et `/administration/utilisateurs` sont uniquement du scaffolding
technique F1 pour vérifier le shell, les rôles et le responsive. Les écrans métier F2/F3
ne sont pas implémentés ; aucune liste fictive, mutation métier ou API IA n'est appelée.

## Développement

Node compatible Angular 22 : `^22.22.3 || ^24.15.0 || >=26.0.0`.
Environnement de réalisation : Node 24.21.0, npm 11.19.0.

Depuis `frontend/` :

```powershell
npm ci
npm start
npm run build
npm test -- --watch=false
```

`src/environments/environment.development.ts` centralise l’API locale
`http://localhost:8080/api/v1`. Le backend existant doit tourner avec
`CORS_ALLOWED_ORIGINS=http://localhost:4200`. Aucun secret ne figure côté frontend.
L’environnement de production utilise `/api/v1` sur la même origine ; le serveur de
production et son routage SPA/API restent hors F1.

## Organisation

- `core/session` : session mémoire et types du contrat de login.
- `core/guards`, `core/http` : navigation UX et transport HTTP.
- `core/layout/shell` : navigation par union des rôles, responsive via CDK.
- `features/auth` : formulaire Reactive Forms et service login.
- `features/demandes`, `features/administration` : points d’entrée F1 seulement.
- `shared/ui/page-state` : présentation minimale loading/error/empty/success.
- `src/styles.scss` : tokens SAFYRON, thème, badges génériques (texte toujours requis).

Le logo officiel source `images/logo.png` est préservé. Sa copie statique
`public/safyron-logo.png` est identique octet par octet, sans filtre ni déformation.
SHA256 : `FAAA78EFF30225ADB508208D84CDF86D69607E12D5892811B7F84DD4395D891C`.
Aucun favicon fabriqué depuis le logo ; aucun asset/police externe.

## Vérifications réalisées

- Installation réussie ; compilation production réussie sans warning de budget/theming.
- 24 tests Vitest via le builder Angular `@angular/build:unit-test`.
- Connexions réelles depuis le navigateur contre Spring Boot/PostgreSQL : RT, AT,
  ADM seul et RT+ADM ; union des sections et destination initiale vérifiées.
- Mauvais password : message générique, pas de révélation d’existence du compte.
- Logout et perte de session après rechargement complet vérifiés.
- Revue visuelle login/shell desktop, mobile, drawer et navigation clavier.
- Fixtures de comptes dédiées à cette revue uniquement, retirées après les vérifications ; les quatre comptes locaux initiaux sont conservés. Aucune donnée métier frontend.

## Écarts documentaires signalés

`PROJECT_TRUTH.md` conserve les validations et preuves backend des Blocs 1, 2 et 3 et enregistre la validation humaine de F1. F2 et F3 restent non implémentés.
ADR-006 et la conception frontend contiennent également des phrases historiques
« sans implémentation ». Ce sont des documents de conception validés, inchangés ici.
L’endpoint d’analyse IA est prévu au contrat mais absent du backend actuel : aucun appel F1.
Le contrat login et la relecture backend des rôles correspondent aux décisions validées.

Arrêt après F1 ; F2/F3/IA restent hors de cette livraison.
