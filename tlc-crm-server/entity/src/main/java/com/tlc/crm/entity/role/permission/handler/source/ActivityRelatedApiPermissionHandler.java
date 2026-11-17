package com.tlc.crm.entity.role.permission.handler.source;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.field.permissions.PermissionApiKeys;

import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.role.permission.handler.RelatedApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.request.RelatedApiHandlerRequest;

import com.tlc.crm.security.permission.verifier.request.VerificationRequest;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles source-related authorization for activity APIs
 *
 * @author Sathish
 * @version 1.0
 */
public class ActivityRelatedApiPermissionHandler extends RelatedApiPermissionHandler {

    private ActivityRelatedApiPermissionHandler() {
    }

    private static final class Instance {
        private static final ActivityRelatedApiPermissionHandler ACTIVITY_RELATED_API_PERMISSION_HANDLER = new ActivityRelatedApiPermissionHandler();
    }

    public static ActivityRelatedApiPermissionHandler getInstance() {
        return Instance.ACTIVITY_RELATED_API_PERMISSION_HANDLER;
    }

    /**
     * <p>
     * Handles the particular requests of the activity.
     * </p>
     *
     * @param apiHandlerRequest Represents the request of the API handler.
     */
    @Override
    public void handle(final RelatedApiHandlerRequest apiHandlerRequest) {
        final VerificationRequest<?> request = apiHandlerRequest.getVerificationRequest();
        final long roleId = request.roleId();
        final JsonObject requestJson = request.requestJson();
        final JsonObject data = requestJson.optJsonObject(FieldKeys.DATA.getName());

        switch (request.apiDetails().type()) {
            case "getActivities" -> {
                final Set<Long> entityIds = Set.of(data.getLong(PermissionApiKeys.ENTITY_ID));

                validateSourceAndEntityPermissions(data.getString(PermissionApiKeys.SOURCE), roleId, entityIds, apiHandlerRequest);
            }
            case "create", "bCreate", "update", "pUpdate", "bUpdate", "pBulkUpdate" -> {
                final String contactIdsKey = ActivityFields.CONTACT_IDS.getName();
                final String companyIdKey = ActivityFields.COMPANY_ID.getName();
                final String dealIdKey = ActivityFields.DEAL_ID.getName();

                if (data.containsKey(contactIdsKey) && !data.isKeyPresentWithNullValue(contactIdsKey)) {
                    final Set<Long> contactIds = data.getJsonArray(contactIdsKey).toList().stream()
                            .map(value -> (Long) value).collect(Collectors.toSet());

                    validateSourceAndEntityPermissions(Source.CONTACT.getName(), roleId, contactIds, apiHandlerRequest);
                }

                if (data.containsKey(companyIdKey) && !data.isKeyPresentWithNullValue(companyIdKey)) {
                    validateSourceAndEntityPermissions(Source.COMPANY.getName(), roleId, Set.of(data.getLong(companyIdKey)), apiHandlerRequest);
                }

                if (data.containsKey(dealIdKey) && !data.isKeyPresentWithNullValue(dealIdKey)) {
                    validateSourceAndEntityPermissions(Source.DEAL.getName(), roleId, Set.of(data.getLong(dealIdKey)), apiHandlerRequest);
                }
            }
        }
    }
}