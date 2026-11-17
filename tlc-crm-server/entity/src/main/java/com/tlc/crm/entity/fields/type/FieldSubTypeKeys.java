package com.tlc.crm.entity.fields.type;

import com.tlc.crm.entity.models.FieldNameHolder;
import com.tlc.crm.fields.models.FieldSubType;

/**
 * Contains the {@link FieldSubType} related keys
 *
 * @author sundar
 * @version 1.0
 */
public enum FieldSubTypeKeys implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    NAME("name"),
    CODE("code"),
    REASON("reason"),
    UPDATED_RECORDS("updatedRecords"),
    FAILED_RECORDS("failedRecords"),
    COUNTRY_CODE("countryCode"),
    CURRENCY_ID("currencyId"),
    ADDRESS_LINE1("addressLine1"),
    ADDRESS_LINE2("addressLine2"),
    DISPLAY_NAME("displayName"),
    VALUE("value"),
    NUMBER("number"),
    SYMBOL("symbol"),
    IS_BASE("isBase"),
    IS_ACTIVE("isActive"),
    ;

    private final String name;

    FieldSubTypeKeys(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
