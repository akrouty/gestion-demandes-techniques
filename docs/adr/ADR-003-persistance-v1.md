# ADR-003 — Persistance V1

## Statut

**VALIDATED**

## 1. Contexte

L'architecture V1 retient PostgreSQL, JPA/Hibernate et Spring Data JPA. Le modèle métier validé définit les entités et leurs relations, tandis qu'ADR-002 confie les transactions à la responsabilité `application` et les invariants intrinsèques au domaine.

Le présent ADR formalise les décisions conceptuelles de persistance déjà validées. Il ne crée aucun schéma, mapping JPA, repository ou contrat API.

## 2. Besoin et contraintes

La persistance doit :

- préserver le sens du modèle métier validé ;
- utiliser des identifiants techniques sans signification métier ;
- garantir les contraintes d'unicité réellement décidées ;
- conserver la traçabilité sans permettre la réécriture fonctionnelle de l'historique ;
- éviter les chargements et cascades trop larges ;
- garantir l'atomicité entre une modification métier et son historique ;
- faire évoluer le schéma de manière explicite et versionnée.

Les invariants du cycle de vie restent dans le domaine. Ils ne sont pas traduits intégralement en contraintes SQL complexes.

## 3. Décision

La V1 persiste quatre entités :

- `Utilisateur` ;
- `Client` ;
- `DemandeTechnique` ;
- `HistoriqueDemande`.

`Role`, `StatutDemande`, `Priorite` et `Categorie` restent des enums. `Role` n'est pas une entité autonome.

Les associations sont principalement chargées à la demande, les cascades sont minimales et l'évolution du schéma est gérée par des scripts SQL PostgreSQL versionnés avec Flyway.

## 4. Entités et identifiants

Les quatre entités utilisent un identifiant technique de type `Long`, généré par une séquence PostgreSQL selon la stratégie JPA `SEQUENCE`. Une allocation simple, adaptée au volume de la V1, est retenue.

Ces identifiants :

- ne portent aucune signification métier ;
- peuvent contenir des trous ;
- ne remplacent jamais la référence métier d'une demande.

Les noms exacts des séquences et la configuration complète des annotations restent différés.

## 5. Référence métier

`DemandeTechnique.reference` est distincte de l'identifiant technique. Elle est obligatoire, unique, générée côté serveur et immuable après la création.

Son format exact sera décidé pendant la conception de la couche application et de l'API. Aucun format n'est fixé dans cet ADR.

`Utilisateur.email` est obligatoire, unique et normalisé avant persistance afin que de simples différences de casse ne produisent pas plusieurs comptes logiquement équivalents. La procédure technique exacte de normalisation reste à définir.

Les credentials d'un `Utilisateur` ne sont jamais persistés en clair. Seul le hash du mot de passe est persisté comme donnée technique de sécurité ; il ne constitue pas un attribut du modèle métier UML.

L'adresse email d'un `Client` n'est pas soumise à une contrainte d'unicité, faute de règle métier validée qui la justifie.

## 6. Relations et ownership

Les relations du modèle métier sont conservées :

- un `Client` est associé à plusieurs `DemandeTechnique`, chaque demande ayant exactement un client ;
- un `Utilisateur` créateur est associé à plusieurs `DemandeTechnique`, chaque demande ayant exactement un créateur ;
- un `Utilisateur` peut être l'Agent affecté de plusieurs demandes, chaque demande ayant au plus un Agent affecté ;
- une `DemandeTechnique` possède plusieurs `HistoriqueDemande`, chaque événement appartenant à exactement une demande ;
- un `Utilisateur` peut être l'auteur de plusieurs `HistoriqueDemande`, chaque événement ayant exactement un auteur.

Le mapping JPA est principalement unidirectionnel. Les collections inverses telles que `Client.demandes`, `Utilisateur.demandesCreees` ou `Utilisateur.demandesAffectees` ne sont pas ajoutées automatiquement. Une association visible dans le modèle métier ne justifie pas à elle seule une navigation inverse dans le mapping.

Le lien persistant d'un événement vers sa demande porte son appartenance à l'historique de cette demande. Aucun repository autonome pour `HistoriqueDemande` n'est créé à ce stade : l'historique est manipulé dans le contexte de `DemandeTechnique`.

## 7. Enums et rôles

Tous les enums sont persistés sous forme de codes textuels stables, jamais sous forme d'ordinaux numériques.

Pour `Categorie`, le code technique persistant est distinct du libellé français affiché. Les codes retenus sont :

- `ETUDE_DANGERS` ;
- `ANALYSE_RISQUES_INDUSTRIELS` ;
- `PROTECTION_INCENDIE` ;
- `NOTE_CALCUL` ;
- `DOSSIER_TECHNIQUE` ;
- `ASSISTANCE_REGLEMENTAIRE` ;
- `AUTRE`.

`Utilisateur` possède une collection d'enums `Role`. Une table de collection associe conceptuellement un utilisateur et un rôle. La combinaison utilisateur/rôle est unique et aucun identifiant technique supplémentaire n'est ajouté à cette association sans besoin identifié. Cette collection est chargée en `LAZY`.

## 8. Chargement et cascades

Les associations sont `LAZY` par défaut lorsque le mapping le permet. Sont notamment chargées en `LAZY` :

