# PROJECT_TRUTH

## Phase actuelle

Frontend V1 implémenté et validé.

## État des livrables

- Périmètre fonctionnel V1 : validé.
- Diagramme Use Case global : validé.
- Diagramme d'activité du cycle de vie : validé.
- Fiches détaillées des cas d'utilisation : DRAFT.
- Cinq diagrammes de séquence V1 : validés.
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
- Diagramme de classes de conception backend V1 : validé.
- Diagramme des entités persistantes / modèle de données V1 : validé.

## Décisions d'architecture retenues

- Frontend : Angular SPA.
- Backend : API REST Spring Boot sous forme de monolithe modulaire organisé module-first autour de `request`, `identity / administration`, `security` et `ai`.
- Responsabilités internes du backend : `presentation`, `application`, `domain` et `persistence`, avec les packages Java réalisés dans main.
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

Ces décisions portent sur l'architecture générale, l'architecture backend, la persistance, l'API REST, la sécurité et le frontend. Les classes techniques et leurs dépendances sont désormais implémentées dans main ; les cinq diagrammes de séquence V1 et les vues de composants, de conception backend et des entités persistantes V1 sont validés humainement.

## Conception frontend validée

ADR-006 et FRONTEND-DESIGN-V1 retiennent une organisation Angular SPA feature-based avec responsabilités `core`, `shared` et `features` (authentification, demandes, administration). Ces décisions sont validées humainement ; leur implémentation frontend V1 est réalisée et validée, comme détaillé ci-dessous.

- Session : JWT, expiration et utilisateur courant conservés uniquement en mémoire, conformément à la sécurité validée ; snapshot de rôles réservé à l'UX.
- Guards comme aides à la navigation, interceptor pour le transport du token et la fin de session ; backend autoritatif pour RBAC et contrôles contextuels.
- État principalement local, services API par feature, Reactive Forms et détail de demande centralisant les actions ; navigation par union des rôles.
- Implémentation frontend : Angular 22 et Angular Material 22 avec thème Safyron ; aucun NgRx/Redux introduit.

## État de l'implémentation

- Backend V1 : implémenté et validé dans les blocs documentés ci-dessous ; diagrammes de séquence, de composants, de conception backend et des entités persistantes V1 validés humainement.
- Bloc 1 — fondation Spring Boot + persistance V1 : implémenté et validé.
- Bloc 2 — Identity + Sécurité V1 : implémenté et validé.
- Bloc 3 — Module Demandes + API + historique : implémenté et validé.
- Schéma initial Flyway (`V1__initial_schema.sql`) : implémenté.
- Entités, enums et repositories de persistance V1 : implémentés.
- Tests Bloc 1 : 16 tests exécutés avec succès dans l'environnement H2 isolé ;
  `clean test` et `clean verify` réussis. Aucune validation PostgreSQL réelle
  n'est prétendue : les tests automatisés s'exécutent sur H2 et ne remplacent
  pas une validation PostgreSQL.
- Sécurité JWT/RBAC : implémentée pour le périmètre du Bloc 2 (JWT, relecture
  utilisateur, RBAC, 401/403, CORS, CSRF, stateless) ; validée humainement.
- Contrôles métier contextuels des demandes : implémentés et validés dans le Bloc 3.
- Validation finale du Bloc 3 : 218 tests réussis, avec `clean test` et
  `clean verify` en succès, sans failure, error ni test ignoré.
- Validation PostgreSQL réelle du Bloc 3 : connexion réussie, migration Flyway V1
  appliquée, `Hibernate validate` réussi, backend démarré et workflow API V1
  vérifié par appels HTTP réels.
