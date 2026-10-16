# Préparation locale et scénario des captures V1

**LOCAL DEMO ONLY — données intégralement fictives.** Référence de lecture : `main`, commit `f967afbd`. La V1 reste gelée ; ce document et les scripts ne modifient pas le produit, sa configuration ou `PROJECT_TRUTH.md`.

## Prérequis et commandes

- PowerShell **7 ou supérieur** (`pwsh`), backend local déjà lancé avec sa configuration habituelle et sa **base PostgreSQL réelle de démonstration**. Le script ne démarre ni serveur ni base et ne modifie aucune variable de configuration.
- Un compte **Administrateur existant et actif**, dont vous connaissez les identifiants. L’API V1 ne permet pas d’attribuer `ADMINISTRATEUR` : le script ne crée pas d’administrateur et ne propose aucun contournement SQL. Sans ce compte, arrêter ici et utiliser le provisionnement local déjà établi pour l’application.
- Choisir un mot de passe dédié **LOCAL DEMO ONLY**, commun aux trois comptes fictifs, conforme à `PASSWORD_MIN_LENGTH` du backend. Ne pas utiliser un mot de passe personnel. Garder le même lors des relances : aucun endpoint de changement de mot de passe n’existe dans ce périmètre.
- La cible par défaut est `http://localhost:8080/api/v1`. Seules les adresses de boucle locale sont acceptées, sans redirection. Vérifier soi-même que le backend utilise la bonne base de démonstration : l’API n’expose pas le moteur ou le nom de sa base.

Depuis PowerShell 7 :

```powershell
Set-Location 'D:\gestion_demandes_techniques'
# Aperçu uniquement : aucun appel réseau, aucune écriture, aucun secret demandé.
.\scripts\demo\prepare-report-data.ps1 -Plan

# Vérification du script hors ligne : aucune donnée réelle créée.
.\scripts\demo\prepare-report-data.tests.ps1

# Préparation réelle : deux invites locales, administrateur puis mot de passe DEMO.
.\scripts\demo\prepare-report-data.ps1
```

Pour un autre port local :

```powershell
.\scripts\demo\prepare-report-data.ps1 -BaseUrl 'http://localhost:8081/api/v1'
```

Le script retourne les neuf références réellement générées, titres, clients et statuts. Ne pas copier un JWT ou un mot de passe dans le rapport, dans Git ou dans une commande littérale. Les credentials sont saisis sans écho ; aucun fichier de secrets, journal de réponse ou manifeste contenant un jeton n’est écrit. Aucun appel IA n’est effectué par le script et aucune clé Groq n’est lue.

## Ce que prépare le script

Personnages fictifs, actifs, chacun avec un seul rôle métier :

| Nom fictif | Email de connexion LOCAL DEMO ONLY | Rôle |
|---|---|---|
| Nadia Belrive | `rapport.rt@example.test` | Responsable technique |
| Julien Montel | `rapport.agent.a@example.test` | Agent technique A |
| Lina Valcour | `rapport.agent.b@example.test` | Agent technique B |

Clients fictifs : **Orvane Industries**, **Veloria Conditionnement**, **Nerelis Logistique**, **Caldoria Equipements**. Les emails utilisent le domaine réservé `.test` ; les coordonnées sont fictives, sans contact à effectuer. Chaque client est créé avec sa première demande via `nouveauClient`, puis réutilisé par `clientId`. Aucun endpoint autonome de création de client n’est inventé.

| Demande | Client | Catégorie | Priorité | État final | Agent |
|---|---|---|---|---|---|
| Revue des dangers du stockage de solvants | Orvane | ETUDE_DANGERS | HAUTE | NOUVELLE | — |
| Consolidation du dossier technique de la ligne de conditionnement | Veloria | DOSSIER_TECHNIQUE | BASSE | NOUVELLE | — |
| Vérification des charges sur la plateforme de maintenance | Caldoria | NOTE_CALCUL | MOYENNE | ASSIGNEE | Julien |
| Revue du plan de protection incendie de l’entrepôt | Nerelis | PROTECTION_INCENDIE | HAUTE | ASSIGNEE | Lina |
| Analyse des risques du transfert de liquide en atelier | Orvane | ANALYSE_RISQUES_INDUSTRIELS | CRITIQUE | EN_COURS | Julien |
| Préparation de la revue documentaire réglementaire | Veloria | ASSISTANCE_REGLEMENTAIRE | MOYENNE | EN_COURS | Lina |
| Clarification des interfaces du réseau d’air comprimé | Caldoria | AUTRE | MOYENNE | RESOLUE | Julien |
| Mise en cohérence du dossier de ventilation de l’atelier | Orvane | DOSSIER_TECHNIQUE | BASSE | CLOTUREE | Lina |
| Étude de déplacement de la zone de préparation | Nerelis | ANALYSE_RISQUES_INDUSTRIELS | MOYENNE | ANNULEE | — |

