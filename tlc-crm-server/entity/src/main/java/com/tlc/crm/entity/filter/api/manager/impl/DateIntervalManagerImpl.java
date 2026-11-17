package com.tlc.crm.entity.filter.api.manager.impl;

import com.tlc.crm.entity.filter.api.manager.DateIntervalManager;
import com.tlc.crm.entity.filter.api.models.DateInterval;
import com.tlc.crm.entity.filter.api.models.DateIntervalGroup;
import com.tlc.crm.entity.sql.resource.METFDATEINTERVAL;
import com.tlc.crm.entity.sql.resource.METFDATEINTERVALGROUP;
import com.tlc.crm.fields.manager.AbstractReadableConfigManager;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;

/**
 * <p>
 * Provides the {@link DateInterval} related fetch actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class DateIntervalManagerImpl extends AbstractReadableConfigManager<DateInterval> implements DateIntervalManager {

    private static class Instance {
        private static final DateIntervalManagerImpl INSTANCE = new DateIntervalManagerImpl();
    }

    private DateIntervalManagerImpl() {
    }

    public static DateIntervalManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param row Row that holds the information of model.
     * @return Converted entity.
     */
    @Override
    public DateInterval convertRowToModel(final Row row) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return Table of date interval.
     */
    @Override
    public Table getTable() {
        return Table.get(METFDATEINTERVAL.TABLE);
    }

    private Table getDateIntervalGroupTable() {
        return Table.get(METFDATEINTERVALGROUP.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link DateInterval}.
     */
    @Override
    public Collection<DateInterval> getAllDateIntervals() {
        final Table dateIntervalTable = getTable();
        final Table dateIntervalGroupTable = getDateIntervalGroupTable();
        final SelectQuery selectQuery = SelectQuery.get(dateIntervalTable);

        selectQuery.addJoinClause(new JoinClause(dateIntervalTable.getColumn(METFDATEINTERVAL.GROUP_ID), dateIntervalGroupTable.getPKColumn()));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(dateIntervalTable).map(row -> convertRowToDateInterval(row, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param dateIntervalId Represents the date interval id
     * @return The {@link DateInterval}
     */
    @Override
    public DateInterval getDateInterval(final Long dateIntervalId) {
        final Table dateIntervalTable = getTable();
        final Table dateIntervalGroupTable = getDateIntervalGroupTable();
        final SelectQuery selectQuery = SelectQuery.get(dateIntervalTable);

        selectQuery.addJoinClause(new JoinClause(dateIntervalTable.getColumn(METFDATEINTERVAL.GROUP_ID), dateIntervalGroupTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dateIntervalTable.getPKColumn(), dateIntervalId)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return convertRowToDateInterval(dataContainer.getRow(dateIntervalTable), dataContainer);
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link DateInterval}.
     * </p>
     *
     * @param row           Holds the details of date interval.
     * @param dataContainer Holds the retrieved entities.
     * @return Converted {@link DateInterval}.
     */
    private DateInterval convertRowToDateInterval(final Row row, final DataContainer dataContainer) {
        final DateInterval dateInterval = new DateInterval();

        dateInterval.setId(row.getPKValue());
        dateInterval.setName(row.get(METFDATEINTERVAL.NAME));
        dateInterval.setDisplayName(row.get(METFDATEINTERVAL.DISPLAY_NAME));
        dateInterval.setSourceId(row.get(METFDATEINTERVAL.SOURCE_ID));
        dateInterval.setGroup(convertRowToDateIntervalGroup(dataContainer.getRow(getDateIntervalGroupTable(),
                (Long) row.get(METFDATEINTERVAL.GROUP_ID))));

        return dateInterval;
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link DateIntervalGroup}.
     * </p>
     *
     * @param row Holds the details of date interval group.
     * @return Converted {@link DateIntervalGroup}.
     */
    private DateIntervalGroup convertRowToDateIntervalGroup(final Row row) {
        final DateIntervalGroup group = new DateIntervalGroup();

        group.setId(row.getPKValue());
        group.setName(row.get(METFDATEINTERVALGROUP.NAME));
        group.setRequired(row.get(METFDATEINTERVALGROUP.IS_REQUIRED));

        return group;
    }
}