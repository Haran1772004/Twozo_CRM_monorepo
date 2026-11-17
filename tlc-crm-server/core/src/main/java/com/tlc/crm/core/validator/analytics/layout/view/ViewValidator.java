package com.tlc.crm.core.validator.analytics.layout.view;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.listeners.Callback;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularFieldConfig;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMChildViewKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMKPIKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.general.analytics.CRMChartViewTypeService;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;
import com.tlc.crm.core.validator.analytics.Validator;
import com.tlc.crm.core.validator.analytics.layout.view.axis.DurationValidator;
import com.tlc.crm.core.validator.analytics.layout.view.axis.MetricsValidator;
import com.tlc.crm.core.validator.analytics.layout.view.axis.ViewByValidator;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Provides an abstract way to validate the view apply and view data configurations
 *
 * @author Saravanan
 * @version 1.0
 *
 */
class ViewValidator implements RequestValidator<JsonObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViewValidator.class);

    private final int viewNameLimit;
    private final Validator validator;
    private final CRMChartViewTypeService viewTypeService;
    private final ReportModularFieldService reportModularFieldService;
    private final MetricsValidator metricsValidator;
    private final DurationValidator durationValidator;
    private final ViewByValidator viewByValidator;

    private ViewValidator() {
        this.viewNameLimit = NumberUtils.parseNumber(ConfHolder.getInstance().getProperties(CRMReportKeys.ANALYTICS)
                        .getNestedConfigs(CRMReportKeys.VIEW)
                        .getConfig(CRMReportKeys.NAME_LIMIT))
                .intValue();
        this.validator = Validator.INSTANCE();
        this.viewTypeService = CRMChartViewTypeService.INSTANCE();
        this.reportModularFieldService = ReportModularFieldService.getInstance();
        this.metricsValidator = MetricsValidator.INSTANCE();
        this.durationValidator = DurationValidator.INSTANCE();
        this.viewByValidator = ViewByValidator.INSTANCE();
    }

    private static class Instance {
        private static final ViewValidator INSTANCE = new ViewValidator();
    }

    public static ViewValidator INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Synchronous validation is not supported by {@link ViewValidator}.
     * Use {@link #validateRequest(Long, Long, JsonObject, Consumer)} for async validation instead.
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userId  Represents the user id
     * @param request Represents the {@link JsonObject} containing the configuration
     */
    @Override
    public void validateRequest(@NonNull Long orgId, @NonNull Long userId, @NonNull JsonObject request) throws Exception {

    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param requestData       Represents the {@link JsonObject} containing the configuration
     * @param completionHandler Called with {@code null} on success, or with the exception on failure
     */
    @Override
    public void validateRequest(final @NonNull Long orgId,
                                final @NonNull Long userId,
                                final @NonNull JsonObject requestData,
                                final @NonNull Consumer<Exception> completionHandler) {
        reportModularFieldService.getByOrgId(orgId, new Callback<>() {
            @Override
            public void onSuccess(final Map<Long, ReportModularField> allReportFields) {
                LOGGER.info("CRM Analytics [ASYNC-TRACE2] : callback.onSuccess on thread [{}]", Thread.currentThread().getName());

                try {
                    validateView(orgId, userId, requestData, allReportFields);
                    LOGGER.info("CRM Analytics [ASYNC-TRACE3] : validation passed on thread [{}]", Thread.currentThread().getName());
                    completionHandler.accept(null);
                } catch (final Exception ex) {
                    LOGGER.error("CRM Analytics [ASYNC-TRACE4] : validation failed on thread [{}], error: {}", Thread.currentThread().getName(), ex.getMessage());
                    completionHandler.accept(ex);
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.error("CRM Analytics [ASYNC-TRACE5] : callback.onFailure on thread [{}], error: {}", Thread.currentThread().getName(), cause.getMessage());
                completionHandler.accept(cause instanceof Exception ? (Exception) cause : new RuntimeException(cause));
            }
        });

    }

    /**
     * Validates the view configuration including view name, metrics, view-by fields,
     * duration settings, and the selected view type against supported types.
     *
     * @param orgId                  the organization ID
     * @param userId                 the user ID
     * @param requestData            the {@link JsonObject} containing the view configuration
     * @param reportModularFieldMap  the map of report modular fields keyed by field ID
     * @throws Exception if the view configuration is invalid
     */
    private void validateView(final Long orgId, final Long userId, final JsonObject requestData,
                              final Map<Long, ReportModularField> reportModularFieldMap) throws Exception {
        if (Objects.equals(requestData.get(CRMViewPropertyKeys.IS_PREVIEW), true)) {
            final CRMViewType crmViewType;

            try {
                crmViewType = CRMViewType.getCrmViewType(requestData.getInt(CRMReportKeys.VIEW_TYPE_ID));
            } catch (final ErrorCode errorCode) {
                throw ErrorCode.get(AnalyticsErrorCodes.INVALID_VIEW_TYPE,
                        "i18n.analytics.config.invalid.view.type");
            }
            final String viewName = requestData.getString(CRMChildViewKeys.VIEW_NAME);

            if (Objects.isNull(viewName) || viewName.isBlank()) {
                throw ErrorCode.get(AnalyticsErrorCodes.VIEW_NAME_SHOULD_NOT_BE_EMPTY,
                        "i18n.analytics.config.view.invalid.name");
            }

            if (viewNameLimit < viewName.length()) {
                throw ErrorCode.get(AnalyticsErrorCodes.VIEW_NAME_TOO_LONG,
                        "i18n.analytics.config.view.name.limit.exceeded");
            }
            final JsonObject viewProperties = requestData.getJsonObject(CRMChildViewKeys.VIEW_PROPERTIES);
            final JsonArray metricsArray = viewProperties.getJsonArray(CRMChildViewKeys.METRICS);
            final int totalMetrics = metricsArray.size();

            if (totalMetrics < NumericalConstant.ONE) {
                throw ErrorCode.get(AnalyticsErrorCodes.AT_LEAST_CONTAIN_ONE_METRICS,
                        "i18n.analytics.config.view.invalid");
            }

            final Set<Long> moduleIds = new HashSet<>();
            final Collection<Long> metricsOrders = new HashSet<>();
            final Set<Long> measureIds = new HashSet<>();

            for (int index = NumericalConstant.ZERO; index < totalMetrics; index++) {
                final JsonObject metricJson = metricsArray.getJsonObject(index);
                final long order = metricJson.getLong(CRMViewPropertyKeys.ORDER);

                if (NumericalConstant.ONE > order || NumericalConstant.FOUR < order) {
                    throw ErrorCode.get(AnalyticsErrorCodes.INVALID_METRICS_ORDER,
                            "i18n.analytics.config.view.metrics.invalid.order");
                }

                if (metricsOrders.contains(order)) {
                    throw ErrorCode.get(AnalyticsErrorCodes.METRICS_ORDER_ALREADY_EXISTS,
                            "i18n.analytics.config.view.metrics.order.exists");
                }

                metricsOrders.add(metricJson.getLong(CRMViewPropertyKeys.ORDER));
                moduleIds.add((long) metricJson.getInt(CRMReportKeys.MODULE_ID));
                measureIds.add(metricJson.getLong(CRMReportKeys.MEASURE_ID));

                if (viewProperties.containsKey(CommonKeys.VIEW_BY)) {
                    metricJson.put(CommonKeys.VIEW_BY, viewProperties.getJsonArray(CommonKeys.VIEW_BY));
                }

                metricsValidator.validateRequest(orgId, metricJson, reportModularFieldMap);
            }

            final boolean isViewByPresent = viewProperties.containsKey(CommonKeys.VIEW_BY);
            final boolean isSameMeasure = Objects.equals(measureIds.size(), NumericalConstant.ONE);
            final List<Long> fieldIds = new LinkedList<>();
            int totalViewBy = NumericalConstant.ZERO;

            if (CRMViewType.isChartType(crmViewType) && !isViewByPresent && Objects.equals(totalMetrics, NumericalConstant.ONE)) {
                throw ErrorCode.get(AnalyticsErrorCodes.MULTIPLE_METRICS_NEEDED,
                        "i18n.analytics.config.multiple.metrics.needed");
            }

            if (isViewByPresent) {

                final JsonArray viewByArray = viewProperties.getJsonArray(CommonKeys.VIEW_BY);
                totalViewBy = viewByArray.size();
                final Collection<Integer> viewByOrders = new HashSet<>();
                final boolean isSameModule = Objects.equals(moduleIds.size(), NumericalConstant.ONE);

                if (totalViewBy < NumericalConstant.ONE || totalViewBy > NumericalConstant.TWO) {
                    throw ErrorCode.get(totalViewBy < NumericalConstant.ONE ? AnalyticsErrorCodes.VIEW_BY_ORDER_TOO_LOW
                            : AnalyticsErrorCodes.VIEW_BY_ORDER_EXCEEDS_MAX_LIMIT, "i18n.analytics.config.invalid.view.by.count");
                }

                for (int index = NumericalConstant.ZERO; index < totalViewBy; index++) {
                    final JsonObject viewBy = viewByArray.getJsonObject(index);

                    fieldIds.add(CRMViewType.isChartType(crmViewType) && viewBy.containsKey(CRMReportKeys.FIELD_ID) ? viewBy.getLong(CRMReportKeys.FIELD_ID) : null);
                    final int viewByOrder = viewBy.getInt(CRMReportKeys.ORDER);

                    if (!viewByOrders.add(viewByOrder)) {
                        throw ErrorCode.get(AnalyticsErrorCodes.DUPLICATE_VIEW_BY_ORDER);
                    }

                    if (viewByOrder < NumericalConstant.ONE || viewByOrder > NumericalConstant.TWO) {
                        throw ErrorCode.get(viewByOrder < NumericalConstant.ONE ? AnalyticsErrorCodes.VIEW_BY_ORDER_TOO_LOW
                                : AnalyticsErrorCodes.VIEW_BY_ORDER_EXCEEDS_MAX_LIMIT);
                    }

                    viewByValidator.validateRequest(orgId, viewBy, reportModularFieldMap);
                }

                switch (crmViewType) {
                    case LINE, AREA, HEAT_MAP, GROUPED_BAR, GROUPED_HORIZONTAL_BAR,
                         STACKED_BAR, STACKED_HORIZONTAL_BAR, SUMMARY -> {
                        if (NumericalConstant.FOUR < totalMetrics) {
                            throw ErrorCode.get(AnalyticsErrorCodes.Y_AXIS_LIMIT_EXCEED);
                        }
                    }
                    case BAR, DONUT, HORIZONTAL_BAR, PIE -> {
                        if (NumericalConstant.ONE < totalMetrics) {
                            throw ErrorCode.get(AnalyticsErrorCodes.Y_AXIS_LIMIT_EXCEED);
                        }
                    }
                    case BUBBLE -> {
                        if (NumericalConstant.TWO < totalMetrics) {
                            throw ErrorCode.get(AnalyticsErrorCodes.Y_AXIS_LIMIT_EXCEED);
                        }
                    }
                    case KPI -> {
                        if (NumericalConstant.ONE < totalMetrics) {
                            throw ErrorCode.get(AnalyticsErrorCodes.Y_AXIS_LIMIT_EXCEED);
                        }

                        durationValidator.validateRequest(orgId, viewProperties.getJsonObject(CRMKPIKeys.DURATION), reportModularFieldMap);
                        validator.getCompareToValidator()
                                .validateRequest(orgId, userId, viewProperties.getJsonObject(CRMKPIKeys.COMPARE_TO));
                    }
                }

                final Long primaryViewByFieldId = fieldIds.get(NumericalConstant.ZERO);
                final Long secondaryViewByFieldId = fieldIds.size() > NumericalConstant.ONE ? fieldIds.get(NumericalConstant.ONE) : null;

                if (totalViewBy > NumericalConstant.ONE) {
                    if (totalMetrics > NumericalConstant.ONE && !isSameModule) {
                        throw ErrorCode.get(AnalyticsErrorCodes.MULTIPLE_VIEW_BY_NOT_SUPPORTED, "i18n.analytics.config.view.multiple.view.by.not.supported");
                    }

                    if (viewTypeService.isDateType(orgId, primaryViewByFieldId, reportModularFieldMap) && viewTypeService.isDateType(orgId, secondaryViewByFieldId, reportModularFieldMap)) {
                        throw ErrorCode.get(AnalyticsErrorCodes.MULTIPLE_DATE_TYPE_NOT_SUPPORTED, "i18n.analytics.config.view.date.type.field.already.selected");
                    }

                    if (Objects.nonNull(primaryViewByFieldId) && Objects.nonNull(secondaryViewByFieldId)) {
                        final ReportModularField primaryReportModularField = reportModularFieldMap.get(primaryViewByFieldId);
                        final ReportModularField secondaryReportModularField = reportModularFieldMap.get(secondaryViewByFieldId);

                        final ModularField primaryModularField = primaryReportModularField.getModularField();
                        final ModularField secondaryModularField = secondaryReportModularField.getModularField();

                        final ModularFieldConfig primaryFieldConfig = primaryModularField.getModularFieldConfig();
                        final ModularFieldConfig secondaryFieldConfig = secondaryModularField.getModularFieldConfig();

                        if (Objects.nonNull(primaryFieldConfig) && primaryFieldConfig.isBucketByEnabled() && Objects.nonNull(secondaryFieldConfig) && secondaryFieldConfig.isBucketByEnabled()) {
                            throw ErrorCode.get(AnalyticsErrorCodes.MULTIPLE_BUCKET_BY_NOT_SUPPORTED, "i18n.analytics.config.view.bucket.by.field.already.selected");
                        }
                    }
                }

                if (totalMetrics > NumericalConstant.ONE && Objects.equals(NumericalConstant.ONE, totalViewBy) && !isSameModule && !viewTypeService.isDateType(orgId, primaryViewByFieldId, reportModularFieldMap)) {
                    throw ErrorCode.get(AnalyticsErrorCodes.INVALID_FIELD_TYPE,
                            "i18n.analytics.config.invalid.field.type");
                }
            }

            if (CRMViewType.isChartType(crmViewType)) {
                validateSelectedViewType(crmViewType, isSameMeasure, totalMetrics, totalViewBy, orgId, fieldIds, reportModularFieldMap);
            }
        }
    }

    /**
     * Validates whether the selected {@link CRMViewType} is supported based on the number of metrics,
     * whether the selected measures are the same, and the field type (date or non-date).
     *
     * @param selectedViewType the view type selected for the report
     * @param isSameMeasure    whether all selected metrics are the same
     * @param totalMetrics     the number of selected metrics
     * @param totalViewBy      the number of view-by fields
     * @param orgId            the organization ID
     * @param fieldIds         the field ID used to determine if the field is a date type
     * @param reportFieldMap   the map of report modular fields keyed by field ID
     */
    private void validateSelectedViewType(final CRMViewType selectedViewType, final boolean isSameMeasure,
                                          final int totalMetrics, final int totalViewBy, final Long orgId, final List<Long> fieldIds,
                                          final Map<Long, ReportModularField> reportFieldMap) {
        final EnumSet<CRMViewType> supportedTypes = viewTypeService.getSupportedViewTypes(isSameMeasure, totalMetrics, orgId, fieldIds, totalViewBy, reportFieldMap);
        final boolean isDateType = viewTypeService.isAnyDateType(orgId, fieldIds, reportFieldMap);
        final Locale locale = resolveLocale();
        final I18nResolver i18nResolver = I18nServiceProvider.get().getResolver(locale);

        if (Objects.isNull(supportedTypes) || supportedTypes.isEmpty() || !supportedTypes.contains(selectedViewType)) {
            switch (selectedViewType) {
                case GROUPED_BAR, GROUPED_HORIZONTAL_BAR, STACKED_BAR, STACKED_HORIZONTAL_BAR -> {
                    if (!Objects.equals(totalMetrics, NumericalConstant.FOUR)) {
                        throw ErrorCode.get(AnalyticsErrorCodes.VIEW_TYPE_NOT_SUPPORTED,
                                String.format(i18nResolver.get("i18n.analytics.config.view.not.supported.for.one.metric"), selectedViewType));
                    }
                }
                case PIE, BAR, DONUT, HORIZONTAL_BAR -> {
                    if (!Objects.equals(totalMetrics, NumericalConstant.ONE)) {
                        throw ErrorCode.get(AnalyticsErrorCodes.VIEW_TYPE_NOT_SUPPORTED,
                                String.format(i18nResolver.get("i18n.analytics.config.view.not.supported.for.two.metrics"), selectedViewType));
                    }
                }
            }

            if (Objects.equals(totalMetrics, NumericalConstant.ONE) || isSameMeasure) {
                if (!isDateType) {
                    throw ErrorCode.get(AnalyticsErrorCodes.VIEW_TYPE_NOT_SUPPORTED_WITHOUT_DATE,
                            String.format(i18nResolver.get("i18n.analytics.config.view.not.supported.without.date"), selectedViewType));
                } else {
                    throw ErrorCode.get(AnalyticsErrorCodes.VIEW_TYPE_NOT_SUPPORTED,
                            String.format(i18nResolver.get("i18n.analytics.config.view.not.supported"), selectedViewType));
                }
            } else {
                throw ErrorCode.get(AnalyticsErrorCodes.VIEW_TYPE_NOT_SUPPORTED_FOR_DIFFERENT_MEASURES,
                        String.format(i18nResolver.get("i18n.analytics.config.view.not.supported.for.different.measures"), selectedViewType));
            }
        }
    }

    /**
     * Resolves the {@link Locale} from the logged-in user's regional preference.
     * Falls back to {@link Locale#ENGLISH} if the preference or language is not available.
     *
     * @return the resolved {@link Locale}, never null
     */
    private Locale resolveLocale() {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference preference = crmUser.getPreferenceHolder().get(UserPreferenceType.USER_REGIONAL_PREFERENCE);

        return Locale.forLanguageTag(preference.getLanguage().getName());
    }
}
