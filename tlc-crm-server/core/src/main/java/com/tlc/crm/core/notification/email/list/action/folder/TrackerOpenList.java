package com.tlc.crm.core.notification.email.list.action.folder;

import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;

import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;

import com.tlc.web.WebAction;

import java.util.Collection;

/**
 * <p>
 * Handles the HTTP requests that are relevant to opened tracker list action
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@WebAction(path = "/email/tracker/open/list")
public class TrackerOpenList extends CrmMailListAction {

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @return The count of opened tracker mails
     */
    @Override
    protected Long getTotalRecords(final long orgId, final long userId, final EmailListFilter emailFilter) {
        return emailListManager.getTrackingThreadCount(orgId, userId, EmailTrackingDetail.Type.OPENED);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link Thread} to be fetched
     * @param limit       Represents the maximum number of {@link Thread} to be fetched
     * @return The collection of thread for opened tracking list
     */
    @Override
    protected Collection<EntityThread> fetchThreads(final long orgId, final long userId, final EmailListFilter emailFilter,
                                                    final int start, final int limit) {
        return emailListManager.fetchTrackingThreads(orgId, userId, start, limit, EmailTrackingDetail.Type.OPENED);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Folder}
     */
    @Override
    protected Folder getFolder() {
        return Folder.OPENED;
    }
}
