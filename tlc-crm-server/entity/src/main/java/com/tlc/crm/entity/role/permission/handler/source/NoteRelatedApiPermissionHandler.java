package com.tlc.crm.entity.role.permission.handler.source;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.field.permissions.PermissionApiKeys;

import com.tlc.crm.entity.features.api.note.model.extractor.ContentExtractor;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.role.permission.handler.RelatedApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.request.RelatedApiHandlerRequest;
import com.tlc.crm.entity.role.permission.util.SubCategoryTypeUtils;
import com.tlc.crm.entity.role.permission.validator.EntityPermissionValidator;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.web.HttpStatus;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles source-related authorization for note APIs
 *
 * @author Sathish
 * @version 1.0
 */
public class NoteRelatedApiPermissionHandler extends RelatedApiPermissionHandler {

    private final RoleManager roleManager;

    private NoteRelatedApiPermissionHandler() {
        roleManager = RoleManager.getInstance();
    }

    private static final class Instance {
        private static final NoteRelatedApiPermissionHandler NOTE_RELATED_API_PERMISSION_HANDLER = new NoteRelatedApiPermissionHandler();
    }

    public static NoteRelatedApiPermissionHandler getInstance() {
        return NoteRelatedApiPermissionHandler.Instance.NOTE_RELATED_API_PERMISSION_HANDLER;
    }

    /**
     * {@inheritDoc}
     *
     * @param apiHandlerRequest Represents the request of the API handler.
     */
    @Override
    public void handle(final RelatedApiHandlerRequest apiHandlerRequest) {
        final VerificationRequest<?> verificationRequest = apiHandlerRequest.getVerificationRequest();
        final Category.Type categoryType = apiHandlerRequest.getCategoryType();
        final String sourceKey = NoteFields.SOURCE.getName();
        final JsonObject requestJson = verificationRequest.requestJson();
        final JsonObject data = requestJson.optJsonObject(FieldKeys.DATA.getName());

        if (data.containsKey(sourceKey)) {
            final String sourceName = data.getString(sourceKey);

            validateSourcePermission(sourceName, apiHandlerRequest);

            if (!apiHandlerRequest.getVerificationResponse().hasErrors()) {
                final String entityIdKey = NoteFields.ENTITY_ID.getName();

                if (data.containsKey(entityIdKey)) {
                    final SubCategory.Type subCategoryType = SubCategoryTypeUtils.getBySourceName(sourceName);

                    if (Objects.nonNull(subCategoryType)) {
                        final long roleId = verificationRequest.roleId();
                        final StoreType storeType = SubCategoryTypeUtils.findStoreType(subCategoryType);
                        final Set<Long> entityIds = Set.of(data.getLong(entityIdKey));

                        switch (apiHandlerRequest.getVerificationRequest().apiDetails().type()) {
                            case PermissionApiKeys.CREATE, PermissionApiKeys.UPDATE -> {
                                final long orgId = verificationRequest.orgId();
                                final String noteContent = data.getString(PermissionApiKeys.CONTENT);
                                final ContentExtractor contentExtractor = new ContentExtractor(orgId, noteContent);
                                final Collection<Long> crmUserIds = contentExtractor.extractMentions()
                                        .stream().map(CrmUser::id).collect(Collectors.toSet());

                                if (!crmUserIds.isEmpty()) {
                                    final Map<Long, CrmRole> userRoles = roleManager.getRolesByUsers(orgId, crmUserIds);

                                    for (final Map.Entry<Long, CrmRole> userRoleEntry : userRoles.entrySet()) {
                                        final Long userRoleId = userRoleEntry.getValue().id();
                                        final EntityPermissionValidator entityPermissionValidator =
                                                new EntityPermissionValidator(orgId, userRoleEntry.getKey(), userRoleId);

                                        validateEntityPermission(userRoleId, storeType, categoryType, subCategoryType, entityIds,
                                                apiHandlerRequest, HttpStatus.OK, entityPermissionValidator, false);
                                    }
                                }
                            }
                        }
                        validateEntityPermission(roleId, storeType, categoryType, subCategoryType, entityIds, apiHandlerRequest);
                    }
                }
            }
        }
    }
}