package com.tlc.crm.entity.service.dropdown.internal;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.entity.service.dropdown.DropdownOptionReferenceService;
import com.tlc.crm.entity.sql.resource.OMETENTITY;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Implements {@link DropdownOptionReferenceService} for mutable dropdown option removal.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 * @see DropdownOptionReferenceService
 */
public final class DropdownOptionReferenceServiceImpl implements DropdownOptionReferenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DropdownOptionReferenceServiceImpl.class);

    private final FieldService fieldService;
    private final OptionService optionService;

    private DropdownOptionReferenceServiceImpl() {

        fieldService = FieldService.getInstance();
        optionService = OptionService.getInstance();
    }

    /**
     * <p>
     * Gets the {@link DropdownOptionReferenceServiceImpl} instance.
     * </p>
     *
     * @return {@link DropdownOptionReferenceServiceImpl}.
     */
    public static DropdownOptionReferenceServiceImpl getInstance() {

        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldInfo {@link FieldInfo}.
     * @return {@code true} when the field is an active mutable {@link StoreType#DROPDOWN}; otherwise {@code false}.
     */
    @Override
    public boolean isReassignRequired(final FieldInfo fieldInfo) {

        if (Objects.isNull(fieldInfo) || Boolean.FALSE.equals(fieldInfo.isActive())) {
            return false;
        }

        if (StoreType.DROPDOWN != fieldInfo.storeType()) {
            return false;
        }

        final DropdownType dropdownType = fieldInfo.presetConfig().dropdownType();

        return Objects.nonNull(dropdownType) && dropdownType.isMutable();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId            Organization id.
     * @param fieldId          Field id.
     * @param removedOptionIds Collection of removed option ids.
     */
    @Override
    public void reassign(final long orgId, final long fieldId, final Collection<Long> removedOptionIds) {
        reassign(orgId, fieldId, removedOptionIds, null);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId            Organization id.
     * @param fieldId          Field id.
     * @param removedOptionIds Collection of removed option ids.
     * @param fieldConfigJson  Field config from the update request.
     */
    @Override
    public void reassign(final long orgId, final long fieldId, final Collection<Long> removedOptionIds,
                         final JsonObject fieldConfigJson) {
        if (Objects.isNull(removedOptionIds) || removedOptionIds.isEmpty()) {
            return;
        }
        final Set<Long> excludedOptionIds = new HashSet<>();

        for (final Long removedOptionId : removedOptionIds) {
            if (Objects.nonNull(removedOptionId)) {
                excludedOptionIds.add(removedOptionId);
            }
        }

        for (final Long removedOptionId : excludedOptionIds) {
            reassign(orgId, fieldId, removedOptionId, excludedOptionIds, fieldConfigJson);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Organization id.
     * @param fieldId         Field id.
     * @param removedOptionId Removed option id.
     */
    @Override
    public void reassign(final long orgId, final long fieldId, final long removedOptionId) {
        reassign(orgId, fieldId, removedOptionId, Set.of(removedOptionId), null);
    }

    /**
     * <p>
     * Reassigns references for a removed option, choosing a replacement from retained request options or from the database.
     * </p>
     *
     * @param orgId             Organization id.
     * @param fieldId           Field id.
     * @param removedOptionId   Removed option id.
     * @param excludedOptionIds Option ids that must not be used as the replacement value.
     * @param fieldConfigJson   Field config from the update request (optional).
     */
    private void reassign(final long orgId, final long fieldId, final long removedOptionId,
                          final Set<Long> excludedOptionIds, final JsonObject fieldConfigJson) {
        final FieldInfo fieldInfo = fieldService.getField(orgId, fieldId);

        if (!isReassignRequired(fieldInfo)) {
            return;
        }
        final Long replacementOptionId = resolveReplacementOptionId(orgId, fieldInfo, excludedOptionIds, fieldConfigJson);

        if (Objects.isNull(replacementOptionId)) {
            LOGGER.warn("No replacement option for fieldId={} after removing optionId={}", fieldId, removedOptionId);

            return;
        }
        updateReferences(orgId, fieldInfo, removedOptionId, replacementOptionId);
    }

    /**
     * <p>
     * Resolves the default option, or the first remaining option by order, excluding options that are being removed.
     * </p>
     *
     * @param orgId             Organization id.
     * @param fieldInfo         {@link FieldInfo}.
     * @param excludedOptionIds Option ids to exclude from consideration.
     * @return Replacement {@link Option}, or {@code null} if none remain.
     */
    private Option resolveReplacementOption(final long orgId, final FieldInfo fieldInfo,
                                            final Set<Long> excludedOptionIds) {
        final Collection<Option> options = optionService.get(orgId, fieldInfo);

        if (Objects.isNull(options) || options.isEmpty()) {
            return null;
        }
        Option first = null;

        for (final Option option : options) {

            if (Objects.isNull(option.id()) || excludedOptionIds.contains(option.id())) {
                continue;
            }

            if (Objects.isNull(first)) {
                first = option;
            }

            if (option.isDefault()) {
                return option;
            }
        }

        return first;
    }

    /**
     * <p>
     * Resolves a replacement option id from the update request config, or from persisted options when config is absent.
     * </p>
     *
     * @param orgId             Organization id.
     * @param fieldInfo         {@link FieldInfo}.
     * @param excludedOptionIds Removed option ids.
     * @param fieldConfigJson   Update request config.
     * @return Replacement option id, or {@code null}.
     */
    private Long resolveReplacementOptionId(final long orgId, final FieldInfo fieldInfo, final Set<Long> excludedOptionIds,
                                            final JsonObject fieldConfigJson) {
        final String optionsKey = FieldKeys.OPTIONS.getName();
        final String valueKey = FieldKeys.VALUE.getName();
        final String defaultKey = FieldKeys.IS_DEFAULT.getName();

        if (Objects.nonNull(fieldConfigJson) && fieldConfigJson.containsKey(optionsKey)) {
            final JsonArray optionArray = fieldConfigJson.optJsonArray(optionsKey);

            if (Objects.nonNull(optionArray)) {
                Long firstRetainedId = null;
                final int optionSize = optionArray.size();

                for (int count = NumericalConstant.ZERO; count < optionSize; count++) {
                    final JsonObject optionJson = optionArray.getJsonObject(count);

                    final Long optionId = extractOptionId(optionJson, valueKey);

                    if (Objects.isNull(optionId)) {
                        continue;
                    }

                    if (excludedOptionIds.contains(optionId)) {
                        continue;
                    }

                    if (Objects.isNull(firstRetainedId)) {
                        firstRetainedId = optionId;
                    }

                    if (optionJson.optBoolean(defaultKey)) {
                        return optionId;
                    }
                }

                if (Objects.nonNull(firstRetainedId)) {
                    return firstRetainedId;
                }
            }
        }
        final Option replacement = resolveReplacementOption(orgId, fieldInfo, excludedOptionIds);

        return Objects.nonNull(replacement) ? replacement.id() : null;
    }

    /**
     * <p>
     * Updates entity field values that reference {@code removedOptionId} to use {@code replacementOptionId}.
     * </p>
     *
     * @param orgId               Organization id.
     * @param fieldInfo           {@link FieldInfo} for the dropdown field.
     * @param removedOptionId     Option id being removed.
     * @param replacementOptionId Option id to assign as the new value.
     */
    private void updateReferences(final long orgId, final FieldInfo fieldInfo, final long removedOptionId,
                                  final long replacementOptionId) {

        if (removedOptionId == replacementOptionId) {
            return;
        }

        if (fieldInfo.isPersistent()) {
            updatePersistentColumn(orgId, fieldInfo, removedOptionId, replacementOptionId);
        } else if (Objects.nonNull(fieldInfo.getMapperTable()) || fieldInfo.presetConfig().hasMapperTable()) {
            applyMapperReassignment(orgId, fieldInfo, removedOptionId, replacementOptionId);
        } else {
            LOGGER.warn("Skipping dropdown reassignment for transient field without mapper table: fieldId={}", fieldInfo.id());
        }
    }

    /**
     * <p>
     * Reassigns references in a persistent custom-field column.
     * </p>
     *
     * @param orgId               Organization id.
     * @param fieldInfo           {@link FieldInfo}.
     * @param removedOptionId     Removed option id.
     * @param replacementOptionId Replacement option id.
     */
    private void updatePersistentColumn(final long orgId, final FieldInfo fieldInfo, final long removedOptionId,
                                        final long replacementOptionId) {
        final Table table = fieldInfo.table();
        final Column valueColumn = fieldInfo.column();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(valueColumn, replacementOptionId);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(valueColumn, removedOptionId)));
        getOrgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Reassigns references in a transient system-field mapper table, per entity row, avoiding unique-key conflicts.
     * </p>
     *
     * @param orgId               Organization id.
     * @param fieldInfo           {@link FieldInfo}.
     * @param removedOptionId     Removed option id.
     * @param replacementOptionId Replacement option id.
     */
    private void applyMapperReassignment(final long orgId, final FieldInfo fieldInfo, final long removedOptionId,
                                         final long replacementOptionId) {
        final String mapperTableName = Objects.requireNonNullElse(fieldInfo.getMapperTable(), fieldInfo.table().getName());
        final Table mapperTable = Table.get(mapperTableName);
        final Column valueColumn = fieldInfo.column();
        final Column fieldIdColumn = mapperTable.getColumn(OMETENTITY.FIELD_ID);
        final Column entityIdColumn = mapperTable.getColumn(OMETENTITY.ENTITY_ID);
        final SelectQuery selectQuery = SelectQuery.get(mapperTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(fieldIdColumn, fieldInfo.id()))
                .and(Criteria.eq(valueColumn, removedOptionId)));
        final OrgDataStore orgDataStore = getOrgDataStore(orgId);
        final DataContainer sourceContainer = orgDataStore.get(selectQuery);
        final List<Row> affectedRows = sourceContainer.getRows(mapperTable).toList();

        if (affectedRows.isEmpty()) {
            LOGGER.warn("No mapper rows to reassign before option delete: table={}, fieldId={}, removedOptionId={}",
                    mapperTableName, fieldInfo.id(), removedOptionId);

            return;
        }
        final DataContainer dataContainer = DataContainer.create();

        for (final Row row : affectedRows) {
            final Object entityId = row.get(entityIdColumn);
            final boolean replacementExists = isMapperRowExists(orgDataStore, mapperTable, fieldInfo.id(), entityId,
                    replacementOptionId, fieldIdColumn, entityIdColumn, valueColumn);

            if (replacementExists) {
                dataContainer.deleteRow(row);
            } else {
                row.set(valueColumn, replacementOptionId);
                dataContainer.updateRow(row);
            }
        }

        if (!dataContainer.isEmpty()) {
            orgDataStore.commitChanges(dataContainer);
        }
    }

    /**
     * <p>
     * Checks whether a mapper row already exists for the entity and replacement option.
     * </p>
     */
    private boolean isMapperRowExists(final OrgDataStore orgDataStore, final Table mapperTable, final long fieldId,
                                      final Object entityId, final long replacementOptionId, final Column fieldIdColumn,
                                      final Column entityIdColumn, final Column valueColumn) {
        final SelectQuery existsQuery = SelectQuery.get(mapperTable);

        existsQuery.setWhereClause(new WhereClause(Criteria.eq(fieldIdColumn, fieldId)).and(
                Criteria.eq(entityIdColumn, entityId)).and(Criteria.eq(valueColumn, replacementOptionId)));

        return orgDataStore.get(existsQuery).getRows(mapperTable).findAny().isPresent();
    }

    /**
     * <p>
     * Gets the organization-specific data store.
     * </p>
     *
     * @param orgId Organization id.
     * @return {@link OrgDataStore}.
     */
    private OrgDataStore getOrgDataStore(final long orgId) {

        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Reads a persisted option id from request option JSON ({@code value} or {@code id}).
     * </p>
     */
    private Long extractOptionId(final JsonObject optionJson, final String valueKey) {

        if (optionJson.containsKey(valueKey)) {
            return optionJson.getLong(valueKey);
        }

        if (optionJson.containsKey(OptionKeys.ID)) {
            return optionJson.getLong(OptionKeys.ID);
        }

        return null;
    }

    private static final class Instance {

        private static final DropdownOptionReferenceServiceImpl INSTANCE = new DropdownOptionReferenceServiceImpl();
    }
}