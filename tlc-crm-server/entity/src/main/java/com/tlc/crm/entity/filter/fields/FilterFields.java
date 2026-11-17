package com.tlc.crm.entity.filter.fields;

import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Holds the entity of a {@link Filter} and provides access to those field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see FieldNameHolder
 */
public enum FilterFields implements FieldNameHolder {

    ID("id"),
    NAME("name"),
    CRITERIA_LIST("criteriaList"),
    FILTER_VISIBILITY("filterVisibility"),
    OWNER("owner"),
    CREATED_TIME("createdTime"),
    MODIFIED_TIME("modifiedTime"),
    VISIBILITY_ID("visibilityId"),
    IS_FAVORITE("isFavorite"),
    FILTER_ID("filterId"),
    FILTERS("filters"),
    FILTER("filter"),
    SOURCE_ID("sourceId"),
    CREATOR_ID("creatorId"),
    IS_DEFAULT("isDefault"),
    IS_EDITABLE("isEditable"),
    IS_DELETABLE("isDeletable"),
    IS_SHAREABLE("isShareable"),
    IS_ACTIVE("isActive"),
    TITLE("title"),
    OPTIONS("options"),
    SIZE("size"),
    SALES_OWNER("salesOwner"),
    USER_NAME("userName"),
    IS_SHARED_FILTER("isSharedFilter"),
    STATE("state"),
    ;

    private final String name;

    FilterFields(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
