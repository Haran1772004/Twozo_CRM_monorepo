package com.tlc.crm.entity.attachment.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.entity.attachment.api.models.EntityAttachment;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.attachment.fields.AttachmentFields;
import com.tlc.crm.entity.attachment.api.manager.AttachmentConfigManager;

import com.tlc.file.client.converter.AttachmentConverter;
import com.tlc.file.common.models.Attachable;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.client.internal.status.AttachmentErrorCodes;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.queue.AttachmentRemovalPublisher;

import com.tlc.validator.ModelValidator;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import jakarta.validation.ConstraintViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;

import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the {@link Attachment} related web actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AttachmentAction<T extends Attachable> extends AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentAction.class);

    private final AttachmentService attachmentService;
    private final AttachmentConverter attachmentConverter;
    private final AttachmentRemovalPublisher attachmentRemovalPublisher;

    public AttachmentAction() {
        this.attachmentService = AttachmentService.getInstance();
        this.attachmentConverter = AttachmentConverter.getInstance();
        this.attachmentRemovalPublisher = AttachmentRemovalPublisher.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     The received request.
     * @param crmResponse The response to be sent.
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final Long orgId = request.orgId();
        final JsonObject data = requestJson.getJsonObject(AttachmentFields.DATA.getName());
        final String type = requestJson.getString(AttachmentFields.TYPE.getName());

        switch (type) {
            case "get" -> {
                get(orgId, data, crmResponse);
            }
            case "exists" -> {
                exists(orgId, data.getLong(AttachmentFields.ID.getName()), crmResponse);
            }
            case "delete" -> {
                delete(orgId, data, crmResponse);
            }
            default -> {
                handle(request, crmResponse);
            }
        }
    }

    /**
     * Deletes the attachment details for the given organization.
     *
     * @param orgId       Refers the organization id.
     * @param data        Refers the request json data.
     * @param crmResponse Refers the response to be sent.
     */
    private void delete(final long orgId, final JsonObject data, final CrmResponse crmResponse) {
        attachmentService.getAttachments(List.of(data.getLong(AttachmentFields.ID.getName()))).stream().findFirst().ifPresentOrElse(attachment -> {
            final EntityAttachment entityAttachment = new EntityAttachment();
            final Long id = attachment.id();

            entityAttachment.setId(id);
            entityAttachment.setOrgId(orgId);
            entityAttachment.setExtension(attachment.getExtension());
            entityAttachment.setFileName(attachment.getFileName());
            entityAttachment.setFileSize(attachment.getFileSize());
            entityAttachment.setMediaType(attachment.getMediaType());
            entityAttachment.setCreatedTime(attachment.getCreatedTime());

            delete(orgId, crmResponse, entityAttachment);
            attachmentRemovalPublisher.publish(String.valueOf(id), List.of(id));
            crmResponse.send();
        }, () ->
            sendError(crmResponse, null, ErrorCode.get(EntityErrorCodes.ATTACHMENT_DETAILS_NOT_FOUND, "i18n.entity.attachment.not.found")));
    }

    /**
     * Retrieves attachment for a specific organization and processes the response asynchronously.
     *
     * @param orgId       The ID of the organization for which data is being retrieved.
     * @param data        The JSON object containing the request data.
     * @param crmResponse The response handler that will process the result of the data retrieval.
     */
    private void get(final long orgId, final JsonObject data, final CrmResponse crmResponse) {
        attachmentService.getAttachments(List.of(data.getLong(AttachmentFields.ID.getName()))).stream().findFirst().ifPresentOrElse(attachment -> {
            crmResponse.put(AttachmentFields.DATA.getName(), attachmentConverter.convert(attachment));
            crmResponse.send();
        }, () ->
            sendError(crmResponse, null, ErrorCode.get(EntityErrorCodes.ATTACHMENT_DETAILS_NOT_FOUND, "i18n.entity.attachment.not.found")));
    }

    /**
     * <p>
     * Loads the {@link Attachment} into search container.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param models Collection of entities.
     * @param action Represents {@link Action}.
     */
    protected <R extends Attachable> void loadAttachmentIntoSearchContainer(final Long orgId, final Collection<R> models, final Action action) {

    }

    /**
     * Deletes the given attachment details.
     *
     * @param crmResponse The response to be sent.
     * @param attachment  Attachment.
     */
    protected void delete(final long orgId, final CrmResponse crmResponse, final Attachment attachment) {
        delete(crmResponse, orgId, List.of(attachment));
    }

    /**
     * Deletes the given attachment details.
     *
     * @param crmResponse The response to be sent.
     * @param orgId       Organization id.
     * @param attachments Collection of {@link Attachment}.
     */
    private void delete(final CrmResponse crmResponse, final Long orgId, final Collection<Attachment> attachments) {
        final Collection<String> errors = validate((Collection<T>) attachments, Group.Delete.class);

        if (errors.isEmpty()) {
            getAttachmentConfigManager().delete(orgId, attachments);
            loadAttachmentIntoSearchContainer(orgId, attachments, Action.DELETE);
            crmResponse.put(AttachmentFields.ERRORS.getName(), errors);
        } else {
            throw ErrorCode.get(AttachmentErrorCodes.DELETE_ACTION_FAILED, "i18n.entity.attachment.delete.action.failed");
        }
    }

    /**
     * Checks whether an attachment exists for the given id or not.
     *
     * @param orgId        Organization id.
     * @param attachmentId Entity id.
     * @param crmResponse  Refers the response to be sent.
     */
    private void exists(final long orgId, final long attachmentId, final CrmResponse crmResponse) {
        crmResponse.put(AttachmentFields.FOUND.getName(), attachmentService.exists(attachmentId));
        crmResponse.send();
    }

    /**
     * Validates the models against the given validator groups.
     *
     * @param models          Which represents the respective Tlc model.
     * @param validatorGroups Validation groups.
     */
    private Collection<String> validate(final Collection<T> models, final Class<?>... validatorGroups) {
        final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
        final Set<ConstraintViolation<T>> violations = new HashSet<>();

        models.forEach(model -> violations.addAll(validator.validate(model, validatorGroups)));

        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    /**
     * Validates the details of respective models.
     *
     * @param model Respective TlcModel.
     */
    protected boolean isValidDetails(final T model) {
        return true;
    }

    /**
     * Handles the incoming request.
     *
     * @param request  The received request.
     * @param response The response to be sent.
     */
    protected abstract void handle(final CrmRequest request, final CrmResponse response);

    /**
     * Gets {@link AttachmentConfigManager} <T> of same type.
     */
    protected abstract AttachmentConfigManager<T> getAttachmentConfigManager();

    /**
     * Converts the JsonObject into subclass of TlcModel.
     *
     * @param request    The received request.
     * @param jsonObject JsonObject.
     */
    protected abstract T convert(final CrmRequest request, final JsonObject jsonObject);

}
