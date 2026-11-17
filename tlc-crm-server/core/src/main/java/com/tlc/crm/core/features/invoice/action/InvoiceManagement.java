package com.tlc.crm.core.features.invoice.action;

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
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.service.EventRequestService;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.attachment.model.FileCategory;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;
import com.tlc.crm.core.features.invoice.manager.InvoiceManager;
import com.tlc.crm.core.features.invoice.manager.InvoiceManagerImpl;
import com.tlc.crm.core.features.invoice.model.Invoice;
import com.tlc.crm.core.features.invoice.model.InvoiceFile;
import com.tlc.crm.core.features.invoice.model.converter.InvoiceConverter;
import com.tlc.crm.core.features.invoice.resolver.InvoiceJsonPlaceholderResolver;
import com.tlc.crm.core.features.invoice.service.InvoiceURLGenerator;
import com.tlc.crm.core.internal.invoice.status.InvoiceErrorCodes;
import com.tlc.crm.entity.role.permission.validator.EntityPermissionValidator;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.permission.annotation.authorizer.InvoiceAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.QuotationAuthorizer;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.models.ObjectAttachment;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles web actions related to {@link Invoice} management.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@InvoiceAuthorizer(subCategories = {SubCategory.Type.CONTACTS, SubCategory.Type.PRODUCTS})
@WebAction(path = "/invoice/mgmt")
public class InvoiceManagement extends AuthorizationController {

    private static final long MAX_INVOICE_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final String I18N_CREATE_DISABLED_ENTITY_ACCESS = "i18n.invoice.action.create.disabled.entity.access";

    private final InvoiceManager invoiceManager;
    private final InvoiceConverter invoiceConverter;
    private final ModelValidator modelValidator;
    private final UserService iamUserService;
    private final OrganizationService organizationService;
    private final EventRequestHandler fileEventRequestHandler;
    private final AttachmentService attachmentService;
    private final InvoiceURLGenerator invoiceURLGenerator;
    private final DealManager dealManager;

    /**
     * <p>
     * Initializes the services required for invoice management.
     * </p>
     */
    public InvoiceManagement() {
        this.invoiceManager = InvoiceManagerImpl.getInstance();
        this.invoiceConverter = InvoiceConverter.getInstance();
        this.modelValidator = ValidatorServiceProvider.get().modelValidator();
        this.iamUserService = UserService.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.fileEventRequestHandler = EventRequestService.get().getFileEventRequestHandler();
        this.attachmentService = AttachmentService.getInstance();
        this.invoiceURLGenerator = InvoiceURLGenerator.getInstance();
        this.dealManager = DealManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Routes incoming requests to the appropriate invoice operation based on
     * the requested action type.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}.
     * @param crmResponse Represents the {@link CrmResponse}.
     * @throws Exception If an error occurs while processing the request.
     */
    @Override
    protected void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(InvoiceFields.DATA);
        final String type = requestJson.getString(InvoiceFields.TYPE);

