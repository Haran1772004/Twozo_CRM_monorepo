package com.tlc.crm.entity.filter.fields;

import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Holds the entity of a {@link FilterCriteria} and provides access to those field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see FieldNameHolder
 */
public enum CriteriaFields implements FieldNameHolder {

    ID("id"),
    VALUES("values"),
    COMPARATOR("comparator"),
    FIELD("field"),
    CONNECTOR("connector"),
    FIELD_ID("fieldId"),
    COMPARATOR_ID("comparatorId"),
    CONNECTOR_ID("connectorId"),
    ADDITIONAL_OPTION_ID("additionalOptionId"),
    SOURCE_ID("sourceId"),
    SOURCE("source"),
    FILTER_ID ("filterId"),
    CRITERIA("criteria"),
    OWNER_ID("ownerId"),
    SALES_OWNER("salesOwner"),
    ADDITIONAL_CRITERIA("additionalCriteria"),
    VALUE_ENTITY_ID("valueEntityId"),
    VALUE("value"),
    DEAL_CRITERIA("dealCriteria"),
    COMPANY_CRITERIA("companyCriteria"),
    CONTACT_CRITERIA("contactCriteria");
    ;
    private final String name;

    CriteriaFields(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
