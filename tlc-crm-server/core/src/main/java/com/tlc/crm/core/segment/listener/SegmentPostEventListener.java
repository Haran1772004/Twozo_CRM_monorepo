package com.tlc.crm.core.segment.listener;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.segment.models.SegmentCondition;
import com.tlc.crm.core.segment.models.SegmentConditionCriteria;
import com.tlc.crm.core.segment.models.SegmentConditionGroup;
import com.tlc.crm.core.segment.service.SegmentRecomputeService;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.notification.app.Event;
import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

/**
 * <p>
 * Post-event listener responsible for publishing segment recalculation
 * events after segment create/update/refresh operations complete.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class SegmentPostEventListener implements PostEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentPostEventListener.class);

    private final SegmentRecomputeService segmentRecomputeService;

    private SegmentPostEventListener() {
        this.segmentRecomputeService = SegmentRecomputeService.getInstance();
    }

    private static final class Instance {
        private static final SegmentPostEventListener INSTANCE = new SegmentPostEventListener();
    }

    /**
     * <p>
     * Returns the singleton instance of {@code SegmentPostEventListener}.
     * </p>
     *
     * @return the singleton instance
     */
    public static SegmentPostEventListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public Event event() {
        return SegmentEvent.SEGMENT;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        if (Objects.isNull(eventData)) {
            return;
        }
        final Collection<TlcModel> values = eventData.getValues();

        if (Objects.isNull(values) || values.isEmpty()) {
            return;
        }
        final Map<Long, TlcModel> oldValues = eventData.getPreviousValues();
        final Action action = eventData.getAction();

        for (final TlcModel model : values) {

            if (!(model instanceof Segment currentSegment)) {
                continue;
            }
            final Long orgId = currentSegment.orgId();
            final Long segmentId = currentSegment.id();

            try {
                if (action.isCreate()) {
                    segmentRecomputeService.publish(orgId, segmentId);
                } else if (action.isUpdate()) {
                    final Segment oldSegment = Objects.isNull(oldValues) ? null : (Segment) oldValues.get(segmentId);

                    if (hasSegmentGroupChanges(oldSegment, currentSegment)) {
                        segmentRecomputeService.publish(orgId, segmentId);
                    }
                }
            } catch (final Throwable throwable) {
                LOGGER.warn("Segment post listener: failed to publish recompute event for segment {} org {} : {}",
                        segmentId, orgId, throwable.getMessage());
            }
        }
    }

    /**
     * <p>
     * Checks whether segment groups have changed between two segment instances
     * by performing a deep comparison of group structures, conditions, and criteria.
     * </p>
     *
     * @param oldSegment     the previous segment state, may be {@code null}
     * @param currentSegment the current segment state
     * @return {@code true} if groups have changed, {@code false} otherwise
     */
    private boolean hasSegmentGroupChanges(final Segment oldSegment,
                                           final Segment currentSegment) {
        return !areGroupsEqual(oldSegment.getSegmentConditionGroups(), currentSegment.getSegmentConditionGroups());
    }

    /**
     * <p>
     * Compares two collections of {@link SegmentConditionGroup} for structural equality,
     * including all nested conditions and criteria. Uses iterator-based comparison
     * to minimize overhead when collections are not Lists.
     * </p>
     *
     * @param oldGroups the previous segment groups collection
     * @param newGroups the current segment groups collection
     * @return {@code true} if groups are structurally equal, {@code false} otherwise
     */
    private boolean areGroupsEqual(final Collection<SegmentConditionGroup> oldGroups,
                                   final Collection<SegmentConditionGroup> newGroups) {
        if (oldGroups == newGroups) {
            return true;
        }

        if (Objects.isNull(oldGroups) || Objects.isNull(newGroups)
            || oldGroups.size() != newGroups.size()) {
            return false;
        }
        final Iterator<SegmentConditionGroup> oldIterator = oldGroups.iterator();
        final Iterator<SegmentConditionGroup> newIterator = newGroups.iterator();

        while (oldIterator.hasNext()) {
            if (!isGroupEqual(oldIterator.next(), newIterator.next())) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * Compares two {@link SegmentConditionGroup} objects for equality by examining
     * connector and nested conditions.
     * </p>
     *
     * @param oldGroup the previous segment group
     * @param newGroup the current segment group
     * @return {@code true} if groups are equal, {@code false} otherwise
     */
    private boolean isGroupEqual(final SegmentConditionGroup oldGroup,
                                 final SegmentConditionGroup newGroup) {
        if (oldGroup == newGroup) {
            return true;
        }

        if (Objects.isNull(oldGroup) || Objects.isNull(newGroup)
                || (oldGroup.getConnector() != newGroup.getConnector())) {
            return false;
        }

        return areConditionsEqual(oldGroup.getConditions(), newGroup.getConditions());
    }

    /**
     * <p>
     * Compares two collections of {@link SegmentCondition} for equality
     * by examining type, connector, and criteria condition. Uses iterator-based
     * comparison to minimize overhead.
     * </p>
     *
     * @param oldConditions the previous segment conditions collection
     * @param newConditions the current segment conditions collection
     * @return {@code true} if conditions are equal, {@code false} otherwise
     */
    private boolean areConditionsEqual(final Collection<SegmentCondition> oldConditions,
                                       final Collection<SegmentCondition> newConditions) {
        if (oldConditions == newConditions) {
            return true;
        }

        if (Objects.isNull(oldConditions) || Objects.isNull(newConditions)
            || oldConditions.size() != newConditions.size()) {
            return false;
        }
        final Iterator<SegmentCondition> oldIterator = oldConditions.iterator();
        final Iterator<SegmentCondition> newIterator = newConditions.iterator();

        while (oldIterator.hasNext()) {
            if (!isConditionEquals(oldIterator.next(), newIterator.next())) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * Compares two {@link SegmentCondition} objects for equality by examining
     * type, connector, and nested criteria condition.
     * </p>
     *
     * @param oldCondition the previous segment condition
     * @param newCondition the current segment condition
     * @return {@code true} if conditions are equal, {@code false} otherwise
     */
    private boolean isConditionEquals(final SegmentCondition oldCondition,
                                      final SegmentCondition newCondition) {
        if (oldCondition == newCondition) {
            return true;
        }

        if (Objects.isNull(oldCondition) || Objects.isNull(newCondition)
            || (oldCondition.getType() != newCondition.getType())
            || (oldCondition.getConnector() != newCondition.getConnector())) {
            return false;
        }

        return areCriteriaConditionsEqual(
                oldCondition.getConditionCriteria(), newCondition.getConditionCriteria()
        );
    }

    /**
     * <p>
     * Compares two {@link SegmentConditionCriteria} objects for equality
     * by examining their nested {@link FilterCriteria}.
     * </p>
     *
     * @param oldCriteria the previous criteria condition
     * @param newCriteria the current criteria condition
     * @return {@code true} if criteria conditions are equal, {@code false} otherwise
     */
    private boolean areCriteriaConditionsEqual(final SegmentConditionCriteria oldCriteria,
                                               final SegmentConditionCriteria newCriteria) {
        if (oldCriteria == newCriteria) {
            return true;
        }

        if (Objects.isNull(oldCriteria) || Objects.isNull(newCriteria)) {
            return false;
        }

        return areFilterCriteriaEqual(
                oldCriteria.getFilterCriteria(), newCriteria.getFilterCriteria()
        );
    }

    /**
     * <p>
     * Compares two {@link FilterCriteria} objects for equality by examining
     * comparator, field, source, connector, and values.
     * </p>
     *
     * @param oldFilter the previous filter criteria
     * @param newFilter the current filter criteria
     * @return {@code true} if filter criteria are equal, {@code false} otherwise
     */
    private boolean areFilterCriteriaEqual(final FilterCriteria oldFilter,
                                           final FilterCriteria newFilter) {
        if (oldFilter == newFilter) {
            return true;
        }
        if (Objects.isNull(oldFilter) || Objects.isNull(newFilter)
            || (!Objects.equals(oldFilter.getComparator(), newFilter.getComparator()))
            || (!areCriteriaFieldsEqual(oldFilter.getCriteriaField(), newFilter.getCriteriaField()))
            || (!Objects.equals(oldFilter.getFieldSource(), newFilter.getFieldSource()))
            || (oldFilter.getConnector() != newFilter.getConnector())) {
            return false;
        }

        return areCriteriaValuesEqual(oldFilter.getCriteriaValues(), newFilter.getCriteriaValues());
    }

    /**
     * <p>
     * Compares two {@link CriteriaField} objects for equality by examining
     * field ID and alias name.
     * </p>
     *
     * @param oldField the previous criteria field
     * @param newField the current criteria field
     * @return {@code true} if criteria fields are equal, {@code false} otherwise
     */
    private boolean areCriteriaFieldsEqual(final CriteriaField oldField,
                                           final CriteriaField newField) {
        if (oldField == newField) {
            return true;
        }

        if (Objects.isNull(oldField) || Objects.isNull(newField)) {
            return false;
        }

        return Objects.equals(oldField.getId(), newField.getId())
               && Objects.equals(oldField.getAliasName(), newField.getAliasName());
    }

    /**
     * <p>
     * Compares two lists of {@link CriteriaValue} for equality by examining
     * size and individual value objects.
     * </p>
     *
     * @param oldValues the previous criteria values list
     * @param newValues the current criteria values list
     * @return {@code true} if criteria values are equal, {@code false} otherwise
     */
    private boolean areCriteriaValuesEqual(final List<CriteriaValue> oldValues,
                                           final List<CriteriaValue> newValues) {
        if (oldValues == newValues) {
            return true;
        }

        if (Objects.isNull(oldValues) || Objects.isNull(newValues)) {
            return false;
        }
        final int size = oldValues.size();

        if (size != newValues.size()) {
            return false;
        }

        for (int index = NumericalConstant.ZERO; index < size; index++) {

            if (!isCriteriaValueEquals(oldValues.get(index), newValues.get(index))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * Compares two {@link CriteriaValue} objects for equality by examining
     * value entity ID and the value itself.
     * </p>
     *
     * @param oldValue the previous criteria value
     * @param newValue the current criteria value
     * @return {@code true} if criteria values are equal, {@code false} otherwise
     */
    private boolean isCriteriaValueEquals(final CriteriaValue oldValue,
                                          final CriteriaValue newValue) {
        if (oldValue == newValue) {
            return true;
        }

        if (Objects.isNull(oldValue) || Objects.isNull(newValue)) {
            return false;
        }

        return Objects.equals(oldValue.getValueEntityId(), newValue.getValueEntityId())
               && Objects.equals(oldValue.getValue(), newValue.getValue());
    }

    /**
     * <p>
     * Represents the SEGMENT post-event type used for registering this
     * listener with the event dispatch framework.
     * </p>
     */
    public static final class SegmentEvent implements Event {

        public static final SegmentEvent SEGMENT = new SegmentEvent();
        private static final String NAME = "SEGMENT";

        private SegmentEvent() {
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         */
        @Override
        public String name() {
            return NAME;
        }
    }
}