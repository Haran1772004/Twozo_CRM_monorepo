package com.tlc.crm.core.action.deal;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.ForecastSummary;
import com.tlc.crm.core.api.deal.model.ForecastView;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.core.api.deal.manager.ForecastManager;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.ForecastArrangement;
import com.tlc.crm.core.api.deal.model.ForecastStage;
import com.tlc.crm.core.api.deal.model.ForecastInterval;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.field.deal.ForecastFields;
import com.tlc.crm.core.internal.deal.status.ForecastErrorCodes;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.date.DateIntervals;


import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.filter.FilterCriteriaBuilder;
import com.tlc.crm.entity.currency.BaseCurrencyService;

import com.tlc.l10n.temporal.TemporalPatternType;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.l10n.temporal.service.TemporalService;
import com.tlc.web.WebAction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Month;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the {@link Deal} forecast related web actions.
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.DEALS, validateFieldPermission = true)
@WebAction(path = "/deal/forecast")
public class ForecastAction extends AuthorizationController {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final long ALL_PIPELINE_ID = -1L;
    private static final JsonObject DEFAULT_CONFIGS = initializeDefaultConfigs();

    private final ForecastManager forecastManager;
    private final PipelineManager pipelineManager;
    private final DealConverter dealConverter;
    private final FilterCriteriaBuilder filterCriteriaBuilder;
    private final BaseCurrencyService baseCurrencyService;
    private final DateTimeUtil dateTimeUtil;

