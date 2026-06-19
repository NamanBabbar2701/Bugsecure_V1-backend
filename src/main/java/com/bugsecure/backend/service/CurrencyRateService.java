package com.bugsecure.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-time FX rates with in-memory caching.
 *
 * This implementation is production-friendly:
 * - caches rates per base currency with TTL
 * - falls back to a static USD->INR approximation if API key/provider is not configured
 */
@Service
public class CurrencyRateService {

    private static final double FALLBACK_USD_TO_INR = 83.0;
    private static final long CACHE_TTL_SECONDS_DEFAULT = 1800; // 30 minutes

    @Value("${fx.provider.api-key:}")
    private String apiKey;

    @Value("${fx.provider.base-url:https://v6.exchangerate-api.com/v6}")
    private String providerBaseUrl;

    @Value("${fx.cache.ttl-seconds:" + CACHE_TTL_SECONDS_DEFAULT + "}")
    private long cacheTtlSeconds;

    private final RestTemplate restTemplate = new RestTemplate();

    private static class CachedRates {
        private final Map<String, Double> rates;
        private final long fetchedAtEpochSeconds;

        private CachedRates(Map<String, Double> rates, long fetchedAtEpochSeconds) {
            this.rates = rates;
            this.fetchedAtEpochSeconds = fetchedAtEpochSeconds;
        }
    }

    // base -> cached rates
    private final Map<String, CachedRates> cache = new ConcurrentHashMap<>();

    public Map<String, Object> getRates(String base, Set<String> symbols) {
        String normalizedBase = (base == null || base.isBlank()) ? "USD" : base.toUpperCase();
        Set<String> normalizedSymbols = (symbols == null || symbols.isEmpty())
                ? Set.of("INR")
                : symbols.stream().map(String::toUpperCase).collect(java.util.stream.Collectors.toSet());

        Map<String, Double> rates = fetchRatesForBase(normalizedBase);

        // Subset only requested symbols
        Map<String, Double> outRates = new ConcurrentHashMap<>();
        for (String symbol : normalizedSymbols) {
            if (normalizedBase.equals(symbol)) {
                outRates.put(symbol, 1.0);
            } else if (rates.containsKey(symbol)) {
                outRates.put(symbol, rates.get(symbol));
            }
        }

        // Ensure USD->INR works even if provider isn't configured
        if (outRates.isEmpty() && "USD".equals(normalizedBase) && normalizedSymbols.contains("INR")) {
            outRates.put("INR", FALLBACK_USD_TO_INR);
        }

        CachedRates cached = cache.get(normalizedBase);
        long fetchedAt = cached != null ? cached.fetchedAtEpochSeconds : Instant.now().getEpochSecond();

        return Map.of(
                "base", normalizedBase,
                "symbols", normalizedSymbols,
                "rates", outRates,
                "fetchedAt", fetchedAt
        );
    }

    private Map<String, Double> fetchRatesForBase(String base) {
        CachedRates existing = cache.get(base);
        long now = Instant.now().getEpochSecond();

        if (existing != null && (now - existing.fetchedAtEpochSeconds) <= cacheTtlSeconds) {
            return existing.rates;
        }

        // Provider not configured: return limited fallback.
        if (apiKey == null || apiKey.isBlank()) {
            cache.put(base, new CachedRates(Collections.emptyMap(), now));
            return Collections.emptyMap();
        }

        try {
            // Example for exchangerate-api:
            // GET {base-url}/{apiKey}/latest/{base}
            String url = String.format("%s/%s/latest/%s", providerBaseUrl, apiKey, base);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = restTemplate.getForObject(url, Map.class);

            if (payload == null || !payload.containsKey("rates")) {
                cache.put(base, new CachedRates(Collections.emptyMap(), now));
                return Collections.emptyMap();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> ratesObj = (Map<String, Object>) payload.get("rates");

            Map<String, Double> normalized = new ConcurrentHashMap<>();
            for (Map.Entry<String, Object> entry : ratesObj.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    normalized.put(entry.getKey().toUpperCase(), ((Number) entry.getValue()).doubleValue());
                }
            }

            cache.put(base, new CachedRates(normalized, now));
            return normalized;
        } catch (Exception e) {
            // Fail closed but safe: use empty rates and later fallback for USD->INR.
            cache.put(base, new CachedRates(Collections.emptyMap(), now));
            return Collections.emptyMap();
        }
    }
}

