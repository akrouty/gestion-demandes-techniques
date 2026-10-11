# SOURCES-LOG — registre interne des sources du rapport

> **Statut : registre de travail interne — ce fichier n'est pas la bibliographie finale.**
>
> Objectif : conserver une trace vérifiable des sources externes réellement consultées
> pendant le projet afin de préparer ensuite `references.bib` sans inventer d'URL,
> de date de consultation, d'auteur, de titre ou de résultat.
>
> Règles :
>
> - inscrire uniquement une source réellement ouverte et vérifiée ;
> - conserver la **vraie date de consultation** ;
> - préciser l'information réellement utilisée ou susceptible d'être utilisée ;
> - ne transférer une entrée vers `references.bib` que si elle est effectivement citée
>   ou exploitée dans le rapport ;
> - une source marquée **conditionnelle** ne doit pas être citée comme preuve
>   d'une technologie finalement utilisée tant que cette technologie n'est pas
>   implémentée et validée ;
> - lors de la rédaction finale, rouvrir les sources web importantes et consigner
>   la nouvelle date si elles sont revérifiées ;
> - les documents internes du projet (code, UML, ADR, PROJECT_TRUTH, tests) servent
>   à vérifier ce qui a été conçu/implémenté/testé, mais ne remplacent pas les
>   références externes lorsqu'une affirmation générale sur une technologie a
>   besoin d'une source.

## Statuts

- **UTILISABLE** : source officielle vérifiée et déjà pertinente pour un choix ou une notion du projet.
- **CONDITIONNELLE** : source vérifiée, mais à citer seulement si la technologie ou le modèle concerné est finalement retenu/utilisé.
- **À REVERIFIER** : source déjà identifiée mais devant être rouverte avant citation finale.

---

## SRC-001 — JSON Web Token (JWT), RFC 7519

- **Organisation / auteurs :** IETF — M. Jones, J. Bradley, N. Sakimura
- **Titre :** JSON Web Token (JWT)
- **Type :** norme / RFC
- **URL :** https://www.rfc-editor.org/rfc/rfc7519.html
- **Date de publication :** mai 2015
- **Date de consultation :** 2026-10-11
- **Information utile :** définition du JWT comme format compact de représentation de claims et noms de claims standards tels que `iss`, `sub`, `aud`, `exp` et `iat`.
- **Usage potentiel dans le rapport :** partie sécurité / authentification JWT.
- **Statut :** UTILISABLE
- **Clé BibLaTeX proposée :** `rfc7519-jwt`
- **Remarque :** citer la RFC pour la définition du standard ; décrire l'implémentation réelle du projet à partir du code et des tests, pas à partir de la RFC.

---

## SRC-002 — Spring Security Password Storage

- **Organisation :** Spring
- **Titre :** Password Storage
- **Type :** documentation officielle
- **URL :** https://docs.spring.io/spring-security/reference/7.0/features/authentication/password-storage.html
- **Date de consultation :** 2026-10-11
- **Information utile :** rôle de `PasswordEncoder` pour la transformation à sens unique des mots de passe ; documentation de `BCryptPasswordEncoder` et du coût BCrypt.
- **Usage potentiel dans le rapport :** justification du hachage des mots de passe avec Spring Security / BCrypt.
- **Statut :** UTILISABLE
- **Clé BibLaTeX proposée :** `spring-security-password-storage`
- **Remarque :** ne pas utiliser la documentation pour prétendre qu'un coût précis a été mesuré dans notre projet ; toute valeur réellement utilisée doit être vérifiée dans le code/configuration.

---

## SRC-003 — Spring Boot Externalized Configuration

- **Organisation :** Spring
- **Titre :** Properties and Configuration — Externalize the Configuration of SpringApplication
- **Type :** documentation officielle
- **URL :** https://docs.spring.io/spring-boot/how-to/properties-and-configuration.html
- **Date de consultation :** 2026-10-11
- **Information utile :** Spring Boot permet d'externaliser la configuration et d'utiliser notamment propriétés et variables d'environnement.
- **Usage potentiel dans le rapport :** configuration externe des secrets et paramètres d'environnement.
- **Statut :** UTILISABLE
- **Clé BibLaTeX proposée :** `spring-boot-externalized-configuration`
- **Remarque :** le projet utilise réellement des variables externes pour les secrets ; les noms exacts et leur présence doivent être vérifiés dans le dépôt au moment de la rédaction.

---

## SRC-004 — PostgreSQL Date/Time Types

- **Organisation :** PostgreSQL Global Development Group
- **Titre :** PostgreSQL Documentation — Date/Time Types
- **Type :** documentation officielle
- **URL :** https://www.postgresql.org/docs/current/datatype-datetime.html
- **Date de consultation :** 2026-10-11
- **Information utile :** `timestamp with time zone` / `timestamptz`, stockage des instants et comportement lié aux fuseaux horaires.
- **Usage potentiel dans le rapport :** justification du stockage temporel `Instant` + PostgreSQL `timestamptz`.
- **Statut :** UTILISABLE
- **Clé BibLaTeX proposée :** `postgresql-datetime-types`
- **Remarque :** la page `current` peut évoluer ; la rouvrir avant la bibliographie finale et, si nécessaire, figer la version PostgreSQL réellement utilisée.

---

## SRC-005 — Flyway Versioned Migrations

