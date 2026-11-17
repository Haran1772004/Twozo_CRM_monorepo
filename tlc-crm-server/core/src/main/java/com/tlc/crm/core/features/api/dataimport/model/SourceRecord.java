package com.tlc.crm.core.features.api.dataimport.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * This model organizes and stores the data retrieved from an Excel row based on the specific source it belongs to
 *
 * @author Sundar
 * @version 1.0
 */
public class SourceRecord {

    private final Map<Source, JsonObject> sourceJsonMap;
    private final int index;

    public SourceRecord(final int index) {
        this.sourceJsonMap = new EnumMap<>(Source.class);
        this.index = index;
    }

    /**
     * Adds the value to the map associated with the provided source
     *
     * @param source Represents the entity
     * @param value  Represents the value for the specified field
     */
    public void put(final Source source, final JsonObject value) {
        sourceJsonMap.put(source, value);
    }

    /**
     * Gets the {@link JsonObject} of specified source
     *
     * @param source Represents the source
     * @return {@link JsonObject} which contains the data of specified source
     */
    public JsonObject get(final Source source) {
        return sourceJsonMap.get(source);
    }

    /**
     * Gets the collection of sources which contains the data
     *
     * @return {@link Collection} of sources
     */
    public Collection<Source> sources() {
        return sourceJsonMap.keySet();
    }

    /**
     * Gets the index of the {@link SourceRecord}
     *
     * @return the index of the {@link SourceRecord}
     */
    public int index() {
        return index;
    }
}
