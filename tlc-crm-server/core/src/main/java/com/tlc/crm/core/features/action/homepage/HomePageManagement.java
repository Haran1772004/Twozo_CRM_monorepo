package com.tlc.crm.core.features.action.homepage;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.field.homepage.HomePageFields;

import com.tlc.crm.entity.fields.MenuFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.internal.DefaultFilter;
import com.tlc.crm.entity.internal.status.SettingsErrorCodes;
import com.tlc.crm.entity.list.filter.resolver.FilterValueResolver;
import com.tlc.crm.entity.manager.menu.MenuManager;
import com.tlc.crm.entity.models.menu.HomeMenuMetrics;
import com.tlc.crm.entity.models.menu.Menu;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.service.list.ProviderService;

import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.web.WebAction;

import java.util.Collection;

/**
 * <p>
 * Handles the web action related to Home page details
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@WebAction(path = "/menu/home/mgmt")
@SourceAuthorizer(needRelatedApiHandling = true, relatedApiTypes = SourceAuthorizer.RelatedApiType.HOME_PAGE, actions = Action.Type.ACCESS)
public class HomePageManagement extends AuthorizationController {

    private final SourceActivityManager sourceActivityManager;
    private final DealManager dealManager;
    private final FilterManager filterManager;
    private final ProviderService providerService;
    private final ContactManager contactManager;
    private final MenuManager menuManager;

    public HomePageManagement() {
        this.sourceActivityManager = SourceActivityManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.filterManager = FilterManager.getInstance();
        this.providerService = ProviderService.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.menuManager = MenuManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {

    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    public void handleMobileRequest(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(MenuFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(MenuFields.DATA);
        final long orgId = crmRequest.orgId();
        final long userId = crmRequest.userId();

        switch (type) {
            case "getOverdueSummary" -> crmResponse.put(HomePageFields.NEEDS_ATTENTION, getOverdueSummaryData(orgId, userId, data));
            case "keyMetrics" -> loadKeyMetricsData(orgId, userId, data, crmRequest.getI18NCRMResolutionAdapter(), crmResponse);
        }
    }

    /**
     * <p>
     * Loads the overdue summary data
     * </p>
     *
     * @param orgId       Represents the organisation Id
     * @param userId      Represents the user Id
     * @param requestJson Represents the data json
     * @return The needs attention data
     */
    private JsonObject getOverdueSummaryData(final Long orgId, final Long userId, final JsonObject requestJson) {
        final JsonObject dataJson = Json.object();
        final JsonObject activityJson = Json.object();
        final Long overdueActivitiesFilterId = filterManager.getFilterId(orgId, providerService.getIdBySource(Source.ACTIVITY), DefaultFilter.MY_OVERDUE_ACTIVITIES.name());

        activityJson.put(HomePageFields.COUNT, sourceActivityManager.getActivitiesCount(orgId, getFilterCriteriaList(orgId, userId, overdueActivitiesFilterId)));
        activityJson.put(HomePageFields.SOURCE, Source.ACTIVITY.getName());
        activityJson.put(HomePageFields.FILTER_ID, overdueActivitiesFilterId);

        dataJson.put(HomePageFields.OVERDUE_ACTIVITIES, activityJson);
        final JsonObject sourceJson = requestJson.getJsonObject(HomePageFields.SOURCE);
        final String dealSource = Source.DEAL.getName();

        if (sourceJson.getBoolean(dealSource)) {
            final JsonObject dealJson = Json.object();
            final Long overdueDealsFilterId = filterManager.getFilterId(orgId, providerService.getIdBySource(Source.DEAL), DefaultFilter.MY_OVERDUE_DEALS.name());

            dealJson.put(HomePageFields.COUNT, dealManager.getDealCount(orgId, getFilterCriteriaList(orgId, userId, overdueDealsFilterId)));
            dealJson.put(HomePageFields.SOURCE, dealSource);
            dealJson.put(HomePageFields.FILTER_ID, overdueDealsFilterId);

            dataJson.put(HomePageFields.OVERDUE_DEALS, dealJson);
        }
        return dataJson;
    }

    /**
     * <p>
     * Loads the Key metrics data
     * </p>
     *
     * @param orgId         Represents the organisation Id
     * @param userId        Represents the user Id
     * @param dataJson      Represents the data json
     */
    private void loadKeyMetricsData(final Long orgId, final Long userId, final JsonObject dataJson, final I18NCRMResolutionAdapter i18NCRMResolutionAdapter, final CrmResponse crmResponse) {
        final JsonObject sourceJson = dataJson.getJsonObject(HomePageFields.SOURCE);

        menuManager.getMenuIdByName(MenuFields.HOME).ifPresent(menuId -> {
            final Menu menu = menuManager.getMenu(orgId, userId, menuId);
            final Collection<Integer> metricsIds = menu.getConfig().getMetricsIds();

            crmResponse.put(MenuFields.MENU, getMenuData(i18NCRMResolutionAdapter, sourceJson, metricsIds));
            crmResponse.put(HomePageFields.METRICS, getMetricsData(orgId, userId, i18NCRMResolutionAdapter, metricsIds, sourceJson));
        });
        crmResponse.send();
    }

    /**
     * <p>
     * Gets the metrics data
     * </p>
     *
     * @param orgId                     Represents the organisation Id
     * @param userId                    Represents the user Id
     * @param i18NCRMResolutionAdapter  Represents the {@link I18NCRMResolutionAdapter}
     * @param metricsIds                Represents the metric ids
     * @param sourceJson                Represents the source json
     * @return The Metrics data
     */
    private JsonArray getMetricsData(final Long orgId, final Long userId, final I18NCRMResolutionAdapter i18NCRMResolutionAdapter,
            final Collection<Integer> metricsIds, final JsonObject sourceJson) {
        final JsonArray metricsArray = Json.array();

        metricsIds.forEach(metricsId -> {
            final HomeMenuMetrics homeMenuMetrics = HomeMenuMetrics.getById(metricsId.shortValue());
            final Source source = homeMenuMetrics.getSource();
            final JsonObject entityJson = Json.object();

            entityJson.put(MenuFields.NAME, i18NCRMResolutionAdapter.get(homeMenuMetrics.getDisplayName()));

            if (sourceJson.getBoolean(source.getName())) {

                switch (homeMenuMetrics) {
                    case DEAL_CLOSING_THIS_WEEK -> {
                        loadDealData(orgId, userId, entityJson, filterManager.getFilterId(orgId, providerService.getIdBySource(source), DefaultFilter.MY_DEALS_CLOSING_THIS_WEEK.name()));
                        metricsArray.put(entityJson);
                    }
                    case MY_OPEN_DEALS -> {
                        loadDealData(orgId, userId, entityJson, filterManager.getFilterId(orgId, providerService.getIdBySource(source), DefaultFilter.MY_OPEN_DEALS.name()));
                        metricsArray.put(entityJson);
                    }
                    case NEW_CONTACTS -> {
                        loadContactData(orgId, userId, entityJson, filterManager.getFilterId(orgId, providerService.getIdBySource(source), DefaultFilter.MY_NEW_CONTACTS.name()));
                        metricsArray.put(entityJson);
                    }
                    case RECENT_CONTACTS -> {
                        loadContactData(orgId, userId, entityJson, filterManager.getFilterId(orgId, providerService.getIdBySource(source), DefaultFilter.MY_RECENT_CONTACTS.name()));
                        metricsArray.put(entityJson);
                    }
                    default -> throw ErrorCode.get(SettingsErrorCodes.MENU_CONFIG_NOT_FOUND);
                }
            }
        });
        return metricsArray;
    }

    /**
     * <p>
     * Gets the metrics menu data
     * </p>
     *
     * @param i18NCRMResolutionAdapter Represents the {@link I18NCRMResolutionAdapter}
     * @param sourceJson               Represents the source json
     * @param metricsIds               Represents the metric ids
     * @return The metrics menu data
     */
    private JsonArray getMenuData(final I18NCRMResolutionAdapter i18NCRMResolutionAdapter, final JsonObject sourceJson, final Collection<Integer> metricsIds) {
        final JsonArray menuArray = Json.array();

        for (final HomeMenuMetrics homeMenuMetrics: HomeMenuMetrics.values()) {

            if (sourceJson.getBoolean(homeMenuMetrics.getSource().getName())) {
                final JsonObject menuJson = Json.object();
                final int id = homeMenuMetrics.getId();

                menuJson.put(MenuFields.NAME, i18NCRMResolutionAdapter.get(homeMenuMetrics.getDisplayName()));
                menuJson.put(MenuFields.ID, id);
                menuJson.put(MenuFields.SOURCE, homeMenuMetrics.getSource().getName());
                menuJson.put(MenuFields.IS_ENABLED, metricsIds.contains(id));
                menuArray.put(menuJson);
            }
        }
        return menuArray;
    }

    /**
     * <p>
     * Loads the deal key metrics data
     * </p>
     * @param orgId         Represents the organisation Id
     * @param userId        Represents the user Id
     * @param entityJson    Represents the entity json
     * @param filterId      Represents the filter Id
     */
    private void loadDealData(final Long orgId, final Long userId, final JsonObject entityJson, final Long filterId) {
        final Pair<Long, Monetary> dealCountWithMonetary = dealManager.getTotalDealCountAndValue(orgId, getFilterCriteriaList(orgId, userId, filterId));
        final Monetary monetary = dealCountWithMonetary.getValue();

        entityJson.put(HomePageFields.COUNT, dealCountWithMonetary.getKey());
        entityJson.put(HomePageFields.MONETARY, monetary.getValue());
        entityJson.put(HomePageFields.CODE, monetary.getCurrencyCode());
        entityJson.put(HomePageFields.SOURCE, Source.DEAL.getName());
        entityJson.put(HomePageFields.FILTER_ID, filterId);
    }

    /**
     * <p>
     * Loads the contact key metrics data
     * </p>
     * @param orgId         Represents the organisation Id
     * @param userId        Represents the user Id
     * @param entityJson    Represents the entity json
     * @param filterId      Represents the filter Id
     */
    private void loadContactData(final Long orgId, final Long userId, final JsonObject entityJson, final Long filterId) {
        entityJson.put(HomePageFields.COUNT, contactManager.getContactCount(orgId, getFilterCriteriaList(orgId, userId, filterId)));
        entityJson.put(HomePageFields.SOURCE, Source.CONTACT.getName());
        entityJson.put(HomePageFields.FILTER_ID, filterId);
    }

    /**
     * <p>
     * Gets the filter criteria list
     * </p>
     * @param orgId         Represents the organisation Id
     * @param userId        Represents the user Id
     * @param filterId      Represents the filter Id
     */
    private Collection<FilterCriteria> getFilterCriteriaList(final Long orgId, final Long userId, final Long filterId) {
        final Filter filter = filterManager.get(orgId, filterId);
        final Collection<FilterCriteria> criteriaList = filter.getCriteriaList();
        final FilterValueResolver valueResolver = new FilterValueResolver(orgId, userId, DefaultFilter.valueOf(filter.getAliasName()));

        criteriaList.forEach(valueResolver::resolve);

        return criteriaList;
    }
}