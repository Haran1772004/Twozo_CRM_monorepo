package com.tlc.crm.notifications.calendar.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.util.oauth.RedirectHandler;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;

import com.tlc.crm.notifications.calendar.api.manager.CalendarSyncManager;
import com.tlc.crm.notifications.calendar.api.model.CalendarSyncSettings;
import com.tlc.crm.notifications.calendar.api.model.internal.CalendarSyncConfig;
import com.tlc.crm.notifications.calendar.fields.CalendarFields;
import com.tlc.crm.notifications.calendar.internal.status.CalendarErrorCodes;

import com.tlc.notification.model.email.EmailAddress;
import com.tlc.notification.model.sync.ConnectionStatus;

import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.SyncUrlConfig;
import com.tlc.sync.service.oauth.model.provider.CalendarProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest.TokenType;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenWriteRequest;
import com.tlc.sync.service.oauth.service.sync.SyncService;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;

import java.util.Objects;
import java.util.List;
import java.util.Collection;

import static com.tlc.notification.model.sync.ConnectionStatus.INACTIVE;

@WebAction(path = "/oauth/calendar/sync/{provider}", method = HttpMethod.GET)
public class CalendarSyncAction extends AuthorizationController {

    private static final String LOCAL_NAVIGATE_URL = "local.navigateUrl";
    private static final String SYNC_REDIRECT_URL_KEY = "redirectUrl.sync";
    private static final String SYNC_ERROR_REDIRECT_URL = "redirectUrl.error";

    private final CalendarSyncManager calendarSyncManager;
    private final ActivityTypeManager activityTypeManager;
    private final Configurations configurations;
    private final RedirectHandler redirectHandler;

    public CalendarSyncAction() {
        this.calendarSyncManager = CalendarSyncManager.getInstance();
        this.activityTypeManager = ActivityTypeManager.getInstance();
        this.configurations = ConfHolder.getInstance().getProperties(CalendarFields.CALENDAR);
        this.redirectHandler = RedirectHandler.getInstance();
    }

    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final CalendarProvider calendarProvider = CalendarProvider.get(request.getParameter(CalendarFields.PROVIDER));
        final SyncUrlConfig syncUrlConfig = SyncService.getSyncUrlDetails(calendarProvider);
        final String code = request.getParameter(CalendarFields.CODE);

        if (Objects.isNull(code)) {
            redirectHandler.buildRedirectUrlWithState(request, syncUrlConfig.getConsentUrl(), crmResponse);
        } else {
            final EmailTokenWriteRequest calendarTokenWriteRequest = SyncTokenRequestInstanceProvider.getCalendarTokenWriteRequest(code, syncUrlConfig, TokenType.REFRESH);

            calendarProvider.getSyncService().fetchAndStoreRefreshToken(calendarTokenWriteRequest);
            final long orgId = request.orgId();
            final long userId = request.userId();
            final EmailAddress address = new EmailAddress(orgId, calendarTokenWriteRequest.getEmailAddress().getEmailId());

            if (calendarSyncManager.exists(orgId, address)) {
                crmResponse.setTemporaryRedirectUrl(HttpUtil.buildAbsoluteUrl(configurations.getConfig(SYNC_ERROR_REDIRECT_URL)));
                crmResponse.send();
                return;
            }
            final CalendarSyncSettings calendarSyncSettings = new CalendarSyncSettings();

            calendarSyncSettings.setAddress(address);
            calendarSyncSettings.setOrgId(orgId);
            calendarSyncSettings.setUserId(userId);
            calendarSyncSettings.setCalendarSyncConfig(new CalendarSyncConfig(CalendarSyncConfig
                    .getDefaultConfigJsonAsString(activityTypeManager.getActivityTypes(orgId, true))));
            final ConnectionStatus connectionStatus = calendarSyncManager.getConnectionStatus(orgId, userId, address);

            if (Objects.isNull(connectionStatus)) {
                createCalendarSyncSettings(crmResponse, orgId, List.of(calendarSyncSettings));
            } else {

                if (INACTIVE == connectionStatus) {
                    calendarSyncManager.updateDefaultConfiguration(orgId, userId, address);
                } else {
                    throw ErrorCode.get(CalendarErrorCodes.CALENDAR_ALREADY_SYNCED, "i18n.notifications.calendar.already.synced");
                }
            }
            redirectHandler.handleClientRedirect(request, crmResponse, HttpUtil.buildAbsoluteUrl(configurations.getConfig(SYNC_REDIRECT_URL_KEY)),
                    configurations.getConfig(LOCAL_NAVIGATE_URL));
        }
    }

    /**
     * <p>
     * Creates a new calendar sync settings.
     * </p>
     *
     * @param crmResponse  The response to be displayed
     * @param orgId        Represents an organization id
     * @param syncSettings An entity that holds sync calendar attributes
     */
    private void createCalendarSyncSettings(final CrmResponse crmResponse, final long orgId,
                                            final Collection<CalendarSyncSettings> syncSettings) {
        final Collection<ErrorDescriptor> errors = ValidatorServiceProvider.get().modelValidator().validate(syncSettings, Group.Create.class);

        if (errors.isEmpty()) {
            final ProcessOutcome processOutcome = calendarSyncManager.create(orgId, syncSettings);

            crmResponse.put(ActionFields.CREATED_RECORDS, processOutcome.getProcessedRecords());
            final ErrorSummary errorSummary = processOutcome.getErrorSummary();

            if (Objects.nonNull(errorSummary)) {
                crmResponse.setErrorCode(ErrorCode.get(CalendarErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed"));
                crmResponse.setErrorSummary(errorSummary);
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(CalendarErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
        }
    }
}
