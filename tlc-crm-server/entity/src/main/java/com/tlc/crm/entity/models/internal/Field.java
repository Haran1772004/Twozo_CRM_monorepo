package com.tlc.crm.entity.models.internal;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.i18n.I18nKey;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * A model that represents the field related information
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class Field implements TlcModel {

    private Long id;
    private Long orgId;
    private Boolean isActive;
    private String name;
    private String internalName;
    private I18nKey i18nName;
    private String aliasName;
    private FieldType fieldType;
    private String storageType;
    private FieldConfig fieldConfig;
    private FieldColumn fieldColumn;
    private FieldStore store;
    private FieldSource source;
    private String mapperTable;
    private String dataTable;

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

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public FieldColumn getFieldColumn() {
        return fieldColumn;
    }

    public void setFieldColumn(FieldColumn fieldColumn) {
        this.fieldColumn = fieldColumn;
    }

    public FieldSource getSource() {
        return source;
    }

    public void setSource(FieldSource source) {
        this.source = source;
    }

    public FieldStore getStore() {
        return store;
    }

    public void setStore(FieldStore fieldStore) {
        this.store = fieldStore;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public I18nKey getI18nName() {
        return i18nName;
    }

    public void setI18nName(I18nKey i18nName) {
        this.i18nName = i18nName;
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(final String internalName) {
        this.internalName = internalName;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public FieldConfig getFieldConfig() {
        return fieldConfig;
    }

    public void setFieldConfig(FieldConfig fieldConfig) {
        this.fieldConfig = fieldConfig;
    }

    public boolean isPersistent() {
        return FieldStorageType.valueOf(storageType.toUpperCase()).equals(FieldStorageType.PERSISTENT);
    }

    @Override
    public Object identity() {
        return null;
    }

    public String getDataTable() {
        return dataTable;
    }

    public void setDataTable(final String dataTable) {
        this.dataTable = dataTable;
    }

    public String getMapperTable() {
        return mapperTable;
    }

    public void setMapperTable(final String mapperTable) {
        this.mapperTable = mapperTable;
    }
}
