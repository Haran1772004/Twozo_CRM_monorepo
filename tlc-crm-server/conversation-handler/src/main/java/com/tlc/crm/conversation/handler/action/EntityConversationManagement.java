package com.tlc.crm.conversation.handler.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.conversation.handler.api.converter.EntityConversationConverter;
import com.tlc.crm.conversation.handler.api.manager.EntityConversationManager;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.holder.ConfigManagerHolder;
import com.tlc.crm.core.conversation.api.model.ConversableModel;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.common.iam.service.OrganizationService;

import com.tlc.crm.core.conversation.api.model.EntityConversation;
import com.tlc.crm.conversation.handler.fields.EntityConversationFields;
import com.tlc.crm.core.conversation.status.EntityConversationErrorCodes;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.social.client.service.ChatService;
import com.tlc.social.common.model.Chat;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages web actions related to {@link EntityConversation} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@SourceAuthorizer(relatedApiTypes = SourceAuthorizer.RelatedApiType.EMAIL, needRelatedApiHandling = true)
@WebAction(path = "/entity/conversations/mgmt")
public class EntityConversationManagement extends AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityConversationManagement.class);

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;

    private final OrganizationService organizationService;
    private final UserService userService;
    private final EntityConversationManager entityConversationManager;
    private final EntityConversationConverter entityConversationConverter;
    private final ChatService chatService;

    public EntityConversationManagement() {
        organizationService = OrganizationService.getInstance();
        userService = UserService.getInstance();
        entityConversationManager = EntityConversationManager.getInstance();
        entityConversationConverter = EntityConversationConverter.getInstance();
        chatService = ChatService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Handles the received request
     * @param crmResponse Generates the CRM response
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(CoreEmailFields.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(CoreEmailFields.DATA.getName());

        switch (type) {
            case "getEntityConversations" -> loadEntityConversations(request, data, crmResponse);
            case "delete" -> delete(request.orgId(), data);
        }
    }

    /**
     * <p>
     * Loads conversations based on the given source and retrieves the relevant data to be included at {@link CrmResponse}
     * </p>
     *
     * @param crmRequest Represents the {@link CrmRequest}
     * @param jsonObject Contains the JSON object
     * @param response   Represents the crm response
     */
    private void loadEntityConversations(final CrmRequest crmRequest, final JsonObject jsonObject, final CrmResponse response) {
        final Long orgId = crmRequest.orgId();
        final Long userId = crmRequest.userId();
        final int start = Math.max(jsonObject.optInt(CoreEmailFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(jsonObject.optInt(CoreEmailFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Long entityId = jsonObject.getLong(CoreEmailFields.ENTITY_ID.getName());
        final Source source = Source.valueOf(jsonObject.getString(CoreEmailFields.SOURCE.getName()).toUpperCase());
        final String typeKey = CoreEmailFields.TYPE.getName();
        final Collection<EntityConversation.Type> types;

        if (jsonObject.containsKey(typeKey)) {
            final EntityConversation.Type type = EntityConversation.Type.get(jsonObject.getString(typeKey));
            types = type.isCallType() ? EntityConversation.Type.getCallTypes() : List.of(type);
        } else {
            types = Collections.emptyList();
        }

        if (!ConfigManagerHolder.getInstance().getManager(source).exists(orgId, entityId)) {
            throw ErrorCode.get(EntityConversationErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }
        final long totalRecords = entityConversationManager.getSourceConversationCount(orgId, userId, entityId, source, types);
        final Collection<EntityConversation<? extends ConversableModel>> conversations = totalRecords > 0 && start <= totalRecords ?
                entityConversationManager.getConversations(orgId, userId, entityId, types, source, start, limit) : Collections.emptyList();

        if (conversations.isEmpty()) {
            setAndDispatchResponse(crmRequest, totalRecords, start, limit, response, conversations);
        } else {
            fetchUserAndDispatchConversations(crmRequest, response, start, limit, totalRecords, conversations);
        }
    }

    /**
     * <p>
     * Fetches user data associated with conversations and dispatches the updated conversations records.
     * </p>
     *
     * @param crmRequest          Represents the {@link CrmRequest}
     * @param crmResponse         Represents the crm response
     * @param start               Represents the starting index of {@link EntityConversation} to be fetched
     * @param limit               Represents the maximum number of {@link EntityConversation} to be fetched
     * @param totalRecords        Represents the total number of conversations records for the specified source
     * @param entityConversations An entity which holds the {@link EntityConversation}
     */
    private void fetchUserAndDispatchConversations(final CrmRequest crmRequest, final CrmResponse crmResponse, final int start, final int limit,
                                                   final Long totalRecords, final Collection<EntityConversation<? extends ConversableModel>> entityConversations) {
        final Collection<Long> userIds = entityConversations.stream().map(EntityConversation::getCrmUser).filter(Objects::nonNull).map(CrmUser::getIamId).collect(Collectors.toUnmodifiableSet());

        if (userIds.isEmpty()) {
            setAndDispatchResponse(crmRequest, totalRecords, start, limit, crmResponse, entityConversations);
        } else {
            crmResponse.convertIntoAsync();
            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    final Map<Long, User> user = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    for (final EntityConversation<? extends ConversableModel> entityConversation : entityConversations) {
                        final CrmUser crmUser = entityConversation.getCrmUser();

                        if (Objects.nonNull(crmUser)) {
                            crmUser.setUser(user.get(crmUser.getIamId()));
                            entityConversation.setCrmUser(crmUser);
                        }
                    }
                    setAndDispatchResponse(crmRequest, totalRecords, start, limit, crmResponse, entityConversations);
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    crmResponse.setErrorCode(ErrorCode.get(EntityConversationErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                    crmResponse.send();
                }
            };
            userService.fetchUsers(organizationService.getIamOrgId(crmRequest.orgId()), userIds, usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Sets the conversations response for the given entity id and dispatches it
     * </p>
     *
     * @param crmRequest          Represents an {@link CrmRequest}
     * @param start               Represents the starting index of {@link EntityConversation} to be fetched
     * @param limit               Represents the maximum number of {@link EntityConversation} to be fetched
     * @param totalRecords        Represents the total number of conversations records for the specified source
     * @param crmResponse         Represents the crm response
     * @param entityConversations Represents the {@link Collection} of {@link EntityConversation}
     */
    private void setAndDispatchResponse(final CrmRequest crmRequest, final Long totalRecords, final int start, final int limit, final CrmResponse crmResponse, final Collection<EntityConversation<? extends ConversableModel>> entityConversations) {
        final ConverterConfig converterConfig = new ConverterConfig(crmRequest.orgId(), crmRequest.userId(), crmRequest.locale(), false, null);
        final JsonArray conversationArray = Json.array();
        final Collection<Long> chatIds = entityConversations.stream().filter(EntityConversation::isSocialType)
                .map(entityConversation -> ((Chat) entityConversation.getConversableModel()).id())
                .collect(Collectors.toUnmodifiableSet());

        crmResponse.put(CoreEmailFields.TOTAL_RECORDS.getName(), totalRecords);
        crmResponse.put(CoreEmailFields.HAS_MORE.getName(), (start + limit) <= totalRecords);

        if (chatIds.isEmpty()) {
            for (final EntityConversation<? extends ConversableModel> entityConversation : entityConversations) {
                conversationArray.put(entityConversationConverter.convert(converterConfig, entityConversation));
            }
            crmResponse.put(EntityConversationFields.CONVERSATIONS.getName(), conversationArray);

            crmResponse.send();
        } else {
            crmResponse.convertIntoAsync();
            final EventCallback<Map<Long, Chat>> callback = new EventCallback<>() {

                @Override
                public void onSuccess(final Map<Long, Chat> chatMap) {
                    try {
                        for (final EntityConversation<? extends ConversableModel> entityConversation : entityConversations) {

                            if (entityConversation.isSocialType()) {
                                final Chat crmChat = (Chat) entityConversation.getConversableModel();
                                final Chat chat = chatMap.get(crmChat.id());

                                if (Objects.nonNull(chat)) {
                                    crmChat.setPlatform(chat.getPlatform());
                                    crmChat.setChannel(chat.getChannel());
                                    crmChat.setProfile(chat.getProfile());
                                }
                            }
                            conversationArray.put(entityConversationConverter.convert(converterConfig, entityConversation));

                            crmResponse.put(EntityConversationFields.CONVERSATIONS.getName(), conversationArray);
                        }
                    } catch (final Throwable cause) {
                        final String errorDetail = cause.getMessage();

                        LOGGER.error("Entity Conversation: Failed to fetch entity conversation. Chat fetch failed. Org Id: {}, User Id:{}, Reason:{}", crmRequest.orgId(), crmRequest.userId(), errorDetail);
                        setErrorDescriptor(crmResponse, ErrorCode.get(EntityConversationErrorCodes.CHATS_FETCH_FAILED, "i18n.messenger.chat.list.retrieval.failed"), errorDetail, null);
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {
                    final String errorDetail = cause.getMessage();

                    LOGGER.error("Entity Conversation: Failed to fetch the chats. Org Id: {}, User Id:{}, Reason:{}", crmRequest.orgId(), crmRequest.userId(), errorDetail);
                    setErrorDescriptor(crmResponse, ErrorCode.get(EntityConversationErrorCodes.CHATS_FETCH_FAILED, "i18n.messenger.chat.list.retrieval.failed"), errorDetail, null);
                    crmResponse.send();
                }
            };
            chatService.fetchChats(chatIds, callback);
        }
    }

    /**
     * <p>
     * Deletes the entity conversation by the provided id
     * </p>
     *
     * @param orgId Represents the organization id
     * @param data  Represents the request data
     */
    private void delete(final long orgId, final JsonObject data) {
        final long entityConversationId = data.getLong(EntityConversationFields.ID.getName());

        if (entityConversationManager.exists(orgId, entityConversationId)) {
            entityConversationManager.delete(orgId, entityConversationId);
        } else {
            throw ErrorCode.get(EntityConversationErrorCodes.ENTITY_CONVERSATION_NOT_FOUND, "i18n.entity.conversation.not.found");
        }
    }
}
