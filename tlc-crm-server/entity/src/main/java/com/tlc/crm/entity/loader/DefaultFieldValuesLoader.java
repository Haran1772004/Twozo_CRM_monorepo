package com.tlc.crm.entity.loader;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.common.service.FieldPropertiesService;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.common.service.PropertyDescriptorService;
import com.tlc.crm.entity.validator.annotation.Field;
import com.tlc.crm.fields.models.StoreType;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the loader which loads the default field values into {@link ValueHolder}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class DefaultFieldValuesLoader {

    private final FieldService fieldService;
    private final FieldPropertiesService fieldPropertiesService;
    private final PropertyDescriptorService propertyDescriptorService;

    private static final class Instance {
        private static final DefaultFieldValuesLoader INSTANCE = new DefaultFieldValuesLoader();
    }

    public static DefaultFieldValuesLoader getInstance() {
        return Instance.INSTANCE;
    }

    private DefaultFieldValuesLoader() {
        fieldService = FieldService.getSecureInstance();
        fieldPropertiesService = FieldPropertiesService.getInstance();
        propertyDescriptorService = PropertyDescriptorService.getInstance();
    }

    /**
     * <p>
     * Loads the default fields into {@link ValueHolder}.
     * </p>
     *
     * @param entity Holds the details of the entity.
     */
    public <T extends Entity> void load(final T entity) {
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(entity.getEntityType());
        final Class<? extends Entity> entityClass = entity.getClass();
        final ValueHolder oldValueHolder = entity.getValueHolder();
        final ValueHolder valueHolder = Objects.isNull(oldValueHolder) ? new ValueHolder() : oldValueHolder;

        for (final java.lang.reflect.Field field : fieldPropertiesService.getAnnotatedFields(entityClass, Field.class)) {
            final Field fieldAnnotation = field.getAnnotation(Field.class);
            final Optional<FieldInfo> optionalFieldInfo = Optional.ofNullable(fields.get(fieldAnnotation.aliasName()));

            if (optionalFieldInfo.isPresent()) {
                final FieldInfo fieldInfo = optionalFieldInfo.get();
                final StoreType storeType = fieldInfo.storeType();

                if (storeType.hasSubtypes() || StoreType.isSelectorType(storeType)) {
                    final PropertyDescriptor propertyDescriptor = propertyDescriptorService.get(field.getName(), entityClass);
                    final Object value;

                    try {
                        value = propertyDescriptor.getReadMethod().invoke(entity);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw ErrorCode.get(EntityErrorCodes.UNABLE_TO_INVOKE_VALUE);
                    }

                    if (Objects.nonNull(value)) {
                        if (value instanceof final Option option) {
                            valueHolder.put(fieldInfo.id(), option.id());
                        } else if (value instanceof final Collection<?> values
                                && values.stream().filter(Objects::nonNull).findFirst().filter(Option.class::isInstance).isPresent()) {
                            valueHolder.put(fieldInfo.id(), values.stream().map(option -> ((Option) option).id()).collect(Collectors.toSet()));
                        } else {
                            valueHolder.put(fieldInfo.id(), value);
                        }
                    }
                }
            }
        }
        entity.setValueHolder(valueHolder);
    }
}
