package com.tlc.crm.core.notification.email.list.action.folder;

import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;

import com.tlc.web.WebAction;

import java.util.Collection;

/**
 * Handles the HTTP requests that are relevant to trash list action
 *
 * @author KarthickV
 * @version 1.0
 */
@WebAction(path = "/email/trash/list")
public class TrashList extends CrmMailListAction {

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link Thread} to be fetched
     * @param limit       Represents the maximum number of {@link Thread} to be fetched
     * @return the collection of {@link Thread}
     */
    @Override
    protected Collection<EntityThread> fetchThreads(final long orgId, final long userId, final EmailListFilter emailFilter, final int start, final int limit) {
        return emailListManager.fetchTrashThreads(orgId, userId, emailFilter, start, limit);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Folder}
     */
    @Override
    public Folder getFolder() {
        return Folder.TRASH;
    }
}
