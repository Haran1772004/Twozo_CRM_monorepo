package com.tlc.crm.icon.manager;

import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.icon.sql.resource.MICICON;
import com.tlc.crm.icon.sql.resource.MICICONHOLDER;
import com.tlc.crm.icon.sql.resource.MICICONTYPE;

import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Collection;

/**
 * Manages the {@link Icon} related DB actions
 *
 * @author kowsalya
 * @version 1.0
 */
public class IconManager {

    private final ReadableDataStore readableDataStore;

    private static class Instance {
        private static final IconManager INSTANCE = new IconManager();
    }

    private IconManager() {
        final SQLService sqlService = SqlServiceProvider.get();

        readableDataStore = sqlService.getAdminDataStore();
    }

    /**
     * <p>
     * Gets the {@link IconManager} Instance
     * </p>
     *
     * @return {@link IconManager}
     */
    public static IconManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Returns {@link ReadableDataStore} of an organization
     * </p>
     *
     * @return {@link ReadableDataStore}
     */
    private ReadableDataStore readableDataStore() {
        return readableDataStore;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of icon
     */
    private Table getTable() {
        return Table.get(MICICON.TABLE);
    }

    /**
     * Gets the table of icon holder
     *
     * @return the {@link Table} of iconHolder
     */
    private Table getHolderTable() {
        return Table.get(MICICONHOLDER.TABLE);
    }

    /**
     * Gets the table of icon type
     *
     * @return the {@link Table} of iconType
     */
    private Table getTypeTable() {
        return Table.get(MICICONTYPE.TABLE);
    }

    /**
     * <p>
     * Converts row into {@link Icon}
     * </p>
     *
     * @param row Represents the icon row to be converted
     * @return {@link Icon} Which holds the attributes of icon
     */
    public Icon convertRowToModel(final Row row) {
        final Icon icon = new Icon();

        icon.setId(row.getPKValue());
        icon.setName(row.get(MICICON.NAME));
        icon.setI18nName(row.get(MICICON.DISPLAY_NAME));

        return icon;
    }

    /**
     * Fetches the all icons
     *
     * @return {@link Collection} of icon
     */
    public Collection<Icon> getAllIcons() {
        final Table iconTable = getTable();
        final Table holderTable = getHolderTable();
        final Table typeTable = getTypeTable();
        final SelectQuery selectQuery = SelectQuery.get(iconTable);

        selectQuery.addJoinClause(new JoinClause(iconTable.getPKColumn(), holderTable.getColumn(MICICONHOLDER.ICON_ID)));
        selectQuery.addJoinClause(new JoinClause(holderTable.getColumn(MICICONHOLDER.TYPE_ID), typeTable.getPKColumn()));
        selectQuery.addSelectClause(iconTable, holderTable, typeTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.notEq(iconTable.getPKColumn(), null)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(holderTable).map(row -> convertPartialRowToModel(row, dataContainer)).toList();
    }

    /**
     * Converts partial Row in to {@link Icon}
     *
     * @param row           Represents the row of icon holder
     * @param dataContainer Holds the existing records of the icon
     * @return {@link Icon} Which holds the attributes of icon
     */
    private Icon convertPartialRowToModel(final Row row, final DataContainer dataContainer) {
        final Table iconTable = getTable();
        final Table typeTable = getTypeTable();
        final Long typeId = row.get(MICICONHOLDER.TYPE_ID);
        final Icon icon = new Icon();

        icon.setId(row.getPKValue());
        icon.setName(dataContainer.getRow(iconTable, new WhereClause(Criteria.eq(iconTable.getPKColumn(),
                row.get(MICICONHOLDER.ICON_ID)))).get(MICICON.NAME));
        final String typeName = dataContainer.getRow(typeTable, new WhereClause(Criteria.eq(typeTable.getPKColumn(),
                typeId))).get(MICICONTYPE.NAME);

        icon.setType(Icon.Type.getIconType(typeName));
        icon.setI18nName(dataContainer.getRow(iconTable, new WhereClause(Criteria.eq(iconTable.getPKColumn(),
                row.get(MICICONHOLDER.ICON_ID)))).get(MICICON.DISPLAY_NAME));

        return icon;
    }

    /**
     * <p>
     * Gets collection of icons for the given icon type name
     * </p>
     *
     * @param iconTypeName Represents the {@link Icon.Type}
     * @return The Collection of {@link Icon}
     */
    public Collection<Icon> getIconsByIconType(final String iconTypeName) {
        final Table iconTable = getTable();
        final Table holderTable = getHolderTable();
        final Table typeTable = getTypeTable();
        final SelectQuery selectQuery = SelectQuery.get(iconTable);

        selectQuery.addJoinClause(new JoinClause(iconTable.getPKColumn(), holderTable.getColumn(MICICONHOLDER.ICON_ID)));
        selectQuery.addJoinClause(new JoinClause(holderTable.getColumn(MICICONHOLDER.TYPE_ID), typeTable.getPKColumn()));
        selectQuery.addSelectClause(iconTable, holderTable, typeTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(typeTable.getColumn(MICICONTYPE.NAME), iconTypeName)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(holderTable).map(row -> convertPartialRowToModel(row, dataContainer)).toList();
    }
}
