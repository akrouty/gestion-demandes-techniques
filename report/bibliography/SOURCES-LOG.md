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
- **Information utile :** disponibilité via API, identifiant gpt-5.6-luna, endpoint Responses, Structured Outputs et support de reasoning.effort = none, revérifiés le 2026-10-11.
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
- **Information utile :** caractéristiques du modèle, identifiant API mistral-small-2603, Chat Completions et Structured Outputs ; ces trois derniers éléments sont revérifiés le 2026-10-11. Les données de contexte et de tarification restent celles consignées lors de la consultation initiale.
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

## SRC-011 — Sokolova & Lapalme : mesures de classification

- **Auteurs :** Marina Sokolova et Guy Lapalme
- **Titre :** A systematic analysis of performance measures for classification tasks
- **Type :** article scientifique ; notice et résumé consultés sur OpenAIRE
- **URL consultée :** https://explore.openaire.eu/search/result?pid=10.1016/j.ipm.2009.03.002
- **DOI :** `10.1016/j.ipm.2009.03.002`
- **Publication vérifiée :** Information Processing & Management, volume 45, pages 427–437, 2009.
- **Date de consultation :** 2026-10-11
- **Information utile :** le résumé décrit une analyse des mesures de classification et de leur invariance face à des changements de la matrice de confusion et de la distribution des labels.
- **Usage potentiel dans le rapport :** cadre méthodologique des métriques de classification de `docs/ai/AI-DESIGN-V1.md`.
- **Statut :** À REVERIFIER
- **Clé BibLaTeX proposée :** `sokolova-lapalme-2009`
- **Remarque :** métadonnées et résumé vérifiés sur OpenAIRE ; texte intégral non consulté (ScienceDirect : HTTP 403 ; copie universitaire : accès refusé). Le numéro 4 fourni dans la conception n'a pas été revérifié sur la notice ouverte. Consulter le texte avant toute attribution méthodologique détaillée ; cette vérification ne valide aucun benchmark du projet.

---

## SRC-012 — Cohen : weighted kappa

- **Auteur :** J. Cohen
- **Titre :** Weighted kappa: Nominal scale agreement with provision for scaled disagreement or partial credit
- **Type :** article scientifique ; notice bibliographique PubMed consultée
- **URL consultée :** https://pubmed.ncbi.nlm.nih.gov/19673146/
- **DOI :** `10.1037/h0026256`
- **Publication vérifiée :** Psychological Bulletin, 70(4), 213–220, octobre 1968.
- **Date de consultation :** 2026-10-11
- **Information utile :** identité bibliographique de la référence retenue pour le weighted kappa.
- **Usage potentiel dans le rapport :** référence historique de l'option Weighted Cohen's Kappa dans la conception IA.
- **Statut :** À REVERIFIER
- **Clé BibLaTeX proposée :** `cohen-1968-weighted-kappa`
- **Remarque :** PubMed ne fournit pas de résumé ; le texte intégral n'a pas été consulté. Vérifier le texte avant toute attribution détaillée. La métrique reste OPTIONNELLE / À VALIDER, sans pondération choisie.

---

## SRC-013 — Klie et al. : qualité de l'annotation

- **Auteurs :** Jan-Christoph Klie, Richard Eckart de Castilho et Iryna Gurevych
- **Titre :** Analyzing Dataset Annotation Quality Management in the Wild
- **Type :** article scientifique ; notice et résumé officiels ACL Anthology consultés
- **URL consultée :** https://aclanthology.org/2024.cl-3.1/
- **DOI :** `10.1162/coli_a_00516`
- **Publication vérifiée :** Computational Linguistics, 50(3), 817–866, septembre 2024 ; MIT Press.
- **Date de consultation :** 2026-10-11
- **Information utile :** le résumé traite de la gestion des annotateurs, de l'accord, de l'adjudication et de la validation des données ; il signale des erreurs dans l'utilisation des mesures d'accord et des taux d'erreur d'annotation.
- **Usage potentiel dans le rapport :** méthode de provenance et d'annotation humaine du futur dataset d'évaluation IA.
- **Statut :** UTILISABLE
- **Clé BibLaTeX proposée :** `klie-etal-2024-analyzing`
- **Remarque :** vérification limitée à la notice et au résumé ; aucune lecture intégrale revendiquée. Les labels P01–P12 restent proposés et non validés humainement ; le nombre d'annotateurs et l'adjudication restent ouverts.

