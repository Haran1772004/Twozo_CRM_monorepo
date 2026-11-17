package com.tlc.crm.workflow.cache.service.internal;

import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.TriggerActionField;
import com.tlc.crm.workflow.cache.resolver.TriggerActionFieldResolver;
import com.tlc.crm.workflow.cache.service.TriggerActionFieldService;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the implementation for the services offered by {@link TriggerActionFieldService}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see TriggerActionFieldService
 */
public class TriggerActionFieldServiceImpl implements TriggerActionFieldService {

    private final TriggerActionFieldResolver triggerActionFieldResolver;

    private TriggerActionFieldServiceImpl() {
        this.triggerActionFieldResolver = TriggerActionFieldResolver.getInstance();
    }

    private static class Instance {
        private static final TriggerActionFieldServiceImpl INSTANCE = new TriggerActionFieldServiceImpl();
    }

    /**
     * Gets the instance of {@link TriggerActionFieldServiceImpl}.
     *
     * @return The {@link TriggerActionFieldServiceImpl}.
     */
    public static TriggerActionFieldServiceImpl getInstance() {
        return TriggerActionFieldServiceImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionId      Represents the trigger action id
     * @param triggerActionFieldId Represents the {@link TriggerActionField} id
     * @return the {@link TriggerActionField}
     */
    @Override
    public TriggerActionField get(final Long triggerActionId, final Long triggerActionFieldId) {
        final Collection<TriggerActionField> triggerActionFields = triggerActionFieldResolver.getTriggerActionFields(triggerActionId);

        return triggerActionFields.isEmpty() ? null : triggerActionFields.stream().filter(triggerActionField -> Objects.equals(triggerActionField.getFieldId(), triggerActionFieldId)).findFirst().orElseGet(() -> null);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionId Represents the source
     * @return A map containing trigger action field  ids as keys and corresponding {@link TriggerActionField} objects as values.
     */
    @Override
    public Collection<TriggerActionField> get(final Long triggerActionId) {
        return triggerActionFieldResolver.getTriggerActionFields(triggerActionId);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionIds Which contains the collection of trigger action ids.
     * @return If all the trigger action ids exists it will return true, otherwise it will return false.
     */
    @Override
    public boolean exists(final Collection<Long> triggerActionIds) {
        if (triggerActionIds.isEmpty()) {
            return false;
        } else {
            final Map<Long, Collection<TriggerActionField>> triggerActionFieldMap = triggerActionFieldResolver.getAll();

            return Objects.nonNull(triggerActionFieldMap) && triggerActionIds.stream().allMatch(triggerActionFieldMap::containsKey);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionId Represents the trigger action id.
     * @return If the trigger action exists it will return true, otherwise it will return false.
     */
    @Override
    public boolean exist(final Long triggerActionId) {
        return Objects.nonNull(get(triggerActionId));
    }

    /**
     * {@inheritDoc}
     *
     * @param targetFieldGroup    The {@link FieldGroup} to match when filtering ActionFields
     * @param triggerActionFields The collection of {@link TriggerActionField} to be filtered
     * @return A filtered collection containing only trigger action fields with the specified {@link FieldGroup}
     */
    @Override
    public Collection<TriggerActionField> groupActionFieldsByFieldGroup(final FieldGroup targetFieldGroup, final Collection<TriggerActionField> triggerActionFields) {
        return triggerActionFields.stream().filter(actionField -> Objects.equals(targetFieldGroup, actionField.getFieldGroup())).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionFields Represents the collection of {@link TriggerActionField} details
     * @return A map where {@link TriggerActionField} are grouped by the rowIndex attribute
     */
    public Map<Integer, List<TriggerActionField>> groupActionFieldsByConfig(final Collection<TriggerActionField> triggerActionFields) {
        return triggerActionFields.stream().sorted(Comparator.comparingInt(TriggerActionField::rowIndex)
                .thenComparing(TriggerActionField::order)).collect(Collectors.groupingBy(TriggerActionField::rowIndex));
    }
}
