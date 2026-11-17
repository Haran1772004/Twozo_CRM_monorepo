package com.tlc.crm.security.permission.verifier.response;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.crm.security.permission.model.LimitActionType;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.crm.security.permission.validator.FieldPermissionValidator;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.I18nResolver;

import com.tlc.web.HttpStatus;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Represents a response from the verifier.
 *
 * @author sundar
 * @version 1.0
 */
public class VerificationResponse {
    private final Long orgId;
    private final Long userId;
    private final I18nResolutionAdapter i18nResolutionAdapter;
    private final Collection<String> errors;

    private FieldPermissionValidator fieldPermissionValidator;
    private HttpStatus status;
    private boolean hasErrors;

    public VerificationResponse(final Long orgId, final Long userId, final I18nResolutionAdapter i18nResolutionAdapter) {
        this.orgId = orgId;
        this.userId = userId;
        this.i18nResolutionAdapter = i18nResolutionAdapter;
        this.errors = new HashSet<>();
        this.status = HttpStatus.FORBIDDEN;
    }

    public Long getOrgId() {
        return orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public I18nResolutionAdapter getI18nResolutionAdapter() {
        return i18nResolutionAdapter;
    }

    public Collection<String> getErrors() {
        return errors;
    }

    public void setFieldPermissionValidator(final FieldPermissionValidator fieldPermissionValidator) {
        this.fieldPermissionValidator = fieldPermissionValidator;
    }

    public FieldPermissionValidator getFieldPermissionValidator() {
        return fieldPermissionValidator;
    }

    /**
     * Checks whether the response has any errors
     *
     * @return true if the response has any errors
     */
    public boolean hasErrors() {
        return hasErrors;
    }

    /**
     * Gets the permission denied {@link ErrorCode} if the response has any errors
     *
     * @return the permission denied {@link ErrorCode} if the response has any errors
     */
    public ErrorCode getErrorCode() {
        return hasErrors ? ErrorCode.get(PermissionErrorCodes.PERMISSION_DENIED, "i18n.role.permission.error.action.denied") : null;
    }

    /**
     * Adds the error message into the response.
     *
     * @param error Represents the error message to be added.
     */
    public void addError(final String error) {
        errors.add(error);
        hasErrors = true;
    }

    /**
     * Adds the entity privilege error message into the response.
     *
     * @param subCategory Holds the detail of sub-category of the permission.
     */
    public void addEntityPrivilegeError(final SubCategory subCategory) {
        addError(String.format(resolveI18nKey("i18n.role.permission.error.entity.privilege.restricted"),
                resolveI18nKey(subCategory.getDisplayName())));
    }

    /**
     * Adds the sub-category action error message into the response.
     *
     * @param subCategory Holds the detail of sub-category of the permission.
     * @param action      Holds the detail of action of the permission.
     */
    public void addSubCategoryActionError(final SubCategory subCategory, final Action action) {
        final String subCategoryName = resolveI18nKey(subCategory.getDisplayName());
        final String error = Objects.isNull(action) || Action.Type.ACCESS == action.getType()
                ? String.format(resolveI18nKey("i18n.role.permission.error.subcategory.access.restricted"), subCategoryName)
                : String.format(resolveI18nKey("i18n.role.permission.error.subcategory.action.restricted"),
                resolveI18nKey(action.getDisplayName()), subCategoryName);

        addError(error);
    }

    /**
     * Adds the field privilege error message into the response.
     *
     * @param fieldName Represents the field name.
     */
    public void addFieldPrivilegeError(final I18nKey fieldName) {
        addError(String.format(resolveI18nKey("i18n.role.permission.error.field.edit.privilege.restricted"), resolveI18nKey(fieldName)));
    }

    /**
     * <p>
     * Resolves the i18n key to value based on the locale.
     * </p>
     *
     * @param i18nKey Represents the i18n key.
     * @return Value of the given key.
     */
    public String resolveI18nKey(final String i18nKey) {
        return i18nResolutionAdapter.get(i18nKey);
    }

    /**
     * <p>
     * Resolves the {@link I18nKey} to value based on the locale.
     * </p>
     *
     * @param i18nKey Represents the i18n key.
     * @return Value of the given key.
     */
    public String resolveI18nKey(final I18nKey i18nKey) {
        return i18nResolutionAdapter.get(i18nKey);
    }

    /**
     * Adds the entity privilege error message into the response.
     *
     * @param limitActionType Represents the limit action type.
     */
    public void addLimitationError(final LimitActionType limitActionType) {
        addError(String.format(resolveI18nKey("i18n.role.permission.error.limit.exceeded"),
                resolveI18nKey(limitActionType.getDisplayName())));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(final HttpStatus status) {
        if (Objects.nonNull(status)) {
            this.status = status;
        }
    }
}