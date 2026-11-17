package com.tlc.crm.entity.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.entity.cache.filter.FilterFieldCache;
import com.tlc.crm.entity.filter.api.manager.CriteriaFieldManager;
import com.tlc.crm.entity.filter.api.models.CriteriaOption;
import com.tlc.crm.entity.filter.api.models.DropdownDetails;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.fields.models.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * <p>
 * Stores the filter fields into cache.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FilterFieldCacheImpl implements FilterFieldCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(FilterFieldCacheImpl.class);

    private final Cache<Long, Map<Long, CriteriaOption>> filterOptionCache;
    private final Cache<Long, Map<Boolean, Collection<DropdownDetails>>> typeDropdownDetailsCache;
    private final Cache<Long, Map<Boolean, Collection<DropdownDetails>>> optionDropdownDetailsCache;
    private final FieldTypeService fieldTypeService;
    private final ReentrantReadWriteLock lock;

    private static class Instance {
        private static final FilterFieldCacheImpl INSTANCE = new FilterFieldCacheImpl();
    }

    private FilterFieldCacheImpl() {
        final CacheManager cacheManager = CacheManager.getInstance();

        filterOptionCache = cacheManager.createCache();
        typeDropdownDetailsCache = cacheManager.createCache();
        optionDropdownDetailsCache = cacheManager.createCache();
        fieldTypeService = FieldTypeService.getInstance();
        lock = new ReentrantReadWriteLock();
    }

    /**
     * Gets the {@link FilterFieldCacheImpl} instance.
     *
     * @return {@link FilterFieldCacheImpl}.
     */
    public static FilterFieldCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<CriteriaOption> criteriaOptions = CriteriaFieldManager.getInstance().getAllFilterOptions();

        if (!criteriaOptions.isEmpty()) {
            final Map<Long, Map<Boolean, Collection<DropdownDetails>>> groupedOptionDropdownDetails = groupOptionDropdownDetails(criteriaOptions);
            final Map<Long, Map<Long, CriteriaOption>> groupedFilterOptions = groupFilterOptions(criteriaOptions);

            LOGGER.info("Total number of filter option entity loaded into cache : {}", criteriaOptions.size());
            filterOptionCache.putAll(groupedFilterOptions);
            optionDropdownDetailsCache.putAll(groupedOptionDropdownDetails);
            typeDropdownDetailsCache.putAll(fetchTypeDropdownDetails(fieldTypeService.getAllFieldTypes().stream()
                    .map(FieldType::id).collect(Collectors.toSet())));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        typeDropdownDetailsCache.clear();
        optionDropdownDetailsCache.clear();
        filterOptionCache.clear();
    }

    /**
     * <p>
     * Groups the {@link CriteriaOption} entity by entitySource.
     * </p>
     *
     * @param criteriaOptions List of criteria options.
     * @return Filter fields will be by grouped source.
     */
    private Map<Long, Map<Long, CriteriaOption>> groupFilterOptions(final Collection<CriteriaOption> criteriaOptions) {
        final Map<Long, Map<Long, CriteriaOption>> groupedCriteriaOption = new HashMap<>();

        for (final CriteriaOption criteriaOption : criteriaOptions) {
            final Map<Long, CriteriaOption> filterOptions = groupedCriteriaOption.computeIfAbsent(criteriaOption.getSourceId(),
                    options -> new HashMap<>());

            filterOptions.putIfAbsent(criteriaOption.getId(), criteriaOption);
        }

        return groupedCriteriaOption;
    }

    /**
     * <p>
     * Loads the dropdown details based on field types.
     * </p>
     *
     * @param fieldTypeIds Holds the collection of the field type ids.
     */
    private Map<Long, Map<Boolean, Collection<DropdownDetails>>> fetchTypeDropdownDetails(final Collection<Long> fieldTypeIds) {
        final Map<Long, Map<Boolean, Collection<DropdownDetails>>> groupedDropdownDetails = new HashMap<>();
        final Map<Long, List<DropdownDetails>> typeDropdownDetails = CriteriaFieldManager.getInstance().fetchDropdownDetailsByFieldTypeId(fieldTypeIds);

        for (final Long fieldTypeId : fieldTypeIds) {
            if (typeDropdownDetails.containsKey(fieldTypeId)) {
                final List<DropdownDetails> dropdownDetailsList = typeDropdownDetails.get(fieldTypeId);
                final Map<Boolean, Collection<DropdownDetails>> dropdownDetails =
                        groupedDropdownDetails.computeIfAbsent(fieldTypeId, fieldDropdownDetail -> new HashMap<>());

                for (final DropdownDetails dropdownDetail : dropdownDetailsList) {
                    final Collection<DropdownDetails> dropdownDetailList =
                            dropdownDetails.computeIfAbsent(dropdownDetail.isAdditionalOption(), dropdownList -> new HashSet<>());

                    dropdownDetailList.add(dropdownDetail);
                }
            }
        }

        return groupedDropdownDetails;
    }

    /**
     * <p>
     * Groups the {@link DropdownDetails} by the fieldId.
     * </p>
     *
     * @param criteriaOptions Holds the collection of the {@link CriteriaOption} entities.
     * @return Grouped dropdown details.
     */
    private Map<Long, Map<Boolean, Collection<DropdownDetails>>> groupOptionDropdownDetails(final Collection<CriteriaOption> criteriaOptions) {
        final Map<Long, Map<Boolean, Collection<DropdownDetails>>> groupedDropdownDetails = new HashMap<>();

        for (final CriteriaOption criteriaOption : criteriaOptions) {
            final Collection<DropdownDetails> dropdownDetails = criteriaOption.getDropdownDetails();

            if (Objects.nonNull(dropdownDetails)) {
                final Map<Boolean, Collection<DropdownDetails>> filterOptions =
                        groupedDropdownDetails.computeIfAbsent(criteriaOption.getId(), options -> new HashMap<>());

                for (final DropdownDetails dropdownDetail : dropdownDetails) {
                    final Collection<DropdownDetails> dropdownDetailList =
                            filterOptions.computeIfAbsent(dropdownDetail.isAdditionalOption(), dropdownList -> new HashSet<>());

                    dropdownDetailList.add(dropdownDetail);
                }
                criteriaOption.setDropdownDetails(null);
            }
        }

        return groupedDropdownDetails;
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaOptionId             Represents the criteria option id.
     * @param groupedOptionDropdownDetails Which contains {@link Map} key as boolean value as collections of {@link DropdownDetails}.
     */
    @Override
    public void setOptionDropdownDetails(final Long criteriaOptionId, final Map<Boolean, Collection<DropdownDetails>> groupedOptionDropdownDetails) {
        optionDropdownDetailsCache.put(criteriaOptionId, groupedOptionDropdownDetails);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId                Represents the field type id
     * @param groupedTypeDropdownDetails Which contains {@link Map} key as boolean value as collection of {@link DropdownDetails}
     */
    @Override
    public void setTypeDropdownDetails(final Long fieldTypeId, final Map<Boolean, Collection<DropdownDetails>> groupedTypeDropdownDetails) {
        typeDropdownDetailsCache.put(fieldTypeId, groupedTypeDropdownDetails);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaOption Represents the {@link CriteriaOption}
     */
    @Override
    public void setFilterOption(final CriteriaOption criteriaOption) {
        filterOptionCache.computeIfAbsent(criteriaOption.getSourceId(), criteriaOptionMap -> new HashMap<>()).
                put(criteriaOption.getId(), criteriaOption);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaOptionId Represents the criteria option id
     * @return The {@link Map} containing key as boolean value as collection of {@link DropdownDetails}
     */
    @Override
    public Map<Boolean, Collection<DropdownDetails>> fetchOptionDropdownDetails(final Long criteriaOptionId) {
        return optionDropdownDetailsCache.get(criteriaOptionId);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type id
     * @return The {@link Map} containing key as boolean value as collection of {@link DropdownDetails}.
     */
    @Override
    public Map<Boolean, Collection<DropdownDetails>> fetchFieldTypeDropdownDetails(final Long fieldTypeId) {
        return typeDropdownDetailsCache.get(fieldTypeId);
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Which holds the collection of source ids
     * @return The {@link Map} key as the source id, inner {@link Map} key as criteria option id value as {@link CriteriaOption}
     */
    @Override
    public Map<Long, Map<Long, CriteriaOption>> getFilterOptions(final Set<Long> ids) {
        return filterOptionCache.getAll(ids);
    }

    /**
     * {@inheritDoc}
     *
     * @return A {@link Map} contains criteria option id and {@link CriteriaOption}.
     */
    @Override
    public Map<Long, CriteriaOption> getAllFilterOptions() {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            final Map<Long, CriteriaOption> filterOptions = new HashMap<>();

            filterOptionCache.forEach((sourceId, criteriaOptions) -> filterOptions.putAll(criteriaOptions));

            return filterOptions;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaOptionId Represents the criteria option id.
     * @return If option dropdown details exists it will return true, otherwise it will return false
     */
    @Override
    public boolean checkOptionDropdownDetailExists(final Long criteriaOptionId) {
        return optionDropdownDetailsCache.containsKey(criteriaOptionId);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type id.
     * @return If type dropdown details exists it will return true, otherwise it will return false
     */
    @Override
    public boolean checkTypeDropdownDetailExists(final Long fieldTypeId) {
        return typeDropdownDetailsCache.containsKey(fieldTypeId);
    }
}
