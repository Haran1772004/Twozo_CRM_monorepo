package com.tlc.crm.core.features.api.merge;

import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.action.service.EmailTransferActionService;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealStatistics;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;
import com.tlc.crm.core.features.api.file.manager.FileHolderManager;
import com.tlc.crm.core.features.api.followers.source.manager.SourceFollowersManager;
import com.tlc.crm.core.features.api.merge.service.EntityMergeCallbackService;
import com.tlc.crm.core.features.api.merge.service.EntityMergeCallbackType;
import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.api.tag.SourceTagManager;
import com.tlc.crm.core.sql.resource.OMNTCONTACTNOTE;
import com.tlc.crm.core.sql.resource.OMNTCOMPANYNOTE;
import com.tlc.crm.core.sql.resource.OMNTDEALNOTE;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMFICONTACTFILE;
import com.tlc.crm.core.sql.resource.OMFICOMPANYFILE;
import com.tlc.crm.core.sql.resource.OMFIDEALFILE;
import com.tlc.crm.core.sql.resource.MTCONTACTTAG;
import com.tlc.crm.core.sql.resource.MTCOMPANYTAG;
import com.tlc.crm.core.sql.resource.MTDEALTAG;
import com.tlc.crm.core.sql.resource.OMCONFOLLOWERS;
import com.tlc.crm.core.sql.resource.OMCOMFOLLOWERS;
import com.tlc.crm.core.sql.resource.OMDOFOLLOWERS;
import com.tlc.crm.core.sql.resource.MACCONTACTACTIVITY;
import com.tlc.crm.core.sql.resource.MACCOMPANYACTIVITY;
import com.tlc.crm.core.sql.resource.MACDEALACTIVITY;
import com.tlc.crm.entity.manager.field.type.AddressManager;
import com.tlc.crm.entity.manager.field.type.DateRangeManager;
import com.tlc.crm.entity.manager.field.type.DateTimeRangeManager;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.manager.field.type.OptionManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.manager.field.type.TimeRangeManager;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.sql.resource.OMETDISMISSFIELDS;
import com.tlc.crm.entity.sql.resource.OMETENTITY;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Abstract base class for entity-level merge managers.
 * Contains all merge-related DB operations previously split between DuplicateManager and EntityMergeManager,
 * covering activity, note, file, followers, tags, email transfer, field rows, and timeline merge actions
 * that are shared across Contact, Company and Deal merge flows.
 * </p>
 *
 * @version 1.0
 */
public abstract class EntityMergeManager {

    private static final String OPTION_ID = "OPTION_ID";

    /**
     * <p>
     * Holds table and owner-column details for direct mapping row transfers.
     * </p>
     *
     * @param table           Represents the {@link Table}.
     * @param ownerColumnName Represents the owner column name.
     */
    public record MapperTableTransferSpec(Table table, String ownerColumnName) {
    }

    /**
     * <p>
     * Holds details for dedupe-safe mapping row transfers.
     * </p>
     *
     * @param table            Represents the {@link Table}.
     * @param ownerColumnName  Represents the owner column name.
     * @param uniqueColumnName Represents the unique column name.
     */
    public record MapperTableDedupedTransferSpec(Table table, String ownerColumnName, String uniqueColumnName) {
    }

    protected final SourceActivityManager sourceActivityManager;
    protected final SourceNoteManager sourceNoteManager;
    protected final FileHolderManager fileHolderManager;
    protected final SourceFollowersManager sourceFollowersManager;
    protected final SourceTagManager sourceTagManager;
    protected final EmailTransferActionService emailTransferActionService;
    protected final CommonTimelineManager commonTimelineManager;
    protected final UserService userService;

    private final FieldService fieldService;
    private final AddressManager addressManager;
    private final MonetaryManager monetaryManager;
    private final EmailManager emailManager;
    private final PhoneManager phoneManager;
    private final OptionManager optionManager;
    private final TimeRangeManager timeRangeManager;
    private final DateRangeManager dateRangeManager;
    private final DateTimeRangeManager dateTimeRangeManager;

    protected EntityMergeManager() {
        this.sourceActivityManager = SourceActivityManager.getInstance();
        this.sourceNoteManager = SourceNoteManager.getInstance();
        this.fileHolderManager = FileHolderManager.getInstance();
        this.sourceFollowersManager = SourceFollowersManager.getInstance();
        this.sourceTagManager = SourceTagManager.getInstance();
        this.emailTransferActionService = EmailTransferActionService.getInstance();
        this.commonTimelineManager = CommonTimelineManager.getInstance();
        this.userService = UserService.getInstance();
        this.fieldService = FieldService.getSecureInstance();
        this.addressManager = AddressManager.getInstance();
        this.monetaryManager = MonetaryManager.getInstance();
        this.emailManager = EmailManager.getInstance();
        this.phoneManager = PhoneManager.getInstance();
        this.optionManager = OptionManager.getInstance();
        this.timeRangeManager = TimeRangeManager.getInstance();
        this.dateRangeManager = DateRangeManager.getInstance();
        this.dateTimeRangeManager = DateTimeRangeManager.getInstance();
    }

    /**
     * <p>
     * Before merging need to update all duplicate ids default field rows with primary id row.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param crmField        Represents a data carrier that holds field details.
     * @param primaryId       Represents the primary id.
     * @param duplicateIds    {@link Collection} of duplicate ids.
     * @param dataContainer   A container that holds the user defined field values and system values.
     * @param targetContainer Holds the updated information of user defined and system field values.
     */
    protected void loadDefaultFieldRowsIntoContainer(final Long orgId, final FieldInfo crmField, final Long primaryId,
                                                     final Collection<Long> duplicateIds,
                                                     final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table fieldValueTable = crmField.table();
        final Long fieldId = crmField.id();
        final Column entityIdColumn = fieldValueTable.getColumn(OMETENTITY.ENTITY_ID);
        final Column fieldIdColumn = fieldValueTable.getColumn(OMETENTITY.FIELD_ID);
        final Row primaryFieldRow = dataContainer.getRow(fieldValueTable,
                new WhereClause(Criteria.eq(entityIdColumn, primaryId)).and(Criteria.eq(fieldIdColumn, fieldId)));

        if (Objects.isNull(primaryFieldRow)) {
            for (final Long duplicateId : duplicateIds) {
                final Row secondaryFieldRow = dataContainer.getRow(fieldValueTable,
                        new WhereClause(Criteria.eq(entityIdColumn, duplicateId)).and(Criteria.eq(fieldIdColumn, fieldId)));

                if (Objects.nonNull(secondaryFieldRow)) {
                    secondaryFieldRow.set(entityIdColumn, primaryId);
                    targetContainer.updateRow(secondaryFieldRow);
                    break;
                }
            }
        }

        final Stream<Row> rows = dataContainer.getRows(fieldValueTable,
                new WhereClause(Criteria.in(entityIdColumn, duplicateIds))
                        .and(new WhereClause(Criteria.eq(fieldIdColumn, fieldId))));

        loadMapperTableDeletableRows(orgId, crmField.storeType(), getStoreTypeValueColumnName(crmField.storeType()), rows, targetContainer);
    }

