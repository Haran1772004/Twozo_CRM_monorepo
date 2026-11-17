package com.tlc.crm.core.field.deal;

import com.tlc.crm.entity.models.FieldNameHolder;

public enum PipelineStageFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    NAME("name"),
    ORDER("order"),
    PROBABILITY("probability"),
    PIPELINE_ID("pipelineId"),
    SUMMARY("summary"),
    DEALS("deals"),
    TOTAL_DEALS("totalDeals"),
    TOTAL_VALUE("totalValue"),
    PROBABLE_VALUE("probableValue"),
    IS_DEFAULT("isDefault"),
    HAS_MORE("hasMore"),
    IDS("ids"),
    NEW("New"),
    WON("Won"),
    LOST("Lost"),
    SYMBOL("symbol"),
    TOTAL_RECORDS("totalRecords"),
    VALUE("value"),
    SORT("sort"),
    CODE("code"),
    CURRENCY("currency"),
    ;

    private final String name;

    PipelineStageFields(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
