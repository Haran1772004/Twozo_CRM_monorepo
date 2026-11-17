package com.tlc.crm.core.notification.email.list.action.folder;

import com.tlc.attachment.model.profilePicture.DisplayPicture;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;

import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EntityInboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox;
import com.tlc.crm.core.notification.email.inbox.api.model.converter.TeamInboxConverter;
import com.tlc.crm.core.notification.email.inbox.fields.TeamInboxFields;
import com.tlc.crm.core.notification.email.list.api.filter.FilterOption;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.FilterInterval;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.LinkedCategory;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.FilterAttribute;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Unread;
import com.tlc.crm.core.notification.email.list.api.filter.model.SyncEmailFilterOption;
import com.tlc.crm.core.notification.email.list.api.filter.service.EmailFilterService;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;

import com.tlc.crm.core.notification.email.list.api.list.EmailListManager;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;

import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.type.Email.MessageCategory;
import com.tlc.email.common.service.EmailSanitizeService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * Handles the HTTP requests that are relevant to mail list action
 *
 * @author KarthickV
 * @version 1.0
 */
public abstract class CrmMailListAction extends AuthorizationController {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;
    private static final String TRACKING_COUNT = "count";
    private static final String TRACKING_TIME = "time";

    protected final EmailListManager emailListManager;
    private final EmailFilterService emailFilterService;
    private final ContactConverter contactConverter;
    private final EmailSanitizeService emailSanitizeService;
    private final TeamInboxConverter teamInboxConverter;

    public CrmMailListAction() {
        this.emailListManager = EmailListManager.getInstance();
        this.emailFilterService = EmailFilterService.get();
        this.contactConverter = ContactConverter.getInstance();
        this.emailSanitizeService = EmailSanitizeService.getInstance();
        this.teamInboxConverter = TeamInboxConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents an {@link CrmRequest}
     * @param crmResponse Represents an {@link CrmResponse}
     */
    @Override
    protected final void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(CoreEmailFields.DATA.getName());
        final Long orgId = crmRequest.orgId();
        final Long userId = crmRequest.userId();

        switch (requestJson.optString(CoreEmailFields.TYPE.getName(), "")) {
            case "getFilter" -> getFilter(orgId, userId, crmResponse);
            default -> processEmailList(orgId, userId, data, crmResponse);
        }
    }

    /**
     * <p>
     * Retrieves the total number of emails for a given organization and user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @return The total number of threads associated with the organization
     */
    protected Long getTotalRecords(final long orgId, final long userId, final EmailListFilter emailFilter) {
        return emailListManager.getThreadsCount(orgId, userId, emailFilter, getFolder());
    }

    /**
     * <p>
     * Fetches a collection of threads for a given organization and user, starting from a specified index and limiting the number of threads returned.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents a user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link EntityThread} to be fetched
     * @param limit       Represents the maximum number of {@link EntityThread} to be fetched
     * @return the collection of {@link EntityThread}
     */
    protected abstract Collection<EntityThread> fetchThreads(final long orgId, final long userId,
                                                             final EmailListFilter emailFilter,
                                                             final int start, final int limit);

    /**
     * <p>
     * Constructs the emailFilter by applying the criteria of {@link JsonObject}.
     * </p>
     *
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param jsonObject  Represents the request {@link JsonObject} data
     */
    protected void constructFilterConfig(final EmailListFilter emailFilter, final JsonObject jsonObject) {
    }

    /**
     * <p>
     * Loads the sync email filter attribute by extracting sync email data from the given
     * {@link JsonObject} and adding it to the filter attributes map.
     * </p>
     *
     * @param filterAttributes Represents the map of {@link FilterAttribute} to {@link FilterOption}
     * @param jsonObject       Represents the request {@link JsonObject} data
     */
    protected void loadSyncEmailFilterAttribute(final Map<FilterAttribute, FilterOption> filterAttributes, final JsonObject jsonObject) {
        if (jsonObject.containsKey(CoreEmailFields.SYNC_EMAILS.getName())) {
            filterAttributes.put(FilterAttribute.get(CoreEmailFields.SYNC_EMAILS.getName()),
                    new SyncEmailFilterOption(getSyncEmails(jsonObject.getJsonArray(CoreEmailFields.SYNC_EMAILS.getName()))));
        }
    }

    /**
     * <p>
     * Gets the folder of mail.
     * </p>
     *
     * @return {@link Folder}
     */
    protected abstract Folder getFolder();

