package com.tlc.crm.security.internal.permission.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.sql.common.api.Operation;

/**
 * <p>
 * Holds the constraint of the restriction.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public record RestrictionConstraint(Type type, Operation operation) {

    private static final String TYPE_KEY = "type";
    private static final String OPERATION_KEY = "operation";

    public RestrictionConstraint(final String type, final String operation) {
        this(Type.valueOf(type), Operation.valueOf(operation));
    }

    public RestrictionConstraint(final JsonObject constrainJson) {
        this(constrainJson.getString(TYPE_KEY), constrainJson.getString(OPERATION_KEY));
    }

    /**
     * <p>
     * Provides the type of the {@link RestrictionConstraint}.
     * </p>
     */
    public enum Type {

        /**
         * Represents the table/column to be considered for permission verification.
         */
        INCLUDE,

        /**
         * Represents the table/column to be ignored for permission verification.
         */
        IGNORE
    }
}
