package com.tlc.crm.webform.action;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmReadableConfigAction;

import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.manager.ReadableConfigManager;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.webform.cache.TemplateService;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.web.WebAction;

import com.tlc.crm.webform.api.manager.TemplateManager;
import com.tlc.crm.webform.api.model.FormField;
import com.tlc.crm.webform.api.model.FormFieldProperty;
import com.tlc.crm.webform.api.model.Template;
import com.tlc.crm.webform.api.model.converter.WebFormConverter;
import com.tlc.crm.webform.field.WebFormFields;
import com.tlc.crm.webform.field.WebFormStyleFields;
import com.tlc.crm.webform.internal.status.WebFormErrorCodes;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the {@link Template} related web actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 * @see CrmReadableConfigAction
 */
@WebAction(path = "/webform/template/mgmt")
public class TemplateManagement extends CrmReadableConfigAction<Template> {

    private final I18nService i18nAccess;
    private final TemplateManager templateManager;
    private final TemplateService templateService;
    private final WebFormConverter webFormConverter;
    private final FieldTypeService fieldTypeService;

    public TemplateManagement() {
        templateService = TemplateService.getInstance();
        templateManager = TemplateManager.getInstance();
        i18nAccess = I18nServiceProvider.get();
        webFormConverter = WebFormConverter.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link ReadableConfigManager} instance of template.
     */
    @Override
    public ReadableConfigManager<Template> getConfigManager() {
        return templateManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request     The request.
     * @param crmResponse The response to be sent.
     */
    @Override
    public void handleReadRequest(final CrmRequest request, final CrmResponse crmResponse) {
    }

    /**
     * {@inheritDoc}
     *
     * @param id Refers the template id.
     * @return The {@link Template}.
     */
    @Override
    public Template get(final Long id) {
        final Template template = templateService.get(id);

        if (Objects.nonNull(template)) {
            return template;
        } else {
            throw ErrorCode.get(WebFormErrorCodes.TEMPLATE_NOT_FOUND, String.format("ID : %d", id));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Collection} of {@link Template}.
     */
    @Override
    public Collection<Template> getAllData() {
        return templateService.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Refers the {@link CrmRequest}
     * @param template Refers the model.
     * @return The {@link JsonObject} which holds the template data.
     */
    @Override
    protected final JsonObject convertModelToJsonObject(final CrmRequest request, final Template template) {
        final JsonObject jsonObject = Json.object();
        final I18nResolver i18nResolver = i18nAccess.getResolver(request.locale());

        jsonObject.put(WebFormFields.ID.getName(), template.id());
        jsonObject.put(WebFormFields.FORM_NAME.getName(), i18nResolver.get(template.name()));
        jsonObject.put(WebFormFields.DESCRIPTION.getName(), i18nResolver.get(template.description()));
        jsonObject.put(WebFormFields.SUBMIT_BUTTON_NAME.getName(), i18nResolver.get(template.submitButtonName()));
        jsonObject.put(WebFormFields.FORM_PROPERTIES.getName(), convertFieldPropertiesToJson(template, request.getI18NCRMResolutionAdapter()));
        jsonObject.put(WebFormStyleFields.STYLE.getName(), convertStyleToJson(template));
        jsonObject.put(WebFormFields.ON_SUBMIT.getName(), webFormConverter.convertSubmitActionToJson(i18nResolver, template.submitAction()));

        return jsonObject;
    }

    /**
     * Converts the {@link Template} styles into the {@link JsonObject}.
     *
     * @param template Refers the {@link Template}.
     * @return The {@link JsonObject} which contains the template style data.
     */
    private JsonObject convertStyleToJson(final Template template) {
        final JsonObject styleJson = Json.object();

        styleJson.put(WebFormStyleFields.FORM_STYLE.getName(),
                webFormConverter.convertFormStyleToJson(template.formStyle()));
        styleJson.put(WebFormStyleFields.FIELD_STYLE.getName(),
                webFormConverter.convertFieldStyleToJson(template.fieldStyle()));
        styleJson.put(WebFormStyleFields.SUBMIT_BUTTON_STYLE.getName(),
                webFormConverter.convertSubmitButtonStyleToJson(template.submitButtonStyle()));

        return styleJson;
    }

    /**
     * Converts the form field properties details into the {@link JsonArray}.
     *
     * @param template     Refers the {@link Template}.
     * @param i18nResolver Refers the {@link I18nResolver} for resolve the i18n data.
     * @return The {@link JsonArray} which contains the form field properties details.
     */
    private JsonArray convertFieldPropertiesToJson(final Template template, final I18NCRMResolutionAdapter i18nResolver) {
        final JsonArray jsonArray = Json.array();
        final Collection<FormFieldProperty> formFieldProperties = template.formFieldProperties()
                .stream().filter(fieldProperty -> Objects.equals(FormFieldProperty.Type.FIELD, fieldProperty.getType()))
                .sorted(Comparator.comparing(FormFieldProperty::order)).toList();

        if (!formFieldProperties.isEmpty()) {
            final Collection<Long> mandatoryFieldIds = FieldService.getInstance()
                    .groupDefaultFieldsByAliasName(Source.CONTACT.getName(), List.of(WebFormFields.EMAILS.getName(), WebFormFields.PHONES.getName()))
                    .values().stream().map(FieldInfo::id).collect(Collectors.toUnmodifiableSet());

            for (final FormFieldProperty formFieldProperty : formFieldProperties) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(WebFormFields.LABEL.getName(), i18nResolver.get(formFieldProperty.getI18nLabel()));
                jsonObject.put(WebFormFields.CONFIG.getName(), formFieldProperty.getConfigAsJson());
                jsonObject.put(WebFormFields.TYPE.getName(), formFieldProperty.getType().name());

                if (formFieldProperty instanceof final FormField formField) {
                    final Long fieldId = formField.getFieldId();
                    final JsonObject fieldTypeJsonObject = Json.object();
                    final FieldType fieldType = fieldTypeService.get(formField.getFieldTypeId());

                    fieldTypeJsonObject.put(WebFormFields.NAME.getName(), fieldType.name()).
                            put(WebFormFields.DISPLAY_NAME.getName(), i18nResolver.get(fieldType.displayName()));
                    jsonObject.put(WebFormFields.FIELD_TYPE.getName(), fieldTypeJsonObject);
                    jsonObject.put(WebFormFields.FIELD_ID.getName(), fieldId);

                    if (mandatoryFieldIds.contains(fieldId)) {
                        jsonObject.put(WebFormFields.IS_SYSTEM_FIELD.getName(), true);
                    }
                }
                jsonArray.put(jsonObject);
            }
        }

        return jsonArray;
    }

}
