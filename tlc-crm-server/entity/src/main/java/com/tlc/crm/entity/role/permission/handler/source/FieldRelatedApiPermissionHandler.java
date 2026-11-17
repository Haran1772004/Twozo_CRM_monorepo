package com.tlc.crm.entity.role.permission.handler.source;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.role.permission.handler.RelatedApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.request.RelatedApiHandlerRequest;
import com.tlc.crm.entity.role.permission.validator.SourcePermissionValidator;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;
import com.tlc.i18n.I18nKey;

import java.util.Objects;
import java.util.Optional;

/**
 * Handles source-related authorization for field APIs
 *
 * @author sundar
 * @version 1.0
 */
public class FieldRelatedApiPermissionHandler extends RelatedApiPermissionHandler {

    private final FieldService fieldService;

    private FieldRelatedApiPermissionHandler() {
        this.fieldService = FieldService.getSecureInstance();
    }

    private static final class Instance {
        private static final FieldRelatedApiPermissionHandler FIELD_RELATED_API_PERMISSION_HANDLER = new FieldRelatedApiPermissionHandler();
    }

    /**
     * Gets the {@link FieldRelatedApiPermissionHandler} instance
     *
     * @return the {@link FieldRelatedApiPermissionHandler}
     */
    public static FieldRelatedApiPermissionHandler getInstance() {
        return FieldRelatedApiPermissionHandler.Instance.FIELD_RELATED_API_PERMISSION_HANDLER;
    }

    /**
     * {@inheritDoc}
     *
     * @param apiHandlerRequest Represents the request of the API handler.
     */
    @Override
    public void handle(final RelatedApiHandlerRequest apiHandlerRequest) {
        final Optional<SourcePermissionValidator> optionalSourcePermissionValidator = apiHandlerRequest.getPermissionValidator(SourcePermissionValidator.TYPE);

        optionalSourcePermissionValidator.ifPresent(sourcePermissionValidator -> {
            final VerificationRequest<?> verificationRequest = apiHandlerRequest.getVerificationRequest();
            final VerificationResponse verificationResponse = apiHandlerRequest.getVerificationResponse();
            final long orgId = verificationRequest.orgId();
            final JsonObject requestJson = verificationRequest.requestJson();
            final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());

            switch (verificationRequest.apiDetails().type()) {
                case "list", "formRender" -> sourcePermissionValidator.validate(data.getString(FieldKeys.SOURCE.getName()), verificationResponse);
                case "get" -> {
                    final Long fieldId = data.getLong(FieldKeys.ID.getName());
                    final FieldInfo fieldInfo = fieldService.getField(orgId, fieldId);

                    if (Objects.isNull(fieldInfo)) {
                        verificationResponse.addFieldPrivilegeError(new I18nKey(fieldId.toString()));
                    } else {
                        sourcePermissionValidator.validate(fieldInfo.fieldSource().getName(), verificationResponse);
                    }
                }
                case "create", "update", "updateFieldOrder" ->
                        sourcePermissionValidator.validate(data.getLong(FieldKeys.SOURCE_ID.getName()), verificationResponse);
                case "bCreate", "bUpdate" -> {
                    final JsonArray fieldsArray = data.getJsonArray(FieldKeys.LIST.getName());

                    for (int index = 0; index < fieldsArray.size(); index++) {

                        if (verificationResponse.hasErrors()) {
                            break;
                        }
                        final JsonObject fieldsJson = fieldsArray.getJsonObject(index);

                        sourcePermissionValidator.validate(fieldsJson.getLong(FieldKeys.SOURCE_ID.getName()), verificationResponse);
                    }
                }
            }
        });
    }
}