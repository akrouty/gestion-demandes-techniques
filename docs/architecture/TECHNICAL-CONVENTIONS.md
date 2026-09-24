# Conventions de conception technique

**Statut : DRAFT**

## 1. Objectif

Ce document régit la manière de préparer, décider, documenter, valider et vérifier les choix de conception technique du projet.

Il complète `docs/uml/UML-CONVENTIONS.md` sans le remplacer : les conventions UML définissent comment modéliser correctement, tandis que le présent document définit comment une décision technique devient suffisamment fondée pour être documentée puis représentée.

Son objectif est d'empêcher qu'une hypothèse, une préférence d'outil ou une proposition produite par une IA soit traitée comme une décision acquise. La conception reste progressive, justifiable et traçable jusqu'à l'implémentation et aux tests.

## 2. Sources de vérité

Avant toute décision technique, consulter dans cet ordre :

1. `docs/requirements/functional-scope.md` ;
2. `PROJECT_TRUTH.md` ;
3. `docs/uml/UML-CONVENTIONS.md` ;
4. `docs/architecture/TECHNICAL-CONVENTIONS.md` ;
5. les ADR validés applicables ;
6. les UML validés applicables.

Les documents au statut `DRAFT` peuvent alimenter une étude, mais ne constituent pas une décision validée. Une décision technique ne peut jamais contredire une règle métier validée. En cas d'incohérence, il faut la signaler et suspendre la décision concernée plutôt que d'inventer une résolution.

## 3. Méthode obligatoire

Toute décision technique importante suit la chaîne suivante :

```text
besoin
→ contraintes
→ options
→ compromis
→ décision
→ documentation
→ validation
→ implémentation
→ tests
```

Chaque étape doit rester identifiable :

- une option proposée n'est pas une décision ;
- une recommandation n'est pas une décision ;
- une décision validée n'est pas une implémentation ;
- une implémentation n'est pas terminée tant qu'elle n'est pas vérifiée et suffisamment testée.

Il ne faut pas créer de classes, de composants, de contrats ou d'infrastructure pour rendre concrète une option qui n'a pas encore été décidée.

## 4. États des décisions

### PROPOSED / proposé

Solution étudiée ou recommandée, mais non retenue définitivement. Elle peut encore être modifiée ou rejetée.

### DECIDED / décidé

Décision explicitement validée humainement et documentée dans la source de vérité appropriée. Elle décrit ce qui doit être réalisé, sans prétendre que le code existe.

### IMPLEMENTED / implémenté

Décision réellement traduite dans le code ou la configuration du projet. Cet état exige une vérification directe du dépôt.

### VERIFIED / vérifié

Implémentation contrôlée par les vérifications et tests adaptés au risque. La compilation seule ne suffit pas à atteindre cet état.

Ces états ne doivent jamais être mélangés. `PROJECT_TRUTH.md` doit refléter l'état réel et non l'état attendu.

## 5. Principe de simplicité

Dans le contexte de ce stage court, privilégier :

- la simplicité ;
- la maintenabilité ;
- la testabilité ;
- la compréhension par l'équipe et le jury ;
- les modifications progressives et vérifiables.

Une abstraction, une couche, un pattern ou un composant n'est ajouté que s'il répond à un besoin identifiable. Le nombre d'éléments architecturaux n'est pas un indicateur de qualité. L'overengineering doit être évité.

## 6. Architecture

Toute décision d'architecture doit être justifiée par le besoin du projet. Pour chaque choix important, documenter :

- le problème à résoudre ;
- les contraintes applicables ;
- les options réalistes ;
- les compromis de chaque option pertinente ;
- la décision retenue et sa portée.

Les expressions « best practice », « moderne », « scalable » ou « standard industrie » ne constituent pas une justification suffisante sans explication du lien concret avec le projet.

Une décision d'architecture générale ne détermine pas automatiquement toutes les classes, tous les packages, tous les endpoints ou toutes les dépendances qui l'implémenteront.

## 7. Dépendances entre modules

Une dépendance entre modules doit correspondre à un besoin réel et son sens doit pouvoir être expliqué. Le module utilisateur dépend du module ou du contrat utilisé.

Les dépendances circulaires sont à éviter. Si une dépendance circulaire paraît nécessaire, elle doit être analysée et explicitement justifiée avant d'être conservée.

Les modules logiques servent à séparer les responsabilités dans l'application. Ils ne doivent pas être transformés artificiellement en microservices ni supposer des déploiements indépendants non décidés.

## 8. Modèle métier et persistance

Le modèle métier validé reste la référence fonctionnelle. La persistance traduit ce modèle sans redéfinir ses règles.

Ne pas créer automatiquement trois représentations distinctes :

```text
Domain Entity
↔ JPA Entity
↔ Persistence Model
```

si une même entité bien contrôlée suffit. Toute séparation supplémentaire doit répondre à une contrainte réelle et documentée.

