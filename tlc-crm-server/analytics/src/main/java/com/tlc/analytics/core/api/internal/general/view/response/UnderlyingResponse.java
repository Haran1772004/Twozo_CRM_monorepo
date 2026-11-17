package com.tlc.analytics.core.api.internal.general.view.response;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.analytics.config.keys.data.UnderlyingDataKeys;
import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsConfiguredField;
import com.tlc.analytics.core.api.ViewResponse;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.UnderlyingDataRequest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.executor.CodeBlockExecutor;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a response containing underlying data.
 *
 * @author KarthickV
 * @version 1.0
 */
@Getter
@SuperBuilder
public class UnderlyingResponse implements ViewResponse<UnderlyingDataRequest>
{
    @NonNull
    private final List<GraphAxis<Object>> graphAxes;
    private static final String DEFAULT_VALUE = "Null";

    /**
     * {@inheritDoc}
     *
     * @param request The request object containing parameters for generating the response.
     * @return A {@link JsonObject} representing the JSON response.
     */
    @Override
    public @NonNull JsonObject getJsonResponse( final @NonNull UnderlyingDataRequest request )
    {
        final JsonObject dataJson = Json.object();

        try
        {
            final Set<String> columnHeaders = new LinkedHashSet<>();
            final List<String> values = new ArrayList<>();
            final JsonObject jsonObject = Json.object();
            final JsonArray jsonArray = Json.array();

            CodeBlockExecutor.forEachSafely( graphAxes,
                                             graphAxis ->
                                             {
                                                 final Object value = graphAxis.getValue();

                                                 columnHeaders.add( graphAxis.getColumnName()
                                                                             .toString() );
                                                 values.add( Objects.isNull( value ) ? DEFAULT_VALUE : value.toString() );
                                             } );
            final List<String> headers = new ArrayList<>( columnHeaders );
            final int headerCount = headers.size();
            final int totalCount = values.size();

            for( int index = 0; index < totalCount; index += headerCount )
            {
                final JsonArray rowValues = Json.array();

                for( int headerIndex = 0; headerIndex < headerCount; headerIndex++ )
                {
                    final int indexCount =  index + headerIndex;

                    if( indexCount < totalCount )
                    {
                        rowValues.put( values.get( indexCount ) );
                    }
                }
                jsonArray.put( rowValues );
            }

            jsonObject.put( UnderlyingDataKeys.HEADERS, headers );
            jsonObject.put( UnderlyingDataKeys.VALUES, jsonArray );

            dataJson.put( UnderlyingDataKeys.TOTAL_RECORDS, jsonArray.size() );
            dataJson.put( UnderlyingDataKeys.ROWS, jsonObject );
            dataJson.put( UnderlyingDataKeys.STRUCTURE, fetchStructure( request ) );
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( ConfigErrorCodes.UNDERLYING_DATA_RESPONSE_FAILED,
                        "i18n.analytics.underlying.data.response.failed" );
        }
        return dataJson;
    }

    /**
     * {@inheritDoc}
     *
     * @param request The request object containing parameters to fetch the graph axis.
     * @return A list of GraphAxis objects representing the axes of the graph.
     */
    @Override
    public @NonNull List<GraphAxis<Object>> getGraphAxis( final @NonNull UnderlyingDataRequest request )
    {
        return graphAxes;
    }

    /**
     * <p>
     * Fetches the structure based on the configured fields
     * </p>
     *
     * @param underlyingDataRequest The request object containing parameters for generating the response
     * @return {@link JsonObject} of structure
     */
    @NonNull
    private JsonObject fetchStructure( final UnderlyingDataRequest underlyingDataRequest )
    {
        final JsonObject structureJson = Json.object();
        final JsonArray fieldsArray = Json.array();
        final Collection<AnalyticsConfiguredField> analyticsConfiguredFields = underlyingDataRequest.getView()
                                                                                                    .getConfiguredFieldDetails();

        try
        {
            CodeBlockExecutor.forEachSafely( analyticsConfiguredFields,
                                             analyticsConfiguredField ->
                                             {
                                                 final JsonObject jsonObject = Json.object();

                                                 jsonObject.put( UnderlyingDataKeys.ID, analyticsConfiguredField.id() );
                                                 jsonObject.put( UnderlyingDataKeys.DISPLAY_NAME, analyticsConfiguredField.fieldName() );
                                                 jsonObject.put( UnderlyingDataKeys.HIDDEN, analyticsConfiguredField.hidden() );

                                                 fieldsArray.put( jsonObject );
                                             } );

            structureJson.put( UnderlyingDataKeys.COLUMNS, fieldsArray );

            return structureJson;

        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( ConfigErrorCodes.UNDERLYING_DATA_RESPONSE_FAILED,
                         "i18n.analytics.underlying.data.response.failed" );
        }
    }
}
