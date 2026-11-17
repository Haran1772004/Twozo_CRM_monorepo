package com.tlc.crm.entity.manager.menu;

import com.tlc.crm.entity.internal.menu.MenuManagerImpl;
import com.tlc.crm.entity.models.menu.Menu;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Performs the functionality for the {@link Menu} related DB actions
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface MenuManager {

    /**
     * <p>
     * Gets the instance of {@link MenuManager}
     * </p>
     *
     * @return {@link MenuManager}
     */
    static MenuManager getInstance() {
        return MenuManagerImpl.getInstance();
    }

    /**
     * <p>
     * Creates the {@link Menu} related config
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param menus  Represents the {@link Collection} of {@link Menu}
     */
    void create(final long orgId, final long userId, final Collection<Menu> menus);

    /**
     * <p>
     * Gets the {@link Collection} of {@link Menu} mapped for the given org and user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return {@link Collection} of {@link Menu}
     */
    Collection<Menu> getMenuList(final long orgId, final long userId);

    /**
     * <p>
     * Updates the order for the given {@link Collection} menu ids
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param ids    Represents the collection of menu ids
     */
    void updateOrder(final long orgId, final long userId, final Collection<Long> ids);

    /**
     * <p>
     * Gets the {@link Collection} of default {@link Menu}
     * </p>
     *
     * @return the collection of menu
     */
    Collection<Menu> getDefaultMenuList();

    /**
     * <p>
     * Checks whether all the given menu are exists or not
     * </p>
     *
     * @param orgId  Represents an organisation ID
     * @param userId Represents the user id
     * @param ids    Which contains the collection of menu IDs
     * @return true if all the menu are exists
     */
    boolean exists(final long orgId, final long userId, final Set<Long> ids);

    /**
     * Gets the menu ID for the given name.
     *
     * @param name Refers the name to fetch.
     * @return {@code Optional} menu ID.
     */
    Optional<Long> getMenuIdByName(final String name);

    /**
     * Gets the menu by its id.
     *
     * @param orgId  Refers the organisation ID.
     * @param userId Refers the user ID.
     * @param menuId Refer the menu ID.
     * @return The {@code Menu} with config.
     */
    Menu getMenu(final long orgId, final long userId, final Long menuId);

    /**
     * Updates the menu config.
     *
     * @param orgId  Refers the organisation ID.
     * @param userId Refers the user ID.
     * @param menu   Refer the {@code Menu}.
     */
    void updateConfig(final long orgId, final long userId, final Menu menu);
}
