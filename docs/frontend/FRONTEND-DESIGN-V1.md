# Conception frontend Angular V1

## Statut

**VALIDATED** — validation humaine enregistrée, sans implémentation.

## 1. Portée et références

Ce document guide l'implémentation future de la SPA Angular interne ; aucun frontend, backend, base de données ou mécanisme JWT/RBAC n'est présenté comme implémenté. Il détaille les décisions validées de [ADR-006](../adr/ADR-006-frontend-v1.md).

Sources consultées par priorité : [périmètre fonctionnel](../requirements/functional-scope.md), [PROJECT_TRUTH](../../PROJECT_TRUTH.md), [conventions UML](../uml/UML-CONVENTIONS.md), [conventions techniques](../architecture/TECHNICAL-CONVENTIONS.md), ADR-002/003/004/005 validés, [contrat API](../api/API-CONTRACT-V1.md), [sécurité](../security/SECURITY-DESIGN-V1.md), diagramme de composants, cycle de vie et séquences validés applicables. Le contrat précise les échanges HTTP ; les séquences fonctionnelles ne créent pas d'opération HTTP supplémentaire.

## 2. Principes et limites de responsabilité

Une seule SPA sert les trois rôles, cumulables. Angular présente les données et intentions, vérifie la forme des saisies et adapte l'UX. Spring Boot reste l'unique autorité sur RBAC, contrôles contextuels, invariants, transitions et validation métier. **Visibilité frontend ≠ autorisation backend.**

Aucun moteur de cycle de vie ou de permissions métier n'est dupliqué. Un bouton visible ne garantit jamais le succès : `403` et `409` font autorité. Aucune modification locale de statut n'est présentée comme acquise avant réponse serveur.

Pas de CRUD Client autonome, historique autonome, suppression de demande/utilisateur, endpoint générique de statut, portail Client, dashboard ou fonctionnalité métier supplémentaire.

## 3. Architecture logique

Organisation conceptuelle sous `src/app/` : `core/`, `shared/`, `features/`. Il s'agit de responsabilités, pas d'une arborescence exhaustive à générer.

Flux : page/component → service API de la feature → HttpClient → API Spring Boot. Retour : API → DTO → service → page/component → rendu. Les services encapsulent HTTP et le mapping technique nécessaire ; ils ne reproduisent ni les services métier ni des repositories backend.

Pas de repositories frontend, façades ou ViewModels systématiques, CQRS, Clean Architecture complète, MVVM officiel ou hiérarchie Smart/Dumb imposée. Un modèle de vue distinct pourra être justifié plus tard par un besoin d'écran réel.

## 4. Responsabilités transversales

| Zone | Responsabilité | Limite |
|---|---|---|
| `core` | Session mémoire, token courant, utilisateur courant, guards UX, interceptor Authorization, fin de session. | Seulement les préoccupations globales/singleton nécessaires ; aucune collection métier globale. |
| `shared` | Présentation générique réutilisée par plusieurs features, utilitaires de présentation. | Aucun élément propre à une seule feature ni logique métier ; pas de bibliothèque anticipée. |
| `features` | Pages, formulaires, services API et état local d'un parcours. | Pas d'accès arbitraire à l'état interne d'une autre feature. |

Les composants de présentation partagés reçoivent leurs données en entrée et remontent les interactions ; ils n'appellent pas arbitrairement les services de l'application.

## 5. Features

| Feature | Périmètre |
|---|---|
| `auth` | Login, appel d'authentification et établissement de la session détenue par `core`. |
| `demandes` | Liste, création, détail, qualification, IA, affectation/réaffectation, traitement, résolution, examen, clôture, refus et annulation ; recherche Client pendant création et sélection Agent. |
| `administration` | Liste, création, consultation nécessaire à la modification, nom/email, activation/désactivation et rôles métier. |

La recherche Client et la sélection d'Agent restent des besoins de demandes, pas des modules autonomes.

## 6. Pages V1

Login : formulaire de connexion. Liste demandes : résultats paginés, filtres et lien de création pour RT. Création demande : saisie, choix Client et assistance IA facultative. Détail demande : informations et actions contextuelles dans le même écran.

