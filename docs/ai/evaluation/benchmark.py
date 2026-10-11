"""Benchmark V1 autonome. Aucun appel hors de la commande explicite `run`."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import os
from pathlib import Path
import re
import socket
import sys
import time
from collections import Counter
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.request import HTTPRedirectHandler, Request, build_opener

BASE = Path(__file__).resolve().parent
DESIGN = BASE.parent / "AI-DESIGN-V1.md"
DATASET = BASE / "dataset-v1.csv"
RESULTS = BASE / "benchmark-results.csv"
ATTEMPTS = BASE / "benchmark-attempts.jsonl"
MANIFEST = BASE / "benchmark-run.json"
PROVIDERS = {
    "OPENAI": ("gpt-5.6-luna", "https://api.openai.com/v1/responses", "OPENAI_API_KEY"),
    "MISTRAL": ("mistral-small-2603", "https://api.mistral.ai/v1/chat/completions", "MISTRAL_API_KEY"),
}
ERRORS = {"NONE", "TIMEOUT", "HTTP_429", "HTTP_5XX", "INVALID_JSON",
          "INVALID_SCHEMA", "INVALID_ENUM", "EMPTY_RESPONSE", "OTHER"}
EXPECTED_COUNT = 24
INVALID = "__INVALID__"  # Representation technique, jamais une classe metier.


class ValidationError(ValueError):
    pass


class DuplicateKey(ValueError):
    pass


def strict_json(text):
    def pairs(items):
        result = {}
        for key, value in items:
            if key in result:
                raise DuplicateKey("Propriete JSON dupliquee")
            result[key] = value
        return result

    def constant(_):
        raise ValueError("Constante non JSON")

    return json.loads(text, object_pairs_hook=pairs, parse_constant=constant)


def section(text, number):
    match = re.search(rf"^## {number}\. .*?\n(.*?)(?=^## \d+\.|\Z)", text, re.M | re.S)
    if not match:
        raise ValidationError(f"Section {number} absente du document")
    return match.group(1)


def fenced(text, language):
    matches = re.findall(rf"^```{language}\n(.*?)^```", text, re.M | re.S)
    if len(matches) != 1:
        raise ValidationError("Un seul bloc source attendu")
    return matches[0]


def load_spec():
    text = DESIGN.read_text(encoding="utf-8")
    prompt_section = section(text, 7)
    version = re.search(r"\*\*Nom :\*\* `([^`]+)`", prompt_section)
    if not version or version.group(1) != "AI-QUALIFICATION-PROMPT-v1.0.0":
        raise ValidationError("Version du prompt inattendue")
    prompt = fenced(prompt_section, "text")
    if sorted(re.findall(r"\{\{(.*?)\}\}", prompt)) != ["description", "titre"]:
        raise ValidationError("Placeholders du prompt inattendus")
    schema = strict_json(fenced(section(text, 8), "json"))
    fields = {"categorieProposee", "prioriteProposee"}
    if (schema.get("type") != "object" or schema.get("additionalProperties") is not False
            or set(schema.get("required", [])) != fields
            or set(schema.get("properties", {})) != fields):
        raise ValidationError("Schema metier inattendu")
    enums = {}
    for field, size in (("categorieProposee", 7), ("prioriteProposee", 4)):
        prop = schema["properties"][field]
        values = prop.get("enum", [])
        if (prop.get("type") != "string" or len(values) != size
                or any(not isinstance(v, str) for v in values) or len(set(values)) != size):
            raise ValidationError("Enums du schema invalides")
        enums[field] = values
    dataset_columns = fenced(section(text, 12), "csv").strip().split(",")
    result_blocks = re.findall(r"^```text\n([^\n]*caseId,provider,[^\n]*)\n```",
                               section(text, 16), re.M)
    if len(result_blocks) != 1:
        raise ValidationError("Structure des resultats absente ou ambigue")
    return {"prompt": prompt, "version": version.group(1), "schema": schema,
            "categories": enums["categorieProposee"], "priorities": enums["prioriteProposee"],
            "dataset_columns": dataset_columns, "result_columns": result_blocks[0].split(",")}


def validate_dataset(rows, spec):
    if len(rows) != EXPECTED_COUNT:
        raise ValidationError("Le dataset doit contenir exactement 24 cas")
    expected = {f"P{i:02d}" for i in range(1, EXPECTED_COUNT + 1)}
    ids = [r.get("id") for r in rows]
    if len(set(ids)) != EXPECTED_COUNT or set(ids) != expected:
        raise ValidationError("IDs P01 a P24 uniques requis")
    for row in rows:
        if set(row) != set(spec["dataset_columns"]) or any(v is None for v in row.values()):
            raise ValidationError("Colonnes ou valeurs CSV manquantes/supplementaires")
        if row["statutValidation"] != "VALIDE":
            raise ValidationError(f"{row['id']} non valide")
        if row["sourceType"] != "IA_ASSISTE_RELU_HUMAINEMENT":
            raise ValidationError(f"{row['id']} : provenance inattendue")
        if (row["categorieReference"] not in spec["categories"]
                or row["prioriteReference"] not in spec["priorities"]
                or row["difficulte"] not in {"SIMPLE", "AMBIGU", "DIFFICILE"}):
            raise ValidationError(f"{row['id']} : enum inconnu")
        if any(not row[f].strip() for f in ("titre", "description", "justificationReference")):
            raise ValidationError(f"{row['id']} : texte vide")
    return sorted(rows, key=lambda r: r["id"])


def load_dataset(spec):
    # Aucun chemin de dataset alternatif, aucune entree provenant des labels du Markdown.
    with DATASET.open(encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file, strict=True)
        if reader.fieldnames != spec["dataset_columns"]:
            raise ValidationError("En-tete dataset different du document")
        return validate_dataset(list(reader), spec)


def render_prompt(case, spec):
    # Substitution en une passe : aucun placeholder contenu dans une donnee n'est interprete.
    return re.sub(r"\{\{(titre|description)\}\}", lambda m: case[m.group(1)], spec["prompt"])


def payload(provider, case, spec):
    prompt = render_prompt(case, spec)
    model = PROVIDERS[provider][0]
    output_format = {"type": "json_schema", "name": "qualification_v1",
                     "strict": True, "schema": spec["schema"]}
    if provider == "OPENAI":
        return {"model": model, "input": [{"role": "user", "content": prompt}],
                "reasoning": {"effort": "none"}, "text": {"format": output_format},
                "tools": [], "store": False}
    return {"model": model, "messages": [{"role": "user", "content": prompt}],
            "temperature": 0, "n": 1, "stream": False,
            "response_format": {"type": "json_schema", "json_schema": {
                "name": "qualification_v1", "strict": True, "schema": spec["schema"]}}}


def validate_business(text, spec):
    if text is None or (isinstance(text, str) and not text.strip()):
        return None, "EMPTY_RESPONSE"
    if not isinstance(text, str):
        return None, "INVALID_SCHEMA"
    try:
        result = strict_json(text)
    except DuplicateKey:
        return None, "INVALID_SCHEMA"
    except (ValueError, TypeError):
        return None, "INVALID_JSON"
    if not isinstance(result, dict) or set(result) != set(spec["schema"]["properties"]):
        return None, "INVALID_SCHEMA"
    if any(not isinstance(v, str) for v in result.values()):
        return None, "INVALID_SCHEMA"
    if (result["categorieProposee"] not in spec["categories"]
            or result["prioriteProposee"] not in spec["priorities"]):
        return None, "INVALID_ENUM"
    return result, "NONE"


def extract_business(provider, response):
    """Refus, troncature, appels d'outils et multiples reponses ne sont jamais repares."""
    try:
        if provider == "OPENAI":
            if response.get("status") != "completed":
                return None, "INVALID_SCHEMA"
            output = response.get("output", [])
            if any(item.get("type") not in {"message", "reasoning"} for item in output):
                return None, "INVALID_SCHEMA"
            messages = [x for x in output if x.get("type") == "message"]
            if not messages:
                return None, "EMPTY_RESPONSE"
            if len(messages) != 1 or messages[0].get("role") != "assistant":
                return None, "INVALID_SCHEMA"
            parts = messages[0].get("content", [])
            if not parts:
                return None, "EMPTY_RESPONSE"
            if len(parts) != 1 or parts[0].get("type") != "output_text":
                return None, "INVALID_SCHEMA"
            return parts[0].get("text"), "NONE"
        choices = response.get("choices", [])
        if not choices:
            return None, "EMPTY_RESPONSE"
        if len(choices) != 1 or choices[0].get("finish_reason") != "stop":
            return None, "INVALID_SCHEMA"
        message = choices[0]["message"]
        if message.get("role") != "assistant" or message.get("tool_calls"):
            return None, "INVALID_SCHEMA"
        return message.get("content"), "NONE"
    except (TypeError, KeyError, AttributeError):
        return None, "INVALID_SCHEMA"


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def post_json(url, key, body, timeout):
    request = Request(url, data=json.dumps(body, ensure_ascii=False, allow_nan=False).encode("utf-8"),
                      headers={"Authorization": "Bearer " + key, "Content-Type": "application/json"},
                      method="POST")
    # Pas de SDK : aucun retry cache et aucun transfert d'Authorization sur redirection.
    with build_opener(NoRedirect()).open(request, timeout=timeout) as response:
        return response.read().decode("utf-8")


