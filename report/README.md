# Rapport évolutif EPI

**DRAFT — EN ATTENTE DE VALIDATION HUMAINE**

Lire obligatoirement [RAPPORT-CONVENTIONS.md](../RAPPORT-CONVENTIONS.md) et `../PROJECT_TRUTH.md` avant toute modification. Ce dossier contient un rapport évolutif : les sections sont rédigées ou mises à jour lorsqu’elles reposent sur une phase réellement validée, puis compilées et soumises à revue humaine. Les sections 1.3 à 1.5 disposent d’une première rédaction ; les autres contenus restent à compléter selon leurs sources et statuts.

## Compilation

Moteur officiel : **pdfLaTeX**. Compilation recommandée depuis LaTeX Workshop avec sa recette normale `latexmk -pdf`. Aucune configuration VS Code spécifique n'est nécessaire.

Prérequis : pdfLaTeX, latexmk, Biber et les packages du préambule. La police libre de style Times est fournie par `newtxtext` et `newtxmath`, avec l'encodage T1 (`fontenc`). Aucune installation locale de Times New Roman ni police Windows n'est requise. XeLaTeX et LuaLaTeX ne sont pas nécessaires. Les sources restent en UTF-8, pris en charge nativement par LaTeX moderne ; le français utilise `babel`.

Depuis `report/`, dans PowerShell :

```powershell
latexmk -pdf -interaction=nonstopmode -file-line-error main.tex
```

Le PDF est `main.pdf`. Pour isoler les fichiers générés, ajouter `-outdir=.build/pdflatex` à la commande ; le PDF sera alors `.build/pdflatex/main.pdf`. Seul `main.pdf` est versionné parmi les produits de compilation ; les artefacts temporaires et `.build/` restent ignorés. Latexmk pilote les passes pdfLaTeX et Biber nécessaires. Une bibliographie vide peut produire les avertissements « Empty bibliography » et « does not contain any citations » : ils sont attendus tant qu'aucune source n'a été citée. Ne jamais ajouter une fausse entrée pour les supprimer.

## Organisation

- `main.tex` : orchestration uniquement.
- `config/` : packages, mise en page, métadonnées vides à compléter avec des informations vérifiées.
- `frontmatter/` : pages préliminaires ; garde officielle `frontmatter/page_du_garde.pdf`, incluse par `cover.tex`.
- `chapters/` : introduction, quatre chapitres, conclusion ; statuts et preuves dans les commentaires internes.
- `appendices/` : mécanisme des annexes, sans contenu artificiel.
- `bibliography/references.bib` : sources réellement exploitées ; classement par auteur avec BibLaTeX/Biber.
- `figures/uml/` : rendus seulement ; les sources restent dans `../docs/uml/`.
- `figures/architecture/`, `screenshots/`, `results/` : vides jusqu'à disponibilité de contenus réels.

Les chapitres commencent par une page séparatrice. Le sommaire et la table détaillée sont générés automatiquement à partir des mêmes titres. Les listes des figures et tableaux sont actuellement vides. Les numéros visibles commencent à l'introduction. Les sections encore en attente conservent seulement leurs titres et des commentaires internes. Les statuts et TODO ne doivent jamais apparaître dans le PDF.

## État de référence et synchronisation

La branche `rapport-validation` provient de `main` (`cb721ab`). Son `PROJECT_TRUTH.md` local décrit encore l’état antérieur à la conception technique. Pour la correction de la section 1.5, la revue humaine a explicitement autorisé la consultation de `origin/conception-technique-draft`, au commit `b77d3c5a4a1991ca7955c83c4785c471ec376543`, sans fusion ni copie de ses fichiers techniques.

Cet état courant indique une conception technique en cours. Le périmètre et les UML métier sont validés ; les fiches UC restent DRAFT. ADR-002 à ADR-005, le contrat API REST, la conception sécurité et le diagramme de composants sont validés. ADR-001, les conventions techniques et le diagramme de classes backend restent DRAFT. Backend et frontend ne sont pas commencés ; base de données, sécurité et module IA ne sont pas implémentés ; les tests d’implémentation ne sont pas commencés. La frontière IA est définie, mais le fournisseur et le modèle restent à choisir.

La référence interbranche et son commit sont consignés dans la section 1.5. Les commentaires des autres chapitres restent à réexaminer avant leur rédaction ; ils ne sont pas automatiquement actualisés par cette correction. Recontrôler la version et les statuts des sources lors de chaque nouvelle phase.

Documents consultés : `PROJECT_TRUTH.md`, README principal, `docs/requirements/functional-scope.md`, `docs/requirements/use-case-specifications.md`, `docs/uml/UML-CONVENTIONS.md`, les huit sources `.puml` présentes (Use Case, activité, classes, cinq séquences), ainsi que le guide EPI joint `Guide PFE.pdf` (absent du dépôt).

Incohérence repérée : le README principal affirme encore qu'aucun UML n'est généré, alors que les sources existent et sont marquées VALIDATED dans les diagrammes et la référence projet. Aucun contenu narratif n'est rédigé pour trancher ce conflit ; correction du README à traiter séparément. La garde officielle est intégrée. Seule la correction de son année de 2025/2026 vers 2026/2027 reste en attente pour cette couverture ; le TODO interne est conservé afin de ne pas altérer le modèle. Les champs personnels et professionnels restent à renseigner après confirmation.

Toute modification décrivant une fonctionnalité implémentée doit être vérifiée contre le code réel. Toute section décrivant des résultats doit être vérifiée contre les tests ou observations correspondants. Recontrôler les statuts à chaque phase et actualiser les commentaires concernés. La revue du rapport ne valide pas une implémentation. Maintenir DRAFT jusqu'à décision humaine ; aucun merge vers main dans cette mission.
