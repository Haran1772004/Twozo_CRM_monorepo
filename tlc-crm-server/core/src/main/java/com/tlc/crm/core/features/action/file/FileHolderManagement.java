package com.tlc.crm.core.features.action.file;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.handler.ModificationHandler;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.features.api.file.manager.FileHolderManager;
import com.tlc.crm.core.features.api.file.model.FileHolder;
import com.tlc.crm.core.features.api.file.model.FileSourceMapper;
import com.tlc.crm.core.features.api.file.model.converter.FileHolderConverter;
import com.tlc.crm.core.features.field.file.FileHolderFields;
import com.tlc.crm.core.features.internal.file.status.FileHolderErrorCodes;
import com.tlc.crm.entity.attachment.api.models.EntityAttachment;

import com.tlc.file.common.models.Attachable;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.attachment.action.AttachmentAction;
import com.tlc.crm.entity.attachment.api.manager.AttachmentConfigManager;

import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * Manages the {@link FileHolder} related web actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/file/mgmt")
public class FileHolderManagement extends AttachmentAction<FileHolder> {

    private static final int DEFAULT_START = 1;
    private static final int DEFAULT_LIMIT = 50;
    private static final String CREATED_TIME = "createdTime";
    private static final String UPDATED_TIME = "updatedTime";

    private final ConverterConfigBuilder converterConfigBuilder;
    private final FileHolderManager fileHolderManager;
    private final FileHolderConverter fileHolderConverter;
    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final CompanyManager companyManager;
    private final UserService userService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final OrganizationService organizationService;
    private final IndexDataPublisher indexDataPublisher;
    private final ModificationHandler modificationHandler;
    private final AttachmentService attachmentService;

    public FileHolderManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        fileHolderManager = FileHolderManager.getInstance();
        fileHolderConverter = FileHolderConverter.getInstance();
        contactManager = ContactManager.getInstance();
        dealManager = DealManager.getInstance();
        companyManager = CompanyManager.getInstance();
        userService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        indexDataPublisher = IndexDataPublisher.getInstance();
        modificationHandler = ModificationHandler.getInstance();
        this.attachmentService = AttachmentService.getInstance();
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
        final Long orgId = request.orgId();
        final JsonObject data = requestJson.getJsonObject(FileHolderFields.DATA.getName());
        final String type = requestJson.getString(FileHolderFields.TYPE.getName());

        if ("getFiles".equals(type)) {
            loadSourceFiles(orgId, data, response);
        }
    }

    /**
     * Deletes the given attachment details.
     *
     * @param crmResponse The response to be sent.
     * @param attachment  Attachment.
     */
    @Override
    public void delete(final long orgId, final CrmResponse crmResponse, final Attachment attachment) {
        final Map<Source, Pair<Long, Long>> entityIdMap = fileHolderManager.getEntityUserMap(orgId, attachment.id());

        super.delete(orgId, crmResponse, attachment);

        if (!entityIdMap.isEmpty()) {
            entityIdMap.forEach(((source, entityUserPair) ->
                    modificationHandler.updateEntityModificationDetails(orgId, entityUserPair.getValue(), entityUserPair.getKey(), source)));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link AttachmentConfigManager} of file holder.
     */
    @Override
    public AttachmentConfigManager<FileHolder> getAttachmentConfigManager() {
        return fileHolderManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The received request.
     * @param jsonObject JsonObject.
     * @return {@link FileHolder}.
     */
    @Override
    public FileHolder convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return fileHolderConverter.convert(converterConfig, jsonObject);
    }

    /**
     * <p>
     * Loads the source files for the given org id
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param data     Represents the request data
     * @param response Represents the response
     */
    private void loadSourceFiles(final Long orgId, final JsonObject data, final CrmResponse response) {
        final int start = Math.max(data.optInt(FileHolderFields.START.getName(), DEFAULT_START), DEFAULT_START);
        final int limit = Math.min(data.optInt(FileHolderFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final long entityId = data.getLong(FileHolderFields.ENTITY_ID.getName());
        final String source = data.getString(FileHolderFields.SOURCE.getName());
        long totalCount = 0;
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
            }

            fetchUserAndDispatchFiles(orgId, entityAttachmentMap.values(), response);
        } else {
            response.put(FileHolderFields.FILES.getName(), jsonArray);
        }
    }

    /**
     * Fetches users for the given organization ID and dispatches them based on the provided attachments.
     *
     * @param orgId       The ID of the organization for which users are being fetched.
     * @param attachments A collection of {@link EntityAttachment} objects that contain user-related data to be processed.
     * @param response    A {@link CrmResponse} object where the results or status of the dispatch will be recorded.
     */
    private void fetchUserAndDispatchFiles(final Long orgId, final Collection<EntityAttachment> attachments, final CrmResponse response) {
        response.convertIntoAsync();

        final Collection<Long> userIds = attachments.stream().map(EntityAttachment::getOwnerId).collect(Collectors.toUnmodifiableSet());
        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

            /**
             * {@inheritDoc}
             *
             * @param users Which contains collection of {@link User}
             */
            @Override
            public void onSuccess(final Collection<User> users) {
                final JsonArray jsonArray = Json.array();
                final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                for (final EntityAttachment entityAttachment : attachments.stream()
                        .sorted(Comparator.comparing(EntityAttachment::id).reversed()).toList()) {
                    final CrmUser owner = crmUserService.getUser(orgId, entityAttachment.getOwnerId());

                    if (Objects.nonNull(owner)) {
                        owner.setUser(userMap.get(owner.getIamId()));
                        entityAttachment.setOwner(owner);
                    }
                    jsonArray.put(fileHolderConverter.convert(entityAttachment));
                }
                response.put(FileHolderFields.FILES.getName(), jsonArray);
                response.send();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void onFailure(final Throwable throwable) {
                sendError(response, null, ErrorCode.get(FileHolderErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
            }
        };

        userService.fetchUsers(organizationService.getIamOrgId(orgId), crmUserService.getUsers(orgId, userIds).stream()
                .map(CrmUser::getIamId).collect(Collectors.toSet()), usersFetchCallBack);
    }

    /**
     * {@inheritDoc}
     *
     * @param fileHolder Refers the {@link FileHolder} details.
     */
    @Override
    protected boolean isValidDetails(final FileHolder fileHolder) {
        final FileSourceMapper fileSourceMapper = FileSourceMapper
                .get(Source.valueOf(fileHolder.getSource().toUpperCase()));

        fileSourceMapper.mapRelatedEntityId(fileHolder, fileHolder.orgId(), fileHolder.getEntityId());
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <R extends Attachable> void loadAttachmentIntoSearchContainer(final Long orgId, final Collection<R> models, final Action action) {
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
        indexDataPublisher.publishIndexData(orgId, fileHolders, action);
    }
}