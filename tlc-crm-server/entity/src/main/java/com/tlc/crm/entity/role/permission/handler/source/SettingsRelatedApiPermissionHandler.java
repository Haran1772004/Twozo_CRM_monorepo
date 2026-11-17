package com.tlc.crm.entity.role.permission.handler.source;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.settings.SettingsFields;
import com.tlc.crm.entity.role.permission.handler.RelatedApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.request.RelatedApiHandlerRequest;
import com.tlc.crm.entity.role.permission.validator.SourcePermissionValidator;

import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.service.settings.SourceSettingsService;

import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Objects;
import java.util.Optional;

/**
 * Handles source-related authorization for source settings APIs
 *
 * @author sundar
 * @version 1.0
 */
public class SettingsRelatedApiPermissionHandler extends RelatedApiPermissionHandler {

    private final SourceSettingsService sourceSettingsService;

    private SettingsRelatedApiPermissionHandler() {
        this.sourceSettingsService = SourceSettingsService.getInstance();
    }

    private static final class Instance {
        private static final SettingsRelatedApiPermissionHandler SETTINGS_RELATED_API_PERMISSION_HANDLER = new SettingsRelatedApiPermissionHandler();
    }

    /**
     * Gets the {@link SettingsRelatedApiPermissionHandler} instance
     *
     * @return the {@link SettingsRelatedApiPermissionHandler}
     */
    public static RelatedApiPermissionHandler getInstance() {
        return Instance.SETTINGS_RELATED_API_PERMISSION_HANDLER;
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
                case "rename" -> {
                    final JsonArray sourceSettingsArray = data.getJsonArray(FieldKeys.LIST.getName());
                    final String alternateNameKey = SettingsFields.ALTERNATE_NAME.getName();
                    final String sourceNameKey = SettingsFields.SOURCE_NAME.getName();

                    for (int index = 0; index < sourceSettingsArray.size(); index++) {

                        if (verificationResponse.hasErrors()) {
                            break;
                        }
                        final JsonObject sourceSettingJson = sourceSettingsArray.getJsonObject(index);
                        final String sourceName = sourceSettingJson.getString(sourceNameKey);

                        if (!sourcePermissionValidator.validate(sourceName) && sourceSettingJson.containsKey(alternateNameKey)) {
                            final SourceSettings sourceSettings = sourceSettingsService.getSourceSettingsBySource(orgId, sourceName);

                            if (!Objects.equals(sourceSettingJson.getString(alternateNameKey), sourceSettings.getAlternateName())) {
                                sourcePermissionValidator.validate(sourceName, verificationResponse);
                            }
                        }
                    }
                }
                case "updateConfig" -> sourcePermissionValidator.validate(data.getString(SettingsFields.SOURCE.getName()), verificationResponse);
            }
        });
    }
}
