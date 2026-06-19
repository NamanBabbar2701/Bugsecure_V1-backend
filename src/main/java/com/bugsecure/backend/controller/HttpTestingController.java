package com.bugsecure.backend.controller;

import com.bugsecure.backend.model.HttpRequestLog;
import com.bugsecure.backend.model.HttpTestCase;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.service.SafeHttpRequestService;
import com.bugsecure.backend.service.SafeHttpRequestService.HttpSendResult;
import com.bugsecure.backend.service.SafeHttpRequestService.KeyValue;
import com.bugsecure.backend.repository.HttpRequestLogRepository;
import com.bugsecure.backend.repository.HttpTestCaseRepository;
import com.bugsecure.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/testing/http")
@CrossOrigin(origins = "*")
public class HttpTestingController {

    @Autowired
    private SafeHttpRequestService safeHttpRequestService;

    @Autowired
    private HttpRequestLogRepository httpRequestLogRepository;

    @Autowired
    private HttpTestCaseRepository httpTestCaseRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/send")
    public Map<String, Object> sendRequest(
            @RequestBody SendRequest request,
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        if (request == null) {
            response.put("success", false);
            response.put("error", "Request body is required");
            return response;
        }

        if (request.method == null || request.method.isBlank()) {
            response.put("success", false);
            response.put("error", "method is required");
            return response;
        }

        if (request.url == null || request.url.isBlank()) {
            response.put("success", false);
            response.put("error", "url is required");
            return response;
        }

        if (request.headers == null) request.headers = new ArrayList<>();
        if (request.queryParams == null) request.queryParams = new ArrayList<>();
        if (request.bodyFields == null) request.bodyFields = new ArrayList<>();

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Basic authorization: allow only authenticated researchers.
        boolean isUser = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority()));
        if (!isUser) {
            response.put("success", false);
            response.put("error", "Unauthorized. Researchers only.");
            return response;
        }

        String email = userDetails.getUsername();
        User researcher = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create log entry up-front so *all* attempts are persisted (success + blocked + failures).
        HttpRequestLog log = new HttpRequestLog();
        log.setResearcher(researcher);
        log.setMethod(request.method);
        log.setUrl(request.url);
        log.setEndpoint(request.url); // v1 UI only sends full URL, keep both for convenience
        log.setFullUrlPreview(request.url);
        log.setHeaders(toModelKvList(request.headers));
        log.setQueryParams(toModelKvList(request.queryParams));
        log.setBodyType(request.bodyType);
        log.setBodyRaw(truncateForStorage(request.bodyRaw, 200_000));
        log.setBodyFields(toModelKvList(request.bodyFields));
        log.setTimeoutMs(request.timeoutMs);
        log.setMaxResponseBytes(request.maxResponseBytes);
        log.setStatusCode(null);
        log.setDurationMs(null);
        log.setResponseSizeBytes(null);
        log.setTruncated(false);
        log.setResponseHeaders(null);
        log.setResponseBody(null);
        log.setError(null);
        log.setCreatedAtIfNew();

        try {
            HttpSendResult result = safeHttpRequestService.send(
                    email,
                    request.method,
                    request.url,
                    toSafeKvList(request.headers),
                    toSafeKvList(request.queryParams),
                    request.bodyType,
                    request.bodyRaw,
                    toSafeKvList(request.bodyFields),
                    request.timeoutMs,
                    request.maxResponseBytes
            );

            log.setStatusCode(result.statusCode());
            log.setDurationMs(result.durationMs());
            log.setResponseSizeBytes(result.responseSizeBytes());
            log.setTruncated(result.truncated());
            log.setResponseHeaders(result.headers());
            log.setResponseBody(truncateForStorage(result.body(), 2_000_000));
            log.setError(null);

            HttpRequestLog saved = httpRequestLogRepository.save(log);

            // Response: keep existing top-level response fields, plus `log` for UI history.
            response.put("success", true);
            response.put("data", Map.of(
                    "statusCode", result.statusCode(),
                    "durationMs", result.durationMs(),
                    "responseSizeBytes", result.responseSizeBytes(),
                    "truncated", result.truncated(),
                    "body", result.body(),
                    "headers", result.headers(),
                    "log", Map.of(
                            "id", saved.getId(),
                            "createdAt", saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null,
                            "method", saved.getMethod(),
                            "endpoint", saved.getEndpoint(),
                            "fullUrlPreview", saved.getFullUrlPreview(),
                            "request", Map.of(
                                    "method", request.method,
                                    "url", request.url,
                                    "headers", saved.getHeaders(),
                                    "queryParams", saved.getQueryParams(),
                                    "bodyType", saved.getBodyType(),
                                    "bodyRaw", saved.getBodyRaw(),
                                    "bodyFields", saved.getBodyFields(),
                                    "timeoutMs", saved.getTimeoutMs(),
                                    "maxResponseBytes", saved.getMaxResponseBytes()
                            )
                    )
            ));
            return response;
        } catch (Exception e) {
            log.setError(e.getMessage());
            HttpRequestLog saved = httpRequestLogRepository.save(log);

            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("data", Map.of("logId", saved.getId()));
            return response;
        }
    }

    @GetMapping("/history")
    public Map<String, Object> getHistory(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User researcher = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isUser = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority()));
            if (!isUser) throw new RuntimeException("Unauthorized. Researchers only.");

            List<HttpRequestLog> logs = httpRequestLogRepository.findByResearcherOrderByCreatedAtDesc(researcher);
            response.put("success", true);
            response.put("data", logs.stream().map(this::toHistoryItem).collect(Collectors.toList()));
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @DeleteMapping("/history/{logId}")
    public Map<String, Object> deleteHistoryItem(
            @PathVariable String logId,
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User researcher = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isUser = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority()));
            if (!isUser) throw new RuntimeException("Unauthorized. Researchers only.");

            HttpRequestLog log = httpRequestLogRepository.findById(logId)
                    .orElseThrow(() -> new RuntimeException("History item not found"));

            if (log.getResearcher() == null || log.getResearcher().getId() == null || !log.getResearcher().getId().equals(researcher.getId())) {
                throw new RuntimeException("Unauthorized delete attempt");
            }

            httpRequestLogRepository.delete(log);
            response.put("success", true);
            response.put("data", Map.of("deleted", true));
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @PostMapping("/test-cases")
    public Map<String, Object> createTestCase(
            @RequestBody CreateTestCaseRequest request,
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User researcher = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isUser = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority()));
            if (!isUser) throw new RuntimeException("Unauthorized. Researchers only.");

            if (request == null) throw new RuntimeException("Request body is required");
            if (request.name == null || request.name.isBlank()) throw new RuntimeException("Test case name is required");
            if (request.snapshot == null) throw new RuntimeException("Test case snapshot is required");

            HttpTestCase tc = new HttpTestCase();
            tc.setResearcher(researcher);
            tc.setName(request.name.trim());
            tc.setDescription(request.description);

            tc.setMethod(request.snapshot.method);
            tc.setUrl(request.snapshot.url);
            tc.setEndpoint(request.snapshot.url);
            tc.setFullUrlPreview(request.snapshot.url);

            tc.setHeaders(toModelKvList(request.snapshot.headers));
            tc.setQueryParams(toModelKvList(request.snapshot.queryParams));
            tc.setBodyType(request.snapshot.bodyType);
            tc.setBodyRaw(truncateForStorage(request.snapshot.bodyRaw, 200_000));
            tc.setBodyFields(toModelKvList(request.snapshot.bodyFields));
            tc.setTimeoutMs(request.snapshot.timeoutMs);
            tc.setMaxResponseBytes(request.snapshot.maxResponseBytes);

            tc.setCreatedAtIfNew();
            HttpTestCase saved = httpTestCaseRepository.save(tc);

            response.put("success", true);
            response.put("data", Map.of(
                    "id", saved.getId(),
                    "name", saved.getName(),
                    "createdAt", saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null,
                    "request", toRequestSnapshot(saved)
            ));
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @GetMapping("/test-cases")
    public Map<String, Object> listTestCases(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User researcher = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isUser = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority()));
            if (!isUser) throw new RuntimeException("Unauthorized. Researchers only.");

            List<HttpTestCase> tcs = httpTestCaseRepository.findByResearcherOrderByCreatedAtDesc(researcher);
            response.put("success", true);
            response.put("data", tcs.stream().map(tc -> Map.of(
                    "id", tc.getId(),
                    "name", tc.getName(),
                    "createdAt", tc.getCreatedAt() != null ? tc.getCreatedAt().toString() : null,
                    "request", toRequestSnapshot(tc)
            )).collect(Collectors.toList()));
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @DeleteMapping("/test-cases/{testCaseId}")
    public Map<String, Object> deleteTestCase(
            @PathVariable String testCaseId,
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User researcher = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isUser = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority()));
            if (!isUser) throw new RuntimeException("Unauthorized. Researchers only.");

            HttpTestCase tc = httpTestCaseRepository.findById(testCaseId)
                    .orElseThrow(() -> new RuntimeException("Test case not found"));

            if (tc.getResearcher() == null || tc.getResearcher().getId() == null || !tc.getResearcher().getId().equals(researcher.getId())) {
                throw new RuntimeException("Unauthorized delete attempt");
            }

            httpTestCaseRepository.delete(tc);
            response.put("success", true);
            response.put("data", Map.of("deleted", true));
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    private Map<String, Object> toHistoryItem(HttpRequestLog log) {
        return Map.of(
                "id", log.getId(),
                "createdAt", log.getCreatedAt() != null ? log.getCreatedAt().toString() : null,
                "method", log.getMethod(),
                "endpoint", log.getEndpoint(),
                "fullUrlPreview", log.getFullUrlPreview(),
                "request", toRequestSnapshotFromLog(log)
        );
    }

    private Map<String, Object> toRequestSnapshot(HttpTestCase tc) {
        return Map.of(
                "method", tc.getMethod(),
                "url", tc.getUrl(),
                "headers", tc.getHeaders(),
                "queryParams", tc.getQueryParams(),
                "bodyType", tc.getBodyType(),
                "bodyRaw", tc.getBodyRaw(),
                "bodyFields", tc.getBodyFields(),
                "timeoutMs", tc.getTimeoutMs(),
                "maxResponseBytes", tc.getMaxResponseBytes()
        );
    }

    private Map<String, Object> toRequestSnapshotFromLog(HttpRequestLog log) {
        return Map.of(
                "method", log.getMethod(),
                "url", log.getUrl(),
                "headers", log.getHeaders(),
                "queryParams", log.getQueryParams(),
                "bodyType", log.getBodyType(),
                "bodyRaw", log.getBodyRaw(),
                "bodyFields", log.getBodyFields(),
                "timeoutMs", log.getTimeoutMs(),
                "maxResponseBytes", log.getMaxResponseBytes()
        );
    }

    private List<HttpRequestLog.KeyValue> toModelKvList(List<Kv> list) {
        List<HttpRequestLog.KeyValue> out = new ArrayList<>();
        if (list == null) return out;
        for (Kv kv : list) {
            if (kv == null) continue;
            String key = kv.key;
            String value = kv.value;
            boolean enabled = kv.enabled != null ? kv.enabled : true;
            if (key == null || key.isBlank()) continue;
            // Never persist sensitive auth headers in logs/test-cases.
            if (key.equalsIgnoreCase("authorization") || key.equalsIgnoreCase("cookie")) {
                continue;
            }
            out.add(new HttpRequestLog.KeyValue(key, value, enabled));
        }
        return out;
    }

    private String truncateForStorage(String s, int maxChars) {
        if (s == null) return null;
        if (maxChars <= 0) return s;
        if (s.length() <= maxChars) return s;
        return s.substring(0, maxChars) + "\n...[truncated]";
    }

    private List<KeyValue> toSafeKvList(List<Kv> list) {
        List<KeyValue> out = new ArrayList<>();
        if (list == null) return out;
        for (Kv kv : list) {
            if (kv == null) continue;
            String key = kv.key;
            String value = kv.value;
            boolean enabled = kv.enabled != null ? kv.enabled : true;
            out.add(new KeyValue(key, value, enabled));
        }
        return out;
    }

    public static class SendRequest {
        public String method;
        public String url;

        public List<Kv> headers;
        public List<Kv> queryParams;

        public String bodyType; // json | form-data | x-www-form-urlencoded | raw
        public String bodyRaw;
        public List<Kv> bodyFields; // used for urlencoded or form-data (text fields)

        public Integer timeoutMs;
        public Integer maxResponseBytes;
    }

    public static class Kv {
        public String key;
        public String value;
        public Boolean enabled;
    }

    public static class CreateTestCaseRequest {
        public String name;
        public String description;
        public SendRequest snapshot;
    }
}