- **Organisation :** Redgate
- **Titre :** Versioned migrations
- **Type :** documentation officielle
- **URL :** https://documentation.red-gate.com/fd/versioned-migrations-273973333.html
- **Date de consultation :** 2026-10-11
- **Information utile :** migrations versionnées appliquées dans l'ordre, une fois, et suivies dans l'historique de schéma.
- **Usage potentiel dans le rapport :** justification de Flyway pour versionner et reproduire l'évolution du schéma de base de données.
- **Statut :** UTILISABLE
- **Clé BibLaTeX proposée :** `redgate-flyway-versioned-migrations`
- **Remarque :** l'implémentation réelle du projet doit être illustrée par les migrations effectivement présentes dans le dépôt.

---

## SRC-006 — Angular Accessibility

- **Organisation :** Angular / Google
- **Titre :** Accessibility
- **Type :** documentation officielle
- **URL :** https://angular.dev/best-practices/a11y
- **Date de consultation :** 2026-10-11
- **Information utile :** bonnes pratiques d'accessibilité Angular ; la documentation indique qu'Angular Material fournit des composants réutilisables visant l'accessibilité et que le CDK propose des outils dédiés.
- **Usage potentiel dans le rapport :** choix UI/UX et accessibilité du frontend.
- **Statut :** UTILISABLE
- **Clé BibLaTeX proposée :** `angular-accessibility`

---

## SRC-007 — Angular Material

- **Organisation :** Angular / Google
- **Titre :** Angular Material — Material Design components for Angular
- **Type :** documentation officielle
- **URL :** https://material.angular.dev/
- **Date de consultation :** 2026-10-11
- **Information utile :** bibliothèque de composants Angular, intégration avec Angular, composants accessibles, testés et personnalisables.
- **Usage potentiel dans le rapport :** justification du choix Angular Material pour les formulaires, tables, navigation, feedback et cohérence visuelle.
- **Statut :** UTILISABLE
- **Clé BibLaTeX proposée :** `angular-material`

---

## SRC-008 — OpenAI GPT-5.6 Luna

- **Organisation :** OpenAI
- **Titre :** GPT-5.6 Luna Model
- **Type :** documentation officielle du fournisseur
- **URL :** https://developers.openai.com/api/docs/models/gpt-5.6-luna
- **Date de consultation :** 2026-10-11
- **Information utile :** disponibilité via API, caractéristiques du modèle et prise en charge des structured outputs.
- **Usage potentiel dans le rapport :** comparaison/benchmark des candidats IA et, uniquement s'il est retenu, description du modèle intégré.
- **Statut :** CONDITIONNELLE
- **Clé BibLaTeX proposée :** `openai-gpt-5-6-luna`
- **Remarque :** GPT-5.6 Luna est actuellement un **candidat à évaluer**, pas encore le modèle final du projet. Ne pas écrire dans le rapport qu'il est utilisé tant que le benchmark et l'intégration ne le prouvent pas.

---

## SRC-009 — Mistral Small 4

- **Organisation :** Mistral AI
- **Titre :** Mistral Small 4
- **Type :** documentation officielle du fournisseur
- **URL :** https://docs.mistral.ai/models/mistral-small-4-0-26-03
- **Date indiquée sur la page :** 16 mars 2026
- **Date de consultation :** 2026-10-11
- **Information utile :** caractéristiques du modèle, identifiant API, contexte, structured outputs et informations de tarification affichées par le fournisseur au moment de la consultation.
- **Usage potentiel dans le rapport :** comparaison/benchmark des candidats IA et, uniquement s'il est retenu, description du modèle intégré.
- **Statut :** CONDITIONNELLE
- **Clé BibLaTeX proposée :** `mistral-small-4`
- **Remarque :** modèle challenger pour l'évaluation ; ne pas le présenter comme modèle retenu avant résultats réels.

---

## SRC-010 — Docker Compose

- **Organisation :** Docker
- **Titre :** Docker Compose
- **Type :** documentation officielle
- **URL :** https://docs.docker.com/compose/
- **Date de consultation :** 2026-10-11
- **Information utile :** Compose permet de définir et exécuter une application multi-conteneurs et de gérer services, réseaux et volumes depuis une configuration déclarative.
- **Usage potentiel dans le rapport :** déploiement / reproductibilité de l'environnement, **uniquement si la conteneurisation est réellement implémentée et validée dans le projet**.
- **Statut :** CONDITIONNELLE
- **Clé BibLaTeX proposée :** `docker-compose`
- **Remarque :** Docker/Compose n'est pas encore présenté comme implémenté à cette date ; cette source ne doit pas servir à prétendre le contraire.

---

# Sources à ajouter plus tard

Ajouter ici, au moment où elles sont réellement consultées :

- documentation officielle du fournisseur IA finalement retenu ;
- documentation exacte de l'API/Structured Outputs réellement utilisée ;
- éventuelle documentation Docker/Nginx/hébergement si le déploiement est réellement réalisé ;
- toute source académique ou industrielle réellement utilisée pour l'état de l'art ;
- sources relatives aux critères métier IA si elles sont fondées sur des documents externes ;
- normes ou recommandations de sécurité supplémentaires réellement exploitées.

# Procédure avant rédaction finale d'un chapitre

1. Identifier les affirmations externes du chapitre.
2. Vérifier si une source correspondante existe dans ce registre.
3. Rouvrir la source web si l'information est susceptible d'avoir changé.
4. Mettre à jour la date de **dernière vérification** si elle est revue.
5. Transférer uniquement les sources réellement citées vers `references.bib`.
6. Insérer les citations LaTeX au niveau des affirmations concernées.
7. Ne jamais utiliser ce registre comme preuve qu'une fonctionnalité du projet est implémentée : pour cela, vérifier le code et les tests.
