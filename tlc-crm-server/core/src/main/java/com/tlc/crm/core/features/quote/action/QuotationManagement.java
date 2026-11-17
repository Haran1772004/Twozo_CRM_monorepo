package com.tlc.crm.core.features.quote.action;

import com.tlc.attachment.io.File;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.handler.EventRequestHandler;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.service.EventRequestService;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.attachment.model.FileCategory;
import com.tlc.crm.core.features.quote.util.QuotationURLGenerator;
import com.tlc.crm.core.features.quote.file.QuotationFile;
import com.tlc.crm.core.features.quote.fields.QuotationFields;
import com.tlc.crm.core.features.quote.manager.QuotationManager;
import com.tlc.crm.core.features.quote.model.Quotation;
import com.tlc.crm.core.features.quote.model.converter.QuotationConverter;
import com.tlc.crm.core.features.quote.resolver.QuotationJsonPlaceholderResolver;
import com.tlc.crm.core.holder.ConfigManagerHolder;
import com.tlc.crm.core.internal.quote.status.QuotationErrorCodes;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.security.permission.annotation.authorizer.QuotationAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.models.ObjectAttachment;
import com.tlc.file.server.api.manager.AttachmentManager;
import com.tlc.iam.common.models.User;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Handles all web actions related to {@link Quotation} management,
 * including CRUD operations and clone functionality.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@QuotationAuthorizer(subCategories = {SubCategory.Type.DEALS, SubCategory.Type.CONTACTS, SubCategory.Type.PRODUCTS})
@WebAction(path = "/quote/mgmt")
public class QuotationManagement extends AuthorizationController {

    /**
     * Maximum permitted size (in bytes) for any single quotation file artifact (HTML or PDF).
     * Files that exceed this limit are rejected before upload to prevent unbounded storage
     * consumption and downstream rendering failures.
     * Current cap: 10 MB.
     */
    private static final long MAX_QUOTATION_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB


    private final QuotationManager quotationManager;
    private final QuotationConverter quotationConverter;
    private final ModelValidator modelValidator;
    private final com.tlc.iam.client.UserService iamUserService;
    private final OrganizationService organizationService;
    private final QuotationURLGenerator quotationUrlGenerator;
    private final ConfigManagerHolder configManagerHolder;
    private final AttachmentService attachmentService;
    private final AttachmentManager attachmentManager;
    private final EventRequestHandler fileEventRequestHandler;

    /**
     * <p>
     * Initialises the quotation manager, converter, validator, and IAM services.
     * </p>
     */
    public QuotationManagement() {
        this.quotationManager = QuotationManager.getInstance();
        this.quotationConverter = QuotationConverter.getInstance();
        this.modelValidator = ValidatorServiceProvider.get().modelValidator();
        this.iamUserService = com.tlc.iam.client.UserService.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.quotationUrlGenerator = QuotationURLGenerator.getInstance();
        this.configManagerHolder = ConfigManagerHolder.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        this.attachmentManager = AttachmentManager.getInstance();
        this.fileEventRequestHandler = EventRequestService.get().getFileEventRequestHandler();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Routes incoming requests to the appropriate quotation operation based on action type.
     * </p>
     *
     * @param crmRequest  The request to be handled
     * @param crmResponse The response
     * @throws Exception if an error occurs during the request processing
     */
    @Override
    protected void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(QuotationFields.DATA);
        final String type = requestJson.getString(QuotationFields.TYPE);

        switch (type) {
            case "create" -> create(crmRequest, crmResponse, data);
            case "get" -> get(crmRequest, crmResponse, data);
            case "draft" -> draft(crmRequest, crmResponse, data);
            case "update" -> update(crmRequest, crmResponse, data);
            case "delete" -> delete(crmRequest, crmResponse, data);
            case "clone" -> clone(crmRequest, crmResponse, data);
            case "bCreate" -> bulkCreate(crmRequest, crmResponse, data);
            case "bUpdate" -> bulkUpdate(crmRequest, crmResponse, data);
            case "recall" -> recall(crmRequest, crmResponse, data);
        }
    }

