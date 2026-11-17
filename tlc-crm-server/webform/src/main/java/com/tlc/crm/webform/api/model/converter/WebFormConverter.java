package com.tlc.crm.webform.api.model.converter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.models.Language;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.webform.url.UrlGenerator;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;

import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.webform.api.manager.WebFormManager;
import com.tlc.crm.webform.api.model.Alignment;
import com.tlc.crm.webform.api.model.Font;
import com.tlc.crm.webform.api.model.WebForm;
import com.tlc.crm.webform.api.model.RecipientEmail;
import com.tlc.crm.webform.api.model.SubmitAction;
import com.tlc.crm.webform.api.model.FieldStyle;
import com.tlc.crm.webform.api.model.FormField;
import com.tlc.crm.webform.api.model.FormElement;
import com.tlc.crm.webform.api.model.FormStyle;
import com.tlc.crm.webform.api.model.SizeType;
import com.tlc.crm.webform.api.model.SubmitButtonStyle;
import com.tlc.crm.webform.api.model.FormFieldProperty;
import com.tlc.crm.webform.api.model.FormFieldProperty.Type;
import com.tlc.crm.webform.field.WebFormFields;
import com.tlc.crm.webform.field.WebFormStyleFields;
import com.tlc.crm.webform.internal.status.WebFormErrorCodes;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides different ways to convert the data from / into {@link WebForm}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class WebFormConverter extends EntityConverter<WebForm> {

    private final WebFormManager webFormManager;
    private final I18nService i18nAccess;
    private final UrlGenerator urlGenerator;
    private final FieldTypeService fieldTypeService;

    private WebFormConverter() {
        webFormManager = WebFormManager.getInstance();
        i18nAccess = I18nServiceProvider.get();
        urlGenerator = UrlGenerator.get();
        fieldTypeService = FieldTypeService.getInstance();
    }

    private static class Instance {
        private static final WebFormConverter INSTANCE = new WebFormConverter();
    }

    /**
     * Gets the {@link WebFormConverter} Instance
     *
     * @return {@link WebFormConverter}
     */
    public static WebFormConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Which refers the {@link ConverterConfig}.
     * @param jsonObject      Which holds the json data.
     * @return {@link WebForm} which contains the webForm details.
     */
    @Override
    public WebForm convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? constructWebFormByPartialData(converterConfig, jsonObject)
                : constructWebForm(converterConfig, jsonObject);
    }

    /**
     * Converts the partial {@link JsonObject} into the {@link WebForm}.
     *
     * @param converterConfig Which refers the {@link ConverterConfig}.
     * @param jsonObject      Which holds the json data.
     * @return {@link WebForm} which contains the webForm details.
     */
    private WebForm constructWebFormByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final WebForm webForm = webFormManager.get(converterConfig.orgId(), jsonObject.getLong(WebFormFields.ID.getName()));

        setJsonDataToModel(converterConfig, webForm, jsonObject);
        return webForm;
    }

    /**
     * Converts the {@link JsonObject} into the {@link WebForm}.
     *
     * @param converterConfig Which refers the {@link ConverterConfig}.
     * @param jsonObject      Which holds the json data.
     * @return {@link WebForm} which contains the webForm details.
     */
    private WebForm constructWebForm(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final WebForm webForm = new WebForm();
        final String idKey = WebFormFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            webForm.setId(jsonObject.getLong(idKey));
        }
        setJsonDataToModel(converterConfig, webForm, jsonObject);

        return webForm;
    }

    /**
     * Sets the json data into the web form.
     *
     * @param converterConfig Which refers the {@link ConverterConfig}.
     * @param webForm         Which refers the {@link WebForm}.
     * @param jsonObject      Which holds the json data.
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final WebForm webForm, final JsonObject jsonObject) {
        final String nameKey = WebFormFields.FORM_NAME.getName();
        final String descriptionKey = WebFormFields.DESCRIPTION.getName();
        final String submitButtonNameKey = WebFormFields.SUBMIT_BUTTON_NAME.getName();
        final String attachmentIdKey = WebFormFields.ATTACHMENT_ID.getName();
        final String languageIdKey = WebFormFields.LANGUAGE_ID.getName();
        final String templateIdKey = WebFormFields.TEMPLATE_ID.getName();

        webForm.setOrgId(converterConfig.orgId());
        webForm.setOwnerId(converterConfig.ownerId());
        webForm.setName(jsonObject.containsKey(nameKey)
                ? jsonObject.getString(nameKey) : webForm.getName());
        webForm.setDescription(jsonObject.containsKey(descriptionKey)
                ? jsonObject.getString(descriptionKey) : webForm.getDescription());
        webForm.setSubmitButtonName(jsonObject.containsKey(submitButtonNameKey)
                ? jsonObject.getString(submitButtonNameKey) : webForm.getSubmitButtonName());
        webForm.setLanguage(jsonObject.containsKey(languageIdKey)
                ? Language.getLanguageById(jsonObject.getInt(languageIdKey)) : webForm.getLanguage());
        webForm.setTemplateId(jsonObject.containsKey(templateIdKey)
                ? jsonObject.getLong(templateIdKey) : webForm.getTemplateId());
        webForm.setAttachmentId(jsonObject.containsKey(attachmentIdKey)
                ? (Long) jsonObject.opt(attachmentIdKey) : webForm.getAttachmentId());
        webForm.setActive(jsonObject.optBoolean(WebFormFields.IS_ACTIVE.getName(), true));

        if (Objects.isNull(webForm.id())) {
            webForm.setVisits(0);
            webForm.setSubmissions(0);
            webForm.setConversions(0.0);
        }
        constructStyles(converterConfig, webForm, jsonObject);
        constructFieldProperties(converterConfig, webForm, jsonObject);
        constructSubmitAction(converterConfig, webForm, jsonObject);
    }

    /**
     * Constructs the web form styles.
     *
     * @param converterConfig Refers the {@link ConverterConfig}.
     * @param webForm         Refers the {@link WebForm}.
     * @param jsonObject      Refers the {@link JsonObject} which holds the data.
     */
    private void constructStyles(final ConverterConfig converterConfig, final WebForm webForm, final JsonObject jsonObject) {
        final String styleKey = WebFormStyleFields.STYLE.getName();

        if (jsonObject.containsKey(styleKey)) {
            final JsonObject styleJson = jsonObject.getJsonObject(styleKey);
            final String backgroundColorKey = WebFormStyleFields.BACKGROUND_COLOR.getName();
            final String textColorKey = WebFormStyleFields.TEXT_COLOR.getName();

            constructFormStyle(converterConfig, webForm, styleJson, backgroundColorKey);
            constructFieldStyle(converterConfig, webForm, styleJson, backgroundColorKey, textColorKey);
            constructSubmitButtonStyle(converterConfig, webForm, styleJson, backgroundColorKey, textColorKey);
        }
    }

    /**
     * Constructs the {@link JsonObject} details into {@link SubmitButtonStyle}.
     *
     * @param converterConfig    Refers the {@link ConverterConfig}.
     * @param webForm            Refers the {@link WebForm}.
     * @param jsonObject         Refers the {@link JsonObject} which holds the data.
     * @param backgroundColorKey Refers the background color key name.
     * @param textColorKey       Refers the text color key name.
     */
    private void constructSubmitButtonStyle(final ConverterConfig converterConfig, final WebForm webForm, final JsonObject jsonObject,
                                            final String backgroundColorKey, final String textColorKey) {
        final String submitButtonStyleKey = WebFormStyleFields.SUBMIT_BUTTON_STYLE.getName();

        if (jsonObject.containsKey(submitButtonStyleKey)) {
            final JsonObject submitButtonStyleJson = jsonObject.getJsonObject(submitButtonStyleKey);
            final String idKey = WebFormFields.ID.getName();
            final String alignmentKey = WebFormStyleFields.ALIGNMENT_ID.getName();
            final String widthKey = WebFormStyleFields.WIDTH.getName();
            final SubmitButtonStyle submitButtonStyle = new SubmitButtonStyle();

            if (submitButtonStyleJson.containsKey(idKey)) {
                submitButtonStyle.setId(submitButtonStyleJson.getLong(idKey));
            }
            final boolean isPartialData = converterConfig.isPartialData();
            final SubmitButtonStyle oldButtonStyle = isPartialData ? webForm.getSubmitButtonStyle() : null;
            final boolean isOldButtonStyleExist = isPartialData && Objects.nonNull(oldButtonStyle)
                    && Objects.equals(submitButtonStyle.id(), oldButtonStyle.id());

            if (submitButtonStyleJson.containsKey(backgroundColorKey)) {
                submitButtonStyle.setBackgroundColor(submitButtonStyleJson.getString(backgroundColorKey));
            }

            if (submitButtonStyleJson.containsKey(alignmentKey)) {
                submitButtonStyle.setAlignment(Alignment.getAlignment(submitButtonStyleJson.getInt(alignmentKey)));
            }

            if (submitButtonStyleJson.containsKey(textColorKey)) {
                submitButtonStyle.setTextColor(submitButtonStyleJson.getString(textColorKey));
            }

            if (submitButtonStyleJson.containsKey(widthKey)) {
                submitButtonStyle.setWidth(submitButtonStyleJson.getInt(widthKey));
            }

            if (isOldButtonStyleExist) {

                if (Objects.isNull(submitButtonStyle.getBackgroundColor())) {
                    submitButtonStyle.setBackgroundColor(oldButtonStyle.getBackgroundColor());
                }

                if (Objects.isNull(submitButtonStyle.getAlignment())) {
                    submitButtonStyle.setAlignment(oldButtonStyle.getAlignment());
                }

                if (Objects.equals(0, submitButtonStyle.getWidth())) {
                    submitButtonStyle.setWidth(oldButtonStyle.getWidth());
                }

                if (Objects.isNull(submitButtonStyle.getTextColor())) {
                    submitButtonStyle.setTextColor(oldButtonStyle.getTextColor());
                }
            }
            webForm.setSubmitButtonStyle(submitButtonStyle);
        }
    }

    /**
     * Constructs the {@link JsonObject} details into {@link FieldStyle}.
     *
     * @param converterConfig    Refers the {@link ConverterConfig}.
     * @param webForm            Refers the {@link WebForm}.
     * @param jsonObject         Refers the {@link JsonObject} which holds the data.
     * @param backgroundColorKey Refers the background color key name.
     * @param textColorKey       Refers the text color key name.
     */
    private void constructFieldStyle(final ConverterConfig converterConfig, final WebForm webForm, final JsonObject jsonObject,
                                     final String backgroundColorKey, final String textColorKey) {
        final String fieldStyleKey = WebFormStyleFields.FIELD_STYLE.getName();

        if (jsonObject.containsKey(fieldStyleKey)) {
            final JsonObject fieldStyleJson = jsonObject.getJsonObject(fieldStyleKey);
            final String idKey = WebFormFields.ID.getName();
            final String labelKey = WebFormStyleFields.LABEL.getName();
            final String colorKey = WebFormStyleFields.COLOR.getName();
            final String fontSizeKey = WebFormStyleFields.FONT_SIZE.getName();
            final String inputKey = WebFormStyleFields.INPUT.getName();
            final FieldStyle fieldStyle = new FieldStyle();
            final boolean isPartialData = converterConfig.isPartialData();

            if (fieldStyleJson.containsKey(idKey)) {
                fieldStyle.setId(fieldStyleJson.getLong(idKey));
            }
            final FieldStyle oldFieldStyle = isPartialData ? webForm.getFieldStyle() : null;
            final boolean isOldFieldStyleExist = isPartialData && Objects.nonNull(oldFieldStyle)
                    && Objects.equals(fieldStyle.id(), oldFieldStyle.id());

            if (fieldStyleJson.containsKey(labelKey)) {
                final JsonObject labelJson = fieldStyleJson.getJsonObject(labelKey);

                if (labelJson.containsKey(colorKey)) {
                    fieldStyle.setLabelColor(labelJson.getString(colorKey));
                }

                if (labelJson.containsKey(fontSizeKey)) {
                    fieldStyle.setLabelFontSize(labelJson.getInt(fontSizeKey));
                }

                if (isOldFieldStyleExist) {

                    if (Objects.isNull(fieldStyle.getLabelColor())) {
                        fieldStyle.setLabelColor(oldFieldStyle.getLabelColor());
                    }

                    if (fieldStyle.getLabelFontSize() == 0) {
                        fieldStyle.setLabelFontSize(oldFieldStyle.getLabelFontSize());
                    }
                }
            } else if (isPartialData) {
                fieldStyle.setLabelColor(oldFieldStyle.getLabelColor());
                fieldStyle.setLabelFontSize(oldFieldStyle.getLabelFontSize());
            }

            if (fieldStyleJson.containsKey(inputKey)) {
                final JsonObject inputJson = fieldStyleJson.getJsonObject(inputKey);

                if (inputJson.containsKey(textColorKey)) {
                    fieldStyle.setInputTextColor(inputJson.getString(textColorKey));
                }

                if (inputJson.containsKey(backgroundColorKey)) {
                    fieldStyle.setInputBackgroundColor(inputJson.getString(backgroundColorKey));
                }

                if (inputJson.containsKey(fontSizeKey)) {
                    fieldStyle.setInputFontSize(inputJson.getInt(fontSizeKey));
                }

                if (isOldFieldStyleExist) {

                    if (Objects.isNull(fieldStyle.getInputTextColor())) {
                        fieldStyle.setInputTextColor(oldFieldStyle.getInputTextColor());
                    }

                    if (Objects.isNull(fieldStyle.getInputBackgroundColor())) {
                        fieldStyle.setInputBackgroundColor(oldFieldStyle.getInputBackgroundColor());
                    }

                    if (Objects.equals(0, fieldStyle.getInputFontSize())) {
                        fieldStyle.setInputFontSize(oldFieldStyle.getInputFontSize());
                    }
                }
            } else if (isPartialData) {
                fieldStyle.setInputTextColor(oldFieldStyle.getInputTextColor());
                fieldStyle.setInputFontSize(oldFieldStyle.getInputFontSize());
                fieldStyle.setInputBackgroundColor(oldFieldStyle.getInputBackgroundColor());
            }
            webForm.setFieldStyle(fieldStyle);
        }
    }

    /**
     * Constructs the {@link JsonObject} details into {@link FormStyle}.
     *
     * @param converterConfig    Refers the {@link ConverterConfig}.
     * @param webForm            Refers the {@link WebForm}.
     * @param jsonObject         Refers the {@link JsonObject} which holds the data.
     * @param backgroundColorKey Refers the background color key name.
     */
    private void constructFormStyle(final ConverterConfig converterConfig, final WebForm webForm,
                                    final JsonObject jsonObject, final String backgroundColorKey) {
        final String formStyleKey = WebFormStyleFields.FORM_STYLE.getName();

        if (jsonObject.containsKey(formStyleKey)) {
            final JsonObject formStyleJson = jsonObject.getJsonObject(formStyleKey);
            final String idKey = WebFormFields.ID.getName();
            final String alignmentKey = WebFormStyleFields.ALIGNMENT_ID.getName();
            final String fontKey = WebFormStyleFields.FONT_ID.getName();
            final boolean isPartialData = converterConfig.isPartialData();
            final FormStyle formStyle = new FormStyle();

            if (formStyleJson.containsKey(idKey)) {
                formStyle.setId(formStyleJson.getLong(idKey));
            }
            final FormStyle oldFormStyle = isPartialData ? webForm.getFormStyle() : null;
            final boolean isOldFormStyleExist = isPartialData && Objects.nonNull(oldFormStyle)
                    && Objects.equals(formStyle.id(), oldFormStyle.id());

            if (formStyleJson.containsKey(backgroundColorKey)) {
                formStyle.setBackgroundColor(formStyleJson.getString(backgroundColorKey));
            }

            if (formStyleJson.containsKey(alignmentKey)) {
                formStyle.setAlignment(Alignment.getAlignment(formStyleJson.getInt(alignmentKey)));
            }

            if (formStyleJson.containsKey(fontKey)) {
                formStyle.setFont(Font.getFont(formStyleJson.getInt(fontKey)));
            }

            if (isOldFormStyleExist) {

                if (Objects.isNull(formStyle.getBackgroundColor())) {
                    formStyle.setBackgroundColor(oldFormStyle.getBackgroundColor());
                }

                if (Objects.isNull(formStyle.getAlignment())) {
                    formStyle.setAlignment(oldFormStyle.getAlignment());
                }

                if (Objects.isNull(formStyle.getFont())) {
                    formStyle.setFont(oldFormStyle.getFont());
                }
            }
            constructHeaderAndSubHeaderStyle(formStyle, formStyleJson, isPartialData, isOldFormStyleExist, oldFormStyle);
            constructDescriptionAndTextStyle(formStyle, formStyleJson, isPartialData, isOldFormStyleExist, oldFormStyle);
            webForm.setFormStyle(formStyle);
        }
    }

    /**
     * Constructs the header and sub header json data into {@link FormStyle}.
     *
     * @param formStyle           Refers the {@link FormStyle}.
     * @param formStyleJson       Refers the {@link JsonObject} which holds the data.
     * @param isPartialData       A flag that indicates whether the json is partial or not.
     * @param isOldFormStyleExist A flag that indicates whether the old form style is exist or not.
     * @param oldFormStyle        Refers the old form style.
     */
    private void constructHeaderAndSubHeaderStyle(final FormStyle formStyle, final JsonObject formStyleJson,
                                                  final boolean isPartialData, final boolean isOldFormStyleExist,
                                                  final FormStyle oldFormStyle) {
        final String headerKey = WebFormStyleFields.HEADER.getName();
        final String subHeaderKey = WebFormStyleFields.SUB_HEADER.getName();
        final String textColorKey = WebFormStyleFields.TEXT_COLOR.getName();
        final String fontSizeKey = WebFormStyleFields.FONT_SIZE.getName();

        if (formStyleJson.containsKey(headerKey)) {
            final JsonObject headerJson = formStyleJson.getJsonObject(headerKey);

            if (headerJson.containsKey(textColorKey)) {
                formStyle.setHeaderTextColor(headerJson.getString(textColorKey));
            }

            if (headerJson.containsKey(fontSizeKey)) {
                formStyle.setHeaderFontSize(headerJson.getInt(fontSizeKey));
            }

            if (isOldFormStyleExist) {

                if (Objects.isNull(formStyle.getHeaderTextColor())) {
                    formStyle.setHeaderTextColor(oldFormStyle.getHeaderTextColor());
                }

                if (Objects.equals(0, formStyle.getHeaderFontSize())) {
                    formStyle.setHeaderFontSize(oldFormStyle.getHeaderFontSize());
                }
            }
        } else if (isPartialData) {
            formStyle.setHeaderTextColor(oldFormStyle.getHeaderTextColor());
            formStyle.setHeaderFontSize(oldFormStyle.getHeaderFontSize());
        }

        if (formStyleJson.containsKey(subHeaderKey)) {
            final JsonObject subHeaderJson = formStyleJson.getJsonObject(subHeaderKey);

            if (subHeaderJson.containsKey(textColorKey)) {
                formStyle.setSubHeaderTextColor(subHeaderJson.getString(textColorKey));
            }

            if (subHeaderJson.containsKey(fontSizeKey)) {
                formStyle.setSubHeaderFontSize(subHeaderJson.getInt(fontSizeKey));
            }

            if (isOldFormStyleExist) {

                if (Objects.isNull(formStyle.getSubHeaderTextColor())) {
                    formStyle.setSubHeaderTextColor(oldFormStyle.getSubHeaderTextColor());
                }

                if (Objects.equals(0, formStyle.getSubHeaderFontSize())) {
                    formStyle.setSubHeaderFontSize(oldFormStyle.getSubHeaderFontSize());
                }
            }
        } else if (isPartialData) {
            formStyle.setSubHeaderTextColor(oldFormStyle.getSubHeaderTextColor());
            formStyle.setSubHeaderFontSize(oldFormStyle.getSubHeaderFontSize());
        }
    }

    /**
     * Constructs the description and text json data into {@link FormStyle}.
     *
     * @param formStyle           Refers the {@link FormStyle}.
     * @param formStyleJson       Refers the {@link JsonObject} which holds the data.
     * @param isPartialData       A flag that indicates whether the json is partial or not.
     * @param isOldFormStyleExist A flag that indicates whether the old form style is exist or not.
     * @param oldFormStyle        Refers the old form style.
     */
    private void constructDescriptionAndTextStyle(final FormStyle formStyle, final JsonObject formStyleJson,
                                                  final boolean isPartialData, final boolean isOldFormStyleExist,
                                                  final FormStyle oldFormStyle) {
        final String descriptionKey = WebFormStyleFields.DESCRIPTION.getName();
        final String textKey = WebFormStyleFields.TEXT.getName();
        final String textColorKey = WebFormStyleFields.TEXT_COLOR.getName();
        final String fontSizeKey = WebFormStyleFields.FONT_SIZE.getName();

        if (formStyleJson.containsKey(descriptionKey)) {
            final JsonObject descriptionJson = formStyleJson.getJsonObject(descriptionKey);

            if (descriptionJson.containsKey(textColorKey)) {
                formStyle.setDescriptionTextColor(descriptionJson.getString(textColorKey));
            }

            if (descriptionJson.containsKey(fontSizeKey)) {
                formStyle.setDescriptionFontSize(descriptionJson.getInt(fontSizeKey));
            }

            if (isOldFormStyleExist) {

                if (Objects.isNull(formStyle.getDescriptionTextColor())) {
                    formStyle.setDescriptionTextColor(oldFormStyle.getDescriptionTextColor());
                }

                if (Objects.equals(0, formStyle.getDescriptionFontSize())) {
                    formStyle.setDescriptionFontSize(oldFormStyle.getDescriptionFontSize());
                }
            }
        } else if (isPartialData) {
            formStyle.setDescriptionTextColor(oldFormStyle.getDescriptionTextColor());
            formStyle.setDescriptionFontSize(oldFormStyle.getDescriptionFontSize());
        }

        if (formStyleJson.containsKey(textKey)) {
            final JsonObject textJson = formStyleJson.getJsonObject(textKey);

            if (textJson.containsKey(textColorKey)) {
                formStyle.setTextColor(textJson.getString(textColorKey));
            }

            if (textJson.containsKey(fontSizeKey)) {
                formStyle.setTextFontSize(textJson.getInt(fontSizeKey));
            }

            if (isOldFormStyleExist) {

                if (Objects.isNull(formStyle.getTextColor())) {
                    formStyle.setTextColor(oldFormStyle.getTextColor());
                }

                if (Objects.equals(0, formStyle.getTextFontSize())) {
                    formStyle.setTextFontSize(oldFormStyle.getTextFontSize());
                }
            }
        } else if (isPartialData) {
            formStyle.setTextColor(oldFormStyle.getTextColor());
            formStyle.setTextFontSize(oldFormStyle.getTextFontSize());
        }
    }

    /**
     * Constructs the {@link JsonObject} details into {@link SubmitAction}.
     *
     * @param converterConfig Which refers the {@link ConverterConfig}.
     * @param webForm         Which refers the {@link WebForm}.
     * @param jsonObject      Which holds the json data.
     */
    private void constructSubmitAction(final ConverterConfig converterConfig, final WebForm webForm, final JsonObject jsonObject) {
        final String onSubmitKey = WebFormFields.ON_SUBMIT.getName();
        final boolean isPartialData = converterConfig.isPartialData();

        if (jsonObject.containsKey(onSubmitKey)) {
            final JsonObject submitActionJson = jsonObject.getJsonObject(onSubmitKey);
            final String idKey = WebFormFields.ID.getName();
            final String thankYouMessageKey = WebFormFields.THANK_YOU_MESSAGE.getName();
            final String redirectKey = WebFormFields.REDIRECT_URL.getName();
            final String emailNotificationKey = WebFormFields.EMAIL_NOTIFICATION.getName();
            final String isEnabledKey = WebFormFields.IS_ENABLED.getName();
            final SubmitAction submitAction = new SubmitAction();

            if (submitActionJson.containsKey(idKey)) {
                submitAction.setId(submitActionJson.getLong(idKey));
            }
            final SubmitAction oldSubmitAction = isPartialData ? webForm.getSubmitAction() : null;
            final boolean isOldSubmitActionExist = isPartialData && Objects.nonNull(oldSubmitAction)
                    && Objects.equals(submitAction.id(), oldSubmitAction.id());

            if (submitActionJson.containsKey(thankYouMessageKey)) {
                final String messageKey = WebFormFields.MESSAGE.getName();
                final JsonObject thankYouMessageJson = submitActionJson.getJsonObject(thankYouMessageKey);

                submitAction.setThankYouMessageEnabled(thankYouMessageJson.optBoolean(isEnabledKey));
                submitAction.setThankYouMessage(thankYouMessageJson.containsKey(messageKey)
                        ? thankYouMessageJson.getString(messageKey) : submitAction.getThankYouMessage());

            } else if (isOldSubmitActionExist && !submitActionJson.containsKey(thankYouMessageKey)) {
                final I18nKey i18nMessage = oldSubmitAction.getI18nMessage();

                submitAction.setThankYouMessageEnabled(oldSubmitAction.isThankYouMessageEnabled());
                submitAction.setThankYouMessage(Objects.nonNull(i18nMessage)
                        ? i18nAccess.getResolver(converterConfig.locale()).get(i18nMessage) : null);
            }

            if (submitActionJson.containsKey(redirectKey)) {
                final String urlKey = WebFormFields.URL.getName();
                final JsonObject redirectJson = submitActionJson.getJsonObject(redirectKey);

                submitAction.setRedirectEnabled(redirectJson.optBoolean(isEnabledKey));
                submitAction.setRedirectUrl(redirectJson.containsKey(urlKey)
                        ? redirectJson.getString(urlKey) : submitAction.getRedirectUrl());

            } else if (isOldSubmitActionExist && !submitActionJson.containsKey(redirectKey)) {
                submitAction.setRedirectEnabled(oldSubmitAction.isRedirectEnabled());
                submitAction.setRedirectUrl(oldSubmitAction.getRedirectUrl());
            }

            submitAction.setContactCreationEnabled(submitActionJson.optBoolean(WebFormFields.IS_CONTACT_CREATION_ENABLED.getName(),
                    isOldSubmitActionExist ? oldSubmitAction.isContactCreationEnabled() : submitAction.isContactCreationEnabled()));
            submitAction.setDealCreationWithContactEnabled(submitActionJson.optBoolean(WebFormFields.IS_DEAL_CREATION_WITH_CONTACT_ENABLED.getName(),
                    isOldSubmitActionExist ? oldSubmitAction.isDealCreationWithContactEnabled() : submitAction.isDealCreationWithContactEnabled()));

            if (isOldSubmitActionExist && !submitActionJson.containsKey(emailNotificationKey)) {
                submitAction.setEmailNotificationEnabled(oldSubmitAction.isEmailNotificationEnabled());
                submitAction.setRecipientEmails(oldSubmitAction.getRecipientEmails());
            } else {
                final JsonObject emailJson = submitActionJson.optJsonObject(emailNotificationKey);

                if (null != emailJson) {
                    submitAction.setEmailNotificationEnabled(emailJson.containsKey(isEnabledKey)
                            ? emailJson.getBoolean(isEnabledKey) : submitAction.isEmailNotificationEnabled());
                    buildRecipientEmails(converterConfig, webForm, submitAction, emailJson);
                }
            }
            webForm.setSubmitAction(submitAction);
        } else if (isPartialData) {
            final SubmitAction oldSubmitAction = webForm.getSubmitAction();
            final SubmitAction submitAction = new SubmitAction();

            if (Objects.nonNull(oldSubmitAction)) {
                submitAction.setId(oldSubmitAction.id());
                final I18nKey i18nMessage = oldSubmitAction.getI18nMessage();

                submitAction.setThankYouMessageEnabled(oldSubmitAction.isThankYouMessageEnabled());
                submitAction.setThankYouMessage(Objects.nonNull(i18nMessage)
                        ? i18nAccess.getResolver(converterConfig.locale()).get(i18nMessage) : oldSubmitAction.getThankYouMessage());
                submitAction.setRedirectEnabled(oldSubmitAction.isRedirectEnabled());
                submitAction.setRedirectUrl(oldSubmitAction.getRedirectUrl());
                submitAction.setEmailNotificationEnabled(oldSubmitAction.isEmailNotificationEnabled());
                submitAction.setRecipientEmails(oldSubmitAction.getRecipientEmails());
                submitAction.setContactCreationEnabled(oldSubmitAction.isContactCreationEnabled());
                submitAction.setDealCreationWithContactEnabled(oldSubmitAction.isDealCreationWithContactEnabled());
            }
            webForm.setSubmitAction(submitAction);
        }
    }

    /**
     * Constructs the recipient emails {@link JsonObject} data into {@link RecipientEmail}.
     *
     * @param converterConfig Which refers the {@link ConverterConfig}.
     * @param webForm         Which refers the {@link WebForm}.
     * @param submitAction    Which refers the {@link SubmitAction}.
     * @param emailJson       Which refers the webForm email notification json.
     */
    private void buildRecipientEmails(final ConverterConfig converterConfig, final WebForm webForm,
                                      final SubmitAction submitAction, final JsonObject emailJson) {
        final JsonArray emails = emailJson.optJsonArray(WebFormFields.RECIPIENTS.getName());
        final boolean isPartialData = converterConfig.isPartialData();
        final Collection<RecipientEmail> oldRecipientEmails = isPartialData
                ? webForm.getSubmitAction().getRecipientEmails() : submitAction.getRecipientEmails();

        submitAction.setRecipientEmails(null);

        if (submitAction.isEmailNotificationEnabled() && Objects.nonNull(emails)) {
            final String valueKey = WebFormFields.VALUE.getName();
            final String idKey = WebFormFields.ID.getName();

            for (int index = 0; index < emails.size(); index++) {
                final JsonObject recipientJson = emails.optJsonObject(index);
                final RecipientEmail recipientEmail = new RecipientEmail();

                if (recipientJson.containsKey(idKey)) {
                    recipientEmail.setId(recipientJson.getLong(idKey));
                }
                final RecipientEmail oldRecipientEmail = isPartialData ? oldRecipientEmails.stream()
                        .filter(oldRecipient -> Objects.nonNull(oldRecipient.id()) && Objects.equals(recipientEmail.id(), oldRecipient.id()))
                        .findAny().orElse(null) : null;

                recipientEmail.setValue(recipientJson.containsKey(valueKey) ? recipientJson.getString(valueKey)
                        : Objects.nonNull(oldRecipientEmail) ? oldRecipientEmail.getValue() : recipientEmail.getValue());
                submitAction.addRecipientEmail(recipientEmail);
            }

            if (isPartialData) {
                final Set<Long> currentRecipientEmailIds = submitAction.getRecipientEmails().stream()
                        .map(RecipientEmail::id).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

                oldRecipientEmails.stream().filter(oldRecipientEmail -> !currentRecipientEmailIds.contains(oldRecipientEmail.id()))
                        .forEach(submitAction::addRecipientEmail);
            }
        } else if (isPartialData) {
            submitAction.setRecipientEmails(oldRecipientEmails);
        }
    }

    /**
     * Constructs the {@link FormField} and {@link FormElement} details into the {@link WebForm}.
     *
     * @param converterConfig Which refers the {@link ConverterConfig}.
     * @param webForm         Which refers the {@link WebForm}.
     * @param jsonObject      Which holds the json data.
     */
    private void constructFieldProperties(final ConverterConfig converterConfig, final WebForm webForm,
                                          final JsonObject jsonObject) {
        final JsonArray fieldProperties = jsonObject.optJsonArray(WebFormFields.FORM_PROPERTIES.getName());
        final Collection<FormFieldProperty> oldFormFieldProperties = webForm.getFormFieldProperties();
        final boolean isPartialData = converterConfig.isPartialData();

        webForm.setFormFieldProperties(null);

        if (Objects.nonNull(fieldProperties)) {
            final String idKey = WebFormFields.ID.getName();
            final String fieldIdKey = WebFormFields.FIELD_ID.getName();
            final Long orgId = webForm.orgId();
            final Long webformId = webForm.id();
            final int fieldPropertiesSize = fieldProperties.size();

            for (int index = 0; index < fieldPropertiesSize; index++) {
                final JsonObject fieldPropertyJson = fieldProperties.optJsonObject(index);
                final Type type;

                try {
                    type = Type.valueOf(fieldPropertyJson.getString(WebFormFields.TYPE.getName()).toUpperCase());
                } catch (IllegalArgumentException exception) {
                    throw ErrorCode.get(WebFormErrorCodes.INVALID_FIELD_PROPERTY_TYPE);
                }
                final FormFieldProperty formFieldProperty = switch (type) {
                    case FIELD -> new FormField();
                    case ELEMENT -> new FormElement();
                };

                if (fieldPropertyJson.containsKey(idKey)) {
                    formFieldProperty.setId(fieldPropertyJson.getLong(idKey));
                } else if (Type.FIELD == type) {
                    webFormManager.getFormFieldPropertyId(orgId, webformId, fieldPropertyJson.getLong(fieldIdKey)).ifPresent(formFieldProperty::setId);
                }

                final FormFieldProperty oldFormFieldProperty = isPartialData ? oldFormFieldProperties.stream()
                        .filter(oldProperty -> Objects.nonNull(oldProperty.id()) && Objects.equals(formFieldProperty.id(), oldProperty.id())
                                && Objects.equals(formFieldProperty.getType(), oldProperty.getType())).findAny().orElse(null) : null;

                addFieldProperty(isPartialData, formFieldProperty, webForm, fieldPropertyJson, oldFormFieldProperty);
            }

            if (isPartialData) {
                final Set<Long> presentFieldPropertyIds = webForm.getFormFieldProperties().stream()
                        .map(FormFieldProperty::id).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

                oldFormFieldProperties.stream().filter(oldFieldProperty -> !presentFieldPropertyIds.contains(oldFieldProperty.id()))
                        .forEach(webForm::addFormFieldProperty);
            }
        } else if (isPartialData) {
            webForm.setFormFieldProperties(oldFormFieldProperties);
        }
    }

    /**
     * Adds the json data into {@link FormFieldProperty}.
     *
     * @param isPartialData        A flag that refers the json data is partial or not.
     * @param formFieldProperty    An entity that holds the attributes of form field property.
     * @param webForm              Refers the {@link WebForm}.
     * @param fieldPropertyJson    Which holds the field values of form fields and elements details.
     * @param oldFormFieldProperty Refers the old field property.
     */
    private void addFieldProperty(final boolean isPartialData, final FormFieldProperty formFieldProperty, final WebForm webForm,
                                  final JsonObject fieldPropertyJson, final FormFieldProperty oldFormFieldProperty) {
        final String labelKey = WebFormFields.LABEL.getName();
        final String fieldIdKey = WebFormFields.FIELD_ID.getName();
        final String elementNameKey = WebFormFields.NAME.getName();
        final String configKey = WebFormFields.CONFIG.getName();
        final boolean isOldFieldPropertyExist = isPartialData && Objects.nonNull(oldFormFieldProperty);

        if (isOldFieldPropertyExist && !fieldPropertyJson.containsKey(configKey)) {
            formFieldProperty.setConfig(oldFormFieldProperty.getConfig());
        } else {
            final JsonObject configJson = fieldPropertyJson.getJsonObject(configKey);

            formFieldProperty.setConfig(isOldFieldPropertyExist
                    ? Json.object(oldFormFieldProperty.getConfig()).append(configJson).toString() : configJson.toString());
        }

        if (fieldPropertyJson.containsKey(labelKey)) {
            formFieldProperty.setLabel(fieldPropertyJson.getString(labelKey));
        } else if (isOldFieldPropertyExist) {
            formFieldProperty.setLabel(oldFormFieldProperty.getLabel());
        }

        if (formFieldProperty instanceof final FormField formField) {

            if (fieldPropertyJson.containsKey(fieldIdKey)) {
                formField.setFieldId((Long) fieldPropertyJson.get(fieldIdKey));
            }

            if (isOldFieldPropertyExist) {

                if (Objects.isNull(formField.getFieldId())) {
                    formField.setFieldId(((FormField) oldFormFieldProperty).getFieldId());
                }
            }
        } else if (formFieldProperty instanceof final FormElement formElement) {

            if (fieldPropertyJson.containsKey(elementNameKey)) {
                formElement.setName(fieldPropertyJson.getString(elementNameKey));
            } else if (isOldFieldPropertyExist) {
                formElement.setName(((FormElement) oldFormFieldProperty).getName());
            }

            if (FormElement.Category.IMAGE == formElement.getCategory()) {
                final String attachmentIdKey = WebFormFields.ATTACHMENT_ID.getName();
                final String alignmentIdKey = WebFormFields.ALIGN.getName();
                final String sizeTypeIdKey = WebFormFields.SIZE_TYPE_ID.getName();
                final JsonObject configJson = formElement.getConfigAsJson();

                if (configJson.containsKey(attachmentIdKey)) {
                    configJson.put(attachmentIdKey, configJson.getLong(attachmentIdKey));
                }

                final int alignmentId = configJson.containsKey(alignmentIdKey) ? configJson.getInt(alignmentIdKey) : Alignment.LEFT.getId();
                final int sizeTypeId = configJson.containsKey(sizeTypeIdKey) ? configJson.getInt(sizeTypeIdKey) : SizeType.AUTOMATICALLY_ADJUST.getId();

                configJson.put(alignmentIdKey, Alignment.getAlignment(alignmentId).getName().toLowerCase());
                configJson.put(sizeTypeIdKey, sizeTypeId);
                formElement.setConfig(configJson.toString());
            }
        }
        webForm.addFormFieldProperty(formFieldProperty);
    }

    /**
     * {@inheritDoc}
     *
     * @param webForm Refers the {@link WebForm}.
     * @return The {@link JsonObject} which holds the form data.
     */
    @Override
    public JsonObject convert(final WebForm webForm) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(WebFormFields.ID.getName(), webForm.id());
        jsonObject.put(WebFormFields.ORG_ID.getName(), webForm.orgId());
        jsonObject.put(WebFormFields.NAME.getName(), webForm.getName());
        jsonObject.put(WebFormFields.IS_ACTIVE.getName(), webForm.isActive());
        jsonObject.put(WebFormFields.VISITS.getName(), webForm.getVisits());
        jsonObject.put(WebFormFields.SUBMISSIONS.getName(), webForm.getSubmissions());
        jsonObject.put(WebFormFields.CONVERSIONS.getName(), webForm.getConversions());
        jsonObject.put(WebFormFields.CREATED_TIME.getName(), webForm.getCreatedTime());

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Which refers the {@link ConverterConfig}.
     * @param webForm         Which refers the {@link WebForm}.
     * @return The {@link JsonObject} which holds the webForm details.
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final WebForm webForm) {
        final JsonObject webFormJson = Json.object();
        final Long id = webForm.id();
        final Long orgId = webForm.orgId();
        final Long ownerId = webForm.getOwnerId();
        final I18nResolver i18nResolver = i18nAccess.getResolver(converterConfig.locale());

        webFormJson.put(WebFormFields.ID.getName(), id);
        webFormJson.put(WebFormFields.ORG_ID.getName(), orgId);
        webFormJson.put(WebFormFields.FORM_NAME.getName(), webForm.getName());
        webFormJson.put(WebFormFields.DESCRIPTION.getName(), webForm.getDescription());
        webFormJson.put(WebFormFields.SUBMIT_BUTTON_NAME.getName(), webForm.getSubmitButtonName());
        webFormJson.put(WebFormFields.URL.getName(), webForm.getUrl());
        webFormJson.put(WebFormFields.IFRAME_URL.getName(), urlGenerator.getIframeUrl(orgId, ownerId, id));
        webFormJson.put(WebFormFields.LANGUAGE_ID.getName(), webForm.getLanguage().getId());
        webFormJson.put(WebFormFields.TEMPLATE_ID.getName(), webForm.getTemplateId());
        webFormJson.put(WebFormFields.ATTACHMENT_ID.getName(), webForm.getAttachmentId());
        webFormJson.put(WebFormFields.IS_ACTIVE.getName(), webForm.isActive());
        webFormJson.put(WebFormFields.OWNER_ID.getName(), webForm.getOwnerId());
        webFormJson.put(WebFormStyleFields.STYLE.getName(), convertStyleToJson(webForm));
        webFormJson.put(WebFormFields.ON_SUBMIT.getName(), convertSubmitActionToJson(i18nResolver, webForm.getSubmitAction()));
        webFormJson.put(WebFormFields.FORM_PROPERTIES.getName(), convertFieldPropertiesToJson(webForm, i18nResolver));

        return webFormJson;
    }

    /**
     * Converts the field properties details into the {@link JsonArray}.
     *
     * @param webForm      Refers the {@link WebForm}.
     * @param i18nResolver Represents the {@link I18nResolver}
     * @return The {@link JsonArray} which contains the field properties details.
     */
    private JsonArray convertFieldPropertiesToJson(final WebForm webForm, final I18nResolver i18nResolver) {
        final JsonArray jsonArray = Json.array();
        final Collection<FormFieldProperty> formFieldProperties = webForm.getFormFieldProperties()
                .stream().sorted(Comparator.comparing(FormFieldProperty::order)).toList();

        if (!formFieldProperties.isEmpty()) {
            final Map<Long, FieldInfo> fields = fieldService.getFieldsBySource(webForm.orgId(), Source.CONTACT.getName()).stream()
                    .collect(Collectors.toMap(FieldInfo::id, Function.identity()));

            for (final FormFieldProperty formFieldProperty : formFieldProperties) {
                final JsonObject jsonObject = Json.object();
                final JsonObject configJson = formFieldProperty.getConfigAsJson();

                jsonObject.put(WebFormFields.ID.getName(), formFieldProperty.id());
                jsonObject.put(WebFormFields.LABEL.getName(), formFieldProperty.getLabel());

                if (formFieldProperty instanceof final FormElement formElement && FormElement.Category.IMAGE == formElement.getCategory()) {
                    final String alignmentKey = WebFormFields.ALIGN.getName();

                    if (configJson.containsKey(alignmentKey)) {
                        configJson.put(alignmentKey, Alignment.valueOf(configJson.getString(alignmentKey).toUpperCase()).getId());
                    }
                }

                jsonObject.put(WebFormFields.CONFIG.getName(), formFieldProperty.getConfigAsJson());
                jsonObject.put(WebFormFields.TYPE.getName(), formFieldProperty.getType().name());

                if (formFieldProperty instanceof final FormField formField) {
                    final Long fieldId = formField.getFieldId();

                    if (fields.containsKey(fieldId)) {
                        final JsonObject fieldTypeJson = Json.object();
                        final StoreType storeType = fields.get(fieldId).storeType();
                        final FieldType fieldType = fieldTypeService.getFieldTypeByStoreType(
                                switch (storeType) {
                                    case COMPANY_ONBOARD -> StoreType.SINGLE_STRING;
                                    case EMAILS -> {
                                        jsonObject.put(WebFormFields.IS_SYSTEM_FIELD.getName(), true);
                                        yield StoreType.EMAIL;
                                    }
                                    case PHONES -> {
                                        jsonObject.put(WebFormFields.IS_SYSTEM_FIELD.getName(), true);
                                        yield StoreType.PHONE;
                                    }
                                    default -> storeType;
                                }
                        );

                        fieldTypeJson.put(WebFormFields.NAME.getName(), fieldType.name());
                        fieldTypeJson.put(WebFormFields.DISPLAY_NAME.getName(), i18nResolver.get(fieldType.displayName()));
                        jsonObject.put(WebFormFields.FIELD_TYPE.getName(), fieldTypeJson);
                        jsonObject.put(WebFormFields.FIELD_ID.getName(), fieldId);
                    } else {
                        continue;
                    }
                } else if (formFieldProperty instanceof final FormElement formElement) {
                    jsonObject.put(WebFormFields.NAME.getName(), formElement.getName());
                }
                jsonArray.put(jsonObject);
            }
        }
        return jsonArray;
    }

    /**
     * Constructs the {@link SubmitAction} details into the {@link JsonObject}.
     *
     * @param i18nResolver Which refers the {@link I18nResolver}.
     * @param submitAction Refers the {@link SubmitAction}.
     * @return The {@link JsonObject} which holds the submit action details.
     */
    public JsonObject convertSubmitActionToJson(final I18nResolver i18nResolver, final SubmitAction submitAction) {
        final JsonObject submitActionJson = Json.object();

        submitActionJson.put(WebFormFields.THANK_YOU_MESSAGE.getName(), convertThankYouJson(i18nResolver, submitAction));
        submitActionJson.put(WebFormFields.REDIRECT_URL.getName(), convertRedirectJson(submitAction));
        submitActionJson.put(WebFormFields.ID.getName(), submitAction.id());
        submitActionJson.put(WebFormFields.IS_CONTACT_CREATION_ENABLED.getName(), submitAction.isContactCreationEnabled());
        submitActionJson.put(WebFormFields.IS_DEAL_CREATION_WITH_CONTACT_ENABLED.getName(), submitAction.isDealCreationWithContactEnabled());
        submitActionJson.put(WebFormFields.EMAIL_NOTIFICATION.getName(), convertEmailDataToJson(submitAction));

        return submitActionJson;
    }

    /**
     * Constructs the redirect details into the {@link JsonObject}.
     *
     * @param submitAction Refers the {@link SubmitAction}.
     * @return The {@link JsonObject} which holds the redirect details.
     */
    private JsonObject convertRedirectJson(final SubmitAction submitAction) {
        final JsonObject redirectJson = Json.object();

        redirectJson.put(WebFormFields.IS_ENABLED.getName(), submitAction.isRedirectEnabled());
        redirectJson.put(WebFormFields.URL.getName(), submitAction.getRedirectUrl());

        return redirectJson;
    }

    /**
     * Constructs the thank-you message details into the {@link JsonObject}.
     *
     * @param i18nResolver Which refers the {@link I18nResolver} to resolve the i18n string.
     * @param submitAction Refers the {@link SubmitAction}.
     * @return The {@link JsonObject} which holds the thank-you message details.
     */
    private JsonObject convertThankYouJson(final I18nResolver i18nResolver, final SubmitAction submitAction) {
        final I18nKey i18nMessage = submitAction.getI18nMessage();
        final JsonObject thankYouMessageJson = Json.object();

        thankYouMessageJson.put(WebFormFields.IS_ENABLED.getName(), submitAction.isThankYouMessageEnabled());

        if (Objects.nonNull(i18nMessage)) {
            thankYouMessageJson.put(WebFormFields.MESSAGE.getName(), i18nResolver.get(i18nMessage));
        }

        return thankYouMessageJson;
    }

    /**
     * Converts the webForm email notification details into the {@link JsonObject}.
     *
     * @param submitAction Refers the {@link SubmitAction}.
     * @return The {@link JsonObject} which contains the webForm email notification details.
     */
    public JsonObject convertEmailDataToJson(final SubmitAction submitAction) {
        final JsonObject jsonObject = Json.object();
        final JsonArray jsonArray = Json.array();
        final Collection<RecipientEmail> recipientEmails = submitAction.getRecipientEmails();

        if (Objects.nonNull(recipientEmails) && !recipientEmails.isEmpty()) {

            for (final RecipientEmail recipientEmail : recipientEmails) {
                final JsonObject recipientJson = Json.object();

                recipientJson.put(WebFormFields.ID.getName(), recipientEmail.id());
                recipientJson.put(WebFormFields.VALUE.getName(), recipientEmail.getValue());

                jsonArray.put(recipientJson);
            }
        }
        jsonObject.put(WebFormFields.IS_ENABLED.getName(), submitAction.isEmailNotificationEnabled());
        jsonObject.put(WebFormFields.RECIPIENTS.getName(), jsonArray);

        return jsonObject;
    }

    /**
     * Converts the {@link WebForm} styles into the {@link JsonObject}.
     *
     * @param webForm Refers the {@link WebForm}.
     * @return The {@link JsonObject} which contains the webForm style data.
     */
    private JsonObject convertStyleToJson(final WebForm webForm) {
        final JsonObject styleJson = Json.object();

        styleJson.put(WebFormStyleFields.FORM_STYLE.getName(), convertFormStyleToJson(webForm.getFormStyle()));
        styleJson.put(WebFormStyleFields.FIELD_STYLE.getName(), convertFieldStyleToJson(webForm.getFieldStyle()));
        styleJson.put(WebFormStyleFields.SUBMIT_BUTTON_STYLE.getName(), convertSubmitButtonStyleToJson(webForm.getSubmitButtonStyle()));

        return styleJson;
    }

    /**
     * Constructs the {@link FormStyle} into {@link JsonObject}.
     *
     * @param formStyle Refers the {@link FormStyle}.
     * @return The {@link JsonObject} which contains the form style details.
     */
    public JsonObject convertFormStyleToJson(final FormStyle formStyle) {
        final JsonObject formStyleJson = Json.object();
        final JsonObject headerJson = Json.object();
        final JsonObject subHeaderJson = Json.object();
        final JsonObject descriptionJson = Json.object();
        final JsonObject textJson = Json.object();
        final String textColorKey = WebFormStyleFields.TEXT_COLOR.getName();
        final String fontSizeKey = WebFormStyleFields.FONT_SIZE.getName();

        headerJson.put(textColorKey, formStyle.getHeaderTextColor());
        headerJson.put(fontSizeKey, formStyle.getHeaderFontSize());
        subHeaderJson.put(textColorKey, formStyle.getSubHeaderTextColor());
        subHeaderJson.put(fontSizeKey, formStyle.getSubHeaderFontSize());

        descriptionJson.put(textColorKey, formStyle.getDescriptionTextColor());
        descriptionJson.put(fontSizeKey, formStyle.getDescriptionFontSize());
        textJson.put(textColorKey, formStyle.getTextColor());
        textJson.put(fontSizeKey, formStyle.getTextFontSize());

        formStyleJson.put(WebFormStyleFields.ID.getName(), formStyle.id());
        formStyleJson.put(WebFormStyleFields.BACKGROUND_COLOR.getName(), formStyle.getBackgroundColor());
        formStyleJson.put(WebFormStyleFields.ALIGNMENT_ID.getName(), formStyle.getAlignment().getId());
        formStyleJson.put(WebFormStyleFields.FONT_ID.getName(), formStyle.getFont().getId());
        formStyleJson.put(WebFormStyleFields.HEADER.getName(), headerJson);
        formStyleJson.put(WebFormStyleFields.SUB_HEADER.getName(), subHeaderJson);
        formStyleJson.put(WebFormStyleFields.DESCRIPTION.getName(), descriptionJson);
        formStyleJson.put(WebFormStyleFields.TEXT.getName(), textJson);

        return formStyleJson;
    }

    /**
     * Constructs the {@link FieldStyle} into {@link JsonObject}.
     *
     * @param fieldStyle Refers the {@link FieldStyle}.
     * @return The {@link JsonObject} which contains the field style details.
     */
    public JsonObject convertFieldStyleToJson(final FieldStyle fieldStyle) {
        final JsonObject fieldStyleJson = Json.object();
        final JsonObject labelJson = Json.object();
        final JsonObject inputJson = Json.object();
        final String fontSizeKey = WebFormStyleFields.FONT_SIZE.getName();

        labelJson.put(WebFormStyleFields.COLOR.getName(), fieldStyle.getLabelColor());
        labelJson.put(fontSizeKey, fieldStyle.getLabelFontSize());
        inputJson.put(WebFormStyleFields.TEXT_COLOR.getName(), fieldStyle.getInputTextColor());
        inputJson.put(WebFormStyleFields.BACKGROUND_COLOR.getName(), fieldStyle.getInputBackgroundColor());
        inputJson.put(fontSizeKey, fieldStyle.getInputFontSize());

        fieldStyleJson.put(WebFormStyleFields.ID.getName(), fieldStyle.id());
        fieldStyleJson.put(WebFormStyleFields.LABEL.getName(), labelJson);
        fieldStyleJson.put(WebFormStyleFields.INPUT.getName(), inputJson);

        return fieldStyleJson;
    }

    /**
     * Constructs the {@link SubmitButtonStyle} into {@link JsonObject}.
     *
     * @param submitButtonStyle Refers the {@link SubmitButtonStyle}.
     * @return The {@link JsonObject} which contains the submit button style details.
     */
    public JsonObject convertSubmitButtonStyleToJson(final SubmitButtonStyle submitButtonStyle) {
        final JsonObject submitButtonStyleJson = Json.object();

        submitButtonStyleJson.put(WebFormStyleFields.ID.getName(), submitButtonStyle.id());
        submitButtonStyleJson.put(WebFormStyleFields.BACKGROUND_COLOR.getName(), submitButtonStyle.getBackgroundColor());
        submitButtonStyleJson.put(WebFormStyleFields.TEXT_COLOR.getName(), submitButtonStyle.getTextColor());
        submitButtonStyleJson.put(WebFormStyleFields.WIDTH.getName(), submitButtonStyle.getWidth());
        submitButtonStyleJson.put(WebFormStyleFields.ALIGNMENT_ID.getName(), submitButtonStyle.getAlignment().getId());

        return submitButtonStyleJson;
    }

}
