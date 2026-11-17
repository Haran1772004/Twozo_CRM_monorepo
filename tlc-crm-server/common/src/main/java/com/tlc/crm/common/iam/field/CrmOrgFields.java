package com.tlc.crm.common.iam.field;

/**
 * Holds the organization related fields
 *
 * @author sundar
 * @version 1.0
 */
public enum CrmOrgFields {

    IS_INITIALIZED("isInitialized"),
    MESSAGE("message"),
    IS_SIGNUP_COMPLETED("isSignupCompleted"),
    TYPE("type"),
    DATA("data"),
    ORG_INITIALIZATION("ORG_INITIALIZATION"),
    ;

    private final String name;

    CrmOrgFields(final String name) {
        this.name = name;
    }

    /**
     * Gets the field name
     *
     * @return the field name
     */
    public String getName() {
        return name;
    }
}
