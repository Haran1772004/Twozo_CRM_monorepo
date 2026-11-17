package com.tlc.crm.entity.fieldaudit;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.entity.fieldaudit.manager.FieldTrackingConfigManager;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditInfo;
import com.tlc.crm.entity.fieldaudit.models.FieldTrackingConfig;
import com.tlc.crm.entity.fieldaudit.sql.resource.OMFAFIELDAUDIT;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.Range;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.l10n.temporal.model.Temporal;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * <p>
 * Utility class that loads field audit rows into a {@link DataContainer} based on field changes detected from old and new value maps.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class FieldAuditHandler {

    private final FieldTrackingConfigManager fieldTrackingConfigManager;
    private final FieldService fieldService;

    private FieldAuditHandler() {
        fieldTrackingConfigManager = FieldTrackingConfigManager.getInstance();
        fieldService = FieldService.getSecureInstance();
    }

    private static final class InstanceHolder {
        private static final FieldAuditHandler INSTANCE = new FieldAuditHandler();
    }

    public static FieldAuditHandler getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private Table getHistoryTable() {
        return Table.get(OMFAFIELDAUDIT.TABLE);
    }

    /**
     * <p>
     * Determines whether a field is eligible for field audit tracking.
     * A field is trackable if it satisfies all of the following conditions:
     * </p>
     *
     * @param fieldInfo the field metadata to evaluate
     * @return {@code true} if the field is eligible for field history tracking
     */
    public static boolean isFieldAuditTrackable(final FieldInfo fieldInfo) {
        if (Objects.isNull(fieldInfo)) {
            return false;
        }

        if (!fieldInfo.isActive()) {
            return false;
        }
        final PresetConfig presetConfig = fieldInfo.presetConfig();

        if (Objects.isNull(presetConfig)) {
            return true;
        }
        return !presetConfig.readOnly();
    }

    /**
     * <p>
     * Loads field audit rows into the target {@link DataContainer} for all tracked fields
     * whose values have changed. This method is designed to be called from inside the existing
     * timeline loading methods so that field history rows are committed together with the
     * entity update and timeline entries.
     * </p>
     *
     * @param orgId           the organisation id
     * @param source          the entity source (Contact / Company / Deal)
     * @param recordId        the entity record id
     * @param userId          the user performing the change
     * @param editContext     the context describing the source of the change
     * @param allFields       all field metadata for the source module (used for alias → ID resolution)
     * @param oldValues       prior field values keyed by field alias name (may be empty for CREATE)
     * @param newValues       current field values keyed by field alias name
     * @param targetContainer the container to load history rows into
     */
    public void loadFieldAudit(final long orgId, final Source source, final long recordId, final Long userId, final FieldAuditInfo editContext,
            final Collection<FieldInfo> allFields, final Map<String, String> oldValues, final Map<String, String> newValues, final DataContainer targetContainer) {
        final FieldTrackingConfig config = fieldTrackingConfigManager.getOrDefault(orgId, (short) source.getId());

        if (!config.isTrackingEnabled()) {
            return;
        }
        final Map<String, FieldInfo> fieldsByAlias = allFields.stream()
                .filter(fieldInfo -> Objects.nonNull(fieldInfo.aliasName()))
                .collect(Collectors.toMap(FieldInfo::aliasName, fieldInfo -> fieldInfo));
        final Set<String> trackedFields = config.trackedFields();
        final Set<String> fieldsToInspect = (Objects.isNull(trackedFields) || trackedFields.isEmpty()) ? newValues.keySet() : trackedFields;
        final FieldAuditInfo fieldAuditInfo = resolveContext(editContext);

        for (final String fieldName : fieldsToInspect) {

            if (!newValues.containsKey(fieldName) && !oldValues.containsKey(fieldName)) {
                continue;
            }
            final FieldInfo fieldInfo = fieldsByAlias.get(fieldName);

            if (Objects.isNull(fieldInfo)) {
                continue;
            }
            final String oldValue = normalize(oldValues.get(fieldName));
            final String newValue = normalize(newValues.get(fieldName));

            if (Objects.equals(oldValue, newValue)) {
                continue;
            }
            loadHistoryRow(orgId, source, recordId, fieldInfo.id(), oldValue, newValue,
                    editContext.fieldAuditSource().hasDynamicSubvalue() ? null : userId, fieldAuditInfo, targetContainer);
        }
    }

    /**
     * <p>
     * Loads field audit rows for all trackable fields from the provided field metadata
     * </p>
     *
     * @param orgId           the organisation id
     * @param source          the entity source (Contact / Company / Deal)
     * @param recordId        the entity record id
     * @param userId          the user performing the change
     * @param editContext     the context describing the source of the change
     * @param allFields       all field metadata for the source module
     * @param oldValues       prior field values keyed by field alias name
     * @param newValues       current field values keyed by field alias name
     * @param targetContainer the container to load history rows into
     */
    public void loadFieldAuditForTrackableFields(final long orgId, final Source source, final long recordId, final Long userId,
            final FieldAuditInfo editContext, final Collection<FieldInfo> allFields, final Map<String, String> oldValues,
            final Map<String, String> newValues, final DataContainer targetContainer) {
        final FieldTrackingConfig config = fieldTrackingConfigManager.getOrDefault(orgId, (short) source.getId());

        if (!config.isTrackingEnabled()) {
            return;
        }
        final FieldAuditInfo fieldAuditInfo = resolveContext(editContext);

        for (final FieldInfo fieldInfo : allFields) {

            if (!isFieldAuditTrackable(fieldInfo)) {
                continue;
            }
            final String fieldName = fieldInfo.aliasName();

            if (!config.isFieldTracked(fieldName)) {
                continue;
            }

            if (!newValues.containsKey(fieldName) && !oldValues.containsKey(fieldName)) {
                continue;
            }
            final String oldValue = normalize(oldValues.get(fieldName));
            final String newValue = normalize(newValues.get(fieldName));

            if (Objects.equals(oldValue, newValue)) {
                continue;
            }
            loadHistoryRow(orgId, source, recordId, fieldInfo.id(), oldValue, newValue,
                    editContext.fieldAuditSource().hasDynamicSubvalue() ? null : userId, fieldAuditInfo, targetContainer);
        }
    }

    /**
     * <p>
     * Detects a single field change and loads it as a field audit row if the field
     * is tracked and the value has actually changed. This convenience method can be called
     * directly from the timeline comparison code where old and new values are already available.
     * </p>
     *
     * <p>
     * The field's numeric ID is resolved internally from the source and alias name.
     * </p>
     *
     * @param orgId           the organisation id
     * @param source          the entity source
     * @param recordId        the entity record id
     * @param userId          the user performing the change
     * @param editContext     the context describing the source of the change
     * @param fieldName       the alias name of the changed field
     * @param oldValue        the previous value (may be null)
     * @param newValue        the new value (may be null)
     * @param targetContainer the container to load the history row into
     */
    public void loadFieldAuditEntry(final long orgId, final Source source, final long recordId, final Long userId,
            final FieldAuditInfo editContext, final String fieldName, final String oldValue, final String newValue, final DataContainer targetContainer) {
        final FieldTrackingConfig config = fieldTrackingConfigManager.getOrDefault(orgId, (short) source.getId());

        if (!config.isFieldTracked(fieldName)) {
            return;
        }
        final String normalizedOld = normalize(oldValue);
        final String normalizedNew = normalize(newValue);

        if (Objects.equals(normalizedOld, normalizedNew)) {
            return;
        }
        final Optional<FieldInfo> optionalField = fieldService.getDefaultFieldByAliasName(source, fieldName);

        if (optionalField.isEmpty()) {
            return;
        }
        loadHistoryRow(orgId, source, recordId, optionalField.get().id(), normalizedOld, normalizedNew,
                editContext.fieldAuditSource().hasDynamicSubvalue() ? null : userId, resolveContext(editContext), targetContainer);
    }

    /**
     * Creates and adds a single field audit row to the target container.
     *
     * @param fieldId the numeric field ID (foreign key to OMETFields.ID)
     */
    private void loadHistoryRow(final long orgId, final Source source, final long recordId, final Long fieldId, final String oldValue,
            final String newValue, final Long userId, final FieldAuditInfo editContext, final DataContainer targetContainer) {
        final Row historyRow = new Row(getHistoryTable());

        historyRow.setOrgId(orgId);
        historyRow.set(OMFAFIELDAUDIT.SOURCE_ID, (short) source.getId());
        historyRow.set(OMFAFIELDAUDIT.ENTITY_ID, recordId);
        historyRow.set(OMFAFIELDAUDIT.FIELD_ID, fieldId);
        historyRow.set(OMFAFIELDAUDIT.PREVIOUS_VALUE, oldValue);
        historyRow.set(OMFAFIELDAUDIT.NEW_VALUE, newValue);
        historyRow.set(OMFAFIELDAUDIT.UPDATED_BY, userId);
        historyRow.set(OMFAFIELDAUDIT.UPDATED_AT, Instant.now().toEpochMilli());
        historyRow.set(OMFAFIELDAUDIT.EDIT_SOURCE_ID, editContext.fieldAuditSource().getId());
        final Long sourceReferenceId = editContext.sourceReferenceId();
        final JsonObject metadata = editContext.metadata();

        if (Objects.nonNull(sourceReferenceId)) {
            historyRow.set(OMFAFIELDAUDIT.EDIT_REFERENCE_ID, sourceReferenceId);
        }
        if (Objects.nonNull(metadata)) {
            historyRow.set(OMFAFIELDAUDIT.METADATA, metadata.toString());
        }
        targetContainer.addNewRow(historyRow);
    }

    /**
     * Resolves the effective context, falling back to the default user context if null.
     */
    private static FieldAuditInfo resolveContext(final FieldAuditInfo editContext) {
        return Objects.isNull(editContext) ? FieldAuditInfo.defaultContext() : editContext;
    }

    /**
     * <p>
     * Normalises a field value for comparison. {@code null} and the empty string are
     * treated as semantically equal.
     * </p>
     */
    private static String normalize(final String value) {
        if (Objects.isNull(value) || value.isEmpty()) {
            return null;
        }
        return value;
    }

    /**
     * <p>
     * Extracts field values from a {@link ValueHolder}, filtering to only tracking-enabled fields
     * when a non-null {@link FieldTrackingConfig} is provided. This avoids unnecessary value
     * extraction and conversion for fields that are not being tracked.
     * </p>
     *
     * @param valueHolder the value holder containing field values (may be null)
     * @param allFields   all field metadata for the source module
     * @param config      tracking config to filter fields; if null, all persistent active fields are extracted
     * @return a map of field alias name to string value
     */
    public static Map<String, String> extractFieldValues(final ValueHolder valueHolder, final Collection<FieldInfo> allFields,
            final FieldTrackingConfig config) {
        final Map<String, String> values = new HashMap<>();

        if (Objects.isNull(valueHolder) || Objects.isNull(allFields)) {
            return values;
        }

        for (final FieldInfo fieldInfo : allFields) {

            if (!fieldInfo.isPersistent() || !fieldInfo.isActive()) {
                continue;
            }

            if (Objects.nonNull(config) && !config.isFieldTracked(fieldInfo.aliasName())) {
                continue;
            }
            final Long fieldId = fieldInfo.id();
            final Object rawValue = valueHolder.get(fieldId);

            if (Objects.isNull(rawValue)) {
                continue;
            }
            final String stringValue = convertValueToString(rawValue, fieldInfo.storeType());

            if (Objects.nonNull(stringValue)) {
                values.put(fieldInfo.aliasName(), stringValue);
            }
        }
        return values;
    }

    /**
     * <p>
     * Resolves new custom field values for MODEL and MULTI_VALUE_STORE fields that may contain only IDs
     * in the value holder. This method iterates over all fields, checks tracking eligibility, and
     * delegates to the provided resolver function for actual value resolution.
     * </p>
     *
     * @param config          the tracking config for the source module
     * @param valueHolder     the value holder containing raw field values
     * @param allFields       all field metadata for the source module
     * @param newCustomValues the map to populate with resolved values (skips already-present keys)
     * @param resolver        a function that takes (FieldInfo, rawValue) and returns the resolved string, or null
     */
    public static void resolveNewCustomFieldValues(final FieldTrackingConfig config, final ValueHolder valueHolder,
            final Collection<FieldInfo> allFields, final Map<String, String> newCustomValues, final BiFunction<FieldInfo, Object, String> resolver) {
        if (Objects.isNull(valueHolder) || Objects.isNull(allFields) || !config.isTrackingEnabled()) {
            return;
        }

        for (final FieldInfo field : allFields) {
            final String aliasName = field.aliasName();
            final StoreType storeType = field.storeType();

            if (!field.isPersistent() || Objects.isNull(aliasName) || Objects.isNull(storeType) || !config.isFieldTracked(aliasName)
                    || (!StoreType.isModelStore(storeType) && !StoreType.isMultiValueStore(storeType))) {
                continue;
            }
            final Object rawValue = valueHolder.get(field.id());

            if (Objects.isNull(rawValue)) {
                continue;
            }
            final String resolved = resolver.apply(field, rawValue);

            if (Objects.nonNull(resolved)) {
                newCustomValues.put(aliasName, resolved);
            }
        }
    }

    /**
     * <p>
     * Converts a raw field value from the {@link ValueHolder} to its string representation
     * based on the field's {@link StoreType}. Handles model types, collections, temporal values,
     * and primitive types uniformly.
     * </p>
     *
     * @param value     the raw value from the value holder
     * @param storeType the store type of the field
     * @return the string representation, or null if the value cannot be converted
     */
    private static String convertValueToString(final Object value, final StoreType storeType) {

        if (StoreType.isSingleValueStore(storeType)) {

            if (value instanceof final Temporal temporal) {
                return Objects.nonNull(temporal.getTimeStamp()) ? String.valueOf(temporal.getTimeStamp()) : null;
            } else {
                return String.valueOf(value);
            }
        } else if (StoreType.isMultiValueStore(storeType)) {

             if (value instanceof final Collection<?> collection) {

                if (collection.isEmpty()) {
                    return null;
                }
                return collection.stream().map(option -> {

                    if (option instanceof Option optionValue) {
                        final Long optionId = optionValue.id();
                        final String optionName = optionValue.getOption();

                        if (Objects.isNull(optionName) && Objects.nonNull(optionId)) {
                            return String.valueOf(optionId);
                        } else if (Objects.nonNull(optionName)) {
                            return optionName;
                        }
                    } else {
                       return String.valueOf(option);
                    }
                    return null;
                }).collect(Collectors.joining(","));
            }
        } else if (StoreType.isModelStore(storeType)) {

            if (value instanceof final Range range) {
                return String.format("%s,%s", range.getFrom().getTimeStamp(), range.getTo().getTimeStamp());
            } else if (value instanceof final Entity entity) {
                return entity.getName();
            } else if (value instanceof final Option option) {
                return option.getOption();
            } else if (value instanceof final Monetary monetary) {
                return String.format("%s%s", monetary.getSymbol(), monetary.getValue());
            } else if (value instanceof final Email email) {
                return email.getValue();
            } else if (value instanceof final Phone phone) {
                return phone.getNumberWithCode();
            } else if (value instanceof final Address address) {
                return address.toString();
            } else if (value instanceof final CrmUser user) {
                return user.getFullName();
            }
        }
        return String.valueOf(value);
    }
}