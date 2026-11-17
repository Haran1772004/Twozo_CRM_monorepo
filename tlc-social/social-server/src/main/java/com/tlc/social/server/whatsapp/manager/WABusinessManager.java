package com.tlc.social.server.whatsapp.manager;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.social.common.model.Channel;
import com.tlc.commons.language.Language;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.WABusinessAccount;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;
import com.tlc.social.common.whatsapp.model.WAPhoneRecipient;
import com.tlc.social.common.whatsapp.model.template.Body;
import com.tlc.social.common.whatsapp.model.template.Button;
import com.tlc.social.common.whatsapp.model.template.Component;
import com.tlc.social.common.whatsapp.model.template.Footer;
import com.tlc.social.common.whatsapp.model.template.Header;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.social.server.manager.ChatManager;
import com.tlc.social.server.manager.SocialEntityManager;
import com.tlc.social.server.sql.resource.MSOCHAT;
import com.tlc.social.server.sql.resource.MSOMESSAGE;
import com.tlc.social.server.sql.resource.MSORECIPIENT;
import com.tlc.social.server.whatsapp.internal.model.WAMessage;
import com.tlc.social.server.whatsapp.sql.resource.MSOWABUSINESSACCOUNT;
import com.tlc.social.server.whatsapp.sql.resource.MSOWABUSINESSPHONENUMBER;
import com.tlc.social.server.whatsapp.sql.resource.MSOWAMESSAGE;
import com.tlc.social.server.whatsapp.sql.resource.MSOWAPHONERECIPIENT;
import com.tlc.social.server.whatsapp.sql.resource.MSOWARECIPIENT;

