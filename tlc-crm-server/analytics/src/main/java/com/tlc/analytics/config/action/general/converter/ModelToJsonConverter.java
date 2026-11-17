package com.tlc.analytics.config.action.general.converter;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.commons.util.executor.NullSafeExecutor;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.entity.Entity;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.converter.AbstractConverter;
import com.tlc.entity.converter.ConverterParams;
import com.tlc.entity.converter.ConverterUtil;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * Provides different ways to convert the data from model into json model
 *
 * @author Jamesh
 * @version 1.0
 * @see AbstractConverter
 * @see ReportsClientConverter
 */
class ModelToJsonConverter extends AbstractConverter<JsonObject> implements ReportsClientConverter<JsonObject>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( ModelToJsonConverter.class );
    private static final ModelToJsonConverter MODEL_TO_JSON_CONVERTER = new ModelToJsonConverter();
    private static final ThreadLocal<BiFunction<String, String, Boolean>> predicateFunction  = new ThreadLocal<>();

    private ModelToJsonConverter(){}

    static ModelToJsonConverter getMJConverter( final @NonNull BiFunction<String, String, Boolean> predicate )
    {
        predicateFunction.set( predicate );
        return MODEL_TO_JSON_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param entity The source entity.
     * @param target The target object.
     */
    @Override
    protected void preConversion( final Entity<?> entity, final JsonObject target )
    {
    }

    /**
     * {@inheritDoc}
     *
     * @param entity The source entity.
     * @param target The target object.
     */
    @Override
    protected void postProcess( final Entity<?> entity, final JsonObject target )
    {
    }

    /**
     * {@inheritDoc}
     *
     * @param columnName      The name of the primary key column.
     * @param converterParams The parameters required for conversion.
     * @throws Exception If there is an issue accessing or setting the primary key field
     */
    @Override
    protected void handlePKColumn( final String columnName, ConverterParams<JsonObject> converterParams ) throws Exception
    {
        final Field pkField = converterParams.field();
        final Entity<?> entity = converterParams.entity();
        final Long value = (Long) pkField.get(entity);
        final String jsonKey = getKey(pkField);

        Objects.requireNonNull( value);
        converterParams.target().put( jsonKey, value );
        LOGGER.debug( "Analytics: Added primary key value: {} into the target with key: {}", value, jsonKey );
    }

    /**
     * {@inheritDoc}
     *
     * @param columnName      The name of the column.
     * @param converterParams The parameters required for conversion.
     * @throws Exception If there is an issue accessing or setting the column field
     */
    @Override
    protected void handleColumn( final String columnName, ConverterParams<JsonObject> converterParams ) throws Exception
    {
        final String tableName = converterParams.tableName();
        final Object value = converterParams.field().get( converterParams.entity() );

        if( Objects.nonNull( value ) )
        {
            final String jsonKey = getKey( converterParams.field() );

            if( predicateFunction.get()
                                 .apply( columnName, tableName ) )
            {
                converterParams.target()
                               .put( jsonKey, converterParams.valueTransformer()
                               .apply( tableName, columnName, value ) );
            }
        }
        LOGGER.debug( "Analytics: Added transformed value for column {} from table {} to the target JSON key {}", columnName, tableName, value );
    }

    /**
     * {@inheritDoc}
     *
     * @param converterParams The parameters required for conversion.
     * @param join            The annotation defining the join.
     * @throws Exception If an error occurs during conversion or if the primary key validation fails
     */
    @Override
    protected void handleSingularRelation( ConverterParams<JsonObject> converterParams, Join join ) throws Exception
    {
        LOGGER.info( "Analytics: Handling the singular relation between tables." );

        final Object value  = converterParams.field().get( converterParams.entity() );
        NullSafeExecutor.acceptIfPresentSafely( value, nonNullValue ->
        {
            final BaseModel refModel = ConverterUtil.typecastAndGet( BaseModel.class, value );
            NullSafeExecutor.acceptIfPresentSafely(findUsedJoinTable( join.joinTables(), refModel ), joinTableAnnot ->
            {
                final String jsonKey = getKey( converterParams.field() );
                final JsonObject newJson = Json.object();

                convert( refModel, newJson, converterParams.valueTransformer() );
                converterParams.target().put(jsonKey, newJson);
            });
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param converterParams The parameters required for conversion.
     * @param join            The annotation defining the join.
     * @throws Exception If an error occurs during conversion or if related models cannot be found or converted
     */
    @Override
    protected void handlePluralRelation( ConverterParams<JsonObject> converterParams, Join join ) throws Exception
    {
        LOGGER.info("Analytics: Handling the plural relations between tables.");
        final Object value  = converterParams.field().get( converterParams.entity() );
        NullSafeExecutor.acceptIfPresentSafely( value, nonNullValue ->
        {
            final Collection< ? > refModelList = ConverterUtil.typecastAndGet( Collection.class, value );
            final Object objClass = refModelList.stream().findFirst().orElseGet( () -> null );
            NullSafeExecutor.acceptIfPresentSafely( findUsedJoinTable( join.joinTables(),
                                                                       ConverterUtil.typecastAndGet( BaseModel.class ,
                                                                                                     objClass )),
            joinTableAnnot ->
            {
                final String jsonKey = getKey( converterParams.field() );
                final JsonArray newJsonArr = Json.array();
                for( Object obj : refModelList )
                {
                    final JsonObject newJsonObj = Json.object();
                    final BaseModel baseModel = ConverterUtil.typecastAndGet( BaseModel.class, obj );

                    convert( baseModel, newJsonObj, converterParams.valueTransformer() );
                    newJsonArr.put( newJsonObj );
                }
                converterParams.target().put( jsonKey, newJsonArr );
                LOGGER.debug( "Analytics: Successfully added transformed values for field: '{}'.", jsonKey );
            });
        });
    }

    /**
     * <p>
     * Finds the used join table for a given reference model.
     * </p>
     *
     * @param joinTables the array of join tables to search
     * @param refModel the reference model to find the join table for
     * @return the used join table, or null if not found
     */
    private JoinTable findUsedJoinTable( final JoinTable[] joinTables, final BaseModel refModel )
    {
        return Stream.of( joinTables )
                     .filter( joinTable -> Objects.equals( refModel.getClass(), joinTable.entityClass() ) )
                     .findFirst()
                     .orElseGet( () -> null );
    }
}
