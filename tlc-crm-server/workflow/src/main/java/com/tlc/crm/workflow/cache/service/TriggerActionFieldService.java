package com.tlc.crm.workflow.cache.service;

import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.TriggerActionField;
import com.tlc.crm.workflow.cache.service.internal.TriggerActionFieldServiceImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Provides a service for managing and retrieving {@link TriggerActionField} and related data
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface TriggerActionFieldService {

    /**
     * Gets the instance of {@link TriggerActionFieldService}
     *
     * @return {@link TriggerActionFieldService}
     */
    static TriggerActionFieldService getInstance() {
        return TriggerActionFieldServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link TriggerActionField} from the cache.
     * </p>
     *
     * @param triggerActionId      Represents the trigger action id
     * @param triggerActionFieldId Represents the {@link TriggerActionField} id
     * @return the {@link TriggerActionField}
     */
    TriggerActionField get(final Long triggerActionId, final Long triggerActionFieldId);

    /**
     * <p>
     * Retrieves the collection of {@link TriggerActionField} from the cache based on the provided trigger action id
     * </p>
     *
     * @param triggerActionId Represents the source
     * @return A map containing trigger action field  ids as keys and corresponding {@link TriggerActionField} objects as values.
     */
    Collection<TriggerActionField> get(final Long triggerActionId);

    /**
     * <p>
     * Checks whether all the trigger action are exist or not for the given trigger action ids.
     * </p>
     *
     * @param triggerActionIds Which contains the collection of trigger action ids.
     * @return If all the trigger action ids exists it will return true, otherwise it will return false.
     */
    boolean exists(final Collection<Long> triggerActionIds);

    /**
     * <p>
     * Checks whether all the trigger action are exist or not for the given trigger action id.
     * </p>
     *
     * @param triggerActionId Represents the trigger action id.
     * @return If the trigger action exists it will return true, otherwise it will return false.
     */
    boolean exist(final Long triggerActionId);

    /**
     * <p>
     * Groups a collection of {@link TriggerActionField} based on the specified {@link FieldGroup}.
     * </p>
     *
     * @param fieldGroup          The {@link FieldGroup} to match when filtering ActionFields
     * @param triggerActionFields The collection of {@link TriggerActionField} to be filtered
     * @return A filtered collection containing only trigger action fields with the specified {@link FieldGroup}
     */
    Collection<TriggerActionField> groupActionFieldsByFieldGroup(final FieldGroup fieldGroup, final Collection<TriggerActionField> triggerActionFields);

    /**
     * <p>
     * Groups {@link TriggerActionField} based on their configuration attributes
     * </p>
     *
     * @param triggerActionFields Represents the collection of {@link TriggerActionField} details
     * @return A map where {@link TriggerActionField} are grouped by the rowIndex attribute
     */
    Map<Integer, List<TriggerActionField>> groupActionFieldsByConfig(final Collection<TriggerActionField> triggerActionFields);
}
