package com.tlc.crm.core.notification.email.tracking.action;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.PublicEventAction;
import com.tlc.crm.common.action.res.CrmStreamResponse;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.notification.email.api.manager.SubscriptionTrackerManager;
import com.tlc.crm.core.notification.email.api.model.NotificationTrackingDetails;
import com.tlc.crm.core.notification.email.tracking.fields.EmailTrackingFields;
import com.tlc.crm.core.notification.email.internal.OutboundEmailService;

import com.tlc.crm.core.workflow.internal.WorkflowEmailTrackerManager;
import com.tlc.crm.core.workflow.internal.WorkflowEmailPublisher;

import com.tlc.crm.email.notification.email.tracking.internal.EmailTrackingFile;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;
import com.tlc.crm.email.notification.email.tracking.status.TrackingErrorCodes;

import com.tlc.crm.notification.app.Event;
import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;
import com.tlc.web.WebExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import static com.tlc.crm.notification.app.event.NotificationEvent.APP_NOTIFICATION_EVENT;

/**
 * <p>
 * Manages email tracking related web action
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@WebAction(path = "/notifications/email/tracker/{trackerType}/{trackerId}/tracker.png", method = HttpMethod.GET)
public class EmailTrackingManagement extends PublicEventAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailTrackingManagement.class);

    private final EmailTrackingFile emailTrackingFile;
    private final OutboundEmailService outboundEmailService;
    private final SubscriptionTrackerManager subscriptionTrackerManager;
    private final WorkflowEmailPublisher workflowEmailPublisher;
    private final WorkflowEmailTrackerManager workflowEmailTrackerManager;

    public EmailTrackingManagement() {
        emailTrackingFile = EmailTrackingFile.getInstance();
        outboundEmailService = OutboundEmailService.getInstance();
        subscriptionTrackerManager = SubscriptionTrackerManager.getInstance();
        this.workflowEmailPublisher = WorkflowEmailPublisher.getInstance();
        this.workflowEmailTrackerManager = WorkflowEmailTrackerManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    public void publicProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final String type = crmRequest.getParameter(EmailTrackingFields.TRACKER_TYPE.getName());
        final String trackingId = crmRequest.getParameter(EmailTrackingFields.TRACKER_ID.getName());
        final String redirectedUrl = crmRequest.getParameter(EmailTrackingFields.LINK.getName());

        switch (type) {
            case "click" -> updateClickedTime(trackingId, redirectedUrl, crmResponse);
            case "open" -> updateOpenedTime(trackingId, crmResponse);
            case "unsubscribe" -> unsubscribe(trackingId, redirectedUrl, crmResponse);
        }
    }

    /**
     * <p>
     * Updates the email opened time by given tracking id
     * </p>
     *
     * @param trackingId  Represents the tracking id
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void updateOpenedTime(final String trackingId, final CrmResponse crmResponse) {
        final boolean hasTrackerLog = subscriptionTrackerManager.hasTrackerLog(trackingId);

        outboundEmailService.updateTrackingDetails(EmailTrackingDetail.Type.OPENED, trackingId);

        if (!hasTrackerLog) {
            checkAndProcessPostEvent(trackingId);
        }

        try {
            if (crmResponse instanceof CrmStreamResponse streamResponse) {
                final FileInputStream fileInputStream = new FileInputStream(emailTrackingFile);

                streamResponse.setFileInputStream(fileInputStream);
                streamResponse.setContentType(Files.probeContentType(emailTrackingFile.toPath()));
                streamResponse.setOutputFileName(emailTrackingFile.getName());
                streamResponse.setHeader(WebExchange.Header.CACHE_CONTROL.getName(),
                        String.format("%s, %s, %s, %s", "private", "max-age=86400", "no-transform", "must-revalidate"));
            }
        } catch (Exception exception) {
            LOGGER.error("Tracking image creation failed : {}", exception.getMessage());
            throw ErrorCode.get(TrackingErrorCodes.FAILED_TO_LOAD_THE_IMAGE_FILE);
        }
        crmResponse.send();
    }

    /**
     * <p>
     * Updates the email clicked tracking time by tracking id
     * </p>
     *
     * @param trackingId    Represents the tracking id
     * @param redirectedUrl Represents the redirected url
     * @param crmResponse   Represents the {@link CrmResponse}
     */
    private void updateClickedTime(final String trackingId, final String redirectedUrl, final CrmResponse crmResponse) {
        final boolean hasTrackerLog = subscriptionTrackerManager.hasTrackerLog(trackingId);

        outboundEmailService.updateTrackingDetails(EmailTrackingDetail.Type.CLICKED, trackingId);

        if (!hasTrackerLog) {
            checkAndProcessPostEvent(trackingId);
        }
        final Long conversationId = subscriptionTrackerManager.getConversationId(trackingId);

        if (Objects.nonNull(conversationId)) {
            final boolean updatedClicked = workflowEmailTrackerManager.updateClicked(conversationId);

            if (updatedClicked) {
                workflowEmailPublisher.publish(conversationId, null, true, null);
            }
        }
        crmResponse.setTemporaryRedirectUrl(redirectedUrl);
        crmResponse.send();
    }

    /**
     * <p>
     * Check the tracking id is already exists and process the post events based on it
     * </p>
     *
     * @param trackingId Represents the {@link String} tracking id
     */
    private void checkAndProcessPostEvent(final String trackingId) {
        if (subscriptionTrackerManager.trackingExists(trackingId)) {
            subscriptionTrackerManager.updateEntityStatusByTrackingId(trackingId);
            final NotificationTrackingDetails notificationTrackingDetails = subscriptionTrackerManager.getNotificationTrackingDetails(trackingId);
            final EventData eventData = new EventData();

            if (Objects.nonNull(notificationTrackingDetails)) {
                eventData.setUserId(notificationTrackingDetails.getUserId());
                eventData.setOrgId(notificationTrackingDetails.orgId());
                eventData.setValues(List.of(notificationTrackingDetails));
                eventData.setAction(Action.CREATE);

                triggerEvent(APP_NOTIFICATION_EVENT, eventData, Event.Type.POST);
            }
        }
    }

    /**
     * <p>
     * Unsubscribes the provided details for an email based on the tracking id.
     * </p>
     *
     * @param trackingId    Represents the tracking id
     * @param redirectedUrl Represents the redirected url
     * @param crmResponse   Represents the {@link CrmResponse}
     */
    private void unsubscribe(final String trackingId, final String redirectedUrl, final CrmResponse crmResponse) {
        outboundEmailService.updateUnsubscribeDetails(trackingId);
        crmResponse.setTemporaryRedirectUrl(redirectedUrl);
        crmResponse.send();
    }
}
