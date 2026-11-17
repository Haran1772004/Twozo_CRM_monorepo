package com.tlc.crm.icon.service;

import com.tlc.crm.icon.internal.IconServiceImpl;
import com.tlc.crm.icon.model.Icon;

import java.util.Collection;

/**
 * <p>
 * Provides an abstract way to deal with {@link Icon}
 * </p>
 *
 * @author kishore J
 * @version 1.0
 */
public interface IconService {

    /**
     * <p>
     * Gets the {@link IconService} instance.
     * </p>
     *
     * @return {@link IconService}.
     */
    static IconService getInstance() {
        return IconServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the icon by given id
     * </p>
     *
     * @param id Represents the icon id
     * @return {@link Icon} Which contains the {@link Icon}
     */
    Icon get(final Long id);

    /**
     * <p>
     * Gets the {@link Icon} given by icon type and icon id
     * </p>
     *
     * @param iconType Represents the icon type
     * @param id       Represents the icon id
     * @return {@link Icon} Which contains the icon data
     */
    Icon getIconById(final Icon.Type iconType, final Long id);

    /**
     * <p>
     * Gets the {@link Icon} by icon type and name
     * </p>
     *
     * @param iconType Represents the type of icon type
     * @param name     Represents the name of icon
     * @return {@return Icon} Which contains the attributes of icon
     */
    Icon fetchIconByTypeAndName(final Icon.Type iconType, final String name);

    /**
     * <p>
     * Gets the {@link Collection} of {@link Icon}
     * </p>
     *
     * @param iconType Which contains the collection of {@link Icon}
     * @return {@link Collection} of icon
     */
    Collection<Icon> getIconsByType(final Icon.Type iconType);

    /**
     * <p>
     * Fetches the icons for dropdown
     * </p>
     *
     * @return {@link Collection} of {@link Icon}
     */
    Collection<Icon> getIcons();

    /**
     * <p>
     * Checks whether all the given icon are exists or not for the given icon ids
     * </p>
     *
     * @param ids Which contains the collection of icon IDs
     * @return If the icon details exist it will return true, otherwise it will return false
     */
    boolean exists(final Collection<Long> ids);

    /**
     * <p>
     * Checks whether the icon exists or not for the given icon type and ids
     * </p>
     *
     * @param iconType Represents the type of icon
     * @param ids      Represents the collection of icon id
     * @return If the icon details exist it will return true, otherwise it will return false
     */
    boolean exists(final Icon.Type iconType, final Collection<Long> ids);

    /**
     * <p>
     * Checks whether the icon exists or not for the given icon type and name.
     * </p>
     *
     * @param iconType Represents the type of icon type
     * @param name     Represents the name of icon
     * @return If the icon details exist it will return true, otherwise it will return false
     */
    boolean exists(final Icon.Type iconType, final String name);
}