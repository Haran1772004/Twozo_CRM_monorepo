package com.tlc.email.server.internal.provider.gmail.model;

/**
 * <p>
 * Represents the format types available for Gmail messages.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum GmailFormat {

    FULL("full"),
    META_DATA("metadata");

    private final String name;

    public String getName() {
        return name;
    }

    GmailFormat(final String name) {
        this.name = name;
    }
}