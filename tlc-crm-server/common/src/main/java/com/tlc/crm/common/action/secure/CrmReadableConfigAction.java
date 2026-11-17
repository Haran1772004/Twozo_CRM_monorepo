package com.tlc.crm.common.action.secure;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.fields.manager.ReadableConfigManager;

import java.util.Collection;

/**
 * <p>
 * Provides an abstract way to configure the secured readable action
 * </p>
 *
 * @param <T>
 * @author Selvakumar
 * @version 1.0
 */
public abstract class CrmReadableConfigAction<T> extends AuthorizationController {

    /**
     * <p>
     * Process the incoming secured read request and performs the appropriate read actions
     * </p>
     *
     * @param request
     * @param crmResponse
     * @throws Exception
     */
    @Override
    protected void authorizedProcess(CrmRequest request, CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");

        switch (type) {
            case "get" -> {
                crmResponse.put("data", convertModelToJsonObject(request, get(data.getLong("id"))));
            }
            case "list" -> {
                final Collection<T> models = getAllData();

                crmResponse.put("data", convertModelsToJsonArray(request, models));
            }
            case "exists" -> {
                crmResponse.put("found", exists(data.getLong("id")));
            }
            default -> {
                handleReadRequest(request, crmResponse);
            }
        }
    }

    /**
     * <p>
     * Checks whether an entry exists for the given id or not
     * </p>
     *
     * @param id
     * @return
     */
    public boolean exists(Long id) {
        return getConfigManager().exists(id);
    }

    /**
     * <p>
     * Fetches an entity by using the given id
     * </p>
     *
     * @param id
     * @return
     */
    public T get(Long id) {
        return getConfigManager().get(id);
    }

    /**
     * <p>
     * Fetches the available entities by using the given ids
     * </p>
     *
     * @param ids
     * @return
     */
    public Collection<T> get(Collection<Long> ids) {
        return getConfigManager().get(ids);
    }

    /**
     * <p>
     * Fetches all the data available for a form
     * </p>
     *
     * @return
     */
    public abstract Collection<T> getAllData();

    /**
     * <p>
     * Converts models into json array
     * </p>
     *
     * @param models
     * @return
     */
    public final JsonArray convertModelsToJsonArray(final CrmRequest request, final Collection<T> models) {
        final JsonArray jsonArray = Json.array();

        models.stream().forEach(model -> jsonArray.put(convertModelToJsonObject(request, model)));

        return jsonArray;
    }

    /**
     * <p>
     * Gets {@link ReadableConfigManager<T>} of same type
     * </p>
     *
     * @return
     */
    public abstract ReadableConfigManager<T> getConfigManager();

    /**
     * <p>
     * Converts model to json object
     * </p>
     *
     * @param request Refers the {@link CrmRequest}
     * @param model   Refers the model.
     * @return {@link JsonObject} of model.
     */
    protected JsonObject convertModelToJsonObject(final CrmRequest request, final T model) {
        return convertModelToJsonObject(model);
    }

    /**
     * <p>
     * Converts model to json object.
     * </p>
     *
     * @param model Refers the model.
     * @return {@link JsonObject}
     */
    protected JsonObject convertModelToJsonObject(final T model) {
        return Json.object();
    }

    /**
     * <p>
     * Handles the incoming read request
     * </p>
     *
     * @param request
     * @param crmResponse
     */
    public abstract void handleReadRequest(CrmRequest request, CrmResponse crmResponse);

}