    /**
     * <p>
     * Processes a list of emails and applying logic based on provided options and configurations.
     * These processing actions may encompass operations such as filtering and applying limits.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param data        Represents the {@link JsonObject} request
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void processEmailList(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final int start = Math.max(data.optInt(CoreEmailFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(CoreEmailFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final EmailListFilter emailFilter = getEmailFilter(orgId, userId, data);
        final Long totalRecords = getTotalRecords(orgId, userId, emailFilter);
        final boolean hasMore = (start + limit) <= totalRecords;
        final JsonArray jsonArray = Json.array();

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<EntityThread> threads = fetchThreads(orgId, userId, emailFilter, start, limit);

            if (Objects.nonNull(threads)) {

                for (final EntityThread thread : threads) {
                    jsonArray.put(convert(thread, crmResponse));
                }
            }
        }
        crmResponse.put(CoreEmailFields.LIST.getName(), jsonArray);
        crmResponse.put(CoreEmailFields.HAS_MORE.getName(), hasMore);
        crmResponse.put(CoreEmailFields.TOTAL_RECORDS.getName(), totalRecords);
    }

    /**
     * <p>
     * Converts a {@link EntityThread} into a {@link JsonObject}
     * </p>
     *
     * @param thread An entity that holds the attributes of thread
     * @return {@link JsonObject}
     */
    private JsonObject convert(final EntityThread thread, final CrmResponse crmResponse) {
        final JsonObject jsonObject = Json.object();
        final EntityEmail mail = thread.getComposedEmail();

        if (Objects.nonNull(mail)) {
            final EmailMetaData emailMetaData = mail.getMetaData();
            final String message = mail.getContent();
            final String subject = mail.getSubject();
            final Long dealId = thread.getEntityId();
            final Contact contact = mail.getContact();
            final EmailAddress emailAddress = mail.getSender();
            final int recipientsCount = mail.getRecipientsCount();
            final Boolean isStarred = mail.isStarred();
            final Boolean isArchived = mail.isArchived();
            final MessageCategory messageCategory = mail.getMessageCategory();

            jsonObject.put(CoreEmailFields.ID.getName(), thread.getCrmId());
            jsonObject.put(CoreEmailFields.ORG_ID.getName(), thread.orgId());
            jsonObject.put(CoreEmailFields.TIME_STAMP.getName(), emailMetaData.getDate());
            jsonObject.put(CoreEmailFields.CONVERSATION_COUNT.getName(), thread.getConversationCount());
            jsonObject.put(CoreEmailFields.MESSAGE_CATEGORY.getName(), messageCategory.name());
            jsonObject.put(CoreEmailFields.HAS_DRAFT.getName(), thread.hasDrafts());
            jsonObject.put(CoreEmailFields.HAS_ATTACHMENT.getName(), thread.hasAttachments());
            jsonObject.put(CoreEmailFields.IS_ARCHIVED.getName(), isArchived);
            jsonObject.put(CoreEmailFields.IS_STARRED.getName(), isStarred);
            jsonObject.put(CoreEmailFields.VARIANT.getName(), mail.getEmailVariant().getType().name());

            if (recipientsCount > 0) {
                jsonObject.put(CoreEmailFields.RECIPIENTS_COUNT.getName(), recipientsCount);
            }

            if (Objects.nonNull(message)) {
                jsonObject.put(CoreEmailFields.MESSAGE.getName(), emailSanitizeService.trimContent(message));
            }

            if (Objects.nonNull(subject)) {
                jsonObject.put(CoreEmailFields.SUBJECT.getName(), subject);
            }

            if (Objects.nonNull(dealId)) {
                jsonObject.put(CoreEmailFields.DEAL_ID.getName(), dealId);
            }

            if (Objects.nonNull(contact)) {
                final JsonObject contactJson = Json.object();
                final Long id = contact.id();
                final DisplayPicture thumbnail = contact.getThumbnail();

                contactJson.put(CoreEmailFields.CONTACT_NAME.getName(), contact.getFullName());
                contactJson.put(CoreEmailFields.CONTACT_ID.getName(), id);

                if (Objects.nonNull(thumbnail)) {
                    contactJson.put(CoreEmailFields.THUMBNAIL.getName(), contactConverter.convertThumbnailToJson(id, Contact.ENTITY_TYPE, thumbnail));
                }
                jsonObject.put(CoreEmailFields.CONTACT.getName(), contactJson);
            } else {
                jsonObject.put(CoreEmailFields.EMAIL.getName(), Objects.isNull(emailAddress) ? null : emailAddress.getEmailAddress());
            }

            switch (messageCategory) {
                case SCHEDULED -> {
                    final EntityScheduledEmail scheduledMail = (EntityScheduledEmail) mail;

                    jsonObject.put(CoreEmailFields.SCHEDULED_TIME.getName(), scheduledMail.getScheduledTime());
                    jsonObject.put(CoreEmailFields.SEND_STATUS.getName(), scheduledMail.sendStatus());
                }
                case INBOUND -> {
                    final EntityInboundEmail inboundEmail = (EntityInboundEmail) mail;

                    jsonObject.put(CoreEmailFields.IS_READ.getName(), inboundEmail.isRead());
                }
                case OUTBOUND -> {
                    if (mail instanceof EntityOutboundEmail entityOutboundEmail) {
                        final Boolean isScheduled = entityOutboundEmail.isScheduled();
                        final Collection<EmailTrackingDetail> emailTrackingDetails = entityOutboundEmail.getEmailTrackingDetails();

                        if (Objects.nonNull(isScheduled)) {
                            jsonObject.put(CoreEmailFields.IS_SCHEDULED.getName(), isScheduled);
                        }

                        if (Objects.nonNull(emailTrackingDetails) && !emailTrackingDetails.isEmpty()) {
                            final EmailTrackingDetail emailTrackingDetail = emailTrackingDetails.stream().findFirst().get();
                            final Long time = emailTrackingDetail.getTime();
                            final JsonObject trackingJson = Json.object();

                            trackingJson.put(TRACKING_COUNT, emailTrackingDetail.getCount());
                            trackingJson.put(TRACKING_TIME, time);
                            jsonObject.put(crmResponse.resolveI18nString(emailTrackingDetail.getType().getName()), trackingJson);
                        }
                    }
                }
            }
        }

        return jsonObject;
    }