---

## SRC-014 — scikit-learn : accuracy_score

- **Organisation :** scikit-learn
- **Titre :** accuracy_score
- **Type :** documentation officielle
- **URL consultée :** https://scikit-learn.org/stable/modules/generated/sklearn.metrics.accuracy_score.html
- **Date de consultation :** 2026-10-11
- **Information utile :** la sortie normalisée représente la fraction des observations correctement classées.
- **Usage potentiel dans le rapport :** Accuracy pour la catégorie et la priorité.
- **Statut :** UTILISABLE
- **Clé BibLaTeX proposée :** `sklearn-accuracy-score`
- **Remarque :** Source méthodologique de la conception DRAFT ; aucun benchmark exécuté ni seuil d'acceptation validé.

---

## SRC-015 — scikit-learn : f1_score

- **Organisation :** scikit-learn
- **Titre :** f1_score
- **Type :** documentation officielle
- **URL consultée :** https://scikit-learn.org/stable/modules/generated/sklearn.metrics.f1_score.html
- **Date de consultation :** 2026-10-11
- **Information utile :** le mode macro calcule la moyenne non pondérée des scores par label ; il ne tient pas compte du déséquilibre des effectifs des labels.
- **Usage potentiel dans le rapport :** Macro-F1 pour la catégorie et la priorité.
- **Statut :** UTILISABLE
- **Clé BibLaTeX proposée :** `sklearn-f1-score`
- **Remarque :** Source méthodologique de la conception DRAFT ; aucun benchmark exécuté ni seuil d'acceptation validé.

---

## SRC-016 — scikit-learn : confusion_matrix

- **Organisation :** scikit-learn
- **Titre :** confusion_matrix
- **Type :** documentation officielle
- **URL consultée :** https://scikit-learn.org/stable/modules/generated/sklearn.metrics.confusion_matrix.html
- **Date de consultation :** 2026-10-11
- **Information utile :** la cellule C(i, j) compte les observations de classe réelle i et de classe prédite j.
- **Usage potentiel dans le rapport :** Interprétation des matrices de confusion de catégorie et de priorité.
- **Statut :** UTILISABLE
- **Clé BibLaTeX proposée :** `sklearn-confusion-matrix`
- **Remarque :** Source méthodologique de la conception DRAFT ; aucun benchmark exécuté ni seuil d'acceptation validé.

---

## SRC-017 — scikit-learn : cohen_kappa_score

- **Organisation :** scikit-learn
- **Titre :** cohen_kappa_score
- **Type :** documentation officielle
- **URL consultée :** https://scikit-learn.org/stable/modules/generated/sklearn.metrics.cohen_kappa_score.html
- **Date de consultation :** 2026-10-11
- **Information utile :** le paramètre weights distingue absence de pondération, pondération linéaire et pondération quadratique.
- **Usage potentiel dans le rapport :** Documentation technique de l'option Weighted Cohen's Kappa.
- **Statut :** CONDITIONNELLE
- **Clé BibLaTeX proposée :** `sklearn-cohen-kappa-score`
- **Remarque :** Métrique OPTIONNELLE / À VALIDER ; aucune pondération retenue et aucun calcul exécuté.

---

## SRC-018 — OpenAI — Responses API

- **Organisation :** OpenAI
- **Titre :** Text generation
- **Type :** documentation officielle du fournisseur
- **URL consultée :** https://developers.openai.com/api/docs/guides/text
- **Date de consultation :** 2026-10-11
- **Information utile :** Le guide présente la génération via Responses API et des exemples avec instructions, input et reasoning.effort.
- **Usage potentiel dans le rapport :** Documentation du choix Responses API pour le benchmark.
- **Statut :** CONDITIONNELLE
- **Clé BibLaTeX proposée :** `ai-benchmark-src-018`
- **Remarque :** Guide général consulté ; aucun appel ni accès au compte testé. Benchmark non exécuté ; aucun fournisseur ou modèle final retenu.