Une décision SQL, JPA ou de mapping ne doit jamais être présentée comme une règle métier. Les annotations, types d'identifiants, relations de persistance, cascades et stratégies de chargement doivent être décidés à l'étape de persistance.

## 9. Couche application

Les services applicatifs orchestrent les cas d'utilisation et portent les transactions lorsque cette responsabilité est décidée.

Un service ne doit pas être créé uniquement pour reproduire mécaniquement une architecture en couches. Chaque service doit correspondre à une orchestration ou une responsabilité identifiable.

Les règles métier importantes doivent rester visibles, compréhensibles et testables. Leur emplacement ne doit pas dépendre d'un simple choix de framework.

## 10. API REST

L'API REST représente les besoins et actions métier. Elle ne doit pas exposer arbitrairement les changements d'état internes ni permettre de contourner le cycle de vie validé.

Les endpoints définitifs ne sont pas définis avant l'étude des cas d'utilisation concernés, de leurs entrées, sorties, erreurs et autorisations.

Les DTO et le contrat HTTP doivent rester distincts du modèle JPA. Cette séparation n'autorise pas la création anticipée d'une multitude de DTO sans contrat analysé.

## 11. Sécurité

La conception distingue trois responsabilités :

- l'authentification, qui établit l'identité ;
- l'autorisation RBAC, qui vérifie les permissions associées aux rôles ;
- l'autorisation métier, qui vérifie les conditions liées à la ressource et au cas d'utilisation.

Par exemple, posséder le rôle `AGENT_TECHNIQUE` ne suffit pas pour traiter n'importe quelle demande : l'utilisateur doit aussi être l'Agent technique affecté à cette demande.

Ne pas ajouter automatiquement de refresh token, blacklist JWT, rotation, OAuth2 ou Redis. Chacun de ces mécanismes exige un besoin et une décision validés.

## 12. IA

Le module IA reste une assistance. Une valeur codée en dur, une règle déterministe ou un contenu lu depuis un fichier ne doit jamais être présenté comme un résultat IA.

La conception sépare :

- le contrat métier attendu ;
- le port d'intégration ;
- le fournisseur et le modèle concrets.

Le fournisseur et le modèle ne sont choisis que pendant l'étape dédiée à l'IA. La validation humaine des propositions reste obligatoire et l'échec de l'IA reste non bloquant conformément au périmètre fonctionnel.

## 13. UML technique

Les UML techniques découlent des décisions prises. Un diagramme ne doit pas servir à décider implicitement l'architecture, les classes ou leurs dépendances.

L'ordre obligatoire est :

```text
décision technique
→ documentation
→ diagramme UML correspondant
→ revue sémantique
→ rendu
→ validation humaine
```

Tout nouveau diagramme reste `DRAFT` jusqu'à validation humaine explicite et respecte intégralement `docs/uml/UML-CONVENTIONS.md`.

Un rendu lisible n'est pas une validation UML. Une compilation PlantUML réussie n'est pas une validation UML. Un diagramme `DRAFT` ne rend pas définitifs les composants, classes ou dépendances qu'il propose.

## 14. Implémentation

Une partie du code ne commence que lorsque les décisions nécessaires à cette partie sont suffisamment définies et validées.

Les modifications doivent rester petites, cohérentes et testables. Avant toute modification importante :

- identifier les risques de régression ;
- définir les critères de réussite ;
- définir comment vérifier le résultat.

L'existence d'un ADR ou d'un UML ne prouve pas que l'implémentation existe.

## 15. Tests

Une fonctionnalité n'est pas terminée parce qu'elle compile. Selon le besoin et le risque, vérifier notamment :

- les règles métier par des tests unitaires ;
- les collaborations et la persistance par des tests d'intégration ;
- les scénarios négatifs ;
- les contrôles d'accès ;
- les transitions métier interdites ;
- les régressions sur les comportements validés.

Le niveau de test doit être proportionné au risque et apporter une preuve utile, sans dupliquer mécaniquement l'implémentation.

## 16. Cohérence documentaire

Le code, les UML, les ADR, `PROJECT_TRUTH.md` et le périmètre fonctionnel doivent rester cohérents.

Après chaque décision, validation ou implémentation significative, vérifier si les documents concernés doivent être synchronisés. Ne jamais écrire dans `PROJECT_TRUTH.md` qu'une fonctionnalité est implémentée lorsqu'elle est seulement prévue ou décidée.

Toute incohérence détectée doit être signalée. Elle ne doit pas être corrigée par une nouvelle décision implicite.

## 17. Ordre de conception

Après validation du présent document, la conception progresse dans l'ordre suivant :

1. architecture générale ;
2. architecture backend ;
3. persistance ;
4. API REST ;
5. sécurité ;
6. frontend ;
7. IA ;
8. UML techniques consolidés ;
9. implémentation ;
10. tests.

Cet ordre ne signifie pas que tous les détails d'un domaine doivent être figés avant l'étape suivante. Seules les décisions nécessaires au niveau étudié doivent être prises. Les étapes suivantes ne doivent pas être anticipées artificiellement.
