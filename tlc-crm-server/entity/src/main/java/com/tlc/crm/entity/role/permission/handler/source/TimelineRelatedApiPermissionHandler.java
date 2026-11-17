package com.tlc.crm.entity.role.permission.handler.source;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.role.permission.handler.RelatedApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.request.RelatedApiHandlerRequest;
import com.tlc.crm.entity.role.permission.util.SubCategoryTypeUtils;
import com.tlc.crm.entity.timeline.field.TimelineFields;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;

import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Handles source-related authorization for timeline APIs
 *
 * @author Sathish
 * @version 1.0
 */
public class TimelineRelatedApiPermissionHandler extends RelatedApiPermissionHandler {

    private TimelineRelatedApiPermissionHandler() {
    }

    private static final class Instance {
        private static final TimelineRelatedApiPermissionHandler TIMELINE_RELATED_API_PERMISSION_HANDLER = new TimelineRelatedApiPermissionHandler();
    }

    public static TimelineRelatedApiPermissionHandler getInstance() {
        return TimelineRelatedApiPermissionHandler.Instance.TIMELINE_RELATED_API_PERMISSION_HANDLER;
    }

    /**
     * {@inheritDoc}
     *
     * @param apiHandlerRequest Represents the request of the API handler.
     */
    @Override
    public void handle(final RelatedApiHandlerRequest apiHandlerRequest) {
        final VerificationRequest<?> verificationRequest = apiHandlerRequest.getVerificationRequest();

        final long roleId = verificationRequest.roleId();
        final JsonObject requestJson = verificationRequest.requestJson();
        final JsonObject data = requestJson.optJsonObject(FieldKeys.DATA.getName());
        final BiConsumer<SubCategory.Type, Action.Type> timeLinePermissionLoader = (subCategory, actionType) ->
                permissionLoader.loadPermissionIntoContext(rolePermissionService.getPermission(verificationRequest.orgId(), roleId,
                apiHandlerRequest.getCategoryType(), subCategory, actionType));

        switch (verificationRequest.apiDetails().type()) {
            case "contactTimeline" -> {
                final String source = Source.CONTACT.getName();

                validateSourceAndEntityPermissions(source, roleId, Set.of(data.getLong(TimelineFields.CONTACT_ID.getName())),
                        apiHandlerRequest);
                timeLinePermissionLoader.accept(SubCategoryTypeUtils.getBySourceName(source), Action.Type.VIEW);
            }
            case "dealTimeline" -> {
                final String source = Source.DEAL.getName();

                validateSourceAndEntityPermissions(source, roleId, Set.of(data.getLong(TimelineFields.DEAL_ID.getName())),
                        apiHandlerRequest);
                timeLinePermissionLoader.accept(SubCategoryTypeUtils.getBySourceName(source), Action.Type.VIEW);
            }
            case "companyTimeline" -> {
                final String source = Source.COMPANY.getName();

                validateSourceAndEntityPermissions(source, roleId, Set.of(data.getLong(TimelineFields.COMPANY_ID.getName())),
                        apiHandlerRequest);
                timeLinePermissionLoader.accept(SubCategoryTypeUtils.getBySourceName(source), Action.Type.VIEW);
            }

        }
    }
}