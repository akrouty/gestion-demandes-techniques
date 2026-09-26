# RAPPORT-CONVENTIONS

**DRAFT — EN ATTENTE DE VALIDATION HUMAINE**

## 1. Objectif

Référence obligatoire avant toute création ou modification du rapport : structure, rédaction, LaTeX, figures, UML, architecture, références, traçabilité et validation progressive. Le rapport évolue par étapes : phase réellement validée → rédaction ou mise à jour correspondante → compilation → revue humaine.

## 2. Hiérarchie des sources de vérité

1. Guide officiel EPI.
2. `PROJECT_TRUTH.md`.
3. Documents explicitement `VALIDATED` ou explicitement validés par l'humain.
4. ADR explicitement validés.
5. Code réellement présent.
6. Tests réellement exécutés.
7. Autres documents cohérents avec les précédents.

Cette hiérarchie ne transforme jamais une conception en preuve de réalisation. En cas de contradiction, signaler le conflit, suspendre la rédaction concernée et attendre sa résolution ; ne pas choisir arbitrairement ni masquer l'incohérence.

## 3. États du projet et vocabulaire

| État | Signification et formulation autorisée |
|---|---|
| Prévu | Intention documentée : « Cette fonctionnalité est prévue. » |
| En conception | Analyse encore ouverte : « La conception est en cours. » |
| Conception validée | Décision humaine explicite : « L'architecture retenue prévoit… » |
| En implémentation | Code incomplet : « Le développement est en cours. » |
| Implémenté | Fonction présente dans le code identifié : « Le backend implémente… » |
| Testé | Test réellement exécuté avec résultat et périmètre identifiables. |
| Validé | Validation humaine explicite du livrable et de sa version. |

Prévu ≠ conçu ≠ implémenté ≠ testé ≠ validé. Un document DRAFT reste DRAFT. Codex ne peut jamais décider seul du passage à VALIDATED.

## 4. Règle d'évolution du rapport

Phase réellement validée → modification ciblée → compilation → contrôle visuel → revue humaine → validation. Identifier les sources et leur version dans les commentaires internes ou la revue Git. Ne mettre à jour que les sections supportées par la phase validée.

### Travail obligatoire chapitre par chapitre

La branche officielle et unique de référence est `main`. Travailler directement sur son état courant, sans créer de branche de rapport, consulter une ancienne branche comme source de vérité ni fusionner une ancienne branche.

À tout moment, un seul chapitre est actif. Le chapitre actif actuel est **Chapitre 1 — Contexte et environnement du projet**, dans `report/chapters/01-contexte-environnement.tex`.

Processus obligatoire : chapitre actif → rédaction / correction → compilation → contrôle du PDF → revue humaine → corrections éventuelles → validation humaine explicite → seulement ensuite chapitre suivant.

Codex ne doit jamais commencer à rédiger le chapitre suivant sans accord explicite de l'utilisateur, par exemple : « Chapitre 1 validé, passe au chapitre 2. » Une compilation réussie ou une revue partielle ne vaut pas cette autorisation.

Sans cet accord, ne rédiger ni le chapitre 2, ni le chapitre 3, ni aucun autre chapitre suivant et ne pas anticiper leur contenu. Les squelettes existants peuvent rester présents, sans nouveau contenu rédactionnel. L'introduction générale, la conclusion, le résumé, la dédicace et les remerciements restent également hors du travail actif actuel.

## 5. Règles concernant le contenu

Interdiction d'inventer résultats, métriques, tests, captures, architecture, fonctionnalités, décisions techniques, références, citations, dates ou problèmes métier. Une technologie ne peut être annoncée comme utilisée avant son utilisation réelle. Toute affirmation de réalisation doit être reliée au code ; tout résultat doit être relié à un test exécuté ou une observation traçable.

## 6. Règles concernant l'IA

Aucun résultat codé en dur ne peut être présenté comme de l'IA. Distinguer assistance IA, décision humaine, conception fonctionnelle et implémentation réelle. La validation humaine des suggestions reste obligatoire conformément au périmètre validé. Ne pas annoncer de modèle, fournisseur, API, performances ou métriques avant décision, intégration et vérification réelles. L'exigence de fonctionnement manuel en cas d'échec IA n'est pas la preuve d'un mécanisme technique existant.

## 7. UML

Les `.puml` de `docs/uml/` sont les sources officielles. Respecter `docs/uml/UML-CONVENTIONS.md`. Seuls les diagrammes VALIDATED peuvent être présentés comme validés. Les PDF/SVG du rapport représentent ces sources ; aucune retouche manuelle ne doit changer leur sens. Expliquer les diagrammes essentiels dans le corps ; réserver les annexes aux compléments. Ne pas dupliquer les sources ni modifier les UML pour faciliter le rapport.

## 8. Architecture et ADR

Une décision technique importante entre dans le rapport lorsqu'elle est suffisamment stabilisée, cohérente avec `PROJECT_TRUTH.md`, les ADR concernés et les autres documents de conception. Une décision d'architecture ne prouve pas son implémentation. Vérifier son intégration et son statut dans la branche de référence `main` avant de la reprendre dans le rapport.