def technical_error(exc):
    if isinstance(exc, HTTPError):
        if exc.code == 429:
            return "HTTP_429", True, exc.code
        if 500 <= exc.code <= 599:
            return "HTTP_5XX", exc.code in {500, 502, 503, 504}, exc.code
        return "OTHER", False, exc.code
    reason = exc.reason if isinstance(exc, URLError) else exc
    if isinstance(reason, (TimeoutError, socket.timeout)):
        return "TIMEOUT", True, None
    if isinstance(reason, (ConnectionResetError, ConnectionAbortedError)):
        return "OTHER", True, None
    return "OTHER", False, None


def token_usage(provider, response):
    usage = response.get("usage") or {}
    if not isinstance(usage, dict):
        return [None, None, None]
    keys = ("input_tokens", "output_tokens", "total_tokens") if provider == "OPENAI" else (
        "prompt_tokens", "completion_tokens", "total_tokens")
    return [value if type(value := usage.get(key)) is int and value >= 0 else None for key in keys]


def utc_now():
    return datetime.now(timezone.utc).isoformat()


def perform_case(provider, case, spec, key, timeout, retry_transient, log):
    started = time.perf_counter()
    body = payload(provider, case, spec)
    returned_model = PROVIDERS[provider][0]
    tokens = [None, None, None]
    result, error, fatal = None, "OTHER", False
    for attempt in range(1, 2 + int(retry_transient)):
        attempt_start = time.perf_counter()
        record = {"caseId": case["id"], "provider": provider,
                  "requestedModel": PROVIDERS[provider][0], "attempt": attempt,
                  "startedAtUtc": utc_now()}
        transient = False
        try:
            raw = post_json(PROVIDERS[provider][1], key, body, timeout)
            record["rawResponse"] = raw  # Sortie reelle seulement ; jamais de headers ni de cle.
            try:
                response = strict_json(raw)
            except (ValueError, TypeError):
                response = None
                error = "EMPTY_RESPONSE" if not raw.strip() else "INVALID_JSON"
            if response is not None:
                if not isinstance(response, dict):
                    error = "INVALID_SCHEMA"
                else:
                    if isinstance(response.get("model"), str) and response["model"]:
                        returned_model = response["model"]
                    tokens = token_usage(provider, response)
                    content, error = extract_business(provider, response)
                    if error == "NONE":
                        result, error = validate_business(content, spec)
        except (HTTPError, URLError, TimeoutError, OSError) as exc:
            error, transient, status = technical_error(exc)
            record["httpStatus"] = status
            fatal = status in {400, 401, 403, 404, 422}
            if isinstance(exc, HTTPError):
                exc.close()
        except UnicodeError:
            error = "INVALID_JSON"
        record.update({"typeErreur": error, "latenceMs": round(
            (time.perf_counter() - attempt_start) * 1000, 3),
            "retryNext": transient and retry_transient and attempt == 1})
        log.write(json.dumps(record, ensure_ascii=False, allow_nan=False) + "\n")
        log.flush()
        if not record["retryNext"]:
            break
    row = {"caseId": case["id"], "provider": provider, "model": returned_model,
           "promptVersion": spec["version"], "categorieReference": case["categorieReference"],
           "prioriteReference": case["prioriteReference"],
           "categoriePredite": result["categorieProposee"] if result else "",
           "prioritePredite": result["prioriteProposee"] if result else "",
           "sortieValide": "true" if result else "false", "typeErreur": error,
           "latenceMs": round((time.perf_counter() - started) * 1000, 3),
           "inputTokens": tokens[0], "outputTokens": tokens[1], "totalTokens": tokens[2],
           "coutObserveUsd": None}
    return row, fatal


