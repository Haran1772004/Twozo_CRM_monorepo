package com.tlc.crm.messenger.api.conversation.manager;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.messenger.api.conversation.model.Conversation;
import com.tlc.crm.messenger.api.conversation.model.ConversationActivity;
import com.tlc.crm.messenger.api.conversation.model.ConversationActivityTemplate;
import com.tlc.crm.messenger.api.conversation.model.MessageType;
import com.tlc.crm.messenger.api.conversation.model.CrmMessage;
import com.tlc.crm.messenger.config.manager.AbstractMessengerManager;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.internal.service.ConversationActivityTemplateService;
import com.tlc.crm.messenger.sql.resource.OMECCHATCONVERSATIONS;
import com.tlc.crm.messenger.sql.resource.OMSOCHAT;
import com.tlc.crm.messenger.sql.resource.OMSOCONVERSATION;
import com.tlc.crm.messenger.sql.resource.OMSOCONVERSATIONACTIVITY;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.RecipientProfile;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.CountQuery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.tlc.social.common.model.Message.MessageFlow.OUTBOUND;

/**
 * <p>
 * Manages conversation related data base actions.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class ConversationManager extends AbstractMessengerManager {

    private ConversationManager() {
    }

    private static final class Instance {
        private static final ConversationManager CONVERSATION_MANAGER = new ConversationManager();
    }

    /**
     * <p>
     * Gets {@link ConversationManager}.
     * </p>
     *
     * @return {@link ConversationManager}.
     */
    public static ConversationManager getInstance() {
        return Instance.CONVERSATION_MANAGER;
    }

    /**
     * <p>
     * Gets {@link OMSOCONVERSATION} count.
     * </p>
     *
     * @param orgId  Organization's id.
     * @param chatId Chat's id.
     * @return gets {@link OMSOCONVERSATION} count.
     */
    public long getConversationCount(final Long orgId, final Long chatId) {
        final Table conversationTable = getConversationTable();
        final CountQuery countQuery = CountQuery.get(conversationTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getColumn(OMSOCONVERSATION.CHAT_ID), chatId)));

        return getOrgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets {@link ConversationActivityTemplate.Type}.
     * </p>
     *
     * @param orgId  Organization's id.
     * @param chatId Chat id.
     * @return {@link ConversationActivityTemplate.Type}.
     */
    public ConversationActivityTemplate.Type getType(final Long orgId, final Long chatId) {
        final Table conversationTable = getConversationTable();
        final CountQuery conversationCountQuery = CountQuery.get(conversationTable);

        conversationCountQuery.setWhereClause(new WhereClause(Criteria.eq(
                conversationTable.getColumn(OMSOCONVERSATION.CHAT_ID), chatId)).and(Criteria.notEq(
                conversationTable.getColumn(OMSOCONVERSATION.ACTIVITY_ID), null)));

        return getOrgDataStore(orgId).get(conversationCountQuery) == NumericalConstant.ZERO ?
                ConversationActivityTemplate.Type.USER_ASSIGNMENT : ConversationActivityTemplate.Type.USER_REASSIGNMENT;
    }

    /**
     * <p>
     * Adds conversation activity.
     * </p>
     *
     * @param orgId      Organization's id.
     * @param chatId     Chat id.
     * @param activityId Conversation activity's id.
     * @param userId     User's id.
     * @param ownerId    Owner's id.
     */
    public void addActivity(final Long orgId, final Long chatId, final Long activityId, final Long userId, final Long ownerId) {
        final DataContainer dataContainer = DataContainer.create();
        final Row conversationRow = new Row(getConversationTable());

        conversationRow.set(OMSOCONVERSATION.CHAT_ID, chatId);
        conversationRow.set(OMSOCONVERSATION.TYPE, MessageType.ACTIVITY.getId());
        conversationRow.set(OMSOCONVERSATION.TIMESTAMP, Instant.now().toEpochMilli());
        conversationRow.set(OMSOCONVERSATION.ACTIVITY_ID, activityId);
        conversationRow.set(OMSOCONVERSATION.PLACE_HOLDER_VALUES, new ConversationActivity.PlaceHolderValue(userId, ownerId).getValueAsJson());
        dataContainer.addNewRow(conversationRow);
        getOrgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * <p>
     * Gets {@link Conversation}.
     * </p>
     *
     * @param orgId  Organization's id.
     * @param chatId Chat's id.
     * @param start  Starting index of chat to be fetched.
     * @param limit  Maximum number of chat to be fetched.
     * @return {@link Conversation}.
     */
    public Conversation getConversation(final Long orgId, final Long chatId, final int start, final int limit) {
        final Table conversationTable = getConversationTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.addSelectClause(conversationTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getColumn(OMSOCONVERSATION.CHAT_ID), chatId)));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(new OrderByClause(conversationTable.getPKColumn(), OrderByClause.OrderType.DESCENDING));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : convertRows(orgId, dataContainer.getRows(conversationTable).toList());
    }

    /**
     * <p>
     * Gets {@link ConversationActivityTemplate}.
     * </p>
     *
     * @param typeId type id.
     * @return {@link ConversationActivityTemplate}.
     */
    public ConversationActivityTemplate getActivityTemplate(final Long typeId) {
        final Table conversationActivityTable = getConversationActivityTable();
        final SelectQuery conversationActivityQuery = SelectQuery.get(conversationActivityTable);

        conversationActivityQuery.addSelectClause(conversationActivityTable);
        conversationActivityQuery.setWhereClause(new WhereClause(Criteria.eq(
                conversationActivityTable.getPKColumn(), typeId)));
        final DataContainer dataContainer = readableDataStore().get(conversationActivityQuery);

        return dataContainer.isEmpty() ? null : convertRow(dataContainer.getRow(conversationActivityTable));
    }

    /**
     * <p>
     * Saves the chats and messages into the database
     * </p>
     *
     * @param orgIds          Represents the collection of org id
     * @param chats           Represents the collection of chats
     * @param chatId          Represents the chat id
     * @param orgCreatorMap   Represents the org creator map
     * @param isMessageEcho   Represents whether message echo or not
     * @param messageResponse Represents the message response
     * @param targetContainer Represents the target container
     * @param newChats        Represents the new chats grouped by org id
     */
    public void loadMessage(final Collection<Long> orgIds, final Collection<CrmChat> chats, final Long chatId,
                            final Map<Long, Long> orgCreatorMap, final boolean isMessageEcho, final JsonObject messageResponse,
                            final DataContainer targetContainer, final Map<Long, CrmChat> newChats) {
        final Table chatTable = getChatTable();
        final Table entityConversationTable = Table.get(OMECENTITYCONVERSATIONS.TABLE);
        final Table chatEntityConversationTable = Table.get(OMECCHATCONVERSATIONS.TABLE);
        final Column chatIdColumn = chatTable.getColumn(OMSOCHAT.CHAT_ID);
        final Column orgIdColumn = chatTable.getColumn(OMSOCHAT.ORG_ID);
        final DataContainer chatContainer = getChatContainer(orgIds, chatId);
        final Long messageId = messageResponse.getLong(MessengerFields.MESSAGE_ID);
        final Long sentTime = messageResponse.getLong(MessengerFields.SENT_TIME);
        final String messagePreview = messageResponse.getString(MessengerFields.MESSAGE_PREVIEW);
        final JsonObject recipientJson = messageResponse.getJsonObject(MessengerFields.RECIPIENT);
        final String recipientProfileName = recipientJson.getString(MessengerFields.NAME);
        final Long recipientProfileId = recipientJson.getLong(MessengerFields.ID);
        final int unreadCount = messageResponse.getInt(MessengerFields.UNREAD_COUNT);

        for (final Long orgId : orgIds) {
            final CrmChat chat = new CrmChat();
            final CrmMessage message = new CrmMessage();
            final RecipientProfile recipientProfile = new RecipientProfile();

            recipientProfile.setId(recipientProfileId);
            recipientProfile.setName(recipientProfileName);
            chat.setProfile(recipientProfile);

            message.setId(messageId);
            message.setMessageFlow(isMessageEcho ? Message.MessageFlow.OUTBOUND : Message.MessageFlow.INBOUND);
            message.setSentTime(sentTime);
            message.setContent(messagePreview);

            if (isMessageEcho) {
                message.setOutboundMessageSource(CrmMessage.OutboundMessageSource.MANUAL);
            }

            chat.setId(chatId);
            chat.setLastMessage(message);
            chat.setOrgId(orgId);
            chats.add(chat);
            final Row chatRow = chatContainer.getOrInitRow(chatTable, new WhereClause(Criteria.eq(chatIdColumn, chat.id())).and(Criteria.eq(orgIdColumn, orgId)));
            final Long crmChatId = chatRow.getPKValue();
            final Long currentTime = Instant.now().toEpochMilli();

            if (chatRow.isNewRow()) {
                chatRow.setOrgId(orgId);
                chatRow.set(OMSOCHAT.CHAT_ID, chatId);
                chatRow.set(OMSOCHAT.CREATED_AT, currentTime);
                chatRow.set(OMSOCHAT.UNREAD_COUNT, unreadCount);
                chatRow.set(OMSOCHAT.RECIPIENT_PROFILE_NAME, recipientProfileName);
                chat.setNewChat(true);
                newChats.put(orgId, chat);
            } else {
                chatRow.set(OMSOCHAT.UNREAD_COUNT, (int) chatRow.get(OMSOCHAT.UNREAD_COUNT) + unreadCount);
            }
            chatRow.set(OMSOCHAT.UPDATED_AT, currentTime);
            chatRow.set(OMSOCHAT.LAST_MESSAGE, messagePreview);
            targetContainer.updateRow(chatRow);

            chat.setCrmChatId(chatRow.getPKValue());

            loadMessageIntoContainer(orgId, isMessageEcho ? orgCreatorMap.get(orgId) : null, crmChatId, message, targetContainer);
            final Row chatEntityConversationRow = chatContainer.getRow(chatEntityConversationTable, new WhereClause(Criteria.eq(chatEntityConversationTable.getColumn(OMECCHATCONVERSATIONS.CHAT_ID), crmChatId))
                    .and(Criteria.eq(chatEntityConversationTable.getColumn(OMECCHATCONVERSATIONS.ORG_ID), orgId)));

            if (Objects.nonNull(chatEntityConversationRow)) {
                final Row entityConversationRow = chatContainer.getRow(entityConversationTable, (Long) chatEntityConversationRow.get(OMECCHATCONVERSATIONS.ENTITY_CONVERSATION_ID));

                if (Objects.nonNull(entityConversationRow)) {
                    entityConversationRow.set(OMECENTITYCONVERSATIONS.CREATED_AT, sentTime);

                    targetContainer.updateRow(entityConversationRow);
                }
            }
        }
    }

    /**
     * <p>
     * Saves the collection of {@link Message} into database
     * </p>
     *
     * @param orgId           Represents the org id
     * @param userId          Represents the user id
     * @param crmChat         Represents the {@link CrmChat}
     * @param targetContainer Represents the target container
     */
    public void saveMessages(final Long orgId, final Long userId, final CrmChat crmChat, final Collection<CrmMessage> messages, final DataContainer targetContainer) {
        final boolean isNewChat = crmChat.isNewChat();

        final Long crmChatId = crmChat.getCrmChatId();
        final DataContainer chatContainer = isNewChat ? DataContainer.create() : getChatContainer(orgId, crmChatId);

        for (final CrmMessage message : messages) {
            loadMessageIntoContainer(orgId, userId, crmChatId, message, targetContainer);

            if (Objects.nonNull(message.getContent())) {
                loadLastMessageIntoChat(crmChatId, message, chatContainer, targetContainer);
            }
        }
        final Row entityConversationRow = chatContainer.getRow(Table.get(OMECENTITYCONVERSATIONS.TABLE));

        if (Objects.nonNull(entityConversationRow)) {
            entityConversationRow.set(OMECENTITYCONVERSATIONS.CREATED_AT, crmChat.getLastMessage().getSentTime());
            targetContainer.updateRow(entityConversationRow);
        }
        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     *  Loads the last message into the container
     * </p>
     * @param chatId Represents the chat id
     * @param lastMessage Represents the last message
     * @param dataContainer Represents the chat data container
     * @param targetContainer Represents the target container
     */
    private void loadLastMessageIntoChat(final Long chatId, final Message lastMessage, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Row chatRow = dataContainer.getRow(getChatTable(), chatId);

        if (Objects.nonNull(chatRow)) {
            chatRow.set(OMSOCHAT.LAST_MESSAGE, lastMessage.getContent());
            chatRow.set(OMSOCHAT.UPDATED_AT, Instant.now().toEpochMilli());
            targetContainer.updateRow(chatRow);
        }
    }

    /**
     * <p>
     * Gets the chat data container
     * </p>
     *
     * @param orgId Represents the org id
     * @param chatId Represents the crm chat id
     * @return the data container which contains chat details
     */
    private DataContainer getChatContainer(final Long orgId, final Long chatId) {
        final Table chatTable = getChatTable();
        final Column chatTablePKColumn = chatTable.getPKColumn();
        final Table entityConversationTable = Table.get(OMECENTITYCONVERSATIONS.TABLE);
        final Table chatEntityConversationTable = getChatEntityConversationTable();
        final SelectQuery selectQuery = SelectQuery.get(chatTable);

        selectQuery.addSelectClause(chatTable, entityConversationTable);
        selectQuery.addJoinClause(List.of(new JoinClause(chatTablePKColumn, chatEntityConversationTable.getColumn(OMECCHATCONVERSATIONS.CHAT_ID), JoinClause.JoinType.LEFT),
                new JoinClause(chatEntityConversationTable.getColumn(OMECCHATCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationTable.getPKColumn(), JoinClause.JoinType.LEFT)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(chatTablePKColumn, chatId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Gets the last inbound message id of a chat
     * </p>
     *
     * @param orgId  Represents the org id
     * @param chatId Represents the chat id
     * @return the {@link Message}
     */
    public Optional<Long> getLastInboundMessageId(final Long orgId, final Long chatId) {
        final Table conversationTable = getConversationTable();
        final Row conversationRow = getLastInboundMessageContainer(orgId, chatId,
                List.of(conversationTable.getColumn(OMSOCONVERSATION.MESSAGE_ID))).getRow(conversationTable);

        return Objects.isNull(conversationRow) ? Optional.empty() : Optional.of(conversationRow.get(OMSOCONVERSATION.MESSAGE_ID));
    }

    /**
     * <p>
     * Gets the last inbound message of a chat
     * </p>
     *
     * @param orgId  Represents the org id
     * @param chatId Represents the chat id
     * @return the {@link Message}
     */
    public Optional<Message> getLastInboundMessage(final Long orgId, final Long chatId) {
        final Table conversationTable = getConversationTable();
        final Collection<Column> selectColumns  = List.of(conversationTable.getColumn(OMSOCONVERSATION.MESSAGE_ID), conversationTable.getColumn(OMSOCONVERSATION.MESSAGE_FLOW),
                conversationTable.getColumn(OMSOCONVERSATION.TIMESTAMP));

        return convertRowIntoMessage(getLastInboundMessageContainer(orgId, chatId, selectColumns).getRow(conversationTable));
    }

    /**
     * <p>
     * Gets the data container of the last inbound message
     * </p>
     *
     * @param orgId  Represents the org id
     * @param chatId Represents the chat id
     * @return the data container
     */
    private DataContainer getLastInboundMessageContainer(final Long orgId, final Long chatId, final Collection<Column> selectColumns) {
        final Table conversationTable = getConversationTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationTable);
        final Column timestampColumn = conversationTable.getColumn(OMSOCONVERSATION.TIMESTAMP);
        final Column messageIdColumn = conversationTable.getColumn(OMSOCONVERSATION.MESSAGE_ID);

        selectQuery.addSelectClause(selectColumns);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getColumn(OMSOCONVERSATION.CHAT_ID), chatId))
                .and(Criteria.eq(conversationTable.getColumn(OMSOCONVERSATION.MESSAGE_FLOW), Message.MessageFlow.INBOUND.getId()))
                .and(Criteria.notEq(messageIdColumn, null)));
        selectQuery.addOrderByClause(new OrderByClause(timestampColumn, OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(new LimitClause(1));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Converts the conversation row into {@link Message}
     * </p>
     *
     * @param conversationRow Represents the conversation row
     * @return the {@link Message}
     */
    private Optional<Message> convertRowIntoMessage(final Row conversationRow) {
        if (Objects.isNull(conversationRow)) {
            return Optional.empty();
        }
        final Message message = new Message();

        message.setSentTime(conversationRow.get(OMSOCONVERSATION.TIMESTAMP));
        message.setId(conversationRow.get(OMSOCONVERSATION.MESSAGE_ID));
        message.setMessageFlow(Message.MessageFlow.get((short) conversationRow.get(OMSOCONVERSATION.MESSAGE_FLOW)));

        return Optional.of(message);
    }

    /**
     * <p>
     * Converts rows into {@link Conversation}.
     * </p>
     *
     * @param orgId Organization's id.
     * @param rows  {@link Row}.
     * @return {@link Conversation}.
     */
    private Conversation convertRows(final Long orgId, final Collection<Row> rows) {
        final ConversationActivityTemplateService conversationActivityTemplateService = ConversationActivityTemplateService.getInstance();
        final Conversation conversation = new Conversation();
        final Collection<CrmMessage> crmMessages = new ArrayList<>();
        final Collection<ConversationActivity> conversationActivities = new ArrayList<>();

        for (final Row conversationRow : rows) {
            final short typeId = conversationRow.get(OMSOCONVERSATION.TYPE);

            if (MessageType.ACTIVITY.getId() == typeId) {
                final ConversationActivity conversationActivity = new ConversationActivity();
                final ConversationActivityTemplate conversationActivityTemplate = conversationActivityTemplateService.get(
                        conversationRow.get(OMSOCONVERSATION.ACTIVITY_ID));

                conversationActivity.setOrgId(orgId);
                conversationActivity.setId(conversationRow.getPKValue());
                conversationActivity.setTimeStamp(conversationRow.get(OMSOCONVERSATION.TIMESTAMP));
                conversationActivity.setConversationActivityTemplate(conversationActivityTemplate);
                conversationActivity.setPlaceHolderValue(new ConversationActivity.PlaceHolderValue(
                        (String) conversationRow.get(OMSOCONVERSATION.PLACE_HOLDER_VALUES)));
                conversationActivities.add(conversationActivity);
            } else if (MessageType.MESSAGE.getId() == typeId) {
                final CrmMessage crmMessage = new CrmMessage();
                final Short outboundMessageSourceId = conversationRow.get(OMSOCONVERSATION.OUTBOUND_MESSAGE_SOURCE);

                crmMessage.setOrgId(orgId);
                crmMessage.setCrmId(conversationRow.getPKValue());
                crmMessage.setTimeStamp(new DateTime(conversationRow.get(OMSOCONVERSATION.TIMESTAMP)));
                crmMessage.setId(conversationRow.get(OMSOCONVERSATION.MESSAGE_ID));
                crmMessage.setUserId(conversationRow.get(OMSOCONVERSATION.USER_ID));
                crmMessages.add(crmMessage);

                if (Objects.nonNull(outboundMessageSourceId)) {
                    crmMessage.setOutboundMessageSource(CrmMessage.OutboundMessageSource.getById(outboundMessageSourceId));
                }
            }
        }
        conversation.setOrgId(orgId);
        conversation.setCrmMessages(crmMessages);
        conversation.setConversationActivities(conversationActivities);

        return conversation;
    }

    /**
     * <p>
     * Converts row to {@link ConversationActivityTemplate}.
     * </p>
     *
     * @param row {@link Row}.
     * @return {@link ConversationActivityTemplate}.
     */
    private ConversationActivityTemplate convertRow(final Row row) {
        final ConversationActivityTemplate conversationActivityTemplate = new ConversationActivityTemplate();

        conversationActivityTemplate.setType(ConversationActivityTemplate.Type.get(row.get(OMSOCONVERSATIONACTIVITY.TYPE)));
        conversationActivityTemplate.setChatOwnerTemplate(row.get(OMSOCONVERSATIONACTIVITY.CHAT_OWNER_TEMPLATE));
        conversationActivityTemplate.setOtherUserTemplate(row.get(OMSOCONVERSATIONACTIVITY.OTHER_USER_TEMPLATE));

        return conversationActivityTemplate;
    }

    /**
     * Loads {@link Message} into the container
     *
     * @param orgId           Represents the org id
     * @param userId          Represents the user id
     * @param chatId          Represents the chat id
     * @param message         Represents the {@link Message}
     * @param targetContainer Represents the target container
     */
    private void loadMessageIntoContainer(final Long orgId, final Long userId, final Long chatId, final CrmMessage message,
                                          final DataContainer targetContainer) {
        final Table conversationTable = getConversationTable();
        final Row row = new Row(conversationTable);
        final Long messageId = message.id();
        final Message.MessageFlow messageFlow = message.getMessageFlow();
        final CrmMessage.OutboundMessageSource outboundMessageSource = message.getOutboundMessageSource();

        row.setOrgId(orgId);
        row.set(OMSOCONVERSATION.MESSAGE_ID, messageId);
        row.set(OMSOCONVERSATION.CHAT_ID, chatId);
        row.set(OMSOCONVERSATION.TYPE, MessageType.MESSAGE.getId());
        row.set(OMSOCONVERSATION.TIMESTAMP, message.getSentTime());
        row.set(OMSOCONVERSATION.MESSAGE_FLOW, messageFlow.getId());

        if (OUTBOUND == messageFlow) {
            row.set(OMSOCONVERSATION.USER_ID, userId);

            if (Objects.nonNull(outboundMessageSource)) {
                row.set(OMSOCONVERSATION.OUTBOUND_MESSAGE_SOURCE, outboundMessageSource.getId());
            }
        }
        targetContainer.addNewRow(row);
    }

    /**
     * <p>
     * Gets the conversation activity table.
     * </p>
     *
     * @return the outbound message table.
     */
    private Table getConversationActivityTable() {
        return Table.get(OMSOCONVERSATIONACTIVITY.TABLE);
    }
}