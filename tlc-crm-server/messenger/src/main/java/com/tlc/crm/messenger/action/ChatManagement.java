package com.tlc.crm.messenger.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.SecureEventAction;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.handler.EventRequestHandler;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.service.EventRequestService;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.template.PlaceHolderValueInterpreter;
import com.tlc.crm.core.validator.internal.ContactDataValidator;
import com.tlc.crm.core.validator.internal.DealDataValidator;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;

import com.tlc.crm.entity.service.internal.SecureFieldService;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.messenger.api.channel.manager.FBPageManager;
import com.tlc.crm.messenger.api.channel.manager.InstagramPageManager;
import com.tlc.crm.messenger.api.channel.model.ChannelState;
import com.tlc.crm.messenger.api.chat.manager.ChatManager;
import com.tlc.crm.messenger.api.chat.manager.ChatSourceManager;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.social.common.model.ChatPlaceHolder;
import com.tlc.crm.messenger.api.chat.model.CrmChatFilter;
import com.tlc.crm.messenger.api.conversation.manager.ConversationManager;
import com.tlc.crm.messenger.api.conversation.model.Conversation;
import com.tlc.crm.messenger.api.conversation.model.ConversationActivityTemplate.Type;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.messenger.api.conversation.model.CrmMessage;
import com.tlc.crm.messenger.api.conversation.model.ConversationActivity;
import com.tlc.crm.messenger.api.conversation.model.converter.ConversationConverter;
import com.tlc.crm.messenger.api.chat.converter.ChatConverter;
import com.tlc.crm.messenger.api.whatsapp.converter.WATemplateConverter;
import com.tlc.crm.messenger.fields.MessengerApiKeys;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.internal.status.CrmSocialErrorCodes;
import com.tlc.crm.messenger.internal.status.MessengerErrorCodes;
import com.tlc.crm.messenger.internal.status.whatsapp.WAErrorCodes;
import com.tlc.crm.messenger.page.ChatPageHandler;
import com.tlc.crm.messenger.page.ConversationPageHandler;
import com.tlc.crm.messenger.sql.resource.OMSOCHAT;

import com.tlc.crm.messenger.validator.group.MessengerValidatorGroup;
import com.tlc.crm.security.permission.annotation.authorizer.SocialChatAuthorizer;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;
import com.tlc.crm.notification.app.Event;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.social.client.service.ChatService;
import com.tlc.social.client.service.whatsapp.WABusinessAccountService;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.Channel;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.MessageSendEligibility;
import com.tlc.social.common.model.Platform;

import com.tlc.social.common.model.RecipientProfile;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tlc.crm.notification.app.event.NotificationEvent.APP_NOTIFICATION_EVENT;

