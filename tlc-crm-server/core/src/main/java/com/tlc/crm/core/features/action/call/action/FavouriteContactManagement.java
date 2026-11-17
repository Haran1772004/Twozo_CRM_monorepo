package com.tlc.crm.core.features.action.call.action;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.features.action.call.manager.FavouriteContactManager;
import com.tlc.crm.core.features.action.call.model.FavouriteContact;
import com.tlc.crm.core.features.action.call.model.converter.FavouriteContactConverter;
import com.tlc.crm.entity.action.AsyncBulkConfigAction;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.web.WebAction;

import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Web action controller for managing {@link FavouriteContact}
 *
 * @author kishore J
 * @version 1.0
 */
@WebAction(path = "/call/favourite/contact")
public class FavouriteContactManagement extends AsyncBulkConfigAction<FavouriteContact> {

    private static final String CONFIG_ACTION_NAME = "favouriteContact";
    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 100;
    private static final String CONTACT_IDS = "contactIds";

    private final FavouriteContactManager favouriteContactManager;
    private final FavouriteContactConverter favouriteContactConverter;

    public FavouriteContactManagement() {
        favouriteContactManager = FavouriteContactManager.getInstance();
        favouriteContactConverter = FavouriteContactConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the incoming {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final Long orgId = request.orgId();
        final Long userId = request.userId();

        switch (type) {
            case ActionFields.LIST -> list(request, response, data, orgId, userId);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link FavouriteContactManager} instance handling business logic.
     */
    @Override
    public ConfigManager<FavouriteContact> getConfigManager() {
        return favouriteContactManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The {@link CrmRequest} providing context like orgId and userId.
     * @param jsonObject The payload containing the full favourite contact data.
     * @return The {@link FavouriteContact} converted from the provided JSON.
     */
    @Override
    public FavouriteContact convert(final CrmRequest request, final JsonObject jsonObject) {
        return favouriteContactConverter.convertToModel(new CrmConverterData<>(request.getUser(), request.locale(), jsonObject));
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The {@link CrmRequest} providing context like orgId and userId.
     * @param jsonObject The payload containing partial fields of favourite contact data.
     * @return The {@link FavouriteContact} converted from partial JSON data.
     */
    @Override
    public FavouriteContact convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        return favouriteContactConverter.convertToModel(new CrmConverterData<>(request.getUser(), request.locale(), true, true, jsonObject));
    }

    /**
     * {@inheritDoc}
     *
     * @return The configuration action name used to identity and segregate bulk operations.
     */
    @Override
    protected String getConfigActionName() {
        return CONFIG_ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest}
     * @param crmResponse Holds the {@link CrmResponse}.
     * @param eventData   {@link EventData} defining the current event context.
     */
    @Override
    protected void bCreate(final CrmRequest crmRequest, final CrmResponse crmResponse, final EventData eventData) {
        final long orgId = crmRequest.orgId();
        final long userId = crmRequest.userId();
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(ActionFields.DATA);

        if (data.containsKey(CONTACT_IDS)) {
            final Collection<Long> contactIds = new HashSet<>();
            final JsonArray idsArray = data.getJsonArray(CONTACT_IDS);

            idsArray.forEach(id -> contactIds.add((Long) id));

            publish(orgId, userId, requestJson, contactIds, Action.CREATE);
        }
        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s:%s:%d", orgId, userId,
                getConfigActionName(), Instant.now().toEpochMilli()).getBytes());

        crmResponse.put(ActionFields.ID, requestId);
        crmResponse.put(ActionFields.MESSAGE, crmRequest.getI18NCRMResolutionAdapter().get("i18n.favourite.contact.bulk.create"));
    }

    /**
     * Fetches and dispatches a listing of favourite contacts for the given user.
     *
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse} to be populated.
     * @param data     Represents the request payload containing pagination parameters.
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id whose favourites are to be listed.
     */
    private void list(final CrmRequest request, final CrmResponse response, final JsonObject data,
                      final Long orgId, final Long userId) {
        final long totalRecords = favouriteContactManager.getTotalRecords(orgId, userId);

        response.put(FieldKeys.TOTAL_RECORDS.getName(), totalRecords);

        if (totalRecords == 0L) {
            response.put(FieldKeys.LIST.getName(), Collections.emptyList());
            response.put(FieldKeys.HAS_MORE.getName(), false);
            return;
        }
        final int start = Math.max(data.optInt(FieldKeys.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(FieldKeys.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Collection<FavouriteContact> favouriteContacts = favouriteContactManager.get(orgId, userId, new LimitConfig(start, limit));
        final JsonArray favouriteContactsJsonArray = Json.array();

        for (final FavouriteContact favouriteContact : favouriteContacts) {
            favouriteContactsJsonArray.put(favouriteContactConverter.convertToJson(new CrmConverterData<>(request.getUser(), request.locale(), favouriteContact)));
        }
        response.put(FieldKeys.LIST.getName(), favouriteContactsJsonArray);
        response.put(FieldKeys.HAS_MORE.getName(), (start + limit) <= totalRecords);
    }
}