    public ForecastAction() {
        this.forecastManager = ForecastManager.getInstance();
        this.pipelineManager = PipelineManager.getInstance();
        this.dealConverter = DealConverter.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
        this.filterCriteriaBuilder = FilterCriteriaBuilder.getInstance();
        this.dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param request     Refers the {@link CrmRequest}.
     * @param crmResponse Refers the {@link CrmResponse}.
     * @throws Exception The exception to be thrown.
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(ForecastFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(ForecastFields.DATA);
        final Long orgId = request.orgId();
        final Long userId = request.userId();

        switch (type) {
            case "getConfig" -> getForecastViewControls(orgId, userId, crmResponse);
            case "intervalsOverview" -> intervalsOverview(orgId, userId, data, crmResponse);
            case "get" -> crmResponse.put(ForecastFields.FORECAST, getDealsForecast(orgId, userId, data));
        }
    }

    /**
     * Processes an overview of forecast intervals for a specific organization and user.
     *
     * @param orgId       The unique identifier of the organization.
     * @param userId      The unique identifier of the user.
     * @param data        A {@link JsonObject} containing the input data.
     * @param crmResponse The {@link CrmResponse} object to be populated with the resulting intervals overview
     */
    private void intervalsOverview(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final ForecastInterval forecastInterval = ForecastInterval.getForeCastIntervalById(data.optShort(ForecastFields.INTERVAL, ForecastInterval.MONTH.id()));
        final Collection<Pipeline> pipelines;

        if (data.containsKey(ForecastFields.PIPELINE_ID)) {
            final Long pipelineId = data.getLong(ForecastFields.PIPELINE_ID);

            if (Objects.equals(ALL_PIPELINE_ID, pipelineId)) {
                pipelines = pipelineManager.getAllPipelineByOrg(orgId);
            } else if (pipelineManager.exists(orgId, pipelineId)) {
                pipelines = pipelineManager.get(orgId, List.of(pipelineId));
            } else {
                throw ErrorCode.get(ForecastErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
            }
        } else {
            pipelines = pipelineManager.getAllPipelineByOrg(orgId);
        }
        crmResponse.put(ForecastFields.INTERVALS, groupOverviewByIntervals(orgId, userId, forecastInterval, pipelines, data));

        final Optional<ForecastView> optionalForecastView = forecastManager.getViewDetails(orgId, userId);
        final JsonObject jsonObject;
        final ForecastView forecastView;

        if (optionalForecastView.isPresent()) {
            forecastView = optionalForecastView.get();
            jsonObject = forecastView.getConfigAsJson();
        } else {
            jsonObject = Json.object();
            forecastView = new ForecastView(orgId, userId);

            jsonObject.put(ForecastFields.ARRANGE_BY, ForecastArrangement.OPEN_DEAL_FIRST.id());
        }

        jsonObject.put(ForecastFields.INTERVAL, forecastInterval.id());
        jsonObject.put(ForecastFields.COLUMNS, data.optShort(ForecastFields.COLUMNS, ForecastStage.FOUR_COLUMNS.id()));
        jsonObject.put(ForecastFields.SHOW_BY, data.getLong(ForecastFields.SHOW_BY));

        forecastView.setConfig(jsonObject.toString());
        forecastManager.updateViewDetails(forecastView);
        crmResponse.put(ForecastFields.VIEW_CONTROLS, jsonObject);
    }

    /**
     * Groups and aggregates overview data by the specified forecast interval.
     *
     * @param orgId     The unique identifier of the organization.
     * @param userId    The unique identifier of the user.
     * @param interval  The {@link ForecastInterval} specifying how the data should be grouped (e.g., weekly, monthly, quarterly).
     * @param pipelines The collection of {@link Pipeline} objects whose data will be included in the grouping.
     * @param data      A {@link JsonObject} containing any additional parameters or filters required for grouping.
     * @return A {@link JsonArray} representing the grouped overview data by the specified interval.
     */
    private JsonArray groupOverviewByIntervals(final long orgId, final long userId, final ForecastInterval interval,
                                               final Collection<Pipeline> pipelines, final JsonObject data) {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userRegionalPreference = crmUser.getPreferenceHolder().get(UserRegionalPreference.TYPE);
        final TimezoneInfo timezoneInfo = userRegionalPreference.getTimezoneInfo();
        final Calendar calendar = Calendar.getInstance(timezoneInfo.getTimeZone());
        final long startDate = data.optLong(ForecastFields.START_DATE, Instant.now().toEpochMilli());

        calendar.setTimeInMillis(startDate);

        return switch (interval) {
            case MONTH -> groupForecastByMonth(orgId, userId, calendar, data, pipelines, timezoneInfo);
            case WEEK -> groupForecastByWeek(orgId, userId, calendar, data, pipelines, timezoneInfo);
            case QUARTER -> groupForecastByQuarter(orgId, userId, calendar, data, pipelines, timezoneInfo);
        };
    }

    /**
     * <p>
     * Gets the {@link JsonArray} which holds the deals for forecast.
     * </p>
     *
     * @param orgId  Refers the organization id.
     * @param userId Refers the user id.
     * @param data   Which contains the request json.
     * @return The {@link JsonArray} which contains the deals forecast details.
     */
    private JsonObject getDealsForecast(final long orgId, final long userId, final JsonObject data) {
        final Collection<Pipeline> pipelines;

        if (data.containsKey(ForecastFields.PIPELINE_ID)) {
            final Long pipelineId = data.getLong(ForecastFields.PIPELINE_ID);

            if (Objects.equals(pipelineId, ALL_PIPELINE_ID)) {
                pipelines = pipelineManager.getAllPipelineByOrg(orgId);
                pipelineManager.loadUserForecastPreference(orgId, userId, null);
            } else if (pipelineManager.exists(orgId, pipelineId)) {
                pipelines = pipelineManager.get(orgId, List.of(pipelineId));
                pipelineManager.loadUserForecastPreference(orgId, userId, pipelineId);
            } else {
                throw ErrorCode.get(ForecastErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
            }
        } else {
            pipelines = pipelineManager.getAllPipelineByOrg(orgId);
        }

        return getDealsForecast(orgId, userId, data, new Interval<>(data.getLong(ForecastFields.START_DATE), data.getLong(ForecastFields.END_DATE)), pipelines);
    }

    /**
     * <p>
     * Groups the deals according to the interval-month.
     * </p>
     *
     * @param orgId        Refers the organization id.
     * @param userId       Refers the user id.
     * @param calendar     Refers the {@link Calendar} object.
     * @param data         Which contains the request json.
     * @param pipelines    Which contains the {@link Collection} of pipelines.
     * @param timezoneInfo Refers the {@link TimezoneInfo}.
     * @return The {@link JsonArray} of deals grouped by month.
     */
    private JsonArray groupForecastByMonth(final long orgId, final long userId, final Calendar calendar,
                                           final JsonObject data, final Collection<Pipeline> pipelines, final TimezoneInfo timezoneInfo) {
        final int noOfColumns = ForecastStage.getForecastStageById(data.optShort(ForecastFields.COLUMNS, ForecastStage.FOUR_COLUMNS.id())).getValue();
        final JsonArray jsonArray = Json.array();

        for (int size = 0; size < noOfColumns; size++) {
            final Month presentMonth = Month.of(calendar.get(Calendar.MONTH) + 1);
            final Interval<Long, Long> interval = getStartAndEndDate(calendar, presentMonth);
            final JsonObject jsonObject = getSummary(orgId, userId, data, interval, pipelines);

            jsonObject.put(ForecastFields.TITLE, String.format("%s %d", TemporalServiceProvider.get(interval.start())
                    .convertDateToString(timezoneInfo, TemporalPatternType.ABBREVIATED_MONTH), calendar.get(Calendar.YEAR)));
            jsonObject.put(ForecastFields.START_DATE, interval.start());
            jsonObject.put(ForecastFields.END_DATE, interval.end());
            jsonArray.put(jsonObject);

            calendar.setTimeInMillis(interval.end());
            calendar.add(Calendar.DATE, 1);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Groups the deals according to the interval-week.
     * </p>
     *
     * @param orgId        Refers the organization id.
     * @param userId       Refers the user id.
     * @param calendar     Refers the {@link Calendar} object.
     * @param data         Which contains the request json.
     * @param pipelines    Which contains the {@link Collection} of pipelines.
     * @param timezoneInfo Refers the {@link TimezoneInfo}.
     * @return The {@link JsonArray} of deals grouped by week.
     */
    private JsonArray groupForecastByWeek(final long orgId, final long userId, final Calendar calendar,
                                          final JsonObject data, final Collection<Pipeline> pipelines, final TimezoneInfo timezoneInfo) {
        final int noOfColumns = ForecastStage.getForecastStageById(data.optShort(ForecastFields.COLUMNS, ForecastStage.FOUR_COLUMNS.id())).getValue();
        final JsonArray jsonArray = Json.array();
        final int weekOfYearKey = Calendar.WEEK_OF_YEAR;
        final int year = Calendar.YEAR;
        int currentWeek = calendar.get(weekOfYearKey);

        for (int size = 0; size < noOfColumns; size++) {
            final Interval<Long, Long> interval = getStartAndEndDate(calendar, currentWeek);
            final int currentYear = calendar.get(year);
            final JsonObject jsonObject = getSummary(orgId, userId, data, interval, pipelines);
            final String weekStartDate = TemporalServiceProvider.get(interval.start()).convertDateToString(timezoneInfo, TemporalPatternType.ABBREVIATED_DATE);
            final String weekEndDate = TemporalServiceProvider.get(interval.end()).convertDateToString(timezoneInfo, TemporalPatternType.ABBREVIATED_DATE);

            jsonObject.put(ForecastFields.TITLE, String.format("%s - %s %d (W %d)", weekStartDate, weekEndDate, currentYear, currentWeek));
            jsonObject.put(ForecastFields.START_DATE, interval.start());
            jsonObject.put(ForecastFields.END_DATE, interval.end());
            jsonArray.put(jsonObject);

            if (Objects.equals(currentWeek, calendar.getActualMaximum(weekOfYearKey))) {
                calendar.set(calendar.get(year) + 1, Calendar.JANUARY, calendar.getActualMinimum(Calendar.DATE),
                        calendar.getActualMinimum(Calendar.HOUR), calendar.getActualMinimum(Calendar.MINUTE),
                        calendar.getActualMinimum(Calendar.SECOND));
                currentWeek = calendar.get(weekOfYearKey);
            } else {
                currentWeek += 1;
            }
        }

        return jsonArray;
    }

    /**
     * <p>
     * Groups the deals according to the interval-quarter.
     * </p>
     *
     * @param orgId        Refers the organization id.
     * @param userId       Refers the user id.
     * @param calendar     Refers the {@link Calendar} object.
     * @param data         Which contains the request json.
     * @param pipelines    Which contains the {@link Collection} of pipelines.
     * @param timezoneInfo Refers the {@link TimezoneInfo}.
     * @return The {@link JsonArray} of deals grouped by quarter.
     */
    private JsonArray groupForecastByQuarter(final long orgId, final long userId, final Calendar calendar, final JsonObject data,
                                             final Collection<Pipeline> pipelines, final TimezoneInfo timezoneInfo) {
        final int noOfColumns = ForecastStage.getForecastStageById(data.optShort(ForecastFields.COLUMNS, ForecastStage.FOUR_COLUMNS.id())).getValue();
        final JsonArray jsonArray = Json.array();

        for (int size = 0; size < noOfColumns; size++) {
            final int presentQuarterInAYear = (calendar.get(Calendar.MONTH) / 3) + 1;
            final Interval<Long, Long> interval = getStartAndEndDate(calendar, null);
            final JsonObject jsonObject = getSummary(orgId, userId, data, interval, pipelines);
            final String quarterStartMonth = TemporalServiceProvider.get(interval.start()).convertDateToString(timezoneInfo, TemporalPatternType.ABBREVIATED_MONTH);
            final String quarterEndMonth = TemporalServiceProvider.get(interval.end()).convertDateToString(timezoneInfo, TemporalPatternType.ABBREVIATED_MONTH);

            jsonObject.put(ForecastFields.TITLE, String.format("%s - %s %d (Q %d)", quarterStartMonth, quarterEndMonth,
                    calendar.get(Calendar.YEAR), presentQuarterInAYear));
            jsonObject.put(ForecastFields.START_DATE, interval.start());
            jsonObject.put(ForecastFields.END_DATE, interval.end());
            jsonArray.put(jsonObject);

            calendar.setTimeInMillis(interval.end());
            calendar.add(Calendar.DATE, 1);
        }

        return jsonArray;
    }

    /**
     * Retrieves or computes a summary of data for the specified organization and user
     * within a given interval and across the provided pipelines.
     *
     * @param orgId     The unique identifier of the organization.
     * @param userId    The unique identifier of the user.
     * @param data      A {@link JsonObject} containing filters, parameters, or additional data required to generate the summary.
     * @param interval  An {@link Interval} representing the start and end values.
     * @param pipelines The collection of {@link Pipeline}.
     * @return A {@link JsonObject} containing the computed summary for the given parameters.
     */
    private JsonObject getSummary(final long orgId, final long userId, final JsonObject data,
                                  final Interval<Long, Long> interval, final Collection<Pipeline> pipelines) {
        final JsonObject jsonObject = Json.object();
        final Set<Long> pipelineIds = new HashSet<>();
        final Set<Long> pipelineStageIds = new HashSet<>();
        final Collection<FilterCriteria> filterCriteriaList = new ArrayList<>();
        final Long fieldId = data.getLong(ForecastFields.SHOW_BY);

        if (data.containsKey(ForecastFields.FILTER_ID) || data.containsKey(ForecastFields.CRITERIA)
                || data.containsKey(ForecastFields.OWNER_ID)) {
            filterCriteriaList.addAll(filterCriteriaBuilder.constructFilterCriteria(orgId, userId, Provider.DEAL_LIST, data));
        }

        for (final Pipeline pipeline : pipelines) {
            pipelineIds.add(pipeline.id());
            pipelineStageIds.addAll(pipeline.getPipelineStages().stream().map(PipelineStage::id)
                    .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet()));
        }
        final ForecastSummary forecastSummary = forecastManager.getSummary(orgId, fieldId, pipelineIds, pipelineStageIds, interval, filterCriteriaList);

        jsonObject.put(ForecastFields.TOTAL_SUMMARY, getTotalSummaryJson(orgId, forecastSummary));

        return jsonObject;
    }

    /**
     * <p>
     * Gets the {@link JsonObject} which has the deals grouped by the interval and
     * the summary of deal values.
     * </p>
     *
     * @param orgId     Refers the organization id.
     * @param userId    Refers the user id.
     * @param data      Which contains the request json.
     * @param interval  Refers the {@link Interval} of start and end dates.
     * @param pipelines Which contains the {@link Collection} of pipelines.
     * @return The {@link JsonObject} which has the forecast details.
     */
    private JsonObject getDealsForecast(final long orgId, final long userId, final JsonObject data,
                                        final Interval<Long, Long> interval, final Collection<Pipeline> pipelines) {
        final short arrangeBy = ForecastArrangement.getForecastArrangementById(data.getShort(ForecastFields.ARRANGE_BY)).id();
        final Long fieldId = data.getLong(ForecastFields.SHOW_BY);
        final int start = Math.max(data.optInt(DealFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(DealFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final JsonArray dealArray = Json.array();
        final JsonObject jsonObject = Json.object();
        final Set<Long> pipelineIds = new HashSet<>();
        final Set<Long> pipelineStageIds = new HashSet<>();
        final String state = Deal.State.getStateById(arrangeBy).getName();
        final Collection<FilterCriteria> filterCriteriaList = new ArrayList<>();

        if (data.containsKey(ForecastFields.FILTER_ID) || data.containsKey(ForecastFields.CRITERIA)
                || data.containsKey(ForecastFields.OWNER_ID)) {
            filterCriteriaList.addAll(filterCriteriaBuilder.constructFilterCriteria(orgId, userId, Provider.DEAL_LIST, data));
        }

        for (final Pipeline pipeline : pipelines) {
            pipelineIds.add(pipeline.id());
            pipelineStageIds.addAll(pipeline.getPipelineStages().stream().map(PipelineStage::id)
                    .filter(Objects::nonNull).collect(Collectors.toSet()));
        }
        final long totalCount = forecastManager.getTotalCount(orgId, pipelineIds, pipelineStageIds, fieldId, interval, filterCriteriaList);

        jsonObject.put(ForecastFields.TOTAL_RECORDS, totalCount);
        jsonObject.put(ForecastFields.HAS_MORE, (start + limit) <= totalCount);

        if (totalCount > 0 && start <= totalCount) {
            forecastManager.getViewDetails(orgId, userId).ifPresent(forecastView -> {
                final JsonObject configAsJson = forecastView.getConfigAsJson();

                configAsJson.put(ForecastFields.SHOW_BY, fieldId);
                configAsJson.put(ForecastFields.ARRANGE_BY, arrangeBy);
                forecastView.setConfig(configAsJson.toString());

                forecastManager.updateViewDetails(forecastView);
            });

            final Map<Deal, Collection<Activity>> dealActivities = forecastManager.getDealsByPipelineAndStage(orgId,
                    pipelineIds, pipelineStageIds, fieldId, interval, state, start, limit, filterCriteriaList);

            for (final Map.Entry<Deal, Collection<Activity>> dealActivity : dealActivities.entrySet()) {
                final Deal deal = dealActivity.getKey();
                final JsonObject dealJson = dealConverter.convertPartialJsonForDealForecast(deal);

                loadPartialDataIntoJson(deal.getPrimaryContact(), dealJson);
                dealJson.put(ActivityFields.ICON.getName(), getActivityIconType(dealActivities.get(deal)));
                dealArray.put(dealJson);
            }
        }
        jsonObject.put(ForecastFields.DEALS, dealArray);

        return jsonObject;
    }

    /**
     * <p>
     * Populates the given JSON object with partial data of the primary contact.
     * </p>
     *
     * @param primaryContact    Primary contact of a Deal
     * @param dataJson          JSON of the deal object
     */
    private void loadPartialDataIntoJson(final Contact primaryContact, final JsonObject dataJson) {

        if (Objects.nonNull(primaryContact)) {
            final String idKey = DealFields.ID;
            final String nameKey = DealFields.NAME;
            final JsonObject previewJson = Json.object();
            final Long contactId = primaryContact.id();

            previewJson.put(nameKey, primaryContact.getFullName());
            previewJson.put(idKey, contactId);
            final Email primaryEmail = primaryContact.getPrimaryEmail();
            final Phone primaryPhone = primaryContact.getPrimaryPhone();

            if (Objects.nonNull(primaryPhone)) {
                final JsonObject phoneJson = Json.object();

                phoneJson.put(idKey, primaryPhone.id());
                phoneJson.put(ContactFields.NUMBER, primaryPhone.getNumberWithCode());

                previewJson.put(ContactFields.PRIMARY_PHONE, phoneJson);
            }

            if (Objects.nonNull(primaryEmail)) {
                final JsonObject emailJson = Json.object();

                emailJson.put(idKey, primaryEmail.id());
                emailJson.put(ContactFields.TYPE, primaryEmail.getType());
                emailJson.put(ContactFields.VALUE, primaryEmail.getValue());

                previewJson.put(ContactFields.PRIMARY_EMAIL, emailJson);
            }
            dataJson.put(DealFields.PARTIAL_DATA, previewJson);
        }
    }

    /**
     * <p>
     * Sets the activity icon type related to the deal.
     * </p>
     *
     * @param activities Represents the collection of {@link Activity}
     * @return the activity icon type related to the deal.
     */
    private String getActivityIconType(final Collection<Activity> activities) {
        final String activityIconType;
        final Interval<Long, Long> interval = DateIntervals.TODAY.getInterval();

        if (activities.isEmpty()) {
            activityIconType = ActivityFields.NO_ACTIVITIES.getName();
        } else {
            final long currentTime = Instant.now().toEpochMilli();
            final Counter overDueActivitiesCount = new Counter();
            final Counter todayActivitiesCount = new Counter();
            final long startTime = interval.start();
            final long endTime = interval.end();

            for (final Activity activity : activities) {
                final Long activityStartTime = dateTimeUtil.getTimeStamp(activity.getStartTime());
                final Long activityEndTime = dateTimeUtil.getTimeStamp(activity.getEndTime());

                if (activityEndTime < currentTime) {
                    overDueActivitiesCount.getAndIncrement();
                    break;
                } else if (activityStartTime >= startTime && activityStartTime <= endTime) {
                    todayActivitiesCount.getAndIncrement();
                }
            }

            if (overDueActivitiesCount.getCount() > 0) {
                activityIconType = ActivityFields.OVER_DUE.getName();
            } else if (todayActivitiesCount.getCount() > 0) {
                activityIconType = ActivityFields.SCHEDULED.getName();
            } else {
                activityIconType = ActivityFields.UPCOMING_ACTIVITIES.getName();
            }
        }

        return activityIconType;
    }

    /**
     * <p>
     * Gets the details of total summary in {@link JsonObject}.
     * </p>
     *
     * @param orgId           Represents an organization.
     * @param forecastSummary Represents the {@link ForecastSummary}
     * @return The {@link JsonObject} of total summary details.
     */
    private JsonObject getTotalSummaryJson(final long orgId, final ForecastSummary forecastSummary) {
        final BigDecimal totalWonDealsValue = forecastSummary.totalWonDealsValue();
        final BigDecimal totalOpenDealsValue = forecastSummary.totalOpenDealsValue();
        final JsonObject totalSummary = Json.object();
        final Currency baseCurrency = baseCurrencyService.getDefaultCurrency(orgId);
        final JsonObject currencyJson = Json.object();

        currencyJson.put(ForecastFields.CODE, baseCurrency.getCode());
        currencyJson.put(ForecastFields.SYMBOL, Currency.getSymbol(baseCurrency.getCode()));

        totalSummary.put(ForecastFields.CURRENCY, currencyJson);
        totalSummary.put(ForecastFields.WON_VALUE, totalWonDealsValue);
        totalSummary.put(ForecastFields.OPEN_VALUE, totalOpenDealsValue);
        totalSummary.put(ForecastFields.TOTAL_VALUE, totalWonDealsValue.add(totalOpenDealsValue));
        totalSummary.put(ForecastFields.TOTAL_DEALS, forecastSummary.totalDeals());

        return totalSummary;
    }

    /**
     * <p>
     * Gets the start date of specified interval in milliseconds.
     * </p>
     *
     * @param calendar The {@link Calendar} instance.
     * @param interval Refers the {@link ForecastInterval}.
     * @return The start date.
     */
    private Long getStartDateValue(final Calendar calendar, final ForecastInterval interval) {
        switch (interval) {
            case MONTH -> calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
            case WEEK -> calendar.set(Calendar.DAY_OF_WEEK, calendar.getActualMinimum(Calendar.DAY_OF_WEEK));
            case QUARTER -> {
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
                calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) / 3 * 3);
            }
            default -> throw ErrorCode.get(ForecastErrorCodes.INVALID_FORECAST_INTERVAL);
        }

        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));

        return calendar.getTimeInMillis();
    }

    /**
     * <p>
     * Gets the end date of specified interval in milliseconds.
     * </p>
     *
     * @param calendar The {@link Calendar} instance.
     * @param interval Refers the {@link ForecastInterval}.
     * @return The end date.
     */
    private Long getEndDateValue(final Calendar calendar, final ForecastInterval interval) {
        switch (interval) {
            case MONTH -> calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            case WEEK -> calendar.set(Calendar.DAY_OF_WEEK, calendar.getActualMaximum(Calendar.DAY_OF_WEEK));
            case QUARTER -> {
                calendar.set(Calendar.MONTH, (calendar.get(Calendar.MONTH) / 3 * 3) + 2);
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            }
            default -> throw ErrorCode.get(ForecastErrorCodes.INVALID_FORECAST_INTERVAL);
        }

        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMaximum(Calendar.MILLISECOND));

        return calendar.getTimeInMillis();
    }

    /**
     * <p>
     * Gets the start date and end date.
     * </p>
     *
     * @param calendar The {@link Calendar} object.
     * @param value    Represents the interval value.
     * @return {@link Interval} of start and end date.
     */
    private Interval<Long, Long> getStartAndEndDate(final Calendar calendar, final Object value) {
        final ForecastInterval forecastInterval;

        if (value instanceof final Month month) {
            calendar.set(Calendar.MONTH, month.getValue() - 1);
            forecastInterval = ForecastInterval.MONTH;
        } else if (value instanceof final Integer integer) {
            calendar.set(Calendar.WEEK_OF_YEAR, integer);
            forecastInterval = ForecastInterval.WEEK;
        } else {
            forecastInterval = ForecastInterval.QUARTER;
        }
        final Long startDate = getStartDateValue(calendar, forecastInterval);
        final Long endDate = getEndDateValue(calendar, forecastInterval);

        return new Interval<>(startDate, endDate);
    }

    /**
     * Fetches the forecast view control configurations for the specified user and organization.
     * If a saved configuration exists, it is returned; otherwise, the default forecast
     * configuration is used.
     *
     * @param orgId       The unique identifier of the organization.
     * @param userId      The unique identifier of the user.
     * @param crmResponse The {@link CrmResponse} to populate with the forecast view controls.
     */
    private void getForecastViewControls(final Long orgId, final Long userId, final CrmResponse crmResponse) {
        final Optional<ForecastView> optionalForecastView = forecastManager.getViewDetails(orgId, userId);

        optionalForecastView.ifPresentOrElse(forecastView -> crmResponse.put(ForecastFields.VIEW_CONTROLS, forecastView.getConfigAsJson()),
                () -> crmResponse.put(ForecastFields.VIEW_CONTROLS, getDefaultConfigs()));
    }

    /**
     * Retrieves the default forecast view control configurations.
     *
     * @return A {@link JsonObject} containing the default forecast interval, column count,
     *         arrangement mode, and grouping date field.
     */
    private JsonObject getDefaultConfigs() {
        return DEFAULT_CONFIGS;
    }

    /**
     * Initializes the default forecast view control configurations with preset values
     * for the forecast interval, column count, arrangement mode, and grouping date field.
     *
     * @return A {@link JsonObject} containing the initialized default forecast configurations.
     */
    private static JsonObject initializeDefaultConfigs() {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(ForecastFields.INTERVAL, ForecastInterval.MONTH.id());
        jsonObject.put(ForecastFields.COLUMNS, ForecastStage.FOUR_COLUMNS.id());
        jsonObject.put(ForecastFields.ARRANGE_BY, ForecastArrangement.OPEN_DEAL_FIRST.id());

        final Optional<FieldInfo> dealFields = FieldService.getInstance().getDefaultFieldByAliasName(Source.DEAL, DealFields.DEAL_CREATED);

        dealFields.ifPresent(createdAt -> jsonObject.put(ForecastFields.SHOW_BY, createdAt.id()));

        return jsonObject;
    }

}
