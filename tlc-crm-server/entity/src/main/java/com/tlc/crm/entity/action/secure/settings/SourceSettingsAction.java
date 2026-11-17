package com.tlc.crm.entity.action.secure.settings;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.SecureEventAction;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.entity.fields.settings.ProductConfigKeys;
import com.tlc.crm.entity.fields.settings.SettingsFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.internal.status.SettingsErrorCodes;
import com.tlc.crm.entity.role.permission.validator.SourcePermissionValidator;
import com.tlc.crm.fields.manager.SourceSettingsManager;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.crm.notification.app.Event;

import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.web.WebAction;
import jakarta.validation.ConstraintViolation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tlc.crm.notification.app.event.NotificationEvent.APP_NOTIFICATION_EVENT;

/**
 * <p>
 * Manages {@link SourceSettings} related web actions
 * </p>
 *
 * @author PandiarajkumarG
 */
@SourceAuthorizer(actions = com.tlc.crm.security.permission.model.Action.Type.VIEW, relatedApiTypes = SourceAuthorizer.RelatedApiType.SOURCE_SETTINGS, needRelatedApiHandling = true)
@WebAction(path = "/settings/mgmt")
public class SourceSettingsAction extends SecureEventAction {

    private final SourceSettingsManager sourceSettingsManager;
    private final FieldSourceService fieldSourceService;
    private final SourceSettingsService sourceSettingsService;

    public SourceSettingsAction() {
        sourceSettingsManager = SourceSettingsManager.getInstance();
        fieldSourceService = FieldSourceService.getInstance();
        sourceSettingsService = SourceSettingsService.getInstance();
    }

    @Override
    protected void secureProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final Long orgId = request.orgId();
        final JsonObject data = requestJson.getJsonObject("data");
        final String type = requestJson.getString("type");
        final CrmEventRequest crmEventRequest = (CrmEventRequest) request;
        final EventData eventData =  crmEventRequest.getEventData();

