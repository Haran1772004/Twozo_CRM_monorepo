package com.tlc.crm.core.segment.fields;

import com.tlc.crm.entity.models.FieldNameHolder;
import com.tlc.crm.core.segment.models.SegmentFolder;

/**
 * <p>
 * Holds the field names of {@link SegmentFolder}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum SegmentFolderFields implements FieldNameHolder {

    ID("id"),
    NAME("name"),
    IS_STARRED("isStarred"),
    SEGMENT_COUNT("segmentCount"),
    TYPE("type"),
    SEGMENT_FOLDERS("segmentFolders"),
    AUDIT("audit"),
    CREATED("created"),
    MODIFIED("modified"),
    BY("by"),
    TIME("time"),
    ;

    private final String name;

    SegmentFolderFields(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}