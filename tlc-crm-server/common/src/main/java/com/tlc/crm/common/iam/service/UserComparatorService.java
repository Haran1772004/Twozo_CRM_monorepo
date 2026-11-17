package com.tlc.crm.common.iam.service;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.internal.UserComparatorServiceImpl;

import java.util.Comparator;
import java.util.Locale;

/**
 * <p>
 * Represents the user comparator functionality by fields
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface UserComparatorService {

    /**
     * <p>
     * Gets the instance of {@link UserComparatorService}
     * </p>
     *
     * @return The instance of user comparator service
     */
    static UserComparatorService getInstance() {
        return UserComparatorServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the comparator for sorting CrmUser based on field and order type
     * </p>
     *
     * @param jsonObject Holds the sort {@link Json} details
     * @param locale     Represents a locale.
     * @return The {@link CrmUser} comparator
     */
    Comparator<CrmUser> getUserComparator(final JsonObject jsonObject, final Locale locale);
}
