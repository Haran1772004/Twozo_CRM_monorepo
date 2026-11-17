package com.tlc.crm.core.api.analytics.converter.data.view;

import com.tlc.analytics.core.api.ChartRequest;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.api.analytics.converter.data.CRMReportsData;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.i18n.I18nResolver;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The {@link BubbleChartData} class is responsible for handling the processing of bubble chart data
 * requests within the CRM reporting system.
 *
 * @author karthick V
 * @version 1.0
 * @see CRMReportsData
 */
class BubbleChartData extends AbstractViewData {
    private final CRMBubbleChartDataConverter bubbleChartDataConverter;

    private BubbleChartData() {
        bubbleChartDataConverter = CRMBubbleChartDataConverter.INSTANCE();
    }

    private static class Instance {
        private static final BubbleChartData INSTANCE = new BubbleChartData();
    }

    public static BubbleChartData INSTANCE() {
        return BubbleChartData.Instance.INSTANCE;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BubbleChartData.class);

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organisation id
     * @param graphAxes  Contains the list of {@link GraphAxis}
     * @param crmUserMap The map key represents the user id and value is {@link CrmUser} details
     * @return The {@link JsonObject}
     */
    @Override
    public final JsonObject getResponse(final @NonNull Long orgId,
                                        final @NonNull List<GraphAxis<Object>> graphAxes,
                                        final @NonNull Map<Long, CrmUser> crmUserMap,
                                        final @NonNull I18nResolver i18nResolver) {
        try {
            return bubbleChartDataConverter.getData(orgId, graphAxes, crmUserMap, i18nResolver);
        } catch (final Exception error) {
            LOGGER.error("CRM Analytics: Failed to fetch chart response for orgId: {}. Reason: {}", orgId, error);
            throw ErrorCode.get(AnalyticsErrorCodes.CHART_DATA_FETCH_ACTION_FAILED,
                    "i18n.analytics.chart.data.fetch.action.failed");
        }
    }

    /**
     * Merges all {@link GraphAxis} lists from the given map into a single list.
     *
     * @param graphAxisMap a map where each key is a {@link ChartRequest} and each value
     *                     is a list of {@link GraphAxis} objects
     * @return a combined list containing all {@link GraphAxis} instances from the map values
     */
    @Override
    protected List<GraphAxis<Object>> mergeGraphAxis(final Map<ChartRequest, List<GraphAxis<Object>>> graphAxisMap) {
        return graphAxisMap.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }
}