    /**
     * <p>
     * Creates a new {@link Quotation} for the organisation.
     * </p>
     *
     * <p>
     * When the quotation is linked to a deal, the sales owner's user and organisation
     * details are fetched asynchronously from the IAM microservice using {@link EventCallback}.
     * The response is converted to async mode, and placeholder resolution, quotation creation,
     * and response dispatch are all handled within the callback.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @param data        Represents the request data
     */
    private void create(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final String sourceKey = FieldKeys.SOURCE.getName();
        final String entityIdKey = FieldKeys.ENTITY_ID.getName();

        if (data.containsKey(sourceKey) && data.containsKey(entityIdKey)) {
            final EntityConfigManager<? extends TlcModel> entityConfigManager = (EntityConfigManager<?>) configManagerHolder.getManager(Source.get(data.getString(sourceKey)));

            entityConfigManager.associateEntityWithDeal(crmRequest.orgId(), data.getLong(entityIdKey), data.getLong(QuotationFields.DEAL_ID));
        }
        final Quotation quotation = quotationConverter.convertToModel(new CrmConverterData<>(crmRequest.getUser(),
                crmRequest.locale(), false, false, data));
        final Deal deal = quotation.getDeal();
        final ErrorCode errorCode = ErrorCode.get(QuotationErrorCodes.QUOTATION_CREATE_FAILED, "i18n.quotation.create.failed");

        if (Objects.nonNull(deal)) {
            final CrmUser salesOwner = deal.getSalesOwner();

            if (Objects.nonNull(salesOwner) && Objects.nonNull(salesOwner.getIamId())) {
                final long orgId = crmRequest.orgId();
                final Collection<Long> iamUserIds = Set.of(salesOwner.getIamId());

                crmResponse.convertIntoAsync();

                final EventCallback<Collection<User>> salesOwnerFetchCallback = new EventCallback<>() {
                    @Override
                    public void onSuccess(final Collection<User> users) {
                        users.stream().findFirst().ifPresent(salesOwner::setUser);
                        final QuotationJsonPlaceholderResolver resolver = new QuotationJsonPlaceholderResolver(orgId);

                        quotation.setQuoteJson(resolver.resolve(quotation.getSchemaJson(), quotation));

                        if (validate(quotation, Group.Create.class, errorCode, crmResponse)) {
                            quotationManager.create(quotation);
                            final Long id = quotation.id();
                            final JsonObject quoteJson = quotation.getQuoteJson();

                            loadQuoteUrl(orgId, quotation.getOwner().id(), id, quoteJson);
                            crmResponse.put(QuotationFields.ID, id);
                            crmResponse.put(QuotationFields.QUOTE_JSON, quoteJson);
                            crmResponse.put(QuotationFields.MESSAGE, crmResponse.resolveI18nString("i18n.quotation.create.success"));
                        }
                        crmResponse.send();
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        sendError(crmResponse, null, ErrorCode.get(QuotationErrorCodes.QUOTATION_CREATE_FAILED, "i18n.quotation.create.failed", throwable));
                    }
                };

                iamUserService.fetchUsersWithOrg(organizationService.getIamOrgId(orgId), iamUserIds, salesOwnerFetchCallback);
                return;
            }
        }

        if (validate(quotation, Group.Create.class, errorCode, crmResponse)) {
            quotationManager.create(quotation);

            crmResponse.put(QuotationFields.ID, quotation.id());
            crmResponse.put(QuotationFields.QUOTE_JSON, quotation.getQuoteJson());
            crmResponse.put(QuotationFields.MESSAGE, crmResponse.resolveI18nString("i18n.quotation.create.success"));
        }
    }

    /**
     * <p>
     * Retrieves a single {@link Quotation} by its ID and puts the result in the response.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @param data        Represents the request data
     */
    private void get(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final Long orgId = crmRequest.orgId();
        final Quotation quotation = quotationManager.get(orgId, data.getLong(QuotationFields.ID));
        final JsonObject quoteJson = quotation.getQuoteJson();

        if (Objects.nonNull(quoteJson)) {
            loadQuoteUrl(orgId, crmRequest.userId(), quotation.id(), quoteJson);
        }

        crmResponse.put(QuotationFields.QUOTATION, quotationConverter.convertToJson(new CrmConverterData<>(crmRequest.getUser(), crmRequest.locale(), quotation)));
    }

