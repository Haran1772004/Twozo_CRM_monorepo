package com.tlc.cdc.core.enrichment;

import com.tlc.commons.json.JsonObject;

import java.util.Collection;
import java.util.Map;

/**
 * Enriches CDC records consumed from Kafka by applying domain-specific
 * transformations or augmentations before publishing or persisting the data.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface CdcEnricher {

    /**
     * Enriches the CDC record with additional data.
     */
    Collection<EnrichedRecord> toEnrichedRecords(final Map<String, Collection<JsonObject>> groupedRecords);
}