Les textes de traitement et de solution sont des éléments de démonstration fictifs, pas des constats d’intervention réelle. Les références et les dates restent celles du serveur ; aucune date antérieure n’est fabriquée. Les données déjà présentes dans la base ne sont ni purgées ni masquées : la liste peut donc contenir plus de neuf demandes.

## API, relance et reprise

Le script se connecte par `POST /auth/login`. L’administrateur lit les pages `GET /utilisateurs` et crée les seuls comptes manquants avec `POST /utilisateurs`. Le RT lit `GET /clients`, `GET /demandes` et les détails ; il crée les demandes par `POST /demandes`, affecte par `PUT /demandes/{reference}/affectation`, clôture par `POST .../cloture` et annule par `POST .../annulation`. Chaque agent affecté utilise sa propre session pour `POST .../demarrage-traitement`, `PATCH .../traitement` et `POST .../resolution`.

Toutes les mutations passent donc par les services métier et leur historisation transactionnelle existante. Le script n’écrit ni statut, auteur, référence ni date arbitraire et ne prétend pas consulter un historique : aucun endpoint d’historique n’existe.

Les emails utilisateurs servent à retrouver les comptes ; nom, état et rôle doivent rester conformes. Pour les clients, l’email est recherché puis nom et téléphone sont comparés ; plusieurs résultats provoquent un arrêt, car l’email client n’est pas unique en V1. Pour les demandes, le titre exact, le client, le créateur, la description et la qualification doivent correspondre. Les pages sont parcourues entièrement.

Une relance terminée ne réécrit rien. Après une interruption, le script relit l’état et reprend uniquement les étapes manquantes, sans effacer les saisies ni ramener une demande en arrière. En cas d’état divergent, d’agent différent ou de texte modifié, il s’arrête avec un diagnostic. Les créations déjà réussies restent en base : aucune transaction globale ni suppression automatique. Une erreur réseau n’entraîne **aucun rejeu automatique de mutation** ; relancer explicitement permet de retrouver une création dont la réponse a été perdue.

Ne pas lancer plusieurs préparations depuis des machines différentes ni modifier simultanément ces demandes. Un verrou empêche les exécutions concurrentes sur une même machine ; l’API n’offre pas de clé d’idempotence ni d’unicité sur le titre des demandes. Le contrôle de doublons reste donc une protection locale, pas une garantie distribuée.

## Scénario principal réservé à la saisie manuelle

**Le script ne crée ni ce client ni cette demande et ne joue aucune de leurs transitions.** Si vous avez déjà réalisé ce scénario, vérifier la base avant de le rejouer : les clients ne sont pas dédupliqués automatiquement et la clôture est terminale.

Client fictif réservé :

- Nom : **Asterive Fabrication**.
- Email : `bureau.etudes@asterive.example.test`.
- Téléphone fictif : `+33 1 99 00 61 05`.

Titre à saisir : **Revue de la protection incendie du nouvel atelier d’assemblage**.

Description à saisir :

> Le projet d’aménagement d’un atelier d’assemblage prévoit une nouvelle implantation des postes et des zones de stockage temporaire. Nous souhaitons examiner l’accessibilité des moyens de première intervention, la lisibilité des cheminements d’évacuation et la cohérence des plans disponibles. La revue doit être réalisée avant la réunion de validation du projet prévue la semaine prochaine. Merci d’identifier les écarts documentaires et les points nécessitant une décision du responsable technique.

Hypothèse à confronter à la **vraie réponse IA** : catégorie `PROTECTION_INCENDIE`, priorité éventuellement `HAUTE` compte tenu de l’échéance. Ce n’est ni un résultat imposé ni une garantie. Ne pas modifier le texte pour faire croire à une réponse obtenue, ni substituer une suggestion hardcodée. Le RT doit lire la proposition, décider humainement et peut ajuster catégorie/priorité.

