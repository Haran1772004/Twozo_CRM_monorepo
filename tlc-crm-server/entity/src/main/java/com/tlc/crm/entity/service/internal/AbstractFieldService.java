package com.tlc.crm.entity.service.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.internal.FieldConfig;
import com.tlc.crm.entity.resolver.FieldResolver;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.security.iam.LoggedInUserInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implements the {@link FieldService} provides functionality for accessing the {@link FieldInfo}
 *
 * @author Kishore J
 * @version 1.0
 * @see FieldService
 */
public abstract class AbstractFieldService implements FieldService {

    private final FieldResolver fieldResolver;

    protected AbstractFieldService(final FieldResolver fieldResolver) {
        this.fieldResolver = fieldResolver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<FieldInfo> getFieldsBySource(final Long orgId, final String... entitySources) {
        return getFieldsBySource(orgId, List.of(entitySources));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<FieldInfo> getFieldsBySource(final Long orgId, final Collection<String> entitySources) {
        final List<FieldInfo> fields = new ArrayList<>();

        for (final String entitySource : entitySources) {
            final Collection<FieldInfo> sourceFields = fieldResolver.getFieldsBySource(orgId, entitySource);

            if (Objects.nonNull(sourceFields) && !sourceFields.isEmpty()) {
                final Map<Long, FieldInfo> configUpdatedFields = resolveFieldConfigs(orgId, sourceFields);

                fields.addAll(configUpdatedFields.values());
            }
        }

        return sortFields(fields);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, FieldInfo> groupDefaultFieldsByAliasName(final String sourceName, final String... aliasName) {
        return groupDefaultFieldsByAliasName(sourceName, List.of(aliasName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, FieldInfo> groupDefaultFieldsByAliasName(final String sourceName, final Collection<String> aliasNames) {
        final Collection<FieldInfo> fields = getDefaultFieldsBySource(List.of(sourceName));

        return fields.stream().filter(field -> Objects.nonNull(field.aliasName()) && aliasNames.contains(field.aliasName()))
                .collect(Collectors.toUnmodifiableMap(FieldInfo::aliasName, Function.identity()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, FieldInfo> groupDefaultFieldsByAliasName(final String sourceName) {
        final Collection<FieldInfo> fields = getDefaultFieldsBySource(List.of(sourceName));

        return fields.stream().filter(field -> Objects.nonNull(field.aliasName()))
                .collect(Collectors.toUnmodifiableMap(FieldInfo::aliasName, Function.identity()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, FieldInfo> groupDefaultFieldsByAliasName(final Long orgId, final String sourceName) {
        final Collection<FieldInfo> fields = getFieldsBySource(orgId, sourceName);

        return fields.stream().filter(field -> Objects.nonNull(field.aliasName()))
                .collect(Collectors.toUnmodifiableMap(FieldInfo::aliasName, field -> field));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<FieldInfo> getAllFields(final Long orgId) {
        final Collection<FieldInfo> fields = fieldResolver.get(orgId);

        if (Objects.nonNull(fields) && !fields.isEmpty()) {
            final Map<Long, FieldInfo> configUpdatedFields = resolveFieldConfigs(orgId, fields);

            return configUpdatedFields.values();
        }
        return Collections.emptyList();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param sourceName Represents the source name
     * @return the {@link Collection} of field details
     */
    @Override
    public Collection<FieldInfo> getPersistentFieldsBySource(final Long orgId, final String sourceName) {
        return getFieldsBySource(orgId, sourceName).stream().filter(FieldInfo::isPersistent).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, Collection<FieldInfo>> groupAllFieldsByType(final Collection<FieldInfo> fields) {
        final Map<Integer, Collection<FieldInfo>> groupedFields = new HashMap<>();

        fields.forEach(field ->
                groupedFields.computeIfAbsent(field.fieldStore().storeType().getType(), entitySource -> new ArrayList<>()).add(field));
        return groupedFields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, Collection<FieldInfo>> groupAllCustomFieldsByType(final Collection<FieldInfo> fields) {
        final Map<Integer, Collection<FieldInfo>> groupedFields = new HashMap<>();

        fields.stream().filter(FieldInfo::isPersistent).forEach(field ->
            groupedFields.computeIfAbsent(field.fieldStore().storeType().getType(), entitySource -> new ArrayList<>()).add(field));

        return groupedFields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, Collection<FieldInfo>> groupAllFieldsByType(final Long orgId, final String... entitySources) {
        return groupAllFieldsByType(orgId, List.of(entitySources));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, Collection<FieldInfo>> groupAllFieldsByType(final Long orgId, final Collection<String> entitySources) {
        return groupAllFieldsByType(getFieldsBySource(orgId, entitySources));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<FieldInfo> getOrgFields(final Long orgId) {
        final Collection<FieldInfo> fields = fieldResolver.get(orgId).stream().filter(FieldInfo::isPersistent).toList();
        final Map<Long, FieldInfo> configUpdatedFields = resolveFieldConfigs(orgId, fields);

        return configUpdatedFields.values();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Collection<FieldInfo>> getOrgFieldsBySource(final Long orgId) {
        return getOrgFields(orgId).stream().collect(Collectors.groupingBy(
                fieldInfo -> fieldInfo.fieldSource().getName(), Collectors.mapping(fieldInfo -> fieldInfo,
                        Collectors.toCollection(ArrayList::new))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, FieldInfo> getFieldsById(final Long orgId, final Collection<Long> ids) {
        final Map<Long, FieldInfo> fieldMap = new HashMap<>();
        final Collection<FieldInfo> fields = getAllFields(orgId);

        fields.stream().filter(field -> ids.contains(field.id()))
                .forEach(crmField -> fieldMap.put(crmField.id(), crmField));

        return fieldMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FieldInfo getField(final Long orgId, final Long fieldId) {
        return getFieldsById(orgId, List.of(fieldId)).get(fieldId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Collection<FieldInfo>> groupAllFieldsBySource(final Long orgId, final String... sourceNames) {
        return groupAllFieldsBySource(orgId, List.of(sourceNames));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Collection<FieldInfo>> groupAllFieldsBySource(final Long orgId, final Collection<String> sourceNames) {
        final Collection<FieldInfo> fields = getFieldsBySource(orgId, sourceNames);
        final Map<String, Collection<FieldInfo>> groupedFields = new HashMap<>();

        fields.forEach(crmField -> {
            final String sourceName = crmField.fieldSource().getName();

            groupedFields.computeIfAbsent(sourceName, entitySource -> new ArrayList<>()).add(crmField);
        });

        return groupedFields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FieldInfo getField(final String sourceName, final Long fieldId) {
        final Collection<FieldInfo> fields = getDefaultFieldsBySource(List.of(sourceName));

        if (Objects.isNull(fields) || fields.isEmpty()) {
            throw ErrorCode.get(EntityErrorCodes.NO_FIELDS_REGISTERED_FOR_SOURCE, "Source : " + sourceName);
        } else {
            final Optional<FieldInfo> identifiedField = fields.stream().filter(field -> Objects.equals(field.id(), fieldId))
                    .findFirst();

            if (identifiedField.isPresent()) {
                return identifiedField.get();
            } else {
                throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_NOT_FOUND, "Field Id : " + fieldId);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceNames Represents the source name.
     * @return The collection of {@link FieldInfo}.
     */
    @Override
    public Collection<FieldInfo> getDefaultFieldsBySource(final Collection<String> sourceNames) {
        final List<FieldInfo> fields;

        if (LoggedInUserInfo.hasSecurityContext()) {
            final Collection<FieldInfo> sourceFields = getFieldsBySource(LoggedInUserInfo.getUser().orgId(), sourceNames);

            fields = sourceFields.stream().filter(fieldInfo -> !fieldInfo.isPersistent()).toList();
        } else {
            final Map<String, Collection<FieldInfo>> sourceFields = fieldResolver.getDefaultFieldsBySource(sourceNames);

            fields = sourceFields.values().stream().flatMap(Collection::stream).toList();
        }
        return sortFields(fields);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param fieldId Represents the field id.
     * @return The {@link FieldInfo}
     */
    @Override
    public FieldInfo getDefaultFieldById(final Long fieldId) {
        if (LoggedInUserInfo.hasSecurityContext()) {
            return getField(LoggedInUserInfo.getUser().orgId(), fieldId);
        }
        return fieldResolver.getDefaultFields(Set.of(fieldId)).get(fieldId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id
     * @param sourceName Represents the source name
     * @param aliasNames Represents the {@link Collection} of aliasNames
     * @return A map that holds the alias name as key and the respective field as value
     */
    @Override
    public Map<String, FieldInfo> groupDefaultFieldsByAliasName(final long orgId, final String sourceName, Collection<String> aliasNames) {
        final Collection<FieldInfo> fields = getFieldsBySource(orgId, sourceName);

        return fields.stream().filter(field -> {
            final String aliasName = field.aliasName();

            return Objects.nonNull(aliasName) && aliasNames.contains(aliasName);
        }).collect(Collectors.toUnmodifiableMap(FieldInfo::aliasName, Function.identity()));
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldIds Contains the collection of field ids
     * @return The collection of {@link FieldInfo}
     */
    @Override
    public Collection<FieldInfo> getDefaultFieldsByFieldId(final Collection<Long> fieldIds) {
        if (LoggedInUserInfo.hasSecurityContext()) {
            return getFieldsById(LoggedInUserInfo.getUser().orgId(), fieldIds).values();
        }
        return fieldResolver.getDefaultFields(fieldIds).values();
    }

    /**
     * {@inheritDoc}
     *
     * @param source    Represents the {@link Source}
     * @param aliasName Represents the alias names of entity
     * @return the {@link FieldInfo}
     */
    @Override
    public Optional<FieldInfo> getDefaultFieldByAliasName(final Source source, final String aliasName) {
        final Collection<FieldInfo> fields = getDefaultFieldsBySource(List.of(source.getName()));

        return fields.stream().filter(field -> Objects.equals(aliasName, field.aliasName())).findFirst();
    }

    /**
     * {@inheritDoc}
     *
     * @param source       Represents the {@link Source}.
     * @param internalName Represents the alias names of entity.
     * @return the {@link FieldInfo}.
     */
    @Override
    public Optional<FieldInfo> getDefaultFieldByInternalName(final Source source, final String internalName) {
        final Collection<FieldInfo> fields = getDefaultFieldsBySource(List.of(source.getName()));

        return fields.stream().filter(field -> Objects.equals(internalName, field.getInternalName())).findFirst();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organisation ID.
     * @param internalName Represents the internal names of entity.
     * @return the {@link FieldInfo}.
     */
    @Override
    public Optional<FieldInfo> getFieldByInternalName(final long orgId, final String internalName) {
        final Collection<FieldInfo> fields = getAllFields(orgId);

        return fields.stream().filter(field -> Objects.equals(internalName, field.getInternalName())).findFirst();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents the organisation ID.
     * @param internalNames Represents the internal names of entity.
     * @return the {@link FieldInfo}.
     */
    @Override
    public Collection<FieldInfo> getFieldsByInternalNames(final long orgId, final Collection<String> internalNames) {
        final Collection<FieldInfo> fields = getAllFields(orgId);
        final Collection<FieldInfo> matchedFields = new HashSet<>();

        for (final String internalName : internalNames) {
            fields.stream().filter(field -> Objects.equals(internalName, field.getInternalName()))
                    .findFirst().ifPresent(matchedFields::add);
        }

        return matchedFields;
    }

    /**
     * <p>
     * Sorts the given entity based on the CRM entity order and name
     * </p>
     *
     * @param fields Collection of field details
     * @return Sorted entity
     */
    private Collection<FieldInfo> sortFields(final List<FieldInfo> fields) {
        return fields.stream().filter(FieldInfo::hasPresetConfig)
                .sorted(Comparator.comparing(FieldInfo::presetConfig, Comparator.nullsLast(Comparator.comparing(PresetConfig::order))).thenComparing(fieldInfo ->
                        Objects.requireNonNullElse(fieldInfo.aliasName(), ""))).toList();
    }

    /**
     * Resolves the field configurations of the fields.
     *
     * @param orgId  Organization ID
     * @param fields which contains the collection of {@link FieldInfo}
     * @return Resolved {@link FieldInfo}.
     */
    private Map<Long, FieldInfo> resolveFieldConfigs(final Long orgId, final Collection<FieldInfo> fields) {
        final Map<Long, FieldInfo> fieldsMap = new HashMap<>();

        if (Objects.nonNull(fields) && !fields.isEmpty()) {

            if (Objects.nonNull(orgId)) {
                final Map<Long, FieldConfig> fieldConfigs = fieldResolver.getFieldConfigs(orgId);

                fields.forEach(fieldInfo -> {
                    final Long fieldId = fieldInfo.id();
                    final FieldConfig fieldConfig = Optional.ofNullable(fieldConfigs)
                            .map(configs -> configs.get(fieldId)).orElseGet(fieldInfo::fieldConfig);

                    if (Objects.nonNull(fieldConfig)) {
                        final JsonObject defaultPresetConfig = Json.object(fieldInfo.fieldConfig().getPresetConfig());
                        final JsonObject orgPresetConfig = Json.object(fieldConfig.getPresetConfig());

                        defaultPresetConfig.append(orgPresetConfig);
                        fieldsMap.put(fieldId, new FieldInfo(fieldInfo.id(), fieldInfo.idAsString(), fieldInfo.orgId(),
                                fieldInfo.columnIndex(), fieldInfo.tableIndex(), fieldInfo.isActive(), fieldInfo.i18nName(),
                                fieldInfo.aliasName(), fieldInfo.storeType(), fieldInfo.storageType(),
                                fieldInfo.fieldConfig(), defaultPresetConfig, fieldInfo.storeId(),
                                fieldInfo.sourceId(), fieldInfo.getPresetConfig(), fieldInfo.getMapperTable(),
                                fieldInfo.getDataTable(), fieldInfo.getPlaceHolder(), fieldInfo.getInternalName()));
                    }
                });
            }
        }

        return fieldsMap;
    }
}
