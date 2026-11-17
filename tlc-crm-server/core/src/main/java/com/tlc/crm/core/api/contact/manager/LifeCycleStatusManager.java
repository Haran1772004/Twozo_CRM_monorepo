package com.tlc.crm.core.api.contact.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.core.api.contact.manager.impl.LifeCycleStatusManagerImpl;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Implements the {@link LifeCycleStatus} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface LifeCycleStatusManager extends ConfigManager<LifeCycleStatus> {

    static LifeCycleStatusManager getInstance() {
        return LifeCycleStatusManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the table of the lifecycle status.
     * </p>
     *
     * @return OMCOLifecycleStatus table.
     */
    Table getTable();

    /**
     * <p>
     * Loads the lifecycle status details into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param stageId         Represents the lifecycle stage id.
     * @param lifeCycleStatus An entity that holds the status attributes.
     * @param sourceContainer Holds the user defined and system field values of existing status.
     * @param targetContainer Holds the updated information of user defined and system field values.
     */
    void loadStatusIntoContainer(final boolean isCreateEvent, final Long stageId, final LifeCycleStatus lifeCycleStatus,
                                 final DataContainer sourceContainer, final DataContainer targetContainer);

    /**
     * <p>
     * Converts the row into {@link LifeCycleStatus}.
     * </p>
     *
     * @param row Represents the attributes of {@link LifeCycleStatus}.
     * @return {@link LifeCycleStatus}.
     */
    LifeCycleStatus convertRowToModel(final Row row);

    /**
     * <p>
     * Fetches the {@link LifeCycleStatus} for the dropdown actions.
     * </p>
     *
     * @param orgId            Represents the organization id.
     * @param lifecycleStageId Represents the lifecycle stage id.
     * @param limit            Which holds the limit configuration.
     * @param criteria         Which configure the criteria.
     * @return Collection of lifeCycleStatuses.
     */
    Collection<LifeCycleStatus> getLifeCycleStatusDropdownList(final Long orgId, final Long lifecycleStageId,
                                                               final DropdownLimit limit, final DropdownCriteria criteria);

    /**
     * <p>
     * Updates the order of the {@link LifeCycleStatus}.
     * </p>
     *
     * @param orgId        Represents the organisation id.
     * @param statusOrders Represents the orders of the lifecycle statuses.
     */
    void updateOrder(final long orgId, final Map<Short, Long> statusOrders);

    /**
     * <p>
     * Gets the {@link Collection} of lifecycle statuses by their names.
     * </p>
     *
     * @param orgId                 Represents the organization ID.
     * @param lifeCycleStatusValues Which contains the collection of lifecycle status names.
     * @return {@link Collection} of lifecycle statuses.
     */
    Collection<LifeCycleStatus> getLifeCycleStatuses(final Long orgId, final Collection<String> lifeCycleStatusValues);

    /**
     * <p>
     * Checks whether all the given life cycle statuses are exists or not.
     * </p>
     *
     * @param orgId              Represents the organisation id.
     * @param lifeCycleStatusIds Contains the collection of life cycle status ids.
     * @return true if all the life cycle statuses are exists.
     */
    boolean exists(final Long orgId, final Set<Long> lifeCycleStatusIds);

    /**
     * <p>
     * Checks whether the given life cycle status is present or not.
     * </p>
     *
     * @param orgId             Represents the organisation id.
     * @param lifeCycleStageId  Represents the life cycle stage id.
     * @param lifeCycleStatusId Represents the life cycle status id.
     * @return true if the life cycle status is present.
     */
    boolean exists(final Long orgId, final Long lifeCycleStageId, final Long lifeCycleStatusId);

    /**
     * <p>
     * Checks whether all the given life cycle statuses are exists or not.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param statuses Contains the collection of life cycle statuses.
     * @return true if all the life cycle statuses are exists.
     */
    boolean exists(final Long orgId, final Collection<LifeCycleStatus> statuses);

    /**
     * <p>
     * Gets the default lifecycle status id mapped for the given stage id.
     * </p>
     *
     * @param orgId            Represent the organization id.
     * @param lifecycleStageId Represent the lifecycle stage id.
     * @return {@link Long} of lifecycle status id.
     */
    Long getDefaultStatusId(final Long orgId, final Long lifecycleStageId);

    /**
     * <p>
     * Gets the default lifecycle status id mapped for the given stage id.
     * </p>
     *
     * @param orgId             Represent the organization id.
     * @param lifecycleStageIds Represent the lifecycle stage ids.
     * @return {@link Map} of lifecycle stage id and Status id.
     */
    Map<Long, Long> getDefaultStatusIdsByStage(final Long orgId, final Collection<Long> lifecycleStageIds);

    /**
     * <p>
     * Gets the default lifecycle status mapped for the given stage id.
     * </p>
     *
     * @param orgId                Represent the organization id.
     * @param lifecycleStageId     Represent the lifecycle stage id.
     * @param levelOfDefaultStatus Represent the level of the default status.
     *                             NOTE: The first status is the default. If the first status has to be deactivated,
     *                             the next status becomes the default, and this pattern continues through to the last status.
     * @return the default {@link LifeCycleStatus}.
     */
    LifeCycleStatus getDefaultStatus(final Long orgId, final Long lifecycleStageId, final int levelOfDefaultStatus);

    /**
     * <p>
     * Gets the default lifecycle status for the given stage.
     * </p>
     *
     * @param orgId            Represent the organization id.
     * @param lifecycleStageId Represent the lifecycle stage id.
     * @return the default {@link LifeCycleStatus}.
     */
    LifeCycleStatus getDefaultStatus(final long orgId, final Long lifecycleStageId);

    /**
     * <p>
     * Fetches the labels of the given statuses.
     * </p>
     *
     * @param orgId     Represents the organisation id.
     * @param statusIds Represents the lifecycle status ids.
     * @return Labeled statuses.
     */
    Map<Long, LifeCycleStatus.Label> getLabelsByStatus(final Long orgId, final Set<Long> statusIds);

    /**
     * <p>
     * Checks whether the given statuses are deletable or not.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param statusIds Represents the status ids.
     * @return True, if the given statuses are deletable or not.
     */
    boolean isDeletable(final Long orgId, final Set<Long> statusIds);

    /**
     * <p>
     * Fetches the count of the lifecycle statuses in the given stage.
     * </p>
     *
     * @param orgId   Represent the organization id.
     * @param stageId Represent the lifecycle stage id.
     * @return Count of the statuses.
     */
    long getStatusesCount(final Long orgId, final Long stageId);

    /**
     * <p>
     * Updates the order of the lifecycle status that follows the removed status.
     * </p>
     *
     * @param orgId              Represents the organisation id.
     * @param stageId            Represents the stage id.
     * @param removedStatusOrder Represents the removed status order.
     */
    void reArrangeStatusOrder(final long orgId, final long stageId, final short removedStatusOrder);

    /**
     * <p>
     * Fetches the status IDs excluding the specified statuses for the given stages.
     * </p>
     *
     * @param orgId     Represents the organisation id.
     * @param stageIds  Represents the stage ids.
     * @param statusIds Represents the status ids.
     * @return Remaining status ids.
     */
    Collection<Long> getRemainingStatuses(final Long orgId, final Set<Long> stageIds, final Set<Long> statusIds);

    /**
     * <p>
     * Fetches the total status count of the organisation.
     * </p>
     *
     * @param orgId            Represents an organisation.
     * @param lifeCycleStageId Represents the lifecycle stage id.
     * @param criteria         Holds the data retrieval criteria.
     * @return Total status count.
     */
    long getTotalRecordsCount(final Long orgId, final Long lifeCycleStageId, final DropdownCriteria criteria);

    /**
     * <p>
     * Retrieves the name of the lifecycle status by id.
     * </p>
     *
     * @param orgId             The ID of the organization to which the life cycle status belongs.
     * @param lifeCycleStatusId The ID of the life cycle status to retrieve.
     * @return The name of the lifecycle status.
     */
     String getLifeCycleStatusName(final long orgId, final Long lifeCycleStatusId);
}
