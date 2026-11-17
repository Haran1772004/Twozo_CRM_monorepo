package com.tlc.crm.core.features.timeline.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.map.util.MapUrlProvider;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.service.UserPreferenceHolderService;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.features.timeline.model.CallTimeline;
import com.tlc.crm.core.features.timeline.manager.ReadableTimelineManager;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.status.TimelineErrorCodes;
import com.tlc.crm.entity.models.type.converter.IconConverter;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.field.TimelineFields;
import com.tlc.crm.entity.timeline.model.ActivityTimeline;
import com.tlc.crm.entity.timeline.model.AttachmentTimeline;
import com.tlc.crm.entity.timeline.model.CompanyTimeline;
import com.tlc.crm.entity.timeline.model.ContactTimeline;
import com.tlc.crm.entity.timeline.model.DealTimeline;
import com.tlc.crm.entity.timeline.model.EmailTimeline;
import com.tlc.crm.entity.timeline.model.EntityTimelines;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.InvoiceTimeline;
import com.tlc.crm.entity.timeline.model.NotesTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.Timeline.Category;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.entity.timeline.model.WebFormTimeline;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;
import com.tlc.i18n.I18nKey;
import com.tlc.iam.common.models.User;
import com.tlc.l10n.internal.temporal.TemporalPattern;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;

