package com.tlc.crm.workflow.cache.service;

import com.tlc.crm.workflow.api.model.StepOption;
import com.tlc.crm.workflow.cache.service.internal.StepOptionServiceImpl;

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
public interface StepOptionService {

    /**
     * <p>
     * Gets the instance of {@link StepOptionService}
     * </p>
     *
     * @return The {@link StepOptionService}
     */
    static StepOptionService getInstance() {
        return StepOptionServiceImpl.getInstance();
    }

    /**
     * <p>
     * Checks whether the given {@link StepOption} ids are exists or not
     * </p>
     *
     * @param stepOptionIds Represents the {@link Collection} of {@link StepOption} ids
     * @return true if all the ids are present
     */
    boolean exists(final Collection<Long> stepOptionIds);

    /**
     * <p>
     * Gets the {@link Collection} of {@link StepOption} for the given ids
     * </p>
     *
     * @param ids Represents the {@link Set} of step option ids
     * @return {@link Collection} of {@link StepOption}
     */
    Collection<StepOption> getStepOptions(final Set<Long> ids);

    /**
     * <p>
     * Gets the {@link StepOption} for the given id
     * </p>
     *
     * @param id Represents the {@link StepOption} id
     * @return {@link StepOption}
     */
    StepOption getStepOption(final Long id);

    /**
     * <p>
     * Gets the step options which is mapped for the given {@link Collection} of alias name
     * </p>
     *
     * @param aliasNames Represents the {@link Collection} of alias name
     * @return {@link Map} of {@link StepOption}
     */
    Map<String, StepOption> getStepOptionsByAliasName(final Collection<String> aliasNames);
}
