package com.tlc.crm.messenger.api.channel.manager;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.messenger.api.channel.model.CrmSocialBusinessChannel;
import com.tlc.crm.messenger.api.channel.model.ChannelState;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.messenger.config.manager.AbstractMessengerManager;
import com.tlc.crm.messenger.sql.resource.OMSOFBPAGE;
import com.tlc.crm.messenger.sql.resource.OMSOFBPAGECHAT;

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
import com.tlc.sql.common.api.dml.UpdateQuery;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Manages data base actions related to channels.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class AbstractPageManager<T extends CrmSocialBusinessChannel> extends AbstractMessengerManager {

    /**
     * <p>
     * Gets page table.
     * </p>
     *
     * @return FB page table.
     */
    protected abstract Table getPageTable();

    /**
     * <p>
     * Gets page chat table.
     * </p>
     *
     * @return page chat table.
     */
    protected abstract Table getPageChatTable();

    /**
     * <p>
     * Converts row to {@link CrmSocialBusinessChannel}.
     * </p>
     *
     * @param row {@link Row}.
     * @return {@link CrmSocialBusinessChannel}.
     */
    protected abstract T convertRowToPage(final Row row);

    /**
     * <p>
     * Loads page into container.
     * </p>
     *
     * @param crmPage         {@link CrmSocialBusinessChannel}.
     * @param dataContainer   {@link DataContainer}.
     * @param targetContainer {@link DataContainer}.
     */
    protected abstract void loadPageIntoContainer(final T crmPage, final DataContainer dataContainer,
                                                  final DataContainer targetContainer);

    /**
     * <p>
     * Collects social page ids.
     * </p>
     *
     * @param pages {@link CrmSocialBusinessChannel} in a collection
     * @return social page ids..
     */
    protected abstract Collection<Long> collectPageIds(final Collection<T> pages);

    /**
     * <p>
     * Loads pages into crm tables.
     * </p>
     *
     * @param orgId Organization's id.
     * @param pages Collection of {@link CrmSocialBusinessChannel}.
     */
    public void updatePages(final Long orgId, final Collection<T> pages) {
        final DataContainer dataContainer = getPageContainer(orgId);
        final DataContainer targetContainer = DataContainer.create();

        for (final T page : pages) {
            loadPageIntoContainer(page, dataContainer, targetContainer);
        }
        deleteUnmappedPages(pages, dataContainer, targetContainer);
        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Checks whether the page exists.
     * </p>
     *
     * @param orgId  Organization's id.
     * @param pageId Page's id.
     * @return whether the page exists.
     */
    public boolean isPageExists(final Long orgId, final Long pageId) {
        final Table pageTable = getPageTable();
        final CountQuery countQuery = CountQuery.get(pageTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(pageTable.getPKColumn(), pageId)));

        return getOrgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Gets {@link CrmSocialBusinessChannel}.
     * </p>
     *
     * @param orgId  Organization's id.
     * @param pageId Page's id.
     * @return {@link CrmSocialBusinessChannel}.
     */
    public T getPage(final Long orgId, final Long pageId) {
        final Table pageTable = getPageTable();
        final SelectQuery pageQuery = SelectQuery.get(pageTable);

        pageQuery.addSelectClause(pageTable);
        pageQuery.setWhereClause(new WhereClause(Criteria.eq(pageTable.getPKColumn(), pageId)).and(Criteria.in(pageTable.getColumn(
                OMSOFBPAGE.STATE), List.of(ChannelState.ENABLED.getId(), ChannelState.DISABLED.getId()))));
        final DataContainer pageContainer = getOrgDataStore(orgId).get(pageQuery);
        final Row row = pageContainer.getRow(pageTable);

        return Objects.isNull(row) ? null : convertRowToPage(row);
    }

    /**
     * <p>
     * Gets {@link CrmSocialBusinessChannel} count.
     * </p>
     *
     * @param orgId Organization's id.
     * @return gets {@link CrmSocialBusinessChannel} count.
     */
    public long getPageCount(final Long orgId) {
        final Table pageTable = getPageTable();
        final CountQuery pageQuery = CountQuery.get(pageTable);

        pageQuery.setWhereClause(new WhereClause(Criteria.in(pageTable.getColumn(OMSOFBPAGE.STATE), List.of(
                ChannelState.ENABLED.getId(), ChannelState.DISABLED.getId()))));

        return getOrgDataStore(orgId).get(pageQuery);
    }

    /**
     * <p>
     * Checks whether fb pages are synced with the org or not.
     * </p>
     *
     * @param orgId Represents the org id.
     * @return whether fb pages are synced.
     */
    public boolean arePagesSynced(final Long orgId) {
        final Table pageTable = getPageTable();
        final CountQuery countQuery = CountQuery.get(pageTable);

        countQuery.setWhereClause(new WhereClause(Criteria.notEq(pageTable.getColumn(OMSOFBPAGE.STATE), ChannelState.DELETED.getId())));

        return getOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the optional of fb page id from the provided crm page id
     * </p></p>
     *
     * @param orgId     Represents the org id
     * @param crmPageId Represents the crm page id
     * @return the page id
     */
    public Optional<Long> getPageId(final Long orgId, final Long crmPageId) {
        final Table pageTable = getPageTable();
        final Column pageIdColumn = pageTable.getColumn(OMSOFBPAGE.PAGE_ID);
        final SelectQuery selectQuery = SelectQuery.get(pageTable);

        selectQuery.addSelectClause(pageIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pageTable.getPKColumn(), crmPageId)));
        final Row pageRow = getOrgDataStore(orgId).get(selectQuery).getRow(pageTable);

        return Objects.nonNull(pageRow) ? Optional.ofNullable(pageRow.get(pageIdColumn)) : Optional.empty();
    }

    /**
     * <p>
     * Gets Collection of {@link CrmSocialBusinessChannel}.
     * </p>
     *
     * @param orgId Organization's id.
     * @param start Start point to fetch.
     * @param count Total count to fetch.
     * @return Collection of {@link CrmSocialBusinessChannel}.
     */
    public Collection<T> getPages(final Long orgId, final int start, final int count) {
        final Table pageTable = getPageTable();
        final SelectQuery pageQuery = SelectQuery.get(pageTable);

        pageQuery.setWhereClause(new WhereClause(Criteria.in(pageTable.getColumn(OMSOFBPAGE.STATE), List.of(
                ChannelState.ENABLED.getId(), ChannelState.DISABLED.getId()))));
        pageQuery.setLimitClause(new LimitClause(start, count));
        pageQuery.addOrderByClause(new OrderByClause(pageTable.getPKColumn()));
        final Stream<Row> rows = getOrgDataStore(orgId).get(pageQuery).getRows(pageTable);

        return rows.map(this::convertRowToPage).toList();
    }

    /**
     * <p>
     * Updates the page state.
     * </p>
     *
     * @param orgId  Organization's id.
     * @param pageId Page's id.
     */
    public void updateState(final Long orgId, final Long pageId, final ChannelState state) {
        final Table pageTable = getPageTable();
        final UpdateQuery updateQuery = UpdateQuery.get(pageTable);

        updateQuery.addUpdateColumn(pageTable.getColumn(OMSOFBPAGE.STATE), state.getId());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(pageTable.getPKColumn(), pageId)));
        getOrgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Gets the page container.
     * </p>
     *
     * @param orgId Organization's id.
     * @return {@link DataContainer}.
     */
    private DataContainer getPageContainer(final Long orgId) {
        final Table pageTable = getPageTable();
        final SelectQuery selectQuery = SelectQuery.get(pageTable);

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Deletes the unmapped pages.
     * </p>
     *
     * @param pages           Collection of {@link CrmSocialBusinessChannel}.
     * @param dataContainer   {@link DataContainer}.
     * @param targetContainer {@link DataContainer}.
     */
    private void deleteUnmappedPages(final Collection<T> pages,
                                     final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table pageTable = getPageTable();
        final Collection<Long> pageIds = collectPageIds(pages);
        final Stream<Row> unmappedPageRows = dataContainer.getRows(pageTable, new WhereClause(Criteria.notIn(pageTable.getColumn(OMSOFBPAGE.PAGE_ID), pageIds)));
        final short deletedStateId = ChannelState.DELETED.getId();

        unmappedPageRows.forEach(row -> {
            final short stateId = row.get(OMSOFBPAGE.STATE);

            if (deletedStateId != stateId) {
                row.set(OMSOFBPAGE.STATE, deletedStateId);

                targetContainer.updateRow(row);
            }
        });
    }

    /**
     * <p>
     * Gets the associated org ids from the provided page id
     * </p>
     *
     * @param pageId Represents the page id
     * @return the collection of org id
     */
    public Collection<Long> getAssociatedOrgIds(final Long pageId) {
        final Table pageTable = getPageTable();
        final SelectQuery selectQuery = SelectQuery.get(pageTable);

        selectQuery.addSelectClause(pageTable.getColumn(OMSOFBPAGE.ORG_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pageTable.getColumn(OMSOFBPAGE.PAGE_ID), pageId)).and(Criteria.eq(pageTable.getColumn(OMSOFBPAGE.STATE), ChannelState.ENABLED.getId())));

        return adminDataStore().get(selectQuery).getRows(pageTable).map(Row::getOrgId).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Links the chats to the fb page
     * </p>
     *
     * @param orgIds          Represents the collection of org id
     * @param chats           Represents the collection of chats
     * @param pageId          Represents the page id
     * @param targetContainer Represents the target container
     */
    public void linkChatToPage(final Collection<Long> orgIds, final Collection<CrmChat> chats, final Long pageId, final DataContainer targetContainer) {
        final Table pageChatTable = getPageChatTable();
        final Table pageTable = getPageTable();
        final DataContainer pageDataContainer = getPageDataContainer(orgIds, pageId);

        for (final CrmChat chat : chats) {
            final Long orgId = chat.orgId();
            final Long crmChatId = chat.getCrmChatId();
            final Row existingRow = pageDataContainer.getRow(pageChatTable, new WhereClause(Criteria.eq(pageChatTable.getColumn(OMSOFBPAGECHAT.CHAT_ID), crmChatId))
                    .and(Criteria.eq(pageChatTable.getColumn(OMSOFBPAGECHAT.ORG_ID), orgId)));

            if (Objects.isNull(existingRow)) {
                final Row pageRow = pageDataContainer.getRow(pageTable, new WhereClause(Criteria.eq(pageTable.getColumn(OMSOFBPAGE.ORG_ID), orgId)));
                final Row pageChatRow = new Row(pageChatTable);

                pageChatRow.setOrgId(orgId);
                pageChatRow.set(OMSOFBPAGECHAT.CHAT_ID, crmChatId);
                pageChatRow.set(OMSOFBPAGECHAT.PAGE_ID, pageRow.getPKValue());

                targetContainer.addNewRow(pageChatRow);
            }
        }
        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Gets the page state from the provided chat id
     * </p>
     *
     * @param orgId  Represents the org id
     * @param chatId Represents the chat id
     * @return the page state
     */
    public ChannelState getPageStatus(final Long orgId, final Long chatId) {
        final Table pageTable = getPageTable();
        final Table pageChatTable = getPageChatTable();
        final SelectQuery selectQuery = SelectQuery.get(pageTable);

        selectQuery.addSelectClause(pageTable.getColumn(OMSOFBPAGE.STATE));
        selectQuery.addJoinClause(new JoinClause(pageTable.getPKColumn(), pageChatTable.getColumn(OMSOFBPAGECHAT.PAGE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pageChatTable.getColumn(OMSOFBPAGECHAT.CHAT_ID), chatId)));
        final Row row = getOrgDataStore(orgId).get(selectQuery).getRow(pageTable);

        return Objects.isNull(row) ? null : ChannelState.getStateById(row.get(OMSOFBPAGE.STATE));
    }

    /**
     * <p>
     * Checks whether the page is synced with other org or not
     * </p>
     *
     * @param orgId  Represents the org id
     * @param pageId Represents the page id
     * @return true if the page is associated to other org, false otherwise
     */
    public boolean isPageSyncedWithOtherOrg(final Long orgId, final Long pageId) {
        final Table pageTable = getPageTable();
        final CountQuery countQuery = CountQuery.get(pageTable);
        final Column orgIdColumn = pageTable.getColumn(OMSOFBPAGE.ORG_ID);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(pageTable.getColumn(OMSOFBPAGE.PAGE_ID), pageId))
                .and(Criteria.notEq(pageTable.getColumn(OMSOFBPAGE.STATE), ChannelState.DELETED.getId()))
                .and(Criteria.notEq(orgIdColumn, orgId)));

        return adminDataStore().get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Gets the page data container from the provided page ids
     * </p>
     *
     * @param orgIds Represents the collection of org ids
     * @param pageId Represents the page id
     * @return the page data container
     */
    private DataContainer getPageDataContainer(final Collection<Long> orgIds, final Long pageId) {
        final Table pageTable = getPageTable();
        final Table pageChatTable = getPageChatTable();
        final SelectQuery selectQuery = SelectQuery.get(pageTable);

        selectQuery.addJoinClause(new JoinClause(pageTable.getPKColumn(), pageChatTable.getColumn(OMSOFBPAGECHAT.PAGE_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pageTable.getColumn(OMSOFBPAGE.PAGE_ID), pageId))
                .and(Criteria.in(pageTable.getColumn(OMSOFBPAGE.ORG_ID), orgIds)));

        return adminDataStore().get(selectQuery);
    }
}