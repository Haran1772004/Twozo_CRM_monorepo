package com.tlc.crm.entity.audit.log.manager.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.entity.audit.log.fields.AuditEntryFields;
import com.tlc.crm.entity.audit.log.manager.AuditManager;
import com.tlc.crm.entity.audit.log.model.AuditAction;
import com.tlc.crm.entity.audit.log.model.AuditAttribute;
import com.tlc.crm.entity.audit.log.model.AuditCategory;
import com.tlc.crm.entity.audit.log.model.AuditEntity;
import com.tlc.crm.entity.audit.log.model.AuditLog;
import com.tlc.crm.entity.audit.log.model.AuditField;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.entity.sql.resource.MAEFIELDS;
import com.tlc.crm.entity.sql.resource.METDATAPROVIDER;
import com.tlc.crm.entity.sql.resource.METFIELDS;
import com.tlc.crm.entity.sql.resource.OMALAUDITENTITY;
import com.tlc.crm.entity.sql.resource.OMALAUDITLOG;

import com.tlc.i18n.I18nKey;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Criteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>
 * Manages the audit log related DB actions
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AuditManager
 */
public class AuditManagerImpl implements AuditManager {

    private final FieldSourceService fieldSourceService;

    private static final class Instance {
        private static final AuditManagerImpl AUDIT_MANAGER = new AuditManagerImpl();
    }

    private AuditManagerImpl() {
        fieldSourceService = FieldSourceService.getInstance();
    }

    public static AuditManagerImpl getInstance() {
        return Instance.AUDIT_MANAGER;
    }

    /**
     * <p>
     * Gets the audit log table
     * </p>
     *
     * @return The audit log table
     */
    private Table getAuditLogTable() {
        return Table.get(OMALAUDITLOG.TABLE);
    }

