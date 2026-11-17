package com.tlc.file.server.api.manager;

import com.tlc.file.common.models.Attachable;
import com.tlc.file.server.sql.resource.METATTACHMENT;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Collection;
import java.util.Set;

/**
 * Provides an abstract way to achieve the attachment related action.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AbstractAttachmentManager<T extends Attachable> {

    /**
     * Gets the {@link AdminDataStore}.
     *
     * @return The {@link AdminDataStore}.
     */
    protected final AdminDataStore readableDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * Checks whether any rows are having the given id.
     *
     * @param id Entity id.
     * @return true if exists, false otherwise.
     */
    public final boolean exists(final long id) {
        final Table table = Table.get(METATTACHMENT.TABLE);
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        return readableDataStore().get(countQuery) > 0;
    }

    /**
     * <p>
     * Checks whether the collection of attachments exists or not.
     * </p>
     *
     * @param ids Represents the {@link Collection} of attachment ids
     * @return true, if the given {@link Collection} of attachments are exists; otherwise false
     */
    public final boolean exists(final Set<Long> ids) {
        final Table table = Table.get(METATTACHMENT.TABLE);
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        return ids.size() == readableDataStore().get(countQuery);
    }

    /**
     * Converts the row data into model entity
     *
     * @param row Attachment row to be converted.
     * @return Converted entity.
     */
    protected abstract T convertRowToModel(final Row row);

    /**
     * Fetches the table of attachment.
     *
     * @return The attachment table.
     */
    protected abstract Table getTable();

    /**
     * Fetches a row that has the given id.
     *
     * @param ids Refers the attachment ids.
     */
    public final void delete(final Collection<Long> ids) {
        readableDataStore().delete(getTable(), ids);
    }

    /**
     * Fetches a row that has the given id.
     *
     * @param ids Refers the attachment ids.
     */
    public abstract Collection<T> get(final Collection<Long> ids);

}
