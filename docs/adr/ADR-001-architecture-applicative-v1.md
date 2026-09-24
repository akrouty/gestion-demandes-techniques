# ADR-001 — Architecture applicative V1

## Statut

**DRAFT**

## 1. Contexte

La V1 est une application interne de gestion des demandes techniques. Elle couvre l'enregistrement et la qualification des demandes, leur affectation, leur traitement, leur résolution, leur clôture ou leur annulation, ainsi que l'administration des utilisateurs et des rôles. Une assistance IA facultative peut proposer une catégorie et une priorité, sous validation obligatoire du Responsable technique.

Le périmètre fonctionnel est validé. La conception doit maintenant traduire ces besoins en une architecture réalisable, testable et maintenable, sans introduire de complexité qui ne répond pas à un besoin identifié.

## 2. Problème et forces en présence

L'architecture doit concilier plusieurs contraintes :

- proposer une interface web adaptée à une application interne ;
- centraliser les règles de cycle de vie et d'autorisation afin d'éviter leur contournement ;
- conserver une séparation nette entre la gestion des demandes, l'identité, la sécurité et l'assistance IA ;
- assurer la persistance relationnelle des demandes, des utilisateurs et de leur historique ;
- permettre le remplacement futur du mécanisme d'analyse IA sans engager aujourd'hui un fournisseur, un modèle ou un SDK ;
- rester proportionnée à une V1 de taille maîtrisée, dont les domaines fonctionnels sont fortement liés ;
- éviter les coûts de développement et d'exploitation d'une architecture distribuée sans besoin de déploiement indépendant.

La simplicité recherchée ne doit pas conduire à un bloc applicatif sans frontières. Un monolithe dont les dépendances internes ne sont pas maîtrisées devient difficile à tester et à faire évoluer.

## 3. Décision

La V1 adopte :

- une SPA Angular pour l'interface utilisateur ;
- une API REST Spring Boot sous forme de monolithe modulaire ;
- PostgreSQL comme système de persistance ;
- JPA/Hibernate avec Spring Data JPA ;
- des DTO séparés des entités JPA et des mappers explicites ;
- des transactions portées par la couche application/service ;
- Spring Security, JWT et un contrôle d'accès fondé sur les rôles, complété par des contrôles métier dans les services ;
- un port abstrait pour l'analyse IA, réalisé par un adaptateur interne au module `ai`.

L'API expose des opérations correspondant aux cas d'utilisation métier. Elle n'expose pas une modification CRUD arbitraire du statut d'une demande.

## 4. Architecture retenue

L'application suit une architecture client–serveur : Angular consomme l'API REST protégée du backend Spring Boot, qui applique les règles métier et persiste les données dans PostgreSQL.

Le backend est un monolithe modulaire. Ce choix correspond à une V1 de taille maîtrisée, à des domaines fonctionnels liés et à l'absence de besoin identifié de déploiement indépendant. Il simplifie le développement, les transactions et l'exploitation tout en conservant des frontières logiques entre responsabilités.

Ce compromis exige de contrôler les dépendances internes. Sans cette discipline, le monolithe pourrait devenir plus difficile à faire évoluer. Ce risque est accepté pour la V1 et réduit par l'organisation modulaire, les couches internes et des dépendances explicites.

## 5. Organisation du backend

Le backend est organisé autour de quatre modules logiques :

- `request` : cycle de vie, qualification, affectation, traitement, résolution, annulation, clients et historique des demandes ;
- `identity / administration` : utilisateurs internes, activation des comptes et rôles applicatifs ;
- `security` : authentification, validation JWT et application du RBAC ;
- `ai` : adaptateur d'analyse derrière le port défini pour le besoin métier.

À l'intérieur des modules, les responsabilités sont séparées entre présentation, application/service, domaine et persistance lorsque cela est utile. La couche application orchestre les cas d'utilisation et porte les transactions. Les contrôleurs traduisent les échanges REST. Les repositories isolent l'accès persistant.

Cette organisation n'impose pas une architecture hexagonale complète. Le port/adaptateur est retenu uniquement à la frontière IA, car cette dépendance est volontairement laissée interchangeable.

## 6. API REST

L'API REST est orientée vers les cas d'utilisation validés : enregistrer et qualifier, affecter ou réaffecter, commencer ou poursuivre un traitement, déclarer une résolution, examiner une résolution et annuler.

Les transitions de statut sont les conséquences de ces opérations métier. Aucune opération générique ne permet de choisir librement un statut. Cette règle maintient dans le backend les invariants du cycle de vie, notamment les états terminaux, les conditions de résolution et les restrictions d'affectation.

Les contrats détaillés, les routes, les codes de réponse et les DTO exacts sont différés à l'étape de conception des contrats REST.

## 7. Persistance

