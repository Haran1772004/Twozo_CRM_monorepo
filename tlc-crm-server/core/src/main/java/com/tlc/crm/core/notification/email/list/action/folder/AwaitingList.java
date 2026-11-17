package com.tlc.crm.core.notification.email.list.action.folder;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.list.api.filter.FilterOption;
import com.tlc.crm.core.notification.email.list.api.list.EmailListManager;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;

import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Handles the HTTP requests that are relevant to awaiting list action
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@WebAction(path = "/email/awaiting/list")
public class AwaitingList extends CrmMailListAction {


    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link EntityThread} to be fetched
     * @param limit       Represents the maximum number of {@link EntityThread} to be fetched
     * @return collection of {@link EntityThread}
     */
    @Override
    protected Collection<EntityThread> fetchThreads(final long orgId, final long userId, final EmailListFilter emailFilter,
                                                    final int start, final int limit) {
        return emailListManager.fetchAwaitingThreads(orgId, userId, emailFilter, start, limit);
    }

    /**
     * <p>
     * Constructs the emailFilter by applying the criteria of {@link JsonObject}.
     * </p>
     *
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param jsonObject  Represents the request {@link JsonObject} data
     */
    protected void constructFilterConfig(final EmailListFilter emailFilter, final JsonObject jsonObject) {
        final Map<EmailListFilter.FilterAttribute, FilterOption> filterAttributes = new HashMap<>();

        loadSyncEmailFilterAttribute(filterAttributes, jsonObject);

        emailFilter.setFolder(Folder.AWAITING);
        emailFilter.setFilterAttribute(filterAttributes);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Folder}
     */
    @Override
    public Folder getFolder() {
        return Folder.AWAITING;
    }
}