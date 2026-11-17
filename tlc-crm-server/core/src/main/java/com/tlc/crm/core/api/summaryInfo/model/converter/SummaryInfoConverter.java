package com.tlc.crm.core.api.summaryInfo.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.deal.model.DealStatistics;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;
import com.tlc.crm.core.field.summary.SummaryFields;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.features.api.tag.model.converter.TagConverter;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.l10n.temporal.resolver.DateUtil;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link SummaryInfo}
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class SummaryInfoConverter {

    private final TagConverter tagConverter;
    private final DateUtil dateUtil;

    private SummaryInfoConverter() {
        tagConverter = TagConverter.getInstance();
        dateUtil = DateUtil.getInstance();
    }

    private static class Instance {
        private static final SummaryInfoConverter INSTANCE = new SummaryInfoConverter();
    }

    /**
     * <p>
     * Gets the {@link SummaryInfoConverter} Instance
     * </p>
     *
     * @return {@link SummaryInfoConverter}
     */
    public static SummaryInfoConverter getInstance() {
        return SummaryInfoConverter.Instance.INSTANCE;
    }

    /**
     * <p>
     * Builds the {@link Tag} entity from the given json objects
     * </p>
     *
     * @param fieldId         Represents the field id as String to obtain the tag values from the Json
     * @param summaryInfo     Represents the {@link SummaryInfo}
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param jsonObject      Holds the field values of tag
     */
    public SummaryInfo buildTags(final String fieldId, final SummaryInfo summaryInfo,
                                 final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Collection<Tag> tags = tagConverter.buildTags(fieldId, converterConfig, jsonObject);

        if (Objects.isNull(summaryInfo)) {
            final SummaryInfo summary = new SummaryInfo();

            summary.setTags(tags);

            return summary;
        } else {
            summaryInfo.setTags(tags);
        }

        return summaryInfo;
    }

    /**
     * Converts the {@link SummaryInfo} into the {@link JsonObject}.
     *
     * @param summaryInfo Refers the {@link SummaryInfo} details.
     * @return The json filled with summary details.
     */
    public JsonObject convert(final SummaryInfo summaryInfo) {
        final JsonObject summaryJson = Json.object();

        if (Objects.nonNull(summaryInfo)) {
            final ActivitySummary activitySummary = summaryInfo.getActivitySummary();
            final EmailSummary emailSummary = summaryInfo.getEmailSummary();
            final DealStatistics dealStatistics = summaryInfo.getDealStatistics();
            final Collection<Tag> tags = summaryInfo.getTags();

            if (Objects.nonNull(activitySummary)) {
                final JsonObject activitySummaryJson = Json.object();

                activitySummaryJson.put(SummaryFields.LAST_ACTIVITY_DATE, dateUtil.getTimeStamp(activitySummary.getLastActivityDate()));
                activitySummaryJson.put(SummaryFields.NEXT_ACTIVITY_DATE, dateUtil.getTimeStamp(activitySummary.getNextActivityDate()));
                activitySummaryJson.put(SummaryFields.LAST_ACTIVITY_TYPE, activitySummary.getLastActivityType());
                activitySummaryJson.put(SummaryFields.DONE_ACTIVITIES, activitySummary.getDoneActivities());
                activitySummaryJson.put(SummaryFields.UPCOMING_ACTIVITIES, activitySummary.getUpcomingActivities());
                activitySummaryJson.put(SummaryFields.TOTAL_ACTIVITIES, activitySummary.getTotalActivities());
                activitySummaryJson.put(SummaryFields.PENDING_ACTIVITIES, activitySummary.getPendingActivities());

                summaryJson.put(SummaryFields.ACTIVITY_SUMMARY, activitySummaryJson);
            }

            if (Objects.nonNull(emailSummary)) {
                final JsonObject emailSummaryJson = Json.object();

                emailSummaryJson.put(SummaryFields.EMAIL_MESSAGE_COUNT, emailSummary.getEmailMessageCount());
                emailSummaryJson.put(SummaryFields.LAST_EMAIL_RECEIVED, dateUtil.getTimeStamp(emailSummary.getLastEmailReceived()));
                emailSummaryJson.put(SummaryFields.LAST_EMAIL_SENT, dateUtil.getTimeStamp(emailSummary.getLastEmailSent()));

                summaryJson.put(SummaryFields.EMAIL_SUMMARY, emailSummaryJson);
            }

            if (Objects.nonNull(dealStatistics)) {
                final JsonObject dealStatisticsJson = Json.object();

                dealStatisticsJson.put(SummaryFields.OPEN_DEALS_COUNT, dealStatistics.getOpenDealsCount());
                dealStatisticsJson.put(SummaryFields.CLOSED_DEALS_COUNT, dealStatistics.getClosedDealsCount());
                dealStatisticsJson.put(SummaryFields.WON_DEALS_COUNT, dealStatistics.getWonDealsCount());
                dealStatisticsJson.put(SummaryFields.LOST_DEALS_COUNT, dealStatistics.getLostDealsCount());

                summaryJson.put(SummaryFields.DEAL_STATISTICS, dealStatisticsJson);
            }

            if (Objects.nonNull(tags) && !tags.isEmpty()) {
                summaryJson.put(SummaryFields.TAGS, tagConverter.convertTagsForSummary(tags));
            }
            summaryJson.put(SummaryFields.WEB_FORM, summaryInfo.getWebFormName());

        }

        return summaryJson;
    }

    /**
     * Converts the {@link JsonObject} into {@link SummaryInfo}.
     *
     * @param converterConfig Refers the {@link ConverterConfig}.
     * @param summaryJson     Refers the summary {@link JsonObject}.
     * @return {@link SummaryInfo}.
     */
    public SummaryInfo convert(final ConverterConfig converterConfig, final JsonObject summaryJson) {
        final SummaryInfo summaryInfo = new SummaryInfo();

        if (summaryJson.containsKey(SummaryFields.ACTIVITY_SUMMARY)) {
            final JsonObject activitySummaryJson = summaryJson.getJsonObject(SummaryFields.ACTIVITY_SUMMARY);
            final ActivitySummary activitySummary = new ActivitySummary();

            activitySummary.setDoneActivities(activitySummaryJson.containsKey(SummaryFields.DONE_ACTIVITIES)
                    ? activitySummaryJson.getLong(SummaryFields.DONE_ACTIVITIES) : null);
            activitySummary.setLastActivityDate(activitySummaryJson.containsKey(SummaryFields.LAST_ACTIVITY_DATE)
                    ? dateUtil.getTemporal(activitySummaryJson.getLong(SummaryFields.LAST_ACTIVITY_DATE)) : null);
            activitySummary.setPendingActivities(activitySummaryJson.containsKey(SummaryFields.PENDING_ACTIVITIES)
                    ? activitySummaryJson.getLong(SummaryFields.PENDING_ACTIVITIES) : null);
            activitySummary.setUpcomingActivities(activitySummaryJson.containsKey(SummaryFields.UPCOMING_ACTIVITIES)
                    ? activitySummaryJson.getLong(SummaryFields.UPCOMING_ACTIVITIES) : null);
            activitySummary.setNextActivityDate(activitySummaryJson.containsKey(SummaryFields.NEXT_ACTIVITY_DATE)
                    ? dateUtil.getTemporal(activitySummaryJson.getLong(SummaryFields.NEXT_ACTIVITY_DATE)) : null);
            activitySummary.setLastActivityType(activitySummaryJson.containsKey(SummaryFields.LAST_ACTIVITY_TYPE)
                    ? activitySummaryJson.getString(SummaryFields.LAST_ACTIVITY_TYPE) : null);

            summaryInfo.setActivitySummary(activitySummary);
        }

        if (summaryJson.containsKey(SummaryFields.DEAL_STATISTICS)) {
            final JsonObject dealStatisticsJson = summaryJson.getJsonObject(SummaryFields.DEAL_STATISTICS);
            final DealStatistics dealStatistics = new DealStatistics();

            dealStatistics.setLostDealsCount(dealStatisticsJson.optLong(SummaryFields.LOST_DEALS_COUNT, 0));
            dealStatistics.setOpenDealsCount(dealStatisticsJson.optLong(SummaryFields.OPEN_DEALS_COUNT, 0));
            dealStatistics.setWonDealsCount(dealStatisticsJson.optLong(SummaryFields.WON_DEALS_COUNT, 0));

            summaryInfo.setDealStatistics(dealStatistics);
        }

        if (summaryJson.containsKey(SummaryFields.EMAIL_SUMMARY)) {
            final JsonObject emailSummaryJson = summaryJson.getJsonObject(SummaryFields.EMAIL_SUMMARY);
            final EmailSummary emailSummary = new EmailSummary();

            emailSummary.setEmailMessageCount(emailSummaryJson.containsKey(SummaryFields.EMAIL_MESSAGE_COUNT)
                    ? emailSummaryJson.getLong(SummaryFields.EMAIL_MESSAGE_COUNT) : null);
            emailSummary.setLastEmailSent(emailSummaryJson.containsKey(SummaryFields.LAST_EMAIL_SENT)
                    ? dateUtil.getTemporal(emailSummaryJson.getLong(SummaryFields.LAST_EMAIL_SENT)) : null);
            emailSummary.setLastEmailReceived(emailSummaryJson.containsKey(SummaryFields.LAST_EMAIL_RECEIVED)
                    ? dateUtil.getTemporal(emailSummaryJson.getLong(SummaryFields.LAST_EMAIL_RECEIVED)) : null);

            summaryInfo.setEmailSummary(emailSummary);
        }

        if (summaryJson.containsKey(SummaryFields.TAGS)) {
            summaryInfo.setTags(tagConverter.convert(converterConfig, summaryJson.optJsonArray(SummaryFields.TAGS)));
        }
        summaryInfo.setWebFormName(summaryJson.optString(SummaryFields.WEB_FORM, ""));

        return summaryInfo;
    }
}
