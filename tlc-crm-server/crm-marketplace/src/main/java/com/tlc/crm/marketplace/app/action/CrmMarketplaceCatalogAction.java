package com.tlc.crm.marketplace.app.action;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.marketplace.app.api.model.CrmMarketplaceApp;
import com.tlc.crm.marketplace.app.api.model.AppInstallationInfo;
import com.tlc.crm.marketplace.app.api.model.converter.CrmMarketplaceAppConverter;
import com.tlc.crm.marketplace.app.fields.CrmMarketplaceFields;
import com.tlc.crm.marketplace.app.page.AppPageHandler;
import com.tlc.crm.marketplace.app.page.InstalledAppPageHandler;
import com.tlc.marketplace.fields.MarketplaceFields;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles secure access to the marketplace catalog within the CRM system.
 * Responsible for exposing available apps or services that can be discovered,
 * browsed, or installed by organizations.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/marketplace/catalog")
public class CrmMarketplaceCatalogAction extends CrmSecureAction {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 20;

    private final UserService crmUserService;
    private final CrmMarketplaceAppConverter crmMarketplaceAppConverter;

    public CrmMarketplaceCatalogAction() {
        this.crmUserService = UserService.getInstance();
        this.crmMarketplaceAppConverter = CrmMarketplaceAppConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Refers the request.
     * @param response Refers the response to be sent.
     */
    @Override
    public void sProcess(final CrmRequest request, final CrmResponse response) throws Exception {
        final long orgId = request.orgId();
        final long userId = request.userId();
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(MarketplaceFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(MarketplaceFields.DATA);

        switch (type) {
            case "getAll" -> response.put(MarketplaceFields.APPS, getAppList(orgId, data, request, response));
            case "installedApps" -> response.put(MarketplaceFields.APPS, fetchInstalledApps(orgId, userId, data, response));
        }
    }

    /**
     * Retrieves the list of marketplace apps available to the specified organization.
     *
     * @param orgId    The ID of the organization requesting the app list.
     * @param data     Optional JSON payload containing filters or query parameters.
     * @param request  The CRM request context.
     * @param response The CRM response context for writing errors or status if needed.
     * @return A {@link JsonArray} containing the list of available or matching apps.
     */
    private JsonArray getAppList(final long orgId, final JsonObject data,
                                 final CrmRequest request, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final int start = Math.max(data.optInt(MarketplaceFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(MarketplaceFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final String searchText = data.optString(MarketplaceFields.SEARCH_TEXT, "");
        final AppPageHandler appPageHandler = new AppPageHandler(orgId, start, limit, searchText);
        final long totalRecords = appPageHandler.getTotalRecordsCount();

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<CrmMarketplaceApp> crmMarketplaceApps = appPageHandler.fetchPageData().getData();

            if (Objects.nonNull(crmMarketplaceApps) && !crmMarketplaceApps.isEmpty()) {
                crmMarketplaceApps.forEach(marketplaceApp -> jsonArray.put(crmMarketplaceAppConverter.convert(orgId, request, marketplaceApp)));
            }
        }
        response.put(MarketplaceFields.TOTAL_RECORDS, totalRecords);
        response.put(MarketplaceFields.HAS_MORE, appPageHandler.hasMore());

        return jsonArray;
    }

    /**
     * Sends the list or data of apps already installed by the given organization and user.
     *
     * @param orgId    The ID of the organization.
     * @param userId   The ID of the user requesting the data.
     * @param data     Optional filters or request metadata.
     * @param response The CRM response context to write the installed apps data.
     * @return A {@link JsonArray} containing the list of installed apps.
     */
    private JsonArray fetchInstalledApps(final long orgId, final long userId, final JsonObject data, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final int start = Math.max(data.optInt(MarketplaceFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(MarketplaceFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final SortConfig sortConfig;

        if (data.containsKey(CrmMarketplaceFields.SORT)) {
            final JsonObject sortJson = data.getJsonObject(CrmMarketplaceFields.SORT);

            sortConfig = new SortConfig(sortJson.getString(CrmMarketplaceFields.FIELD), SortType.getSortType(sortJson.getString(MarketplaceFields.TYPE)));
        } else {
            sortConfig = new SortConfig(MarketplaceFields.INSTALLED_TIME, SortType.ASCENDING);
        }
        final InstalledAppPageHandler installedAppPageHandler = new InstalledAppPageHandler(orgId, start, limit, userId, sortConfig);
        final long totalRecords = installedAppPageHandler.getTotalRecordsCount();

        response.put(MarketplaceFields.TOTAL_RECORDS, totalRecords);
        response.put(MarketplaceFields.HAS_MORE, installedAppPageHandler.hasMore());

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<AppInstallationInfo> appInstallationInfos = installedAppPageHandler.fetchPageData().getData();

            if (Objects.nonNull(appInstallationInfos) && !appInstallationInfos.isEmpty()) {
                final Collection<Long> userIds = appInstallationInfos.stream().map(AppInstallationInfo::getUserId).collect(Collectors.toUnmodifiableSet());
                final Map<Long, CrmUser> userMap = crmUserService.getUsers(orgId, userIds).stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));

                for (final AppInstallationInfo appInstallationInfo : appInstallationInfos) {
                    final CrmUser crmUser = crmUserService.getUser(orgId, appInstallationInfo.getUserId());
                    final JsonObject jsonObject = crmMarketplaceAppConverter.convertInstalledApp(response, appInstallationInfo, userMap.get(crmUser.id()).getFullName());

                    jsonArray.put(jsonObject);
                }
            }
        }
        return jsonArray;
    }
}