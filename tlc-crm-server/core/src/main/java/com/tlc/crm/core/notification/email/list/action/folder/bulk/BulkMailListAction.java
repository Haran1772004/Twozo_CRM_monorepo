package com.tlc.crm.core.notification.email.list.action.folder.bulk;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.notification.email.bulk.api.model.BulkMail;
import com.tlc.crm.core.notification.email.bulk.api.model.MailInfo;
import com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics;
import com.tlc.crm.core.notification.email.bulk.fields.BulkMailFields;
import com.tlc.email.common.service.EmailSanitizeService;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Handles and Manages the HTTP requests and retrieves a list of {@link BulkMail}.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AuthorizationController
 */
public abstract class BulkMailListAction extends AuthorizationController {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;

    private final EmailSanitizeService emailSanitizeService;

    public BulkMailListAction() {
        this.emailSanitizeService = EmailSanitizeService.getInstance();
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
        final JsonObject data = requestJson.getJsonObject(BulkMailFields.DATA.getName());
        final int start = Math.max(data.optInt(BulkMailFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(BulkMailFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final long orgId = crmRequest.orgId();
        final long userId = crmRequest.userId();
        final JsonArray jsonArray = Json.array();
        final long totalRecords = getTotalBulkMailCount(orgId, userId);

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<BulkMail> bulkMails = fetchBulkMails(orgId, userId, start, limit);

            if (Objects.nonNull(bulkMails)) {

                for (final BulkMail bulkMail : bulkMails) {
                    jsonArray.put(convert(bulkMail, crmResponse));
                }
            }
        }
        crmResponse.put(BulkMailFields.LIST.getName(), jsonArray);
        crmResponse.put(BulkMailFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
        crmResponse.put(BulkMailFields.TOTAL_RECORDS.getName(), totalRecords);
    }

    /**
     * Retrieves the total count of {@link BulkMail} associated with the provided organization id and user id.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @return the total count of {@link BulkMail}
     */
    protected abstract long getTotalBulkMailCount(final long orgId, final long userId);

    /**
     * Retrieves a {@link Collection} of {@link BulkMail} associated
     * with the provided organization id, applying start and limit indices to fetch the records.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @param start  Represents the lower bound of {@link BulkMail} to be fetched
     * @param limit  Represents the upper bound of {@link BulkMail} to be fetched
     * @return {@link Collection} of {@link BulkMail}
     */
    protected abstract Collection<BulkMail> fetchBulkMails(final long orgId, final long userId, final int start, final int limit);

    /**
     * Converts the {@link BulkMail} to {@link JsonObject}
     *
     * @param bulkMail    Represents the {@link BulkMail}
     * @param crmResponse Represents the {@link CrmResponse}
     * @return {@link JsonObject} of {@link BulkMail}
     */
    private JsonObject convert(final BulkMail bulkMail, final CrmResponse crmResponse) {
        final JsonObject jsonObject = Json.object();
        final Metrics metrics = bulkMail.getMetrics();
        final String subject = bulkMail.getSubject();
        final String content = bulkMail.getContent();

        jsonObject.put(BulkMailFields.ID.getName(), bulkMail.id());
        jsonObject.put(BulkMailFields.ORG_ID.getName(), bulkMail.orgId());
        jsonObject.put(BulkMailFields.SENDER_ID.getName(), bulkMail.getSenderId());
        jsonObject.put(BulkMailFields.SOURCE.getName(), bulkMail.getSource());
        jsonObject.put(BulkMailFields.TOTAL_RECIPIENTS.getName(), bulkMail.getTotalRecipientsCount());

        if (Objects.nonNull(subject)) {
            jsonObject.put(BulkMailFields.SUBJECT.getName(), subject);
        }

        if (Objects.nonNull(content)) {
            jsonObject.put(BulkMailFields.MESSAGE.getName(), emailSanitizeService.trimContent(content));
        }

        if (Objects.nonNull(metrics)) {
            loadMetricsToJson(metrics, jsonObject, crmResponse);
        }

        if (bulkMail.hasScheduledConversation()) {
            final MailInfo mailInfo = bulkMail.getMailInfo().stream().findFirst().orElseGet(() -> null);

            if (Objects.nonNull(mailInfo)) {
                final EntityScheduledEmail scheduledConversation = (EntityScheduledEmail) mailInfo.getOutboundEmail();

                if (Objects.nonNull(scheduledConversation)) {
                    jsonObject.put(BulkMailFields.SCHEDULED_TIME.getName(), scheduledConversation.getScheduledTime());
                    jsonObject.put(BulkMailFields.SEND_STATUS.getName(), scheduledConversation.sendStatus());
                }
            }
        } else {
            jsonObject.put(BulkMailFields.UPDATED_TIME.getName(), bulkMail.getUpdatedTime());
        }

        return jsonObject;
    }

    /**
     * Loads the bulk mail {@link Metrics} to {@link JsonObject}
     *
     * @param metrics     Represents the {@link Metrics}
     * @param jsonObject  Represents the response of metrics {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void loadMetricsToJson(final Metrics metrics, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final long sentCount = metrics.getSentCount();
        final BiConsumer<String, Integer> computePercentageJson = (field, count) ->
                jsonObject.put(field, metrics.computePercentage(count, sentCount));
        final int openedCount = metrics.getOpenedCount();
        final int clickedCount = metrics.getClickedCount();
        final String notAvailableKey = crmResponse.resolveI18nString("i18n.notifications.tracking.not.available");

        jsonObject.put(BulkMailFields.SENT.getName(), sentCount);
        jsonObject.put(BulkMailFields.OPENED.getName(), openedCount == 0 ? notAvailableKey : openedCount);
        jsonObject.put(BulkMailFields.CREATED_TIME.getName(), metrics.getCreatedTime());

        if (clickedCount == 0) {
            jsonObject.put(BulkMailFields.CLICKED.getName(), notAvailableKey);
        } else {
            computePercentageJson.accept(BulkMailFields.CLICKED.getName(), clickedCount);
        }
        computePercentageJson.accept(BulkMailFields.UNSUBSCRIBED.getName(), metrics.getUnsubscribedCount());
        computePercentageJson.accept(BulkMailFields.REPLIED.getName(), metrics.getRepliedCount());
        computePercentageJson.accept(BulkMailFields.BOUNCED.getName(), metrics.getBouncedCount());
    }
}
