# Conventions UML du projet

Ce document est la référence obligatoire de modélisation UML pour l'ensemble du projet.

Avant toute création, correction ou validation d'un diagramme UML, il faut lire :

- `docs/requirements/functional-scope.md` ;
- `PROJECT_TRUTH.md` ;
- `docs/uml/UML-CONVENTIONS.md`.

Les règles sémantiques UML définissent ce que le diagramme signifie. Les conventions PlantUML définissent uniquement la manière de produire un rendu clair. Une convention de rendu ne doit jamais modifier, remplacer ou contredire la sémantique UML.

## 1. Principes généraux de modélisation

- Respecter la notation UML et la sémantique réelle de chaque élément et relation.
- Ne jamais inventer une fonctionnalité, un acteur, une classe, un composant, une infrastructure ou une relation pour enrichir artificiellement un diagramme.
- Conserver exactement la terminologie métier validée.
- Fonder tout diagramme sur le périmètre et les décisions réellement validés.
- Donner à chaque diagramme un objectif précis et un niveau explicite : métier/analyse, conception logique, conception technique ou déploiement.
- Ne pas mélanger dans un même diagramme des concepts métier, des détails d'implémentation et des éléments d'infrastructure, sauf si son objectif l'exige explicitement.
- Distinguer les diagrammes d'analyse des diagrammes techniques.
- Privilégier la lisibilité à la quantité d'informations et à la complexité graphique.
- Minimiser les croisements, les relations ambiguës et les éléments redondants.
- Conserver le statut `DRAFT` jusqu'à une validation humaine explicite.
- Codex ne doit jamais passer de lui-même un nouveau diagramme à `VALIDATED`.
- Les diagrammes existants restent `DRAFT` tant qu'ils n'ont pas été relus selon la version actuelle de ces conventions.
- Une compilation PlantUML réussie vérifie la syntaxe, pas la correction UML.

## 2. Règles sémantiques UML

### 2.1 Diagrammes de cas d'utilisation

- Un acteur représente un rôle externe vis-à-vis du système, pas nécessairement une personne physique.
- Une même personne réelle peut correspondre à plusieurs acteurs lorsque cette personne cumule plusieurs rôles.
- Placer les acteurs à l'extérieur de la frontière du système et les cas d'utilisation à l'intérieur.
- Nommer la frontière du système étudié.
- Nommer un cas d'utilisation comme un objectif utilisateur ou métier, avec un verbe d'action clair.
- Représenter une association acteur–cas d'utilisation par une ligne simple sans flèche. Elle exprime une interaction, pas un ordre chronologique.
- Utiliser `<<include>>` uniquement lorsqu'un cas de base exécute obligatoirement un comportement réutilisé.
- La dépendance `<<include>>` part du cas de base et pointe vers le cas inclus.
- Utiliser `<<extend>>` uniquement pour un comportement optionnel qui complète un cas de base restant compréhensible et exécutable sans cette extension.
- La dépendance `<<extend>>` part du cas d'extension et pointe vers le cas de base.
- Vérifier systématiquement le sens de toutes les flèches.
- Ne pas transformer une précondition, notamment la connexion préalable, en relation `<<include>>`.
- Ne pas utiliser `<<include>>` ou `<<extend>>` uniquement pour réduire le nombre d'ovales ou embellir le diagramme.
- N'utiliser une généralisation d'acteurs ou de cas que si la relation de spécialisation est réelle et utile au besoin.
- Ne pas représenter dans un cas d'utilisation les étapes internes ou les composants techniques du système.

### 2.2 Diagrammes d'activité

- Utiliser correctement le nœud initial, l'Activity Final et, lorsque nécessaire, le Flow Final.
- Un Activity Final termine toute l'activité ; un Flow Final termine uniquement le flux concerné.
- Un nœud de décision sépare des chemins alternatifs ; un nœud de fusion réunit ces chemins.
- Ne pas utiliser un join pour réunir de simples alternatives.
- Réserver les fork/join aux flux réellement parallèles.
- Associer aux branches des gardes explicites sous la forme `[condition]`.
- Les gardes issues d'une décision doivent être cohérentes et, autant que possible, mutuellement exclusives et complètes ; utiliser `[else]` pour le cas restant.
- Placer chaque action dans la swimlane de l'acteur ou du système qui l'exécute réellement.
- Faire porter les changements automatiques de statut par le système et les décisions métier par l'acteur responsable.
- Représenter explicitement les boucles et les faire revenir vers l'action réellement reprise.
- Ne pas laisser un flux métier actif se terminer artificiellement.
- Ne pas confondre une action, un état métier et une garde de décision.
- Nommer les actions avec un verbe clair et conserver les statuts métier validés.

### 2.3 Diagrammes de séquence

