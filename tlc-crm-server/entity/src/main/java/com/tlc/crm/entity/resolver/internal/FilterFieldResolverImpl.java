package com.tlc.crm.entity.resolver.internal;

import com.tlc.crm.entity.cache.filter.FilterFieldCache;
import com.tlc.crm.entity.filter.api.manager.CriteriaFieldManager;
import com.tlc.crm.entity.filter.api.models.CriteriaOption;
import com.tlc.crm.entity.filter.api.models.DropdownDetails;
import com.tlc.crm.entity.resolver.filter.FilterFieldResolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Resolves the filter field related details
 *
 * @author Kishore J
 * @version 1.0
 * @see FilterFieldResolver
 */
public class FilterFieldResolverImpl implements FilterFieldResolver {

    private final ReentrantReadWriteLock lock;
    private final AtomicBoolean cacheLoaded;
    private final FilterFieldCache filterFieldCache;

    private FilterFieldResolverImpl() {
        lock = new ReentrantReadWriteLock();
        cacheLoaded = new AtomicBoolean();
        filterFieldCache = FilterFieldCache.getInstance();
    }

    private static class Instance {
        private static final FilterFieldResolverImpl INSTANCE = new FilterFieldResolverImpl();
    }

    /**
     * Gets the instance of  {@link FilterFieldResolverImpl}.
     *
     * @return {@link FilterFieldResolverImpl}.
     */
    public static FilterFieldResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaFieldId Represents the criteria field ID
     * @return If option dropdown details exists it will return true, otherwise it will return false
     */
    @Override
    public boolean checkOptionDropdownDetailExists(final Long criteriaFieldId) {
        final Map<Boolean, Collection<DropdownDetails>> optionDropdownDetails = filterFieldCache.fetchOptionDropdownDetails(criteriaFieldId);

        if (Objects.isNull(optionDropdownDetails)) {
            final CriteriaOption criteriaOption = CriteriaFieldManager.getInstance().getCriteriaOptionById(criteriaFieldId);

            if (Objects.nonNull(criteriaOption)) {
                final Collection<DropdownDetails> dropdownDetails = criteriaOption.getDropdownDetails();

                if (Objects.nonNull(dropdownDetails) && !dropdownDetails.isEmpty()) {
                    final Map<Boolean, Collection<DropdownDetails>> filterOptions = new HashMap<>();

                    for (final DropdownDetails dropdownDetail : dropdownDetails) {
                        filterOptions.computeIfAbsent(dropdownDetail.isAdditionalOption(),
                                dropdownSet -> new HashSet<>()).add(dropdownDetail);
                    }
                    criteriaOption.setDropdownDetails(null);
                    filterFieldCache.setOptionDropdownDetails(criteriaOption.getId(), filterOptions);

                    return true;
                }
            }
        }

        return filterFieldCache.checkOptionDropdownDetailExists(criteriaFieldId);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaFieldId Represents the criteria field ID
     * @return The {@link Map} containing key as boolean values as {@link DropdownDetails}.
     */
    @Override
    public Map<Boolean, Collection<DropdownDetails>> getOptionDropdownDetails(final Long criteriaFieldId) {
        return filterFieldCache.fetchOptionDropdownDetails(criteriaFieldId);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type ID.
     * @return If type dropdown details exists it will return true, otherwise it will return false
     */
    @Override
    public boolean checkTypeDropdownDetailExists(final Long fieldTypeId) {
        final Map<Boolean, Collection<DropdownDetails>> dropdownDetailsMap = filterFieldCache.fetchFieldTypeDropdownDetails(fieldTypeId);

        if (Objects.isNull(dropdownDetailsMap)) {
            final Map<Long, List<DropdownDetails>> typeDropdownDetails = CriteriaFieldManager.getInstance().fetchDropdownDetailsByFieldTypeId(List.of(fieldTypeId));

            if (typeDropdownDetails.containsKey(fieldTypeId)) {
                final Map<Boolean, Collection<DropdownDetails>> dropdownDetails = new HashMap<>();

                for (final DropdownDetails dropdownDetail : typeDropdownDetails.get(fieldTypeId)) {
                    dropdownDetails.computeIfAbsent(dropdownDetail.isAdditionalOption(),
                            dropdownList -> new HashSet<>()).add(dropdownDetail);
                }
                filterFieldCache.setTypeDropdownDetails(fieldTypeId, dropdownDetails);

                return true;
            }
        }

        return filterFieldCache.checkTypeDropdownDetailExists(fieldTypeId);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type ID
     * @return The {@link Map} containing key as boolean value as {@link DropdownDetails}.
     */
    @Override
    public Map<Boolean, Collection<DropdownDetails>> getTypeDropdownDetails(final Long fieldTypeId) {
        return filterFieldCache.fetchFieldTypeDropdownDetails(fieldTypeId);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} containing key as criteria option ID value as {@link CriteriaOption}.
     */
    @Override
    public Map<Long, CriteriaOption> getAllFilterOptions() {
        if (!cacheLoaded.get()) {
            reloadFilterOption();
        }

        return filterFieldCache.getAllFilterOptions();
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceIds Represents the source IDs.
     * @return Outer {@link Map} key as the source ID, inner {@link Map} contains the criteria option ID value as {@link CriteriaOption}
     */
    @Override
    public Map<Long, Map<Long, CriteriaOption>> getCriteriaOptions(final Set<Long> sourceIds) {
        final Map<Long, Map<Long, CriteriaOption>> filterCriteriaOptions = filterFieldCache.getFilterOptions(sourceIds);

        if (filterCriteriaOptions.isEmpty()) {

            for (final CriteriaOption criteriaOption : CriteriaFieldManager.getInstance().getCriteriaOptionBySourceId(sourceIds)) {
                filterFieldCache.setFilterOption(criteriaOption);
            }

            return filterFieldCache.getFilterOptions(sourceIds);
        }

        return filterCriteriaOptions;
    }

    /**
     * <p>
     * Reloads the filter options into cache.
     * </p>
     */
    private void reloadFilterOption() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (!cacheLoaded.get()) {
                final Collection<CriteriaOption> criteriaOptions = CriteriaFieldManager.getInstance().getAllFilterOptions();

                for (final CriteriaOption criteriaOption : criteriaOptions) {
                    filterFieldCache.setFilterOption(criteriaOption);
                }
                cacheLoaded.set(true);
            }
        } finally {
            writeLock.unlock();
        }
    }
}