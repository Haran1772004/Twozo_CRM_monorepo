package com.tlc.crm.core.api.analytics.converter.data;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.UnderlyingDataRequest;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;

import com.tlc.commons.json.JsonObject;
import com.tlc.i18n.I18nResolver;

import lombok.NonNull;

import java.util.List;
import java.util.Map;

/**
 * The {@link CRMReportsData} interface defines the contract for handling CRM report data processing.
 *
 * @author KarthickV
 * @version 1.0
 */
public interface CRMReportsData
{

    /**
     * <p>
     * Gets the {@link GraphAxis} list based on the given request
     * </p>
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param actionType       Represents the {@link ActionType}
     * @param requestData      Holds the {@link JsonObject} data
     * @param i18nResolver     Represents the {@link I18nResolver}
     * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     * @return The list of {@link GraphAxis}
     */
    List<GraphAxis<Object>> getGraphAxis( final @NonNull AnalyticsRequest analyticsRequest,
                                          final @NonNull ActionType actionType,
                                          final @NonNull JsonObject requestData,
                                          final @NonNull I18nResolver i18nResolver,
                                          final Map<Long, ReportModularField> reportFieldMap );

    /**
     * Gets the {@link GraphAxis} list for a paginated request.
     *
     * <p>Default behavior delegates to the existing non-paginated method so only
     * implementations that support direct paging need to override it.</p>
     */
    default List<GraphAxis<Object>> getGraphAxis(final @NonNull AnalyticsRequest analyticsRequest,
                                                 final @NonNull ActionType actionType,
                                                 final @NonNull JsonObject requestData,
                                                 final @NonNull I18nResolver i18nResolver,
                                                 final int start,
                                                 final int count,
                                                 final Map<Long, ReportModularField> reportFieldMap) {
        return getGraphAxis(analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap);
    }

    /**
     * Pre-builds and returns the {@link UnderlyingDataRequest} from the given request context,
     * without any pagination limit. Implementations that support the optimised export flow
     * (build-once, paginate-many) should override this method.
     *
     * <p>The default implementation throws {@link UnsupportedOperationException}.</p>
     */
    default @NonNull UnderlyingDataRequest buildUnderlyingRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                                                  final @NonNull ActionType actionType,
                                                                  final @NonNull JsonObject requestData,
                                                                  final @NonNull I18nResolver i18nResolver,
                                                                  final Map<Long, ReportModularField> reportFieldMap) {
        throw new UnsupportedOperationException("buildUnderlyingRequest is not supported for this data type");
    }

    /**
     * Retrieves graph axis data using an already-built {@link UnderlyingDataRequest}, applying only the
     * given pagination window. Implementations that support the optimised export flow should override this.
     *
     * <p>The default implementation throws {@link UnsupportedOperationException}.</p>
     */
    default @NonNull List<GraphAxis<Object>> getGraphAxis(final @NonNull UnderlyingDataRequest builtRequest,
                                                          final int start,
                                                          final int count) {
        throw new UnsupportedOperationException("getGraphAxis with pre-built request is not supported for this data type");
    }

    /**
     * <p>
     * Gets the {@link JsonObject} response for given list of {@link GraphAxis}
     * </p>
     *
     * @param orgId      Represents an organisation id
     * @param graphAxes  Contains the list of {@link GraphAxis}
     * @param crmUserMap The user id as a key and {@link CrmUser} as value
     * @return The {@link JsonObject}
     */
    JsonObject getResponse( final @NonNull Long orgId,
                            final @NonNull List<GraphAxis<Object>> graphAxes,
                            final @NonNull Map<Long, CrmUser> crmUserMap,
                            final @NonNull I18nResolver i18nResolver );
}
