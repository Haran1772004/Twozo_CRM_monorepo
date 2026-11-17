package com.tlc.crm.core.api.analytics.cdc;

import com.tlc.analytics.sql.resource.OACDIMTERRITORY;

import com.tlc.cdc.core.enrichment.EnrichedRecord;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;
import com.tlc.crm.core.api.analytics.general.keys.AnalyticsCdcKeys;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Enriches CRM territory CDC events with analytics specific data.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
final class AnalyticsCrmTerritoryEnricher extends AbstractAnalyticsCdcEnricher {

    private AnalyticsCrmTerritoryEnricher() {
    }

    private static class Instance {
        private static final AnalyticsCrmTerritoryEnricher TERRITORY_ENRICHER = new AnalyticsCrmTerritoryEnricher();
    }

    static AnalyticsCrmTerritoryEnricher getInstance() {
        return Instance.TERRITORY_ENRICHER;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupedRecords Map of JSON objects to be enriched which is grouped by table.
     * @return Collection of enriched records.
     */
    @Override
    public Collection<EnrichedRecord> toEnrichedRecords(final Map<String, Collection<JsonObject>> groupedRecords) {
        final Collection<JsonObject> records = groupedRecords.get(OMCRMTERRITORY.TABLE.toLowerCase());
        final Collection<EnrichedRecord> enrichedRecords = new ArrayList<>();

        if (Objects.nonNull(records) && !records.isEmpty()) {
            for (final JsonObject territoryJson : records) {
                final String operation = territoryJson.getString(AnalyticsCdcKeys.OPERATION);
                final long transactionId = territoryJson.getJsonObject(AnalyticsCdcKeys.SOURCE).getLong(AnalyticsCdcKeys.TXN_ID);
                final boolean isDeleteEvent = Objects.equals(AnalyticsCdcKeys.DELETE_OPERATION, operation);
                final JsonObject jsonObject = isDeleteEvent ? getRowJson(tableResolver.resolve(OACDIMTERRITORY.TABLE), territoryJson.getJsonObject(AnalyticsCdcKeys.BEFORE).getLong(OMCRMTERRITORY.ID)) : Json.object();

                if (isDeleteEvent) {
                    jsonObject.put(OACDIMTERRITORY.UPDATED_AT, TemporalServiceProvider.get(Instant.now().toEpochMilli()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                } else {
                    final JsonObject cdcJson = territoryJson.getJsonObject(AnalyticsCdcKeys.AFTER);

                    jsonObject.put(OACDIMTERRITORY.ID, cdcJson.getLong(OMCRMTERRITORY.ID));
                    jsonObject.put(OACDIMTERRITORY.ORG_ID, cdcJson.getLong(OMCRMTERRITORY.ORG_ID));
                    jsonObject.put(OACDIMTERRITORY.NAME, cdcJson.getString(OMCRMTERRITORY.NAME));
                    jsonObject.put(OACDIMTERRITORY.CREATED_AT, TemporalServiceProvider.get(cdcJson.getLong(OMCRMTERRITORY.CREATED_TIME)).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                    jsonObject.put(OACDIMTERRITORY.UPDATED_AT, TemporalServiceProvider.get(cdcJson.getLong(OMCRMTERRITORY.UPDATED_TIME)).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                }

                jsonObject.put(OACDIMTERRITORY.IS_DELETED, isDeleteEvent);
                jsonObject.put(AnalyticsCdcKeys.TOPIC, OACDIMTERRITORY.TABLE.toLowerCase());

                enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
            }
        }

        return enrichedRecords;
    }
}