## 9. Bibliographie et netographie

Aucune référence ou citation inventée ; chaque entrée correspond à une source réellement exploitée. Ne pas remplir artificiellement `references.bib`. Pour Internet : URL complète et vraie date de consultation, jamais estimée. Classer les références par nom d'auteur ; renseigner les données réellement connues : nom et prénom, année, titre, éditeur, édition et pages selon le type de source. Ne pas fabriquer les champs manquants. Le guide interne ne reçoit pas une URL ou une date de publication supposée.

## 10. Style académique

Français clair et naturel, phrases raisonnablement courtes, terminologie de génie logiciel adaptée, paragraphes structurés et transitions utiles. Éviter remplissage, formulations vagues, répétitions, phrases automatiques génériques, adjectifs promotionnels et conclusions sans preuve. Relier chaque choix technologique à un besoin réel documenté.

## Règles pédagogiques et niveau de lecture du rapport

### Lecteur cible et autonomie du texte

Le rapport s'adresse à un enseignant ou à un jury qui ne connaît ni le dépôt Git, ni les conversations, ni les documents internes, ni leur circuit de validation. Il doit être compréhensible de manière autonome : présenter le contexte, les besoins et les décisions avec les explications nécessaires à leur compréhension.

### Sources internes et narration académique

Les sources internes servent à vérifier la vérité du projet ; elles ne constituent pas la narration du rapport. Sauf nécessité académique justifiée, ne pas citer dans le corps du texte les identifiants ADR-001, ADR-002, etc., `PROJECT_TRUTH.md`, les noms ou chemins de fichiers Markdown, les branches, les commits, les codes de règles tels que RM01 ou les statuts DRAFT et VALIDATED. Ces références peuvent rester dans les commentaires internes.

Éviter : « L'architecture backend est validée dans ADR-002. »

Préférer : « Une architecture modulaire a été retenue afin de séparer les principales responsabilités de l'application tout en conservant un déploiement simple. »

Expliquer la décision, sa raison et ses conséquences, plutôt que le document qui la consigne. L'exemple de formulation ne dispense pas de vérifier la décision dans les sources actuelles.

### Progression du général vers le particulier

Construire la lecture selon la progression : contexte → problème → besoin → objectifs → fonctionnement attendu → analyse → modélisation → architecture générale → choix techniques expliqués → implémentation → tests → résultats. Introduire un besoin et son sens avant les mécanismes techniques qui y répondent, en respectant l'avancement réel du projet et la validation chapitre par chapitre.

### Justification des choix

Présenter les choix importants selon le raisonnement : besoin → problème ou contrainte → options envisagées → choix retenu → justification → conséquence pour le projet. Ne pas produire un catalogue de technologies ni justifier un choix par sa seule popularité. Ne pas inventer une comparaison d'options qui n'a pas été menée.

### Définition des termes et acronymes

Définir les termes et acronymes importants à leur première apparition, en expliquant leur rôle dans le projet. Par exemple, présenter un jeton d'authentification au format JSON Web Token (JWT), une interface de programmation (API) suivant les principes REST (Representational State Transfer), le contrôle d'accès fondé sur les rôles (RBAC, Role-Based Access Control) ou une application web à page unique (SPA, Single-Page Application) avant de réutiliser leurs formes abrégées. N'introduire ces notions que lorsqu'elles sont utiles à la compréhension.

### Niveau de détail et langue

Employer un français académique simple et précis. Éviter le jargon non expliqué, les accumulations d'acronymes et les noms de classes, de packages ou de points d'accès techniques sans utilité pour le lecteur.

Le corps du rapport explique uniquement les concepts et décisions nécessaires à la compréhension du projet. Les détails exhaustifs de conception, contrats API, endpoints, classes, packages ou documents internes restent dans les documents techniques ou éventuellement en annexe lorsqu'ils apportent une vraie valeur académique.

### Construction des sections

Une section importante comporte généralement une introduction, le besoin ou le concept étudié, les explications essentielles, une figure ou un tableau si utile, son interprétation et une transition. Adapter cette organisation au propos : ne pas la transformer en sous-sections artificielles ni en simple liste de décisions.

### Usage pédagogique des diagrammes

