package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.keys.view.filter.FilterFieldKeys;
import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.analytics.config.model.view.ViewType;
import com.tlc.analytics.config.model.view.layout.DashboardLayout;

import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.KPIView;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.entity.share.VisibilityPermission;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMChildViewKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.general.analytics.AnalyticsServiceAPI;
import com.tlc.crm.core.api.analytics.integ.general.analytics.builders.CRMReportsBuilder;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.DashboardLayoutPosition;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.DashboardLayoutConfig;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMANAViewMapper;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.api.analytics.permission.DashboardPermissionService;
import com.tlc.crm.core.api.analytics.permission.handler.AnalyticsPermissionHandler;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.crm.core.sql.resource.DASHBOARDLAYOUTPOSITION;
import com.tlc.i18n.I18nResolver;

import com.tlc.sql.common.api.DataContainer;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.validator.TlcModel;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides an abstract implementation for handling the creation, update, and retrieval of dashboard
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
abstract class CRMAbstractDashboardConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMAbstractDashboardConverter.class);

    private final CRMReportsAPI crmReportsAPI;
    private final AnalyticsServiceAPI analyticsServiceAPI;
    private final CRMViewManagerAPI crmViewManagerAPI;
    private final CRMViewConfigManager<DashboardLayoutPosition> dashboardLayoutPositionManager;
    private final AnalyticsPermissionHandler analyticsPermissionHandler;
    private final CRMViewConfigAPI<KPIView> crmKPIViewConfigAPI;
    private final CRMViewConfigAPI<ChartView> crmChartViewConfigAPI;
    private final CRMReportsBuilder crmReportsBuilder;

    protected CRMAbstractDashboardConverter() {
        crmReportsAPI = CRMReportsAPI.getInstance();
        analyticsServiceAPI = AnalyticsServiceAPI.INSTANCE();
        crmViewManagerAPI = CRMViewManagerAPI.INSTANCE();
        dashboardLayoutPositionManager = crmViewManagerAPI.getDashboardLayoutManager();
        analyticsPermissionHandler = AnalyticsPermissionHandler.getInstance();
        crmKPIViewConfigAPI = crmReportsAPI.getKPIConfigAPI();
        crmChartViewConfigAPI = crmReportsAPI.getChartConfigAPI();
        crmReportsBuilder = CRMReportsBuilder.INSTANCE();
    }

    /**
     * <p>
     * Creates the {@link DashboardLayout} based on the {@link JsonObject}
     * </p>
     *
     * @param anaRequest    Represents the {@link AnalyticsRequest}
     * @param request       Holds the {@link JsonObject}
     * @param i18nResolver  Represents the {@link I18nResolver}
     * @param dataContainer Holds the rows that will get committed
     * @return The {@link DashboardLayout}
     */
    protected final @NonNull DashboardLayout createDashboardLayout(final AnalyticsRequest anaRequest,
                                                                   final JsonObject request,
                                                                   final I18nResolver i18nResolver,
                                                                   final DataContainer dataContainer) {
        final Long accessTime = System.currentTimeMillis();
        final Long orgId = anaRequest.orgId();
        final Long userId = anaRequest.userId();

        LOGGER.info("CRM Analytics: Dashboard create initiated.");

        final ViewDetails dashboardViewDetails = crmReportsBuilder.getViewDetails(orgId, userId, userId, accessTime,
                accessTime, ViewType.DASHBOARD.getTypeCode(),
                request.getString(CRMDashboardKeys.DASHBOARD_NAME), request.optString(CommonKeys.DESCRIPTION, ""),
                false);

        final DashboardLayout dashboardLayout = crmReportsBuilder.getDashboardBuilder(orgId, dashboardViewDetails);

        try {
            analyticsServiceAPI.getDashboardLayout().loadDataIntoContainer(List.of(dashboardLayout), dataContainer);
        } catch (Exception error) {
            LOGGER.error("CRM Analytics: Failed to load dashboard layout into dataContainer for orgId: {}", orgId, error);
            throw ErrorCode.get(AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed");
        }

        return dashboardLayout;
    }

    /**
     * <p>
     * Updates the {@link DashboardLayout} based on the request {@link JsonObject}
     * </p>
     *
     * @param anaRequest          Represents the {@link AnalyticsRequest}
     * @param crmDashboardDetails Represents the {@link CRMDashboardDetails}
     * @param request             Holds the request {@link JsonObject}
     * @param i18nResolver        Represents the {@link I18nResolver}
     * @return The updated {@link JsonObject}
     */
    protected final @NonNull JsonObject updateDashboard(final AnalyticsRequest anaRequest,
                                                        final CRMDashboardDetails crmDashboardDetails,
                                                        final JsonObject request,
                                                        final I18nResolver i18nResolver,
                                                        final ActionType actionType,
                                                        final Map<Long, ReportModularField> reportFieldMap) throws Exception {
        final DataContainer dataContainer = DataContainer.create();
        final Long anaDashboardId = crmDashboardDetails.getReportsDashboardId();
        final Long orgId = anaRequest.orgId();
        final Long crmDashboardId = crmDashboardDetails.id();
        LOGGER.info("CRM Analytics: Dashboard update initiated. Dashboard Id: {}", crmDashboardId);

        final DashboardLayout anaDashboard = getDashboardLayout(orgId, anaDashboardId);
        final ViewDetails viewDetails = anaDashboard.getViewDetails();
        final int viewTypeId = ViewType.getViewTypeId(ViewType.DASHBOARD.name());
        final Long dashboardId = request.getLong(CRMReportKeys.DASHBOARD_ID);
        final String newDashboardName = request.getString(CRMDashboardKeys.DASHBOARD_NAME);
        final String newDescription = request.optString(CRMDashboardKeys.DESCRIPTION, StringUtil.EMPTY_STRING);
        final Long getCreatedBy = viewDetails.getCreatedBy();
        final Long getCreatedTime = viewDetails.getCreatedTime();
        final Long userId = anaRequest.userId();
        final Collection<CRMChildViewDetails> crmChildViewDetails = crmDashboardDetails.getCrmChildViews();

        final ViewDetails newViewDetails = ViewDetails.builder()
                .id(viewDetails.id())
                .orgId(orgId)
                .viewTypeId(viewTypeId)
                .displayName(newDashboardName)
                .description(newDescription)
                .modifiedTime(System.currentTimeMillis())
                .isDeleted(false)
                .createdBy(getCreatedBy)
                .createdTime(getCreatedTime)
                .modifiedBy(userId)
                .isCustomView(viewDetails.getIsCustomView())
                .isHidden(viewDetails.getIsHidden())
                .moveToTrash(viewDetails.getMoveToTrash())
                .build();

        try {
            analyticsServiceAPI.getViewDetailsManager()
                    .loadUpdateDataIntoContainer(List.of(newViewDetails), dataContainer);
        } catch (Exception error) {
            LOGGER.error("CRM Analytics: Failed to load view details into dataContainer for orgId: {}", orgId, error);
            throw ErrorCode.get(AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed");
        }

        final CRMDashboardDetails newCRMDashboardDetails = CRMDashboardDetails.builder()
                .reportsDashboardId(anaDashboardId)
                .markAsTrash(false)
                .name(newDashboardName)
                .description(newDescription)
                .createdBy(getCreatedBy)
                .createdTime(getCreatedTime)
                .modifiedBy(userId)
                .modifiedTime(System.currentTimeMillis())
                .isCurated(false)
                .crmChildViews(crmChildViewDetails)
                .orgId(orgId)
                .id(crmDashboardId).build();

        try {
            crmViewManagerAPI.getCRMDashboardManager()
                    .loadUpdateDataIntoContainer(List.of(newCRMDashboardDetails), dataContainer);
        } catch (Exception error) {
            LOGGER.error("CRM Analytics: Failed to load CRM dashboard details into dataContainer for orgId: {}", orgId, error);
            throw ErrorCode.get(AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed");
        }

        checkAndDeleteViews(crmChildViewDetails, anaRequest, request, i18nResolver, dataContainer);

        if (request.containsKey(CRMDashboardKeys.CHILD_VIEW)) {
            final JsonArray childViewArray = request.getJsonArray(CRMDashboardKeys.CHILD_VIEW);
            final Map<Integer, DashboardLayoutConfig> layoutConfigMap = new HashMap<>();

            if (request.containsKey(CRMDashboardKeys.LAYOUT)) {
                final JsonArray layoutJson = request.getJsonArray(CRMDashboardKeys.LAYOUT);

                for (int index = 0; index < layoutJson.size(); index++) {
                    final JsonObject layoutObj = layoutJson.getJsonObject(index);
                    final int position = layoutObj.getInt(CRMChildViewKeys.VIEW_POSITION);

                    layoutConfigMap.put(position, new DashboardLayoutConfig(layoutObj.toString()));
                }
            }
            final int childViewSize = childViewArray.size();

            for (int index = 0; index < childViewSize; index++) {
                final JsonObject childView = childViewArray.getJsonObject(index);
                final int childViewTypeId = childView.getInt(CRMReportKeys.VIEW_TYPE_ID);
                final int childPosition = childView.getInt(CRMChildViewKeys.VIEW_POSITION);
                final DashboardLayoutConfig layoutConfig = layoutConfigMap.get(childPosition);
                final CRMViewType viewType = CRMViewType.getCrmViewType(childViewTypeId);
                final boolean isKPI = Objects.equals(viewType, CRMViewType.KPI);
                final boolean isUpdate = childView.containsKey(CRMReportKeys.VIEW_ID);

                if (isKPI) {
                    if (isUpdate) {
                        crmKPIViewConfigAPI.checkAndUpdate(anaRequest, crmDashboardId, anaDashboardId, childView,
                                i18nResolver, dataContainer, layoutConfig, reportFieldMap);
                    } else {
                        crmKPIViewConfigAPI.createModel(anaRequest, crmDashboardId, anaDashboardId, childView,
                                i18nResolver, dataContainer, layoutConfig, reportFieldMap);
                    }
                } else {
                    if (isUpdate) {
                        crmChartViewConfigAPI.checkAndUpdate(anaRequest, crmDashboardId, anaDashboardId, childView,
                                i18nResolver, dataContainer, layoutConfig, reportFieldMap);
                    } else {
                        crmChartViewConfigAPI.createModel(anaRequest, crmDashboardId, anaDashboardId, childView,
                                i18nResolver, dataContainer, layoutConfig, reportFieldMap);
                    }
                }
            }

            try {
                crmViewManagerAPI.getCRMDashboardManager().getOrgDataStore(anaRequest.orgId()).commitChanges(dataContainer);
            } catch (Exception error) {
                LOGGER.error("CRM Analytics: Failed to commit dataContainer changes for orgId: {}", orgId, error);
                throw ErrorCode.get(AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed");
            }
        }

        LOGGER.info("CRM Analytics: Dashboard update completed.");
        final CRMDashboardDetails newCrmDashboardDetails = crmViewManagerAPI.getCRMDashboardManager()
                .get(orgId, dashboardId);

        return getDashboardResponse(Collections.emptyList(), new HashSet<>(), anaRequest, actionType, request, newCrmDashboardDetails, i18nResolver);
    }

    /**
     * <p>
     * Gets the dashboard layout response based on the {@link CRMDashboardDetails}
     * </p>
     *
     * @param userIds             A collection of userIds
     * @param fieldIds            the collection used to collect all field IDs involved in this configuration
     * @param actionType          defining the operation being performed
     * @param request             Represents the {@link AnalyticsRequest} details
     * @param viewRequest         representing the view request payload
     * @param crmDashboardDetails Represents the {@link CRMDashboardDetails}
     * @param i18nResolver        An instance of {@link I18nResolver} for handling internationalization
     * @return The dashboard layout response
     */
    protected final @NonNull JsonObject getDashboardResponse(final @NonNull Collection<Long> userIds,
                                                             final @NonNull Collection<Long> fieldIds,
                                                             final @NonNull AnalyticsRequest request,
                                                             final @NonNull ActionType actionType,
                                                             final @NonNull JsonObject viewRequest,
                                                             final @NonNull CRMDashboardDetails crmDashboardDetails,
                                                             final @NonNull I18nResolver i18nResolver) throws Exception {
        final Long orgId = request.orgId();
        final Long anaDashboardId = crmDashboardDetails.getReportsDashboardId();
        final DashboardLayout dashboardLayout = analyticsServiceAPI.getDashboardLayout().get(orgId, anaDashboardId);
        final Criteria criteria = Criteria.eq(dashboardLayoutPositionManager.getTable().getColumn(DASHBOARDLAYOUTPOSITION.DASHBOARD_ID), anaDashboardId);
        final Collection<DashboardLayoutPosition> dashboardLayoutPositions = dashboardLayoutPositionManager.getAll(orgId, criteria);

        return getDashboardResponse(userIds, fieldIds, dashboardLayoutPositions, actionType, request, viewRequest, crmDashboardDetails, dashboardLayout, i18nResolver);
    }

    /**
     * <p>
     * Resolves user-related fields in the given json object by mapping user IDs to their corresponding full names
     * </p>
     *
     * @param crmUsers   A collection of {@link CrmUser} objects containing user information, including IDs and full names.
     * @param jsonObject A {@link JsonObject} representing the dashboard's JSON structure,
     *                   which contains child views, view properties, and filter details.
     */
    protected final void resolveUserFields(final @NonNull Collection<CrmUser> crmUsers,
                                           final @NonNull JsonObject jsonObject) {
        final JsonArray childViewInfoArray = jsonObject.getJsonArray(CRMDashboardKeys.CHILD_VIEW);
        final Map<Long, String> crmUserMap = crmUsers.stream().collect(Collectors.toMap(CrmUser::id, CrmUser::getFullName));
        final int childViewInfoArraySize = childViewInfoArray.size();

        for (int index = NumericalConstant.ZERO; index < childViewInfoArraySize; index++) {
            final JsonObject childViewJson = childViewInfoArray.getJsonObject(index);
            final JsonObject viewPropertyJson = childViewJson.getJsonObject(CRMChildViewKeys.VIEW_PROPERTIES);
            final JsonArray metricsJsonArray = viewPropertyJson.getJsonArray(CRMViewPropertyKeys.METRICS);
            final int metricsJsonArraySize = metricsJsonArray.size();

            for (int metricsIndex = NumericalConstant.ZERO; metricsIndex < metricsJsonArraySize; metricsIndex++) {
                final JsonObject metricsJson = metricsJsonArray.getJsonObject(metricsIndex);
                final JsonArray filtersJsonArray = metricsJson.getJsonArray(FilterFieldKeys.FILTERS);
                final int filterJsonArraySize = filtersJsonArray.size();

                for (int filterIndex = NumericalConstant.ZERO; filterIndex < filterJsonArraySize; filterIndex++) {
                    final JsonObject filterJson = filtersJsonArray.getJsonObject(filterIndex);

                    if (filterJson.containsKey(FilterFieldKeys.VALUES)) {
                        final JsonArray filterValueJsonArray = filterJson.getJsonArray(FilterFieldKeys.VALUES);

                        if (NumericalConstant.ZERO < filterValueJsonArray.size()) {
                            final JsonObject filterValueJson = filterValueJsonArray.getJsonObject(NumericalConstant.ZERO);

                            if (filterValueJson.containsKey(FilterFieldKeys.VALUE)) {
                                final Object filterValue = filterValueJson.get(FilterFieldKeys.VALUE);

                                if (filterValue instanceof final Map<?, ?> map) {
                                    final Map<String, Long> valueMap = (Map<String, Long>) map;

                                    if (valueMap.containsKey(CRMReportKeys.USER_ID)) {
                                        final Long userId = valueMap.get(CRMReportKeys.USER_ID);
                                        final JsonObject valueJson = Json.object();

                                        valueJson.put(CRMReportKeys.ID, userId);
                                        valueJson.put(CommonKeys.NAME, crmUserMap.get(userId));
                                        filterValueJson.put(FilterFieldKeys.VALUE, valueJson);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Gets the dashboard layout response based on the {@link CRMDashboardDetails}
     * </p>
     *
     * @param crmDashboardDetails Represents the {@link CRMDashboardDetails}
     * @param dashboardLayout     Represents the {@link DashboardLayout}
     * @param userIds             Contains the collection of user ids
     * @return The dashboard layout response
     */
    protected final @NonNull JsonObject getDashboardResponse(final @NonNull Collection<Long> userIds,
                                                             final @NonNull Collection<Long> fieldIds,
                                                             final Collection<DashboardLayoutPosition> dashboardLayoutPositions,
                                                             final @NonNull ActionType actionType,
                                                             final @NonNull AnalyticsRequest request,
                                                             final @NonNull JsonObject viewRequest,
                                                             final @NonNull CRMDashboardDetails crmDashboardDetails,
                                                             final @NonNull DashboardLayout dashboardLayout,
                                                             final @NonNull I18nResolver i18nResolver) {
        final JsonObject response = Json.object();

        final Long dashboardId = crmDashboardDetails.id();
        final String dashboardName = crmDashboardDetails.getName();
        final String description = crmDashboardDetails.getDescription();
        final JsonArray childViewInfo = Json.array();
        final Collection<CRMChildViewDetails> crmChildViewDetails = crmDashboardDetails.getCrmChildViews();
        final boolean fetchProperties = viewRequest.optBoolean(CRMViewPropertyKeys.FETCH_VIEW_PROPERTIES, false);

        if (viewRequest.containsKey(CRMDashboardKeys.DASHBOARD_ID)) {
            final JsonObject dashboardShareConfig = crmReportsAPI.getDashboardVisibility().getShareVisibility(request, viewRequest, i18nResolver);

            if (dashboardShareConfig.containsKey(CRMDashboardKeys.CURRENT_USER_CONFIG)) {
                final JsonArray jsonArray = dashboardShareConfig.getJsonArray(CRMDashboardKeys.CURRENT_USER_CONFIG);
                final int totalConfig = jsonArray.size();
                int userVisibilityPermission = NumericalConstant.ONE;

                for (int index = NumericalConstant.ZERO; index < totalConfig; index++) {
                    final int value = jsonArray.getInt(index);

                    if (value > userVisibilityPermission) {
                        userVisibilityPermission = value;
                    }
                }
                response.put(CRMDashboardKeys.USER_PERMISSION, userVisibilityPermission);
            } else {
                response.put(CRMDashboardKeys.USER_PERMISSION, VisibilityPermission.OWNER.getId());
            }
        }

        if (actionType.equals(ActionType.GET) && !dashboardLayoutPositions.isEmpty()) {
            final JsonArray layoutJsonArray = Json.array();

            for (final DashboardLayoutPosition dashboardLayoutPosition : dashboardLayoutPositions) {
                final DashboardLayoutConfig dashboardLayoutConfig = new DashboardLayoutConfig(dashboardLayoutPosition.getLayoutAsString());
                final JsonObject layoutJson = Json.object();

                layoutJson.put(CRMReportKeys.ID, dashboardLayoutConfig.getId());
                layoutJson.put(CRMReportKeys.X_POSITION, dashboardLayoutConfig.getX());
                layoutJson.put(CRMReportKeys.Y_POSITION, dashboardLayoutConfig.getY());
                layoutJson.put(CRMReportKeys.HEIGHT, dashboardLayoutConfig.getHeight());
                layoutJson.put(CRMReportKeys.WIDTH, dashboardLayoutConfig.getWidth());
                layoutJson.put(CRMChildViewKeys.VIEW_POSITION, dashboardLayoutConfig.getViewPosition());

                layoutJsonArray.put(layoutJson);
            }
            response.put(CRMDashboardKeys.LAYOUT, layoutJsonArray);
        }

        crmDashboardDetails.setOrgId(request.orgId());
        response.put(CRMReportKeys.DASHBOARD_ID, dashboardId);
        response.put(CRMDashboardKeys.DASHBOARD_NAME, dashboardName);
        response.put(CRMDashboardKeys.DESCRIPTION, description);
        response.put(CRMDashboardKeys.IS_FAVOURITE, crmDashboardDetails.isFavourite());
        //TODO Enable it after implementing dashboard export functionality
//        response.put(CRMDashboardKeys.CAN_EXPORT, DashboardPermissionService.INSTANCE().canExport(crmDashboardDetails.orgId(), dashboardId, request));
        response.put(CRMDashboardKeys.CAN_EXPORT, false);

        if (crmDashboardDetails.isMarkAsTrash()) {
            response.put(CRMDashboardKeys.CAN_RESTORE, crmDashboardDetails.canRestore());
            response.put(CRMDashboardKeys.CAN_DELETE, crmDashboardDetails.isDeletable());
        } else {
            response.put(CRMDashboardKeys.CAN_MOVE_TO_TRASH, crmDashboardDetails.canMoveToTrash());
        }

        if (crmChildViewDetails != null) {

            for (final CRMChildViewDetails crmChildViewDetail : crmChildViewDetails) {
                final JsonObject childViewRequest = Json.object();
                childViewRequest.put(CRMReportKeys.DASHBOARD_ID, dashboardId);
                childViewRequest.put(CRMReportKeys.VIEW_ID, crmChildViewDetail.id());

                if (crmChildViewDetail.getViewType() == CRMViewType.KPI) {
                    final JsonObject childViewResponse = crmReportsAPI.getKPIConfigAPI()
                            .getConfigJsonResponse(fetchProperties, userIds, fieldIds, request, childViewRequest, i18nResolver);
                    childViewInfo.put(childViewResponse);
                } else {
                    final JsonObject childViewResponse = crmReportsAPI.getChartConfigAPI()
                            .getConfigJsonResponse(fetchProperties, userIds, fieldIds, request, childViewRequest, i18nResolver);
                    childViewInfo.put(childViewResponse);
                }

            }

            if (!crmChildViewDetails.isEmpty()) {
                response.put(CRMDashboardKeys.CHILD_VIEW, childViewInfo);
            }
        }

        response.put(CRMDashboardKeys.CAN_CLONE, fieldIds.isEmpty() || analyticsPermissionHandler.isClonable(request.orgId(), request.roleId(), fieldIds));

        return response;
    }

    /**
     * <p>
     * Gets the {@link DashboardLayout} based on the dashboard id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param dashboardId Represents the dashboard id
     * @return The {@link DashboardLayout} details
     */
    private DashboardLayout getDashboardLayout(final Long orgId, final Long dashboardId) {
        return analyticsServiceAPI.getDashboardLayout().get(orgId, dashboardId);
    }

    /**
     * <p>
     * Checks and delete the views that are already present
     * </p>
     *
     * @param crmChildViewDetails Holds the {@link CRMChildViewDetails} details
     * @param analyticsRequest    Represents the dashboard id
     * @param request             Represents the dashboard id
     * @param i18nResolver        Represents the dashboard id
     * @param dataContainer       Represents the dashboard id
     */
    private void checkAndDeleteViews(final Collection<CRMChildViewDetails> crmChildViewDetails,
                                     final AnalyticsRequest analyticsRequest,
                                     final JsonObject request,
                                     final I18nResolver i18nResolver,
                                     final DataContainer dataContainer) {
        try {
            final Long orgId = analyticsRequest.orgId();

            if (crmChildViewDetails == null) {
                return;
            }

            if (!request.containsKey(CRMChildViewKeys.VIEW_PROPERTIES)) {
                final Collection<Long> anaViewDetailsId = new ArrayList<>();

                for (CRMChildViewDetails crmChildViewDetail : crmChildViewDetails) {
                    if (crmChildViewDetail.getCrmViewMapper() == null) {
                        continue;
                    }
                    anaViewDetailsId.addAll(crmChildViewDetail.getCrmViewMapper().stream()
                            .map(CRMANAViewMapper::getAnaViewDetailsId).toList());
                }

                CRMDashboardViewRemover.deleteUnMappedAnaViewDetails(orgId, anaViewDetailsId, dataContainer, analyticsServiceAPI);
                CRMDashboardViewRemover.deleteUnMappedCrmDetails(orgId, crmChildViewDetails.stream().map(TlcModel::id)
                                .filter(Objects::nonNull).toList(), dataContainer, crmViewManagerAPI);
            } else {
                final JsonArray childViewArray = request.getJsonArray(CRMChildViewKeys.VIEW_PROPERTIES);
                final int size = childViewArray.size();

                for (final CRMChildViewDetails crmChildViewDetail : crmChildViewDetails) {

                    for (int index = NumericalConstant.ZERO; index < size; index++) {
                        final JsonObject childView = childViewArray.getJsonObject(index);

                        if (childView.containsKey(CRMReportKeys.VIEW_ID) &&
                                childView.getLong(CRMReportKeys.VIEW_ID) == crmChildViewDetail.id()) {
                            break;
                        }
                    }

                    if (crmChildViewDetail.getCrmViewMapper() != null) {
                        final Collection<Long> anaViewDetailsIds = crmChildViewDetail.getCrmViewMapper().stream()
                                .map(CRMANAViewMapper::getAnaViewDetailsId).toList();

                        CRMDashboardViewRemover.deleteUnMappedAnaViewDetails(orgId, anaViewDetailsIds, dataContainer, analyticsServiceAPI);
                    }

                    CRMDashboardViewRemover.deleteUnMappedCrmDetails(orgId, crmChildViewDetails.stream()
                                    .map(TlcModel::id).filter(Objects::nonNull).toList(), dataContainer, crmViewManagerAPI);
                }
            }
        } catch (final Exception error) {
            LOGGER.error("CRM Analytics: Failed to delete unmapped views for orgId: {}", analyticsRequest.orgId(), error);
        }
    }
}
