package com.tlc.crm.core.features.internal.dataimport;

import com.tlc.crm.core.features.field.dataimport.ImportFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.field.product.ProductFields;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.StoreType;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides the service for the import fields
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class ImportFieldService {

    private final FieldService fieldService;
    private final Collection<String> activityImportableFieldNames;
    private final Collection<String> noteImportableFieldNames;
    private final Collection<String> entityImportableReadOnlyFieldNames;
    private final Predicate<FieldInfo> importableFieldPredicate;

    private ImportFieldService() {
        this.fieldService = FieldService.getInstance();
        this.noteImportableFieldNames = Set.of(NoteFields.NOTE.getName(), NoteFields.CREATED_TIME.getName());
        this.entityImportableReadOnlyFieldNames = Set.of(ImportFields.ID.getName(), ImportFields.CREATED_AT.getName(),
                DealFields.DEAL_CREATED, ProductFields.CREATED_ON);
        this.activityImportableFieldNames = Set.of(ActivityFields.TITLE.getName(),
                ActivityFields.AVAILABILITY.getName(), ActivityFields.START_TIME.getName(),
                ActivityFields.END_TIME.getName(), ActivityFields.DESCRIPTION.getName(), ActivityFields.TYPE_ID.getName(),
                ActivityFields.NOTE.getName(), ActivityFields.ASSIGNEE_ID.getName(), ActivityFields.IS_DONE.getName(),
                ActivityFields.PRIORITY.getName(), ActivityFields.REMINDER.getName(), ActivityFields.COLLABORATORS.getName(),
                ActivityFields.CREATED_AT.getName());
        this.importableFieldPredicate = fieldInfo -> {
            final String aliasName = fieldInfo.aliasName();

            return switch (Source.get(fieldInfo.fieldSource().getName())) {
                case ACTIVITY -> activityImportableFieldNames.contains(aliasName);
                case NOTE -> noteImportableFieldNames.contains(aliasName);
                default -> !fieldInfo.presetConfig().readOnly() || entityImportableReadOnlyFieldNames.contains(aliasName);
            };
        };
    }

    private static final class Instance {
        private static final ImportFieldService IMPORT_FIELD_SERVICE = new ImportFieldService();
    }

    /**
     * <p>
     * Gets the instance of {@link ImportFieldService}
     * </p>
     *
     * @return {@link ImportFieldService} instance
     */
    public static ImportFieldService getInstance() {
        return Instance.IMPORT_FIELD_SERVICE;
    }

    /**
     * <p>
     * Gets the optional field by the provided source and field id
     * </p>
     *
     * @param orgId   Represents the organization id.
     * @param fieldId Represents the field id
     * @param source  Represents the source
     * @return The optional of {@link FieldInfo}
     */
    public Optional<FieldInfo> getField(final long orgId, final Source source, final Long fieldId) {
        return fieldService.getFieldsBySource(orgId, source.getName())
                .stream()
                .filter(fieldInfo -> Objects.equals(fieldInfo.id(), fieldId))
                .findFirst();
    }

    /**
     * <p>
     * Gets the fields grouped by the sources.
     * </p>
     *
     * @param orgId   Represents the organization id.
     * @param sources Represents the sources
     * @return the grouped fields
     */
    public Map<Source, Collection<FieldInfo>> groupAllFieldsBySources(final long orgId, final Collection<Source> sources) {
        final Map<Source, Collection<FieldInfo>> sourceFieldMap = new EnumMap<>(Source.class);

        for (final Source source : sources) {
            sourceFieldMap.put(source, getImportableSourceFields(orgId, source));
        }

        return sourceFieldMap;
    }

    /**
     * <p>
     * Gets the collection of importable source fields
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param source Represents the source
     * @return the collection of {@link FieldInfo}
     */
    public Collection<FieldInfo> getImportableSourceFields(final long orgId, final Source source) {
        return fieldService.getFieldsBySource(orgId, source.getName()).stream().filter(importableFieldPredicate).toList();
    }

    /**
     * <p>
     * Gets the fields grouped by the store type
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param source Represents the source
     * @return the fields grouped by store type
     */
    public Map<StoreType, List<FieldInfo>> groupFieldsByStoreType(final long orgId, final Source source) {
        final Collection<FieldInfo> sourceFields = getImportableSourceFields(orgId, source);

        return sourceFields.isEmpty() ? Collections.emptyMap()
                : sourceFields.stream().collect(Collectors.groupingBy(FieldInfo::storeType));
    }

    /**
     * <p>
     * Fetches the fields grouped by field type
     * </p>
     *
     * @param orgId         Represents the organization id
     * @param entitySources Represents the source names
     * @return A map that holds the field type as key and the value as field info collection
     */
    public Map<Integer, Collection<FieldInfo>> groupAllFieldsByType(final Long orgId, final String... entitySources) {
        return fieldService.groupAllFieldsByType(fieldService.getFieldsBySource(orgId, entitySources).stream().filter(importableFieldPredicate).toList());
    }

    /**
     * <p>
     * Gets the default fields grouped by the alias names
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param source Represents the source
     * @return the collection of {@link FieldInfo}
     */
    public Map<String, FieldInfo> groupDefaultFieldsByAliasName(final long orgId, final Source source) {
        final Collection<FieldInfo> importFields = getImportableSourceFields(orgId, source);

        if (importFields.isEmpty()) {
            return Collections.emptyMap();
        }
        final Stream<FieldInfo> importFieldStream;

        if (Source.isPrimarySource(source)) {
            importFieldStream = importFields.stream().filter(importField -> {
                final PresetConfig config = importField.presetConfig();

                return Objects.nonNull(config) && config.isDefaultField();
            });
        } else {
            importFieldStream = importFields.stream();
        }
        return importFieldStream.collect(Collectors.toMap(FieldInfo::aliasName, Function.identity()));
    }
}