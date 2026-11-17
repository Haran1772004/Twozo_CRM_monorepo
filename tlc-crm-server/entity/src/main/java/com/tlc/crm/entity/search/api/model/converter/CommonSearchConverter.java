package com.tlc.crm.entity.search.api.model.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.search.api.model.ViewedEntries;
import com.tlc.crm.entity.search.fields.CommonSearchFields;

/**
 * <p>
 * Provides different ways to convert the data from database
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class CommonSearchConverter {

    private static class Instance {
        private static final CommonSearchConverter INSTANCE = new CommonSearchConverter();
    }

    public static CommonSearchConverter getInstance() {
        return Instance.INSTANCE;
    }

    private CommonSearchConverter() {
    }

    /**
     * <p>
     * Converts the given data into model
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @param data   Represents the {@link JsonObject}
     * @return the {@link ViewedEntries}
     */
    public ViewedEntries convertDataIntoModel(final long orgId, final long userId, final JsonObject data) {
        final ViewedEntries viewedEntries = new ViewedEntries();

        viewedEntries.setOrgId(orgId);
        viewedEntries.setUserId(userId);
        viewedEntries.setIndex(data.getString(CommonSearchFields.INDEX));
        viewedEntries.setId(data.getLong(CommonSearchFields.ENTRY_ID));
        viewedEntries.setSearchText(data.getString(CommonSearchFields.SEARCH_TEXT));

        return viewedEntries;
    }
}