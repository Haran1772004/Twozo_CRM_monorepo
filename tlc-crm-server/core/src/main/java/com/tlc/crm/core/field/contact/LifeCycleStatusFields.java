package com.tlc.crm.core.field.contact;

import com.tlc.crm.entity.models.FieldNameHolder;

public enum LifeCycleStatusFields implements FieldNameHolder {

    ID("id"),
    IDS("ids"),
    ORG_ID("orgId"),
    NAME("name"),
    LABEL("label"),
    IS_DEFAULT("isDefault"),
    IS_SYSTEM_STATUS("isSystemStatus"),
    VALUE("value"),
    LIFE_CYCLE_STAGE_ID("lifeCycleStageId"),
    DEPENDENT_ID("dependentId"),
    CONTACT_ID("contactId"),
    IS_ASSOCIATED("isAssociated"),
    ;

    private final String name;

    LifeCycleStatusFields(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
