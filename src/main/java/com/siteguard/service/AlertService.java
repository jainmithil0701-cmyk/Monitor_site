package com.siteguard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Sends DOWN / RECOVERED alerts to a webhook (Discord). If no webhook is set, it only logs. */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final String webhookUrl;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AlertService(@Value("${siteguard.webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void siteDown(String name, String url, String reason) {
        send("🔴 DOWN: " + name + " (" + url + ") - " + reason);
    }

    public void siteRecovered(String name, String url) {
        send("🟢 RECOVERED: " + name + " (" + url + ") is back UP");
    }

    private void send(String message) {
        log.warn("ALERT -> {}", message);
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            String safe = message.replace("\\", "\\\\").replace("\"", "\\\"");
            String json = "{\"content\":\"" + safe + "\"}";
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            log.info("Webhook responded with status {}", response.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Could not send alert: {}", e.getMessage());
        }
    }
}
