package com.tlc.crm.common.iam.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.common.iam.account.cancellation.AccountCancellationManager;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.account.cancellation.AccountCancellationEntry;
import com.tlc.crm.common.iam.api.model.account.cancellation.Survey;
import com.tlc.crm.common.iam.field.AccountCancellationKeys;
import com.tlc.crm.common.iam.internal.status.AccountCancellationErrorCodes;
import com.tlc.crm.common.iam.service.RoleService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.validator.Group;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.crm.security.iam.DefaultRole;
import com.tlc.web.WebAction;
import jakarta.validation.ConstraintViolation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the {@link AccountCancellationEntry} related web actions
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@WebAction(path = "/account/{application}/cancellation")
public class AccountCancellationManagement extends CrmSecureAction {

    private final AccountCancellationManager accountCancellationManager;
    private final RoleService roleService;
    private final ModelValidator modelValidator;

    public AccountCancellationManagement() {
        accountCancellationManager = AccountCancellationManager.getInstance();
        roleService = RoleService.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  The {@link CrmRequest} to be handled
     * @param crmResponse The {@link CrmResponse}
     * @throws Exception
     */
    @Override
    public void sProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final long orgId = crmRequest.orgId();
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(AccountCancellationKeys.TYPE);
        final JsonObject data = requestJson.getJsonObject(AccountCancellationKeys.DATA);

        switch (type) {
            case "cancelAccount" -> cancelAccount(orgId, crmRequest.getUser().id(), data, crmResponse);
            case "getState" -> crmResponse.put(AccountCancellationKeys.STATE, getState(orgId));
            case "submitSurvey" -> submitSurvey(orgId, data, crmResponse);
            case "retain" -> retain(orgId, crmResponse);
        }
    }

