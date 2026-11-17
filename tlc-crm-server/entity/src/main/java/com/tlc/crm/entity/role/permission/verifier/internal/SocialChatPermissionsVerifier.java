package com.tlc.crm.entity.role.permission.verifier.internal;

import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.field.permissions.PermissionApiKeys;
import com.tlc.crm.common.iam.service.RoleService;
import com.tlc.crm.entity.role.permission.validator.SourcePermissionValidator;
import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.crm.security.permission.annotation.authorizer.SocialChatAuthorizer;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Handles source-related authorization for messenger module APIs.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Verifier(authorizerClass = SocialChatAuthorizer.class)
public class SocialChatPermissionsVerifier extends AbstractPermissionsVerifier<SocialChatAuthorizer> {

    public SocialChatPermissionsVerifier() {
    }

    private static final class Instance {
        private static final SocialChatPermissionsVerifier INSTANCE = new SocialChatPermissionsVerifier();
    }

    /**
     * <p>
     * Gets {@link SocialChatPermissionsVerifier}.
     * </p>
     *
     * @return {@link SocialChatPermissionsVerifier}.
     */
    @InstanceProvider
    public static SocialChatPermissionsVerifier getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param request  {@link VerificationRequest}.
     * @param response {@link VerificationResponse}.
     */
    @Override
    public void verify(final VerificationRequest<SocialChatAuthorizer> request, final VerificationResponse response) {
        final long orgId = request.orgId();
        final long roleId = request.roleId();
        final SocialChatAuthorizer authorizer = request.authorizer();
        final RolePermission rolePermission = rolePermissionService.getPermission(orgId, roleId,
                authorizer.category(), authorizer.subCategory(), authorizer.action());

        if (rolePermission.isGranted()) {
            final JsonObject data = request.requestJson().getJsonObject(PermissionApiKeys.DATA);
            final String apiType = request.apiDetails().type();

            switch (apiType) {
                case PermissionApiKeys.ASSIGN_OWNER, PermissionApiKeys.UN_ASSIGN_OWNER ->
                        validateOwnerRelatedRequest(orgId, data, response);
                case PermissionApiKeys.LINK_TO_CONTACT, PermissionApiKeys.LINK_TO_DEAL ->
                        validateEntityLinkRequest(orgId, request.userId(), roleId, apiType, data, response);
            }
        } else {
            response.addError("i18n.role.permission.error.entity.privilege.restricted");
        }
    }

    /**
     * <<p>
     * Validates entity link request.
     * </p>
     *
     * @param orgId                An organization's id.
     * @param data                 Request data.
     * @param verificationResponse {@link VerificationResponse}.
     */
    private void validateOwnerRelatedRequest(final Long orgId, final JsonObject data,
                                             final VerificationResponse verificationResponse) {
        final Long ownerId = data.getLong(PermissionApiKeys.OWNER_ID);
        final RoleService roleService = RoleService.getInstance();
        final CrmRole role = roleService.getRoleByUser(orgId, ownerId);

        if (Objects.isNull(role)) {
            throw ErrorCode.get(PermissionErrorCodes.ROLE_NOT_FOUND, "i18n.permission.role.required");
        } else {
            final RolePermission rolePermission = rolePermissionService.getPermission(orgId, role.id(),
                    Category.Type.ADMIN_ACCESS, SubCategory.Type.MANAGE_CONVERSATIONAL_CHANNELS, Action.Type.ACCESS);

            if (!rolePermission.isGranted()) {
                verificationResponse.addError("i18n.role.permission.error.chat.user.restricted");
            }
        }
    }

    /**
     * <<p>
     * Validates entity link request.
     * </p>
     *
     * @param orgId                An organization's id.
     * @param userId               A user's id.
     * @param roleId               Role's id.
     * @param type                 API type.
     * @param data                 Request data.
     * @param verificationResponse {@link VerificationResponse}.
     */
    private void validateEntityLinkRequest(final Long orgId, final Long userId, final Long roleId, final String type,
                                           final JsonObject data, final VerificationResponse verificationResponse) {
        final SourcePermissionValidator sourcePermissionValidator = new SourcePermissionValidator(orgId, roleId);
        final boolean isCreation;
        final String source;
        final SubCategory.Type subcategoryType;

        switch (type) {
            case PermissionApiKeys.LINK_TO_CONTACT -> {
                isCreation = data.containsKey(PermissionApiKeys.CONTACT);
                source = PermissionApiKeys.CONTACT;
                subcategoryType = SubCategory.Type.CONTACTS;
            }
            case PermissionApiKeys.LINK_TO_DEAL -> {
                isCreation = data.containsKey(PermissionApiKeys.DEAL);
                source = PermissionApiKeys.DEAL;
                subcategoryType = SubCategory.Type.DEALS;
            }
            default -> {
                isCreation = false;
                source = null;
                subcategoryType = null;
            }
        }

        if (Objects.nonNull(source) && sourcePermissionValidator.validate(source)) {

            if (isCreation) {
                sourcePermissionValidator.validateCreatePermission(subcategoryType, verificationResponse);
            } else {
                final Long entityId;
                final StoreType storeType;

                switch (source) {
                    case PermissionApiKeys.DEAL -> {
                        entityId = data.getLong(PermissionApiKeys.DEAL_ID);
                        storeType = StoreType.DEAL;
                    }
                    case PermissionApiKeys.CONTACT -> {
                        entityId = data.getLong(PermissionApiKeys.CONTACT_ID);
                        storeType = StoreType.CONTACT;
                    }
                    default -> {
                        entityId = null;
                        storeType = null;
                    }
                }
                sourcePermissionValidator.validateEntityPermission(userId, storeType, subcategoryType, Set.of(entityId), verificationResponse);
            }
        } else {
            verificationResponse.addError("i18n.role.permission.error.entity.privilege.restricted");
        }
    }
}