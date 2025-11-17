package io.kestra.plugin.n8n;

public enum ContentType {
    TEXT("text/plain"),
    JSON("application/json"),
    XML("application/xml"),
    BINARY("application/octet-stream");


    ContentType(String contentType) {
    }
}
