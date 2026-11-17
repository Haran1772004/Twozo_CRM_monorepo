package com.tlc.crm.zapier.internal.async;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.CrmModel;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.zapier.api.models.ZapEvent;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.event.ZapEventData;
import com.tlc.crm.zapier.internal.event.processor.ZapEventProcessor;

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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A consumer that consumes the zap trigger requests and triggers the various actions
 * based on the request parameters.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ZapTriggerEventProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZapTriggerEventProcessor.class);

    private final Executor triggerExecutor;
    private final OrganizationService organizationService;
    private final UserService iamUserService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;

    public ZapTriggerEventProcessor(final Executor triggerExecutor) {
        this.triggerExecutor = triggerExecutor;
        this.organizationService = OrganizationService.getInstance();
        this.iamUserService = UserService.getInstance();
        this.crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage stream message.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final String requestId = streamMessage.id();

        LOGGER.info("Zapier: Zapier trigger request received by the consumer. Request ID : {}", requestId);
        final JsonObject jsonObject = Json.object(streamMessage.data());
        final Source source = Source.get(jsonObject.getString(ZapAttributes.SOURCE));
        final Action action = Action.valueOf(jsonObject.getString(ZapAttributes.ACTION));
        final long orgId = jsonObject.getLong(ZapAttributes.ORG_ID);
        final long userId = jsonObject.getLong(ZapAttributes.USER_ID);
        final Optional<String> apiType = Optional.ofNullable(jsonObject.optString(ZapAttributes.API_TYPE, null));
        final Collection<Long> modelIds = new HashSet<>();

        try {
            jsonObject.getJsonArray(ZapAttributes.MODEL_IDS).forEach(modelId -> modelIds.add((Long) modelId));
            final ZapEventProcessor<?> zapEventProcessor = ZapEventProcessor.getInstance(source);
            final Collection<TlcModel> models = (Collection<TlcModel>) zapEventProcessor.fetchModels(orgId, modelIds);
            final Set<Long> userIds = new HashSet<>();

            userIds.add(userId);
            models.forEach(model -> userIds.addAll(((CrmModel) model).userIds()));

            final ZapEventData zapEventData = new ZapEventData(orgId, userId, models);

            zapEventData.setAction(action);
            zapEventData.setLocale(Locale.forLanguageTag(jsonObject.getString(ZapAttributes.LOCALE)));
            zapEventData.setSource(source);
            zapEventData.setZapEvent(apiType.isPresent() ? ZapEvent.getByType(apiType.get()) : ZapEvent.getBySource(source, action));

            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                /**
                 * {@inheritDoc}
                 *
                 * @param users Response from the consumer.
                 */
                @Override
                public void onSuccess(final Collection<User> users) {
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    zapEventData.setGroupedUsers(userMap);
                    triggerExecutor.execute(() -> {

                        try {
                            zapEventProcessor.process(zapEventData);
                        } catch (Throwable exception) {
                            LOGGER.error("Zapier: Error occurred in zap event process. RequestId: {}, Reason: {}, Stack: ", requestId, exception.getMessage(), exception);
                        } finally {
                            streamMessage.clean();
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
                    LoggerUtil.logError(LOGGER, throwable, "Zapier: User retrieval failed for OrgId: {} RequestId: {}, Reason: {}", orgId, requestId, throwable.getMessage());
                }
            };

            iamUserService.fetchUsers(organizationService.getIamOrgId(orgId),
                    crmUserService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).toList(), usersFetchCallBack);
        } catch (Throwable throwable) {
            LOGGER.error("Zapier: Zapier trigger request process failed RequestId: {}, OrgId: {}, Reason: {}, Stack: ", streamMessage.id(), orgId, throwable.getMessage(), throwable);
            streamMessage.clean();
        }
    }
}
