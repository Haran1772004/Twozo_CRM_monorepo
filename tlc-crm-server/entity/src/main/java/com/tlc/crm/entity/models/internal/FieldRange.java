package com.tlc.crm.entity.models.internal;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.FieldStore;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * A model that represents the available field range's of a store
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class FieldRange implements TlcModel {

    private Long id;
    private Long orgId;
    private FieldSource fieldSource;
    private FieldStore fieldStore;
    private String availableRange;
    private String usedIndices;

    @Override
    public Long id() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public FieldSource getFieldSource() {
        return fieldSource;
    }

    public void setFieldSource(FieldSource fieldSource) {
        this.fieldSource = fieldSource;
    }

    public FieldStore getFieldStore() {
        return fieldStore;
    }

    public void setFieldStore(FieldStore fieldStore) {
        this.fieldStore = fieldStore;
    }

    public String getAvailableRange() {
        return availableRange;
    }

    public void setAvailableRange(String availableRange) {
        this.availableRange = availableRange;
    }

    public String getUsedIndices() {
        return usedIndices;
    }

    public void setUsedIndices(String usedIndices) {
        this.usedIndices = usedIndices;
    }

    @Override
    public Object identity() {
        return null;
    }

}
