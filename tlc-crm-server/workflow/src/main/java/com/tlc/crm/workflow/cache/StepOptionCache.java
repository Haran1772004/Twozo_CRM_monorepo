package com.tlc.crm.workflow.cache;

import com.tlc.crm.common.cache.LocalCache;

import com.tlc.crm.workflow.api.model.StepOption;
import com.tlc.crm.workflow.cache.internal.StepOptionCacheImpl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Provides an abstract way to deal with {@link StepOption}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface StepOptionCache extends LocalCache {

    /**
     * <p>
     * Retrieves the instance of {@link StepOptionCache}
     * </p>
     *
     * @return The {@link StepOptionCache} instance
     */
    static StepOptionCache getInstance() {
        return StepOptionCacheImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link StepOption} by step option id from the cache, If not found in the cache, fetch it from the DB
     * and stores it in cache and returns the {@link StepOption}
     * </p>
     *
     * @param stepOptionId Represents the step option id
     * @return {@link StepOption} An entity which holds the attributes of {@link StepOption}
     */
    StepOption getStepOption(final Long stepOptionId);

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link StepOption} for the given ids
     * </p>
     *
     * @param stepOptionIds Represents the {@link Set} of {@link StepOption} id
     * @return {@link Collection} of {@link StepOption}
     */
    Collection<StepOption> getStepOptions(final Set<Long> stepOptionIds);

    /**
     * <p>
     * Sets the given the {@link StepOption} into cache
     * </p>
     *
     * @param stepOption Represents the {@link StepOption}
     */
    void setStepOption(final StepOption stepOption);

    /**
     * <p>
     * Sets the {@link Collection} of {@link StepOption} into the cache
     * </p>
     *
     * @param stepOptions Represents the {@link Collection} of {@link StepOption}
     */
    void setStepOptions(final Collection<StepOption> stepOptions);

    /**
     * <p>
     * Retrieves the step options for the given {@link Collection} of alias name
     * </p>
     *
     * @param aliasNames Represents the {@link Collection} alias name
     * @return {@link Map} of {@link StepOption}
     */
    Map<String, StepOption> getStepOptionsByAliasName(final Collection<String> aliasNames);
}
