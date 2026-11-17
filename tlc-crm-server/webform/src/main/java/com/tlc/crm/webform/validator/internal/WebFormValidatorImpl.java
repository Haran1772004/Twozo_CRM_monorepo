package com.tlc.crm.webform.validator.internal;

import com.tlc.commons.Counter;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.validator.ValidatorContext;
import com.tlc.crm.webform.api.model.FormField;
import com.tlc.crm.webform.api.model.FormFieldProperty;
import com.tlc.crm.webform.api.model.SubmitAction;
import com.tlc.crm.webform.api.model.WebForm;
import com.tlc.crm.webform.api.model.FormElement;
import com.tlc.crm.webform.field.WebFormFields;
import com.tlc.crm.webform.validator.annotation.WebFormValidator;

import com.tlc.file.client.service.AttachmentService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Validates the {@link WebForm} entity and returns appropriate message.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class WebFormValidatorImpl implements ConstraintValidator<WebFormValidator, WebForm> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebFormValidatorImpl.class);
    private static final List<String> REQUIRED_IMAGE_CONFIG_FIELDS = List.of(WebFormFields.ATTACHMENT_ID.getName(), WebFormFields.SIZE.getName(), WebFormFields.EXTENSION.getName());
    private final FieldService fieldService;
    private final AttachmentService attachmentService;

    public WebFormValidatorImpl() {
        fieldService = FieldService.getInstance();
        attachmentService = AttachmentService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param webForm {@link WebForm} to validate.
     * @param context Context in which the constraint is evaluated.
     */
    @Override
    public boolean isValid(final WebForm webForm, final ConstraintValidatorContext context) {
        final ValidatorContext validatorContext = new ValidatorContext(context);
        final Collection<FormFieldProperty> formFieldProperties = webForm.getFormFieldProperties();

        if (Objects.nonNull(formFieldProperties)) {
            final Collection<FormField> formFields = formFieldProperties.stream()
                    .filter(formField -> Objects.equals(FormFieldProperty.Type.FIELD, formField.getType()))
                    .map(formField -> (FormField) formField).toList();

            if (formFields.isEmpty()) {
                addConstraintViolationMessage(validatorContext, "i18n.webForm.validation.fields.empty");
            } else {
                final String emailsKey = WebFormFields.EMAILS.getName();
                final String phonesKey = WebFormFields.PHONES.getName();
                final Map<String, FieldInfo> contactFields = fieldService
                        .groupDefaultFieldsByAliasName(Source.CONTACT.getName(), List.of(emailsKey, phonesKey));
                final Set<Long> formFieldIds = formFields.stream().map(FormField::getFieldId)
                        .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

                if (!formFieldIds.contains(contactFields.get(emailsKey).id())
                        && !formFieldIds.contains(contactFields.get(phonesKey).id())) {
                    addConstraintViolationMessage(validatorContext, "i18n.webForm.validation.mandatory.fields.invalid");
                } else {
                    final Long orgId = webForm.orgId();
                    final FormField requiredField = formFields.stream().filter(field -> {
                        final StoreType storeType = fieldService.getField(orgId, field.getFieldId()).storeType();

                        return (Objects.equals(StoreType.EMAILS, storeType) || Objects.equals(StoreType.PHONES, storeType))
                                && Objects.equals(true, field.getConfigAsJson().getBoolean(WebFormFields.IS_REQUIRED.getName()));
                    }).findAny().orElse(null);

                    if (Objects.isNull(requiredField)) {
                        addConstraintViolationMessage(validatorContext, "i18n.webForm.validation.fields.invalid.required.key");
                    }
                }
            }
        }
        final SubmitAction submitAction = webForm.getSubmitAction();

        if (Objects.nonNull(submitAction)) {
            final boolean emailNotificationEnabled = submitAction.isEmailNotificationEnabled();
            final boolean isThankYouMessageEnabled = submitAction.isThankYouMessageEnabled();
            final boolean isRedirectEnabled = submitAction.isRedirectEnabled();
            final String thankYouMessage = submitAction.getThankYouMessage();
            final String redirectUrl = submitAction.getRedirectUrl();

            if (Objects.equals(false, isThankYouMessageEnabled) && Objects.equals(false, isRedirectEnabled)
                    && Objects.equals(false, emailNotificationEnabled)
                    && Objects.equals(false, submitAction.isContactCreationEnabled())
                    && Objects.equals(false, submitAction.isDealCreationWithContactEnabled())) {
                addConstraintViolationMessage(validatorContext, "i18n.webForm.validation.submitAction.invalid");
            }

            if (isThankYouMessageEnabled && (Objects.isNull(thankYouMessage) || thankYouMessage.isBlank())) {
                addConstraintViolationMessage(validatorContext, "i18n.webForm.validation.submitAction.thankYouMessage.invalid");
            }

            if (isRedirectEnabled && (Objects.isNull(redirectUrl) || redirectUrl.isBlank())) {
                addConstraintViolationMessage(validatorContext, "i18n.webForm.validation.submitAction.redirect.url.invalid");
            }

            if (emailNotificationEnabled && Objects.isNull(submitAction.getRecipientEmails())) {
                addConstraintViolationMessage(validatorContext, "i18n.webForm.validation.submitAction.email.notification.invalid");
            }
        }

        validateImageElements(webForm, validatorContext);

        if (validatorContext.isErrorsFound()) {
            validatorContext.getContext().disableDefaultConstraintViolation();
        }

        return !validatorContext.isErrorsFound();
    }

    /**
     * Adds the constraint violation message to the validation context.
     *
     * @param validatorContext Refers the validator context.
     * @param message          Violation message.
     */
    private void addConstraintViolationMessage(final ValidatorContext validatorContext, final String message) {
        validatorContext.setErrorsFound(true);
        validatorContext.getContext().buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    /**
     * <p>
     * Validates Image elements in a webform. Ensures at most one image element exists,
     * that it has a non-blank label, and that its config contains all required metadata
     * (attachmentId, height, width, size, extension). Attachment existence is verified
     * in a single batch call to avoid N+1 queries.
     * </p>
     *
     * @param webForm          Refers the {@link WebForm} to validate.
     * @param validatorContext Refers the validator context.
     */
    private void validateImageElements(final WebForm webForm, final ValidatorContext validatorContext) {
        final Set<Long> attachmentIds = new HashSet<>();
        final Counter imageCounter = new Counter();
        boolean skipAttachmentCheck = false;
        final Collection<FormFieldProperty> formFieldProperties = webForm.getFormFieldProperties();

        if (Objects.nonNull(formFieldProperties)) {
            for (final FormFieldProperty formFieldProperty : formFieldProperties) {
                if (!(formFieldProperty instanceof final FormElement formElement)
                        || FormElement.Category.IMAGE != formElement.getCategory()) {
                    continue;
                }

                imageCounter.increment();

                if (NumericalConstant.ONE < imageCounter.getCount()) {
                    LOGGER.debug("WebForm: Multiple image elements found in webform with ID: {}, OrgID: {}", webForm.id(), webForm.orgId());
                    addConstraintViolationMessage(validatorContext, "i18n.webForm.image.multiple.not.allowed");
                    skipAttachmentCheck = true;
                    break;
                }

                final String label = formElement.getLabel();

                if (Objects.isNull(label) || label.isBlank()) {
                    LOGGER.debug("WebForm: Invalid image element label found in webform with ID: {}, OrgID: {}", webForm.id(), webForm.orgId());
                    addConstraintViolationMessage(validatorContext, "i18n.webForm.image.invalid.label");
                    skipAttachmentCheck = true;
                    break;
                }

                final JsonObject config = formElement.getConfigAsJson();
                final List<String> missingFields = REQUIRED_IMAGE_CONFIG_FIELDS.stream().filter(
                        field -> !config.containsKey(field)).toList();

                if (!missingFields.isEmpty()) {
                    LOGGER.debug("WebForm: Invalid image element config found in webform with ID: {}, OrgID: {}, Config: {}", webForm.id(), webForm.orgId(), config);
                    addConstraintViolationMessage(validatorContext, "i18n.webForm.image.invalid.config");
                    skipAttachmentCheck = true;
                    break;
                }

                attachmentIds.add(config.getLong(WebFormFields.ATTACHMENT_ID.getName()));
            }
        }

        if (!skipAttachmentCheck && !attachmentIds.isEmpty() && !attachmentService.exists(attachmentIds)) {
            LOGGER.debug("WebForm: Invalid image attachment found in webform with ID: {}, OrgID: {}, Attachment ids: {}", webForm.id(), webForm.orgId(), attachmentIds);
            addConstraintViolationMessage(validatorContext, "i18n.webForm.image.attachment.not.found");
        }
    }

}