Liste utilisateurs : comptes paginés avec rôles et état actif. Création utilisateur : formulaire administratif. Modification utilisateur : détail chargé puis groupes séparés pour nom/email, activation et rôles métier.

Le détail central évite une page par action et conserve la référence, le statut et la solution dans le contexte de travail. Les erreurs de page et accès refusés peuvent être rendus sur place, sans imposer des routes supplémentaires.

## 7. Routing

Les chemins suivants sont des routes SPA ; les appels HTTP utilisent séparément le préfixe `/api/v1`.

| Route SPA | Page | Protection UX |
|---|---|---|
| `/login` | Connexion | Publique. |
| `/demandes` | Liste | Session + RT ou AT. |
| `/demandes/nouvelle` | Création | Session + RT. |
| `/demandes/:reference` | Détail | Session + RT ou AT ; contrôle de ressource par API. |
| `/administration/utilisateurs` | Liste utilisateurs | Session + ADM. |
| `/administration/utilisateurs/nouveau` | Création utilisateur | Session + ADM. |
| `/administration/utilisateurs/:id/modifier` | Modification utilisateur | Session + ADM. |

Reconnaître les chemins statiques `nouvelle`/`nouveau` avant les paramètres. La référence métier identifie la demande, jamais un identifiant JPA supposé. Un chemin inconnu donne un message compréhensible et un retour à une section disponible. Pagination/filtres peuvent rester dans les query parameters ; aucun token ni mot de passe ne figure dans une URL.

## 8. Matrice pages/rôles UX

RT = `RESPONSABLE_TECHNIQUE`, AT = `AGENT_TECHNIQUE`, ADM = `ADMINISTRATEUR`. Chaque colonne décrit le rôle seul ; les combinaisons prennent l'union.

| Page / fonction | RT | AT | ADM |
|---|---|---|---|
| Login | Public | Public | Public |
| Liste demandes | Oui, périmètre API global | Oui, affectées selon API | Non seul |
| Détail demande | Oui, sous contrôle API | Oui si backend autorise | Non seul |
| Nouvelle demande | Oui | Non | Non |
| Administration utilisateurs | Non | Non | Oui |
| Création utilisateur | Non | Non | Oui |
| Modification utilisateur | Non | Non | Oui |

Cette matrice décrit la présentation et ne remplace pas la matrice backend.

## 9. Session et authentification

`POST /api/v1/auth/login` est public. `LoginRequest` contient `email`, `password`. `LoginResponse` contient `accessToken`, `tokenType` = `Bearer`, `expiresAt` ISO-8601 correspondant au claim `exp`, et `user` avec `id`, `nom`, `email`, `roles` multiples.

Après succès, conserver ensemble token, expiration et snapshot utilisateur uniquement en mémoire. Aucun stockage via `localStorage`, `sessionStorage`, cookie d'authentification ou IndexedDB. Les credentials ne sont pas intégrés à la session ni journalisés. Le frontend ne tire pas ses rôles du JWT ; aucun claim obligatoire supplémentaire n'est requis.

Un rechargement complet perd la session et impose une connexion. L'expiration connue permet de considérer la session inutilisable ; la validation effective du JWT reste backend. La déconnexion efface la session, les données sensibles d'écran associées et revient au login, sans endpoint backend. Une réponse HTTP tardive d'une session terminée ne doit pas rétablir cette session.

Le snapshot peut devenir ancien. Si un rôle est retiré, le backend relit l'utilisateur existant et actif et les rôles actuels depuis Identity à chaque requête protégée, puis applique RBAC et contrôles contextuels. Une action encore visible peut donc recevoir `403`. Aucun `/me`, `/auth/me`, refresh token, synchronisation périodique ou websocket de rôles n'est prévu.

## 10. Guards

Auth guard : éviter la navigation dans une zone protégée sans session utilisable et orienter vers `/login`. Role guard : éviter une section manifestement incompatible avec le snapshot, par exemple administration sans ADM ou création sans RT ; afficher un accès refusé ou proposer une section compatible.

Aucun guard de ressource ne détermine si l'Agent est affecté. La saisie manuelle de `/demandes/ABC` par un AT déclenche la consultation API ; un refus est rendu proprement. Les guards sont une commodité UX, jamais une barrière de sécurité.

