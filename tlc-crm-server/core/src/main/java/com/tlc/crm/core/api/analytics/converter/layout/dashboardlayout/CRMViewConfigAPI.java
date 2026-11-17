package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.model.view.report.AnalyticsReport;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.DashboardLayoutConfig;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;

import com.tlc.i18n.I18nResolver;

import com.tlc.sql.common.api.DataContainer;

import lombok.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * A interface defines the API for handling the configuration and operations related to CRM view models
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
public interface CRMViewConfigAPI<T extends AnalyticsReport>
{

    /**
     * <p>
     * Retrieves the model configuration for a specific CRM view.
     * </p>
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest}
     * @param childViewRequest Holds the view request json
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     * @return The collection of models
     */
    @NonNull
    Collection<T> getModelConfig( final @NonNull AnalyticsRequest analyticsRequest,
                                  final @NonNull JsonObject childViewRequest,
                                  final @NonNull I18nResolver i18nResolver,
                                  final Map<Long, ReportModularField> reportFieldMap );

    /**
     * <p>
     * Creates a new models based on the view request json
     * </p>
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest}
     * @param childViewRequest Holds the view request json
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     * @param dataContainer    Holds the rows that will get committed
     * @return The {@link JsonObject}
     */
    @NonNull
    JsonObject createModel( final @NonNull AnalyticsRequest analyticsRequest,
                            final @NonNull Long crmDashboardId,
                            final @NonNull Long anaDashboardId,
                            final @NonNull JsonObject childViewRequest,
                            final @NonNull I18nResolver i18nResolver,
                            final @NonNull DataContainer dataContainer,
                            final DashboardLayoutConfig dashboardLayoutConfig,
                            final Map<Long, ReportModularField> reportFieldMap );

    /**
     * <p>
     * Checks and updates the models based on the view request json
     * </p>
     */
    @NonNull
    JsonObject checkAndUpdate( final @NonNull AnalyticsRequest analyticsRequest,
                               final @NonNull Long crmDashboardId,
                               final @NonNull Long anaDashboardId,
                               final @NonNull JsonObject childViewRequest,
                               final @NonNull I18nResolver i18nResolver,
                               final @NonNull DataContainer dataContainer,
                               final DashboardLayoutConfig dashboardLayoutConfig,
                               final Map<Long, ReportModularField> reportFieldMap );

    /**
     * <p>
     * Gets the configuration json response based on the child view
     * </p>
     *
     * @param fetchProperties  if {@code true}, includes field and view-specific properties in the response
     * @param userIds          the collection of user IDs relevant to the configuration
     * @param fieldIds         the collection used to collect all field IDs involved in this configuration
     * @param analyticsRequest Represents the {@link AnalyticsRequest}
     * @param childViewRequest Holds the view request json
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     * @return The {@link JsonObject}
     */
    @NonNull
    JsonObject getConfigJsonResponse(final boolean fetchProperties,
                                     final @NonNull Collection<Long> userIds,
                                     final @NonNull Collection<Long> fieldIds,
                                     final @NonNull AnalyticsRequest analyticsRequest,
                                     final @NonNull JsonObject childViewRequest,
                                     final @NonNull I18nResolver i18nResolver);

    /**
     * <p>
     * Gets the configuration json response based on the child view and additional context.
     * </p>
     *
     * @param fetchProperties     if {@code true}, includes field and view-specific properties in the response
     * @param userIds             the collection of user IDs relevant to the configuration
     * @param reportFieldIds      the collection used to collect all field IDs involved in this configuration
     * @param models              Collection of models to be processed in the response
     * @param crmChildViewDetails Details of the CRM child view configuration
     * @param analyticsRequest    Represents the {@link AnalyticsRequest}
     * @param i18nResolver        An instance of {@link I18nResolver} for handling internationalization
     * @return The {@link JsonObject}
     */
    @NonNull
    JsonObject getConfigJsonResponse( final boolean fetchProperties,
                                      final @NonNull Collection<Long> userIds,
                                      final Collection<Long> reportFieldIds,
                                      final @NonNull Collection<T> models,
                                      final @NonNull CRMChildViewDetails crmChildViewDetails,
                                      final @NonNull AnalyticsRequest analyticsRequest,
                                      final @NonNull I18nResolver i18nResolver);

    /**
     * <p>
     * Deletes the view based on the child view json
     * </p>
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest}
     * @param childViewRequest Holds the view request json
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     * @param dataContainer    Holds the rows that will get committed
     * @return The {@link JsonObject}
     */
    @NonNull
    JsonObject deleteView( final @NonNull AnalyticsRequest analyticsRequest,
                           final @NonNull Long crmDashboardId,
                           final @NonNull Long anaDashboardId,
                           final @NonNull JsonObject childViewRequest,
                           final @NonNull I18nResolver i18nResolver,
                           final DataContainer dataContainer );

}
