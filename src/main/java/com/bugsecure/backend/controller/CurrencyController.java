package com.bugsecure.backend.controller;

import com.bugsecure.backend.service.CurrencyRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/currency")
public class CurrencyController {

    @Autowired
    private CurrencyRateService currencyRateService;

    @GetMapping("/rates")
    public ResponseEntity<Map<String, Object>> getRates(
            @RequestParam(required = false, defaultValue = "USD") String base,
            @RequestParam(required = false, defaultValue = "INR,USD") String symbols
    ) {
        Set<String> symbolSet = new HashSet<>();
        if (symbols != null && !symbols.isBlank()) {
            symbolSet.addAll(Arrays.asList(symbols.split(",")));
        }

        Map<String, Object> rates = currencyRateService.getRates(base, symbolSet);
        return ResponseEntity.ok(rates);
    }
}