Chaque diagramme doit répondre à une question précise : qui utilise le système (cas d'utilisation), quel est le cycle de vie d'une demande (activité), quelles données sont liées (classes), comment se déroule un scénario (séquence). Introduire la question avant la figure et interpréter les éléments importants après celle-ci. Sélectionner les diagrammes utiles à l'argumentation ; réserver les compléments secondaires aux annexes au lieu de reproduire systématiquement tous les modèles.

### Conception et réalisation

Distinguer systématiquement conception et implémentation. « La solution prévoit un contrôle des droits » décrit une conception ; « Le serveur contrôle les droits lors de cette opération » affirme une réalisation et exige une vérification du code actuel. « Ce contrôle a été testé » exige en plus un test réellement exécuté et un résultat traçable. Une décision ou un modèle validé ne prouve jamais que l'application est implémentée ou testée.

### Traçabilité interne

Conserver la traçabilité détaillée dans les commentaires LaTeX, le document de vérité du projet, les ADR, les documents de conception et l'historique Git. Le texte destiné au jury expose le raisonnement et l'état réel du projet, sans raconter ce suivi interne. Ces règles complètent les prescriptions EPI et ne changent ni le statut DRAFT ni l'obligation de validation humaine du chapitre actif avant le suivant.

## 11. Conventions LaTeX

### Sources académiques et structure

Règles vérifiées dans le fichier joint `Guide PFE.pdf`, « Final Project Report Preparation Guide », pages 1 à 5 ; ce fichier n'est pas présent dans le dépôt au point de départ `cb721ab`. Conserver l'accès au guide pour les revues futures. Le format A4 est demandé par la consigne de cette mission ; le texte du guide joint ne le précise pas explicitement. Les choix d'outillage ci-dessous sont des conventions du projet, pas des prescriptions EPI.

Pages préliminaires : garde, dédicace, remerciements, résumé (150 à 250 mots), 5 à 7 mots-clés, sommaire, listes des figures et tableaux. La page officielle EPI `report/frontmatter/page_du_garde.pdf` constitue la référence graphique et est incluse comme unique première page, sans numéro visible. Ne pas recréer son design en LaTeX : conserver sa charte graphique, ses logos, ses couleurs et ses textes institutionnels. Seules les informations variables nécessaires peuvent être adaptées. L'année universitaire du présent rapport est 2026/2027 ; le PDF fourni porte encore 2025/2026, à remplacer proprement dans le modèle source (TODO interne dans `cover.tex`). Les informations personnelles ou professionnelles non confirmées ne doivent pas être inventées. Puis introduction générale (2 pages maximum), quatre chapitres : contexte et environnement ; étude de marché et état de l'art ; cadre théorique et analytique ; implémentation et mise en pratique. Terminer par conclusion générale (2 pages maximum), annexes numérotées et titrées, références et table des matières détaillée (guide p. 2). Chaque chapitre principal dispose d'une page séparatrice portant son titre.

### Mise en page

A4 ; Times New Roman ou Times Roman ; corps 12 pt ; interligne 1,5 ; marges de 2,5 cm. En-tête et pied à 1 cm, titre courant de chapitre limité à 40 caractères, caractères de 10 pt de graisse normale, pagination en pied. La pagination commence à l'introduction générale. Les pages préliminaires ne portent pas de numéro visible (choix du projet). Titres alignés à gauche, numérotation décimale, pas de soulignement ni d'excès de majuscules ; emphase en gras ou italique. Désactiver la césure automatique, éviter veuves, orphelines et titres isolés en bas de page.

Figures et tableaux : numérotation continue indépendante des chapitres, légende 12 pt, figure sous l'image et tableau au-dessus ; une ligne avant et deux après la légende. Le guide mentionne à la fois justification et centrage : une ligne est centrée, une légende multiligne justifiée. Placer l'élément près de sa première mention, images de qualité et présentation homogène. Graphiques : axes identifiés, titre bref en gras 12 pt et légende sous le graphique. Formules numérotées de (1) à (n), numéro à droite. Notes de bas de page : 10 pt, interligne 11 pt, justifiées sans alinéa, numérotation continue ; appel 10 pt relevé de 3 pt. La notation des remarques du guide est « NOTE.— ».

### Organisation technique

`main.tex` assemble uniquement le document. Séparer configuration, métadonnées, chapitres, bibliographie, figures et annexes. UTF-8, chemins relatifs, labels explicites et références croisées LaTeX : ne jamais saisir manuellement les numéros de figures ou de sections. Packages limités aux besoins réels, structure simple, aucune police propriétaire versionnée. Moteur officiel : pdfLaTeX. Police libre de style Times via `fontenc` (T1), `newtxtext` et `newtxmath`, sans dépendance à une police système spécifique. Compilation reproductible via LaTeX Workshop / `latexmk -pdf` ; aucune police propriétaire versionnée.

## 12. Statuts éditoriaux

- `RÉDIGER MAINTENANT` : sources suffisamment validées pour rédiger la section correspondante, puis la compiler et la soumettre à revue humaine.
- `COMPLÉTER PLUS TARD` : structure préparée, informations encore manquantes.
- `ATTENDRE` : dépend d'une implémentation, de tests, de mesures ou de décisions absentes.

Ces informations restent dans les commentaires LaTeX ; elles ne doivent pas apparaître dans le PDF final. Un statut éditorial ne valide ni une source ni une fonctionnalité.

## 13. Contrôle obligatoire avant modification

1. Lire ce fichier.
2. Lire `PROJECT_TRUTH.md`.
3. Identifier les sources concernées.
4. Vérifier leurs statuts et versions.
5. Vérifier le code pour toute description d'implémentation.
6. Vérifier les tests réellement exécutés pour toute description de validation.
7. Modifier uniquement le contenu supporté.
8. Compiler.
9. Contrôler visuellement le rendu et les avertissements.
10. Soumettre à validation humaine sans modifier soi-même le statut DRAFT.
