package com.tlc.social.server.instagram.manager;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.instagram.model.InstagramPage;
import com.tlc.social.common.instagram.model.InstagramPageRecipient;
import com.tlc.social.common.model.Channel;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.RecipientProfile;
import com.tlc.social.server.instagram.internal.model.InstagramMessage;
import com.tlc.social.server.instagram.resource.MSOINSTAGRAMMESSAGE;
import com.tlc.social.server.instagram.resource.MSOINSTAGRAMPAGE;
import com.tlc.social.server.instagram.resource.MSOINSTAGRAMPAGERECIPIENT;
import com.tlc.social.server.manager.ChatManager;
import com.tlc.social.server.manager.SocialEntityManager;
import com.tlc.social.server.sql.resource.MSOMESSAGE;
import com.tlc.social.server.sql.resource.MSORECIPIENT;
import com.tlc.social.server.sql.resource.MSORECIPIENTPROFILE;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Pertains to instagram related data base functions.
 * </p>>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramPageManager extends SocialEntityManager {

    private final ChatManager chatManager;

    private InstagramPageManager() {
        chatManager = ChatManager.getInstance();
    }

    private static class InstanceHolder {
        private static final InstagramPageManager INSTANCE = new InstagramPageManager();
    }

    /**
     * <p>
     * Gets the instance of {@link InstagramPageManager}
     * </p>
     *
     * @return the instance of {@link InstagramPageManager}
     */
    public static InstagramPageManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Gets the page table
     * </p>
     *
     * @return the page table
     */
    public Table getPageTable() {
        return Table.get(MSOINSTAGRAMPAGE.TABLE);
    }

    /**
     * <p>
     * Gets the instagram page recipient table
     * </p>
     *
     * @return the page recipient table
     */
    public Table getPageRecipientTable() {
        return Table.get(MSOINSTAGRAMPAGERECIPIENT.TABLE);
    }

    /**
     * <p>
     * Gets the instagram message table
     * </p>
     *
     * @return the instagram message table
     */
    public Table getInstagramMessageTable() {
        return Table.get(MSOINSTAGRAMMESSAGE.TABLE);
    }

    /**
     * <p>
     * Stores the instagram pages into the database
     * </p>
     *
     * @param pageMap Represents the map of {@link InstagramPage} with page access token
     */
    public void storePages(final Map<InstagramPage, String> pageMap) {
        final Table pageTable = getPageTable();
        final Collection<InstagramPage> pages = pageMap.keySet();
        final Collection<Long> pageReferences = pages.stream().map(InstagramPage::getExternalPageId).toList();
        final DataContainer pageDataContainer = getPageDataContainer(pageReferences);
        final DataContainer targetContainer = DataContainer.create();

        for (final InstagramPage page : pages) {
            final Channel channel = page.getChannel();
            final Row pageRow = pageDataContainer.getRow(pageTable, new WhereClause(Criteria.eq(pageTable.getColumn(MSOINSTAGRAMPAGE.EXTERNAL_PAGE_ID), page.getExternalPageId())));

            if (Objects.nonNull(pageRow)) {
                final Long channelId = pageRow.get(MSOINSTAGRAMPAGE.CHANNEL_ID);

                channel.setId(channelId);
            }
            chatManager.loadChannelIntoContainer(channel, pageDataContainer, targetContainer);
            loadPageIntoContainer(page, pageMap.get(page), pageDataContainer, targetContainer);
        }
        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the instagram page information into the data container
     * </p>
     *
     * @param page            Represents the {@link InstagramPage}
     * @param pageAccessToken Represents the page access token
     * @param dataContainer   Represents the data container which contains page details
     * @param targetContainer Represents the target data container
     */
    private void loadPageIntoContainer(final InstagramPage page, final String pageAccessToken, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table pageTable = getPageTable();
        final String pageName = page.getName();
        final Row pageRow = dataContainer.getOrInitRow(pageTable, new WhereClause(Criteria.eq(pageTable.getColumn(MSOINSTAGRAMPAGE.EXTERNAL_PAGE_ID), page.getExternalPageId())));

        if (pageRow.isNewRow()) {
            pageRow.set(MSOINSTAGRAMPAGE.FB_PAGE_EXTERNAL_ID, page.getFbPageExternalId());
            pageRow.set(MSOINSTAGRAMPAGE.EXTERNAL_PAGE_ID, page.getExternalPageId());
            pageRow.set(MSOINSTAGRAMPAGE.CHANNEL_ID, page.getChannel().id());
        }
        page.setId(pageRow.getPKValue());
        pageRow.set(MSOINSTAGRAMPAGE.NAME, pageName);
        pageRow.set(MSOINSTAGRAMPAGE.ACCESS_TOKEN, pageAccessToken);

        targetContainer.updateRow(pageRow);
    }

    /**
     * <p>
     * Gets the {@link DataContainer} which contains the instagram page details.
     * </p>
     *
     * @param externalPageIds Represents the external page Ids.
     * @return the {@link DataContainer}.
     */
    private DataContainer getPageDataContainer(final Collection<Long> externalPageIds) {
        final Table pageTable = getPageTable();
        final Table channelTable = getChannelTable();
        final SelectQuery selectQuery = SelectQuery.get(pageTable);

        selectQuery.addJoinClause(new JoinClause(pageTable.getColumn(MSOINSTAGRAMPAGE.CHANNEL_ID), channelTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(pageTable.getColumn(MSOINSTAGRAMPAGE.EXTERNAL_PAGE_ID), externalPageIds)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Gets the optional of pages access token.
     * </p>
     *
     * @param pageId Represents the page id.
     * @return the optional of page access token.
     */
    public Optional<String> getPageAccessToken(final Long pageId) {
        final Table pageTable = getPageTable();
        final SelectQuery selectQuery = SelectQuery.get(pageTable);
        final Column accessTokenColumn = pageTable.getColumn(MSOINSTAGRAMPAGE.ACCESS_TOKEN);

        selectQuery.addSelectClause(accessTokenColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pageTable.getPKColumn(), pageId)));

        final Row pageRow = adminDataStore().get(selectQuery).getRow(pageTable);

        return Objects.isNull(pageRow) ? Optional.empty() : Optional.ofNullable(pageRow.get(accessTokenColumn));
    }

    /**
     * <p>
     * Gets the optional {@link InstagramPage} from the provided external page id.
     * </p>
     *
     * @param externalPageId Represents the external page id.
     * @return the optional {@link InstagramPage}.
     */
    public Optional<InstagramPage> getPage(final Long externalPageId) {
        final Table pageTable = getPageTable();
        final SelectQuery selectQuery = SelectQuery.get(pageTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pageTable.getColumn(MSOINSTAGRAMPAGE.EXTERNAL_PAGE_ID), externalPageId)));

        final Row pageRow = adminDataStore().get(selectQuery).getRow(pageTable);

        return Objects.nonNull(pageRow) ? Optional.of(convertRowToModel(pageRow)) : Optional.empty();
    }

    /**
     * <p>
     * Converts page row into {@link InstagramPage}
     * </p>
     *
     * @param pageRow Represents the row which contains page details
     * @return the {@link InstagramPage}
     */
    private InstagramPage convertRowToModel(final Row pageRow) {
        final InstagramPage instagramPage = new InstagramPage();
        final Channel channel = new Channel();

        channel.setId(pageRow.get(MSOINSTAGRAMPAGE.CHANNEL_ID));
        channel.setType(Channel.ChannelType.INSTAGRAM_PAGE);

        instagramPage.setId(pageRow.getPKValue());
        instagramPage.setChannel(channel);
        instagramPage.setName(pageRow.get(MSOINSTAGRAMPAGE.NAME));
        instagramPage.setExternalPageId(pageRow.get(MSOINSTAGRAMPAGE.EXTERNAL_PAGE_ID));
        instagramPage.setFbPageExternalId(pageRow.get(MSOINSTAGRAMPAGE.FB_PAGE_EXTERNAL_ID));

        return instagramPage;
    }

    /**
     * <p>
     * Loads the recipient into the container
     * </p>
     *
     * @param instagramPageRecipient     Represents the {@link InstagramPageRecipient}
     * @param dataContainer       Represents the data container which contains the recipient details
     * @param targetDataContainer Represents the target container
     */
    public void loadRecipientIntoContainer(final InstagramPageRecipient instagramPageRecipient, final DataContainer dataContainer, final DataContainer targetDataContainer) {
        final Table recipientTable = getRecipientTable();
        final Table instagramPageRecipientTable = getPageRecipientTable();
        final Long pageScopedId = instagramPageRecipient.getPageScopedId();
        final Row recipientRow = dataContainer.getOrInitRow(recipientTable, instagramPageRecipient.id());

        if (recipientRow.isNewRow()) {
            recipientRow.set(MSORECIPIENT.CHAT_ID, instagramPageRecipient.getChatId());
            recipientRow.set(MSORECIPIENT.PROFILE_ID, instagramPageRecipient.getRecipientProfile().id());
            instagramPageRecipient.setId(recipientRow.getPKValue());
        }
        final Row pageRecipientRow = dataContainer.getOrInitRow(instagramPageRecipientTable,
                new WhereClause(Criteria.eq(instagramPageRecipientTable.getColumn(MSOINSTAGRAMPAGERECIPIENT.PAGE_SCOPED_ID), pageScopedId)));

        if (pageRecipientRow.isNewRow()) {
            pageRecipientRow.set(MSOINSTAGRAMPAGERECIPIENT.PAGE_ID, instagramPageRecipient.getInstagramPage().id());
            pageRecipientRow.set(MSOINSTAGRAMPAGERECIPIENT.PAGE_SCOPED_ID, pageScopedId);
            pageRecipientRow.set(MSOINSTAGRAMPAGERECIPIENT.RECIPIENT_ID, instagramPageRecipient.id());

            instagramPageRecipient.setPageRecipientId(pageRecipientRow.getPKValue());
        }

        targetDataContainer.updateRow(recipientRow);
        targetDataContainer.updateRow(pageRecipientRow);
    }

    /**
     * <p>
     * Loads the recipient profile into the container
     * </p>
     *
     * @param recipientProfile    Represents the {@link RecipientProfile}
     * @param dataContainer       Represents the container which contains existing profile details
     * @param targetDataContainer Represents the target container
     */
    public void loadRecipientProfile(final RecipientProfile recipientProfile, final DataContainer dataContainer, final DataContainer targetDataContainer) {
        final Table recipientProfileTable = getRecipientProfileTable();
        final Row profileRow = dataContainer.getOrInitRow(recipientProfileTable, recipientProfile.id());
        final Attachment profilePicture = recipientProfile.getProfilePicture();

        if (profileRow.isNewRow()) {
            recipientProfile.setId(profileRow.getPKValue());
        }

        if (Objects.nonNull(profilePicture)) {
            profileRow.set(MSORECIPIENTPROFILE.PROFILE_PIC_ID, profilePicture.id());
        }
        profileRow.set(MSORECIPIENTPROFILE.NAME, recipientProfile.getName());

        targetDataContainer.updateRow(profileRow);
    }

    /**
     * <p>
     * Gets the chat data container from the provided page scoped id
     * </p>
     *
     * @param pageScopedId Represents the page scoped id
     * @return the {@link DataContainer}
     */
    public DataContainer getChatDataContainer(final Long pageScopedId) {
        final Table recipientTable = getRecipientTable();
        final Table recipientProfileTable = getRecipientProfileTable();
        final Table pageRecipientTable = getPageRecipientTable();
        final Table chatTable = getChatTable();
        final SelectQuery selectQuery = SelectQuery.get(pageRecipientTable);

        selectQuery.addJoinClause(List.of(new JoinClause(pageRecipientTable.getColumn(MSOINSTAGRAMPAGERECIPIENT.RECIPIENT_ID), recipientTable.getPKColumn()),
                new JoinClause(recipientTable.getColumn(MSORECIPIENT.PROFILE_ID), recipientProfileTable.getPKColumn()),
                new JoinClause(recipientTable.getColumn(MSORECIPIENT.CHAT_ID), chatTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pageRecipientTable.getColumn(MSOINSTAGRAMPAGERECIPIENT.PAGE_SCOPED_ID), pageScopedId)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Loads the {@link InstagramMessage} into the database
     * </p>
     *
     * @param instagramMessage Represents the message to be stored
     * @param dataContainer    Represents the data container which contains chat and message info
     * @param targetContainer  Represents the target data container
     */
    public void loadInstagramMessage(final InstagramMessage instagramMessage, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table instagramMessageTable = getInstagramMessageTable();
        final Row messageRow = dataContainer.getOrInitRow(instagramMessageTable, new WhereClause(Criteria.eq(instagramMessageTable.getColumn(MSOINSTAGRAMMESSAGE.EXTERNAL_MESSAGE_ID),
                instagramMessage.getExternalMessageId())));

        if (messageRow.isNewRow()) {
            messageRow.set(MSOINSTAGRAMMESSAGE.EXTERNAL_MESSAGE_ID, instagramMessage.getExternalMessageId());
            messageRow.set(MSOINSTAGRAMMESSAGE.MESSAGE_ID, instagramMessage.id());

            targetContainer.updateRow(messageRow);
        }
    }

    /**
     * <p>
     * Gets the chat id from the provided page scoped id
     * </p>
     *
     * @param pageScopedId Represents the page scoped id
     * @return the optional of chat id
     */
    public Optional<Long> getChatId(final Long pageScopedId) {
        final Table pageRecipientTable = getPageRecipientTable();
        final Table recipientTable = getRecipientTable();
        final SelectQuery selectQuery = SelectQuery.get(pageRecipientTable);

        selectQuery.addJoinClause(new JoinClause(pageRecipientTable.getColumn(MSOINSTAGRAMPAGERECIPIENT.RECIPIENT_ID), recipientTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pageRecipientTable.getColumn(MSOINSTAGRAMPAGERECIPIENT.PAGE_SCOPED_ID), pageScopedId)));
        final Row recipientRow = adminDataStore().get(selectQuery).getRow(recipientTable);

        return Objects.isNull(recipientRow) ? Optional.empty() : Optional.ofNullable(recipientRow.get(recipientTable.getColumn(MSORECIPIENT.CHAT_ID)));
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
        final Table instagramMessageTable = getInstagramMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(instagramMessageTable);

        selectQuery.addSelectClause(instagramMessageTable.getColumn(MSOINSTAGRAMMESSAGE.MESSAGE_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(instagramMessageTable.getColumn(MSOINSTAGRAMMESSAGE.EXTERNAL_MESSAGE_ID), externalMessageIds)));

        return adminDataStore().get(selectQuery).getRows(instagramMessageTable).map(messageRow -> (Long) messageRow.get(MSOINSTAGRAMMESSAGE.MESSAGE_ID)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Fetches the unread outbound message ids from the provided chat id and the time stamp
     * </p>
     *
     * @param chatId Represents the chat id
     * @return the collection of message ids
     */
    public Collection<Long> fetchUnReadOutboundMessageIds(final Long chatId, final Long timestamp) {
        final Table messageTable = getMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(messageTable);

        selectQuery.addSelectClause(messageTable.getPKColumn());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(messageTable.getColumn(MSOMESSAGE.CHAT_ID), chatId))
                .and(Criteria.notEq(messageTable.getColumn(MSOMESSAGE.STATE), Message.MessageState.READ.getId()))
                .and(Criteria.eq(messageTable.getColumn(MSOMESSAGE.MESSAGE_FLOW), Message.MessageFlow.OUTBOUND.getId()))
                .and(Criteria.le(messageTable.getColumn(MSOMESSAGE.SEND_TIME), timestamp)));

        return adminDataStore().get(selectQuery).getRows(messageTable).map(Row::getPKValue).toList();
    }

    /**
     * Checks whether the page exists or not
     *
     * @param pageIds Represents the page ids
     * @return true, if it exists, false otherwise
     */
    @Override
    public boolean exists(final Collection<Long> pageIds) {
        final Table pageTable = getPageTable();
        final CountQuery countQuery = CountQuery.get(pageTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(pageTable.getPKColumn(), pageIds)));

        return pageIds.size() == adminDataStore().get(countQuery);
    }

    /**
     * <p>
     * Gets the {@link InstagramPageRecipient} from the provided chat id
     * </p>
     *
     * @param chatId Represents the chat id
     * @return the {@link InstagramPageRecipient}
     */
    public InstagramPageRecipient getInstagramPageRecipient(final Long chatId) {
        final Table recipientTable = getRecipientTable();
        final Table pageTable = getPageTable();
        final Table pageRecipientTable = getPageRecipientTable();
        final SelectQuery selectQuery = SelectQuery.get(recipientTable);

        selectQuery.addJoinClause(List.of(new JoinClause(recipientTable.getPKColumn(), pageRecipientTable.getColumn(MSOINSTAGRAMPAGERECIPIENT.RECIPIENT_ID)),
                new JoinClause(pageRecipientTable.getColumn(MSOINSTAGRAMPAGERECIPIENT.PAGE_ID), pageTable.getPKColumn())));
        selectQuery.addSelectClause(pageTable, pageRecipientTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(recipientTable.getColumn(MSORECIPIENT.CHAT_ID), chatId)));
        final DataContainer dataContainer = adminDataStore().get(selectQuery);

        return convertRowToInstagramPageRecipient(dataContainer.getRow(pageRecipientTable), dataContainer);
    }

    /**
     * <p>
     * Converts the page recipient row into {@link InstagramPageRecipient}
     * </p>
     *
     * @param pageRecipientRow Represents the page recipient row
     * @param dataContainer    Represents the data container which contains recipient details
     * @return the {@link InstagramPageRecipient}
     */
    private InstagramPageRecipient convertRowToInstagramPageRecipient(final Row pageRecipientRow, final DataContainer dataContainer) {
        final Row pageRow = dataContainer.getRow(getPageTable(), (Long) pageRecipientRow.get(MSOINSTAGRAMPAGERECIPIENT.PAGE_ID));
        final InstagramPageRecipient instagramPageRecipient = new InstagramPageRecipient();
        final InstagramPage instagramPage = new InstagramPage();

        instagramPage.setFbPageExternalId(pageRow.get(MSOINSTAGRAMPAGE.FB_PAGE_EXTERNAL_ID));
        instagramPage.setExternalPageId(pageRow.get(MSOINSTAGRAMPAGE.EXTERNAL_PAGE_ID));
        instagramPage.setId(pageRow.getPKValue());
        instagramPage.setName(pageRow.get(MSOINSTAGRAMPAGE.NAME));

        instagramPageRecipient.setId(pageRecipientRow.get(MSOINSTAGRAMPAGERECIPIENT.RECIPIENT_ID));
        instagramPageRecipient.setPageScopedId(pageRecipientRow.get(MSOINSTAGRAMPAGERECIPIENT.PAGE_SCOPED_ID));
        instagramPageRecipient.setInstagramPage(instagramPage);

        return instagramPageRecipient;
    }

    /**
     * <p>
     * Gets the collection of {@link InstagramPage} from the provided page ids
     * </p>
     *
     * @param pageIds Represents the page ids
     * @return the collection of {@link InstagramPage}
     */
    public Collection<InstagramPage> get(final Collection<Long> pageIds) {
        final Table pageTable = getPageTable();
        final SelectQuery selectQuery = SelectQuery.get(pageTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(pageTable.getPKColumn(), pageIds)));

        return adminDataStore().get(selectQuery).getRows(pageTable).map(this::convertRowToModel).toList();
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
        final Table instagramMessageTable = getInstagramMessageTable();
        final CountQuery countQuery = CountQuery.get(instagramMessageTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(instagramMessageTable.getColumn(MSOINSTAGRAMMESSAGE.EXTERNAL_MESSAGE_ID), externalMessageId)));

        return adminDataStore().get(countQuery) > NumericalConstant.ZERO;
    }
}