package com.tlc.crm.core.field.deal;

import com.tlc.crm.entity.models.FieldNameHolder;

public enum PipelineFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    STAGES("stages"),
    TOTAL_DEALS("totalDeals"),
    TOTAL_STAGES("totalStages"),
    TOTAL_VALUE("totalValue"),
    NAME("name"),
    IS_DEFAULT("isDefault"),
    ROTTEN_DAYS("rottenDays"),
    START("start"),
    LIMIT("limit"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    LIST("list"),
    ORDER("order"),
    PIPELINE("pipeline"),
    PIPELINE_ID("pipelineId"),
    PIPELINE_STAGE_ID("pipelineStageId"),
    IS_DRAGGABLE("isDraggable"),
    IS_EDITABLE("isEditable"),
    IDS("ids"),
    GENERAL("General"),
    IS_SELECTED("isSelected"),
    SYMBOL("symbol"),
    VALUE("value"),
    IS_ALL_PIPELINE_ENABLED("isAllPipelineEnabled"),
    ;

    private final String name;

    PipelineFields(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
