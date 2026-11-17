package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.keys.view.ViewKeys;
import com.tlc.analytics.config.model.view.layout.DashboardLayout;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.listeners.Callback;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.api.analytics.converter.layout.CRMReportsLayout;
import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.clone.AnalyticsCopyHandler;
import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.clone.DashboardCopyConfigBuilder;
import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.move.AnalyticsTransferManager;
import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.move.CRMDashboardLayoutTransferHandler;
import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.trash.CRMDashboardTrashBuilder;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMFavourite;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.core.sql.resource.CRMFAVOURITEVIEW;

import com.tlc.i18n.I18nResolver;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Criteria;

import com.tlc.web.client.event.EventCallback;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * {@link CRMDashboardAPI} represents an implementation of a dashboard request
 * {@link CRMReportsLayout} interface. This class contains the view request category.
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
class CRMDashboardAPI extends CRMAbstractDashboardConverter implements CRMReportsLayout<DashboardLayout> {
    private final CRMViewManagerAPI crmViewManagerAPI;
    private final CRMReportsAPI crmReportsAPI;
    private final DashboardCopyConfigBuilder dashboardCopyConfigBuilder;
    private final AnalyticsCopyHandler analyticsCopyHandler;
    private final AnalyticsTransferManager analyticsTransferManager;
    private final CRMDashboardLayoutTransferHandler crmDashboardLayoutTransferHandler;
    private final UserService userService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final OrganizationService organizationService;
    private final CRMDashboardTrashBuilder crmDashboardTrashBuilder;
    private final CRMViewConfigManager<CRMDashboardDetails> crmDashboardManager;
    private final ReportModularFieldService reportModularFieldService;

    private CRMDashboardAPI() {
        crmViewManagerAPI = CRMViewManagerAPI.INSTANCE();
        crmReportsAPI = CRMReportsAPI.getInstance();
        dashboardCopyConfigBuilder = DashboardCopyConfigBuilder.INSTANCE();
        analyticsCopyHandler = AnalyticsCopyHandler.getInstance();
        analyticsTransferManager = AnalyticsTransferManager.getInstance();
        crmDashboardLayoutTransferHandler = CRMDashboardLayoutTransferHandler.getInstance();
        userService = UserService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        this.crmDashboardTrashBuilder = CRMDashboardTrashBuilder.getInstance();
        this.crmDashboardManager = crmViewManagerAPI.getCRMDashboardManager();
        this.reportModularFieldService = ReportModularFieldService.getInstance();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMDashboardAPI.class);

    private static class Instance {
        private static final CRMDashboardAPI INSTANCE = new CRMDashboardAPI();
    }