- Lire le temps verticalement, de haut en bas.
- Représenter chaque acteur ou participant une seule fois, avec une seule ligne de vie.
- Un acteur représente une entité extérieure au système étudié.
- Ne pas répéter un acteur sous plusieurs noms, sauf si plusieurs personnes distinctes participent réellement au scénario.
- Dans un diagramme de séquence fonctionnel, représenter le système comme une boîte noire unique et rester au niveau acteur ↔ système.
- Ne pas introduire de `Controller`, `Service`, `Repository`, DTO, base de données ou autre participant technique avant la conception technique.
- Faire correspondre chaque diagramme à un scénario identifiable d'un cas d'utilisation.
- Donner à chaque message un émetteur, un récepteur et un libellé exprimant une action ou une intention claire.
- Représenter les messages d'appel par une ligne continue.
- Utiliser une ligne pointillée pour un retour uniquement lorsque ce retour apporte une information utile.
- Ne pas utiliser une flèche comme simple annotation visuelle.
- Distinguer appel synchrone, appel asynchrone et retour seulement lorsque cette distinction est connue et utile.
- Utiliser `alt` pour des alternatives exclusives, `opt` pour un comportement conditionnel unique, `loop` pour une répétition et `break` pour une interruption réelle du scénario.
- Associer aux opérandes de `alt`, `opt` et `loop` des gardes explicites sous la forme `[condition]`.
- Ne pas utiliser `opt` pour un comportement obligatoire.
- Utiliser une barre d'activation uniquement pendant une exécution réelle ; ne pas l'étendre parce qu'un participant reste visible.
- Éviter de dupliquer le même scénario dans plusieurs diagrammes sans justification.

### 2.4 Diagrammes de classes

- Faire correspondre les classes métier, attributs, types, associations et multiplicités au besoin validé.
- Utiliser la notation `nom : Type` et, si nécessaire, `nom : Type [multiplicité]`.
- Ne pas imposer un type technique lorsqu'il n'a pas été décidé.
- Représenter par `enum` les ensembles fermés de valeurs.
- Une association représente un lien structurel entre instances.
- Une dépendance représente un usage plus faible et ne doit pas remplacer une association métier réelle.
- Une composition utilise un losange noir du côté du tout et suppose une appartenance forte de la partie au tout.
- N'utiliser l'agrégation partagée, avec losange blanc, que lorsqu'une sémantique tout/partie indépendante est réellement justifiée ; sinon préférer une association simple.
- Placer les multiplicités aux bonnes extrémités et pouvoir justifier chacune d'elles par une règle métier, pas par une structure de base de données supposée.
- Nommer les associations ou leurs rôles lorsque leur sens n'est pas évident.
- Utiliser la notation `{contrainte}` pour une contrainte métier utile à la lecture.
- Une généralisation utilise un triangle blanc pointant vers le parent.
- Ne pas représenter deux fois la même information comme attribut et comme association sans justification.
- Ne pas créer d'association classique lorsqu'une information est uniquement le type d'un attribut.
- Une dépendance vers une énumération peut être affichée si elle améliore réellement la lecture, sans être interprétée comme une association métier.
- Ne pas ajouter d'opérations aux classes métier avant la conception de leurs responsabilités.
- Ne pas introduire de détails Spring/JPA, DTO, mapper ou SQL dans un diagramme de classes métier.

### 2.5 Diagrammes de composants

- Un composant représente une unité logicielle cohérente ayant une responsabilité et des interfaces identifiables, pas une classe individuelle.
- Ne représenter que les composants réellement décidés dans l'architecture.
- Distinguer clairement les composants internes, les bibliothèques et les systèmes externes.
- Placer les systèmes externes hors de la frontière du système étudié.
- Utiliser une dépendance dans le sens du composant utilisateur vers le composant utilisé.
- Représenter une interface fournie ou requise uniquement lorsqu'un contrat réel a été identifié.
- Ne pas inventer d'interface, de port, de protocole, de service IA, de proxy, de broker ou de couche technique non décidé.
- Ne pas confondre composant, package, classe, artefact déployable et nœud d'exécution.
- Éviter les dépendances circulaires ; toute dépendance circulaire conservée doit être explicitement justifiée.
- Ne pas mélanger l'architecture logique des composants avec la topologie physique de déploiement.

### 2.6 Diagrammes de déploiement

- Un diagramme de déploiement représente une topologie d'exécution réelle ou explicitement prévue.
- Représenter les appareils, nœuds, environnements d'exécution, artefacts déployés et chemins de communication utiles.
- Distinguer un composant logique, un artefact déployable, un environnement d'exécution et un nœud physique ou virtuel.
- Placer un artefact dans le nœud ou l'environnement où il est réellement déployé.
- Ne pas représenter les acteurs métier dans un diagramme de déploiement.
- Ne nommer un protocole, un port réseau, une zone de sécurité ou une multiplicité d'instances que si cette décision est prise.
- Ne pas inventer Docker, Nginx, cloud, machine virtuelle, équilibrage de charge, réplication ou infrastructure réseau.
- Ne pas utiliser un diagramme de déploiement pour décrire les responsabilités fonctionnelles des composants.
- Maintenir le diagramme cohérent avec l'architecture décidée et, pour sa version finale, avec l'implémentation réellement déployée.

