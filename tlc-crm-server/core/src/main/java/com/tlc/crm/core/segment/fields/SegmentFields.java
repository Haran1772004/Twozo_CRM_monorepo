package com.tlc.crm.core.segment.fields;

import com.tlc.crm.entity.models.FieldNameHolder;
import com.tlc.crm.core.segment.models.Segment;

/**
 * <p>
 * Holds the field names of {@link Segment}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum SegmentFields implements FieldNameHolder {

    ID("id"),
    NAME("name"),
    SEGMENT("segment"),
    STATUS_ID("statusId"),
    COMPUTATION_STATE("computationState"),
    FOLDER("folder"),
    GROUPS("groups"),
    MODIFIED_TIME("modifiedTime"),
    LIST("list"),
    START("start"),
    LIMIT("limit"),
    SEARCH_TEXT("searchText"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    SORT_CONFIG("sortConfig"),
    TYPE("type"),
    ORG_ID("orgId"),
    USER_ID("userId"),
    DATA("data"),
    GROUP_ID("groupId"),
    GROUP("group"),
    DEFAULT_FILTER("defaultFilter"),
    CRITERIA("criteria"),
    FILTER("filter"),
    AUDIT("audit"),
    CREATED("created"),
    MODIFIED("modified"),
    BY("by"),
    TIME("time"),
    MESSAGE("message"),
    DISPLAY_NAME("displayName"),
    SOURCE("source"),
    SUBGROUP("subGroup"),
    SUBGROUPS("subGroups"),
    CRITERIA_CONDITION("criteriaCondition"),
    CONNECTOR_ID("connectorId"),
    CONDITIONS("conditions"),
    ACTION_TYPE_ID("actionTypeId"),
    FOLDER_ID("folderId"),
    IDS("ids"),
    CONTACT_COUNT("contactCount"),
    INCLUDE_CONTACT_COUNT("includeContactCount"),
    SEGMENT_ID("segmentId"),
    ;

    private final String name;

    SegmentFields(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}