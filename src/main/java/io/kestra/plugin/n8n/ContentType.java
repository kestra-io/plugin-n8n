package io.kestra.plugin.n8n;

public enum ContentType {
    TEXT("text/plain"),
    JSON("application/json"),
    XML("application/xml"),
    BINARY("application/octet-stream");

    private final String mimeType;

    ContentType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }
}
