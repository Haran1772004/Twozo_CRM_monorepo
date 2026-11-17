package com.tlc.crm.entity.builder;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.internal.Field;
import com.tlc.crm.entity.models.internal.FieldColumn;
import com.tlc.crm.entity.models.internal.FieldConfig;
import com.tlc.crm.entity.models.internal.FieldStorageType;
import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.i18n.I18nKey;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds {@link FieldInfo} from {@link Field}
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class FieldInfoBuilder {

    private final FieldTypeService fieldTypeService;

    private static class Instance {
        private static final FieldInfoBuilder INSTANCE = new FieldInfoBuilder();
    }

    private FieldInfoBuilder() {
        fieldTypeService = FieldTypeService.getInstance();
    }

    public static FieldInfoBuilder getInstance() {
        return FieldInfoBuilder.Instance.INSTANCE;
    }

    public Collection<FieldInfo> build(final Field... fields) {
        return build(List.of(fields));
    }

    public Collection<FieldInfo> build(final Collection<Field> fields) {
        return fields.stream().map(this::build).toList();
    }

    public FieldInfo build(final Field field) {
        final FieldColumn fieldColumn = field.getFieldColumn();
        final FieldConfig fieldConfig = field.getFieldConfig();

        final String idAsString = field.id().toString();
        final String presetConfig = fieldConfig.getPresetConfig();
        final String storageType = field.getStorageType();
        final Integer columnIndex = Objects.isNull(fieldColumn) ? null : fieldColumn.getIndex();
        final Short tableIndex = Objects.isNull(fieldColumn) ? null : fieldColumn.getTableIndex();
        final JsonObject presetConfigAsJson = Objects.isNull(presetConfig) || presetConfig.isBlank() ? Json.object() : Json.object(presetConfig);
        final FieldStorageType fieldStorageType = Objects.isNull(storageType) ? FieldStorageType.TRANSIENT : FieldStorageType.valueOf(storageType.toUpperCase());
        final FieldStore fieldStore = field.getStore();
        final StoreType storeType = fieldStore.storeType();
        final Optional<String> optionalPlaceHolderValue = Optional.ofNullable(presetConfigAsJson.optString(PresetConfig.Configuration.PLACE_HOLDER.getName(), null));
        final I18nKey placeHolder = optionalPlaceHolderValue.map(I18nKey::new).orElseGet(() -> fieldTypeService.getFieldTypeByStoreType(storeType).placeholder());

        return new FieldInfo(field.id(), idAsString, field.orgId(), columnIndex, tableIndex,
                field.getActive(), field.getI18nName(), field.getAliasName(), storeType,
                fieldStorageType, fieldConfig, presetConfigAsJson, fieldStore.id(), field.getSource().id(), presetConfig,
                field.getMapperTable(), field.getDataTable(), placeHolder, field.getInternalName());
    }

}
