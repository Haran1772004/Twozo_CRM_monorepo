package com.tlc.crm.core.webform.api.model.converter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.dropdown.contact.ContactSourceStaticOption;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.field.deal.PipelineFields;
import com.tlc.crm.core.internal.formatter.ValueFormatter;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.dropdown.DealSourceStaticOption;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.webform.api.model.FormField;
import com.tlc.crm.webform.api.model.FormFieldProperty;
import com.tlc.crm.webform.api.model.WebForm;
import com.tlc.crm.webform.field.WebFormFields;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts the submit action json into the respective entity json.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class FormSubmitActionConverter implements ValueFormatter<WebForm, JsonObject, JsonObject> {

    public static final String AMOUNT = "amount";
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    private final PipelineManager pipelineManager;
    private final FieldService fieldService;
    private final FieldTypeService fieldTypeService;
    private final OptionService optionService;

    private FormSubmitActionConverter() {
        pipelineManager = PipelineManager.getInstance();
        fieldService = FieldService.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
        optionService = OptionService.getInstance();
    }

    private static class Instance {
        private static final FormSubmitActionConverter INSTANCE = new FormSubmitActionConverter();
    }

    /**
     * Gets the {@link FormSubmitActionConverter} Instance
     *
     * @return {@link FormSubmitActionConverter}
     */
    public static FormSubmitActionConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts the given json to the {@link JsonObject} of entity field.
     *
     * @param webForm Refers the {@link WebForm} details.
     * @param data    Refers the request json.
     * @return The converted entity {@link JsonObject}.
     */
    public JsonObject format(final long orgId, final WebForm webForm, final JsonObject data) {
        final JsonObject jsonObject = Json.object();
        final Collection<FormField> formFields = webForm.getFormFieldProperties().stream()
                .filter(formField -> Objects.equals(FormFieldProperty.Type.FIELD, formField.getType()))
                .map(formField -> (FormField) formField).toList();
        final String contactSource = Contact.ENTITY_TYPE;
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, contactSource);

        for (final FormField formField : formFields) {
            final Object value = data.opt(formField.id().toString());

            if (Objects.nonNull(value)) {
                final FieldInfo fieldInfo = fields.stream().filter(field ->
                        Objects.equals(formField.getFieldId(), field.id())).findFirst().orElse(null);

                if (Objects.nonNull(fieldInfo)) {
                    final StoreType storeType = fieldInfo.storeType();

                    switch (storeType) {
                        case EMAIL, EMAILS, PHONE, PHONES, MONETARY, ADDRESS -> {
                            buildAndLoadModelStoreJson(formField, fieldInfo, value, jsonObject, storeType);
                        }
                        case MULTI_SELECT -> {
                            if (value instanceof List<?>) {
                                jsonObject.put(fieldInfo.idAsString(), value);
                            }
                        }
                        case SINGLE_STRING, SINGLE_NUMBER, TEXT, DROPDOWN, COMPANY_ONBOARD -> {

                            if (Objects.equals(StoreType.COMPANY_ONBOARD, storeType)) {
                                final JsonObject companyJson = Json.object();
                                final String nameKey = WebFormFields.NAME.getName();

                                companyJson.put(fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE,
                                        nameKey).get(nameKey).idAsString(), value);
                                jsonObject.put(fieldInfo.idAsString(), companyJson);
                            } else {
                                jsonObject.put(fieldInfo.idAsString(), value);
                            }
                        }
                        case DATE -> {
                            try {
                                jsonObject.put(fieldInfo.idAsString(), DATE_FORMATTER.parse(value.toString()).getTime());
                            } catch (ParseException e) {
                                throw ErrorCode.get(CoreErrorCodes.UNABLE_TO_PARSE_DATE, e.getMessage());
                            }
                        }
                    }
                }
            }
        }

        final Long ownerId = webForm.getOwnerId();
        final String salesOwnerKey = ContactFields.SALES_OWNER;
        final String nameKey = ContactFields.FIRST_NAME;
        final String contactSourceKey = ContactFields.CONTACT_SOURCE;
        final Map<String, FieldInfo> contactFields = fieldService.groupDefaultFieldsByAliasName(contactSource, List.of(nameKey, salesOwnerKey, contactSourceKey));
        final FormField formField = formFields.stream().filter(fieldProperty -> {
            final StoreType storeType = fieldService.getField(orgId, fieldProperty.getFieldId()).storeType();

            return ((Objects.equals(StoreType.PHONES, storeType) || Objects.equals(StoreType.EMAILS, storeType))
                    && fieldProperty.getConfigAsJson().getBoolean(WebFormFields.IS_REQUIRED.getName()));
        }).findFirst().get();
        final Object value = data.opt(formField.id().toString());
        final String nameIdAsString = contactFields.get(nameKey).idAsString();
        final FieldInfo contactSourceFieldInfo = contactFields.get(contactSourceKey);

        if (!jsonObject.containsKey(nameIdAsString)) {
            jsonObject.put(nameIdAsString, String.format("%s %s", value, contactSource));
        }
        jsonObject.put(contactFields.get(salesOwnerKey).idAsString(), ownerId);
        jsonObject.put(contactSourceFieldInfo.idAsString(), optionService.getOption(orgId, contactSourceFieldInfo,
                ContactSourceStaticOption.WEB_FORM.getName()).id());

        if (webForm.getSubmitAction().isDealCreationWithContactEnabled()) {
            final JsonObject dealJson = Json.object();
            final String dealSource = Deal.ENTITY_TYPE;
            final Map<String, FieldInfo> dealFields = fieldService.groupDefaultFieldsByAliasName(dealSource);
            final Map<String, Long> pipelineAndStageMap = pipelineManager.getDefaultPipelineIdAndStageId(orgId);
            final FieldInfo dealSourceFieldInfo = dealFields.get(DealFields.DEAL_SOURCE);

            dealJson.put(dealFields.get(DealFields.TITLE).idAsString(), String.format("%s %s", jsonObject.containsKey(nameIdAsString)
                    ? jsonObject.getString(nameIdAsString) : value, dealSource));
            dealJson.put(dealFields.get(DealFields.PRIMARY_CONTACT).idAsString(), jsonObject);
            dealJson.put(dealFields.get(DealFields.STATUS).idAsString(), Deal.State.OPEN.getId());
            dealJson.put(dealFields.get(DealFields.SALES_OWNER).idAsString(), ownerId);
            dealJson.put(dealSourceFieldInfo.idAsString(), optionService.getOption(orgId, dealSourceFieldInfo,
                    DealSourceStaticOption.WEB_FORM.getName()).id());

            if (!pipelineAndStageMap.isEmpty()) {
                dealJson.put(dealFields.get(DealFields.PIPELINE).idAsString(),
                        pipelineAndStageMap.get(PipelineFields.PIPELINE_ID.getName()));
                dealJson.put(dealFields.get(DealFields.PIPELINE_STAGE).idAsString(),
                        pipelineAndStageMap.get(PipelineFields.PIPELINE_STAGE_ID.getName()));
            }

            return dealJson;
        }

        return jsonObject;
    }

    /**
     * Builds and loads the json of model store type fields.
     *
     * @param formField  Refers the {@link FormField} of webForm.
     * @param fieldInfo  Refers the {@link FieldInfo}.
     * @param value      Refers the field value.
     * @param jsonObject The {@link JsonObject} which refers the converted entity json details.
     * @param storeType  Refers the field {@link StoreType}.
     */
    private void buildAndLoadModelStoreJson(final FormField formField, final FieldInfo fieldInfo, final Object value,
                                            final JsonObject jsonObject, final StoreType storeType) {
        final JsonObject subFieldJson = Json.object();
        final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

        switch (storeType) {
            case EMAIL, EMAILS -> {
                if (storeType == StoreType.EMAIL) {
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.EMAIL_VALUE).idAsString(), value);
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.EMAIL_TYPE).idAsString(), Email.Type.WORK.getId());

                    jsonObject.put(fieldInfo.idAsString(), subFieldJson);
                } else {
                    final JsonArray jsonArray = Json.array();
                    final Map<StoreSubType, FieldSubType> emailsSubtype = fieldTypeService.groupSubtypesByStoreSubType(StoreType.EMAILS);

                    subFieldJson.put(emailsSubtype.get(StoreSubType.EMAIL_VALUE).idAsString(), value);
                    subFieldJson.put(emailsSubtype.get(StoreSubType.EMAIL_TYPE).idAsString(), Email.Type.WORK.getId());

                    jsonArray.put(subFieldJson);
                    jsonObject.put(fieldInfo.idAsString(), jsonArray);
                }
            }
            case PHONE, PHONES -> {
                if (storeType == StoreType.PHONE) {
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.PHONE_NUMBER).idAsString(), value);
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.PHONE_TYPE).idAsString(), Phone.Type.WORK.getId());

                    jsonObject.put(fieldInfo.idAsString(), subFieldJson);
                } else {
                    final JsonArray jsonArray = Json.array();
                    final Map<StoreSubType, FieldSubType> phonesSubtype = fieldTypeService.groupSubtypesByStoreSubType(StoreType.PHONES);

                    subFieldJson.put(phonesSubtype.get(StoreSubType.PHONE_NUMBER).idAsString(), value);
                    subFieldJson.put(phonesSubtype.get(StoreSubType.PHONE_TYPE).idAsString(), Phone.Type.WORK.getId());

                    jsonArray.put(subFieldJson);
                    jsonObject.put(fieldInfo.idAsString(), jsonArray);
                }
            }
            case ADDRESS -> {
                subFieldJson.put(fieldSubTypes.get(StoreSubType.ADDRESS_LINE1).idAsString(), value);
                jsonObject.put(fieldInfo.idAsString(), subFieldJson);
            }
            case MONETARY -> {
                if (value instanceof final Map<?, ?> dataMap) {
                    final Map<String, Object> monetaryMap = (Map<String, Object>) dataMap;

                    subFieldJson.put(fieldSubTypes.get(StoreSubType.CURRENCY).idAsString(), monetaryMap.get(StoreSubType.CURRENCY.getName().toLowerCase()));
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.VALUE).idAsString(), monetaryMap.get(AMOUNT));
                    jsonObject.put(fieldInfo.idAsString(), subFieldJson);
                }
            }
        }
    }

}