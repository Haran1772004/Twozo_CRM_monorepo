package com.tlc.crm.core.features.action.activity;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.common.service.UserPreferenceHolderService;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.insights.service.EntityInsightsService;
import com.tlc.crm.core.features.api.activity.manager.ActivityIndexManager;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityFilterCriteria;
import com.tlc.crm.core.features.api.activity.model.ActivityGuest;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.api.activity.model.converter.SourceActivityConverter;
import com.tlc.crm.core.internal.event.email.EmailAlertDataPublisher;
import com.tlc.crm.core.page.SourceActivityMobilePageHandler;
import com.tlc.crm.core.features.field.activity.ActivityHolderFields;
import com.tlc.crm.core.features.internal.feature.status.StatusErrorCode;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.page.SourceActivityWebPageHandler;
import com.tlc.crm.core.page.ActivityHolderPageHandler;
import com.tlc.crm.core.page.ActivityMobilePageHandler;
import com.tlc.crm.entity.action.AsyncBulkConfigAction;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.activity.model.CheckInOutRecord;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.features.api.activity.model.converter.ActivityConverter;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.internal.status.ActivityErrorCodes;
import com.tlc.crm.entity.list.filter.FilterCriteriaBuilder;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.list.sort.api.manager.SortManager;
import com.tlc.crm.common.list.sort.models.KanbanSortConfig;
import com.tlc.crm.common.list.sort.models.SortCriteria;
import com.tlc.crm.common.list.sort.models.SortViewType;
import com.tlc.crm.fields.cache.FieldSourceCache;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.iam.common.models.User;
import com.tlc.l10n.internal.temporal.TemporalPattern;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;

import com.tlc.validator.ModelValidator;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.validator.model.ErrorDescriptor;

import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import jakarta.validation.ConstraintViolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tlc.crm.entity.list.model.Provider.ACTIVITY_LIST;

/**
 * <p>
 * Manages {@link Activity} related web actions
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see CrmConfigAction
 */
