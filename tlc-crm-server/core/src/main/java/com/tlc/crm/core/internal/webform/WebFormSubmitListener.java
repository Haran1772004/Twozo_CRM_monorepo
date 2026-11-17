package com.tlc.crm.core.internal.webform;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.email.service.NoReplyMailService;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.models.CommonFields;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.dropdown.contact.ContactSourceStaticOption;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.webform.api.manager.FormSubmitActionManager;
import com.tlc.crm.core.webform.api.model.converter.FormSubmitActionConverter;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.dropdown.DealSourceStaticOption;
import com.tlc.crm.entity.models.Entity;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.webform.api.manager.WebFormManager;
import com.tlc.crm.webform.api.model.RecipientEmail;
import com.tlc.crm.webform.api.model.SubmitAction;
import com.tlc.crm.webform.api.model.WebForm;
import com.tlc.crm.webform.api.model.WebFormEmailRequest;
import com.tlc.crm.webform.field.WebFormFields;

import com.tlc.email.client.template.mail.EmailNotificationBuilder;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.Email;
import com.tlc.notification.service.template.EmailTemplateBuildRequest;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.web.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

/**
 * <p>
 * A consumer that consumes the webform submit requests and triggers the respective web form submit actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class WebFormSubmitListener implements StreamListener<byte[]>, SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebFormSubmitListener.class);

    private final FormSubmitActionConverter formSubmitActionConverter;
    private final FormSubmitActionManager formSubmitActionManager;
    private final ContactConverter contactConverter;
    private final DealConverter dealConverter;
    private final WebFormManager webFormManager;
    private final FieldService fieldService;
    private final OptionService optionService;
    private final EmailNotificationBuilder emailNotificationBuilder;
    private final NoReplyMailService noReplyMailService;

    public WebFormSubmitListener() {
        this.formSubmitActionConverter = FormSubmitActionConverter.getInstance();
        this.formSubmitActionManager = FormSubmitActionManager.getInstance();
        this.contactConverter = ContactConverter.getInstance();
        this.dealConverter = DealConverter.getInstance();
        this.webFormManager = WebFormManager.getInstance();
        this.fieldService = FieldService.getInstance();
        this.optionService = OptionService.getInstance();
        emailNotificationBuilder = EmailNotificationBuilder.getInstance();
        this.noReplyMailService = NoReplyMailService.get();
    }

    /**
     * <p>
     * Enables to process the {@link StreamMessage} data.
     * </p>
     *
     * @param streamMessage Represents {@link StreamMessage}.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final JsonObject requestParameters = Json.object(streamMessage.data());
        final long orgId = requestParameters.getLong(WebFormFields.ORG_ID.getName());
        final long ownerId = requestParameters.getLong(WebFormFields.OWNER_ID.getName());
        final String refId = requestParameters.getString(WebFormFields.REF_ID.getName());

        LOGGER.info("WebForm: Received webform submit message. orgId={}, ownerId={}, refId={}", orgId, ownerId, refId);

        try {
            secureProcess(orgId, ownerId, () -> {
                final WebForm webForm = webFormManager.partialGet(orgId, requestParameters.getLong(WebFormFields.ID.getName()));

                if (Objects.isNull(webForm)) {
                    LOGGER.error("WebForm: webForm is null for orgId={}, webFormId={}, refId={} - aborting", orgId, requestParameters.getLong(WebFormFields.ID.getName()), refId);
                    return;
                }

                final SubmitAction submitAction = webForm.getSubmitAction();
                final boolean isDealCreationEnabled = submitAction.isDealCreationWithContactEnabled();
                final Entity entity;

                if (submitAction.isContactCreationEnabled() || isDealCreationEnabled) {
                    Entity builtEntity = null;

                    try {
                        final JsonObject jsonObject = formSubmitActionConverter.format(orgId, webForm, requestParameters);
                        final ConverterConfig converterConfig = new ConverterConfig(orgId, ownerId, null, false, requestParameters);
                        final String contactSourceKey = ContactFields.CONTACT_SOURCE;
                        final FieldInfo contactSourceFieldInfo = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, contactSourceKey).get(contactSourceKey);

                        if (Objects.isNull(contactSourceFieldInfo)) {
                            LOGGER.error("WebForm: contactSourceFieldInfo is null for orgId={}, contactSourceKey={} - field alias not resolved", orgId, contactSourceKey);
                        }
                        final String contactSourceFieldIdAsString = contactSourceFieldInfo.idAsString();
                        final Long contactWebFormOptionId = optionService.getOption(orgId, contactSourceFieldInfo, ContactSourceStaticOption.WEB_FORM.getName()).id();

                        if (isDealCreationEnabled) {
                            final String dealSourceKey = DealFields.DEAL_SOURCE;
                            final FieldInfo dealSourceFieldInfo = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, dealSourceKey).get(dealSourceKey);
                            final String primaryContactKey = DealFields.PRIMARY_CONTACT;
                            final FieldInfo primaryContactFieldInfo = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, primaryContactKey).get(primaryContactKey);

                            if (Objects.isNull(primaryContactFieldInfo)) {
                                LOGGER.error("WebForm: primaryContactFieldInfo is null for orgId={}, primaryContactKey={}", orgId, primaryContactKey);
                            }
                            final JsonObject primaryContactJson = jsonObject.getJsonObject(primaryContactFieldInfo.idAsString());

                            if (Objects.isNull(primaryContactJson)) {
                                LOGGER.error("WebForm: primaryContactJson is null in payload for orgId={}, webFormId={} - primary contact data missing", orgId, webForm.id());
                            }
                            jsonObject.put(dealSourceFieldInfo.idAsString(), optionService.getOption(orgId, dealSourceFieldInfo, DealSourceStaticOption.WEB_FORM.getName()).id());
                            primaryContactJson.put(contactSourceFieldIdAsString, contactWebFormOptionId);
                            builtEntity = dealConverter.convert(converterConfig, jsonObject);
                        } else {
                            jsonObject.put(contactSourceFieldIdAsString, contactWebFormOptionId);
                            builtEntity = contactConverter.convert(converterConfig, jsonObject);
                        }
                    } catch (Exception exception) {
                        LOGGER.error("WebForm: failed to build entity from webform submission. orgId={}, ownerId={}, webFormId={}, refId={}", orgId, ownerId, webForm.id(), refId);
                        exception.printStackTrace();
                        throw exception;
                    }
                    entity = builtEntity;
                } else {
                    entity = null;
                }

                final boolean created = formSubmitActionManager.createEntity(webForm, entity, refId);

                if (created) {
                    LOGGER.info("WebForm: createEntity returned true. webFormId={}, refId={}, entityId={}", webForm.id(), refId, Objects.nonNull(entity) ? entity.id() : "null");

                    if (submitAction.isEmailNotificationEnabled()) {
                        try {
                            final WebFormEmailRequest webFormEmailRequest = new WebFormEmailRequest();

                            webFormEmailRequest.setWebForm(webForm);
                            webFormEmailRequest.setEntity(entity);
                            webFormEmailRequest.setDomain(HttpUtil.getDomain());
                            final Optional<Email> optionalEmail = emailNotificationBuilder.build(EmailNotificationTemplate.WEB_FORM_EMAIL.getName(),
                                    new EmailTemplateBuildRequest(Map.of(CommonFields.MODEL, webFormEmailRequest)));

                            optionalEmail.ifPresentOrElse(email -> {
                                final Collection<RecipientEmail> recipientEmails = submitAction.getRecipientEmails();

                                for (final RecipientEmail recipientEmail : recipientEmails) {
                                    final EmailRecipients emailRecipients = new EmailRecipients();

                                    emailRecipients.addToRecipient(recipientEmail.getValue());
                                    email.setRecipients(List.of(emailRecipients));

                                    noReplyMailService.send(email);
                                }
                            }, () -> LOGGER.warn("WebForm: emailNotificationBuilder.build returned empty. webFormId={}", webForm.id()));
                        } catch (Exception exception) {
                            LOGGER.error("WebForm: failed to send notification email. webFormId={}, refId={}", webForm.id(), refId);
                            exception.printStackTrace();
                        }
                    }
                } else {
                    LOGGER.warn("WebForm: createEntity returned false. orgId={}, webFormId={}, refId={}", orgId, webForm.id(), refId);
                }
            });
        } catch (Exception exception) {
            LOGGER.error("WebForm: secureProcess failed. orgId={}, ownerId={}, refId={}", orgId, ownerId, refId, exception);
            exception.printStackTrace();
        }
    }
}