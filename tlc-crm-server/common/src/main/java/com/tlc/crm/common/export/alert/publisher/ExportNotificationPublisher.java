package com.tlc.crm.common.export.alert.publisher;

import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.export.model.AppExportAlertRequest;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Event;

import java.util.List;

import static com.tlc.crm.notification.app.event.NotificationEvent.APP_NOTIFICATION_EVENT;

/**
 * <p>
 * Publishes export related alert events.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class ExportNotificationPublisher {

    private final EventDataHandler eventDataHandler;

    private ExportNotificationPublisher() {
        this.eventDataHandler = EventDataHandler.getInstance();
    }

    private static class Instance {
        private static final ExportNotificationPublisher INSTANCE_HOLDER = new ExportNotificationPublisher();
    }

    /**
     * Gets the {@link ExportNotificationPublisher} instance.
     *
     * @return Instance of {@link ExportNotificationPublisher}.
     */
    public static ExportNotificationPublisher getInstance() {
        return Instance.INSTANCE_HOLDER;
    }

    /**
     * <p>
     * Publishes the export related alert event.
     * </p>
     *
     * @param orgId           Represents an organization id.
     * @param userId          Represents the user id.
     * @param exportId        Represents the export id.
     * @param appNotificationEventType Represents the {@link AppNotificationEventType}.
     * @param exportName      Represents the export name.
     * @param fileDownloadUrl Represents the file download url.
     */
    public void publish(final long orgId, final long userId, final long exportId, final AppNotificationEventType appNotificationEventType,
                        final String exportName, final String fileDownloadUrl) {
        final AppExportAlertRequest exportAlertRequest = new AppExportAlertRequest();

        exportAlertRequest.setId(exportId);
        exportAlertRequest.setOrgId(orgId);
        exportAlertRequest.setUserId(userId);
        exportAlertRequest.setExportName(exportName);
        exportAlertRequest.setFileDownloadUrl(fileDownloadUrl);
        exportAlertRequest.setEventType(appNotificationEventType);

        final EventData eventData = new SecureEventData(orgId, userId);

        eventData.setAction(Action.CREATE);
        eventData.setValues(List.of(exportAlertRequest));
        eventDataHandler.handleEvent(APP_NOTIFICATION_EVENT, eventData, Event.Type.POST);
    }
}