def metric_axis(rows, reference, prediction, labels):
    matrix = [[0] * (len(labels) + 1) for _ in labels]
    f1_scores = []
    correct = 0
    for row in rows:
        observed = row[prediction] if row["sortieValide"] == "true" else INVALID
        matrix[labels.index(row[reference])][labels.index(observed) if observed in labels else len(labels)] += 1
        correct += observed == row[reference]
    for index in range(len(labels)):
        tp = matrix[index][index]
        fp = sum(matrix[i][index] for i in range(len(labels)) if i != index)
        fn = sum(matrix[index]) - tp  # Inclut explicitement la colonne invalide.
        denominator = 2 * tp + fp + fn
        f1_scores.append(2 * tp / denominator if denominator else 0.0)
    return {"correct": correct, "denominator": EXPECTED_COUNT,
            "accuracy": correct / EXPECTED_COUNT, "macroF1": sum(f1_scores) / len(labels),
            "confusionMatrix": {"rows": labels, "columns": labels + [INVALID], "counts": matrix}}


def validate_results(rows, cases, spec):
    if len(rows) != 2 * EXPECTED_COUNT:
        raise ValidationError("48 resultats requis ; un fichier partiel n'est pas score")
    expected = {(case["id"], provider) for case in cases for provider in PROVIDERS}
    pairs = [(row.get("caseId"), row.get("provider")) for row in rows]
    if len(set(pairs)) != len(pairs) or set(pairs) != expected:
        raise ValidationError("Chaque couple cas/fournisseur doit apparaitre une seule fois")
    by_id = {case["id"]: case for case in cases}
    for row in rows:
        if set(row) != set(spec["result_columns"]) or any(v is None for v in row.values()):
            raise ValidationError("Structure CSV resultats invalide")
        case = by_id[row["caseId"]]
        if (row["promptVersion"] != spec["version"] or not row["model"]
                or row["categorieReference"] != case["categorieReference"]
                or row["prioriteReference"] != case["prioriteReference"]):
            raise ValidationError("Version, modele ou reference du resultat incoherent")
        if row["sortieValide"] not in {"true", "false"} or row["typeErreur"] not in ERRORS:
            raise ValidationError("Statut resultat invalide")
        if row["sortieValide"] == "true":
            _, error = validate_business(json.dumps({"categorieProposee": row["categoriePredite"],
                "prioriteProposee": row["prioritePredite"]}), spec)
            if error != "NONE" or row["typeErreur"] != "NONE":
                raise ValidationError("Prediction marquee valide mais non conforme")
        elif row["typeErreur"] == "NONE" or row["categoriePredite"] or row["prioritePredite"]:
            raise ValidationError("Sortie invalide incoherente : aucune priorite inventee")
        for name in ("latenceMs", "coutObserveUsd"):
            if row[name] != "":
                try:
                    value = float(row[name])
                except ValueError as exc:
                    raise ValidationError("Valeur numerique invalide") from exc
                if not math.isfinite(value) or value < 0:
                    raise ValidationError("Valeur numerique non finie ou negative")
        if not row["latenceMs"]:
            raise ValidationError("Latence absente")
        for name in ("inputTokens", "outputTokens", "totalTokens"):
            if row[name] and not row[name].isdigit():
                raise ValidationError("Nombre de tokens invalide")
    for provider in PROVIDERS:
        requested = PROVIDERS[provider][0]
        if len({r["model"] for r in rows if r["provider"] == provider and r["model"] != requested}) > 1:
            raise ValidationError("Modeles retournes differents : ne pas agreger silencieusement")


