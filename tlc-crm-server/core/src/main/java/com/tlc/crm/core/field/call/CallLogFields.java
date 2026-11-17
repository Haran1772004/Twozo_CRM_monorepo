package com.tlc.crm.core.field.call;

import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * Represents the call log related fields
 *
 * @author Sathish
 * @version 1.0
 */
public enum CallLogFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    START_TIME("startTime"),
    SEARCH_TEXT("searchText"),
    CRITERIA("criteria"),
    START("start"),
    LIMIT("limit"),
    AGENDA("agenda"),
    CALL_TYPE("callType"),
    PARTICIPANT_ID("participantId"),
    OUTCOME_ID("outcomeId"),
    RELATED_DEAL_ID("relatedDealId"),
    LOGGED_TIME("loggedTime"),
    CREATED_BY("createdBy"),
    OUTCOME("outcome"),
    PARTICIPANT("participant"),
    RELATED_DEAL("relatedDeal"),
    NAME("name"),
    STAGE_NAME("stateName"),
    EXPECTED_CLOSE_DATE("expectedCloseDate"),
    VALUE("value"),
    IS_DEFAULT("isDefault"),
    CONTACT_ID("contactId"),
    COMPANY("company"),
    TITLE("title"),
    PIPELINE_STAGE("pipelineStage"),
    DEAL_VALUE("dealValue"),
    IS_ARCHIVED("isArchived"),
    NUMBER("number"),
    CONTACT_NUMBER("contactNumber"),
    IS_CALL_LOGGED("isCallLogged"),
    ICON("icon"),
    CALL("call"),
    ;

    private final String name;

    CallLogFields(final String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }
}
