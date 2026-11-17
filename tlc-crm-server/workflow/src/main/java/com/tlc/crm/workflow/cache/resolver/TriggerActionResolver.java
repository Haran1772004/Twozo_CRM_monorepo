package com.tlc.crm.workflow.cache.resolver;

import com.tlc.crm.workflow.api.model.TriggerAction;
import com.tlc.crm.workflow.cache.resolver.internal.TriggerActionResolverImpl;

import java.util.Collection;

/**
 * <p>
 * Offers services to retrieve the {@link TriggerAction} from the {@link com.tlc.cache.Cache}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface TriggerActionResolver {

    /**
     * Gets the instance of {@link TriggerActionResolver}.
     *
     * @return The {@link TriggerActionResolver}
     */
    static TriggerActionResolver getInstance() {
        return TriggerActionResolverImpl.getInstance();
    }

    /**
     * Gets the {@link TriggerAction} by its id.
     *
     * @param triggerSourceId Represents the trigger action source id.
     * @return {@link TriggerAction} which contains the trigger action source info.
     */
    TriggerAction get(final Short triggerSourceId);

    /**
     * Gets the collection of {@link TriggerAction}.
     *
     * @return {@link Collection} of {@link TriggerAction}.
     */
    Collection<TriggerAction> getAll();

}
