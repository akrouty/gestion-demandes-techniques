package com.akrouty.gestiondemandes.ai.groq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import com.akrouty.gestiondemandes.request.application.AnalyseIaIndisponibleException;
import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class GroqAnalyseIaAdapterTest {
    private static final String CLE_TEST = "test-only-groq-api-key";
    private static final String VALIDE = "{\"categorieProposee\":\"NOTE_CALCUL\",\"prioriteProposee\":\"HAUTE\"}";
    private final JsonMapper json = JsonMapper.builder().build();
    private HttpServer server;
    private ExecutorService executor;
    private volatile int status = 200;
    private volatile long delayMillis;
    private volatile String body;
    private final AtomicInteger appels = new AtomicInteger();
    private final AtomicReference<String> requete = new AtomicReference<>();
    private final AtomicReference<String> autorisation = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicReference<String> methode = new AtomicReference<>();

    @BeforeEach
    void stubHttpLocal() throws IOException {
        body = enveloppe(VALIDE);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.createContext("/openai/v1/chat/completions", exchange -> {
            appels.incrementAndGet();
            requete.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            autorisation.set(exchange.getRequestHeaders().getFirst("Authorization"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            methode.set(exchange.getRequestMethod());
            try {
                if (delayMillis > 0) Thread.sleep(delayMillis);
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                // Le test de timeout ferme volontairement le client avant la reponse.
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    @AfterEach
    void fermerStub() {
        if (server != null) server.stop(0);
        executor.shutdownNow();
    }

    private GroqProperties properties() {
        GroqProperties properties = new GroqProperties();
        properties.setApiKey(CLE_TEST);
        properties.setTimeout(Duration.ofSeconds(2));
        properties.setBaseUrl(URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
        return properties;
    }

    private String enveloppe(String content) {
        return json.writeValueAsString(Map.of("model", GroqAnalyseIaAdapter.MODEL, "choices",
                List.of(Map.of("finish_reason", "stop", "message",
                        Map.of("role", "assistant", "content", content)))));
    }

    private void echec(GroqProperties properties) {
        assertThatThrownBy(() -> new GroqAnalyseIaAdapter(properties).analyser("Titre", "Description"))
                .isInstanceOf(AnalyseIaIndisponibleException.class)
                .hasMessage("Analyse IA temporairement indisponible.")
                .hasNoCause();
    }

    @Test
    void succes_payload_prompt_schema_et_enums_fideles() throws IOException {
        var resultat = new GroqAnalyseIaAdapter(properties()).analyser("Calcul", "Dimensionner le support");
        assertThat(resultat.categorieProposee()).isEqualTo(Categorie.NOTE_CALCUL);
        assertThat(resultat.prioriteProposee()).isEqualTo(Priorite.HAUTE);
        assertThat(appels.get()).isEqualTo(1);
        assertThat(methode.get()).isEqualTo("POST");
        assertThat(contentType.get()).startsWith("application/json");
        assertThat(autorisation.get()).isEqualTo("Bearer " + CLE_TEST);
        var payload = json.readTree(requete.get());
        assertThat(payload.properties()).extracting(Map.Entry::getKey).containsExactlyInAnyOrder(
                "model", "reasoning_effort", "n", "stream", "messages", "response_format");
        assertThat(payload.path("model").asString()).isEqualTo("qwen/qwen3.8-27b");
        assertThat(payload.path("reasoning_effort").asString()).isEqualTo("none");
        assertThat(payload.path("n").asInt()).isEqualTo(1);
        assertThat(payload.path("stream").asBoolean()).isFalse();
        assertThat(payload.path("messages").size()).isEqualTo(1);
        assertThat(payload.path("messages").path(0).path("role").asString()).isEqualTo("user");
        String reference = Files.readString(Path.of("../docs/ai/AI-DESIGN-V1.md"), StandardCharsets.UTF_8);
        var promptMatcher = Pattern.compile("## 7\\..*?```text\\n(.*?)```", Pattern.DOTALL).matcher(reference);
        assertThat(promptMatcher.find()).isTrue();
        assertThat(GroqAnalyseIaAdapter.PROMPT).isEqualTo(promptMatcher.group(1));
        assertThat(payload.path("messages").path(0).path("content").asString())
                .isEqualTo(promptMatcher.group(1).replace("{{titre}}", "Calcul")
                        .replace("{{description}}", "Dimensionner le support"));
        var schemaMatcher = Pattern.compile("## 8\\..*?```json\\n(.*?)```", Pattern.DOTALL).matcher(reference);
        assertThat(schemaMatcher.find()).isTrue();
        var format = payload.path("response_format");
        assertThat(format.path("type").asString()).isEqualTo("json_schema");
        assertThat(format.path("json_schema").path("strict").asBoolean()).isTrue();
        assertThat(format.path("json_schema").path("schema")).isEqualTo(json.readTree(schemaMatcher.group(1)));
        var schema = format.path("json_schema").path("schema").path("properties");
        assertThat(schema.path("categorieProposee").path("enum"))
                .isEqualTo(json.valueToTree(Arrays.stream(Categorie.values()).map(Enum::name).toList()));
        assertThat(schema.path("prioriteProposee").path("enum"))
                .isEqualTo(json.valueToTree(Arrays.stream(Priorite.values()).map(Enum::name).toList()));
    }

    @Test
    void donnees_ne_reinterpretent_pas_les_placeholders() {
        new GroqAnalyseIaAdapter(properties()).analyser("{{description}} $1 \\ test", "Valeur description");
        String prompt = json.readTree(requete.get()).path("messages").path(0).path("content").asString();
        assertThat(prompt).contains("Titre :\n{{description}} $1 \\ test\n", "Description :\nValeur description\n");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "pas du JSON", "```json\n{}\n```", "[]", "null", "{}", " ",
        "{\"categorieProposee\":\"INCONNUE\",\"prioriteProposee\":\"HAUTE\"}",
        "{\"categorieProposee\":\"NOTE_CALCUL\",\"prioriteProposee\":\"URGENT\"}",
        "{\"categorieProposee\":\"NOTE_CALCUL\"}",
        "{\"prioriteProposee\":\"HAUTE\"}",
        "{\"categorieProposee\":\"NOTE_CALCUL\",\"prioriteProposee\":\"HAUTE\",\"confidence\":1}",
        "{\"categorieProposee\":1,\"prioriteProposee\":\"HAUTE\"}",
        "{\"categorieProposee\":\"NOTE_CALCUL\",\"prioriteProposee\":null}",
        "{\"categorieProposee\":\"NOTE_CALCUL\",\"prioriteProposee\":\"haute\"}",
        "{\"categorieProposee\":\"NOTE_CALCUL\",\"prioriteProposee\":\"HAUTE\"} {}",
        "{\"categorieProposee\":\"NOTE_CALCUL\",\"categorieProposee\":\"AUTRE\",\"prioriteProposee\":\"HAUTE\"}"
    })
    void sortie_metier_invalide_sans_reparation_ni_retry(String content) {
        body = enveloppe(content);
        echec(properties());
        assertThat(appels.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "null", "[]", "{}", "invalid-json", "{\"choices\":[]}",
        "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\"}}]}",
        "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"role\":\"assistant\",\"content\":\"{}\"}}]}"})
    void enveloppe_ou_contenu_absent_invalide(String reponse) {
        body = reponse;
        echec(properties());
        assertThat(appels.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500, 503, 401, 403, 302})
    void erreurs_http_sans_details_fournisseur_ni_retry(int httpStatus) {
        status = httpStatus;
        body = "details-provider-et-donnees-ne-doivent-pas-sortir";
        echec(properties());
        assertThat(appels.get()).isEqualTo(1);
    }

    @Test
    void timeout_http_controle() {
        delayMillis = 1000;
        GroqProperties properties = properties();
        properties.setTimeout(Duration.ofMillis(100));
        assertTimeout(Duration.ofSeconds(3), () -> echec(properties));
        assertThat(appels.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void cle_absente_ou_vide_aucun_appel(String cle) {
        GroqProperties properties = properties();
        properties.setApiKey(cle);
        echec(properties);
        assertThat(appels.get()).isZero();
    }

    @Test
    void timeout_absent_aucun_appel() {
        GroqProperties properties = properties();
        properties.setTimeout(null);
        echec(properties);
        assertThat(appels.get()).isZero();
    }

    @Test
    void erreur_reseau_locale() {
        GroqProperties properties = properties();
        server.stop(0);
        server = null;
        echec(properties);
        assertThat(appels.get()).isZero();
    }
}
