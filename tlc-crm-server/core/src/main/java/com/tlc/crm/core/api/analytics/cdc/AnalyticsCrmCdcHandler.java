package com.tlc.crm.core.api.analytics.cdc;

import com.tlc.cdc.core.enrichment.EnrichedRecord;
import com.tlc.cdc.core.event.CdcEvent;
import com.tlc.cdc.core.models.AggregatedCdcEventRequest;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.cdc.CrmCdcEventHandler;
import com.tlc.crm.core.api.analytics.general.keys.AnalyticsCdcKeys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Handles CDC events for analytics processing by aggregating, enriching, and publishing the data.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@CdcEvent(event = "analytics")
public class AnalyticsCrmCdcHandler extends CrmCdcEventHandler {

    /**
     * <p>
     * Set of alias names that the analytics pipeline knows how to enrich.
     * </p>
     */
    private static final Collection<String> ANALYTICS_SUPPORTED_ALIASES = Set.of(
            AnalyticsCdcKeys.COMPANY, AnalyticsCdcKeys.CONTACT, AnalyticsCdcKeys.DEAL,
            AnalyticsCdcKeys.PRODUCT, AnalyticsCdcKeys.DEAL_LINE_ITEMS, AnalyticsCdcKeys.ACTIVITY,
            AnalyticsCdcKeys.TERRITORY, AnalyticsCdcKeys.USER, AnalyticsCdcKeys.ENTITY_INSIGHT
    );

    public AnalyticsCrmCdcHandler() {
    }

    /**
     * {@inheritDoc}
     *
     * @param aggregatedCdcEventRequest The aggregated CDC event containing transaction metadata and processed payload.
     */
    @Override
    protected void secureHandle(final AggregatedCdcEventRequest aggregatedCdcEventRequest) {
        LOGGER.info("Analytics CDC: Going to process the aggregated CDC data for request ID: {}", aggregatedCdcEventRequest.id());

        final JsonArray jsonArray = aggregatedCdcEventRequest.data().getJsonArray(AnalyticsCdcKeys.EVENT);
        final Map<String, Map<String, Collection<JsonObject>>> groupedCdcJsonByModule = new HashMap<>();
        final Collection<EnrichedRecord> enrichedRecords = new LinkedList<>();
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            final JsonObject jsonObject = jsonArray.getJsonObject(index);
            final String table = jsonObject.getJsonObject(AnalyticsCdcKeys.SOURCE).getString(AnalyticsCdcKeys.TABLE);
            final String aliasName = cdcConnectorManager.getAliasName(table);

            if (Objects.isNull(aliasName) || !ANALYTICS_SUPPORTED_ALIASES.contains(aliasName)) {
                LOGGER.debug("Analytics CDC: Skipping event from table '{}' — alias '{}' has no registered analytics enricher.",
                        table, aliasName);
                continue;
            }

            groupedCdcJsonByModule.computeIfAbsent(aliasName, k -> new HashMap<>())
                    .computeIfAbsent(table, k -> new ArrayList<>()).add(jsonObject);
        }

        for (final String aliasName : groupedCdcJsonByModule.keySet()) {
            LOGGER.info("Analytics CDC: Going to enrich layer for the module: {}", aliasName);
            final AnalyticsCdcEnricher analyticsCdcEnricher = AnalyticsCdcEnricher.get(aliasName);
            final Collection<EnrichedRecord> cdcRecords = analyticsCdcEnricher.toEnrichedRecords(groupedCdcJsonByModule.get(aliasName));

            if (!cdcRecords.isEmpty()) {
                enrichedRecords.addAll(cdcRecords);
            }
        }

        cdcPublisher.publish(enrichedRecords);
    }
}
