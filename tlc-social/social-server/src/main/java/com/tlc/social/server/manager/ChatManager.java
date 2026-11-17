package com.tlc.social.server.manager;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.file.common.models.Attachment;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;

import com.tlc.social.common.model.Channel;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.MessageTimestamps;
import com.tlc.social.common.model.Platform;
import com.tlc.social.common.model.RecipientProfile;
import com.tlc.social.server.sql.resource.MSOATTACHMENT;
import com.tlc.social.server.sql.resource.MSOCHANNEL;
import com.tlc.social.server.sql.resource.MSOCHAT;
import com.tlc.social.server.sql.resource.MSOMESSAGE;
import com.tlc.social.server.sql.resource.MSORECIPIENT;
import com.tlc.social.server.sql.resource.MSORECIPIENTPROFILE;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.tlc.social.common.model.Message.MessageFlow.INBOUND;

/**
 * Manages the chat related database actions
 *
 * @author Maharaja S
 * @version 1.0
 */
public class ChatManager extends SocialEntityManager {

    private ChatManager() {
    }

    private static class InstanceHolder {
        private static final ChatManager INSTANCE = new ChatManager();
    }

    /**
     * <p>
     * Gets the instance of {@link ChatManager}
     * </p>
     *
     * @return the instance of {@link ChatManager}
     */
    public static ChatManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Fetches all the chats based on the provided chat Ids
     * </p>
     *
     * @param chatIds Represents the chat ids
     * @return the collection of {@link Chat}
     */
    public Collection<Chat> getAllChats(final Collection<Long> chatIds) {
        final Table chatTable = getChatTable();
        final Table recipientProfileTable = getRecipientProfileTable();
        final Table channelTable = getChannelTable();
        final SelectQuery selectQuery = SelectQuery.get(chatTable);

        selectQuery.addJoinClause(new JoinClause(chatTable.getColumn(MSOCHAT.PROFILE_ID), recipientProfileTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(chatTable.getColumn(MSOCHAT.CHANNEL_ID), channelTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(chatTable.getPKColumn(), chatIds)));

        final DataContainer dataContainer = adminDataStore().get(selectQuery);

        return dataContainer.getRows(chatTable).map(chatRow -> convertRowToModel(chatRow, dataContainer)).toList();
    }

    /**
     * <p>
     * Gets the chat data container from the given chat id
     * </p>
     *
     * @param chatId Represents the chat id
     * @return the {@link DataContainer} which contains chat details
     */
    public DataContainer getChatContainer(final Long chatId) {
        final Table chatTable = getChatTable();
        final SelectQuery selectQuery = SelectQuery.get(chatTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(chatTable.getPKColumn(), chatId)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Converts the chat row to {@link Chat}
     * </p>
     *
     * @param chatRow       Represents the chat row
     * @param dataContainer Represents the{@link DataContainer} which contains chat information
     * @return the {@link Chat}
     */
    private Chat convertRowToModel(final Row chatRow, final DataContainer dataContainer) {
        final Chat chat = new Chat();
        final Message lastMessage = new Message();
        final Channel channel = new Channel();
        final RecipientProfile recipientProfile = new RecipientProfile();
        final Long channelId = chatRow.get(MSOCHAT.CHANNEL_ID);
        final Long profileId = chatRow.get(MSOCHAT.PROFILE_ID);
        final Row channelRow = dataContainer.getRow(getChannelTable(), channelId);
        final Row profileRow = dataContainer.getRow(getRecipientProfileTable(), profileId);

        chat.setId(chatRow.getPKValue());
        chat.setUnreadCount(chatRow.get(MSOCHAT.UNREAD_COUNT));
        chat.setPlatform(Platform.get((short) chatRow.get(MSOCHAT.PLATFORM)));

        lastMessage.setMessageFlow(Message.MessageFlow.get((short) chatRow.get(MSOCHAT.LAST_MESSAGE_TYPE)));
        lastMessage.setSentTime(chatRow.get(MSOCHAT.LAST_MESSAGE_TIME));
        chat.setLastMessage(lastMessage);

        if (Objects.nonNull(channelRow)) {
            channel.setName(channelRow.get(MSOCHANNEL.NAME));
            channel.setType(Channel.ChannelType.get(channelRow.get(MSOCHANNEL.TYPE)));
        }
        channel.setId(channelId);
        chat.setChannel(channel);

        if (Objects.nonNull(profileRow)) {
            recipientProfile.setName(profileRow.get(MSORECIPIENTPROFILE.NAME));
        }
        recipientProfile.setId(profileId);
        chat.setProfile(recipientProfile);

        return chat;
    }

    /**
     * <p>
     * Checks whether the chat exists or not
     * </p>
     *
     * @param chatIds Represents the chat ids
     * @return true if it exists, false otherwise
     */
    @Override
    public boolean exists(final Collection<Long> chatIds) {
        final Table chatTable = getChatTable();
        final CountQuery countQuery = CountQuery.get(chatTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(chatTable.getPKColumn(), chatIds)));

        return chatIds.size() == adminDataStore().get(countQuery);
    }

    /**
     * <p>
     * Loads {@link Chat} into the container
     * </p>
     *
     * @param chat            Represents the {@link Chat}
     * @param dataContainer   Represents the container which contains the chat details
     * @param targetContainer Represents the target container
     */
    public void loadChatIntoContainer(final Chat chat, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Long chatId = chat.id();
        final Table chatTable = getChatTable();
        final Row chatRow = dataContainer.getOrInitRow(chatTable, chatId);
        final int unreadCount = chat.getUnreadCount();

        if (chatRow.isNewRow()) {
            chatRow.set(MSOCHAT.CHANNEL_ID, chat.getChannel().id());
            chatRow.set(MSOCHAT.PROFILE_ID, chat.getProfile().id());
            chatRow.set(MSOCHAT.PLATFORM, chat.getPlatform().getId());
            chatRow.set(MSOCHAT.UNREAD_COUNT, unreadCount);

            chat.setId(chatRow.getPKValue());
        } else if (INBOUND == chat.getLastMessage().getMessageFlow()) {
            final int existingUnreadCount = chatRow.get(MSOCHAT.UNREAD_COUNT);

            chatRow.set(MSOCHAT.UNREAD_COUNT, existingUnreadCount + unreadCount);
        }
        chatRow.set(MSOCHAT.LAST_MESSAGE_TYPE, chat.getLastMessageType().getId());
        chatRow.set(MSOCHAT.LAST_MESSAGE_TIME, chat.getLastMessageTime());

        targetContainer.updateRow(chatRow);
    }

    /**
     * <p>
     * Loads the message into container
     * </p>
     *
     * @param chat            Represents the {@link Chat}
     * @param dataContainer   Represents the data container which contain message details
     * @param targetContainer Represents the target container
     */
    public void loadMessageIntoContainer(final Chat chat, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table messageTale = getMessageTable();
        final Long chatId = chat.id();
        final Message message = chat.getLastMessage();
        final Row messageRow = dataContainer.getOrInitRow(messageTale, message.id());
        final Collection<Attachment> attachments = message.getAttachments();

        if (messageRow.isNewRow()) {
            messageRow.set(MSOMESSAGE.CHAT_ID, chatId);
            messageRow.set(MSOMESSAGE.MESSAGE_FLOW, message.getMessageFlow().getId());
            messageRow.set(MSOMESSAGE.SEND_TIME, message.getSentTime());
            messageRow.set(MSOMESSAGE.DELIVERED_TIME, message.getDeliveredTime());
            messageRow.set(MSOMESSAGE.READ_TIME, message.getReadTime());
            messageRow.set(MSOMESSAGE.STATE, message.getMessageState().getId());

            message.setId(messageRow.getPKValue());
        }
        messageRow.set(MSOMESSAGE.CONTENT, message.getContent());

        if (Objects.nonNull(attachments) && !attachments.isEmpty()) {
            final Table attachmentTable = getAttachmentTable();
            final Long messageId = message.id();

            for (final Attachment attachment : attachments) {
                final Long attachmentId = attachment.id();
                final Row attachmentRow = new Row(attachmentTable);

                attachmentRow.set(MSOATTACHMENT.ATTACHMENT_ID, attachmentId);
                attachmentRow.set(MSOATTACHMENT.MESSAGE_ID, messageId);
                targetContainer.updateRow(attachmentRow);
            }
        }
        targetContainer.updateRow(messageRow);
    }

    /**
     * <p>
     * Updates the message status of the provided message ids
     * </p>
     *
     * @param messageIds   Represents the message ids to be updated
     * @param timestamp    Represents the timestamp
     * @param messageState Represents the {@link Message.MessageState}
     * @param failedReason Represents the message failure reason
     */
    public void updateMessageStatus(final Collection<Long> messageIds, final Long timestamp, final Message.MessageState messageState, final String failedReason) {
        final Table messageTable = getMessageTable();
        final UpdateQuery updateQuery = UpdateQuery.get(messageTable);

        updateQuery.addUpdateColumn(messageTable.getColumn(MSOMESSAGE.STATE), messageState.getId());

        switch (messageState) {
            case READ -> updateQuery.addUpdateColumn(messageTable.getColumn(MSOMESSAGE.READ_TIME), timestamp);
            case SENT -> updateQuery.addUpdateColumn(messageTable.getColumn(MSOMESSAGE.SEND_TIME), timestamp);
            case DELIVERED -> updateQuery.addUpdateColumn(messageTable.getColumn(MSOMESSAGE.DELIVERED_TIME), timestamp);
            case FAILED -> updateQuery.addUpdateColumn(messageTable.getColumn(MSOMESSAGE.FAILED_REASON), failedReason);
            default -> {
            }
        }
        updateQuery.setWhereClause(new WhereClause(Criteria.in(messageTable.getPKColumn(), messageIds)));

        adminDataStore().update(updateQuery);
    }

    /**
     * <p>
     * Gets the collection of {@link Message} by the provided chat id.
     * </p>
     *
     * @param ids Message ids.
     * @return the collection of {@link Message}.
     */
    public Collection<Message> getMessages(final Collection<Long> ids) {
        final Table messageTable = getMessageTable();
        final Table attachmentTable = getAttachmentTable();
        final SelectQuery selectQuery = SelectQuery.get(messageTable);
        final Column messagePkColumn = messageTable.getPKColumn();

        selectQuery.addJoinClause(new JoinClause(messagePkColumn, attachmentTable.getColumn(MSOATTACHMENT.MESSAGE_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(messagePkColumn, ids)));
        final DataContainer dataContainer = adminDataStore().get(selectQuery);

        return dataContainer.getRows(messageTable).map(messageRow -> convertRowToMessage(messageRow, dataContainer)).toList();
    }

    /**
     * <p>
     * Gets send, delivered, and read timestamps for the provided message ids.
     * </p>
     *
     * @param messageIds Represents the message ids
     * @return A map of message id to {@link MessageTimestamps}; missing ids are omitted
     */
    public Map<Long, MessageTimestamps> getMessageTimestamps(final Collection<Long> messageIds) {
        if (Objects.isNull(messageIds) || messageIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final Table messageTable = getMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(messageTable);
        final Column messagePkColumn = messageTable.getPKColumn();

        selectQuery.addSelectClause(List.of(messagePkColumn, messageTable.getColumn(MSOMESSAGE.SEND_TIME),
                messageTable.getColumn(MSOMESSAGE.DELIVERED_TIME), messageTable.getColumn(MSOMESSAGE.READ_TIME)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(messagePkColumn, messageIds)));
        final Map<Long, MessageTimestamps> messageTimestampsMap = new HashMap<>(messageIds.size());

        adminDataStore().get(selectQuery).getRows(messageTable).forEach(messageRow ->
                messageTimestampsMap.put(messageRow.getPKValue(), new MessageTimestamps(messageRow.get(MSOMESSAGE.SEND_TIME),
                        messageRow.get(MSOMESSAGE.DELIVERED_TIME), messageRow.get(MSOMESSAGE.READ_TIME))));

        return messageTimestampsMap;
    }

    /**
     * <p>
     * Converts the message row into {@link Message}
     * </p>
     *
     * @param messageRow Represents the row which contains message details
     * @return the {@link Message}
     */
    private Message convertRowToMessage(final Row messageRow, final DataContainer dataContainer) {
        final Table attachmentTable = getAttachmentTable();
        final Message message = new Message();
        final Long messageId = messageRow.getPKValue();
        final Collection<Attachment> attachments = new ArrayList<>();

        message.setId(messageId);
        message.setContent(messageRow.get(MSOMESSAGE.CONTENT));
        message.setMessageFlow(Message.MessageFlow.get((short) messageRow.get(MSOMESSAGE.MESSAGE_FLOW)));
        message.setMessageState(Message.MessageState.get((short) messageRow.get(MSOMESSAGE.STATE)));
        message.setSentTime(messageRow.get(MSOMESSAGE.SEND_TIME));
        message.setReadTime(messageRow.get(MSOMESSAGE.READ_TIME));
        message.setDeliveredTime(messageRow.get(MSOMESSAGE.DELIVERED_TIME));
        message.setFailedReason(messageRow.get(MSOMESSAGE.FAILED_REASON));

        dataContainer.getRows(attachmentTable, new WhereClause(Criteria.eq(attachmentTable.getColumn(MSOATTACHMENT.MESSAGE_ID), messageId)))
                .forEach(attachmentRow -> {
                    final Attachment attachment = new Attachment();

                    attachment.setId(attachmentRow.get(MSOATTACHMENT.ATTACHMENT_ID));
                    attachments.add(attachment);
                });
        message.setAttachments(attachments);

        return message;
    }

    /**
     * <p>
     * Loads the {@link Channel} into the container
     * </p>
     *
     * @param channel         Represents the {@link Channel}
     * @param dataContainer   Represents the container which contains existing channel data
     * @param targetContainer Represents the target container
     */
    public void loadChannelIntoContainer(final Channel channel, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table channelTable = getChannelTable();
        final Row channelRow = dataContainer.getOrInitRow(channelTable, channel.id());

        if (channelRow.isNewRow()) {
            channel.setId(channelRow.getPKValue());
            channelRow.set(MSOCHANNEL.TYPE, channel.getType().getId());
        }
        channelRow.set(MSOCHANNEL.NAME, channel.getName());

        targetContainer.updateRow(channelRow);
    }

    /**
     * <p>
     * Checks whether the 24-hour messaging window is expired or not for the provided chat id
     * </p>
     *
     * @param chatId Represents the chat id
     */
    public boolean hasMessageWindowExpired(final Long chatId) {
        final Table messageTable = getMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(messageTable);
        final Column sentTimeColumn = messageTable.getColumn(MSOMESSAGE.SEND_TIME);

        selectQuery.addSelectClause(sentTimeColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(messageTable.getColumn(MSOMESSAGE.CHAT_ID), chatId))
                .and(Criteria.eq(messageTable.getColumn(MSOMESSAGE.MESSAGE_FLOW), INBOUND.getId())));
        selectQuery.addOrderByClause(new OrderByClause(sentTimeColumn, OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(new LimitClause(NumericalConstant.ONE));
        final Row messageRow = adminDataStore().get(selectQuery).getRow(messageTable);

        return Objects.isNull(messageRow) || Instant.now().toEpochMilli() >= TemporalServiceProvider.get((Long) messageRow.get(MSOMESSAGE.SEND_TIME)).plus(24, DateInterval.HOURS);
    }

    /**
     * <p>
     * Resets the unread count of the chat to zero
     * </p>
     * @param chatId Represents the chat id
     */
    public void resetUnreadCount(final Long chatId) {
        final Table chatTable = getChatTable();
        final UpdateQuery updateQuery = UpdateQuery.get(chatTable);

        updateQuery.addUpdateColumn(chatTable.getColumn(MSOCHAT.UNREAD_COUNT), NumericalConstant.ZERO);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(chatTable.getPKColumn(), chatId)));

        adminDataStore().update(updateQuery);
    }

    /**
     * <p>
     * Gets the optional of {@link RecipientProfile} from the provided chat id
     * </p>
     *
     * @param chatId Represents the chat id
     * @return the {@link RecipientProfile}
     */
    public Optional<RecipientProfile> getRecipientProfile(final Long chatId) {
        final Table recipientTable = getRecipientTable();
        final Table recipientProfileTable = getRecipientProfileTable();
        final SelectQuery selectQuery = SelectQuery.get(recipientTable);

        selectQuery.addSelectClause(List.of(recipientProfileTable.getColumn(MSORECIPIENTPROFILE.NAME), recipientProfileTable.getColumn(MSORECIPIENTPROFILE.PROFILE_PIC_ID)));
        selectQuery.addJoinClause(new JoinClause(recipientTable.getColumn(MSORECIPIENT.PROFILE_ID), recipientProfileTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(recipientTable.getColumn(MSORECIPIENT.CHAT_ID), chatId)));
        final Row row = adminDataStore().get(selectQuery).getRow(recipientProfileTable);

        return Objects.isNull(row) ? Optional.empty() : Optional.of(convertRowToRecipientProfile(row));
    }

    /**
     * <p>
     * Gets the optional of {@link Channel} from the provided chat id
     * </p>
     *
     * @param chatId Represents the chat id
     * @return the {@link Channel}
     */
    public Optional<Channel> getChannel(final Long chatId) {
        final Table chatTable = getChatTable();
        final Table channelTable = getChannelTable();
        final SelectQuery selectQuery = SelectQuery.get(chatTable);

        selectQuery.addSelectClause(channelTable);
        selectQuery.addJoinClause(new JoinClause(chatTable.getColumn(MSOCHAT.CHANNEL_ID), channelTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(chatTable.getPKColumn(), chatId)));
        final Row row = adminDataStore().get(selectQuery).getRow(channelTable);

        if (Objects.isNull(row)) {
            return Optional.empty();
        } else {
            final Channel channel = new Channel();

            channel.setId(row.getPKValue());
            channel.setName(row.get(MSOCHANNEL.NAME));
            channel.setType(Channel.ChannelType.get(row.get(MSOCHANNEL.TYPE)));

            return Optional.of(channel);
        }
    }
    /**
     * <p>
     * Converts the recipient profile row into {@link RecipientProfile}
     * </p>
     *
     * @param recipientProfileRow Represents the recipient profile row
     * @return the {@link RecipientProfile}
     */
    private RecipientProfile convertRowToRecipientProfile(final Row recipientProfileRow) {
        final RecipientProfile recipientProfile = new RecipientProfile();
        final Attachment profilePicture = new Attachment();

        recipientProfile.setId(recipientProfileRow.getPKValue());
        recipientProfile.setName(recipientProfileRow.get(MSORECIPIENTPROFILE.NAME));
        profilePicture.setId(recipientProfileRow.get(MSORECIPIENTPROFILE.PROFILE_PIC_ID));
        recipientProfile.setProfilePicture(profilePicture);

        return recipientProfile;
    }
}
