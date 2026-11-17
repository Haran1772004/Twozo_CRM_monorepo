package com.tlc.crm.core.notification.email.org.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.commons.util.StringUtil;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.org.status.OrgEmailErrorCodes;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.org.api.OrgEmailManager;
import com.tlc.crm.core.notification.email.org.api.model.OrgEmail;
import com.tlc.crm.core.notification.email.org.api.model.converter.OrgEmailConverter;

import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.address.EmailRecipients;

import com.tlc.notification.service.email.EmailTokenService;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import static com.tlc.email.common.model.type.OutboundEmail.OutboundType.SEND;

/**
 * <p>
 * Manages {@link OrgEmail} related web actions
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
@WebAction(path = "/org/email/mgmt")
public class OrgEmailAction extends AuthorizationController {

    private static final int DEFAULT_START = 1;
    private static final int DEFAULT_LIMIT = 50;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final OrgEmailConverter orgEmailConverter;
    private final OrgEmailManager orgEmailManager;
    private final EmailTokenService emailTokenService;
    private final RoleManager roleManager;
    private final RandomIdGenerator randomIdGenerator;

    public OrgEmailAction() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        orgEmailConverter = OrgEmailConverter.getInstance();
        orgEmailManager = OrgEmailManager.getInstance();
        emailTokenService = EmailTokenService.getInstance();
        roleManager = RoleManager.getInstance();
        randomIdGenerator = RandomIdGenerator.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Refers {@link CrmRequest}
     * @param crmResponse Refers {@link CrmResponse}
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final long orgId = request.orgId();
        final long userId = request.userId();
        final JsonObject data = requestJson.getJsonObject(CoreEmailFields.DATA.getName());
        final String type = requestJson.getString(CoreEmailFields.TYPE.getName());

        switch (type) {
            case "create" -> {
                final OrgEmail orgEmail = convert(request, data);

                create(crmResponse, orgId, orgEmail);
                sendOtp(orgId, request.userId(), orgEmail.getAddress().getEmailAddress());
            }
            case "update" -> update(crmResponse, orgId, convert(request, data));
            case "delete" -> delete(crmResponse, orgId, userId,
                    orgEmailManager.get(orgId, List.of(data.getLong(CoreEmailFields.ID.getName()))));
            case "get" ->
                    crmResponse.put(CoreEmailFields.DATA.getName(), convert(orgEmailManager.get(orgId, data.getLong(CoreEmailFields.ID.getName()))));
            case "sendOtp" -> crmResponse.put(CoreEmailFields.IS_OTP_SENT.getName(), sendOtp(orgId, request.userId(),
                    data.getString(CoreEmailFields.EMAIL_ID.getName())));
            case "verifyOtp" -> crmResponse.put(CoreEmailFields.IS_VERIFIED.getName(), validateOtp(orgId, userId, data));
            case "list" -> crmResponse.put(CoreEmailFields.LIST.getName(), getOrgEmailList(orgId, data, crmResponse));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link OrgEmail} filled with input json data
     */
    private OrgEmail convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return orgEmailConverter.convert(converterConfig, jsonObject);
    }

    /**
     * <p>
     * Creates the given entities
     * </p>
     *
     * @param orgEmail Holds the {@link OrgEmail} details
     */
    private void create(final CrmResponse crmResponse, final long orgId, final OrgEmail orgEmail) {
        final Collection<ErrorDescriptor> errors = ValidatorServiceProvider.get().modelValidator().validate(List.of(orgEmail), Group.Create.class);

        if (errors.isEmpty() && isValidDetails(orgId, Action.CREATE, orgEmail)) {
            final ProcessOutcome processOutcome = orgEmailManager.create(orgId, List.of(orgEmail));

            crmResponse.put(CoreEmailFields.CREATED_RECORDS.getName(), processOutcome.getProcessedRecords());
            final ErrorSummary errorSummary = processOutcome.getErrorSummary();

            if (Objects.nonNull(errorSummary)) {
                crmResponse.setErrorSummary(errorSummary);
                crmResponse.setErrorCode(ErrorCode.get(OrgEmailErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed"));
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(OrgEmailErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode,
                    ErrorKeys.CREATE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Sends a one-time password (OTP) to the specified email address for verification.
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param userId  Represents the user id
     * @param emailId The email address to which the OTP is sent
     * @return True if the OTP is successfully sent and cached, false otherwise
     */
    private boolean sendOtp(final long orgId, final long userId, final String emailId) {
        final String verificationCode = randomIdGenerator.generateUniqueASCII(6);
        final EntityOutboundEmail mail = new EntityOutboundEmail();
        final EmailMetaData emailData = new EmailMetaData();
        final EmailRecipients emailRecipients = new EmailRecipients();

        emailRecipients.addRecipients(AddressType.TO, List.of(new EmailAddress(emailId)));

        mail.setRecipients(List.of(emailRecipients));
        mail.setContent(String.format("Your verification code is: %s. Please enter this code to complete the verification process. Thank you!", verificationCode));
        mail.setSubject(CoreEmailFields.VERIFICATION_CODE.getName());
        mail.setOutboundType(SEND);
        mail.setMetaData(emailData);

        mail.setOrgId(orgId);
        mail.setUserId(userId);

        emailTokenService.setOtp(StringUtil.joinNonNullValues(StringUtil.Delimiter.UNDER_SCORE, orgId, emailId).orElseGet(() -> ""), verificationCode);

        return true;
    }

    /**
     * <p>
     * Updates the given entities
     * </p>
     *
     * @param crmResponse the response to be sent
     * @param orgId       represents the organization id
     * @param orgEmail    An entity that holds the attributes of {@link OrgEmail}
     */
    private void update(final CrmResponse crmResponse, final long orgId, final OrgEmail orgEmail) {
        final Collection<ErrorDescriptor> errors = ValidatorServiceProvider.get().modelValidator().validate(List.of(orgEmail), Group.Update.class);

        if (errors.isEmpty() && isValidDetails(orgId, Action.UPDATE, orgEmail)) {
            final ProcessOutcome processOutcome = orgEmailManager.update(orgId, List.of(orgEmail));

            crmResponse.put(ActionFields.UPDATED_RECORDS, processOutcome.getProcessedRecords());
            final ErrorSummary errorSummary = processOutcome.getErrorSummary();

            if (Objects.nonNull(errorSummary)) {
                crmResponse.setErrorCode(ErrorCode.get(OrgEmailErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed"));
                crmResponse.setErrorSummary(errorSummary);
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(OrgEmailErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.UPDATE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * Deletes the given attachment details.
     *
     * @param crmResponse The response to be sent.
     * @param orgId       Organization id.
     * @param userId      Represents the user id
     * @param orgEmails   Collection of {@link OrgEmail}.
     */
    private void delete(final CrmResponse crmResponse, final long orgId, final long userId, final Collection<OrgEmail> orgEmails) {
        final Collection<ErrorDescriptor> errors = ValidatorServiceProvider.get().modelValidator().validate(orgEmails, Group.Delete.class);

        if (errors.isEmpty()) {
            final Set<Long> orgEmailIds = orgEmails.stream().map(OrgEmail::id).collect(Collectors.toUnmodifiableSet());

            if (!orgEmailIds.isEmpty() && orgEmailManager.exists(orgId, orgEmailIds)) {
                orgEmailManager.delete(orgId, userId, orgEmailIds);
            } else {
                crmResponse.setErrorCode(ErrorCode.get(OrgEmailErrorCodes.ORG_EMAIL_NOT_FOUND, "i18n.notifications.orgEmail.invalid.id"));
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(OrgEmailErrorCodes.DELETE_ACTION_FAILED, "i18n.notifications.orgEmail.delete.action.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DELETE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return A {@link JsonObject} representation of the provided {@link OrgEmail}
     */
    private JsonObject convert(final OrgEmail orgEmail) {
        return orgEmailConverter.convert(orgEmail);
    }

    /**
     * <p>
     * Validates the OTP for a specific organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param data   Represents the json object
     * @return True if the OTP is valid, false otherwise
     */
    private boolean validateOtp(final long orgId, final long userId, final JsonObject data) {
        final String emailId = data.getString(CoreEmailFields.EMAIL_ID.getName());
        final String otp = emailTokenService.getOtp(StringUtil.joinNonNullValues(StringUtil.Delimiter.UNDER_SCORE, orgId, emailId).orElseGet(() -> ""));

        if (Objects.nonNull(otp) && otp.equals(data.getString(CoreEmailFields.OTP.getName()).trim())) {
            orgEmailManager.updateVerificationStatus(orgId, userId, orgEmailManager.getEmailId(orgId, emailId));
        } else {
            throw ErrorCode.get(OrgEmailErrorCodes.INVALID_OTP, "i18n.notifications.orgEmail.invalid.otp");
        }

        return true;
    }

    /**
     * <p>
     * Gets the {@link OrgEmail} list for a specific organization id
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param data     An object that holds request data
     * @param response Represents the {@link CrmResponse}
     * @return {@link JsonArray} of org emails
     */
    private JsonArray getOrgEmailList(final Long orgId, final JsonObject data, final CrmResponse response) {
        final JsonArray orgEmailArray = Json.array();
        final int start = Math.max(data.optInt(CoreEmailFields.START.getName(), DEFAULT_START), DEFAULT_START);
        final int limit = Math.min(data.optInt(CoreEmailFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final long totalRecords = orgEmailManager.getOrgEmailCount(orgId);

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<OrgEmail> orgEmails = orgEmailManager.getOrgEmailList(orgId, start, limit);

            if (!orgEmails.isEmpty()) {
                orgEmails.forEach(orgEmail -> orgEmailArray.put(convert(orgEmail)));
            }
        }
        response.put(CoreEmailFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(CoreEmailFields.HAS_MORE.getName(), (start + limit) <= totalRecords);

        return orgEmailArray;
    }

    /**
     * <p>
     * Validates the roles associated with {@link OrgEmail}
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param orgEmail An entity that holds the collection of {@link OrgEmail}
     */
    private void validateOrgEmailRoles(final long orgId, final OrgEmail orgEmail) {
        if (!roleManager.exists(orgId, orgEmail.getCrmRoles().stream().map(CrmRole::id).collect(Collectors.toUnmodifiableSet()))) {
            throw ErrorCode.get(OrgEmailErrorCodes.ROLE_NOT_FOUND);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the Organization id
     * @param action   Performs action like create, update
     * @param orgEmail Holds the {@link OrgEmail} details
     */
    protected final boolean isValidDetails(final Long orgId, final Action action, final OrgEmail orgEmail) {

        if (Objects.nonNull(orgEmail)) {
            final String name = orgEmail.getName();

            switch (action) {
                case CREATE -> {
                    final EmailAddress emailAddress = orgEmail.getAddress();

                    if (Objects.nonNull(emailAddress) && orgEmailManager.checkEmailAddressExists(orgId, emailAddress.getEmailAddress())) {
                        throw ErrorCode.get(OrgEmailErrorCodes.ORG_EMAIL_ADDRESS_ALREADY_EXISTS);
                    }
                }
                case UPDATE -> {
                    if (!orgEmailManager.exists(orgId, orgEmail.id())) {
                        throw ErrorCode.get(OrgEmailErrorCodes.ORG_EMAIL_NOT_FOUND);
                    }
                }
            }

            if (Objects.nonNull(name) && orgEmailManager.checkDisplayNameExists(orgId, name)) {
                throw ErrorCode.get(OrgEmailErrorCodes.ORG_EMAIL_DISPLAY_NAME_ALREADY_EXISTS);
            }

            validateOrgEmailRoles(orgId, orgEmail);
        }

        return true;
    }
}
