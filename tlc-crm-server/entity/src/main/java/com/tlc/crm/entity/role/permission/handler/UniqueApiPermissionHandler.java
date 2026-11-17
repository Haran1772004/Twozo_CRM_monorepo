package com.tlc.crm.entity.role.permission.handler;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.field.permissions.PermissionApiKeys;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.role.permission.handler.request.UniqueApiHandlerRequest;
import com.tlc.crm.security.permission.PermissionLoader;
import com.tlc.crm.security.permission.handler.ApiPermissionHandler;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

/**
 * <p>
 * Handles the {@link RolePermission} of the unique APIs.
 * NOTE: Unique APIs -> Which API is need to handle uniquely.
 * EXAMPLE: Share email templates folder, etc,..
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class UniqueApiPermissionHandler implements ApiPermissionHandler<UniqueApiHandlerRequest> {

    private final RolePermissionService rolePermissionService;
    private final TeamManager teamManager;
    private final TerritoryManager territoryManager;
    private final PermissionLoader permissionLoader;

    private static final class Instance {
        private static final UniqueApiPermissionHandler INSTANCE = new UniqueApiPermissionHandler();
    }

    private UniqueApiPermissionHandler() {
        rolePermissionService = RolePermissionService.getInstance();
        teamManager = TeamManager.getInstance();
        territoryManager = TerritoryManager.getInstance();
        permissionLoader = PermissionLoader.getInstance();
    }

    public static UniqueApiPermissionHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Handles the {@link RolePermission} of the unique APIs which depends on {@link SubCategory.Type}.
     * </p>
     *
     * @param apiHandlerRequest Represents the request of the API handler.
     */
    @Override
    public void handle(final UniqueApiHandlerRequest apiHandlerRequest) {
        final VerificationRequest<?> verificationRequest = apiHandlerRequest.getVerificationRequest();
        final VerificationResponse verificationResponse = apiHandlerRequest.getVerificationResponse();
        final Category.Type categoryType = apiHandlerRequest.getCategoryType();
        final SubCategory.Type subCategoryType = apiHandlerRequest.getSubcategoryType();
        final long orgId = verificationRequest.orgId();
        final long userId = verificationRequest.userId();
        final long roleId = verificationRequest.roleId();
        final JsonObject requestJson = verificationRequest.requestJson();
        final String type = verificationRequest.apiDetails().type();
        final JsonObject data = requestJson.optJsonObject(FieldKeys.DATA.getName());

        switch (subCategoryType) {
            case SHARE_EMAIL_TEMPLATES_FOLDER -> {//TODO need to validate search bar request of user, team, territory.
                if (Objects.equals(PermissionApiKeys.SETTINGS, type)) {
                    final Function<Action.Type, RolePermission> permissionProvider = actionType ->
                            rolePermissionService.getPermission(orgId, roleId, categoryType, subCategoryType, actionType);
                    final RolePermission subCategoryPermission = permissionProvider.apply(Action.Type.ACCESS);

                    if (subCategoryPermission.isGranted()) {
                        final RolePermission actionPermission = permissionProvider.apply(Action.Type.EVERYONE);

                        if (!actionPermission.isGranted()) {

                            if (data.containsKey(PermissionApiKeys.TEAMS)) {
                                final RolePermission teamPermission = permissionProvider.apply(Action.Type.MY_TEAMS);
                                final Collection<Object> teamIds = data.getJsonArray(PermissionApiKeys.TEAMS).toList();

                                if (!teamPermission.isGranted() || !teamManager.getUserTeamIds(orgId, userId).containsAll(teamIds)) {
                                    verificationResponse.addError(verificationResponse.resolveI18nKey("i18n.role.permission.error.shareEmailTemplatesFolder.team.restricted"));
                                }
                            }

                            if (data.containsKey(PermissionApiKeys.TERRITORIES)) {
                                final RolePermission territoryPermission = permissionProvider.apply(Action.Type.MY_TERRITORIES);
                                final Collection<Object> territoryIds = data.getJsonArray(PermissionApiKeys.TERRITORIES).toList();

                                if (!territoryPermission.isGranted() || !territoryManager.getUserTerritoryIds(orgId, userId).containsAll(territoryIds)) {
                                    verificationResponse.addError(verificationResponse.resolveI18nKey("i18n.role.permission.error.shareEmailTemplatesFolder.territory.restricted"));
                                }
                            }

                            if (data.containsKey(PermissionApiKeys.USERS)) {
                                final Collection<Object> userIds = data.getJsonArray(PermissionApiKeys.USERS).toList();
                                final Collection<Long> teamMateIds = teamManager.getTeamMateIds(orgId, userId);
                                final Collection<Long> territoryMateIds = territoryManager.getTerritoryMateIds(orgId, userId);

                                if (userIds.stream().anyMatch(userIdentity -> !(teamMateIds.contains(userIdentity)
                                        || territoryMateIds.contains(userIdentity)))) {
                                    verificationResponse.addError(verificationResponse.resolveI18nKey("i18n.role.permission.error.shareEmailTemplatesFolder.user.restricted"));
                                }
                            }
                        }
                        permissionLoader.loadPermissionIntoContext(actionPermission);
                    } else {
                        final String visibility = data.optString(PermissionApiKeys.VISIBLE_TO, null);

                        if (!Objects.equals(PermissionApiKeys.JUST_ME, visibility)) {
                            verificationResponse.addError(verificationResponse.resolveI18nKey("i18n.role.permission.error.shareEmailTemplatesFolder.restricted"));
                        }
                    }
                }
            }
            //TODO If any sub-category needs handler of special case API, then define the suitable case.
        }
    }
}
