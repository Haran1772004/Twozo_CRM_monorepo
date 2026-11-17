package com.tlc.crm.entity.listeners;

import com.tlc.crm.entity.manager.menu.MenuManager;
import com.tlc.crm.entity.models.menu.Menu;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowAddListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * <p>
 * A Listener that listens create actions of user entry for managing the menu settings action
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class MenuUserListener implements RowAddListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MenuUserListener.class);

    private final MenuManager menuManager;

    private static class Instance {
        private static final MenuUserListener MENU_USER_LISTENER = new MenuUserListener();
    }

    public MenuUserListener() {
        menuManager = MenuManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link MenuUserListener} Instance
     * </p>
     *
     * @return {@link MenuUserListener}
     */
    public static MenuUserListener getInstance() {
        return Instance.MENU_USER_LISTENER;
    }

    /**
     * {@inheritDoc}
     *
     * @param userRows Represents the {@link Collection} of user rows
     */
    @Override
    public void addRows(final Collection<Row> userRows) {
        final Collection<Menu> menus = menuManager.getDefaultMenuList();

        for (final Row userRow : userRows) {
            final Long userId = userRow.getPKValue();
            final Long orgId = userRow.getOrgId();

            try {
                menuManager.create(orgId, userId, menus);
                LOGGER.info("Menu: Default menu settings config has been successfully added to the database. Org ID: {}, User IDs: {}", orgId, userId);
            } catch (final Exception exception) {
                LOGGER.error("Menu: Default menu config create action failed. Org ID: {}, User ID: {}", orgId, userId);
            }
        }
    }
}