package com.tlc.crm.common.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.annotation.EmailAlertTemplate;
import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.notification.model.alert.EmailNotifiableModel;
import com.tlc.validator.TlcModel;

/**
 * Represents the model for user deactivation notifications
 *
 * @author kishore J
 * @version 1.0
 */
@EmailAlertTemplate(templates = {EmailNotificationTemplate.USER_DEACTIVATION})
public class UserDeactivation implements EmailNotifiableModel, TlcModel {

    @EmailAlertField(
            fieldType = EmailAlertFieldType.TLC_MODEL
    )
    private CrmUser crmUser;

    @EmailAlertField(
            isUserField = true,
            fieldType = EmailAlertFieldType.NUMBER,
            placeholderTemplates = {EmailNotificationTemplate.USER_DEACTIVATION}
    )
    private Long adminUserId;


    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(final Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    public CrmUser getCrmUser() {
        return crmUser;
    }

    public void setCrmUser(final CrmUser crmUser) {
        this.crmUser = crmUser;
    }

    @Override
    public Long id() {
       return crmUser.id();
    }

    @Override
    public Long orgId() {
        return crmUser.orgId();
    }

    @Override
    public Object identity() {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
    }
}
