package com.tlc.crm.entity.models.type.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Email.Type;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import java.util.Map;
import java.util.Objects;

import static com.tlc.crm.entity.models.type.Email.Type.WORK;

/**
 * Provides different ways to convert the data from / into {@link Email}
 *
 * @author Sundar
 * @version 1.0
 */
public class EmailConverter extends EntityConverter<Email> {

    private final EmailManager emailManager;
    private final FieldTypeService fieldTypeService;

    private static class Instance {
        private static final EmailConverter INSTANCE = new EmailConverter();
    }

    private EmailConverter() {
        emailManager = EmailManager.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
    }

    /**
     * Gets the {@link EmailConverter} instance
     *
     * @return {@link EmailConverter}
     */
    public static EmailConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts {@link JsonObject} into {@link Email}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the email data
     * @return the {@link Email}
     */
    @Override
    public Email convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? buildEmailByPartialData(converterConfig, jsonObject)
                : buildEmail(converterConfig, jsonObject);
    }

    /**
     * Converts {@link JsonObject} into {@link Email}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the email data
     * @return the {@link Email}
     */
    private Email buildEmail(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Email email = new Email();
        final String idKey = FieldSubTypeKeys.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            email.setId(jsonObject.getLong(idKey));
        }
        setDataToEmail(converterConfig, email, jsonObject);

        return email;
    }

    /**
     * Sets the json data into {@link Email}
     *
     * @param converterConfig converter configuration
     * @param email           which contains the email data
     * @param jsonObject      which contains email fields data
     */
    private void setDataToEmail(final ConverterConfig converterConfig, final Email email, final JsonObject jsonObject) {
        final Map<StoreSubType, FieldSubType> emailSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.EMAIL);
        final Map<StoreSubType, FieldSubType> emailsSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.EMAILS);

        email.setOrgId(converterConfig.orgId());

        if (Objects.nonNull(emailSubtypes) && !emailSubtypes.isEmpty() &&
                Objects.nonNull(emailsSubtypes) && !emailsSubtypes.isEmpty()) {
            final String emailTypeKey = emailSubtypes.get(StoreSubType.EMAIL_TYPE).idAsString();
            final String emailValueKey = emailSubtypes.get(StoreSubType.EMAIL_VALUE).idAsString();
            final String emailsTypeKey = emailsSubtypes.get(StoreSubType.EMAIL_TYPE).idAsString();
            final String emailsValueKey = emailsSubtypes.get(StoreSubType.EMAIL_VALUE).idAsString();
            final Type type = jsonObject.containsKey(emailTypeKey) ? Type.valueOf(jsonObject.getInt(emailTypeKey))
                    : jsonObject.containsKey(emailsTypeKey) ? Type.valueOf(jsonObject.getInt(emailsTypeKey))
                    : converterConfig.isPartialData() ? email.getType() : Type.valueOf(WORK.getId());
            final String emailValue = jsonObject.optString(jsonObject.containsKey(emailValueKey) ?
                    emailValueKey : emailsValueKey, email.getValue());

            email.setType(type);
            email.setValue(emailValue);
        }
    }

    /**
     * Converts {@link JsonObject} into {@link Email}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the email data
     * @return the {@link Email}
     */
    private Email buildEmailByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Email email = emailManager.get(converterConfig.orgId(),
                jsonObject.getLong(FieldSubTypeKeys.ID.getName()));

        setDataToEmail(converterConfig, email, jsonObject);

        return email;
    }

    /**
     * Converts {@link Email} into {@link JsonObject}
     *
     * @param email A model that holds the attributes of email
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final Email email) {
        return convert(email, fieldTypeService.groupSubtypesByStoreSubType(StoreType.EMAIL));
    }

    /**
     * Converts {@link Email} into {@link JsonObject}
     *
     * @param email A model that holds the attributes of email
     * @return {@link JsonObject}
     */
    public JsonObject convertEmail(final Email email) {
        return convert(email, fieldTypeService.groupSubtypesByStoreSubType(StoreType.EMAILS));
    }

    /**
     * Converts {@link Email} into {@link JsonObject}
     *
     * @param email A model that holds the attributes of email
     * @return {@link JsonObject}
     */
    private JsonObject convert(final Email email, final Map<StoreSubType, FieldSubType> groupedSubtypes) {
        final JsonObject jsonObject = Json.object();

        if (Objects.nonNull(groupedSubtypes) && !groupedSubtypes.isEmpty()) {
            jsonObject.put(FieldSubTypeKeys.ID.getName(), email.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), email.orgId());
            jsonObject.put(groupedSubtypes.get(StoreSubType.EMAIL_TYPE).idAsString(), email.getType().getId());
            jsonObject.put(groupedSubtypes.get(StoreSubType.EMAIL_VALUE).idAsString(), email.getValue());
        }

        return jsonObject;
    }

    /**
     * Converts {@link Email} into {@link JsonObject}
     *
     * @param email           A model that holds the attributes of email
     * @param converterConfig Converter configuration settings
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final Email email) {
        final JsonObject jsonObject = Json.object();

        if (converterConfig.isIdRequiredOnKey()) {
            return convert(email);
        } else {
            jsonObject.put(FieldSubTypeKeys.ID.getName(), email.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), email.orgId());
            jsonObject.put(StoreSubType.EMAIL_TYPE.getAliasName(), email.getType().getId());
            jsonObject.put(StoreSubType.EMAIL_VALUE.getAliasName(), email.getValue());
        }

        return jsonObject;
    }

    /**
     * Converts {@link Email} into {@link JsonObject}
     *
     * @param model           The {@link Email} model that holds the attributes of the email.
     * @param storeType       The {@link StoreType} that specifies the context for grouping subtypes.
     * @param converterConfig The configuration used for conversion, providing locale or other settings.
     * @return {@link JsonObject}
     */
    public JsonObject convertModelToJson(final Email model, final StoreType storeType, final ConverterConfig converterConfig) {
        final Map<StoreSubType, FieldSubType> groupedSubtypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);
        final JsonObject jsonObject = Json.object();

        if (Objects.nonNull(groupedSubtypes) && !groupedSubtypes.isEmpty()) {
            final Email.Type emailType = model.getType();

            jsonObject.put(FieldSubTypeKeys.ID.getName(), model.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), model.orgId());
            jsonObject.put(groupedSubtypes.get(StoreSubType.EMAIL_TYPE).idAsString(), emailType.convertModelToOptionJson(emailType, converterConfig));
            jsonObject.put(groupedSubtypes.get(StoreSubType.EMAIL_VALUE).idAsString(), model.getValue());
        }

        return jsonObject;
    }

    /**
     * Converts {@link Email} into {@link JsonObject}
     *
     * @param model           The {@link Email} model that holds the attributes of the email.
     * @param converterConfig The configuration used for conversion, providing locale or other settings.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToJson(final Email model, final ConverterConfig converterConfig) {
        return convertModelToJson(model, StoreType.EMAIL, converterConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param model           The {@link Email} model that holds the attributes of the email.
     * @param converterConfig The configuration used for conversion, providing locale or other settings.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertToRestJson(final Email model, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final Email.Type emailType = model.getType();

        jsonObject.put(FieldSubTypeKeys.ID.getName(), model.id());
        jsonObject.put(StoreSubType.EMAIL_TYPE.getApiPropertyName(), emailType.convertModelToOptionJson(emailType, converterConfig));
        jsonObject.put(StoreSubType.EMAIL_VALUE.getApiPropertyName(), model.getValue());

        return jsonObject;
    }
}
