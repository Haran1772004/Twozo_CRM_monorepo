package com.tlc.crm.core.action.analytics.reports.data.export;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.keys.view.BaseFieldKeys;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.executor.Executor;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.listeners.Callback;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.executor.CodeBlockExecutor;
import com.tlc.commons.util.executor.NullSafeExecutor;

import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.crm.common.export.manager.DataExportManager;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.action.analytics.common.service.impl.AnalyticsEmailSenderImpl;
import com.tlc.crm.core.action.analytics.common.service.impl.AnalyticsExportViewServiceImpl;
import com.tlc.crm.core.action.analytics.reports.data.export.internal.AnalyticsExportDelegator;
import com.tlc.crm.core.action.analytics.reports.data.export.internal.AnalyticsExportedDataUploader;
import com.tlc.crm.core.action.analytics.reports.data.export.model.AnalyticsExportEmailRequest;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.resolver.module.ReportModularFieldResolver;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.email.client.template.mail.EmailNotificationBuilder;
import com.tlc.email.common.model.type.Email;

import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.notification.model.alert.EmailNotifiableModel;

import com.tlc.notification.service.template.EmailTemplateBuildRequest;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import com.tlc.web.HttpUtil;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.text.MessageFormat;

import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Processor responsible for handling analytics data export messages from a stream.
 * <p>
 * It decodes the request ID, performs the export using {@link AnalyticsExportDelegator},
 * uploads the file via {@link AnalyticsExportedDataUploader}, and sends a download email if needed.
 * </p>
 * <p>
 * The class implements {@link StreamListener} to listen to streaming data and
 * {@link SecureUserTaskProcessor} to process user-specific tasks securely.
 * </p>
 *
 * @author — Saravanan
 * @version 1.0
 */
class AnalyticsExportProcessor implements StreamListener<byte[]>, SecureUserTaskProcessor
{

    private static final String MODEL = "model";
    private static final String SOURCE = "{0}&source=export";
    private static final Logger LOGGER = LoggerFactory.getLogger( AnalyticsExportProcessor.class );

    private final String exportFileDownloadUrl;
    private final Executor executor;
    private final DataExportManager dataExportManager;
    private final AnalyticsExportDelegator analyticsExportDelegator;
    private final AnalyticsExportedDataUploader analyticsExportedDataUploader;
    private final UserService userService;
    private final AnalyticsExportViewServiceImpl viewService;
    private final ReportModularFieldResolver reportModularFieldResolver;

    AnalyticsExportProcessor( final Executor executor )
    {
        this.exportFileDownloadUrl = ConfHolder.getInstance()
                                               .getProperties( "data-export" )
                                               .getConfig( "redirect-url.fileDownload" );
        this.executor = executor;
        this.dataExportManager = DataExportManager.getInstance();
        this.analyticsExportDelegator = AnalyticsExportDelegator.getInstance();
        this.analyticsExportedDataUploader = AnalyticsExportedDataUploader.getInstance();
        this.userService = UserService.getInstance();
        this.viewService = AnalyticsExportViewServiceImpl.getInstance();
        this.reportModularFieldResolver = ReportModularFieldResolver.getInstance();
    }

