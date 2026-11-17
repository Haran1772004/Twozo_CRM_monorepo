package com.tlc.crm.entity.builder;

import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.DropdownDetails;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Builds the {@link CriteriaField} from {@link FieldInfo}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class CriteriaFieldBuilder {

    private final FieldTypeService fieldTypeService;

    private static class Instance {
        private static final CriteriaFieldBuilder INSTANCE = new CriteriaFieldBuilder();
    }

    private CriteriaFieldBuilder() {
        fieldTypeService = FieldTypeService.getInstance();
    }

    public static CriteriaFieldBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Builds the collection of {@link CriteriaField} from array of {@link FieldInfo}.
     * </p>
     *
     * @param fields Array of {@link FieldInfo}.
     * @return Collection of {@link CriteriaField}.
     */
    public Collection<CriteriaField> build(final FieldInfo... fields) {
        return build(List.of(fields));
    }

    /**
     * <p>
     * Builds the collection of {@link CriteriaField} from collection of {@link FieldInfo}.
     * </p>
     *
     * @param fields Collection of {@link FieldInfo}.
     * @return Collection of {@link CriteriaField}.
     */
    public Collection<CriteriaField> build(final Collection<FieldInfo> fields) {
        return fields.stream().filter(fieldInfo -> !fieldInfo.storeType().isTextType()
                && fieldInfo.presetConfig().isFilterable()).map(this::build).toList();
    }

    /**
     * <p>
     * Builds the {@link CriteriaField} from {@link FieldInfo}.
     * </p>
     *
     * @param field {@link FieldInfo}.
     * @return {@link CriteriaField}.
     */
    public CriteriaField build(final FieldInfo field) {
        final CriteriaField criteriaField = new CriteriaField();
        final PresetConfig presetConfig = field.presetConfig();
        final StoreType storeType = presetConfig.contains(PresetConfig.Configuration.FILTER_FIELD_TYPE) ?
                StoreType.valueOf(presetConfig.getFilterFieldType()) : field.storeType();
        final FieldType fieldType = fieldTypeService.getFieldTypeByStoreType(storeType);
        final String fieldAlias = field.aliasName();
        final String dropdown = presetConfig.contains(PresetConfig.Configuration.FILTER_DROPDOWN) ?
                presetConfig.filterDropdown() : presetConfig.dropdown();

        criteriaField.setId(field.id());
        criteriaField.setSourceId(field.sourceId());
        criteriaField.setFieldType(fieldType);
        criteriaField.setName(field.i18nName());
        criteriaField.setAliasName(Objects.isNull(fieldAlias) ? field.fieldName().toLowerCase().trim().replace(" ", "_") : fieldAlias);
        criteriaField.setHasValueDropdown(StoreType.isSelectorType(storeType) || storeType.isEntityType());

        if (Objects.nonNull(dropdown)) {
            final DropdownDetails dropdownDetails = new DropdownDetails();
            final DropdownType filterDropdownType = presetConfig.filterDropdownType();

            dropdownDetails.setDropdownType(filterDropdownType);
            dropdownDetails.setStatic(DropdownType.STATIC == filterDropdownType);
            dropdownDetails.setDropdownName(dropdown);
            criteriaField.addDropdownDetails(dropdownDetails);
        }

        return criteriaField;
    }
}