    /**
     * <p>
     * Injects the live preview URL into the {@code settings} node of the resolved quote JSON.
     * </p>
     *
     * <p>
     * The URL is generated by {@link QuotationURLGenerator#generatePreviewURL(long, long, long)} using the
     * quotation ID and is written directly to the {@code quoteUrl} field inside the
     * {@code settings} object. This ensures that every response carrying a quote JSON always
     * contains an up-to-date, ID-specific preview link without storing it persistently.
     * </p>
     *
     * @param orgId     Represents the organization ID.
     * @param userId    Represents the user ID.
     * @param id        The unique identifier of the {@link Quotation}
     * @param quoteJson The resolved quote JSON whose {@code settings} node will be updated in place
     */
    private void loadQuoteUrl(final long orgId, final long userId, final long id, final JsonObject quoteJson) {
        final JsonObject settingsJson = quoteJson.getJsonObject(QuotationFields.SETTINGS);

        if (Objects.nonNull(settingsJson)) {
            settingsJson.put(QuotationFields.QUOTE_URL, quotationUrlGenerator.generatePreviewURL(orgId, userId, id));
            settingsJson.put(QuotationFields.QUOTE_DOWNLOAD_URL, quotationUrlGenerator.generateDownloadURL(orgId, userId, id));
        }
    }

    /**
     * <p>
     * Drafts an existing {@link Quotation}.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @param data        Represents the request data
     */
    private void draft(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final Quotation quotation = quotationConverter.convertToModel(new CrmConverterData<>(crmRequest.getUser(),
                crmRequest.locale(), true, false, data));

        quotation.setStatus(Quotation.Status.DRAFT);
        final ErrorCode errorCode = ErrorCode.get(QuotationErrorCodes.QUOTATION_UPDATE_FAILED, "i18n.quotation.draft.failed");

        if (validate(quotation, Group.Update.class, errorCode, crmResponse)) {
            quotationManager.update(quotation);
            crmResponse.put(QuotationFields.MESSAGE, crmResponse.resolveI18nString("i18n.quotation.draft.success"));
        }
    }

    /**
     * <p>
     * Updates an existing {@link Quotation}.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @param data        Represents the request data
     */
    private void update(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final Quotation quotation = quotationConverter.convertToModel(new CrmConverterData<>(crmRequest.getUser(),
                crmRequest.locale(), true, false, data));
        final ErrorCode errorCode = ErrorCode.get(QuotationErrorCodes.QUOTATION_UPDATE_FAILED, "i18n.quotation.update.failed");

        if (validate(quotation, Group.Update.class, errorCode, crmResponse)) {
            final Long orgId = quotation.orgId();
            final CrmUser owner = quotation.getOwner();
            final Collection<Long> iamUserIds = Set.of(owner.getIamId());

            crmResponse.convertIntoAsync();

            final EventCallback<Collection<User>> ownerFetchCallback = new EventCallback<>() {
                @Override
                public void onSuccess(final Collection<User> users) {
                    users.stream().findFirst().ifPresent(owner::setUser);

                    try {
                        if (quotation.getStatus() != Quotation.Status.DRAFT) {
                            fileEventRequestHandler.handle(() -> {
                                try {
                                    final QuotationFile quotationFile = new QuotationFile.Builder(quotation).build();

                                    quotationFile.getPreviewFile().ifPresent(file -> quotation.setHtmlFileId(uploadFile(orgId, file).id()));
                                    quotationFile.getDownloadFile().ifPresent(file -> quotation.setPdfFileId(uploadFile(orgId, file).id()));

                                    quotationManager.update(quotation);
                                    crmResponse.put(QuotationFields.MESSAGE, crmResponse.resolveI18nString("i18n.quotation.update.success"));
                                    crmResponse.send();
                                } catch (final Exception exception) {
                                    sendError(crmResponse, null, ErrorCode.get(QuotationErrorCodes.QUOTATION_UPDATE_FAILED, "i18n.quotation.update.failed", exception));
                                }
                            });
                        } else {
                            quotationManager.update(quotation);
                            crmResponse.put(QuotationFields.MESSAGE, crmResponse.resolveI18nString("i18n.quotation.update.success"));
                            crmResponse.send();
                        }
                    } catch (final Exception exception) {
                        sendError(crmResponse, null, ErrorCode.get(QuotationErrorCodes.QUOTATION_UPDATE_FAILED, "i18n.quotation.update.failed", exception));
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(QuotationErrorCodes.QUOTATION_UPDATE_FAILED, "i18n.quotation.update.failed", throwable));
                }
            };

            iamUserService.fetchUsersWithOrg(organizationService.getIamOrgId(orgId), iamUserIds, ownerFetchCallback);
        }
    }