        switch (type) {
            case "rename" -> {
                final Collection<SourceSettings> models = convertToModels(orgId, data.getJsonArray("list"));

                update(crmResponse, orgId, models);
            }
            case "list" -> {
                final boolean fetchPermittedSourcesOnly = data.optBoolean(SettingsFields.FETCH_PERMITTED_SOURCES_ONLY.getName());
                final Collection<SourceSettings> accessibleSourceSettings = new ArrayList<>();
                final SourcePermissionValidator sourcePermissionValidator = new SourcePermissionValidator(orgId, request.getUser().getRoleId());
                final Collection<SourceSettings> sourceSettings = sourceSettingsService.getCommonSourceSettings(orgId);

                if (fetchPermittedSourcesOnly) {
                    for (final SourceSettings sourceSetting : sourceSettings) {

                        if (sourcePermissionValidator.validate(sourceSetting.getSourceId())) {
                            accessibleSourceSettings.add(sourceSetting);
                        }
                    }
                } else {
                    accessibleSourceSettings.addAll(sourceSettings);
                }
                crmResponse.put(SettingsFields.LIST.getName(), convertModelsToJsonArray(accessibleSourceSettings));
            }
            case "updateConfig" -> updateConfig(orgId, request.userId(), data, crmResponse, eventData);
        }
    }

    /**
     * <p>
     * Converts collection of json models {@link JsonArray} into Collection of {@link SourceSettings}
     * </p>
     *
     * @param orgId Organization id
     * @param modulesList  A json array that holds the information of multiple entity
     * @return {@link SourceSettings}
     */
    private Collection<SourceSettings> convertToModels(final Long orgId, final JsonArray modulesList) {
        final Collection<SourceSettings> sourceSettings = new ArrayList<>();

        for (int index = 0; index < modulesList.size(); index++) {
            sourceSettings.add(convertToModel(orgId, modulesList.optJsonObject(index)));
        }

        return sourceSettings;
    }

    /**
     * <p>
     * Converts models into json array {@link JsonArray} into Collection of {@link SourceSettings}
     * </p>
     *
     * @param models A Collection of models
     * @return {@link JsonArray}
     */
    private JsonArray convertModelsToJsonArray(final Collection<SourceSettings> models) {
        final JsonArray jsonArray = Json.array();

        models.forEach(model -> jsonArray.put(convertModelToJsonObject(model)));
        return jsonArray;
    }

    /**
     * <p>
     * Converts model to json object i.e {@link SourceSettings} into {@link JsonObject}
     * </p>
     *
     * @param sourceSettings Rename Object
     * @return {@link JsonObject}
     */
    private JsonObject convertModelToJsonObject(final SourceSettings sourceSettings) {
        final JsonObject jsonObject = Json.object();
        final boolean isEnabled = sourceSettings.isEnabled();
        final String config = sourceSettings.getConfig();

        jsonObject.put(SettingsFields.SOURCE_ID.getName(), sourceSettings.getSourceId());
        jsonObject.put(SettingsFields.ALTERNATE_NAME.getName(), sourceSettings.getAlternateName());
        jsonObject.put(SettingsFields.SOURCE_NAME.getName(), fieldSourceService.getSource(sourceSettings
                .getSourceId()).getName());
        jsonObject.put(SettingsFields.IS_ENABLED.getName(), isEnabled);

        if (isEnabled && Objects.nonNull(config)) {
            jsonObject.put(SettingsFields.CONFIG.getName(), Json.object(config));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Updates the given sources  {@link CrmResponse} into Collection of {@link SourceSettings}
     * </p>
     *
     * @param orgId       Organisation id
     * @param crmResponse response to API call
     * @param models      Rename Object
     */
    public void update(final CrmResponse crmResponse, final Long orgId, final Collection<SourceSettings> models) {
        validateDuplicateNames(orgId, models);
        final Collection<ErrorDescriptor> errors = validate(models, Group.Update.class);
        final String updatedRecordsKey = ActionFields.UPDATED_RECORDS;

        if (errors.isEmpty()) {
            final ProcessOutcome processOutcome = sourceSettingsManager.update(orgId, models);

            crmResponse.put(updatedRecordsKey, processOutcome.getProcessedRecords());
            final ErrorSummary errorSummary = processOutcome.getErrorSummary();

            if (Objects.nonNull(errorSummary)) {
                crmResponse.setErrorCode(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed"));
                crmResponse.setErrorSummary(errorSummary);
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode,
                    ErrorKeys.UPDATE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * Validates that the updated module name does not duplicate any existing
     * module name, including both default source names and renamed module names.
     *
     * @param orgId Organisation id
     * @param models Module rename objects
     */
    private void validateDuplicateNames(final Long orgId, final Collection<SourceSettings> models) {

        final Set<Long> sourceIdsToBeUpdated = models.stream()
                .map(SourceSettings::getSourceId)
                .collect(Collectors.toSet());

        final Collection<String> moduleNames = new HashSet<>();

        for (final SourceSettings sourceSettings : sourceSettingsService.getCommonSourceSettings(orgId)) {

            if (sourceIdsToBeUpdated.contains(sourceSettings.getSourceId())) {
                continue;
            }

            moduleNames.add(resolveModuleName(sourceSettings).trim().toLowerCase());
        }

        for (final SourceSettings model : models) {

            final String moduleName = resolveModuleName(model).trim().toLowerCase();

            if (!moduleNames.add(moduleName)) {
                throw ErrorCode.get(SettingsErrorCodes.SOURCE_NAME_ALREADY_EXISTS, "i18n.account.source.name.already.exists");
            }
        }
    }

    /**
     *
     * Resolves the effective display name of a module
     *
     * @param sourceSettings module rename object
     * @return the effective name of the module
     */
    private String resolveModuleName(final SourceSettings sourceSettings) {
        final String alternateName = sourceSettings.getAlternateName();

        return Objects.isNull(alternateName) || alternateName.isBlank() ? fieldSourceService.getSource(sourceSettings.getSourceId()).getName() : alternateName;
    }

    /**
     * <p>
     * Converts the {@link JsonArray} into collection of subclass of {@link TlcModel}
     * </p>
     *
     * @param orgId      Organization id
     * @param jsonObject Holds multiple entries
     * @return List of models
     */
    SourceSettings convertToModel(final Long orgId, final JsonObject jsonObject) {
        final String alternateNameKey = SettingsFields.ALTERNATE_NAME.getName();
        final SourceSettings sourceSettings = new SourceSettings();

        if (jsonObject.containsKey(alternateNameKey)) {
            final String alternateName = jsonObject.optString(alternateNameKey, null);

            if (Objects.nonNull(alternateName) && !alternateName.isBlank()) {
                sourceSettings.setAlternateName(alternateName);
            }
        }
        sourceSettings.setOrgId(orgId);
        sourceSettings.setSourceId(fieldSourceService.getSource((String) jsonObject.opt(SettingsFields.SOURCE_NAME.getName())).id());
        final SourceSettings oldSourceSettings = sourceSettingsService.getSourceSettings(orgId, sourceSettings.getSourceId());

        sourceSettings.setEnabled(oldSourceSettings.isEnabled());

        return sourceSettings;
    }

    /**
     * <p>
     * Validates the models against the given validator groups i.e {@link SourceSettings}
     * </p>
     *
     * @param models          module rename object
     * @param validatorGroups validation groups
     */
    private <T extends TlcModel> Collection<ErrorDescriptor> validate(final Collection<T> models,
                                                             final Class... validatorGroups) {
        final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
        final Set<ConstraintViolation<T>> violations = new HashSet<>();
        final ErrorCodeProvider errorCodeProvider = CommonErrorCodes.DATA_VALIDATION_FAILED;

        models.forEach(model -> violations.addAll(validator.validate(model, validatorGroups)));

        return violations.stream().map(violation -> new ErrorDescriptor(ErrorCode.get(errorCodeProvider, violation.getMessage()), null,
                violation.getPropertyPath().toString())).collect(Collectors.toSet());
    }

    /**
     * Updates the entity source configuration.
     *
     * @param orgId       Refers the organization id.
     * @param data        Refers the json data.
     * @param crmResponse Refers the response to be sent.
     * @param eventData   Represents the {@link EventData}
     */
    private void updateConfig(final Long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse, final EventData eventData) {
        final String enableKey = SettingsFields.IS_ENABLED.getName();
        final String configKey = SettingsFields.CONFIG.getName();
        final Map<String, Object> errors = new HashMap<>();
        final String entitySource = data.getString(SettingsFields.SOURCE.getName());
        final Long sourceId = fieldSourceService.getSource(entitySource).id();
        final SourceSettings oldSourceSettings = sourceSettingsService.getSourceSettings(orgId, sourceId);

        if (!oldSourceSettings.isEnabled() && !data.optBoolean(enableKey)) {
            throw ErrorCode.get(SettingsErrorCodes.UNABLE_TO_UPDATE_CONFIG_AS_SOURCE_IS_DISABLED);
        }
        final SourceSettings sourceSettings = new SourceSettings();

        sourceSettings.setOrgId(orgId);
        sourceSettings.setSourceId(sourceId);
        sourceSettings.setUserId(userId);
        sourceSettings.setEnabled(data.containsKey(enableKey) ? data.getBoolean(enableKey) : oldSourceSettings.isEnabled());

        if (data.containsKey(configKey)) {
            final JsonObject configJson = data.getJsonObject(configKey);
            final Collection<String> sourceConfigKeys;

            switch (Source.valueOf(entitySource.toUpperCase())) {
                case PRODUCT -> {
                    sourceConfigKeys = Arrays.stream(ProductConfigKeys.values()).map(ProductConfigKeys::getName).toList();
                }
                default -> throw ErrorCode.get(EntityErrorCodes.ENTITY_SOURCE_NOT_FOUND, "i18n.source.not.found");
            }
            final Collection<String> invalidKeys = configJson.keySet().stream().filter(key -> !sourceConfigKeys.contains(key)).toList();

            if (!invalidKeys.isEmpty()) {
                errors.put(SettingsFields.INVALID_CONFIG_KEYS.getName(), invalidKeys);
            }
            sourceSettings.setConfig(configJson.toString());
        }

        if (errors.isEmpty()) {
            final boolean isUpdated = sourceSettingsManager.updateConfig(orgId, sourceSettings);
            final boolean isSourceSettingsEnabled = sourceSettings.isEnabled();

            crmResponse.put(SettingsFields.STATUS.getName(), isUpdated);
            crmResponse.put(SettingsFields.IS_ENABLED.getName(), isUpdated ? isSourceSettingsEnabled : oldSourceSettings.isEnabled());

            if (isSourceSettingsEnabled) {
                eventData.setAction(Action.UPDATE);
                eventData.setValues(List.of(sourceSettings));

                triggerEvent(APP_NOTIFICATION_EVENT, eventData, Event.Type.POST);
            }
        } else {
            crmResponse.setErrorCode(ErrorCode.get(SettingsErrorCodes.SOURCE_CONFIG_UPDATE_ACTION_FAILED, "i18n.account.source.settings.update.failed"));
            crmResponse.put(SettingsFields.ERRORS.getName(), errors);
            crmResponse.put(SettingsFields.STATUS.getName(), SettingsFields.FAILED.getName());
            crmResponse.put(SettingsFields.IS_ENABLED.getName(), oldSourceSettings.isEnabled());
        }
    }
}