    /**
     * <p>
     * Initiates the account cancellation
     * </p>
     *
     * @param orgId        Represents the organization Id
     * @param userId       Represents the user Id
     * @param data         Represents the data {@link JsonObject}
     * @param crmResponse  Represents the {@link CrmResponse}
     */
    private void cancelAccount(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {

        if (isValidDetails(orgId, userId, crmResponse)) {
            final AccountCancellationEntry accountCancellationEntry = new AccountCancellationEntry(orgId);

            if (data.containsKey(AccountCancellationKeys.REASON)) {
                final JsonObject reasonJson = data.getJsonObject(AccountCancellationKeys.REASON);

                if (reasonJson.containsKey(AccountCancellationKeys.ID)) {
                    final short reasonId = reasonJson.getShort(AccountCancellationKeys.ID);

                    accountCancellationEntry.setReason(AccountCancellationEntry.ReasonType.getReasonType(reasonId));

                    if (reasonJson.containsKey(AccountCancellationKeys.DETAILS) && Objects.equals(AccountCancellationEntry.ReasonType.OTHER_REASONS.getId(), reasonId)) {
                        accountCancellationEntry.setOtherReason(reasonJson.getString(AccountCancellationKeys.DETAILS));
                    }
                    final Long retentionEndTime = TemporalServiceProvider.get(Instant.now().plus(AccountCancellationEntry.RETENTION_DAYS,
                            ChronoUnit.DAYS).toEpochMilli()).getEndTimeOfDay(TimezoneInfo.UTC);

                    accountCancellationEntry.setRetentionEndTime(retentionEndTime);
                }
            }
            final ErrorCodeProvider errorCodeProvider = CommonErrorCodes.DATA_VALIDATION_FAILED;
            final Set<ConstraintViolation<AccountCancellationEntry>> constraintViolations = modelValidator.validate(accountCancellationEntry, Group.CancelAccount.class);
            final Collection<ErrorDescriptor> errors = constraintViolations.stream()
                    .map(violation -> new ErrorDescriptor(ErrorCode.get(errorCodeProvider,
                            violation.getMessage()), null, violation.getPropertyPath().toString())).collect(Collectors.toSet());

            if (errors.isEmpty()) {
                accountCancellationManager.cancelAccount(accountCancellationEntry);
                crmResponse.put(AccountCancellationKeys.MESSAGE, crmResponse.resolveI18nString("i18n.account.cancellation.request.placed"));
            } else {
                final ErrorCode errorCode = ErrorCode.get(errorCodeProvider, "i18n.account.cancellation.validation.failed");

                crmResponse.setErrorCode(errorCode);
                crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode), errors));
            }
        }
    }

    /**
     * <p>
     * Checks whether the data are valid or not
     * </p>
     *
     * @param orgId        Represents the organization Id
     * @param userId       Represents the user Id
     * @param crmResponse  Represents the {@link CrmResponse}
     * @return True if the details are valid
     */
    private boolean isValidDetails(final long orgId, final long userId, final CrmResponse crmResponse) {
        if (accountCancellationManager.exists(orgId)) {
            throw ErrorCode.get(AccountCancellationErrorCodes.ORG_CANCELLATION_REQUEST_EXISTS, crmResponse.resolveI18nString("i18n.account.cancellation.request.already.exists"));
        }
        final CrmRole role = roleService.getRoleByUser(orgId, userId);

        if (!Objects.equals(DefaultRole.ACCOUNT_ADMIN.getName(), role.getName())) {
            throw ErrorCode.get(AccountCancellationErrorCodes.ORG_CANCELLATION_FORBIDDEN, crmResponse.resolveI18nString("i18n.account.cancellation.access.denied"));
        }
        return true;
    }

    /**
     * <p>
     * Gets the account cancellation request state details
     * </p>
     *
     * @param orgId The organisation Id
     */
    private JsonObject getState(final long orgId) {
        final JsonObject stateJson = Json.object();
        final AccountCancellationEntry accountCancellationEntry = accountCancellationManager.get(orgId);

        if (Objects.isNull(accountCancellationEntry)) {
            stateJson.put(AccountCancellationKeys.STATE, AccountCancellationEntry.AccountState.ACTIVE.getId());
        } else {
            stateJson.put(AccountCancellationKeys.STATE, AccountCancellationEntry.AccountState.AWAITING_CANCELLATION.getId());
            stateJson.put(AccountCancellationKeys.REMOVAL_DATE, accountCancellationEntry.getRetentionEndTime());
            stateJson.put(AccountCancellationKeys.SURVEY_DONE, accountCancellationEntry.isSurveyDone());
        }
        return stateJson;
    }

    /**
     * <p>
     * Adds the survey info for account cancellation
     * </p>
     *
     * @param orgId       The organisation Id
     * @param data        The data {@link JsonObject}
     * @param crmResponse The {@link CrmResponse}
     */
    private void submitSurvey(final long orgId, final JsonObject data, final CrmResponse crmResponse) {

        if (accountCancellationManager.exists(orgId)) {
            final String retentionReason = data.getString(AccountCancellationKeys.RETENTION_REASON);
            final Short reConsiderationId = data.getShort(AccountCancellationKeys.LIKELY_TO_RECONSIDER);
            final JsonObject alternateJson = data.getJsonObject(AccountCancellationKeys.ALTERNATIVE);
            final Short alternateCrmId = alternateJson.getShort(AccountCancellationKeys.ID);
            final Survey survey = new Survey(retentionReason, Survey.AlternateProduct.getAlternateProduct(alternateCrmId),
                    Survey.Reconsideration.getReConsiderationType(reConsiderationId));

            if (alternateJson.containsKey(AccountCancellationKeys.NAME)) {
                survey.setAlternateProductName(alternateJson.getString(AccountCancellationKeys.NAME));
            }
            survey.setSurveyJson(data);
            final ErrorCodeProvider errorCodeProvider = CommonErrorCodes.DATA_VALIDATION_FAILED;
            final Set<ConstraintViolation<Survey>> constraintViolations = modelValidator.validate(survey, Group.AddSurvey.class);
            final Collection<ErrorDescriptor> errors = constraintViolations.stream()
                    .map(violation -> new ErrorDescriptor(ErrorCode.get(errorCodeProvider, violation.getMessage()),
                            null, violation.getPropertyPath().toString())).collect(Collectors.toSet());

            if (errors.isEmpty()) {
                accountCancellationManager.submitSurvey(orgId, survey);
                crmResponse.put(AccountCancellationKeys.MESSAGE, crmResponse.resolveI18nString("i18n.account.cancellation.survey"));
            } else {
                final ErrorCode errorCode = ErrorCode.get(CommonErrorCodes.DATA_VALIDATION_FAILED, "i18n.survey.invalid.details");

                crmResponse.setErrorCode(errorCode);
                crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode), errors));
            }
        } else {
            throw ErrorCode.get(AccountCancellationErrorCodes.ORG_CANCELLATION_REQUEST_NOT_FOUND, "i18n.account.cancellation.request.not.found");
        }
    }

    /**
     * <p>
     * Retains the account cancellation
     * </p>
     *
     * @param orgId       Represents the organization Id
     * @param crmResponse The {@link CrmResponse}
     */
    private void retain(final Long orgId, final CrmResponse crmResponse) {

        if (accountCancellationManager.exists(orgId)) {

            if (accountCancellationManager.canRetain(orgId)) {
                accountCancellationManager.retain(orgId);
                crmResponse.put(AccountCancellationKeys.MESSAGE, crmResponse.resolveI18nString("i18n.account.cancellation.withdrawn"));
            } else {
                throw ErrorCode.get(AccountCancellationErrorCodes.RETENTION_TIME_EXPIRED, "i18n.account.cancellation.retention.time.expired");
            }
        } else {
            throw ErrorCode.get(AccountCancellationErrorCodes.ORG_CANCELLATION_REQUEST_NOT_FOUND, "i18n.account.cancellation.request.not.found");
        }
    }
}