- `DemandeTechnique` vers `Client` (`ManyToOne`) ;
- `DemandeTechnique` vers son créateur (`ManyToOne`) ;
- `DemandeTechnique` vers son Agent affecté (`ManyToOne`) ;
- `HistoriqueDemande` vers son auteur (`ManyToOne`) ;
- `HistoriqueDemande` vers `DemandeTechnique` (`ManyToOne`) ;
- `DemandeTechnique` vers son historique (`OneToMany`).

Les besoins de lecture futurs ne sont pas résolus par un passage généralisé en `EAGER`. Les risques N+1 seront traités par des requêtes adaptées aux cas d'utilisation lors de la conception des repositories et de l'API.

Aucune cascade ne relie des agrégats indépendants. `DemandeTechnique` ne cascade donc aucune opération vers `Client`, son créateur ou son Agent affecté.

De `DemandeTechnique` vers `HistoriqueDemande`, seule la cascade `PERSIST` est retenue. `orphanRemoval` reste désactivé et `CascadeType.ALL` n'est pas utilisé. L'historique peut ainsi être créé avec la modification métier sans être supprimé automatiquement.

## 9. Dates et nullabilité

Les instants métier utilisent `Instant` côté Java et `timestamp with time zone` (`timestamptz`) côté PostgreSQL afin de représenter un instant absolu cohérent. Cette décision concerne :

- `dateCreation` ;
- `dateModification` ;
- `dateResolution` ;
- `dateCloture` ;
- `dateAnnulation` ;
- `dateEvenement`.

Pour `DemandeTechnique`, sont obligatoires : `reference`, `titre`, `description`, `categorie`, `priorite`, `statut`, `client`, `createur`, `dateCreation` et `dateModification`.

Sont optionnels selon le cycle de vie : `agentAffecte`, `descriptionTraitement`, `solution`, `motifAnnulation`, `dateResolution`, `dateCloture` et `dateAnnulation`.

Pour `HistoriqueDemande`, sont obligatoires : `demande`, `dateEvenement`, `typeEvenement` et `auteur`. `ancienneValeur` et `nouvelleValeur` sont optionnelles.

## 10. Historisation et transactions

Un `HistoriqueDemande` créé constitue une donnée de traçabilité. Il n'est ni modifié ni supprimé fonctionnellement. Une correction future produit un nouvel événement au lieu de réécrire un événement existant. Cette décision n'introduit pas d'Event Sourcing.

Toute modification métier importante et son historique associé sont enregistrés dans la même transaction. Une réaffectation, son éventuel changement de statut et le nouvel `HistoriqueDemande` forment ainsi une seule unité atomique, conformément à ADR-002.

`DemandeTechnique` ne fait l'objet d'aucune suppression fonctionnelle ni d'un soft-delete supplémentaire. Aucun champ `deleted`, `isDeleted` ou `deletedAt` n'est ajouté. `CLOTUREE` et `ANNULEE` restent ses états terminaux métier. La désactivation fonctionnelle d'un `Utilisateur` continue de reposer sur son attribut `actif`.

## 11. Gestion du schéma avec Flyway

Flyway gère l'évolution du schéma au moyen de scripts SQL PostgreSQL versionnés.

Hibernate/JPA assure le mapping objet-relationnel, mais ne constitue pas le mécanisme normal de migration du schéma. L'évolution de la base ne dépend pas de `ddl-auto=update`.

## 12. Alternatives et compromis

### IDENTITY ou SEQUENCE

`IDENTITY` serait simple, mais `SEQUENCE` s'intègre directement aux séquences PostgreSQL et permet de contrôler l'allocation. Une allocation simple est moins optimisée qu'une allocation par blocs, mais elle suffit pour la V1.

### Enum ordinal ou texte

L'ordinal est compact, mais il dépend de l'ordre de déclaration et rend les données peu explicites. Les codes textuels stables sont retenus malgré un stockage légèrement plus volumineux.

### EAGER généralisé ou LAZY contrôlé

Un chargement `EAGER` généralisé paraît simple, mais charge des graphes inutiles et masque les besoins réels des cas d'utilisation. Le chargement `LAZY` est retenu, avec des requêtes adaptées lorsque les données associées sont nécessaires.

### ddl-auto ou migrations versionnées

La génération automatique du schéma réduit l'effort initial, mais ne fournit pas un historique maîtrisé des évolutions. Les migrations SQL versionnées par Flyway sont retenues.

## 13. Risques

- Des requêtes mal conçues peuvent provoquer des problèmes N+1 avec les associations `LAZY`.
- Des cascades trop larges pourraient modifier ou supprimer accidentellement des données liées.
- La modification d'un code enum persistant exige une migration de données coordonnée.
- Les scripts Flyway et les mappings JPA doivent rester cohérents.
- L'unicité de `Utilisateur.email` pourrait devenir restrictive si le besoin métier évolue.
- L'allocation simple des séquences est moins performante qu'une allocation par blocs, mais reste suffisante pour la V1.

## 14. Décisions différées

Restent à définir pendant les étapes de conception correspondantes :

- les noms exacts des tables et colonnes lorsqu'ils ne sont pas nécessaires à la compréhension ;
- les noms exacts des séquences ;
- les annotations JPA complètes ;
- les requêtes des repositories ;
- le choix entre `JOIN FETCH`, `EntityGraph` ou projections selon chaque besoin de lecture ;
- le format exact de `DemandeTechnique.reference` ;
- les index supplémentaires justifiés par les futurs cas de recherche ;
- les détails de l'API REST.

