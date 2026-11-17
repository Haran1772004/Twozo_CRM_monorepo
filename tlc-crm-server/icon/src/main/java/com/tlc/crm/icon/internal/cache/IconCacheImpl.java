package com.tlc.crm.icon.internal.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;

import com.tlc.crm.icon.manager.IconManager;
import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.icon.service.IconCache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stores the details of icons into cache
 *
 * @author kowsalya
 * @version 1.0
 */
public class IconCacheImpl implements IconCache {

    private final Cache<String, Map<Long, Icon>> iconCache;
    private final IconManager iconManager;
    private final ReentrantReadWriteLock lock;

    private static class Instance {
        private static final IconCacheImpl INSTANCE = new IconCacheImpl();
    }

    /**
     * Gets the {@link IconCache} instance
     *
     * @return {@link IconCacheImpl}
     */
    public static IconCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    private IconCacheImpl() {
        this.iconManager = IconManager.getInstance();
        this.iconCache = CacheManager.getInstance().createCache();
        lock = new ReentrantReadWriteLock();
    }

    /**
     * <p>
     * Loads the icon data in cache
     * </p>
     */
    public void load() {
        final Collection<Icon> icons = iconManager.getAllIcons();

        if (Objects.nonNull(icons) && !icons.isEmpty()) {
            final Map<String, Map<Long, Icon>> groupedIcons = groupIconsByType(icons);

            iconCache.putAll(groupedIcons);
        }
    }

    /**
     * <p>
     * Fetches the group of icons based on type
     * </p>
     *
     * @param icons Represents the collection of icon
     * @return A {@link Map} containing the icons grouped by their type
     */
    private Map<String, Map<Long, Icon>> groupIconsByType(final Collection<Icon> icons) {
        return icons.stream().collect(Collectors.groupingBy(icon -> icon.getType().name(), Collectors.toMap(Icon::id, Function.identity())));
    }

    /**
     * <p>
     * Clears the icons from cache
     * </p>
     */
    public void clear() {
        iconCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} containing key as icon ID and value as {@link Icon}
     */
    @Override
    public Map<Long, Icon> getAllIcons() {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            final Map<Long, Icon> iconMap = new HashMap<>();

            iconCache.forEach((iconType, icons) -> iconMap.putAll(icons));

            return iconMap;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param iconType Represents the {@link Icon.Type}.
     * @return The {@link Map} containing the key as icon ID and value as {@link Icon}
     */
    @Override
    public Map<Long, Icon> getIconsByType(final String iconType) {
        return iconCache.get(iconType);
    }

    /**
     * {@inheritDoc}
     *
     * @param iconType Represents the {@link Icon.Type}.
     * @param icon     Represents the {@link Icon}
     */
    @Override
    public void setIcon(final String iconType, final Icon icon) {
        iconCache.computeIfAbsent(iconType, iconMap -> new HashMap<>()).put(icon.id(), icon);
    }
}
