package com.tlc.crm.entity.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.handler.ModelDescriptorLoader;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.StoreType;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Implements the {@link ModelDescriptorLoader} and provides the way to merge the properties
 *
 * @param <T>
 * @author KowsalyaSP
 * @version 1.0
 */
public class ModelDescriptorLoaderImpl<T> implements ModelDescriptorLoader<T> {
    private final FieldService fieldService;

    public ModelDescriptorLoaderImpl() {
        this.fieldService = FieldService.getInstance();
    }

    private static class Instance {
        private static final ModelDescriptorLoaderImpl<?> INSTANCE = new ModelDescriptorLoaderImpl<>();
    }

    /**
     * Fetches the {@link ModelDescriptorLoader}
     *
     * @return {@link ModelDescriptorLoader}
     */
    public static ModelDescriptorLoader<?> getInstance() {
        return ModelDescriptorLoaderImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldNames     Represents the {@link Collection} of field names
     * @param primaryModel   Represents the primary model
     * @param secondaryModel Represents the secondary model
     */
    @Override
    public void mergeProperties(final Collection<String> fieldNames, final T primaryModel, final T secondaryModel) {
        if (!fieldNames.isEmpty()) {
            final Class<?> entityClass = primaryModel.getClass();

            for (final String fieldName : fieldNames) {

                try {
                    final Field field = entityClass.getDeclaredField(fieldName);

                    field.setAccessible(true);

                    final Object secondaryEntityValue = field.get(secondaryModel);
                    final Object primaryEntityValue = field.get(primaryModel);

                    if (Objects.isNull(primaryEntityValue) && Objects.nonNull(secondaryEntityValue)) {
                        field.set(primaryModel, secondaryEntityValue);
                    }
                } catch (Exception exception) {
                    throw ErrorCode.get(EntityErrorCodes.MERGE_ACTION_FAILED, "i18n.contact.merge.action.failed");
                }
            }
        }

        if (primaryModel instanceof final Entity primaryEntity && secondaryModel instanceof final Entity secondaryEntity) {
            loadEntityValueHolder(primaryEntity, secondaryEntity);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param primaryEntity   Represents the primary entity
     * @param secondaryEntity Represents the secondary entity
     */
    public void loadEntityValueHolder(final Entity primaryEntity, final Entity secondaryEntity) {
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(primaryEntity.orgId(), primaryEntity.getEntityType());
        final ValueHolder secondaryValueHolder = secondaryEntity.getValueHolder();

        if (Objects.nonNull(secondaryValueHolder)) {
            final ValueHolder existingValueHolder = primaryEntity.getValueHolder();
            final ValueHolder primaryValueHolder = Objects.isNull(existingValueHolder) ? new ValueHolder() : existingValueHolder;

            fields.stream().filter(FieldInfo::isPersistent).forEach(field -> {
                final Long fieldId = field.id();
                final Object value = secondaryValueHolder.get(fieldId);

                if ((Objects.isNull(primaryValueHolder.get(fieldId)) || StoreType.MULTI_SELECT.equals(field.storeType())) && Objects.nonNull(value)) {
                    primaryValueHolder.put(fieldId, value);
                }
            });
            primaryEntity.setValueHolder(primaryValueHolder);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param entity         Represents the entity
     * @param excludedFields Holds the excluded fields to remove
     * @return {@link Set} of field names
     */
    @Override
    public Set<String> getFieldNames(final T entity, final Set<String> excludedFields) {
        if (entity == null) {
            return Collections.emptySet();
        }
        final Set<String> fieldNames = new HashSet<>();

        for (final Field field : entity.getClass().getDeclaredFields()) {
            final String fieldName = field.getName();

            if (!excludedFields.contains(fieldName)) {
                fieldNames.add(fieldName);
            }
        }

        return fieldNames;
    }
}
