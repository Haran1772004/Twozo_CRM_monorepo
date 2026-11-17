package com.tlc.crm.entity.role.permission.handler;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.field.permissions.PermissionApiKeys;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.models.Visibility;
import com.tlc.crm.entity.filter.fields.FilterFields;
import com.tlc.crm.entity.filter.fields.FilterVisibilityAttributes;
import com.tlc.crm.entity.role.permission.handler.request.CommonApiHandlerRequest;
import com.tlc.crm.entity.role.permission.util.SubCategoryTypeUtils;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.security.permission.PermissionLoader;
import com.tlc.crm.security.permission.handler.ApiPermissionHandler;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Objects;

/**
 * <p>
 * Handles the {@link RolePermission} of the common APIs.
 * NOTE: Common APIs -> Which is defined commonly for all the sources(Contact, Company, Deal, Product)
 * EXAMPLE: Dropdown, View share, import, export, etc,..
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CommonApiPermissionHandler implements ApiPermissionHandler<CommonApiHandlerRequest> {

    private final RolePermissionService rolePermissionService;
    private final FieldSourceService fieldSourceService;
    private final PermissionLoader permissionLoader;

    private static final class Instance {
        private static final CommonApiPermissionHandler INSTANCE = new CommonApiPermissionHandler();
    }

    private CommonApiPermissionHandler() {
        rolePermissionService = RolePermissionService.getInstance();
        fieldSourceService = FieldSourceService.getInstance();
        permissionLoader = PermissionLoader.getInstance();
    }

    public static CommonApiPermissionHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Handles the {@link RolePermission} of the common APIs which depends on {@link Action.Type}.
     * </p>
     *
     * @param apiHandlerRequest Represents the request of the API handler.
     */
    @Override
    public void handle(final CommonApiHandlerRequest apiHandlerRequest) {
        final VerificationRequest<?> verificationRequest = apiHandlerRequest.getVerificationRequest();
        final VerificationResponse verificationResponse = apiHandlerRequest.getVerificationResponse();
        final Category.Type categoryType = apiHandlerRequest.getCategoryType();
        final Action.Type actionType = apiHandlerRequest.getActionType();
        final long orgId = verificationRequest.orgId();
        final long roleId = verificationRequest.roleId();
        final JsonObject requestJson = verificationRequest.requestJson();
        final JsonObject data = requestJson.optJsonObject(FieldKeys.DATA.getName());

        switch (actionType) {
            case VIEW -> {
                final String nameKey = FieldKeys.NAME.getName();

                if (Objects.nonNull(data) && data.containsKey(nameKey)) {
                    final String dropdownName = data.getString(nameKey);
                    final SubCategory.Type subCategoryType = SubCategory.Type.getByDropdownName(dropdownName);

                    if (Objects.nonNull(subCategoryType)) {
                        final RolePermission permission = rolePermissionService.getPermission(orgId, roleId, categoryType, subCategoryType, actionType);

                        permissionLoader.loadPermissionIntoContext(permission);
                    }
                }
            }
            case SHARE_VIEW -> {

                if (Objects.nonNull(data) && data.containsKey(PermissionApiKeys.SOURCE_ID)) {
                    final Long sourceId = data.getLong(PermissionApiKeys.SOURCE_ID);
                    final FieldSource source = fieldSourceService.getSource(sourceId);

                    if (Objects.nonNull(source) && source.isCommon()) {
                        final SubCategory.Type subCategoryType = SubCategoryTypeUtils.getBySourceName(source.getName());
                        final RolePermission rolePermission = rolePermissionService.getPermission(orgId, roleId, categoryType, subCategoryType, actionType);

                        if (!rolePermission.isGranted()) {
                            final String filterVisibilityKey = FilterFields.FILTER_VISIBILITY.getName();

                            if (data.containsKey(filterVisibilityKey)) {
                                final JsonObject filterVisibilityJson = data.getJsonObject(filterVisibilityKey);
                                final short visibilityId = filterVisibilityJson.getShort(FilterVisibilityAttributes.VISIBILITY_ID.getName());
                                final Visibility visibility = Visibility.get(visibilityId);

                                if (Objects.nonNull(visibility) && Visibility.JUST_ME != visibility) {
                                    verificationResponse.addError(String.format(verificationResponse.resolveI18nKey("i18n.role.permission.error.shareView.restricted")));
                                }
                            }
                        }
                        permissionLoader.loadPermissionIntoContext(rolePermission);
                    }
                }
            }
            case EXPORT_RECORDS -> {

                if (requestJson.containsKey(PermissionApiKeys.EXPORTER_NAME)) {
                    final SubCategory.Type subCategoryType = SubCategory.Type.getByExporterName(requestJson.getString(PermissionApiKeys.EXPORTER_NAME));

                    if (Objects.nonNull(subCategoryType)) {
                        final RolePermission rolePermission = rolePermissionService.getPermission(orgId, roleId, categoryType, subCategoryType, actionType);

                        if (!rolePermission.isGranted()) {
                            verificationResponse.addError(String.format(verificationResponse.resolveI18nKey("i18n.role.permission.error.export.restricted"),
                                    verificationResponse.resolveI18nKey(rolePermission.getAction().getDisplayName())));
                        }
                        permissionLoader.loadPermissionIntoContext(rolePermission);
                    }
                }
            }
            case IMPORT_RECORDS -> {
                switch (verificationRequest.apiDetails().type()) {
                    case "sources" -> {
                    }
                    case "fields" -> {
                        final String sourceName = data.getString(PermissionApiKeys.SOURCE);

                        if (Source.isPrimarySource(Source.get(sourceName))) {
                            final RolePermission rolePermission = rolePermissionService.getPermission(orgId, roleId, categoryType, SubCategoryTypeUtils.getBySourceName(sourceName), actionType);

                            if (!rolePermission.isGranted()) {
                                verificationResponse.addError(String.format(verificationResponse.resolveI18nKey("i18n.role.permission.error.import.restricted"),
                                        verificationResponse.resolveI18nKey(rolePermission.getAction().getDisplayName())));
                            }
                            permissionLoader.loadPermissionIntoContext(rolePermission);
                        }
                    }
                }
            }
            case ACCESS_RECYCLE_BIN -> {

                if (data.containsKey(PermissionApiKeys.SOURCE_ID)) {
                    final String sourceName = fieldSourceService.getSource(data.getLong(PermissionApiKeys.SOURCE_ID)).getName();

                    if (Source.isPrimarySource(Source.get(sourceName))) {
                        final RolePermission rolePermission = rolePermissionService.getPermission(orgId, roleId, categoryType, SubCategoryTypeUtils.getBySourceName(sourceName), actionType);

                        permissionLoader.loadPermissionIntoContext(rolePermission);
                    }
                }
            }
            //TODO If any action needs handler of common API, then define the suitable case.
        }
    }
}
