package com.bugsecure.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CurrencyConversionService {

    // Basic fixed rates (can be swapped with live FX later)
    // All rates are relative to 1 USD.
    private static final Map<String, Double> USD_RATES = Map.of(
            "USD", 1.0,
            "INR", 83.0,
            "EUR", 0.92
    );

    public String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) return "USD";
        String c = currency.trim().toUpperCase();
        return USD_RATES.containsKey(c) ? c : "USD";
    }

    public double toUsd(double amount, String currency) {
        String c = normalizeCurrency(currency);
        double rate = USD_RATES.getOrDefault(c, 1.0);
        // amount (currency) = amountUsd * rate => amountUsd = amount / rate
        return amount / rate;
    }

    public double fromUsd(double amountUsd, String currency) {
        String c = normalizeCurrency(currency);
        double rate = USD_RATES.getOrDefault(c, 1.0);
        return amountUsd * rate;
    }
}

