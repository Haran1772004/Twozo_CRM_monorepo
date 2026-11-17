package com.tlc.crm.marketplace.app.api.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.i18n.I18nKey;

import com.tlc.marketplace.model.AppRating;
import com.tlc.marketplace.model.Category;
import com.tlc.marketplace.model.MarketplaceApp;
import com.tlc.marketplace.model.Pricing;

/**
 * Refers the model of {@code Marketplace}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class CrmMarketplaceApp extends MarketplaceApp {

    private static final String LOGO_URL = "/marketplace/logo";

    private Long id;
    private I18nKey description;
    private Long marketplaceAppId;
    private String publishedDate;
    private String latestReleasedDate;
    private String developedBy;
    private double version;
    private Pricing pricing;
    private Category category;
    private AppRating appRating;
    private Long installs;
    private transient JsonObject permission;
    private ApplicationScope applicationScope;

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getMarketplaceAppId() {
        return marketplaceAppId;
    }

    public void setMarketplaceAppId(final Long marketplaceAppId) {
        this.marketplaceAppId = marketplaceAppId;
    }

    public I18nKey getDescription() {
        return description;
    }

    public void setDescription(final I18nKey description) {
        this.description = description;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(final String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getLatestReleasedDate() {
        return latestReleasedDate;
    }

    public void setLatestReleasedDate(final String latestReleasedDate) {
        this.latestReleasedDate = latestReleasedDate;
    }

    public String getDevelopedBy() {
        return developedBy;
    }

    public void setDevelopedBy(final String developedBy) {
        this.developedBy = developedBy;
    }

    public double getVersion() {
        return version;
    }

    public void setVersion(final double version) {
        this.version = version;
    }

    public Pricing getPricing() {
        return pricing;
    }

    public void setPricing(final Pricing pricing) {
        this.pricing = pricing;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public AppRating getAppRating() {
        return appRating;
    }

    public void setAppRating(final AppRating appRating) {
        this.appRating = appRating;
    }

    public Long getInstalls() {
        return installs;
    }

    public void setInstalls(final Long installs) {
        this.installs = installs;
    }

    public String getLogo() {
        return String.join("/", LOGO_URL, id.toString());
    }

    public JsonObject getPermission() {
        return permission;
    }

    public void setPermission(final JsonObject permission) {
        this.permission = permission;
    }

    public ApplicationScope getInstallationType() {
        return applicationScope;
    }

    public void setInstallationType(final ApplicationScope applicationScope) {
        this.applicationScope = applicationScope;
    }

    @Override
    public Long id() {
        return id;
    }
}
