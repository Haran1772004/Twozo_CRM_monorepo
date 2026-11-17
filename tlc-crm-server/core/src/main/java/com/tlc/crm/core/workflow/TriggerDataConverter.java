package com.tlc.crm.core.workflow;

import com.tlc.validator.TlcModel;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.core.workflow.model.TriggerInfo;
import com.tlc.crm.core.workflow.internal.trigger.converter.TriggerDataConverterImpl;

import java.util.Collection;
import java.util.Map;

/**
 * The processor is tasked with encoding data into Json
 * and decoding them back, serving the purpose for workflow triggering.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface TriggerDataConverter {

    /**
     * Gets the instance of {@link TriggerDataConverter}.
     *
     * @return The instance of {@link TriggerDataConverter}.
     */
    static TriggerDataConverter getInstance() {
        return TriggerDataConverterImpl.getInstance();
    }

    /**
     * Encapsulates the provided data into {@link JsonObject}.
     *
     * @param orgId  Refers the organization id.
     * @param models Refers the models to be encapsulated.
     * @return The wrapped json object.
     */
    JsonObject convertToJson(final Long orgId, final Collection<? extends TlcModel> models, final TriggerInfo triggerInfo);

    /**
     * Converts the {@link JsonObject} into the respective collection of models.
     *
     * @param jsonObject Refers the json object to be unwrapped.
     * @param source     Refers the source of the model.
     * @param action     Refers the action event.
     * @return The collection of models.
     */
    Collection<Pair<TlcModel, TlcModel>> extractModelPairs(final JsonObject jsonObject, final Source source,
                                                           final Action action, final Map<Long, Long> entityStateIdMap);
}
