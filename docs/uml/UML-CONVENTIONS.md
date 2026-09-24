# Conventions UML du projet

Ce document définit les règles obligatoires de modélisation UML pour l'ensemble du projet.

Avant toute création ou modification d'un diagramme UML, il faut lire :

- `docs/requirements/functional-scope.md` ;
- `PROJECT_TRUTH.md` ;
- `docs/uml/UML-CONVENTIONS.md`.

Les diagrammes UML ne doivent pas seulement compiler. Ils doivent être sémantiquement corrects, pédagogiques, lisibles et adaptés à un rapport d'ingénierie.

## Règles générales

- Respecter la notation UML et la sémantique réelle des relations.
- Ne jamais inventer une fonctionnalité, un acteur, une classe ou un composant pour enrichir artificiellement un diagramme.
- Conserver exactement la terminologie métier validée.
- Distinguer les diagrammes d'analyse des diagrammes techniques.
- Privilégier la lisibilité à la complexité.
- Minimiser les croisements et les relations ambiguës.
- Garder le statut `DRAFT` jusqu'à validation humaine.
- Codex ne doit jamais passer lui-même un nouveau diagramme à `VALIDATED` sans demande explicite.

## Diagrammes de cas d'utilisation

- Placer les acteurs à l'extérieur de la frontière du système.
- Placer les cas d'utilisation à l'intérieur de la frontière du système.
- Représenter les associations entre acteurs et cas d'utilisation par des lignes simples.
- Utiliser `<<include>>` uniquement pour un comportement obligatoire réutilisé.
- Utiliser `<<extend>>` uniquement pour un comportement optionnel.
- Vérifier systématiquement le sens des flèches `<<include>>` et `<<extend>>`.
- Ne pas transformer une simple précondition en relation `<<include>>`.

## Diagrammes d'activité

- Utiliser correctement les nœuds initiaux et finaux, les décisions, les fusions et les gardes.
- Utiliser la notation `[condition]` pour les branches.
- Respecter les swimlanes et la responsabilité réelle de chaque acteur.
- Représenter explicitement les boucles.
- Ne pas laisser un flux métier encore actif se terminer artificiellement.

## Diagrammes de séquence

- Placer les acteurs et les participants une seule fois en haut du diagramme.
- Utiliser `hide footbox` afin d'éviter leur répétition en bas.
- Utiliser des lignes de vie verticales.
- Représenter les messages d'appel par une ligne continue.
- Représenter les messages de retour par une ligne pointillée lorsque le retour apporte une information utile.
- Utiliser correctement `alt`, `opt` et `loop` avec des gardes `[condition]`.
- Ne pas utiliser `opt` lorsqu'un comportement est obligatoire.
- Utiliser les barres d'activation (`activate` / `deactivate`, ou leur équivalent PlantUML) uniquement pendant une exécution réelle.
- Ne pas ajouter une activation longue simplement parce qu'un acteur reste présent dans le scénario.
- Ne pas répéter les acteurs sous différents noms, sauf si deux personnes distinctes participent réellement au scénario.
- Dans les diagrammes de séquence fonctionnels, rester au niveau acteur ↔ système et ne pas introduire de `Controller`, `Service`, `Repository`, DTO ou base de données avant la conception technique.

## Diagrammes de classes

- Faire correspondre les classes métier, les attributs, les types, les associations et les multiplicités au besoin validé.
- Utiliser correctement les associations, les dépendances, les agrégations et les compositions.
- Ne pas utiliser une association classique lorsque l'information est seulement un type d'attribut.
- Utiliser `enum` pour les ensembles de valeurs fermés.
- Éviter les doublons entre attributs et associations.
- Justifier toutes les multiplicités par une règle métier.
- Ne pas introduire de détails Spring/JPA dans le diagramme métier.

## Relecture avant validation

Toute génération UML par IA doit être relue selon ces règles avant validation. Un diagramme syntaxiquement valide n'est pas nécessairement un diagramme UML correctement conçu.