## 11. Interceptor HTTP

Ajouter `Authorization: Bearer <token>` aux requêtes protégées destinées à l'API de l'application à partir de la session mémoire. Le login ne nécessite pas cet en-tête ; ne pas envoyer le token à une autre origine ou à des ressources hors API.

Sur `401` d'une requête protégée, nettoyer la session et revenir une seule fois au login, même si plusieurs appels échouent ensemble. Ne pas relancer automatiquement la requête ni créer une boucle de redirection. Le `401` du login reste une erreur générique sur ce formulaire. Sur `403`, conserver la session et laisser l'écran expliquer l'accès refusé.

Les erreurs métier, champs invalides, conflits et indisponibilité IA sont traités par le parcours concerné ; l'interceptor ne devient pas un gestionnaire métier global. Les décisions stateless, HTTPS, CORS/CSRF et BCrypt validées sont inchangées.

## 12. Services API et correspondance des actions

Les chemins de ce tableau sont relatifs à `/api/v1`. Les noms de classes futurs restent ouverts. Les mutations de demande retournent `DemandeDetailResponse`, sauf l'analyse IA ; les mutations d'utilisateur retournent `UtilisateurDetailResponse`. Création : `201` et `Location` selon contrat ; autres succès : `200`.

| Service conceptuel / action | Méthode et route | Entrée | Sortie |
|---|---|---|---|
| Auth / connexion | `POST /auth/login` | `LoginRequest` | `LoginResponse` |
| Demandes / liste | `GET /demandes` | Pagination, tri, filtres | Page de `DemandeSummaryResponse` |
| Demandes / détail | `GET /demandes/{reference}` | — | `DemandeDetailResponse` |
| Demandes / création | `POST /demandes` | `CreationDemandeRequest` | Détail |
| Demandes / qualification | `PUT /demandes/{reference}/qualification` | `QualificationDemandeRequest` | Détail |
| Demandes / IA | `POST /demandes/analyse-ia` | `AnalyseIaRequest` | `AnalyseIaResponse` |
| Demandes / affectation ou réaffectation | `PUT /demandes/{reference}/affectation` | `AffectationDemandeRequest` | Détail |
| Demandes / démarrage | `POST /demandes/{reference}/demarrage-traitement` | — | Détail |
| Demandes / traitement et solution | `PATCH /demandes/{reference}/traitement` | `TraitementDemandeRequest` | Détail |
| Demandes / résolution | `POST /demandes/{reference}/resolution` | — | Détail |
| Demandes / clôture | `POST /demandes/{reference}/cloture` | — | Détail |
| Demandes / refus | `POST /demandes/{reference}/refus-resolution` | — | Détail |
| Demandes / annulation | `POST /demandes/{reference}/annulation` | `AnnulationDemandeRequest` | Détail |
| Demandes / recherche Client | `GET /clients` | Recherche, pagination éventuelle | Page de `ClientSummaryResponse` |
| Demandes / sélection Agent | `GET /agents` | — | Liste de `AgentAssignableResponse` |
| Administration / liste | `GET /utilisateurs` | Pagination, tri | Page de `UtilisateurSummaryResponse` |
| Administration / consultation | `GET /utilisateurs/{id}` | — | `UtilisateurDetailResponse` |
| Administration / création | `POST /utilisateurs` | `CreationUtilisateurRequest` | Détail utilisateur |
| Administration / nom et email | `PUT /utilisateurs/{id}` | `ModificationUtilisateurRequest` | Détail utilisateur |
| Administration / activation | `POST /utilisateurs/{id}/activation` | — | Détail utilisateur |
| Administration / désactivation | `POST /utilisateurs/{id}/desactivation` | — | Détail utilisateur |
| Administration / rôles métier | `PUT /utilisateurs/{id}/roles-metier` | `RolesMetierRequest` | Détail utilisateur |

## 13. DTO

Les futurs DTO TypeScript représentent exactement le contrat REST, sans entités JPA ni code produit à cette étape.

