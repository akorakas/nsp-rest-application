package com.example.kafka.nsp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NspClient {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    // ───── Values from application.yml ─────
    @Value("${app.rest.nsp.scheme:https}")
    private String scheme;

    @Value("${app.rest.nsp.host}")
    private String host;

    @Value("${app.rest.nsp.paths.token}")
    private String tokenPath;

    @Value("${app.rest.nsp.paths.alarms}")
    private String alarmsPath;

    @Value("${app.rest.nsp.auth.basic}")
    private String basicAuth;

    @Value("${app.rest.nsp.auth.grant-type:client_credentials}")
    private String grantType;

    @Value("${app.rest.nsp.headers.content-type:application/json}")
    private String contentType;

    @Value("${app.rest.nsp.headers.accept:application/json}")
    private String accept;

    // ───────────────────────────────────────

    private String cachedToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    private String baseUrl() {
        return scheme + "://" + host;
    }

    private synchronized String getAccessToken() throws Exception {
        if (cachedToken != null &&
            Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return cachedToken;
        }

        String url = baseUrl() + tokenPath;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + basicAuth);
        headers.setContentType(MediaType.valueOf(contentType));
        headers.setAccept(List.of(MediaType.valueOf(accept)));

        Map<String, String> body = Map.of("grant_type", grantType);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("NSP token request failed: " + response.getStatusCode());
        }

        JsonNode json = objectMapper.readTree(response.getBody());
        String accessToken = json.path("access_token").asText(null);
        int expiresIn = json.path("expires_in").asInt(600);

        if (accessToken == null) {
            throw new IllegalStateException("No access_token in NSP response: " + response.getBody());
        }

        this.cachedToken = accessToken;
        this.tokenExpiresAt = Instant.now().plusSeconds(expiresIn);

        return accessToken;
    }

    public String fetchActiveAlarmsRaw() throws Exception {
        String token = getAccessToken();

        String url = baseUrl() + alarmsPath;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.valueOf(accept)));
        headers.setContentType(MediaType.valueOf(contentType));

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("NSP alarms request failed: " + response.getStatusCode());
        }

        return response.getBody();
    }

    public List<String> fetchActiveAlarmEvents() throws Exception {
        String raw = fetchActiveAlarmsRaw();
        JsonNode root = objectMapper.readTree(raw);
        List<String> result = new ArrayList<>();

        if (root.isArray()) {
            for (JsonNode node : root) {
                result.add(objectMapper.writeValueAsString(node));
            }
        } else if (root.has("alarms") && root.get("alarms").isArray()) {
            for (JsonNode node : root.get("alarms")) {
                result.add(objectMapper.writeValueAsString(node));
            }
        } else {
            result.add(raw);
        }

        return result;
    }
}
