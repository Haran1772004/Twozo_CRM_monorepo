package com.tlc.crm.entity.fieldaudit.models;

import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Source of a field-level change captured by the Field Audit feature.
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public enum FieldAuditSource {

    MANUAL((short) 1, "i18n.field.audit.edit.source.manually", null),
    CONTACT((short) 2, "i18n.field.audit.edit.source.contact.placeholder", "i18n.field.audit.edit.source.email"),
    WORKFLOW((short) 3, "i18n.field.audit.edit.source.workflow.automation", null),
    AUTO_ASSIGNMENT((short) 4, "i18n.field.audit.edit.source.auto.assignment.rules", null);

    private final short id;
    private final String displayName;
    private final String subvalue;

    FieldAuditSource(final short id, final String displayName, final String subvalue) {
        this.id = id;
        this.displayName = displayName;
        this.subvalue = subvalue;
    }

    public short getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSubvalue() {
        return subvalue;
    }

    public static FieldAuditSource get(final int id) {
        for (final FieldAuditSource fieldAuditSource : values()) {

            if (fieldAuditSource.id == id) {
                return fieldAuditSource;
            }
        }
        return null;
    }

    /**
     * <p>
     * Gets the field audit sources for the given source ID
     * </p>
     *
     * @param sourceId Represents the source ID
     * @return The collection of field audit source
     */
    public static Collection<FieldAuditSource> getFieldAuditSources(final short sourceId) {
        if (sourceId == Source.CONTACT.getId()) {
            return List.of(values());
        } else if (sourceId == Source.COMPANY.getId() || sourceId == Source.DEAL.getId()) {
            return List.of(MANUAL, WORKFLOW, AUTO_ASSIGNMENT);
        }
        return Collections.emptyList();
    }

    /**
     * <p>
     * Checks whether the source have dynamic sub value
     * </p>
     *
     * @return True if yes, false otherwise
     */
    public boolean hasDynamicSubvalue() {
        return this == WORKFLOW || this == AUTO_ASSIGNMENT;
    }
}