/**
 * <p>
 * Manages {@link Timeline} related web actions
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@SourceAuthorizer(relatedApiTypes = SourceAuthorizer.RelatedApiType.TIMELINE, needRelatedApiHandling = true)
@WebAction(path = "/timeline/mgmt")
public class TimelineManagement extends AuthorizationController {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 100;
    private static final int ZERO = 0;
    private static final int MAX_LENGTH = 60;
    private static final int THREE = 3;
    private static final int MIN_SIZE = 1;

    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final CompanyManager companyManager;
    private final ReadableTimelineManager timelineManager;
    private final IconConverter iconConverter;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final UserService crmUserService;
    private final com.tlc.iam.client.UserService userService;
    private final OrganizationService organizationService;
    private final HtmlParser htmlParser;
    private final AttachmentService attachmentService;
    private final UserPreferenceHolderService userPreferenceHolderService;

    public TimelineManagement() {
        contactManager = ContactManager.getInstance();
        dealManager = DealManager.getInstance();
        companyManager = CompanyManager.getInstance();
        timelineManager = ReadableTimelineManager.getInstance();
        iconConverter = IconConverter.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        crmUserService = UserService.getInstance();
        userService = com.tlc.iam.client.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        htmlParser = HtmlParser.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        userPreferenceHolderService = UserPreferenceHolderService.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Refers the {@link CrmRequest}.
     * @param response Refers the {@link CrmResponse}.
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject("data");
        final Long orgId = request.orgId();
        final String timeline = TimelineFields.TIMELINE.getName();
        final int start = Math.max(data.optInt(TimelineFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(TimelineFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);

        switch (requestJson.getString("type")) {
            case "contactTimeline" ->
                    response.put(timeline, getContactTimeline(orgId, data, response, start, limit, request));
            case "dealTimeline" ->
                    response.put(timeline, getDealTimeline(orgId, data, response, start, limit, request));
            case "companyTimeline" ->
                    response.put(timeline, getCompanyTimeline(orgId, data, response, start, limit, request));
        }
    }

    /**
     * <p>
     * Gets the {@link JsonArray} of timeline for given contact id
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param data     Which contains the request data
     * @param response Which represents the response data
     * @param request  Represents the {@link CrmRequest}
     * @return the {@link JsonArray} of the mapped Timeline
     */
    private JsonArray getContactTimeline(final Long orgId, final JsonObject data, final CrmResponse response,
                                         final int start, final int limit, final CrmRequest request) {
        final JsonArray jsonArray = Json.array();
        final Long contactId = data.getLong(TimelineFields.CONTACT_ID.getName());
        final Set<Category> categories = new HashSet<>();
        final Long totalRecords;

        if (contactManager.exists(orgId, contactId)) {
            final String timelineFilter = TimelineFields.FILTER.getName();

            if (data.containsKey(timelineFilter)) {
                setFilterCategories(data.getJsonObject(timelineFilter), categories);
                totalRecords = timelineManager.getContactTimelineCount(orgId, contactId, categories);
            } else {
                totalRecords = timelineManager.getContactTimelineCount(orgId, contactId);
            }
        } else {
            throw ErrorCode.get(TimelineErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }

        if (totalRecords > 0) {
            final Collection<Timeline> timelines = timelineManager.getContactTimeline(orgId, contactId, start, limit, categories);
            final Collection<Long> attachmentIds = getTimelineAttachmentIds(timelines);

            if (!attachmentIds.isEmpty()) {
                setTimelineAttachmentDetails(timelines, attachmentIds);
            }
            fetchAndDispatchTimeline(orgId, timelines, Source.CONTACT, totalRecords, start, limit, request, response);
        }
        return jsonArray;
    }

    /**
     * <p>
     * Fetches the dispatches the timeline
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param timelines    Contains the collection of {@link Timeline}
     * @param source       Represents the {@link Source}
     * @param totalRecords Represents the timeline total records
     * @param start        Represents the starting index of timeline to be fetched
     * @param limit        Represents the maximum number of timeline to be fetched
     * @param crmRequest   Holds the {@link CrmRequest} details
     * @param crmResponse  Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchTimeline(final Long orgId, final Collection<Timeline> timelines, final Source source, final Long totalRecords,
                                          final int start, final int limit, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Collection<Long> userIds = timelines.stream().flatMap(timeline -> timeline.getUserIds().stream()).collect(Collectors.toSet());
        final JsonArray jsonArray = Json.array();
        final Collection<Timeline> emailTimelines = timelines.stream().filter(timeline -> {
            final EmailTimeline emailTimeline = timeline.getEntityTimeline().get(EmailTimeline.TYPE);

            if (Objects.nonNull(emailTimeline)) {
                userIds.add(emailTimeline.getUserId());
                return true;
            }
            return false;
        }).toList();

        if (userIds.isEmpty()) {
            groupTimelinesByDate(timelines, totalRecords, source, start, limit, jsonArray, crmResponse, crmRequest);
        } else {
            final Collection<Long> iamUserIds = crmUserService.getUsers(orgId, userIds).stream()
                    .map(CrmUser::getIamId).collect(Collectors.toSet());

            crmResponse.convertIntoAsync();

            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                    final Map<Long, User> userByCrmId = new HashMap<>();

                    for (final Timeline timeline : timelines) {
                        final CrmUser crmUser = crmUserService.getUser(orgId, timeline.getOwnerId());
                        final User user = userMap.get(crmUser.getIamId());

                        crmUser.setUser(user);
                        timeline.setSalesOwner(crmUser);
                        userByCrmId.put(crmUser.id(), user);
                    }
                    setRecipients(userByCrmId, emailTimelines);
                    setUsersIntoFieldTimeline(userMap, orgId, timelines);
                    groupTimelinesByDate(timelines, totalRecords, source, start, limit, jsonArray, crmResponse, crmRequest);
                    crmResponse.put(TimelineFields.TIMELINE.getName(), jsonArray);
                    crmResponse.send();
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    crmResponse.setErrorCode(ErrorCode.get(TimelineErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                    crmResponse.send();
                }
            };
            userService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Gets the {@link JsonArray} of timeline for given deal id
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param data     Which contains the request data
     * @param response Which represent the response data
     * @param request  Represents the {@link CrmRequest}
     * @return the {@link JsonArray} of the mapped Timeline
     */
    private JsonArray getDealTimeline(final Long orgId, final JsonObject data, final CrmResponse response,
                                      final int start, final int limit, final CrmRequest request) {
        final JsonArray jsonArray = Json.array();
        final Long dealId = data.getLong(TimelineFields.DEAL_ID.getName());
        final Set<Category> categories = new HashSet<>();
        final Long totalRecords;

        if (dealManager.exists(orgId, dealId)) {
            final String timelineFilter = TimelineFields.FILTER.getName();

            if (data.containsKey(timelineFilter)) {
                setFilterCategories(data.getJsonObject(timelineFilter), categories);
                totalRecords = timelineManager.getDealTimelineCount(orgId, dealId, categories);
            } else {
                totalRecords = timelineManager.getDealTimelineCount(orgId, dealId);
            }
        } else {
            throw ErrorCode.get(TimelineErrorCodes.DEAL_NOT_FOUND);
        }

        if (totalRecords > 0) {
            final Collection<Timeline> timelines = timelineManager.getDealTimeline(orgId, dealId, start, limit, categories);
            final Collection<Long> attachmentIds = getTimelineAttachmentIds(timelines);

            if (!attachmentIds.isEmpty()) {
                setTimelineAttachmentDetails(timelines, attachmentIds);
            }
            fetchAndDispatchTimeline(orgId, timelines, Source.DEAL, totalRecords, start, limit, request, response);
        }
        return jsonArray;
    }

    /**
     * <p>
     * Gets the {@link JsonArray} of timeline for given company id
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param data     Which contains the request data
     * @param response Which contains the response data
     * @param request  Represents the {@link CrmRequest}
     * @return the {@link JsonArray} of the mapped Timeline
     */
    private JsonArray getCompanyTimeline(final Long orgId, final JsonObject data, final CrmResponse response,
                                         final int start, final int limit, final CrmRequest request) {
        final JsonArray jsonArray = Json.array();
        final Long companyId = data.getLong(TimelineFields.COMPANY_ID.getName());
        final Set<Category> categories = new HashSet<>();
        final Long totalRecords;

        if (companyManager.exists(orgId, companyId)) {
            final String timelineFilter = TimelineFields.FILTER.getName();

            if (data.containsKey(timelineFilter)) {
                setFilterCategories(data.getJsonObject(timelineFilter), categories);
                totalRecords = timelineManager.getCompanyTimelineCount(orgId, companyId, categories);
            } else {
                totalRecords = timelineManager.getCompanyTimelineCount(orgId, companyId);
            }
        } else {
            throw ErrorCode.get(TimelineErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
        }

        if (totalRecords > 0) {
            final Collection<Timeline> timelines = timelineManager.getCompanyTimeline(orgId, companyId, start, limit, categories);
            final Collection<Long> attachmentIds = getTimelineAttachmentIds(timelines);

            if (!attachmentIds.isEmpty()) {
                setTimelineAttachmentDetails(timelines, attachmentIds);
            }
            fetchAndDispatchTimeline(orgId, timelines, Source.COMPANY, totalRecords, start, limit, request, response);
        }
        return jsonArray;
    }

    /**
     * Extracts and returns the attachment IDs from a collection of Timeline objects.
     *
     * @param timelines The collection of {@code Timeline} objects from which to extract attachment IDs.
     * @return The collection of attachment IDs.
     */
    private Collection<Long> getTimelineAttachmentIds(final Collection<Timeline> timelines) {
        final Collection<Long> attachmentIds = new HashSet<>();

        for (final Timeline timeline : timelines) {
            final AttachmentTimeline attachmentTimeline = timeline.getEntityTimeline().get(AttachmentTimeline.TYPE);

            if (Objects.nonNull(attachmentTimeline)) {
                attachmentIds.add(attachmentTimeline.getId());
            }
        }
        return attachmentIds;
    }

    /**
     * Populates each {@code Timeline} object in the given collection with details from the provided
     * JSON array of attachments.
     *
     * @param timelines     The collection of {@code Timeline} objects to which attachment details will be added.
     * @param attachmentIds The {@code Collection} containing the attachment ids.
     */
    private void setTimelineAttachmentDetails(final Collection<Timeline> timelines, final Collection<Long> attachmentIds) {
        final Map<Long, String> attachments = attachmentService.getAttachments(attachmentIds).stream().collect(Collectors.toMap(Attachment::id, Attachment::getFileName));

        for (final Timeline timeline : timelines) {
            final AttachmentTimeline attachmentTimeline = timeline.getEntityTimeline().get(AttachmentTimeline.TYPE);

            if (Objects.nonNull(attachmentTimeline)) {
                attachmentTimeline.setName(attachments.get(attachmentTimeline.getId()));
            }
        }
    }

    /**
     * <p>
     * Groups the {@link Collection} of {@link Timeline} based on date.
     * </p>
     *
     * @param timelines    Represent the {@link Collection} of timelines to be converted
     * @param totalRecords Represents total records mapped for the given id
     * @param source       Represents the {@link Source}
     * @param start        Start value represents the lower bound of timeline to be fetched
     * @param limit        Limit value represents the upper bound of timeline to be fetched
     * @param jsonArray    Which contain the timeline records
     * @param response     Which represents the response data
     * @param request      Represents the {@link CrmRequest}
     */
    private void groupTimelinesByDate(final Collection<Timeline> timelines, final Long totalRecords, final Source source, final int start,
                                      final int limit, final JsonArray jsonArray, final CrmResponse response, final CrmRequest request) {
        final Map<Date, List<Timeline>> timelinesMap = new TreeMap<>(Collections.reverseOrder());

        timelinesMap.putAll(timelines.stream().collect(Collectors
                .groupingBy(timeline -> new Date(timeline.getAuditedTime().getTimeStamp()))));
        setTimelineData(totalRecords, source, (start + limit) < totalRecords, timelinesMap, jsonArray, response, request);
    }

    /**
     * <p>
     * Sets the timeline related records
     * </p>
     *
     * @param totalRecords Represents total records mapped for the given id
     * @param source       Represents the {@link Source}
     * @param hasMore      Represents the remaining records to be fetched
     * @param timelines    Represent the {@link Collection} of timelines to be converted
     * @param jsonArray    Which contain the timeline records
     * @param response     Which represents the response data
     * @param request      Represents the {@link CrmRequest}
     */
    private void setTimelineData(final Long totalRecords, final Source source, final boolean hasMore, final Map<Date, List<Timeline>> timelines,
                                 final JsonArray jsonArray, final CrmResponse response, final CrmRequest request) {
        final TemporalPattern.DatePattern datePattern = TemporalPattern.DatePattern.MONTH_DAY_YEAR;
        final CrmUser user = LoggedInUserInfo.getUser();
        final UserRegionalPreference userRegionalPreference = userPreferenceHolderService.get(user.orgId(), user.id()).get(UserPreferenceType.USER_REGIONAL_PREFERENCE);
        final TimezoneInfo timezoneInfo = userRegionalPreference.getTimezoneInfo();

        timelines.forEach((key, values) -> {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(TimelineFields.DATE.getName(), DateTimeFormatter.ofPattern(datePattern.get()).format(TemporalServiceProvider.get(key.getTime()).getZonedDate(timezoneInfo)));
            jsonObject.put(TimelineFields.DATA.getName(), convertTimelinesToJson(source, values.stream().sorted((primary, secondary) ->
                    Long.compare(secondary.getAuditedTime().getTimeStamp(), primary.getAuditedTime().getTimeStamp())).toList(), response, request));

            jsonArray.put(jsonObject);
        });

        response.put(TimelineFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(TimelineFields.HAS_MORE.getName(), hasMore);
    }

    /**
     * <p>
     * Converts the {@link List} of timelines
     * </p>
     *
     * @param source    Represents the {@link Source}
     * @param timelines Represents the list of timelines
     * @param response  Represents the crm response
     * @param request   Represents the {@link CrmRequest}
     * @return {@link JsonArray}
     */
    private JsonArray convertTimelinesToJson(final Source source, final Collection<Timeline> timelines, final CrmResponse response, final CrmRequest request) {
        final JsonArray jsonArray = Json.array();

        for (final Timeline timeline : timelines) {

            if (Objects.nonNull(timeline.getFieldTimeline())) {
                setFieldTimelineIntoJsonArray(timeline, source, response, jsonArray, request);
            } else {
                final JsonObject jsonObject = Json.object();
                final EntityTimelines entityTimelines = timeline.getEntityTimeline();
                final NotesTimeline notesTimeline = entityTimelines.get(NotesTimeline.TYPE);
                final ContactTimeline contactTimeline = entityTimelines.get(ContactTimeline.TYPE);
                final DealTimeline dealTimeline = entityTimelines.get(DealTimeline.TYPE);
                final CompanyTimeline companyTimeline = entityTimelines.get(CompanyTimeline.TYPE);
                final ActivityTimeline activityTimeline = entityTimelines.get(ActivityTimeline.TYPE);
                final AttachmentTimeline attachmentTimeline = entityTimelines.get(AttachmentTimeline.TYPE);
                final EmailTimeline emailTimeline = entityTimelines.get(EmailTimeline.TYPE);
                final WebFormTimeline webFormTimeline = entityTimelines.get(WebFormTimeline.TYPE);
                final CallTimeline callTimeline = entityTimelines.get(CallTimeline.TYPE);
                final InvoiceTimeline invoiceTimeline = entityTimelines.get(InvoiceTimeline.TYPE);

                setTimelineIntoJson(jsonObject, timeline, request);

                if (Objects.nonNull(notesTimeline)) {
                    jsonObject.put(TimelineFields.NOTE.getName(), notesTimeline.getNotes());
                }

                if (Objects.nonNull(companyTimeline)) {
                    jsonObject.put(TimelineFields.COMPANY_NAME.getName(), companyTimeline.getName());
                }

                if (Objects.nonNull(attachmentTimeline)) {
                    jsonObject.put(TimelineFields.ATTACHMENT_ID.getName(), attachmentTimeline.getId());
                    jsonObject.put(TimelineFields.FILE.getName(), attachmentTimeline.getName());
                }

                if (Objects.nonNull(contactTimeline)) {
                    jsonObject.put(TimelineFields.CONTACT_NAME.getName(), contactTimeline.getName());
                }

                if (Objects.nonNull(dealTimeline)) {
                    jsonObject.put(TimelineFields.DEAL_TITLE.getName(), dealTimeline.getTitle());

                    if (Source.DEAL != source) {
                        jsonObject.put(TimelineFields.DEAL_ID.getName(), dealTimeline.getId());
                    }
                }

                if (Objects.nonNull(activityTimeline)) {
                    jsonObject.put(TimelineFields.ACTIVITY_NAME.getName(), activityTimeline.getName());
                    jsonObject.put(TimelineFields.IS_ACTIVITY_DONE.getName(), activityTimeline.isDoneActivity());
                    jsonObject.put(TimelineFields.ACTIVITY_NOTE.getName(), activityTimeline.getActivityNote());
                }

                if (Objects.nonNull(webFormTimeline)) {
                    final JsonObject webFormJson = Json.object();

                    webFormJson.put(TimelineFields.ID.getName(), webFormTimeline.getId());
                    webFormJson.put(TimelineFields.NAME.getName(), webFormTimeline.getName());

                    jsonObject.put(TimelineFields.WEB_FORM.getName(), webFormJson);
                }

                if (Objects.nonNull(emailTimeline)) {
                    final JsonObject emailJson = Json.object();
                    final String content = emailTimeline.getContent();

                    if (Objects.nonNull(content)) {
                        emailJson.put(TimelineFields.CONTENT.getName(), trimToLength(htmlParser.convertHtmlToText(content)));
                    }
                    emailJson.put(TimelineFields.SUBJECT.getName(), emailTimeline.getSubject());
                    emailJson.put(TimelineFields.TO.getName(), emailTimeline.getToName());
                    emailJson.put(TimelineFields.FROM.getName(), emailTimeline.getFromName());
                    emailJson.put(TimelineFields.CONVERSATION_ID.getName(), emailTimeline.getId());

                    jsonObject.put(TimelineFields.DATA.getName(), emailJson);
                }

                if (Objects.nonNull(callTimeline)) {
                    final String duration = callTimeline.getDuration();

                    if (Objects.nonNull(duration)) {
                        jsonObject.put(TimelineFields.CALL_DURATION.getName(), TemporalServiceProvider.get().formatDuration(NumberUtils.parseNumber(duration).longValue()));
                    }
                    jsonObject.put(TimelineFields.AGENDA.getName(), callTimeline.getAgenda());
                }

                if (Objects.nonNull(invoiceTimeline)) {
                    jsonObject.put(TimelineFields.INVOICE_NUMBER.getName(), invoiceTimeline.getNumber());
                }

                jsonArray.put(jsonObject);
            }
        }

        return jsonArray;
    }

    /**
     * <p>
     * Sets the filter option based on user required category
     * </p>
     *
     * @param filterJson Which contains the timeline filter json
     * @param categories Represents {@link Set} of category for filter
     */
    private void setFilterCategories(final JsonObject filterJson, final Set<Category> categories) {
        final JsonArray categoryArray = filterJson.getJsonArray(TimelineFields.CATEGORY.getName());

        categoryArray.forEach(categoryOption -> {
            try {
                categories.add(Category.valueOf(categoryOption.toString().toUpperCase()));
            } catch (Exception exception) {
                throw ErrorCode.get(TimelineErrorCodes.UNSUPPORTED_TIMELINE_FILTER, "i18n.timeline.filter.unsupported");
            }
        });
    }

    /**
     * <p>
     * Sets the updated timeline records into the {@link JsonArray}.
     * </p>
     *
     * @param timeline  An entity that holds the timeline attributes
     * @param source    Represents the {@link Source}
     * @param response  Which represents the response data
     * @param jsonArray Which holds the field timeline record
     * @param request   Represents the {@link CrmRequest}
     */
    private void setFieldTimelineIntoJsonArray(final Timeline timeline, final Source source, final CrmResponse response, final JsonArray jsonArray,
                                               final CrmRequest request) {
        final FieldTimeline fieldTimeline = timeline.getFieldTimeline();
        final JsonObject jsonObject = Json.object();
        final TimelineType timelineType = timeline.getTimelineType();
        final NotesTimeline notesTimeline = timeline.getEntityTimeline().get(NotesTimeline.TYPE);
        final ContactTimeline contactTimeline = timeline.getEntityTimeline().get(ContactTimeline.TYPE);
        final DealTimeline dealTimeline = timeline.getEntityTimeline().get(DealTimeline.TYPE);
        final ActivityTimeline activityTimeline = timeline.getEntityTimeline().get(ActivityTimeline.TYPE);
        final InvoiceTimeline invoiceTimeline = timeline.getEntityTimeline().get(InvoiceTimeline.TYPE);

        setTimelineIntoJson(jsonObject, timeline, request);

        if (Objects.nonNull(fieldTimeline)) {
            final Collection<Integer> mergedTypes = List.of(TimelineType.CONTACT_MERGED.getId(), TimelineType.COMPANY_MERGED.getId());
            final Collection<String> timelineActionTypes = List.of(TimelineActionFields.FOLLOWERS_ADDED, TimelineActionFields.FOLLOWERS_REMOVED,
                    TimelineActionFields.RELATED_CONTACTS_ADDED, TimelineActionFields.RELATED_CONTACTS_REMOVED, TimelineActionFields.PRODUCTS_ADDED,
                    TimelineActionFields.PRODUCTS_REMOVED);

            if (mergedTypes.contains(timelineType.getId())) {
                jsonObject.put(TimelineFields.OLD_VALUES.getName(), response.resolveI18nString(fieldTimeline.getOldValues()));
            } else {
                jsonObject.put(TimelineFields.OLD_VALUE.getName(), response.resolveI18nString(fieldTimeline.getOldValue()));
            }
            final Collection<String> newValues = fieldTimeline.getNewValues();

            if (timelineActionTypes.contains(timeline.getAction())) {
                jsonObject.put(TimelineFields.NEW_VALUE.getName(), response.resolveI18nString(newValues));
            } else if (Objects.equals(timelineType, TimelineType.CHECK_IN) || Objects.equals(timelineType, TimelineType.CHECK_OUT)) {
                final Optional<String> optionalValue = fieldTimeline.getNewValues().stream().findFirst();

                optionalValue.ifPresent(value -> {
                    final JsonObject valueJson = Json.object(value);
                    final String operatingSystem = request.getUserAgent().getOperatingSystem();

                    loadCheckInOutLocationUrl(valueJson, ActivityFields.CHECK_IN.getName(), operatingSystem);
                    loadCheckInOutLocationUrl(valueJson, ActivityFields.CHECK_OUT.getName(), operatingSystem);

                    jsonObject.put(TimelineFields.NEW_VALUE.getName(), valueJson);
                });
            } else if (Objects.equals(TimelineType.ASSIGNED_TO_CHAT, timelineType)) {
                jsonObject.append(Json.object(newValues.iterator().next()));
            } else {
                jsonObject.put(TimelineFields.NEW_VALUE.getName(), response.resolveI18nString(newValues.iterator().next()));
            }
            final I18nKey fieldName = fieldTimeline.getFieldName();

            if (Objects.nonNull(fieldName)) {
                jsonObject.put(TimelineFields.FIELD_NAME.getName(), response.resolveI18nPlaceholder(fieldName));
            }
        }

        if (Objects.nonNull(notesTimeline)) {
            jsonObject.put(TimelineFields.NOTE.getName(), notesTimeline.getNotes());
        }

        if (Objects.nonNull(contactTimeline)) {
            jsonObject.put(TimelineFields.CONTACT_NAME.getName(), contactTimeline.getName());
        }

        if (Objects.nonNull(activityTimeline)) {
            jsonObject.put(TimelineFields.ACTIVITY_NOTE.getName(), activityTimeline.getActivityNote());
        }

        if (Objects.nonNull(dealTimeline)) {

            if (Source.DEAL != source) {
                jsonObject.put(TimelineFields.DEAL_ID.getName(), dealTimeline.getId());
            }

            if (TimelineType.DEAL_PIPELINE_MOVED == timeline.getTimelineType()) {
                final String newValueKey = TimelineFields.NEW_VALUE.getName();

                jsonObject.append(Json.object(jsonObject.getString(newValueKey)));
                jsonObject.remove(newValueKey);
            }
        }

        if (TimelineType.TRANSFERRED_CALL == timelineType) {
            final Long orgId = timeline.orgId();
            final String newValueKey = TimelineFields.NEW_VALUE.getName();
            final String fromKey = TimelineFields.FROM.getName();
            final String toKey = TimelineFields.TO.getName();
            final JsonObject transferredJson = Json.object(jsonObject.getString(newValueKey));

            transferredJson.put(fromKey, crmUserService.getUser(orgId, transferredJson.getLong(fromKey)).getFullName());
            transferredJson.put(toKey, crmUserService.getUser(orgId, transferredJson.getLong(toKey)).getFullName());
            jsonObject.remove(newValueKey);
            jsonObject.append(transferredJson);
        }

        if (Objects.nonNull(invoiceTimeline)) {
            jsonObject.put(TimelineFields.INVOICE_NUMBER.getName(), invoiceTimeline.getNumber());
        }

        jsonArray.put(jsonObject);
    }

    /**
     * <p>
     * Loads the location url for activity check-in and check-out
     * </p>
     *
     * @param valueJson       Represents the value json
     * @param key             Represents the field key
     * @param operatingSystem Represents the operating system name
     */
    private void loadCheckInOutLocationUrl(final JsonObject valueJson, final String key, final String operatingSystem) {

        if (valueJson.containsKey(key)) {
            final JsonObject checkInOutJson = valueJson.getJsonObject(key);
            final String longitude = checkInOutJson.getString(ActivityFields.LONGITUDE.getName());
            final String latitude = checkInOutJson.getString(ActivityFields.LATITUDE.getName());
            final String locationUrl = MapUrlProvider.fetchUrl(operatingSystem, latitude, longitude, null);

            checkInOutJson.put(ActivityFields.LOCATION_URL.getName(), locationUrl);
        }
    }

    /**
     * <p>
     * Trims the input to the tailored specifications.
     * </p>
     *
     * @param input Refers the input text.
     * @return {@link String} input.
     */
    private String trimToLength(final String input) {
        return input.length() > MAX_LENGTH ? String.valueOf(new StringBuilder(
                input.substring(ZERO, MAX_LENGTH - THREE)).append("...")) : input;
    }

    /**
     * <p>
     * Sets the recipients for email timeline
     * </p>
     *
     * @param userMap   A map to hold the {@link User} details
     * @param timelines Represents the {@link Collection} of {@link Timeline}
     */
    private void setRecipients(final Map<Long, User> userMap, final Collection<Timeline> timelines) {

        for (final Timeline timeline : timelines) {
            if (Objects.equals(Category.EMAILS, timeline.getCategory())) {
                final EmailTimeline emailTimeline = timeline.getEntityTimeline().get(EmailTimeline.TYPE);
                final List<String> toName = emailTimeline.getToName();
                final long userId = emailTimeline.getUserId();

                if (userMap.containsKey(userId)) {
                    final User user = userMap.get(userId);
                    final String userName = user.getFullName();
                    final String userEmail = user.getEmail();

                    if (Objects.equals(INBOUND, emailTimeline.getMessageCategory())) {
                        toName.removeIf(recipients -> Objects.equals(recipients, userEmail));
                        toName.add(NumericalConstant.ZERO, userName);
                        emailTimeline.setToName(toName);
                    } else {
                        emailTimeline.setFromName(userName);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Sets the {@link Timeline} into {@link JsonObject}
     * </p>
     *
     * @param jsonObject Represents the {@link JsonObject}
     * @param timeline   Represents the {@link Timeline}
     * @param crmRequest Represents the {@link CrmRequest}
     */
    private void setTimelineIntoJson(final JsonObject jsonObject, final Timeline timeline, final CrmRequest crmRequest) {
        final CrmUser salesOwner = timeline.getSalesOwner();

        jsonObject.put(TimelineFields.CATEGORY.getName(), timeline.getCategory().name().toLowerCase());
        jsonObject.put(TimelineFields.ACTION.getName(), timeline.getAction());
        jsonObject.put(TimelineFields.AUDITED_TIME.getName(), timeline.getAuditedTime().getTimeStamp());
        jsonObject.put(TimelineFields.TYPE.getName(), timeline.getTimelineType().getName());
        jsonObject.put(TimelineFields.ICON.getName(), iconConverter.convert(converterConfigBuilder.build(crmRequest, false, null), timeline.getIcon()));
        jsonObject.put(TimelineFields.ID.getName(), timeline.id());

        if (Objects.nonNull(salesOwner)) {
            final JsonObject salesOwnerJson = Json.object();

            salesOwnerJson.put(TimelineFields.ID.getName(), salesOwner.id());
            salesOwnerJson.put(TimelineFields.NAME.getName(), salesOwner.getFullName());

            jsonObject.put(TimelineFields.SALES_OWNER.getName(), salesOwnerJson);
        }
    }

    /**
     * <p>
     * Sets the field value related records into timeline
     * </p>
     *
     * @param userMap   A map to hold the {@link User} details
     * @param orgId     Represents an organization id
     * @param timelines Represents the {@link Collection} of {@link Timeline}
     */
    private void setUsersIntoFieldTimeline(final Map<Long, User> userMap, final Long orgId, final Collection<Timeline> timelines) {
        final Collection<Long> userIds = timelines.stream().flatMap(timeline -> timeline.getUserIds().stream()).collect(Collectors.toSet());
        final Map<Long, CrmUser> crmUsers = crmUserService.getUsers(orgId, userIds).stream().collect(Collectors.toMap(CrmUser::id, crmUser -> crmUser));

        for (final Timeline timeline : timelines) {
            final FieldTimeline fieldTimeline = timeline.getFieldTimeline();

            if (Objects.nonNull(fieldTimeline)) {
                switch (timeline.getAction()) {
                    case TimelineActionFields.REASSIGNED -> {
                        final CrmUser oldUser = crmUsers.get(Long.parseLong(fieldTimeline.getOldValue()));
                        final String newValue = fieldTimeline.getNewValues().iterator().next();
                        final CrmUser newUser = Objects.isNull(newValue) ? null : crmUsers.get(Long.parseLong(newValue));
                        final String oldValue = Objects.isNull(oldUser) ? null : userMap.get(oldUser.getIamId()).getFullName();

                        timeline.setFieldTimeline(new FieldTimeline(Objects.isNull(oldValue) ? null : List.of(oldValue),
                                Objects.isNull(newUser) ? null : userMap.get(newUser.getIamId()).getFullName(), fieldTimeline.getFieldName()));
                    }
                    case TimelineActionFields.FOLLOWERS_ADDED, TimelineActionFields.FOLLOWERS_REMOVED -> {
                        final Collection<String> values = fieldTimeline.getNewValues();
                        final Collection<String> newValues = new ArrayList<>();

                        for (final String value : values) {
                            newValues.add(userMap.get(crmUsers.get(Long.parseLong(value)).getIamId()).getFullName());
                        }
                        timeline.setFieldTimeline(new FieldTimeline(null, newValues, fieldTimeline.getFieldName()));
                    }
                }
            }
        }
    }
}