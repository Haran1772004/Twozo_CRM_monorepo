package com.tlc.crm.core.features.internal.dataimport;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import java.util.Objects;
import java.util.Optional;

/**
 * Holds the field and header mapping information
 *
 * @author sundar
 * @version 1.0
 */
public class FieldHeader {

    private final int headerIndex;
    private final FieldInfo importField;
    private final Long subFieldId;
    private final boolean isAssociatedFieldHeader;

    public FieldHeader(final FieldInfo importField, final int headerIndex, final boolean isAssociatedFieldHeader) {
        this(importField, headerIndex, null, isAssociatedFieldHeader);
    }

    public FieldHeader(final FieldInfo importField, final int headerIndex) {
        this(importField, headerIndex, null);
    }

    public FieldHeader(final FieldInfo importField, final int headerIndex, final Long subFieldId) {
        this(importField, headerIndex, subFieldId, false);
    }

    private FieldHeader(final FieldInfo importField, final int headerIndex, final Long subFieldId,
                        final boolean isAssociatedFieldHeader) {
        this.importField = importField;
        this.headerIndex = headerIndex;
        this.subFieldId = subFieldId;
        this.isAssociatedFieldHeader = isAssociatedFieldHeader;
    }

    /**
     * Gets the source of the field
     *
     * @return the source of the field
     */
    public Source getSource() {
        return Source.get(importField.fieldSource().getName());
    }

    /**
     * Gets the header index associated with the field
     *
     * @return the header index associated with the field
     */
    public int getHeaderIndex() {
        return headerIndex;
    }

    /**
     * Gets the {@link FieldInfo} associated with the header
     *
     * @return the {@link FieldInfo} associated with the header
     */
    public FieldInfo getImportField() {
        return importField;
    }

    /**
     * Gets the subfield ID
     *
     * @return the ID of the subfield
     */
    public long getSubFieldId() {
        return Objects.requireNonNull(subFieldId);
    }

    /**
     * Gets the subfield ID optionally
     *
     * @return the {@link Optional} subfield ID
     */
    public Optional<Long> optSubFieldId() {
        return Optional.ofNullable(subFieldId);
    }

    /**
     * Checks whether the current field header is associated with the subfield or not
     *
     * @return true if the field header is associated with the subfield, false otherwise
     */
    public boolean isSubFieldHeader() {
        return Objects.nonNull(subFieldId);
    }

    /**
     * Checks whether the field header is associated with another source's field or not
     *
     * @return true if the field header is associated with another source, false otherwise
     */
    public boolean isAssociatedFieldHeader() {
        return isAssociatedFieldHeader;
    }
}