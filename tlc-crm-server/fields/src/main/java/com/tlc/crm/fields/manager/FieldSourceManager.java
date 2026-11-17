package com.tlc.crm.fields.manager;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.sql.resource.METSOURCE;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * <p>
 * Provides the {@link FieldSource} related DB actions
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public class FieldSourceManager extends AbstractReadableConfigManager<FieldSource> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldSourceManager.class);

    private static class Instance {
        private static final FieldSourceManager INSTANCE = new FieldSourceManager();
    }

    private FieldSourceManager() {
    }

    public static FieldSourceManager getInstance() {
        return FieldSourceManager.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(METSOURCE.TABLE);
    }

    /**
     * <p>
     * Converts the row into {@link FieldSource}
     * </p>
     *
     * @param row A row that holds the attributes information of {@link FieldSource}
     */
    @Override
    public FieldSource convertRowToModel(final Row row) {
        final FieldSource fieldSource = new FieldSource();

        fieldSource.setId(row.getPKValue());
        fieldSource.setName(row.get(METSOURCE.NAME));
        fieldSource.setDisplayName(row.get(METSOURCE.DISPLAY_NAME));
        fieldSource.setCommon(row.get(METSOURCE.IS_COMMON));
        fieldSource.setImportable(row.get(METSOURCE.IS_IMPORTABLE));

        return fieldSource;
    }

    /**
     * <p>
     * Fetches the available entity sources
     * </p>
     *
     * @return Collection of {@link FieldSource}
     */
    public Collection<FieldSource> getAllEntitySources() {
        final Table table = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.notEq(table.getColumn(METSOURCE.NAME), null));

        return get(whereClause);
    }

    /**
     * <p>
     * Gets the field source given by source id
     * </p>
     *
     * @param sourceId Represents the source id.
     * @return The {@link FieldSource}
     */
    public FieldSource getFieldSource(final Long sourceId) {
        final Table table = getTable();

        return convertRowToModel(readableDataStore().get(table, new WhereClause(Criteria.eq(table.getPKColumn(), sourceId)).
                and(Criteria.eq(table.getColumn(METSOURCE.IS_COMMON), true))).getRow(table));
    }

    /**
     * <p>
     * Fetches all the available filter sources.
     * </p>
     *
     * @return Collection of {@link FieldSource}.
     */
    public Collection<FieldSource> getFilterSources() {
        return get(new WhereClause(Criteria.eq(getTable().getColumn(METSOURCE.IS_FILTERABLE), true)));
    }

    /**
     * <p>
     * Gets the field source given by source id
     * </p>
     *
     * @param sourceId Represents the source id.
     * @return The {@link FieldSource}
     */
    public FieldSource getFilterSource(final Long sourceId) {
        final Table table = getTable();

        return get(new WhereClause(Criteria.eq(table.getPKColumn(), sourceId))
                .and(Criteria.eq(table.getColumn(METSOURCE.IS_FILTERABLE), true))).stream().findAny().orElse(null);
    }
}