def calculate_metrics(rows, spec):
    reports = []
    for provider in PROVIDERS:
        group = [row for row in rows if row["provider"] == provider]
        if len(group) != EXPECTED_COUNT:
            raise ValidationError("24 observations par modele requises")
        valid = [row for row in group if row["sortieValide"] == "true"]
        joint = sum(row["categoriePredite"] == row["categorieReference"]
                    and row["prioritePredite"] == row["prioriteReference"] for row in valid)
        under = sum(spec["priorities"].index(row["prioritePredite"]) <
                    spec["priorities"].index(row["prioriteReference"]) for row in valid)
        observed_models = sorted({row["model"] for row in group})
        effective = [model for model in observed_models if model != PROVIDERS[provider][0]]
        reports.append({"provider": provider, "model": effective[0] if effective else PROVIDERS[provider][0],
            "requestedModel": PROVIDERS[provider][0], "modelsRecorded": observed_models,
            "categorie": metric_axis(group, "categorieReference", "categoriePredite", spec["categories"]),
            "priorite": metric_axis(group, "prioriteReference", "prioritePredite", spec["priorities"]),
            "exactMatch": {"correct": joint, "denominator": EXPECTED_COUNT, "rate": joint / EXPECTED_COUNT},
            "sousPriorisation": {"count": under, "rate": under / EXPECTED_COUNT,
                                  "denominator": EXPECTED_COUNT, "validPrioritiesExamined": len(valid)},
            "sortiesInvalides": dict(Counter(row["typeErreur"] for row in group if row["sortieValide"] == "false"))})
    return reports


