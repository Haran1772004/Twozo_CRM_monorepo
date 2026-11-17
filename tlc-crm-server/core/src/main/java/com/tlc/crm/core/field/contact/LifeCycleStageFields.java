package com.tlc.crm.core.field.contact;

import com.tlc.crm.entity.models.FieldNameHolder;

public enum LifeCycleStageFields implements FieldNameHolder {

    ID("id"),
    IDS("ids"),
    ORG_ID("orgId"),
    NAME("name"),
    STATUSES("statuses"),
    IS_ACTIVE("isActive"),
    IS_DEFAULT("isDefault"),
    VALUE("value"),
    IS_SYSTEM_STAGE("isSystemStage"),
    RULES("rules"),
    STAGE_ID("stageId"),
    TYPE("type"),
    STAGES("stages"),
    CUSTOMER("customer"),
    IS_DELETABLE("isDeletable"),
    IS_MOVABLE("isMovable"),
    DISABLED_STAGES("disabledStages"),
    IS_ASSOCIATED("isAssociated"),
    CONTACT_ID("contactId"),
    IS_DEACTIVATABLE("isDeactivatable"),
    ;

    private final String name;

    LifeCycleStageFields(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
