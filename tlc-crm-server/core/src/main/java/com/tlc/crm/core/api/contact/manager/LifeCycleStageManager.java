package com.tlc.crm.core.api.contact.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.core.api.contact.manager.impl.LifeCycleStageManagerImpl;
import com.tlc.crm.core.api.contact.model.LifeCycleRuleDetails;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Implements the {@link LifeCycleStage} related DB actions
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface LifeCycleStageManager extends ConfigManager<LifeCycleStage> {

    static LifeCycleStageManager getInstance() {
        return LifeCycleStageManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the table of the lifecycle stage.
     * </p>
     *
     * @return OMCOLifecycleStage table.
     */
    Table getTable();

    /**
     * <p>
     * Loads the stage details into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param lifeCycleStage  An entity that holds the stage attributes.
     * @param sourceContainer Holds the user defined and system field values of existing stage.
     * @param targetContainer Holds the updated information of user defined and system field values.
     */
    void loadLifecycleStageIntoContainer(final boolean isCreateEvent, final LifeCycleStage lifeCycleStage,
                                         final DataContainer sourceContainer, final DataContainer targetContainer);

    /**
     * <p>
     * Fetches the count of the lifecycle stages in the given organisation.
     * </p>
     *
     * @param orgId Represents the organisation id.
     * @return Count of the stages.
     */
    long getStagesCount(final long orgId);

    /**
     * <p>
     * Fetches the count of the lifecycle stages in the given organisation.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param criteria Represents the retrieval criteria.
     * @return Count of the stages.
     */
    long getStagesCount(final long orgId, final DropdownCriteria criteria);

    /**
     * <p>
     * Converts the row into {@link LifeCycleStage}.
     * </p>
     *
     * @param row Represents the attributes of {@link LifeCycleStage}.
     * @return {@link LifeCycleStage}.
     */
    LifeCycleStage convertRowToModel(final Row row);

    /**
     * <p>
     * Updates the order of the {@link LifeCycleStage}.
     * </p>
     *
     * @param orgId       Represents the organisation id.
     * @param stageOrders Represents the orders of the lifecycle stages.
     */
    void updateOrder(final long orgId, final Map<Short, Long> stageOrders);

    /**
     * <p>
     * Updates the active status of the {@link LifeCycleStage}.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param id       Represents the lifecycle stage id.
     * @param isActive A flag that indicates whether the lifecycle stage is active or not.
     */
    void updateStatus(final long orgId, final long id, final boolean isActive);

    /**
     * <p>
     * Fetches the {@link LifeCycleStage} for the dropdown actions.
     * </p>
     *
     * @param orgId                           Organization id.
     * @param applyCriteriaForLifeCycleStatus Flag to identify whether to apply the criteria for lifecycle stage or lifecycle status
     * @param limit                           Which holds the limit configuration.
     * @param criteria                        Which configure the criteria.
     * @return Collection of lifeCycleStages.
     */
    Collection<LifeCycleStage> getLifeCycleStageDropdownList(final Long orgId, final boolean applyCriteriaForLifeCycleStatus, final DropdownLimit limit,
                                                             final DropdownCriteria criteria);

    /**
     * <p>
     * Gets the {@link Collection} of lifecycle stages by their names.
     * </p>
     *
     * @param orgId                Represents the organization ID.
     * @param lifeCycleStageValues Which contains the collection of lifecycle stage names.
     * @return {@link Collection} of lifecycle stages.
     */
    Collection<LifeCycleStage> getLifeCycleStages(final Long orgId, final Collection<String> lifeCycleStageValues);

    /**
     * <p>
     * Checks whether all the given life cycle stages are exists or not.
     * </p>
     *
     * @param orgId             Represents the organisation id.
     * @param lifeCycleStageIds Contains the collection of life cycle stage ids.
     * @return true if all the life cycle stages are exists.
     */
    boolean exists(final Long orgId, final Set<Long> lifeCycleStageIds);

    /**
     * <p>
     * Checks whether all the given life cycle stages are exists or not.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param stages Contains the collection of life cycle stages.
     * @return true if all the life cycle stages are exists.
     */
    boolean exists(final Long orgId, final Collection<LifeCycleStage> stages);

    /**
     * <p>
     * Gets the default lifecycle stage id mapped for the given org id.
     * </p>
     *
     * @param orgId Represent the organization id.
     * @return {@link Long} of lifecycle stage id.
     */
    Long getDefaultStageId(final Long orgId);

    /**
     * <p>
     * Gets the default lifecycle stage id mapped for the given org id.
     * </p>
     *
     * @param orgId               Represent the organization id.
     * @param levelOfDefaultStage Represent the level of the default stage.
     *                            NOTE: The first stage is the default. If the first stage has to be deactivated,
     *                            the next stage becomes the default, and this pattern continues through to the last stage.
     * @return {@link Long} of lifecycle stage id.
     */
    Long getDefaultStageId(final Long orgId, final int levelOfDefaultStage);

    /**
     * <p>
     * Gets the default lifecycle stage for the given organization.
     * </p>
     *
     * @param orgId Represent the organization id.
     * @return the default {@link LifeCycleStage}.
     */
    LifeCycleStage getDefaultStage(final long orgId);

    /**
     * <p>
     * Fetches the lifecycle stages and their statuses by using the given labels.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param labels Represents the labels of the lifecycle status.
     * @return Map of stage ids and their statuses.
     */
    Map<Long, Collection<Long>> getStagesByLabels(final long orgId, final Collection<LifeCycleStatus.Label> labels);

    /**
     * <p>
     * Updates the rules of the lifecycle stage changes.
     * </p>
     *
     * @param orgId                Represents the organisation id.
     * @param lifeCycleRuleDetails Holds the details of the lifecycle stage rules.
     */
    void updateRules(final long orgId, final Collection<LifeCycleRuleDetails> lifeCycleRuleDetails);

    /**
     * <p>
     * Fetches the collection of {@link LifeCycleRuleDetails} of the organisation.
     * </p>
     *
     * @param orgId Represents the organisation id.
     * @return Collection of {@link LifeCycleRuleDetails}.
     */
    Collection<LifeCycleRuleDetails> getRules(final long orgId);

    /**
     * <p>
     * Fetches the {@link LifeCycleRuleDetails} of the organisation based on {@link LifeCycleRuleDetails.LifeCycleRuleType}.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param ruleType Represents the type of the lifecycle stage rule.
     * @return {@link LifeCycleRuleDetails}.
     */
    Optional<LifeCycleRuleDetails> getRule(final long orgId, final LifeCycleRuleDetails.LifeCycleRuleType ruleType);

    /**
     * <p>
     * Fetches all the available {@link LifeCycleStage} of an organisation.
     * </p>
     *
     * @param orgId Represents the organisation id.
     * @return Collection of {@link LifeCycleStage}.
     */
    Collection<LifeCycleStage> getAll(final Long orgId);

    /**
     * <p>
     * Checks whether the given stage is last stage or not.
     * </p>
     *
     * @param orgId   Represents the organisation id.
     * @param stageId Represents the stage id.
     * @return A flag that indicates whether the given stage is last stage or not.
     */
    boolean isLastStage(final Long orgId, final Long stageId);

    /**
     * <p>
     * Checks whether the given stage is de-activatable or not.
     * </p>
     *
     * @param orgId   Represents the organisation id.
     * @param stageId Represents the stage id.
     * @return A flag that indicates whether the given stage is de-activatable or not.
     */
    boolean isDeactivatableStage(final Long orgId, final Long stageId);

    /**
     * <p>
     * Checks whether the given stages are system default or custom stages.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param stageIds Represents the stage ids.
     * @return True, if the given stages are system default stages.
     */
    boolean isSystemStages(final Long orgId, final Set<Long> stageIds);

    /**
     * <p>
     * Updates the order of the lifecycle stage that follows the removed stage.
     * </p>
     *
     * @param orgId             Represents the organisation id.
     * @param removedStageOrder Represents the removed stage order.
     */
    void reArrangeStageOrders(final long orgId, final short removedStageOrder);

    /**
     * <p>
     * Checks whether the given stage has labeled statuses or not.
     * </p>
     *
     * @param orgId   Represents the organisation id.
     * @param stageId Represents the stage id.
     * @param label   Represents the label of the status.
     * @return True, if the given stage has labeled statuses.
     */
    boolean isLabeledStatusExists(final Long orgId, final Long stageId, final LifeCycleStatus.Label label);

    /**
     * <p>
     * Fetches the total stages count of the organisation.
     * </p>
     *
     * @param orgId    Represents an organisation.
     * @param criteria Holds the data retrieval criteria.
     * @return Total status count.
     */
    long getTotalRecordsCount(final Long orgId, final DropdownCriteria criteria);

    /**
     * <p>
     * Retrieves the name of the lifecycle stage by id.
     * </p>
     *
     * @param orgId            The ID of the organization to which the life cycle status belongs.
     * @param lifeCycleStageId The ID of the life cycle status to retrieve.
     * @return The name of the lifecycle stage.
     */
    String getLifeCycleStageName(final long orgId, final Long lifeCycleStageId);

    /**
     * <p>
     * Fetches the collections of {@link LifeCycleStage} for the given org id
     * </p>
     *
     * @param orgId Represents an organization id
     * @param start Represents the starting index
     * @param limit Represents the maximum number of records to fetch
     * @return The collection of {@link LifeCycleStage}
     */
    Collection<LifeCycleStage> getLifeCycleStages(final long orgId, final int start, final int limit);
}
