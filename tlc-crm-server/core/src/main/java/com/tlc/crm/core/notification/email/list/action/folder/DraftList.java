package com.tlc.crm.core.notification.email.list.action.folder;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.list.api.filter.FilterOption;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.FilterAttribute;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.LinkedCategory;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;

import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Handles the HTTP requests that are relevant to draft list action
 *
 * @author KarthickV
 * @version 1.0
 */
@WebAction(path = "/email/draft/list")
public class DraftList extends CrmMailListAction {

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
        return emailListManager.fetchDraftThreads(orgId, userId, emailFilter, start, limit);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param jsonObject  Represents the request {@link JsonObject}
     */
    @Override
    public void constructFilterConfig(final EmailListFilter emailFilter, final JsonObject jsonObject) {
        final Map<FilterAttribute, FilterOption> filterAttributes = new HashMap<>();
        final LinkedCategory linkedCategory = emailFilter.getLinkedCategory();

        if (Objects.nonNull(linkedCategory)) {
            filterAttributes.put(FilterAttribute.get(CoreEmailFields.LINKED_CATEGORY.getName()), linkedCategory);
        }
        loadSyncEmailFilterAttribute(filterAttributes, jsonObject);

        emailFilter.setFolder(EmailListFilter.Folder.DRAFT);
        emailFilter.setFilterAttribute(filterAttributes);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Folder}
     */
    @Override
    public Folder getFolder() {
        return Folder.DRAFT;
    }
}
