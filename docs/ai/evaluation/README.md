# Benchmark comparatif V1 exploratoire

Python 3.10 ou supérieur ; bibliothèque standard uniquement, sans installation pip.
Le script lit exclusivement le dataset voisin `dataset-v1.csv`. Il extrait le prompt,
le JSON Schema, leurs enums et les en-têtes CSV de `../AI-DESIGN-V1.md`.
Aucune règle de classification n’est recodée dans le script.

Depuis la racine du dépôt, validations sans clé et sans réseau :

```powershell
python -B docs/ai/evaluation/benchmark.py validate
python -B -m unittest discover -s docs/ai/evaluation -p "test_benchmark.py" -v
```

Les tests utilisent uniquement des fixtures en mémoire, jamais un fichier de résultats.
`validate` ne crée aucun fichier et ne charge aucune clé. Aucun appel n’est effectué
à l’import, par les tests, ou par `score`.

## Exécution ultérieure réelle

`.env.example` indique uniquement les deux noms de variables nécessaires.
Le script lit `OPENAI_API_KEY` et `MISTRAL_API_KEY` dans l’environnement du processus ;
il ne charge pas automatiquement un `.env`. Ne committer aucune clé.

Le timeout n’est pas une convention métier : l’opérateur doit choisir sa valeur
positive en secondes. Après configuration externe des variables et définition
PowerShell de `$timeoutSeconds`, la commande réelle est :

```powershell
python -B docs/ai/evaluation/benchmark.py run --timeout $timeoutSeconds
```

`--retry-transient` est facultatif : au plus **un** retry après timeout,
HTTP 429, HTTP 500/502/503/504 ou connexion interrompue/reset. Aucun retry de sortie
métier invalide, de refus, de JSON tronqué, ou de prédiction valide incorrecte.
Sans cette option, aucun retry. Un timeout peut avoir été traité côté fournisseur :
la nouvelle tentative éventuelle est enregistrée comme telle, sans garantie d’unicité
ni de déterminisme du fournisseur.

OpenAI : `gpt-5.6-luna`, Responses, `reasoning.effort=none`, température omise.
Mistral : `mistral-small-2603`, Chat Completions, `temperature=0`.
Même prompt rendu en une seule substitution et même schéma strict, sans outils,
recherche, RAG, historique ou information métier supplémentaire.

Le run réel crée seulement alors :

- `benchmark-results.csv` : exactement les colonnes définies par la conception ;
- `benchmark-attempts.jsonl` : chaque tentative réelle, sa latence, son erreur et la
  réponse brute fournisseur, permettant d’auditer les retries sans correction silencieuse ;
- `benchmark-run.json` : date UTC, empreintes SHA-256 du dataset, du document, du
  prompt et du script, version Python et paramètres effectivement envoyés.

La latence CSV couvre le traitement du cas, retries éventuels inclus. Les tokens
CSV sont ceux de la dernière tentative et restent vides si indisponibles ; les
réponses des tentatives antérieures sont conservées dans le journal. Le modèle
CSV est celui retourné par le fournisseur, sinon le modèle demandé. Les coûts
restent vides : aucun tarif ni coût estimé n’est codé en dur.

Les fichiers existants ne sont jamais écrasés : archiver les trois artefacts avant
un nouveau run explicite. Une interruption conserve le CSV partiel. HTTP
400/401/403/404/422 interrompt le run après enregistrement de l’échec : les résultats
partiels ne sont pas scorés et aucun redémarrage automatique n’est prévu.

## Calcul hors ligne après un run réel complet

```powershell
python -B docs/ai/evaluation/benchmark.py score
```

Le manifeste d’un run terminé est requis et les empreintes du dataset, du document
et du CSV doivent correspondre. 48 lignes uniques sont requises : P01–P24 pour chacun des deux fournisseurs, avec
références et version conformes. Les métriques sont affichées en JSON sur la sortie
standard, séparément pour chaque fournisseur/modèle retourné. Les modèles enregistrés sont explicitement affichés. Le modèle demandé utilisé comme
repli lors d’un échec peut coexister avec un unique snapshot retourné. Plusieurs
snapshots retournés différents sont refusés plutôt qu’agrégés silencieusement.

Accuracy et exact match ont toujours le dénominateur 24. Les invalides produisent
un faux négatif de leur classe réelle dans le Macro-F1, sans classe métier ajoutée.
Les matrices ont les classes réelles en lignes, les classes prédites dans l’ordre du
schéma en colonnes et une colonne technique `__INVALID__`. Aucun invalide n’est retiré.
Le taux de sous-priorisation est le nombre de cas valides sous-priorisés / 24 ; le
nombre de priorités valides examinées est aussi affiché, les invalides restant
rapportés séparément par type et sans priorité artificielle. Aucun Kappa ni seuil ajouté.

Ce benchmark de 24 cas synthétiques reste exploratoire et ne prouve pas une
performance générale ou représentative de la production Safyron.

Formats fournisseur vérifiés dans les documentations officielles :
[OpenAI Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs),
[GPT-5.6 Luna](https://developers.openai.com/api/docs/models/gpt-5.6-luna),
[Mistral Chat](https://docs.mistral.ai/api/endpoint/chat),
[Mistral Custom Structured Outputs](https://docs.mistral.ai/studio/conversations/structured-output/custom).
La compatibilité effective avec les comptes/API n’a pas été testée : aucun appel réel
n’a été exécuté lors de la préparation. Le support OpenAI de température reste non
confirmé et ce paramètre est donc omis.
