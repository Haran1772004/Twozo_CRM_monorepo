package com.tlc.crm.workflow.v2.internal.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.handler.EventRequestHandler;
import com.tlc.crm.common.service.EventRequestService;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.messenger.api.chat.manager.ChatManager;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.messenger.api.conversation.manager.ConversationManager;
import com.tlc.crm.messenger.api.conversation.model.CrmMessage;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.sql.resource.OMSOCHAT;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.internal.trigger.action.tracker.ActionFailureCauseTracker;
import com.tlc.crm.workflow.v2.api.model.ActionConfiguration;
import com.tlc.crm.workflow.v2.internal.action.formatter.WAJsonFormatterV2;
import com.tlc.crm.workflow.v2.internal.execution.AsyncActionCompletionGateway;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;

import com.tlc.social.client.service.ChatService;

import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.Channel;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.RecipientProfile;

import com.tlc.sql.common.api.DataContainer;

import com.tlc.validator.ModelValidator;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles the WhatsApp actions for the workflow. Copied from {@code WAActionHandler}.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
final class WAActionHandlerV2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(WAActionHandlerV2.class);

    private final WAJsonFormatterV2 waJsonFormatter;
    private final ChatManager chatManager;
    private final ChatService chatService;
    private final ConversationManager conversationManager;
    private final WABusinessAccountManager waBusinessAccountManager;
    private final EventRequestHandler socialEventRequestHandler;
    private final ContactManager contactManager;
    private final ModelValidator modelValidator;
    private final AsyncActionCompletionGateway asyncActionCompletionGateway;

    private WAActionHandlerV2() {
        waJsonFormatter = WAJsonFormatterV2.getInstance();
        chatManager = ChatManager.getInstance();
        chatService = ChatService.getInstance();
        conversationManager = ConversationManager.getInstance();
        waBusinessAccountManager = WABusinessAccountManager.getInstance();
        socialEventRequestHandler = EventRequestService.get().getSocialEventRequestHandler();
        contactManager = ContactManager.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        asyncActionCompletionGateway = AsyncActionCompletionGateway.getInstance();
    }

    /**
     * Holds the instance of {@link WAActionHandlerV2}.
     */
    private static final class InstanceHolder {
        private static final WAActionHandlerV2 WA_ACTION_HANDLER = new WAActionHandlerV2();
    }

    /**
     * <p>
     * Gets the instance of {@link WAActionHandlerV2}.
     * </p>
     *
     * @return the {@link WAActionHandlerV2} instance.
     */
    static WAActionHandlerV2 getInstance() {
        return InstanceHolder.WA_ACTION_HANDLER;
    }

    ActionOutcome sendMessage(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                              final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        final Pair<TriggerSource, TlcModel> triggerModel = nodeExecutionContext.triggerSourceModel();
        final Long workflowActionId = actionConfiguration.id();
        final JsonObject waSendJson = waJsonFormatter.format(orgId, workflowActionId, actionConfiguration.getFieldMappingAsJson(), nodeExecutionContext, actionConfiguration.getTriggerActionSource(), actionFailureCauseTracker);
        final Collection<String> phoneNumbers = (Collection<String>) waSendJson.get(MessengerFields.RECIPIENT_PHONE_NUMBERS);

        if (phoneNumbers.isEmpty() || phoneNumbers.stream().anyMatch(phoneNumber -> !phoneNumber.startsWith(MessengerFields.COUNTRY_CODE_PREFIX))) {
            actionFailureCauseTracker.track(workflowActionId, WorkflowActionExecutionFailureCause.WHATSAPP_INVALID_RECIPIENT_PHONE_NUMBER);

            return ActionOutcome.syncFailure();
        }
        final Long crmBusinessPhoneNumberId = waSendJson.getLong(MessengerFields.BUSINESS_PHONE_NUMBER_ID);

        if (!waBusinessAccountManager.isPhoneNumberActive(orgId, crmBusinessPhoneNumberId)) {
            actionFailureCauseTracker.track(workflowActionId, WorkflowActionExecutionFailureCause.WHATSAPP_PHONE_NUMBER_INACTIVE);

            return ActionOutcome.syncFailure();
        }
        final Long contactId;
        final String contactFullName;

        if (TriggerSource.CONTACT == triggerModel.getKey()) {
            final Contact contact = (Contact) triggerModel.getValue();
            contactId = contact.id();
            contactFullName = contact.getFullName();
        } else {
            final Contact primaryContact = ((Deal) triggerModel.getValue()).getPrimaryContact();
            final String primaryContactFullName = primaryContact.getFullName();
            contactId = primaryContact.id();
            contactFullName = Objects.nonNull(primaryContactFullName) ? primaryContactFullName : contactManager.getContactFullName(orgId, contactId);
        }

        waBusinessAccountManager.getPhoneNumberId(orgId, crmBusinessPhoneNumberId)
                .ifPresent(phoneNumberId -> waSendJson.put(MessengerFields.BUSINESS_PHONE_NUMBER_ID, phoneNumberId));

        final long executionId = nodeExecutionContext.executionId();
        final long nodeId = nodeExecutionContext.nodeId();

        final EventCallback<JsonObject> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final JsonObject response) {
                socialEventRequestHandler.handle(() -> {
                    try {
                        if (response.containsKey(MessengerFields.ERRORS)) {
                            final String errorDetail = ((List<String>) response.get(MessengerFields.ERRORS)).get(NumericalConstant.ZERO);
                            final ErrorCode errorCode = ErrorCode.get(SocialErrorCodes.MESSAGE_SEND_ACTION_FAILED, errorDetail);

                            LoggerUtil.logError(LOGGER, errorCode, "Workflow: Failed to send whatsapp template message. Data validation error. OrgId: {}, Reason: {}", orgId, errorDetail);
                            asyncActionCompletionGateway.failAsyncAction(orgId, executionId, nodeId, WorkflowActionExecutionFailureCause.INVALID_DATA.getId());
                        } else {
                            final Long businessPhoneNumberId = waSendJson.getLong(MessengerFields.BUSINESS_PHONE_NUMBER_ID);

                            if (response.containsKey(MessengerFields.LIST)) {
                                final JsonArray responseArray = response.getJsonArray(MessengerFields.LIST);
                                final int responseSize = responseArray.size();
                                int failedEntries = NumericalConstant.ZERO;
                                CrmChat lastStoredChat = null;

                                for (int index = 0; index < responseSize; index++) {
                                    final JsonObject messageResponse = responseArray.getJsonObject(index);

                                    if (messageResponse.containsKey(MessengerFields.ERRORS)) {
                                        failedEntries++;
                                        final String errorDetail = ((List<String>) messageResponse.get(MessengerFields.ERRORS)).get(NumericalConstant.ZERO);
                                        final ErrorCode errorCode = ErrorCode.get(SocialErrorCodes.MESSAGE_SEND_ACTION_FAILED, errorDetail);

                                        LoggerUtil.logError(LOGGER, errorCode, "Workflow: Failed to send whatsapp template message. Data validation error. Org Id: {}, Reason: {}", orgId, errorDetail);
                                    } else {
                                        lastStoredChat = storeChat(orgId, userId, executionId, nodeId, businessPhoneNumberId, contactId, contactFullName, messageResponse);
                                    }
                                }

                                if (Objects.equals(responseSize, failedEntries)) {
                                    asyncActionCompletionGateway.failAsyncAction(orgId, executionId, nodeId, WorkflowActionExecutionFailureCause.INVALID_DATA.getId());
                                } else if (Objects.nonNull(lastStoredChat)) {
                                    asyncActionCompletionGateway.completeAsyncAction(orgId, executionId, nodeId, lastStoredChat);
                                }
                            } else {
                                final CrmChat crmChat = storeChat(orgId, userId, executionId, nodeId, businessPhoneNumberId, contactId, contactFullName, response);

                                if (Objects.nonNull(crmChat)) {
                                    asyncActionCompletionGateway.completeAsyncAction(orgId, executionId, nodeId, crmChat);
                                }
                            }
                        }
                    } catch (Throwable throwable) {
                        final String errorDetail = throwable.getMessage();

                        LoggerUtil.logError(LOGGER, throwable, "Workflow: Failed to send whatsapp template message, Internal error, OrgId: {}, Message: {}", orgId, errorDetail);
                        asyncActionCompletionGateway.failAsyncAction(orgId, executionId, nodeId, WorkflowActionExecutionFailureCause.UNIDENTIFIED_ERROR.getId());
                    }
                });
            }

            @Override
            public void onFailure(final Throwable cause) {
                final String errorDetail = cause.getMessage();

                LOGGER.error("Workflow: Failed to send whatsapp template message, OrgId: {}, Message: {}", orgId, errorDetail);
                asyncActionCompletionGateway.failAsyncAction(orgId, executionId, nodeId, WorkflowActionExecutionFailureCause.UNIDENTIFIED_ERROR.getId());
            }
        };

        chatService.sendMessage(waSendJson, eventCallback);

        return ActionOutcome.asyncPending();
    }

    private CrmChat storeChat(final Long orgId, final Long userId, final long executionId, final long nodeId,
                              final Long businessPhoneNumberId, final Long contactId, final String contactFullName,
                              final JsonObject messageResponse) {
        final JsonArray messageArray = messageResponse.getJsonArray(MessengerFields.MESSAGES);
        final int size = messageArray.size();
        final Collection<CrmMessage> messages = new ArrayList<>();
        final CrmChat crmChat = new CrmChat();
        final Long chatId = messageResponse.getLong(MessengerFields.CHAT_ID);
        final Optional<Long> optionalChatId = chatManager.getCrmChatId(orgId, chatId);

        if (optionalChatId.isPresent()) {
            crmChat.setCrmChatId(optionalChatId.get());
            crmChat.setNewChat(false);
        } else {
            final Contact contact = new Contact();
            final RecipientProfile recipientProfile = new RecipientProfile();

            recipientProfile.setName(String.join("|", contactFullName, messageResponse.getString(MessengerFields.RECIPIENT_PHONE_NUMBER)));

            crmChat.setProfile(recipientProfile);
            contact.setId(contactId);
            crmChat.setContact(contact);
            crmChat.setNewChat(true);
        }
        crmChat.setId(chatId);
        crmChat.setOrgId(orgId);

        for (int messageIndex = 0; messageIndex < size; messageIndex++) {
            final JsonObject messageResponseJson = messageArray.getJsonObject(messageIndex);
            final CrmMessage message = new CrmMessage();

            message.setId(messageResponseJson.getLong(MessengerFields.ID));
            message.setSentTime(messageResponseJson.getLong(MessengerFields.SENT_TIME));
            message.setMessageFlow(Message.MessageFlow.OUTBOUND);
            message.setOutboundMessageSource(CrmMessage.OutboundMessageSource.WORKFLOW);

            if (messageIndex == size - NumericalConstant.ONE) {
                message.setContent(messageResponseJson.getString(MessengerFields.LAST_MESSAGE));
                crmChat.setLastMessage(message);
            }
            messages.add(message);
        }
        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(crmChat), Group.Create.class);

        if (errors.isEmpty()) {
            final DataContainer targetContainer = DataContainer.create();

            if (crmChat.isNewChat()) {
                final Channel channel = new Channel();

                channel.setType(Channel.ChannelType.WHATSAPP_BUSINESS);
                channel.setName(messageResponse.getString(MessengerFields.CHANNEL_NAME));
                crmChat.setChannel(channel);
                waBusinessAccountManager.loadNewChat(orgId, crmChat, targetContainer, businessPhoneNumberId);
                chatManager.linkToEntity(orgId, userId, crmChat, crmChat.getContact().id(), OMSOCHAT.CONTACT_ID, Source.CONTACT, targetContainer);
            }
            conversationManager.saveMessages(orgId, userId, crmChat, messages, targetContainer);

            return crmChat;
        }
        LOGGER.error("Whatsapp: Chat creation failed. Validation error. Org Id:{}, Message:{}", orgId, errors.iterator().next());
        asyncActionCompletionGateway.failAsyncAction(orgId, executionId, nodeId, WorkflowActionExecutionFailureCause.INVALID_DATA.getId());

        return null;
    }
}
