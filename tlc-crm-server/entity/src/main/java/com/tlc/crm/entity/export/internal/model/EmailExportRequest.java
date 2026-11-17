package com.tlc.crm.entity.export.internal.model;

import com.tlc.crm.common.email.alert.annotation.EmailAlertTemplate;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.notification.model.alert.EmailNotifiableModel;

/**
 * <p>
 * Represents the data model for exporting email-related information
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@EmailAlertTemplate(templates = {EmailNotificationTemplate.DATA_EXPORT})
public class EmailExportRequest extends ExportRequest implements EmailNotifiableModel {

    private String fileDownloadUrl;
    private String exportName;

    public String getFileDownloadUrl() {
        return fileDownloadUrl;
    }

    public void setFileDownloadUrl(final String fileDownloadUrl) {
        this.fileDownloadUrl = fileDownloadUrl;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(final String exportName) {
        this.exportName = exportName;
    }
}
