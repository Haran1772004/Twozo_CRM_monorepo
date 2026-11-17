package com.tlc.crm.core.features.action.rest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.rest.secure.AbstractSecureRestAction;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.handler.ModificationHandler;
import com.tlc.crm.core.features.api.file.manager.FileHolderManager;
import com.tlc.crm.core.features.api.file.model.FileHolder;
import com.tlc.crm.core.features.api.file.model.converter.FileHolderConverter;
import com.tlc.crm.core.features.field.file.FileHolderFields;
import com.tlc.crm.core.features.internal.file.status.FileHolderErrorCodes;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;

import com.tlc.crm.entity.attachment.api.models.EntityAttachment;
import com.tlc.crm.entity.attachment.fields.AttachmentFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;

import com.tlc.file.client.converter.AttachmentConverter;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachable;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.queue.AttachmentRemovalPublisher;

import com.tlc.validator.TlcModel;

import com.tlc.web.HttpMethod;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages the {@code Attachment} related REST web actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@RestApiHandler(path = "/file")
public class FileRestManagement extends AbstractSecureRestAction {

    private final AttachmentService attachmentService;
    private final AttachmentConverter attachmentConverter;
    private final AttachmentRemovalPublisher attachmentRemovalPublisher;
    private final FileHolderManager fileHolderManager;
    private final FileHolderConverter fileHolderConverter;
    private final ModificationHandler modificationHandler;
    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final CompanyManager companyManager;
    private final UserService crmUserService;

    public FileRestManagement() {
        this.attachmentService = AttachmentService.getInstance();
        this.attachmentConverter = AttachmentConverter.getInstance();
        this.attachmentRemovalPublisher = AttachmentRemovalPublisher.getInstance();
        this.fileHolderManager = FileHolderManager.getInstance();
        this.fileHolderConverter = FileHolderConverter.getInstance();
        this.modificationHandler = ModificationHandler.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.crmUserService = UserService.getInstance();
    }

