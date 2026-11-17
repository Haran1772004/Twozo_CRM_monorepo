package com.tlc.notification.model;

/**
 * <p>
 * Enum that represents the token type
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public enum TokenType {

    SYNC_TOKEN("syncToken"),
    PAGE_TOKEN("pageToken");

    private final String name;

    TokenType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}