    /**
     * <p>
     * Before merging need to update all duplicate ids custom field rows with primary id row.
     * </p>
     *
     * @param entity          {@link Entity}.
     * @param duplicateIds    {@link Collection} of duplicate ids.
     * @param dataContainer   A container that holds the user defined field values and system values.
     * @param targetContainer Holds the updated information of user defined and system field values.
     */
    protected void loadCustomFieldRowsIntoContainer(final Entity entity, final Collection<Long> duplicateIds,
                                                    final DataContainer dataContainer, final DataContainer targetContainer, final String entityType) {
        final Long primaryId = entity.id();
        final Long orgId = entity.orgId();
        final ValueHolder valueHolder = new ValueHolder();
        final Collection<FieldInfo> fields = fieldService.getOrgFieldsBySource(orgId).get(entity.getEntityType());

        if (Objects.nonNull(fields)) {
            for (final FieldInfo crmField : fields) {
                if (crmField.isActive()) {
                    final Table fieldValueTable = crmField.table();
                    final Column fieldColumn = crmField.column();
                    final Long fieldId = crmField.id();
                    final Column entityIdColumn = fieldValueTable.getColumn(OMETENTITY.ENTITY_ID);
                    final StoreType storeType = crmField.storeType();
                    final Row primaryFieldRow = dataContainer.getRow(fieldValueTable, new WhereClause(Criteria.eq(entityIdColumn, primaryId)));

                    if (StoreType.isSingleValueStore(storeType)) {

                        if (Objects.nonNull(primaryFieldRow)) {
                            final Object primaryValue = primaryFieldRow.get(fieldColumn);

                            if (Objects.nonNull(primaryValue)) {
                                valueHolder.put(fieldId, primaryValue);
                            }
                        }

                        if (Objects.isNull(valueHolder.get(fieldId))) {

                            for (final Long duplicateId : duplicateIds) {
                                final Row secondaryFieldRow = dataContainer.getRow(fieldValueTable, new WhereClause(Criteria.eq(entityIdColumn, duplicateId)));

                                if (Objects.nonNull(secondaryFieldRow)) {
                                    final Object secondaryValue = secondaryFieldRow.get(fieldColumn);

                                    if (Objects.nonNull(secondaryValue)) {
                                        valueHolder.put(fieldId, secondaryValue);

                                        if (Objects.nonNull(primaryFieldRow)) {
                                            primaryFieldRow.set(fieldColumn, secondaryValue);
                                            targetContainer.updateRow(primaryFieldRow);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (StoreType.isMultiValueStore(storeType)) {

                        if (Objects.nonNull(primaryFieldRow)) {
                            final Object primaryValue = primaryFieldRow.get(fieldColumn);

                            if (Objects.nonNull(primaryValue)) {
                                valueHolder.put(fieldId, primaryValue);
                            }
                        }

                        if (Objects.isNull(valueHolder.get(fieldId))) {

                            for (final Long duplicateId : duplicateIds) {
                                final Row secondaryFieldRow = dataContainer.getRow(fieldValueTable, new WhereClause(Criteria.eq(entityIdColumn, duplicateId)));

                                if (Objects.nonNull(secondaryFieldRow)) {
                                    final Object secondaryValue = secondaryFieldRow.get(fieldColumn);

                                    if (Objects.nonNull(secondaryValue)) {
                                        valueHolder.put(fieldId, secondaryValue);

                                        if (Objects.nonNull(primaryFieldRow)) {
                                            primaryFieldRow.set(fieldColumn, secondaryValue);
                                            targetContainer.updateRow(primaryFieldRow);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (StoreType.isModelStore(storeType)) {

                        if (Objects.nonNull(primaryFieldRow)) {
                            final Object primaryValue = primaryFieldRow.get(fieldColumn);

                            if (Objects.nonNull(primaryValue)) {
                                valueHolder.put(fieldId, primaryValue);
                            }
                        }

                        if (Objects.isNull(valueHolder.get(fieldId))) {

                            for (final Long duplicateId : duplicateIds) {
                                final Row secondaryFieldRow = dataContainer.getRow(fieldValueTable, new WhereClause(Criteria.eq(entityIdColumn, duplicateId)));

                                if (Objects.nonNull(secondaryFieldRow)) {
                                    final Object secondaryValue = secondaryFieldRow.get(fieldColumn);

                                    if (Objects.nonNull(secondaryValue)) {
                                        valueHolder.put(fieldId, secondaryValue);

                                        if (Objects.nonNull(primaryFieldRow)) {
                                            primaryFieldRow.set(fieldColumn, secondaryValue);
                                            targetContainer.updateRow(primaryFieldRow);
                                        }
                                        break;
                                    }
                                }
                            }
                        } else {
                            deleteDuplicateCustomFieldRows(orgId, duplicateIds, crmField, dataContainer, targetContainer);
                        }
                    }
                }
            }
        }
        entity.setValueHolder(valueHolder);
    }

    /**
     * <p>
     * Loads mapping rows for multiple tables into target container by reassigning owner id to primary.
     * </p>
     *
     * @param orgId           Represents an organization id.
     * @param primaryId       Represents the primary id.
     * @param secondaryIds    Represents the duplicate/secondary ids.
     * @param transferSpecs   Represents the transfer specs.
     * @param targetContainer Holds the updated information of user defined and system field values.
     */
    protected void loadMappingTableRowsIntoContainer(final Long orgId, final Long primaryId, final Collection<Long> secondaryIds,
                                                     final Collection<MapperTableTransferSpec> transferSpecs,
                                                     final DataContainer targetContainer) {
        for (final MapperTableTransferSpec transferSpec : transferSpecs) {
            loadMappingTableRowsIntoContainer(orgId, primaryId, secondaryIds, transferSpec.ownerColumnName(), transferSpec.table(), targetContainer);
        }
    }

    /**
     * <p>
     * Loads update-only dedupe mapping rows from secondary ids to primary id.
     * </p>
     *
     * @param orgId            Represents an organization id.
     * @param primaryId        Represents the primary id.
     * @param secondaryIds     Represents the duplicate/secondary ids.
     * @param ownerColumnName  Represents the owner column name.
     * @param uniqueColumnName Represents the unique child column name.
     * @param table            Represents the table.
     * @param targetContainer  Holds the updated information of user defined and system field values.
     */
    protected void loadDeduplicatedMappingRowsIntoContainer(final Long orgId, final Long primaryId, final Collection<Long> secondaryIds,
                                                            final String ownerColumnName, final String uniqueColumnName,
                                                            final Table table, final DataContainer targetContainer) {
        final Column ownerColumn = table.getColumn(ownerColumnName);
        final Column uniqueColumn = table.getColumn(uniqueColumnName);
        final Set<Long> primaryUniqueIds = orgDataStore(orgId)
                .get(table, new WhereClause(Criteria.eq(ownerColumn, primaryId)), Set.of(uniqueColumn))
                .getRows(table)
                .map(row -> (Long) row.get(uniqueColumnName))
                .collect(Collectors.toSet());

        orgDataStore(orgId).get(table, new WhereClause(Criteria.in(ownerColumn, secondaryIds))).getRows(table).forEach(row -> {
            final Long uniqueValue = row.get(uniqueColumnName);

            if (!primaryUniqueIds.contains(uniqueValue)) {
                row.set(ownerColumnName, primaryId);
                targetContainer.updateRow(row);
                primaryUniqueIds.add(uniqueValue);
            }
        });
    }

    /**
     * <p>
     * Deletes the mapping table row based on store type.
     * </p>
     *
     * @param orgId                    Represents the organization id.
     * @param storeType                Represents the field store type.
     * @param storeTypeValueColumnName Represents the value column name in the store type table.
     * @param rows                     Represents the rows to process.
     * @param targetContainer          Holds the updated rows to be committed.
     */
    protected void loadMapperTableDeletableRows(final Long orgId, final StoreType storeType, final String storeTypeValueColumnName,
                                                final Stream<Row> rows, final DataContainer targetContainer) {

        if (!storeType.isEntityType()) {
            final OrgDataStore orgDataStore = orgDataStore(orgId);
            final Collection<Long> ids = rows.map(row -> (Long) row.get(storeTypeValueColumnName)).toList();
            final Table table = switch (storeType) {
                case ADDRESS -> addressManager.getTable();
                case MONETARY -> monetaryManager.getTable();
                case EMAIL, EMAILS -> emailManager.getTable();
                case PHONE, PHONES -> phoneManager.getTable();
                default -> null;
            };

            if (Objects.nonNull(table)) {
                ids.forEach(id -> targetContainer.deleteRow(orgDataStore.get(table, id)));
            }
        }
    }

    /**
     * <p>
     * Resolves the value-id column name for the given {@link StoreType}.
     * </p>
     *
     * @param storeType Represents the field store type.
     * @return the mapped value-id column name.
     */
    protected String getStoreTypeValueColumnName(final StoreType storeType) {
        return switch (storeType) {
            case ADDRESS -> "ADDRESS_ID";
            case MONETARY -> "MONETARY_ID";
            case EMAIL, EMAILS -> "EMAIL_ID";
            case PHONE, PHONES -> "PHONE_ID";
            default -> null;
        };
    }


    /**
     * <p>
     * Deletes the custom field wider table rows for duplicate entities.
     * Persistent custom fields are stored in a wider table with one row per entity.
     * When duplicates are merged, the duplicate entity wide table rows must be deleted.
     * Also handles deletion of FK-referenced rows (address, monetary, email, phone) for model-store fields.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param duplicateIds    The duplicate entity ids.
     * @param dataContainer   The data container holding the loaded wide table rows.
     * @param targetContainer The target container to mark rows for deletion.
     */
    protected void deleteDuplicateCustomFieldRows(final Long orgId, final Collection<Long> duplicateIds,
            final FieldInfo crmField, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table fieldValueTable = crmField.table();
        final Column entityIdColumn = fieldValueTable.getColumn(OMETENTITY.ENTITY_ID);

        for (final Long duplicateId : duplicateIds) {
            final Row duplicateRow = dataContainer.getRow(fieldValueTable, new WhereClause(Criteria.eq(entityIdColumn, duplicateId)));

            if (Objects.nonNull(duplicateRow)) {
                deleteModelStoreFkReferencesFromRow(orgId, duplicateRow, crmField, targetContainer);
                targetContainer.deleteRow(duplicateRow);
            }
        }
    }

    /**
     * <p>
     * Deletes FK-referenced rows (address, monetary, email, phone) from a wider table row
     * for all model-store persistent fields.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param row             The wider table row to process.
     * @param targetContainer The target container to mark rows for deletion.
     */
    private void deleteModelStoreFkReferencesFromRow(final Long orgId, final Row row, final FieldInfo crmField,
            final DataContainer targetContainer) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);

        if (StoreType.isModelStore(crmField.storeType()) && !crmField.storeType().isEntityType()) {
            final Table fieldValueTable = crmField.table();
            final Object fkValue = row.get(fieldValueTable.getColumn(crmField.columnName()));

            if (fkValue instanceof Long fkId) {

                final Table fkTable = switch (crmField.storeType()) {
                    case ADDRESS -> addressManager.getTable();
                    case MONETARY -> monetaryManager.getTable();
                    case EMAIL -> emailManager.getTable();
                    case DATE_RANGE -> dateRangeManager.getTable();
                    case TIME_RANGE -> timeRangeManager.getTable();
                    case DATE_TIME_RANGE -> dateTimeRangeManager.getTable();
                    default -> null;
                };

                if (Objects.nonNull(fkTable)) {
                    final Row fkRow = orgDataStore.get(fkTable, fkId);

                    if (Objects.nonNull(fkRow)) {
                        targetContainer.deleteRow(fkRow);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Updates the dismissed values in a container for a given organization, field, and source.
     * </p>
     *
     * @param orgId    Represents an organization id.
     * @param fieldId  Represents the field id.
     * @param sourceId Represents the source id.
     * @param values   which holds the {@link Collection} of field values.
     */
    public final void updateDismissedValues(final long orgId, final Long fieldId, final Long sourceId,
                                            final Collection<String> values) {
        if (Objects.nonNull(values) && !values.isEmpty()) {
            final Table table = Table.get(OMETDISMISSFIELDS.TABLE);
            final DataContainer dataContainer = DataContainer.create();

            for (final String value : values) {
                final Row row = new Row(table);

                row.setOrgId(orgId);
                row.set(OMETDISMISSFIELDS.VALUE, value);
                row.set(OMETDISMISSFIELDS.FIELD_ID, fieldId);
                row.set(OMETDISMISSFIELDS.SOURCE_ID, sourceId);

                dataContainer.addNewRow(row);
            }
            orgDataStore(orgId).commitChanges(dataContainer);
        }
    }

    /**
     * <p>
     * Gets the dismissed field values for the given organization id.
     * </p>
     *
     * @param orgId    Represents an organization id.
     * @param sourceId Represents the source id.
     * @param fieldId  Represents the field id.
     * @return the dismissed field values.
     */
    protected final Collection<String> getDismissedValues(final long orgId, final long sourceId, final long fieldId) {
        final Table table = Table.get(OMETDISMISSFIELDS.TABLE);
        final Column fieldValueColumn = table.getColumn(OMETDISMISSFIELDS.VALUE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(fieldValueColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMETDISMISSFIELDS.SOURCE_ID), sourceId))
                .and(Criteria.eq(table.getColumn(OMETDISMISSFIELDS.FIELD_ID), fieldId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? Collections.emptySet() : dataContainer.getRows(table)
                .map(row -> (String) row.get(fieldValueColumn)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Loads multi-select fields into target {@link DataContainer}.
     * </p>
     *
     * @param entityId        Represents the entity id.
     * @param fieldId         Represents the field id.
     * @param dependsOn       Represents whether the field depends on another field or not.
     * @param duplicateIds    Represents the {@link Collection} of duplicate ids.
     * @param table           Represents the {@link Table}.
     * @param dataContainer   Holds the existing data for the respective duplicate and primary entity ids.
     * @param targetContainer Holds the updated information into {@link DataContainer}.
     */
    protected void loadMultiSelectFieldsIntoContainer(final Long entityId, final Long fieldId, final boolean dependsOn,
                                                      final Collection<Long> duplicateIds, final Table table,
                                                      final DataContainer dataContainer, final DataContainer targetContainer) {
        final Collection<Option> options = getOptions(duplicateIds, fieldId, table, dataContainer);

        loadMultiSelectRowsIntoContainer(options, entityId, fieldId, dependsOn, table, dataContainer, targetContainer);
    }

    /**
     * <p>
     * Before merging need to update all duplicate ids dropdown field rows with primary id row.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param fieldInfo       Represents a data carrier that holds field details.
     * @param notDependsOn    Represents whether the field does NOT depend on another field.
     * @param primaryId       Represents the primary id.
     * @param duplicateIds    {@link Collection} of duplicate ids.
     * @param dataContainer   A container that holds the user defined field values and system values.
     * @param targetContainer Holds the updated information of user defined and system field values.
     */
    protected void loadDropdownFieldRowsIntoContainer(final Long orgId, final FieldInfo fieldInfo, final boolean notDependsOn,
                                                      final Long primaryId, final Collection<Long> duplicateIds,
                                                      final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table fieldValueTable = fieldInfo.table();
        final Long fieldId = fieldInfo.id();
        final Column entityIdColumn = fieldValueTable.getColumn(OMETENTITY.ENTITY_ID);
        final Column fieldIdColumn = fieldValueTable.getColumn(OMETENTITY.FIELD_ID);
        final Criteria fieldIdCriteria = Criteria.eq(fieldIdColumn, fieldId);
        final Row primaryFieldRow = dataContainer.getRow(fieldValueTable,
                new WhereClause(Criteria.eq(entityIdColumn, primaryId)).and(fieldIdCriteria));

        if (notDependsOn && Objects.isNull(primaryFieldRow)) {
            for (final Long duplicateId : duplicateIds) {
                final Row secondaryFieldRow = dataContainer.getRow(fieldValueTable,
                        new WhereClause(Criteria.eq(entityIdColumn, duplicateId)).and(fieldIdCriteria));

                if (Objects.nonNull(secondaryFieldRow)) {
                    secondaryFieldRow.set(entityIdColumn, primaryId);
                    targetContainer.updateRow(secondaryFieldRow);
                    break;
                }
            }
        }

        final Stream<Row> rows = dataContainer.getRows(fieldValueTable,
                new WhereClause(Criteria.in(entityIdColumn, duplicateIds)).and(fieldIdCriteria));

        loadMapperTableDeletableRows(orgId, fieldInfo.storeType(), getStoreTypeValueColumnName(fieldInfo.storeType()), rows, targetContainer);
    }

    /**
     * <p>
     * Builds partial territory.
     * </p>
     *
     * @param territoryId Territory's id.
     * @return {@link Territory}.
     */
    protected Territory buildPartialTerritory(final Long territoryId) {
        if (Objects.nonNull(territoryId)) {
            final Territory territory = new Territory();

            territory.setId(territoryId);

            return territory;
        }
        return null;
    }

    /**
     * <p>
     * Collects createdBy/updatedBy/salesOwner ids for one-shot user hydration.
     * </p>
     *
     * @param createdById  Represents the created by user id.
     * @param updatedById  Represents the updated by user id.
     * @param salesOwnerId Represents the sales owner user id.
     * @return {@link Set} of user ids.
     */
    protected Set<Long> collectUserIds(final Long createdById, final Long updatedById, final Long salesOwnerId) {
        return new java.util.HashSet<>(List.of(createdById, updatedById, salesOwnerId));
    }

    /**
     * <p>
     * Fetches users in a single call and returns a map keyed by user id.
     * </p>
     *
     * @param orgId   Represents the organization id.
     * @param userIds Represents the {@link Collection} of user ids.
     * @return {@link Map} of user id to {@link CrmUser}.
     */
    protected Map<Long, CrmUser> getUsersById(final Long orgId, final Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userService.getUsers(orgId, userIds).stream()
                .collect(Collectors.toMap(CrmUser::id, user -> user, (first, second) -> first));
    }

    /**
     * <p>
     * Returns the primary value when available, otherwise falls back to the secondary value.
     * </p>
     *
     * @param primaryValue   Represents the primary value.
     * @param secondaryValue Represents the secondary/fallback value.
     * @param <T>            The type of the value.
     * @return the primary value if non-null, otherwise the secondary value.
     */
    protected <T> T preferPrimaryValue(final T primaryValue, final T secondaryValue) {
        return Objects.isNull(primaryValue) ? secondaryValue : primaryValue;
    }

    /**
     * <p>
     * Adds two nullable long values safely by treating null as 0.
     * </p>
     *
     * @param primaryValue   Represents the primary long value.
     * @param secondaryValue Represents the secondary long value.
     * @return the sum of both values.
     */
    protected long sumNullableLong(final Long primaryValue, final Long secondaryValue) {
        return Objects.requireNonNullElse(primaryValue, 0L) + Objects.requireNonNullElse(secondaryValue, 0L);
    }

    /**
     * <p>
     * Merges two collections and keeps only one element per key, preserving primary order and preference.
     * </p>
     *
     * @param primaryValues   Represents the primary collection.
     * @param secondaryValues Represents the secondary collection.
     * @param keyMapper       Represents the function to extract the unique key.
     * @param <T>             The type of elements in the collection.
     * @param <K>             The type of the key.
     * @return the merged collection with unique keys.
     */
    protected <T, K> Collection<T> mergeUniqueByKey(final Collection<T> primaryValues, final Collection<T> secondaryValues,
                                                     final Function<T, K> keyMapper) {
        if ((Objects.isNull(primaryValues) || primaryValues.isEmpty()) && (Objects.isNull(secondaryValues) || secondaryValues.isEmpty())) {
            return null;
        }

        if (Objects.isNull(primaryValues) || primaryValues.isEmpty()) {
            return new ArrayList<>(secondaryValues);
        }

        if (Objects.isNull(secondaryValues) || secondaryValues.isEmpty()) {
            return new ArrayList<>(primaryValues);
        }

        final Map<Object, T> mergedValues = new LinkedHashMap<>();

        primaryValues.forEach(value -> mergedValues.put(resolveMergeKey(value, keyMapper), value));
        secondaryValues.forEach(value -> mergedValues.putIfAbsent(resolveMergeKey(value, keyMapper), value));

        return new ArrayList<>(mergedValues.values());
    }

    /**
     * <p>
     * Resolves the merge key for a given value using the provided key mapper.
     * Returns the value itself as a fallback key when the mapped key is null.
     * </p>
     *
     * @param value     Represents the element from the collection.
     * @param keyMapper Represents the function to extract the unique key from the element.
     * @param <T>       The type of the element.
     * @param <K>       The type of the key.
     * @return the resolved key, or the value itself if the key is null.
     */
    private <T, K> Object resolveMergeKey(final T value, final Function<T, K> keyMapper) {
        final K key = keyMapper.apply(value);

        return Objects.nonNull(key) ? key : value;
    }

    /**
     * <p>
     * Copies missing custom field values from secondary entity to primary entity for preview responses.
     * </p>
     *
     * @param primaryEntity   Represents the primary entity.
     * @param secondaryEntity Represents the secondary entity.
     */
    protected void mergeCustomFields(final Entity primaryEntity, final Entity secondaryEntity) {
        final Collection<FieldInfo> fields = fieldService.getOrgFieldsBySource(primaryEntity.orgId()).get(primaryEntity.getEntityType());

        if (Objects.nonNull(fields)) {
            ValueHolder primaryValueHolder = primaryEntity.getValueHolder();

            if (Objects.isNull(primaryValueHolder)) {
                primaryValueHolder = new ValueHolder();
                primaryEntity.setValueHolder(primaryValueHolder);
            }

            final ValueHolder secondaryValueHolder = secondaryEntity.getValueHolder();

            if (Objects.nonNull(secondaryValueHolder)) {
                for (final FieldInfo crmField : fields) {
                    final Long fieldId = crmField.id();

                    if (Objects.isNull(primaryValueHolder.get(fieldId))) {
                        final Object secondaryValue = secondaryValueHolder.get(fieldId);

                        if (Objects.nonNull(secondaryValue)) {
                            primaryValueHolder.put(fieldId, secondaryValue);
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Merges summary information from secondary into primary for preview responses.
     * </p>
     *
     * @param primarySummary   Represents the primary {@link SummaryInfo}.
     * @param secondarySummary Represents the secondary {@link SummaryInfo}.
     * @return the merged {@link SummaryInfo}.
     */
    protected SummaryInfo mergeSummaryInfoForPreview(final SummaryInfo primarySummary, final SummaryInfo secondarySummary) {
        if (Objects.isNull(primarySummary)) {
            return secondarySummary;
        }

        if (Objects.isNull(secondarySummary)) {
            return primarySummary;
        }

        primarySummary.setWebFormName(preferPrimaryValue(primarySummary.getWebFormName(), secondarySummary.getWebFormName()));
        primarySummary.setTags(mergeUniqueByKey(primarySummary.getTags(), secondarySummary.getTags(), Tag::id));
        primarySummary.setEmailSummary(mergeEmailSummaryForPreview(primarySummary.getEmailSummary(), secondarySummary.getEmailSummary()));
        primarySummary.setActivitySummary(mergeActivitySummaryForPreview(primarySummary.getActivitySummary(), secondarySummary.getActivitySummary()));
        primarySummary.setDealStatistics(mergeDealStatisticsForPreview(primarySummary.getDealStatistics(), secondarySummary.getDealStatistics()));
        primarySummary.setDealsByState(mergeDealsByStateForPreview(primarySummary.getDealsByState(), secondarySummary.getDealsByState()));

        return primarySummary;
    }

    /**
     * <p>
     * Merges email summary data from the secondary entity into the primary entity for preview responses.
     * Prefers the primary value when available and sums the email message count from both sides.
     * </p>
     *
     * @param primaryEmailSummary   Represents the primary {@link EmailSummary}.
     * @param secondaryEmailSummary Represents the secondary {@link EmailSummary}.
     * @return the merged {@link EmailSummary}.
     */
    private EmailSummary mergeEmailSummaryForPreview(final EmailSummary primaryEmailSummary, final EmailSummary secondaryEmailSummary) {
        if (Objects.isNull(primaryEmailSummary)) {
            return secondaryEmailSummary;
        }

        if (Objects.isNull(secondaryEmailSummary)) {
            return primaryEmailSummary;
        }

        primaryEmailSummary.setLastEmailReceived(preferPrimaryValue(primaryEmailSummary.getLastEmailReceived(), secondaryEmailSummary.getLastEmailReceived()));
        primaryEmailSummary.setLastEmailSent(preferPrimaryValue(primaryEmailSummary.getLastEmailSent(), secondaryEmailSummary.getLastEmailSent()));
        primaryEmailSummary.setEmailMessageCount(sumNullableLong(primaryEmailSummary.getEmailMessageCount(), secondaryEmailSummary.getEmailMessageCount()));

        return primaryEmailSummary;
    }

    /**
     * <p>
     * Merges activity summary data from the secondary entity into the primary entity for preview responses.
     * Prefers primary date and type values and sums all activity counts from both sides.
     * </p>
     *
     * @param primaryActivitySummary   Represents the primary {@link ActivitySummary}.
     * @param secondaryActivitySummary Represents the secondary {@link ActivitySummary}.
     * @return the merged {@link ActivitySummary}.
     */
    private ActivitySummary mergeActivitySummaryForPreview(final ActivitySummary primaryActivitySummary, final ActivitySummary secondaryActivitySummary) {
        if (Objects.isNull(primaryActivitySummary)) {
            return secondaryActivitySummary;
        }

        if (Objects.isNull(secondaryActivitySummary)) {
            return primaryActivitySummary;
        }

        primaryActivitySummary.setNextActivityDate(preferPrimaryValue(primaryActivitySummary.getNextActivityDate(), secondaryActivitySummary.getNextActivityDate()));
        primaryActivitySummary.setLastActivityDate(preferPrimaryValue(primaryActivitySummary.getLastActivityDate(), secondaryActivitySummary.getLastActivityDate()));
        primaryActivitySummary.setLastActivityType(preferPrimaryValue(primaryActivitySummary.getLastActivityType(), secondaryActivitySummary.getLastActivityType()));
        primaryActivitySummary.setPendingActivities(sumNullableLong(primaryActivitySummary.getPendingActivities(), secondaryActivitySummary.getPendingActivities()));
        primaryActivitySummary.setUpcomingActivities(sumNullableLong(primaryActivitySummary.getUpcomingActivities(), secondaryActivitySummary.getUpcomingActivities()));
        primaryActivitySummary.setDoneActivities(sumNullableLong(primaryActivitySummary.getDoneActivities(), secondaryActivitySummary.getDoneActivities()));
        primaryActivitySummary.setTotalActivities(primaryActivitySummary.getDoneActivities() + primaryActivitySummary.getPendingActivities());

        return primaryActivitySummary;
    }

    /**
     * <p>
     * Merges deal statistics from the secondary entity into the primary entity for preview responses.
     * Sums open, won and lost deal counts from both sides.
     * </p>
     *
     * @param primaryStatistics   Represents the primary {@link DealStatistics}.
     * @param secondaryStatistics Represents the secondary {@link DealStatistics}.
     * @return the merged {@link DealStatistics}.
     */
    private DealStatistics mergeDealStatisticsForPreview(final DealStatistics primaryStatistics, final DealStatistics secondaryStatistics) {
        if (Objects.isNull(primaryStatistics)) {
            return secondaryStatistics;
        }

        if (Objects.isNull(secondaryStatistics)) {
            return primaryStatistics;
        }

        primaryStatistics.setOpenDealsCount(sumNullableLong(primaryStatistics.getOpenDealsCount(), secondaryStatistics.getOpenDealsCount()));
        primaryStatistics.setWonDealsCount(sumNullableLong(primaryStatistics.getWonDealsCount(), secondaryStatistics.getWonDealsCount()));
        primaryStatistics.setLostDealsCount(sumNullableLong(primaryStatistics.getLostDealsCount(), secondaryStatistics.getLostDealsCount()));

        return primaryStatistics;
    }

    /**
     * <p>
     * Creates and loads the merge timeline entry for the given entity merge.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param userId          Represents the acting user id.
     * @param category        Represents the timeline category.
     * @param duplicateIds    Represents the duplicate ids being merged.
     * @param primaryId       Represents the primary entity id.
     * @param fieldTimeline   Represents the merge field timeline payload.
     * @param dataContainer   Holds the existing information of timeline.
     * @param targetContainer Holds the updated information of timeline.
     */
    protected void loadMergeTimelineIntoContainer(final long orgId, final long userId, final Timeline.Category category,
                                                  final Collection<Long> duplicateIds, final Long primaryId,
                                                  final FieldTimeline fieldTimeline, final DataContainer dataContainer,
                                                  final DataContainer targetContainer) {
        final Timeline timeline = commonTimelineManager.getMergedTimeline(orgId, userId, category);

        timeline.setFieldTimeline(fieldTimeline);
        commonTimelineManager.loadMergeTimeline(timeline, duplicateIds, primaryId, dataContainer, targetContainer);
    }

    /**
     * <p>
     * Merges deals grouped by state from the secondary entity into the primary entity for preview responses.
     * Combines deals from both sides for each {@link Deal.State}, keeping only unique deals by id.
     * </p>
     *
     * @param primaryDealsByState   Represents the primary map of deals grouped by {@link Deal.State}.
     * @param secondaryDealsByState Represents the secondary map of deals grouped by {@link Deal.State}.
     * @return the merged map of deals grouped by {@link Deal.State}.
     */
    private Map<Deal.State, Collection<Deal>> mergeDealsByStateForPreview(final Map<Deal.State, Collection<Deal>> primaryDealsByState,
                                                                           final Map<Deal.State, Collection<Deal>> secondaryDealsByState) {
        if (Objects.isNull(primaryDealsByState)) {
            return secondaryDealsByState;
        }

        if (Objects.isNull(secondaryDealsByState)) {
            return primaryDealsByState;
        }

        final Map<Deal.State, Collection<Deal>> mergedDealsByState = new EnumMap<>(Deal.State.class);

        for (final Deal.State state : Deal.State.values()) {
            final Collection<Deal> mergedDeals = mergeUniqueByKey(primaryDealsByState.get(state), secondaryDealsByState.get(state), Deal::id);

            if (Objects.nonNull(mergedDeals) && !mergedDeals.isEmpty()) {
                mergedDealsByState.put(state, mergedDeals);
            }
        }

        return mergedDealsByState;
    }

    /**
     * <p>
     * Loads the standard merge actions for the given entity source into the target container.
     * This centralizes activity, note, file, tag, follower and optional mail/CRM merge actions.
     * </p>
     *
     * @param orgId                 Represents the organization id.
     * @param source                Represents the entity source.
     * @param primaryId             Represents the primary entity id.
     * @param secondaryIds          Represents the secondary entity ids.
     * @param targetContainer       Holds the updated rows to be committed.
     * @param additionalDirectSpecs Extra direct mapping transfers specific to the entity.
     * @param additionalDedupSpecs  Extra dedupe-safe mapping transfers specific to the entity.
     */
    protected void loadStandardMergeRowsIntoContainer(final Long orgId, final Source source, final Long primaryId,
                                                      final Collection<Long> secondaryIds, final DataContainer targetContainer,
                                                      final Collection<MapperTableTransferSpec> additionalDirectSpecs,
                                                      final Collection<MapperTableDedupedTransferSpec> additionalDedupSpecs) {
        switch (source) {
            case CONTACT -> {
                loadActivityRowsIntoContainer(orgId, primaryId, secondaryIds, sourceActivityManager.getContactActivityTable(), MACCONTACTACTIVITY.CONTACT_ID, targetContainer);
                loadNoteRowsIntoContainer(orgId, primaryId, secondaryIds, sourceNoteManager.getContactNoteTable(), OMNTCONTACTNOTE.CONTACT_ID, targetContainer);
                loadFileRowsIntoContainer(orgId, primaryId, secondaryIds, fileHolderManager.getContactFileTable(), OMFICONTACTFILE.CONTACT_ID, targetContainer);
                loadTagRowsIntoContainer(orgId, primaryId, secondaryIds, sourceTagManager.getContactTagTable(), MTCONTACTTAG.CONTACT_ID, targetContainer);
                loadFollowersIntoContainer(orgId, primaryId, secondaryIds, Source.CONTACT, OMCONFOLLOWERS.CONTACT_ID, targetContainer);
                loadTimelineRowsIntoContainer(orgId, primaryId, secondaryIds, commonTimelineManager.getContactTimelineTable(),
                        MTLCONTACTTIMELINE.CONTACT_ID, targetContainer);
                loadConversationRowsIntoContainer(orgId, primaryId, secondaryIds, OMECCONTACTCONVERSATIONS.TABLE, OMECCONTACTCONVERSATIONS.CONTACT_ID,
                        OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID, targetContainer);
            }
            case COMPANY -> {
                loadActivityRowsIntoContainer(orgId, primaryId, secondaryIds, sourceActivityManager.getCompanyActivityTable(), MACCOMPANYACTIVITY.COMPANY_ID, targetContainer);
                loadNoteRowsIntoContainer(orgId, primaryId, secondaryIds, sourceNoteManager.getCompanyNoteTable(), OMNTCOMPANYNOTE.COMPANY_ID, targetContainer);
                loadFileRowsIntoContainer(orgId, primaryId, secondaryIds, fileHolderManager.getCompanyFileTable(), OMFICOMPANYFILE.COMPANY_ID, targetContainer);
                loadTagRowsIntoContainer(orgId, primaryId, secondaryIds, sourceTagManager.getCompanyTagTable(), MTCOMPANYTAG.COMPANY_ID, targetContainer);
                loadFollowersIntoContainer(orgId, primaryId, secondaryIds, Source.COMPANY, OMCOMFOLLOWERS.COMPANY_ID, targetContainer);
                loadTimelineRowsIntoContainer(orgId, primaryId, secondaryIds, commonTimelineManager.getCompanyTimelineTable(),
                        MTLCOMPANYTIMELINE.COMPANY_ID, targetContainer);
                loadConversationRowsIntoContainer(orgId, primaryId, secondaryIds, OMECCOMPANYCONVERSATIONS.TABLE, OMECCOMPANYCONVERSATIONS.COMPANY_ID,
                        OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID, targetContainer);
            }
            case DEAL -> {
                loadActivityRowsIntoContainer(orgId, primaryId, secondaryIds, sourceActivityManager.getDealActivityTable(), MACDEALACTIVITY.DEAL_ID, targetContainer);
                loadNoteRowsIntoContainer(orgId, primaryId, secondaryIds, sourceNoteManager.getDealNoteTable(), OMNTDEALNOTE.DEAL_ID, targetContainer);
                loadFileRowsIntoContainer(orgId, primaryId, secondaryIds, fileHolderManager.getDealFileTable(), OMFIDEALFILE.DEAL_ID, targetContainer);
                loadTagRowsIntoContainer(orgId, primaryId, secondaryIds, sourceTagManager.getDealTagTable(), MTDEALTAG.DEAL_ID, targetContainer);
                loadFollowersIntoContainer(orgId, primaryId, secondaryIds, Source.DEAL, OMDOFOLLOWERS.DEAL_ID, targetContainer);
                loadConversationRowsIntoContainer(orgId, primaryId, secondaryIds, OMECDEALCONVERSATIONS.TABLE, OMECDEALCONVERSATIONS.DEAL_ID,
                        OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID, targetContainer);
                loadTimelineRowsIntoContainer(orgId, primaryId, secondaryIds, commonTimelineManager.getDealTimelineTable(),
                        MTLDEALTIMELINE.DEAL_ID, targetContainer);
            }
            default -> {
                return;
            }
        }

        if (Objects.nonNull(additionalDirectSpecs) && !additionalDirectSpecs.isEmpty()) {
            loadMappingTableRowsIntoContainer(orgId, primaryId, secondaryIds, additionalDirectSpecs, targetContainer);
        }

        if (Objects.nonNull(additionalDedupSpecs) && !additionalDedupSpecs.isEmpty()) {
            loadDeduplicatedMappingRowsIntoContainerBatch(orgId, primaryId, secondaryIds, additionalDedupSpecs, targetContainer);
        }

        EntityMergeCallbackService.opt(EntityMergeCallbackType.CHAT_MERGE)
                .ifPresent(service -> service.loadRowsIntoContainer(orgId, primaryId, secondaryIds, source, targetContainer));
        transferMails(orgId, source, primaryId, secondaryIds);
    }

    /**
     * <p>
     * Loads activity mapping rows from duplicates to primary into the target container.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param primaryId       Represents the primary entity id.
     * @param secondaryIds    Represents the duplicate/secondary entity ids.
     * @param activityTable   Represents the entity-activity mapping table.
     * @param ownerColumnName Represents the owner (entity) column name in the activity table.
     * @param targetContainer Holds the updated rows to be committed.
     */
    private void loadActivityRowsIntoContainer(final Long orgId, final Long primaryId, final Collection<Long> secondaryIds,
                                               final Table activityTable, final String ownerColumnName,
                                               final DataContainer targetContainer) {
        loadMappingTableRowsIntoContainer(orgId, primaryId, secondaryIds,
                List.of(new MapperTableTransferSpec(activityTable, ownerColumnName)),
                targetContainer);
    }

    /**
     * <p>
     * Loads note mapping rows from duplicates to primary into the target container.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param primaryId       Represents the primary entity id.
     * @param secondaryIds    Represents the duplicate/secondary entity ids.
     * @param noteTable       Represents the entity-note mapping table.
     * @param ownerColumnName Represents the owner (entity) column name in the note table.
     * @param targetContainer Holds the updated rows to be committed.
     */
    private void loadNoteRowsIntoContainer(final Long orgId, final Long primaryId, final Collection<Long> secondaryIds,
                                           final Table noteTable, final String ownerColumnName,
                                           final DataContainer targetContainer) {
        loadMappingTableRowsIntoContainer(orgId, primaryId, secondaryIds,
                List.of(new MapperTableTransferSpec(noteTable, ownerColumnName)),
                targetContainer);
    }

    /**
     * <p>
     * Loads file mapping rows from duplicates to primary into the target container.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param primaryId       Represents the primary entity id.
     * @param secondaryIds    Represents the duplicate/secondary entity ids.
     * @param fileTable       Represents the entity-file mapping table.
     * @param ownerColumnName Represents the owner (entity) column name in the file table.
     * @param targetContainer Holds the updated rows to be committed.
     */
    private void loadFileRowsIntoContainer(final Long orgId, final Long primaryId, final Collection<Long> secondaryIds,
                                           final Table fileTable, final String ownerColumnName,
                                           final DataContainer targetContainer) {
        loadMappingTableRowsIntoContainer(orgId, primaryId, secondaryIds,
                List.of(new MapperTableTransferSpec(fileTable, ownerColumnName)),
                targetContainer);
    }

    /**
     * <p>
     * Loads follower mapping rows from duplicates to primary (deduplicated) into the target container.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param primaryId       Represents the primary entity id.
     * @param secondaryIds    Represents the duplicate/secondary entity ids.
     * @param source          Represents the {@link Source} (CONTACT, COMPANY, DEAL).
     * @param ownerColumnName Represents the owner (entity) column name in the followers table.
     * @param targetContainer Holds the updated rows to be committed.
     */
    private void loadFollowersIntoContainer(final Long orgId, final Long primaryId, final Collection<Long> secondaryIds,
                                              final Source source, final String ownerColumnName,
                                              final DataContainer targetContainer) {
        loadDeduplicatedMappingRowsIntoContainer(orgId, primaryId, secondaryIds,
                ownerColumnName, "FOLLOWER_ID",
                sourceFollowersManager.getMapperTable(source), targetContainer);
    }

    /**
     * <p>
     * Loads tag mapping rows from duplicates to primary (deduplicated) into the target container.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param primaryId       Represents the primary entity id.
     * @param secondaryIds    Represents the duplicate/secondary entity ids.
     * @param tagTable        Represents the entity-tag mapping table.
     * @param ownerColumnName Represents the owner (entity) column name in the tag table.
     * @param targetContainer Holds the updated rows to be committed.
     */
    private void loadTagRowsIntoContainer(final Long orgId, final Long primaryId, final Collection<Long> secondaryIds,
                                            final Table tagTable, final String ownerColumnName,
                                            final DataContainer targetContainer) {
        loadDeduplicatedMappingRowsIntoContainer(orgId, primaryId, secondaryIds,
                ownerColumnName, "TAG_ID", tagTable, targetContainer);
    }

    /**
     * <p>
     * Transfers mails from each duplicate/secondary entity to the primary entity.
     * </p>
     *
     * @param orgId        Represents the organization id.
     * @param source       Represents the {@link Source} (CONTACT, COMPANY, DEAL).
     * @param primaryId    Represents the primary entity id.
     * @param secondaryIds Represents the duplicate/secondary entity ids.
     */
    private void transferMails(final Long orgId, final Source source, final Long primaryId,
                                 final Collection<Long> secondaryIds) {
        secondaryIds.forEach(secondaryId ->
                emailTransferActionService.transferMails(orgId, source, primaryId, secondaryId));
    }

    /**
     * <p>
     * Loads a batch of deduplicated mapping transfer specs into the target container (DB-backed).
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param primaryId       Represents the primary entity id.
     * @param secondaryIds    Represents the duplicate/secondary entity ids.
     * @param transferSpecs   Represents the {@link Collection} of {@link MapperTableDedupedTransferSpec}.
     * @param targetContainer Holds the updated rows to be committed.
     */
    private void loadDeduplicatedMappingRowsIntoContainerBatch(final Long orgId, final Long primaryId,
                                                               final Collection<Long> secondaryIds,
                                                               final Collection<MapperTableDedupedTransferSpec> transferSpecs,
                                                               final DataContainer targetContainer) {
        for (final MapperTableDedupedTransferSpec spec : transferSpecs) {
            loadDeduplicatedMappingRowsIntoContainer(orgId, primaryId, secondaryIds,
                    spec.ownerColumnName(), spec.uniqueColumnName(), spec.table(), targetContainer);
        }
    }

    /**
     * <p>
     * Loads mapping rows into target container by reassigning owner id to primary (single table).
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param primaryId       Represents the primary entity id.
     * @param secondaryIds    Represents the duplicate/secondary entity ids.
     * @param columnName      Represents the owner column name.
     * @param table           Represents the {@link Table}.
     * @param targetContainer Holds the updated rows to be committed.
     */
    private void loadMappingTableRowsIntoContainer(final Long orgId, final Long primaryId, final Collection<Long> secondaryIds,
                                                   final String columnName, final Table table,
                                                   final DataContainer targetContainer) {
        final Column ownerColumn = table.getColumn(columnName);

        orgDataStore(orgId).get(table, new WhereClause(Criteria.in(ownerColumn, secondaryIds))).getRows(table).forEach(row -> {
            row.set(ownerColumn, primaryId);
            targetContainer.updateRow(row);
        });
    }

    /**
     * <p>
     * Updates the multi-select rows for a given entity id.
     * </p>
     *
     * @param options         Represents the {@link Collection} of {@link Option}.
     * @param entityId        Represents the entity id.
     * @param fieldId         Represents the field id.
     * @param dependsOn       Represents whether the field depends on another field or not.
     * @param table           Represents the {@link Table}.
     * @param dataContainer   Holds the existing data.
     * @param targetContainer Holds the updated rows to be committed.
     */
    private void loadMultiSelectRowsIntoContainer(final Collection<Option> options, final Long entityId, final Long fieldId,
                                                  final boolean dependsOn, final Table table,
                                                  final DataContainer dataContainer, final DataContainer targetContainer) {
        if (Objects.nonNull(options)) {
            final Collection<Long> optionIds = new ArrayList<>(options.stream().map(Option::id).toList());
            final Column fieldIdColumn = table.getColumn(OMETENTITY.FIELD_ID);
            final Criteria fieldIdCriteria = Criteria.eq(fieldIdColumn, fieldId);
            final Collection<Row> rows = dataContainer.getRows(table,
                    new WhereClause(Criteria.eq(table.getColumn(OMETENTITY.ENTITY_ID), entityId))
                            .and(fieldIdCriteria)).toList();
            final Collection<Long> entityOptionIds = rows.stream().map(row -> (Long) row.get(OPTION_ID)).toList();
            final Collection<Row> entityDropdownRows = dataContainer.getRows(table,
                    new WhereClause(Criteria.in(table.getColumn(OPTION_ID), optionIds)).and(fieldIdCriteria)).toList();

            if (!(dependsOn && rows.isEmpty())) {
                for (final Row entityDropdownRow : entityDropdownRows) {
                    final Long optionId = entityDropdownRow.get(OPTION_ID);
                    final boolean hasOptionId = entityOptionIds.contains(optionId);
                    final long id = entityDropdownRow.get(OMETENTITY.ENTITY_ID);

                    if (hasOptionId && id != entityId) {
                        targetContainer.deleteRow(entityDropdownRow);
                    } else if (!hasOptionId && optionIds.remove(optionId)) {
                        entityDropdownRow.set(OMETENTITY.ENTITY_ID, entityId);
                        targetContainer.updateRow(entityDropdownRow);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link Option} for the given entity ids.
     * </p>
     *
     * @param duplicateIds  Represents the {@link Collection} of duplicate entity ids.
     * @param fieldId       Represents the field id.
     * @param table         Represents the {@link Table}.
     * @param dataContainer Holds the existing data.
     * @return {@link Collection} of {@link Option}.
     */
    private Collection<Option> getOptions(final Collection<Long> duplicateIds, final Long fieldId,
                                          final Table table, final DataContainer dataContainer) {
        final Collection<Row> entityDropdownRows = dataContainer.getRows(table,
                new WhereClause(Criteria.in(table.getColumn(OMETENTITY.ENTITY_ID), duplicateIds))
                        .and(Criteria.eq(table.getColumn(OMETENTITY.FIELD_ID), fieldId))).toList();

        if (!entityDropdownRows.isEmpty()) {
            final Table optionTable = optionManager.getTable();
            final Collection<Long> entityDropdownIds = entityDropdownRows.stream()
                    .map(row -> (Long) row.get(OPTION_ID)).toList();
            final Collection<Row> optionRows = dataContainer.getRows(optionTable,
                    new WhereClause(Criteria.in(optionTable.getPKColumn(), entityDropdownIds))).toList();

            if (!optionRows.isEmpty()) {
                final Map<Long, Option> options = new HashMap<>();
                final Column optionColumn = optionTable.getPKColumn();

                dataContainer.indexRows(optionColumn);

                optionRows.forEach(optionRow -> {
                    final Long value = optionRow.getPKValue();
                    final Row row = dataContainer.getRow(optionTable, new WhereClause(Criteria.eq(optionColumn, value)));

                    if (Objects.nonNull(row) && !options.containsKey(value)) {
                        final Option option = optionManager.convertRowToModel(row);

                        options.put(option.id(), option);
                    }
                });

                return options.values();
            }
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore}.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return {@link OrgDataStore}.
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Loads conversation mapping rows from duplicates to primary (deduplicated) into the target container.
     * </p>
     *
     * @param orgId            Represents the organization id.
     * @param primaryId        Represents the primary entity id.
     * @param secondaryIds     Represents the duplicate/secondary entity ids.
     * @param tableName        Represents the entity-conversation mapping table name.
     * @param ownerColumnName  Represents the owner (entity) column name.
     * @param uniqueColumnName Represents the conversation id column name.
     * @param targetContainer  Holds the updated rows to be committed.
     */
    private void loadConversationRowsIntoContainer(final Long orgId, final Long primaryId, final Collection<Long> secondaryIds,
                                                   final String tableName, final String ownerColumnName,
                                                   final String uniqueColumnName, final DataContainer targetContainer) {
        loadDeduplicatedMappingRowsIntoContainer(orgId, primaryId, secondaryIds,
                ownerColumnName, uniqueColumnName, Table.get(tableName), targetContainer);
    }

    /**
     * <p>
     * Loads timeline mapping rows from duplicates to primary (deduplicated) into the target container.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param primaryId       Represents the primary entity id.
     * @param secondaryIds    Represents the duplicate/secondary entity ids.
     * @param timelineTable   Represents the entity-timeline mapping table.
     * @param ownerColumnName Represents the owner (entity) column name in the timeline table.
     * @param targetContainer Holds the updated rows to be committed.
     */
    private void loadTimelineRowsIntoContainer(final Long orgId, final Long primaryId, final Collection<Long> secondaryIds,
                                               final Table timelineTable, final String ownerColumnName,
                                               final DataContainer targetContainer) {
        loadDeduplicatedMappingRowsIntoContainer(orgId, primaryId, secondaryIds,
                ownerColumnName, "TIMELINE_ID", timelineTable, targetContainer);
    }
}
