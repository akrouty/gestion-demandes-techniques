# Benchmark comparatif V1 exploratoire

Python 3.10 ou supérieur ; SDK Python officiel `groq` uniquement pour le transport Groq.
Gemini conserve son transport `urllib`. Installation depuis la racine du dépôt :

```powershell
python -m pip install -r docs/ai/evaluation/requirements.txt
```

Le diagnostic réel rapporté par l’utilisateur est un HTTP 403 Cloudflare `error code: 1010`
avec `urllib`, alors qu’un appel Groq valide avec un autre client répond HTTP 200.
La correction porte uniquement sur le client HTTP, pas sur le modèle ni sur la clé.
Le SDK appelle `chat.completions.create` via son wrapper officiel `with_raw_response`
pour conserver le texte brut et les en-têtes rate-limit sans modifier la validation métier.
Ses retries automatiques sont désactivés (`max_retries=0`) : seul le retry optionnel
existant du benchmark s’applique. Le timeout de l’opérateur est transmis au SDK.
Les exceptions SDK sont traduites vers les catégories existantes ; aucun résultat
métier invalide n’est réparé ou rejoué. `validate` et `score` n’importent pas le SDK.
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
Le script lit `GEMINI_API_KEY` et `GROQ_API_KEY` dans l’environnement du processus ;
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

Gemini : `gemini-3.8-flash`, Interactions API stable v1, `thinking_level=low`,
`thinking_summaries=none`, `store=false`, sans temperature/top_p/top_k.
Groq : `qwen/qwen3.8-27b`, Chat Completions, `reasoning_effort=none`,
Structured Outputs avec `strict=true`, une seule choice et `stream=false`.
Même prompt et schéma métier, sans outils, recherche, RAG ou historique.

La contrainte du stage est coût API = 0 sans carte bancaire. Gemini Free Tier et
Groq Free Plan permettent le benchmark prévu sans paiement tant que les comptes
restent sur ces offres, dans leurs quotas et conditions d’éligibilité. Les conditions
peuvent évoluer : les revérifier avant une exécution réelle. Le script ne peut pas
certifier le plan de facturation d’un compte et ne code aucun coût financier.

OpenAI/Mistral sont écartés pour la contrainte de facturation rapportée dans ce stage,
sans jugement sur leur qualité. Les anciens artefacts restent conservés.

Le pacing Groq respecte les headers Retry-After, remaining/reset et la limite TPM
retournée, avec étalement basé sur les tokens réels. Aucun quota futur n’est codé en dur.
Les headers requests décrivent les quotas par jour. Le délai volontaire est tracé
séparément en `pacingSeconds`, exclu de `latenceMs`. Aucun pacing ne garantit zéro 429
si le compte est utilisé ailleurs. Les retries restent désactivés par défaut et limités
à une tentative technique supplémentaire, jamais pour une sortie métier.

Le run réel crée seulement alors :

- `benchmark-results.csv` : exactement les colonnes définies par la conception ;
- `benchmark-attempts.jsonl` : chaque tentative réelle, sa latence, son erreur et la
  réponse brute fournisseur, permettant d’auditer les retries sans correction silencieuse ;
- `benchmark-run.json` : date UTC, empreintes SHA-256 du dataset, du document, du
  prompt et du script, version Python et paramètres effectivement envoyés.

La latence CSV additionne uniquement les durées HTTP des tentatives du cas ; le pacing volontaire est exclu. Les tokens
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

Formats et conditions vérifiés le 2026-10-11 dans les documentations officielles :
[Gemini Interactions v1](https://ai.google.dev/api/interactions-api-v1),
[Gemini 3.8 Flash](https://ai.google.dev/gemini-api/docs/latest-model),
[Gemini pricing](https://ai.google.dev/gemini-api/docs/pricing),
[Groq Structured Outputs](https://console.groq.com/docs/structured-outputs),
[Groq rate limits](https://console.groq.com/docs/rate-limits).
La compatibilité effective des comptes/API n’est pas testée : aucun appel réel ici.
La référence Interactions v1 ne liste pas encore gemini-3.8-flash dans son enum model,
bien que le modèle soit documenté ailleurs : compatibilité effective v1 à confirmer.
Aucun fallback v1beta ni réparation automatique n’est introduit.

Les résultats/journaux/manifeste historiques actuellement présents ne sont pas
modifiés. `run` refusera de les écraser ; archiver ces fichiers avant une future
exécution explicite. `score` contrôle le manifeste et les empreintes et refuse de
mélanger des résultats historiques avec la nouvelle conception/fournisseurs.
