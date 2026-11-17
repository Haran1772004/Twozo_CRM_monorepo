package com.tlc.crm.core.notification.email.list.action.folder;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox;
import com.tlc.crm.core.notification.email.inbox.fields.TeamInboxFields;
import com.tlc.crm.core.notification.email.list.api.filter.FilterOption;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter;
import com.tlc.crm.core.notification.email.list.api.filter.model.TeamInboxFilterOption;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * <p>
 * Handles the HTTP requests that are relevant to team inbox list action
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@WebAction(path = "/email/team/inbox/list")
public class TeamInboxList extends CrmMailListAction {

    /**
     * <p>
     * Fetches a collection of threads for a given organization and user, starting from a specified index and limiting the number of threads returned.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents an user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link EntityThread} to be fetched
     * @param limit       Represents the maximum number of {@link EntityThread} to be fetched
     * @return the collection of {@link EntityThread}
     */
    @Override
    protected Collection<EntityThread> fetchThreads(long orgId, long userId, EmailListFilter emailFilter, int start, int limit) {
        return emailListManager.fetchTeamInboxThreads(orgId, userId, emailFilter, start, limit);
    }

    /**
     * <p>
     * Gets the folder of mail.
     * </p>
     *
     * @return {@link EmailListFilter.Folder}
     */
    @Override
    protected EmailListFilter.Folder getFolder() {
        return EmailListFilter.Folder.TEAM_INBOX;
    }

    /**
     * <p>
     * Constructs the emailFilter by applying the criteria of {@link JsonObject}.
     * </p>
     *
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param jsonObject  Represents the request {@link JsonObject} data
     */
    @Override
    protected void constructFilterConfig(final EmailListFilter emailFilter, final JsonObject jsonObject) {
        final Map<EmailListFilter.FilterAttribute, FilterOption> filterAttributes = new HashMap<>();
        final String teamInboxesKey = TeamInboxFields.TEAM_INBOXES.getName();

        if (jsonObject.containsKey(teamInboxesKey)) {
            final JsonArray jsonArray = jsonObject.getJsonArray(teamInboxesKey);
            final Collection<TeamInbox> teamInboxes = new HashSet<>();

            jsonArray.forEach(value -> {
                final TeamInbox teamInbox = new TeamInbox();

                teamInbox.setId((Long) value);

                teamInboxes.add(teamInbox);
            });

            if (!teamInboxes.isEmpty()) {
                filterAttributes.put(EmailListFilter.FilterAttribute.TEAM_INBOX, new TeamInboxFilterOption(teamInboxes));
            }
        }
        emailFilter.setFilterAttribute(filterAttributes);
    }
}