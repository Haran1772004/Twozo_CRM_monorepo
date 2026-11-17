package com.tlc.crm.icon.internal.resolver;

import com.tlc.crm.icon.manager.IconManager;
import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.icon.service.IconCache;
import com.tlc.crm.icon.service.IconResolver;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Resolves the {@link Icon} related details
 *
 * @author Kishore J
 * @version 1.0
 * @see IconResolver
 */
public class IconResolverImpl implements IconResolver {

    private final IconCache iconCache;
    private final IconManager iconManager;
    private final ReentrantReadWriteLock lock;
    private final AtomicBoolean cacheLoaded;

    private IconResolverImpl() {
        iconCache = IconCache.getInstance();
        iconManager = IconManager.getInstance();
        lock = new ReentrantReadWriteLock();
        cacheLoaded = new AtomicBoolean();
    }

    private static class Instance {
        private static final IconResolverImpl INSTANCE = new IconResolverImpl();
    }

    /**
     * Gets the instance of {@link IconResolverImpl}.
     *
     * @return {@link IconResolverImpl}.
     */
    public static IconResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} containing key as icon ID value as {@link Icon}.
     */
    @Override
    public Map<Long, Icon> getAll() {
        if (!cacheLoaded.get()) {
            reloadIcon();
        }

        return iconCache.getAllIcons();
    }

    /**
     * {@inheritDoc}
     *
     * @param iconType Represents the {@link Icon.Type}.
     * @return The {@link Map} containing key as icon ID value as {@link Icon}.
     */
    @Override
    public Map<Long, Icon> getIconsByType(final String iconType) {
        final Map<Long, Icon> iconMap = iconCache.getIconsByType(iconType);

        if (Objects.isNull(iconMap)) {
            final Collection<Icon> icons = iconManager.getIconsByIconType(iconType);

            for (final Icon icon : icons) {
                iconCache.setIcon(iconType.toUpperCase(), icon);
            }

            return iconCache.getIconsByType(iconType);
        }

        return iconMap;
    }

    /**
     * <p>
     * Reloads the icons into cache.
     * </p>
     */
    private void reloadIcon() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (!cacheLoaded.get()) {
                final Collection<Icon> icons = iconManager.getAllIcons();

                if (Objects.nonNull(icons) && !icons.isEmpty()) {
                    final Map<String, List<Icon>> iconMap = icons.stream()
                            .collect(Collectors.groupingBy(icon -> icon.getType().name()));

                    for (final Map.Entry<String, List<Icon>> iconEntry : iconMap.entrySet()) {

                        for (final Icon icon : iconEntry.getValue()) {
                            iconCache.setIcon(iconEntry.getKey(), icon);
                        }
                    }
                    cacheLoaded.set(true);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }
}