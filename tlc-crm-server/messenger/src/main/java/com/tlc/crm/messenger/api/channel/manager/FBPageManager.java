package com.tlc.crm.messenger.api.channel.manager;

import com.tlc.crm.messenger.api.channel.model.CrmFBPage;
import com.tlc.crm.messenger.api.channel.model.ChannelState;
import com.tlc.crm.messenger.sql.resource.OMSOFBPAGE;
import com.tlc.crm.messenger.sql.resource.OMSOFBPAGECHAT;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;

/**
 * <p>
 * Manages data base actions related to Facebook.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class FBPageManager extends AbstractPageManager<CrmFBPage> {

    private FBPageManager() {
    }

    private static final class Instance {
        private static final FBPageManager CRM_FB_CHANNEL_MANAGER = new FBPageManager();
    }

    /**
     * <p>
     * Gets the instance.
     * </p>
     *
     * @return {@link FBPageManager}.
     */
    public static FBPageManager getInstance() {
        return Instance.CRM_FB_CHANNEL_MANAGER;
    }

    /**
     * <p>
     * Gets the FB page table.
     * </p>
     *
     * @return FB page table.
     */
    public Table getPageTable() {
        return Table.get(OMSOFBPAGE.TABLE);
    }

    /**
     * <p>
     * Gets the FB page chat table.
     * </p>
     *
     * @return FB page chat table.
     */
    protected Table getPageChatTable() {
        return Table.get(OMSOFBPAGECHAT.TABLE);
    }

    /**
     * <p>
     * Converts row to {@link CrmFBPage}.
     * </p>
     *
     * @param row {@link Row}.
     * @return {@link CrmFBPage}.
     */
    protected CrmFBPage convertRowToPage(final Row row) {
        final CrmFBPage crmFbPage = new CrmFBPage();

        crmFbPage.setOrgId(row.getOrgId());
        crmFbPage.setCrmPageId(row.getPKValue());
        crmFbPage.setState(ChannelState.getStateById(row.get(OMSOFBPAGE.STATE)));
        crmFbPage.setId(row.get(OMSOFBPAGE.PAGE_ID));

        return crmFbPage;
    }

    /**
     * <p>
     * Loads page into container.
     * </p>
     *
     * @param crmFBPage       {@link CrmFBPage}.
     * @param dataContainer   {@link DataContainer}.
     * @param targetContainer {@link DataContainer}.
     */
    protected void loadPageIntoContainer(final CrmFBPage crmFBPage, final DataContainer dataContainer,
                                         final DataContainer targetContainer) {
        final Table pageTable = getPageTable();
        final Long pageId = crmFBPage.id();
        final Row pageRow = dataContainer.getOrInitRow(pageTable, new WhereClause(Criteria.eq(
                pageTable.getColumn(OMSOFBPAGE.PAGE_ID), pageId)));

        if (pageRow.isNewRow()) {
            pageRow.setOrgId(crmFBPage.getOrgId());
            pageRow.set(OMSOFBPAGE.PAGE_ID, pageId);
            crmFBPage.setCrmPageId(pageRow.getPKValue());
        }
        pageRow.set(OMSOFBPAGE.STATE, crmFBPage.getState().getId());

        targetContainer.updateRow(pageRow);
    }

    /**
     * <p>
     * Collects social page ids.
     * </p>
     *
     * @param pages {@link CrmFBPage} in a collection
     * @return social page ids.
     */
    @Override
    protected Collection<Long> collectPageIds(final Collection<CrmFBPage> pages) {
        return pages.stream().map(CrmFBPage::id).toList();
    }
}