    /**
     * Uploads the given file as an attachment for the specified organization.
     *
     * @param orgId Organization identifier associated with the attachment.
     * @param file  File to upload.
     *
     * @return Uploaded attachment details.
     */
    private Attachment uploadFile(final long orgId, final File file) {
        if (file.size() > MAX_QUOTATION_FILE_SIZE_BYTES) {
            throw ErrorCode.get(QuotationErrorCodes.QUOTATION_FILE_TOO_LARGE, "i18n.quotation.file.too.large");
        }
        final Long iamOrgId = organizationService.getIamOrgId(orgId);
        final Attachment attachment = new ObjectAttachment();

        attachment.setInputStream(file.inputStream());
        attachment.setMediaType(file.mediaType());
        attachment.setExtension(file.extension());
        attachment.setFileName(file.name());
        attachment.setFileSize(file.size());

        attachmentService.upload(iamOrgId, List.of(attachment), FileCategory.QUOTATION.getName());
        return attachment;
    }

    /**
     * <p>
     * Deletes a {@link Quotation} by its ID.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @param data        Represents the request data
     */
    private void delete(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final long orgId = crmRequest.orgId();
        final long id = data.getLong(QuotationFields.ID);
        final Quotation quotation = new Quotation();

        quotation.setOrgId(orgId);
        quotation.setId(id);
        final ErrorCode errorCode = ErrorCode.get(QuotationErrorCodes.QUOTATION_DELETE_FAILED, "i18n.quotation.delete.failed");

        if (validate(quotation, Group.Delete.class, errorCode, crmResponse)) {
            final Collection<Long> fileIds = quotationManager.getQuoteFileIds(orgId, id);

            quotationManager.delete(orgId, id);

            if (!fileIds.isEmpty()) {
                attachmentManager.delete(fileIds);
            }
            crmResponse.put(QuotationFields.MESSAGE, crmResponse.resolveI18nString("i18n.quotation.delete.success"));
        }
    }

    /**
     * <p>
     * Clones an existing {@link Quotation} and returns the newly created quotation ID.
     * The clone receives a new ID, a new UUID and its status is reset to DRAFT.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @param data        Represents the request data
     */
    private void clone(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final long orgId = crmRequest.orgId();
        final long id = data.getLong(QuotationFields.ID);
        final Quotation cloned = quotationManager.clone(orgId, id);

        crmResponse.put(QuotationFields.ID, cloned.id());
        crmResponse.put(QuotationFields.NAME, cloned.getName());
        crmResponse.put(QuotationFields.MESSAGE, crmResponse.resolveI18nString("i18n.quotation.clone.success"));
    }

    /**
     * <p>
     * Creates multiple new {@link Quotation} entities for the organisation in a single transaction.
     * This method processes an array of quotation data, validates each quotation, and if all
     * validations pass, creates all quotations atomically. If any validation fails, no quotations
     * are created and an error response is returned.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest} containing user and organisation context
     * @param crmResponse Represents the {@link CrmResponse} that will contain the result
     * @param data        Represents the request data containing an array of quotations to create
     */
    private void bulkCreate(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final long orgId = crmRequest.orgId();
        final JsonArray items = data.getJsonArray(QuotationFields.QUOTATIONS);
        final Collection<Quotation> quotations = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            final JsonObject quotationJson = items.getJsonObject(i);
            final Quotation quotation = quotationConverter.convertToModel(new CrmConverterData<>(crmRequest.getUser(),
                    crmRequest.locale(), false, false, quotationJson));

            quotation.setCreatedBy(crmRequest.getUser());
            quotations.add(quotation);
        }
        final ErrorCode errorCode = ErrorCode.get(QuotationErrorCodes.QUOTATION_CREATE_FAILED, "i18n.quotation.create.failed");

