package com.tlc.crm.workflow.cache.resolver;

import com.tlc.crm.workflow.api.model.TriggerActionEvent;
import com.tlc.crm.workflow.cache.resolver.internal.TriggerActionEventResolverImpl;

import java.util.Collection;

/**
 * <p>
 * Offers services to retrieve the {@link TriggerActionEvent} from the {@link com.tlc.cache.Cache}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface TriggerActionEventResolver {

    /**
     * Gets the instance of {@link TriggerActionEventResolver}.
     *
     * @return The {@link TriggerActionEventResolver}
     */
    static TriggerActionEventResolver getInstance() {
        return TriggerActionEventResolverImpl.getInstance();
    }

    /**
     * Gets the {@link TriggerActionEvent} by its id.
     *
     * @param triggerActionEventId Represents the trigger action event id.
     * @return {@link TriggerActionEvent} which contains the trigger action event info.
     */
    TriggerActionEvent get(final Long triggerActionEventId);

    /**
     * Gets the collection of {@link TriggerActionEvent}.
     *
     * @return {@link Collection} of {@link TriggerActionEvent}.
     */
    Collection<TriggerActionEvent> getAll();

}
