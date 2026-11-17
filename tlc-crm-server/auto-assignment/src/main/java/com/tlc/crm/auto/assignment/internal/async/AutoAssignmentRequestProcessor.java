package com.tlc.crm.auto.assignment.internal.async;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.crm.auto.assignment.api.manager.AutoAssignmentManager;
import com.tlc.crm.auto.assignment.api.model.AssignmentModule;
import com.tlc.crm.auto.assignment.api.model.AssignmentRuleUser;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentRule;
import com.tlc.crm.auto.assignment.fields.AutoAssignmentAttributes;
import com.tlc.crm.auto.assignment.internal.event.AutoAssignmentEventData;
import com.tlc.crm.auto.assignment.internal.event.processor.AutoAssignmentEventProcessor;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.models.CrmModel;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import com.tlc.validator.TlcModel;

import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Processes incoming auto-assignment requests received through the message stream.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class AutoAssignmentRequestProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoAssignmentRequestProcessor.class);

    private final AutoAssignmentManager autoAssignmentManager;
    private final Executor executor;
    private final OrganizationService organizationService;
    private final UserService iamUserService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;

    AutoAssignmentRequestProcessor(final Executor executor) {
        this.autoAssignmentManager = AutoAssignmentManager.getInstance();
        this.executor = executor;
        this.organizationService = OrganizationService.getInstance();
        this.iamUserService = UserService.getInstance();
        this.crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final String requestId = streamMessage.id();
        LOGGER.info("Auto assignment: Assignment execution request received by the consumer. Id : {}", requestId);

        final JsonObject jsonObject = Json.object(streamMessage.data());
        final Source source = Source.get(jsonObject.getString(AutoAssignmentAttributes.SOURCE));
        final long orgId = jsonObject.getLong(AutoAssignmentAttributes.ORG_ID);
        final AssignmentModule assignmentModule = AssignmentModule.getBySource(source);
        final long currentTime = System.currentTimeMillis();

        if (autoAssignmentManager.exists(orgId, assignmentModule)) {
            executor.execute(() -> {

                try {
                    final Collection<Long> modelIds = new HashSet<>();
                    final long userId = jsonObject.getLong(AutoAssignmentAttributes.USER_ID);

                    jsonObject.getJsonArray(AutoAssignmentAttributes.MODEL_IDS).forEach(modelId -> modelIds.add((Long) modelId));

                    final AutoAssignmentEventProcessor<?> assignmentEventProcessor = AutoAssignmentEventProcessor.getInstance(source);
                    final Collection<TlcModel> models = (Collection<TlcModel>) assignmentEventProcessor.fetchModels(orgId, modelIds);

                    LOGGER.info("Auto assignment: Successfully fetched the models for request Id: {} in {} ms. Org Id: {}, Source: {}", requestId, System.currentTimeMillis() - currentTime, orgId, source);
                    final Collection<AutoAssignmentRule> autoAssignmentRules = autoAssignmentManager.getByAssignmentModule(orgId, assignmentModule);
                    final AutoAssignmentEventData autoAssignmentEventData = new AutoAssignmentEventData(orgId, userId, models, unused -> streamMessage.clean());
                    final Set<Long> userIds = collectUserIds(models, autoAssignmentRules, userId);
                    final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                        /**
                         * {@inheritDoc}
                         *
                         * @param users Response from the consumer.
                         */
                        @Override
                        public void onSuccess(final Collection<User> users) {
                            final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                            autoAssignmentEventData.setUserMap(userMap);
                            executor.execute(() -> {

                                try {
                                    assignmentEventProcessor.process(autoAssignmentEventData, autoAssignmentRules);
                                } catch (Throwable exception) {
                                    streamMessage.clean();
                                    LOGGER.error("Auto assignment: Error occurred in assignment event process. RequestId: {}, Reason: {}, Stack: ", requestId, exception.getMessage(), exception);
                                }
                            });
                        }

                        /**
                         * {@inheritDoc}
                         *
                         * @param throwable Cause of the failure.
                         */
                        @Override
                        public void onFailure(final Throwable throwable) {
                            LoggerUtil.logError(LOGGER, throwable, "Auto assignment: User retrieval failed for OrgId: {} RequestId: {}, Reason: {}", orgId, requestId, throwable.getMessage());
                        }
                    };
                    iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), crmUserService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).toList(), usersFetchCallBack);
                } catch (Throwable throwable) {
                    streamMessage.clean();
                    LoggerUtil.logError(LOGGER, throwable, "Auto assignment: Assignment execution failed for request Id: {}. Org Id: {}, Source: {}, Reason: {}", requestId, orgId, source, throwable.getMessage());
                }
            });
        } else {
            streamMessage.clean();
            LOGGER.info("Auto assignment: Assignment request completed for Id: {} in {} ms", requestId, System.currentTimeMillis() - currentTime);
        }
    }

    /**
     * Collects and returns a set of user IDs that should participate in the
     * auto-assignment flow based on the provided TLC models and assignment rules.
     *
     * @param models              The TLC models representing territory–level configurations; must not be null.
     * @param autoAssignmentRules The auto-assignment rules used to determine user eligibility; must not be null.
     * @param userId              An optional user ID that may be added to the result set; may be null.
     * @return A {@link Set} of user IDs eligible for auto-assignment, never null.
     */
    private Set<Long> collectUserIds(final Collection<TlcModel> models, final Collection<AutoAssignmentRule> autoAssignmentRules, final Long userId) {
        final Set<Long> userIds = new HashSet<>();

        userIds.add(userId);
        models.forEach(model -> userIds.addAll(((CrmModel) model).userIds()));

        for (final AutoAssignmentRule autoAssignmentRule : autoAssignmentRules) {

            for (final AssignmentRuleUser assignmentRuleUser : autoAssignmentRule.getAssignmentRuleUsers()) {
                userIds.add(assignmentRuleUser.getUserId());
            }
        }
        return userIds;
    }
}