        if (validate(quotations, Group.Create.class, errorCode, crmResponse)) {
            quotationManager.bulkCreate(orgId, quotations);
            final JsonArray createdIds = Json.array();

            quotations.forEach(quotation -> createdIds.put(quotation.id()));

            crmResponse.put(QuotationFields.CREATED_RECORDS, createdIds.size());
            crmResponse.put(QuotationFields.QUOTATIONS, createdIds);
            crmResponse.put(QuotationFields.MESSAGE, crmResponse.resolveI18nString("i18n.quotation.create.success"));
        }
    }

    /**
     * <p>
     * Updates multiple existing {@link Quotation} entities for the organisation in a single transaction.
     * This method processes an array of quotation data, validates each quotation for update operations,
     * and if all validations pass, updates all quotations atomically. If any validation fails, no quotations
     * are updated and an error response is returned.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest} containing user and organisation context
     * @param crmResponse Represents the {@link CrmResponse} that will contain the result
     * @param data        Represents the request data containing an array of quotations to update
     */
    private void bulkUpdate(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final long orgId = crmRequest.orgId();
        final JsonArray items = data.getJsonArray(QuotationFields.QUOTATIONS);
        final Collection<Quotation> quotations = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            final JsonObject quotationJson = items.getJsonObject(i);
            final Quotation quotation = quotationConverter.convertToModel(new CrmConverterData<>(crmRequest.getUser(),
                    crmRequest.locale(), true, false, quotationJson));

            quotation.setUpdatedBy(crmRequest.getUser());
            quotations.add(quotation);
        }
        final ErrorCode errorCode = ErrorCode.get(QuotationErrorCodes.QUOTATION_UPDATE_FAILED, "i18n.quotation.update.failed");

        if (validate(quotations, Group.Update.class, errorCode, crmResponse)) {
            quotationManager.bulkUpdate(orgId, quotations);
            final JsonArray updatedIds = Json.array();

            quotations.forEach(quotation -> updatedIds.put(quotation.id()));

            crmResponse.put(QuotationFields.UPDATED_RECORDS, updatedIds.size());
            crmResponse.put(QuotationFields.QUOTATIONS, updatedIds);
            crmResponse.put(QuotationFields.MESSAGE, crmResponse.resolveI18nString("i18n.quotation.update.success"));
        }
    }

    /**
     * <p>
     * Recalls an existing {@link Quotation} by setting its status back to DRAFT.
     * This allows a previously published or sent quotation to be edited again.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @param data        Represents the request data containing the quotation ID
     */
    private void recall(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final long orgId = crmRequest.orgId();
        final long id = data.getLong(QuotationFields.ID);

        quotationManager.updateStatus(orgId, crmRequest.getUser().id(), id, Quotation.Status.DRAFT);
        crmResponse.put(QuotationFields.ID, id);
        crmResponse.put(QuotationFields.MESSAGE, crmResponse.resolveI18nString("i18n.quotation.recall.success"));
    }

    /**
     * <p>
     * Validates a single {@link Quotation} against the specified validation group
     * and sets the error response if validation fails.
     * </p>
     *
     * @param quotation   Represents the {@link Quotation} to validate
     * @param group       Represents the validation group class
     * @param errorCode   Represents the {@link ErrorCode} to set on failure
     * @param crmResponse Represents the {@link CrmResponse} to populate with errors
     * @return {@code true} if validation passes, {@code false} otherwise
     */
    private boolean validate(final Quotation quotation, final Class<?> group, final ErrorCode errorCode, final CrmResponse crmResponse) {
        return validate(List.of(quotation), group, errorCode, crmResponse);
    }

    /**
     * <p>
     * Validates a collection of {@link Quotation} entities against the specified validation group.
     * If any validation errors are found, the error code and error summary are set on the response.
     * </p>
     *
     * @param quotations  Represents the collection of {@link Quotation} entities to validate
     * @param group       Represents the validation group class
     * @param errorCode   Represents the {@link ErrorCode} to set on failure
     * @param crmResponse Represents the {@link CrmResponse} to populate with errors
     * @return {@code true} if all validations pass, {@code false} otherwise
     */
    private boolean validate(final Collection<Quotation> quotations, final Class<?> group, final ErrorCode errorCode, final CrmResponse crmResponse) {
        final Collection<ErrorDescriptor> errors = modelValidator.validate(quotations, group);

        if (errors.isEmpty()) {
            return true;
        }
        crmResponse.setErrorCode(errorCode);
        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
        return false;
    }

}
