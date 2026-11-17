package com.tlc.crm.workflow.cache.resolver;

import com.tlc.crm.workflow.api.model.TriggerActionField;
import com.tlc.crm.workflow.cache.resolver.internal.TriggerActionFieldResolverImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Offers services to retrieve the {@link TriggerActionField} from the {@link com.tlc.cache.Cache}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface TriggerActionFieldResolver {

    /**
     * <p>
     * Gets the instance of {@link TriggerActionFieldResolver}
     * </p>
     *
     * @return {@link TriggerActionFieldResolver}
     */
    static TriggerActionFieldResolver getInstance() {
        return TriggerActionFieldResolverImpl.getInstance();
    }

    /**
     * <p>
     * Gets the import fields of given org ID and source, grouped by their IDs
     * </p>
     *
     * @param triggerActionId Represents the trigger action id
     * @return {@link Map} that containing the trigger action field id as keys and {@link TriggerActionField} as values
     */
    Collection<TriggerActionField> getTriggerActionFields(final Long triggerActionId);

    /**
     * <p>
     * Gets all the {@link TriggerActionField} from the cache.
     * </p>
     *
     * @return The map containing trigger action ids as keys and corresponding map of trigger action field ids to {@link TriggerActionField} objects as values
     */
    Map<Long, Collection<TriggerActionField>> getAll();

}
