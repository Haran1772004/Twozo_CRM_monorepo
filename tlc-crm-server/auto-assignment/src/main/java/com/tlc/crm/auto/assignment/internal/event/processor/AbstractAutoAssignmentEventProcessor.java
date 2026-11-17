package com.tlc.crm.auto.assignment.internal.event.processor;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.crm.auto.assignment.api.model.AssignmentRuleUser;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaSet;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentRule;
import com.tlc.crm.auto.assignment.internal.event.AutoAssignmentEventData;
import com.tlc.crm.auto.assignment.internal.event.evaluate.AutoAssignmentCriteriaEvaluator;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.iam.common.models.User;
import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * Base implementation of the {@link AutoAssignmentEventProcessor} interface that
 * provides common functionality shared across different auto-assignment event processors.
 *
 * @param <T> the type of the event payload to be processed
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
abstract class AbstractAutoAssignmentEventProcessor<T> implements AutoAssignmentEventProcessor<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAutoAssignmentEventProcessor.class);

    private final Source source;
    private final Executor executor;
    private final AutoAssignmentCriteriaEvaluator assignmentCriteriaEvaluator;
    private final OwnerAssignmentService ownerAssignmentService;

    protected AbstractAutoAssignmentEventProcessor(final Source source) {
        final String[] poolNameAndSize = ConfHolder.getInstance().getProperties("auto-assignment").getConfig("event.delegator.pool").split(":");
        this.executor = ExecutorsHandler.createTaskExecutor(Integer.parseInt(poolNameAndSize[1]), poolNameAndSize[0], false);
        this.source = source;
        this.assignmentCriteriaEvaluator = AutoAssignmentCriteriaEvaluator.getInstance();
        this.ownerAssignmentService = OwnerAssignmentService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param autoAssignmentEventData Additional metadata required to process the event.
     * @param autoAssignmentRules     Refers the collection {@code AutoAssignmentRule}.
     */
    @Override
    public void process(final AutoAssignmentEventData autoAssignmentEventData, final Collection<AutoAssignmentRule> autoAssignmentRules) {
        final long orgId = autoAssignmentEventData.getOrgId();
        final Map<Long, User> userMap = autoAssignmentEventData.getUserMap();

        for (final TlcModel model : autoAssignmentEventData.models()) {
            executor.execute(() -> {

                for (final AutoAssignmentRule autoAssignmentRule : autoAssignmentRules) {

                    for (final AssignmentRuleUser assignmentRuleUser : autoAssignmentRule.getAssignmentRuleUsers()) {
                        final CrmUser crmUser = assignmentRuleUser.getCrmUser();

                        crmUser.setUser(userMap.get(crmUser.getIamId()));
                    }

                    final Long modelId = model.id();
                    final Long assignmentRuleId = autoAssignmentRule.id();

                    LOGGER.info("Auto assignment: Trigger execution initiated for Org ID: {}, Model ID: {}, Assignment ID: {}, Source: {}", orgId, modelId, assignmentRuleId, source);

                    try {
                        final Collection<AutoAssignmentCriteriaSet> assignmentCriteriaSet = autoAssignmentRule.getCriteriaSets();

                        if (assignmentCriteriaSet.isEmpty()) {
                            ownerAssignmentService.assign(orgId, model, autoAssignmentRule, source);
                        } else {
                            try {
                                LOGGER.info("Auto assignment: Expression evaluation process initiated for Org ID: {}, Model ID: {}, Assignment ID: {}, Source: {}", orgId, modelId, assignmentRuleId, source);
                                final boolean status = assignmentCriteriaEvaluator.evaluate(model, assignmentCriteriaSet, source);

                                LOGGER.info("Auto assignment: Expression evaluation process completed with Status: {} for Org ID: {}, Model ID: {}, Assignment ID: {}, Source: {}", status, orgId, modelId, assignmentRuleId, source);

                                if (status) {
                                    ownerAssignmentService.assign(orgId, model, autoAssignmentRule, source);
                                }
                            } catch (Throwable cause) {
                                autoAssignmentEventData.clean();
                                LoggerUtil.logError(LOGGER, cause, "Auto assignment: Expression evaluation failed for Model ID: {}, Assignment ID: {}, Source: {}, Reason: {}", modelId, assignmentRuleId, source, cause.getMessage(), cause);
                            }
                        }
                        autoAssignmentEventData.clean();
                    } catch (Exception exception) {
                        autoAssignmentEventData.clean();
                        LoggerUtil.logError(LOGGER, exception, "Auto assignment: Error occurred while executing the rule. Model ID: {}, Assignment ID: {}, Source: {}, Reason: {}", modelId, assignmentRuleId, source, exception.getMessage(), exception);
                    }
                }
            });
        }
    }

}