    /**
     * Gets the {@link EmailListFilter} from the provider request {@link JsonObject} and respective organization.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param data   Represents the request {@link JsonObject} data
     * @return {@link EmailListFilter}
     */
    private EmailListFilter getEmailFilter(final long orgId, final long userId, final JsonObject data) {
        final String criteriaKey = CoreEmailFields.CRITERIA.getName();
        final EmailListFilter emailFilter;

        if (data.containsKey(criteriaKey)) {
            final JsonObject criteriaJson = data.getJsonObject(criteriaKey);
            emailFilter = new EmailListFilter();

            emailFilter.setUserId(userId);
            emailFilter.setOrgId(orgId);
            emailFilter.setFolder(getFolder());

            if (criteriaJson.size() > 0) {
                final String linkedCategoryKey = CoreEmailFields.LINKED_CATEGORY.getName();

                if (criteriaJson.containsKey(linkedCategoryKey)) {
                    emailFilter.setLinkedCategory(LinkedCategory.valueOf(criteriaJson.getInt(linkedCategoryKey)));
                }
                constructFilterConfig(emailFilter, criteriaJson);
            } else {
                emailFilterService.updateFilterAttributeConfig(emailFilter);

                return null;
            }
        } else {
            emailFilter = fetchExistingFilterConfig(orgId, userId);
        }

        if (Objects.nonNull(emailFilter) && !emailFilter.isConfigured()) {
            emailFilterService.updateFilterAttributeConfig(emailFilter);
        }

        return emailFilter;
    }

    /**
     * <p>
     * Fetches the existing filter config preferences based on user and organization.
     * </p>
     *
     * @param orgId  Represents an organization
     * @param userId Represents the user
     * @return {@link EmailListFilter}
     */
    private EmailListFilter fetchExistingFilterConfig(final long orgId, final long userId) {
        return emailFilterService.getExistingEmailListFilter(orgId, userId, getFolder());
    }

