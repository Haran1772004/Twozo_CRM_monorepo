package com.tlc.crm.common.list;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.SubFieldConfig;
import com.tlc.crm.common.list.transformer.ModelTransformer;
import com.tlc.crm.common.list.transformer.FieldTransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Represents the structure defining the fields and their configurations for a list, along with the provider
 * </p>
 *
 * @author Abishek
 * @version 1.0
 */
public class ListStructure {
    private final Long providerId;
    private final ModelTransformer modelTransformer;
    private final Map<String, FieldConfig> fieldsMap;
    private final Map<String, SubFieldConfig> subFieldsMap;
    private Collection<FieldConfig> fieldList;
    private final FieldConfig primaryField;
    private final Map<String, FieldTransformer> fieldTransformerMap;

    public ListStructure(final Long providerId, final List<FieldConfig> fields, final ModelTransformer modelTransformer) {
        this.providerId = Objects.requireNonNull(providerId);
        this.modelTransformer = modelTransformer;
        this.fieldsMap = new HashMap<>();
        this.fieldTransformerMap = new HashMap<>();
        this.subFieldsMap = new HashMap<>();

        this.fieldList = List.copyOf(fields);
        for (FieldConfig field : fieldList) {
            final String fieldName = Objects.requireNonNullElse(field.getName(), field.getIdAsString());
            final Collection<SubFieldConfig> subFieldsConfig = field.getSubFieldsConfig();

            fieldsMap.put(fieldName, field);

            final FieldTransformer fieldTransformer = field.getTransformer();
            if (fieldTransformer != null) {
                fieldTransformerMap.put(fieldName, fieldTransformer);
            }

            if (Objects.nonNull(subFieldsConfig) && !subFieldsConfig.isEmpty()) {
                for (final SubFieldConfig subFieldConfig : subFieldsConfig) {
                    subFieldsMap.put(subFieldConfig.getName(), subFieldConfig);
                }
            }
        }
        final Optional<FieldConfig> primaryField = fields.stream().filter(FieldConfig::isPrimary).findFirst();
        if (primaryField.isPresent()) {
            this.primaryField = primaryField.get();
        } else {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, "Primary Field not found!!!!");
        }
    }

    /**
     * <p>
     * Sets the updated field configs into the listStructure.
     * </p>
     *
     * @param fieldConfigs Represents the {@link Collection} of {@link FieldConfig}
     */
    public void setFieldConfigs(final Collection<FieldConfig> fieldConfigs) {
        final Collection<FieldConfig> configs = new ArrayList<>(fieldList);

        for (final FieldConfig fieldConfig : fieldConfigs) {
            configs.removeIf(config -> config.getName().equals(fieldConfig.getName())
                    && config.getFieldId().equals(fieldConfig.getFieldId()));

            configs.add(fieldConfig);
        }
        this.fieldList = configs;
    }

    public FieldConfig getPrimaryField() {
        return primaryField;
    }

    public Long getProviderId() {
        return providerId;
    }

    public FieldConfig getField(final String fieldName) {
        return fieldsMap.get(fieldName);
    }

    public FieldTransformer getTransformer(String fieldName) {
        return fieldTransformerMap.get(fieldName);
    }

    public Collection<FieldConfig> getFields() {
        return fieldList;
    }

    /**
     * Fetches the {@link SubFieldConfig} by the given sub-field name.
     *
     * @param subFieldName Refers the sub-field name.
     * @return The {@link SubFieldConfig}.
     */
    public SubFieldConfig getSubField(final String subFieldName) {
        return subFieldsMap.get(subFieldName);
    }

    /**
     * Gets the {@link ModelTransformer} of the provider.
     *
     * @return {@link ModelTransformer}.
     */
    public ModelTransformer getProviderTransformer() {
        return modelTransformer;
    }
}