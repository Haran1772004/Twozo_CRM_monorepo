package com.tlc.crm.workflow.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;

import com.tlc.crm.workflow.api.manager.StepOptionManager;
import com.tlc.crm.workflow.api.model.StepOption;
import com.tlc.crm.workflow.cache.StepOptionCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Provides the implementation for {@link StepOptionCache}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class StepOptionCacheImpl implements StepOptionCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(WorkflowComparatorCacheImpl.class);

    private final Cache<Long, StepOption> stepOptionCache;
    private final StepOptionManager stepOptionManager;

    private StepOptionCacheImpl() {
        this.stepOptionCache = CacheManager.getInstance().createCache();
        this.stepOptionManager = StepOptionManager.getInstance();
    }

    private static class Instance {
        private static final StepOptionCacheImpl INSTANCE = new StepOptionCacheImpl();
    }

    /**
     * Gets the {@link StepOptionCacheImpl} instance
     *
     * @return {@link StepOptionCacheImpl}
     */
    public static StepOptionCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param stepOptionId Represents the step option id
     * @return {@link StepOption} for the given step option id
     */
    @Override
    public StepOption getStepOption(final Long stepOptionId) {
        return stepOptionCache.get(stepOptionId);
    }

    /**
     * {@inheritDoc}
     *
     * @param stepOptionIds Represents the step option ids
     * @return {@link Collection} of {@link StepOption}
     */
    @Override
    public Collection<StepOption> getStepOptions(final Set<Long> stepOptionIds) {
        return stepOptionCache.getAll(stepOptionIds).values();
    }

    /**
     * {@inheritDoc}
     *
     * @param stepOption Represents the {@link StepOption}
     */
    @Override
    public void setStepOption(final StepOption stepOption) {
        stepOptionCache.put(stepOption.id(), stepOption);
    }

    /**
     * {@inheritDoc}
     *
     * @param stepOptions Represents the {@link Collection} of {@link StepOption}
     */
    @Override
    public void setStepOptions(final Collection<StepOption> stepOptions) {
        for (final StepOption stepOption : stepOptions) {
            stepOptionCache.put(stepOption.id(), stepOption);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param aliasNames Represents the {@link Collection} alias names
     * @return {@link Map} of {@link StepOption}
     */
    @Override
    public Map<String, StepOption> getStepOptionsByAliasName(final Collection<String> aliasNames) {
        final Map<String, StepOption> stepOptions = new HashMap<>();
        final Collection<StepOption> workflowStepOptions = new ArrayList<>();

        stepOptionCache.forEach((cache, stepOption) -> workflowStepOptions.add(stepOption));

        for (final StepOption stepOption : workflowStepOptions) {
            final String aliasName = stepOption.aliasName();

            if (aliasNames.contains(aliasName)) {
                stepOptions.put(aliasName, stepOption);
            }
        }

        return stepOptions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<StepOption> stepOptions = stepOptionManager.getAllStepOptions();

        for (final StepOption stepOption : stepOptions) {
            stepOptionCache.put(stepOption.id(), stepOption);
        }
        LOGGER.info("Workflow: Total number of step option data loaded into cache : {}", stepOptions.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        stepOptionCache.clear();
    }
}
