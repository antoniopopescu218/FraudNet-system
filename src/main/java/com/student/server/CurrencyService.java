package com.student.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class CurrencyService {

    // Free API endpoint (No key required for base currency EUR)
    // Returns JSON like: {"rates": {"RON": 4.97, "USD": 1.08 ...}}
    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/EUR";

    private final HttpClient httpClient;

    public CurrencyService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public double getEuroRate(String currencyCode) {
        if ("EUR".equalsIgnoreCase(currencyCode)) return 1.0;

        try {
            System.out.println("[API] Fetching live rates for " + currencyCode + "...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse JSON using Gson
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject rates = json.getAsJsonObject("rates");

            if (rates.has(currencyCode)) {
                // Math: If 1 EUR = 4.97 RON, then 1 RON = 1 / 4.97 EUR
                double rateToEur = rates.get(currencyCode).getAsDouble();
                return 1.0 / rateToEur;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[API ERROR] Could not fetch rates: " + e.getMessage());
        }

        return 0.0; // Fail-safe (Block transaction if rate unknown)
    }
}