package com.tlc.crm.common.export.model;

import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * Represents the export alert request for in-app notifications.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Notifiable(types = {AppNotificationEventType.EXPORTING, AppNotificationEventType.EXPORTED}, modelName = CrmModelType.EXPORT, needHandling = true)
public class AppExportAlertRequest implements TlcModel {

    private Long id;
    private Long orgId;
    private Long userId;
    private String exportName;
    private String fileDownloadUrl;
    private AppNotificationEventType eventType;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(final String exportName) {
        this.exportName = exportName;
    }

    public String getFileDownloadUrl() {
        return fileDownloadUrl;
    }

    public void setFileDownloadUrl(final String fileDownloadUrl) {
        this.fileDownloadUrl = fileDownloadUrl;
    }

    public AppNotificationEventType getEventType() {
        return eventType;
    }

    public void setEventType(final AppNotificationEventType eventType) {
        this.eventType = eventType;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}
