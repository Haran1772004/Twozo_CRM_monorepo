package com.tlc.crm.workflow.cache.service.internal;

import com.tlc.crm.workflow.api.model.StepOption;
import com.tlc.crm.workflow.cache.resolver.StepOptionResolver;
import com.tlc.crm.workflow.cache.service.StepOptionService;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Provides the services to handle the {@link StepOption}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class StepOptionServiceImpl implements StepOptionService {

    private final StepOptionResolver stepOptionResolver;

    private StepOptionServiceImpl() {
        this.stepOptionResolver = StepOptionResolver.getInstance();
    }

    private static class Instance {
        private static final StepOptionServiceImpl INSTANCE = new StepOptionServiceImpl();
    }

    /**
     * <p>
     *  Gets the instance of {@link StepOptionServiceImpl}
     * </p>
     *
     * @return The {@link StepOptionServiceImpl}
     */
    public static StepOptionServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param stepOptionIds Represents the {@link Collection} of {@link StepOption} ids
     * @return true if all the ids are exits
     */
    @Override
    public boolean exists(final Collection<Long> stepOptionIds) {
        return stepOptionResolver.exists(stepOptionIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Represents the {@link Set} of step option ids
     * @return {@link Collection} of {@link StepOption}
     */
    @Override
    public Collection<StepOption> getStepOptions(final Set<Long> ids) {
        return stepOptionResolver.getStepOptions(ids);
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the {@link StepOption} id
     * @return {@link StepOption}
     */
    @Override
    public StepOption getStepOption(final Long id) {
        return stepOptionResolver.get(id);
    }

    /**
     * {@inheritDoc}
     *
     * @param aliasNames Represents the {@link Collection} of alias name
     * @return {@link Map} of {@link StepOption}
     */
    @Override
    public Map<String, StepOption> getStepOptionsByAliasName(final Collection<String> aliasNames) {
        return stepOptionResolver.getStepOptionsByAliasName(aliasNames);
    }
}
