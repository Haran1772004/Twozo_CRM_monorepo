package com.tlc.crm.common.action.iam.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.fields.QueryParamAttributes;
import com.tlc.crm.common.iam.service.PasswordGenerator;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.identity.service.oidc.fields.OIDCFields;
import com.tlc.identity.service.oidc.model.OIDCProviderUserData;
import com.tlc.identity.service.oidc.provider.OIDCProvider;
import com.tlc.identity.service.oidc.provider.OIDCProvider.Action;
import com.tlc.identity.service.oidc.service.OIDCIdentityService;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * <p>
 * Handles the provider oauth based signup requests
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
@WebAction(path = "/oidc/{application}/signup/{provider}", method = HttpMethod.GET)
public class OIDCSignUpAction extends RegistrationAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(OIDCSignUpAction.class);

    @Override
    public void pProcess(final IamHttpRequest request, final IamHttpResponse response) throws Exception {
        final String phoneNumber = request.getParameter(UserFields.MOBILE);

        if (Objects.nonNull(phoneNumber) && !phoneNumber.isBlank()) {
            completeSignUp(request, response, phoneNumber);
            return;
        }

        final String provider = request.getParameter(OIDCFields.PROVIDER);
        final String code = request.getParameter(OIDCFields.CODE);
        final OIDCIdentityService identityService = OIDCProvider.get(provider).getIdentityService();

        if (Objects.isNull(code)) {
            redirectHandler.buildRedirectUrlWithState(request, response,
                    identityService.getUserConsentUri(Action.SIGNUP));
            response.send();
            return;
        }

        final OIDCProviderUserData userData = identityService.getUserData(code, Action.SIGNUP);
        final String emailAddress = userData.emailAddress();
        final EventCallback<Boolean> emailExistenceCallback = new EventCallback<>() {
            @Override
            public void onSuccess(final Boolean isEmailExists) {

                if (isEmailExists) {
                    redirectHandler.handleClientRedirect(
                            request, response,
                            new StringBuilder()
                                    .append(iamConfigurations.getConfig(IamFields.REDIRECT_URL_SERVER_ACCOUNT_EXISTS))
                                    .append(emailAddress).toString(),
                            new StringBuilder()
                                    .append(iamConfigurations.getConfig(IamFields.REDIRECT_URL_LOCAL_ACCOUNT_EXISTS))
                                    .append(emailAddress).toString()
                    );
                } else {
                    final String encodedData = redirectHandler.buildPhoneNumberRedirectState(
                            request, userData.firstName(), userData.userId(), userData.emailAddress());

                    redirectHandler.redirectToPhoneNumberPage(
                            response,
                            iamConfigurations.getConfig(IamFields.REDIRECT_URL_SERVER_PHONE_NUMBER_REQUIRED),
                            iamConfigurations.getConfig(IamFields.REDIRECT_URL_LOCAL_PHONE_NUMBER_REQUIRED),
                            provider,
                            encodedData
                    );
                }
                response.send();
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "OIDC SignUp: Signup action failed, Reason : {}", throwable.getMessage());
            }
        };

        eventNotifier.send(new Event<>(IamEvents.CHECK_EMAIL_EXISTENCE,
                        Json.object().put(UserFields.EMAIL, emailAddress).getBytes()), emailExistenceCallback);
    }

    /**
     * Completes the OIDC signup process by validating the signup state and phone number,
     * decoding the signup details, and registering the user.
     *
     * @param request     the IAM HTTP request containing the signup state and application details
     * @param response    the IAM HTTP response used to send the signup result
     * @param phoneNumber the phone number provided by the user during signup
     */
    private void completeSignUp(final IamHttpRequest request, final IamHttpResponse response,
                                final String phoneNumber) {

        final String encodedState = request.getParameter(QueryParamAttributes.STATE);

        if (Objects.isNull(encodedState)) {
            sendError(response, HttpStatus.BAD_REQUEST, ErrorCode.get(IamErrorCodes.SIGNUP_FAILED, "i18n.iam.accounts.state.required"));
            return;
        }

        if (phoneNumber.isBlank()) {
            sendError(response, HttpStatus.BAD_REQUEST, ErrorCode.get(IamErrorCodes.SIGNUP_FAILED, "i18n.iam.accounts.phone.number.required"));
            return;
        }

        final JsonObject signupState = redirectHandler.decodePhoneNumberRedirectState(encodedState);

        registerUser(request, response, phoneNumber,
                signupState.getString(UserFields.FIRST_NAME),
                signupState.getString(UserFields.EMAIL),
                signupState.getString(UserFields.IDP_USER_ID));
    }

    /**
     * Registers a new user using the OIDC provider details and the phone number
     * provided during the signup flow.
     *
     * @param request     the IAM HTTP request containing application and user-agent details
     * @param response    the IAM HTTP response used to send the registration result
     * @param firstName   the user's first name obtained from the OIDC provider
     * @param email       the user's email address obtained from the OIDC provider
     * @param userId      the user's ID from the OIDC provider
     * @param phoneNumber the phone number provided by the user
     */
    private void registerUser(final IamHttpRequest request, final IamHttpResponse response, final String phoneNumber,
                              final String firstName, final String email, final String userId) {
        register(Json.object()
                        .put(IamFields.APPLICATION, request.getParameter(IamFields.APPLICATION))
                        .put(OrganizationFields.USER_AGENT, request.getUserAgent().toJson())
                        .put(UserFields.USER, Json.object()
                                .put(UserFields.FIRST_NAME, firstName)
                                .put(UserFields.EMAIL, email)
                                .put(UserFields.MOBILE, phoneNumber)
                                .put(UserFields.HAS_ACTIVE_PASSWORD, false)
                                .put(UserFields.IDP_USER_ID, userId)
                                .put(UserFields.IS_EMAIL_VERIFIED, true)
                                .put(UserFields.PASSWORD,PasswordGenerator.generatePassword(NumericalConstant.TWELVE))),
                request, response, true
        );
    }
}