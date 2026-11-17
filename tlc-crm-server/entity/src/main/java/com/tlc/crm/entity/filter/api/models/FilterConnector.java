package com.tlc.crm.entity.filter.api.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.FilterErrorCodes;
import com.tlc.sql.common.api.dml.Connector;

/**
 * <p>
 * A model that represents the connector data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum FilterConnector {

    AND(1, Connector.AND),
    OR(2, Connector.OR),
    ;

    private final int id;
    private final Connector connector;

    FilterConnector(final int id, final Connector connector) {
        this.id = id;
        this.connector = connector;
    }

    public int getId() {
        return id;
    }

    public Connector getConnector() {
        return connector;
    }

    public static FilterConnector get(final int id) {
        return switch (id) {
            case 1 -> AND;
            case 2 -> OR;
            default -> throw ErrorCode.get(FilterErrorCodes.CONNECTOR_NOT_FOUND);
        };
    }
}
