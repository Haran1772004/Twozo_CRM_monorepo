package com.tlc.crm.entity.role.permission.handler.source;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.field.permissions.PermissionApiKeys;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.role.permission.handler.RelatedApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.request.RelatedApiHandlerRequest;
import com.tlc.crm.entity.role.permission.util.SubCategoryTypeUtils;
import com.tlc.crm.entity.role.permission.validator.SourcePermissionValidator;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Optional;
import java.util.Set;

/**
 * Handles source-related authorization for email APIs
 *
 * @author Sathish
 * @version 1.0
 */
public class EmailRelatedApiPermissionHandler extends RelatedApiPermissionHandler {

    private EmailRelatedApiPermissionHandler() {
    }

    private static final class Instance {
        private static final EmailRelatedApiPermissionHandler EMAIL_RELATED_API_PERMISSION_HANDLER = new EmailRelatedApiPermissionHandler();
    }

    public static EmailRelatedApiPermissionHandler getInstance() {
        return EmailRelatedApiPermissionHandler.Instance.EMAIL_RELATED_API_PERMISSION_HANDLER;
    }

    /**
     * <p>
     * Handles the particular requests of the email.
     * </p>
     *
     * @param apiHandlerRequest Represents the request of the API handler.
     */
    @Override
    public void handle(final RelatedApiHandlerRequest apiHandlerRequest) {
        final VerificationRequest<?> verificationRequest = apiHandlerRequest.getVerificationRequest();
        final VerificationResponse verificationResponse = apiHandlerRequest.getVerificationResponse();
        final long orgId = verificationRequest.orgId();
        final JsonObject requestJson = verificationRequest.requestJson();
        final JsonObject data = requestJson.optJsonObject(FieldKeys.DATA.getName());

        switch (verificationRequest.apiDetails().type()) {
            case "getEntityConversations" -> {
                final String source = data.getString(PermissionApiKeys.SOURCE);
                final long roleId = verificationRequest.roleId();
                final Category.Type categoryType = apiHandlerRequest.getCategoryType();
                final SubCategory.Type subCategoryType = SubCategoryTypeUtils.getBySourceName(source);
                final Optional<StoreType> optionalStoreType = StoreType.getTypeBySource(source);

                optionalStoreType.ifPresent(storeType -> {
                    validateSourcePermission(source, apiHandlerRequest);
                    validateEntityPermission(roleId, storeType, categoryType, subCategoryType, Set.of(data.getLong(PermissionApiKeys.ENTITY_ID)),
                            apiHandlerRequest);
                });
            }
            case "send", "bulkSend", "draft", "bulkDraft", "reply", "replyAll", "forward" -> {
                if (data.containsKey(PermissionApiKeys.SOURCE)) {

                    validateSourceAndEntityPermissions(data.getString(PermissionApiKeys.SOURCE), verificationRequest.roleId(),
                            Set.of(data.getLong(PermissionApiKeys.ENTITY_ID)), apiHandlerRequest);
                }
            }
            case "linkToDeal" -> {
                validateSourcePermission(PermissionApiKeys.DEAL, apiHandlerRequest);

                if (!verificationResponse.hasErrors()) {
                    final JsonObject dealJson = data.getJsonObject(PermissionApiKeys.DEAL);
                    final Category.Type categoryType = Category.Type.SOURCES;
                    final SubCategory.Type subCategoryType = SubCategory.Type.DEALS;
                    final long roleId = verificationRequest.roleId();

                    if (dealJson.containsKey(PermissionApiKeys.ID)) {
                        validateEntityPermission(roleId, StoreType.DEAL, categoryType, subCategoryType, Set.of(dealJson.getLong(PermissionApiKeys.ID)), apiHandlerRequest);
                    } else {
                        final SourcePermissionValidator validator = new SourcePermissionValidator(orgId, roleId);

                        validator.validateCreatePermission(subCategoryType, verificationResponse);
                    }
                }
            }
            case "unlinkDeal" -> validateSourcePermission(Source.DEAL.getName(), apiHandlerRequest);
            case "createNewContacts" -> {
                final SourcePermissionValidator validator = new SourcePermissionValidator(orgId, verificationRequest.roleId());

                validator.validateCreatePermission(SubCategory.Type.CONTACTS, verificationResponse);
            }
        }
    }
}