def score(spec, cases):
    manifest = strict_json(MANIFEST.read_text(encoding="utf-8"))
    if (not isinstance(manifest, dict) or manifest.get("status") != "completed"
            or manifest.get("datasetSha256") != sha256(DATASET)
            or manifest.get("designSha256") != sha256(DESIGN)
            or manifest.get("resultsSha256") != sha256(RESULTS)):
        raise ValidationError("Run incomplet ou empreintes dataset/conception/resultats differentes")
    with RESULTS.open(encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file, strict=True)
        if reader.fieldnames != spec["result_columns"]:
            raise ValidationError("En-tete resultats different du document")
        rows = list(reader)
    validate_results(rows, cases, spec)
    return calculate_metrics(rows, spec)


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(spec, cases, timeout, retry_transient):
    keys = {provider: os.environ.get(values[2], "").strip() for provider, values in PROVIDERS.items()}
    if not all(keys.values()):
        raise ValidationError("Variables OPENAI_API_KEY et MISTRAL_API_KEY requises pour run")
    for path in (RESULTS, ATTEMPTS, MANIFEST):
        if path.exists():
            raise ValidationError(f"Execution refusee : {path.name} existe deja. Archiver le run precedent.")
    manifest = {"startedAtUtc": utc_now(), "status": "running", "python": sys.version,
        "datasetSha256": sha256(DATASET), "designSha256": sha256(DESIGN),
        "scriptSha256": sha256(Path(__file__)), "promptVersion": spec["version"],
        "promptSha256": hashlib.sha256(spec["prompt"].encode()).hexdigest(),
        "timeoutSeconds": timeout, "maxTechnicalRetries": int(retry_transient),
        "providers": {provider: {"model": values[0], "endpoint": values[1]} for provider, values in PROVIDERS.items()},
        "openaiReasoningEffort": "none", "openaiTemperature": "omitted", "mistralTemperature": 0,
        "tokenAccounting": "final attempt only; earlier attempts preserved in JSONL", "costAccounting": "unknown, empty CSV cells"}
    with MANIFEST.open("x", encoding="utf-8") as file:
        json.dump(manifest, file, ensure_ascii=False, indent=2)
    try:
        with RESULTS.open("x", encoding="utf-8", newline="") as file, ATTEMPTS.open("x", encoding="utf-8") as log:
            writer = csv.DictWriter(file, fieldnames=spec["result_columns"])
            writer.writeheader()
            for provider in PROVIDERS:
                for case in cases:
                    row, fatal = perform_case(provider, case, spec, keys[provider], timeout, retry_transient, log)
                    writer.writerow(row)
                    file.flush()
                    print(f"{provider} {case['id']} : {row['typeErreur']}", file=sys.stderr)
                    if fatal:
                        raise ValidationError("Erreur API de configuration/authentification : execution arretee, fichier partiel conserve")
        manifest["status"] = "completed"
        manifest["resultsSha256"] = sha256(RESULTS)
    finally:
        manifest["finishedAtUtc"] = utc_now()
        if manifest["status"] == "running":
            manifest["status"] = "incomplete"
        MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    return score(spec, cases)


def positive_seconds(value):
    try:
        number = float(value)
    except ValueError as exc:
        raise argparse.ArgumentTypeError("Timeout numerique requis") from exc
    if not math.isfinite(number) or number <= 0:
        raise argparse.ArgumentTypeError("Timeout strictement positif et fini requis")
    return number


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("validate", help="Validation locale uniquement, aucun fichier de resultat cree")
    commands.add_parser("score", help="Calcul hors ligne des resultats reels existants")
    live = commands.add_parser("run", help="Execute reellement les 48 appels fournisseur")
    live.add_argument("--timeout", type=positive_seconds, required=True, help="Timeout HTTP par tentative, en secondes (choix operateur)")
    live.add_argument("--retry-transient", action="store_true", help="Au plus une nouvelle tentative technique, tracee ; desactive par defaut")
    args = parser.parse_args(argv)
    try:
        spec = load_spec()
        cases = load_dataset(spec)
        if args.command == "validate":
            print(json.dumps({"status": "VALID", "cases": len(cases), "promptVersion": spec["version"],
                "datasetSha256": sha256(DATASET), "designSha256": sha256(DESIGN),
                "categories": dict(Counter(c["categorieReference"] for c in cases)),
                "priorities": dict(Counter(c["prioriteReference"] for c in cases))}, ensure_ascii=False, indent=2))
            return 0
        report = score(spec, cases) if args.command == "score" else run(spec, cases, args.timeout, args.retry_transient)
        print(json.dumps(report, ensure_ascii=False, indent=2, allow_nan=False))
        return 0
    except (ValidationError, OSError, ValueError, csv.Error) as exc:
        print(f"Erreur : {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
