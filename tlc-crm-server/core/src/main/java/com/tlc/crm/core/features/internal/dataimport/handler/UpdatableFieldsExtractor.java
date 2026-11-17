package com.tlc.crm.core.features.internal.dataimport.handler;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.internal.dataimport.handler.impl.UpdatableFieldsExtractorImpl;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;

/**
 * <p>
 * Compares the json and extracts the updated field list
 * </p>
 *
 * @author Muthu kumar V
 * @version 2.0
 */
public interface UpdatableFieldsExtractor {

    /**
     * Gets the {@link UpdatableFieldsExtractor} instance
     *
     * @return {@link UpdatableFieldsExtractor}
     */
    static UpdatableFieldsExtractor getInstance() {
        return UpdatableFieldsExtractorImpl.getInstance();
    }

    /**
     * <p>
     * Compares the previous and current state json and extracts the collection of updated field keys
     * </p>
     *
     * @param orgId             Represents the organization ID
     * @param source            Represents the {@link Source}
     * @param previousStateJson Represents the previous state json
     * @param currentStateJson  Represents the current state json
     * @return The extracted Collection of updated field keys
     */
    Collection<String> extractUpdatedFieldKeys(final Long orgId, final Source source, final JsonObject previousStateJson, final JsonObject currentStateJson);
}