| Famille | Types conceptuels et contenu utile |
|---|---|
| Auth | `LoginRequest`, `LoginResponse` et bloc snapshot `user` décrit en section 9 ; pas de DTO supplémentaire systématique. |
| Écriture demandes | `CreationDemandeRequest` : titre, description, categorie, priorite et exactement clientId ou nouveauClient ; `QualificationDemandeRequest` : categorie, priorite ; `AffectationDemandeRequest` : agentId ; `TraitementDemandeRequest` : descriptionTraitement et/ou solution ; `AnnulationDemandeRequest` : motif. |
| IA | `AnalyseIaRequest` : titre, description ; `AnalyseIaResponse` : categorieProposee, prioriteProposee. |
| Lecture demandes | `DemandeSummaryResponse` : reference, titre, categorie, priorite, statut, client, agentAffecte nullable, dateCreation, dateModification. `DemandeDetailResponse` ajoute description, createur, descriptionTraitement, solution, motifAnnulation, dateResolution, dateCloture, dateAnnulation ; respecter les nullabilités du contrat. |
| Client | `NouveauClientRequest` : nom, email, telephone ; `ClientSummaryResponse` ajoute id. Usage limité au flux demande. |
| Agent | `AgentAssignableResponse` : id, nom, email, actif ; API retourne seulement les Agents actifs assignables. |
| Écriture utilisateurs | `CreationUtilisateurRequest` : nom, email, actif, rolesMetier, password ; `ModificationUtilisateurRequest` : nom, email ; `RolesMetierRequest` : rolesMetier. |
| Lecture utilisateurs | `UtilisateurSummaryResponse`, `UtilisateurDetailResponse` : id, nom, email, actif, roles ; aucun credential. |
| Pagination | items, page, size, totalElements, totalPages. |
| Erreur | code, message et fieldErrors optionnel ; chaque erreur de champ contient field, code, message. |

Les instants reçus sont ISO-8601 ; leur formatage d'affichage ne change pas les valeurs envoyées. Ne pas déduire le type transport des identifiants du type JPA `Long` : suivre le contrat à préciser lors de l'implémentation. Le détail n'inclut pas automatiquement l'historique complet ; aucune vue d'historique nécessitant une nouvelle API n'est promise.

## 14. Enums et libellés

| Ensemble | Codes API exacts |
|---|---|
| Statut | `NOUVELLE`, `ASSIGNEE`, `EN_COURS`, `RESOLUE`, `CLOTUREE`, `ANNULEE` |
| Priorité | `BASSE`, `MOYENNE`, `HAUTE`, `CRITIQUE` |
| Catégorie | `ETUDE_DANGERS`, `ANALYSE_RISQUES_INDUSTRIELS`, `PROTECTION_INCENDIE`, `NOTE_CALCUL`, `DOSSIER_TECHNIQUE`, `ASSISTANCE_REGLEMENTAIRE`, `AUTRE` |
| Rôle | `RESPONSABLE_TECHNIQUE`, `AGENT_TECHNIQUE`, `ADMINISTRATEUR` |

Les libellés humains peuvent différer ; toujours envoyer les codes, jamais les libellés. La priorité ne découle pas automatiquement de la catégorie.

## 15. Reactive Forms

| Formulaire | Champs et comportement |
|---|---|
| Login | email, password obligatoires ; erreur générique sans révéler existence ou état du compte. |
| Création demande | titre et description non vides, categorie, priorite ; choix exclusif clientId ou nouveauClient. |
| Nouveau Client | nom non vide, email syntaxiquement valide, telephone non vide ; ni unicité email Client ni format national arbitraire. |
| Qualification | categorie, priorite ; aucun statut. |
| Affectation | Agent sélectionné transmis comme agentId ; choix chargé par `GET /agents`. |
| Traitement | descriptionTraitement, solution : n'envoyer que les champs modifiés, au moins un, chaque valeur fournie non vide. |
| Annulation | motif obligatoire, non vide. |
| Création utilisateur | nom non vide, email valide, actif explicite, rolesMetier RT/AT seulement, password initial obligatoire. |
| Modification utilisateur | nom/email ; activation et rôles via commandes séparées. |

Un champ composé d'espaces n'est pas une valeur non vide. Pour le PATCH, absent signifie inchangé, pas suppression ; une tentative de vider une valeur ne devient ni `null` ni chaîne vide envoyée automatiquement. Expliquer que l'effacement n'est pas prévu par ce contrat. Aucun champ de statut, créateur, référence générée ou date métier n'est accepté en écriture.