- Frontend V1 : implémenté et validé (IMPLEMENTED + VALIDATED ; validation humaine finale F4 confirmée par l’utilisateur).
- F1 — fondation Angular + Angular Material + thème Safyron + shell + authentification : implémenté et validé (IMPLEMENTED + VALIDATED ; validation humaine autorisée).
- Tests frontend F1 : 24 tests réussis ; `npm ci`, `npm run build` et `npm test -- --watch=false` réussis avant intégration, sans démarrage de serveur.
- F2 — demandes frontend : implémenté et validé (IMPLEMENTED + VALIDATED ; validation humaine explicitement confirmée par l’utilisateur).
- Livraison F2 validée : liste avec filtres/tri/pagination serveur, création avec choix exclusif du client, détail et actions métier consommant les DTO/API existants ; aucune modification backend.
- Tests frontend F1 + F2 : 90 tests réussis (24 F1 conservés + 66 F2), répartis dans 10 fichiers ; installation propre avec npm ci, build production et npm test -- --watch=false réussis, sans démarrage de serveur.
- Validation fonctionnelle réelle F2 confirmée par l’utilisateur : intégration Spring Boot, tests navigateur et workflow RT/AT (création, affectation, traitement, résolution, refus, clôture, annulation) sans problème bloquant. La finition visuelle globale est réservée à F4.
- F3 — administration frontend : implémenté et validé (IMPLEMENTED + VALIDATED ; validation humaine navigateur/backend confirmée par l’utilisateur, y compris la correction de l’auto-désactivation).
- Livraison technique F3 : liste avec pagination/tri serveur, création, modification nom/email, activation/désactivation et remplacement des rôles métier ; ADMINISTRATEUR affiché en lecture seule, jamais envoyé dans rolesMetier.
- Tests frontend F1 + F2 + F3 après correction : 165 tests réussis (24 F1 + 66 F2 + 75 F3), répartis dans 15 fichiers ; npm run build et npm test -- --watch=false réussis, sans démarrage de serveur. L’installation npm ci avait été vérifiée à la livraison initiale.
- Audit initial d’intégration F3 : compatibilité des API constatée. La validation navigateur a ensuite révélé l’auto-désactivation administrative ; correction backend imposant le refus 409 et UX Angular correspondante, validation humaine de la correction désormais confirmée.
- Vérification technique de la correction F3 : 19 tests API utilisateurs ciblés réussis ; clean test et clean verify réussis avec 220 tests backend sur H2 isolé, sans échec, erreur ni test ignoré. Le refus HTTP direct 409 préserve le compte actif et son accès protégé ; activation/désactivation des autres comptes, y compris un autre ADM, conservées. Aucun serveur démarré pour ces vérifications ; retest navigateur humain confirmé : auto-désactivation refusée, action absente dans Angular et désactivation d’un autre utilisateur conservée.
- F4 — finition UI/UX, responsive et accessibilité : implémenté et validé (IMPLEMENTED + VALIDATED ; validation humaine finale sur plusieurs tailles et appareils confirmée par l’utilisateur, sans problème fonctionnel, responsive ou UX bloquant).
- IA — Bloc IA-1 backend : IMPLEMENTED + VALIDATED par les tests automatisés et intégration réelle locale avec Groq validée manuellement avec succès ; endpoint RT POST /api/v1/demandes/analyse-ia disponible, propositions temporaires, validation humaine obligatoire, aucune persistance automatique. Bloc IA-2 Angular : IMPLEMENTED + VALIDATED par les tests automatisés et VALIDATED humainement dans le navigateur ; assistance facultative dans /demandes/nouvelle pour RT. Le clic « Analyser avec l’IA » envoie uniquement titre + description à POST /api/v1/demandes/analyse-ia, sans appel Angular direct à Groq. Catégorie/priorité seulement proposées, application par clic humain explicite, valeurs finales modifiables avant création. Une modification du titre ou de la description invalide la suggestion et nécessite une nouvelle analyse volontaire ; les valeurs déjà appliquées restent éditables. Indisponibilité IA non bloquante, aucune persistance automatique des suggestions. Vérification frontend de clôture : npm run build et npm test -- --watch=false réussis, 184 tests réussis, 0 échoué, dans 15 fichiers. Validation humaine locale confirmée par l’utilisateur : connexion RT, ouverture de /demandes/nouvelle, analyse réelle Angular → Spring Boot → Groq et affichage d’une catégorie et d’une priorité, sans application automatique ; application par clic humain explicite, modification manuelle possible et création avec les valeurs finales du RT ; invalidation après modification du titre ou de la description, sans nouvelle analyse automatique ; espacement du bloc IA vérifié dans le navigateur. Sans GROQ_API_KEY locale et après redémarrage backend, indisponibilité 503 vérifiée, avec saisie et qualification manuelles toujours disponibles. Fonctionnalité IA V1 terminée dans son périmètre défini : RT uniquement, titre + description en entrée, catégorie + priorité en sortie, assistance facultative avec validation humaine obligatoire ; aucune décision automatique, persistance automatique de suggestion, affectation automatique, résumé ni fallback local présenté comme IA ; panne IA non bloquante.
- Conception IA V1 : DRAFT, sans validation humaine explicite du statut global. Six conventions V1 et dataset P01–P24 figés et validés humainement ; provenance IA_ASSISTE_RELU_HUMAINEMENT, sans dataset historique Safyron exploitable. Benchmark exploratoire Groq exécuté et validé humainement : 24/24 sorties valides ; catégorie 23/24 (95,8 %), priorité et exact match 22/24 (91,7 %), aucune sous-priorisation ni sortie invalide observée. Gemini incomplet à cause des limites Free Tier : aucune conclusion « Groq meilleur que Gemini ». Choix V1 : Groq / qwen/qwen3.8-27b, compromis entre qualité observée, accessibilité cloud du stage, quotas pratiques et simplicité ; Gemini et Ollama non retenus. Résultats attribués à la validation humaine, sans fabrication d’artefacts bruts finaux absents de Git.
- Frontière architecturale IA : réalisée via AnalyseIaPort et l’adaptateur Groq isolé dans ai.groq ; modèle V1 fixé à qwen/qwen3.8-27b. Configuration Groq absente : démarrage possible, analyse IA seule en 503 et qualification manuelle disponible. Tests IA-1 : 65 ciblés réussis (50 nouveaux IA) ; clean test et clean verify réussis chacun avec 270 tests, sans échec, erreur ni test ignoré ; 220 tests antérieurs conservés. Tests hors ligne sur H2 et stub HTTP local, sans appel Groq réel ; validation réelle locale Angular → Spring Boot → Groq désormais effectuée avec succès et scénario 503 sans clé après redémarrage validé manuellement par l’utilisateur.

