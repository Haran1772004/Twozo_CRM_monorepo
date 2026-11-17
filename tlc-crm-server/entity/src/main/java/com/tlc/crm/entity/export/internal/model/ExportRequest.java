package com.tlc.crm.entity.export.internal.model;

import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.export.model.ExportProviderType;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * Represents the export request information
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class ExportRequest implements TlcModel {

    private Long orgId;

    @EmailAlertField(
            name = "userId",
            isUserField = true,
            placeholderTemplates = {EmailNotificationTemplate.DATA_EXPORT},
            recipientTemplates = {EmailNotificationTemplate.DATA_EXPORT},
            fieldType = EmailAlertFieldType.NUMBER
    )
    private Long userId;
    private Long id;
    private ExportProviderType exportProviderType;

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setExportProvider(final ExportProviderType exportProviderType) {
        this.exportProviderType = exportProviderType;
    }

    public Long getOrgId() {
        return orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public ExportProviderType getExportProvider() {
        return exportProviderType;
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