Les Reactive Forms évitent les erreurs manifestes et rattachent les `fieldErrors` aux champs ; la validation backend reste autoritative. Ne pas inventer de longueur maximale, politique de mot de passe ou précision de normalisation non validée.

## 16. Liste des demandes

Consommer `GET /api/v1/demandes`. Le backend retourne toutes les demandes accessibles au RT, uniquement les affectées à l'AT seul ; ADM seul n'accède pas à cette fonction. Les colonnes présentent référence, titre, catégorie, priorité, statut, Client, Agent éventuel et dates utiles disponibles dans le DTO.

Filtres : `statut`, `priorite`, `categorie`, `clientId`, `agentId`, `recherche`. Envoyer filtres, `page`, `size` et `sort` lorsque pertinent ; tri backend par défaut `dateCreation` décroissante. Ne pas télécharger toutes les demandes pour les filtrer localement. Les filtres ne peuvent élargir les droits.

Ne pas utiliser `GET /clients` ou `GET /agents`, réservés au RT, pour fabriquer des sélecteurs destinés à l'AT. Ne pas étendre la recherche Client hors de son usage contractuel pendant l'enregistrement ; aucun nouvel endpoint de référentiel de filtres n'est introduit.

## 17. Détail et matrice des actions

Charger `GET /api/v1/demandes/{reference}` et rendre référence, description, qualification, Client, créateur, Agent, traitement, solution et dates disponibles. `403` et `404` remplacent le contenu par un état explicite avec retour possible.

Les routes ci-dessous sont relatives à `/api/v1`. Les conditions indiquées sont des indices de présentation fondés sur l'état reçu, pas une réimplémentation d'autorisation.

| Action visible | Snapshot | État pertinent pour l'UX | Appel |
|---|---|---|---|
| Qualifier | RT | Demande non terminale ; contrôle final de modifiabilité par API | `PUT /demandes/{reference}/qualification` |
| Affecter/réaffecter | RT | NOUVELLE, ASSIGNEE, EN_COURS | `PUT /demandes/{reference}/affectation` |
| Démarrer | AT | ASSIGNEE | `POST /demandes/{reference}/demarrage-traitement` |
| Renseigner traitement/solution | AT | EN_COURS | `PATCH /demandes/{reference}/traitement` |
| Déclarer résolue | AT | EN_COURS, solution enregistrée | `POST /demandes/{reference}/resolution` |
| Clôturer | RT | RESOLUE | `POST /demandes/{reference}/cloture` |
| Refuser résolution | RT | RESOLUE | `POST /demandes/{reference}/refus-resolution` |
| Annuler | RT | NOUVELLE, ASSIGNEE, EN_COURS | `POST /demandes/{reference}/annulation` |

Le backend vérifie notamment l'Agent réellement affecté pour chaque action AT, y compris pour RT+AT. Les états terminaux ne proposent pas d'édition. Examiner une résolution consiste à lire la solution puis clôturer ou refuser ; aucun endpoint d'examen supplémentaire ni motif obligatoire de refus n'est créé.

L'analyse IA est présentée dans le flux de création avant enregistrement prévu par le contrat. Le détail propose la qualification manuelle ; aucune extension IA après enregistrement n'est implicitement décidée.

## 18. Création d'une demande

1. Le RT ouvre la création et saisit titre et description.
2. Il recherche/sélectionne un Client existant via `GET /clients`, ou renseigne nom, email, telephone d'un nouveau Client.
3. Il choisit catégorie/priorité manuellement ou demande l'assistance IA facultative.
4. Il valide les valeurs finales et soumet `CreationDemandeRequest` avec exactement une représentation du Client.
5. Le backend enregistre la demande et, si nécessaire, le nouveau Client ; aucun appel autonome de création Client n'est effectué.
6. Sur `201`, utiliser le détail retourné et sa référence pour ouvrir la demande créée. Référence, créateur, statut initial et dates proviennent exclusivement du serveur.

Une erreur conserve la saisie utile dans l'écran tant que la session reste utilisable. Aucun enregistrement préalable fictif ou création automatique par IA n'est proposé.

## 19. Assistance IA