    public static CRMDashboardAPI INSTANCE() {
        return CRMDashboardAPI.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     */
    @Override
    public final void create(final @NonNull AnalyticsRequest analyticsRequest,
                             final @NonNull JsonObject request,
                             final @NonNull I18nResolver i18nResolver,
                             final @NonNull ActionType actionType) {

        try {
            final Long userId = analyticsRequest.userId();
            final Long currentTime = System.currentTimeMillis();
            final Long orgId = analyticsRequest.orgId();
            final DataContainer dataContainer = DataContainer.create();
            final DashboardLayout anaDashboardLayout = createDashboardLayout(analyticsRequest, request, i18nResolver, dataContainer);
            final String dashboardName = request.getString(CRMDashboardKeys.DASHBOARD_NAME);
            final String description = request.optString(CommonKeys.DESCRIPTION, "");
            final CRMDashboardDetails crmDashboardDetails = CRMDashboardDetails.builder()
                    .reportsDashboardId(anaDashboardLayout.id())
                    .name(dashboardName)
                    .createdBy(userId)
                    .createdTime(currentTime)
                    .modifiedBy(userId)
                    .modifiedTime(currentTime)
                    .isCurated(false)
                    .description(description)
                    .markAsTrash(false)
                    .orgId(orgId)
                    .build();

            crmViewManagerAPI.getCRMDashboardManager()
                    .loadDataIntoContainer(List.of(crmDashboardDetails), dataContainer);
            LOGGER.info("CRM Analytics: Successfully loaded values into the container.");
            final Long crmDashboardId = crmDashboardDetails.id();

            crmReportsAPI.getDashboardVisibility()
                    .createVisibility(orgId, analyticsRequest.userId(), crmDashboardId, dataContainer);

            try {
                crmViewManagerAPI.getCRMDashboardManager()
                        .getOrgDataStore(orgId).commitChanges(dataContainer);
                LOGGER.info("CRM Analytics: Successfully committed values from dataContainer for orgId: {} and DashboardId: {} ", orgId, crmDashboardId);
            } catch (Exception error) {
                LOGGER.error("CRM Analytics: Failed to commit values from dataContainer for orgId: {}. Reason: {}", orgId, error);
                throw ErrorCode.get(AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed");
            }

            LOGGER.info("CRM Dashboard created successfully Dashboard ID: {}", crmDashboardId);

            sendResponse(analyticsRequest, getDashboardResponse(Collections.emptyList(), new HashSet<>(), Collections.emptyList(),
                    actionType, analyticsRequest, request, crmDashboardDetails, anaDashboardLayout, i18nResolver));
        } catch (Exception error) {
            LOGGER.error("Unable to perform dashboard creation reason:", error);

            throw ErrorCode.get(AnalyticsErrorCodes.CREATE_ACTION_FAILED,
                    "i18n.analytics.create.action.failed");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     */
    @Override
    public final void update(final @NonNull AnalyticsRequest analyticsRequest,
                             final @NonNull JsonObject request,
                             final @NonNull I18nResolver i18nResolver,
                             final @NonNull ActionType actionType) {
        final Long orgId = analyticsRequest.orgId();
        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        reportModularFieldService.getByOrgId(orgId, new Callback<>() {

            @Override
            public void onSuccess(final Map<Long, ReportModularField> reportFieldMap) {
                try {
                    final Long dashboardId = request.getLong(CRMReportKeys.DASHBOARD_ID);
                    final CRMDashboardDetails crmDashboardDetails = crmViewManagerAPI.getCRMDashboardManager()
                            .get(orgId, dashboardId);
                    final JsonObject response = updateDashboard(analyticsRequest, crmDashboardDetails, request, i18nResolver, actionType, reportFieldMap);
                    response.forEach(crmResponse::put);
                    crmResponse.send();
                } catch (final Exception ex) {
                    LOGGER.error("CRM Analytics: Dashboard update failed for orgId: {}", orgId, ex);
                    crmResponse.setErrorCode(ErrorCode.get(AnalyticsErrorCodes.DASHBOARD_GET_ACTION_FAILED, "i18n.analytics.dashboard.fetch.action.failed"));
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.error("CRM Analytics: getByOrgId callback failed for dashboard update, orgId: {}", orgId, cause);
                crmResponse.setErrorCode(ErrorCode.get(AnalyticsErrorCodes.DASHBOARD_GET_ACTION_FAILED, "i18n.analytics.dashboard.fetch.action.failed"));
                crmResponse.send();
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     */
    @Override
    public final void processDashboardResponse(final @NonNull AnalyticsRequest analyticsRequest,
                                               final @NonNull JsonObject request,
                                               final @NonNull I18nResolver i18nResolver,
                                               final @NonNull ActionType actionType) {
        try {
            final Long dashboardId = request.getLong(CRMReportKeys.DASHBOARD_ID);

            final CRMDashboardDetails crmDashboardDetails = crmViewManagerAPI.getCRMDashboardManager()
                    .get(analyticsRequest.orgId(), dashboardId);
            final Collection<Long> userIds = new HashSet<>();
            final JsonObject jsonObject = getDashboardResponse(userIds, new HashSet<>(), analyticsRequest, actionType, request, crmDashboardDetails, i18nResolver);

            if (userIds.isEmpty()) {
                sendResponse(analyticsRequest, jsonObject);
            } else {
                fetchAndDispatchUsers(userIds, jsonObject, analyticsRequest);
            }
        } catch (Exception error) {
            LOGGER.error("Unable to get dashboard reason:", error);
            throw ErrorCode.get(AnalyticsErrorCodes.DASHBOARD_GET_ACTION_FAILED,
                    "i18n.analytics.dashboard.fetch.action.failed");
        }
    }

    /**
     * <p>
     * Fetches user details based on the provided user IDs and dispatches the resolved data
     * </p>
     *
     * @param userIds          A {@link Collection} of user IDs to be processed.
     * @param jsonObject       A {@link JsonObject} representing the data structure to be enriched with user details.
     * @param analyticsRequest An {@link AnalyticsRequest} containing request-related details such as organization ID and CRM response.
     */
    private void fetchAndDispatchUsers(final Collection<Long> userIds,
                                       final JsonObject jsonObject,
                                       final AnalyticsRequest analyticsRequest) {
        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        crmResponse.convertIntoAsync();
        final Long orgId = analyticsRequest.orgId();
        final Collection<CrmUser> crmUsers = crmUserService.getUsers(orgId, userIds);
        final Long iamOrgId = organizationService.getIamOrgId(orgId);
        final Collection<Long> iamUserIds = crmUsers.stream()
                .map(CrmUser::getIamId)
                .collect(Collectors.toUnmodifiableSet());

        final EventCallback<Collection<User>> usersCallBack = new EventCallback<>() {
            @Override
            public void onSuccess(final Collection<User> users) {
                final Map<Long, User> userMap = users.stream()
                        .collect(Collectors.toMap(User::id,
                                Function.identity()));

                for (final CrmUser crmUser : crmUsers) {
                    crmUser.setUser(userMap.get(crmUser.getIamId()));
                }
                resolveUserFields(crmUsers, jsonObject);
                jsonObject.forEach(crmResponse::put);

                crmResponse.send();
            }

            @Override
            public void onFailure(final Throwable throwable) {
                crmResponse.setErrorCode(ErrorCode.get(CoreErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                crmResponse.send();
            }
        };
        userService.fetchUsers(iamOrgId, iamUserIds, usersCallBack);
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     */
    @Override
    public final void delete(final @NonNull AnalyticsRequest analyticsRequest,
                             final @NonNull JsonObject request,
                             final @NonNull I18nResolver i18nResolver) {
        final JsonObject jsonObject = Json.object();
        final Long dashboardId = request.getLong(CRMReportKeys.DASHBOARD_ID);
        final Long orgId = analyticsRequest.orgId();

        try {
            final CRMViewConfigManager<CRMDashboardDetails> crmViewConfigManager = crmViewManagerAPI.getCRMDashboardManager();
            final CRMDashboardDetails crmDashboardDetails = crmViewConfigManager.get(orgId, dashboardId);

            crmDashboardDetails.setOrgId(orgId);
            crmViewConfigManager.delete(crmDashboardDetails);

            LOGGER.info("CRM Analytics: Dashboard deleted successfully. dashboard ID: {}", dashboardId);
            jsonObject.put(ViewKeys.MESSAGE, i18nResolver.get("i18n.analytics.dashboard.deleted.successfully"));

            sendResponse(analyticsRequest, jsonObject);
        } catch (Exception exception) {
            LOGGER.error("CRM Analytics: Failed to delete the dashboard. org ID: {}", orgId, exception);
            throw ErrorCode.get(AnalyticsErrorCodes.DASHBOARD_DELETE_ACTION_FAILED,
                    "i18n.analytics.dashboard.delete.action.failed");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     */
    @Override
    public final void trash(final @NonNull AnalyticsRequest analyticsRequest,
                            final @NonNull JsonObject request,
                            final @NonNull I18nResolver i18nResolver) {
        final JsonObject jsonObject = Json.object();
        final Long dashboardId = request.getLong(CRMReportKeys.DASHBOARD_ID);
        final Long orgId = analyticsRequest.orgId();

        try {
            final CRMDashboardDetails crmDashboardDetails = crmViewManagerAPI.getCRMDashboardManager()
                    .get(orgId, dashboardId);

            crmViewManagerAPI.getCRMDashboardManager()
                    .update(crmDashboardTrashBuilder.buildDashboardDetails(true, orgId, analyticsRequest.userId(), crmDashboardDetails));
            LOGGER.info("CRM Analytics: Dashboard successfully moved to trash. orgId: {} and dashboardId: {}", orgId, dashboardId);
            jsonObject.put(ViewKeys.MESSAGE, i18nResolver.get("i18n.analytics.dashboard.move.to.trash.successfully"));
        } catch (Exception exception) {
            LOGGER.error("CRM Analytics: Failed to move the dashboard to trash. Dashboard ID: {}", dashboardId, exception);
            jsonObject.put(ViewKeys.MESSAGE, i18nResolver.get("i18n.analytics.dashboard.move.to.trash.failed"));
        }

        sendResponse(analyticsRequest, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     * @return The {@link DashboardLayout} details
     */
    @NonNull
    @Override
    public final DashboardLayout apply(final @NonNull AnalyticsRequest analyticsRequest,
                                       final @NonNull JsonObject request,
                                       final @NonNull I18nResolver i18nResolver) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     */
    @Override
    public final void favourite(final @NonNull AnalyticsRequest analyticsRequest,
                                final @NonNull JsonObject request,
                                final @NonNull I18nResolver i18nResolver) {
        final JsonObject jsonObject = Json.object();
        final Long userId = analyticsRequest.userId();
        final Long orgId = analyticsRequest.orgId();
        final Long dashboardId = request.getLong(CRMDashboardKeys.DASHBOARD_ID);
        final boolean isFavouriteView = request.optBoolean(CRMDashboardKeys.IS_FAVOURITE, false);
        final CRMViewConfigManager<CRMFavourite> crmFavouriteManager = crmViewManagerAPI.getCRMFavouriteManager();

        try {
            if (isFavouriteView) {
                final CRMFavourite crmFavourite = CRMFavourite.builder()
                        .userId(userId)
                        .dashboardId(dashboardId)
                        .orgId(orgId)
                        .build();

                crmFavouriteManager.create(crmFavourite);
                LOGGER.info("CRM Analytics: Dashboard has been successfully added to favorites. Dashboard ID: {}", dashboardId);
                jsonObject.put(ViewKeys.MESSAGE, i18nResolver.get("i18n.crm.analytics.dashboard.added.favourite.successfully"));
            } else {
                final Collection<CRMFavourite> crmFavourite = crmFavouriteManager.getAll(orgId,
                        Criteria.eq(crmFavouriteManager.getTable()
                                        .getColumn(CRMFAVOURITEVIEW.CRM_DASHBOARD_ID),
                                dashboardId));

                crmFavouriteManager.delete(orgId, crmFavourite);
                LOGGER.info("CRM Analytics: Dashboard has been successfully removed to favorites. Dashboard ID: {}", dashboardId);
                jsonObject.put(ViewKeys.MESSAGE, i18nResolver.get("i18n.crm.analytics.dashboard.removed.favourite.successfully"));
            }
        } catch (Exception exception) {
            LOGGER.error("CRM Analytics: Failed to add dashboard layout to favorites for orgId: {}", orgId, exception);
            throw ErrorCode.get(AnalyticsErrorCodes.FAVORITES_ACTION_FAILED,
                    "i18n.analytics.favourite.action.failed");
        }

        sendResponse(analyticsRequest, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     */
    @Override
    public final void restore(final @NonNull AnalyticsRequest analyticsRequest,
                              final @NonNull JsonObject request,
                              final @NonNull I18nResolver i18nResolver) {
        final JsonObject jsonObject = Json.object();
        final Long orgId = analyticsRequest.orgId();
        final Long dashboardId = request.getLong(CRMReportKeys.DASHBOARD_ID);

        try {
            final CRMDashboardDetails crmDashboardDetails = crmViewManagerAPI.getCRMDashboardManager()
                    .get(orgId, dashboardId);

            crmDashboardManager.update(crmDashboardTrashBuilder.buildDashboardDetails(false, orgId, analyticsRequest.userId(), crmDashboardDetails));
            LOGGER.info("CRM Analytics: Dashboard successfully restored. orgId: {} and dashboardId: {}", orgId, dashboardId);
            jsonObject.put(ViewKeys.MESSAGE, i18nResolver.get("i18n.analytics.dashboard.restore.successfully"));
        } catch (Exception exception) {
            LOGGER.error("CRM Analytics: Failed to restore the dashboard. Dashboard ID: {}", dashboardId, exception);
            jsonObject.put(ViewKeys.MESSAGE, i18nResolver.get("i18n.analytics.dashboard.restore.failed"));
        }

        sendResponse(analyticsRequest, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     */
    @Override
    public final void share(final @NonNull AnalyticsRequest analyticsRequest,
                            final @NonNull JsonObject request,
                            final @NonNull I18nResolver i18nResolver) {
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     */
    @Override
    public final void clone(final @NonNull AnalyticsRequest analyticsRequest,
                            final @NonNull JsonObject request,
                            final @NonNull I18nResolver i18nResolver) {
        final Long orgId = analyticsRequest.orgId();
        final JsonObject response = Json.object();
        final Long userId = analyticsRequest.userId();
        final DataContainer dataContainer = DataContainer.create();
        try {
            analyticsCopyHandler.cloneDashboard(dashboardCopyConfigBuilder.getDashboardCopyConfig(orgId,
                            request,
                            dataContainer,
                            userId),
                    dataContainer);
            crmViewManagerAPI.getCRMANAViewManager().getOrgDataStore(orgId).commitChanges(dataContainer);
            response.put(ViewKeys.MESSAGE, i18nResolver.get("i18n.analytics.dashboard.cloned.successfully"));
            LOGGER.info("CRM Analytics: Successfully cloned dashboard layout for orgId: {} and dashboardId: {}", orgId, request.getLong(CRMReportKeys.DASHBOARD_ID));
        } catch (Exception exception) {
            LOGGER.error("CRM Analytics: failed to clone dashboard layout for orgId: {}", orgId, exception);
            throw ErrorCode.get(AnalyticsErrorCodes.CLONE_ACTION_FAILED,
                    "i18n.analytics.clone.action.failed");
        }

        sendResponse(analyticsRequest, response);
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     */
    @Override
    public final void move(final @NonNull AnalyticsRequest analyticsRequest,
                           final @NonNull JsonObject request,
                           final @NonNull I18nResolver i18nResolver) {
        final JsonObject jsonObject = Json.object();
        final Long sourceDashboardId = request.getLong(CRMDashboardKeys.SOURCE_DASHBOARD_ID);

        try {
            analyticsTransferManager.move(crmDashboardLayoutTransferHandler.getEntityConfig(analyticsRequest.orgId(), request));
            LOGGER.info("CRM Analytics: Widget has been moved successfully to another dashboard. Target Dashboard ID: {}", sourceDashboardId);
            jsonObject.put(ViewKeys.MESSAGE, i18nResolver.get("i18n.crm.analytics.dashboard.moved.successfully"));
        } catch (Exception exception) {
            LOGGER.error("CRM Analytics: Widget move operation failed. Source dashboard ID: {}", sourceDashboardId, exception);
            jsonObject.put(ViewKeys.MESSAGE, i18nResolver.get("i18n.crm.analytics.dashboard.move.failed"));
        }

        sendResponse(analyticsRequest, jsonObject);
    }

    /**
     * <p>
     * Populates the CRM response with the given JSON data and sends the response.
     * </p>
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} containing the CRM response.
     * @param jsonObject       The {@link JsonObject} containing the response data to be sent.
     */
    private void sendResponse(final AnalyticsRequest analyticsRequest,
                              final JsonObject jsonObject) {
        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        jsonObject.forEach(crmResponse::put);
        crmResponse.send();
    }
}
