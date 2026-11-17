package com.tlc.crm.icon.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.icon.internal.status.IconErrorCodes;
import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.icon.service.IconResolver;
import com.tlc.crm.icon.service.IconService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implements the {@link IconService } provides functionality for accessing {@link Icon}
 *
 * @author Kishore J
 * @version 1.0
 * @see IconService
 */
public class IconServiceImpl implements IconService {

    private final IconResolver iconResolver;

    private IconServiceImpl() {
        iconResolver = IconResolver.getInstance();
    }

    private static class Instance {
        private static final IconServiceImpl INSTANCE = new IconServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link IconServiceImpl}.
     * </p>
     *
     * @return {@link IconServiceImpl}.
     */
    public static IconServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the icon id
     * @return {@link Icon} Which holds the attributes of icon
     */
    @Override
    public Icon get(final Long id) {
        return iconResolver.getAll().get(id);
    }

    /**
     * {@inheritDoc}
     *
     * @param iconType Represents the type of name
     * @param id       Represents the icon id
     * @return {@link Icon}
     */
    @Override
    public Icon getIconById(final Icon.Type iconType, final Long id) {
        final Collection<Icon> icons = getIconsByType(iconType);

        return Objects.isNull(icons) ? null : icons.stream()
                .filter(icon -> Objects.equals(id, icon.id())).findFirst().orElse(null);
    }

    /**
     * {@inheritDoc}
     *
     * @param iconType Represents the type of name
     * @param name     Represents the icon name
     * @return {@link Icon} Which holds the attributes of icon
     */
    @Override
    public Icon fetchIconByTypeAndName(final Icon.Type iconType, final String name) {
        final Collection<Icon> icons = getIconsByType(iconType);

        return Objects.isNull(icons) ? null : icons.stream()
                .filter(icon -> Objects.equals(name, icon.getName())).findFirst().orElse(null);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of icon
     */
    @Override
    public Collection<Icon> getIcons() {
       return iconResolver.getAll().values();
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Which contains the collection of icon ids
     * @return If the icon details exist it will return true, otherwise it will return false
     */
    @Override
    public boolean exists(final Collection<Long> ids) {
        final Collection<Long> iconIds = iconResolver.getAll().keySet();

        return Objects.nonNull(ids) && iconIds.containsAll(ids);
    }

    /**
     * {@inheritDoc}
     *
     * @param iconType Represents the type of icon
     * @return {@link Collection} of icon
     */
    @Override
    public Collection<Icon> getIconsByType(final Icon.Type iconType) {
        final Map<Long, Icon> iconMap = iconResolver.getIconsByType(iconType.name());

        if (Objects.nonNull(iconMap)) {
            return List.copyOf(iconMap.values());
        } else {
            throw ErrorCode.get(IconErrorCodes.NO_ICONS_REGISTERED_FOR_TYPE);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param iconType Represents the type of icon
     * @param ids      Represents the collection of icon id
     * @return If the icon details exist it will return true, otherwise it will return false
     */
    @Override
    public boolean exists(final Icon.Type iconType, final Collection<Long> ids) {
        return ids.stream().map(id -> getIconById(iconType, id)).allMatch(Objects::nonNull);
    }

    /**
     * {@inheritDoc}
     *
     * @param iconType Represents the icon type
     * @param name     Represents the icon name
     * @return If the icon details exist it will return true, otherwise it will return false
     */
    @Override
    public boolean exists(final Icon.Type iconType, final String name) {
        return !Objects.nonNull(fetchIconByTypeAndName(iconType, name));
    }
}