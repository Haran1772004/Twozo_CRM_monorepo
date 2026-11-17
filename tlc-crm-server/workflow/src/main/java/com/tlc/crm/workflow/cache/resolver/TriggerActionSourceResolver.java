package com.tlc.crm.workflow.cache.resolver;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.cache.resolver.internal.TriggerActionSourceResolverImpl;

import java.util.Collection;

/**
 * <p>
 * Offers services to retrieve the {@link TriggerActionSource} from the {@link com.tlc.cache.Cache}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface TriggerActionSourceResolver {

    /**
     * Gets the instance of {@link TriggerActionSourceResolver}.
     *
     * @return The {@link TriggerActionSourceResolver}
     */
    static TriggerActionSourceResolver getInstance() {
        return TriggerActionSourceResolverImpl.getInstance();
    }

    /**
     * Gets the {@link TriggerActionSource} by its id.
     *
     * @param triggerActionSourceId Represents the trigger action source id.
     * @return {@link TriggerActionSource} which contains the trigger action source info.
     */
    TriggerActionSource get(final Long triggerActionSourceId);

    /**
     * Gets the collection of {@link TriggerActionSource}.
     *
     * @return {@link Collection} of {@link TriggerActionSource}.
     */
    Collection<TriggerActionSource> getAll();

}
