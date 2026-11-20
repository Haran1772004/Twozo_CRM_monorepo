package com.tlc.crm.common.action.iam.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.field.IamFields;
import com.tlc.crm.common.models.CommonFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.otp.manager.OTPRequestManager;
import com.tlc.otp.model.Category;
import com.tlc.otp.model.Channel;
import com.tlc.web.WebAction;

import java.util.Optional;

/**
 * Handles the signup requests
 *
 * @author Selvakumar
 * @version 1.0
 */
@WebAction(path = "/accounts/{application}/signup")
public class SignUpManagement extends RegistrationAction {

    private final OTPRequestManager otpRequestManager;

    public SignUpManagement() {
        this.otpRequestManager = OTPRequestManager.getInstance();
    }

    /**
     * Handles the signup operation
     *
     * @param request  Http request
     * @param response Http response
     */
    @Override
    public void pProcess(final IamHttpRequest request, final IamHttpResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(IamFields.DATA);
        final Optional<Category> accountVerificationCategory = otpRequestManager
                .getCategory(CommonFields.CRM, CommonFields.ACCOUNT_VERIFICATION);

        if (accountVerificationCategory.isPresent() &&
                !otpRequestManager.isReceiverVerified(data.getJsonObject(UserFields.USER).getString(UserFields.EMAIL),
                        Channel.EMAIL, accountVerificationCategory.get())) {
            throw ErrorCode.get(IamErrorCodes.EMAIL_IS_NOT_VERIFIED, "i18n.iam.user.email.address.not.verified");
        }
        data.put(IamFields.APPLICATION, request.getParameter(IamFields.APPLICATION));
        data.put(OrganizationFields.USER_AGENT, request.getUserAgent().toJson());

        register(data, request, response, false);
    }
}