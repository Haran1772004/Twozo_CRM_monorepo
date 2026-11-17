package com.tlc.crm.messenger.api.chat.manager;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.conversation.api.model.EntityConversation;
import com.tlc.crm.core.features.timeline.manager.ReadableTimelineManager;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.field.TimelineFields;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.icon.service.IconService;
import com.tlc.crm.messenger.api.channel.manager.FBPageManager;
import com.tlc.crm.messenger.api.channel.manager.InstagramPageManager;
import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.messenger.api.channel.model.ChannelState;
import com.tlc.crm.messenger.api.channel.model.CrmFBPage;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.messenger.api.chat.model.CrmChatFilter;
import com.tlc.crm.messenger.config.manager.AbstractMessengerManager;
import com.tlc.crm.messenger.sql.resource.OMECCHATCONVERSATIONS;
import com.tlc.crm.messenger.sql.resource.OMSOCHAT;

import com.tlc.crm.messenger.sql.resource.OMSOFBPAGE;
import com.tlc.crm.messenger.sql.resource.OMSOINSTAGRAMPAGE;
import com.tlc.crm.messenger.sql.resource.OMSOWABUSINESSPHONENUMBER;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.social.common.fields.ChatFields;
import com.tlc.social.common.model.Channel;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.RecipientProfile;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.UpdateQuery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>
 * Manages chat related data base actions.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class ChatManager extends AbstractMessengerManager {

    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final CommonTimelineManager commonTimelineManager;
    private final ReadableTimelineManager readableTimelineManager;
    private final IconService iconService;
    private final FBPageManager fbPageManager;
    private final InstagramPageManager instagramPageManager;
    private final WABusinessAccountManager waBusinessAccountManager;
    private final FieldService fieldService;

    private ChatManager() {
        contactManager = ContactManager.getInstance();
        dealManager = DealManager.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        readableTimelineManager = ReadableTimelineManager.getInstance();
        iconService = IconService.getInstance();
        fbPageManager = FBPageManager.getInstance();
        instagramPageManager = InstagramPageManager.getInstance();
        waBusinessAccountManager = WABusinessAccountManager.getInstance();
        fieldService = FieldService.getInstance();
    }

    private static final class Instance {
        private static final ChatManager CHAT_MANAGER = new ChatManager();
    }

    /**
     * <p>
     * Gets {@link ChatManager}.
     * </p>
     *
     * @return {@link ChatManager}.
     */
    public static ChatManager getInstance() {
        return Instance.CHAT_MANAGER;
    }

    /**
     * <p>
     * Gets Collection of {@link CrmChat}.
     * </p>
     *
     * @param orgId          Organization's id.
     * @param start          Starting index of chat to be fetched.
     * @param limit          Maximum number of chat to be fetched.
     * @param filter         {@link CrmChatFilter}
     * @param searchCriteria Represents the search criteria
     * @return Collection of {@link CrmChat}.
     */
    public Collection<CrmChat> get(final Long orgId, final int start, final int limit, final CrmChatFilter filter, final String searchCriteria) {
        final Table chatTable = getChatTable();
        final SelectQuery chatQuery = SelectQuery.get(chatTable);

        chatQuery.setWhereClause(getFilterWhereClause(filter, searchCriteria));
        chatQuery.setLimitClause(new LimitClause(start, limit));
        chatQuery.addSelectClause(chatTable);
        chatQuery.addOrderByClause(new OrderByClause(chatTable.getColumn(OMSOCHAT.UPDATED_AT), OrderByClause.OrderType.DESCENDING));
        final Stream<Row> rows = getOrgDataStore(orgId).get(chatQuery).getRows(chatTable);

        return rows.map(this::convertRowToPage).toList();
    }

    /**
     * <p>
     * Checks whether the chat exists.
     * </p>
     *
     * @param orgId  Organization's id.
     * @param chatId Chat's id.
     * @return whether the chat exists.
     */
    public boolean exists(final Long orgId, final Long chatId) {
        final Table chatTable = getChatTable();
        final CountQuery countQuery = CountQuery.get(chatTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(chatTable.getPKColumn(), chatId)));

        return getOrgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Gets {@link CrmFBPage} count.
     * </p>
     *
     * @param orgId          Organization's id.
     * @param searchCriteria Represents the search criteria
     * @return gets {@link CrmFBPage} count.
     */
    public long getChatCount(final Long orgId, final CrmChatFilter filter, final String searchCriteria) {
        final Table chatTable = getChatTable();
        final CountQuery countQuery = CountQuery.get(chatTable);
        final WhereClause whereClause = getFilterWhereClause(filter, searchCriteria);

        if (Objects.nonNull(whereClause)) {
            countQuery.setWhereClause(whereClause);
        }

        return getOrgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Un-assigns owner from chat.
     * </p>
     *
     * @param chat Represents the {@link CrmChat}
     */
    public void updateOwner(final CrmChat chat) {
        final Table chatTable = getChatTable();
        final UpdateQuery chatUpdateQuery = UpdateQuery.get(chatTable);
        final CrmUser owner = chat.getOwner();

        chatUpdateQuery.addUpdateColumn(chatTable.getColumn(OMSOCHAT.OWNER_ID), Objects.nonNull(owner) ? owner.id() : null);
        chatUpdateQuery.addUpdateColumn(chatTable.getColumn(OMSOCHAT.UPDATED_AT), Instant.now().toEpochMilli());
        chatUpdateQuery.setWhereClause(new WhereClause(Criteria.eq(chatTable.getPKColumn(), chat.getCrmChatId())));
        getOrgDataStore(chat.orgId()).update(chatUpdateQuery);
    }

    /**
     * <p>
     * Resets the unread count of the chat to zero
     * </p>
     *
     * @param orgId  Represents the org id
     * @param chatId Represents the chat id
     */
    public void resetUnreadCount(final Long orgId, final Long chatId) {
        final Table chatTable = getChatTable();
        final UpdateQuery updateQuery = UpdateQuery.get(chatTable);

        updateQuery.addUpdateColumn(chatTable.getColumn(OMSOCHAT.UNREAD_COUNT), NumericalConstant.ZERO);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(chatTable.getColumn(OMSOCHAT.CHAT_ID), chatId)));

        getOrgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Links chat to the entity.
     * </p>
     *
     * @param orgId        Represents the org id
     * @param userId       Represents the user id
     * @param chat         Represents the {@link CrmChat}
     * @param entityId     Represents the entity id
     * @param entityColumn Represents the entity column in chat table
     * @param source       Represents the {@link Source}
     */
    public void linkToEntity(final Long orgId, final Long userId, final CrmChat chat, final Long entityId, final String entityColumn, final Source source, final DataContainer targetContainer) {
        final Long crmChatId = chat.getCrmChatId();
        final Table chatTable = getChatTable();
        final Row chatRow = getChatContainer(orgId, crmChatId).getRow(chatTable, crmChatId);

        if (Objects.nonNull(chatRow)) {
            chatRow.set(entityColumn, entityId);
            chatRow.set(chatTable.getColumn(OMSOCHAT.UPDATED_AT), Instant.now().toEpochMilli());
            final String recipientProfileName = chatRow.get(OMSOCHAT.RECIPIENT_PROFILE_NAME);

            if (Source.CONTACT == source && Objects.nonNull(recipientProfileName)) {
                final String contactFullName = contactManager.getContactFullName(orgId, entityId);
                final int delimiterIndex = recipientProfileName.lastIndexOf('|');

                if (-1 != delimiterIndex) {
                    final String updatedProfileName = recipientProfileName.replace(recipientProfileName.substring(0, delimiterIndex), contactFullName);

                    chatRow.set(chatTable.getColumn(OMSOCHAT.RECIPIENT_PROFILE_NAME), updatedProfileName);
                }
            }

            targetContainer.updateRow(chatRow);
        }
        loadTimeline(orgId, userId, entityId, chat, source, targetContainer);
        loadEntityConversation(userId, entityId, source, chat, targetContainer);
    }

    /**
     * <p>
     * Gets the chat container from the provided chat id
     * </p>
     *
     * @param orgId     Represents the org id
     * @param crmChatId Represents the chat id
     * @return the container
     */
    private DataContainer getChatContainer(final Long orgId, final Long crmChatId) {
        final Table chatTable = getChatTable();
        final SelectQuery selectQuery = SelectQuery.get(chatTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(chatTable.getPKColumn(), crmChatId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Creates a new contact and links it to the chat
     * </p>
     *
     * @param orgId   Represents the org id
     * @param chat    Represents the chat
     * @param contact Represents the contact to be created and linked
     */
    public void linkToNewContact(final Long orgId, final Long userId, final CrmChat chat, final Contact contact) {
        final DataContainer targetContainer = DataContainer.create();

        contactManager.loadContactIntoContainer(true, contact, targetContainer);
        final Long contactId = contact.id();

        loadEntityIntoContainer(orgId, chat.getCrmChatId(), OMSOCHAT.CONTACT_ID, targetContainer, Source.CONTACT, contact);
        loadTimeline(orgId, contact.getOwnerId(), contactId, chat, Source.CONTACT, targetContainer);
        loadEntityConversation(userId, contactId, Source.CONTACT, chat, targetContainer);

        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the entity conversation for chat into the container
     * </p>
     *
     * @param userId          Represents the user id
     * @param entityId        Represents the entity id
     * @param source          Represents the source
     * @param crmChat         Represents the {@link CrmChat}
     * @param targetContainer Represents the target container
     */
    private void loadEntityConversation(final Long userId, final Long entityId, final Source source, final CrmChat crmChat, final DataContainer targetContainer) {
        final Long orgId = crmChat.orgId();
        final Long crmChatId = crmChat.getCrmChatId();
        final DataContainer entityConversationContainer = getEntityConversationContainer(orgId, crmChatId, source);
        final Table entityConversationTable = Table.get(OMECENTITYCONVERSATIONS.TABLE);
        final Table chatEntityConversationTable = getChatEntityConversationTable();
        final Row entityConversationRow = entityConversationContainer.getOrInitRow(entityConversationTable,
                new WhereClause(Criteria.in(entityConversationTable.getColumn(OMECENTITYCONVERSATIONS.TYPE), EntityConversation.Type.getSocialTypes().stream().map(EntityConversation.Type :: getId).toList())));
        final Long entityConversationId = entityConversationRow.getPKValue();
        final Row chatEntityConversationRow = entityConversationContainer.getOrInitRow(chatEntityConversationTable, new WhereClause(Criteria.eq(chatEntityConversationTable.getColumn(OMECCHATCONVERSATIONS.CHAT_ID), crmChatId))
                .and(Criteria.eq(chatEntityConversationTable.getColumn(OMECCHATCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationId)));

        if (entityConversationRow.isNewRow()) {
            entityConversationRow.setOrgId(orgId);
            entityConversationRow.set(OMECENTITYCONVERSATIONS.TYPE, switch (crmChat.getChannel().getType()) {
                case FB_PAGE -> EntityConversation.Type.FACEBOOK.getId();
                case WHATSAPP_BUSINESS -> EntityConversation.Type.WHATSAPP.getId();
                case INSTAGRAM_PAGE -> EntityConversation.Type.INSTAGRAM.getId();
            });
            entityConversationRow.set(OMECENTITYCONVERSATIONS.USER_ID, userId);
        }
        entityConversationRow.set(OMECENTITYCONVERSATIONS.CREATED_AT, Instant.now().toEpochMilli());
        targetContainer.updateRow(entityConversationRow);

        if (chatEntityConversationRow.isNewRow()) {
            chatEntityConversationRow.setOrgId(orgId);
            chatEntityConversationRow.set(OMECCHATCONVERSATIONS.CHAT_ID, crmChatId);
            chatEntityConversationRow.set(OMECCHATCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationId);
            targetContainer.addNewRow(chatEntityConversationRow);
        }

        switch (source) {
            case CONTACT -> loadContactEntityConversation(orgId, entityConversationId, entityId, entityConversationContainer, targetContainer);
            case DEAL -> {
                final Row dealEntityConversationRow = new Row(Table.get(OMECDEALCONVERSATIONS.TABLE));

                dealEntityConversationRow.set(OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationId);
                dealEntityConversationRow.set(OMECDEALCONVERSATIONS.DEAL_ID, entityId);

                targetContainer.addNewRow(dealEntityConversationRow);
            }
        }
    }

    /**
     * <p>
     * Gets the entity conversation container which contains chat entity conversation details
     * </p>
     *
     * @param orgId  Represents the org id
     * @param chatId Represents the chat id
     * @param source Represents the source of the entity
     * @return the data container
     */
    private DataContainer getEntityConversationContainer(final Long orgId, final Long chatId, final Source source) {
        final Table entityConversationTable = Table.get(OMECENTITYCONVERSATIONS.TABLE);
        final Table chatEntityConversationTable = getChatEntityConversationTable();
        final Table contactEntityConversationTable = Table.get(OMECCONTACTCONVERSATIONS.TABLE);
        final Table companyEntityConversationTable = Table.get(OMECCOMPANYCONVERSATIONS.TABLE);
        final Column entityConversationPkColumn = entityConversationTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(entityConversationTable);
        final List<JoinClause> joins = new ArrayList<>();

        joins.add(new JoinClause(entityConversationPkColumn, chatEntityConversationTable.getColumn(OMECCHATCONVERSATIONS.ENTITY_CONVERSATION_ID)));

        if (Source.CONTACT == source) {
            joins.add(new JoinClause(entityConversationPkColumn, contactEntityConversationTable.getColumn(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID), JoinClause.JoinType.LEFT));
            joins.add(new JoinClause(entityConversationPkColumn, companyEntityConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID), JoinClause.JoinType.LEFT));
        }
        selectQuery.addJoinClause(joins);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(chatEntityConversationTable.getColumn(OMECCHATCONVERSATIONS.CHAT_ID), chatId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Loads the contact entity conversation along with its company entity conversation into the data container
     * </p>
     *
     * @param orgId                Represents the org id
     * @param entityConversationId Represents the entity conversation id
     * @param contactId            Represents the contact id
     * @param targetContainer      Represents the target container
     */
    private void loadContactEntityConversation(final Long orgId, final Long entityConversationId, final Long contactId, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table contactEntityConversationTable = Table.get(OMECCONTACTCONVERSATIONS.TABLE);
        final Table contactCompanyTable = contactManager.getContactCompanyTable();
        final String companyKey = ContactFields.COMPANY;
        final Optional<FieldInfo> contactCompanyField = fieldService.getDefaultFieldByAliasName(Source.CONTACT, companyKey);
        final Row contactEntityConversationRow = dataContainer.getOrInitRow(contactEntityConversationTable,
                new WhereClause(Criteria.eq(contactEntityConversationTable.getColumn(OMECCONTACTCONVERSATIONS.CONTACT_ID), contactId))
                        .and(Criteria.eq(contactEntityConversationTable.getColumn(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationId)));

        if (contactEntityConversationRow.isNewRow()) {
            contactEntityConversationRow.setOrgId(orgId);
            contactEntityConversationRow.set(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationId);
            contactEntityConversationRow.set(OMECCONTACTCONVERSATIONS.CONTACT_ID, contactId);
            targetContainer.addNewRow(contactEntityConversationRow);
        }
        final Row contactCompanyRow = contactCompanyField.isPresent() ? targetContainer.getRow(contactCompanyTable,
                new WhereClause(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), contactCompanyField.get().id()))) : null;
        final Optional<Long> optionalCompanyId = Objects.nonNull(contactCompanyRow) ? Optional.of(contactCompanyRow.get(MCONCOMPANYFIELDS.COMPANY_ID)) : contactManager.getRelatedCompanyId(orgId, contactId);

        if (optionalCompanyId.isPresent()) {
            final Long companyId = optionalCompanyId.get();
            final Table companyEntityConversationTable = Table.get(OMECCOMPANYCONVERSATIONS.TABLE);
            final Row companyEntityConversationRow = dataContainer.getOrInitRow(companyEntityConversationTable,
                    new WhereClause(Criteria.eq(companyEntityConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.COMPANY_ID), companyId))
                            .and(Criteria.eq(companyEntityConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationId)));

            if (companyEntityConversationRow.isNewRow()) {
                companyEntityConversationRow.setOrgId(orgId);
                companyEntityConversationRow.set(OMECCOMPANYCONVERSATIONS.COMPANY_ID, companyId);
                companyEntityConversationRow.set(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationId);

                targetContainer.addNewRow(companyEntityConversationRow);
            }
        }
    }

    /**
     * <p>
     * Loads chat timeline into container
     * </p>
     *
     * @param orgId           Represents the org id
     * @param userId          Represents the user id
     * @param entityId        Represents the entity id
     * @param chat            Represents the {@link CrmChat}
     * @param source          Represents the {@link Source}
     * @param targetContainer Represents the target container
     */
    private void loadTimeline(final Long orgId, final Long userId, final Long entityId, final CrmChat chat, final Source source, final DataContainer targetContainer) {
        final Timeline timeline = buildTimeline(orgId, userId, chat);

        commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), targetContainer);
        final Long timelineId = timeline.id();

        switch (source) {
            case CONTACT ->
                    commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, entityId, MTLCONTACTTIMELINE.CONTACT_ID,
                            MTLCONTACTTIMELINE.TIMELINE_ID, new Row(readableTimelineManager.getContactTimelineTable()), targetContainer);
            case DEAL ->
                    commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, entityId, MTLDEALTIMELINE.DEAL_ID,
                            MTLDEALTIMELINE.TIMELINE_ID, new Row(readableTimelineManager.getDealTimelineTable()), targetContainer);
        }
    }

    /**
     * <p>
     * Builds the {@link Timeline} for chat assignment and returns it
     * </p>
     *
     * @param orgId  Represents the org id
     * @param userId Represents the user id
     * @param chat   Represents the {@link CrmChat}
     * @return the {@link Timeline}
     */
    private Timeline buildTimeline(final Long orgId, final Long userId, final CrmChat chat) {
        final Timeline timeline = new Timeline();
        final Channel channel = chat.getChannel();
        final String channelName = channel.getName();

        timeline.setOrgId(orgId);
        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));
        timeline.setAction(TimelineActionFields.ASSIGNED);
        timeline.setOwnerId(userId);
        timeline.setCategory(Timeline.Category.CHATS);
        timeline.setTimelineType(TimelineType.ASSIGNED_TO_CHAT);
        final JsonObject valueJson = Json.object();

        valueJson.put(ChatFields.CHAT_ID, chat.getCrmChatId());

        switch (channel.getType()) {
            case FB_PAGE -> {
                valueJson.put(ChatFields.CHANNEL_NAME, channelName);
                timeline.setIcon(iconService.fetchIconByTypeAndName(Icon.Type.TIMELINE, TimelineFields.FACEBOOK.getName()));
            }
            case INSTAGRAM_PAGE -> {
                valueJson.put(ChatFields.CHANNEL_NAME, channelName);
                timeline.setIcon(iconService.fetchIconByTypeAndName(Icon.Type.TIMELINE, TimelineFields.INSTAGRAM.getName()));
            }
            case WHATSAPP_BUSINESS -> {
                valueJson.put(ChatFields.CHANNEL_NAME, String.format("Whatsapp Number (%s)", channelName));
                timeline.setIcon(iconService.fetchIconByTypeAndName(Icon.Type.TIMELINE, TimelineFields.WHATSAPP.getName()));
            }
        }
        timeline.setFieldTimeline(new FieldTimeline(valueJson.toString()));

        return timeline;
    }

    /**
     * <p>
     * Creates a new deal and links it to the chat
     * </p>
     *
     * @param orgId Represents the org id
     * @param chat  Represents the {@link CrmChat}
     * @param deal  Represents the deal to be created and linked
     */
    public void linkToNewDeal(final Long orgId, final Long userId, final CrmChat chat, final Deal deal) {
        final DataContainer targetContainer = DataContainer.create();

        dealManager.loadDealIntoContainer(true, deal, targetContainer);
        final Long dealId = deal.id();

        loadEntityIntoContainer(orgId, chat.getCrmChatId(), OMSOCHAT.DEAL_ID, targetContainer, Source.DEAL, deal);
        loadTimeline(orgId, deal.getOwnerId(), dealId, chat, Source.DEAL, targetContainer);
        loadEntityConversation(userId, dealId, Source.DEAL, chat, targetContainer);

        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the entity into container to link with the chat
     * </p>
     *
     * @param orgId           Represents the org id
     * @param chatId          Represents the chat id
     * @param entityColumn    Represents the entity column
     * @param targetContainer Represents the target container
     * @param entity          Represents the entity to be linked
     * @param source          Represents the source of the entity
     */
    private void loadEntityIntoContainer(final Long orgId, final Long chatId, final String entityColumn, final DataContainer targetContainer, final Source source, final Entity entity) {
        final Table chatTable = getChatTable();
        final SelectQuery selectQuery = SelectQuery.get(chatTable);
        final Long entityId = entity.id();

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(chatTable.getPKColumn(), chatId)));
        final Row chatRow = getOrgDataStore(orgId).get(selectQuery).getRow(chatTable);

        chatRow.set(chatTable.getColumn(entityColumn), entityId);
        chatRow.set(chatTable.getColumn(OMSOCHAT.UPDATED_AT), Instant.now().toEpochMilli());
        final String recipientProfileName = chatRow.get(OMSOCHAT.RECIPIENT_PROFILE_NAME);

        if (Source.CONTACT == source && Objects.nonNull(recipientProfileName)) {
            final Contact contact = (Contact) entity;
            final int delimiterIndex = recipientProfileName.lastIndexOf('|');

            if (-1 != delimiterIndex) {
                final String updatedProfileName = recipientProfileName.replace(recipientProfileName.substring(0, delimiterIndex), contact.getFullName());

                chatRow.set(chatTable.getColumn(OMSOCHAT.RECIPIENT_PROFILE_NAME), updatedProfileName);
            }
        }
        targetContainer.updateRow(chatRow);
    }

    /**
     * <p>
     * Gets entity id.
     * </p>
     *
     * @param orgId        Organization's id.
     * @param chatId       Chat id.
     * @param entityColumn Such as deal_id, contact_id.
     * @return entity id.
     */
    public Long getEntityId(final Long orgId, final Long chatId, final String entityColumn) {
        final Table chatTable = getChatTable();
        final SelectQuery chatQuery = SelectQuery.get(chatTable);

        chatQuery.addSelectClause(chatTable);
        chatQuery.setWhereClause(new WhereClause(Criteria.eq(chatTable.getPKColumn(), chatId)));
        final Row row = getOrgDataStore(orgId).get(chatQuery).getRow(chatTable);

        return Objects.isNull(row) ? null : row.get(entityColumn);
    }

    /**
     * <p>
     * Gets the optional chat id from the crm chat id
     * </p>
     *
     * @param orgId     Represents the org id
     * @param crmChatId Represents the crm chat id
     * @return the optional of chat id
     */
    public Optional<Long> getChatId(final Long orgId, final Long crmChatId) {
        final Table chatTable = getChatTable();
        final SelectQuery selectQuery = SelectQuery.get(chatTable);

        selectQuery.addSelectClause(chatTable.getColumn(OMSOCHAT.CHAT_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(chatTable.getPKColumn(), crmChatId)));
        final Row chatRow = getOrgDataStore(orgId).get(selectQuery).getRow(chatTable);

        return Objects.isNull(chatRow) ? Optional.empty() : Optional.ofNullable(chatRow.get(OMSOCHAT.CHAT_ID));
    }

    /**
     * <p>
     * Gets the optional crm chat id from the chat id
     * </p>
     *
     * @param orgId  Represents the org id
     * @param chatId Represents the chat id
     * @return the optional of crm chat id
     */
    public Optional<Long> getCrmChatId(final Long orgId, final Long chatId) {
        final Table chatTable = getChatTable();
        final SelectQuery selectQuery = SelectQuery.get(chatTable);
        final Column chatIdColumn = chatTable.getColumn(OMSOCHAT.CHAT_ID);

        selectQuery.addSelectClause(chatIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(chatIdColumn, chatId)));
        final Row chatRow = getOrgDataStore(orgId).get(selectQuery).getRow(chatTable);

        return Objects.isNull(chatRow) ? Optional.empty() : Optional.ofNullable(chatRow.getPKValue());
    }

    /**
     * <p>
     * Deletes the chat
     * </p>
     *
     * @param orgId  Represents the org id
     * @param chatId Represents the chat id
     */
    public void delete(final Long orgId, final Long chatId) {
        final Table chatTable = getChatTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(chatTable);

        deleteQuery.setWhereClause(new WhereClause(Criteria.eq(chatTable.getPKColumn(), chatId)));

        getOrgDataStore(orgId).delete(deleteQuery);
    }

    /**
     * <p>
     * Checks whether any of the messaging channels are synced with the provided org
     * </p>
     *
     * @param orgId Represents the org id
     * @return true, if any of the messaging channels synced, false otherwise
     */
    public boolean isAnyChannelSynced(final Long orgId) {
        final Table fbPageTable = fbPageManager.getPageTable();

        if (isChannelSynced(orgId, fbPageTable, fbPageTable.getColumn(OMSOFBPAGE.STATE))) {
            return true;
        }
        final Table instagramPageTable = instagramPageManager.getPageTable();

        if (isChannelSynced(orgId, instagramPageTable, instagramPageTable.getColumn(OMSOINSTAGRAMPAGE.STATE))) {
            return true;
        }
        final Table waPhoneNumberTable = waBusinessAccountManager.getPhoneNumberTable();

        return isChannelSynced(orgId, waPhoneNumberTable, waPhoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.STATE));
    }

    /**
     * <p>
     * Checks whether the channel is synced or not
     * </p>
     *
     * @param orgId       Represents the org if
     * @param table       Represents the page table
     * @param stateColumn Represents the state column
     * @return true, if synced
     */
    private boolean isChannelSynced(final Long orgId, final Table table, final Column stateColumn) {
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.notEq(stateColumn, ChannelState.DELETED.getId())));

        return getOrgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Transfers the user chat from source user ids to the target user id
     * </p>
     *
     * @param orgId           Represents the org id
     * @param assignedUserIds Represents the assigned user ids
     * @param targetUserId    Represents the target user ids
     */
    public void transferUserChat(final Long orgId, final Collection<Long> assignedUserIds, final Long targetUserId) {
        final Table chatTable = getChatTable();
        final UpdateQuery updateQuery = UpdateQuery.get(chatTable);
        final Column ownerIdColumn = chatTable.getColumn(OMSOCHAT.OWNER_ID);

        updateQuery.addUpdateColumn(ownerIdColumn, targetUserId);
        updateQuery.addUpdateColumn(chatTable.getColumn(OMSOCHAT.UPDATED_AT), Instant.now().toEpochMilli());

        for (final Collection<Long> batchedUserIds : JavaUtil.splitCollection(assignedUserIds, NumericalConstant.TEN)) {
            updateQuery.setWhereClause(new WhereClause(Criteria.in(ownerIdColumn, batchedUserIds)));

            getOrgDataStore(orgId).update(updateQuery);
        }
    }

    /**
     * <p>
     * Converts row to {@link CrmChat}.
     * </p>
     *
     * @param row {@link Row}.
     * @return {@link CrmChat}.
     */
    private CrmChat convertRowToPage(final Row row) {
        final CrmChat crmChat = new CrmChat();
        final Message lastMessage = new Message();
        final Long assignedContactId = row.get(OMSOCHAT.CONTACT_ID);
        final String recipientProfileName = row.get(OMSOCHAT.RECIPIENT_PROFILE_NAME);

        crmChat.setOrgId(row.getOrgId());
        crmChat.setCrmChatId(row.getPKValue());
        crmChat.setOwnerId(row.get(OMSOCHAT.OWNER_ID));
        crmChat.setId(row.get(OMSOCHAT.CHAT_ID));
        crmChat.setUnreadCount(row.get(OMSOCHAT.UNREAD_COUNT));

        if (Objects.nonNull(assignedContactId)) {
            final Contact contact = new Contact();

            contact.setId(assignedContactId);
            crmChat.setContact(contact);
        }
        lastMessage.setContent(row.get(OMSOCHAT.LAST_MESSAGE));

        crmChat.setLastMessage(lastMessage);

        if (Objects.nonNull(recipientProfileName)) {
            final RecipientProfile recipientProfile = new RecipientProfile();

            recipientProfile.setName(recipientProfileName);
            crmChat.setProfile(recipientProfile);
        }

        return crmChat;
    }

    /**
     * Gets the {@link WhereClause} for the provided filter
     *
     * @param filter Represents the {@link CrmChatFilter}
     * @return the {@link WhereClause}
     */
    private WhereClause getFilterWhereClause(final CrmChatFilter filter, final String searchCriteria) {
        final Table chatTable = getChatTable();
        final WhereClause whereClause = switch (filter) {
            case ASSIGNED_TO_ME -> new WhereClause(Criteria.eq(chatTable.getColumn(OMSOCHAT.OWNER_ID),
                    LoggedInUserInfo.getUser().id()));
            case UNASSIGNED -> new WhereClause(Criteria.eq(chatTable.getColumn(OMSOCHAT.OWNER_ID), null));
            case CONTACTS -> new WhereClause(Criteria.notEq(chatTable.getColumn(OMSOCHAT.CONTACT_ID), null));
            case NON_CONTACTS -> new WhereClause(Criteria.eq(chatTable.getColumn(OMSOCHAT.CONTACT_ID), null));
            case UNREAD -> new WhereClause(Criteria.gt(chatTable.getColumn(OMSOCHAT.UNREAD_COUNT), NumericalConstant.ZERO));
            default -> null;
        };

        if (Objects.nonNull(searchCriteria)) {
            final Criteria criteria = Criteria.contains(chatTable.getColumn(OMSOCHAT.RECIPIENT_PROFILE_NAME), searchCriteria);

            return Objects.isNull(whereClause) ? new WhereClause(criteria) : whereClause.and(criteria);
        } else {
            return whereClause;
        }
    }
}