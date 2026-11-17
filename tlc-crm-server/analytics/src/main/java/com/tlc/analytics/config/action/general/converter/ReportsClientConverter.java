package com.tlc.analytics.config.action.general.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.entity.converter.Converter;
import lombok.NonNull;

import java.util.function.BiFunction;

/**
 * Provides different ways to convert the reports data
 *
 * @author Jamesh
 * @version 1.0
 * @see Converter
 */
public interface ReportsClientConverter<T> extends Converter<T> {

    /**
     * <p>
     * Retrieves a converter that converts json objects to data models
     * </p>
     *
     * @return a {@link ReportsClientConverter} that converts {@link JsonObject} to data models
     */
    static ReportsClientConverter<JsonObject> getJsonToModelConverter()
    {
        return JsonToModelConverter.getJMConverter();
    }

    /**
     * <p>
     * Retrieves a converter that converts data models to JSON objects
     * </p>
     *
     * @param predicate a {@link BiFunction} that determines whether to include or exclude specific
     *                  fields based on their table name and column name
     * @return a {@link ReportsClientConverter} that converts data models to {@link JsonObject} using the specified predicate
     */
    static ReportsClientConverter<JsonObject> getModelToJsonConverter( final @NonNull BiFunction<String, String, Boolean> predicate )
    {
        return ModelToJsonConverter.getMJConverter(predicate);
    }
}
