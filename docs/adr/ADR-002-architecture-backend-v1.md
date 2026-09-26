# ADR-002 — Architecture backend V1

## Statut

**VALIDATED**

## 1. Contexte

L'architecture applicative V1 retient un backend Spring Boot sous forme de monolithe modulaire. Le périmètre fonctionnel, le modèle métier et les composants logiques sont définis, mais l'organisation interne du backend doit être formalisée avant la conception détaillée de la persistance, de l'API REST et de la sécurité.

Le présent ADR précise les responsabilités architecturales du backend. Il ne définit ni classes Spring, ni packages Java définitifs, ni contrats techniques détaillés.

## 2. Besoin

Le backend doit :

- préserver les frontières entre les domaines fonctionnels ;
- rendre les cas d'utilisation et les règles métier visibles et testables ;
- empêcher qu'un module contourne les responsabilités publiques d'un autre module ;
- garantir la cohérence transactionnelle entre une modification métier et son historisation ;
- rester simple et proportionné à la V1.

## 3. Contraintes

- Le backend reste un monolithe modulaire, sans déploiement indépendant des modules.
- Les modules principaux sont `request`, `identity / administration`, `security` et `ai`.
- Le modèle métier validé reste la référence fonctionnelle.
- Les contrôles métier sont appliqués côté backend.
- La validation humaine des suggestions IA demeure obligatoire et l'échec de l'IA reste non bloquant.
- Les détails Spring, JWT, JPA et REST ne doivent pas être décidés implicitement par cette architecture.

## 4. Options étudiées

### Organisation globale par couches techniques

Une organisation globale de type `controller` / `service` / `repository` / `entity` est simple à reconnaître. Elle regroupe toutefois les éléments par nature technique et risque de disperser ou de mélanger les domaines fonctionnels, ce qui rend leurs frontières et leurs dépendances moins visibles.

### Organisation module-first

Chaque domaine principal forme un module logique, avec des responsabilités internes adaptées à ses besoins. Cette organisation rend les frontières fonctionnelles explicites, limite les accès internes entre modules et reste compatible avec un monolithe simple. Cette option est retenue.

### Architecture hexagonale complète

Une architecture hexagonale complète systématiserait les ports, adaptateurs et représentations sur toutes les frontières. Elle ajouterait davantage d'abstractions que nécessaire pour cette V1. Le principe port/adaptateur reste limité à la frontière IA, où l'interchangeabilité est une décision explicite.

## 5. Décision

Le backend adopte une organisation **module-first** autour de :

- `request` ;
- `identity / administration` ;
- `security` ;
- `ai`.

À l'intérieur des modules métier, les responsabilités sont organisées selon les besoins autour de `presentation`, `application`, `domain` et `persistence`. Ces termes décrivent des responsabilités architecturales et ne figent pas les noms exacts des packages Java.

Le cycle de vie d'une demande repose sur :

- un enum de statut ;
- des opérations métier explicites ;
- des validations explicites.

Le State Pattern n'est pas retenu. Les six statuts connus et le cycle de vie maîtrisé de la V1 ne justifient pas sa complexité supplémentaire.

## 6. Responsabilités internes

### Presentation

La responsabilité `presentation` :

- gère la frontière HTTP ;
- reçoit les entrées ;
- applique la validation de forme ;
- appelle la responsabilité `application` ;
- construit les réponses.

Elle ne contient pas de logique métier.

### Application

La responsabilité `application` :

- orchestre les cas d'utilisation ;
- coordonne les accès nécessaires ;
- applique les contrôles contextuels ;
- porte les transactions.

### Domain

La responsabilité `domain` porte le modèle métier et ses invariants intrinsèques. Elle exprime notamment les transitions de statut autorisées ou interdites, l'impossibilité de résoudre une demande sans solution et l'impossibilité de modifier une demande terminale.

Les signatures des méthodes Java ne sont pas définies dans cet ADR.

### Persistence

La responsabilité `persistence` assure l'accès aux données du module et la traduction nécessaire vers le mécanisme de persistance. Elle ne redéfinit pas les règles métier. Les repositories exacts, leurs méthodes, les annotations JPA et les mappings restent à concevoir.

## 7. Répartition des règles métier

### Invariants intrinsèques au domaine

Les invariants qui dépendent de l'état propre des objets métier appartiennent au domaine. Par exemple, une demande `EN_COURS` avec une solution peut être résolue, tandis qu'une demande sans solution ou dans un état incompatible ne peut pas l'être.

### Règles nécessitant un contexte applicatif

Les règles qui exigent l'identité de l'utilisateur, ses rôles, une ressource externe ou la coordination de plusieurs accès sont appliquées par la responsabilité `application`. Par exemple, elle vérifie que l'utilisateur connecté est bien l'Agent technique affecté avant d'orchestrer le traitement d'une demande.

Cette répartition préserve les invariants métier dans le domaine sans imposer à celui-ci des dépendances vers l'authentification ou la persistance.

## 8. Dépendances entre modules

Les collaborations inter-modules passent par une responsabilité publique du module fournisseur. Un module ne doit pas accéder directement au repository interne d'un autre module.

Ainsi :

- `request` dépend d'une responsabilité publique de `identity / administration` pour les utilisateurs liés aux demandes ;
- `security` dépend d'une responsabilité publique de `identity / administration` pour les informations d'identité et de rôles ;
- `request` dépend de `AnalyseIaPort` pour demander une analyse sans connaître son implémentation ;
- `ai` fournit l'adaptateur qui réalise `AnalyseIaPort`.

Le nom exact et la forme technique des responsabilités publiques de `identity / administration` ne sont pas décidés ici. Aucun fournisseur ni modèle IA n'est choisi.

## 9. Transactions et historisation

Une modification métier et l'événement `HistoriqueDemande` qui lui est associé appartiennent à la même transaction.

Par exemple, une réaffectation, son éventuel changement de statut et son historisation sont atomiques : l'ensemble réussit ou échoue comme une seule unité. La responsabilité `application` porte cette frontière transactionnelle et coordonne l'opération métier avec sa persistance.

## 10. Choix de simplicité

La V1 retient explicitement :

- aucun State Pattern pour le cycle de vie ;
- aucune architecture hexagonale complète ;
- aucune duplication systématique entre modèle métier, entités JPA et modèle de persistance.

Une séparation supplémentaire ne sera introduite que si une contrainte concrète la justifie pendant la conception détaillée.

## 11. Conséquences

L'organisation module-first rend les domaines et les dépendances internes plus explicites. Elle facilite une évolution progressive du monolithe sans introduire la complexité opérationnelle de services distribués.

La séparation entre `presentation`, `application`, `domain` et `persistence` clarifie les responsabilités, tout en laissant la structure Java exacte ouverte jusqu'à la conception détaillée. Cette souplesse exige de vérifier que les dépendances réelles respectent les frontières définies.

Les invariants intrinsèques restent proches du modèle métier, tandis que les contrôles dépendant du contexte et les transactions sont orchestrés par la responsabilité `application`. Les accès inter-modules nécessitent une responsabilité publique explicite, qui devra être conçue sans exposer les repositories internes.

## 12. Décisions différées

Restent à définir et valider :

- la structure exacte des packages Java ;
- les classes et méthodes exactes ;
- les annotations et mappings JPA ;
- les contrats et méthodes détaillés des repositories ;
- les DTO et mappers exacts ;
- les contrats, endpoints et réponses REST ;
- les classes et détails Spring Security/JWT ;
- le fournisseur, le modèle et le mécanisme d'intégration IA.