---

## SRC-019 — OpenAI — Structured Outputs

- **Organisation :** OpenAI
- **Titre :** Structured model outputs
- **Type :** documentation officielle du fournisseur
- **URL consultée :** https://developers.openai.com/api/docs/guides/structured-outputs
- **Date de consultation :** 2026-10-11
- **Information utile :** Le guide documente une sortie structurée via text.format, json_schema et strict dans Responses.
- **Usage potentiel dans le rapport :** Format structuré du benchmark, sans modification du JSON Schema métier.
- **Statut :** CONDITIONNELLE
- **Clé BibLaTeX proposée :** `ai-benchmark-src-019`
- **Remarque :** La documentation du format ne remplace pas la validation backend ni ne prouve la correction des labels. Benchmark non exécuté ; aucun fournisseur ou modèle final retenu.

---

## SRC-020 — OpenAI — paramètres de raisonnement

- **Organisation :** OpenAI
- **Titre :** Reasoning models
- **Type :** documentation officielle du fournisseur
- **URL consultée :** https://developers.openai.com/api/docs/guides/reasoning
- **Date de consultation :** 2026-10-11
- **Information utile :** reasoning.effort dans Responses est spécifique au modèle ; la page GPT-5.6 Luna déjà enregistrée en SRC-008 confirme le support de none.
- **Usage potentiel dans le rapport :** Traçabilité du choix reasoning = none pour Luna et des différences de paramètres entre API.
- **Statut :** CONDITIONNELLE
- **Clé BibLaTeX proposée :** `ai-benchmark-src-020`
- **Remarque :** Aucun alignement artificiel avec les paramètres Mistral. Le support précis de temperature = 0 pour Luna via Responses n’a pas été établi. La référence API Responses essayée n’a pas pu être ouverte ; elle n’est pas ajoutée comme source vérifiée. Benchmark non exécuté ; aucun fournisseur ou modèle final retenu.

---

## SRC-021 — Mistral — Chat Completions

- **Organisation :** Mistral AI
- **Titre :** Chat
- **Type :** documentation officielle du fournisseur
- **URL consultée :** https://docs.mistral.ai/api/endpoint/chat
- **Date de consultation :** 2026-10-11
- **Information utile :** La référence documente Chat Completions, response_format de type json_schema et le paramètre temperature, avec une plage recommandée incluant 0.0.
- **Usage potentiel dans le rapport :** Paramètres et format du candidat mistral-small-2603, identifiant vérifié sur la page SRC-009.
- **Statut :** CONDITIONNELLE
- **Clé BibLaTeX proposée :** `ai-benchmark-src-021`
- **Remarque :** Référence API consultée, sans appel de modèle ni validation effective d’une requête. Benchmark non exécuté ; aucun fournisseur ou modèle final retenu.

---

## SRC-022 — Mistral — Custom Structured Outputs

- **Organisation :** Mistral AI
- **Titre :** Custom
- **Type :** documentation officielle du fournisseur
- **URL consultée :** https://docs.mistral.ai/studio/conversations/structured-output/custom
- **Date de consultation :** 2026-10-11
- **Information utile :** Le guide décrit les sorties structurées à partir d’un JSON Schema et indique une instruction de schéma ajoutée au prompt système par le fournisseur.
- **Usage potentiel dans le rapport :** Documentation de Structured Outputs et traçabilité des différences internes entre fournisseurs.
- **Statut :** CONDITIONNELLE
- **Clé BibLaTeX proposée :** `ai-benchmark-src-022`
- **Remarque :** La différence fournisseur est documentée ; aucune donnée métier supplémentaire n’est ajoutée au prompt du projet. Benchmark non exécuté ; aucun fournisseur ou modèle final retenu.

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