import com.tlc.social.server.whatsapp.sql.resource.MSOWATEMPLATE;
import com.tlc.social.server.whatsapp.sql.resource.MSOWATEMPLATECOMPONENT;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sync.service.oauth.internal.manager.SyncManager;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.whatsapp.WATokenReadRequest;
import com.tlc.sync.service.oauth.model.request.whatsapp.WATokenWriteRequest;
import com.tlc.sync.service.oauth.resource.MOAREFRESHABLETOKEN;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages the Whatsapp business related database actions
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WABusinessManager extends SocialEntityManager {

    private static final int DEFAULT_TOKEN_EXPIRY_DAYS = 59;
    private static final String NON_DIGIT_REGEX_PATTERN = "[^\\d]";
    private static final int MESSAGE_BATCH_SIZE = 500;

    private final ChatManager chatManager;
    private final SyncManager syncManager;

    private WABusinessManager() {
        chatManager = ChatManager.getInstance();
        syncManager = SyncManager.getInstance();
    }

    /**
     * Holds the instance of {@link WABusinessManager}
     */
    private static final class InstanceHolder {
        private static final WABusinessManager INSTANCE = new WABusinessManager();
    }

    /**
     * <p>
     * Gets the instance of {@link ChatManager}
     * </p>
     *
     * @return the instance of {@link ChatManager}
     */
    public static WABusinessManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Gets the WhatsApp recipient table
     * </p>
     *
     * @return the WhatsApp recipient table
     */
    public Table getWARecipientTable() {
        return Table.get(MSOWARECIPIENT.TABLE);
    }

    /**
     * <p>
     * Gets the WhatsApp business phone number table
     * </p>
     *
     * @return the WhatsApp business phone number table
     */
    public Table getWABusinessPhoneNumberTable() {
        return Table.get(MSOWABUSINESSPHONENUMBER.TABLE);
    }

    /**
     * <p>
     * Gets the WhatsApp message table
     * </p>
     *
     * @return the WhatsApp message table
     */
    public Table getWAMessageTable() {
        return Table.get(MSOWAMESSAGE.TABLE);
    }

    /**
     * <p>
     * Gets the WhatsApp business account table
     * </p>
     *
     * @return the WhatsApp business account table
     */
    public Table getWABussinessAccountTable() {
        return Table.get(MSOWABUSINESSACCOUNT.TABLE);
    }

    /**
     * <p>
     * Gets the WhatsApp phone recipient table
     * </p>
     *
     * @return the WhatsApp phone recipient table
     */
    public Table getWAPhoneRecipientTable() {
        return Table.get(MSOWAPHONERECIPIENT.TABLE);
    }

    /**
     * <p>
     * Gets the Whatsapp template table
     * </p>
     *
     * @return the template table
     */
    public Table getTemplateTable() {
        return Table.get(MSOWATEMPLATE.TABLE);
    }

    /**
     * <p>
     * Gets the Whatsapp template component table
     * </p>
     *
     * @return the template component table
     */
    public Table getTemplateComponentTable() {
        return Table.get(MSOWATEMPLATECOMPONENT.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param phoneNumberIds Represents the phone number ids
     * @return true if phone number exists, false otherwise
     */
    @Override
    public boolean exists(final Collection<Long> phoneNumberIds) {
        final Table phoneNumberTable = getWABusinessPhoneNumberTable();
        final CountQuery countQuery = CountQuery.get(phoneNumberTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(phoneNumberTable.getPKColumn(), phoneNumberIds)));

        return phoneNumberIds.size() == adminDataStore().get(countQuery);
    }

    /**
     * <p>
     * Gets the collection of {@link WABusinessPhoneNumber} from the provided phone number ids
     * </p>
     *
     * @param phoneNumberIds Represents the business phone number ids
     * @return the collection of {@link WABusinessPhoneNumber}
     */
    public Collection<WABusinessPhoneNumber> getBusinessPhoneNumbers(final Collection<Long> phoneNumberIds) {
        final Table phoneNumberTable = getWABusinessPhoneNumberTable();
        final SelectQuery selectQuery = SelectQuery.get(phoneNumberTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(phoneNumberTable.getPKColumn(), phoneNumberIds)));

        return adminDataStore().get(selectQuery).getRows(phoneNumberTable).map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Gets the count of {@link WABusinessPhoneNumber} matching the provided filters.
     * </p>
     *
     * @param phoneNumberIds Represents the business phone number ids
     * @param searchText     Represents the search text to filter by display phone number
     * @return the count
     */
    public long getBusinessPhoneNumberCount(final Collection<Long> phoneNumberIds, final String searchText) {
        final Table phoneNumberTable = getWABusinessPhoneNumberTable();
        final CountQuery countQuery = CountQuery.get(phoneNumberTable);

        countQuery.setWhereClause(buildBusinessPhoneNumberFilterWhereClause(phoneNumberIds, searchText));

        return readableDataStore().get(countQuery);
    }

    /**
     * <p>
     * Gets the collection of {@link WABusinessPhoneNumber} matching the provided filters.
     * </p>
     *
     * @param phoneNumberIds Represents the business phone number ids
     * @param searchText     Represents the search text to filter by display phone number
     * @param start          Represents the start of the list
     * @param limit          Represents the limit of the list
     * @return the collection of {@link WABusinessPhoneNumber}
     */
    public Collection<WABusinessPhoneNumber> getBusinessPhoneNumbers(final Collection<Long> phoneNumberIds, final String searchText,
                                                                   final int start, final int limit) {
        final Table phoneNumberTable = getWABusinessPhoneNumberTable();
        final SelectQuery selectQuery = SelectQuery.get(phoneNumberTable);

        selectQuery.setWhereClause(buildBusinessPhoneNumberFilterWhereClause(phoneNumberIds, searchText));
        selectQuery.setLimitClause(new LimitClause(start, limit));

        return adminDataStore().get(selectQuery).getRows(phoneNumberTable).map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Builds the where clause for business phone number filters.
     * </p>
     */
    private WhereClause buildBusinessPhoneNumberFilterWhereClause(final Collection<Long> phoneNumberIds, final String searchText) {
        final Table phoneNumberTable = getWABusinessPhoneNumberTable();
        WhereClause whereClause = new WhereClause(Criteria.in(phoneNumberTable.getPKColumn(), phoneNumberIds));

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            whereClause = whereClause.and(Criteria.contains(phoneNumberTable.getColumn(MSOWABUSINESSPHONENUMBER.DISPLAY_PHONE_NUMBER), searchText));
        }

        return whereClause;
    }

    /**
     * <p>
     * Gets the chat id map for the provided phone number ids and the recipient phone id
     * </p>
     *
     * @param phoneNumberIds   Represents the business phone number ids
     * @param recipientPhoneId Represents the recipient phone id
     * @return the chat id map
     */
    public Map<Long, Long> getChatIdMap(final Collection<Long> phoneNumberIds, final Long recipientPhoneId) {
        final Table waPhoneRecipientTable = getWAPhoneRecipientTable();
        final Table waRecipientTable = getWARecipientTable();
        final Table recipientTable = getRecipientTable();
        final SelectQuery selectQuery = SelectQuery.get(waPhoneRecipientTable);

        selectQuery.addJoinClause(List.of(new JoinClause(waPhoneRecipientTable.getColumn(MSOWAPHONERECIPIENT.WA_RECIPIENT_ID), waRecipientTable.getPKColumn()),
                new JoinClause(waPhoneRecipientTable.getColumn(MSOWAPHONERECIPIENT.RECIPIENT_ID), recipientTable.getPKColumn())
        ));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(waPhoneRecipientTable.getColumn(MSOWAPHONERECIPIENT.BUSINESS_PHONE_NUMBER_ID), phoneNumberIds))
                .and(Criteria.eq(waRecipientTable.getColumn(MSOWARECIPIENT.EXTERNAL_RECIPIENT_ID), recipientPhoneId)));
        final DataContainer dataContainer = adminDataStore().get(selectQuery);

        return dataContainer.getRows(waPhoneRecipientTable).collect(Collectors
                .toMap(phoneRecipientRow -> phoneRecipientRow.get(MSOWAPHONERECIPIENT.BUSINESS_PHONE_NUMBER_ID), phoneRecipientRow -> {
                    final Row recipientRow = dataContainer.getRow(recipientTable, (Long) phoneRecipientRow.get(MSOWAPHONERECIPIENT.RECIPIENT_ID));

                    return recipientRow.get(MSORECIPIENT.CHAT_ID);
                }));
    }

    /**
     * <p>
     * Gets the {@link WAPhoneRecipient} from the provided chat id
     * </p>
     *
     * @param chatId Represents the chat id
     * @return the {@link WAPhoneRecipient}
     */
    public WAPhoneRecipient getWARecipient(final Long chatId) {
        final Table recipientTable = getRecipientTable();
        final Table phoneRecipientTable = getWAPhoneRecipientTable();
        final Table waRecipientTable = getWARecipientTable();
        final Table businessPhoneNumberTable = getWABusinessPhoneNumberTable();
        final SelectQuery selectQuery = SelectQuery.get(recipientTable);

        selectQuery.addJoinClause(List.of(new JoinClause(recipientTable.getPKColumn(), phoneRecipientTable.getColumn(MSOWAPHONERECIPIENT.RECIPIENT_ID)),
                new JoinClause(phoneRecipientTable.getColumn(MSOWAPHONERECIPIENT.WA_RECIPIENT_ID), waRecipientTable.getPKColumn()),
                new JoinClause(phoneRecipientTable.getColumn(MSOWAPHONERECIPIENT.BUSINESS_PHONE_NUMBER_ID), businessPhoneNumberTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(recipientTable.getColumn(MSORECIPIENT.CHAT_ID), chatId)));
        final DataContainer dataContainer = adminDataStore().get(selectQuery);

        return convertRowToWARecipient(dataContainer.getRow(phoneRecipientTable), dataContainer);
    }

    /**
     * <p>
     * Loads the {@link WAMessage} into the database
     * </p>
     *
     * @param waMessage       Represents the message to be stored
     * @param dataContainer   Represents the data container which contains chat and message info
     * @param targetContainer Represents the target data container
     */
    public void loadWAMessage(final WAMessage waMessage, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table waMessageTable = getWAMessageTable();
        final String externalMessageId = waMessage.getExternalMessageId();
        final Row messageRow = dataContainer.getOrInitRow(waMessageTable, new WhereClause(Criteria.eq(waMessageTable.getColumn(MSOWAMESSAGE.EXTERNAL_MESSAGE_ID), externalMessageId)));

        if (messageRow.isNewRow()) {
            messageRow.set(MSOWAMESSAGE.EXTERNAL_MESSAGE_ID, externalMessageId);
            messageRow.set(MSOWAMESSAGE.MESSAGE_ID, waMessage.id());
            messageRow.set(MSOWAMESSAGE.IS_DELETED, false);

            targetContainer.updateRow(messageRow);
        }
    }

    /**
     * <p>
     * Stores the WhatsApp business account into the database
     * </p>
     *
     * @param waBusinessAccount Represents the {@link WABusinessAccount}
     * @param businessToken     Represents the business access token
     */
    public void storeWABusinessAccount(final WABusinessAccount waBusinessAccount, final String businessToken) {
        final Table waBussinessAccountTable = getWABussinessAccountTable();
        final Table businessPhoneNumberTable = getWABusinessPhoneNumberTable();
        final Long externalBusinessId = waBusinessAccount.getExternalBusinessId();
        final Long externalWABAId = waBusinessAccount.getExternalWABAId();
        final DataContainer businessAccountDataContainer = getBusinessAccountDataContainer(waBusinessAccount);
        final DataContainer targetContainer = DataContainer.create();
        final Row businessAccountRow = businessAccountDataContainer.getOrInitRow(waBussinessAccountTable,
                new WhereClause(Criteria.eq(waBussinessAccountTable.getColumn(MSOWABUSINESSACCOUNT.EXTERNAL_BUSINESS_ID), externalBusinessId))
                        .and(Criteria.eq(waBussinessAccountTable.getColumn(MSOWABUSINESSACCOUNT.EXTERNAL_WABA_ID), externalWABAId)));
        final boolean isNewBusinessAccountRow = businessAccountRow.isNewRow();

        if (isNewBusinessAccountRow) {
            businessAccountRow.set(MSOWABUSINESSACCOUNT.EXTERNAL_BUSINESS_ID, externalBusinessId);
            businessAccountRow.set(MSOWABUSINESSACCOUNT.EXTERNAL_WABA_ID, externalWABAId);
            businessAccountRow.set(MSOWABUSINESSACCOUNT.SYNC_STATUS, WABusinessAccount.TemplateSyncStatus.NEW.getId());
        }
        final Long businessAccountId = businessAccountRow.getPKValue();
        final WATokenWriteRequest waTokenWriteRequest = SyncTokenRequestInstanceProvider.getWATokenWriteRequest(businessAccountId, businessToken, TokenRequest.TokenType.ACCESS);
        final Long nextRefreshedAt = TemporalServiceProvider.get().plus(DEFAULT_TOKEN_EXPIRY_DAYS, DateInterval.DAYS);

        waTokenWriteRequest.setNextRefreshedAt(nextRefreshedAt);
        loadWABusinessToken(waTokenWriteRequest, businessAccountDataContainer, targetContainer);

        if (isNewBusinessAccountRow) {
            businessAccountRow.set(MSOWABUSINESSACCOUNT.TOKEN_ID, waTokenWriteRequest.getTokenId());
            targetContainer.addNewRow(businessAccountRow);
        }
        waBusinessAccount.setId(businessAccountId);

        for (final WABusinessPhoneNumber waBusinessPhoneNumber : waBusinessAccount.getBusinessPhoneNumbers()) {
            final Channel channel = waBusinessPhoneNumber.getChannel();
            final Row phoneNumberRow = businessAccountDataContainer.getRow(businessPhoneNumberTable,
                    new WhereClause(Criteria.eq(businessPhoneNumberTable.getColumn(MSOWABUSINESSPHONENUMBER.EXTERNAL_PHONE_NUMBER_ID), waBusinessPhoneNumber.getExternalPhoneNumberId())));

            if (Objects.nonNull(phoneNumberRow)) {
                final Long channelId = phoneNumberRow.get(MSOWABUSINESSPHONENUMBER.CHANNEL_ID);

                channel.setId(channelId);
            }
            waBusinessPhoneNumber.setWaBusinessAccountId(businessAccountId);
            chatManager.loadChannelIntoContainer(channel, businessAccountDataContainer, targetContainer);
            loadWAPhoneNumberIntoContainer(waBusinessPhoneNumber, businessAccountDataContainer, targetContainer);
        }

        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the WhatsApp business access token into the data container
     * </p>
     *
     * @param waTokenWriteRequest Represents the {@link WATokenWriteRequest}
     * @param dataContainer       Represents the data container which contains WhatsApp business account details
     * @param targetContainer     Represents the target container
     */
    public void loadWABusinessToken(final WATokenWriteRequest waTokenWriteRequest, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table refreshableTokenTable = syncManager.getRefreshableTokenTable();
        final Row tokenRow = dataContainer.getRow(refreshableTokenTable);

        waTokenWriteRequest.setTokenId(Objects.isNull(tokenRow) ? null : tokenRow.getPKValue());
        syncManager.loadRefreshableTokenIntoContainer(waTokenWriteRequest, dataContainer, targetContainer);
    }

    /**
     * <p>
     * Gets the WhatsApp business token from the provided {@link WATokenReadRequest}
     * </p>
     *
     * @param waTokenReadRequest Represents the {@link WATokenReadRequest}
     * @return the optional of business token
     */
    public Optional<String> getWABusinessToken(final WATokenReadRequest waTokenReadRequest) {
        final Table waBussinessAccountTable = getWABussinessAccountTable();
        final Table refreshableTokenTable = syncManager.getRefreshableTokenTable();
        final SelectQuery selectQuery = SelectQuery.get(waBussinessAccountTable);

        selectQuery.addJoinClause(new JoinClause(waBussinessAccountTable.getColumn(MSOWABUSINESSACCOUNT.TOKEN_ID), refreshableTokenTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(waBussinessAccountTable.getPKColumn(), waTokenReadRequest.getWaBusinessAccountId())));
        final Row tokenRow = adminDataStore().get(selectQuery).getRow(refreshableTokenTable);

        return Objects.isNull(tokenRow) ? Optional.empty() : Optional.ofNullable(tokenRow.get(MOAREFRESHABLETOKEN.TOKEN));
    }

    /**
     * <p>
     * Gets the chat data container from the provided recipient whatsapp id
     * </p>
     *
     * @param waBusinessPhoneNumberId Represents the whatsapp business phone number id
     * @param recipientWAId Represents the recipient whatsapp id
     * @return the {@link DataContainer}
     */
    public DataContainer getChatDataContainer(final Long waBusinessPhoneNumberId, final Long recipientWAId) {
        final Table recipientTable = getRecipientTable();
        final Table recipientProfileTable = getRecipientProfileTable();
        final Table waPhoneRecipientTable = getWAPhoneRecipientTable();
        final Table waRecipientTable = getWARecipientTable();
        final Table chatTable = getChatTable();
        final SelectQuery selectQuery = SelectQuery.get(waPhoneRecipientTable);

        selectQuery.addJoinClause(List.of(new JoinClause(waPhoneRecipientTable.getColumn(MSOWAPHONERECIPIENT.RECIPIENT_ID), recipientTable.getPKColumn()),
                new JoinClause(recipientTable.getColumn(MSORECIPIENT.PROFILE_ID), recipientProfileTable.getPKColumn()),
                new JoinClause(recipientTable.getColumn(MSORECIPIENT.CHAT_ID), chatTable.getPKColumn()),
                new JoinClause(waPhoneRecipientTable.getColumn(MSOWAPHONERECIPIENT.WA_RECIPIENT_ID), waRecipientTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(waRecipientTable.getColumn(MSOWARECIPIENT.EXTERNAL_RECIPIENT_ID), recipientWAId))
                .and(Criteria.eq(waPhoneRecipientTable.getColumn(MSOWAPHONERECIPIENT.BUSINESS_PHONE_NUMBER_ID), waBusinessPhoneNumberId)));

        return adminDataStore().get(selectQuery);
    }


    /**
     * <p>
     * Gets the whatsapp recipient container from the provided external recipient id
     * </p>
     * @param externalWARecipientId Represents the external recipient id
     * @return the data container
     */
    private DataContainer getWARecipientContainer(final Long externalWARecipientId) {
        final Table waRecipientTable = getWARecipientTable();
        final SelectQuery selectQuery = SelectQuery.get(waRecipientTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(waRecipientTable.getColumn(MSOWARECIPIENT.EXTERNAL_RECIPIENT_ID), externalWARecipientId)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Loads the recipient into the container
     * </p>
     *
     * @param waPhoneRecipient         Represents the {@link WAPhoneRecipient}
     * @param dataContainer       Represents the data container which contains the recipient details
     * @param targetDataContainer Represents the target container
     */
    public void loadRecipientIntoContainer(final WAPhoneRecipient waPhoneRecipient, final DataContainer dataContainer, final DataContainer targetDataContainer) {
        final Table recipientTable = getRecipientTable();
        final Table phoneRecipientTable = getWAPhoneRecipientTable();
        final Table waRecipientTable = getWARecipientTable();
        final Long recipientWAId = waPhoneRecipient.getExternalRecipientId();
        final Row recipientRow = dataContainer.getOrInitRow(recipientTable, waPhoneRecipient.id());

        if (recipientRow.isNewRow()) {
            recipientRow.set(MSORECIPIENT.CHAT_ID, waPhoneRecipient.getChatId());
            recipientRow.set(MSORECIPIENT.PROFILE_ID, waPhoneRecipient.getRecipientProfile().id());
            waPhoneRecipient.setId(recipientRow.getPKValue());
        }
        final Row waRecipientRow = getWARecipientContainer(recipientWAId).getOrInitRow(waRecipientTable,
                new WhereClause(Criteria.eq(waRecipientTable.getColumn(MSOWARECIPIENT.EXTERNAL_RECIPIENT_ID), recipientWAId)));

        if (waRecipientRow.isNewRow()) {
            waRecipientRow.set(MSOWARECIPIENT.EXTERNAL_RECIPIENT_ID, recipientWAId);
        }

        final Row waPhoneRecipientRow = dataContainer.getOrInitRow(phoneRecipientTable,
                new WhereClause(Criteria.eq(phoneRecipientTable.getColumn(MSOWAPHONERECIPIENT.RECIPIENT_ID), waPhoneRecipient.id()))
                        .and(Criteria.eq(phoneRecipientTable.getColumn(MSOWAPHONERECIPIENT.WA_RECIPIENT_ID), waRecipientRow.getPKValue())));

        if (waPhoneRecipientRow.isNewRow()) {
            waPhoneRecipientRow.set(MSOWAPHONERECIPIENT.WA_RECIPIENT_ID, waRecipientRow.getPKValue());
            waPhoneRecipientRow.set(MSOWAPHONERECIPIENT.RECIPIENT_ID, waPhoneRecipient.id());
            waPhoneRecipientRow.set(MSOWAPHONERECIPIENT.BUSINESS_PHONE_NUMBER_ID, waPhoneRecipient.getWaBusinessPhoneNumber().id());

            waPhoneRecipient.setWaPhoneRecipientId(waPhoneRecipientRow.getPKValue());
        }
        targetDataContainer.updateRow(recipientRow);
        targetDataContainer.updateRow(waRecipientRow);
        targetDataContainer.updateRow(waPhoneRecipientRow);
    }

    /**
     * <p>
     * Gets the message ids from the provided external message ids
     * </p>
     *
     * @param externalMessageIds Represents the collection of external message ids
     * @return the collection of message ids
     */
    public Collection<Long> getMessageIds(final Collection<String> externalMessageIds) {
        final Table waMessageTable = getWAMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(waMessageTable);

        selectQuery.addSelectClause(waMessageTable.getColumn(MSOWAMESSAGE.MESSAGE_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(waMessageTable.getColumn(MSOWAMESSAGE.EXTERNAL_MESSAGE_ID), externalMessageIds)));

        return adminDataStore().get(selectQuery).getRows(waMessageTable).map(messageRow -> (Long) messageRow.get(MSOWAMESSAGE.MESSAGE_ID)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Gets the optional {@link WABusinessPhoneNumber} from the provided external id
     * </p>
     *
     * @param externalPhoneNumberId Represents the external phone number id
     * @return the optional {@link WABusinessPhoneNumber}
     */
    public Optional<WABusinessPhoneNumber> getWABusinessPhoneNumber(final Long externalPhoneNumberId) {
        final Table phoneNumberTable = getWABusinessPhoneNumberTable();
        final SelectQuery selectQuery = SelectQuery.get(phoneNumberTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(phoneNumberTable.getColumn(MSOWABUSINESSPHONENUMBER.EXTERNAL_PHONE_NUMBER_ID), externalPhoneNumberId)));
        final Row phoneNumberRow = adminDataStore().get(selectQuery).getRow(phoneNumberTable);

        return Objects.nonNull(phoneNumberRow) ? Optional.of(convertRowToModel(phoneNumberRow)) : Optional.empty();
    }

    /**
     * <p>
     * Gets the optional {@link WABusinessAccount} from the provided external id
     * </p>
     *
     * @param externalWABusinessAccountId Represents the external whatsapp business account id
     * @return the optional {@link WABusinessAccount}
     */
    public Optional<WABusinessAccount> getWABusinessAccount(final Long externalWABusinessAccountId) {
        final Table waBussinessAccountTable = getWABussinessAccountTable();
        final SelectQuery selectQuery = SelectQuery.get(waBussinessAccountTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(waBussinessAccountTable.getColumn(MSOWABUSINESSACCOUNT.EXTERNAL_WABA_ID), externalWABusinessAccountId)));
        final Row businessAccountRow = adminDataStore().get(selectQuery).getRow(waBussinessAccountTable);

        return Objects.nonNull(businessAccountRow) ? Optional.of(convertRowToBusinessAccount(businessAccountRow)) : Optional.empty();
    }

    /**
     * <p>
     * Converts the business account row into {@link WABusinessAccount}
     * </p>
     *
     * @param businessAccountRow Represents the business account row
     * @return the {@link WABusinessPhoneNumber}
     */
    private WABusinessAccount convertRowToBusinessAccount(final Row businessAccountRow) {
        final WABusinessAccount waBusinessAccount = new WABusinessAccount();

        waBusinessAccount.setId(businessAccountRow.getPKValue());
        waBusinessAccount.setExternalBusinessId(businessAccountRow.get(MSOWABUSINESSACCOUNT.EXTERNAL_BUSINESS_ID));
        waBusinessAccount.setExternalWABAId(businessAccountRow.get(MSOWABUSINESSACCOUNT.EXTERNAL_WABA_ID));
        waBusinessAccount.setSyncStatus(WABusinessAccount.TemplateSyncStatus.getStatus(businessAccountRow.get(MSOWABUSINESSACCOUNT.SYNC_STATUS)));
        waBusinessAccount.setLastSyncedTime(businessAccountRow.get(MSOWABUSINESSACCOUNT.LAST_SYNCED_TIME));

        return waBusinessAccount;
    }

    /**
     * <p>
     * Gets the total count of the templates
     * </p>
     *
     * @param businessAccountId Represents the business account is
     * @param searchText        Represents the search text to filter the templates based on template name
     * @return the total count
     */
    public long getTotalTemplateCount(final Long businessAccountId, final String searchText) {
        return getTemplateCount(businessAccountId, searchText, null,
                List.of(WATemplate.Status.APPROVED.getId()), null);
    }

    /**
     * <p>
     * Gets the total count of templates matching the provided filters.
     * </p>
     *
     * @param businessAccountId Represents the business account id
     * @param searchText        Represents the search text to filter templates by name
     * @param categoryIds       Represents the category ids to filter by
     * @param statusIds         Represents the status ids to filter by
     * @param languageIds       Represents the language ids to filter by
     * @return the total count
     */
    public long getTemplateCount(final Long businessAccountId, final String searchText,
                                 final Collection<Short> categoryIds, final Collection<Short> statusIds,
                                 final Collection<Short> languageIds) {
        final Table templateTable = getTemplateTable();
        final CountQuery countQuery = CountQuery.get(templateTable);

        countQuery.setWhereClause(buildTemplateFilterWhereClause(templateTable, businessAccountId, searchText,
                categoryIds, statusIds, languageIds));

        return adminDataStore().get(countQuery);
    }

    /**
     * <p>
     * Stores the whatsapp templates into the database
     * </p>
     *
     * @param waBusinessAccount Represents the business account
     * @param templates         Represents the collection of templates
     */
    public void storeTemplates(final WABusinessAccount waBusinessAccount, final Collection<WATemplate> templates) {
        final DataContainer targetContainer = DataContainer.create();
        final Table templateTable = getTemplateTable();
        final Table templateComponentTable = getTemplateComponentTable();
        final Long currentTime = Instant.now().toEpochMilli();

        for (final WATemplate template : templates) {
            final Row templateRow = new Row(templateTable);
            final Long templateId = templateRow.getPKValue();

            templateRow.set(MSOWATEMPLATE.EXTERNAL_TEMPLATE_ID, template.getExternalTemplateId());
            templateRow.set(MSOWATEMPLATE.WA_BUSINESS_ACCOUNT_ID, waBusinessAccount.id());
            templateRow.set(MSOWATEMPLATE.NAME, template.getName());
            templateRow.set(MSOWATEMPLATE.LANGUAGE, template.getLanguage().getId());
            templateRow.set(MSOWATEMPLATE.PARAMETER_FORMAT, template.getParameterFormat().getId());
            templateRow.set(MSOWATEMPLATE.CATEGORY, template.getCategory().getId());
            templateRow.set(MSOWATEMPLATE.SUB_CATEGORY, template.getTemplateSubCategory().getId());
            templateRow.set(MSOWATEMPLATE.STATUS, template.getStatus().getId());
            templateRow.set(MSOWATEMPLATE.CREATED_AT, currentTime);
            templateRow.set(MSOWATEMPLATE.UPDATED_AT, currentTime);

            targetContainer.addNewRow(templateRow);
            final Header header = template.getHeader();
            final Footer footer = template.getFooter();
            final Body body = template.getBody();
            final Collection<Button> buttons = template.getButtons();

            if (Objects.nonNull(header)) {
                final Row componentRow = new Row(templateComponentTable);

                componentRow.set(MSOWATEMPLATECOMPONENT.TYPE, header.getType().getId());
                componentRow.set(MSOWATEMPLATECOMPONENT.TEMPLATE_ID, templateId);
                componentRow.set(MSOWATEMPLATECOMPONENT.HEADER_TYPE, header.getFormat().getId());
                componentRow.set(MSOWATEMPLATECOMPONENT.CONTENT, header.getText());
                componentRow.set(MSOWATEMPLATECOMPONENT.HAS_PLACEHOLDER, header.hasPlaceholder());
                targetContainer.addNewRow(componentRow);
            }

            if (Objects.nonNull(footer)) {
                final Row componentRow = new Row(templateComponentTable);

                componentRow.set(MSOWATEMPLATECOMPONENT.TEMPLATE_ID, templateId);
                componentRow.set(MSOWATEMPLATECOMPONENT.TYPE, footer.getType().getId());
                componentRow.set(MSOWATEMPLATECOMPONENT.CONTENT, footer.getText());
                targetContainer.addNewRow(componentRow);
            }

            if (Objects.nonNull(body)) {
                final Row componentRow = new Row(templateComponentTable);

                componentRow.set(MSOWATEMPLATECOMPONENT.TEMPLATE_ID, templateId);
                componentRow.set(MSOWATEMPLATECOMPONENT.TYPE, body.getType().getId());
                componentRow.set(MSOWATEMPLATECOMPONENT.CONTENT, body.getText());
                componentRow.set(MSOWATEMPLATECOMPONENT.HAS_PLACEHOLDER, body.hasPlaceholder());
                targetContainer.addNewRow(componentRow);
            }

            if (Objects.nonNull(buttons)) {
                for (final Button button : buttons) {
                    final Row componentRow = new Row(templateComponentTable);
                    final String url = button.getUrl();
                    final String phoneNumber = button.getPhoneNumber();

                    componentRow.set(MSOWATEMPLATECOMPONENT.TEMPLATE_ID, templateId);
                    componentRow.set(MSOWATEMPLATECOMPONENT.TYPE, button.getType().getId());
                    componentRow.set(MSOWATEMPLATECOMPONENT.BUTTON_TYPE, button.getButtonType().getId());
                    componentRow.set(MSOWATEMPLATECOMPONENT.CONTENT, button.getText());
                    componentRow.set(MSOWATEMPLATECOMPONENT.HAS_PLACEHOLDER, button.hasPlaceholder());
                    final JsonObject additionalData = Json.object();

                    additionalData.put(WAFields.INDEX, button.getIndex());
                    if (Objects.nonNull(url)) {
                        additionalData.put(WAFields.URL, url);
                    }

                    if (Objects.nonNull(phoneNumber)) {
                        additionalData.put(WAFields.PHONE_NUMBER, phoneNumber);
                    }
                    componentRow.set(MSOWATEMPLATECOMPONENT.ADDITIONAL_DATA, additionalData.toString());

                    targetContainer.addNewRow(componentRow);
                }
            }
        }

        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Updates the {@link WATemplate}
     * </p>
     *
     * @param waBusinessAccount Represents the {@link WABusinessAccount}
     * @param template          Represents the {@link WATemplate}
     */
    public void updateTemplate(final WABusinessAccount waBusinessAccount, final WATemplate template) {
        final Long externalTemplateId = template.getExternalTemplateId();
        final Table templateTable = getTemplateTable();
        final Table templateComponentTable = getTemplateComponentTable();
        final DataContainer templateContainer = getTemplateContainer(waBusinessAccount, externalTemplateId);
        final DataContainer targetContainer = DataContainer.create();
        final Row templateRow = templateContainer.getRow(templateTable);
        final Long templateId = templateRow.getPKValue();

        templateRow.set(MSOWATEMPLATE.NAME, template.getName());
        templateRow.set(MSOWATEMPLATE.LANGUAGE, template.getLanguage().getId());
        templateRow.set(MSOWATEMPLATE.PARAMETER_FORMAT, template.getParameterFormat().getId());
        templateRow.set(MSOWATEMPLATE.CATEGORY, template.getCategory().getId());
        templateRow.set(MSOWATEMPLATE.SUB_CATEGORY, template.getTemplateSubCategory().getId());
        templateRow.set(MSOWATEMPLATE.STATUS, template.getStatus().getId());
        templateRow.set(MSOWATEMPLATE.UPDATED_AT, Instant.now().toEpochMilli());
        template.setId(templateId);

        targetContainer.updateRow(templateRow);
        final Function<Component.Type, WhereClause> criteriaFetcher = type ->
                new WhereClause(Criteria.eq(templateComponentTable.getColumn(MSOWATEMPLATECOMPONENT.TEMPLATE_ID), templateId))
                        .and(Criteria.eq(templateComponentTable.getColumn(MSOWATEMPLATECOMPONENT.TYPE), type.getId()));
        final Header header = template.getHeader();
        final Footer footer = template.getFooter();
        final Body body = template.getBody();
        final Collection<Button> buttons = template.getButtons();
        final Row existingHeaderRow = templateContainer.getRow(templateComponentTable, criteriaFetcher.apply(Component.Type.HEADER));
        final Row existingFooterRow = templateContainer.getRow(templateComponentTable, criteriaFetcher.apply(Component.Type.FOOTER));
        final Row existingBodyRow = templateContainer.getRow(templateComponentTable, criteriaFetcher.apply(Component.Type.BODY));

        templateContainer.getRows(templateComponentTable, criteriaFetcher.apply(Component.Type.BUTTONS))
                .forEach(targetContainer::deleteRow);

        if (Objects.nonNull(header)) {
            final Row headerRow = Objects.nonNull(existingHeaderRow) ? existingHeaderRow : new Row(templateComponentTable);

            headerRow.set(MSOWATEMPLATECOMPONENT.TYPE, header.getType().getId());
            headerRow.set(MSOWATEMPLATECOMPONENT.TEMPLATE_ID, templateId);
            headerRow.set(MSOWATEMPLATECOMPONENT.HEADER_TYPE, header.getFormat().getId());
            headerRow.set(MSOWATEMPLATECOMPONENT.CONTENT, header.getText());
            headerRow.set(MSOWATEMPLATECOMPONENT.HAS_PLACEHOLDER, header.hasPlaceholder());
            targetContainer.updateRow(headerRow);
        } else if (Objects.nonNull(existingHeaderRow)) {
            targetContainer.deleteRow(existingHeaderRow);
        }

        if (Objects.nonNull(footer)) {
            final Row footerRow = Objects.nonNull(existingFooterRow) ? existingFooterRow : new Row(templateComponentTable);

            footerRow.set(MSOWATEMPLATECOMPONENT.TEMPLATE_ID, templateId);
            footerRow.set(MSOWATEMPLATECOMPONENT.TYPE, footer.getType().getId());
            footerRow.set(MSOWATEMPLATECOMPONENT.CONTENT, footer.getText());
            targetContainer.updateRow(footerRow);
        } else if (Objects.nonNull(existingFooterRow)) {
            targetContainer.deleteRow(existingFooterRow);
        }

        if (Objects.nonNull(body)) {
            final Row bodyRow = Objects.nonNull(existingBodyRow) ? existingBodyRow : new Row(templateComponentTable);

            bodyRow.set(MSOWATEMPLATECOMPONENT.TEMPLATE_ID, templateId);
            bodyRow.set(MSOWATEMPLATECOMPONENT.TYPE, body.getType().getId());
            bodyRow.set(MSOWATEMPLATECOMPONENT.CONTENT, body.getText());
            bodyRow.set(MSOWATEMPLATECOMPONENT.HAS_PLACEHOLDER, body.hasPlaceholder());
            targetContainer.updateRow(bodyRow);
        } else if (Objects.nonNull(existingBodyRow)) {
            targetContainer.deleteRow(existingBodyRow);
        }

        if (Objects.nonNull(buttons)) {
            for (final Button button : buttons) {
                final Row buttonRow = new Row(templateComponentTable);
                final String url = button.getUrl();
                final String phoneNumber = button.getPhoneNumber();

                buttonRow.set(MSOWATEMPLATECOMPONENT.TEMPLATE_ID, templateId);
                buttonRow.set(MSOWATEMPLATECOMPONENT.TYPE, button.getType().getId());
                buttonRow.set(MSOWATEMPLATECOMPONENT.BUTTON_TYPE, button.getButtonType().getId());
                buttonRow.set(MSOWATEMPLATECOMPONENT.CONTENT, button.getText());
                buttonRow.set(MSOWATEMPLATECOMPONENT.HAS_PLACEHOLDER, button.hasPlaceholder());
                final JsonObject additionalData = Json.object();

                additionalData.put(WAFields.INDEX, button.getIndex());
                if (Objects.nonNull(url)) {
                    additionalData.put(WAFields.URL, url);
                }

                if (Objects.nonNull(phoneNumber)) {
                    additionalData.put(WAFields.PHONE_NUMBER, phoneNumber);
                }
                buttonRow.set(MSOWATEMPLATECOMPONENT.ADDITIONAL_DATA, additionalData.toString());

                targetContainer.addNewRow(buttonRow);
            }
        }

        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Gets the template data container
     * </p>
     *
     * @param waBusinessAccount  Represents the {@link WABusinessAccount}
     * @param externalTemplateId Represents the external template id
     * @return the data container
     */
    private DataContainer getTemplateContainer(final WABusinessAccount waBusinessAccount, final Long externalTemplateId) {
        final Table templateTable = getTemplateTable();
        final Table templateComponentTable = getTemplateComponentTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);

        selectQuery.addJoinClause(new JoinClause(templateTable.getPKColumn(), templateComponentTable.getColumn(MSOWATEMPLATECOMPONENT.TEMPLATE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(templateTable.getColumn(MSOWATEMPLATE.WA_BUSINESS_ACCOUNT_ID), waBusinessAccount.id()))
                .and(Criteria.eq(templateTable.getColumn(MSOWATEMPLATE.EXTERNAL_TEMPLATE_ID), externalTemplateId)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Checks whether the template exist or not
     * </p>
     *
     * @param externalTemplateId Represents the external template id
     * @param waBusinessAccountId Represents the whatsapp business account id
     * @return true, if the template exists, false otherwise
     */
    public boolean isTemplateExists(final Long externalTemplateId, final Long waBusinessAccountId) {
        final Table templateTable = getTemplateTable();
        final Table waBussinessAccountTable = getWABussinessAccountTable();
        final CountQuery countQuery = CountQuery.get(templateTable);
        final Column waBussinessAccountTablePKColumn = waBussinessAccountTable.getPKColumn();

        countQuery.addJoinClause(new JoinClause(templateTable.getColumn(MSOWATEMPLATE.WA_BUSINESS_ACCOUNT_ID), waBussinessAccountTablePKColumn));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(templateTable.getColumn(MSOWATEMPLATE.EXTERNAL_TEMPLATE_ID), externalTemplateId))
                .and(Criteria.eq(waBussinessAccountTablePKColumn, waBusinessAccountId)));

        return adminDataStore().get(countQuery) == NumericalConstant.ONE;
    }

    /**
     * <p>
     * Updates the status of the template
     * </p>
     *
     * @param waBusinessAccountId Represents the whatsapp business account id
     * @param externalTemplateId  Represents the external template id
     * @param status              Represents the template status
     */
    public void updateTemplateStatus(final Long waBusinessAccountId, final Long externalTemplateId, final WATemplate.Status status) {
        final Table templateTable = getTemplateTable();
        final UpdateQuery updateQuery = UpdateQuery.get(templateTable);

        updateQuery.addUpdateColumn(templateTable.getColumn(MSOWATEMPLATE.STATUS), status.getId());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(templateTable.getColumn(MSOWATEMPLATE.EXTERNAL_TEMPLATE_ID), externalTemplateId))
                .and(Criteria.eq(templateTable.getColumn(MSOWATEMPLATE.WA_BUSINESS_ACCOUNT_ID), waBusinessAccountId)));

        adminDataStore().update(updateQuery);
    }

    /**
     * <p>
     * Gets the collection of {@link WATemplate}
     * </p>
     *
     * @param businessAccountId Represents the business account id
     * @param start             Represents the start of the template list
     * @param limit             Represents the limit of the template list
     * @param searchText        Represents the search text to filter the templates based on template name
     * @return the collection of {@link WATemplate}
     */
    public Collection<WATemplate> getTemplates(final Long businessAccountId, final int start, final int limit, final String searchText) {
        return getTemplates(businessAccountId, start, limit, searchText, null,
                List.of(WATemplate.Status.APPROVED.getId()), null);
    }

    /**
     * <p>
     * Gets the collection of {@link WATemplate} summaries matching the provided filters.
     * </p>
     *
     * @param businessAccountId Represents the business account id
     * @param start             Represents the start of the template list
     * @param limit             Represents the limit of the template list
     * @param searchText        Represents the search text to filter templates by name
     * @param categoryIds       Represents the category ids to filter by
     * @param statusIds         Represents the status ids to filter by
     * @param languageIds       Represents the language ids to filter by
     * @return the collection of {@link WATemplate}
     */
    public Collection<WATemplate> getTemplates(final Long businessAccountId, final int start, final int limit, final String searchText,
                                               final Collection<Short> categoryIds, final Collection<Short> statusIds,
                                               final Collection<Short> languageIds) {
        final Table templateTable = getTemplateTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);

        selectQuery.addSelectClause(List.of(templateTable.getPKColumn(), templateTable.getColumn(MSOWATEMPLATE.NAME),
                templateTable.getColumn(MSOWATEMPLATE.LANGUAGE), templateTable.getColumn(MSOWATEMPLATE.CATEGORY),
                templateTable.getColumn(MSOWATEMPLATE.STATUS)));
        selectQuery.setWhereClause(buildTemplateFilterWhereClause(templateTable, businessAccountId, searchText,
                categoryIds, statusIds, languageIds));
        selectQuery.setLimitClause(new LimitClause(start, limit));

        return adminDataStore().get(selectQuery).getRows(templateTable).map(this::convertRowToTemplate).toList();
    }

    /**
     * <p>
     * Gets the collection of {@link WATemplate} with components for the provided template ids.
     * </p>
     *
     * @param businessAccountId Represents the business account id
     * @param start             Represents the start of the template list
     * @param limit             Represents the limit of the template list
     * @param searchText        Represents the search text to filter templates by name
     * @param categoryIds       Represents the category ids to filter by
     * @param statusIds         Represents the status ids to filter by
     * @param languageIds       Represents the language ids to filter by
     * @return the map of template id to {@link WATemplate}
     */
    public Collection<WATemplate> getTemplatesWithComponents(final Long businessAccountId, final int start, final int limit, final String searchText,
                                                             final Collection<Short> categoryIds, final Collection<Short> statusIds,
                                                             final Collection<Short> languageIds) {
        final Table templateTable = getTemplateTable();
        final Table templateComponentTable = getTemplateComponentTable();
        final SelectQuery innerQuery = SelectQuery.get(templateTable);
        final Column templatePkColumn = templateTable.getPKColumn();

        innerQuery.addSelectClause(templatePkColumn);
        innerQuery.setWhereClause(buildTemplateFilterWhereClause(templateTable, businessAccountId, searchText,
                categoryIds, statusIds, languageIds));
        innerQuery.setLimitClause(new LimitClause(start, limit));
        final SelectQuery selectQuery = SelectQuery.get(templateTable);

        selectQuery.addJoinClause(new JoinClause(templatePkColumn, templateComponentTable.getColumn(MSOWATEMPLATECOMPONENT.TEMPLATE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(templatePkColumn,  innerQuery)));
        final DataContainer dataContainer = adminDataStore().get(selectQuery);

        return dataContainer.getRows(templateTable).map(templateRow -> convertRowToTemplate(templateRow, dataContainer)).toList();
    }

    /**
     * <p>
     * Builds the where clause for template list filters.
     * </p>
     */
    private WhereClause buildTemplateFilterWhereClause(final Table templateTable, final Long businessAccountId,
                                                       final String searchText, final Collection<Short> categoryIds,
                                                       final Collection<Short> statusIds, final Collection<Short> languageIds) {
        WhereClause whereClause = new WhereClause(Criteria.eq(templateTable.getColumn(MSOWATEMPLATE.WA_BUSINESS_ACCOUNT_ID), businessAccountId));

        if (Objects.nonNull(categoryIds) && !categoryIds.isEmpty()) {
            whereClause = whereClause.and(Criteria.in(templateTable.getColumn(MSOWATEMPLATE.CATEGORY), categoryIds));
        }

        if (Objects.nonNull(statusIds) && !statusIds.isEmpty()) {
            whereClause = whereClause.and(Criteria.in(templateTable.getColumn(MSOWATEMPLATE.STATUS), statusIds));
        }

        if (Objects.nonNull(languageIds) && !languageIds.isEmpty()) {
            whereClause = whereClause.and(Criteria.in(templateTable.getColumn(MSOWATEMPLATE.LANGUAGE), languageIds));
        }

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            whereClause = whereClause.and(Criteria.contains(templateTable.getColumn(MSOWATEMPLATE.NAME), searchText));
        }

        return whereClause;
    }

    /**
     * <p>
     * Converts the template row into {@link WATemplate}
     * </p>
     *
     * @param templateRow Represents the template row
     * @return the {@link WATemplate}
     */
    private WATemplate convertRowToTemplate(final Row templateRow) {
        final WATemplate waTemplate = new WATemplate();

        waTemplate.setId(templateRow.getPKValue());
        waTemplate.setName(templateRow.get(MSOWATEMPLATE.NAME));
        waTemplate.setLanguage(Language.getById(templateRow.get(MSOWATEMPLATE.LANGUAGE)));
        waTemplate.setCategory(WATemplate.Category.getById(templateRow.get(MSOWATEMPLATE.CATEGORY)));
        waTemplate.setStatus(WATemplate.Status.getById(templateRow.get(MSOWATEMPLATE.STATUS)));

        return waTemplate;
    }

    /**
     * <p>
     * Gets the {@link WATemplate} from the provided template id
     * </p>
     *
     * @param templateId Represents the template id
     * @return optional of {@link WATemplate}
     */
    public Optional<WATemplate> getTemplate(final Long templateId) {
        final Table templateTable = getTemplateTable();
        final Table tempalteComponentTable = getTemplateComponentTable();
        final Column templatePkColumn = templateTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);

        selectQuery.addJoinClause(new JoinClause(templatePkColumn, tempalteComponentTable.getColumn(MSOWATEMPLATECOMPONENT.TEMPLATE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(templatePkColumn, templateId)));
        final DataContainer dataContainer  = adminDataStore().get(selectQuery);
        final Row templateRow = dataContainer.getRow(templateTable);

        return Objects.isNull(templateRow) ? Optional.empty() : Optional.of(convertRowToTemplate(templateRow, dataContainer));
    }

    /**
     * <p>
     * Gets the {@link WATemplate} from the provided template ids
     * </p>
     *
     * @param templateIds Represents the collection of template ids
     * @return optional of {@link WATemplate}
     */
    public Map<Long, String> getTemplateNames(final Collection<Long> templateIds) {
        final Table templateTable = getTemplateTable();
        final Column templatePkColumn = templateTable.getPKColumn();
        final Column templateNameColumn = templateTable.getColumn(MSOWATEMPLATE.NAME);
        final SelectQuery selectQuery = SelectQuery.get(templateTable);

        selectQuery.addSelectClause(List.of(templatePkColumn, templateNameColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(templatePkColumn, templateIds)));

        return adminDataStore().get(selectQuery).getRows(templateTable).collect(Collectors.toMap(Row::getPKValue, row -> row.get(templateNameColumn)));
    }

    /**
     * <p>
     * Convert template row into {@link WATemplate}
     * </p>
     *
     * @param templateRow   Represents the template row
     * @param dataContainer Represents the container which contains additional template data
     * @return {@link WATemplate}
     */
    private WATemplate convertRowToTemplate(final Row templateRow, final DataContainer dataContainer) {
        final WATemplate waTemplate = new WATemplate();
        final WATemplate.Category category = WATemplate.Category.getById(templateRow.get(MSOWATEMPLATE.CATEGORY));
        final short subCategoryId = templateRow.get(MSOWATEMPLATE.SUB_CATEGORY);
        final Long templateId = templateRow.getPKValue();
        final Table componentTable = getTemplateComponentTable();

        waTemplate.setId(templateId);
        waTemplate.setExternalTemplateId(templateRow.get(MSOWATEMPLATE.EXTERNAL_TEMPLATE_ID));
        waTemplate.setName(templateRow.get(MSOWATEMPLATE.NAME));
        waTemplate.setLanguage(Language.getById(templateRow.get(MSOWATEMPLATE.LANGUAGE)));
        waTemplate.setParameterFormat(WATemplate.ParameterFormat.getById(templateRow.get(MSOWATEMPLATE.PARAMETER_FORMAT)));
        waTemplate.setCategory(category);
        waTemplate.setTemplateSubCategory(switch (category) {
            case MARKETING -> WATemplate.MarketingSubCategory.getById(subCategoryId);
            case UTILITY -> WATemplate.UtilitySubCategory.getById(subCategoryId);
            case AUTHENTICATION -> null;
        });
        waTemplate.setStatus(WATemplate.Status.getById(templateRow.get(MSOWATEMPLATE.STATUS)));
        waTemplate.setCreatedAt(templateRow.get(MSOWATEMPLATE.CREATED_AT));
        waTemplate.setUpdatedAt(templateRow.get(MSOWATEMPLATE.UPDATED_AT));

        dataContainer.getRows(componentTable, new WhereClause(Criteria.eq(componentTable.getColumn(MSOWATEMPLATECOMPONENT.TEMPLATE_ID), templateId)))
                .forEach(componentRow -> {
                    final Component.Type componentType = Component.Type.getById(componentRow.get(MSOWATEMPLATECOMPONENT.TYPE));

                    switch (componentType) {
                        case HEADER -> {
                            final Header.Format headerFormat = Header.Format.getById(componentRow.get(MSOWATEMPLATECOMPONENT.HEADER_TYPE));
                            final Header header = new Header(headerFormat);

                            header.setHasPlaceholder(componentRow.get(MSOWATEMPLATECOMPONENT.HAS_PLACEHOLDER));
                            header.setText(componentRow.get(MSOWATEMPLATECOMPONENT.CONTENT));
                            waTemplate.setHeader(header);
                        }
                        case BODY -> {
                            final Body body = new Body();

                            body.setHasPlaceholder(componentRow.get(MSOWATEMPLATECOMPONENT.HAS_PLACEHOLDER));
                            body.setText(componentRow.get(MSOWATEMPLATECOMPONENT.CONTENT));
                            waTemplate.setBody(body);
                        }
                        case FOOTER -> {
                            final Footer footer = new Footer();

                            footer.setText(componentRow.get(MSOWATEMPLATECOMPONENT.CONTENT));
                            waTemplate.setFooter(footer);
                        }
                        case BUTTONS -> {
                            final Button.ButtonType buttonType = Button.ButtonType.getById(componentRow.get(MSOWATEMPLATECOMPONENT.BUTTON_TYPE));
                            final Button button = new Button(buttonType);
                            final JsonObject additionalData = Json.object((String) componentRow.get(MSOWATEMPLATECOMPONENT.ADDITIONAL_DATA));

                            button.setHasPlaceholder(componentRow.get(MSOWATEMPLATECOMPONENT.HAS_PLACEHOLDER));
                            button.setText(componentRow.get(MSOWATEMPLATECOMPONENT.CONTENT));
                            button.setIndex(additionalData.getInt(WAFields.INDEX));

                            switch (buttonType) {
                                case URL -> button.setUrl(additionalData.getString(WAFields.URL));
                                case PHONE_NUMBER -> button.setPhoneNumber(additionalData.getString(WAFields.PHONE_NUMBER));
                            }
                            waTemplate.addButton(button);
                        }
                    }
                });

        return waTemplate;
    }

    /**
     * <p>
     * Updates the business phone number registration details
     * </p>
     *
     * @param waBusinessPhoneNumber Represents the {@link WABusinessPhoneNumber}
     */
    public void updatePhoneRegistrationDetails(final WABusinessPhoneNumber waBusinessPhoneNumber) {
        final Table waPhoneNumberTable = getWABusinessPhoneNumberTable();
        final UpdateQuery updateQuery = UpdateQuery.get(waPhoneNumberTable);

        updateQuery.addUpdateColumn(waPhoneNumberTable.getColumn(MSOWABUSINESSPHONENUMBER.IS_REGISTERED), waBusinessPhoneNumber.isRegistered());
        updateQuery.addUpdateColumn(waPhoneNumberTable.getColumn(MSOWABUSINESSPHONENUMBER.ENCODED_PIN), waBusinessPhoneNumber.getEncodedPIN());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(waPhoneNumberTable.getColumn(MSOWABUSINESSPHONENUMBER.EXTERNAL_PHONE_NUMBER_ID), waBusinessPhoneNumber.getExternalPhoneNumberId())));

        adminDataStore().update(updateQuery);
    }

    /**
     * <p>
     * Converts the phone number row into {@link WABusinessPhoneNumber}
     * </p>
     *
     * @param phoneNumberRow Represents the phone number row
     * @return the {@link WABusinessPhoneNumber}
     */
    private WABusinessPhoneNumber convertRowToModel(final Row phoneNumberRow) {
        final WABusinessPhoneNumber waBusinessPhoneNumber = new WABusinessPhoneNumber();
        final Channel channel = new Channel();

        channel.setId(phoneNumberRow.get(MSOWABUSINESSPHONENUMBER.CHANNEL_ID));
        channel.setType(Channel.ChannelType.WHATSAPP_BUSINESS);
        waBusinessPhoneNumber.setId(phoneNumberRow.getPKValue());
        waBusinessPhoneNumber.setChannel(channel);
        waBusinessPhoneNumber.setDisplayPhoneNumber(phoneNumberRow.get(MSOWABUSINESSPHONENUMBER.DISPLAY_PHONE_NUMBER));
        waBusinessPhoneNumber.setWaBusinessAccountId(phoneNumberRow.get(MSOWABUSINESSPHONENUMBER.WA_BUSINESS_ACCOUNT_ID));
        waBusinessPhoneNumber.setExternalPhoneNumberId(phoneNumberRow.get(MSOWABUSINESSPHONENUMBER.EXTERNAL_PHONE_NUMBER_ID));
        waBusinessPhoneNumber.setRegistered(phoneNumberRow.get(MSOWABUSINESSPHONENUMBER.IS_REGISTERED));

        return waBusinessPhoneNumber;
    }

    /**
     * <p>
     * Loads the {@link WABusinessPhoneNumber} into container
     * </p>
     *
     * @param waBusinessPhoneNumber Represents the {@link WABusinessPhoneNumber}
     * @param dataContainer         Represents the data container which contains business account details
     * @param targetContainer       Represents the target container
     */
    private void loadWAPhoneNumberIntoContainer(final WABusinessPhoneNumber waBusinessPhoneNumber, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table businessPhoneNumberTable = getWABusinessPhoneNumberTable();
        final Long externalPhoneNumberId = waBusinessPhoneNumber.getExternalPhoneNumberId();
        final Row phoneNumberRow = dataContainer.getOrInitRow(businessPhoneNumberTable, new WhereClause(Criteria.eq(businessPhoneNumberTable.getColumn(MSOWABUSINESSPHONENUMBER.EXTERNAL_PHONE_NUMBER_ID), externalPhoneNumberId)));

        if (phoneNumberRow.isNewRow()) {
            phoneNumberRow.set(MSOWABUSINESSPHONENUMBER.EXTERNAL_PHONE_NUMBER_ID, externalPhoneNumberId);
            phoneNumberRow.set(MSOWABUSINESSPHONENUMBER.CHANNEL_ID, waBusinessPhoneNumber.getChannel().id());
            phoneNumberRow.set(MSOWABUSINESSPHONENUMBER.WA_BUSINESS_ACCOUNT_ID, waBusinessPhoneNumber.getWaBusinessAccountId());
            phoneNumberRow.set(MSOWABUSINESSPHONENUMBER.IS_REGISTERED, waBusinessPhoneNumber.isRegistered());
        } else {
            waBusinessPhoneNumber.setEncodedPIN(phoneNumberRow.get(MSOWABUSINESSPHONENUMBER.ENCODED_PIN));
            waBusinessPhoneNumber.setRegistered(phoneNumberRow.get(MSOWABUSINESSPHONENUMBER.IS_REGISTERED));
        }
        waBusinessPhoneNumber.setId(phoneNumberRow.getPKValue());
        phoneNumberRow.set(MSOWABUSINESSPHONENUMBER.DISPLAY_PHONE_NUMBER, waBusinessPhoneNumber.getDisplayPhoneNumber());
        phoneNumberRow.set(MSOWABUSINESSPHONENUMBER.IS_DELETED, false);

        targetContainer.updateRow(phoneNumberRow);
    }

    /**
     * <p>
     * Gets the optional {@link WABusinessPhoneNumber} from the provided whatsapp business phone number id
     * </p>
     *
     * @param businessPhoneNumberId Represents the business phone number id
     * @return the optional {@link WABusinessPhoneNumber}
     */
    public Optional<WABusinessPhoneNumber> getBusinessPhoneNumber(final Long businessPhoneNumberId) {
        final Table phoneNumberTable = getWABusinessPhoneNumberTable();
        final SelectQuery selectQuery = SelectQuery.get(phoneNumberTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(phoneNumberTable.getPKColumn(), businessPhoneNumberId)));
        final Row phoneNumberRow = adminDataStore().get(selectQuery).getRow(phoneNumberTable);

        return Objects.nonNull(phoneNumberRow) ? Optional.of(convertRowToModel(phoneNumberRow)) : Optional.empty();
    }

    /**
     * <p>
     * Deletes the state of the business phone number
     * </p>
     *
     * @param displayPhoneNumber Represents the display phone number
     * @param externalWABAId     Represents the external whatsapp business account id
     * @return true, if the number is deleted, false otherwise
     */
    public Long deletePhoneNumber(final Long externalWABAId, final String displayPhoneNumber) {
        final Table waBusinessAccountTable = getWABussinessAccountTable();
        final Table phoneNumberTable = getWABusinessPhoneNumberTable();
        final SelectQuery selectQuery = SelectQuery.get(waBusinessAccountTable);
        final String resolvedDisplayPhoneNumber = displayPhoneNumber.replaceAll(NON_DIGIT_REGEX_PATTERN, "");
        final DataContainer targetContainer = DataContainer.create();

        selectQuery.addSelectClause(phoneNumberTable);
        selectQuery.addJoinClause(new JoinClause(waBusinessAccountTable.getPKColumn(), phoneNumberTable.getColumn(MSOWABUSINESSPHONENUMBER.WA_BUSINESS_ACCOUNT_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(waBusinessAccountTable.getColumn(MSOWABUSINESSACCOUNT.EXTERNAL_WABA_ID), externalWABAId))
                .and(Criteria.eq(phoneNumberTable.getColumn(MSOWABUSINESSPHONENUMBER.IS_DELETED), false)));

        final Optional<Row> optionalPhoneRow = adminDataStore().get(selectQuery).getRows(phoneNumberTable).filter(phoneNumberRow -> {
            final String existingPhoneNumber = ((String) phoneNumberRow.get(MSOWABUSINESSPHONENUMBER.DISPLAY_PHONE_NUMBER)).replaceAll(NON_DIGIT_REGEX_PATTERN, "");

            return Objects.equals(resolvedDisplayPhoneNumber, existingPhoneNumber);
        }).findFirst();

        if (optionalPhoneRow.isPresent()) {
            final Row phoneNumberRow = optionalPhoneRow.get();

            phoneNumberRow.set(MSOWABUSINESSPHONENUMBER.IS_DELETED, true);
            targetContainer.updateRow(phoneNumberRow);
            adminDataStore().commitChanges(targetContainer);

            return phoneNumberRow.getPKValue();
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Checks whether the message exist or not
     * </p>
     *
     * @param externalMessageId Represents the external message id
     * @return true, if message exists, false otherwise
     */
    public boolean isMessageExists(final String externalMessageId) {
        final Table waMessageTable = getWAMessageTable();
        final CountQuery countQuery = CountQuery.get(waMessageTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(waMessageTable.getColumn(MSOWAMESSAGE.EXTERNAL_MESSAGE_ID), externalMessageId)));

        return adminDataStore().get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Gets the {@link WAMessage} from the provided message id
     * </p>
     *
     * @param chatId    Represents the chat id
     * @param messageId Represents the message id
     * @return the optional of {@link WAMessage}
     */
    public Optional<WAMessage> getInboundMessage(final Long chatId, final Long messageId) {
        final Table chatTable = getChatTable();
        final Table recipientTable = getRecipientTable();
        final Table waPhoneRecipientTable = getWAPhoneRecipientTable();
        final Table messageTable = getMessageTable();
        final Table waMessageTable = getWAMessageTable();
        final Table businessPhoneNumberTable = getWABusinessPhoneNumberTable();
        final SelectQuery selectQuery = SelectQuery.get(chatTable);
        final Column messagePkColumn = messageTable.getPKColumn();
        final Column messageIdColumn = waMessageTable.getColumn(MSOWAMESSAGE.MESSAGE_ID);
        final Column chatTablePKColumn = chatTable.getPKColumn();
        final WhereClause whereClause = new WhereClause(Criteria.eq(chatTablePKColumn, chatId))
                .and(Criteria.eq(messageTable.getColumn(MSOMESSAGE.MESSAGE_FLOW), Message.MessageFlow.INBOUND.getId()));
        final boolean isMessageIdNonNull = Objects.nonNull(messageId);

        selectQuery.addSelectClause(List.of(chatTablePKColumn, messagePkColumn, messageIdColumn, waMessageTable.getColumn(MSOWAMESSAGE.EXTERNAL_MESSAGE_ID),
                waMessageTable.getColumn(MSOWAMESSAGE.IS_DELETED)));
        selectQuery.addSelectClause(waPhoneRecipientTable, businessPhoneNumberTable);

        selectQuery.addJoinClause(List.of(new JoinClause(chatTablePKColumn, messageTable.getColumn(MSOMESSAGE.CHAT_ID)),
                new JoinClause(messageTable.getPKColumn(), messageIdColumn),
                new JoinClause(chatTablePKColumn, recipientTable.getColumn(MSORECIPIENT.CHAT_ID)),
                new JoinClause(recipientTable.getPKColumn(), waPhoneRecipientTable.getColumn(MSOWAPHONERECIPIENT.RECIPIENT_ID)),
                new JoinClause(waPhoneRecipientTable.getColumn(MSOWAPHONERECIPIENT.BUSINESS_PHONE_NUMBER_ID), businessPhoneNumberTable.getPKColumn())));
        selectQuery.setWhereClause(isMessageIdNonNull ? whereClause.and(Criteria.eq(messagePkColumn, messageId)) : whereClause);

        if (!isMessageIdNonNull) {
            selectQuery.addOrderByClause(new OrderByClause(messageTable.getColumn(MSOMESSAGE.SEND_TIME), OrderByClause.OrderType.DESCENDING));
            selectQuery.setLimitClause(new LimitClause(NumericalConstant.ONE));
        }
        final DataContainer dataContainer = adminDataStore().get(selectQuery);
        final Row waMessageRow = dataContainer.getRow(waMessageTable, isMessageIdNonNull ? new WhereClause(Criteria.eq(messageIdColumn, messageId)) : null);

        if (Objects.isNull(waMessageRow)) {
            return Optional.empty();
        } else {
            return waMessageRow.get(MSOWAMESSAGE.IS_DELETED) ? Optional.empty() : Optional.of(convertRowToMessage(waMessageRow, dataContainer));
        }
    }

    /**
     * <p>
     * Gets the total count of chats associated with the provided phone number id
     * </p>
     *
     * @param phoneNumberId Represents the business phone number id
     * @return the total chat count
     */
    public long getChatCount(final Long phoneNumberId) {
        final Table businessPhoneNumberTable = getWABusinessPhoneNumberTable();
        final Table chatTable = getChatTable();
        final CountQuery countQuery = CountQuery.get(businessPhoneNumberTable);

        countQuery.addJoinClause(new JoinClause(businessPhoneNumberTable.getColumn(MSOWABUSINESSPHONENUMBER.CHANNEL_ID),
                chatTable.getColumn(MSOCHAT.CHANNEL_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(businessPhoneNumberTable.getPKColumn(), phoneNumberId)));

        return adminDataStore().get(countQuery);
    }

    /**
     * <p>
     * Gets the collection of chat ids by the provided phone number id with pagination
     * </p>
     *
     * @param phoneNumberId Represents the business phone number id
     * @param start         Represents the start index
     * @param limit         Represents the limit of records
     * @return the collection of chat ids
     */
    public Collection<Long> getChatIds(final Long phoneNumberId, final int start, final int limit) {
        final Table businessPhoneNumberTable = getWABusinessPhoneNumberTable();
        final Table chatTable = getChatTable();
        final SelectQuery selectQuery = SelectQuery.get(businessPhoneNumberTable);

        selectQuery.addSelectClause(chatTable.getPKColumn());
        selectQuery.addJoinClause(new JoinClause(businessPhoneNumberTable.getColumn(MSOWABUSINESSPHONENUMBER.CHANNEL_ID),
                chatTable.getColumn(MSOCHAT.CHANNEL_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(businessPhoneNumberTable.getPKColumn(), phoneNumberId)));
        selectQuery.setLimitClause(new LimitClause(start, limit));

        return adminDataStore().get(selectQuery).getRows(chatTable).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Marks the messages as deleted for the provided chat ids
     * </p>
     *
     * @param chatIds Represents the collection of chat ids
     */
    public void markMessagesAsDeleted(final Collection<Long> chatIds) {
        final long totalMessageCount = getActiveMessageCount(chatIds);
        int start = 1;

        while (start <= totalMessageCount) {
            final Collection<Long> waMessageIds = getActiveMessageIds(chatIds, MESSAGE_BATCH_SIZE);

            if (!waMessageIds.isEmpty()) {
                final Table waMessageTable = getWAMessageTable();
                final UpdateQuery updateQuery = UpdateQuery.get(waMessageTable);

                updateQuery.addUpdateColumn(waMessageTable.getColumn(MSOWAMESSAGE.IS_DELETED), true);
                updateQuery.setWhereClause(new WhereClause(Criteria.in(waMessageTable.getPKColumn(), waMessageIds)));

                adminDataStore().update(updateQuery);
            }
            start += MESSAGE_BATCH_SIZE;
        }
    }

    /**
     * <p>
     * Gets the WhatsApp message ids for the provided chat ids with pagination
     * </p>
     *
     * @param chatIds Represents the collection of chat ids
     * @param limit   Represents the limit of records
     * @return the collection of WhatsApp message ids
     */
    private Collection<Long> getActiveMessageIds(final Collection<Long> chatIds, final int limit) {
        final Table messageTable = getMessageTable();
        final Table waMessageTable = getWAMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(messageTable);
        final Column waMessageTablePKColumn = waMessageTable.getPKColumn();

        selectQuery.addSelectClause(waMessageTablePKColumn);
        selectQuery.addJoinClause(new JoinClause(messageTable.getPKColumn(), waMessageTable.getColumn(MSOWAMESSAGE.MESSAGE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(messageTable.getColumn(MSOMESSAGE.CHAT_ID), chatIds))
                .and(Criteria.eq(waMessageTable.getColumn(MSOWAMESSAGE.IS_DELETED), false)));
        selectQuery.setLimitClause(new LimitClause(limit));

        return adminDataStore().get(selectQuery).getRows(waMessageTable).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Gets the total message count for the provided collection of chat ids
     * </p>
     *
     * @param chatIds Represents the collection of chat ids
     * @return the total message count
     */
    public long getActiveMessageCount(final Collection<Long> chatIds) {
        final Table messageTable = getMessageTable();
        final Table waMessageTable = getWAMessageTable();
        final CountQuery countQuery = CountQuery.get(messageTable);

        countQuery.addJoinClause(new JoinClause(messageTable.getPKColumn(), waMessageTable.getColumn(MSOWAMESSAGE.MESSAGE_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.in(messageTable.getColumn(MSOMESSAGE.CHAT_ID), chatIds))
                .and(Criteria.eq(waMessageTable.getColumn(MSOWAMESSAGE.IS_DELETED), false)));

        return adminDataStore().get(countQuery);
    }

    /**
     * <p>
     * Converts the row into {@link WAMessage}
     * </p>
     *
     * @param waMessageRow  Represents the message row
     * @param dataContainer Represents the data container
     * @return the {@link WAMessage}
     */
    private WAMessage convertRowToMessage(final Row waMessageRow, final DataContainer dataContainer) {
        final WAMessage waMessage = new WAMessage();
        final WAPhoneRecipient waPhoneRecipient = new WAPhoneRecipient();
        final WABusinessPhoneNumber waBusinessPhoneNumber = new WABusinessPhoneNumber();

        waMessage.setId(waMessageRow.get(MSOWAMESSAGE.MESSAGE_ID));
        waMessage.setExternalMessageId(waMessageRow.get(MSOWAMESSAGE.EXTERNAL_MESSAGE_ID));
        final Row waPhoneRecipientRow = dataContainer.getRow(getWAPhoneRecipientTable());
        final Long phoneNumberId = waPhoneRecipientRow.get(MSOWAPHONERECIPIENT.BUSINESS_PHONE_NUMBER_ID);

        waPhoneRecipient.setId(waPhoneRecipientRow.get(MSOWAPHONERECIPIENT.RECIPIENT_ID));
        final Row phoneNumberRow = dataContainer.getRow(getWABusinessPhoneNumberTable(), phoneNumberId);

        waBusinessPhoneNumber.setId(phoneNumberId);
        waBusinessPhoneNumber.setWaBusinessAccountId(phoneNumberRow.get(MSOWABUSINESSPHONENUMBER.WA_BUSINESS_ACCOUNT_ID));
        waBusinessPhoneNumber.setExternalPhoneNumberId(phoneNumberRow.get(MSOWABUSINESSPHONENUMBER.EXTERNAL_PHONE_NUMBER_ID));

        waPhoneRecipient.setWaBusinessPhoneNumber(waBusinessPhoneNumber);
        waMessage.setWaRecipient(waPhoneRecipient);

        return waMessage;
    }

    /**
     * <p>
     * Converts the WhatsApp recipient row into {@link WAPhoneRecipient}
     * </p>
     *
     * @param waPhoneRecipientRow Represents the WhatsApp phone recipient row
     * @param dataContainer       Represents the data container which contains recipient details
     * @return the {@link WAPhoneRecipient}
     */
    private WAPhoneRecipient convertRowToWARecipient(final Row waPhoneRecipientRow, final DataContainer dataContainer) {
        final Row businessPhoneNumberRow = dataContainer.getRow(getWABusinessPhoneNumberTable(), (Long) waPhoneRecipientRow.get(MSOWAPHONERECIPIENT.BUSINESS_PHONE_NUMBER_ID));
        final Row waRecipientRow = dataContainer.getRow(getWARecipientTable(), (Long) waPhoneRecipientRow.get(MSOWAPHONERECIPIENT.WA_RECIPIENT_ID));
        final WAPhoneRecipient waPhoneRecipient = new WAPhoneRecipient();
        final WABusinessPhoneNumber waBusinessPhoneNumber = new WABusinessPhoneNumber();

        waBusinessPhoneNumber.setId(businessPhoneNumberRow.getPKValue());
        waBusinessPhoneNumber.setExternalPhoneNumberId(businessPhoneNumberRow.get(MSOWABUSINESSPHONENUMBER.EXTERNAL_PHONE_NUMBER_ID));
        waBusinessPhoneNumber.setDisplayPhoneNumber(businessPhoneNumberRow.get(MSOWABUSINESSPHONENUMBER.DISPLAY_PHONE_NUMBER));
        waBusinessPhoneNumber.setWaBusinessAccountId(businessPhoneNumberRow.get(MSOWABUSINESSPHONENUMBER.WA_BUSINESS_ACCOUNT_ID));

        waPhoneRecipient.setId(waPhoneRecipientRow.get(MSOWAPHONERECIPIENT.RECIPIENT_ID));
        waPhoneRecipient.setExternalRecipientId(waRecipientRow.get(MSOWARECIPIENT.EXTERNAL_RECIPIENT_ID));
        waPhoneRecipient.setWaBusinessPhoneNumber(waBusinessPhoneNumber);

        return waPhoneRecipient;
    }

    /**
     * <p>
     * Gets the {@link DataContainer} which contains WhatsApp business account details
     * </p>
     *
     * @param waBusinessAccount Represents the {@link WABusinessAccount}
     * @return the {@link DataContainer}
     */
    private DataContainer getBusinessAccountDataContainer(final WABusinessAccount waBusinessAccount) {
        final Table waBussinessAccountTable = getWABussinessAccountTable();
        final Table businessPhoneNumberTable = getWABusinessPhoneNumberTable();
        final Table refreshTokenTable = syncManager.getRefreshableTokenTable();
        final Table channelTable = getChannelTable();
        final SelectQuery selectQuery = SelectQuery.get(waBussinessAccountTable);

        selectQuery.addJoinClause(List.of(new JoinClause(waBussinessAccountTable.getPKColumn(), businessPhoneNumberTable.getColumn(MSOWABUSINESSPHONENUMBER.WA_BUSINESS_ACCOUNT_ID)),
                new JoinClause(businessPhoneNumberTable.getColumn(MSOWABUSINESSPHONENUMBER.CHANNEL_ID), channelTable.getPKColumn()),
                new JoinClause(waBussinessAccountTable.getColumn(MSOWABUSINESSACCOUNT.TOKEN_ID), refreshTokenTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(waBussinessAccountTable.getColumn(MSOWABUSINESSACCOUNT.EXTERNAL_BUSINESS_ID), waBusinessAccount.getExternalBusinessId()))
                .and(Criteria.eq(waBussinessAccountTable.getColumn(MSOWABUSINESSACCOUNT.EXTERNAL_WABA_ID), waBusinessAccount.getExternalWABAId())));

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Gets the count of active templates for the provided business account ids
     * </p>
     *
     * @param businessAccountIds Represents the collection of business account ids
     * @return the map with business account id as key and active template count as value
     */
    public Map<Long, Long> getTemplateCount(final Collection<Long> businessAccountIds, final Collection<Short> statusIds) {
        final Table templateTable = getTemplateTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);
        final Column businessAccountIdColumn = templateTable.getColumn(MSOWATEMPLATE.WA_BUSINESS_ACCOUNT_ID);

        selectQuery.addSelectClause(List.of(businessAccountIdColumn, templateTable.getColumn(MSOWATEMPLATE.ID, WAFields.TEMPLATE_COUNT).count()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(businessAccountIdColumn, businessAccountIds))
                .and(Criteria.in(templateTable.getColumn(MSOWATEMPLATE.STATUS), statusIds)));
        selectQuery.addGroupByClause(new GroupByClause(businessAccountIdColumn));
        final List<Map<String, Object>> data = adminDataStore().getData(selectQuery);

        return data.stream().collect(Collectors.toMap(
                row -> (Long) row.get(MSOWATEMPLATE.WA_BUSINESS_ACCOUNT_ID),
                row -> (Long) row.get(WAFields.TEMPLATE_COUNT)
        ));
    }

    /**
     * <p>
     * Gets the optional {@link WABusinessAccount} from the provided internal id
     * </p>
     *
     * @param businessAccountId Represents the whatsapp business account id
     * @return the optional {@link WABusinessAccount}
     */
    public Optional<WABusinessAccount> getBusinessAccountById(final Long businessAccountId) {
        final Table waBusinessAccountTable = getWABussinessAccountTable();
        final SelectQuery selectQuery = SelectQuery.get(waBusinessAccountTable);

        selectQuery.addSelectClause(List.of(waBusinessAccountTable.getColumn(MSOWABUSINESSACCOUNT.SYNC_STATUS),
                waBusinessAccountTable.getColumn(MSOWABUSINESSACCOUNT.LAST_SYNCED_TIME), waBusinessAccountTable.getColumn(MSOWABUSINESSACCOUNT.EXTERNAL_WABA_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(waBusinessAccountTable.getPKColumn(), businessAccountId)));
        final Row businessAccountRow = adminDataStore().get(selectQuery).getRow(waBusinessAccountTable);

        return Objects.nonNull(businessAccountRow) ? Optional.of(convertRowToBusinessAccount(businessAccountRow)) : Optional.empty();
    }

    /**
     * <p>
     * Updates the sync status of the whatsapp business account
     * </p>
     *
     * @param businessAccountId  Represents the whatsapp business account id
     * @param templateSyncStatus Represents the {@link WABusinessAccount.TemplateSyncStatus}
     * @param lastSyncedTime     Represents the last synced time in epoch millis
     */
    public void updateSyncStatus(final Long businessAccountId, final WABusinessAccount.TemplateSyncStatus templateSyncStatus, final Long lastSyncedTime) {
        final Table waBusinessAccountTable = getWABussinessAccountTable();
        final UpdateQuery updateQuery = UpdateQuery.get(waBusinessAccountTable);

        updateQuery.addUpdateColumn(waBusinessAccountTable.getColumn(MSOWABUSINESSACCOUNT.SYNC_STATUS), templateSyncStatus.getId());

        if (Objects.nonNull(lastSyncedTime)) {
            updateQuery.addUpdateColumn(waBusinessAccountTable.getColumn(MSOWABUSINESSACCOUNT.LAST_SYNCED_TIME), lastSyncedTime);
        }
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(waBusinessAccountTable.getPKColumn(), businessAccountId)));
        adminDataStore().update(updateQuery);
    }

    /**
     * <p>
     * Gets the distinct template languages for the business account
     * </p>
     *
     * @param businessAccountId Represents the business account id
     * @param statusIds         Represents the status ids
     * @return the collection of {@link Language}
     */
    public Collection<Short> getTemplateLanguageIds(final Long businessAccountId, final Collection<Short> statusIds) {
        final Table templateTable = getTemplateTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);
        final Column languageColumn = templateTable.getColumn(MSOWATEMPLATE.LANGUAGE);

        selectQuery.addSelectClause(languageColumn.distinct());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(templateTable.getColumn(MSOWATEMPLATE.WA_BUSINESS_ACCOUNT_ID), businessAccountId))
                .and(Criteria.in(templateTable.getColumn(MSOWATEMPLATE.STATUS), statusIds)));

        return adminDataStore().get(selectQuery).getRows(templateTable).map(row -> (Short) row.get(languageColumn)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Checks if the business account exists for the provided business account id
     * </p>
     *
     * @param businessAccountId Represents the business account id
     * @return true if the business account exists, false otherwise
     */
    public boolean isBusinessAccountExists(final Long businessAccountId) {
        final Table waBusinessAccountTable = getWABussinessAccountTable();
        final CountQuery countQuery = CountQuery.get(waBusinessAccountTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(waBusinessAccountTable.getPKColumn(), businessAccountId)));

        return adminDataStore().get(countQuery) > 0;
    }
}