Sur action explicite du RT, envoyer uniquement titre et description à `POST /demandes/analyse-ia`. Afficher un chargement propre à l'analyse, puis `categorieProposee` et `prioriteProposee` comme suggestions temporaires. Le RT accepte ou modifie chacune avant la soumission finale ; aucun effet de persistance, changement de statut ou décision automatique.

Un `503` informe de l'indisponibilité, conserve les données et permet immédiatement la qualification manuelle. Une autre erreur IA reste également non bloquante pour la saisie manuelle, sous réserve d'une session valide. Ne jamais simuler un résultat IA ou substituer des valeurs codées en dur présentées comme générées.

Si les données analysées ont changé pendant l'appel, ne pas appliquer silencieusement une suggestion ancienne aux nouvelles saisies ; laisser le RT relancer l'analyse ou choisir manuellement.

## 20. Parcours Agent

L'AT ouvre la liste de ses demandes retournées par l'API, puis le détail. Il démarre si pertinent, renseigne traitement/solution, enregistre le PATCH et déclare résolue par l'action dédiée. Une solution seulement saisie mais non enregistrée ne suffit pas : attendre la réponse du PATCH avant la résolution.

Après chaque succès, remplacer l'état affiché par `DemandeDetailResponse`. Ne jamais fixer localement `RESOLUE` au clic. Après réaffectation concurrente ou retrait de rôle, traiter le refus backend sans forcer l'action. Un RT+AT conserve les mêmes contrôles contextuels pour ses actions AT.

## 21. Administration

La liste utilise la pagination backend et expose nom, email, rôles et actif. La création envoie nom, email, actif, rolesMetier et mot de passe initial. Seuls RT et AT sont sélectionnables. Aucun rôle ADM ne peut être attribué depuis l'interface et aucun mot de passe n'est présenté dans les données de réponse.

La modification commence par `GET /utilisateurs/{id}`. Sur le même écran, séparer clairement : sauvegarde nom/email (`PUT /utilisateurs/{id}`), activation/désactivation (POST dédiés), remplacement des rôles métier (`PUT /utilisateurs/{id}/roles-metier`). Ne pas envoyer un PUT global combinant ces opérations ; un succès partiel n'est pas présenté comme un succès global.

Si ADM existe déjà, l'afficher en lecture seule ; le tableau envoyé dans `rolesMetier` ne contient que RT/AT. Le backend préserve ADM, qui ne peut être ni ajouté ni retiré par cette opération. Aucune opération de modification de mot de passe ou suppression de compte n'est inventée.

## 22. Pagination, tri et filtres

Approche uniforme pour demandes, utilisateurs et recherche Client lorsqu'elle est paginée : requête de page puis exploitation de `items`, `page`, `size`, `totalElements`, `totalPages`. Un changement de page, tri ou filtre déclenche une nouvelle requête. Après changement de filtre, revenir à la première page selon la convention API qui sera précisée.

Conserver simplement les paramètres pertinents dans l'URL de liste facilite retour arrière et partage interne ; cette URL ne véhicule aucun droit. Ne pas ajouter de filtre utilisateur absent du contrat. La base d'index de page, la syntaxe exacte de `sort`, les tailles par défaut/maximales et le nom précis du paramètre de recherche Client restent à préciser avant implémentation, sans les inventer ici.

Distinguer liste vide et aucun résultat après filtre. Ne pas afficher une réponse ancienne arrivée tardivement à la place de la requête courante. Aucun infinite scroll, cache avancé, moteur de recherche client ou store global n'est nécessaire.

## 23. Erreurs HTTP

| Réponse | Comportement UX |
|---|---|
| `400` | Afficher le message ; rattacher fieldErrors aux champs connus, garder les autres erreurs visibles au niveau du formulaire. |
| `401` | API protégée : effacer la session et revenir au login sans boucle. Login : rester sur le formulaire avec l'échec générique. |
| `403` | Accès refusé à la page ou à l'action ; conserver la session, ne pas contourner le contrôle ni rafraîchir les rôles par une API inventée. |
| `404` | Ressource introuvable ; proposer un retour à la liste pertinente. |
| `409` | Afficher le message API de conflit, transition ou unicité ; pour une demande modifiée, proposer de recharger l'état serveur sans écraser silencieusement la saisie ni forcer un état local. |
| `503` sur analyse IA | Conserver le formulaire et poursuivre manuellement. |
| Réseau ou réponse technique inattendue | Message générique compréhensible, sans stack trace ni détail interne ; permettre une reprise explicite adaptée au contexte. |

