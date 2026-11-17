package com.tlc.crm.entity.role.permission.validator;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldSourceService;

import com.tlc.crm.security.permission.PermissionLoader;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.validator.PermissionValidator;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Validator for checking the permissions of sources accessible by the user
 *
 * @author Sundar
 * @version 1.0
 */
public class SourcePermissionValidator implements PermissionValidator {

    public static final Type TYPE = () -> "SOURCE";

    private final long orgId;
    private final long roleId;
    private final RolePermissionService rolePermissionService;
    private final FieldSourceService fieldSourceService;
    private final PermissionLoader permissionLoader;

    public SourcePermissionValidator(final long orgId, final long roleId) {
        this.orgId = orgId;
        this.roleId = roleId;
        this.rolePermissionService = RolePermissionService.getInstance();
        this.fieldSourceService = FieldSourceService.getInstance();
        this.permissionLoader = PermissionLoader.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Type}
     */
    @Override
    public Type getType() {
        return TYPE;
    }

    /**
     * Validates whether the current user has permission to access the specified source
     *
     * @param source the source to validate
     * @return true if the user is allowed to access the specified source
     */
    public boolean validate(final Source source) {
        final Optional<SubCategory.Type> optionalSubCategoryType = fetchAssociatedSubCategory(source);

        if (optionalSubCategoryType.isPresent()) {
            final SubCategory.Type subCategoryType = optionalSubCategoryType.get();
            final RolePermission permission = rolePermissionService.getPermission(orgId, roleId, Category.Type.SOURCES, subCategoryType, Action.Type.ACCESS);

            return permission.isGranted();
        }
        return true;
    }

    /**
     * Validates whether the current user has permission to access the specified source
     *
     * @param sourceName the source name to validate
     * @return true if the user is allowed to access the specified source
     */
    public boolean validate(final String sourceName) {
        return validate(Source.get(sourceName));
    }

    /**
     * Validates whether the current user has permission to access the specified source
     *
     * @param sourceId the source ID to validate
     * @return true if the user is allowed to access the specified source
     */
    public boolean validate(final Long sourceId) {
        final FieldSource fieldSource = fieldSourceService.getSource(sourceId);

        return Objects.nonNull(fieldSource) && validate(fieldSource);
    }

    /**
     * Validates whether the current user has permission to access the specified {@link FieldSource}
     *
     * @param fieldSource the {@link FieldSource} to validate
     * @return true if the user is allowed to access the specified {@link FieldSource}
     */
    public boolean validate(final FieldSource fieldSource) {
        return validate(Source.get(fieldSource.getName()));
    }

    /**
     * Validates if the provided role ID has permission to access the specified source. If the role is not permitted, the {@link VerificationResponse} is updated with the corresponding error message.
     *
     * @param sourceName           The source name to validate
     * @param verificationResponse The verification response to be updated with the appropriate error message if the role is not permitted
     */
    public void validate(final String sourceName, final VerificationResponse verificationResponse) {
        final Source source = Source.get(sourceName);

        fetchAssociatedSubCategory(source).ifPresent(subCategoryType -> {
            final RolePermission permission = rolePermissionService.getPermission(orgId, roleId, Category.Type.SOURCES, subCategoryType, Action.Type.ACCESS);

            if (!validate(permission)) {
                verificationResponse.addSubCategoryActionError(permission.getSubCategory(), permission.getAction());
            }
        });
    }

    /**
     * Validates if the provided role ID has permission to access the specified source. If the role is not permitted, the {@link VerificationResponse} is updated with the corresponding error message.
     *
     * @param sourceId             The source ID to validate
     * @param verificationResponse The verification response to be updated with the appropriate error message if the role is not permitted
     */
    public void validate(final Long sourceId, final VerificationResponse verificationResponse) {
        final FieldSource fieldSource = fieldSourceService.getSource(sourceId);

        if (Objects.nonNull(fieldSource)) {
            validate(fieldSource.getName(), verificationResponse);
        }
    }

    /**
     * Validates the create action permission for the given role and subcategory type
     *
     * @param subCategoryType Represents the subcategory type
     * @param response        The verification response to be updated with the appropriate error message if the role is not permitted
     */
    public void validateCreatePermission(final SubCategory.Type subCategoryType, final VerificationResponse response) {
        final RolePermission rolePermission = rolePermissionService.getPermission(response.getOrgId(), roleId, Category.Type.SOURCES, subCategoryType, Action.Type.CREATE);
        final SubCategory subCategory = rolePermission.getSubCategory();

        if (!rolePermission.isGranted()) {
            response.addSubCategoryActionError(subCategory, rolePermission.getAction());
        }
    }

    /**
     * <p>
     * Validates the entity permission for the given entity IDs
     * </p>.
     *
     * @param userId          Represents the user's ID.
     * @param storeType       Represents the store type.
     * @param subCategoryType Represents the sub category type.
     * @param entityIds       Which contains the {@link java.util.Collection} of entity IDs to validate.
     * @param response        {@link VerificationResponse}.
     */
    public void validateEntityPermission(final long userId, final StoreType storeType, final SubCategory.Type subCategoryType,
                                         final Set<Long> entityIds, final VerificationResponse response) {
        final EntityPermissionValidator entityPermissionValidator = new EntityPermissionValidator(orgId, userId, roleId);
        final RolePermission rolePermission = rolePermissionService.getPermission(response.getOrgId(), roleId,
                Category.Type.SOURCES, Objects.requireNonNull(subCategoryType), Action.Type.VIEW);

        if (!entityPermissionValidator.validate(entityIds, storeType, rolePermission.getScope())) {
            response.addEntityPrivilegeError(rolePermission.getSubCategory());
        } else {
            permissionLoader.loadPermissionIntoContext(rolePermission);
        }
    }

    /**
     * Validates the specified {@link RolePermission} access is granted or not
     *
     * @param rolePermission the role permission to validate
     * @return true if the role permission is granted; false otherwise
     */
    private boolean validate(final RolePermission rolePermission) {
        return Objects.nonNull(rolePermission) && rolePermission.isGranted();
    }

    /**
     * <p>
     * Fetches the {@link SubCategory.Type} associated with the given {@link Source}.
     * </p>
     *
     * @param source Represents the source
     * @return {@link StoreType}.
     */
    private Optional<SubCategory.Type> fetchAssociatedSubCategory(final Source source) {

        return Optional.ofNullable(switch (source) {
            case COMPANY -> SubCategory.Type.COMPANIES;
            case CONTACT -> SubCategory.Type.CONTACTS;
            case DEAL -> SubCategory.Type.DEALS;
            case PRODUCT -> SubCategory.Type.PRODUCTS;
            default -> null;
        });
    }
}