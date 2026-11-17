package com.tlc.crm.core.api.marketinglist.model.converter;

import com.tlc.commons.converter.model.ConverterData;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.converter.CrmModelConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.core.field.marketinglist.MarketingListFields;

import com.tlc.crm.entity.marketinglist.model.MarketingList;

import java.util.Collection;

/**
 * <p>
 * Converter class for transforming between {@link MarketingList} domain models and their JSON representations
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class MarketingListConverter extends CrmModelConverter<MarketingList, JsonObject> {

    private final UserService userService;

    private MarketingListConverter() {
        userService = UserService.getInstance();
    }

    private static class InstanceHolder {
        private static final MarketingListConverter INSTANCE = new MarketingListConverter();
    }

    /**
     * <p>
     * Gets the {@link MarketingListConverter} instance
     * </p>
     *
     * @return The {@link MarketingListConverter}
     */
    public static MarketingListConverter getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData Represents the {@link ConverterData}
     * @return The {@link MarketingList}
     */
    @Override
    public MarketingList convertToModel(final CrmConverterData<JsonObject> converterData) {
        final Long orgId = converterData.orgId();
        final CrmUser crmUser = userService.getUser(orgId, converterData.ownerId());
        final MarketingList marketingList = new MarketingList();
        final JsonObject data = converterData.dataToBeConverted();

        marketingList.setCreatedBy(crmUser);
        marketingList.setOrgId(orgId);
        marketingList.setUpdatedBy(crmUser);
        marketingList.setDefault(data.optBoolean(MarketingListFields.IS_DEFAULT));

        if (data.containsKey(MarketingListFields.ID)) {
            marketingList.setId(data.getLong(MarketingListFields.ID));
        }

        if (data.containsKey(MarketingListFields.NAME)) {
            marketingList.setName(data.getString(MarketingListFields.NAME));
        }

        return marketingList;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData Represents the {@link ConverterData}
     * @return The {@link JsonObject}
     */
    @Override
    public JsonObject convertToJson(final CrmConverterData<MarketingList> converterData) {
        final JsonObject jsonObject = Json.object();
        final MarketingList marketingList = converterData.dataToBeConverted();

        jsonObject.put(MarketingListFields.VALUE, marketingList.id());
        jsonObject.put(MarketingListFields.NAME, marketingList.getName());

        return jsonObject;
    }

    /**
     * <p>
     * Converts a collection of {@link MarketingList} into a {@link JsonArray} for summary
     * </p>
     *
     * @param marketingLists Represents the collection of {@link MarketingList}
     * @return the json array
     */
    public JsonArray convertForSummary(final Collection<MarketingList> marketingLists) {
        final JsonArray marketingListArray = Json.array();

        for (final MarketingList marketingList : marketingLists) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(MarketingListFields.ID, marketingList.id());
            jsonObject.put(MarketingListFields.NAME, marketingList.getName());

            marketingListArray.put(jsonObject);
        }

        return marketingListArray;
    }
}