Traitement proposé, fictif :

> Les plans d’implantation et le repérage des moyens de première intervention ont été comparés. Les cheminements et les accès ont été examinés sur le dossier fourni. Les écarts documentaires ont été regroupés dans une fiche de revue, puis les points nécessitant une décision ont été présentés au responsable technique.

Solution proposée, fictive :

> Le dossier de revue rassemble le plan d’implantation annoté, la liste des écarts documentaires et les décisions retenues. Les repères des moyens de première intervention et les cheminements sont harmonisés dans les documents. Le dossier est transmis au responsable technique pour examen et clôture de la demande.

Ces textes servent à illustrer le workflow dans une démonstration ; ils ne constituent pas une expertise incendie ou une attestation de conformité réelle.

## Ordre des captures

1. Se connecter avec **Nadia Belrive (RT)**. Capturer la liste peuplée, les catégories, priorités et statuts ; remettre les filtres à zéro avant le parcours principal.
2. Ouvrir **Nouvelle demande**, saisir le titre et la description réservés. Dans Client, rechercher Asterive pour constater son absence lors du premier passage, puis choisir **Nouveau client** et saisir les coordonnées ci-dessus. Le client ne sera persisté qu’à la création de la demande.
3. Cliquer **Analyser avec l’IA**. Capturer exclusivement la réponse Groq réellement reçue. La configuration Groq habituelle doit déjà être fournie localement au backend ; ne jamais afficher sa clé. Si l’analyse renvoie 503, reporter la capture IA ; la qualification manuelle reste possible mais ne doit pas être présentée comme un succès IA.
4. Lire la proposition, cliquer **Appliquer les suggestions**, puis vérifier ou ajuster humainement catégorie et priorité. Capturer l’état réellement retenu. Modifier ensuite le titre ou la description invaliderait la suggestion et nécessiterait une nouvelle analyse volontaire.
5. Créer la demande. Noter sa **référence réellement générée** et capturer le détail `NOUVELLE`.
6. Charger les agents disponibles, sélectionner **Julien Montel**, enregistrer l’affectation. Capturer `ASSIGNEE` et le nom de l’agent.
7. Se déconnecter, puis se connecter avec **Julien Montel (Agent A)**. Retrouver la demande par sa référence ; sa liste est limitée à ses propres affectations.
8. Démarrer le traitement ; capturer `EN_COURS`. Saisir la description du traitement et la solution ci-dessus, puis **enregistrer**. La résolution n’est disponible qu’après l’enregistrement de la solution et des modifications.
9. Déclarer la demande résolue ; capturer `RESOLUE` avec la solution enregistrée.
10. Se déconnecter, puis revenir avec **Nadia Belrive**. Examiner la solution et capturer la demande avant clôture. Cliquer **Clôturer la demande**, puis capturer `CLOTUREE` et la date serveur. Les actions d’édition disparaissent normalement.

Une capture complémentaire de recherche/sélection d’un client existant peut utiliser Orvane dans un nouveau formulaire **sans soumettre une demande supplémentaire**. Pour l’administration, utiliser uniquement un administrateur local fictif déjà provisionné ; si le compte existant porte une identité personnelle, ne pas en publier les informations et ne pas le renommer avec ce script.

Garder une taille de fenêtre et un zoom cohérents, masquer les secrets et les autres applications, attendre la fin des chargements. Les snackbars de succès disparaissent après quelques secondes : capturer rapidement si leur présence est utile, sinon attendre pour montrer le contenu stable. Aucune retouche ne doit inventer un état, une réponse IA ou une action réussie.

## Portée de la vérification

Les tests compagnons remplacent le transport HTTP par un simulateur local pour vérifier routes, rôles, pagination, répartition, absence de mutation lors d’une relance, reprise après réponse perdue, collisions et erreurs. Ils ne remplacent pas l’exécution contre Spring Boot/PostgreSQL. Aucun test backend n’est requis par ces seuls ajouts d’outillage : aucun fichier produit n’est modifié. La préparation réelle nécessite les identifiants locaux et sera confirmée uniquement par une exécution réussie et la lecture des neuf états finaux.