    /**
     * <p>
     * Gets the audit entity table
     * </p>
     *
     * @return The audit entity table
     */
    private Table getAuditEntityTable() {
        return Table.get(OMALAUDITENTITY.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    public void auditEntry(final AuditEntity auditEntity, final DataContainer dataContainer) {
        final long orgId = auditEntity.orgId();
        final short categoryType = auditEntity.auditCategory().getId();
        final Long id = auditEntity.id();
        final Table auditEntityTable = getAuditEntityTable();
        final Row existingRow = dataContainer.getRow(auditEntityTable, new WhereClause(Criteria.eq(auditEntityTable.getColumn(OMALAUDITENTITY.ENTITY_ID), id))
                .and(Criteria.eq(auditEntityTable.getColumn(OMALAUDITENTITY.TYPE), categoryType)));
        final Collection<AuditLog> auditLogs = auditEntity.auditLogs();

        if (Objects.isNull(existingRow)) {
            final String entityName = auditEntity.name();
            final Row entityRow = getEntityRow(orgId, id, categoryType);
            final Long pkValue = entityRow.getPKValue();

            if (entityRow.isNewRow()) {
                entityRow.setOrgId(orgId);
                entityRow.set(OMALAUDITENTITY.ENTITY_ID, id);
                entityRow.set(OMALAUDITENTITY.NAME, entityName);
                entityRow.set(OMALAUDITENTITY.TYPE, categoryType);
                dataContainer.addNewRow(entityRow);
            } else if (!Objects.equals(entityName, entityRow.get(OMALAUDITENTITY.NAME))) {
                entityRow.set(OMALAUDITENTITY.NAME, entityName);
                dataContainer.updateRow(entityRow);
            }
            auditLogs.forEach(auditLog -> loadAuditLogIntoContainer(orgId, pkValue, auditLog, dataContainer));
        }
    }

    /**
     * <p>
     * Gets the entity row based on the entity id and {@link AuditCategory}
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param entityId     Represents the entity id
     * @param categoryType Represents the {@link AuditCategory} details
     * @return The entity row
     */
    private Row getEntityRow(final long orgId, final Long entityId, final short categoryType) {
        final Table auditEntityTable = getAuditEntityTable();
        final SelectQuery selectQuery = SelectQuery.get(auditEntityTable);

        selectQuery.addSelectClause(auditEntityTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(auditEntityTable.getColumn(OMALAUDITENTITY.TYPE), categoryType))
                .and(Criteria.eq(auditEntityTable.getColumn(OMALAUDITENTITY.ENTITY_ID), entityId)));
        final DataContainer dataContainer = DataStoreProvider.getOrgDataStore(orgId).get(selectQuery);
        final Optional<Row> auditEntityRow = dataContainer.getRows(auditEntityTable).findFirst();

        return auditEntityRow.orElseGet(() -> new Row(auditEntityTable));
    }

    /**
     * <p>
     * Loads the {@link AuditLog} details into {@link DataContainer}
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param entityId      Represents the entity id
     * @param auditLog      Represents the {@link AuditLog} details
     * @param dataContainer Holds the audit log related details
     */
    private void loadAuditLogIntoContainer(final long orgId, final Long entityId, final AuditLog auditLog,
                                           final DataContainer dataContainer) {
        final Collection<AuditAttribute> auditAttributes = auditLog.auditAttributes();
        final Row row = new Row(getAuditLogTable());

        if (Objects.nonNull(auditAttributes)) {
            final JsonArray attributeJsonArray = Json.array();

            for (final AuditAttribute auditAttribute : auditAttributes) {
                attributeJsonArray.put(auditAttribute.buildJson());
            }
            row.set(OMALAUDITLOG.DATA, attributeJsonArray);
        }
        row.setOrgId(orgId);
        row.set(OMALAUDITLOG.ACTION, auditLog.action());
        row.set(OMALAUDITLOG.ACTION_TIME, auditLog.actionTime());
        row.set(OMALAUDITLOG.ENTITY_ID, entityId);
        row.set(OMALAUDITLOG.USER_ID, auditLog.userId());

        dataContainer.addNewRow(row);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void auditEntry(final Long orgId, final Collection<AuditEntity> auditEntities) {
        final DataContainer dataContainer = DataContainer.create();

        for (final AuditEntity auditEntity : auditEntities) {
            auditEntry(auditEntity, dataContainer);
        }
        DataStoreProvider.getOrgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Collection<AuditField>> getAllFields() {
        final Table fieldsTable = getFieldsTable();
        final Table auditFieldTable = getAuditFieldTable();
        final Map<String, Collection<AuditField>> auditFields = new HashMap<>();
        final DataContainer fieldsContainer = getFieldsContainer(null);

        fieldsContainer.getRows(getProviderTable()).forEach(providerRow -> {
            final String sourceName = fieldSourceService.getSource((Long) providerRow.get(METDATAPROVIDER.SOURCE_ID)).getName();
            final Stream<Row> fieldRows = fieldsContainer.getRows(fieldsTable,
                    new WhereClause(com.tlc.sql.common.api.dml.Criteria.eq(fieldsTable.getColumn(METFIELDS.PROVIDER_ID), providerRow.getPKValue())));
            final List<AuditField> fields = fieldRows.map(fieldRow -> {
                final Long fieldId = fieldRow.getPKValue();
                final Row auditFieldRow = fieldsContainer.getRow(auditFieldTable,
                        new WhereClause(com.tlc.sql.common.api.dml.Criteria.eq(auditFieldTable.getColumn(MAEFIELDS.FIELD_ID), fieldId)));

                return new AuditField(fieldId, fieldRow.get(METFIELDS.DISPLAY_NAME), fieldRow.get(METFIELDS.NAME), auditFieldRow.get(MAEFIELDS.IS_NAME_ATTRIBUTE));
            }).toList();

            auditFields.put(sourceName, fields);
        });

        return auditFields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalLogsCount(final Long orgId, final Long userId) {
        final Table auditLogTable = getAuditLogTable();
        final CountQuery countQuery = CountQuery.get(auditLogTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(auditLogTable.getColumn(OMALAUDITLOG.USER_ID), userId)));
        return DataStoreProvider.getOrgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the org id
     * @param userId Represents the user id
     * @param start  Represents the start value
     * @param limit  Represents the limit value
     * @return The user's audit logs
     */
    @Override
    public Collection<AuditEntity> getUserAudits(final Long orgId, final Long userId, final int start, final int limit) {
        final Collection<AuditEntity> auditEntities = new ArrayList<>();
        final Table auditLogTable = getAuditLogTable();
        final Table auditEntityTable = getAuditEntityTable();
        final SelectQuery selectQuery = SelectQuery.get(auditLogTable);
        final Column entityIdColumn = auditLogTable.getColumn(OMALAUDITLOG.ENTITY_ID);

        selectQuery.addSelectClause(auditLogTable, auditEntityTable);
        selectQuery.addJoinClause(new JoinClause(entityIdColumn, auditEntityTable.getPKColumn()));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(auditLogTable.getColumn(OMALAUDITLOG.USER_ID), userId)));
        selectQuery.addOrderByClause(new OrderByClause(auditLogTable.getColumn(OMALAUDITLOG.ACTION_TIME), OrderByClause.OrderType.DESCENDING));
        final DataContainer dataContainer = DataStoreProvider.getOrgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(auditEntityTable);

        rows.forEach(auditEntityRow -> {
            final Stream<Row> auditLogRows = dataContainer.getRows(auditLogTable,
                    new WhereClause(Criteria.eq(entityIdColumn, auditEntityRow.getPKValue())));
            final Long entityId = auditEntityRow.get(OMALAUDITENTITY.ENTITY_ID);
            final String entityName = auditEntityRow.get(OMALAUDITENTITY.NAME);
            final short type = auditEntityRow.get(OMALAUDITENTITY.TYPE);
            final ArrayList<AuditLog> auditLogs = new ArrayList<>();

            auditLogRows.forEach(auditLogRow -> {
                final DateTime actionTime = auditLogRow.get(OMALAUDITLOG.ACTION_TIME);
                final AuditAction auditAction = AuditAction.valueOf(auditLogRow.get(OMALAUDITLOG.ACTION));
                final String attributes = auditLogRow.get(OMALAUDITLOG.DATA);
                final AuditLog auditLog;

                if (Objects.isNull(attributes)) {
                    auditLog = new AuditLog(userId, actionTime, auditAction);
                } else {
                    final Collection<AuditAttribute> auditAttributes = buildAuditAttributes(attributes);

                    auditLog = new AuditLog(userId, actionTime, auditAction, auditAttributes);
                }
                auditLogs.add(auditLog);
            });
            auditEntities.add(new AuditEntity(entityId, orgId, entityName, AuditCategory.get(type), auditLogs));
        });

        return auditEntities;
    }

    /**
     * <p>
     * Builds the collection of {@link AuditAttribute} from the {@link String}.
     * </p>
     *
     * @param attributes Represents the attributes as string.
     * @return Collection of {@link AuditAttribute}.
     */
    private Collection<AuditAttribute> buildAuditAttributes(final String attributes) {
        final Collection<AuditAttribute> auditAttributes = new ArrayList<>();
        final JsonArray attributeJson = Json.array(attributes);
        final int size = attributeJson.size();
        final String storeTypeKey = AuditEntryFields.STORE_TYPE.getName();

        for (int index = 0; index < size; index++) {
            final JsonObject jsonObject = attributeJson.getJsonObject(index);
            final I18nKey fieldName = new I18nKey(jsonObject.getString(AuditEntryFields.FIELD_NAME.getName()));
            final Object oldValue = jsonObject.opt(AuditEntryFields.OLD_VALUE.getName());
            final Object newValue = jsonObject.opt(AuditEntryFields.NEW_VALUE.getName());
            final AuditAction subAction = AuditAction.valueOf(jsonObject.getString(AuditEntryFields.SUB_ACTION.getName()));
            final StoreType storeType = StoreType.valueOf(jsonObject.getString(storeTypeKey));

            auditAttributes.add(new AuditAttribute(fieldName, oldValue, newValue, subAction, storeType));
        }
        return auditAttributes;
    }

    /**
     * <p>
     * Fetches the table of the audit fields.
     * </p>
     *
     * @return MAEFields table.
     */
    private Table getAuditFieldTable() {
        return Table.get(MAEFIELDS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the audit fields.
     * </p>
     *
     * @return METDataProvider table.
     */
    private Table getProviderTable() {
        return Table.get(METDATAPROVIDER.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the fields.
     * </p>
     *
     * @return METFields table.
     */
    private Table getFieldsTable() {
        return Table.get(METFIELDS.TABLE);
    }

    /**
     * <p>
     * Fetches the {@link Stream} of rows are available in the DB.
     * </p>
     *
     * @param whereClause Holds the retrieval criteria.
     * @return Stream of field rows.
     */
    private DataContainer getFieldsContainer(final WhereClause whereClause) {
        final Table fieldTable = getFieldsTable();
        final Table providerTable = getProviderTable();
        final Table auditFieldTable = getAuditFieldTable();
        final SelectQuery selectQuery = SelectQuery.get(auditFieldTable);

        selectQuery.addSelectClause(auditFieldTable);
        selectQuery.addSelectClause(List.of(fieldTable.getColumn(METFIELDS.NAME), fieldTable.getColumn(METFIELDS.DISPLAY_NAME),
                fieldTable.getColumn(METFIELDS.PROVIDER_ID), providerTable.getColumn(METDATAPROVIDER.SOURCE_ID)));
        selectQuery.addJoinClause(List.of(
                new JoinClause(auditFieldTable.getColumn(MAEFIELDS.FIELD_ID), fieldTable.getPKColumn()),
                new JoinClause(fieldTable.getColumn(METFIELDS.PROVIDER_ID), providerTable.getPKColumn())));
        selectQuery.setWhereClause(whereClause);

        return DataStoreProvider.getReadableDataStore().get(selectQuery);
    }
}