@SourceAuthorizer(relatedApiTypes = SourceAuthorizer.RelatedApiType.ACTIVITY, needRelatedApiHandling = true)
@WebAction(path = "/activity/mgmt")
public class ActivityManagement extends AsyncBulkConfigAction<ActivityHolder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityManagement.class);

    private static final String CONFIG_ACTION_NAME = "activity";
    private static final String ACTIVITY_NOT_FOUND_I18N_KEY = "i18n.activity.not.found";
    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;
    private static final int ZERO = 0;
    private static final String IS_ACTIVITY_CREATION_REQUIRED = "isActivityCreationRequired";
    private static final byte ONE_DAY_IN_HOURS = 24;
    private static final int BEGIN_INDEX = 0;
    private static final int END_INDEX = 1;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final SourceActivityManager sourceActivityManager;
    private final SourceActivityConverter sourceActivityConverter;
    private final ActivityConverter activityConvert;
    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final CompanyManager companyManager;
    private final ActivityTypeManager activityTypeManager;
    private final ActivityManager activityManager;
    private final ActivityConverter activityConverter;
    private final UserService crmUserService;
    private final com.tlc.iam.client.UserService userService;
    private final OrganizationService organizationService;
    private final CrmUserConverter crmUserConverter;
    private final CrmUserManager crmUserManager;
    private final FilterCriteriaBuilder filterCriteriaBuilder;
    private final FieldSourceCache fieldSourceCache;
    private final SortManager sortManager;
    private final ActivityCalenderService activityCalenderService;
    private final EntityInsightsService entityInsightsService;
    private final EmailAlertDataPublisher emailAlertDataPublisher;

    public ActivityManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        sourceActivityManager = SourceActivityManager.getInstance();
        sourceActivityConverter = SourceActivityConverter.getInstance();
        contactManager = ContactManager.getInstance();
        dealManager = DealManager.getInstance();
        companyManager = CompanyManager.getInstance();
        activityTypeManager = ActivityTypeManager.getInstance();
        activityManager = ActivityManager.getInstance();
        activityConvert = ActivityConverter.getInstance();
        activityConverter = ActivityConverter.getInstance();
        crmUserService = UserService.getInstance();
        userService = com.tlc.iam.client.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        filterCriteriaBuilder = FilterCriteriaBuilder.getInstance();
        fieldSourceCache = FieldSourceCache.getInstance();
        sortManager = SortManager.getInstance();
        activityCalenderService = ActivityCalenderService.getInstance();
        entityInsightsService = EntityInsightsService.getInstance();
        this.emailAlertDataPublisher = EmailAlertDataPublisher.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final String activities = ActivityFields.ACTIVITIES.getName();
        final Long orgId = request.orgId();

        switch (type) {
            case "getActivities" -> fetchAndDispatchSourceActivities(orgId, data, request, response);
            case "calendarView" -> response.put(activities, getActivities(orgId, data, request));
            case "upcomingActivities" -> response.put(activities, getTodoActivities(orgId, data, request, response));
            case "summary" -> fetchAndDispatchSummary(orgId, data, request, response);
            case "getCollaborator" -> fetchAndDispatchCollaborators(orgId, data, response);
            case "getUserActivities" -> response.put(activities, getUserActivities(orgId, data, request, response));
            case "markAsCompletedFromGSTab" -> markAsCompletedFromGSTab(orgId, data, response);
        }
    }

    /**
     * <p>
     * Updates the {@link com.tlc.crm.entity.sql.resource.OMACACTIVITY#IS_DONE} flag for activities opened from the global search tab.
     * </p>
     *
     * @param orgId    Represents an organization id.
     * @param data     Holds activity ids and the done flag.
     * @param response Represents the {@link CrmResponse}.
     */
    private void markAsCompletedFromGSTab(final Long orgId, final JsonObject data, final CrmResponse response) {
        final Collection<Long> activityIds = data.getJsonArray(ActivityFields.ACTIVITY_IDS.getName()).toList().stream()
                .map(id -> (Long) id).toList();

        if (activityManager.exists(orgId, activityIds)) {
            final boolean isDone = data.getBoolean(ActivityFields.IS_DONE.getName());

            final Collection<Long> updatedRecords = activityManager.updateIsDone(orgId, activityIds, isDone);

            ActivityIndexManager.getInstance().updateIsDone(orgId, updatedRecords, isDone);
            response.put(ActivityFields.UPDATED_RECORDS.getName(), updatedRecords);
        } else {
            throw ErrorCode.get(ActivityErrorCodes.ACTIVITY_NOT_FOUND, ACTIVITY_NOT_FOUND_I18N_KEY);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Handles the received request
     * @param crmResponse Generates the CRM response
     * @throws Exception if an error occurs during the request processing
     */
    @Override
    protected void handleMobileRequest(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final Long orgId = crmRequest.orgId();

        switch (type) {
            case "getActivityDates" -> crmResponse.put(ActivityFields.ACTIVITY_DATES.getName(), getActivityDates(orgId, data));
            case "calendarView" -> fetchAndDispatchCalendarActivities(data, crmRequest, crmResponse);
            case "getActivities" -> fetchUserAndDispatchActivities(orgId, data, crmRequest, crmResponse);
            case "kanbanView" -> fetchAndDispatchKanbanActivities(data, crmRequest, crmResponse);
            case "checkIn" -> storeCheckInOutRecord(orgId, crmRequest.userId(), data, crmResponse, true);
            case "checkOut" -> storeCheckInOutRecord(orgId, crmRequest.userId(), data, crmResponse, false);
            default -> authorizedProcess(crmRequest, crmResponse);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmResponse     Represents the crm response
     * @param orgId           Represents an organization id
     * @param activityHolders Represents the {@link ActivityHolder}
     * @param eventData       Represents the {@link EventData}
     */
    @Override
    public void delete(final CrmResponse crmResponse, final Long orgId, final Long userId,
                       final Collection<ActivityHolder> activityHolders, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(activityHolders, Group.Delete.class);
        final Action action = Action.DELETE;

        if (errors.isEmpty()) {

            if (isValidDetails(orgId, userId, action, activityHolders)) {

                try {
                    final Collection<Long> activityIds = activityHolders.stream().map(ActivityHolder::getActivity)
                            .map(Activity::id).collect(Collectors.toSet());

                    entityInsightsService.enqueueMappedEntitiesForDeletedActivities(orgId, activityIds);
                    activityCalenderService.setEventIdsToActivities(orgId, activityHolders, action);
                    getConfigManager().delete(orgId, activityHolders);
                    postAction(action, activityHolders, eventData);
                } catch (ErrorCode errorCode) {
                    crmResponse.setErrorCode(errorCode);
                    crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode)));
                }
            }
        } else {
            loadValidationErrors(action, crmResponse, errors);
        }
    }

    /**
     * <p>
     * Gets the all the dates on which activities are scheduled for the given month and specified organization
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param jsonObject Represents the {@link JsonObject}
     */
    private JsonArray getActivityDates(final long orgId, final JsonObject jsonObject) {
        final TimezoneInfo timezoneInfo = getUserTimezoneInfo();
        final ZoneId userZone = timezoneInfo.getTimeZone().toZoneId();
        final LocalDate firstDay = getRequestedDate(jsonObject.getLong(ActivityFields.START_TIME.getName())).withDayOfMonth(1);
        final long startTime = getStartOfDayInMillis(firstDay, userZone);
        final long endTime = getStartOfDayInMillis(firstDay.plusMonths(1), userZone) - 1;

        final Collection<Interval<Long, Long>> dateIntervals = activityManager.getActivityDurations(orgId, startTime, endTime);
        final Collection<Long> activityDates = new HashSet<>();

        dateIntervals.forEach(dateInterval -> TemporalServiceProvider.get(dateInterval.start()).fetchDatesUpToEndTime(dateInterval.end(),
                timezoneInfo).stream().filter(date -> date >= startTime && date <= endTime).forEach(activityDates::add));

        return Json.array(activityDates);
    }

    /**
     * <p>
     * Fetches and dispatch the collaborators by given activity id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param jsonObject  Holds the activity {@link JsonObject} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchSummary(final Long orgId, final JsonObject jsonObject, final CrmRequest request, final CrmResponse crmResponse) {
        final Long id = jsonObject.getLong(ActivityFields.ID.getName());

        if (sourceActivityManager.exists(orgId, id)) {
            final ActivityHolder activityHolder = sourceActivityManager.getActivities(orgId, id);

            fetchAndDispatchSummary(request, activityHolder, jsonObject, crmResponse);
        } else {
            throw ErrorCode.get(ActivityErrorCodes.ACTIVITY_NOT_FOUND, ACTIVITY_NOT_FOUND_I18N_KEY);
        }
    }

    /**
     * <p>
     * Fetches and dispatch the collaborators by given activity id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param jsonObject  Holds the activity {@link JsonObject} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchCollaborators(final Long orgId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final Long id = jsonObject.getLong(ActivityFields.ID.getName());

        if (sourceActivityManager.exists(orgId, id)) {
            final int start = jsonObject.optInt(ActivityFields.START.getName(), DEFAULT_START_VALUE);
            final int limit = jsonObject.optInt(ActivityFields.LIMIT.getName(), DEFAULT_LIMIT);
            final long collaboratorsCount = activityManager.getCollaboratorCount(orgId, id);
            final Collection<Long> collaboratorIds = collaboratorsCount > ZERO && start <= collaboratorsCount
                    ? activityManager.getCollaborators(orgId, id, start, limit) : Collections.emptyList();
            final JsonArray jsonArray = Json.array();

            if (collaboratorIds.isEmpty()) {
                crmResponse.put(ActivityFields.TOTAL_RECORDS.getName(), collaboratorsCount);
                crmResponse.put(ActivityFields.HAS_MORE.getName(), (start + limit) <= collaboratorsCount);
                crmResponse.put(ActivityFields.COLLABORATORS.getName(), jsonArray);
            } else {
                final Collection<CrmUser> collaborators = crmUserService.getUsers(orgId, collaboratorIds);
                final Collection<Long> iamUserIds = collaborators.stream().map(CrmUser::getIamId).collect(Collectors.toSet());

                crmResponse.convertIntoAsync();
                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        final Map<Long, User> crmUsers = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                        for (final CrmUser crmUser : collaborators) {
                            crmUser.setUser(crmUsers.get(crmUser.getIamId()));
                            jsonArray.put(crmUserConverter.convert(crmUser));
                        }
                        crmResponse.put(ActivityFields.TOTAL_RECORDS.getName(), collaboratorsCount);
                        crmResponse.put(ActivityFields.HAS_MORE.getName(), (start + limit) <= collaboratorsCount);
                        crmResponse.put(ActivityFields.COLLABORATORS.getName(), jsonArray);
                        crmResponse.send();
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        sendError(crmResponse, null, ErrorCode.get(ActivityErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed", throwable));
                    }
                };
                userService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
            }
        } else {
            throw ErrorCode.get(ActivityErrorCodes.ACTIVITY_NOT_FOUND, ACTIVITY_NOT_FOUND_I18N_KEY);
        }
    }

    /**
     * <p>
     * Gets the user activities based on the given user id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param data        Holds the activity holder details
     * @param crmResponse Represents the {@link CrmResponse} details
     * @return The collection of activities for the given user
     */
    private JsonArray getUserActivities(final Long orgId, final JsonObject data, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long userId = data.getLong(ActivityFields.USER_ID.getName());

        if (!crmUserManager.exists(orgId, List.of(userId))) {
            throw ErrorCode.get(ActivityErrorCodes.USER_NOT_FOUND);
        }
        final JsonArray jsonArray = Json.array();
        final boolean isDone = data.getBoolean(ActivityFields.IS_DONE.getName());
        final int start = data.optInt(ActivityFields.START.getName(), DEFAULT_START_VALUE);
        final int limit = data.optInt(ActivityFields.LIMIT.getName(), DEFAULT_LIMIT);
        final Long totalRecords = sourceActivityManager.getUserActivityCount(orgId, userId, isDone);
        final Collection<ActivityHolder> activityHolders = totalRecords > ZERO && start <= totalRecords ?
                sourceActivityManager.getUserActivities(orgId, userId, isDone, start, limit) : null;

        if (Objects.nonNull(activityHolders)) {

            for (final ActivityHolder activityHolder : activityHolders) {
                jsonArray.put(sourceActivityConverter.convertActivity(converterConfigBuilder.build(crmRequest, true, data),
                        activityHolder));
            }
        }
        crmResponse.put(ActivityFields.TOTAL_RECORDS.getName(), totalRecords);
        crmResponse.put(ActivityFields.HAS_MORE.getName(), (start + limit) <= totalRecords);

        return jsonArray;
    }

    /**
     * {@inheritDoc}
     *
     * @param crmResponse     Represents the crm response
     * @param orgId           Represents the organization id
     * @param userId
     * @param activityHolders Represents the {@link ActivityHolder}
     */
    @Override
    public void create(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<ActivityHolder> activityHolders, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(activityHolders, Group.Create.class);
        final Action action = Action.CREATE;

        if (errors.isEmpty()) {

            if (isValidDetails(orgId, userId, action, activityHolders)) {
                preAction(action, activityHolders, eventData);
                final ProcessOutcome processOutcome = getConfigManager().create(orgId, activityHolders);

                loadResponseAndInvokePostAction(action, crmResponse, processOutcome, activityHolders, eventData);
                triggerMentionEmailAlert(orgId, userId, action, activityHolders);
                triggerCompletedEmailAlert(orgId, userId, action, eventData);
            }
        } else {
            loadValidationErrors(action, crmResponse, errors);
        }
    }

    /**
     * <p>
     * Triggers the email alert when the activity is mentioned in the note
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param userId          Represents the user id
     * @param action          Represents the {@link Action}
     * @param activityHolders Represents the {@link ActivityHolder}
     */
    private void triggerMentionEmailAlert(final long orgId, final long userId, final Action action,
                                          final Collection<ActivityHolder> activityHolders) {
        final Collection<Long> noteIds = new HashSet<>();

        activityHolders.forEach(activityHolder -> {
            final Note note = activityHolder.getActivity().getNote();

            if (Objects.nonNull(note)) {
                final Collection<CrmUser> mentionedUsers = note.getMentionedUsers();

                if (Objects.nonNull(mentionedUsers) && !mentionedUsers.isEmpty()) {
                    noteIds.add(note.id());
                }
            }
        });

        if (!noteIds.isEmpty()) {
            this.emailAlertDataPublisher.publish(orgId, userId,
                    action, noteIds, EmailNotificationTemplate.ACTIVITY_NOTE_MENTION.getName());
        }
    }

    /**
     * <p>
     * Triggers the email alert when the activity is marked as completed
     * </p>
     *
     * @param orgId     Represents the org ID
     * @param userId    Represents the user ID
     * @param action    Represents the {@link Action}
     * @param eventData Represents the {@link EventData}
     */
    private void triggerCompletedEmailAlert(final long orgId, final long userId,
                                            final Action action, final EventData eventData) {
        final Map<Long, TlcModel> previousValues = eventData.getPreviousValues();
        final Collection<Long> completedActivityIds = eventData.getValues()
                .stream().map(tlcModel -> (ActivityHolder) tlcModel)
                .filter(currentHolder -> {

                    if (!currentHolder.getActivity().getStatus().isCompleted()) {
                        return false;
                    }

                    if (Objects.isNull(previousValues)) {
                        return true;
                    }
                    final ActivityHolder previousHolder = (ActivityHolder) previousValues.get(currentHolder.id());

                    return Objects.isNull(previousHolder) || !previousHolder.getActivity().getStatus().isCompleted();
                }).map(TlcModel::id).collect(Collectors.toCollection(HashSet::new));

        if (!completedActivityIds.isEmpty()) {
            this.emailAlertDataPublisher.publish(orgId, userId, action,
                    completedActivityIds, EmailNotificationTemplate.ACTIVITY_COMPLETED.getName());
        }
    }

    /**
     * <p>
     * Gets the {@link JsonObject} of one week activities
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param jsonObject Represents the json object
     * @param request    Represents the request data
     * @return The {@link JsonObject} filled with activity data
     */
    private JsonArray getActivities(final Long orgId, final JsonObject jsonObject, final CrmRequest request) {
        final JsonObject filterJson = jsonObject.getJsonObject(ActivityFields.FILTER.getName());
        final int weekCount = filterJson.optInt(ActivityFields.WEEK_COUNT.getName(), ZERO);
        final Long time = weekCount != ZERO ? Instant.now().toEpochMilli() : filterJson.optLong(ActivityFields.START_TIME.getName(), Instant.now().toEpochMilli());
        final boolean isWeekEndKey = filterJson.getBoolean(ActivityFields.IS_WEEK_END.getName());
        final CrmUser user = LoggedInUserInfo.getUser();
        final UserRegionalPreference userPreference = UserPreferenceHolderService.get().get(user.orgId(), user.id())
                .get(UserPreferenceType.USER_REGIONAL_PREFERENCE);
        final TimezoneInfo timezoneInfo = userPreference.getTimezoneInfo();
        final Long weekStartTime = getStartTime(time, weekCount, isWeekEndKey, timezoneInfo);
        final Long weekEndTime = getEndTime(weekStartTime, isWeekEndKey, timezoneInfo);
        final Collection<FilterCriteria> filterCriteriaList = new ArrayList<>();

        if (jsonObject.containsKey(ActivityFields.CRITERIA.getName()) || jsonObject.containsKey(ActivityFields.ADDITIONAL_CRITERIA.getName())) {
            filterCriteriaList.addAll(filterCriteriaBuilder.constructFilterCriteria(orgId, request.userId(),
                    com.tlc.crm.entity.list.model.Provider.ACTIVITY_LIST, jsonObject));
        }
        final Collection<Activity> activities = activityManager.getActivities(orgId, weekStartTime, weekEndTime, filterCriteriaList);
        final JsonArray jsonArray = Json.array();

        for (final Activity activity : activities) {
            jsonArray.put(activityConvert.convertPartial(converterConfigBuilder.build(request, false, null), activity));
        }

        return jsonArray;
    }

    /**
     * <p>
     * Fetches the user and dispatches their corresponding activities for calendar view based on the given criteria
     * </p>
     *
     * @param jsonObject  Represents the {@link JsonObject}
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchCalendarActivities(final JsonObject jsonObject, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final ZoneId userZone = getUserTimezoneInfo().getTimeZone().toZoneId();
        final LocalDate requestedDate = getRequestedDate(jsonObject.getLong(ActivityFields.START_TIME.getName()));
        final long startTime = getStartOfDayInMillis(requestedDate, userZone);
        final long endTime = getStartOfDayInMillis(requestedDate.plusDays(1), userZone) - 1;
        final Collection<FilterCriteria> filterCriteriaList = new ArrayList<>();
        final long orgId = crmRequest.orgId();
        final int start = jsonObject.optInt(ActivityFields.START.getName(), DEFAULT_START_VALUE);
        final int limit = jsonObject.optInt(ActivityFields.LIMIT.getName(), DEFAULT_LIMIT);
        final Collection<Long> activityTypeIds = new HashSet<>();
        final String activityTypeIdsKey = ActivityHolderFields.ACTIVITY_TYPE_IDS.getName();

        if (jsonObject.containsKey(activityTypeIdsKey)) {
            jsonObject.getJsonArray(activityTypeIdsKey).forEach(typeId -> activityTypeIds.add((Long) typeId));
        }

        if (jsonObject.containsKey(ActivityFields.CRITERIA.getName())) {
            filterCriteriaList.addAll(filterCriteriaBuilder.constructFilterCriteria(orgId, crmRequest.userId(), ACTIVITY_LIST, jsonObject));
        }

        final ActivityHolderPageHandler activityHolderPageHandler = new ActivityHolderPageHandler(start, limit);

        activityHolderPageHandler.setStartTime(startTime);
        activityHolderPageHandler.setOrgId(orgId);
        activityHolderPageHandler.setEndTime(endTime);
        activityHolderPageHandler.setActivityTypeIds(activityTypeIds);
        activityHolderPageHandler.setFilterCriteriaList(filterCriteriaList);

        final Long totalRecords = activityHolderPageHandler.getTotalRecordsCount();
        final boolean hasMore = activityHolderPageHandler.hasMore();

        if (totalRecords > 0) {
            fetchUsersAndDispatchActivities(orgId, null, totalRecords, hasMore, jsonObject, activityHolderPageHandler.fetchPageData().getData(), crmRequest, crmResponse);
        } else {
            loadActivitiesIntoResponse(NumericalConstant.ZERO, false, Json.array(), crmResponse);
        }
    }

    /**
     * <p>
     * Gets the start time of the week
     * </p>
     *
     * @param time         Represents the local time of locale
     * @param weekCount    Represents the number of weeks to navigate in the date picker
     * @param timezoneInfo Represents the org/user's configured timezone, used to compute the day boundary correctly
     * @return The starting of the week day
     */
    private Long getStartTime(final Long time, final int weekCount, final boolean isWeekEnd, final TimezoneInfo timezoneInfo) {
        final Calendar calendar = Calendar.getInstance(timezoneInfo.getTimeZone());

        calendar.setTimeInMillis(time);
        calendar.set(Calendar.DAY_OF_WEEK, isWeekEnd ? calendar.getActualMinimum(Calendar.DAY_OF_WEEK) :
                calendar.getActualMinimum(Calendar.DAY_OF_WEEK) + 1);
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
        calendar.add(Calendar.WEEK_OF_YEAR, weekCount);

        return calendar.getTimeInMillis();
    }

    /**
     * <p>
     * Gets the end time of the week
     * </p>
     *
     * @param time Represents the local time of locale
     * @return The ending of the week day
     */
    private Long getEndTime(final Long time, final boolean isWeekEnd, final TimezoneInfo timezoneInfo) {
        final Calendar calendar = Calendar.getInstance(timezoneInfo.getTimeZone());

        calendar.setTimeInMillis(time);
        calendar.set(Calendar.DAY_OF_WEEK, isWeekEnd ? calendar.getActualMaximum(Calendar.DAY_OF_WEEK) :
                calendar.getActualMaximum(Calendar.DAY_OF_WEEK) - 1);
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMaximum(Calendar.MILLISECOND));

        return calendar.getTimeInMillis();
    }

    /**
     * <p>
     * Gets the {@link JsonArray} to retrieve the mapped activity associated with the source
     * </p>
     *
     * @param orgId    Represents an organisation id
     * @param data     Represents the request data
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchSourceActivities(final Long orgId, final JsonObject data,
                                                  final CrmRequest request, final CrmResponse response) {
        final String sourceKey = ActivityHolderFields.SOURCE.getName();
        final String sourceName = data.getString(sourceKey);
        final Source source = Source.get(sourceName);
        final long entityId = data.getLong(ActivityHolderFields.ENTITY_ID.getName());

        if (!entityExists(orgId, entityId, source)) {
            throw ErrorCode.get(ActivityErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }
        final String activityTypeIdsKey = ActivityHolderFields.ACTIVITY_TYPE_IDS.getName();
        final Collection<Long> activityTypeIds = new HashSet<>();

        if (data.containsKey(activityTypeIdsKey)) {
            data.getJsonArray(activityTypeIdsKey)
                    .forEach(typeId -> activityTypeIds.add((Long) typeId));
        }
        final String filterTypeKey = ActivityHolderFields.FILTER_TYPE.getName();
        final SourceActivityWebPageHandler sourceActivityWebPageHandler = new SourceActivityWebPageHandler(
                data.optInt(ActivityFields.START.getName(), DEFAULT_START_VALUE),
                data.optInt(ActivityFields.LIMIT.getName(), DEFAULT_LIMIT)
        );

        sourceActivityWebPageHandler.setSource(source);
        sourceActivityWebPageHandler.setOrgId(orgId);
        sourceActivityWebPageHandler.setEntityId(entityId);
        sourceActivityWebPageHandler.setActivityTypeIds(activityTypeIds);
        sourceActivityWebPageHandler.setActivityFilterCriteria(data.containsKey(filterTypeKey) ?
                ActivityFilterCriteria.get(data.getString(filterTypeKey)) : ActivityFilterCriteria.ALL_ACTIVITIES
        );
        final Long totalRecords = sourceActivityWebPageHandler.getTotalRecordsCount();

        if (totalRecords > NumericalConstant.ZERO) {
            fetchUsersAndDispatchActivities(orgId, sourceName, totalRecords, sourceActivityWebPageHandler.hasMore(),
                    data, sourceActivityWebPageHandler.fetchPageData().getData(), request, response);
        } else {
            loadActivitiesIntoResponse(NumericalConstant.ZERO, false, Json.array(), response);
            response.put(sourceKey, sourceName);
        }
    }

    /**
     * <p>
     * Fetches the users and dispatch the activity for source activities
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param totalRecords    Represents the total number of records to be presented
     * @param hasMore         Indicates if additional records are available for retrieval
     * @param source          Represents the {@link Source}
     * @param data            Holds the {@link JsonObject} details
     * @param activityHolders Represents the {@link ActivityHolder} details
     * @param request         Represents the {@link CrmRequest}
     * @param response        Represents the {@link CrmResponse}
     */
    private void fetchUsersAndDispatchActivities(final Long orgId, final String source, final Long totalRecords, final boolean hasMore, final JsonObject data,
                                                 final Collection<ActivityHolder> activityHolders, final CrmRequest request, final CrmResponse response) {
        final Collection<Long> userIds = activityHolders.stream().flatMap(
                activityHolder -> activityHolder.userIds().stream()).collect(Collectors.toUnmodifiableSet()
        );
        final Map<Long, CrmUser> usersById = crmUserService.getUsers(orgId, userIds)
                .stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));

        setUsersForPreview(activityHolders, usersById);
        try {
            final JsonArray jsonArray = Json.array();
            final ConverterConfig converterConfig = converterConfigBuilder.build(request,
                    false, data, request.getUserAgent().getOperatingSystem());

            for (final ActivityHolder activityHolder : activityHolders) {
                jsonArray.put(sourceActivityConverter.convertActivity(converterConfig, activityHolder));
            }
            loadActivitiesIntoResponse(totalRecords, hasMore, jsonArray, response);
            response.put(ActivityHolderFields.SOURCE.getName(), source);
        } catch (final Exception exception) {
            LOGGER.error("Activity summary list: Error while dispatching the activities for source {} and orgId {} Reason: {}",
                    source, orgId, exception.getMessage(), exception);
        } finally {
            response.send();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigManager<ActivityHolder> getConfigManager() {
        return sourceActivityManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivityHolder convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return sourceActivityConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link JsonObject} filled with the activity data
     */
    @Override
    public JsonObject convert(final CrmRequest request, final ActivityHolder activityHolder) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, null);

        return sourceActivityConverter.convertModelToJson(converterConfig, activityHolder);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Activity} filled with the data
     */
    @Override
    public ActivityHolder convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return sourceActivityConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents the organization id
     * @param userId          Represents the user id.
     * @param action          Perform action like create, update or delete
     * @param activityHolders Which contains the {@link Collection} of activities
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, Long userId, final Action action, final Collection<ActivityHolder> activityHolders) {

        if (Objects.nonNull(activityHolders) && !activityHolders.isEmpty()) {

            switch (action) {
                case CREATE, UPDATE -> {
                    final Set<Long> companyIds = new HashSet<>();
                    final Set<Long> dealIds = new HashSet<>();
                    final Set<Long> contactIds = new HashSet<>();
                    final Set<Long> typeIds = new HashSet<>();
                    final Collection<Long> userIds = new HashSet<>();

                    for (final ActivityHolder activityHolder : activityHolders) {
                        final Deal deal = activityHolder.getDeal();
                        final Activity activity = activityHolder.getActivity();
                        final Collection<Long> collaborators = activity.getCollaboratorIds();
                        final Collection<ActivityGuest> activityActivityGuests = activityHolder.getActivityGuests();
                        final List<Contact> contacts = activityHolder.getContacts();
                        final Company company = activityHolder.getCompany();
                        final ActivityType activityType = activityHolder.getActivity().getType();

                        if (Objects.nonNull(deal)) {
                            dealIds.add(deal.id());
                        }

                        if (Objects.nonNull(company)) {
                            companyIds.add(company.id());
                        }

                        if (Objects.nonNull(activityType)) {
                            typeIds.add(activityType.id());
                        }

                        if (!collaborators.isEmpty()) {
                            userIds.addAll(collaborators);
                        }

                        if (Objects.nonNull(activityActivityGuests)) {
                            final Collection<Contact> guestContacts = activityActivityGuests.stream().map(ActivityGuest::getContact)
                                    .filter(Objects::nonNull).collect(Collectors.toSet());

                            if (!guestContacts.isEmpty()) {
                                contactIds.addAll(guestContacts.stream().map(Contact::id).toList());
                            }
                        }
                        userIds.add(activity.getAssigneeId());
                        contactIds.addAll(contacts.stream().map(Contact::id).filter(Objects::nonNull).collect(Collectors.toSet()));
                    }

                    if (!contactIds.isEmpty() && !contactManager.exists(orgId, contactIds)) {
                        throw ErrorCode.get(StatusErrorCode.CONTACT_NOT_FOUND, "i18n.contact.not.found");
                    }

                    if (!dealIds.isEmpty() && !dealManager.exists(orgId, dealIds)) {
                        throw ErrorCode.get(StatusErrorCode.DEAL_NOT_FOUND, "i18n.deal.not.found");
                    }

                    if (!companyIds.isEmpty() && !companyManager.exists(orgId, companyIds)) {
                        throw ErrorCode.get(StatusErrorCode.COMPANY_NOT_FOUND, "i18n.company.not.found");
                    }

                    if (!activityTypeManager.exists(orgId, typeIds)) {
                        throw ErrorCode.get(ActivityErrorCodes.ACTIVITY_TYPE_NOT_FOUND, "i18n.activity.type.not.found");
                    }

                    if (!crmUserManager.exists(orgId, userIds)) {
                        throw ErrorCode.get(ActivityErrorCodes.USER_NOT_FOUND, "i18n.iam.user.not.found");
                    }
                }
            }
        }
        return true;
    }

    /**
     * <p>
     * Retrieves the upcoming activities and over due activities of the deal.
     * </p>
     *
     * @param orgId    Represents an organization id.
     * @param data     Represents the request json data.
     * @param request  Represents the entity of {@link CrmRequest}
     * @param response Represents the entity of {@link CrmResponse}
     * @return {@link JsonArray} filled with the activity data
     */
    private JsonArray getTodoActivities(final Long orgId, final JsonObject data, final CrmRequest request,
                                            final CrmResponse response) {
        final Long dealId = data.getLong(DealFields.DEAL_ID);
        final int start = Math.max(data.optInt(ActivityFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(ActivityFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Long todoActivitiesCount = sourceActivityManager.getTodoActivitiesCount(orgId, dealId, Source.DEAL);

        if (todoActivitiesCount > ZERO && start <= todoActivitiesCount) {
            response.put(DealFields.TOTAL_RECORDS, todoActivitiesCount);
            response.put(DealFields.HAS_MORE, (start + limit) <= todoActivitiesCount);

            final Collection<ActivityHolder> todoActivities = sourceActivityManager.getTodoActivities(orgId, dealId, start, limit);

            if (Objects.nonNull(todoActivities) && !todoActivities.isEmpty()) {
                final JsonArray jsonArray = Json.array();
                final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, null);

                for (final ActivityHolder activityHolder : todoActivities) {
                    jsonArray.put(convertActivityToJsonObject(activityHolder, converterConfig));
                }
                return jsonArray;
            }
        }

        return Json.array();
    }

    /**
     * Converts the {@link Activity} from an {@link ActivityHolder} to a {@link JsonObject} with additional information.
     *
     * @param activityHolder  Represents the {@link ActivityHolder} containing the activity.
     * @param converterConfig Represents the entity of {@link ConverterConfig}.
     * @return {@link JsonObject} representing the converted activity with additional information.
     */
    private JsonObject convertActivityToJsonObject(final ActivityHolder activityHolder, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final Activity activity = activityHolder.getActivity();
        final Long activityStartTime = activity.getStartTime().getTimeStamp();

        jsonObject.put(ActivityFields.IS_OVER_DUE.getName(), Instant.now().toEpochMilli() > activityStartTime);
        jsonObject.put(ActivityFields.TIME.getName(), activityStartTime);
        jsonObject.put(ActivityFields.ACTIVITIES.getName(),
                activityConverter.convertActivityToJsonObject(converterConfig, activity));
        jsonObject.put(ActivityFields.OWNER_NAME.getName(), activity.getAssignee().getFullName());

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest     Holds the {@link CrmRequest} details
     * @param activityHolder Represents the {@link ActivityHolder} details
     * @param data           Holds the activity holder details
     * @param users          Contains the collection of users
     * @return The {@link JsonObject} details
     */
    @Override
    protected JsonObject convertSummary(final CrmRequest crmRequest, final ActivityHolder activityHolder, final JsonObject data, final Collection<User> users) {

        return sourceActivityConverter.convertSummary(converterConfigBuilder.build(crmRequest, true, data,
                crmRequest.getUserAgent().getOperatingSystem()), activityHolder);
    }

    /**
     * <p>
     * Sets the {@link CrmUser} details into {@link ActivityHolder}
     * </p>
     *
     * @param activityHolder Represents the {@link ActivityHolder} details
     * @param users          Contains the collection of {@link User}
     */
    @Override
    protected void setUser(final ActivityHolder activityHolder, final Collection<User> users) {
        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
        final Activity activity = activityHolder.getActivity();
        final Long assigneeId = activity.getAssigneeId();
        final Long creatorId = activity.getCreatorId();
        final Long modifiedById = activity.getModifiedById();
        final Collection<CrmUser> collaborators = activity.getCollaborators();

        if (Objects.nonNull(assigneeId)) {
            final CrmUser assignee = crmUserService.getUser(activity.orgId(), assigneeId);

            assignee.setUser(userMap.get(assignee.getIamId()));
            activity.setAssignee(assignee);
        }

        if (Objects.nonNull(creatorId)) {
            final CrmUser creator = crmUserService.getUser(activity.orgId(), creatorId);

            creator.setUser(userMap.get(creator.getIamId()));
            activity.setCreator(creator);
        }

        if (Objects.nonNull(modifiedById)) {
            final CrmUser modifiedBy = crmUserService.getUser(activity.orgId(), modifiedById);

            modifiedBy.setUser(userMap.get(modifiedBy.getIamId()));
            activity.setModifiedBy(modifiedBy);
        }

        if (Objects.nonNull(collaborators) && !collaborators.isEmpty()) {

            for (final CrmUser collaborator : collaborators) {
                collaborator.setUser(userMap.get(collaborator.getIamId()));
            }
            activity.setCollaborators(collaborators);
        }
    }

    /**
     * <p>
     * Sets the  required preview {@link CrmUser}
     * such as creator, assignee and modifier details into {@link ActivityHolder}
     * </p>
     *
     * @param activityHolders Represents the collection of {@link ActivityHolder} details
     * @param userMap         Contains the collection of {@link CrmUser}
     */
    private void setUsersForPreview(final Collection<ActivityHolder> activityHolders,
                                    final Map<Long, CrmUser> userMap) {
        activityHolders.forEach(activityHolder -> {
            final Activity activity = activityHolder.getActivity();
            final Long assigneeId = activity.getAssigneeId();
            final Long creatorId = activity.getCreatorId();
            final Long modifiedById = activity.getModifiedById();

            if (Objects.nonNull(assigneeId)) {
                final CrmUser assignee = userMap.get(assigneeId);

                if (Objects.nonNull(assignee)) {
                    activity.setAssignee(assignee);
                }
            }

            if (Objects.nonNull(creatorId)) {
                final CrmUser creator = userMap.get(creatorId);

                if (Objects.nonNull(creator)) {
                    activity.setCreator(creator);
                }
            }

            if (Objects.nonNull(modifiedById)) {
                final CrmUser modifiedBy = userMap.get(modifiedById);

                if (Objects.nonNull(modifiedBy)) {
                    activity.setModifiedBy(modifiedBy);
                }
            }
        });
    }

    /**
     * <p>
     * Creates the given entity.
     * </p>
     *
     * @param orgId           An organization's id.
     * @param userId          A user's id.
     * @param response        {@link CrmResponse}.
     * @param eventData       {@link EventData}.
     * @param activityHolders {@link Collection} of  {@link ActivityHolder}.
     */
    protected void create(final Long orgId, final Long userId, final CrmResponse response, final EventData eventData,
                          final Collection<ActivityHolder> activityHolders, final String type) {
        this.create(response, orgId, userId, activityHolders, eventData);

        if (Objects.equals(type, ActionFields.CREATE)) {
            setIsActivityCreationRequired(activityHolders, response);
        }
    }

    /**
     * <p>
     * Updates the given entity.
     * </p>
     *
     * @param orgId           An organization's id.
     * @param userId          A user's id.
     * @param response        {@link CrmResponse}.
     * @param data            Holds data.
     * @param eventData       {@link EventData}.
     * @param activityHolders {@link Collection} of  {@link ActivityHolder}.
     */
    protected void update(final Long orgId, final Long userId, final CrmResponse response, final JsonObject data,
                          final EventData eventData, final Collection<ActivityHolder> activityHolders, final String type) {
        update(response, orgId, userId, activityHolders, eventData);
        triggerMentionEmailAlert(orgId, userId, Action.UPDATE, activityHolders);
        triggerCompletedEmailAlert(orgId, userId, Action.UPDATE, eventData);

        if (Objects.equals(type, ActionFields.UPDATE)) {
            setIsActivityCreationRequired(activityHolders, response);
        } else if (Objects.equals(type, ActionFields.PARTIAL_UPDATE)) {
            final String sourceKey = ActivityFields.SOURCE.getName();
            final String entityIdKey = ActivityFields.ENTITY_ID.getName();
            final String source = data.containsKey(sourceKey) ? data.getString(sourceKey) : null;
            final Long entityId = data.containsKey(entityIdKey) ? data.getLong(entityIdKey) : null;
            final boolean isEntityIdNull = Objects.isNull(entityId);

            if (Objects.isNull(source) && isEntityIdNull) {
                setIsActivityCreationRequired(activityHolders, response);
            } else if (!isEntityIdNull) {
                setIsActivityCreationRequired(entityId, source, response, activityHolders);
            }
        }
    }

    /**
     * <p>
     * Sets whether the activity creation is required and the required entity in response.
     * </p>
     *
     * @param activityHolders {@link Collection} of {@link ActivityHolder}.
     * @param response        {@link CrmResponse}.
     */
    private void setIsActivityCreationRequired(final Collection<ActivityHolder> activityHolders, final CrmResponse response) {
        activityHolders.stream().findFirst().ifPresent(activityHolder -> {

            if (activityHolder.getActivity().getDone()) {
                final Counter entityCounter = new Counter();
                final Long orgId = activityHolder.orgId();
                final String idKey = ActivityFields.ID.getName();
                final String nameKey = ActivityFields.NAME.getName();
                final Deal deal = activityHolder.getDeal();
                final Company company = activityHolder.getCompany();

                for (final Contact contact : activityHolder.getContacts()) {
                    final Long contactId = contact.id();

                    if (ZERO == sourceActivityManager.getOpenContactActivities(orgId, contactId)) {
                        response.put(ActivityFields.CONTACT.getName(), Map.of(idKey, contactId, nameKey, contact.getFullName()));
                        entityCounter.increment();
                    }
                    break;
                }

                if (Objects.nonNull(deal)) {
                    final Long dealId = deal.id();

                    if (ZERO == sourceActivityManager.getOpenDealActivities(orgId, dealId)) {
                        response.put(ActivityFields.DEAL.getName(), Map.of(idKey, dealId, ActivityFields.TITLE.getName(), deal.getTitle()));
                        entityCounter.increment();
                    }
                }

                if (Objects.nonNull(company)) {
                    final Long companyId = company.id();

                    if (ZERO == sourceActivityManager.getOpenCompanyActivities(orgId, companyId)) {
                        response.put(ActivityFields.COMPANY.getName(), Map.of(idKey, companyId, nameKey, company.getName()));
                        entityCounter.increment();
                    }
                }
                response.put(IS_ACTIVITY_CREATION_REQUIRED, ZERO < entityCounter.getCount());
            }
        });
    }

    /**
     * <p>
     * Sets whether the activity creation is required and the required entity in response.
     * </p>
     *
     * @param entityId        An entity's id.
     * @param source          Represents the source.
     * @param response        {@link CrmResponse}.
     * @param activityHolders Collection of {@link ActivityHolder}.
     */
    private void setIsActivityCreationRequired(final Long entityId, final String source, final CrmResponse response,
                                               final Collection<ActivityHolder> activityHolders) {
        final String contactKey = "contact";
        final String companyKey = "company";
        final String dealKey = "deal";
        final String idKey = ActivityFields.ID.getName();
        final String nameKey = ActivityFields.NAME.getName();

        activityHolders.stream().findFirst().ifPresent(activityHolder -> {

            if (activityHolder.getActivity().getDone()) {
                boolean isLastEntity = false;

                switch (source.toLowerCase()) {
                    case contactKey -> {
                        isLastEntity = ZERO == sourceActivityManager.getOpenContactActivities(activityHolder.orgId(), entityId);

                        if (isLastEntity) {
                            for (final Contact contact : activityHolder.getContacts()) {
                                if (Objects.equals(contact.id(), entityId)) {
                                    response.put(contactKey, Map.of(idKey, entityId, nameKey, contact.getFullName()));
                                    break;
                                }
                            }
                        }
                    }
                    case companyKey -> {
                        isLastEntity = ZERO == sourceActivityManager.getOpenCompanyActivities(activityHolder.orgId(), entityId);

                        if (isLastEntity) {
                            response.put(companyKey, Map.of(idKey, entityId, nameKey, activityHolder.getCompany().getName()));
                        }
                    }
                    case dealKey -> {
                        isLastEntity = ZERO == sourceActivityManager.getOpenDealActivities(activityHolder.orgId(), entityId);

                        if (isLastEntity) {
                            response.put(dealKey, Map.of(idKey, entityId, ActivityFields.TITLE.getName(), activityHolder.getDeal().getTitle()));
                        }
                    }
                    default -> {
                    }
                }
                response.put(IS_ACTIVITY_CREATION_REQUIRED, isLastEntity);
            }
        });
    }

    /**
     * <p>
     * Fetches the user and dispatch the activities associated with the given source
     * </p>
     *
     * @param jsonObject Represents the {@link JsonObject}
     * @param request    Represents the {@link CrmRequest}
     * @param response   Represents the {@link CrmResponse}
     */
    private void fetchUserAndDispatchActivities(final long orgId, final JsonObject jsonObject,
                                                final CrmRequest request, final CrmResponse response) {
        final String sourceName = jsonObject.getString(ActivityHolderFields.SOURCE.getName());
        final Source source = Source.get(sourceName);
        final long entityId = jsonObject.getLong(ActivityHolderFields.ENTITY_ID.getName());

        if (!entityExists(orgId, entityId, source)) {
            throw ErrorCode.get(ActivityErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }
        final Collection<Long> activityTypeIds = new HashSet<>();
        final String activityTypeIdsKey = ActivityHolderFields.ACTIVITY_TYPE_IDS.getName();

        if (jsonObject.containsKey(activityTypeIdsKey)) {
            jsonObject.getJsonArray(activityTypeIdsKey)
                    .forEach(typeId -> activityTypeIds.add((Long) typeId));
        }
        final SourceActivityMobilePageHandler sourceActivityMobilePageHandler = new SourceActivityMobilePageHandler(
                jsonObject.optInt(ActivityFields.START.getName(), DEFAULT_START_VALUE),
                jsonObject.optInt(ActivityFields.LIMIT.getName(), DEFAULT_LIMIT)
        );

        sourceActivityMobilePageHandler.setSource(source);
        sourceActivityMobilePageHandler.setOrgId(orgId);
        sourceActivityMobilePageHandler.setEntityId(entityId);
        sourceActivityMobilePageHandler.setActivityTypeIds(activityTypeIds);
        final String statusKey = ActivityFields.STATUS.getName();

        if (jsonObject.containsKey(statusKey)) {
            sourceActivityMobilePageHandler.setActivityFilterCriteria(
                    switch (Status.get(jsonObject.getInt(statusKey))) {
                        case COMPLETED -> ActivityFilterCriteria.COMPLETED_ACTIVITIES;
                        case UPCOMING -> ActivityFilterCriteria.UPCOMING_ACTIVITIES;
                        case OVERDUE -> ActivityFilterCriteria.OVERDUE_ACTIVITIES;
                    });
        } else {
            sourceActivityMobilePageHandler.setActivityFilterCriteria(ActivityFilterCriteria.ALL_ACTIVITIES);
        }
        final long totalRecords = sourceActivityMobilePageHandler.getTotalRecordsCount();

        if (totalRecords > NumericalConstant.ZERO) {
            fetchUsersAndDispatchActivities(orgId, sourceName, totalRecords, sourceActivityMobilePageHandler.hasMore(),
                    jsonObject, sourceActivityMobilePageHandler.fetchPageData().getData(), request, response);
        } else {
            loadActivitiesIntoResponse(NumericalConstant.ZERO, false, Json.array(), response);
        }
    }

    /**
     * <p>
     * Checks whether  the given entity is exists or not
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param entityId Represents the entity id
     * @param source   Represents the {@link Source}
     * @return true if the entity sis exists
     */
    private boolean entityExists(final long orgId, final Long entityId, final Source source) {
        return switch (source) {
            case CONTACT -> contactManager.exists(orgId, entityId);
            case DEAL -> dealManager.exists(orgId, entityId);
            case COMPANY -> companyManager.exists(orgId, entityId);
            default -> throw ErrorCode.get(ActivityErrorCodes.SOURCE_NOT_FOUND, "i18n.source.not.found");
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param model Represents {@link TlcModel}.
     */
    @Override
    public void resolveDateTimeFields(final TlcModel model) {
        if (model instanceof final ActivityHolder activityHolder) {
            dateTimeFieldsResolver.resolve(activityHolder.getActivity());
        } else {
            dateTimeFieldsResolver.resolve(model);
        }
    }

    /**
     * <p>
     * Gets the activities mapped for the given organization based on given criteria
     * </p>
     *
     * @param data        Represents the {@link JsonObject}
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchKanbanActivities(final JsonObject data, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final long orgId = crmRequest.orgId();
        final Collection<FilterCriteria> filterCriteriaList = new ArrayList<>();
        final String startTimeKey = ActivityFields.START_TIME.getName();
        final String sortKey = ActivityFields.SORT.getName();
        final JsonObject sortJson = data.optJsonObject(sortKey);
        final int start = Math.max(data.optInt(ActivityFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(ActivityFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final String activityTypeIdsKey = ActivityHolderFields.ACTIVITY_TYPE_IDS.getName();
        final Collection<Long> activityTypeIds = new HashSet<>();
        final String statusKey = ActivityFields.STATUS.getName();
        final Status status = data.containsKey(statusKey) ? Status.get(data.getInt(statusKey)) : null;
        final String fieldKey = ActivityFields.FIELD.getName();
        final String typeKey = ActivityFields.TYPE.getName();
        final KanbanSortConfig activityKanbanSortConfig = new KanbanSortConfig(orgId, crmRequest.userId(), fieldSourceCache.getSourceId(Source.ACTIVITY.getName()));

        activityKanbanSortConfig.setSortProvider(SortViewType.KANBAN_SORT);
        activityKanbanSortConfig.setStageId(Long.valueOf(status.getId()));

        if (data.containsKey(activityTypeIdsKey)) {
            data.getJsonArray(activityTypeIdsKey).forEach(typeId -> activityTypeIds.add((Long) typeId));
        }
        String sortField = startTimeKey;
        SortType sortType = SortType.ASCENDING;

        if (Objects.isNull(sortJson)) {
            final String sortEnabledKey = ActivityFields.IS_SORT_ENABLED.getName();

            if (data.containsKey(sortEnabledKey) && !data.getBoolean(sortEnabledKey)) {
                sortManager.remove(activityKanbanSortConfig);
            } else {
                final Optional<SortCriteria> optionalSortCriteria = sortManager.fetchCriteriaList(activityKanbanSortConfig).stream().findFirst();

                if (optionalSortCriteria.isPresent()) {
                    final SortCriteria sortCriteria = optionalSortCriteria.get();

                    sortField = sortCriteria.getFieldName();
                    sortType = sortCriteria.getSortType();
                    final JsonObject sortData = Json.object();

                    sortData.put(fieldKey, sortField);
                    sortData.put(typeKey, sortType.getOrderTypeName());
                    crmResponse.put(sortKey, sortData);
                }
            }
        } else {
            sortField = sortJson.optString(fieldKey, startTimeKey);
            sortType = SortType.getSortType(sortJson.optString(ActivityFields.TYPE.getName(), ActivityFields.ASC.getName()));

            activityKanbanSortConfig.setConfig(Json.array().put(sortJson).toString());
            sortManager.update(activityKanbanSortConfig);
        }

        if (data.containsKey(ActivityFields.FILTER_ID.getName()) || data.containsKey(ActivityFields.CRITERIA.getName()) || data.containsKey(ActivityFields.OWNER_ID.getName())) {
            filterCriteriaList.addAll(filterCriteriaBuilder.constructFilterCriteria(orgId, crmRequest.userId(), com.tlc.crm.entity.list.model.Provider.ACTIVITY_LIST, data));
        }

        final ActivityMobilePageHandler activityMobilePageHandler = new ActivityMobilePageHandler(start, limit, filterCriteriaList, sortType, sortField, status, activityTypeIds);

        activityMobilePageHandler.setOrgId(orgId);
        final long totalRecords = activityMobilePageHandler.getTotalRecordsCount();

        if (totalRecords > NumericalConstant.ZERO) {
            fetchAndDispatchActivities(totalRecords, activityMobilePageHandler.hasMore(),
                    sortField, sortType, activityMobilePageHandler.fetchPageData().getData(), crmRequest, crmResponse);
        } else {
            loadActivitiesIntoResponse(NumericalConstant.ZERO, false, Json.array(), crmResponse);
        }
    }

    /**
     * <p>
     * Fetches the users and dispatch the corresponding activities for kanban view
     * </p>
     *
     * @param totalRecords    Represents the total records
     * @param hasMore         Indicates if additional records are available for fetch the activities
     * @param sortField       Represents the sort field
     * @param sortType        Represents the {@link SortType}
     * @param activityHolders Represents the {@link ActivityHolder} details
     * @param request         Represents the {@link CrmRequest}
     * @param response        Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchActivities(final Long totalRecords, final boolean hasMore, final String sortField, final SortType sortType,
                                            final Collection<ActivityHolder> activityHolders, final CrmRequest request, final CrmResponse response) {
        final Collection<Long> userIds = activityHolders.stream()
                .map(ActivityHolder::getActivity).map(Activity::getAssignee).map(CrmUser::id).collect(Collectors.toUnmodifiableSet());
        final Map<Long, CrmUser> crmUsersById = crmUserService.getUsers(request.orgId(), userIds)
                .stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));

        setUsersForPreview(activityHolders, crmUsersById);
        loadActivitiesIntoResponse(totalRecords, hasMore,
                fetchGroupedActivities(activityHolders, sortField, sortType, request), response);
    }

    /**
     * <p>
     * Loads the activities response data such as total count, hasMore and activities to the CrmResponse
     * </p>
     *
     * @param totalRecords  Represents the total number of records
     * @param hasMore       Represents a boolean to determine if there are more records available
     * @param activities    Represents the {@link JsonArray} of activities
     * @param response      Represents the {@link CrmResponse} to which the data will be loaded
     */
    private void loadActivitiesIntoResponse(final long totalRecords, final boolean hasMore,
                                            final JsonArray activities, final CrmResponse response) {
        response.put(ActivityFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(ActivityFields.HAS_MORE.getName(), hasMore);
        response.put(ActivityFields.ACTIVITIES.getName(), activities);
    }

    /**
     * <p>
     * Fetches the {@link JsonArray} of activities grouped by the given sort field
     * </p>
     *
     * @param activityHolders Represents the {@link Collection} of {@link ActivityHolder}
     * @param sortField       Represents the sort field
     * @param sortType        Represents the {@link SortType}
     * @param crmRequest      Represents the {@link CrmRequest}
     * @return the {@link JsonArray}
     */
    private JsonArray fetchGroupedActivities(final Collection<ActivityHolder> activityHolders, final String sortField, final SortType sortType, final CrmRequest crmRequest) {
        final JsonArray jsonArray = Json.array();

        switch (sortField) {
            case "activityType", "status", "title" -> {
                final Map<String, Collection<ActivityHolder>> groupedActivities = new HashMap<>();

                for (final ActivityHolder activityHolder : activityHolders) {
                    final Activity activity = activityHolder.getActivity();

                    final String header = switch (sortField) {
                        case "activityType" -> activity.getType().getName();
                        case "status" -> activity.getStatus().name();
                        default -> activity.getTitle().toUpperCase().trim().substring(BEGIN_INDEX, END_INDEX);
                    };

                    groupedActivities.computeIfAbsent(header, activities -> new ArrayList<>()).add(activityHolder);
                }
                groupedActivities.entrySet().stream().sorted(sortType == SortType.ASCENDING ? Map.Entry.comparingByKey() : Map.Entry.<String, Collection<ActivityHolder>>comparingByKey().reversed())
                        .forEach(activities -> jsonArray.put(fetchActivityJson(activities.getKey(), activities.getValue())));
            }
            default -> {
                final Map<LocalDate, Collection<ActivityHolder>> groupedActivities = new HashMap<>();
                final TemporalPattern.DatePattern datePattern = TemporalPattern.DatePattern.DATE_1;
                final CrmUser crmUser = LoggedInUserInfo.getUser();
                final UserRegionalPreference userRegionalPreference = crmUser.getPreferenceHolder().get(UserRegionalPreference.TYPE);
                final TimezoneInfo timezoneInfo = userRegionalPreference.getTimezoneInfo();

                for (final ActivityHolder activityHolder : activityHolders) {
                    groupedActivities.computeIfAbsent(TemporalServiceProvider.get(getGroupHeader(sortField,
                            activityHolder.getActivity())).getZonedDate(timezoneInfo), activities -> new ArrayList<>()).add(activityHolder);
                }
                groupedActivities.entrySet().stream().sorted(sortType == SortType.ASCENDING ? Map.Entry.comparingByKey() : Map.Entry.<LocalDate, Collection<ActivityHolder>>comparingByKey().reversed())
                        .forEach(activities -> jsonArray.put(fetchActivityJson(DateTimeFormatter.ofPattern(datePattern.get()).format(activities.getKey()), activities.getValue())));
            }
        }

        return jsonArray;
    }

    /**
     * <p>
     * Gets the group header based on the sort field
     * </p>
     *
     * @param sortField Represents the sort field
     * @param activity  Represents the {@link Activity}
     * @return the group header
     */
    private Long getGroupHeader(final String sortField, final Activity activity) {
        return switch (sortField) {
            case "createdTime" -> activity.getCreatedTime().getTimeStamp();
            case "endTime" -> activity.getEndTime().getTimeStamp();
            case "modifiedTime" -> activity.getUpdatedTime().getTimeStamp();
            case "completedTime" -> activity.getCompletedTime().getTimeStamp();
            case "lastNotificationTime" -> activity.getLastNotificationTime().getTimeStamp();
            case "duration" -> activity.getDuration().longValue();
            default -> activity.getStartTime().getTimeStamp();
        };
    }

    /**
     * <p>
     * Fetches the {@link JsonObject} of grouped activities
     * </p>
     *
     * @param groupHeader Represents the group header
     * @param activities  Represents the grouped activities
     * @return {@link JsonObject}
     */
    private JsonObject fetchActivityJson(final String groupHeader, final Collection<ActivityHolder> activities) {
        final JsonObject jsonObject = Json.object();
        final JsonArray activityArray = Json.array();

        activities.forEach(activityHolder -> activityArray.put(sourceActivityConverter.convertKanbanActivities(activityHolder)));

        jsonObject.put(ActivityFields.GROUP_HEADER.getName(), groupHeader);
        jsonObject.put(ActivityFields.DATA.getName(), activityArray);

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @return The config action name
     */
    @Override
    protected String getConfigActionName() {
        return CONFIG_ACTION_NAME;
    }

    /**
     * <p>
     * Stores the check-in and check-out record
     * </p>
     *
     * @param orgId         Represents the organisation id
     * @param data          Represents the data json
     * @param crmResponse   Represents the {@link CrmResponse}
     * @param isCheckIn     Represents whether its check-in or check-out
     */
    private void storeCheckInOutRecord(final Long orgId, final Long userId, final JsonObject data, final CrmResponse crmResponse, final boolean isCheckIn) {
        final long activityId = data.getLong(ActivityFields.ID.getName());

        if (sourceActivityManager.exists(orgId, activityId)) {

            if (!sourceActivityManager.isUserActivityAssignee(orgId, userId, activityId)) {
                throw ErrorCode.get(ActivityErrorCodes.UNSUPPORTED_ACTION, "i18n.activity.unsupported.action");
            }
            final CheckInOutRecord checkInOutRecord = activityConvert.convertCheckInOutDetails(data);

            checkInOutRecord.setOrgId(orgId);
            checkInOutRecord.setUserId(userId);
            checkInOutRecord.setCheckIn(isCheckIn);

            final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
            final ErrorCodeProvider errorCodeProvider = CommonErrorCodes.DATA_VALIDATION_FAILED;
            final Set<ConstraintViolation<CheckInOutRecord>> violations = validator.validate(checkInOutRecord, Group.Create.class);
            final Set<ErrorDescriptor> errors = violations.stream().map(violation -> new ErrorDescriptor(ErrorCode.get(errorCodeProvider,
                            violation.getMessage()), null, violation.getPropertyPath().toString())).collect(Collectors.toSet());

            if (errors.isEmpty()) {
                checkInOutRecord.setDataJson(data);

                sourceActivityManager.storeCheckInOutRecord(checkInOutRecord);
                ActivityIndexManager.getInstance().updateCheckInOutInfo(orgId, activityId, isCheckIn, checkInOutRecord.getTime(), checkInOutRecord.getLocation());
                final String message = isCheckIn ? "i18n.activity.check.in.message" : "i18n.activity.check.out.message";

                crmResponse.put(ActivityFields.MESSAGE.getName(), crmResponse.resolveI18nString(message));
            } else {
                loadValidationErrors(Action.CREATE, crmResponse, errors);
            }
        } else {
            throw ErrorCode.get(ActivityErrorCodes.ACTIVITY_NOT_FOUND, "i18n.activity.not.found");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param request Represents the {@link CrmRequest}
     * @return The collection of models.
     */
    @Override
    public Collection<TlcModel> getCurrentModels(final CrmRequest request) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(ActionFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(ActionFields.DATA);
        final long orgId = request.orgId();

        return switch (type) {
            case "update", "pUpdate", "delete" -> {
                if (data.containsKey(ActionFields.ID)) {
                    yield List.of(sourceActivityManager.get(orgId, data.getLong(ActionFields.ID)));
                }
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }

    /**
     * <p>
     * Gets the logged-in user's {@link TimezoneInfo} from the regional preference.
     * {@code crmRequest.timeZone()} is always UTC, so it must not be used for day/month calculations
     * </p>
     *
     * @return The user's {@link TimezoneInfo}
     */
    private TimezoneInfo getUserTimezoneInfo() {
        final UserRegionalPreference userRegionalPreference = ((CrmUser) LoggedInUserInfo.getUser()).getPreferenceHolder().get(UserPreferenceType.USER_REGIONAL_PREFERENCE);

        return userRegionalPreference.getTimezoneInfo();
    }

    /**
     * <p>
     * Gets the requested date from the UTC timestamp sent by the client.
     * The timestamp is only a date label (UTC midnight), so it is read in UTC
     * </p>
     *
     * @param requestedTime Represents the requested time in UTC milliseconds
     * @return The requested {@link LocalDate}
     */
    private LocalDate getRequestedDate(final long requestedTime) {
        return Instant.ofEpochMilli(requestedTime).atZone(ZoneOffset.UTC).toLocalDate();
    }

    /**
     * <p>
     * Gets the start of the given date in the user's time zone as UTC milliseconds
     * </p>
     *
     * @param date   Represents the {@link LocalDate}
     * @param zoneId Represents the user's {@link ZoneId}
     * @return The UTC milliseconds of the start of the day in the user's time zone
     */
    private long getStartOfDayInMillis(final LocalDate date, final ZoneId zoneId) {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli();
    }
}