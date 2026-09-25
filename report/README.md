# Squelette du rapport EPI

**DRAFT — EN ATTENTE DE VALIDATION HUMAINE**

Lire obligatoirement [RAPPORT-CONVENTIONS.md](../RAPPORT-CONVENTIONS.md) et `../PROJECT_TRUTH.md` avant toute modification. Ce dossier prépare les titres et mécanismes ; il ne constitue pas un rapport rédigé.

## Compilation

Prérequis : XeLaTeX, Biber et les packages déclarés dans `config/preamble.tex` (fontspec, babel français, geometry, setspace, fancyhdr, titlesec, graphicx, caption, csquotes, biblatex, hyperref). Aucune distribution ni police propriétaire n'est fournie. Times New Roman est sélectionnée lorsqu'elle est disponible ; sinon TeX Gyre Termes permet de compiler avec un avertissement explicite, sans prétendre utiliser la police EPI exacte.

Depuis `report/`, dans PowerShell :

```powershell
New-Item -ItemType Directory -Force .build
xelatex -interaction=nonstopmode -halt-on-error '-output-directory=.build' main.tex
biber --input-directory .build --output-directory .build main
xelatex -interaction=nonstopmode -halt-on-error '-output-directory=.build' main.tex
xelatex -interaction=nonstopmode -halt-on-error '-output-directory=.build' main.tex
```

Le PDF est `.build/main.pdf`. Ne pas versionner les produits de compilation. Une bibliographie vide peut produire les avertissements « Empty bibliography » et « does not contain any citations » : ils sont attendus tant qu'aucune source n'a été citée. Ne jamais ajouter une fausse entrée pour les supprimer. Refaire la compilation jusqu'à stabilisation du sommaire et des références croisées.

## Organisation

- `main.tex` : orchestration uniquement.
- `config/` : packages, mise en page, métadonnées vides à compléter avec des informations vérifiées.
- `frontmatter/` : pages préliminaires ; garde minimale à remplacer par le modèle officiel EPI avant finalisation.
- `chapters/` : introduction, quatre chapitres, conclusion ; statuts et preuves dans les commentaires internes.
- `appendices/` : mécanisme des annexes, sans contenu artificiel.
- `bibliography/references.bib` : sources réellement exploitées ; classement par auteur avec BibLaTeX/Biber.
- `figures/uml/` : rendus seulement ; les sources restent dans `../docs/uml/`.
- `figures/architecture/`, `screenshots/`, `results/` : vides jusqu'à disponibilité de contenus réels.

Les chapitres commencent par une page séparatrice. Le sommaire et la table détaillée sont générés automatiquement à partir des mêmes titres. Les listes des figures et tableaux sont actuellement vides. Les numéros visibles commencent à l'introduction. Les pages contenant seulement des titres sont intentionnelles dans ce squelette. Les statuts et TODO ne doivent jamais apparaître dans le PDF.

## État de référence et synchronisation

Base : `main`, commit `cb721ab`, branche de travail `rapport-validation`. `PROJECT_TRUTH.md` décrit une analyse fonctionnelle terminée et une transition vers la conception technique. Le périmètre V1 est gelé et validé ; Use Case, activité, classes et cinq séquences portent VALIDATED. Les fiches UC portent DRAFT. `docs/adr/`, `backend/`, `frontend/`, `docs/testing/` ne contiennent que `.gitkeep`. Aucune implémentation, architecture IA ou résultat de test applicatif n'est ainsi démontré. Les travaux de `conception-technique-draft` ne sont pas intégrés à cette base.

Documents consultés : `PROJECT_TRUTH.md`, README principal, `docs/requirements/functional-scope.md`, `docs/requirements/use-case-specifications.md`, `docs/uml/UML-CONVENTIONS.md`, les huit sources `.puml` présentes (Use Case, activité, classes, cinq séquences), ainsi que le guide EPI joint `Guide PFE.pdf` (absent du dépôt).

Incohérence repérée : le README principal affirme encore qu'aucun UML n'est généré, alors que les sources existent et sont marquées VALIDATED dans les diagrammes et la référence projet. Aucun contenu narratif n'est rédigé pour trancher ce conflit ; correction du README à traiter séparément. Le guide EPI exige le modèle officiel de garde, encore à fournir et à vérifier.

Toute modification décrivant une fonctionnalité implémentée doit être vérifiée contre le code réel. Toute section décrivant des résultats doit être vérifiée contre les tests ou observations correspondants. Recontrôler les statuts à chaque phase et actualiser les commentaires concernés. La validation du squelette ne valide pas une implémentation. Maintenir DRAFT jusqu'à décision humaine ; aucun merge vers main dans cette mission.
