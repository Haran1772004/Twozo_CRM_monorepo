package com.tlc.crm.entity.filter.fields;

import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Holds the entity of a {@link FilterConnector} and provides access to those field names.Provides the connector fields of the connector.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see FieldNameHolder
 */
public enum ConnectorFields implements FieldNameHolder {

    ID("id"),
    NAME("name"),
    ALIAS_NAME("aliasName"),
    ;
    private final String name;

    ConnectorFields(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
