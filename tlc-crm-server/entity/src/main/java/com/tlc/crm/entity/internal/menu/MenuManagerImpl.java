package com.tlc.crm.entity.internal.menu;

import com.tlc.crm.entity.fields.MenuFields;
import com.tlc.crm.entity.manager.menu.MenuManager;
import com.tlc.crm.entity.models.menu.DefaultMenu;
import com.tlc.crm.entity.models.menu.HomeMenuMetrics;
import com.tlc.crm.entity.models.menu.Menu;
import com.tlc.crm.entity.sql.resource.METMENU;
import com.tlc.crm.entity.sql.resource.OMETMENUCONFIG;

import com.tlc.crm.icon.service.IconService;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.commons.Counter;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * A {@link MenuManagerImpl} that provides the various implementations for the {@link MenuManager}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class MenuManagerImpl implements MenuManager {

    private final IconService iconService;

    private static class Instance {
        private static final MenuManagerImpl INSTANCE = new MenuManagerImpl();
    }

    private MenuManagerImpl() {
        iconService = IconService.getInstance();
    }

    /**
     * <p>
     * Gets the {@link MenuManagerImpl} Instance
     * </p>
     *
     * @return {@link MenuManagerImpl}
     */
    public static MenuManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the table of {@link Menu}
     * </p>
     *
     * @return The menu table
     */
    public Table getTable() {
        return Table.get(METMENU.TABLE);
    }

    /**
     * <p>
     * Gets the table of {@link Menu.Config}
     * </p>
     *
     * @return The menu config table
     */
    public Table getMenuConfigTable() {
        return Table.get(OMETMENUCONFIG.TABLE);
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore}
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the {@link AdminDataStore}
     * </p>
     *
     * @return {@link AdminDataStore}
     */
    private ReadableDataStore readableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @param menus  Represents the {@link Collection} of {@link Menu}
     */
    @Override
    public void create(final long orgId, final long userId, final Collection<Menu> menus) {
        final DataContainer targetContainer = DataContainer.create();
        final Table menuconfigTable = getMenuConfigTable();

        for (final Menu menu : menus) {
            final Row menuConfigRow = new Row(menuconfigTable);

            menuConfigRow.setOrgId(orgId);
            menuConfigRow.set(OMETMENUCONFIG.USER_ID, userId);
            menuConfigRow.set(OMETMENUCONFIG.MENU_ID, menu.getId());
            menuConfigRow.set(OMETMENUCONFIG.ORDER, menu.getConfig().getOrder());
            menuConfigRow.set(OMETMENUCONFIG.CONFIG, menu.getConfig().getMetrics());

            targetContainer.addNewRow(menuConfigRow);
        }
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return {@link Collection} of {@link Menu}
     */
    @Override
    public Collection<Menu> getMenuList(final long orgId, final long userId) {
        final Table menuTable = getTable();
        final Collection<Menu> menuList = new ArrayList<>();
        final Table menuConfigTable = getMenuConfigTable();
        final SelectQuery selectQuery = SelectQuery.get(menuTable);

        selectQuery.addJoinClause(new JoinClause(menuTable.getPKColumn(), menuConfigTable.getColumn(OMETMENUCONFIG.MENU_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(menuConfigTable.getColumn(OMETMENUCONFIG.USER_ID), userId)));
        selectQuery.addSelectClause(menuTable, menuConfigTable);
        selectQuery.addOrderByClause(new OrderByClause(menuConfigTable.getColumn(OMETMENUCONFIG.ORDER)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(menuTable).forEach(row -> menuList.add(convertRowToModel(row, dataContainer)));

        return menuList;
    }

    /**
     * <p>
     * Converts the {@link Row} to {@link Menu}
     * </p>
     *
     * @param row           Represents the {@link Row}
     * @param dataContainer Represents the {@link DataContainer}
     * @return {@link Menu}
     */
    private Menu convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Menu menu = convertRowTomenu(row);
        final Menu.Config config = new Menu.Config();
        final Table menuConfigTable = getMenuConfigTable();
        final Row menuConfigRow = dataContainer.getRow(menuConfigTable, new WhereClause(Criteria.eq(menuConfigTable.getColumn(OMETMENUCONFIG.MENU_ID), row.getPKValue())));

        config.setOrder(menuConfigRow.get(OMETMENUCONFIG.ORDER));
        config.setId(menuConfigRow.getPKValue());
        menu.setConfig(config);
        menu.setDisplayName(row.get(METMENU.DISPLAY_NAME));
        menu.setIcon(iconService.get(row.get(METMENU.ICON_ID)));
        menu.setSourceId(row.get(METMENU.SOURCE_ID));
        menu.setOrgId(menuConfigRow.getOrgId());

        return menu;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param ids    Represents the collection of entity ids
     */
    @Override
    public void updateOrder(final long orgId, final long userId, final Collection<Long> ids) {
        final List<UpdateQuery> updateQueries = new ArrayList<>();
        final Counter counter = new Counter(getHomeMenuOrder());
        final Table menuConfigTable = getMenuConfigTable();

        for (final Long id : ids) {
            final UpdateQuery updateQuery = UpdateQuery.get(menuConfigTable);

            updateQuery.setWhereClause(new WhereClause(Criteria.eq(menuConfigTable.getColumn(OMETMENUCONFIG.MENU_ID), id))
                    .and(Criteria.eq(menuConfigTable.getColumn(OMETMENUCONFIG.USER_ID), userId)));
            updateQuery.addUpdateColumn(menuConfigTable.getColumn(OMETMENUCONFIG.ORDER), counter.incrementAndGet());
            updateQueries.add(updateQuery);
        }
        orgDataStore(orgId).update(updateQueries);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an Organisation ID
     * @param userId  Represents the user id
     * @param menuIds Which contains the collection of menu IDs
     * @return true if all the menu are exists
     */
    @Override
    public boolean exists(final long orgId, final long userId, final Set<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return false;
        } else {
            final Table menuConfigTable = getMenuConfigTable();
            final CountQuery countQuery = CountQuery.get(menuConfigTable);

            countQuery.setWhereClause(new WhereClause(Criteria.in(menuConfigTable.getColumn(OMETMENUCONFIG.MENU_ID), menuIds))
                    .and(Criteria.eq(menuConfigTable.getColumn(OMETMENUCONFIG.USER_ID), userId)));

            return menuIds.size() == orgDataStore(orgId).get(countQuery);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param name Refers the name to fetch.
     * @return {@code Optional} menu ID.
     */
    @Override
    public Optional<Long> getMenuIdByName(final String name) {
        final Table table = getTable();
        final Row row = readableDataStore().get(table, new WhereClause(Criteria.eq(table.getColumn(METMENU.NAME), name))).getRow(table);

        return Optional.ofNullable(row).map(Row::getPKValue);
    }

    /**
     * {@inheritDoc}
     *
     * @return the collection of menus
     */
    @Override
    public Collection<Menu> getDefaultMenuList() {
        final Collection<String> menuNames = Arrays.stream(DefaultMenu.values()).map(DefaultMenu::getName).toList();
        final Table menuTable = getTable();
        final Collection<Menu> menuList = new ArrayList<>();
        final SelectQuery selectQuery = SelectQuery.get(menuTable);
        final Counter counter = new Counter();

        selectQuery.setWhereClause(new WhereClause(Criteria.in(menuTable.getColumn(METMENU.NAME), menuNames)));
        selectQuery.addOrderByClause(new OrderByClause(menuTable.getColumn(METMENU.ORDER)));

        readableDataStore().get(selectQuery).getRows(menuTable).forEach(row -> {
            final Menu menu = convertRowTomenu(row);
            final Menu.Config config = new Menu.Config();

            if (MenuFields.HOME.equals(menu.getName())) {
                config.setMetrics(Json.object().put(MenuFields.METRICS, HomeMenuMetrics.getIds()).toString());
            }
            config.setOrder(counter.incrementAndGet());
            menu.setConfig(config);
            menuList.add(menu);
        });

        return menuList;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organisation ID.
     * @param userId Refers the user ID.
     * @param menuId Refer the menu ID.
     */
    @Override
    public Menu getMenu(final long orgId, final long userId, final Long menuId) {
        final Table menuTable = getTable();
        final Table menuConfigTable = getMenuConfigTable();
        final SelectQuery selectQuery = SelectQuery.get(menuTable);

        selectQuery.addJoinClause(new JoinClause(menuTable.getPKColumn(), menuConfigTable.getColumn(OMETMENUCONFIG.MENU_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(menuConfigTable.getColumn(OMETMENUCONFIG.USER_ID), userId))
                .and(Criteria.eq(menuConfigTable.getColumn(OMETMENUCONFIG.MENU_ID), menuId)));
        selectQuery.addSelectClause(List.of(menuTable.getPKColumn(), menuTable.getColumn(METMENU.NAME)));
        selectQuery.addSelectClause(menuConfigTable);

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(menuTable);
        final Menu menu = convertRowTomenu(row);
        final Menu.Config menuConfig = new Menu.Config();
        final Row menuConfigRow = dataContainer.getRow(menuConfigTable, new WhereClause(Criteria.eq(menuConfigTable.getColumn(OMETMENUCONFIG.MENU_ID), row.getPKValue())));
        final Object config = menuConfigRow.get(OMETMENUCONFIG.CONFIG);

        if (Objects.nonNull(config)) {
            final JsonObject configJson = Json.object((String) config);

            menuConfig.setMetricsIds(configJson.optNullableValue(MenuFields.METRICS, null));
        }
        menuConfig.setOrder(menuConfigRow.get(OMETMENUCONFIG.ORDER));
        menuConfig.setId(menuConfigRow.getPKValue());
        menu.setConfig(menuConfig);
        menu.setOrgId(orgId);

        return menu;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organization ID.
     * @param userId Refers the user ID.
     * @param menu   Refer the {@code Menu}.
     */
    @Override
    public void updateConfig(final long orgId, final long userId, final Menu menu) {
        final Table menuConfigTable = getMenuConfigTable();
        final UpdateQuery updateQuery = UpdateQuery.get(menuConfigTable);

        updateQuery.setWhereClause(new WhereClause(Criteria.eq(menuConfigTable.getColumn(OMETMENUCONFIG.USER_ID), userId))
                .and(Criteria.eq(menuConfigTable.getColumn(OMETMENUCONFIG.MENU_ID), menu.getId())));
        updateQuery.addUpdateColumn(menuConfigTable.getColumn(OMETMENUCONFIG.CONFIG), menu.getConfig().getMetrics());

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Converts the {@link Row} to {@link Menu}
     * </p>
     *
     * @param row Represents the {@link Row}
     * @return the {@link Menu}
     */
    private Menu convertRowTomenu(final Row row) {
        final Menu menu = new Menu();

        menu.setId(row.getPKValue());
        menu.setName(row.get(METMENU.NAME));

        return menu;
    }

    /**
     * Returns the display order position for the Home menu.
     *
     * @return The order value used to position the Home menu.
     */
    private int getHomeMenuOrder() {
        final Table table = getTable();
        final Row row = readableDataStore().get(table, new WhereClause(Criteria.eq(table.getColumn(METMENU.NAME), MenuFields.HOME)),
                List.of(table.getColumn(METMENU.ORDER))).getRow(table);

        return Objects.isNull(row) ? 0 : row.get(METMENU.ORDER);
    }
}
