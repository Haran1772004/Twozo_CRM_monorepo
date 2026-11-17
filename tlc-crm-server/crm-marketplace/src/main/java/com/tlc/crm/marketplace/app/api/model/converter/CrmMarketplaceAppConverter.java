package com.tlc.crm.marketplace.app.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.marketplace.app.api.manager.CrmMarketplaceCatalogManager;
import com.tlc.crm.marketplace.app.api.model.CrmMarketplaceApp;
import com.tlc.crm.marketplace.app.api.model.AppInstallationInfo;

import com.tlc.marketplace.fields.MarketplaceFields;
import com.tlc.marketplace.model.AppRating;
import com.tlc.marketplace.model.Pricing;

import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

/**
 * Provides different ways to convert the data from / into {@link CrmMarketplaceApp}
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class CrmMarketplaceAppConverter {

    private final CrmMarketplaceCatalogManager crmMarketplaceCatalogManager;
    private final I18nService i18nService;

    private CrmMarketplaceAppConverter() {
        this.crmMarketplaceCatalogManager = CrmMarketplaceCatalogManager.getInstance();
        this.i18nService = I18nServiceProvider.get();
    }

    private static class Instance {
        private static final CrmMarketplaceAppConverter MARKETPLACE_APP_CONVERTER = new CrmMarketplaceAppConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link CrmMarketplaceAppConverter}
     * </p>
     *
     * @return The instance of {@link CrmMarketplaceAppConverter}
     */
    public static CrmMarketplaceAppConverter getInstance() {
        return Instance.MARKETPLACE_APP_CONVERTER;
    }

    /**
     * <p>
     * Convert the {@link AppInstallationInfo} into json
     * </p>
     *
     * @param response     Represents the {@link CrmResponse}
     * @param appInstallationInfo Represents the {@link AppInstallationInfo}
     * @return The {@link AppInstallationInfo} json data
     */
    public JsonObject convertInstalledApp(final CrmResponse response, final AppInstallationInfo appInstallationInfo, final String userName) {
        final JsonObject appJson = Json.object();
        final JsonObject jsonObject = Json.object();
        final CrmMarketplaceApp marketplaceApp = appInstallationInfo.getMarketPlaceApp();

        appJson.put(MarketplaceFields.ID, marketplaceApp.id());
        appJson.put(MarketplaceFields.NAME, response.resolveI18nString(marketplaceApp.getDisplayName()));
        appJson.put(MarketplaceFields.DESCRIPTION, response.resolveI18nString(marketplaceApp.getDescription()));
        appJson.put(MarketplaceFields.LOGO, marketplaceApp.getLogo());

        jsonObject.put(MarketplaceFields.APP, appJson);
        jsonObject.put(MarketplaceFields.INSTALLED_BY, userName);
        jsonObject.put(MarketplaceFields.INSTALLED_TIME, appInstallationInfo.getInstalledTime());

        return jsonObject;
    }

    /**
     * Converts a {@link CrmMarketplaceApp} instance into a structured {@link JsonObject}
     * suitable for response output or client-side rendering.
     *
     * @param orgId             Refers the organization id.
     * @param request           The CRM request context.
     * @param crmMarketplaceApp The app to be converted into JSON format.
     * @return A {@link JsonObject} representing the app.
     */
    public JsonObject convert(final long orgId, final CrmRequest request, final CrmMarketplaceApp crmMarketplaceApp) {
        final JsonObject jsonObject = Json.object();
        final JsonObject ratingJson = Json.object();
        final AppRating appRating = crmMarketplaceApp.getAppRating();
        final I18nResolver i18nResolver = i18nService.getResolver(request.locale());
        final Long appId = crmMarketplaceApp.id();

        ratingJson.put(MarketplaceFields.TOTAL_RATINGS, appRating.getTotalRatings());
        ratingJson.put(MarketplaceFields.RATING, appRating.getRating());

        jsonObject.put(MarketplaceFields.ID, appId);
        jsonObject.put(MarketplaceFields.NAME, i18nResolver.get(crmMarketplaceApp.getDisplayName()));
        jsonObject.put(MarketplaceFields.DESCRIPTION, i18nResolver.get(crmMarketplaceApp.getDescription()));
        jsonObject.put(MarketplaceFields.LOGO, crmMarketplaceApp.getLogo());
        jsonObject.put(MarketplaceFields.RATING, ratingJson);
        jsonObject.put(MarketplaceFields.INSTALLS, crmMarketplaceApp.getInstalls());
        jsonObject.put(MarketplaceFields.PRICING, i18nResolver.get(crmMarketplaceApp.getPricing().getDisplayName()));
        jsonObject.put(MarketplaceFields.IS_INSTALLED, crmMarketplaceCatalogManager.isAppInstalled(new AppInstallationInfo(appId, orgId, request.userId(), crmMarketplaceApp.getInstallationType())));

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link CrmMarketplaceApp} into summary json data
     * </p>
     *
     * @param orgId                 Represents the organization Id
     * @param appId                 Represents the app Id
     * @param request               Represents the {@link CrmRequest}
     * @param crmMarketplaceApp     Represents the {@link CrmMarketplaceApp}
     * @return The {@link CrmMarketplaceApp} summary json data
     */
    public JsonObject convertSummaryJson(final long orgId, final long appId, final CrmRequest request, final CrmMarketplaceApp crmMarketplaceApp) {
        final JsonObject jsonObject = Json.object();
        final JsonObject ratingJson = Json.object();
        final AppRating appRating = crmMarketplaceApp.getAppRating();
        final I18nResolver i18nResolver = request.i18nResolver();

        ratingJson.put(MarketplaceFields.TOTAL_RATINGS, appRating.getTotalRatings());
        ratingJson.put(MarketplaceFields.RATING, appRating.getRating());

        jsonObject.put(MarketplaceFields.ID, crmMarketplaceApp.id());
        jsonObject.put(MarketplaceFields.NAME, i18nResolver.get(crmMarketplaceApp.getDisplayName()));
        jsonObject.put(MarketplaceFields.DESCRIPTION, i18nResolver.get(crmMarketplaceApp.getDescription()));
        jsonObject.put(MarketplaceFields.LOGO, crmMarketplaceApp.getLogo());
        jsonObject.put(MarketplaceFields.RATING, ratingJson);
        jsonObject.put(MarketplaceFields.CATEGORY, i18nResolver.get(crmMarketplaceApp.getCategory().getDisplayName()));
        jsonObject.put(MarketplaceFields.INSTALLS, crmMarketplaceApp.getInstalls());
        jsonObject.put(MarketplaceFields.PUBLISHED_DATE, crmMarketplaceApp.getPublishedDate());
        jsonObject.put(MarketplaceFields.LATEST_RELEASED_DATE, crmMarketplaceApp.getLatestReleasedDate());
        jsonObject.put(MarketplaceFields.DEVELOPED_BY, crmMarketplaceApp.getDevelopedBy());
        jsonObject.put(MarketplaceFields.VERSION, crmMarketplaceApp.getVersion());
        jsonObject.put(MarketplaceFields.IS_INSTALLED, crmMarketplaceCatalogManager.isAppInstalled(new AppInstallationInfo(appId, orgId, request.userId(), crmMarketplaceApp.getInstallationType())));
        final Pricing pricing = crmMarketplaceApp.getPricing();

        jsonObject.put(MarketplaceFields.PRICING, i18nResolver.get(pricing.getDisplayName()));

        if (pricing.hasTrialPeriod()) {
            jsonObject.put(MarketplaceFields.TRY_FOR_FREE, true);
        }

        return jsonObject;
    }
}