    /**
     * Listens for export stream messages and processes them asynchronously via callback.
     * All export processing happens inside the ReportModularField callback with the map passed through.
     *
     * @param streamMessage Message from Kafka stream containing export request info.
     */
    @Override
    public void listen( final StreamMessage<byte[]> streamMessage )
    {
        executor.execute( () ->
        {
            try {
                final JsonObject requestJson = Json.object( streamMessage.data() );
                final String exportRequestId = streamMessage.id();
                final String[] decodedKeyParts = new String( Base64.getDecoder()
                                                                   .decode( exportRequestId.getBytes() ) )
                                                                   .split( ":" );
                final long orgId = Long.parseLong( decodedKeyParts[1] );
                final long userId = Long.parseLong( decodedKeyParts[2] );
                final long roleId = Long.parseLong( decodedKeyParts[3] );
                final long exportId = Long.parseLong( decodedKeyParts[4] );
                final ActionType actionType = ActionType.getActionType( decodedKeyParts[0] );
                final UserRegionalPreference userRegionalPreference = userService.getUser( orgId, userId )
                                                                                 .getPreferenceHolder()
                                                                                 .get( UserRegionalPreference.TYPE );
                final Locale locale = Locale.forLanguageTag( userRegionalPreference.getLanguage()
                                                                                   .getName() );
                final I18nResolver i18nResolver = I18nServiceProvider.get()
                                                                     .getResolver( locale );
                final TimezoneInfo timezoneInfo = userRegionalPreference.getTimezoneInfo();
                final AnalyticsRequest analyticsRequest = new AnalyticsRequest( orgId, userId, roleId, timezoneInfo );

                try
                {
                    if ( !Objects.equals( ActionType.INIT_EXPORT, actionType ) )
                    {
                        throw ErrorCode.get( AnalyticsErrorCodes.INVALID_ACTION_TYPE,
                                             "i18n.analytics.invalid.action.type" );
                    }

                    LOGGER.info( "Analytics Data export: Data export request received.id: {}, Org Id: {}, User Id: {}", exportId ,orgId, userId );
                    dataExportManager.updateStatus( orgId, exportId, ExportTracker.ExportStatus.EXPORTING );

                    reportModularFieldResolver.getBySourceId(orgId, new Callback<>() {
                        @Override
                        public void onSuccess(final Collection<ReportModularField> reportModularFields) {
                            File file = null;
                            try {
                                final Map<Long, ReportModularField> reportModularFieldMap = reportModularFields.stream()
                                        .collect(Collectors.toMap(ReportModularField::id, Function.identity()));

                                LOGGER.debug("CRM Analytics: Loaded {} ReportModularField entries for orgId: {}", reportModularFields.size(), orgId);

                                file = analyticsExportDelegator.performExport(orgId, analyticsRequest, actionType,
                                        requestJson, reportModularFieldMap, i18nResolver);

                                analyticsExportedDataUploader.uploadAndGetAttachmentWithSignedUrl( orgId, file, exportId );

                                if (requestJson.containsKey(CRMReportKeys.RECIPIENT_EMAIL)) {
                                    sendExportEmail(analyticsRequest, requestJson, exportId);
                                }

                                dataExportManager.updateStatus( orgId, exportId, ExportTracker.ExportStatus.EXPORTED );

                                LOGGER.info("CRM Analytics: Export completed successfully for exportId: {}, orgId: {}", exportId, orgId);
                            } catch (Throwable throwable) {
                                LoggerUtil.logError( LOGGER, throwable, "CRM Analytics: Error during export processing for exportId: {} orgId: {} userId: {}",
                                        exportId, orgId, userId );
                                dataExportManager.updateStatus( orgId, exportId, ExportTracker.ExportStatus.EXPORT_FAILED );
                            } finally {
                                NullSafeExecutor.acceptIfPresent( file, deleteFile -> CodeBlockExecutor.runSilently( () -> Files.deleteIfExists(deleteFile.toPath() ) ) );
                            }
                        }

                        @Override
                        public void onFailure(final Throwable cause) {
                            LoggerUtil.logError( LOGGER, cause, "CRM Analytics: Failed to load ReportModularField cache for exportId: {} orgId: {} userId: {}", exportId, orgId, userId );
                            dataExportManager.updateStatus( orgId, exportId, ExportTracker.ExportStatus.EXPORT_FAILED );
                        }
                    });
                }
                catch ( Throwable throwable )
                {
                    LoggerUtil.logError( LOGGER, throwable, "Analytics Data export: Export action failed.id: {} Org Id: {} User Id: {} Export Id: {}", orgId, userId, exportId, throwable.getMessage() );
                    dataExportManager.updateStatus( orgId, exportId, ExportTracker.ExportStatus.EXPORT_FAILED );
                }
                streamMessage.clean();
            }
            catch ( Throwable throwable )
            {
                streamMessage.clean();
            }
        });
    }

    /**
     * Sends an export completion email to the user with a download link.
     *
     * @param analyticsRequest Export request context
     * @param requestData      JSON request data
     * @param exportId         Export tracker ID
     */
    private void sendExportEmail( final @NonNull AnalyticsRequest analyticsRequest,
                                  final @NonNull JsonObject requestData,
                                  final @NonNull Long exportId )
    {
        final AnalyticsEmailSenderImpl emailSenderService = AnalyticsEmailSenderImpl.getInstance();
        final AnalyticsExportEmailRequest emailContent = new AnalyticsExportEmailRequest();
        final long viewId = requestData.getLong( BaseFieldKeys.VIEW_ID );

        emailContent.setReceiver( requestData.getString( CRMReportKeys.USER_NAME ) );
        emailContent.setChartName( viewService.getViewName( analyticsRequest, viewId ) );

        final String url = MessageFormat.format( String.format( "%s%s", HttpUtil.buildAbsoluteUrl( exportFileDownloadUrl ), SOURCE ),
                                                                        String.valueOf( exportId ) );

        emailContent.setFileDownloadUrl( url );

        final Optional<Email> emailOptional = buildEmailContent( emailContent );

        emailOptional.ifPresent( email -> emailSenderService.sendEmail( analyticsRequest, requestData, email ) );

    }

    /**
     * Builds the email content using the specified export email model.
     *
     * @param analyticsExportEmailRequest The email content model
     * @return Optional email object ready to be sent
     */
    private @NonNull Optional<Email> buildEmailContent( final @NonNull AnalyticsExportEmailRequest analyticsExportEmailRequest )
    {
        final Map<String, EmailNotifiableModel> emailNotifiableModelMap = new HashMap<>();

        emailNotifiableModelMap.put( MODEL, analyticsExportEmailRequest );

        return EmailNotificationBuilder.getInstance()
                                       .build( EmailNotificationTemplate.ANALYTICS_EXPORT.getName(),
                                               new EmailTemplateBuildRequest( emailNotifiableModelMap ) );
    }
}
