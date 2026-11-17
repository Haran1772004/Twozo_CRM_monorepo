package com.tlc.crm.entity.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.crm.entity.manager.field.type.OptionManager;
import com.tlc.crm.entity.models.type.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Stores the {@link Option} details into cache
 *
 * @author Kishore J
 * @version 1.0
 */
public class OptionCacheImpl implements OptionCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptionCacheImpl.class);

    private final Cache<Long, Map<Long, Collection<Option>>> optioncache;
    private final OptionManager optionManager;

    private OptionCacheImpl() {
        final CacheConfig<Long, Map<Long, Collection<Option>>> optionConfig = new CacheConfig<>("optionConfig");

        optionConfig.setTtl((int) TimeUnit.MINUTES.toMillis(5));
        optioncache = CacheServiceProvider.get().createOrOpenCache(optionConfig);
        optionManager = OptionManager.getInstance();
    }

    private static class Instance {
        private static final OptionCacheImpl INSTANCE = new OptionCacheImpl();
    }

    /**
     * <p>
     * Gets the {@link OptionCacheImpl} instance
     * </p>
     *
     * @return The {@link OptionCacheImpl}
     */
    public static OptionCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Adds the {@link Option} into the cache for the given {@link Option}
     * </p>
     *
     * @param option Represents the {@link Option}
     */
    public void addOption(final Option option) {
        loadOption(option, false);
    }

    /**
     * <p>
     * Removes the {@link Option} from the cache for the given {@link Option}
     * </p>
     *
     * @param option Represents the {@link Option}
     */
    public void removeOption(final Option option) {
        loadOption(option, true);
    }

    /**
     * <p>
     * Loads an {@link Option} into the cache, If the option is marked as deleted,
     * it is removed from the cache
     * </p>
     *
     * @param option    Represents the {@link Option}
     * @param isDeleted A flag that indicated whether the given option is deleted or not
     */
    private void loadOption(final Option option, final boolean isDeleted) {
        final Long optionId = option.id();
        final Long orgId = option.orgId();
        final Long fieldId = option.getFieldId();

        if (isDeleted) {
            final Map<Long, Collection<Option>> optionsMap = optioncache.get(orgId);

            if (Objects.nonNull(optionsMap) && !optionsMap.isEmpty()) {
                final Collection<Option> options = optionsMap.get(fieldId);

                if (Objects.nonNull(options) && !options.isEmpty()) {

                    for (final Option existingOption : options) {

                        if (Objects.equals(existingOption.id(), optionId)) {
                            options.remove(existingOption);
                            optioncache.put(orgId, optionsMap);
                            break;
                        }
                    }
                }
            }
            LOGGER.info("Option removed from cache, Org {}, FieldId {}, OptionId {}", orgId, fieldId, optionId);
        } else {
            final Collection<Option> options = getOptions(orgId, fieldId);

            if (Objects.isNull(options) || options.isEmpty()) {
                final Collection<Option> orgOptions = optionManager.getOptions(orgId, fieldId);

                if (Objects.nonNull(orgOptions) && !orgOptions.isEmpty()) {
                    setOptions(orgId, fieldId, orgOptions);
                }
            } else {
                setOptions(orgId, fieldId, List.of(option));
            }
            LOGGER.info("Org option added or updated in the cache, Org {}, FieldId {}, OptionId {}", orgId, fieldId, optionId);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param fieldId Represents the field id
     * @param options Which contains a collection of {@link Option}
     */
    @Override
    public void setOptions(final long orgId, final long fieldId, final Collection<Option> options) {
        final Map<Long, Collection<Option>> optionsMap = optioncache.computeIfAbsent(orgId, orgOptionsMap -> new HashMap<>());
        final Collection<Option> existingOptions = optionsMap.computeIfAbsent(fieldId, fieldOptions -> new HashSet<>());

        if (!existingOptions.isEmpty()) {

            for (final Option option : options) {

                if (existingOptions.contains(option)) {
                    existingOptions.remove(option);
                    existingOptions.add(option);
                } else {
                    existingOptions.add(option);
                }
            }
        } else {
            existingOptions.addAll(options);
        }
        optioncache.put(orgId, optionsMap);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param fieldId Represents the field id
     * @return The collection {@link Option}
     */
    @Override
    public Collection<Option> getOptions(final long orgId, final long fieldId) {
        final Map<Long, Collection<Option>> optionsMap = optioncache.get(orgId);

        if (Objects.nonNull(optionsMap) && !optionsMap.isEmpty()) {
            final Collection<Option> options = optionsMap.get(fieldId);

            if (Objects.nonNull(options) && !options.isEmpty()) {
                return Collections.unmodifiableCollection(options);
            }
        }

        return Collections.emptyList();
    }
}