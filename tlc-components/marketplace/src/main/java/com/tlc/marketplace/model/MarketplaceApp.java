package com.tlc.marketplace.model;

import com.tlc.i18n.I18nKey;
import com.tlc.validator.TlcModel;

/**
 * Refers the model of {@code Marketplace}.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
public class MarketplaceApp implements TlcModel {

    private static final String LOGO_URL = "/marketplace/logo";

    private Long id;
    private String name;
    private I18nKey displayName;
    private I18nKey description;

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public I18nKey getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final I18nKey displayName) {
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public I18nKey getDescription() {
        return description;
    }

    public void setDescription(final I18nKey description) {
        this.description = description;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return null;
    }

    @Override
    public Object identity() {
        return null;
    }
}