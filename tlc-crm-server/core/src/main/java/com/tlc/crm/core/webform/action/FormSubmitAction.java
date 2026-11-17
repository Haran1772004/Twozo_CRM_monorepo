package com.tlc.crm.core.webform.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.crm.common.action.CSRFProtectedAction;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.core.internal.webform.status.FormErrorCodes;

import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.crm.core.webform.api.manager.FormSubmitActionManager;
import com.tlc.crm.webform.api.manager.WebFormManager;
import com.tlc.crm.webform.api.model.FormFieldProperty;
import com.tlc.crm.webform.api.model.SubmitAction;
import com.tlc.crm.webform.api.model.WebForm;
import com.tlc.crm.webform.field.WebFormFields;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.QueueService;
import com.tlc.queue.service.provider.QueueServiceProvider;

import com.tlc.web.WebAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages the webForm {@link SubmitAction} related web-actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/webform/submit")
public class FormSubmitAction extends CSRFProtectedAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormSubmitAction.class);

    private final WebFormManager webFormManager;
    private final QueueService queueService;
    private final FormSubmitActionManager formSubmitActionManager;
    private final RandomIdGenerator randomIdGenerator;

    public FormSubmitAction() {
        this.webFormManager = WebFormManager.getInstance();
        this.queueService = QueueServiceProvider.get();
        this.formSubmitActionManager = FormSubmitActionManager.getInstance();
        this.randomIdGenerator = RandomIdGenerator.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Refers the {@link CrmRequest}.
     * @param response The {@link CrmResponse} to be sent.
     */
    @Override
    public void secureProcess(final CrmRequest request, final CrmResponse response) {
        LOGGER.info("Webform: secureProcess reached for submit request.");
        final JsonObject data = request.getRequestJson().getJsonObject(WebFormFields.DATA.getName());
        final long id = data.getLong(WebFormFields.ID.getName());
        final Optional<WebForm> optionalWebForm = webFormManager.get(id);

        if (optionalWebForm.isPresent()) {
            final WebForm webForm = optionalWebForm.get();

            LOGGER.info("Webform: ID={}, isActive={}, ownerId={}, orgId={}", webForm.id(), webForm.isActive(), webForm.getOwnerId(), webForm.orgId());

            if (webForm.isActive()) {
                final Collection<String> errors = validateSubmitActionData(webForm, data);

                if (errors.isEmpty()) {
                    final long orgId = webForm.orgId();
                    final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s:%s", orgId, webForm.id(), randomIdGenerator.generateUniqueASCII(5)).getBytes());

                    data.put(WebFormFields.OWNER_ID.getName(), webForm.getOwnerId());
                    data.put(WebFormFields.ORG_ID.getName(), orgId);
                    data.put(WebFormFields.REF_ID.getName(), requestId);

                    LOGGER.info("Webform: Webform ID: {}, OrgID: {}, OwnerID: {}, RequestID: {}", webForm.id(), orgId, webForm.getOwnerId(), requestId);
                    final SubscriptionTopic webFormSubmitEventTopic = CoreQueueTopics.getWebFormSubmitEventTopic(QueueModel.Type.KAFKA);
                    final KafkaProducerConfig webFormSubmitEventProducerConfig = new KafkaProducerConfig(webFormSubmitEventTopic);

                    try (final Producer<byte[]> webFormSubmitEventProducer = queueService.createProducer(webFormSubmitEventProducerConfig)) {
                        formSubmitActionManager.saveFormSubmission(orgId, webForm, requestId, data.toString());
                        webFormSubmitEventProducer.send(new StreamMessage<>(requestId, webFormSubmitEventTopic, data.getBytes()));
                    } catch (Exception exception) {
                        LOGGER.error("Webform: Error while saving/sending the webform submit event. Webform ID: {}, OrgID: {}, RequestID: {}", id, orgId, requestId, exception);
                        exception.printStackTrace();
                    }
                    response.put(WebFormFields.STATUS.getName(), true);
                } else {
                    response.put(WebFormFields.ERRORS.getName(), errors);
                    response.setErrorCode(ErrorCode.get(FormErrorCodes.FORM_REQUIRED_FIELD_DATA_IS_MISSING, "i18n.webform.required.field.data.missing"));
                }
            } else {
                response.setErrorCode(ErrorCode.get(FormErrorCodes.WEB_FORM_IS_INACTIVE, "i18n.webform.status.inactive"));
            }
        } else {
            response.setErrorCode(ErrorCode.get(FormErrorCodes.WEB_FORM_NOT_FOUND, "i18n.webform.not.found"));
        }
        response.send();
    }

    /**
     * Validates the submit action data.
     *
     * @param webForm Refers the {@link WebForm} details.
     * @param data    Refers the request json.
     * @return The {@link Collection} of errors if the data is missing while validating.
     */
    private Collection<String> validateSubmitActionData(final WebForm webForm, final JsonObject data) {
        final Map<String, String> formFieldsMap = webForm.getFormFieldProperties().stream()
                .filter(fieldProperty -> Objects.equals(FormFieldProperty.Type.FIELD, fieldProperty.getType())
                        && Objects.equals(true, fieldProperty.getConfigAsJson().getBoolean(WebFormFields.IS_REQUIRED.getName())))
                .collect(Collectors.toMap(fieldProperty -> fieldProperty.id().toString(), FormFieldProperty::getLabel));
        final Set<String> requestJsonKeys = data.keySet();
        final Collection<String> errors = new ArrayList<>();

        for (final String formFieldIdAsString : formFieldsMap.keySet()) {

            if (!requestJsonKeys.contains(formFieldIdAsString)) {
                errors.add(String.format("Webform: Required field %s is missing", formFieldsMap.get(formFieldIdAsString)));
            } else if (Objects.isNull(data.opt(formFieldIdAsString))) {
                errors.add(String.format("Webform: Required field %s data should not be null", formFieldsMap.get(formFieldIdAsString)));
            }
        }

        return errors;
    }
}