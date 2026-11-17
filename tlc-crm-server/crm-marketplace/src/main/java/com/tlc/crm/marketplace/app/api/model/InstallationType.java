package com.tlc.crm.marketplace.app.api.model;

/**
 * <p>
 * An enum that contains the marketplace installation types
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public enum InstallationType {

    STANDALONE("STANDALONE"),
    VENDOR_DEPENDENT("VENDOR_DEPENDENT"),
    OAUTH_DRIVEN("OAUTH_DRIVEN");

    final String name;

    InstallationType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