PostgreSQL est retenu pour la persistance relationnelle. JPA/Hibernate assure le mapping objet–relationnel et Spring Data JPA fournit les abstractions de repository.

Les DTO REST restent séparés des entités persistées afin que les contrats externes ne dépendent pas directement du modèle de persistance. En revanche, aucune duplication systématique entre modèle métier, modèle JPA et modèle de persistance n'est imposée. Une entité métier peut également être une entité JPA lorsque cette représentation reste claire et maîtrisée.

`HistoriqueDemande` appartient fortement à `DemandeTechnique`. Aucun repository autonome pour l'historique n'est introduit sans besoin de consultation ou de persistance indépendante.

Le modèle physique PostgreSQL, les annotations JPA, les stratégies d'identifiants et les migrations sont différés.

## 8. Sécurité et autorisation

Spring Security protège l'API. JWT porte l'authentification des requêtes et le RBAC applique les permissions correspondant à l'ensemble des rôles de l'utilisateur.

Le RBAC ne suffit pas pour toutes les règles. La couche application/service vérifie également les contraintes métier : un Agent technique ne traite que ses demandes affectées, seul le Responsable technique réalise les actions réservées, et le rôle `ADMINISTRATEUR` n'accorde aucun droit métier automatique sur les demandes.

La durée des jetons, les refresh tokens, la rotation, la blacklist et la stratégie de révocation ne sont pas décidés dans cet ADR.

## 9. Frontière IA

Le module `request` dépend d'un port abstrait d'analyse IA. Le module `ai` fournit l'adaptateur qui réalise ce port. Cette inversion de dépendance permet de maintenir les cas d'utilisation indépendants du fournisseur et du mécanisme d'analyse.

Le port couvre uniquement la proposition d'une catégorie et d'une priorité. L'appel est explicite, son échec reste non bloquant et toute proposition exige une validation humaine avant de devenir une décision métier.

Aucun fournisseur, modèle, SDK, protocole ou infrastructure d'exécution IA n'est choisi à ce stade.

## 10. Alternatives étudiées

### Microservices

Les microservices permettraient des déploiements indépendants, mais aucun besoin de cette nature n'est identifié pour la V1. Ils ajouteraient des communications distribuées, une exploitation plus complexe et une gestion transactionnelle plus coûteuse. Ils ne sont donc pas retenus pour le besoin actuel.

### Architecture hexagonale complète

Une séparation systématique par ports et adaptateurs sur toutes les frontières augmenterait le nombre d'abstractions et de représentations sans bénéfice démontré pour cette V1. Le principe port/adaptateur est limité à la frontière IA, où l'incertitude externe le justifie.

### CQRS, Event Sourcing, Kafka et Event Bus

Le périmètre ne présente ni besoins de traitements asynchrones distribués, ni modèles de lecture/écriture distincts, ni reconstruction d'état à partir d'événements. Ces solutions ne sont pas retenues.

### Duplication systématique des modèles

Séparer systématiquement modèle métier, modèle JPA et modèle de persistance alourdirait les transformations et la maintenance. La V1 privilégie une représentation unique lorsqu'elle reste compréhensible, tout en séparant les DTO exposés par l'API.

## 11. Compromis

Le monolithe modulaire réduit la complexité de livraison et facilite les transactions entre domaines liés. En contrepartie, ses frontières internes doivent être surveillées pour éviter les dépendances incontrôlées.

L'utilisation possible des entités métier comme entités JPA réduit la duplication, mais demande de préserver le sens métier du modèle malgré les contraintes de persistance.

JWT facilite une API sans session serveur imposée, mais les politiques détaillées de cycle de vie et de révocation des jetons restent à concevoir.

Le port IA protège le métier d'un choix prématuré, mais l'adaptateur ne pourra être finalisé qu'après sélection du fournisseur, du modèle et du mode d'intégration.

## 12. Conséquences

- Le développement peut commencer dans un seul backend déployable, structuré par domaines fonctionnels.
- Les règles métier et les transitions de statut restent centralisées dans les services applicatifs.
- Les contrats REST utilisent des DTO et des mappers distincts des entités.
- Les modules `request` et `identity / administration` persistent leurs données dans PostgreSQL.
- La sécurité combine RBAC et contrôles métier contextuels.
- L'intégration IA peut évoluer derrière un port sans modifier les cas d'utilisation métier.
- La qualité de l'architecture dépend du respect durable des frontières modulaires.

## 13. Décisions volontairement différées

- contrats REST détaillés, routes, codes de réponse et DTO exacts ;
- annotations JPA, modèle physique PostgreSQL, migrations et types d'identifiants ;
- durée des JWT, refresh tokens, rotation, blacklist et révocation ;
- fournisseur, modèle, SDK, protocole et infrastructure IA ;
- détails de déploiement et d'exploitation ;
- organisation précise des packages Java et configuration des outils de build.
