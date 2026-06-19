package com.bugsecure.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "http_test_cases")
public class HttpTestCase {

    @Id
    private String id;

    @DBRef
    private User researcher;

    private String name;
    private String description;

    // Snapshot of request
    private String method;
    private String url;
    private String endpoint;
    private String fullUrlPreview;

    private List<HttpRequestLog.KeyValue> headers;
    private List<HttpRequestLog.KeyValue> queryParams;
    private String bodyType;
    private String bodyRaw;
    private List<HttpRequestLog.KeyValue> bodyFields;

    private Integer timeoutMs;
    private Integer maxResponseBytes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public HttpTestCase() {}

    public void setCreatedAtIfNew() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
    }

    public void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<HttpRequestLog.KeyValue> getHeaders() {
        return headers;
    }

    public void setHeaders(List<HttpRequestLog.KeyValue> headers) {
        this.headers = headers;
    }

    public List<HttpRequestLog.KeyValue> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(List<HttpRequestLog.KeyValue> queryParams) {
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

    public List<HttpRequestLog.KeyValue> getBodyFields() {
        return bodyFields;
    }

    public void setBodyFields(List<HttpRequestLog.KeyValue> bodyFields) {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