## 3. Conventions de rendu PlantUML

Ces conventions concernent uniquement la source et la présentation PlantUML. Elles n'ajoutent aucune sémantique UML.

### 3.1 Conventions communes

- Encadrer chaque diagramme par `@startuml` et `@enduml`.
- Indiquer le statut dans un commentaire en tête et dans le titre du diagramme.
- Utiliser `DRAFT` par défaut ; utiliser `VALIDATED` uniquement après une demande explicite suivant la validation humaine.
- Utiliser des alias stables et explicites afin de séparer les libellés affichés des identifiants PlantUML.
- Conserver les fichiers en UTF-8 pour préserver la terminologie française.
- Utiliser les réglages de direction, d'espacement et de tracé uniquement pour améliorer la lisibilité.
- Les liens invisibles, `together` et autres contraintes de placement ne doivent servir qu'à la mise en page et ne doivent jamais remplacer une relation UML réelle.
- Ne pas forcer une direction de flèche qui inverse la sémantique de la relation.
- Minimiser les croisements et garder les libellés, gardes et multiplicités lisibles.
- Vérifier le rendu visuel après chaque modification importante, de préférence dans le format destiné au rapport, notamment SVG ou PDF.

### 3.2 Cas d'utilisation

- Utiliser une `rectangle` nommée pour matérialiser la frontière du système.
- Utiliser une ligne simple `--` pour une association acteur–cas d'utilisation.
- Utiliser une dépendance pointillée orientée vers le cas inclus pour `<<include>>`.
- Utiliser une dépendance pointillée orientée vers le cas de base pour `<<extend>>`.
- Les contraintes de placement ne doivent pas créer de flèche visible supplémentaire.

### 3.3 Activités

- Utiliser des partitions/swimlanes nommées de manière constante.
- Rendre les gardes visibles sous la forme `[condition]` et identifier clairement la branche `[else]`.
- Employer les constructions PlantUML adaptées aux décisions, fusions, boucles et parallélismes ; ne pas simuler ces éléments par de simples textes ou flèches libres.
- Vérifier visuellement que les retours de boucle atteignent bien l'action reprise et qu'aucun chemin actif ne disparaît du rendu.

### 3.4 Séquences

- Utiliser `hide footbox` pour éviter la répétition des participants en bas du diagramme.
- Déclarer les acteurs et participants une seule fois en haut.
- Utiliser les flèches continues pour les appels et les flèches pointillées pour les retours utiles.
- Écrire les gardes des fragments `alt`, `opt` et `loop` entre crochets.
- Utiliser `activate` et `deactivate`, ou leur équivalent PlantUML, seulement lorsque les activations rendent une exécution réelle plus claire.
- Vérifier que les lignes de vie restent verticales et que l'ordre graphique des messages correspond à l'ordre temporel.

### 3.5 Classes

- Utiliser `enum` pour les énumérations et la syntaxe d'attribut UML plutôt qu'une syntaxe propre à Java.
- Écrire les multiplicités entre guillemets sur chaque extrémité pertinente d'une association.
- Utiliser `*--` pour une composition, `o--` uniquement pour une agrégation justifiée et `..>` pour une dépendance.
- Placer le losange du côté du tout et vérifier le sens visuel de chaque dépendance.
- Ne pas masquer les attributs ou valeurs d'énumération nécessaires à la compréhension du modèle.

### 3.6 Composants et déploiement

- Utiliser les symboles PlantUML correspondant réellement à `component`, `interface`, `artifact`, `node` et aux environnements d'exécution.
- Utiliser des regroupements visuels uniquement pour représenter une frontière ou une organisation déjà décidée.
- Conserver les systèmes externes et les nœuds hors des regroupements auxquels ils n'appartiennent pas.
- Ne pas faire passer un choix graphique de conteneur, d'icône ou de stéréotype pour une décision d'architecture.

## 4. Relecture obligatoire avant validation

Avant de proposer la validation d'un diagramme :

1. vérifier sa conformité à `functional-scope.md`, à `PROJECT_TRUTH.md` et au présent document ;
2. vérifier son niveau de modélisation et son objectif ;
3. justifier chaque élément, relation, direction, garde et multiplicité ;
4. confirmer qu'aucune fonctionnalité ni décision technique n'a été inventée ;
5. compiler la source PlantUML ;
6. inspecter le rendu pour détecter croisements, ambiguïtés, libellés masqués et flux interrompus ;
7. maintenir le statut `DRAFT` jusqu'à la validation humaine explicite.

Toute génération UML par IA doit être relue selon ces règles avant validation. Un diagramme syntaxiquement valide n'est pas nécessairement un diagramme UML correctement conçu.
