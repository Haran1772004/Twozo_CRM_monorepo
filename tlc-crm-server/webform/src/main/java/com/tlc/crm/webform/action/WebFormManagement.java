package com.tlc.crm.webform.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.config.manager.ConfigManager;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.security.permission.annotation.authorizer.LeadAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.webform.api.manager.WebFormManager;
import com.tlc.crm.webform.api.model.WebForm;
import com.tlc.crm.webform.api.model.converter.WebFormConverter;
import com.tlc.crm.webform.cache.TemplateService;
import com.tlc.crm.webform.field.WebFormFields;
import com.tlc.crm.webform.internal.status.WebFormErrorCodes;
import com.tlc.crm.webform.url.UrlGenerator;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

/**
 * Manages the {@link WebForm} related web actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@LeadAuthorizer(subCategory = SubCategory.Type.WEB_FORM_TRACKING)
@WebAction(path = "/webform/mgmt")
public class WebFormManagement extends CrmConfigAction<WebForm> {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;
    private static final int DEFAULT_NAME_LIMIT = 100;

    private static final Logger LOGGER = LoggerFactory.getLogger(WebFormManagement.class);

    private final ConverterConfigBuilder converterConfigBuilder;
    private final WebFormManager webFormManager;
    private final WebFormConverter webFormConverter;
    private final TemplateService templateService;
    private final FieldService fieldService;
    private final OptionService optionService;
    private final UrlGenerator urlGenerator;
    private final AttachmentService attachmentService;

    public WebFormManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        webFormManager = WebFormManager.getInstance();
        webFormConverter = WebFormConverter.getInstance();
        templateService = TemplateService.getInstance();
        fieldService = FieldService.getInstance();
        optionService = OptionService.getInstance();
        urlGenerator = UrlGenerator.get();
        attachmentService = AttachmentService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The received request.
     * @param response The response to be sent.
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(WebFormFields.DATA.getName());
        final String type = requestJson.getString(WebFormFields.TYPE.getName());
        final Long orgId = request.orgId();

        switch (type) {
            case "list" -> response.put(WebFormFields.WEB_FORMS.getName(), getFormList(orgId, data, response));
            case "mode" -> updateFormStatus(orgId, data, response);
            case "visit" -> updateFormVisitDetails(orgId, data, response);
            case "dropdownData" -> fetchDropdownData(request, data, response);
            case "clone" -> cloneWebForm(orgId, data, response);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link AbstractConfigManager} instance of webForm.
     */
    @Override
    public ConfigManager<WebForm> getConfigManager() {
        return webFormManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The received request.
     * @param jsonObject Holds entity details.
     * @return
     */
    @Override
    public WebForm convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return webFormConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param request The received request.
     * @param webForm Refers the {@link WebForm}.
     * @return The {@link JsonObject} of webForm data.
     */
    @Override
    public JsonObject convert(final CrmRequest request, final WebForm webForm) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, null);

        return webFormConverter.convert(converterConfig, webForm);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link WebForm} filled with the data.
     */
    @Override
    public WebForm convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return webFormConverter.convert(converterConfig, jsonObject);
    }

    /**
     * Updates the web form active status.
     *
     * @param orgId    Refers the organization id.
     * @param data     The json data.
     * @param response The response to be sent.
     */
    private void updateFormStatus(final Long orgId, final JsonObject data, final CrmResponse response) {
        final String idKey = WebFormFields.ID.getName();
        final Long id = data.getLong(idKey);

        if (webFormManager.exists(orgId, id)) {
            response.put(WebFormFields.STATUS.getName(),
                    webFormManager.updateFormStatus(id, orgId, data.getBoolean(WebFormFields.IS_ACTIVE.getName())));
            response.put(idKey, id);
        } else {
            throw ErrorCode.get(WebFormErrorCodes.WEB_FORM_NOT_FOUND, "i18n.webFrom.not.found");
        }
    }

    /**
     * Fetches the {@link WebForm} list mapped with the given organization.
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the json data.
     * @param response The response to be sent.
     * @return The {@link JsonArray} of web forms.
     */
    private JsonArray getFormList(final Long orgId, final JsonObject data, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final int start = Math.max(data.optInt(WebFormFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(WebFormFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final boolean isActive = data.optBoolean(WebFormFields.IS_ACTIVE.getName(), true);
        final Long totalRecords = webFormManager.getWebFormCount(orgId, isActive);
        final boolean hasMore = (start + limit) <= totalRecords;

        if (totalRecords > 0 && start <= totalRecords) {
            final String sortField;
            final String sortType;

            if (!isActive && data.containsKey(WebFormFields.SORT.getName())) {
                throw ErrorCode.get(WebFormErrorCodes.SORTING_NOT_ALLOWED);
            } else {
                final JsonObject sortJson = data.optJsonObject(WebFormFields.SORT.getName());

                if (null != sortJson) {
                    sortField = sortJson.getString(WebFormFields.FIELD.getName());
                    sortType = sortJson.getString(WebFormFields.TYPE.getName());
                } else {
                    sortField = WebFormFields.CREATED_TIME.getName();
                    sortType = WebFormFields.DESC.getName();
                }
            }
            final Collection<WebForm> webForms = webFormManager.getWebFormList(orgId, start, limit, sortField, sortType, isActive);

            if (Objects.nonNull(webForms) && !webForms.isEmpty()) {
                webForms.forEach(form -> jsonArray.put(webFormConverter.convert(form)));
            }
        }
        response.put(WebFormFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(WebFormFields.HAS_MORE.getName(), hasMore);

        return jsonArray;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id.
     * @param action   Holds the action values.
     * @param webForms Collection of {@link WebForm}.
     * @return true if all details are valid.
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<WebForm> webForms) {
        switch (action) {
            case CREATE, UPDATE -> {
                final Set<Long> templateIds = new HashSet<>();

                for (final WebForm webForm : webForms) {
                    templateIds.add(webForm.getTemplateId());
                }

                if (!templateIds.isEmpty() && !templateService.exists(templateIds)) {
                    throw ErrorCode.get(WebFormErrorCodes.TEMPLATE_NOT_FOUND, "i18n.webForm.template.not.found");
                }

            }
        }
        return true;
    }

    /**
     * Updates the form visit detail for given webForm id.
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the request json.
     * @param response Refers the {@link CrmResponse} to be sent.
     */
    private void updateFormVisitDetails(final Long orgId, final JsonObject data, final CrmResponse response) {
        final long id = data.getLong(WebFormFields.ID.getName());

        if (webFormManager.exists(orgId, id)) {
            response.put(WebFormFields.STATUS.getName(), webFormManager
                    .updateVisitDetails(id, orgId, webFormManager.getFormVisitsAndSubmissionDetails(orgId, id)));
        } else {
            throw ErrorCode.get(WebFormErrorCodes.WEB_FORM_NOT_FOUND, "i18n.webFrom.not.found");
        }
    }

    /**
     * Fetches dropdown data for a specified organization using the provided data and handles the response.
     *
     * @param request     Refers the {@link CrmRequest}
     * @param data        Refers the request json.
     * @param crmResponse Refers the {@link CrmResponse} to be sent.
     */
    private void fetchDropdownData(final CrmRequest request, final JsonObject data, final CrmResponse crmResponse) {
        final Long orgId = request.orgId();
        final FieldInfo field = fieldService.getField(orgId, data.getLong(FieldKeys.ID.getName()));

        if (StoreType.isSelectorType(field.storeType())) {
            final int start = Math.max(data.optInt(WebFormFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
            final int limit = Math.min(data.optInt(WebFormFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
            final JsonObject criteriaJson = data.optJsonObject(FieldKeys.CRITERIA.getName());
            final DropdownCriteria dropdownCriteria = new DropdownCriteria();

            if (Objects.nonNull(criteriaJson)) {
                dropdownCriteria.setCriteriaValue(criteriaJson.get(WebFormFields.VALUE.getName()));
                dropdownCriteria.setCriteriaType(criteriaJson.getString(WebFormFields.TYPE.getName()));
            }
            final OptionContainer optionContainer = new OptionContainer(WebFormFields.ID.getName(), WebFormFields.OPTION.getName(), 0);
            final String order = data.optString(FieldKeys.ORDER.getName(), FieldKeys.ASC.getName());
            final Collection<Option> options = AbstractDropdownAction.applyLimit(optionService.getOptions(orgId, field, dropdownCriteria,
                    request.i18nResolver(), order), start, limit);

            if (!options.isEmpty()) {

                for (final Option option : options) {
                    optionContainer.addOption(option.id(), option.getOption());
                }
            }

            crmResponse.put(WebFormFields.OPTIONS.getName(), optionContainer.getOptions());
            crmResponse.put(WebFormFields.SIZE.getName(), optionContainer.size());
        } else {
            throw ErrorCode.get(WebFormErrorCodes.FIELD_IS_NOT_A_DROPDOWN, "i18n.webForm.dropdown.field.required");
        }
    }

    /**
     * <p>
     * Clones the web form based on the given id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param data     Represents the {@link JsonObject}
     * @param response Represents the {@link CrmResponse}
     */
    private void cloneWebForm(final Long orgId, final JsonObject data, final CrmResponse response) {
        final long id = data.getLong(WebFormFields.ID.getName());

        if (webFormManager.exists(orgId, id)) {
            final WebForm webForm = webFormManager.get(orgId, id);
            final String name = generateUniqueName(orgId, webForm.getName());

            if (DEFAULT_NAME_LIMIT > name.length()) {
                webForm.setName(name);
                webForm.setVisits(0);
                webForm.setSubmissions(0);
                webForm.setConversions(0.0);
                webFormManager.create(webForm);

                response.put(WebFormFields.DUPLICATED_WEB_FORM_ID.getName(), webForm.id());
                response.put(WebFormFields.URL.getName(), webForm.getUrl());
                response.put(WebFormFields.IFRAME_URL.getName(), urlGenerator.getIframeUrl(orgId, webForm.getOwnerId(), id));
            } else {
                throw ErrorCode.get(WebFormErrorCodes.WEB_FORM_NAME_LENGTH_EXCEEDED, "i18n.webForm.invalid.name.max.length");
            }
        } else {
            throw ErrorCode.get(WebFormErrorCodes.WEB_FORM_NOT_FOUND, "i18n.webFrom.not.found");
        }
    }

    /**
     * <p>
     * Generates unique name based on the existing name
     * </p>
     *
     * @param orgId        Represents an organization
     * @param existingName Represents the existing web form name
     * @return the unique name
     */
    private String generateUniqueName(final Long orgId, final String existingName) {
        int index = 1;
        String generatedName = existingName;

        while (webFormManager.exists(orgId, generatedName)) {
            generatedName = String.format("%s(%d)", existingName, index);
            index++;
        }

        return generatedName;
    }
}
