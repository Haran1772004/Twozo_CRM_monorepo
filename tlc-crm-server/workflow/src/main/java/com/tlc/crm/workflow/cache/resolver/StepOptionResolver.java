package com.tlc.crm.workflow.cache.resolver;

import com.tlc.crm.workflow.api.model.StepOption;
import com.tlc.crm.workflow.cache.resolver.internal.StepOptionResolverImpl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * The resolver responsible for retrieving and storing the details of {@link StepOption} from/to cache
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface StepOptionResolver {

    /**
     * Gets the instance of {@link WorkflowTemplateResolver}.
     *
     * @return The {@link WorkflowTemplateResolver}
     */
    static StepOptionResolver getInstance() {
        return StepOptionResolverImpl.getInstance();
    }

    /**
     * <p>
     * Get the {@link StepOption} for the given id
     * </p>
     *
     * @param id Represents the {@link StepOption} id
     * @return the {@link StepOption}
     */
    StepOption get(final Long id);

    /**
     * <p>
     * Gets the {@link Collection} of {@link StepOption} for the given step option ids
     * </p>
     *
     * @param ids Represents the {@link Set} of ids
     * @return {@link Collection} of {@link StepOption} for the given ids
     */
    Collection<StepOption> getStepOptions(final Set<Long> ids);

    /**
     * <p>
     * Checks whether the given id are exists are not
     * </p>
     *
     * @param ids Represent the {@link Collection} of {@link StepOption} id
     * @return true if it is present otherwise false
     */
    boolean exists(final Collection<Long> ids);

    /**
     * <p>
     * Gets the step options which is matched for the given {@link Collection} of alias name
     * </p>
     *
     * @param aliasNames {@link Collection} of alias name
     * @return {@link Map} of {@link StepOption}
     */
    Map<String, StepOption> getStepOptionsByAliasName(final Collection<String> aliasNames);
}
