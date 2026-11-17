package com.tlc.crm.core.action.analytics.reports.data.export.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.export.manager.DataExportManager;
import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.queue.CoreQueueTopics;

import com.tlc.i18n.I18nResolver;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.producer.ProducerStore;

import lombok.NonNull;

import java.util.Base64;
import java.util.Optional;

/**
 * Singleton service for managing analytics data export and upload operations.
 *
 * <p>Triggers export via Kafka and delegates logic to {@link AnalyticsExportDelegator}.
 * Handles file upload and signed URL generation through {@link AnalyticsExportedDataUploader}.</p>
 *
 * <p>Use {@link #getInstance()} to access the singleton instance.</p>
 *
 * @author Saravanan
 * @version 1.0
 */
public final class AnalyticsExportHandler
{
    private final DataExportManager dataExportManager;

    private AnalyticsExportHandler()
    {
        this.dataExportManager = DataExportManager.getInstance();
    }

    private static class Instance
    {
        private static final AnalyticsExportHandler INSTANCE = new AnalyticsExportHandler();
    }

    public static AnalyticsExportHandler getInstance()
    {
        return Instance.INSTANCE;
    }

    /**
     * Initiates the export process for analytics data and triggers an asynchronous file generation and upload.
     *
     * <p>
     * This method creates a new {@link ExportTracker} record to track the export,
     * encodes a unique request ID using export metadata, and sends the export request
     * to a Kafka-based analytics exporter topic.
     * </p>
     *
     * <p>
     * The actual export is handled asynchronously by a background service subscribed to the Kafka topic.
     * </p>
     *
     * @param analyticsRequest The analytics request containing organization, user, and role identifiers.
     * @param actionType       The action to be performed (e.g., EXPORT, DOWNLOAD).
     * @param requestData      The export payload including report/view configuration and filters.
     * @param i18nResolver     Resolver used for internationalized error messages.
     * @return The ID of the created {@link ExportTracker}, used to track the export status.
     *
     */
    public @NonNull Long initiateExportProcess( final @NonNull AnalyticsRequest analyticsRequest,
                                                final @NonNull ActionType actionType,
                                                final @NonNull JsonObject requestData,
                                                final @NonNull I18nResolver i18nResolver )
    {
        final ExportTracker exportTracker = new ExportTracker();

        exportTracker.setOrgId( analyticsRequest.orgId() );
        exportTracker.setUserId( analyticsRequest.userId() );
        exportTracker.setExportStatus( ExportTracker.ExportStatus.NEW );

        dataExportManager.create( exportTracker );

        final ProducerStore producerStore = ProducerStore.getInstance();
        final String requestId = Base64.getEncoder().encodeToString( String.format( "%s:%s:%s:%s:%s",
                                                                     actionType.getActionType(),
                                                                     analyticsRequest.orgId(),
                                                                     analyticsRequest.userId(),
                                                                     analyticsRequest.roleId(),
                                                                     exportTracker.id() )
                                                                     .getBytes() );

        final SubscriptionTopic analyticsExportTopic = CoreQueueTopics.getAnalyticsExporterTopic( QueueModel.Type.KAFKA );
        final Optional<Producer<byte[]>> exportProducer = producerStore.getProducer( analyticsExportTopic );

        try
        {
             exportProducer.ifPresent( producer -> producer.send( new StreamMessage<>( requestId, analyticsExportTopic, requestData.getBytes() ) ) );
        }
        catch ( Throwable throwable )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.EXPORT_PRODUCER_SEND_FAILED,
                                 "i18n.analytics.config.export.producer.send.failed" );
        }
        return exportTracker.id();
    }
}
