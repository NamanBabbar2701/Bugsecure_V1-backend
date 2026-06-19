package com.bugsecure.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "http_request_logs")
public class HttpRequestLog {

    @Id
    private String id;

    @DBRef
    private User researcher;

    private String method;
    private String url;
    private String endpoint;
    private String fullUrlPreview;

    private List<KeyValue> headers;
    private List<KeyValue> queryParams;

    private String bodyType; // json | form-data | x-www-form-urlencoded | raw
    private String bodyRaw;
    private List<KeyValue> bodyFields; // name/value for form-data or urlencoded

    private Integer timeoutMs;
    private Integer maxResponseBytes;

    // Response
    private Integer statusCode;
    private Long durationMs;
    private Long responseSizeBytes;
    private Boolean truncated;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private String error;

    private LocalDateTime createdAt;

    // ======================
    // Embedded Key-Value
    // ======================
    public static class KeyValue {
        private String key;
        private String value;
        private Boolean enabled;

        public KeyValue() {}

        public KeyValue(String key, String value, Boolean enabled) {
            this.key = key;
            this.value = value;
            this.enabled = enabled;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    public HttpRequestLog() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getResearcher() {
        return researcher;
    }

    public void setResearcher(User researcher) {
        this.researcher = researcher;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getFullUrlPreview() {
        return fullUrlPreview;
    }

    public void setFullUrlPreview(String fullUrlPreview) {
        this.fullUrlPreview = fullUrlPreview;
    }

    public List<KeyValue> getHeaders() {
        return headers;
    }

    public void setHeaders(List<KeyValue> headers) {
        this.headers = headers;
    }

    public List<KeyValue> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(List<KeyValue> queryParams) {
        this.queryParams = queryParams;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public String getBodyRaw() {
        return bodyRaw;
    }

    public void setBodyRaw(String bodyRaw) {
        this.bodyRaw = bodyRaw;
    }

    public List<KeyValue> getBodyFields() {
        return bodyFields;
    }

    public void setBodyFields(List<KeyValue> bodyFields) {
        this.bodyFields = bodyFields;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getMaxResponseBytes() {
        return maxResponseBytes;
    }

    public void setMaxResponseBytes(Integer maxResponseBytes) {
        this.maxResponseBytes = maxResponseBytes;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Long getResponseSizeBytes() {
        return responseSizeBytes;
    }

    public void setResponseSizeBytes(Long responseSizeBytes) {
        this.responseSizeBytes = responseSizeBytes;
    }

    public Boolean getTruncated() {
        return truncated;
    }

    public void setTruncated(Boolean truncated) {
        this.truncated = truncated;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAtIfNew() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}

