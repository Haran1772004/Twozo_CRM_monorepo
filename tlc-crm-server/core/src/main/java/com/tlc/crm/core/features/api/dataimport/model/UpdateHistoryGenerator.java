package com.tlc.crm.core.features.api.dataimport.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.api.dataimport.model.impl.UpdateHistoryGeneratorImpl;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import java.util.Collection;

/**
 * Generator to produce a {@link UpdateHistory} based on the provided previous and current data of a {@link Source}
 *
 * @author sundar
 * @version 1.0
 */
public interface UpdateHistoryGenerator {

    /**
     * Gets the {@link UpdateHistoryGenerator} instance
     *
     * @return {@link UpdateHistoryGenerator}
     */
    static UpdateHistoryGenerator get() {
        return UpdateHistoryGeneratorImpl.getInstance();
    }

    /**
     * Generates a {@link UpdateHistory} based on the provided previous and current data of a specific record from a {@link Source}
     *
     * @param orgId        Represents the organization ID
     * @param source       Represents the {@link Source} for which the {@link UpdateHistory} is being generated
     * @param previousJson The previous data of the record in JSON format
     * @param currentJson  The current data of the record in JSON format
     * @return The generated {@link UpdateHistory}
     */
    UpdateHistory generate(final Long orgId, final Source source, final JsonObject previousJson,
                           final JsonObject currentJson);

}
