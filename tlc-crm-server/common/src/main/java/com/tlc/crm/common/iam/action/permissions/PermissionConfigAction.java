package com.tlc.crm.common.iam.action.permissions;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

import com.tlc.crm.security.permission.model.Permission;
import com.tlc.crm.security.permission.model.Permissions;
import com.tlc.crm.security.permission.manager.PermissionManager;

import com.tlc.i18n.I18nResolutionAdapter;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.ValidatorService;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import jakarta.validation.ConstraintViolation;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link Permissions} related web actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AuthorizationController
 */
public abstract class PermissionConfigAction<U extends Permission, T extends Permissions<U>> extends AuthorizationController {

    private static final String DATA = "data";
    private static final String TYPE = "type";
    private static final String UPDATE = "update";
    private static final String GET = "get";
    private static final String UPDATED_RECORDS = "updatedRecords";

    private final ValidatorService validatorService;

    public PermissionConfigAction() {
        validatorService = ValidatorServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  which holds the crm request.
     * @param response which holds the crm response.
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(TYPE);
        final JsonObject data = requestJson.getJsonObject(DATA);
        final Long orgId = request.orgId();

        switch (type) {
            case UPDATE -> update(response, orgId, List.of(convert(orgId, data)));
            case GET -> {
                final T permissions = get(orgId, data);
                final JsonObject permissionJson = convert(permissions, request.getI18NCRMResolutionAdapter());

                loadPermissionJsonIntoResponse(permissionJson, response);
            }
            default -> handle(request, response);
        }
    }

    /**
     * <p>
     * Updates the given permissions of the corresponding entity.
     * </p>
     *
     * @param permissions Holds the details of the permission.
     */
    private void update(final CrmResponse crmResponse, final Long orgId, final Collection<T> permissions) {
        final Collection<ErrorDescriptor> errors = validate(permissions);

        if (errors.isEmpty() && validatePermissionDetails(orgId, permissions)) {
            final ProcessOutcome processOutcome = getPermissionManager().update(orgId, permissions);

            crmResponse.put(UPDATED_RECORDS, processOutcome.getProcessedRecords());
            final ErrorSummary errorSummary = processOutcome.getErrorSummary();

            if (Objects.nonNull(errorSummary)) {
                crmResponse.setErrorCode(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed"));
                crmResponse.setErrorSummary(errorSummary);
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.UPDATE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Validates the permissions against the given validator groups.
     * </p>
     *
     * @param permissions Holds the details of the permission.
     */
    private Collection<ErrorDescriptor> validate(final Collection<T> permissions) {
        final ModelValidator validator = validatorService.modelValidator();
        final Set<ConstraintViolation<T>> violations = new HashSet<>();
        final ErrorCodeProvider errorCodeProvider = CommonErrorCodes.DATA_VALIDATION_FAILED;

        for (final T permission : permissions) {
            violations.addAll(validator.validate(permission, Group.Update.class));
        }

        return violations.stream().map(violation -> new ErrorDescriptor(ErrorCode.get(errorCodeProvider, violation.getMessage()), null,
                violation.getPropertyPath().toString())).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Handles the given {@link CrmRequest} and load the response into {@link CrmResponse}.
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    protected abstract void handle(final CrmRequest request, final CrmResponse response);

    /**
     * Validates the given details of the {@link Permissions}.
     *
     * @param orgId       Represents the organization id.
     * @param permissions Collection of {@link Permissions}.
     * @return true if the given details are valid.
     */
    protected abstract boolean validatePermissionDetails(final Long orgId, final Collection<T> permissions);

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link Permissions}.
     * </p>
     *
     * @param orgId      Represents the organisation id.
     * @param jsonObject Holds the permission values as json.
     * @return {@link Permissions} Holds the details of the permissions.
     */
    protected abstract T convert(final long orgId, final JsonObject jsonObject);

    /**
     * <p>
     * Converts the {@link Permissions} into {@link JsonObject}.
     * </p>
     *
     * @param permissions           Which holds the values of permission.
     * @param i18nResolutionAdapter {@link I18nResolutionAdapter} to resolve the text language.
     * @return {@link JsonObject} holds the permission values as json.
     */
    protected abstract JsonObject convert(final T permissions, final I18nResolutionAdapter i18nResolutionAdapter);

    /**
     * <p>
     * Fetches the {@link PermissionManager} instance.
     * </p>
     *
     * @return {@link PermissionManager} instance.
     */
    protected abstract PermissionManager<U, T> getPermissionManager();

    /**
     * <p>
     * Fetches the {@link Permissions} of the given entity id.
     * </p>
     *
     * @param orgId Represents the organisation ID.
     * @param data  Represents the request data.
     * @return The {@link Permissions}.
     */
    protected abstract T get(final Long orgId, final JsonObject data);

    /**
     * <p>
     * Loads the permission json into {@link CrmResponse}.
     * </p>
     *
     * @param permissionJson Holds the details of the permissions.
     * @param response       Represents the {@link CrmResponse}.
     */
    protected abstract void loadPermissionJsonIntoResponse(final JsonObject permissionJson, final CrmResponse response);
}
