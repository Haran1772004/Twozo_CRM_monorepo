package com.twozo.crm.client.models;

import org.springframework.http.MediaType;

public record Asset(String path, byte[] data, MediaType mediaType) {
}
