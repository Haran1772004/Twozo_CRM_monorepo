package com.tlc.crm.core.features.invoice.model.converter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.converter.CrmModelConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.features.invoice.model.InvoiceLineItem;
import com.tlc.crm.core.features.invoice.service.InvoiceService;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;
import com.tlc.crm.core.features.invoice.manager.InvoiceManager;
import com.tlc.crm.core.features.invoice.manager.InvoiceManagerImpl;
import com.tlc.crm.core.features.invoice.model.Invoice;
import com.tlc.crm.core.features.invoice.model.InvoiceTemplate;
import com.tlc.crm.core.features.invoice.model.InvoiceType;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.core.internal.invoice.status.InvoiceErrorCodes;
import com.tlc.crm.entity.currency.api.manager.BaseCurrencyManager;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.role.permission.validator.EntityPermissionValidator;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.file.client.converter.AttachmentConverter;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.l10n.temporal.model.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>
 * Converts between {@link Invoice} models and their corresponding
 * {@link JsonObject} representations.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class InvoiceConverter extends CrmModelConverter<Invoice, JsonObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceConverter.class);

    private final InvoiceManager invoiceManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;
    private final UserService userService;
    private final BaseCurrencyManager baseCurrencyManager;
    private final RolePermissionService rolePermissionService;
    private final InvoiceService invoiceService;
    private final AttachmentService attachmentService;
    private final AttachmentConverter attachmentConverter;

    private InvoiceConverter() {
        this.invoiceManager = InvoiceManagerImpl.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.userService = UserService.getInstance();
        this.baseCurrencyManager = BaseCurrencyManager.getInstance();
        this.rolePermissionService = RolePermissionService.getInstance();
        this.invoiceService = InvoiceService.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        this.attachmentConverter = AttachmentConverter.getInstance();
    }

    /**
     * <p>
     * Holds the singleton instance of {@link InvoiceConverter}.
     * </p>
     */
    private static final class InstanceHolder {

        private static final InvoiceConverter INVOICE_CONVERTER = new InvoiceConverter();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link InvoiceConverter}.
     * </p>
     *
     * @return The {@link InvoiceConverter} instance.
     */
    public static InvoiceConverter getInstance() {
        return InstanceHolder.INVOICE_CONVERTER;
    }

    /**
     * <p>
     * Converts the supplied converter data into an {@link Invoice} model.
     * </p>
     *
     * @param converterData Represents the data required for invoice conversion.
     * @return The converted {@link Invoice} model, or {@code null} when the
     * conversion is performed for an update action.
     */
    @Override
    public Invoice convertToModel(final CrmConverterData<JsonObject> converterData) {
        final JsonObject data = converterData.dataToBeConverted();
        final long orgId = converterData.orgId();
        final CrmUser crmUser = userService.getUser(orgId, converterData.getUser().id());

        if (converterData.isUpdateAction()) {
            final long invoiceId = data.getLong(InvoiceFields.ID);
            final Invoice invoice = invoiceManager.get(orgId, invoiceId);

            if (data.containsKey(InvoiceFields.TEMPLATE_ID)) {
                invoice.setTemplateId(data.getLong(InvoiceFields.TEMPLATE_ID));
            }

            if (data.containsKey(InvoiceFields.STATUS)) {
                invoice.setStatus(Invoice.Status.get(data.getShort(InvoiceFields.STATUS)));
            }

            if (data.containsKey(InvoiceFields.INVOICE_JSON)) {
                final JsonObject invoiceJson = data.getJsonObject(InvoiceFields.INVOICE_JSON);

                invoice.setInvoiceJson(invoiceJson);
                loadEntityInfoIntoInvoice(invoiceJson, invoice, crmUser);
            }

            return invoice;
        }
        final Optional<InvoiceTemplate> optionalDefaultTemplate = invoiceManager.getDefaultTemplate(orgId);
        final InvoiceTemplate invoiceTemplate;

        if (optionalDefaultTemplate.isEmpty()) {
            final Collection<InvoiceTemplate> defaultTemplates = invoiceManager.getDefaultTemplates();
            final Supplier<ErrorCode> errorCodeProvider = () -> ErrorCode.get(InvoiceErrorCodes.INVOICE_TEMPLATE_NOT_FOUND,
                    "No default invoice template found in the system for organization " + orgId);

            if (defaultTemplates.isEmpty()) {
                LOGGER.warn("No default invoice template found in the system for organization {}", orgId);
                throw errorCodeProvider.get();
            }
            final Collection<Long> templateIds = defaultTemplates.stream().map(InvoiceTemplate::id)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (templateIds.isEmpty()) {
                throw errorCodeProvider.get();
            }
            invoiceManager.createDefaultTemplateMapping(orgId, crmUser.id(), templateIds);

            invoiceTemplate = invoiceManager.getDefaultTemplate(orgId).orElseThrow(errorCodeProvider);
        } else {
            invoiceTemplate = optionalDefaultTemplate.get();
        }
        final Invoice invoice = new Invoice();

        invoice.setOrgId(orgId);
        invoice.setCurrency(baseCurrencyManager.getBaseCurrency(orgId).getDefaultCurrency());
        invoice.setTemplateId(data.optLong(InvoiceFields.TEMPLATE_ID, invoiceTemplate.id()));
        invoice.setInvoiceType(InvoiceType.get(data.optShort(InvoiceFields.INVOICE_TYPE, InvoiceType.ONE_TIME_INVOICE.getValue())));
        invoice.setStatus(Invoice.Status.DRAFT);
        invoice.setOwner(crmUser);
        invoice.setCreatedBy(crmUser);
        invoice.setNumber(invoiceManager.generateInvoiceNumber(orgId));
        invoice.setSchemaJson(invoiceTemplate.getSchemaJson());
        final Date currentTime = new Date(Instant.now().toEpochMilli());

        invoice.setIssueAt(currentTime);
        invoice.setDueBy(currentTime);
        final String sourceKey = FieldKeys.SOURCE.getName();
        final String entityIdKey = FieldKeys.ENTITY_ID.getName();

        if (data.containsKey(sourceKey) && data.containsKey(entityIdKey)) {
            final Source source = Source.get(data.getString(sourceKey));
            final Long entityId = data.getLong(entityIdKey);

            switch (source) {
                case DEAL -> {
                    if (!dealManager.exists(orgId, entityId) || !canAccessEntity(orgId, crmUser.id(),
                            crmUser.getRoleId(), entityId, StoreType.DEAL, SubCategory.Type.DEALS)) {
                        break;
                    }
                    final Deal deal = dealManager.get(orgId, entityId);

                    invoice.setDeal(deal);
                    invoice.setCompany(buildCompany(orgId, crmUser, Optional.ofNullable(deal.getCompany()).map(Company::id).orElse(null)));
                    invoice.setContact(buildContact(orgId, crmUser, Optional.ofNullable(deal.getPrimaryContact()).map(Contact::id).orElse(null)));
                    final ProductDetailsHolder productDetailsHolder;
                    final Collection<ProductDetail> productDetails;

                    if (Objects.nonNull(productDetailsHolder = deal.getProductDetailsHolder())
                            && Objects.equals(productDetailsHolder.getCurrency(), invoice.getCurrency()) &&
                            Objects.nonNull(productDetails = productDetailsHolder.getProductDetails())) {
                        invoice.setInvoiceLineItems(
                                productDetails
                                        .stream()
                                        .filter(Objects::nonNull)
                                        .filter(productDetail -> Objects.nonNull(productDetail.getProduct())
                                                && Objects.nonNull(productDetail.getProduct().id()))
                                        .map(productDetail -> {
                                            final InvoiceLineItem invoiceLineItem = new InvoiceLineItem();
                                            final Product product = productDetail.getProduct();

                                            invoiceLineItem.setProductId(product.id());
                                            invoiceLineItem.setAmount(productDetail.getAmount());

                                            return invoiceLineItem;
                                        })
                                        .collect(Collectors.toSet()));
                    }
                }
                case COMPANY -> invoice.setCompany(buildCompany(orgId, crmUser, entityId));
                case CONTACT -> {
                    final Contact contact = buildContact(orgId, crmUser, entityId);

                    invoice.setContact(contact);
                    invoice.setCompany(buildCompany(orgId, crmUser, Optional.ofNullable(contact).map(Contact::getCompany).map(Company::id).orElse(null)));
                }
                default -> {
                    // No action for unsupported source types.
                }
            }
        }

        return invoice;
    }

    /**
     * <p>
     * Loads the company information associated with the given company ID.
     * </p>
     *
     * @param orgId     the organization ID
     * @param companyId the company ID
     * @return the company domain model, or {@code null} if the company ID is null
     */
    private Company buildCompany(final long orgId, final CrmUser user, final Long companyId) {
        if (Objects.isNull(companyId)) {
            return null;
        }

        if (!companyManager.exists(orgId, companyId)) {
            return null;
        }

        if (!canAccessEntity(orgId, user.id(), user.getRoleId(), companyId, StoreType.COMPANY, SubCategory.Type.COMPANIES)) {
            return null;
        }
        return companyManager.get(orgId, companyId);
    }

    /**
     * <p>
     * Loads the contact information associated with the given contact ID when the user has access.
     * </p>
     *
     * @param orgId     the organization ID
     * @param user      the current user
     * @param contactId the contact ID
     * @return the contact domain model, or {@code null} if the contact ID is null or not accessible
     */
    private Contact buildContact(final long orgId, final CrmUser user, final Long contactId) {
        if (Objects.isNull(contactId)) {
            return null;
        }

        if (!contactManager.exists(orgId, contactId)) {
            return null;
        }

        if (!canAccessEntity(orgId, user.id(), user.getRoleId(), contactId, StoreType.CONTACT, SubCategory.Type.CONTACTS)) {
            return null;
        }
        return contactManager.partialGet(orgId, Set.of(contactId)).stream().findFirst().orElse(null);
    }

    /**
     * <p>
     * Validates that the current user may access the given entity id under the sources' module.
     * </p>
     */
    private boolean canAccessEntity(final long orgId, final long userId, final long roleId, final Long entityId,
                                    final StoreType storeType, final SubCategory.Type subCategoryType) {
        if (Objects.isNull(entityId)) {
            return false;
        }
        final RolePermission permission = rolePermissionService.getPermission(orgId, roleId, Category.Type.SOURCES, subCategoryType, Action.Type.ACCESS);

        if (Objects.isNull(permission)) {
            return false;
        }

        return new EntityPermissionValidator(orgId, userId, roleId).validate(Set.of(entityId), storeType, permission.getScope(), false);
    }

    /**
     * <p>
     * Extracts and loads company, contact, and product information from the invoice JSON
     * into the given {@link Invoice} domain model.
     * </p>
     *
     * @param invoiceJson the JSON object representing the full invoice data
     * @param invoice     the {@link Invoice} domain model to populate
     * @param user        the user performing the operation
     */
    private void loadEntityInfoIntoInvoice(final JsonObject invoiceJson, final Invoice invoice, final CrmUser user) {
        if (Objects.isNull(invoiceJson)) {
            return;
        }
        final JsonArray formAttributes = invoiceJson.getJsonArray(InvoiceFields.FORM_ATTRIBUTES);

        if (Objects.isNull(formAttributes)) {
            return;
        }

        formAttributes.forEach(formAttribute -> {
            final JsonObject formAttributeJson = toJsonObject(formAttribute);

            if (Objects.isNull(formAttributeJson)) {
                return;
            }
            final String attributeType = formAttributeJson.getString(InvoiceFields.TYPE);

            if (Objects.equals(attributeType, InvoiceFields.PARTIES)) {
                loadCompanyAndContact(formAttributeJson, invoice, user);
            } else if (Objects.equals(attributeType, InvoiceFields.LINE_ITEMS)) {
                loadProducts(formAttributeJson, invoice);
            }
        });
    }

    /**
     * <p>
     * Resolves the buyer company and buyer contact out of the {@code PARTIES} section's
     * {@code buyerDetails} array and sets them on the invoice.
     * </p>
     *
     * @param partiesJson the {@code PARTIES} form-attribute JSON object
     * @param invoice     the {@link Invoice} domain model to populate
     */
    private void loadCompanyAndContact(final JsonObject partiesJson, final Invoice invoice, final CrmUser user) {
        final JsonArray buyerDetails = partiesJson.getJsonArray(InvoiceFields.BUYER_DETAILS);
        final Long orgId = invoice.orgId();

        if (Objects.isNull(buyerDetails)) {
            return;
        }

        buyerDetails.forEach(buyerDetail -> {
            final JsonObject buyerDetailJson = toJsonObject(buyerDetail);

            if (Objects.isNull(buyerDetailJson)) {
                return;
            }
            final String detailType = buyerDetailJson.getString(InvoiceFields.TYPE);

            if (Objects.equals(detailType, InvoiceFields.BUYER_COMPANY)) {
                final Optional<Long> optionalCompanyId = extractEntityId(buyerDetailJson, InvoiceFields.COMPANY.toLowerCase());
                final Long companyId;

                if (optionalCompanyId.isPresent() && companyManager.exists(orgId, companyId = optionalCompanyId.get())
                        && canAccessEntity(orgId, user.id(), user.getRoleId(), companyId, StoreType.COMPANY, SubCategory.Type.COMPANIES)) {
                    final Company company = new Company();

                    company.setId(companyId);
                    invoice.setCompany(company);

                } else {
                    invoice.setCompany(null);
                }
            } else if (Objects.equals(detailType, InvoiceFields.BUYER_CONTACT)) {
                final Optional<Long> contactId = extractEntityId(buyerDetailJson, InvoiceFields.CONTACT.toLowerCase());

                if (contactId.isPresent()) {
                    invoice.setContact(buildContact(orgId, user, contactId.get()));
                } else {
                    invoice.setContact(null);
                }
            }
        });
    }

    /**
     * <p>
     * Resolves the product ids out of the {@code LINE_ITEMS} section's {@code items} array
     * (specifically the {@code PRODUCTS} entry) and sets them on the invoice.
     * </p>
     *
     * @param lineItemsJson the {@code LINE_ITEMS} form-attribute JSON object
     * @param invoice       the {@link Invoice} domain model to populate
     */
    private void loadProducts(final JsonObject lineItemsJson, final Invoice invoice) {
        final JsonArray items = lineItemsJson.getJsonArray(InvoiceFields.ITEMS);

        if (Objects.isNull(items)) {
            return;
        }

        items.forEach(item -> {
            final JsonObject itemJson = toJsonObject(item);

            if (Objects.nonNull(itemJson) && Objects.equals(itemJson.getString(InvoiceFields.TYPE), InvoiceFields.PRODUCTS)) {
                final JsonArray values = itemJson.getJsonArray(InvoiceFields.VALUES);

                if (Objects.isNull(values)) {
                    return;
                }
                final Collection<InvoiceLineItem> invoiceLineItems = new LinkedHashSet<>();

                values.forEach(value -> {
                    final JsonObject valueJson = toJsonObject(value);

                    if (Objects.nonNull(valueJson) && valueJson.containsKey(InvoiceFields.VALUE)) {
                        final InvoiceLineItem invoiceLineItem = new InvoiceLineItem();

                        invoiceLineItem.setProductId(valueJson.getLong(InvoiceFields.VALUE));

                        if (valueJson.containsKey(InvoiceFields.AMOUNT)) {
                            invoiceLineItem.setAmount(valueJson.getDouble(InvoiceFields.AMOUNT));
                        }

                        invoiceLineItems.add(invoiceLineItem);
                    }
                });

                if (!invoiceLineItems.isEmpty()) {
                    invoice.setInvoiceLineItems(invoiceLineItems);
                }
            }
        });
    }

    /**
     * <p>
     * Safely extracts a nested entity id (e.g. company/contact id) from a field that is
     * expected to be a {@link JsonObject} holding a {@code value} key.
     * </p>
     *
     * @param json the parent JSON object (a buyer detail entry)
     * @param key  the field name to inspect (e.g. {@code company}, {@code contact})
     * @return the resolved id, or empty if the field is absent, empty, or not a valid id
     */
    private Optional<Long> extractEntityId(final JsonObject json, final String key) {
        if (!json.containsKey(key)) {
            return Optional.empty();
        }
        final JsonObject entityJson = toJsonObject(json.get(key));

        if (Objects.isNull(entityJson) || !entityJson.containsKey(InvoiceFields.VALUE)) {
            return Optional.empty();
        }
        return Optional.of(entityJson.getLong(InvoiceFields.VALUE));
    }

    /**
     * <p>
     * Normalizes an array element that may come back as either a {@link JsonObject} or a raw
     * {@link Map} (as returned by some {@code JsonArray} implementations) into a
     * {@link JsonObject}.
     * </p>
     *
     * @param element the raw array element
     * @return the element as a {@link JsonObject}, or {@code null} if it is neither type
     */
    private JsonObject toJsonObject(final Object element) {
        if (element instanceof JsonObject jsonObject) {
            return jsonObject;
        } else if (element instanceof Map<?, ?> map) {
            return Json.object((Map) map);
        }
        return null;
    }

    /**
     * <p>
     * Converts the supplied invoice converter data into a JSON representation.
     * </p>
     *
     * @param converterData Represents the data required for invoice conversion.
     * @return The JSON representation of the invoice.
     */
    @Override
    public JsonObject convertToJson(final CrmConverterData<Invoice> converterData) {
        final Invoice invoice = converterData.dataToBeConverted();
        final JsonObject json = Json.object();
        final Invoice.Status status = invoice.getStatus();
        final Long orgId = converterData.orgId();
        final Long pdfFileId = invoice.getPdfFileId();

        json.put(InvoiceFields.ID, invoice.id());
        json.put(InvoiceFields.NUMBER, invoice.getFullyQualifiedNumber());
        json.put(InvoiceFields.TEMPLATE_ID, invoice.getTemplateId());
        json.put(InvoiceFields.STATUS, Objects.isNull(status) ? null : status.getId());
        json.put(InvoiceFields.INVOICE_JSON, invoice.getInvoiceJson());

        if (Objects.nonNull(pdfFileId)) {
            json.put(InvoiceFields.PDF_FILE, getAttachmentJson(pdfFileId));
        }

        if (Objects.nonNull(invoice.getOwner())) {
            json.put(InvoiceFields.OWNER, Json.object()
                    .put(InvoiceFields.ID, invoice.getOwner().id())
                    .put(FieldKeys.NAME.getName(), invoice.getOwner().getFullName()));
        }
        final CrmUser user = converterData.getUser();
        final long userId = user.id();
        final long roleId = user.getRoleId();
        final Map<Source, Collection<Long>> entityIdsBySource = new EnumMap<>(Source.class);
        final Contact contact = invoice.getContact();

        if (Objects.nonNull(contact) && Objects.nonNull(contact.id())) {
            entityIdsBySource.computeIfAbsent(Source.CONTACT, source -> new HashSet<>()).add(contact.id());
        }
        final Company company = invoice.getCompany();

        if (Objects.nonNull(company) && Objects.nonNull(company.id())) {
            entityIdsBySource.computeIfAbsent(Source.COMPANY, source -> new HashSet<>()).add(company.id());
        }
        final Deal deal = invoice.getDeal();

        if (Objects.nonNull(deal) && Objects.nonNull(deal.id())) {
            entityIdsBySource.computeIfAbsent(Source.DEAL, source -> new HashSet<>()).add(deal.id());
        }
        final boolean entityAccessRestricted = invoiceService.isEntityAccessRestricted(orgId, userId, roleId, entityIdsBySource);

        if (Objects.nonNull(contact)) {
            final Long contactId = contact.id();

            if (canAccessEntity(orgId, userId, roleId, contactId, StoreType.CONTACT, SubCategory.Type.CONTACTS)) {
                final Contact fullContact = contactManager.partialGet(orgId, Set.of(contactId))
                        .stream().findFirst().orElse(null);

                if (Objects.nonNull(fullContact)) {
                    final JsonObject contactJson = Json.object();
                    final Email email = fullContact.getPrimaryEmail();

                    contactJson.put(InvoiceFields.ID, contactId);
                    contactJson.put(InvoiceFields.NAME, fullContact.getFullName());

                    if (Objects.nonNull(email)) {
                        contactJson.put(InvoiceFields.EMAIL, Json.object()
                                .put(InvoiceFields.ID, email.id())
                                .put(InvoiceFields.VALUE, email.getValue()));
                    }

                    json.put(InvoiceFields.CONTACT, contactJson);
                }
            }
        }

        if (Objects.nonNull(company)) {
            final Long companyId = company.id();

            if (canAccessEntity(orgId, userId, roleId, companyId, StoreType.COMPANY, SubCategory.Type.COMPANIES)) {
                json.put(InvoiceFields.COMPANY_ID, companyId);
            }
        }
        invoiceService.appendActionConstraints(json, status, entityAccessRestricted);

        return json;
    }

    /**
     * <p>
     * Retrieves the attachment for the supplied attachment id and converts it
     * into its JSON representation. If no attachment is found, an empty JSON
     * object is returned.
     * </p>
     *
     * @param attachmentId Represents the id of the attachment to be retrieved.
     * @return The JSON representation of the attachment, or an empty JSON object if none exists.
     */
    private JsonObject getAttachmentJson(final Long attachmentId) {
        return attachmentService.getAttachments(List.of(attachmentId)).stream()
                .findFirst()
                .map(attachmentConverter::convert)
                .orElseGet(Json::object);
    }

    /**
     * <p>
     * Converts the supplied company data into a JSON representation containing
     * the company details along with its formatted address and address JSON.
     * </p>
     *
     * @param company Represents the {@link Company} data to be converted.
     * @return The JSON representation of the company.
     */
    public JsonObject convertInvoiceCompanyDataToJson(final Company company) {
        return CompanyConverter.getInstance().convertCompanyWithAddress(false, company);
    }

    /**
     * <p>
     * Converts the supplied contact data into a JSON representation containing
     * the contact details along with the associated company details.
     * </p>
     *
     * @param contact    Represents the {@link Contact} data to be converted.
     * @param companyMap Represents the {@link Company} data associated with the contact.
     * @return The JSON representation of the contact.
     */
    public JsonObject convertInvoiceContactDataToJson(final Contact contact, final Map<Long, Company> companyMap) {
        final JsonObject jsonObject = ContactConverter.getInstance().convertContactWithEmailAndPhone(contact);
        final Company company = Optional.ofNullable(contact.getCompany())
                .map(Company::id)
                .map(companyMap::get)
                .orElse(null);

        if (Objects.nonNull(company)) {
            jsonObject.put(InvoiceFields.COMPANY, convertInvoiceCompanyDataToJson(company));
        }

        return jsonObject;
    }
}