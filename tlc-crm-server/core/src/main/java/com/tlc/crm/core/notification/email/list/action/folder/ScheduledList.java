package com.tlc.crm.core.notification.email.list.action.folder;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.list.api.filter.FilterOption;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.FilterAttribute;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;

import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Handles the HTTP requests that are relevant to scheduled email list action
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@WebAction(path = "/email/scheduled/list")
public class ScheduledList extends CrmMailListAction {

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
    protected Collection<EntityThread> fetchThreads(long orgId, long userId, EmailListFilter emailFilter, int start, int limit) {
        return emailListManager.fetchScheduledEmailThreads(orgId, userId, emailFilter, start, limit);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailFilter  Represents the {@link EmailListFilter}
     * @param criteriaJson Represents the {@link JsonObject}
     */
    @Override
    protected void constructFilterConfig(final EmailListFilter emailFilter, final JsonObject criteriaJson) {
        final Map<FilterAttribute, FilterOption> filterAttributes = new HashMap<>();
        final EmailListFilter.LinkedCategory linkedCategory = emailFilter.getLinkedCategory();

        if (Objects.nonNull(linkedCategory)) {
            filterAttributes.put(FilterAttribute.get(CoreEmailFields.LINKED_CATEGORY.getName()), linkedCategory);
        }
        loadSyncEmailFilterAttribute(filterAttributes, criteriaJson);

        emailFilter.setFolder(Folder.SCHEDULED);
        emailFilter.setFilterAttribute(filterAttributes);
    }

    @Override
    public Folder getFolder() {
        return Folder.SCHEDULED;
    }
}
