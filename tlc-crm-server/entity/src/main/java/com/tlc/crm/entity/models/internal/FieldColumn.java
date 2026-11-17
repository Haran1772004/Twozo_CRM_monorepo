package com.tlc.crm.entity.models.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.service.FieldStoreService;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;

import com.tlc.validator.TlcModel;

import java.util.Objects;

/**
 * <p>
 * A model that represents the field column related information
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class FieldColumn implements TlcModel {

    private final Long id;
    private final Long orgId;
    private final Integer index;
    private final Short tableIndex;
    private final Long fieldStoreId;
    private final Long fieldSourceId;
    private final Long fieldId;

    public FieldColumn(long id, Long orgId, int index, short tableIndex, long fieldStoreId, long fieldSourceId, final long fieldId) {
        this.id = id;
        this.orgId = orgId;
        this.fieldStoreId = fieldStoreId;
        this.fieldSourceId = fieldSourceId;
        this.fieldId = fieldId;
        this.index = index;
        this.tableIndex = tableIndex;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public Integer getIndex() {
        return index;
    }

    public Short getTableIndex() {
        return tableIndex;
    }

    public Long getFieldSourceId() {
        return this.fieldSourceId;
    }

    public Long getFieldStoreId() {
        return this.fieldStoreId;
    }

    public String columnName() {
        final FieldStore fieldStore = FieldStoreService.getInstance().getFieldStoreById(this.fieldStoreId);

        if (Objects.isNull(fieldStore)) {
            throw ErrorCode.get(EntityErrorCodes.INVALID_CRM_FIELD_STORE_TYPE);
        }

        return fieldStore.column() + this.getIndex();
    }

    public Column column() {
        return table().getColumn(columnName());
    }

    public Table table() {
        final FieldStore fieldStore = FieldStoreService.getInstance().getFieldStoreById(this.fieldStoreId);

        if (Objects.isNull(fieldStore)) {
            throw ErrorCode.get(EntityErrorCodes.INVALID_CRM_FIELD_STORE_TYPE);
        }
        final String baseTableName = fieldStore.table();

        if (Objects.nonNull(this.tableIndex) && this.tableIndex > 1) {
            return Table.get(String.format("%s%d", baseTableName, this.tableIndex));
        }
        return Table.get(baseTableName);
    }

    public Long getFieldId() {
        return fieldId;
    }


    @Override
    public Object identity() {
        return null;
    }
}
