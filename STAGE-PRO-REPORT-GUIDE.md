# Guide de rédaction du rapport de stage professionnel

## Statut et source principale

Ce guide synthétise le document officiel `Plan Type Srtage PRO.pdf`, intitulé « Plan type d'un rapport de stage PRO (1 mois) », pour le parcours Ingénierie logicielle et applications intelligentes (GL+), année universitaire 2026/2027.

Le document officiel est présenté comme un exemple de repère. Il constitue la référence principale pour la structure du rapport. Les conventions internes du dépôt complètent uniquement la présentation lorsqu'elles ne contredisent pas ce plan.

## Structure recommandée

Le rapport vise environ 25 à 35 pages hors annexes, sans allonger artificiellement le contenu.

### Éléments préliminaires

- page de garde ;
- remerciements, environ une demi-page ;
- résumé et abstract de 150 à 200 mots, avec cinq mots-clés ;
- sommaire ;
- liste des figures et des tableaux ;
- glossaire des acronymes.

### Introduction générale

Présenter le contexte du stage, la problématique, les objectifs et l'organisation du rapport, en environ une page.

### Chapitre 1 — Contexte et cadrage du projet

Environ 5 à 7 pages : organisme d'accueil, équipe et cadre du stage, contexte et problématique, étude de l'existant, objectifs, périmètre, méthodologie et planning prévisionnel.

### Chapitre 2 — Analyse et conception

Environ 8 à 10 pages : acteurs, besoins fonctionnels et non fonctionnels, cas d'utilisation, scénarios majeurs, interfaces ou maquettes, architecture, classes de conception, séquences, données et dictionnaire de données.

### Chapitre 3 — Réalisation et validation

Environ 9 à 11 pages : environnement technique, architecture réalisée, fonctionnalités clés illustrées, module complémentaire réellement présent, tests, validation, difficultés et solutions.

Dans ce projet, l'assistance IA est traitée comme module complémentaire du chapitre 3, car elle fait partie de la mission et ne justifie pas à elle seule un quatrième chapitre.

### Conclusion, références et annexes

La conclusion rappelle les objectifs et résultats, les apports du stage, les limites et les perspectives. Les références suivent une norme cohérente (APA ou IEEE). Les annexes contiennent seulement les éléments utiles à la compréhension ou à la vérification du travail.

## Règles rédactionnelles retenues

- Le rapport est un rapport d'ingénierie logicielle, pas un journal de programmation ni une documentation exhaustive du dépôt.
- La progression suit : contexte, problème, besoins, analyse, conception, réalisation, tests, résultats et perspectives.
- Toute affirmation de réalisation, de test ou de validation doit être appuyée par le code, un test exécuté ou une validation humaine identifiable.
- Les diagrammes sont expliqués avant ou après leur insertion ; les modèles secondaires vont en annexe.
- Aucune fonctionnalité, capture, mesure, référence ou information administrative ne doit être inventée.
- Les noms de fichiers internes, commits, prompts et outils d'assistance ne figurent pas dans la narration destinée au jury.

## État technique à respecter

Le frontend Angular utilise actuellement `sessionStorage` pour conserver la session sous la clé `safyron.session.v1`. Le service restaure et contrôle localement la forme et l'expiration du JWT, tandis que la signature du token, le compte actif, les rôles autoritatifs et les contrôles métier restent vérifiés par le backend. Le rapport doit décrire ce fonctionnement réel et non l'ancienne conception « mémoire uniquement ».

Les documents de sécurité, d'API et de conception frontend qui indiquent encore l'absence de `sessionStorage` devront être harmonisés séparément avec le code actuel. Cette harmonisation documentaire ne doit pas modifier le code applicatif dans le cadre de la rédaction du rapport.

## Informations à compléter avant la version finale

- nom et présentation officielle de l'organisme d'accueil ;
- équipe, encadrement et contexte professionnel précis ;
- dates et durée exactes du stage ;
- identité et intitulés institutionnels de la page de garde ;
- planning réel ou prévisionnel suffisamment documenté ;
- références effectivement utilisées et dates de consultation lorsque requises.
