package com.tlc.crm.messenger.api.channel.manager;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.messenger.api.channel.model.ChannelState;
import com.tlc.crm.messenger.api.channel.model.CrmWABusinessAccount;
import com.tlc.crm.messenger.api.channel.model.CrmWAPhoneNumber;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.messenger.config.manager.AbstractMessengerManager;
import com.tlc.crm.messenger.sql.resource.OMSOCHAT;
import com.tlc.crm.messenger.sql.resource.OMSOWABUSINESSACCOUNT;
import com.tlc.crm.messenger.sql.resource.OMSOWABUSINESSPHONENUMBER;

import com.tlc.crm.messenger.sql.resource.OMSOWAPHONENUMBERCHAT;
import com.tlc.social.common.model.RecipientProfile;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the database related actions of Whatsapp Business account
 *
 * @author Maharaja S
 * @version 1.0.
 */
public class WABusinessAccountManager extends AbstractMessengerManager {

    private WABusinessAccountManager() {
    }

    /**
     * Holds the instance of {@link WABusinessAccountManager}
     */
    private static final class InstanceHolder {
        private static final WABusinessAccountManager WA_BUSINESS_ACCOUNT_MANAGER = new WABusinessAccountManager();
    }

    /**
     * <p>
     * Gets the instance of {@link WABusinessAccountManager}
     * </p>
     *
     * @return {@link WABusinessAccountManager}
     */
    public static WABusinessAccountManager getInstance() {
        return InstanceHolder.WA_BUSINESS_ACCOUNT_MANAGER;
    }

    /**
     * <p>
     * Gets the WhatsApp business account table
     * </p>
     *
     * @return the WhatsApp business account table
     */
    private Table getBusinessAccountTable() {
        return Table.get(OMSOWABUSINESSACCOUNT.TABLE);
    }

    /**
     * <p>
     * Gets the WhatsApp business phone number table
     * </p>
     *
     * @return the WhatsApp business phone number table
     */
    public Table getPhoneNumberTable() {
        return Table.get(OMSOWABUSINESSPHONENUMBER.TABLE);
    }

    /**
     * <p>
     * Gets the WhatsApp phone number chat table
     * </p>
     *
     * @return the WhatsApp phone number chat table
     */
    private Table getPhoneNumberChatTable() {
        return Table.get(OMSOWAPHONENUMBERCHAT.TABLE);
    }