## Vérification technique F4

- Écrans revus : login, shell/navigation desktop et drawer mobile, demandes (liste/filtres/pagination, création/recherche client, détail/actions RT et AT), administration (liste, création, modification, rôles, état et interdiction d’auto-désactivation).
- Finitions : couleurs Safyron conservées, espacement et feedback cohérents, champs Material outline harmonisés avec zone d’erreur de hauteur adaptable, états loading/empty partagés avec F2, actions secondaires moins dominantes, annulation/désactivation signalées comme sensibles, solution et actions du détail distinguées des données de lecture.
- Responsive : revue conceptuelle des règles à 1440, 1024, 768 et environ 390 px ; seuil de listes en cards commun à 1100 px, formulaires verticaux à 600 px, drawer à 767 px, textes longs et pagination adaptés. Cette vérification technique a été complétée par la validation humaine finale sur plusieurs tailles et appareils, sans problème bloquant constaté.
- Accessibilité : labels conservés, champs déjà obligatoires explicitement indiqués, aria-busy sur les formulaires, libellé du menu suivant son état, focus visible et lien de saut fonctionnel vérifié, sélection client visible au clavier, actions d’au moins 44 px et états textuels conservés.
- Validation automatique : npm ci réussi (0 vulnérabilité signalée), npm run build réussi, npm test -- --watch=false réussi : 170 tests dans 15 fichiers, dont 5 vérifications F4 supplémentaires ; tous les tests antérieurs conservés. npm ci signale les avertissements existants de politique allowScripts pour quatre dépendances ; le build et les tests aboutissent.
- Aucun backend, contrat API, ADR, règle métier ou mécanisme de sécurité modifié ; aucune fonctionnalité métier ajoutée. Aucun serveur démarré pour F4. F1/F2/F3/F4 sont IMPLEMENTED + VALIDATED ; le Frontend V1 est implémenté et validé après confirmation de la revue humaine finale navigateur.
