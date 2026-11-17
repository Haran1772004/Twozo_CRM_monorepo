package com.tlc.analytics.config.action.general.converter;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.analytics.config.model.BaseModel;

import com.tlc.commons.util.executor.NullSafeExecutor;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.entity.Entity;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.converter.AbstractConverter;
import com.tlc.entity.converter.ConverterParams;
import com.tlc.entity.converter.ConverterUtil;

import lombok.Builder;
import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Provides different ways to convert the data from json object into model
 *
 * @author Jamesh
 * @version 1.0
 * @see AbstractConverter
 * @see ReportsClientConverter
 */
class JsonToModelConverter extends AbstractConverter<JsonObject>
                           implements ReportsClientConverter<JsonObject>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( JsonToModelConverter.class );
    private static final JsonToModelConverter JSON_TO_MODEL_CONVERTER  = new JsonToModelConverter();

    /**
     * {@inheritDoc}
     *
     * @param entity The source entity.
     * @param target The target object.
     */
    @Override
    protected void preConversion( final @NonNull Entity<?> entity,
                                  final @NonNull JsonObject target )
    {}

    /**
     * {@inheritDoc}
     *
     * @param entity The source entity.
     * @param dataContainer The target object.
     */
    @Override
    protected void postProcess( final @NonNull Entity<?> entity,
                                final @NonNull JsonObject dataContainer )
    {}

    private JsonToModelConverter()
    {
    }

    static JsonToModelConverter getJMConverter()
    {
        return JSON_TO_MODEL_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param columnName      The name of the primary key column.
     * @param converterParams The parameters required for conversion.
     * @throws Exception If there is an issue accessing or setting the primary key field
     */
    @Override
    protected void handlePKColumn( final @NonNull String columnName,
                                   final @NonNull ConverterParams<JsonObject> converterParams ) throws Exception
    {
        final Field pkField = converterParams.field();
        final Entity<?> entity = converterParams.entity();
        final String jsonKey = getKey( pkField );

        if( converterParams.target().containsKey( jsonKey ) )
        {
            final Object value = convertValue( converterParams.target()
                                                              .get( jsonKey ) );

            pkField.set( entity, value );
        }
        LOGGER.debug( "Analytics: Setting primary key field {} with value from JSON key {} on the entity", pkField.getName(), entity.getClass().getSimpleName() );
    }

    /**
     * {@inheritDoc}
     *
     * @param columnName      The name of the column.
     * @param converterParams The parameters required for conversion.
     * @throws Exception If there is an issue accessing or setting the column field
     */
    @Override
    protected void handleColumn( final @NonNull String columnName,
                                 final @NonNull ConverterParams<JsonObject> converterParams ) throws Exception
    {
        final Field columnField   = converterParams.field();
        final Entity<?> entity     = converterParams.entity();
        final String jsonKey      = getKey( columnField );
        final String tableName = converterParams.tableName();

        if( converterParams.target().containsKey( jsonKey ) )
        {
            final Object value = convertValue( converterParams.target().get( jsonKey ) );

            columnField.set( entity, converterParams.valueTransformer()
                                                    .apply( tableName, columnName, value ) );
        }
        LOGGER.debug( "Analytics: Setting column field {} for table {} with value from JSON key {} on the entity", columnField.getName(), tableName, entity.getClass().getSimpleName() );
    }

    /**
     * {@inheritDoc}
     *
     * @param converterParams The parameters required for conversion.
     * @param join            The annotation defining the join.
     * @throws Exception If an error occurs during conversion or if the primary key validation fails
     */
    @Override
    protected void handleSingularRelation( final @NonNull ConverterParams<JsonObject> converterParams,
                                           final @NonNull Join join ) throws Exception
    {
        NullSafeExecutor.acceptIfPresentSafely( findPresentJoin( join.joinTables(), converterParams ), tableHolder ->
        {
            final Field refField   = converterParams.field();
            final Entity<?> entity  = converterParams.entity();
            final JsonObject refModelJson = converterParams.target()
                                                           .optJsonObject( tableHolder.tableKeyName );

            if( Objects.isNull( refModelJson ) )
            {
                refField.set( entity, null );
            }
            else
            {
                final Field refModelPK = ConverterUtil.getPKField( tableHolder.joinTable
                                                                              .entityClass() );
                final Object refPK = refModelJson.opt( getKey( refModelPK ) );
                final Entity<?> refEntity;

                if( Objects.isNull( refPK ) )
                {
                    refEntity = ConverterUtil.newInstance( tableHolder.joinTable.entityClass() );
                    refField.set( entity, refEntity );
                }
                else
                {
                    final Object refModelObject = refField.get( entity );

                    refEntity = ConverterUtil.typecastAndGet( Entity.class, refModelObject );
                    final Object pkValue = refEntity.id();

                    if( !String.valueOf( refPK ).equals( String.valueOf( pkValue ) ) )
                    {
                        throw ErrorCode.get( ConfigErrorCodes.INVALID_COLUMN_CONFIGURATION,
                                    "i18n.analytics.invalid.column" );
                    }
                }
                convert( refEntity, refModelJson, converterParams.valueTransformer() );
            }
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
    protected void handlePluralRelation( final @NonNull ConverterParams<JsonObject> converterParams,
                                         final @NonNull Join join) throws Exception
    {
        LOGGER.info( "Analytics: Handling plural table relations" );
        NullSafeExecutor.acceptIfPresentSafely( findPresentJoin( join.joinTables(), converterParams ), tableHolder ->
        {
            final Field refCollectionField = converterParams.field();
            final JsonObject dataChanges = converterParams.target().optJsonObject( getKey( refCollectionField ) );
            final Entity<?> entity = converterParams.entity();

            if( Objects.isNull( dataChanges ) )
            {
                refCollectionField.set(entity, null);
            }
            else
            {
                 final List<BaseModel> refModels = NullSafeExecutor.getIfPresent( refCollectionField.get( entity ),
                        refModelobj -> ConverterUtil.typecastAndGet( List.class, refModelobj ) ).orElseGet( ArrayList::new );

                if( dataChanges.containsKey("delete" ) )
                {
                    final JsonArray toBeDeleted = dataChanges.getJsonArray("delete" );

                    for( int index = 0; index < toBeDeleted.size(); index++ )
                    {
                        final Object id = toBeDeleted.get(index);

                        refModels.removeIf( refModel -> Objects.equals( id, refModel.id() ) );
                    }
                }

                if( dataChanges.containsKey("update" ) )
                {
                    final JsonArray toBeUpdated = dataChanges.getJsonArray("update" );

                    for( int index = 0; index < toBeUpdated.size(); index++ )
                    {
                        final Object refModelObj = toBeUpdated.get( index );
                        final JsonObject refModelJson = ConverterUtil.typecastAndGet( JsonObject.class, refModelObj );
                        final String refModelPK = ConverterUtil.getPKColumnName( tableHolder.joinTable.entityClass() );
                        final Object refModelPKValue = Objects.requireNonNull( refModelJson.opt( refModelPK ) );
                        final BaseModel refModel = refModels.stream()
                                                            .filter( refM -> Objects.equals( refM.id(), refModelPKValue ) )
                                                            .findFirst()
                                                            .orElseThrow( () -> ErrorCode.get( ConfigErrorCodes.MODEL_NOT_FOUND) );
                        convert( refModel, refModelJson, converterParams.valueTransformer() );
                    }
                }

                if( dataChanges.containsKey("create" ) )
                {
                    final JsonArray targetJsonArray = dataChanges.getJsonArray("create" );

                    for( int index = 0; index < targetJsonArray.size(); index++ )
                    {
                        final Entity<?> refEntity = ConverterUtil.newInstance( tableHolder.joinTable.entityClass() );
                        final JsonObject refEntityJson = ConverterUtil.typecastAndGet( JsonObject.class, targetJsonArray.getJsonObject( index ) );

                        convert( refEntity, refEntityJson, converterParams.valueTransformer() );
                    }
                }
                refCollectionField.set( entity, refModels.isEmpty() ? null : refModels );
            }
        });
    }

    /**
     * <p>
     * Finds a present join
     * </p>
     *
     * @param joinTables      the join tables
     * @param converterParams the converter parameters
     * @return the present join
     */
    private JsonToModelConverter.TableHolder findPresentJoin( final @NonNull JoinTable[] joinTables,
                                                              final @NonNull ConverterParams<JsonObject> converterParams )
    {
        return  Stream.of( joinTables ).map( joinTable ->
                {
                    final String key = getKey( converterParams.field() );

                    return JsonToModelConverter.TableHolder
                                               .builder()
                                               .joinTable( joinTable )
                                               .tableKeyName( key )
                                               .build();
                })
                .filter( tableHolder -> converterParams.target()
                                                       .containsKey( tableHolder.tableKeyName ) )
                .findFirst()
                .orElseGet( () -> null );
    }

    /**
     * <p>
     * A holder for table information
     * </p>
     */
    @Builder
    private static class TableHolder
    {
        private final JoinTable joinTable;

        private final String tableKeyName;
    }

    /**
     * <p>
     * Converts a value
     * </p>
     *
     * @param value the value to convert
     * @return the converted value
     */
    private Object convertValue( final @NonNull Object value )
    {
        if( value instanceof Long )
        {
            return ( ( Number ) value).longValue();
        }
        else if( value instanceof Double )
        {
            return ( ( Number ) value).doubleValue();
        }
        else if( value instanceof Integer )
        {
            return ( (Number) value).intValue();
        }
        else if( value instanceof Boolean )
        {
            return value;
        }
        return value.toString();
    }
}