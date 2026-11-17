package com.tlc.crm.core.features.api.dataimport.model.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.internal.dataimport.handler.UpdatableFieldsExtractor;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.core.features.api.dataimport.model.UpdateHistory;
import com.tlc.crm.core.features.api.dataimport.model.UpdateHistoryGenerator;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Provides an implementation of the {@link UpdateHistoryGenerator} to generate a {@link UpdateHistory}.
 *
 * @author sundar
 * @version 1.0
 * @see UpdateHistoryGenerator
 */
public class UpdateHistoryGeneratorImpl implements UpdateHistoryGenerator {

    private static final String ID_KEY = FieldKeys.ID.getName();
    private static final String VALUE_KEY = FieldKeys.VALUE.getName();

    private final UpdatableFieldsExtractor updatableFieldsExtractor;

    private static final class Instance {
        private static final UpdateHistoryGenerator UPDATE_HISTORY_GENERATOR = new UpdateHistoryGeneratorImpl();
    }

    private UpdateHistoryGeneratorImpl() {
        updatableFieldsExtractor = UpdatableFieldsExtractor.getInstance();
    }

    /**
     * Gets the {@link UpdateHistoryGenerator} instance
     *
     * @return {@link UpdateHistoryGenerator}
     */
    public static UpdateHistoryGenerator getInstance() {
        return Instance.UPDATE_HISTORY_GENERATOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organization ID
     * @param source       Represents the {@link Source} for which the snapshot is being generated
     * @param previousJson The previous data of the record in JSON format
     * @param currentJson  The current data of the record in JSON format
     * @return The generated {@link UpdateHistory}
     */
    @Override
    public UpdateHistory generate(final Long orgId, final Source source, final JsonObject previousJson, final JsonObject currentJson) {
        final Collection<String> fields = updatableFieldsExtractor.extractUpdatedFieldKeys(orgId, source, previousJson, currentJson);

        if (Objects.isNull(fields) || fields.isEmpty()) {
            return null;
        }
        final JsonObject previousState = Json.object();
        final JsonObject currentState = Json.object();

        for (final String key : fields) {
            previousState.put(key, previousJson.opt(key));
            currentState.put(key, currentJson.opt(key));
        }

        return new UpdateHistory(previousState, currentState);
    }

    /**
     * Reformat the provided value and retrieve the relevant value of a specified field.
     *
     * @param field          Represents the {@link FieldInfo}
     * @param primaryValue   Represents the primary value to be refined
     * @param secondaryValue Represents the secondary value to be refined
     * @return the refined value of a specified field
     */
    private ValuePair<?> reformat(final FieldInfo field, final Object primaryValue, final Object secondaryValue) {
        return switch (field.storeType()) {
            case CONTACT, CONTACT_ONBOARD, COMPANY, COMPANY_ONBOARD, OWNER, SINGLE_NUMBER, DROPDOWN ->
                    new ValuePair<>(fetchId(primaryValue), fetchId(secondaryValue));
            case MULTI_SELECT, CONTACTS_ONBOARD ->
                    new ValuePair<>(fetchArray(primaryValue), fetchArray(secondaryValue));
            default -> new ValuePair<>(primaryValue, secondaryValue);
        };
    }

    /**
     * Retrieves the primary ID from the given value if it is a model value of our {@link StoreType}.
     *
     * @param value The value used to fetch the ID.
     * @return The primary ID if the given value is a valid object.
     */
    private Long fetchId(final Object value) {
        if (value instanceof final Number number) {
            return number.longValue();
        } else if (value instanceof final JsonObject jsonValue) {

            if (jsonValue.containsKey(ID_KEY)) {
                return jsonValue.getLong(ID_KEY);
            } else if (jsonValue.containsKey(VALUE_KEY)) {
                return jsonValue.getLong(VALUE_KEY);
            }
        } else if (value instanceof final Map<?, ?> mapValue) {
            final Object idValue = mapValue.containsKey(ID_KEY) ? mapValue.get(ID_KEY) : mapValue.getOrDefault(VALUE_KEY, null);

            if (idValue instanceof final Number number) {
                return number.longValue();
            }
        }
        return null;
    }

    /**
     * Fetches the {@link JsonArray} of containing multiple values if the provided value containing multiple values
     *
     * @param value The value used to obtain the collection of values
     * @return {@link JsonArray} that will contain the multi values
     */
    private JsonArray fetchArray(final Object value) {
        final JsonArray jsonArray;

        if (value instanceof final Collection<?> values) {
            jsonArray = Json.array();

            for (final Object optionValue : values) {
                jsonArray.put(fetchId(optionValue));
            }
        } else if (value instanceof final JsonArray values) {
            jsonArray = Json.array();

            values.forEach(optionValue -> jsonArray.put(fetchId(optionValue)));
        } else {
            jsonArray = null;
        }
        return jsonArray;
    }

    /**
     * Container that stores the formatted previous and current values
     *
     * @param previousValue The previous value to be stored
     * @param currentValue  The current value to be stored
     * @param <T>           Represents the type of those values
     */
    private record ValuePair<T>(T previousValue, T currentValue) {

    }
}