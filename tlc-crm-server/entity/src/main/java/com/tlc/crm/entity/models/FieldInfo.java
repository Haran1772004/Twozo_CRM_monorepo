package com.tlc.crm.entity.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.internal.FieldConfig;
import com.tlc.crm.entity.models.internal.FieldStorageType;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.FieldStoreService;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

/**
 * <p>
 * A data carrier that will represents the field details
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class FieldInfo implements Serializable {

    private static final String TABLE_COLUMN_INDEX_FORMAT = "%s%d";

    private final Long id;
    private final String idAsString;
    private final Long orgId;
    private final Integer columnIndex;
    private final Short tableIndex;
    private final Boolean isActive;
    private final I18nKey i18nName;
    private final String aliasName;
    private final StoreType storeType;
    private final FieldStorageType storageType;
    private final FieldConfig fieldConfig;
    private final Long storeId;
    private final Long sourceId;
    private final String presetConfig;
    private final String mapperTable;
    private final String dataTable;
    private transient PresetConfig presetConfigRecord;
    private final I18nKey placeHolder;
    private final String internalName;

    public FieldInfo(final Long id, final String idAsString, final Long orgId, final Integer columnIndex,
                    final Short tableIndex, final Boolean isActive, final I18nKey i18nName, final String aliasName, final StoreType storeType,
                     final FieldStorageType storageType, final FieldConfig fieldConfig, final JsonObject presetConfigAsJson,
                     final Long storeId, final Long sourceId, final String presetConfig, final String mapperTable,
                     final String dataTable, final I18nKey placeHolder, final String internalName) {
        this.id = id;
        this.idAsString = idAsString;
        this.orgId = orgId;
        this.columnIndex = columnIndex;
        this.tableIndex = tableIndex;
        this.isActive = isActive;
        this.i18nName = i18nName;
        this.aliasName = aliasName;
        this.storageType = storageType;
        this.storeType = storeType;
        this.fieldConfig = fieldConfig;
        this.presetConfigRecord = new PresetConfig(Objects.isNull(presetConfigAsJson) && Objects.nonNull(presetConfig)
                ? Json.object(presetConfig) : presetConfigAsJson);
        this.sourceId = sourceId;
        this.storeId = storeId;
        this.presetConfig = presetConfig;
        this.dataTable = dataTable;
        this.mapperTable = mapperTable;
        this.placeHolder = placeHolder;
        this.internalName = internalName;
    }

    public Long id() {
        return id;
    }

    public Long orgId() {
        return orgId;
    }

    public String idAsString() {
        return idAsString;
    }

    public Boolean isActive() {
        return isActive;
    }

    public String aliasName() {
        return isPersistent() ? idAsString : aliasName;
    }

    public I18nKey i18nName() {
        return i18nName;
    }

    public StoreType storeType() {
        return storeType;
    }

    public Integer columnIndex() {
        return columnIndex;
    }

    public FieldStorageType storageType() {
        return storageType;
    }

    public FieldConfig fieldConfig() {
        return fieldConfig;
    }

    public Long storeId() {
        return storeId;
    }

    public Long sourceId() {
        return sourceId;
    }

    public String getPresetConfig() {
        return this.presetConfig;
    }

    public FieldStore fieldStore() {
        return FieldStoreService.getInstance().getFieldStoreById(this.storeId);
    }

    public FieldSource fieldSource() {
        return FieldSourceService.getInstance().getSource(this.sourceId);
    }

    public Short tableIndex() {
        return this.tableIndex;
    }

    public boolean isPersistent() {
        return storageType.equals(FieldStorageType.PERSISTENT);
    }

    public Table table() {
        final FieldStore fieldStore = fieldStore();

        if (Objects.isNull(fieldStore)) {
            throw ErrorCode.get(EntityErrorCodes.INVALID_CRM_FIELD_STORE_TYPE);
        }
        final String valueTableName = fieldStore.table();
        final String tableName;

        if (isPersistent()) {

            if (Objects.nonNull(this.tableIndex) && this.tableIndex > 1) {
                tableName = String.format(TABLE_COLUMN_INDEX_FORMAT, valueTableName, this.tableIndex);
            } else {
                tableName = valueTableName;
            }
        } else {
            tableName = valueTableName;
        }
        return Table.get(tableName);
    }

    public String columnName() {
        final FieldStore fieldStore = fieldStore();

        if (Objects.isNull(fieldStore)) {
            throw ErrorCode.get(EntityErrorCodes.INVALID_CRM_FIELD_STORE_TYPE);
        }
        final String column = fieldStore.column();

        if (isPersistent() && isCustomFieldTable(fieldStore.table())) {

            if (Objects.isNull(this.columnIndex) || this.columnIndex < 1) {
                throw ErrorCode.get(EntityErrorCodes.INVALID_COLUMN_INDEX);
            }
            return String.format(TABLE_COLUMN_INDEX_FORMAT, column, this.columnIndex);
        }

        if (fieldStore.isModelStore() || StoreType.isMultiValueStore(fieldStore.storeType()) || !StoreType.isSingleValueStore(fieldStore.storeType())) {
            return column;
        } else if (this.columnIndex < 1) {
            throw ErrorCode.get(EntityErrorCodes.INVALID_COLUMN_INDEX);
        }
        return column + this.columnIndex;
    }

    private boolean isCustomFieldTable(final String table) {
        return table.endsWith("CustomFieldValues");
    }

    public Column column() {
        return table().getColumn(columnName());
    }

    public String fieldName() {
        return fieldName(Locale.getDefault());
    }

    public String fieldName(final Locale locale) {
        return I18nServiceProvider.get().getResolver(locale).get(i18nName);
    }

    public String getMapperTable() {
        return mapperTable;
    }

    public String getDataTable() {
        return dataTable;
    }

    public PresetConfig presetConfig() {
        return Objects.isNull(presetConfigRecord) ? presetConfigRecord = new PresetConfig(Json.object(presetConfig)) : presetConfigRecord;
    }

    public I18nKey getPlaceHolder() {
        return placeHolder;
    }

    public String getInternalName() {
        return internalName;
    }

    /**
     * {@inheritDoc}
     *
     * @return the hash value of this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, sourceId, orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param object Represents the object to be compared
     * @return true if given object is equals to the current object
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final FieldInfo fieldInfo = (FieldInfo) object;

        return Objects.equals(id, fieldInfo.id) && Objects.equals(sourceId, fieldInfo.sourceId) && Objects.equals(orgId, fieldInfo.orgId);
    }

    public boolean hasPresetConfig() {
        return Objects.nonNull(this.presetConfig);
    }
}