        switch (type) {
            case "create" -> create(crmRequest, crmResponse, data);
            case "get" -> get(crmRequest, crmResponse, data);
            case "update" -> update(crmRequest, crmResponse, data);
            case "delete" -> delete(crmRequest, crmResponse, data);
            case "clone" -> clone(crmRequest, crmResponse, data);
            case "bCreate" -> bulkCreate(crmRequest, crmResponse, data);
            case "bUpdate" -> bulkUpdate(crmRequest, crmResponse, data);
            case "updateStatus" -> updateStatus(crmRequest, crmResponse, data);
            default -> sendError(crmResponse, null, ErrorCode.get(InvoiceErrorCodes.INVOICE_UNSUPPORTED_ACTION));
        }
    }

    /**
     * <p>
     * Creates a new {@link Invoice} for the organization.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}.
     * @param crmResponse Represents the {@link CrmResponse}.
     * @param data        Represents the invoice request data.
     */
    private void create(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final Invoice invoice = invoiceConverter.convertToModel(new CrmConverterData<>(crmRequest.getUser(),
                crmRequest.locale(), false, false, data));
        final long orgId = crmRequest.orgId();
        final ErrorCode errorCode = ErrorCode.get(InvoiceErrorCodes.INVOICE_CREATE_FAILED, "i18n.invoice.create.failed");
        final Collection<Long> entityUserIds = getEntityUserIds(invoice);

        if (entityUserIds.isEmpty()) {
            throw ErrorCode.get(InvoiceErrorCodes.INVALID_ENTITY_ID);
        }
        crmResponse.convertIntoAsync();

        final EventCallback<Collection<User>> salesOwnerFetchCallback =
                new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        populateSalesOwnerUsers(invoice, users);
                        final InvoiceJsonPlaceholderResolver invoiceJsonPlaceholderResolver = new InvoiceJsonPlaceholderResolver(orgId);

                        invoice.setInvoiceJson(invoiceJsonPlaceholderResolver.resolve(invoice.getSchemaJson(), invoice));

                        if (validate(invoice, Group.Create.class, errorCode, crmResponse)) {
                            invoiceManager.create(invoice);

                            crmResponse.put(InvoiceFields.ID, invoice.id());
                            crmResponse.put(InvoiceFields.INVOICE_JSON, invoice.getInvoiceJson());
                            crmResponse.put(InvoiceFields.MESSAGE, crmResponse.resolveI18nString("i18n.invoice.create.success"));
                        }

                        crmResponse.send();
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        sendError(crmResponse, null,
                                ErrorCode.get(InvoiceErrorCodes.INVOICE_CREATE_FAILED, "i18n.invoice.create.failed", throwable));
                    }
                };

        iamUserService.fetchUsersWithOrg(organizationService.getIamOrgId(orgId), entityUserIds, salesOwnerFetchCallback);
    }

    /**
     * <p>
     * Retrieves an existing {@link Invoice} identified by the given ID and
     * writes its JSON representation to the response.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}.
     * @param crmResponse Represents the {@link CrmResponse}.
     * @param data        Represents the request data containing the invoice ID.
     */
    private void get(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final Invoice invoice = invoiceManager.get(crmRequest.orgId(), data.getLong(InvoiceFields.ID));
        final JsonObject invoiceJson = invoice.getInvoiceJson();

        if (Objects.nonNull(invoiceJson)) {
            loadInvoiceUrl(crmRequest.orgId(), crmRequest.userId(), invoice.id(), invoiceJson);
        }

        crmResponse.put(InvoiceFields.INVOICE, invoiceConverter.convertToJson(new CrmConverterData<>(crmRequest.getUser(),
                crmRequest.locale(), invoice)));
    }

    /**
     * <p>
     * Injects the live preview and download URLs into the {@code settings} node of the resolved invoice JSON.
     * </p>
     *
     * @param orgId       Represents the organization ID.
     * @param userId      Represents the user ID.
     * @param id          The unique identifier of the {@link Invoice}
     * @param invoiceJson The resolved invoice JSON whose {@code settings} node will be updated in place
     */
    private void loadInvoiceUrl(final long orgId, final long userId, final long id, final JsonObject invoiceJson) {
        final JsonObject settingsJson = invoiceJson.getJsonObject(InvoiceFields.SETTINGS);

        if (Objects.nonNull(settingsJson)) {
            settingsJson.put(InvoiceFields.INVOICE_URL, invoiceURLGenerator.generatePreviewURL(orgId, userId, id));
            settingsJson.put(InvoiceFields.INVOICE_DOWNLOAD_URL, invoiceURLGenerator.generateDownloadURL(orgId, userId, id));
        }
    }

    /**
     * <p>
     * Updates an existing {@link Invoice}.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}.
     * @param crmResponse Represents the {@link CrmResponse}.
     * @param data        Represents the invoice request data to update.
     */
    private void update(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final Invoice invoice = invoiceConverter.convertToModel(new CrmConverterData<>(crmRequest.getUser(),
                crmRequest.locale(), true, false, data));
        final ErrorCode errorCode = ErrorCode.get(InvoiceErrorCodes.INVOICE_UPDATE_FAILED, "i18n.invoice.update.failed");

        if (validate(invoice, Group.Update.class, errorCode, crmResponse)) {
            final long orgId = invoice.orgId();
            final Collection<Long> entityUserIds = getEntityUserIds(invoice);

            if (entityUserIds.isEmpty()) {
                throw ErrorCode.get(InvoiceErrorCodes.INVALID_ENTITY_ID);
            }
            final Invoice.Status status = invoice.getStatus();

            if (status.isDraft()) {
                invoiceManager.update(invoice);

                crmResponse.put(InvoiceFields.MESSAGE, crmResponse.resolveI18nString("i18n.invoice.draft.success"));
                return;
            }
            crmResponse.convertIntoAsync();

            final EventCallback<Collection<User>> salesOwnerFetchCallback =
                    new EventCallback<>() {

                        @Override
                        public void onSuccess(final Collection<User> users) {
                            populateSalesOwnerUsers(invoice, users);
                            invoice.setUpdatedBy(crmRequest.getUser());

                            try {

                                fileEventRequestHandler.handle(() -> {
                                    try {
                                        final InvoiceFile invoiceFile = new InvoiceFile.Builder(invoice).build();

                                        invoiceFile.getPreviewFile().ifPresent(file -> invoice.setHtmlFileId(uploadFile(orgId, file).id()));
                                        invoiceFile.getDownloadFile().ifPresent(file -> invoice.setPdfFileId(uploadFile(orgId, file).id()));

                                        invoiceManager.update(invoice);

                                        crmResponse.put(InvoiceFields.MESSAGE, crmResponse.resolveI18nString("i18n.invoice.update.success"));

                                        crmResponse.send();
                                    } catch (Exception exception) {
                                        sendError(crmResponse, null,
                                                ErrorCode.get(InvoiceErrorCodes.INVOICE_UPDATE_FAILED, "i18n.invoice.update.failed", exception));
                                    }
                                });
                            } catch (final Exception exception) {
                                sendError(crmResponse, null,
                                        ErrorCode.get(InvoiceErrorCodes.INVOICE_UPDATE_FAILED, "i18n.invoice.update.failed", exception));
                            }
                        }

                        @Override
                        public void onFailure(final Throwable throwable) {
                            sendError(crmResponse, null,
                                    ErrorCode.get(InvoiceErrorCodes.INVOICE_UPDATE_FAILED, "i18n.invoice.update.failed", throwable));
                        }
                    };

            iamUserService.fetchUsersWithOrg(organizationService.getIamOrgId(orgId), entityUserIds, salesOwnerFetchCallback);
        }
    }

    /**
     * <p>
     * Deletes the {@link Invoice} identified by the given ID after validating
     * that it exists for the organization.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}.
     * @param crmResponse Represents the {@link CrmResponse}.
     * @param data        Represents the request data containing the invoice ID.
     */
    private void delete(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final long orgId = crmRequest.orgId();
        final long id = data.getLong(InvoiceFields.ID);
        final Invoice invoice = new Invoice();

        invoice.setOrgId(orgId);
        invoice.setId(id);
        final ErrorCode errorCode = ErrorCode.get(InvoiceErrorCodes.INVOICE_NOT_FOUND, "i18n.invoice.delete.failed");

        if (validate(invoice, Group.Delete.class, errorCode, crmResponse)) {
            invoiceManager.delete(orgId, id);
            crmResponse.put(InvoiceFields.MESSAGE, crmResponse.resolveI18nString("i18n.invoice.delete.success"));
        }
    }

    /**
     * <p>
     * Clones an existing {@link Invoice} and returns the newly created invoice information.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}.
     * @param crmResponse Represents the {@link CrmResponse}.
     * @param data        Represents the request data containing the invoice ID.
     */
    private void clone(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final long orgId = crmRequest.orgId();
        final long id = data.getLong(InvoiceFields.ID);
        final Invoice cloned = invoiceManager.clone(orgId, id);

        crmResponse.put(InvoiceFields.ID, cloned.id());
        crmResponse.put(InvoiceFields.NUMBER, cloned.getNumber());
        crmResponse.put(InvoiceFields.MESSAGE, crmResponse.resolveI18nString("i18n.invoice.clone.success"));
    }

    /**
     * <p>
     * Creates multiple {@link Invoice} records in a single request after
     * validating all of them.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}.
     * @param crmResponse Represents the {@link CrmResponse}.
     * @param data        Represents the request data containing the array of invoices to create.
     */
    private void bulkCreate(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final JsonArray jsonArray = data.getJsonArray(InvoiceFields.INVOICES);
        final Collection<Invoice> invoices = new ArrayList<>();
        final ErrorCode errorCode = ErrorCode.get(InvoiceErrorCodes.INVOICE_CREATE_FAILED, "i18n.invoice.create.failed");

        if (Objects.isNull(jsonArray)) {
            sendError(crmResponse, null, errorCode);
            return;
        }
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            final JsonObject invoiceJson = jsonArray.getJsonObject(index);
            final Invoice invoice = invoiceConverter.convertToModel(new CrmConverterData<>(crmRequest.getUser(),
                    crmRequest.locale(), false, false, invoiceJson));

            invoices.add(invoice);
        }

        if (validate(invoices, Group.Create.class, errorCode, crmResponse)) {
            invoiceManager.bulkCreate(crmRequest.orgId(), invoices);
            final JsonArray createdIds = Json.array();

            invoices.forEach(invoice -> createdIds.put(invoice.id()));

            crmResponse.put(InvoiceFields.CREATED_RECORDS, createdIds.size());
            crmResponse.put(InvoiceFields.INVOICES, createdIds);
            crmResponse.put(InvoiceFields.MESSAGE, crmResponse.resolveI18nString("i18n.invoice.create.success"));
        }
    }

    /**
     * <p>
     * Updates multiple {@link Invoice} records in a single request after
     * validating all of them.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}.
     * @param crmResponse Represents the {@link CrmResponse}.
     * @param data        Represents the request data containing the array of invoices to update.
     */
    private void bulkUpdate(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final JsonArray jsonArray = data.getJsonArray(InvoiceFields.INVOICES);
        final Collection<Invoice> invoices = new java.util.ArrayList<>();
        final ErrorCode errorCode = ErrorCode.get(InvoiceErrorCodes.INVOICE_UPDATE_FAILED, "i18n.invoice.update.failed");

        if (Objects.isNull(jsonArray)) {
            sendError(crmResponse, null, errorCode);
            return;
        }
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            final JsonObject invoiceJson = jsonArray.getJsonObject(index);
            final Invoice invoice = invoiceConverter.convertToModel(new CrmConverterData<>(crmRequest.getUser(),
                    crmRequest.locale(), true, false, invoiceJson));

            invoice.setUpdatedBy(crmRequest.getUser());
            invoices.add(invoice);
        }

        if (validate(invoices, Group.Update.class, errorCode, crmResponse)) {
            invoiceManager.bulkUpdate(crmRequest.orgId(), invoices);
            final JsonArray updatedIds = Json.array();

            invoices.forEach(invoice -> updatedIds.put(invoice.id()));

            crmResponse.put(InvoiceFields.UPDATED_RECORDS, updatedIds.size());
            crmResponse.put(InvoiceFields.INVOICES, updatedIds);
            crmResponse.put(InvoiceFields.MESSAGE, crmResponse.resolveI18nString("i18n.invoice.update.success"));
        }
    }

    /**
     * <p>
     * Updates the status of an invoice identified by the given id.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}.
     * @param crmResponse Represents the {@link CrmResponse}.
     * @param data        Represents the request data containing the invoice id and status.
     */
    private void updateStatus(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final long orgId = crmRequest.orgId();
        final long id = data.getLong(InvoiceFields.ID);
        final Invoice.Status statusToUpdate = Invoice.Status.get(data.getShort(InvoiceFields.STATUS));

        try {
            final Invoice invoice = invoiceManager.get(orgId, id);

            invoiceManager.updateStatus(invoice, crmRequest.userId(), statusToUpdate);
            final String statusLabel = crmResponse.resolveI18nString(statusToUpdate.getI18nKey());
            final String template = crmResponse.resolveI18nString("i18n.invoice.status.update.success");

            crmResponse.put(InvoiceFields.MESSAGE, String.format(template, statusLabel));
        } catch (final Exception exception) {
            sendError(crmResponse, null, ErrorCode.get(InvoiceErrorCodes.INVOICE_UPDATE_FAILED, "i18n.invoice.update.failed", exception));
        }
    }

    /**
     * <p>
     * Populates the IAM {@link User} details into the sales owners associated
     * with the invoice entities.
     * </p>
     *
     * @param invoice Represents the {@link Invoice}.
     * @param users   Represents the users fetched from the IAM service.
     */
    private void populateSalesOwnerUsers(final Invoice invoice, final Collection<User> users) {
        if (users.isEmpty()) {
            return;
        }
        final Map<Long, User> usersById = users.stream().collect(Collectors.toMap(User::id, user -> user));
        final Contact contact = invoice.getContact();
        final Company company = invoice.getCompany();
        final Deal deal = invoice.getDeal();

        final CrmUser contactSalesOwner = Objects.nonNull(contact) ? contact.getSalesOwner() : null;
        final CrmUser companySalesOwner = Objects.nonNull(company) ? company.getSalesOwner() : null;
        final CrmUser dealSalesOwner = Objects.nonNull(deal) ? deal.getSalesOwner() : null;

        populateUser(contactSalesOwner, usersById);
        populateUser(companySalesOwner, usersById);
        populateUser(dealSalesOwner, usersById);
        populateUser(invoice.getOwner(), usersById);
    }

    /**
     * <p>
     * Sets the matching IAM user on the given CRM user.
     * </p>
     *
     * @param crmUser   Represents the CRM sales owner.
     * @param usersById Represents IAM users indexed by their IAM ID.
     */
    private void populateUser(final CrmUser crmUser, final Map<Long, User> usersById) {
        if (Objects.nonNull(crmUser)) {
            crmUser.setUser(usersById.get(crmUser.getIamId()));
        }
    }

    /**
     * <p>
     * Collects the IAM user IDs of the sales owners associated with the
     * invoice contact, company, deal, and invoice owner.
     * </p>
     *
     * @param invoice Represents the {@link Invoice}.
     * @return IAM user IDs associated with the invoice entities.
     */
    private Collection<Long> getEntityUserIds(final Invoice invoice) {
        final Contact contact = invoice.getContact();
        final Deal deal = invoice.getDeal();
        final Company company = invoice.getCompany();
        final CrmUser owner = invoice.getOwner();

        final Collection<Long> entityUserIds = new HashSet<>();

        if (Objects.nonNull(company)) {
            final CrmUser companySalesOwner = company.getSalesOwner();

            if (Objects.nonNull(companySalesOwner)) {
                entityUserIds.add(companySalesOwner.getIamId());
            }
        }

        if (Objects.nonNull(deal)) {
            final CrmUser dealSalesOwner = deal.getSalesOwner();

            if (Objects.nonNull(dealSalesOwner)) {
                entityUserIds.add(dealSalesOwner.getIamId());
            }
        }

        if (Objects.nonNull(contact)) {
            final CrmUser contactSalesOwner = contact.getSalesOwner();

            if (Objects.nonNull(contactSalesOwner)) {
                entityUserIds.add(contactSalesOwner.getIamId());
            }
        }

        if (Objects.nonNull(owner)) {
            entityUserIds.add(owner.getIamId());
        }

        return entityUserIds;
    }

    /**
     * <p>
     * Validates a single {@link Invoice} against the specified validation group.
     * </p>
     *
     * @param invoice     Represents the {@link Invoice} to validate.
     * @param group       Represents the validation group.
     * @param errorCode   Represents the error code returned when validation fails.
     * @param crmResponse Represents the {@link CrmResponse}.
     * @return {@code true} if validation succeeds; {@code false} otherwise.
     */
    private boolean validate(final Invoice invoice, final Class<?> group, final ErrorCode errorCode, final CrmResponse crmResponse) {
        return validate(List.of(invoice), group, errorCode, crmResponse);
    }

    /**
     * <p>
     * Validates a collection of {@link Invoice} entities against the specified
     * validation group.
     * </p>
     *
     * @param invoices    Represents the invoices to validate.
     * @param group       Represents the validation group.
     * @param errorCode   Represents the error code returned when validation fails.
     * @param crmResponse Represents the {@link CrmResponse}.
     * @return {@code true} if all invoices are valid; {@code false} otherwise.
     */
    private boolean validate(final Collection<Invoice> invoices, final Class<?> group, final ErrorCode errorCode, final CrmResponse crmResponse) {
        final Collection<ErrorDescriptor> errors = new ArrayList<>(modelValidator.validate(invoices, group));

        validateContactEmail(invoices, group, errors);
        validateEntityAccess(invoices, group, errors);

        if (errors.isEmpty()) {
            return true;
        }
        crmResponse.setErrorCode(errorCode);
        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));

        return false;
    }

    /**
     * <p>
     * Validates that the contact associated with each invoice has at least one
     * email address configured.
     * </p>
     *
     * @param invoices Collection of invoices whose contact email details are to be validated.
     * @param group    Validation group representing the current operation.
     * @param errors   Collection to which validation errors are added.
     */
    private void validateContactEmail(final Collection<Invoice> invoices, final Class<?> group, final Collection<ErrorDescriptor> errors) {
        if (!Group.Create.class.equals(group) && !Group.Update.class.equals(group)) {
            return;
        }

        for (final Invoice invoice : invoices) {

            if (Objects.isNull(invoice)) {
                continue;
            }
            final Contact contact = invoice.getContact();

            if (Objects.isNull(contact)) {
                continue;
            }

            if (Objects.isNull(contact.getPrimaryEmail())) {
                errors.add(new ErrorDescriptor(
                        ErrorCode.get(CommonErrorCodes.DATA_VALIDATION_FAILED, "i18n.invoice.contact.email.required")
                ));
            }
        }
    }

    /**
     * <p>
     * Validates contact access for invoice create using deal primary contact IDs from the database,
     * not {@link Deal#getPrimaryContact()}, which may be null when the user lacks contact access.
     * </p>
     *
     * @param invoices Collection of invoices to validate.
     * @param group    Validation group representing the current operation.
     * @param errors   Collection to which validation errors are added.
     */
    private void validateEntityAccess(final Collection<Invoice> invoices, final Class<?> group,
                                      final Collection<ErrorDescriptor> errors) {
        if (!Group.Create.class.equals(group)) {
            return;
        }

        final Map<Long, Map<Long, Map<Long, List<Invoice>>>> invoicesByOwner = invoices.stream()
                .filter(Objects::nonNull)
                .filter(invoice -> Objects.nonNull(invoice.getOwner()))
                .filter(invoice -> Objects.nonNull(resolveContactIdForAccessCheck(invoice)))
                .collect(Collectors.groupingBy(
                        Invoice::orgId,
                        Collectors.groupingBy(
                                invoice -> invoice.getOwner().id(),
                                Collectors.groupingBy(invoice -> invoice.getOwner().getRoleId())
                        )
                ));

        for (final Map.Entry<Long, Map<Long, Map<Long, List<Invoice>>>> orgEntry : invoicesByOwner.entrySet()) {
            final long orgId = orgEntry.getKey();

            for (final Map.Entry<Long, Map<Long, List<Invoice>>> userEntry : orgEntry.getValue().entrySet()) {
                final long userId = userEntry.getKey();

                for (final Map.Entry<Long, List<Invoice>> roleEntry : userEntry.getValue().entrySet()) {
                    final long roleId = roleEntry.getKey();
                    final List<Invoice> ownerInvoices = roleEntry.getValue();

                    final Collection<Long> contactIds = ownerInvoices.stream()
                            .map(this::resolveContactIdForAccessCheck)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toCollection(HashSet::new));

                    if (contactIds.isEmpty()) {
                        continue;
                    }

                    if (!hasContactAccess(orgId, userId, roleId, contactIds)) {
                        ownerInvoices.forEach(invoice -> errors.add(new ErrorDescriptor(ErrorCode.get(
                                CommonErrorCodes.DATA_VALIDATION_FAILED, I18N_CREATE_DISABLED_ENTITY_ACCESS))));
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Resolves the contact ID to use for create access validation.
     * so validation still runs when {@link Deal#getPrimaryContact()} is null due to restricted access.
     * </p>
     *
     * @param invoice Represents the invoice being created.
     * @return The contact ID to validate, or {@code null} when none applies.
     */
    private Long resolveContactIdForAccessCheck(final Invoice invoice) {
        final Deal deal = invoice.getDeal();

        if (Objects.nonNull(deal) && Objects.nonNull(deal.id())) {
            return dealManager.getPrimaryContactId(invoice.orgId(), deal.id());
        }

        return null;
    }

    /**
     * <p>
     * Determines whether the user has access to every contact in the given set of contact IDs.
     * </p>
     *
     * @param orgId      Represents the organization ID.
     * @param userId     Represents the ID of the user whose access is being checked.
     * @param roleId     Represents the role ID used to resolve permission scope.
     * @param contactIds Represents the contact IDs to check access for.
     * @return {@code true} when the user can access every contact in the set, {@code false} otherwise.
     */
    private boolean hasContactAccess(final long orgId, final long userId, final long roleId, final Collection<Long> contactIds) {
        final RolePermission permission = RolePermissionService.getInstance().getPermission(orgId, roleId, Category.Type.SOURCES,
                SubCategory.Type.CONTACTS, Action.Type.ACCESS);

        if (Objects.isNull(permission)) {
            return false;
        }
        return new EntityPermissionValidator(orgId, userId, roleId).validate(Set.copyOf(contactIds), StoreType.CONTACT, permission.getScope(), false);
    }

    /**
     * <p>
     * Uploads the given file as an attachment for the specified organization.
     * </p>
     *
     * @param orgId Organization identifier associated with the attachment.
     * @param file  File to upload.
     * @return Uploaded attachment details.
     */
    private Attachment uploadFile(final long orgId, final File file) {
        if (file.size() > MAX_INVOICE_FILE_SIZE_BYTES) {
            throw ErrorCode.get(InvoiceErrorCodes.INVOICE_FILE_TOO_LARGE, "i18n.invoice.file.too.large");
        }
        final Long iamOrgId = organizationService.getIamOrgId(orgId);
        final Attachment attachment = new ObjectAttachment();

        attachment.setInputStream(file.inputStream());
        attachment.setMediaType(file.mediaType());
        attachment.setExtension(file.extension());
        attachment.setFileName(file.name());
        attachment.setFileSize(file.size());

        attachmentService.upload(iamOrgId, List.of(attachment), FileCategory.INVOICE.getName());

        return attachment;
    }
}