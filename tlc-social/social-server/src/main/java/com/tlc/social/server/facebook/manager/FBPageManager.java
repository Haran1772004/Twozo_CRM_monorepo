package com.tlc.social.server.facebook.manager;

import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.social.common.facebook.model.FBPageRecipient;
import com.tlc.social.server.facebook.internal.model.FBMessage;
import com.tlc.social.common.facebook.model.FBPage;
import com.tlc.social.common.model.Channel;
import com.tlc.social.common.model.Message;

import com.tlc.social.server.facebook.sql.resource.MSOFBMESSAGE;
import com.tlc.social.server.facebook.sql.resource.MSOFBPAGERECIPIENT;
import com.tlc.social.server.manager.ChatManager;
import com.tlc.social.server.manager.SocialEntityManager;
import com.tlc.social.server.facebook.sql.resource.MSOFBPAGE;
import com.tlc.social.server.sql.resource.MSOMESSAGE;
import com.tlc.social.server.sql.resource.MSORECIPIENT;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages the facebook related database actions
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBPageManager extends SocialEntityManager {

    private final ChatManager chatManager;

    private FBPageManager() {
        chatManager = ChatManager.getInstance();
    }

    private static class InstanceHolder {
        private static final FBPageManager INSTANCE = new FBPageManager();
    }

    /**
     * <p>
     * Gets the instance of {@link FBPageManager}
     * </p>
     *
     * @return the instance of {@link FBPageManager}
     */
    public static FBPageManager getInstance() {
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
        return Table.get(MSOFBPAGE.TABLE);
    }

    /**
     * <p>
     * Gets the fb page recipient table
     * </p>
     *
     * @return the page recipient table
     */
    public Table getPageRecipientTable() {
        return Table.get(MSOFBPAGERECIPIENT.TABLE);
    }

    /**
     * <p>
     * Gets the fb message table
     * </p>
     *
     * @return the fb message table
     */
    public Table getFBMessageTable() {
        return Table.get(MSOFBMESSAGE.TABLE);
    }

    /**
     * <p>
     * Stores the facebook pages into the database
     * </p>
     *
     * @param pageMap Represents the map of {@link FBPage} with page access token
     */
    public void storePages(final Map<FBPage, String> pageMap) {
        final Table pageTable = getPageTable();
        final Collection<FBPage> pages = pageMap.keySet();
        final Collection<Long> pageReferences = pages.stream().map(FBPage::getExternalPageId).toList();
        final DataContainer pageDataContainer = getPageDataContainer(pageReferences);
        final DataContainer targetContainer = DataContainer.create();

        for (final FBPage page : pages) {
            final Channel channel = page.getChannel();
            final Row pageRow = pageDataContainer.getRow(pageTable, new WhereClause(Criteria.eq(pageTable.getColumn(MSOFBPAGE.EXTERNAL_PAGE_ID), page.getExternalPageId())));

            if (Objects.nonNull(pageRow)) {
                final Long channelId = pageRow.get(MSOFBPAGE.CHANNEL_ID);

                channel.setId(channelId);
            }
            chatManager.loadChannelIntoContainer(channel, pageDataContainer, targetContainer);
            loadPageIntoContainer(page, pageMap.get(page), pageDataContainer, targetContainer);
        }
        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the facebook page information into the data container
     * </p>
     *
     * @param page            Represents the {@link FBPage}
     * @param pageAccessToken Represents the page access token
     * @param dataContainer   Represents the data container which contains page details
     * @param targetContainer Represents the target data container
     */
    private void loadPageIntoContainer(final FBPage page, final String pageAccessToken, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table pageTable = getPageTable();
        final String pageName = page.getName();
        final Row pageRow = dataContainer.getOrInitRow(pageTable, new WhereClause(Criteria.eq(pageTable.getColumn(MSOFBPAGE.EXTERNAL_PAGE_ID), page.getExternalPageId())));

        if (pageRow.isNewRow()) {
            pageRow.set(MSOFBPAGE.EXTERNAL_PAGE_ID, page.getExternalPageId());
            pageRow.set(MSOFBPAGE.CHANNEL_ID, page.getChannel().id());
        }
        page.setId(pageRow.getPKValue());
        pageRow.set(MSOFBPAGE.NAME, pageName);
        pageRow.set(MSOFBPAGE.ACCESS_TOKEN, pageAccessToken);

        targetContainer.updateRow(pageRow);
    }

    /**
     * <p>
     * Gets the {@link DataContainer} which contains the facebook page details
     * </p>
     *
     * @param externalPageIds Represents the external page Ids
     * @return the {@link DataContainer}
     */
    private DataContainer getPageDataContainer(final Collection<Long> externalPageIds) {
        final Table pageTable = getPageTable();
        final Table channelTable = getChannelTable();
        final SelectQuery selectQuery = SelectQuery.get(pageTable);

        selectQuery.addJoinClause(new JoinClause(pageTable.getColumn(MSOFBPAGE.CHANNEL_ID), channelTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(pageTable.getColumn(MSOFBPAGE.EXTERNAL_PAGE_ID), externalPageIds)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Gets the optional of pages access token
     * </p>
     *
     * @param pageId Represents the page id
     * @return the optional of page access token
     */
    public Optional<String> getPageAccessToken(final Long pageId) {
        final Table pageTable = getPageTable();
        final SelectQuery selectQuery = SelectQuery.get(pageTable);
        final Column accessTokenColumn = pageTable.getColumn(MSOFBPAGE.ACCESS_TOKEN);

        selectQuery.addSelectClause(accessTokenColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pageTable.getPKColumn(), pageId)));

        final Row pageRow = adminDataStore().get(selectQuery).getRow(pageTable);

        return Objects.isNull(pageRow) ? Optional.empty() : Optional.ofNullable(pageRow.get(accessTokenColumn));
    }

    /**
     * <p>
     * Gets the optional {@link FBPage} from the provided external page id
     * </p>
     *
     * @param externalPageId Represents the external page id
     * @return the optional {@link FBPage}
     */
    public Optional<FBPage> getPage(final Long externalPageId) {
        final Table pageTable = getPageTable();
        final SelectQuery selectQuery = SelectQuery.get(pageTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pageTable.getColumn(MSOFBPAGE.EXTERNAL_PAGE_ID), externalPageId)));

        final Row pageRow = adminDataStore().get(selectQuery).getRow(pageTable);

        return Objects.nonNull(pageRow) ? Optional.of(convertRowToModel(pageRow)) : Optional.empty();
    }

    /**
     * <p>
     * Converts page row into {@link FBPage}
     * </p>
     *
     * @param pageRow Represents the row which contains page details
     * @return the {@link FBPage}
     */
    private FBPage convertRowToModel(final Row pageRow) {
        final FBPage fbPage = new FBPage();
        final Channel channel = new Channel();

        channel.setId(pageRow.get(MSOFBPAGE.CHANNEL_ID));
        channel.setType(Channel.ChannelType.FB_PAGE);

        fbPage.setId(pageRow.getPKValue());
        fbPage.setChannel(channel);
        fbPage.setName(pageRow.get(MSOFBPAGE.NAME));
        fbPage.setExternalPageId(pageRow.get(MSOFBPAGE.EXTERNAL_PAGE_ID));

        return fbPage;
    }

    /**
     * <p>
     * Loads the recipient into the container
     * </p>
     *
     * @param fbPageRecipient     Represents the {@link FBPageRecipient}
     * @param dataContainer       Represents the data container which contains the recipient details
     * @param targetDataContainer Represents the target container
     */
    public void loadRecipientIntoContainer(final FBPageRecipient fbPageRecipient, final DataContainer dataContainer, final DataContainer targetDataContainer) {
        final Table recipientTable = getRecipientTable();
        final Table fbPageRecipientTable = getPageRecipientTable();
        final Long pageScopedId = fbPageRecipient.getPageScopedId();
        final Row recipientRow = dataContainer.getOrInitRow(recipientTable, fbPageRecipient.id());

        if (recipientRow.isNewRow()) {
            recipientRow.set(MSORECIPIENT.CHAT_ID, fbPageRecipient.getChatId());
            recipientRow.set(MSORECIPIENT.PROFILE_ID, fbPageRecipient.getRecipientProfile().id());
            fbPageRecipient.setId(recipientRow.getPKValue());
        }
        final Row pageRecipientRow = dataContainer.getOrInitRow(fbPageRecipientTable,
                new WhereClause(Criteria.eq(fbPageRecipientTable.getColumn(MSOFBPAGERECIPIENT.PAGE_SCOPED_ID), pageScopedId)));

        if (pageRecipientRow.isNewRow()) {
            pageRecipientRow.set(MSOFBPAGERECIPIENT.PAGE_ID, fbPageRecipient.getFbPage().id());
            pageRecipientRow.set(MSOFBPAGERECIPIENT.PAGE_SCOPED_ID, pageScopedId);
            pageRecipientRow.set(MSOFBPAGERECIPIENT.RECIPIENT_ID, fbPageRecipient.id());

            fbPageRecipient.setPageRecipientId(pageRecipientRow.getPKValue());
        }

        targetDataContainer.updateRow(recipientRow);
        targetDataContainer.updateRow(pageRecipientRow);
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
        final Table fbpageRecipientTable = getPageRecipientTable();
        final Table chatTable = getChatTable();
        final SelectQuery selectQuery = SelectQuery.get(fbpageRecipientTable);

        selectQuery.addJoinClause(List.of(new JoinClause(fbpageRecipientTable.getColumn(MSOFBPAGERECIPIENT.RECIPIENT_ID), recipientTable.getPKColumn()),
                new JoinClause(recipientTable.getColumn(MSORECIPIENT.PROFILE_ID), recipientProfileTable.getPKColumn()),
                new JoinClause(recipientTable.getColumn(MSORECIPIENT.CHAT_ID), chatTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(fbpageRecipientTable.getColumn(MSOFBPAGERECIPIENT.PAGE_SCOPED_ID), pageScopedId)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Loads the {@link FBMessage} into the database
     * </p>
     *
     * @param fbMessage       Represents the message to be stored
     * @param dataContainer   Represents the data container which contains chat and message info
     * @param targetContainer Represents the target data container
     */
    public void loadFBMessage(final FBMessage fbMessage, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table fbMessageTable = getFBMessageTable();
        final Row messageRow = dataContainer.getOrInitRow(fbMessageTable, new WhereClause(Criteria.eq(fbMessageTable.getColumn(MSOFBMESSAGE.EXTERNAL_MESSAGE_ID),
                fbMessage.getExternalMessageId())));

        if (messageRow.isNewRow()) {
            messageRow.set(MSOFBMESSAGE.EXTERNAL_MESSAGE_ID, fbMessage.getExternalMessageId());
            messageRow.set(MSOFBMESSAGE.MESSAGE_ID, fbMessage.id());

            targetContainer.updateRow(messageRow);
        }
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
        final Table fbMessageTable = getFBMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(fbMessageTable);

        selectQuery.addSelectClause(fbMessageTable.getColumn(MSOFBMESSAGE.MESSAGE_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(fbMessageTable.getColumn(MSOFBMESSAGE.EXTERNAL_MESSAGE_ID), externalMessageIds)));

        return adminDataStore().get(selectQuery).getRows(fbMessageTable).map(messageRow -> (Long) messageRow.get(MSOFBMESSAGE.MESSAGE_ID)).toList();
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

        selectQuery.addJoinClause(new JoinClause(pageRecipientTable.getColumn(MSOFBPAGERECIPIENT.RECIPIENT_ID), recipientTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pageRecipientTable.getColumn(MSOFBPAGERECIPIENT.PAGE_SCOPED_ID), pageScopedId)));
        final Row recipientRow = adminDataStore().get(selectQuery).getRow(recipientTable);

        return Objects.isNull(recipientRow) ? Optional.empty() : Optional.ofNullable(recipientRow.get(recipientTable.getColumn(MSORECIPIENT.CHAT_ID)));
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
     * Gets the {@link FBPageRecipient} from the provided chat id
     * </p>
     *
     * @param chatId Represents the chat id
     * @return the {@link FBPageRecipient}
     */
    public FBPageRecipient getFBPageRecipient(final Long chatId) {
        final Table recipientTable = getRecipientTable();
        final Table pageTable = getPageTable();
        final Table pageRecipientTable = getPageRecipientTable();
        final SelectQuery selectQuery = SelectQuery.get(recipientTable);

        selectQuery.addJoinClause(List.of(new JoinClause(recipientTable.getPKColumn(), pageRecipientTable.getColumn(MSOFBPAGERECIPIENT.RECIPIENT_ID)),
                new JoinClause(pageRecipientTable.getColumn(MSOFBPAGERECIPIENT.PAGE_ID), pageTable.getPKColumn())));

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(recipientTable.getColumn(MSORECIPIENT.CHAT_ID), chatId)));
        final DataContainer dataContainer = adminDataStore().get(selectQuery);

        return convertRowToFBPageRecipient(dataContainer.getRow(pageRecipientTable), dataContainer);
    }

    /**
     * <p>
     * Converts the page recipient row into {@link FBPageRecipient}
     * </p>
     *
     * @param pageRecipientRow Represents the page recipient row
     * @param dataContainer    Represents the data container which contains recipient details
     * @return the {@link FBPageRecipient}
     */
    private FBPageRecipient convertRowToFBPageRecipient(final Row pageRecipientRow, final DataContainer dataContainer) {
        final Row pageRow = dataContainer.getRow(getPageTable(), (Long) pageRecipientRow.get(MSOFBPAGERECIPIENT.PAGE_ID));
        final FBPageRecipient fbPageRecipient = new FBPageRecipient();
        final FBPage fbPage = new FBPage();

        fbPage.setExternalPageId(pageRow.get(MSOFBPAGE.EXTERNAL_PAGE_ID));
        fbPage.setId(pageRow.getPKValue());
        fbPage.setName(pageRow.get(MSOFBPAGE.NAME));

        fbPageRecipient.setId(pageRecipientRow.get(MSOFBPAGERECIPIENT.RECIPIENT_ID));
        fbPageRecipient.setPageScopedId(pageRecipientRow.get(MSOFBPAGERECIPIENT.PAGE_SCOPED_ID));
        fbPageRecipient.setFbPage(fbPage);

        return fbPageRecipient;
    }

    /**
     * <p>
     * Gets the collection of {@link FBPage} from the provided page ids
     * </p>
     *
     * @param pageIds Represents the page ids
     * @return the collection of {@link FBPage}
     */
    public Collection<FBPage> get(final Collection<Long> pageIds) {
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
        final Table fbMessageTable = getFBMessageTable();
        final CountQuery countQuery = CountQuery.get(fbMessageTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(fbMessageTable.getColumn(MSOFBMESSAGE.EXTERNAL_MESSAGE_ID), externalMessageId)));

        return adminDataStore().get(countQuery) > NumericalConstant.ZERO;
    }
}