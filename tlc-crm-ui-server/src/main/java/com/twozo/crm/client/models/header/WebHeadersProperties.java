package com.twozo.crm.client.models.header;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "twozo.web")
public class WebHeadersProperties {

    private Map<String, Map<String, String>> headers;

    public Map<String, Map<String, String>> getHeaders() {
        return headers;
    }

    public void setHeaders(final Map<String, Map<String, String>> headers) {
        this.headers = headers;
    }
}