    /**
     * <p>
     * Stores the WhatsApp business account into the database
     * </p>
     *
     * @param orgId                Represents the org id
     * @param crmWABusinessAccount Represents the {@link CrmWABusinessAccount}
     */
    public void storeBusinessAccount(final Long orgId, final CrmWABusinessAccount crmWABusinessAccount) {
        final Long businessAccountId = crmWABusinessAccount.id();
        final Table businessAccountTable = getBusinessAccountTable();
        final DataContainer dataContainer = getWABusinessAccountContainer(orgId);
        final DataContainer targetContainer = DataContainer.create();
        final Collection<CrmWAPhoneNumber> phoneNumbers = crmWABusinessAccount.getCrmWAPhoneNumbers();
        final Row businessAccountRow = dataContainer.getOrInitRow(businessAccountTable, new WhereClause(Criteria.eq(businessAccountTable.getColumn(OMSOWABUSINESSACCOUNT.WA_BUSINESS_ACCOUNT_ID), businessAccountId)));

        if (businessAccountRow.isNewRow()) {
            businessAccountRow.setOrgId(crmWABusinessAccount.orgId());
            businessAccountRow.set(OMSOWABUSINESSACCOUNT.WA_BUSINESS_ACCOUNT_ID, businessAccountId);
            targetContainer.addNewRow(businessAccountRow);
        }
        final Long crmBusinessAccountId = businessAccountRow.getPKValue();

        crmWABusinessAccount.setCrmBusinessAccountId(crmBusinessAccountId);

        for (final CrmWAPhoneNumber crmWAPhoneNumber : phoneNumbers) {
            loadBusinessPhoneNumberIntoContainer(crmBusinessAccountId, crmWAPhoneNumber, dataContainer, targetContainer);
        }
        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Gets the associated org ids from the provided business phone number id
     * </p>
     *
     * @param businessPhoneNumberId Represents the whatsapp business phone number id
     * @return the collection of org ids
     */
    public Collection<Long> getAssociatedOrgIds(final Long businessPhoneNumberId) {
        final Table phoneNumberTable = getPhoneNumberTable();
        final SelectQuery selectQuery = SelectQuery.get(phoneNumberTable);

        selectQuery.addSelectClause(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.ORG_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.WA_BUSINESS_PHONE_NUMBER_ID), businessPhoneNumberId))
                .and(Criteria.eq(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.STATE), ChannelState.ENABLED.getId())));

        return readableDataStore().get(selectQuery).getRows(phoneNumberTable).map(Row::getOrgId).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Updates the state of the business phone number
     * </p>
     *
     * @param orgId         Represents the org id
     * @param phoneNumberID Represents the phone number id
     * @param state         Represents the state to be updated
     */
    public void updateState(final Long orgId, final Long phoneNumberID, final ChannelState state) {
        final Table phoneNumberTable = getPhoneNumberTable();
        final UpdateQuery updateQuery = UpdateQuery.get(phoneNumberTable);

        updateQuery.addUpdateColumn(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.STATE), state.getId());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(phoneNumberTable.getPKColumn(), phoneNumberID)));
        getOrgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Gets the WhatsApp business phone state from the provided chat id
     * </p>
     *
     * @param orgId  Represents the org id
     * @param chatId Represents the chat id
     * @return the phone state
     */
    public ChannelState getPhoneStatus(final Long orgId, final Long chatId) {
        final Table phoneTable = getPhoneNumberTable();
        final Table phoneNumberChatTable = getPhoneNumberChatTable();
        final SelectQuery selectQuery = SelectQuery.get(phoneTable);

        selectQuery.addSelectClause(phoneTable.getColumn(OMSOWABUSINESSPHONENUMBER.STATE));
        selectQuery.addJoinClause(new JoinClause(phoneTable.getPKColumn(), phoneNumberChatTable.getColumn(OMSOWAPHONENUMBERCHAT.BUSINESS_PHONE_NUMBER_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(phoneNumberChatTable.getColumn(OMSOWAPHONENUMBERCHAT.CHAT_ID), chatId)));
        final Row row = getOrgDataStore(orgId).get(selectQuery).getRow(phoneTable);

        return Objects.isNull(row) ? null : ChannelState.getStateById(row.get(OMSOWABUSINESSPHONENUMBER.STATE));
    }

    /**
     * <p>
     * Checks whether fb pages are synced with the org or not.
     * </p>
     *
     * @param orgId Represents the org id.
     * @return whether fb pages are synced.
     */
    public boolean arePhoneNumbersSynced(final Long orgId) {
        final Table phoneNumberTable = getPhoneNumberTable();
        final CountQuery countQuery = CountQuery.get(phoneNumberTable);

        countQuery.setWhereClause(new WhereClause(Criteria.notEq(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.STATE), ChannelState.DELETED.getId())));

        return getOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the optional of whatsapp phone number id from the provided crm phone number id
     * </p>
     *
     * @param orgId            Represents the org id
     * @param crmPhoneNumberId Represents the crm phone number id
     * @return the optional of phone number id
     */
    public Optional<Long> getPhoneNumberId(final Long orgId, final Long crmPhoneNumberId) {
        final Table phoneNumberTable = getPhoneNumberTable();
        final Column phoneNumberIdColumn = phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.WA_BUSINESS_PHONE_NUMBER_ID);
        final SelectQuery selectQuery = SelectQuery.get(phoneNumberTable);

        selectQuery.addSelectClause(phoneNumberIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(phoneNumberTable.getPKColumn(), crmPhoneNumberId)));
        final Row phoneNumberRow = getOrgDataStore(orgId).get(selectQuery).getRow(phoneNumberTable);

        return Objects.nonNull(phoneNumberRow) ? Optional.ofNullable(phoneNumberRow.get(phoneNumberIdColumn)) : Optional.empty();
    }

    /**
     * <p>
     * Gets the collection of whatsapp phone number id from the provided crm phone number ids
     * </p>
     *
     * @param orgId            Represents the org id
     * @param crmPhoneNumberIds Represents the collection of crm phone number ids
     * @return the optional of phone number id
     */
    public Map<Long, Long> getPhoneNumberIds(final Long orgId, final Collection<Long> crmPhoneNumberIds) {
        final Table phoneNumberTable = getPhoneNumberTable();
        final Column phoneNumberIdColumn = phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.WA_BUSINESS_PHONE_NUMBER_ID);
        final SelectQuery selectQuery = SelectQuery.get(phoneNumberTable);

        selectQuery.addSelectClause(phoneNumberIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(phoneNumberTable.getPKColumn(), crmPhoneNumberIds)));

        return getOrgDataStore(orgId).get(selectQuery).getRows(phoneNumberTable).collect(Collectors.toMap(Row::getPKValue, row -> row.get(phoneNumberIdColumn)));
    }

    /**
     * <p>
     * Gets the optional of whatsapp business account id from the provided crm chat id
     * </p>
     *
     * @param orgId     Represents the org id
     * @param crmChatId Represents the crm chat id
     * @return the optional of business account id
     */
    public Optional<Long> getBusinessAccountIdFromChat(final Long orgId, final Long crmChatId) {
        final Table phoneNumberTable = getPhoneNumberTable();
        final Table phoneNumberChatTable = getPhoneNumberChatTable();
        final Table businessAccountTable = getBusinessAccountTable();
        final Column businessAccountIdColumn = businessAccountTable.getColumn(OMSOWABUSINESSACCOUNT.WA_BUSINESS_ACCOUNT_ID);
        final SelectQuery selectQuery = SelectQuery.get(phoneNumberTable);

        selectQuery.addSelectClause(businessAccountIdColumn);
        selectQuery.addJoinClause(List.of(new JoinClause(phoneNumberTable.getPKColumn(), phoneNumberChatTable.getColumn(OMSOWAPHONENUMBERCHAT.BUSINESS_PHONE_NUMBER_ID)),
                new JoinClause(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.BUSINESS_ACCOUNT_ID), businessAccountTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(phoneNumberChatTable.getColumn(OMSOWAPHONENUMBERCHAT.CHAT_ID), crmChatId)));
        final Row businessAccountRow = getOrgDataStore(orgId).get(selectQuery).getRow(businessAccountTable);

        return Objects.nonNull(businessAccountRow) ? Optional.ofNullable(businessAccountRow.get(businessAccountIdColumn)) : Optional.empty();
    }

    /**
     * <p>
     * Gets the phone number count
     * </p>
     *
     * @param orgId    Represents the org id
     * @param stateIds Represents the business phone number state ids
     * @return the count
     */
    public long getPhoneNumberCount(final Long orgId, final Collection<Short> stateIds) {
        final Table phoneNumberTable = getPhoneNumberTable();
        final CountQuery pageQuery = CountQuery.get(phoneNumberTable);

        pageQuery.setWhereClause(new WhereClause(Criteria.in(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.STATE), stateIds)));

        return getOrgDataStore(orgId).get(pageQuery);
    }

    /**
     * <p>
     * Gets the collection of {@link CrmWAPhoneNumber}.
     * </p>
     *
     * @param orgId    Organization's id.
     * @param start    Start point to fetch.
     * @param count    Total count to fetch.
     * @param stateIds Represents the business phone number state ids
     * @return Collection of {@link CrmWAPhoneNumber}.
     */
    public Collection<CrmWAPhoneNumber> getPhoneNumbers(final Long orgId, final int start, final int count, final Collection<Short> stateIds) {
        final Table phoneNumberTable = getPhoneNumberTable();
        final SelectQuery selectQuery = SelectQuery.get(phoneNumberTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.STATE), stateIds)));
        selectQuery.setLimitClause(new LimitClause(start, count));
        selectQuery.addOrderByClause(new OrderByClause(phoneNumberTable.getPKColumn()));
        final Stream<Row> rows = getOrgDataStore(orgId).get(selectQuery).getRows(phoneNumberTable);

        return rows.map(this::convertRowToPhone).toList();
    }

    /**
     * <p>
     * Gets {@link CrmWAPhoneNumber} for the organization matching the given states.
     * </p>
     *
     * @param orgId    Organization's id.
     * @param stateIds Represents the business phone number state ids
     * @return Collection of {@link CrmWAPhoneNumber}.
     */
    public Collection<CrmWAPhoneNumber> getPhoneNumbers(final Long orgId, final Collection<Short> stateIds) {
        final Table phoneNumberTable = getPhoneNumberTable();
        final SelectQuery selectQuery = SelectQuery.get(phoneNumberTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.STATE), stateIds)));

        return getOrgDataStore(orgId).get(selectQuery).getRows(phoneNumberTable).map(this::convertRowToPhone).toList();
    }

    /**
     * Converts the row into {@link CrmWAPhoneNumber}
     *
     * @param row Represents the row to be converted
     * @return the {@link CrmWAPhoneNumber}
     */
    private CrmWAPhoneNumber convertRowToPhone(final Row row) {
        final CrmWAPhoneNumber crmWAPhoneNumber = new CrmWAPhoneNumber();

        crmWAPhoneNumber.setOrgId(row.getOrgId());
        crmWAPhoneNumber.setCrmPhoneNumberId(row.getPKValue());
        crmWAPhoneNumber.setCrmWABusinessAccountId(row.get(OMSOWABUSINESSPHONENUMBER.BUSINESS_ACCOUNT_ID));
        crmWAPhoneNumber.setState(ChannelState.getStateById(row.get(OMSOWABUSINESSPHONENUMBER.STATE)));
        crmWAPhoneNumber.setId(row.get(OMSOWABUSINESSPHONENUMBER.WA_BUSINESS_PHONE_NUMBER_ID));

        return crmWAPhoneNumber;
    }

    /**
     * <p>
     * Links the chats to the WhatsApp business phone number
     * </p>
     *
     * @param orgIds                Represents the collection of org ids
     * @param chats                 Represents the collection of chats
     * @param businessPhoneNumberId Represents the business phone number id
     * @param targetContainer       Represents the target container
     */
    public void linkChatToPhoneNumber(final Collection<Long> orgIds, final Collection<CrmChat> chats, final Long businessPhoneNumberId, final DataContainer targetContainer) {
        final Table phoneNumberChatTable = getPhoneNumberChatTable();
        final Table phoneNumberTable = getPhoneNumberTable();
        final DataContainer phoneChatDataContainer = getPhoneNumberChatContainer(orgIds, businessPhoneNumberId);

        for (final CrmChat chat : chats) {
            final Long crmChatId = chat.getCrmChatId();
            final Long orgId = chat.orgId();
            final Row existingRow = phoneChatDataContainer.getRow(phoneNumberChatTable, new WhereClause(Criteria.eq(phoneNumberChatTable.getColumn(OMSOWAPHONENUMBERCHAT.CHAT_ID), crmChatId)));

            if (Objects.isNull(existingRow)) {
                final Row phoneNumberRow = phoneChatDataContainer.getRow(phoneNumberTable, new WhereClause(Criteria.eq(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.ORG_ID), orgId)));
                final Row phoneChatRow = new Row(phoneNumberChatTable);

                phoneChatRow.setOrgId(orgId);
                phoneChatRow.set(OMSOWAPHONENUMBERCHAT.CHAT_ID, crmChatId);
                phoneChatRow.set(OMSOWAPHONENUMBERCHAT.BUSINESS_PHONE_NUMBER_ID, phoneNumberRow.getPKValue());

                targetContainer.addNewRow(phoneChatRow);
            }
        }
        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Gets the whatsapp business account id from the provided business phone number id
     * </p>
     *
     * @param orgId                    Represents the org id
     * @param crmBusinessPhoneNumberId Represents the business phone number id
     * @return the optional of business account id
     */
    public Optional<Long> getBusinessAccountId(final long orgId, final Long crmBusinessPhoneNumberId) {
        final Table businessAccountTable = getBusinessAccountTable();
        final SelectQuery selectQuery = SelectQuery.get(businessAccountTable);
        final Table phoneNumberTable = getPhoneNumberTable();

        selectQuery.addSelectClause(businessAccountTable.getColumn(OMSOWABUSINESSACCOUNT.WA_BUSINESS_ACCOUNT_ID));
        selectQuery.addJoinClause(new JoinClause(businessAccountTable.getPKColumn(), phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.BUSINESS_ACCOUNT_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(phoneNumberTable.getPKColumn(), crmBusinessPhoneNumberId)));
        final Row row = getOrgDataStore(orgId).get(selectQuery).getRow(businessAccountTable);

        return Objects.nonNull(row) ? Optional.of(row.get(OMSOWABUSINESSACCOUNT.WA_BUSINESS_ACCOUNT_ID)) : Optional.empty();
    }

    /**
     * <p>
     * Checks whether any whatsapp business phone number is synced and enabled
     * </p>
     *
     * @param orgId Represents the org id.
     * @return true if any business number is synced and enabled, false otherwise
     */
    public boolean arePhoneNumbersActive(final Long orgId) {
        final Table phoneNumberTable = getPhoneNumberTable();
        final CountQuery countQuery = CountQuery.get(phoneNumberTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.STATE), ChannelState.ENABLED.getId())));

        return getOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Loads the new chat into the target container
     * </p>
     *
     * @param orgId           Represents the org id
     * @param crmChat         Represents the {@link CrmChat}
     * @param targetContainer Represents the target container
     */
    public void loadNewChat(final Long orgId, final CrmChat crmChat, final DataContainer targetContainer, final Long businessPhoneNumberId) {
        final Table chatTable = getChatTable();
        final Row chatRow = new Row(chatTable);
        final Long currentTime = Instant.now().toEpochMilli();
        final Contact contact = crmChat.getContact();
        final RecipientProfile recipientProfile = crmChat.getProfile();

        chatRow.setOrgId(orgId);
        chatRow.set(OMSOCHAT.CHAT_ID, crmChat.id());
        chatRow.set(OMSOCHAT.LAST_MESSAGE, crmChat.getRecentMessagePreview());
        chatRow.set(OMSOCHAT.CREATED_AT, currentTime);
        chatRow.set(OMSOCHAT.UPDATED_AT, currentTime);

        if (Objects.nonNull(contact)) {
            chatRow.set(OMSOCHAT.CONTACT_ID, contact.id());
        }

        if (Objects.nonNull(recipientProfile)) {
            chatRow.set(OMSOCHAT.RECIPIENT_PROFILE_NAME, recipientProfile.getName());
        }
        crmChat.setCrmChatId(chatRow.getPKValue());
        targetContainer.updateRow(chatRow);

        linkChatToPhoneNumber(List.of(orgId), List.of(crmChat), businessPhoneNumberId, targetContainer);
    }

    /**
     * <p>
     * Gets the crm chat id map for the provided business phone number ids and chat ids
     * </p>
     *
     * @param crmBusinessPhoneNumberIds Represents the crm business phone number ids
     * @param chatIds                   Represents the chat ids
     * @return the chat id map
     */
    public Map<Long, Long> getChatIdMap(final Long orgId, final Collection<Long> crmBusinessPhoneNumberIds, final Collection<Long> chatIds) {
        final Table phoneNumberChatTable = getPhoneNumberChatTable();
        final Table chatTable = getChatTable();
        final SelectQuery selectQuery = SelectQuery.get(phoneNumberChatTable);
        final Column phoneNumberchatIdColumn = phoneNumberChatTable.getColumn(OMSOWAPHONENUMBERCHAT.CHAT_ID);
        final Column chatIdColumn = chatTable.getColumn(OMSOCHAT.CHAT_ID);
        final Column chatTablePKColumn = chatTable.getPKColumn();

        selectQuery.addSelectClause(phoneNumberChatTable);
        selectQuery.addSelectClause(List.of(chatTablePKColumn, chatIdColumn));
        selectQuery.addJoinClause(new JoinClause(phoneNumberchatIdColumn, chatTablePKColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(phoneNumberChatTable.getColumn(OMSOWAPHONENUMBERCHAT.BUSINESS_PHONE_NUMBER_ID), crmBusinessPhoneNumberIds))
                .and(Criteria.in(chatIdColumn, chatIds)));

        return getOrgDataStore(orgId).get(selectQuery).getRows(phoneNumberChatTable).collect(Collectors
                .toMap(phoneNumberChatRow -> phoneNumberChatRow.get(OMSOWAPHONENUMBERCHAT.BUSINESS_PHONE_NUMBER_ID),
                        phoneNumberChatRow -> phoneNumberChatRow.get(OMSOWAPHONENUMBERCHAT.CHAT_ID)));
    }

    /**
     * <p>
     * Deletes the whatsapp business phone number
     * </p>
     *
     * @param businessPhoneNumberId Represents the business phone number id
     */
    public void deleteBusinessPhoneNumber(final Long businessPhoneNumberId) {
        final Table businessPhoneNumberTable = getPhoneNumberTable();
        final UpdateQuery updateQuery = UpdateQuery.get(businessPhoneNumberTable);

        updateQuery.addUpdateColumn(businessPhoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.STATE), ChannelState.DELETED.getId());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(businessPhoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.WA_BUSINESS_PHONE_NUMBER_ID), businessPhoneNumberId)));
        adminDataStore().update(updateQuery);
    }

    /**
     * <p>
     * Checks whether the whatsapp business phone number is active or not
     * </p>
     *
     * @param orgId                    Represents the org id
     * @param crmBusinessPhoneNumberId Represents the business phone number id
     * @return true if the business phone number is active, false otherwise
     */
    public boolean isPhoneNumberActive(final Long orgId, final Long crmBusinessPhoneNumberId) {
        final Table businessPhoneNumberTable = getPhoneNumberTable();
        final CountQuery countQuery = CountQuery.get(businessPhoneNumberTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(businessPhoneNumberTable.getPKColumn(), crmBusinessPhoneNumberId))
                .and(Criteria.eq(businessPhoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.STATE), ChannelState.ENABLED.getId())));

        return getOrgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     *  Gets the data container which contains chat details related to the phone number
     * </p>
     * @param waBusinessPhoneNumberId Represents the WhatsApp business phone number id
     * @return the data container
     */
    private DataContainer getPhoneNumberChatContainer(final Collection<Long> orgIds, final Long waBusinessPhoneNumberId) {
        final Table phoneNumberTable = getPhoneNumberTable();
        final Table phoneNumberChatTable = getPhoneNumberChatTable();
        final SelectQuery selectQuery = SelectQuery.get(phoneNumberTable);

        selectQuery.addJoinClause(new JoinClause(phoneNumberTable.getPKColumn(), phoneNumberChatTable.getColumn(OMSOWAPHONENUMBERCHAT.BUSINESS_PHONE_NUMBER_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.WA_BUSINESS_PHONE_NUMBER_ID), waBusinessPhoneNumberId))
                .and(Criteria.in(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.ORG_ID), orgIds)));

        return readableDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Gets the WhatsApp Business account container
     * </p>
     *
     * @param orgId Represents the org id
     * @return the business account data container
     */
    private DataContainer getWABusinessAccountContainer(final Long orgId) {
        final Table businessAccountTable = getBusinessAccountTable();
        final Table phoneNumberTable = getPhoneNumberTable();
        final SelectQuery selectQuery = SelectQuery.get(businessAccountTable);

        selectQuery.addJoinClause(new JoinClause(businessAccountTable.getPKColumn(), phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.BUSINESS_ACCOUNT_ID)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Loads the {@link CrmWAPhoneNumber} into the container
     * </p>
     *
     * @param crmWAPhoneNumber Represents the {@link CrmWAPhoneNumber}
     * @param dataContainer    Represents the data container which contains WhatsApp business details
     * @param targetContainer  Represents the target container
     */
    private void loadBusinessPhoneNumberIntoContainer(final Long crmBusinessAccountId, final CrmWAPhoneNumber crmWAPhoneNumber, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table phoneNumberTable = getPhoneNumberTable();
        final Long phoneNumberId = crmWAPhoneNumber.id();
        final Row phoneNumberRow = dataContainer.getOrInitRow(phoneNumberTable, new WhereClause(Criteria.eq(phoneNumberTable.getColumn(OMSOWABUSINESSPHONENUMBER.WA_BUSINESS_PHONE_NUMBER_ID), phoneNumberId)));

        if (phoneNumberRow.isNewRow()) {
            phoneNumberRow.setOrgId(crmWAPhoneNumber.getOrgId());
            phoneNumberRow.set(OMSOWABUSINESSPHONENUMBER.WA_BUSINESS_PHONE_NUMBER_ID, phoneNumberId);
            phoneNumberRow.set(OMSOWABUSINESSPHONENUMBER.BUSINESS_ACCOUNT_ID, crmBusinessAccountId);
            crmWAPhoneNumber.setCrmPhoneNumberId(phoneNumberRow.getPKValue());
        }
        phoneNumberRow.set(OMSOWABUSINESSPHONENUMBER.STATE, crmWAPhoneNumber.getState().getId());

        targetContainer.updateRow(phoneNumberRow);
    }

    /**
     * <p>
     * Checks whether the provided business phone number id exists or not
     * </p>
     *
     * @param orgId                    Represents the org id
     * @param crmBusinessPhoneNumberId Represents the business phone number id
     * @return true if the business phone number id exists, false otherwise
     */
    public boolean exists(final Long orgId, final Long crmBusinessPhoneNumberId) {
        final Table businessPhoneNumberTable = getPhoneNumberTable();
        final CountQuery countQuery = CountQuery.get(businessPhoneNumberTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(businessPhoneNumberTable.getPKColumn(), crmBusinessPhoneNumberId)));

        return getOrgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }
}