Respecter `code`, `message`, `fieldErrors` du contrat, sans inventer des codes backend. Ne pas rejouer automatiquement une mutation après erreur réseau : son résultat peut être incertain ; consulter l'état serveur avant une nouvelle action quand nécessaire.

## 24. États d'interface et état local

Un écran dépendant de l'API distingue initial/loading, success, empty si pertinent, error. Une action distingue idle, submitting/loading, success, error. Désactiver raisonnablement la soumission en cours pour éviter les doublons ; après succès, utiliser la réponse backend, afficher une confirmation non intrusive et actualiser l'écran.

Conserver les saisies et suggestions dans l'écran concerné, les appels dans les services et seulement la session dans l'état transversal. Pas de NgRx/Redux, store global pour les listes, cache complexe ou bibliothèque de machine à états. Recharger une liste au retour est acceptable.

## 25. Navigation multi-rôles

| Snapshot | Sections disponibles | Orientation après login |
|---|---|---|
| RT | Demandes, création et actions RT | `/demandes` |
| AT | Demandes et actions AT sous contrôle backend | `/demandes` |
| ADM | Administration uniquement | `/administration/utilisateurs` |
| RT + AT | Union RT/AT | `/demandes` |
| RT + ADM | Demandes RT et administration | `/demandes` |
| AT + ADM | Demandes AT et administration | `/demandes` |
| RT + AT + ADM | Union des trois | `/demandes` |

Aucun rôle principal exclusif ni hiérarchie de sécurité. RT+AT peut consulter globalement comme RT mais traiter seulement si le backend l'autorise comme Agent affecté. Si aucun rôle ne donne de section disponible, afficher cette absence d'accès avec déconnexion possible, sans boucle ou permission inventée.

## 26. UX, responsive et accessibilité

Usage desktop principal, tablette correcte et mobile utilisable pour consultation/actions raisonnables. Navigation compacte sur petit écran ; formulaires verticaux ; tableaux adaptés ou à défilement contrôlé, sans texte rendu illisible. Garder référence, statut, priorité et actions principales identifiables.

Labels réels, ordre clavier logique, boutons nommés par leur action, erreurs proches du contexte, focus cohérent après navigation ou erreur importante. Ne pas utiliser uniquement une couleur pour statut/priorité. Prévoir une confirmation raisonnable pour annulation ou désactivation ; éviter la multiplication des modales, animations, graphiques et widgets décoratifs.

## 27. Conformité et limites

Chaque action de la section 12 correspond au contrat existant ; aucune API métier nouvelle. Les séquences fonctionnelles sont traduites avec les opérations précises du contrat : le nouveau Client est persisté avec la demande et les suggestions IA ne sont enregistrées qu'à travers les valeurs finales soumises. Leurs échanges fonctionnels ne justifient pas des endpoints autonomes.

JWT uniquement mémoire ; snapshot uniquement UX ; rôles et actif autoritatifs relus backend ; guards sans autorisation contextuelle ; statuts changés uniquement sur réponse serveur. Les contrats et UML VALIDATED ne sont pas modifiés. Aucun code Angular ni choix d'API Angular dépendant d'une version n'est inclus. Les deux documents frontend sont VALIDATED.

## 28. Décisions différées

Version exacte Angular selon stable/LTS et environnement lors de l'initialisation ; standalone et organisation précise, syntaxe router, interceptors fonctionnels/classes et détails Signals. Bibliothèque UI, framework CSS, design system, fichiers/classes exacts et stratégie complète de tests restent différés.

Déploiement frontend, serveur web de production, Docker, CI/CD, SSR, PWA et internationalisation sont hors décision. NgRx, cache avancé et websocket ne sont pas introduits. `/me` et refresh token restent exclus du cadre V1 validé et nécessiteraient une décision distincte pour toute évolution. Les précisions du contrat signalées en section 22 et la représentation transport des identifiants ne sont pas tranchées par le frontend.
