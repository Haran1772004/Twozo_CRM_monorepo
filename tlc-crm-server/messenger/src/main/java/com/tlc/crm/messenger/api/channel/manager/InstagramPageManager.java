package com.tlc.crm.messenger.api.channel.manager;

import com.tlc.crm.messenger.api.channel.model.CrmInstagramPage;
import com.tlc.crm.messenger.api.channel.model.ChannelState;
import com.tlc.crm.messenger.sql.resource.OMSOINSTAGRAMPAGE;
import com.tlc.crm.messenger.sql.resource.OMSOINSTAGRAMPAGECHAT;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;

/**
 * <p>
 * Manages data base actions related to Instagram.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramPageManager extends AbstractPageManager<CrmInstagramPage> {

    private InstagramPageManager() {
    }

    private static final class Instance {
        private static final InstagramPageManager INSTAGRAM_CHANNEL_MANAGER = new InstagramPageManager();
    }

    /**
     * <p>
     * Gets the instance.
     * </p>
     *
     * @return {@link InstagramPageManager}.
     */
    public static InstagramPageManager getInstance() {
        return Instance.INSTAGRAM_CHANNEL_MANAGER;
    }

    /**
     * <p>
     * Gets the FB page table.
     * </p>
     *
     * @return FB page table.
     */
    public Table getPageTable() {
        return Table.get(OMSOINSTAGRAMPAGE.TABLE);
    }

    /**
     * <p>
     * Gets the FB page chat table.
     * </p>
     *
     * @return FB page chat table.
     */
    protected Table getPageChatTable() {
        return Table.get(OMSOINSTAGRAMPAGECHAT.TABLE);
    }

    /**
     * <p>
     * Converts row to {@link CrmInstagramPage}.
     * </p>
     *
     * @param row {@link Row}.
     * @return {@link CrmInstagramPage}.
     */
    protected CrmInstagramPage convertRowToPage(final Row row) {
        final CrmInstagramPage crmInstagramPage = new CrmInstagramPage();

        crmInstagramPage.setOrgId(row.getOrgId());
        crmInstagramPage.setCrmPageId(row.getPKValue());
        crmInstagramPage.setState(ChannelState.getStateById(row.get(OMSOINSTAGRAMPAGE.STATE)));
        crmInstagramPage.setId(row.get(OMSOINSTAGRAMPAGE.PAGE_ID));

        return crmInstagramPage;
    }

    /**
     * <p>
     * Loads page into container.
     * </p>
     *
     * @param crmPage         {@link CrmInstagramPage}.
     * @param dataContainer   {@link DataContainer}.
     * @param targetContainer {@link DataContainer}.
     */
    protected void loadPageIntoContainer(final CrmInstagramPage crmPage, final DataContainer dataContainer,
                                         final DataContainer targetContainer) {
        final Table pageTable = getPageTable();
        final Long pageId = crmPage.id();
        final Row pageRow = dataContainer.getOrInitRow(pageTable, new WhereClause(Criteria.eq(
                pageTable.getColumn(OMSOINSTAGRAMPAGE.PAGE_ID), pageId)));

        if (pageRow.isNewRow()) {
            pageRow.setOrgId(crmPage.getOrgId());
            pageRow.set(OMSOINSTAGRAMPAGE.PAGE_ID, pageId);
            crmPage.setCrmPageId(pageRow.getPKValue());
        }
        pageRow.set(OMSOINSTAGRAMPAGE.STATE, crmPage.getState().getId());

        targetContainer.updateRow(pageRow);
    }

    /**
     * <p>
     * Collects social page ids.
     * </p>
     *
     * @param pages {@link CrmInstagramPage} in a collection
     * @return social page ids.
     */
    @Override
    protected Collection<Long> collectPageIds(final Collection<CrmInstagramPage> pages) {
        return pages.stream().map(CrmInstagramPage::id).toList();
    }
}