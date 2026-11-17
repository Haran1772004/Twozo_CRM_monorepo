package com.tlc.crm.core.notification.email.list.api.filter.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.notification.email.list.api.filter.FilterOption;
import com.tlc.i18n.I18nKey;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Encapsulates various attributes and conditions that can be applied to filter emails.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class EmailListFilter implements TlcModel {

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notifications.email.filter.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notifications.email.filter.invalid.user.id")
    private Long userId;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notifications.email.filter.invalid.id")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notifications.email.filter.invalid.folder")
    private Folder folder;

    private boolean isConfigured;
    private LinkedCategory linkedCategory;
    private FilterInterval filterInterval;

    private Map<FilterAttribute, FilterOption> filterAttributes;

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(final Folder folder) {
        this.folder = folder;
    }

    public FilterInterval getFilterInterval() {
        return filterInterval;
    }

    public void setFilterInterval(final FilterInterval filterInterval) {
        this.filterInterval = filterInterval;
    }

    public boolean isConfigured() {
        return isConfigured;
    }

    public void setConfigured(final boolean configured) {
        this.isConfigured = configured;
    }

    public Map<FilterAttribute, FilterOption> getFilterAttribute() {
        return filterAttributes;
    }

    public void setFilterAttribute(final Map<FilterAttribute, FilterOption> filterAttributes) {
        this.filterAttributes = filterAttributes;
    }

    public LinkedCategory getLinkedCategory() {
        return linkedCategory;
    }

    public void setLinkedCategory(final LinkedCategory linkedCategory) {
        this.linkedCategory = linkedCategory;
    }

    public Collection<TeamInbox> getTeamInboxes() {
        if (this.filterAttributes.containsKey(EmailListFilter.FilterAttribute.TEAM_INBOX)) {
            return ((TeamInboxFilterOption) filterAttributes.get(EmailListFilter.FilterAttribute.TEAM_INBOX)).getTeamInboxes();
        }

        return Collections.emptySet();
    }

    public Collection<Long> getSyncEmails() {
        if (this.filterAttributes.containsKey(FilterAttribute.SYNC_EMAILS)) {
            return ((SyncEmailFilterOption) filterAttributes.get(FilterAttribute.SYNC_EMAILS)).getSyncedEmailIds();
        }

        return Collections.emptySet();
    }

    /**
     * <p>
     * Represents the {@link LinkedCategory} which is linked with deal and attachments
     * </p>
     *
     * @author KowsalyaSP
     */
    public enum LinkedCategory implements FilterOption {

        LINKED_WITH_A_DEAL(new I18nKey("i18n.notifications.email.filter.linked.with.deal"), 1),
        LINKED_WITH_AN_OPEN_DEAL(new I18nKey("i18n.notifications.email.filter.linked.with.open.deal"), 2),
        NOT_LINKED_WITH_A_DEAL(new I18nKey("i18n.notifications.email.filter.not.linked.with.deal"), 3),
        ATTACHMENTS(new I18nKey("i18n.notifications.email.filter.linked.with.attachments"), 4);

        private final I18nKey i18nKey;
        private final int id;

        LinkedCategory(final I18nKey i18nKey, final int id) {
            this.i18nKey = Objects.requireNonNull(i18nKey);
            this.id = id;
        }

        /**
         * <p>
         * Returns the {@link I18nKey} associated with the {@link LinkedCategory}.
         * </p>
         *
         * @return the name of {@link LinkedCategory}
         */
        public I18nKey getI18nKey() {
            return this.i18nKey;
        }

        /**
         * <p>
         * Gets the identifier of this {@link LinkedCategory}.
         * </p>
         *
         * @return the id of {@link LinkedCategory}
         */
        public int getId() {
            return this.id;
        }

        /**
         * <p>
         * Gets the {@link LinkedCategory}.
         * </p>
         *
         * @param id Refers the category id
         * @return {@link LinkedCategory}
         */
        public static LinkedCategory valueOf(final int id) {

            return switch (id) {
                case 1 -> LINKED_WITH_A_DEAL;
                case 2 -> LINKED_WITH_AN_OPEN_DEAL;
                case 3 -> NOT_LINKED_WITH_A_DEAL;
                case 4 -> ATTACHMENTS;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_LINKED_CATEGORY,
                        "i18n.notifications.email.filter.linked.category.invalid");
            };
        }
    }

    /**
     * <p>
     * Represents whether it is unread or not
     * </p>
     *
     * @author Mohamed Yasar k
     */
    public enum Unread implements FilterOption {

        TRUE(true),
        FALSE(false),
        ;

        private final boolean status;

        Unread(final boolean status) {
            this.status = status;
        }

        public boolean getStatus() {
            return status;
        }
    }

    /**
     * <p>
     * Represents the {@link FilterInterval}.
     * </p>
     *
     * @author KowsalyaSP
     */
    public enum FilterInterval implements FilterOption {

        /**
         * Shows all the emails until today.
         */
        ANYTIME(new I18nKey("i18n.notifications.email.filter.anytime"), 1),

        /**
         * Shows only Yesterday’s email.
         */
        LONGER_THAN_A_TIME(new I18nKey("i18n.notifications.email.filter.longer.than.time"), 2),

        /**
         * Shows the email up to previous week
         */
        LONGER_THAN_A_WEEK(new I18nKey("i18n.notifications.email.filter.longer.than.week"), 3),

        /**
         * Shows the email up to previous month
         */
        LONGER_THAN_A_MONTH(new I18nKey("i18n.notifications.email.filter.longer.than.month"), 4);

        private final I18nKey i18nKey;
        private final int id;

        FilterInterval(final I18nKey i18nKey, final int id) {
            this.i18nKey = Objects.requireNonNull(i18nKey);
            this.id = id;
        }

        /**
         * <p>
         * Returns the {@link I18nKey} associated with this {@link FilterInterval}.
         * </p>
         *
         * @return the name of {@link FilterInterval}
         */
        public I18nKey getI18nKey() {
            return this.i18nKey;
        }

        /**
         * <p>
         * Gets the identifier of this {@link FilterInterval}.
         * </p>
         *
         * @return the {@link FilterInterval} id
         */
        public int getId() {
            return this.id;
        }

        /**
         * <p>
         * Gets the {@link FilterInterval}.
         * </p>
         *
         * @param id Refers the interval id
         * @return {@link FilterInterval}
         */
        public static FilterInterval valueOf(final int id) {

            return switch (id) {
                case 1 -> ANYTIME;
                case 2 -> LONGER_THAN_A_TIME;
                case 3 -> LONGER_THAN_A_WEEK;
                case 4 -> LONGER_THAN_A_MONTH;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_FILTER_INTERVAL,
                        "i18n.notifications.email.filter.interval.invalid");
            };
        }
    }

    /**
     * <p>
     * Represents the {@link FilterAttribute}.
     * </p>
     *
     * @author KowsalyaSP
     */
    public enum FilterAttribute {

        /**
         * Includes the unread messages
         */
        UNREAD("unread", 1),

        /**
         * Represents the tracking date interval for the opened
         */
        OPENED_INTERVAL("openedInterval", 2),

        /**
         * Represents the tracking date interval for the clicked
         */
        CLICKED_INTERVAL("clickedInterval", 3),

        /**
         * Represents the category associated with threads linked to deals or attachments.
         */
        LINKED_CATEGORY("linkedCategory", 4),

        /**
         * Represents the category of emails for team inbox.
         */
        TEAM_INBOX("teamInbox", 5),

        /**
         * Represents the category of emails for sync emails.
         */
        SYNC_EMAILS("syncEmails", 6),
        ;

        private final int id;
        private final String name;

        FilterAttribute(final String name, final int id) {
            this.id = id;
            this.name = name;
        }

        /**
         * <p>
         * Gets the identifier of this {@link FilterAttribute}.
         * </p>
         *
         * @return the attribute id
         */
        public int getId() {
            return this.id;
        }

        /**
         * <p>
         * Returns the field name associated with this {@link FilterAttribute}.
         * </p>
         *
         * @return filter attribute
         */
        public String getName() {
            return this.name;
        }

        /**
         * <p>
         * Checks whether this category represents a linked category.
         * </p>
         *
         * @return {@code true} if this category is {@code LINKED_CATEGORY}, {@code false} otherwise.
         */
        public boolean isLinkedCategory() {
            return Objects.equals(LINKED_CATEGORY, this);
        }

        /**
         * <p>
         * Checks whether this category represents a team inbox category.
         * </p>
         *
         * @return {@code true} if this category is {@code EMAILS}, {@code false} otherwise.
         */
        public boolean isTeamInboxCategory() {
            return Objects.equals(TEAM_INBOX, this);
        }

        /**
         * <p>
         * Checks whether this category represents a sync emails category.
         * </p>
         *
         * @return {@code true} if this category is {@code EMAILS}, {@code false} otherwise.
         */
        public boolean isSyncEmailCategory() {
            return Objects.equals(SYNC_EMAILS, this);
        }

        /**
         * <p>
         * Gets the filter attribute by name.
         * </p>
         *
         * @param attribute Represents the filter attribute
         * @return {@link FilterAttribute}
         */
        public static FilterAttribute get(final String attribute) {
            return switch (attribute) {
                case "unread" -> UNREAD;
                case "openedInterval" -> OPENED_INTERVAL;
                case "clickedInterval" -> CLICKED_INTERVAL;
                case "linkedCategory" -> LINKED_CATEGORY;
                case "teamInbox" -> TEAM_INBOX;
                case "syncEmails" -> SYNC_EMAILS;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_EMAIL_FILTER_ATTRIBUTE,
                        "i18n.notifications.email.filter.attribute.invalid");
            };
        }

        /**
         * <p>
         * Gets the filter attribute by id.
         * </p>
         *
         * @param id Represents the filter attribute id
         * @return {@link FilterAttribute}
         */
        public static FilterAttribute getFilterAttribute(final int id) {
            return switch (id) {
                case 1 -> UNREAD;
                case 2 -> OPENED_INTERVAL;
                case 3 -> CLICKED_INTERVAL;
                case 4 -> LINKED_CATEGORY;
                case 5 -> TEAM_INBOX;
                case 6 -> SYNC_EMAILS;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_EMAIL_FILTER_ATTRIBUTE,
                        "i18n.notifications.email.filter.attribute.invalid");
            };
        }

        /**
         * <p>
         * Checks whether the filter attribute type has sub types or not.
         * </p>
         *
         * @return true, if the filter attribute type has subType
         */
        public boolean hasSubtypes() {
            return switch (this) {
                case OPENED_INTERVAL, CLICKED_INTERVAL, LINKED_CATEGORY, TEAM_INBOX, SYNC_EMAILS -> true;
                default -> false;
            };
        }

        /**
         * <p>
         * Checks whether the attribute type has {@link FilterInterval} subtype.
         * </p>
         *
         * @return true, if the filter attribute type has {@link FilterInterval} subtype
         */
        public boolean hasIntervalSubTypes() {
            return switch (this) {
                case OPENED_INTERVAL, CLICKED_INTERVAL -> true;
                default -> false;
            };
        }
    }

    /**
     * <p>
     * Represents the {@link Folder} of mail.
     * </p>
     *
     * @author KowsalyaSP
     */
    public enum Folder {

        INBOX("inbox", 1),
        SENT("sent", 2),
        DRAFT("draft", 3),
        SCHEDULED("scheduled", 4),
        STARRED("starred", 5),
        TRASH("trash", 6),
        ARCHIVE("archive", 7),
        AWAITING("awaiting", 8),
        BOUNCE("bounce", 9),
        OPENED("opened", 10),
        CLICKED("clicked", 11),
        TEAM_INBOX("teamInbox", 12),
        ;

        private final String name;
        private final int id;

        Folder(final String name, final int id) {
            this.name = Objects.requireNonNull(name);
            this.id = id;
        }

        /**
         * <p>
         * Returns the name associated with the {@link Folder}.
         * </p>
         *
         * @return the name of {@link Folder}
         */
        public String getName() {
            return this.name;
        }

        /**
         * <p>
         * Gets the identifier of the {@link Folder}.
         * </p>
         *
         * @return the id of {@link Folder}
         */
        public int getId() {
            return this.id;
        }

        /**
         * <p>
         * Retrieves the {@link Folder} for the given id.
         * </p>
         *
         * @return {@link Folder} of mail
         */
        public static Folder valueOf(final int id) {
            return switch (id) {
                case 1 -> INBOX;
                case 2 -> SENT;
                case 3 -> DRAFT;
                case 4 -> SCHEDULED;
                case 5 -> STARRED;
                case 6 -> TRASH;
                case 7 -> ARCHIVE;
                case 8 -> AWAITING;
                case 9 -> BOUNCE;
                case 10 -> OPENED;
                case 11 -> CLICKED;
                case 12 -> TEAM_INBOX;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_EMAIL_FOLDER,
                        "i18n.notifications.email.filter.folder.invalid");
            };
        }

        /**
         * <p>
         * Checks whether the related folder requires an interval.
         * </p>
         *
         * @return true if the folder needs dateInterval
         */
        public boolean hasInterval() {
            return switch (this) {
                case INBOX, SENT, STARRED -> true;
                default -> false;
            };
        }

        public boolean isTeamInbox() {
            return Objects.equals(this, TEAM_INBOX);
        }

        public boolean isTrash() {
            return Objects.equals(this, TRASH);
        }

        public boolean isDraft() {
            return Objects.equals(this, DRAFT);
        }

        /**
         * <p>
         * Checks whether the folder is opened or clicked folder
         * </p>
         *
         * @return true if the folder is either opened or clicked
         */
        public boolean isTrackingFolder() {
            return switch (this) {
                case OPENED, CLICKED -> true;
                default -> false;
            };
        }

        /**
         * <p>
         * Checks whether the related folder requires an unread message.
         * </p>
         *
         * @return true if the folder needs unread message
         */
        public boolean hasUnreadAttribute() {
            return switch (this) {
                case INBOX, STARRED -> true;
                default -> false;
            };
        }
    }
}
