package com.tlc.crm.notifications.calendar.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.crm.entity.async.cache.SyncInfoHolderService;

import com.tlc.crm.notifications.calendar.api.manager.CalendarSyncManager;
import com.tlc.crm.notifications.calendar.api.model.CalendarSyncSettings;
import com.tlc.crm.notifications.calendar.api.model.converter.CalendarSyncSettingsBuilder;
import com.tlc.crm.notifications.calendar.fields.CalendarFields;
import com.tlc.crm.notifications.internal.async.SyncExecutorService;
import com.tlc.crm.notifications.calendar.internal.status.CalendarErrorCodes;
import com.tlc.crm.notifications.internal.async.calendar.CalendarSyncExecutor;
import com.tlc.crm.notifications.internal.async.calendar.cache.CalendarSyncInfoHolderService;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.service.provider.ProviderService;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Manages calendar sync settings related web actions
 * </p>
 *
 * @author Ajith venkadesh, Kowsalya SP
 * @version 1.0
 */
@WebAction(path = "/calendar/sync/settings")
public class CalendarSyncSettingsAction extends AuthorizationController {

    private final CalendarSyncManager calendarSyncManager;
    private final SyncInfoHolderService calendarSyncInfoHolderService;
    private final SyncExecutorService calendarSyncExecutorService;
    private final CalendarSyncSettingsBuilder calendarSyncSettingsBuilder;
    private final ModelValidator modelValidator;
    private final ProviderService providerService;

    public CalendarSyncSettingsAction() {
        this.calendarSyncManager = CalendarSyncManager.getInstance();
        this.calendarSyncInfoHolderService = CalendarSyncInfoHolderService.getInstance();
        this.calendarSyncExecutorService = CalendarSyncExecutor.getInstance();
        this.calendarSyncSettingsBuilder = CalendarSyncSettingsBuilder.getInstance();
        this.modelValidator = ValidatorServiceProvider.get().modelValidator();
        this.providerService = ProviderService.getCalendarProviderService();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the {@link CrmRequest} data
     * @param crmResponse Represents the {@link CrmResponse} data
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(CalendarFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(CalendarFields.DATA);
        final Long orgId = request.orgId();
        final Long userId = request.getUser().id();

        switch (type) {
            case "startSync" -> {
                final Long syncId = data.getLong(CalendarFields.ID);

                if (calendarSyncManager.exists(orgId, userId, syncId)) {
                    final CalendarSyncSettings calendarSyncSettings = calendarSyncSettingsBuilder.convert(orgId, userId, syncId, data);

                    syncCalendarAccount(orgId, userId, calendarSyncSettings, crmResponse);
                } else {
                    throw ErrorCode.get(CalendarErrorCodes.SYNC_EMAIL_NOT_FOUND, "i18n.notifications.calendar.sync.email.invalid");
                }
            }
            case "get" -> {
                final CalendarSyncSettings calendarSyncSettings = calendarSyncManager.getCalenderSyncSettings(orgId, userId);

                crmResponse.put(CalendarFields.SYNC_SETTINGS, Objects.isNull(calendarSyncSettings)
                        ? null : calendarSyncSettingsBuilder.convertCalendarSyncToJson(calendarSyncSettings, crmResponse));
            }
            case "stopSync" -> stopSync(orgId, userId, data, crmResponse);
            case "checkSyncStatus" -> checkSyncStatus(orgId, userId, data, crmResponse);
        }
    }

    /**
     * <p>
     * Stops the calendar account which is synchronized based on given request data.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param data        Represents the request {@link JsonObject} data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void stopSync(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final long syncId = data.getLong(CalendarFields.ID);
        final String messageKey = CalendarFields.MESSAGE;

        if (calendarSyncManager.exists(orgId, userId, syncId)) {

            try {
                calendarSyncManager.stopSync(orgId, userId, syncId);
                calendarSyncExecutorService.remove(syncId);
                crmResponse.put(messageKey, crmResponse.resolveI18nString("i18n.notifications.calendar.sync.stop.success"));
            } catch (Exception e) {
                crmResponse.setErrorCode(ErrorCode.get(CalendarErrorCodes.CALENDAR_SYNC_STOP_FAILED, "i18n.notifications.calendar.sync.stop.failure", e));
            }
        } else {
            throw ErrorCode.get(CalendarErrorCodes.SYNC_EMAIL_NOT_FOUND, "i18n.notifications.calendar.sync.email.invalid");
        }
    }

    /**
     * <p>
     * Syncs the respective calendar account based on {@link CalendarSyncSettings}.
     * </p>
     *
     * @param orgId                Represents an organization id
     * @param userId               Represents the user id
     * @param calendarSyncSettings Represents the {@link CalendarSyncSettings}
     * @param response             Represents the {@link CrmResponse} data
     */
    private void syncCalendarAccount(final long orgId, final long userId, final CalendarSyncSettings calendarSyncSettings, final CrmResponse response) {
        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(calendarSyncSettings), Group.Update.class);

        if (errors.isEmpty()) {
            final Long syncId = calendarSyncSettings.id();

            if (calendarSyncInfoHolderService.isSyncInProgress(orgId, syncId)) {
                final SyncInfo syncInfo = calendarSyncExecutorService.getSyncInfo(orgId, syncId);

                response.put(CalendarFields.IS_IN_PROGRESS, syncInfo.syncStatus() == SyncStatus.IN_PROGRESS);
                response.put(CalendarFields.IS_DONE, false);
            } else {
                final String email = calendarSyncManager.getCalendarSyncEmailAddress(orgId, userId, syncId);

                if (Objects.nonNull(email)) {
                    calendarSyncSettings.setProvider(providerService.findProvider(new EmailAddress(email)).getName());
                }
                calendarSyncInfoHolderService.setSyncStatus(orgId, userId, new SyncInfo(syncId, SyncStatus.NEW));
                calendarSyncExecutorService.execute(calendarSyncSettings);
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(CalendarErrorCodes.CALENDAR_SYNC_ACTION_FAILED, "i18n.notifications.calendar.sync.failed");

            response.setErrorCode(errorCode);
            response.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Checks the synchronization calendar sync status based on the provided {@link JsonObject} request.
     * </p>
     *
     * @param jsonObject Represents the {@link JsonObject} request
     * @param response   Represents the {@link CrmResponse}
     */
    private void checkSyncStatus(final long orgId, final long userId, final JsonObject jsonObject, final CrmResponse response) {
        final long syncId = jsonObject.getLong(CalendarFields.ID);

        if (calendarSyncManager.exists(orgId, userId, syncId)) {
            final SyncStatus syncStatus = calendarSyncExecutorService.getSyncStatus(orgId, syncId);

            response.put(CalendarFields.STATE, response.resolveI18nString(SyncStatus.getStateValue(syncStatus)));
        } else {
            throw ErrorCode.get(CalendarErrorCodes.SYNC_EMAIL_NOT_FOUND, "i18n.notifications.calendar.sync.email.invalid");
        }
    }
}