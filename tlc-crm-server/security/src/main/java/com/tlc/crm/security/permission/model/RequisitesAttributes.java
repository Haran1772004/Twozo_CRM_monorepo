package com.tlc.crm.security.permission.model;


/**
 * <p>
 * Holds the entity of a {@link Requisites} and provides access to those field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
enum RequisitesAttributes {

    TYPE("type"),
    VALUE("value"),
    ORDER("order"),
    FIELD("field"),
    SUB_TYPE("subType"),
    RELATIVE_APIS("relativeApis"),
    DEPENDS_ON("dependsOn"),
    HAS_SUB_TYPE("hasSubType"),
    REGISTERED_PATH("registeredPath"),
    IGNORABLE_APIS("ignorableApis"),
    ;

    private final String name;

    RequisitesAttributes(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
