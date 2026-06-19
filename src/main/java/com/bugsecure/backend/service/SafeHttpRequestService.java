package com.bugsecure.backend.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublisher;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SafeHttpRequestService {
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "host",
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailers",
            "transfer-encoding",
            "upgrade",
            "content-length"
    );

    private static final int DEFAULT_TIMEOUT_MS = 20000;
    private static final int DEFAULT_MAX_RESPONSE_BYTES = 2_000_000; // ~2MB

    // Simple in-memory rate limit placeholder (per user).
    // In production, replace with Redis/multi-instance safe limiter.
    private final ConcurrentHashMap<String, Deque<Long>> userTimestamps = new ConcurrentHashMap<>();
    private static final int RATE_LIMIT_MAX = 20;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public record KeyValue(String key, String value, boolean enabled) {}

    public record HttpSendResult(
            int statusCode,
            long durationMs,
            long responseSizeBytes,
            boolean truncated,
            String body,
            Map<String, String> headers
    ) {}

    public HttpSendResult send(
            String userEmail,
            String method,
            String url,
            List<KeyValue> headers,
            List<KeyValue> queryParams,
            String bodyType,
            String bodyRaw,
            List<KeyValue> bodyFields,
            Integer timeoutMs,
            Integer maxResponseBytes
    ) {
        String m = method == null ? "GET" : method.trim().toUpperCase(Locale.ROOT);
        if (m.isBlank()) m = "GET";

        enforceRateLimit(userEmail);

        URI uri = parseAndValidateUrl(url);
        URI finalUri = appendQueryParams(uri, queryParams);

        long start = System.currentTimeMillis();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(finalUri)
                .timeout(Duration.ofMillis(timeoutMs != null ? timeoutMs : DEFAULT_TIMEOUT_MS));

        // Headers
        if (headers != null) {
            for (KeyValue kv : headers) {
                if (kv == null || !kv.enabled) continue;
                if (kv.key == null || kv.key.isBlank()) continue;
                String headerKey = kv.key.trim();
                if (HOP_BY_HOP_HEADERS.contains(headerKey.toLowerCase(Locale.ROOT))) continue;
                if (headerKey.equalsIgnoreCase("authorization")) {
                    // Let existing auth be controlled by sandbox; do not forward requester auth.
                    continue;
                }
                if (kv.value == null) continue;
                builder.header(headerKey, kv.value);
            }
        }

        // Body
        BodyPublisher bodyPublisher = null;
        boolean hasBody = bodyRaw != null && !bodyRaw.isBlank();
        boolean hasBodyFields = bodyFields != null && !bodyFields.isEmpty();
        String bt = bodyType == null ? "raw" : bodyType.trim().toLowerCase(Locale.ROOT);

        if (!"GET".equals(m) && !"DELETE".equals(m) && !"HEAD".equals(m) && !"OPTIONS".equals(m)) {
            if ("json".equals(bt)) {
                String json = hasBody ? bodyRaw : (hasBodyFields ? "{}" : "{}");
                // Ensure default Content-Type if caller didn't set it.
                if (headers == null || headers.stream().noneMatch(h -> h.enabled && "content-type".equalsIgnoreCase(h.key))) {
                    builder.header("Content-Type", "application/json; charset=utf-8");
                }
                bodyPublisher = HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);
            } else if ("x-www-form-urlencoded".equals(bt) || "urlencoded".equals(bt)) {
                String form = buildUrlEncoded(bodyFields);
                if (headers == null || headers.stream().noneMatch(h -> h.enabled && "content-type".equalsIgnoreCase(h.key))) {
                    builder.header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
                }
                bodyPublisher = HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8);
            } else if ("form-data".equals(bt) || "multipart".equals(bt)) {
                String boundary = "----------------BugSecureBoundary" + UUID.randomUUID();
                Map<String, String> ct = Map.of("Content-Type", "multipart/form-data; boundary=" + boundary);
                if (headers == null || headers.stream().noneMatch(h -> h.enabled && "content-type".equalsIgnoreCase(h.key))) {
                    builder.header(ct.get("Content-Type").split(";", 2)[0], ct.get("Content-Type").split(";", 2)[1].trim());
                    builder.header("Content-Type", ct.get("Content-Type"));
                } else {
                    builder.header("Content-Type", ct.get("Content-Type"));
                }
                String mp = buildMultipartFormData(boundary, bodyFields);
                bodyPublisher = HttpRequest.BodyPublishers.ofString(mp, StandardCharsets.UTF_8);
            } else {
                // raw
                String raw = hasBody ? bodyRaw : (hasBodyFields ? buildUrlEncoded(bodyFields) : "");
                if (headers == null || headers.stream().noneMatch(h -> h.enabled && "content-type".equalsIgnoreCase(h.key))) {
                    builder.header("Content-Type", "text/plain; charset=utf-8");
                }
                bodyPublisher = HttpRequest.BodyPublishers.ofString(raw, StandardCharsets.UTF_8);
            }
        }

        if (bodyPublisher != null) {
            builder.method(m, bodyPublisher);
        } else {
            builder.method(m, HttpRequest.BodyPublishers.noBody());
        }

        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Network error while sending request: " + e.getMessage());
        }

        long durationMs = System.currentTimeMillis() - start;
        byte[] bytes = response.body() != null ? response.body() : new byte[0];
        int maxBytes = maxResponseBytes != null ? maxResponseBytes : DEFAULT_MAX_RESPONSE_BYTES;
        boolean truncated = bytes.length > maxBytes;
        byte[] finalBytes = truncated ? java.util.Arrays.copyOf(bytes, maxBytes) : bytes;
        String body = new String(finalBytes, StandardCharsets.UTF_8);
        if (truncated) {
            body += "\n...[truncated]";
        }

        Map<String, String> resHeaders = new HashMap<>();
        response.headers().map().forEach((k, v) -> {
            if (v == null || v.isEmpty()) return;
            resHeaders.put(k, String.join(",", v));
        });

        return new HttpSendResult(
                response.statusCode(),
                durationMs,
                bytes.length,
                truncated,
                body,
                resHeaders
        );
    }

    private void enforceRateLimit(String userEmail) {
        if (userEmail == null) userEmail = "anonymous";
        long now = System.currentTimeMillis();
        Deque<Long> dq = userTimestamps.computeIfAbsent(userEmail, k -> new ArrayDeque<>());
        synchronized (dq) {
            while (!dq.isEmpty() && (now - dq.peekFirst()) > RATE_LIMIT_WINDOW_MS) {
                dq.removeFirst();
            }
            if (dq.size() >= RATE_LIMIT_MAX) {
                throw new RuntimeException("Rate limit exceeded. Please slow down.");
            }
            dq.addLast(now);
        }
    }

    private URI parseAndValidateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new RuntimeException("URL is required.");
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URL format.");
        }

        if (uri.getScheme() == null || (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
            throw new RuntimeException("Only http/https URLs are allowed.");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new RuntimeException("URL host is required.");
        }
        if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
            throw new RuntimeException("User info in URL is not allowed.");
        }

        // SSRF/internal-call protection:
        // - block localhost and private/link-local ranges by resolving host
        // - if any resolved address is unsafe -> reject
        String host = uri.getHost();
        List<InetAddress> addrs = resolveHost(host);
        for (InetAddress addr : addrs) {
            if (addr.isAnyLocalAddress()
                    || addr.isLoopbackAddress()
                    || addr.isLinkLocalAddress()
                    || addr.isSiteLocalAddress()) {
                throw new RuntimeException("Blocked unsafe destination (internal/local).");
            }
        }
        return uri;
    }

    private List<InetAddress> resolveHost(String host) {
        try {
            return List.of(InetAddress.getAllByName(host));
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not resolve host.");
        }
    }

    private URI appendQueryParams(URI base, List<KeyValue> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) return base;

        String existing = base.getRawQuery();
        StringBuilder sb = new StringBuilder();
        if (existing != null && !existing.isBlank()) {
            sb.append(existing);
        }

        for (KeyValue kv : queryParams) {
            if (kv == null || !kv.enabled) continue;
            if (kv.key == null || kv.key.isBlank()) continue;
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '&') sb.append("&");
            sb.append(encodeQueryComponent(kv.key.trim()));
            sb.append("=");
            sb.append(encodeQueryComponent(kv.value == null ? "" : kv.value));
        }

        try {
            return new URI(
                    base.getScheme(),
                    base.getUserInfo(),
                    base.getHost(),
                    base.getPort(),
                    base.getPath(),
                    sb.length() > 0 ? sb.toString() : null,
                    base.getFragment()
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to build URL with query parameters.");
        }
    }

    private String buildUrlEncoded(List<KeyValue> fields) {
        if (fields == null || fields.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (KeyValue kv : fields) {
            if (kv == null || !kv.enabled) continue;
            if (kv.key == null || kv.key.isBlank()) continue;
            if (sb.length() > 0) sb.append("&");
            sb.append(encodeQueryComponent(kv.key.trim()));
            sb.append("=");
            sb.append(encodeQueryComponent(kv.value == null ? "" : kv.value));
        }
        return sb.toString();
    }

    private String buildMultipartFormData(String boundary, List<KeyValue> fields) {
        if (fields == null || fields.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (KeyValue kv : fields) {
            if (kv == null || !kv.enabled) continue;
            if (kv.key == null || kv.key.isBlank()) continue;

            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"")
                    .append(escapeQuotes(kv.key.trim()))
                    .append("\"\r\n\r\n");
            sb.append(kv.value == null ? "" : kv.value).append("\r\n");
        }
        sb.append("--").append(boundary).append("--\r\n");
        return sb.toString();
    }

    private String escapeQuotes(String s) {
        return s.replace("\"", "\\\"");
    }

    private String encodeQueryComponent(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}

