package com.akrouty.gestiondemandes.ai.groq;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Pas de validation fail-fast ni de toString exposant la cle. */
@Component
@ConfigurationProperties(prefix = "app.ai.groq")
public class GroqProperties {
    private String apiKey;
    private Duration timeout;
    private URI baseUrl = URI.create("https://api.groq.com");

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public URI getBaseUrl() { return baseUrl; }
    public void setBaseUrl(URI baseUrl) { this.baseUrl = baseUrl; }
}