    /**
     * <p>
     * Retrieves the filter for a specified folder, using the organization ID and user ID, and subsequently updates the result in the {@link CrmResponse}.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void getFilter(final long orgId, final long userId, final CrmResponse crmResponse) {
        final EmailListFilter emailFilter = fetchExistingFilterConfig(orgId, userId);

        crmResponse.put(CoreEmailFields.FILTER.getName(), Objects.isNull(emailFilter) ? null : getFilterJson(emailFilter, crmResponse));
    }

    /**
     * <p>
     * Retrieves the filter {@link JsonObject} of the provided EmailFilter and updates
     * the result in the specified {@link CrmResponse}.
     * </p>
     *
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param response    Represents the {@link CrmResponse}
     * @return the filter JSON
     */
    private JsonObject getFilterJson(final EmailListFilter emailFilter, final CrmResponse response) {
        final JsonObject filterJson = Json.object();
        final Map<FilterAttribute, FilterOption> filterAttributes = emailFilter.getFilterAttribute();
        final FilterInterval openedInterval = (FilterInterval) filterAttributes.get(FilterAttribute.OPENED_INTERVAL);
        final FilterInterval clickedInterval = (FilterInterval) filterAttributes.get(FilterAttribute.CLICKED_INTERVAL);
        final LinkedCategory linkedCategory = (LinkedCategory) filterAttributes.get(FilterAttribute.LINKED_CATEGORY);
        final Unread unread = (Unread) filterAttributes.get(FilterAttribute.UNREAD);

        if (Objects.nonNull(linkedCategory)) {
            final JsonObject linkedCategoryJson = Json.object();

            linkedCategoryJson.put(CoreEmailFields.ID.getName(), linkedCategory.getId());
            linkedCategoryJson.put(CoreEmailFields.NAME.getName(), response.resolveI18nString(linkedCategory.getI18nKey()));
            filterJson.put(CoreEmailFields.LINKED_CATEGORY.getName(), linkedCategoryJson);
        }

        if (Objects.nonNull(openedInterval)) {
            filterJson.put(CoreEmailFields.OPENED_INTERVAL.getName(), getFilterIntervalJson(openedInterval, response));
        }

        if (Objects.nonNull(clickedInterval)) {
            filterJson.put(CoreEmailFields.CLICKED_INTERVAL.getName(), getFilterIntervalJson(clickedInterval, response));
        }

        if (Objects.nonNull(unread)) {
            filterJson.put(CoreEmailFields.UNREAD.getName(), unread.getStatus());
        }
        final Collection<TeamInbox> teamInboxes = emailFilter.getTeamInboxes();

        if (Objects.nonNull(teamInboxes) && !teamInboxes.isEmpty()) {
            filterJson.put(TeamInboxFields.TEAM_INBOXES.getName(), teamInboxConverter.convert(teamInboxes));
        }
        final Collection<Long> syncEmailIds = emailFilter.getSyncEmails();
        final boolean hasSyncEmailIds = Objects.nonNull(syncEmailIds) && !syncEmailIds.isEmpty();

        if (hasSyncEmailIds) {
            filterJson.put(CoreEmailFields.SYNC_EMAILS.getName(), getSyncEmails(syncEmailIds));
        }
        final int filterAttributeCount = filterAttributes.size();
        final int totalCount = hasSyncEmailIds ? (filterAttributeCount - 1) + syncEmailIds.size() : filterAttributeCount;

        filterJson.put(CoreEmailFields.FILTER_COUNT.getName(), totalCount);

        return filterJson;
    }

    /**
     * <p>
     * Converts a collection of synced email ids into a {@link JsonArray}.
     * </p>
     *
     * @param syncEmails Represents the collection of synced email ids to be converted
     * @return a {@link JsonArray} containing all the provided synced email ids
     */
    private JsonArray getSyncEmails(final Collection<Long> syncEmails) {
        final JsonArray jsonArray = Json.array();

        for (final Long syncEmail : syncEmails) {
            jsonArray.put(syncEmail);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Retrieves the {@link JsonObject} of the provided {@link FilterInterval}.
     * </p>
     *
     * @param filterInterval Represents the {@link FilterInterval} for opened and clicked attribute
     * @param response       Represents the {@link CrmResponse}
     * @return the filter interval {@link JsonObject}
     */
    private JsonObject getFilterIntervalJson(final FilterInterval filterInterval, final CrmResponse response) {
        final JsonObject intervalJson = Json.object();

        intervalJson.put(CoreEmailFields.ID.getName(), filterInterval.getId());
        intervalJson.put(CoreEmailFields.NAME.getName(), response.resolveI18nString(filterInterval.getI18nKey()));
        intervalJson.put(CoreEmailFields.IS_ENABLED.getName(), true);

        return intervalJson;
    }

    /**
     * <p>
     * Converts a {@link JsonArray} of synced email ids into a {@link Collection} of {@link Long} values.
     * </p>
     *
     * @param jsonArray Represents the {@link JsonArray} containing synced email ids to be converted
     * @return a {@link Collection} of {@link Long} synced email ids extracted from the given {@link JsonArray}
     */
    private Collection<Long> getSyncEmails(final JsonArray jsonArray) {
        final int size = jsonArray.size();
        final Collection<Long> syncEmails = new HashSet<>(size);

        for (int index = 0; index < size; index++) {
            syncEmails.add(jsonArray.getLong(index));
        }
        return syncEmails;
    }
}
