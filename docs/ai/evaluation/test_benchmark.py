"""Tests hors ligne : fixtures en memoire, aucune ligne de resultat sur disque."""
import copy
import io
import json
import socket
import unittest
from unittest.mock import patch
from urllib.error import HTTPError

import benchmark as b


class BenchmarkOfflineTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.spec = b.load_spec()
        cls.cases = b.load_dataset(cls.spec)

    def setUp(self):
        self.network = patch.object(socket, "create_connection", side_effect=AssertionError("Reseau interdit"))
        self.network.start()
        self.addCleanup(self.network.stop)

    def business(self, case=None):
        case = case or self.cases[0]
        return {"categorieProposee": case["categorieReference"], "prioriteProposee": case["prioriteReference"]}

    def envelope(self, provider, content=None):
        content = content if content is not None else json.dumps(self.business())
        if provider == "GEMINI":
            return {"model": "gemini-3.8-flash-snapshot", "status": "completed", "steps": [
                {"type": "model_output", "content": [{"type": "text", "text": content}]}],
                "usage": {"total_input_tokens": 10, "total_output_tokens": 4, "total_tokens": 14}}
        return {"model": "qwen/qwen3.8-27b", "choices": [{"finish_reason": "stop",
            "message": {"role": "assistant", "content": content}}],
            "usage": {"prompt_tokens": 10, "completion_tokens": 4, "total_tokens": 14}}

    def metric_rows(self):
        # Cas de test purement mathematiques en memoire : jamais des resultats du benchmark.
        return [{"caseId": c["id"], "provider": provider, "model": b.PROVIDERS[provider][0],
            "promptVersion": self.spec["version"], "categorieReference": c["categorieReference"],
            "prioriteReference": c["prioriteReference"], "categoriePredite": c["categorieReference"],
            "prioritePredite": c["prioriteReference"], "sortieValide": "true", "typeErreur": "NONE",
            "latenceMs": "1", "inputTokens": "", "outputTokens": "", "totalTokens": "", "coutObserveUsd": ""}
            for provider in b.PROVIDERS for c in self.cases]

    def test_local_validate_does_not_access_network_or_create_results(self):
        before = {path: path.exists() for path in (b.RESULTS, b.ATTEMPTS, b.MANIFEST)}
        with patch.object(b, "post_json", side_effect=AssertionError("API interdite")), patch("sys.stdout", new=io.StringIO()):
            self.assertEqual(b.main(["validate"]), 0)
        self.assertEqual(before, {path: path.exists() for path in before})

    def test_approved_dataset_and_p14(self):
        self.assertEqual(len(self.cases), 24)
        p14 = self.cases[13]
        self.assertEqual((p14["id"], p14["categorieReference"], p14["prioriteReference"], p14["difficulte"]),
                         ("P14", "DOSSIER_TECHNIQUE", "BASSE", "AMBIGU"))

    def test_dataset_rejects_missing_duplicate_unvalidated_and_unknown_values(self):
        modifications = [("id", "P02"), ("statutValidation", "DRAFT"),
            ("categorieReference", "INVENTE"), ("prioriteReference", "INVENTE"),
            ("difficulte", "INVENTE"), ("sourceType", "CONSTRUIT_MANUELLEMENT"), ("titre", " ")]
        for field, value in modifications:
            with self.subTest(field=field):
                rows = copy.deepcopy(self.cases)
                rows[0][field] = value
                with self.assertRaises(b.ValidationError):
                    b.validate_dataset(rows, self.spec)
        with self.assertRaises(b.ValidationError):
            b.validate_dataset(self.cases[:-1], self.spec)

    def test_prompt_one_pass_and_same_context_for_both_providers(self):
        case = dict(self.cases[0], titre="{{description}}", description="texte brut")
        rendered = b.render_prompt(case, self.spec)
        self.assertIn("Titre :\n{{description}}", rendered)
        self.assertIn("Description :\ntexte brut", rendered)
        a, m = (b.payload(provider, self.cases[0], self.spec) for provider in b.PROVIDERS)
        self.assertEqual(a["input"], m["messages"][0]["content"])
        self.assertEqual(a["response_format"]["schema"], self.spec["schema"])
        self.assertEqual(m["response_format"]["json_schema"]["schema"], self.spec["schema"])
        self.assertEqual(a["response_format"]["type"], "text")
        self.assertEqual(a["response_format"]["mime_type"], "application/json")
        self.assertFalse(a["store"])
        self.assertTrue(m["response_format"]["json_schema"]["strict"])
        self.assertEqual(a["generation_config"], {"thinking_level": "low", "thinking_summaries": "none"})
        self.assertNotIn("temperature", a)
        self.assertEqual(m["reasoning_effort"], "none")
        self.assertEqual(m["n"], 1)
        self.assertFalse(m["stream"])
        for name in ["temperature", "top_p", "top_k"]:
            self.assertNotIn(name, a)
        for request in (a, m):
            self.assertNotIn("previous_response_id", request)
            self.assertNotIn("conversation", request)
            self.assertFalse(request.get("tools"))

    def test_business_validation_rejects_without_repair(self):
        valid = json.dumps(self.business())
        examples = [(None, "EMPTY_RESPONSE"), (" ", "EMPTY_RESPONSE"),
            ("```json\n" + valid + "\n```", "INVALID_JSON"),
            ('{"categorieProposee": NaN}', "INVALID_JSON"),
            ('{"categorieProposee":"AUTRE","categorieProposee":"AUTRE","prioriteProposee":"BASSE"}', "INVALID_SCHEMA"),
            ("[]", "INVALID_SCHEMA"), ("{}", "INVALID_SCHEMA"),
            (json.dumps(dict(self.business(), extra="x")), "INVALID_SCHEMA"),
            (json.dumps(dict(self.business(), prioriteProposee=1)), "INVALID_SCHEMA"),
            (json.dumps(dict(self.business(), prioriteProposee="URGENT")), "INVALID_ENUM")]
        self.assertEqual(b.validate_business(valid, self.spec), (self.business(), "NONE"))
        for text, error in examples:
            with self.subTest(error=error, text=text):
                self.assertEqual(b.validate_business(text, self.spec), (None, error))

    def test_refusal_truncation_tools_multiple_outputs_are_rejected(self):
        a = self.envelope("GEMINI")
        a["status"] = "incomplete"
        self.assertEqual(b.extract_business("GEMINI", a)[1], "INVALID_SCHEMA")
        a = self.envelope("GEMINI")
        a["steps"][0]["content"][0] = {"type": "refusal", "refusal": "non"}
        self.assertEqual(b.extract_business("GEMINI", a)[1], "INVALID_SCHEMA")
        a = self.envelope("GEMINI")
        a["steps"].append({"type": "function_call"})
        self.assertEqual(b.extract_business("GEMINI", a)[1], "INVALID_SCHEMA")
        m = self.envelope("GROQ")
        m["choices"][0]["finish_reason"] = "length"
        self.assertEqual(b.extract_business("GROQ", m)[1], "INVALID_SCHEMA")
        m = self.envelope("GROQ")
        m["choices"] *= 2
        self.assertEqual(b.extract_business("GROQ", m)[1], "INVALID_SCHEMA")

    def test_response_model_usage_and_nullable_cost_are_recorded(self):
        for provider in b.PROVIDERS:
            log = io.StringIO()
            envelope = self.envelope(provider)
            with patch.object(b, "post_json", return_value=(json.dumps(envelope), {})) as post:
                row, fatal = b.perform_case(provider, self.cases[0], self.spec, "test-only", 1, True, log)
            self.assertEqual(post.call_count, 1)
            self.assertEqual(row["model"], envelope["model"])
            self.assertEqual((row["inputTokens"], row["outputTokens"], row["totalTokens"]), (10, 4, 14))
            self.assertIsNone(row["coutObserveUsd"])
            self.assertFalse(fatal)
            self.assertGreaterEqual(row["latenceMs"], 0)
            self.assertNotIn("test-only", log.getvalue())

    def test_at_most_one_transient_retry_traced(self):
        log = io.StringIO()
        error = HTTPError("https://example.invalid", 429, "rate", {}, None)
        with patch.object(b, "post_json", side_effect=[error, (json.dumps(self.envelope("GEMINI")), {})]) as post:
            row, _ = b.perform_case("GEMINI", self.cases[0], self.spec, "test-only", 1, True, log)
        events = [json.loads(line) for line in log.getvalue().splitlines()]
        self.assertEqual(post.call_count, 2)
        self.assertEqual([e["attempt"] for e in events], [1, 2])
        self.assertEqual(events[0]["typeErreur"], "HTTP_429")
        self.assertTrue(events[0]["retryNext"])
        self.assertFalse(events[1]["retryNext"])
        self.assertEqual(row["sortieValide"], "true")
        with patch.object(b, "post_json", side_effect=TimeoutError()) as post:
            row, _ = b.perform_case("GEMINI", self.cases[0], self.spec, "test-only", 1, True, io.StringIO())
        self.assertEqual(post.call_count, 2)
        self.assertEqual(row["typeErreur"], "TIMEOUT")

    def test_no_retry_by_default_or_for_http_auth(self):
        for retry, code, expected_fatal in [(False, 429, False), (True, 401, True), (True, 501, False)]:
            with self.subTest(code=code), patch.object(b, "post_json", side_effect=HTTPError("https://example.invalid", code, "err", {}, None)) as post:
                _, fatal = b.perform_case("GEMINI", self.cases[0], self.spec, "test-only", 1, retry, io.StringIO())
                self.assertEqual(post.call_count, 1)
                self.assertEqual(fatal, expected_fatal)

    def test_invalid_or_valid_incorrect_prediction_never_retried(self):
        incorrect = json.dumps({"categorieProposee": "AUTRE", "prioriteProposee": "BASSE"})
        for content in ["broken-json", incorrect]:
            with self.subTest(content=content), patch.object(b, "post_json", return_value=(json.dumps(self.envelope("GEMINI", content)), {})) as post:
                row, _ = b.perform_case("GEMINI", self.cases[0], self.spec, "test-only", 1, True, io.StringIO())
                self.assertEqual(post.call_count, 1)
                if content == "broken-json":
                    self.assertEqual(row["categoriePredite"], "")
                    self.assertEqual(row["prioritePredite"], "")

    def test_all_invalid_metric_input_keeps_24_and_business_only_macro(self):
        rows = self.metric_rows()
        for row in rows:
            row.update(sortieValide="false", typeErreur="EMPTY_RESPONSE", categoriePredite="", prioritePredite="")
        b.validate_results(rows, self.cases, self.spec)
        reports = b.calculate_metrics(rows, self.spec)
        for report in reports:
            self.assertEqual(report["categorie"]["accuracy"], 0)
            self.assertEqual(report["priorite"]["macroF1"], 0)
            self.assertEqual(report["exactMatch"]["denominator"], 24)
            self.assertEqual(report["sortiesInvalides"], {"EMPTY_RESPONSE": 24})
            self.assertEqual(report["sousPriorisation"]["count"], 0)
            self.assertEqual(sum(sum(r) for r in report["categorie"]["confusionMatrix"]["counts"]), 24)

    def test_invalid_contributes_false_negative_and_not_eighth_macro_class(self):
        rows = self.metric_rows()
        rows[0].update(sortieValide="false", typeErreur="INVALID_JSON", categoriePredite="", prioritePredite="")
        report = b.calculate_metrics(rows, self.spec)[0]
        self.assertAlmostEqual(report["categorie"]["accuracy"], 23/24)
        # ETUDE_DANGERS : TP=1,FN=1,FP=0 -> F1=2/3 ; les six autres F1=1.
        self.assertAlmostEqual(report["categorie"]["macroF1"], (6+2/3)/7)
        self.assertAlmostEqual(report["priorite"]["macroF1"], (3+12/13)/4)
        self.assertAlmostEqual(report["exactMatch"]["rate"], 23/24)
        self.assertEqual(len(report["categorie"]["confusionMatrix"]["counts"]), 7)
        self.assertEqual(report["categorie"]["confusionMatrix"]["counts"][0][-1], 1)

    def test_underprioritisation_uses_valid_priorities_and_explicit_denominator(self):
        rows = self.metric_rows()
        rows[0]["prioritePredite"] = "BASSE"
        rows[2].update(sortieValide="false", typeErreur="TIMEOUT", categoriePredite="", prioritePredite="")
        report = b.calculate_metrics(rows, self.spec)[0]
        self.assertEqual(report["sousPriorisation"]["count"], 1)
        self.assertAlmostEqual(report["sousPriorisation"]["rate"], 1/24)
        self.assertEqual(report["sousPriorisation"]["validPrioritiesExamined"], 23)

    def test_score_validation_rejects_partial_duplicate_reference_or_status_tampering(self):
        rows = self.metric_rows()
        b.validate_results(rows, self.cases, self.spec)
        bad = [rows[:-1], rows[:-1]+[rows[0]]]
        for mutation in [{"categorieReference": "AUTRE"}, {"sortieValide": "true", "typeErreur": "TIMEOUT"},
                         {"categoriePredite": "INVENTE"}, {"latenceMs": "NaN"}]:
            altered = copy.deepcopy(rows)
            altered[0].update(mutation)
            bad.append(altered)
        for altered in bad:
            with self.assertRaises(b.ValidationError):
                b.validate_results(altered, self.cases, self.spec)

    def test_requested_model_fallback_and_one_snapshot_are_reported_without_loss(self):
        rows = self.metric_rows()
        for row in rows[1:24]:
            row["model"] = "gemini-3.8-flash-snapshot"
        rows[0].update(sortieValide="false", typeErreur="TIMEOUT", categoriePredite="", prioritePredite="")
        b.validate_results(rows, self.cases, self.spec)
        report = b.calculate_metrics(rows, self.spec)[0]
        self.assertEqual(report["model"], "gemini-3.8-flash-snapshot")
        self.assertEqual(report["modelsRecorded"], ["gemini-3.8-flash", "gemini-3.8-flash-snapshot"])
        rows[2]["model"] = "gemini-3.8-flash-other-snapshot"
        with self.assertRaises(b.ValidationError):
            b.validate_results(rows, self.cases, self.spec)

    def test_score_refuses_changed_inputs_before_reading_results(self):
        with patch.object(b.Path, "read_text", return_value=json.dumps({"status": "completed", "datasetSha256": "different"})):
            with self.assertRaises(b.ValidationError):
                b.score(self.spec, self.cases)

    def test_transport_serializes_utf8_and_uses_timeout_without_real_network(self):
        from unittest.mock import MagicMock
        response = MagicMock()
        response.__enter__.return_value.read.return_value = b'{"ok": true}'
        response.__enter__.return_value.headers = {}
        opener = MagicMock()
        opener.open.return_value = response
        with patch.object(b, "build_opener", return_value=opener):
            result = b.post_json("GROQ", "test-only", {"texte": "échéance"}, 7)
        self.assertEqual(json.loads(result[0]), {"ok": True})
        request = opener.open.call_args.args[0]
        self.assertEqual(request.get_method(), "POST")
        self.assertEqual(json.loads(request.data.decode("utf-8")), {"texte": "échéance"})
        self.assertEqual(opener.open.call_args.kwargs["timeout"], 7)

    def test_gemini_auth_header_has_no_bearer_and_groq_has_bearer(self):
        from unittest.mock import MagicMock
        for provider in b.PROVIDERS:
            response = MagicMock()
            response.__enter__.return_value.read.return_value = b'{}'
            response.__enter__.return_value.headers = {}
            opener = MagicMock()
            opener.open.return_value = response
            with patch.object(b, "build_opener", return_value=opener):
                b.post_json(provider, "test-only", {}, 1)
            request = opener.open.call_args.args[0]
            self.assertEqual(request.full_url, b.PROVIDERS[provider][1])
            headers = {key.lower(): value for key, value in request.header_items()}
            if provider == "GEMINI":
                self.assertEqual(headers["x-goog-api-key"], "test-only")
                self.assertNotIn("authorization", headers)
            else:
                self.assertEqual(headers["authorization"], "Bearer test-only")
                self.assertNotIn("x-goog-api-key", headers)

    def test_gemini_ignores_thought_but_only_reads_model_output(self):
        envelope = self.envelope("GEMINI")
        envelope["steps"].insert(0, {"type": "thought", "summary": [{"type": "text", "text": "ignored"}]})
        self.assertEqual(b.extract_business("GEMINI", envelope), (json.dumps(self.business()), "NONE"))
        envelope["steps"].append(copy.deepcopy(envelope["steps"][1]))
        self.assertEqual(b.extract_business("GEMINI", envelope)[1], "INVALID_SCHEMA")

    def test_rate_limit_duration_formats(self):
        self.assertEqual(b.duration_seconds("2m59.56s"), 179.56)
        self.assertEqual(b.duration_seconds("250ms"), .25)
        self.assertEqual(b.duration_seconds("2"), 2)
        self.assertEqual(b.duration_seconds("NaN"), 0)
        self.assertEqual(b.duration_seconds("not-a-duration"), 0)

    def test_groq_pacing_uses_tpm_usage_and_resets(self):
        pacer = b.GroqPacer()
        with patch.object(b.time, "monotonic", return_value=100):
            pacer.observe({"x-ratelimit-limit-tokens": "8000"}, [1000, 0, 1000])
            self.assertEqual(pacer.ready_at, 107.5)
            pacer.observe({"x-ratelimit-remaining-tokens": "0", "x-ratelimit-reset-tokens": "20s"}, [None]*3)
            self.assertEqual(pacer.ready_at, 120)
            pacer.observe({"x-ratelimit-remaining-requests": "0", "x-ratelimit-reset-requests": "1m"}, [None]*3)
            self.assertEqual(pacer.ready_at, 160)

    def test_groq_retry_after_wait_is_traced_and_excluded_from_latency(self):
        pacer = b.GroqPacer()
        clock = [100.0]
        def sleep(seconds):
            clock[0] += seconds
        error = HTTPError("https://example.invalid", 429, "rate", {"Retry-After": "7"}, None)
        log = io.StringIO()
        with patch.object(b.time, "monotonic", side_effect=lambda: clock[0]), \
             patch.object(b.time, "sleep", side_effect=sleep), \
             patch.object(b.time, "perf_counter", side_effect=[0, .1, 100, 100.2]), \
             patch.object(b, "post_json", side_effect=[error, (json.dumps(self.envelope("GROQ")), {})]) as post:
            row, _ = b.perform_case("GROQ", self.cases[0], self.spec, "test-only", 1, True, log, pacer)
        events = [json.loads(line) for line in log.getvalue().splitlines()]
        self.assertEqual(post.call_count, 2)
        self.assertEqual(events[1]["pacingSeconds"], 7)
        self.assertEqual(events[0]["rateLimitHeaders"]["retry-after"], "7")
        self.assertAlmostEqual(row["latenceMs"], 300)
        self.assertEqual(row["sortieValide"], "true")

    def test_run_missing_keys_fails_before_file_creation_or_network(self):
        with patch.dict(b.os.environ, {}, clear=True), patch.object(b, "post_json", side_effect=AssertionError("API interdite")):
            with self.assertRaises(b.ValidationError):
                b.run(self.spec, self.cases, 1, False)


if __name__ == "__main__":
    unittest.main()