/**
 * <p>
 * Manages messenger's chat related actions.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@WebAction(path = "/messenger/chat/mgmt")
@SocialChatAuthorizer()
public class ChatManagement extends SecureEventAction {

    private static final String CHAT_LIST_RETRIEVAL_FAILURE_MESSAGE = "i18n.messenger.chat.list.retrieval.failed";
    private static final String CONVERSATION_RETRIEVAL_FAILURE_MESSAGE = "i18n.messenger.conversation.load.failed";
    private static final String READ_RECEIPT_FAILURE_MESSAGE_KEY = "i18n.messenger.read.receipt.failed";
    private static final String USER_RETRIEVAL_FAILURE_MESSAGE_KEY = "i18n.messenger.user.retrieval.failed";
    private static final int INITIAL_CAPACITY = 32;
    private static final int CONTACT_FULL_NAME_FIELD_ID = NumericalConstant.NEGATIVE_ONE;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatManagement.class);

    private final ChatManager chatManager;
    private final ChatService chatService;
    private final ChatSourceManager chatSourceManager;
    private final EventRequestHandler socialEventRequestHandler;
    private final ChatConverter chatConverter;
    private final ConversationManager conversationManager;
    private final ConversationConverter conversationConverter;
    private final UserService userService;
    private final com.tlc.iam.client.UserService iamUserService;
    private final OrganizationService organizationService;
    private final FBPageManager fbChannelManager;
    private final InstagramPageManager instagramPageManager;
    private final WABusinessAccountManager waBusinessAccountManager;
    private final CrmUserManager crmUserManager;
    private final DealConverter dealConverter;
    private final ContactConverter contactConverter;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final ModelValidator modelValidator;
    private final ContactDataValidator contactDataValidator;
    private final DealDataValidator dealDataValidator;
    private final AttachmentService attachmentService;
    private final EventRequestHandler eventRequestHandler;
    private final WABusinessAccountService waBusinessAccountService;
    private final ContactManager contactManager;
    private final SecureFieldService fieldService;
    private final PlaceHolderValueInterpreter placeHolderValueInterpreter;
    private final WATemplateConverter waTemplateConverter;
    private final Collection<String> contactPlaceholderFieldNames;
    private final Collection<String> ownerFieldNames;

    public ChatManagement() {
        chatManager = ChatManager.getInstance();
        chatService = ChatService.getInstance();
        chatSourceManager = ChatSourceManager.getInstance();
        socialEventRequestHandler = EventRequestService.get().getSocialEventRequestHandler();
        chatConverter = ChatConverter.getInstance();
        conversationManager = ConversationManager.getInstance();
        conversationConverter = ConversationConverter.getInstance();
        userService = UserService.getInstance();
        iamUserService = com.tlc.iam.client.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        fbChannelManager = FBPageManager.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        dealConverter = DealConverter.getInstance();
        contactConverter = ContactConverter.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        contactDataValidator = ContactDataValidator.getInstance();
        dealDataValidator = DealDataValidator.getInstance();
        attachmentService = AttachmentService.getInstance();
        instagramPageManager = InstagramPageManager.getInstance();
        waBusinessAccountManager = WABusinessAccountManager.getInstance();
        eventRequestHandler = EventRequestService.get().getEventHandler();
        waBusinessAccountService = WABusinessAccountService.getInstance();
        contactManager = ContactManager.getInstance();
        fieldService = SecureFieldService.getInstance();
        placeHolderValueInterpreter = PlaceHolderValueInterpreter.getInstance();
        waTemplateConverter = WATemplateConverter.getInstance();
        contactPlaceholderFieldNames = Set.of(MessengerFields.FIRSTNAME, MessengerFields.LASTNAME, MessengerFields.FULL_NAME,
                MessengerFields.EMAILS, MessengerFields.PHONES, MessengerFields.COMPANY, MessengerFields.SALES_OWNER,
                MessengerFields.CREATED_BY, MessengerFields.UPDATED_BY);
        ownerFieldNames = Set.of(MessengerFields.CREATED_BY, MessengerFields.UPDATED_BY);
    }

    /**
     * <p>
     * Gets the org data store
     * </P>
     *
     * @param orgId Represents an organization id
     * @return The {@link OrgDataStore}
     */
    protected OrgDataStore getOrgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param request     {@link CrmRequest}.
     * @param crmResponse {@link CrmResponse}.
     */
    @Override
    protected void secureProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final Long orgId = request.orgId();
        final Long userId = request.userId();
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());

        switch (type) {
            case "list" -> loadListIntoResponse(orgId, data, crmResponse);
            case "assignOwner" -> assignOwner(orgId, userId, data.getLong(MessengerFields.OWNER_ID), data.getLong(MessengerFields.CHAT_ID), crmResponse);
            case "unAssignOwner" -> unAssignOwner(orgId, userId, data.getLong(MessengerFields.OWNER_ID), data.getLong(MessengerFields.CHAT_ID), crmResponse);
            case MessengerApiKeys.LINK_TO_DEAL -> linkToEntity(orgId, Source.DEAL, data, request, crmResponse);
            case MessengerApiKeys.LINK_TO_CONTACT -> linkToEntity(orgId, Source.CONTACT, data, request, crmResponse);
            case "relatedSource" -> loadSourceDetail(orgId, data.getLong(MessengerFields.CHAT_ID), crmResponse);
            case "send" -> send(orgId, request.userId(), data, crmResponse);
            case "delete" -> delete(orgId, data.getLong(MessengerFields.CHAT_ID), crmResponse);
            case "getConversation" -> loadConversation(orgId, data, crmResponse);
            case "sendReadReceipt" -> sendReadReceipt(orgId, data, crmResponse);
            case "getTemplateList" -> getAndDispatchTemplateList(orgId, data, crmResponse);
            case "getPlaceholderFields" -> fetchAndDispatchPlaceholderFields(orgId, data, crmResponse);
            case "getPlaceholderFieldValues" -> fetchAndDispatchPlaceholderFieldValues(orgId, data, crmResponse);
            case "getTemplatePreview" -> fetchAndDispatchTemplatePreview(orgId, data, crmResponse);
        }
    }

    /**
     * <p>
     * Loads chat list into {@link CrmResponse}.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param data        Request's data.
     * @param crmResponse {@link CrmResponse}.
     */
    private void loadListIntoResponse(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final String startsWith = data.getNullableString(MessengerFields.STARTS_WITH);
        final CrmChatFilter chatFilter = CrmChatFilter.getFilterById(data.getShort(MessengerFields.FILTER_BY));
        final JsonObject range = data.optJsonObject(MessengerFields.RANGE);
        final int start;
        final int count;

        if (Objects.isNull(range)) {
            start = NumericalConstant.ONE;
            count = NumericalConstant.TEN;
        } else {
            start = range.optInt(MessengerFields.START, NumericalConstant.ONE);
            count = range.optInt(MessengerFields.COUNT, NumericalConstant.TEN);
        }
        final ChatPageHandler chatPageHandler = new ChatPageHandler(start, count, orgId, chatFilter, startsWith);
        final long totalRecords = chatPageHandler.getTotalRecordsCount();
        final boolean isSynced = chatManager.isAnyChannelSynced(orgId);

        if (NumericalConstant.ZERO < totalRecords) {
            final Collection<CrmChat> crmChats = chatPageHandler.fetchPageData().getData();

            crmResponse.put(MessengerFields.TOTAL_COUNT, totalRecords);
            crmResponse.put(MessengerFields.HAS_MORE, (start + count) <= totalRecords);
            crmResponse.put(MessengerFields.IS_SYNCED, isSynced);

            if (crmChats.isEmpty()) {
                crmResponse.put(MessengerFields.LIST, Json.array());
                return;
            }
            crmResponse.convertIntoAsync();
            final Collection<Long> chatIds = new HashSet<>(INITIAL_CAPACITY);
            final Collection<Long> salesOwnerIds = new HashSet<>(INITIAL_CAPACITY);

            for (final CrmChat crmChat : crmChats) {
                final Long chatId = crmChat.id();
                final Long salesOwnerId = crmChat.getOwnerId();

                if (Objects.nonNull(chatId)) {
                    chatIds.add(chatId);
                }

                if (Objects.nonNull(salesOwnerId)) {
                    salesOwnerIds.add(salesOwnerId);
                }
            }
            final Map<Long, CrmUser> userMap = userService.getUsers(orgId, salesOwnerIds).stream().collect(Collectors.toMap(CrmUser :: id, Function.identity()));

            final EventCallback<Map<Long, Chat>> callback = new EventCallback<>() {

                @Override
                public void onSuccess(final Map<Long, Chat> chatMap) {

                    try {
                        for (final CrmChat crmChat : crmChats) {
                            final CrmUser owner = userMap.get(crmChat.getOwnerId());

                            if (Objects.nonNull(owner)) {
                                crmChat.setOwner(owner);
                            }
                        }
                        crmResponse.put(MessengerFields.LIST, chatConverter.convert(chatMap, crmChats).getJsonArray(MessengerFields.LIST));
                    } catch (final Throwable cause) {
                        final String errorDetail = cause.getMessage();

                        LoggerUtil.logError(LOGGER, cause, "Messenger: Retrieved chat list, Internal error. OrgId: {}, Message: {}", orgId, errorDetail);
                        setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.CHAT_LIST_RETRIEVAL_FAILED,
                                CHAT_LIST_RETRIEVAL_FAILURE_MESSAGE), errorDetail, null);
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {
                    final String errorDetail = cause.getMessage();

                    LoggerUtil.logError(LOGGER, cause, "Messenger: Chat list retrieval failed, OrgId: {}, Message: {}", orgId, errorDetail);
                    setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.CHAT_LIST_RETRIEVAL_FAILED,
                            CHAT_LIST_RETRIEVAL_FAILURE_MESSAGE), errorDetail, null);
                    crmResponse.send();
                }
            };
            chatService.fetchChats(chatIds, callback);
        } else {
            crmResponse.put(MessengerFields.TOTAL_COUNT, NumericalConstant.ZERO);
            crmResponse.put(MessengerFields.HAS_MORE, false);
            crmResponse.put(MessengerFields.LIST, Json.array());
            crmResponse.put(MessengerFields.IS_SYNCED, isSynced);
        }
    }

    /**
     * <p>
     * Assigns owner to chat.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param ownerId     Owner's id.
     * @param crmChatId      Chat id.
     * @param crmResponse {@link CrmResponse}.
     */
    private void assignOwner(final Long orgId, final Long userId, final Long ownerId, final Long crmChatId, final CrmResponse crmResponse) {

        if (chatManager.exists(orgId, crmChatId)) {
            if (crmUserManager.exists(orgId, List.of(ownerId))) {
                final Type type = conversationManager.getType(orgId, crmChatId);
                final CrmChat crmChat = new CrmChat();

                crmChat.setCrmChatId(crmChatId);
                crmChat.setOwner(userService.getUser(orgId, ownerId));
                crmChat.setOrgId(orgId);
                final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(crmChat), MessengerValidatorGroup.ChatUserAssignment.class);

                if (!errors.isEmpty()) {
                    final ErrorCode errorCode = ErrorCode.get(MessengerErrorCodes.FAILED_TO_ASSIGN_USER_TO_CHAT, "i18n.messenger.chat.owner.assignment.failed");

                    crmResponse.setErrorCode(errorCode);
                    crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));

                    return;
                }
                chatManager.updateOwner(crmChat);
                conversationManager.addActivity(orgId, crmChatId, (long) type.getId(), userId, ownerId);
                crmResponse.put(MessengerFields.MESSAGE, crmResponse.resolveI18nString("i18n.messenger.chat.owner.assigned.successfully"));
                final Optional<Long> optionalChatId = chatManager.getChatId(orgId, crmChatId);

                if (optionalChatId.isPresent()) {
                    final Long chatId = optionalChatId.get();
                    final EventCallback<Chat> chatCallback = new EventCallback<>() {

                        @Override
                        public void onSuccess(final Chat chat) {
                            try {
                                final CrmChat crmChat = new CrmChat();

                                crmChat.setCrmChatId(crmChatId);
                                crmChat.setOwnerId(ownerId);
                                crmChat.setOrgId(orgId);
                                crmChat.setProfile(chat.getProfile());
                                crmChat.setPlatform(chat.getPlatform());
                                final EventData eventData = new SecureEventData(orgId, userId);

                                eventData.setValues(List.of(crmChat));
                                eventData.setAction(Action.CREATE);
                                triggerEvent(APP_NOTIFICATION_EVENT, eventData, Event.Type.POST);
                            } catch (Throwable throwable) {
                                LOGGER.error("Messenger: Failed to send chat related notification event. Org Id:{}, Chat Id:{}", orgId, crmChatId);
                            }
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            LOGGER.error("Messenger: Failed to fetch recipient profile. Org Id:{}, Chat Id:{}", orgId, crmChatId);
                        }
                    };

                    chatService.fetchPlatformAndRecipientProfile(chatId, chatCallback);
                }
            } else {
                setErrorDescriptor(crmResponse, ErrorCode.get(CommonErrorCodes.USER_NOT_FOUND,
                        "i18n.messenger.user.not.found"), null, null);
            }
        } else {
            setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.CHAT_NOT_FOUND,
                    "i18n.messenger.chat.owner.assignment.failed"), null, null);
        }
    }

    /**
     * <p>
     * Un-assigns owner to chat.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param chatId      Chat id.
     * @param userId      User's id.
     * @param crmResponse {@link CrmResponse}.
     */
    private void unAssignOwner(final Long orgId, final Long userId, final Long ownerId, final Long chatId, final CrmResponse crmResponse) {

        if (chatManager.exists(orgId, chatId)) {
            if (crmUserManager.exists(orgId, List.of(ownerId))) {
                final CrmChat crmChat = new CrmChat();

                crmChat.setOrgId(orgId);
                crmChat.setCrmChatId(chatId);
                final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(crmChat), MessengerValidatorGroup.ChatUserUnAssignment.class);

                if (errors.isEmpty()) {
                    chatManager.updateOwner(crmChat);
                    conversationManager.addActivity(orgId, chatId, (long) Type.USER_UNASSIGNMENT.getId(), userId, ownerId);
                    crmResponse.put(MessengerFields.MESSAGE, crmResponse.resolveI18nString("i18n.messenger.chat.owner.unAssigned.successfully"));
                } else {
                    final ErrorCode errorCode = ErrorCode.get(MessengerErrorCodes.FAILED_TO_ASSIGN_USER_TO_CHAT, "i18n.messenger.chat.owner.assignment.failed");

                    crmResponse.setErrorCode(errorCode);
                    crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
                }
            } else {
                setErrorDescriptor(crmResponse, ErrorCode.get(CommonErrorCodes.USER_NOT_FOUND,
                        "i18n.messenger.user.not.found"), null, null);
            }
        } else {
            setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.CHAT_NOT_FOUND,
                    "i18n.messenger.chat.owner.unAssignment.failed"), null, null);
        }
    }

    /**
     * <p>
     * Links the entity to the chat
     * </p>
     *
     * @param orgId       Represents the org id
     * @param source      Represents the {@link Source}
     * @param data        Represents the request data
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void linkToEntity(final Long orgId, final Source source, final JsonObject data, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long crmChatId = data.getLong(MessengerFields.CHAT_ID);

        if (chatManager.exists(orgId, crmChatId)) {
            final Optional<Long> optionalChatId = chatManager.getChatId(orgId, crmChatId);

            if (optionalChatId.isPresent()) {
                crmResponse.convertIntoAsync();
                final Long chatId = optionalChatId.get();
                final EventCallback<Channel> eventCallback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Channel channel) {
                        final CrmChat crmChat = new CrmChat();

                        crmChat.setId(chatId);
                        crmChat.setCrmChatId(crmChatId);
                        crmChat.setOrgId(orgId);
                        crmChat.setChannel(channel);

                        eventRequestHandler.handle(() -> {
                            try {
                                switch (source) {
                                    case DEAL -> linkToDeal(orgId, data, crmChat, crmRequest, crmResponse);
                                    case CONTACT -> linkToContact(orgId, data, crmChat, crmRequest, crmResponse);
                                }
                            } catch (Throwable throwable) {
                                LOGGER.error("Messenger: Failed to link the entity to chat. Org Id:{}, Chat Id:{}, Reason: {}", orgId, crmChatId, throwable.getMessage());
                            } finally {
                                crmResponse.send();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOGGER.error("Messenger: Failed to fetch channel for the chat. Org Id:{}, Chat Id:{}, Reason: {}", orgId, crmChatId, throwable.getMessage());
                        crmResponse.send();
                    }
                };

                chatService.fetchChannel(chatId, eventCallback);
            }
        } else {
            setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.CHAT_NOT_FOUND, "i18n.messenger.chat.not.found"), null, null);
        }
    }

    /**
     * <p>
     * Links chat to the deal.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param data        Represents the json data
     * @param crmResponse {@link CrmResponse}.
     */
    private void linkToDeal(final Long orgId, final JsonObject data, final CrmChat chat, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long userId = crmRequest.userId();

        if (data.containsKey(MessengerFields.DEAL_ID)) {
            final Long dealId = data.getLong(MessengerFields.DEAL_ID);

            if (chatSourceManager.isDealExists(orgId, dealId)) {
                final DataContainer targetContainer = DataContainer.create();

                chatManager.linkToEntity(orgId, userId, chat, dealId, OMSOCHAT.DEAL_ID, Source.DEAL, targetContainer);
                getOrgDataStore(orgId).commitChanges(targetContainer);
                crmResponse.put(MessengerFields.MESSAGE, crmResponse.resolveI18nString("i18n.messenger.chat.deal.mapped.successfully"));
            } else {
                setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.DEAL_NOT_FOUND, "i18n.messenger.chat.deal.mapping.failed"), null, null);
            }
        } else if (data.containsKey(MessengerFields.DEAL)) {
            final JsonObject dealJson = data.getJsonObject(MessengerFields.DEAL);
            final Deal deal = dealConverter.convert(converterConfigBuilder.build(crmRequest, false), dealJson);
            final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(deal), Group.Create.class);

            if (errors.isEmpty()) {
                dealDataValidator.validate(orgId, List.of(deal));
                chatManager.linkToNewDeal(orgId, userId, chat, deal);
                postAction(orgId, userId, Action.CREATE, List.of(deal));
            } else {
                final ErrorCode errorCode = ErrorCode.get(MessengerErrorCodes.ENTITY_CREATE_ACTION_FAILED, "i18n.messenger.chat.deal.create.failed");

                crmResponse.setErrorCode(errorCode);
                crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
            }
        } else {
            setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.NO_ENTITY_FOUND_TO_LINK, "i18n.messenger.chat.entity.not.found.to.link"), null, null);
        }
    }

    /**
     * <p>
     * Links chat to the contact.
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param data        Represents the json data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void linkToContact(final Long orgId, final JsonObject data, final CrmChat chat, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long userId = crmRequest.userId();

        if (data.containsKey(MessengerFields.CONTACT_ID)) {
            final Long contactId = data.getLong(MessengerFields.CONTACT_ID);

            if (chatSourceManager.isContactExists(orgId, contactId)) {
                final DataContainer targetContainer = DataContainer.create();

                chatManager.linkToEntity(orgId, userId, chat, contactId, OMSOCHAT.CONTACT_ID, Source.CONTACT, targetContainer);
                getOrgDataStore(orgId).commitChanges(targetContainer);
                crmResponse.put(MessengerFields.MESSAGE, crmResponse.resolveI18nString("i18n.messenger.chat.contact.mapped.successfully"));
            } else {
                setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.CONTACT_NOT_FOUND, "i18n.messenger.chat.contact.mapping.failed"), null, null);
            }
        } else if (data.containsKey(MessengerFields.CONTACT)) {
            final JsonObject contactJson = data.getJsonObject(MessengerFields.CONTACT);
            final Contact contact = contactConverter.convert(converterConfigBuilder.build(crmRequest, false), contactJson);
            final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(contact), Group.Create.class);

            if (errors.isEmpty()) {
                contactDataValidator.validate(orgId, List.of(contact));
                chatManager.linkToNewContact(orgId, userId, chat, contact);
                postAction(orgId, userId, Action.CREATE, List.of(contact));
            } else {
                final ErrorCode errorCode = ErrorCode.get(MessengerErrorCodes.ENTITY_CREATE_ACTION_FAILED, "i18n.messenger.chat.contact.create.failed");

                crmResponse.setErrorCode(errorCode);
                crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
            }
        } else {
            setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.NO_ENTITY_FOUND_TO_LINK, "i18n.messenger.chat.entity.not.found.to.link"), null, null);
        }
    }

    /**
     * <p>
     * Loads source detail.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param chatId      Chat id.
     * @param crmResponse {@link CrmResponse}.
     */
    private void loadSourceDetail(final Long orgId, final Long chatId, final CrmResponse crmResponse) {

        if (chatManager.exists(orgId, chatId)) {
            loadSourceDetailForChat(orgId, chatId, crmResponse);
        } else {
            setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.CHAT_NOT_FOUND,
                    "i18n.messenger.chat.not.found"), null, null);
        }
    }

    /**
     * <p>
     * Loads source detail for chat.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param chatId      Chat id.
     * @param crmResponse {@link CrmResponse}.
     */
    private void loadSourceDetailForChat(final Long orgId, final Long chatId, final CrmResponse crmResponse) {
        final Long dealId = chatManager.getEntityId(orgId, chatId, OMSOCHAT.DEAL_ID);
        final Long contactId = chatManager.getEntityId(orgId, chatId, OMSOCHAT.CONTACT_ID);

        if (Objects.nonNull(dealId)) {
            chatSourceManager.getDeal(orgId, dealId).ifPresent(deal ->
                    crmResponse.put(DealFields.DEAL, buildDealJson(deal)));
        }

        if (Objects.nonNull(contactId)) {
            chatSourceManager.getContact(orgId, contactId).ifPresent(contact ->
                    crmResponse.put(DealFields.CONTACT, buildContactJson(contact)));
        }
    }

    /**
     * <p>
     * Builds deal json.
     * </p>
     *
     * @param deal {@link Deal}.
     * @return {@link JsonObject}.
     */
    private JsonObject buildDealJson(final Deal deal) {
        final JsonObject dealJson = Json.object();
        final Date expectedCloseDate = deal.getExpectedCloseDate();
        final Pipeline pipeline = deal.getPipeline();
        final PipelineStage pipelineStage = deal.getPipelineStage();
        final DealValue dealValue = deal.getDealValue();

        dealJson.put(DealFields.ID, deal.id());
        dealJson.put(DealFields.TITLE, deal.getTitle());
        dealJson.put(DealFields.IS_ARCHIVED, deal.isArchived());

        if (Objects.nonNull(expectedCloseDate)) {
            final Long expectedCloseDateTimeStamp = expectedCloseDate.getTimeStamp();

            dealJson.put(DealFields.EXPECTED_CLOSE_DATE, expectedCloseDateTimeStamp);
            dealJson.put(DealFields.IS_OVERDUE, Instant.now().toEpochMilli() > expectedCloseDateTimeStamp);
        }

        if (Objects.nonNull(pipeline)) {
            final JsonObject pipelineJson = Json.object();

            pipelineJson.put(MessengerFields.ID, pipeline.id());
            pipelineJson.put(MessengerFields.NAME, pipeline.getName());
            dealJson.put(DealFields.PIPELINE, pipelineJson);
        }

        if (Objects.nonNull(pipelineStage)) {
            final JsonObject pipelineStageJson = Json.object();

            pipelineStageJson.put(MessengerFields.ID, pipelineStage.id());
            pipelineStageJson.put(MessengerFields.NAME, pipelineStage.getName());
            dealJson.put(DealFields.PIPELINE_STAGE, pipelineStageJson);
        }

        if (Objects.nonNull(dealValue)) {
            final JsonObject dealValueJson = Json.object();

            dealValueJson.put(MessengerFields.ID, dealValue.id());
            dealValueJson.put(DealFields.VALUE, dealValue.getValue());
            dealValueJson.put(DealFields.SYMBOL, dealValue.getSymbol());
            dealJson.put(DealFields.DEAL_VALUE, dealValueJson);
        }
        return dealJson;
    }

    /**
     * <p>
     * Builds contact json.
     * </p>
     *
     * @param contact {@link Contact}.
     * @return {@link JsonObject}.
     */
    private JsonObject buildContactJson(final Contact contact) {
        final JsonObject contactJson = Json.object();
        final Email email = contact.getPrimaryEmail();
        final Phone phone = contact.getPrimaryPhone();
        final Company company = contact.getCompany();

        contactJson.put(MessengerFields.ID, contact.id());
        contactJson.put(MessengerFields.NAME, contact.getFullName());

        if (Objects.nonNull(email)) {
            final JsonObject emailJson = Json.object();

            emailJson.put(MessengerFields.ID, email.id());
            emailJson.put(ContactFields.VALUE, email.getValue());
            emailJson.put(ContactFields.TYPE, email.getType());
            contactJson.put(ContactFields.PRIMARY_EMAIL, emailJson);
        }

        if (Objects.nonNull(phone)) {
            final JsonObject phoneJson = Json.object();

            phoneJson.put(MessengerFields.ID, phone.id());
            phoneJson.put(ContactFields.NUMBER, phone.getNumber());
            phoneJson.put(ContactFields.TYPE, phone.getType());
            contactJson.put(ContactFields.PRIMARY_PHONE, phoneJson);
        }

        if (Objects.nonNull(company)) {
            final JsonObject companyJson = Json.object();

            companyJson.put(MessengerFields.ID, company.id());
            companyJson.put(MessengerFields.NAME, company.getName());
            contactJson.put(ContactFields.COMPANY, companyJson);
        }
        contactJson.put(ContactFields.FACEBOOK, contact.getFacebook());
        contactJson.put(ContactFields.LINKEDIN, contact.getLinkedIn());

        return contactJson;
    }

    /**
     * <p>
     * Sends the message to the recipient
     * </p>
     *
     * @param orgId       Represents the org id
     * @param userId      Represents the user id
     * @param data        Represents the json data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void send(final Long orgId, final Long userId, final JsonObject data, final CrmResponse crmResponse) {
        final Platform platform = Platform.get(data.getShort(MessengerFields.PLATFORM));
        final JsonObject messageJson = Json.object();
        final Long crmChatId;
        final Long businessPhoneNumberId;

        if (Platform.WHATSAPP == platform && data.containsKey(MessengerFields.TEMPLATE_ID)) {
            messageJson.put(MessengerFields.TEMPLATE_ID, data.getLong(MessengerFields.TEMPLATE_ID));

            if (data.containsKey(MessengerFields.PARAM_DATA)) {
                messageJson.put(MessengerFields.PARAM_DATA, data.getJsonObject(MessengerFields.PARAM_DATA));
            }

            if (data.containsKey(MessengerFields.CHAT_ID)) {
                businessPhoneNumberId = null;
                crmChatId = data.getLong(MessengerFields.CHAT_ID);
                final Optional<Long> optionalChatId = chatManager.getChatId(orgId, crmChatId);

                if (optionalChatId.isPresent()) {
                    messageJson.put(MessengerFields.CHAT_ID, optionalChatId.get());
                } else {
                    throw ErrorCode.get(MessengerErrorCodes.CHAT_NOT_FOUND, "i18n.messenger.chat.not.found");
                }
            } else {
                crmChatId = null;
                final Optional<Phone> optionalPrimaryPhone = contactManager.getPrimaryPhone(orgId, data.getLong(MessengerFields.CONTACT_ID));

                optionalPrimaryPhone.ifPresentOrElse(primaryPhone -> {
                    final String phoneNumber = primaryPhone.getNumber();

                    if (!phoneNumber.startsWith(MessengerFields.COUNTRY_CODE_PREFIX)) {
                        setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.INVALID_PHONE_NUMBER, "i18n.whatsapp.phone.country.code.not.found"), null, null);
                        return;
                    }
                    messageJson.put(MessengerFields.RECIPIENT_PHONE_NUMBER, phoneNumber);
                }, () -> {
                    throw ErrorCode.get(WAErrorCodes.PRIMARY_PHONE_NOT_FOUND, "i18n.whatsapp.contact.phone.not.found");
                });
                final Optional<Long> optionalPhoneNumberId = waBusinessAccountManager.getPhoneNumberId(orgId, data.getLong(MessengerFields.BUSINESS_PHONE_NUMBER_ID));

                if (optionalPhoneNumberId.isPresent()) {
                    businessPhoneNumberId = optionalPhoneNumberId.get();
                    messageJson.put(MessengerFields.BUSINESS_PHONE_NUMBER_ID, businessPhoneNumberId);
                } else {
                    throw ErrorCode.get(WAErrorCodes.BUSINESS_PHONE_NUMBER_NOT_FOUND, "i18n.whatsapp.business.phone.number.not.found");

                }
            }
        } else {
            crmChatId = data.getLong(MessengerFields.CHAT_ID);
            businessPhoneNumberId = null;
            final Optional<Long> optionalChatId = chatManager.getChatId(orgId, crmChatId);

            if (optionalChatId.isPresent()) {
                messageJson.put(MessengerFields.CHAT_ID, optionalChatId.get());
            } else {
                throw ErrorCode.get(MessengerErrorCodes.CHAT_NOT_FOUND, "i18n.messenger.chat.not.found");
            }
        }
        messageJson.put(MessengerFields.MESSAGE, data.getNullableString(MessengerFields.MESSAGE));

        if (data.containsKey(MessengerFields.ATTACHMENTS)) {
            messageJson.put(MessengerFields.ATTACHMENTS, data.getJsonArray(MessengerFields.ATTACHMENTS));
        }
        messageJson.put(MessengerFields.PLATFORM, data.getShort(MessengerFields.PLATFORM));

        crmResponse.convertIntoAsync();
        final EventCallback<JsonObject> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final JsonObject response) {
                socialEventRequestHandler.handle(() -> {
                    try {
                        if (response.containsKey(MessengerFields.ERRORS)) {
                            final List<String> errors = (List<String>) response.get(MessengerFields.ERRORS);
                            final String errorDetail = errors.get(NumericalConstant.ZERO);
                            final ErrorCode errorCode = ErrorCode.get(SocialErrorCodes.MESSAGE_SEND_ACTION_FAILED, errorDetail);

                            LoggerUtil.logError(LOGGER, errorCode, "Messenger: Failed to send message, OrgId: {}, Message: {}", orgId, errorDetail);
                            setErrorDescriptor(crmResponse, errorCode, errorDetail, null);
                        } else {
                            final JsonArray messageArray = response.getJsonArray(MessengerFields.MESSAGES);
                            final int size = messageArray.size();
                            final Collection<CrmMessage> messages = new ArrayList<>();
                            final CrmChat crmChat = new CrmChat();

                            if (Objects.isNull(crmChatId)) {
                                final Long chatId = response.getLong(MessengerFields.CHAT_ID);
                                final Optional<Long> optionalChatId = chatManager.getCrmChatId(orgId, chatId);

                                if (optionalChatId.isPresent()) {
                                    crmChat.setCrmChatId(optionalChatId.get());
                                    crmChat.setNewChat(false);
                                } else {
                                    final Contact contact = new Contact();
                                    final Long contactId = data.getLong(MessengerFields.CONTACT_ID);
                                    final String phoneNumber = messageJson.getString(MessengerFields.RECIPIENT_PHONE_NUMBER);
                                    final RecipientProfile recipientProfile = new RecipientProfile();

                                    recipientProfile.setName(String.join("|", contactManager.getContactFullName(orgId, contactId), phoneNumber));

                                    crmChat.setProfile(recipientProfile);
                                    contact.setId(contactId);
                                    crmChat.setContact(contact);
                                    crmChat.setNewChat(true);
                                }
                                crmChat.setId(chatId);
                            } else {
                                crmChat.setCrmChatId(crmChatId);
                            }
                            crmChat.setOrgId(orgId);

                            for (int index = 0; index < size; index++) {
                                final JsonObject messageJson = messageArray.getJsonObject(index);
                                final CrmMessage message = new CrmMessage();

                                message.setId(messageJson.getLong(MessengerFields.ID));
                                message.setSentTime(messageJson.getLong(MessengerFields.SENT_TIME));
                                message.setMessageFlow(Message.MessageFlow.OUTBOUND);
                                message.setOutboundMessageSource(CrmMessage.OutboundMessageSource.MANUAL);

                                if (index == size - NumericalConstant.ONE) {
                                    message.setContent(messageJson.getString(MessengerFields.LAST_MESSAGE));
                                    crmChat.setLastMessage(message);
                                }
                                messages.add(message);
                            }
                            final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(crmChat), Group.Create.class);

                            if (errors.isEmpty()) {
                                final DataContainer targetContainer = DataContainer.create();

                                if (Platform.WHATSAPP == platform && crmChat.isNewChat()) {
                                    final Channel channel = new Channel();

                                    channel.setType(Channel.ChannelType.WHATSAPP_BUSINESS);
                                    channel.setName(response.getString(MessengerFields.CHANNEL_NAME));
                                    crmChat.setChannel(channel);
                                    waBusinessAccountManager.loadNewChat(orgId, crmChat, targetContainer, businessPhoneNumberId);
                                    chatManager.linkToEntity(orgId, userId, crmChat, crmChat.getContact().id(), OMSOCHAT.CONTACT_ID, Source.CONTACT, targetContainer);
                                }
                                conversationManager.saveMessages(orgId, userId, crmChat, messages, targetContainer);
                                crmResponse.put(MessengerFields.CHAT_ID, crmChat.getCrmChatId());
                            } else {
                                final ErrorCode errorCode = ErrorCode.get(MessengerErrorCodes.CHAT_CREATE_ACTION_FAILED, "i18n.messenger.chat.create.failed");

                                crmResponse.setErrorCode(errorCode);
                                crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
                            }
                        }
                    } catch (Throwable throwable) {
                        final String errorDetail = throwable.getMessage();

                        LoggerUtil.logError(LOGGER, throwable, "Messenger: Failed to send message, Internal error, OrgId: {}, Message: {}", orgId, errorDetail);
                        setErrorDescriptor(crmResponse, ErrorCode.get(CrmSocialErrorCodes.FAILED_TO_SEND_MESSAGE, "i18n.messenger.chat.message.send.action.failed"), errorDetail, null);
                    } finally {
                        crmResponse.send();
                    }
                });
            }

            @Override
            public void onFailure(Throwable cause) {
                final String errorDetail = cause.getMessage();

                LoggerUtil.logError(LOGGER, cause, "Messenger: Failed to send message, OrgId: {}, Message {}", orgId, errorDetail);
                setErrorDescriptor(crmResponse, ErrorCode.get(CrmSocialErrorCodes.FAILED_TO_SEND_MESSAGE, "i18n.messenger.chat.message.send.action.failed"), errorDetail, null);
                crmResponse.send();
            }
        };

        chatService.sendMessage(messageJson, eventCallback);
    }

    /**
     * <p>
     * Deletes the chat
     * </p>
     *
     * @param orgId       Represents the org id
     * @param chatId      Represents the chat id
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void delete(final Long orgId, final Long chatId, final CrmResponse crmResponse) {
        if (chatManager.exists(orgId, chatId)) {
            chatManager.delete(orgId, chatId);
        } else {
            setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.CHAT_NOT_FOUND, "i18n.messenger.chat.not.found"), null, null);
        }
    }

    /**
     * <p>
     * Loads conversation list into {@link CrmResponse}.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param data        Request's data.
     * @param crmResponse {@link CrmResponse}.
     */
    private void loadConversation(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final Platform platform = Platform.get(data.getShort(MessengerFields.PLATFORM));
        final JsonObject range = data.optJsonObject(MessengerFields.RANGE);
        final Long chatId = data.getLong(MessengerFields.CHAT_ID);
        final int start;
        final int limit;

        if (Objects.isNull(range)) {
            start = NumericalConstant.ONE;
            limit = NumericalConstant.TEN;
        } else {
            start = range.optInt(MessengerFields.START, NumericalConstant.ONE);
            limit = range.optInt(MessengerFields.COUNT, NumericalConstant.TEN);
        }

        if (!chatManager.exists(orgId, chatId)) {
            throw ErrorCode.get(MessengerErrorCodes.CHAT_NOT_FOUND, "i18n.messenger.chat.not.found");
        }
        final ConversationPageHandler conversationPageHandler = new ConversationPageHandler(start, limit, orgId, chatId);
        final long totalRecords = conversationPageHandler.getTotalRecordsCount();

        if (NumericalConstant.ZERO < totalRecords) {
            final Map<Long, JsonObject> conversationMap = new TreeMap<>(Comparator.reverseOrder());
            final Conversation conversation = conversationPageHandler.fetchPageData().getData().stream().findFirst().get();
            final Optional<Message> optionalLastInboundMessage = conversationManager.getLastInboundMessage(orgId, chatId);

            crmResponse.put(MessengerFields.TOTAL_COUNT, totalRecords);
            crmResponse.put(MessengerFields.HAS_MORE, (start + limit) <= totalRecords);
            crmResponse.convertIntoAsync();
            final ChannelState channelState = switch (platform) {
                case FACEBOOK -> fbChannelManager.getPageStatus(orgId, chatId);
                case WHATSAPP -> waBusinessAccountManager.getPhoneStatus(orgId, chatId);
                case INSTAGRAM -> instagramPageManager.getPageStatus(orgId, chatId);
            };
            final Collection<CrmMessage> crmMessages = conversation.getCrmMessages();
            final Collection<ConversationActivity> activities = conversation.getConversationActivities();
            final Collection<Long> userIds = new HashSet<>();
            final Collection<Long> messageIds = crmMessages.stream().map(CrmMessage::id).toList();

            for (final ConversationActivity conversationActivity : activities) {
                final Type type = conversationActivity.getConversationActivityTemplate().getType();

                if (Type.USER_ASSIGNMENT == type || Type.USER_UNASSIGNMENT == type || Type.USER_REASSIGNMENT == type) {
                    final JsonObject placeholderValues = conversationActivity.getPlaceHolderValue().getValueAsJson();

                    userIds.add(placeholderValues.getLong(MessengerFields.PLACEHOLDER_1));
                    userIds.add(placeholderValues.getLong(MessengerFields.PLACEHOLDER_2));
                }
            }

            if (messageIds.isEmpty()) {
                fetchUsersAndDispatch(orgId, chatId, userIds, conversationMap, activities, crmResponse);
            } else {
                final EventCallback<Map<Long, Message>> eventCallback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Map<Long, Message> messageMap) {
                        socialEventRequestHandler.handle(() -> {
                            try {
                                if (messageMap.isEmpty()) {
                                    final ErrorCode errorCode = ErrorCode.get(MessengerErrorCodes.CONVERSATION_NOT_FOUND, CONVERSATION_RETRIEVAL_FAILURE_MESSAGE);

                                    LoggerUtil.logError(LOGGER, errorCode, "Messenger: Messages not found, OrgId: {}, Chat ID: {}, IDs: {}", orgId, chatId, messageIds);
                                    setErrorDescriptor(crmResponse, errorCode, null, null);
                                    crmResponse.send();
                                } else {
                                    loadAttachments(messageMap.values());

                                    for (final CrmMessage crmMessage : crmMessages) {
                                        final Message message = messageMap.get(crmMessage.id());
                                        final Long userId = crmMessage.getUserId();

                                        if (Objects.nonNull(userId)) {
                                            userIds.add(userId);
                                        }
                                        conversationMap.put(crmMessage.getCrmId(), conversationConverter.convert(message, crmMessage));
                                    }
                                    loadOutboundMessagingEligibility(channelState, optionalLastInboundMessage, crmResponse);

                                    if (userIds.isEmpty()) {
                                        loadConversationsIntoResponse(conversationMap.values(), crmResponse);
                                        crmResponse.send();
                                    } else {
                                        fetchUsersAndDispatch(orgId, chatId, userIds, conversationMap, activities, crmResponse);
                                    }
                                }
                            } catch (final Throwable cause) {
                                final String errorDetail = cause.getMessage();

                                LoggerUtil.logError(LOGGER, cause,
                                        "Messenger: Failed to load conversations - Internal error, OrgId: {}, Chat ID: {}, Message: {}", orgId, chatId, errorDetail);
                                setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.CONVERSATION_NOT_FOUND,
                                        CONVERSATION_RETRIEVAL_FAILURE_MESSAGE), errorDetail, null);
                                crmResponse.send();
                            }
                        });
                    }

                    @Override
                    public void onFailure(final Throwable cause) {
                        final String errorDetail = cause.getMessage();

                        LoggerUtil.logError(LOGGER, cause,
                                "Messenger: Failed to retrieve messages - Internal error, OrgId: {}, Chat ID: {}, IDs: {}, Message: {}", orgId, chatId, messageIds, errorDetail);
                        setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.CONVERSATION_NOT_FOUND,
                                CONVERSATION_RETRIEVAL_FAILURE_MESSAGE), errorDetail, null);
                        crmResponse.send();
                    }
                };
                chatService.fetchMessages(messageIds, eventCallback);
            }
        } else {
            crmResponse.put(MessengerFields.TOTAL_COUNT, NumericalConstant.ZERO);
            crmResponse.put(MessengerFields.HAS_MORE, false);
            crmResponse.put(MessengerFields.CONVERSATIONS, Json.array());
            crmResponse.put(MessengerFields.USERS, Json.object());
        }
    }

    /**
     * <p>
     * Loads the outbound messaging eligibility to the response
     * </p>
     *
     * @param channelState               Represents the {@link ChannelState}
     * @param optionalLastInboundMessage Represents the optional of last inbound message
     * @param crmResponse                Represents the {@link CrmResponse}
     */
    private void loadOutboundMessagingEligibility(final ChannelState channelState, final Optional<Message> optionalLastInboundMessage, final CrmResponse crmResponse) {
        final MessageSendEligibility messageSendEligibility;

        switch (channelState) {
            case ENABLED -> {
                if (optionalLastInboundMessage.isPresent()) {
                    final boolean isSendEnabled = Instant.now().toEpochMilli() < TemporalServiceProvider.get(optionalLastInboundMessage.get().getSentTime()).plus(24, DateInterval.HOURS);

                    if (isSendEnabled) {
                        messageSendEligibility = MessageSendEligibility.ELIGIBLE;
                    } else {
                        messageSendEligibility = MessageSendEligibility.MESSAGE_WINDOW_EXPIRED;
                    }
                } else {
                    messageSendEligibility = MessageSendEligibility.WAITING_FOR_RECEIVER_RESPONSE;
                }
            }
            case DISABLED -> messageSendEligibility = MessageSendEligibility.PAGE_DISABLED;
            case DELETED -> messageSendEligibility = MessageSendEligibility.PAGE_DELETED;
            default -> messageSendEligibility = MessageSendEligibility.ELIGIBLE;
        }

        crmResponse.put(MessengerFields.OUTBOUND_ELIGIBILITY, messageSendEligibility);
    }

    /**
     * <p>
     * Loads the attachments into the messages
     * </p>
     *
     * @param messages Represents the messages
     */
    private void loadAttachments(final Collection<Message> messages) {
        final Map<Long, Attachment> attachmentMap = new HashMap<>();
        final Collection<Long> attachmentIds = new HashSet<>();

        messages.stream().filter(message -> {
            final Collection<Attachment> attachments = message.getAttachments();

            return Objects.nonNull(attachments) && !attachments.isEmpty();
        }).forEach(message -> attachmentIds.addAll(message.getAttachments().stream().map(Attachment::id).toList()));

        if (!attachmentIds.isEmpty()) {
            final Collection<Attachment> attachments = attachmentService.getAttachments(attachmentIds);

            for (final Attachment attachment : attachments) {
                attachmentMap.put(attachment.id(), attachment);
            }
        }

        for (final Message message : messages) {
            final Collection<Attachment> attachments = new ArrayList<>();
            final Collection<Attachment> existingAttachments = message.getAttachments();

            if (Objects.nonNull(existingAttachments) && !existingAttachments.isEmpty()) {
                for (final Attachment attachment : existingAttachments) {
                    attachments.add(attachmentMap.get(attachment.id()));
                }

                message.setAttachments(attachments);
            }
        }
    }

    /**
     * <p>
     * Fetches the users and dispatch the conversation response
     * </p>
     *
     * @param orgId           Represents the org id
     * @param chatId          Represents the chat id
     * @param userIds         Represents the collection of user ids
     * @param conversationMap Represents the conversation map
     * @param activities      Represents the activities
     * @param crmResponse     Represents the {@link CrmResponse}
     */
    private void fetchUsersAndDispatch(final Long orgId, final Long chatId, final Collection<Long> userIds, final Map<Long, JsonObject> conversationMap,
                                       final Collection<ConversationActivity> activities, final CrmResponse crmResponse) {
        final Collection<CrmUser> crmUsers = userService.getUsers(orgId, userIds);
        final Collection<Long> iamUserIds = crmUsers.stream().map(CrmUser::getIamId).toList();
        final EventCallback<Collection<User>> userFetchCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final Collection<User> users) {
                try {
                    final Map<Long, User> iamUserMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                    final Map<Long, User> crmUserMap = new HashMap<>();
                    final JsonObject usersJson = Json.object();

                    for (final CrmUser crmUser : crmUsers) {
                        final JsonObject userJson = Json.object();
                        final Long userId = crmUser.id();
                        final Long iamUserId = crmUser.getIamId();

                        userJson.put(MessengerFields.ID, userId);
                        userJson.put(MessengerFields.NAME, iamUserMap.get(iamUserId).getFullName());
                        usersJson.put(String.valueOf(userId), userJson);
                        crmUserMap.put(userId, iamUserMap.get(iamUserId));
                    }

                    for (final ConversationActivity conversationActivity : activities) {
                        conversationMap.put(conversationActivity.getId(), conversationConverter.convertActivity(conversationActivity, crmUserMap));
                    }
                    loadConversationsIntoResponse(conversationMap.values(), crmResponse);

                    crmResponse.put(MessengerFields.USERS, usersJson);
                } catch (Throwable throwable) {
                    final String errorDetail = throwable.getMessage();

                    LoggerUtil.logError(LOGGER, throwable,
                            "Messenger: Failed to load users into conversation - Internal error, Org Id: {}, Chat id:{}, Message: {}", orgId, chatId, errorDetail);
                    setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.USER_RETRIEVAL_FAILED,
                            USER_RETRIEVAL_FAILURE_MESSAGE_KEY), errorDetail, null);
                } finally {
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(Throwable cause) {
                final String errorDetail = cause.getMessage();

                LoggerUtil.logError(LOGGER, cause,
                        "Messenger: Failed to retrieve the users - Internal error, Org Id: {}, Chat id:{}, Message: {}", orgId, chatId, errorDetail);
                setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.USER_RETRIEVAL_FAILED,
                        USER_RETRIEVAL_FAILURE_MESSAGE_KEY), errorDetail, null);
                crmResponse.send();
            }
        };
        iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, userFetchCallback);
    }

    /**
     * <p>
     * Loads the conversation into response
     * </p>
     *
     * @param conversations Represents the conversations
     * @param crmResponse   Represents the {@link CrmResponse}
     */
    private void loadConversationsIntoResponse(final Collection<JsonObject> conversations, final CrmResponse crmResponse) {
        final JsonArray conversationArray = Json.array();

        for (final JsonObject conversationJson : conversations) {
            conversationArray.put(conversationJson);
        }
        crmResponse.put(MessengerFields.CONVERSATIONS, conversationArray);
    }

    /**
     * <p>
     * Sends the read receipt to the recipient
     * </p>
     *
     * @param orgId       Represents the org id
     * @param data        Represents the request data
     * @param crmResponse Represents
     */
    private void sendReadReceipt(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final Long crmChatId = data.getLong(MessengerFields.CHAT_ID);
        final short platformId = data.getShort(MessengerFields.PLATFORM);
        final Platform platform = Platform.get(platformId);

        if (chatManager.exists(orgId, crmChatId)) {
            final Optional<Long> optionalChatId = chatManager.getChatId(orgId, crmChatId);

            if (optionalChatId.isPresent()) {
                final Long chatId = optionalChatId.get();
                final JsonObject readReceiptJson = Json.object();

                readReceiptJson.put(MessengerFields.CHAT_ID, chatId);
                readReceiptJson.put(MessengerFields.PLATFORM, platformId);

                if (Platform.WHATSAPP == platform) {
                    conversationManager.getLastInboundMessageId(orgId, crmChatId).ifPresent(messageId -> readReceiptJson.put(MessengerFields.MESSAGE_ID, messageId));
                }
                crmResponse.convertIntoAsync();
                final EventCallback<JsonObject> eventCallback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final JsonObject response) {
                        try {
                            if (response.containsKey(MessengerFields.ERRORS)) {
                                final List<String> errors = (List<String>) response.get(MessengerFields.ERRORS);
                                final ErrorCode errorCode = ErrorCode.get(MessengerErrorCodes.FAILED_TO_SEND_READ_RECEIPT, READ_RECEIPT_FAILURE_MESSAGE_KEY);
                                final String errorDetail = errors.get(NumericalConstant.ZERO);

                                LoggerUtil.logError(LOGGER, errorCode, "Messenger: Failed to send the read receipt, OrgId: {}, Chat id:{}, Message: {}", orgId, crmChatId, errorDetail);
                                setErrorDescriptor(crmResponse, errorCode, errorDetail, null);
                            } else {
                                chatManager.resetUnreadCount(orgId, chatId);
                            }
                        } catch (Throwable throwable) {
                            final String errorDetail = throwable.getMessage();

                            LoggerUtil.logError(LOGGER, throwable,
                                    "Messenger: Failed to send the read receipt to the recipient - Internal error, OrgId: {}, Chat ID: {}, Message:{}, ", orgId, crmChatId, errorDetail);
                            setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.FAILED_TO_SEND_READ_RECEIPT,
                                    READ_RECEIPT_FAILURE_MESSAGE_KEY), errorDetail, null);
                        } finally {
                            crmResponse.send();
                        }
                    }

                    @Override
                    public void onFailure(Throwable cause) {
                        final String errorDetail = cause.getMessage();

                        LoggerUtil.logError(LOGGER, cause,
                                "Messenger: Failed to send the read receipt to the recipient, OrgId: {}, Chat ID: {}, Message:{}, ", orgId, crmChatId, errorDetail);
                        setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.FAILED_TO_SEND_READ_RECEIPT,
                                READ_RECEIPT_FAILURE_MESSAGE_KEY), errorDetail, null);
                        crmResponse.send();
                    }
                };

                chatService.sendReadReceipt(readReceiptJson, eventCallback);
            }
        } else {
            throw ErrorCode.get(MessengerErrorCodes.CHAT_NOT_FOUND, "i18n.messenger.chat.not.found");
        }
    }

    /**
     * <p>
     * Gets the template list and dispatches the response
     * </p>
     *
     * @param orgId       Represents the org id
     * @param data        Represents the request data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void getAndDispatchTemplateList(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final Platform platform = Platform.get(data.getShort(MessengerFields.PLATFORM));
        final String searchText = data.optString(MessengerFields.SEARCH_TEXT, null);
        final JsonObject rangeJson = data.optJsonObject(MessengerFields.RANGE);
        final int start;
        final int count;

        if (Objects.isNull(rangeJson)) {
            start = NumericalConstant.ONE;
            count = NumericalConstant.TEN;
        } else {
            start = rangeJson.optInt(MessengerFields.START, NumericalConstant.ONE);
            count = rangeJson.optInt(MessengerFields.LIMIT, NumericalConstant.TEN);
        }

        switch (platform) {
            case WHATSAPP -> getAndDispatchWATemplateList(orgId, data, crmResponse, start, count, searchText);
        }
    }

    /**
     * <p>
     * Fetches the whatsapp template list and dispatches the response
     * </p>
     *
     * @param orgId       Represents the org id
     * @param requestData Represents the request json data
     * @param crmResponse Represents the {@link CrmResponse}
     * @param start       Represents the start of the template list
     * @param count       Represents the limit of the templates to be fetched
     * @param searchText  Represents the search text
     */
    private void getAndDispatchWATemplateList(final Long orgId, final JsonObject requestData, final CrmResponse crmResponse, final int start, final int count, final String searchText) {
        final Optional<Long> optionalBusinessAccountId = requestData.containsKey(MessengerFields.BUSINESS_PHONE_NUMBER_ID)
                ? waBusinessAccountManager.getBusinessAccountId(orgId, requestData.getLong(MessengerFields.BUSINESS_PHONE_NUMBER_ID))
                : waBusinessAccountManager.getBusinessAccountIdFromChat(orgId, requestData.getLong(MessengerFields.CHAT_ID));

        if (optionalBusinessAccountId.isPresent()) {
            final Long businessAccountId = optionalBusinessAccountId.get();

            crmResponse.convertIntoAsync();

            final EventCallback<Page<WATemplate>> callback = new EventCallback<>() {

                @Override
                public void onSuccess(final Page<WATemplate> templatePage) {
                    try {
                        final int totalRecords = templatePage.getNumber();
                        final Map<String, List<WATemplate>> templateMap = templatePage.getData().stream().collect(Collectors.groupingBy(WATemplate::getName));
                        final JsonArray templateArray = Json.array();

                        crmResponse.put(MessengerFields.TOTAL_COUNT, totalRecords);
                        crmResponse.put(MessengerFields.HAS_MORE, (start + count) <= totalRecords);

                        for (Map.Entry<String, List<WATemplate>> templateEntry : templateMap.entrySet()) {
                            final JsonObject templateJson = Json.object();
                            final JsonArray languageArray = Json.array();

                            templateJson.put(WAFields.NAME, templateEntry.getKey());

                            for (final WATemplate waTemplate : templateEntry.getValue()) {
                                final JsonObject languageJson = Json.object();

                                languageJson.put(WAFields.ID, waTemplate.id());
                                languageJson.put(WAFields.LANGUAGE, waTemplate.getLanguage().getDisplayLanguage());

                                languageArray.put(languageJson);
                            }
                            templateJson.put(WAFields.LANGUAGES, languageArray);
                            templateArray.put(templateJson);
                        }
                        crmResponse.put(WAFields.TEMPLATES, templateArray);

                    } catch (Throwable throwable) {
                        final String errorDetail = throwable.getMessage();

                        LoggerUtil.logError(LOGGER, throwable, "Whatsapp: Templates retrieved but error during conversion. OrgId: {}, Reason: {}", orgId, errorDetail);
                        setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.TEMPLATE_LIST_RETRIEVAL_FAILED, "i18n.whatsapp.templates.retrieval.failed"), errorDetail, null);
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(Throwable cause) {
                    final String errorDetail = cause.getMessage();

                    LoggerUtil.logError(LOGGER, cause, "Whatsapp: Failed to retrieve templates. OrgId: {}, Reason: {}", orgId, errorDetail);
                    setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.TEMPLATE_LIST_RETRIEVAL_FAILED, "i18n.whatsapp.templates.retrieval.failed"), errorDetail, null);
                }
            };

            waBusinessAccountService.fetchTemplates(businessAccountId, start, count, searchText, callback);
        } else {
            LOGGER.error("Whatsapp: Business account not found. OrgId: {}", orgId);
            throw ErrorCode.get(WAErrorCodes.FAILED_TO_FETCH_WHATSAPP_BUSINESS_ACCOUNT, "i18n.whatsapp.business.account.retrieval.failed");
        }
    }

    /**
     * <p>
     * Fetches the template placeholder fields and dispatches it
     * </p>
     *
     * @param orgId       Organization's id.
     * @param data        Request data.
     * @param crmResponse {@link CrmResponse}.
     */
    private void fetchAndDispatchPlaceholderFields(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final Source source = Source.get(data.getString(MessengerFields.SOURCE));

        switch (source) {
            case DEAL -> loadFieldsIntoJsonArray(orgId, Deal.ENTITY_TYPE, crmResponse);
            case CONTACT -> loadFieldsIntoJsonArray(orgId, Contact.ENTITY_TYPE, crmResponse);
        }
    }

    /**
     * <p>
     * Loads the fields into the json array and dispatches the response
     * </p>
     *
     * @param orgId    Represents the org id
     * @param source   Represents the source
     * @param response Refers to {@link CrmResponse}
     */
    private void loadFieldsIntoJsonArray(final Long orgId, final String source, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final Collection<FieldInfo> defaultFields = fieldService.getDefaultFieldsBySource(List.of(source)).stream().
                filter(fieldInfo -> fieldInfo.presetConfig().addView()).collect(Collectors.toUnmodifiableSet());
        final Collection<FieldInfo> persistentFields = fieldService.getPersistentFieldsBySource(orgId, Contact.ENTITY_TYPE)
                .stream().filter(fieldInfo -> {

                    if (fieldInfo.presetConfig().addView()) {
                        return StoreType.isSingleValueStore(fieldInfo.storeType());
                    }

                    return false;
                }).collect(Collectors.toUnmodifiableSet());

        for (final FieldInfo fieldInfo : sortFields(defaultFields, persistentFields)) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(MessengerFields.ID, fieldInfo.id());
            jsonObject.put(MessengerFields.DISPLAY_NAME, response.resolveI18nPlaceholder(fieldInfo.i18nName()));

            jsonArray.put(jsonObject);
        }

        response.put(MessengerFields.FIELDS, jsonArray);
        response.send();
    }

    /**
     * <p>
     * Fetches the placeholder field values and load them into the response
     * </p>
     *
     * @param orgId       Represents the org id
     * @param data        Represents the request data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchPlaceholderFieldValues(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final long contactId = data.getLong(MessengerFields.CONTACT_ID);

        if (contactManager.exists(orgId, contactId)) {
            final Collection<FieldInfo> persistentFields = fieldService.getPersistentFieldsBySource(orgId, Contact.ENTITY_TYPE)
                    .stream().filter(fieldInfo -> {

                        if (fieldInfo.presetConfig().addView()) {
                            return StoreType.isSingleValueStore(fieldInfo.storeType());
                        }

                        return false;
                    }).collect(Collectors.toUnmodifiableSet());
            final Collection<FieldInfo> defaultFields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, contactPlaceholderFieldNames)
                    .values().stream().filter(fieldInfo -> ownerFieldNames.contains(fieldInfo.aliasName()) || fieldInfo.presetConfig().addView())
                    .collect(Collectors.toUnmodifiableSet());
            final Contact contact = contactManager.get(orgId, contactId);
            final JsonArray fieldArray = Json.array();
            final Set<Long> iamUserIds = new HashSet<>();
            final JsonObject jsonObject = Json.object();

            jsonObject.put(MessengerFields.DISPLAY_NAME, crmResponse.resolveI18nString("i18n.contact.field.fullName"));
            jsonObject.put(MessengerFields.ID, CONTACT_FULL_NAME_FIELD_ID);
            jsonObject.put(MessengerFields.VALUE, contact.getFullName());

            fieldArray.put(jsonObject);
            final CrmUser user = LoggedInUserInfo.getUser();
            final UserRegionalPreference regionalPreference = user.getPreferenceHolder().get(UserRegionalPreference.TYPE);
            final TimeZone timeZone = regionalPreference.getTimezoneInfo().getTimeZone();
            final Map<String, Field> entityFields = placeHolderValueInterpreter.loadEntityFields(contact.getClass());

            sortFields(defaultFields, persistentFields).forEach(fieldInfo -> loadFieldValue(fieldInfo, fieldArray, contact, iamUserIds, crmResponse, timeZone, entityFields));

            if (iamUserIds.isEmpty()) {
                crmResponse.put(MessengerFields.FIELDS, fieldArray);
            } else {
                loadOwnerFieldValuesAndDispatch(orgId, iamUserIds, fieldArray, crmResponse);
            }
        } else {
            setErrorDescriptor(crmResponse, ErrorCode.get(MessengerErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found"), null, null);
        }
    }

    /**
     * <p>
     * Sorts the fields based on the order defined in preset configuration and returns the sorted list
     * </p>
     *
     * @param defaultFields    Represents the default fields
     * @param persistentFields Represents the custom fields
     * @return the sorted list
     */
    private Collection<FieldInfo> sortFields(final Collection<FieldInfo> defaultFields, final Collection<FieldInfo> persistentFields) {
        final List<FieldInfo> allFields = new ArrayList<>(persistentFields.size() + defaultFields.size());

        allFields.addAll(defaultFields);
        allFields.addAll(persistentFields);
        allFields.sort(Comparator.comparingInt(field -> field.presetConfig().order()));

        return allFields;
    }

    /**
     * <p>
     * Loads the field values into the response
     * </p>
     * @param fieldInfo     Represents the field
     * @param fieldArray    Represents the field array
     * @param contact       Represents the {@link Contact}
     * @param userIds       Represents the collection of user ids
     * @param timeZone      Represents the user timezone
     * @param entityFields  Represents the map of entity fields with field name as key and {@link Field} as value
     */
    private void loadFieldValue(final FieldInfo fieldInfo, final JsonArray fieldArray, final Contact contact,
                                final Set<Long> userIds, final CrmResponse crmResponse, final TimeZone timeZone, final Map<String, Field> entityFields) {
        final JsonObject jsonObject = Json.object();
        final ChatPlaceHolder placeHolder = new ChatPlaceHolder();

        if ((Company.ENTITY_TYPE).equals(fieldInfo.fieldSource().getName())) {
            placeHolderValueInterpreter.resolve(placeHolder, fieldInfo, contact.getCompany(), timeZone, entityFields);
        } else {
            placeHolderValueInterpreter.resolve(placeHolder, fieldInfo, contact, timeZone, entityFields);
        }
        final Object value = placeHolder.getValue();

        jsonObject.put(MessengerFields.ID, fieldInfo.id());
        jsonObject.put(MessengerFields.DISPLAY_NAME, crmResponse.resolveI18nPlaceholder(fieldInfo.i18nName()));
        jsonObject.put(MessengerFields.VALUE, value);

        if (Objects.nonNull(value) && fieldInfo.storeType().isOwnerType()) {
            userIds.add((Long) value);
            jsonObject.put(MessengerFields.OWNER, value);
        }
        fieldArray.put(jsonObject);
    }

    /**
     * <p>
     * Loads the owner field values into the field array and dispatches the response
     * </p>
     *
     * @param orgId      Represents the org id
     * @param iamUserIds Represents the user ids
     * @param fieldArray Represents the field array
     * @param response   Represents the {@link CrmResponse}
     */
    private void loadOwnerFieldValuesAndDispatch(final Long orgId, final Collection<Long> iamUserIds, final JsonArray fieldArray, final CrmResponse response) {
        response.convertIntoAsync();
        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Collection<User> users) {
                try {
                    final Map<Long, User> crmUsers = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                    final int fieldSize = fieldArray.size();

                    for (int index = 0; index < fieldSize; index++) {
                        final JsonObject jsonObject = fieldArray.getJsonObject(index);

                        if (jsonObject.containsKey(MessengerFields.OWNER)) {
                            final User user = crmUsers.get(jsonObject.getLong(MessengerFields.OWNER));

                            if (Objects.nonNull(user)) {
                                jsonObject.put(MessengerFields.VALUE, user.getFullName());
                                jsonObject.remove(MessengerFields.OWNER);
                            }
                        }
                    }
                    response.put(MessengerFields.FIELDS, fieldArray);
                } catch (final Throwable throwable) {
                    response.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                } finally {
                    response.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                response.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                response.send();
            }
        };
        iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
    }

    /**
     * <p>
     * Fetches the template and dispatches its preview
     * </p>
     *
     * @param orgId       Represents the org id
     * @param data        Represents the request data
     * @param crmResponse Represents the  {@link CrmResponse}
     */
    private void fetchAndDispatchTemplatePreview(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final Platform platform = Platform.get(data.getShort(MessengerFields.PLATFORM));
        final Long templateId = data.getLong(MessengerFields.ID);

        switch (platform) {
            case WHATSAPP -> {
                crmResponse.convertIntoAsync();

                final EventCallback<WATemplate> callback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final WATemplate template) {
                        try {
                            crmResponse.put(MessengerFields.TEMPLATE, waTemplateConverter.convert(template));
                        } catch (Throwable throwable) {
                            final String errorDetail = throwable.getMessage();

                            LoggerUtil.logError(LOGGER, throwable, "Whatsapp: Template retrieved but error during conversion. OrgId: {}, Reason: {}", orgId, errorDetail);
                            setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.TEMPLATE_RETRIEVAL_FAILED, "i18n.whatsapp.template.retrieval.failed"), errorDetail, null);
                        } finally {
                            crmResponse.send();
                        }
                    }

                    @Override
                    public void onFailure(Throwable cause) {
                        final String errorDetail = cause.getMessage();

                        LoggerUtil.logError(LOGGER, cause, "Whatsapp: Failed to retrieve template. OrgId: {}, Reason: {}", orgId, errorDetail);
                        setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.TEMPLATE_RETRIEVAL_FAILED, "i18n.whatsapp.template.retrieval.failed"), errorDetail, null);
                        crmResponse.send();
                    }
                };

                waBusinessAccountService.fetchTemplate(templateId, callback);
            }
        }
    }
}