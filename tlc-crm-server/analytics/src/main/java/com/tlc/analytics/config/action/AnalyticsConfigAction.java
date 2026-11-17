package com.tlc.analytics.config.action;

import com.tlc.analytics.config.action.general.converter.ReportsClientConverter;
import com.tlc.analytics.config.model.BaseModel;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.models.Action;

import com.tlc.commons.util.executor.CodeBlockExecutor;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.functions.TriFunction;

import com.tlc.entity.converter.ConverterUtil;

import lombok.NonNull;

import java.util.Collection;
import java.util.function.BiFunction;

/**
 * Abstract class defining common behavior for configuration-related actions associated with a specific type of model.
 *
 * @param <M> The type of the model.
 * @author Aswath
 * @version 1.0
 */
public abstract class AnalyticsConfigAction<M extends BaseModel> extends CrmConfigAction<M>
{

    private static final TriFunction<String, String, Object, Object> DEFAULT_TRANSFORMER = ( tableName, columnName, actualValue ) -> actualValue;
    private static final BiFunction<String, String, Boolean> DEFAULT_PREDICATE = ( columnName, tableName ) -> !columnName.isEmpty();

    /**
     * Handles the Analytics configuration action.
     *
     * @param request  The CRM request.
     * @param response The CRM response.
     */
    @Override
    public final void handle( final @NonNull CrmRequest request,
                              final @NonNull CrmResponse response )
    {}

    /**
     * Converts the JSON object to a model instance.
     *
     * @param request    The CRM request.
     * @param jsonObject The JSON object to convert.
     * @return The converted model instance.
     */
    @Override
    public final M convert( final @NonNull CrmRequest request,
                            final @NonNull JsonObject jsonObject )
    {
        return CodeBlockExecutor.getWithUnchecked( () ->
        {
            final M model = ConverterUtil.newInstance( getModelClass() );

            model.setOrgId( request.orgId() );
            convertJsonToModel( model, jsonObject );

            return model;
        } );
    }

    /**
     * Converts partial data from a JSON object to a model instance.
     *
     * @param request    The CRM request.
     * @param jsonObject The JSON object containing partial data.
     * @return The partially converted model instance.
     */
    @Override
    public final M convertPartialData( final @NonNull CrmRequest request,
                                       final @NonNull JsonObject jsonObject )
    {
        return CodeBlockExecutor.getWithUnchecked( () ->
        {
            //TODO have to replace -1 to id of the model
            final M model = getConfigManager().get( request.orgId(), -1L );
            convertJsonToModel( model, jsonObject );
            return model;
        } );
    }

    /**
     * Converts JSON data to the model instance.
     *
     * @param model      The model instance.
     * @param jsonObject The JSON data.
     * @throws Exception Thrown if conversion fails.
     */
    private void convertJsonToModel( final @NonNull M model,
                                     final @NonNull JsonObject jsonObject ) throws Exception
    {
        ReportsClientConverter.getJsonToModelConverter()
                              .convert( model, jsonObject, modelValueTransformer() );
    }

    /**
     * Converts the model instance to a JSON object.
     *
     * @param model The model instance.
     * @return The JSON object representation of the model.
     */
    @Override
    protected JsonObject convert( final @NonNull M model )
    {
        return CodeBlockExecutor.getWithUnchecked( () ->
        {
            final JsonObject resp = Json.object();

            ReportsClientConverter.getModelToJsonConverter( predicate() )
                                  .convert( model, resp, jsonValueTransformer() );
            return resp;
        } );
    }

    /**
     * Checks if the details provided are valid.
     *
     * @param orgId  The organization ID.
     * @param userId The user ID.
     * @param action The action being performed.
     * @param models The collection of models.
     * @return True if details are valid, otherwise false.
     */
    @Override
    protected boolean isValidDetails( final @NonNull Long orgId,
                                      final @NonNull Long userId,
                                      final @NonNull Action action,
                                      final @NonNull Collection<M> models )
    {
        return true;
    }

    /**
     * Provides the default transformer for JSON values.
     *
     * @return The default JSON value transformer.
     */
    protected @NonNull TriFunction<String, String, Object, Object> jsonValueTransformer()
    {
        return DEFAULT_TRANSFORMER;
    }

    /**
     * Provides the default transformer for model values.
     *
     * @return The default model value transformer.
     */
    protected @NonNull TriFunction<String, String, Object, Object> modelValueTransformer()
    {
        return DEFAULT_TRANSFORMER;
    }

    protected @NonNull BiFunction<String, String, Boolean> predicate()
    {
        return DEFAULT_PREDICATE;
    }


    /**
     * Retrieves the class of the model associated with this action.
     *
     * @return The class of the model.
     */
    abstract protected @NonNull Class<M> getModelClass();
}
