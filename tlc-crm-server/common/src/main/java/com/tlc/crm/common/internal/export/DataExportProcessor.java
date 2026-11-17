package com.tlc.crm.common.internal.export;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.export.action.DataExportAction;
import com.tlc.crm.common.export.alert.publisher.ExportNotificationPublisher;
import com.tlc.crm.common.export.field.DataExportFields;
import com.tlc.crm.common.export.manager.DataExportManager;
import com.tlc.crm.common.export.req.DataExportRequest;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.export.container.DataExportActionContainer;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Locale;

import static com.tlc.crm.common.export.req.ExportTracker.ExportStatus.EXPORTING;
import static com.tlc.crm.common.export.req.ExportTracker.ExportStatus.EXPORT_FAILED;

/**
 * A consumer that consumes the data export requests and triggers the respective data export actions
 * based on the request parameters
 *
 * @author Selvakumar
 * @version 1.0
 */
class DataExportProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataExportProcessor.class);

    private final DataExportManager dataExportManager;
    private final DataExportActionContainer dataExportActionContainer;
    private final Executor executor;
    private final UserService crmUserService;
    private final ExportNotificationPublisher exportNotificationPublisher;

    DataExportProcessor(final Executor executor) {
        this.executor = executor;
        this.dataExportManager = DataExportManager.getInstance();
        this.dataExportActionContainer = DataExportActionContainer.getInstance();
        this.crmUserService = UserService.getInstance();
        this.exportNotificationPublisher = ExportNotificationPublisher.getInstance();
    }

    /**
     * Triggers the data export activity
     *
     * @param streamMessage A record that holds the data export request parameters
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {

        executor.execute(() -> {
            final JsonObject requestJson = Json.object(streamMessage.data());
            final String exportRequestId = streamMessage.id();
            final String[] decodedKeyParts = new String(Base64.getDecoder().decode(exportRequestId.getBytes())).split(":");
            final long orgId = Long.parseLong(decodedKeyParts[3]);
            final long userId = Long.parseLong(decodedKeyParts[4]);
            final Long exportId = Long.parseLong(decodedKeyParts[0]);

            requestJson.put(DataExportFields.ORG_ID, orgId);
            requestJson.put(DataExportFields.USER_ID, userId);
            requestJson.put(DataExportFields.ID, exportId);
            final UserRegionalPreference userRegionalPreference = crmUserService.getUser(orgId, userId).getPreferenceHolder().get(UserRegionalPreference.TYPE);

            final DataExportRequest dataExportRequest = new DataExportRequest(exportId, decodedKeyParts[1], decodedKeyParts[2],
                    orgId, userId, requestJson, Locale.forLanguageTag(userRegionalPreference.getLanguage().getName()));
            final String exportName = dataExportRequest.name();

            try {
                LOGGER.info("Data export: Data export request received.Name: {}, Org Id: {}, User Id: {}",
                        exportName, orgId, userId);
                final DataExportAction dataExportAction = dataExportActionContainer.getExportAction(exportName);

                dataExportManager.updateStatus(orgId, exportId, EXPORTING);
                dataExportAction.doExport(dataExportRequest);
                exportNotificationPublisher.publish(orgId, userId, exportId, AppNotificationEventType.EXPORTING, exportName, null);
            } catch (final Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Data export: Export action failed.Name: {} Org Id: {} User Id: {} Export Id: {}", exportName, orgId, userId, exportId);
                dataExportManager.updateStatus(orgId, userId, EXPORT_FAILED);
            } finally {
                streamMessage.clean();
            }
        });
    }
}