    /**
     * Handles an HTTP GET request for downloading the file.
     *
     * @param request  The CRM request containing path parameters, query parameters,
     *                 headers, and contextual metadata.
     * @param response The CRM response used to write the result or error payload.
     */
    @ApiRoute(path = "/download/{id}", method = HttpMethod.GET)
    public void download(final CrmRequest request, final CrmResponse response) {
        final String presignedUrl = attachmentService.getPresignedUrl(Long.parseLong(request.getParameter("id")), 30, null);

        response.setTemporaryRedirectUrl(presignedUrl);
        response.send();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing path parameters, query parameters,
     *                 headers, and contextual metadata.
     * @param response The CRM response used to write the result or error payload.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.GET)
    public void get(final CrmRequest request, final CrmResponse response) {
        attachmentService.getAttachments(List.of(Long.parseLong(request.getParameter("id")))).stream().findFirst().ifPresentOrElse(attachment -> {
            response.put(AttachmentFields.DATA.getName(), attachmentConverter.convert(attachment));
            response.send();
        }, () -> sendError(response, null, ErrorCode.get(EntityErrorCodes.ATTACHMENT_DETAILS_NOT_FOUND, "i18n.entity.attachment.not.found")));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing pagination, filters, or query parameters.
     * @param response The CRM response used to write the resulting collection.
     */
    @ApiRoute(method = HttpMethod.GET)
    public void getAll(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final long entityId = Long.parseLong(request.getParameter("entityId"));
        final String source = Source.get(request.getParameter("source")).getName();
        final String pageParam = request.getParameter("page");
        final String sizeParam = request.getParameter("size");
        final int page = Objects.isNull(pageParam) ? NumericalConstant.ONE : Integer.parseInt(pageParam);
        final int limit = Objects.isNull(sizeParam) ? NumericalConstant.ONE_HUNDRED : Integer.parseInt(sizeParam);
        long totalCount = 0;
        final int start = ((page - 1) * limit) + 1;

        final Collection<EntityAttachment> entityAttachments = switch (source) {
            case Contact.ENTITY_TYPE -> {
                if (contactManager.exists(orgId, entityId)) {
                    totalCount = fileHolderManager.getContactFilesCount(orgId, entityId);

                    yield totalCount > 0 && start <= totalCount ? fileHolderManager.getContactEntityAttachments(orgId, entityId, start, limit) : Collections.emptyList();
                } else {
                    yield Collections.emptyList();
                }
            }
            case Deal.ENTITY_TYPE -> {
                if (dealManager.exists(orgId, entityId)) {
                    totalCount = fileHolderManager.getDealFilesCount(orgId, entityId);

                    yield totalCount > 0 && start <= totalCount ? fileHolderManager.getDealEntityAttachments(orgId, entityId, start, limit) : Collections.emptyList();
                } else {
                    yield Collections.emptyList();
                }
            }
            case Company.ENTITY_TYPE -> {
                if (companyManager.exists(orgId, entityId)) {
                    totalCount = fileHolderManager.getCompanyFilesCount(orgId, entityId);

                    yield totalCount > 0 && start <= totalCount ? fileHolderManager.getCompanyEntityAttachments(orgId, entityId, start, limit) : Collections.emptyList();
                } else {
                    yield Collections.emptyList();
                }
            }
            default -> throw ErrorCode.get(FileHolderErrorCodes.SOURCE_NOT_FOUND,
                    String.format("Org ID : %d, Source Name : %s", orgId, source));
        };
        final JsonArray jsonArray = Json.array();

        response.put(FileHolderFields.TOTAL_RECORDS.getName(), totalCount);
        response.put(FileHolderFields.HAS_MORE.getName(), (start + limit) <= totalCount);
        response.put(FileHolderFields.SOURCE.getName(), source);

        if (!entityAttachments.isEmpty()) {
            final Collection<Attachment> attachments = AttachmentService.getInstance().getAttachments(entityAttachments.stream().map(EntityAttachment::id).toList());
            final Map<Long, EntityAttachment> entityAttachmentMap = entityAttachments.stream().collect(Collectors.toMap(EntityAttachment::id, Function.identity()));
            final Collection<Long> userIds = entityAttachmentMap.values().stream().map(EntityAttachment::getOwnerId).collect(Collectors.toUnmodifiableSet());
            final Map<Long, CrmUser> crmUsersMap = crmUserService.getUsers(orgId, userIds).stream().collect(Collectors.toMap(TlcModel::id, Function.identity()));

            for (final Attachment attachment : attachments) {
                final Long attachmentId = attachment.id();
                final EntityAttachment entityAttachment = entityAttachmentMap.get(attachmentId);

                entityAttachment.setId(attachmentId);
                entityAttachment.setOrgId(orgId);
                entityAttachment.setMediaType(attachment.getMediaType());
                entityAttachment.setFileName(attachment.getFileName());
                entityAttachment.setExtension(attachment.getExtension());
                entityAttachment.setFileSize(attachment.getFileSize());
                entityAttachment.setCreatedTime(attachment.getCreatedTime());
                entityAttachment.setUpdatedTime(attachment.getUpdatedTime());
                entityAttachment.setOwner(crmUsersMap.get(entityAttachment.getOwnerId()));

                jsonArray.put(fileHolderConverter.convert(entityAttachment));
            }
        }
        response.put(FileHolderFields.FILES.getName(), jsonArray);
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing identifiers or parameters required to locate the resource.
     * @param response The CRM response used to send deletion confirmation or error details.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.DELETE)
    public void delete(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();

        attachmentService.getAttachments(List.of(Long.parseLong(request.getParameter("id")))).stream().findFirst()
                .ifPresentOrElse(attachment -> {
                    final EntityAttachment entityAttachment = new EntityAttachment();
                    final Long id = attachment.id();

                    entityAttachment.setId(id);
                    entityAttachment.setOrgId(orgId);
                    entityAttachment.setExtension(attachment.getExtension());
                    entityAttachment.setFileName(attachment.getFileName());
                    entityAttachment.setFileSize(attachment.getFileSize());
                    entityAttachment.setMediaType(attachment.getMediaType());
                    entityAttachment.setCreatedTime(attachment.getCreatedTime());

                    delete(orgId, entityAttachment);
                    attachmentRemovalPublisher.publish(String.valueOf(id), List.of(id));
                    response.send();
                }, () -> sendError(response, null, ErrorCode.get(EntityErrorCodes.ATTACHMENT_DETAILS_NOT_FOUND, "i18n.entity.attachment.not.found")));
    }

    /**
     * Deletes the given attachment details.
     *
     * @param orgId      The organization ID.
     * @param attachment Attachment.
     */
    private void delete(final long orgId, final Attachment attachment) {
        final List<Attachment> attachments = List.of(attachment);
        final Map<Source, Pair<Long, Long>> entityIdMap = fileHolderManager.getEntityUserMap(orgId, attachment.id());

        fileHolderManager.delete(orgId, attachments);
        loadAttachmentIntoSearchContainer(orgId, attachments, Action.DELETE);

        if (!entityIdMap.isEmpty()) {
            entityIdMap.forEach(((source, entityUserPair) -> modificationHandler.updateEntityModificationDetails(orgId, entityUserPair.getValue(), entityUserPair.getKey(), source)));
        }
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
    private <R extends Attachable> void loadAttachmentIntoSearchContainer(final Long orgId, final Collection<R> models, final Action action) {
        final Collection<FileHolder> fileHolders = new ArrayList<>();

        for (final R model : models) {
            if (model instanceof final Attachment attachment) {
                final FileHolder fileHolder = new FileHolder();

                fileHolder.setOrgId(orgId);
                fileHolder.setId(attachment.id());
                fileHolders.add(fileHolder);
            } else if (model instanceof final FileHolder fileAttachmentHolder) {
                final Collection<EntityAttachment> fileAttachments = fileAttachmentHolder.getAttachments();
                final Long entityId = fileAttachmentHolder.getEntityId();
                final String source = fileAttachmentHolder.getSource();
                final Map<String, Long> relatedEntityIds = fileAttachmentHolder.getRelatedEntityIds();

                for (final Attachment fileAttachment : fileAttachments) {
                    final EntityAttachment entityAttachment = new EntityAttachment();
                    final FileHolder fileHolder = new FileHolder();

                    entityAttachment.setId(fileAttachment.id());
                    entityAttachment.setFileName(fileAttachment.getFileName());

                    fileHolder.setId(fileAttachment.id());
                    fileHolder.setOrgId(orgId);
                    fileHolder.setEntityId(entityId);
                    fileHolder.setSource(source);
                    fileHolder.setRelatedEntityIds(relatedEntityIds);
                    fileHolder.addAttachment(entityAttachment);
                    fileHolders.add(fileHolder);
                }
            }
        }
        IndexDataPublisher.getInstance().publishIndexData(orgId, fileHolders, action);
    }
}
