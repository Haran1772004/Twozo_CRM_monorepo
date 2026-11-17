package com.tlc.crm.core.api.handler.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.api.handler.ModificationHandler;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MPOPRODUCT;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * <p>
 * Handles the entity modification information
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public class ModificationHandlerImpl implements ModificationHandler {

    private ModificationHandlerImpl() {
    }

    private static final class Instance {
        private static final ModificationHandlerImpl MODIFICATION_HANDLER = new ModificationHandlerImpl();
    }

    public static ModificationHandlerImpl getInstance() {
        return Instance.MODIFICATION_HANDLER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param entityIds       Which contains the collection of entity ids
     * @param source          Represents the {@link Source}
     * @param targetContainer Holds the entity related details
     */
    public void loadModificationDetailsIntoContainer(final long orgId, final Long userId, final Collection<Long> entityIds, final Source source,
                                                     final DataContainer targetContainer) {
        final Table table = getTable(source);

        if (Objects.nonNull(table)) {
            final DataContainer dataContainer = DataStoreProvider.getOrgDataStore(orgId).get(table, entityIds);
            final Stream<Row> rows = dataContainer.getRows(table);
            final String modifiedTimeKey = FieldKeys.MODIFIED_TIME.getName();
            final String updatedByKey = FieldKeys.UPDATED_BY.getName();

            rows.forEach(row -> {
                final Row entityRow = targetContainer.getRow(table, row.getPKValue());
                final long currentTime = Instant.now().toEpochMilli();

                if (Objects.isNull(entityRow)) {
                    row.set(modifiedTimeKey, currentTime);
                    row.set(updatedByKey, userId);

                    targetContainer.updateRow(row);
                } else {
                    entityRow.set(modifiedTimeKey, currentTime);
                    entityRow.set(updatedByKey, userId);

                    targetContainer.updateRow(entityRow);
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param entityId Represents the entity id
     * @param source   Represents the {@link Source}
     */
    public void updateEntityModificationDetails(final long orgId, final Long userId, final Long entityId, final Source source) {
        final Table table = getTable(source);

        if (Objects.nonNull(table)) {
            final UpdateQuery updateQuery = UpdateQuery.get(table);

            updateQuery.addUpdateColumn(table.getColumn(FieldKeys.MODIFIED_TIME.getName()), Instant.now().toEpochMilli());
            updateQuery.addUpdateColumn(table.getColumn(FieldKeys.UPDATED_BY.getName()), userId);
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), entityId)));

            DataStoreProvider.getSecureOrgDataStore(orgId).update(updateQuery);
        }
    }

    /**
     * <p>
     * Gets the {@link Table}
     * </p>
     *
     * @param source Represents tha {@link Source} details
     * @return The {@link Table}
     */
    private Table getTable(final Source source) {
        return switch (source) {
            case CONTACT -> Table.get(MCOCONTACT.TABLE);
            case COMPANY -> Table.get(MCOCOMPANY.TABLE);
            case DEAL -> Table.get(MDODEAL.TABLE);
            case PRODUCT -> Table.get(MPOPRODUCT.TABLE_NAME);
            default -> throw ErrorCode.get(CoreErrorCodes.INVALID_SOURCE);
        };
    }
}
