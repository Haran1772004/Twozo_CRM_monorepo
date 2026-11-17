package com.tlc.crm.core.audit.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.audit.AuditLogger;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.audit.log.manager.AuditManager;
import com.tlc.crm.entity.audit.log.model.AuditAction;
import com.tlc.crm.entity.audit.log.model.AuditAttribute;
import com.tlc.crm.entity.audit.log.model.AuditCategory;
import com.tlc.crm.entity.audit.log.model.AuditEntity;
import com.tlc.crm.entity.audit.log.model.AuditLog;
import com.tlc.crm.entity.audit.log.model.AuditLogData;
import com.tlc.crm.entity.audit.log.model.AuditSource;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.common.service.FieldPropertiesService;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.common.service.PropertyDescriptorService;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditEntityName;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditField;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.i18n.I18nKey;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * <p>
 * Handles the audit log data related data to audit
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public class AuditLoggerImpl implements AuditLogger {

    private final FieldService fieldService;
    private final FieldPropertiesService fieldPropertiesService;
    private final PropertyDescriptorService propertyDescriptorService;
    private final AuditManager auditManager;

    private AuditLoggerImpl() {
        fieldService = FieldService.getInstance();
        fieldPropertiesService = FieldPropertiesService.getInstance();
        propertyDescriptorService = PropertyDescriptorService.getInstance();
        auditManager = AuditManager.getInstance();
    }

    private static final class Instance {
        private static final AuditLoggerImpl AUDIT_LOG_HANDLER = new AuditLoggerImpl();
    }

    public static AuditLoggerImpl getInstance() {
        return Instance.AUDIT_LOG_HANDLER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param auditLogData Which contains the collection of {@link AuditLogData} details
     */
    @Override
    public void log(long orgId, final Collection<AuditLogData> auditLogData) {
        final Collection<AuditEntity> auditEntities = buildAuditEntity(orgId, auditLogData);

        auditManager.auditEntry(orgId, auditEntities);
    }

    /**
     * <p>
     * Builds the audit entity for given collection of {@link TlcModel}
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param auditLogData Represents the {@link AuditLogData} details
     */
    private Collection<AuditEntity> buildAuditEntity(final long orgId, final Collection<AuditLogData> auditLogData) {
        final Collection<AuditEntity> auditEntities = new ArrayList<>();

        for (final AuditLogData auditData : auditLogData) {
            final String auditSource = auditData.auditSource().getName();
            final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(auditSource);
            final TlcModel oldModel = auditData.oldModel();
            final TlcModel newModel = auditData.newModel();
            final AuditEntity.Builder auditEntityBuilder = new AuditEntity.Builder();
            final AuditAttribute auditAttribute = auditData.auditAttribute();
            final Long userId = auditData.userId();
            final Long entityId = auditData.entityId();
            final String entityName = auditData.entityName();

            if (Objects.nonNull(newModel)) {
                final Class<? extends TlcModel> entityClass = newModel.getClass();

                for (final java.lang.reflect.Field field : fieldPropertiesService.getAnnotatedFields(entityClass, AuditField.class)) {
                    final AuditField auditField = field.getAnnotation(AuditField.class);
                    final FieldInfo fieldInfo = fields.get(auditField.aliasName());
                    final Object value = fetchValue(field, newModel);

                    if (Objects.nonNull(fieldInfo) && fieldInfo.storeType().isEntityType() && Objects.nonNull(value)) {

                        if (value instanceof final Entity entity) {
                            final Entity.State entityState = entity.getEntityState();

                            if (Entity.State.NEW == entityState) {
                                final AuditSource source = AuditSource.get(entity.getEntityType());
                                final Collection<AuditLogData> audits = List.of(new AuditLogData(userId, entity.id(), entity, source, AuditAction.ADDED));

                                auditEntities.addAll(buildAuditEntity(orgId, audits));
                            }
                        }
                    }
                }

                if (Objects.isNull(entityName)) {
                    fetchEntityName(newModel).ifPresent(auditEntityBuilder::setEntityName);
                }
            }
            final Collection<AuditAttribute> auditAttributes = new ArrayList<>();
            final AuditAction auditAction = auditData.auditAction();

            if (Objects.nonNull(auditAttribute)) {
                auditAttributes.add(auditAttribute);
            } else if (Objects.nonNull(oldModel) && Objects.nonNull(newModel)) {
                auditAttributes.addAll(buildAuditAttributes(newModel, oldModel, fields));
            }

            if (Objects.nonNull(entityName)) {
                auditEntityBuilder.setEntityName(entityName);
            }

            if (!auditAction.needFieldChangesInfo() || !auditAttributes.isEmpty()) {
                final AuditLog auditLog = new AuditLog(userId, new DateTime(Instant.now().toEpochMilli()), auditAction, auditAttributes);

                auditEntityBuilder.setAuditLogs(List.of(auditLog));
                auditEntityBuilder.setEntityId(entityId);
                auditEntityBuilder.setAuditCategory(AuditCategory.get(auditSource));
                auditEntityBuilder.setOrgId(orgId);
                auditEntities.add(auditEntityBuilder.build());
            }
        }

        return auditEntities;
    }

    /**
     * Fetches the value of the field annotated with {@link AuditEntityName} from the given model.
     *
     * @param tlcModel the model to extract the entity name from
     * @return an {@link Optional} containing the entity name if found, or {@link Optional#empty()} if not present
     */
    private Optional<String> fetchEntityName(final TlcModel tlcModel) {
        final Optional<Field> entityNameField = fieldPropertiesService.getAnnotatedFields(tlcModel.getClass(), AuditEntityName.class).stream().findFirst();

        return Optional.ofNullable(entityNameField.map(nameField -> (String) fetchValue(nameField, tlcModel)).orElseGet(() -> null));
    }

    /**
     * {@inheritDoc}
     *
     * @param newModel Holds the details of the new model
     * @param oldModel Holds the details of the old model
     * @param fields   a map containing metadata about the fields, keyed by alias name
     * @return The collection of {@link AuditAttribute}
     */
    private Collection<AuditAttribute> buildAuditAttributes(final TlcModel newModel, final TlcModel oldModel,
                                                            final Map<String, FieldInfo> fields) {
        final Collection<AuditAttribute> auditAttributes = new ArrayList<>();
        final Class<? extends TlcModel> modelClass = newModel.getClass();

        for (final java.lang.reflect.Field field : fieldPropertiesService.getAnnotatedFields(modelClass, AuditField.class)) {
            final AuditField auditField = field.getAnnotation(AuditField.class);
            final String fieldName = field.getName();
            final FieldInfo fieldInfo = fields.get(auditField.aliasName());
            final StoreType storeType = Objects.nonNull(fieldInfo) && Objects.nonNull(fieldInfo.storeType()) ? fieldInfo.storeType() : null;
            final PropertyDescriptor propertyDescriptor = propertyDescriptorService.get(fieldName, modelClass);
            final Object newValue = fetchValue(newModel, propertyDescriptor);
            final Object oldValue = fetchValue(oldModel, propertyDescriptor);
            final I18nKey displayName = new I18nKey(auditField.displayName());

            auditAttributes.addAll(buildAuditAttributes(displayName, oldValue, newValue, storeType));
        }

        return auditAttributes;
    }

    /**
     * <p>
     * Fetches the value of the given {@link Field} from the {@link TlcModel}.
     * </p>
     *
     * @param field Represents the metadata of the field.
     * @param model Holds the details of the model.
     * @return Value of the given {@link Field}.
     */
    private Object fetchValue(final Field field, final TlcModel model) {
        final PropertyDescriptor propertyDescriptor = propertyDescriptorService.get(field.getName(), model.getClass());

        return fetchValue(model, propertyDescriptor);
    }

    /**
     * <p>
     * Fetches the value of the {@link Field} from the {@link TlcModel}.
     * </p>
     *
     * @param model              Holds the details of the model.
     * @param propertyDescriptor Represents the descriptor of the field properties.
     * @return Value of the given {@link Field}.
     */
    private Object fetchValue(final TlcModel model, final PropertyDescriptor propertyDescriptor) {
        try {
            return propertyDescriptor.getReadMethod().invoke(model);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw ErrorCode.get(CoreErrorCodes.MODEL_VALUE_ACCESS_FAILED);
        }
    }

    /**
     * <p>
     * Builds the {@link AuditAttribute} using the given field and their values.
     * </p>
     *
     * @param displayName   Represents the display name of the field.
     * @param oldFieldValue Represents the old field value.
     * @param newFieldValue Represents the new field value.
     * @return Collection audit attributes.
     */
    private Collection<AuditAttribute> buildAuditAttributes(final I18nKey displayName, final Object oldFieldValue,
                                                            final Object newFieldValue, final StoreType storeType) {
        final Collection<AuditAttribute> auditAttributes = new ArrayList<>();
        final Function<Collection<?>, Collection<?>> valueResolver = values -> {
            final Optional<?> firstOldValue = values.stream().findFirst();

            return firstOldValue.map(firstValue -> {
                if (firstValue instanceof TlcModel) {
                    return values.stream().map(oldValue -> fetchEntityName((TlcModel) oldValue).orElseGet(() -> null)).toList();
                }
                return values;
            }).stream().toList();
        };

        if (oldFieldValue instanceof final Collection<?> oldValues) {
            final Collection<?> oldValueCollection = valueResolver.apply(oldValues);

            if (newFieldValue instanceof final Collection<?> newValues) {
                if (!oldValues.containsAll(newValues) || !newValues.containsAll(oldValues)) {
                    auditAttributes.add(new AuditAttribute(displayName, oldValueCollection, valueResolver.apply(newValues), AuditAction.UPDATED, storeType));
                }
            } else {
                auditAttributes.add(new AuditAttribute(displayName, oldValueCollection, null, AuditAction.REMOVED, storeType));
            }
        } else if (newFieldValue instanceof final Collection<?> newValues) {
            auditAttributes.add(new AuditAttribute(displayName, null, valueResolver.apply(newValues), AuditAction.ADDED, storeType));
        } else if (!Objects.equals(oldFieldValue, newFieldValue)) {
            final Object oldValue = resolveValue(oldFieldValue);
            final Object newValue = resolveValue(newFieldValue);

            if (!Objects.equals(newValue, oldValue)) {
                final AuditAction subAction = Objects.isNull(oldValue) ? AuditAction.ADDED : Objects.isNull(newValue) ? AuditAction.REMOVED : AuditAction.UPDATED;

                auditAttributes.add(new AuditAttribute(displayName, oldValue, newValue, subAction, storeType));
            }
        }

        return auditAttributes;
    }

    /**
     * <p>
     * Get the resolved value
     * </p>
     *
     * @param fieldValue Represents the  field value
     * @return The resolved value
     */
    private Object resolveValue(final Object fieldValue) {

        if (fieldValue instanceof final TlcModel tlcModel) {
            final Optional<String> value = fetchEntityName(tlcModel);

            return value.orElseGet(() -> null);
        }
        return fieldValue;
    }

    /**
     * <p>
     * Fetches the model value
     * </p>
     *
     * @param model Represents the model
     * @return The model value
     */
    private Map<String, Object> fetchValue(final TlcModel model) {
        final Map<String, Object> valueMap = new HashMap<>();
        final Class<? extends TlcModel> modelClass = model.getClass();

        for (final java.lang.reflect.Field field : fieldPropertiesService.getAnnotatedFields(modelClass, AuditField.class)) {
            try {
                final String fieldName = field.getName();
                final PropertyDescriptor modelPropertyDescriptor = propertyDescriptorService.get(fieldName, modelClass);
                final Object value = modelPropertyDescriptor.getReadMethod().invoke(model);

                valueMap.put(fieldName, value);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                throw ErrorCode.get(CoreErrorCodes.MODEL_VALUE_ACCESS_FAILED);
            }
        }

        return valueMap;
    }
}
