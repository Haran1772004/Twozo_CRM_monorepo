package com.tlc.crm.core.features.invoice.manager;

import com.tlc.cache.remote.CacheService;
import com.tlc.cache.remote.DistributedAtomicCounter;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.features.invoice.model.Invoice;
import com.tlc.crm.core.features.invoice.model.InvoiceLineItem;
import com.tlc.crm.core.features.invoice.model.InvoiceTemplate;

import com.tlc.crm.core.features.invoice.model.InvoiceType;
import com.tlc.crm.core.features.invoice.timeline.InvoiceTimelineManager;
import com.tlc.crm.core.internal.invoice.status.InvoiceErrorCodes;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MIVINVOICETEMPLATE;
import com.tlc.crm.core.sql.resource.MPOPRODUCT;
import com.tlc.crm.core.sql.resource.OMIVINVOICE;
import com.tlc.crm.core.sql.resource.OMIVINVOICEEMAILCONVERSATION;
import com.tlc.crm.core.sql.resource.OMIVINVOICETEMPLATE;
import com.tlc.crm.core.sql.resource.OMIVINVOICELINEITEMS;

import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Provides the {@link Invoice} related DB actions, covering full CRUD, product
 * association, email-conversation linking, and clone operations.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class InvoiceManagerImpl implements InvoiceManager {

    private static final String INVOICE_NUMBER_COUNTER_KEY_FORMAT = "invoice:number:%d";

    private final UserService userService;
    private final AdminDataStore adminDataStore;
    private final CacheService cacheService;
    private final ContactManager contactManager;
    private final EmailManager emailManager;
    private final ProductManager productManager;
    private final InvoiceTimelineManager invoiceTimelineManager;

    private static class Instance {
        private static final InvoiceManagerImpl INSTANCE = new InvoiceManagerImpl();
    }

    private InvoiceManagerImpl() {
        userService = UserService.getInstance();
        adminDataStore = DataStoreProvider.getAdminDataStore();
        cacheService = CacheServiceProvider.get();
        contactManager = ContactManager.getInstance();
        emailManager = EmailManager.getInstance();
        productManager = ProductManager.getInstance();
        invoiceTimelineManager = InvoiceTimelineManager.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link InvoiceManagerImpl}.
     * </p>
     *
     * @return {@link InvoiceManagerImpl}.
     */
    public static InvoiceManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return OMIVInvoice table.
     */
    @Override
    public Table getTable() {
        return Table.get(OMIVINVOICE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return OMIVINVOICELINEITEMS table.
     */
    @Override
    public Table getInvoiceLineItemsTable() {
        return Table.get(OMIVINVOICELINEITEMS.TABLE);
    }

    /**
     * <p>
     * Gets the invoice email-conversation mapping table.
     * </p>
     *
     * @return {@link Table} representing invoice-to-conversation links.
     */
    private Table getInvoiceEmailConversationTable() {
        return Table.get(OMIVINVOICEEMAILCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the organization-specific invoice template mapping table.
     * </p>
     *
     * @return {@link Table} representing the org-to-template mapping.
     */
    private Table getOrgInvoiceTemplateTable() {
        return Table.get(OMIVINVOICETEMPLATE.TABLE);
    }

    /**
     * <p>
     * Gets the master invoice template table.
     * </p>
     *
     * @return {@link Table} representing the global invoice templates.
     */
    private Table getInvoiceTemplateTable() {
        return Table.get(MIVINVOICETEMPLATE.TABLE);
    }

    /**
     * <p>
     * Gets the secure organization-scoped data store for the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return {@link OrgDataStore} instance.
     */
    private OrgDataStore secureOrgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the organization-scoped data store for the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return {@link OrgDataStore} instance.
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param invoice Holds the {@link Invoice} attributes.
     */
    @Override
    public void create(final Invoice invoice) {
        bulkCreate(invoice.orgId(), List.of(invoice));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id.
     * @param invoices Holds the collection of {@link Invoice} attributes to create.
     */
    @Override
    public void bulkCreate(final long orgId, final Collection<Invoice> invoices) {
        if (invoices.isEmpty()) {
            return;
        }
        final DataContainer dataContainer = DataContainer.create();

        for (final Invoice invoice : invoices) {
            loadRowIntoContainer(true, invoice, null, dataContainer);
        }

        secureOrgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param id    Represents the invoice id.
     * @return {@link Invoice} that holds the retrieved data.
     */
    @Override
    public Invoice get(final long orgId, final long id) {
        return get(orgId, Set.of(id)).stream().findAny()
                .orElseThrow(() -> ErrorCode.get(InvoiceErrorCodes.INVOICE_NOT_FOUND, "i18n.invoice.not.found"));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Which contains the collection of invoice ids.
     * @return Collection of {@link Invoice} that hold the retrieved data.
     */
    @Override
    public Collection<Invoice> get(final long orgId, final Collection<Long> ids) {
        final Table invoiceTable = getTable();
        final SelectQuery selectQuery = buildSelectQuery(
                new WhereClause(Criteria.in(invoiceTable.getPKColumn(), ids)));
        final DataContainer dataContainer = secureOrgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(invoiceTable).map(row -> convertRowToInvoice(row, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param invoice Holds the {@link Invoice} attributes.
     */
    @Override
    public void update(final Invoice invoice) {
        bulkUpdate(invoice.orgId(), Set.of(invoice));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id.
     * @param invoices Holds the collection of {@link Invoice} attributes to update.
     */
    @Override
    public void bulkUpdate(final long orgId, final Collection<Invoice> invoices) {
        if (invoices.isEmpty()) {
            return;
        }
        final Table invoiceTable = getTable();
        final OrgDataStore orgDataStore = secureOrgDataStore(orgId);

        final Collection<Long> invoiceIds = invoices.stream()
                .map(Invoice::id)
                .collect(Collectors.toList());
        final SelectQuery selectQuery = buildSelectQuery(new WhereClause(Criteria.in(invoiceTable.getPKColumn(), invoiceIds)));
        final DataContainer sourceContainer = orgDataStore.get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();

        for (final Invoice invoice : invoices) {
            loadRowIntoContainer(false, invoice, sourceContainer, targetContainer);
        }

        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param invoice       Holds the {@link Invoice} attributes.
     * @param userId        Represents the user id performing the status update.
     * @param statusToUpdate Represents the {@link Invoice.Status} to set.
     */
    @Override
    public void updateStatus(final Invoice invoice, final long userId, final Invoice.Status statusToUpdate) {
        final Invoice.Status previousStatus = invoice.getStatus();

        if (previousStatus == statusToUpdate) {
            return;
        }
        final long orgId = invoice.orgId();
        final long id = invoice.id();
        final Table invoiceTable = getTable();
        final OrgDataStore orgDataStore = secureOrgDataStore(orgId);
        final Row invoiceRow = orgDataStore.get(invoiceTable, id);

        if (Objects.isNull(invoiceRow)) {
            return;
        }
        final DataContainer targetContainer = DataContainer.create();
        final Long modifiedAt = TemporalServiceProvider.get().getUtcDateTime();

        invoiceRow.set(OMIVINVOICE.STATUS, statusToUpdate.getId());
        invoiceRow.set(OMIVINVOICE.MODIFIED_AT, modifiedAt);
        invoiceRow.set(OMIVINVOICE.UPDATED_BY, userId);

        targetContainer.updateRow(invoiceRow);

        invoice.setStatus(statusToUpdate);
        invoice.setModifiedAt(new DateTime(modifiedAt));
        invoiceTimelineManager.loadInvoiceTimelineIntoContainer(invoice, false, previousStatus, userId, targetContainer);

        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param id    Represents the invoice id.
     */
    @Override
    public void delete(final long orgId, final long id) {
        secureOrgDataStore(orgId).delete(getTable(), id);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param id    Represents the invoice id to clone.
     * @return {@link Invoice} that holds the newly cloned invoice data.
     */
    @Override
    public Invoice clone(final long orgId, final long id) {
        final Invoice invoice = get(orgId, id);
        final String invoiceJsonAsString = invoice.getInvoiceJsonAsString();

        invoice.setId(null);
        invoice.setStatus(Invoice.Status.DRAFT);
        invoice.setNumber(generateInvoiceNumber(orgId));

        if (Objects.nonNull(invoiceJsonAsString)) {
            invoice.setInvoiceJsonAsString(invoiceJsonAsString);
        }
        create(invoice);

        return invoice;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organization id.
     * @param userId      Represents the user id performing the mapping.
     * @param templateIds Which contains the collection of template ids to map as defaults.
     */
    @Override
    public void createDefaultTemplateMapping(final long orgId, final long userId, final Collection<Long> templateIds) {
        final DataContainer dataContainer = DataContainer.create();
        final Table templateMappingTable = getOrgInvoiceTemplateTable();
        final long currentTime = TemporalServiceProvider.get().getUtcDateTime();
        boolean isDefault = true;

        for (final Long templateId : templateIds) {
            final Row templateMappingRow = new Row(templateMappingTable);

            templateMappingRow.setOrgId(orgId);
            templateMappingRow.set(OMIVINVOICETEMPLATE.TEMPLATE_ID, templateId);
            templateMappingRow.set(OMIVINVOICETEMPLATE.IS_DEFAULT, isDefault);
            templateMappingRow.set(OMIVINVOICETEMPLATE.CREATED_BY, userId);
            templateMappingRow.set(OMIVINVOICETEMPLATE.UPDATED_BY, userId);
            templateMappingRow.set(OMIVINVOICETEMPLATE.CREATED_TIME, currentTime);
            templateMappingRow.set(OMIVINVOICETEMPLATE.UPDATED_TIME, currentTime);

            dataContainer.addNewRow(templateMappingRow);
            isDefault = false;
        }
        secureOrgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link InvoiceTemplate} that hold the default template data.
     */
    @Override
    public Collection<InvoiceTemplate> getDefaultTemplates() {
        final Table templateTable = getInvoiceTemplateTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(templateTable.getColumn(MIVINVOICETEMPLATE.IS_DEFAULT), true)));
        final DataContainer dataContainer = adminDataStore.get(selectQuery);

        return dataContainer.getRows(templateTable).map(templateRow -> convertRowToInvoiceTemplate(templateRow, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @return {@link Optional} of {@link InvoiceTemplate} that holds the default template for the organization.
     */
    @Override
    public Optional<InvoiceTemplate> getDefaultTemplate(final long orgId) {
        final Table templateTable = getInvoiceTemplateTable();
        final Table orgTemplateTable = getOrgInvoiceTemplateTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);

        selectQuery.addJoinClause(new JoinClause(templateTable.getPKColumn(), orgTemplateTable.getColumn(OMIVINVOICETEMPLATE.TEMPLATE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(orgTemplateTable.getColumn(OMIVINVOICETEMPLATE.IS_DEFAULT), true)));
        final DataContainer dataContainer = secureOrgDataStore(orgId).get(selectQuery);

        return Optional.ofNullable(dataContainer.getRow(templateTable)).map(templateRow -> convertRowToInvoiceTemplate(templateRow, dataContainer));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents the organization id.
     * @param id              Represents the invoice id.
     * @param status          Represents the {@link Invoice.Status} to set.
     * @param targetContainer Holds the updated information of the invoice status.
     */
    @Override
    public void loadStatusUpdateIntoContainer(final long orgId, final long actorUserId, final long id,
                                              final Invoice.Status status, final DataContainer targetContainer) {
        final Row invoiceRow = secureOrgDataStore(orgId).get(getTable(), id);

        if (Objects.isNull(invoiceRow)) {
            return;
        }
        final Invoice.Status previousStatus = Invoice.Status.get(invoiceRow.get(OMIVINVOICE.STATUS));

        invoiceRow.set(OMIVINVOICE.STATUS, status.getId());
        invoiceRow.set(OMIVINVOICE.MODIFIED_AT, TemporalServiceProvider.get().getUtcDateTime());

        targetContainer.updateRow(invoiceRow);

        if (previousStatus != status) {
            final Invoice invoice = get(orgId, id);

            invoice.setStatus(status);
            invoiceTimelineManager.loadInvoiceTimelineIntoContainer(invoice, false, previousStatus, actorUserId, targetContainer);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organization id.
     * @param invoiceId Represents the invoice id.
     * @return {@link Optional} of {@link Invoice.Status} that holds the current status.
     */
    @Override
    public Optional<Invoice.Status> getStatus(final long orgId, final long invoiceId) {
        final Row invoiceRow = secureOrgDataStore(orgId).get(getTable(), invoiceId);

        if (Objects.nonNull(invoiceRow)) {
            return Optional.of(Invoice.Status.get(invoiceRow.get(OMIVINVOICE.STATUS)));
        }
        return Optional.empty();
    }

    /**
     * <p>
     * Retrieves the invoice file ids (HTML/PDF) associated with the given organization and invoice id.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param id    Represents the invoice id.
     * @return Collection of the non-null file ids, otherwise empty.
     */
    @Override
    public Collection<Long> getInvoiceFileIds(final long orgId, final long id) {
        final Table table = getTable();
        final DataContainer dataContainer = secureOrgDataStore(orgId).get(table,
                new WhereClause(Criteria.eq(table.getPKColumn(), id)),
                List.of(table.getColumn(OMIVINVOICE.HTML_FILE_ID), table.getColumn(OMIVINVOICE.PDF_FILE_ID))
        );

        return Optional.ofNullable(dataContainer.getRow(table)).map(row -> {
            final Long htmlFileId = row.get(OMIVINVOICE.HTML_FILE_ID);
            final Long pdfFileId = row.get(OMIVINVOICE.PDF_FILE_ID);

            return Stream.of(htmlFileId, pdfFileId).filter(Objects::nonNull).collect(Collectors.toSet());
        }).orElseGet(Collections::emptySet);
    }

    /**
     * <p>
     * Retrieves the associated product ids for each of the given invoices.
     * The returned map is keyed by invoice id, with each value being the collection
     * of product ids linked to that invoice via {@code OMIVProductInvoice}.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param invoiceIds Represents the collection of invoice ids for which to fetch product associations.
     * @return Map of invoice id to collection of associated product ids; never {@code null}, but may be empty.
     */
    @Override
    public Map<Long, Collection<Long>> getProductIds(final long orgId, final Collection<Long> invoiceIds) {
        final Table productInvoiceTable = getInvoiceLineItemsTable();
        final DataContainer dataContainer = fetchProductsContainer(orgId, invoiceIds);
        final Map<Long, Collection<Long>> productsMap = new HashMap<>();

        dataContainer.getRows(productInvoiceTable).forEach(productInvoiceRow -> {
            final Collection<Long> productIds = productsMap.computeIfAbsent(
                    productInvoiceRow.get(OMIVINVOICELINEITEMS.INVOICE_ID), invoiceId -> new HashSet<>());

            productIds.add(productInvoiceRow.get(OMIVINVOICELINEITEMS.PRODUCT_ID));
        });

        return productsMap;
    }

    /**
     * <p>
     * Retrieves the invoice line items for each of the given invoices. The returned map is
     * keyed by invoice id and the value is a collection of {@link InvoiceLineItem} which
     * contains productId and amount information.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param invoiceIds Represents the collection of invoice ids.
     * @return Map of invoice id to collection of {@link InvoiceLineItem}.
     */
    @Override
    public Map<Long, Collection<InvoiceLineItem>> getLineItems(final long orgId, final Collection<Long> invoiceIds) {
        final Table productInvoiceTable = getInvoiceLineItemsTable();
        final Table productTable = productManager.getTable();
        final DataContainer dataContainer = fetchProductsContainer(orgId, invoiceIds);
        final Map<Long, Collection<InvoiceLineItem>> itemsMap = new HashMap<>();

        dataContainer.getRows(productInvoiceTable).forEach(productInvoiceRow -> {
            final Long invoiceId = productInvoiceRow.get(OMIVINVOICELINEITEMS.INVOICE_ID);
            final InvoiceLineItem lineItem = new InvoiceLineItem();
            final Long productId = productInvoiceRow.get(OMIVINVOICELINEITEMS.PRODUCT_ID);
            final Row productRow = dataContainer.getRow(productTable, productId);

            lineItem.setInvoiceId(invoiceId);
            lineItem.setProductId(productId);
            lineItem.setName(productRow.get(MPOPRODUCT.NAME));
            lineItem.setAmount(productInvoiceRow.get(OMIVINVOICELINEITEMS.AMOUNT));

            itemsMap.computeIfAbsent(invoiceId, k -> new ArrayList<>()).add(lineItem);
        });

        return itemsMap;
    }

    /**
     * <p>
     * Fetches invoice preview data, including buyer contact email metadata, in a batched fashion.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param invoiceIds Represents the collection of invoice ids.
     * @return Data container containing the preview data.
     */
    @Override
    public DataContainer getPreviewDetails(final long orgId, final Collection<Long> invoiceIds) {
        final Table invoiceTable = getTable();
        final Table contactTable = contactManager.getTable();
        final Table emailTable = emailManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(invoiceTable);

        selectQuery.addSelectClause(List.of(
                invoiceTable.getColumn(OMIVINVOICE.ID),
                invoiceTable.getColumn(OMIVINVOICE.CONTACT_ID),
                invoiceTable.getColumn(OMIVINVOICE.PDF_FILE_ID),
                contactTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactTable.getColumn(MCOCONTACT.LAST_NAME),
                contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL),
                emailTable.getColumn(OMETEMAIL.TYPE),
                emailTable.getColumn(OMETEMAIL.VALUE)
        ));
        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(invoiceTable.getColumn(OMIVINVOICE.CONTACT_ID), contactTable.getPKColumn(), JoinClause.JoinType.LEFT),
                        new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), emailTable.getPKColumn(), JoinClause.JoinType.LEFT)
                ));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(invoiceTable.getPKColumn(), invoiceIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Links an email conversation to the given invoice.
     * </p>
     *
     * @param orgId          Represents the organization id.
     * @param userId         Represents the user id who initiated the conversation.
     * @param invoiceId      Represents the invoice id.
     * @param conversationId Represents the conversation id to link.
     */
    @Override
    public void addEmailConversation(final long orgId, final long userId, final long invoiceId, final long conversationId) {
        final Table conversationTable = getInvoiceEmailConversationTable();
        final Row conversationRow = new Row(conversationTable);
        final DataContainer dataContainer = DataContainer.create();

        conversationRow.setOrgId(orgId);
        conversationRow.set(OMIVINVOICEEMAILCONVERSATION.USER_ID, userId);
        conversationRow.set(OMIVINVOICEEMAILCONVERSATION.INVOICE_ID, invoiceId);
        conversationRow.set(OMIVINVOICEEMAILCONVERSATION.CONVERSATION_ID, conversationId);

        dataContainer.addNewRow(conversationRow);
        secureOrgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * <p>
     * Retrieves the conversation ids linked to a given invoice.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param invoiceId Represents the invoice id.
     * @return Collection of linked conversation ids; never {@code null}, may be empty.
     */
    @Override
    public Collection<Long> getEmailConversationIds(final long orgId, final long invoiceId) {
        final Table conversationTable = getInvoiceEmailConversationTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.setWhereClause(new WhereClause(
                Criteria.eq(conversationTable.getColumn(OMIVINVOICEEMAILCONVERSATION.INVOICE_ID), invoiceId)));
        final DataContainer dataContainer = secureOrgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(conversationTable)
                .map(row -> (Long) row.get(OMIVINVOICEEMAILCONVERSATION.CONVERSATION_ID))
                .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Map<Source, Collection<Long>>> getContactCompanyAndDealIds(final long orgId, final Collection<Long> invoiceIds) {
        if (invoiceIds.isEmpty()) {
            return Map.of();
        }
        final Table invoiceTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(invoiceTable);

        selectQuery.addSelectClause(List.of(
                invoiceTable.getPKColumn(),
                invoiceTable.getColumn(OMIVINVOICE.CONTACT_ID),
                invoiceTable.getColumn(OMIVINVOICE.COMPANY_ID),
                invoiceTable.getColumn(OMIVINVOICE.DEAL_ID)
        ));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(invoiceTable.getPKColumn(), invoiceIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Map<Long, Map<Source, Collection<Long>>> entityIdsByInvoice = new HashMap<>();

        dataContainer.getRows(invoiceTable).forEach(row -> {
            final Long invoiceId = row.getPKValue();
            final Map<Source, Collection<Long>> sourceEntityIds = entityIdsByInvoice.computeIfAbsent(invoiceId, id -> new EnumMap<>(Source.class));
            final Long contactId = row.get(OMIVINVOICE.CONTACT_ID);
            final Long companyId = row.get(OMIVINVOICE.COMPANY_ID);
            final Long dealId = row.get(OMIVINVOICE.DEAL_ID);

            if (Objects.nonNull(contactId)) {
                sourceEntityIds.computeIfAbsent(Source.CONTACT, source -> new HashSet<>()).add(contactId);
            }

            if (Objects.nonNull(companyId)) {
                sourceEntityIds.computeIfAbsent(Source.COMPANY, source -> new HashSet<>()).add(companyId);
            }

            if (Objects.nonNull(dealId)) {
                sourceEntityIds.computeIfAbsent(Source.DEAL, source -> new HashSet<>()).add(dealId);
            }
        });

        return entityIdsByInvoice;
    }

    /**
     * <p>
     * Loads the invoice status update and invoice email conversation mapping into
     * the target data container using a single database read.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param userId          Represents the user id associated with the invoice email.
     * @param invoiceId       Represents the invoice id.
     * @param conversationId  Represents the email conversation id.
     * @param status          Represents the {@link Invoice.Status} to set.
     * @param targetContainer Holds the updated information to be persisted.
     */
    public void loadInvoiceEmailDataIntoContainer(final long orgId, final long userId,
                                                  final long invoiceId, final long conversationId,
                                                  final Invoice.Status status, final DataContainer targetContainer) {
        final Table invoiceTable = getTable();
        final Table invoiceEmailConversationTable = getInvoiceEmailConversationTable();

        final Column invoicePkColumn = invoiceTable.getPKColumn();
        final Column emailConversationInvoiceIdColumn = invoiceEmailConversationTable.getColumn(OMIVINVOICEEMAILCONVERSATION.INVOICE_ID);
        final Column emailConversationConversationIdColumn = invoiceEmailConversationTable.getColumn(OMIVINVOICEEMAILCONVERSATION.CONVERSATION_ID);

        final SelectQuery selectQuery = SelectQuery.get(invoiceTable);

        selectQuery.addJoinClause(new JoinClause(invoicePkColumn, emailConversationInvoiceIdColumn, JoinClause.JoinType.LEFT));
        selectQuery.addSelectClause(invoiceTable, invoiceEmailConversationTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(invoicePkColumn, invoiceId)));

        final DataContainer sourceContainer = secureOrgDataStore(orgId).get(selectQuery);
        final Row invoiceRow = sourceContainer.getRow(invoiceTable);

        if (Objects.isNull(invoiceRow)) {
            return;
        }
        final Invoice.Status previousStatus = Invoice.Status.get(invoiceRow.get(OMIVINVOICE.STATUS));

        invoiceRow.set(OMIVINVOICE.STATUS, status.getId());
        invoiceRow.set(OMIVINVOICE.MODIFIED_AT, TemporalServiceProvider.get().getUtcDateTime());

        targetContainer.updateRow(invoiceRow);
        final WhereClause emailWhereClause = new WhereClause(Criteria.eq(emailConversationInvoiceIdColumn, invoiceId))
                .and(Criteria.eq(emailConversationConversationIdColumn, conversationId));

        if (Objects.isNull(sourceContainer.getRow(invoiceEmailConversationTable, emailWhereClause))) {
            final Row row = new Row(invoiceEmailConversationTable);

            row.setOrgId(orgId);
            row.set(OMIVINVOICEEMAILCONVERSATION.USER_ID, userId);
            row.set(OMIVINVOICEEMAILCONVERSATION.INVOICE_ID, invoiceId);
            row.set(OMIVINVOICEEMAILCONVERSATION.CONVERSATION_ID, conversationId);

            targetContainer.addNewRow(row);
        }

        if (previousStatus != status) {
            final Invoice invoice = get(orgId, invoiceId);

            invoice.setStatus(status);
            invoiceTimelineManager.loadInvoiceTimelineIntoContainer(invoice, false, previousStatus, userId, targetContainer);
        }
    }

    /**
     * <p>
     * Executes a query against the invoice and product-invoice tables to fetch all
     * product association rows for the specified invoices in a single round-trip.
     * </p>
     *
     * @param orgId      Represents the organization id used to scope the data store lookup.
     * @param invoiceIds Represents the set of invoice ids whose product associations should be fetched.
     * @return {@link DataContainer} holding the joined product-invoice rows for all requested invoices;
     * rows are absent (not {@code null}) when no association exists for a given invoice.
     */
    private DataContainer fetchProductsContainer(final long orgId, final Collection<Long> invoiceIds) {
        final Table invoiceTable = getTable();
        final Table productInvoiceTable = getInvoiceLineItemsTable();
        final Table productTable = productManager.getTable();
        final Column invoicePkColumn = invoiceTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(invoiceTable);

        selectQuery.addSelectClause(invoiceTable, productTable, productInvoiceTable);
        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(invoicePkColumn, productInvoiceTable.getColumn(OMIVINVOICELINEITEMS.INVOICE_ID), JoinClause.JoinType.LEFT),
                        new JoinClause(productInvoiceTable.getColumn(OMIVINVOICELINEITEMS.PRODUCT_ID), productTable.getColumn(MPOPRODUCT.ID), JoinClause.JoinType.LEFT)
                )
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.in(invoicePkColumn, invoiceIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Builds a {@link SelectQuery} for the invoice table with the given
     * filter, joining the product association table.
     * </p>
     *
     * @param whereClause Represents the filter criteria.
     * @return {@link SelectQuery} that was constructed.
     */
    private SelectQuery buildSelectQuery(final WhereClause whereClause) {
        final Table invoiceTable = getTable();
        final Table productInvoiceTable = getInvoiceLineItemsTable();
        final SelectQuery selectQuery = SelectQuery.get(invoiceTable);

        selectQuery.addSelectClause(invoiceTable, productInvoiceTable);
        selectQuery.addJoinClause(new JoinClause(
                invoiceTable.getPKColumn(),
                productInvoiceTable.getColumn(OMIVINVOICELINEITEMS.INVOICE_ID), JoinClause.JoinType.LEFT)
        );
        selectQuery.setWhereClause(whereClause);

        return selectQuery;
    }

    /**
     * <p>
     * Loads the invoice details into the target container, including its product associations.
     * Contact and company links are written directly onto the {@code OMIVInvoice}
     * row itself (via {@code CONTACT_ID} / {@code COMPANY_ID}) since, unlike
     * quotations, invoices do not use separate junction tables for these.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event.
     * @param invoice         An entity that holds the {@link Invoice} attributes.
     * @param sourceContainer Holds the old information of the invoice.
     * @param targetContainer Holds the updated information of the invoice.
     */
    private void loadRowIntoContainer(final boolean isCreateEvent, final Invoice invoice,
                                      final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table invoiceTable = getTable();
        final Long id = invoice.id();
        final Row invoiceRow = isCreateEvent ? new Row(invoiceTable) : sourceContainer.getRow(invoiceTable, id);
        final long currentTime = TemporalServiceProvider.get().getUtcDateTime();
        final Invoice.Status previousStatus = getPreviousStatus(sourceContainer, invoice.id());
        final CrmUser createdBy = invoice.getCreatedBy();
        final CrmUser updatedBy = invoice.getUpdatedBy();

        if (invoiceRow.isNewRow()) {
            final Long invoiceId = invoiceRow.getPKValue();
            final InvoiceType invoiceType = invoice.getInvoiceType();

            invoiceRow.set(OMIVINVOICE.NUMBER, invoice.getNumber());
            invoiceRow.set(OMIVINVOICE.ISSUED_AT, invoice.getIssueAt());
            invoiceRow.set(OMIVINVOICE.TEMPLATE_ID, invoice.getTemplateId());

            if (Objects.nonNull(invoiceType)) {
                invoiceRow.set(OMIVINVOICE.INVOICE_TYPE, invoiceType.getValue());
            }
            invoice.setId(invoiceId);

            invoiceRow.set(OMIVINVOICE.CREATED_BY, createdBy.id());
            invoiceRow.set(OMIVINVOICE.UPDATED_BY, createdBy.id());
        } else {
            invoiceRow.set(OMIVINVOICE.MODIFIED_AT, currentTime);
            invoiceRow.set(OMIVINVOICE.UPDATED_BY, updatedBy.id());
            invoice.setModifiedAt(new DateTime(currentTime));
        }
        final String invoiceJsonAsString = invoice.getInvoiceJsonAsString();
        final CrmUser owner = invoice.getOwner();
        final Contact contact = invoice.getContact();
        final Company company = invoice.getCompany();
        final Deal deal = invoice.getDeal();
        final Date dueBy = invoice.getDueBy();
        final Long htmlFileId = invoice.getHtmlFileId();
        final Long pdfFileId = invoice.getPdfFileId();

        invoiceRow.set(OMIVINVOICE.STATUS, invoice.getStatus().getId());

        if (Objects.nonNull(invoiceJsonAsString)) {
            invoiceRow.set(OMIVINVOICE.INVOICE_JSON, invoiceJsonAsString);
        }

        if (Objects.nonNull(owner)) {
            invoiceRow.set(OMIVINVOICE.OWNER_ID, owner.id());
        }

        if (Objects.nonNull(contact)) {
            invoiceRow.set(OMIVINVOICE.CONTACT_ID, contact.id());
        }

        if (Objects.nonNull(company)) {
            invoiceRow.set(OMIVINVOICE.COMPANY_ID, company.id());
        }

        if (Objects.nonNull(deal)) {
            invoiceRow.set(OMIVINVOICE.DEAL_ID, deal.id());
        }

        if (Objects.nonNull(dueBy)) {
            invoiceRow.set(OMIVINVOICE.DUE_BY, dueBy);
        }

        if (Objects.nonNull(htmlFileId)) {
            invoiceRow.set(OMIVINVOICE.HTML_FILE_ID, htmlFileId);
        }

        if (Objects.nonNull(pdfFileId)) {
            invoiceRow.set(OMIVINVOICE.PDF_FILE_ID, pdfFileId);
        }
        final Currency currency = invoice.getCurrency();

        if (Objects.nonNull(currency)) {
            invoiceRow.set(OMIVINVOICE.CURRENCY_ID, currency.getId());
        }

        targetContainer.updateRow(invoiceRow);
        loadProductDetailsIntoContainer(invoice.orgId(), invoice.id(), invoice.getInvoiceLineItems(), sourceContainer, targetContainer);
        final Long actorUserId = isCreateEvent ? createdBy.id() : updatedBy.id();

        invoiceTimelineManager.loadInvoiceTimelineIntoContainer(invoice, isCreateEvent, previousStatus, actorUserId, targetContainer);
    }

    /**
     * <p>
     * Resolves the previous invoice status from the source container.
     * </p>
     *
     * @param sourceContainer Holds the old information of the invoice.
     * @param invoiceId       Represents the invoice id.
     * @return The previous {@link Invoice.Status}, or {@code null} if unavailable.
     */
    private Invoice.Status getPreviousStatus(final DataContainer sourceContainer, final Long invoiceId) {
        if (Objects.isNull(sourceContainer) || Objects.isNull(invoiceId)) {
            return null;
        }
        final Row invoiceRow = sourceContainer.getRow(getTable(), invoiceId);

        if (Objects.isNull(invoiceRow)) {
            return null;
        }
        return Invoice.Status.get(invoiceRow.get(OMIVINVOICE.STATUS));
    }

    /**
     * <p>
     * Generates the next sequential invoice number for the given organization
     * using a Redis-backed distributed atomic counter to ensure uniqueness across
     * multiple threads and server instances.
     * On first invocation (counter value is {@code 0} sentinel), the counter is
     * seeded from the database's current maximum invoice number using
     * {@code compareAndSet} to prevent initialization races.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return The next invoice number as an integer.
     */
    @Override
    public int generateInvoiceNumber(final long orgId) {
        final String counterKey = String.format(INVOICE_NUMBER_COUNTER_KEY_FORMAT, orgId);
        final DistributedAtomicCounter counter = cacheService.getOrInitAtomicCounter(counterKey);
        final long currentValue = counter.get();

        if (currentValue == 0) {
            final long currentMax = fetchCurrentMaxSequence(orgId);

            counter.compareAndSet(0, currentMax);
        }
        final long nextSequence = counter.incrementAndGet();

        return (int) nextSequence;
    }

    /**
     * <p>
     * Fetches the current maximum invoice sequence number from the database
     * for counter initialization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return The current max sequence number, or {@code 0} if none exist.
     */
    private long fetchCurrentMaxSequence(final long orgId) {
        final Table invoiceTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(invoiceTable);

        selectQuery.addSelectClause(invoiceTable.getColumn(OMIVINVOICE.NUMBER));
        selectQuery.addOrderByClause(new OrderByClause(invoiceTable.getColumn(OMIVINVOICE.NUMBER), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(new LimitClause(1));
        final DataContainer dataContainer = secureOrgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(invoiceTable);

        if (Objects.nonNull(row)) {
            final Integer maxNumber = row.get(OMIVINVOICE.NUMBER);

            if (Objects.nonNull(maxNumber)) {
                return maxNumber;
            }
        }

        return 0;
    }

    /**
     * <p>
     * Converts the raw invoice {@link Row} into {@link Invoice} instance.
     * </p>
     *
     * @param row           Which contains the values of the invoice.
     * @param dataContainer Contextual data container used to resolve joined rows.
     * @return {@link Invoice} that holds the values of the invoice.
     */
    private Invoice convertRowToInvoice(final Row row, final DataContainer dataContainer) {
        final Invoice invoice = new Invoice();
        final Long invoiceId = row.getPKValue();
        final Long orgId = row.getOrgId();
        final Short status = row.get(OMIVINVOICE.STATUS);
        final String invoiceJson = row.get(OMIVINVOICE.INVOICE_JSON);
        final Long createdById = row.get(OMIVINVOICE.CREATED_BY);
        final Long updatedById = row.get(OMIVINVOICE.UPDATED_BY);
        final Long ownerId = row.get(OMIVINVOICE.OWNER_ID);
        final Long contactId = row.get(OMIVINVOICE.CONTACT_ID);
        final Long companyId = row.get(OMIVINVOICE.COMPANY_ID);
        final Long dealId = row.get(OMIVINVOICE.DEAL_ID);
        final Collection<Long> userIds = new HashSet<>();
        final Integer number = row.get(OMIVINVOICE.NUMBER);

        invoice.setId(invoiceId);
        invoice.setOrgId(orgId);

        if (Objects.nonNull(number)) {
            invoice.setNumber(number);
        }
        invoice.setTemplateId(row.get(OMIVINVOICE.TEMPLATE_ID));
        invoice.setHtmlFileId(row.get(OMIVINVOICE.HTML_FILE_ID));
        invoice.setPdfFileId(row.get(OMIVINVOICE.PDF_FILE_ID));
        invoice.setInvoiceType(InvoiceType.get(row.get(OMIVINVOICE.INVOICE_TYPE)));

        invoice.setStatus(Invoice.Status.get(status));

        if (Objects.nonNull(invoiceJson)) {
            invoice.setInvoiceJsonAsString(invoiceJson);
        }
        invoice.setIssueAt(row.get(OMIVINVOICE.ISSUED_AT));
        invoice.setModifiedAt(row.get(OMIVINVOICE.MODIFIED_AT));
        invoice.setDueBy(row.get(OMIVINVOICE.DUE_BY));

        final Short currencyId = row.get(OMIVINVOICE.CURRENCY_ID);

        if (Objects.nonNull(currencyId)) {
            invoice.setCurrency(Currency.getCurrencyById(currencyId));
        }

        if (Objects.nonNull(createdById)) {
            userIds.add(createdById);
        }

        if (Objects.nonNull(updatedById)) {
            userIds.add(updatedById);
        }

        if (Objects.nonNull(ownerId)) {
            userIds.add(ownerId);
        }

        if (!userIds.isEmpty()) {
            final Map<Long, CrmUser> users = userService.getUsers(orgId, userIds)
                    .stream()
                    .collect(Collectors.toMap(CrmUser::id, Function.identity()));

            if (Objects.nonNull(createdById)) {
                invoice.setCreatedBy(users.get(createdById));
            }

            if (Objects.nonNull(updatedById)) {
                invoice.setUpdatedBy(users.get(updatedById));
            }

            if (Objects.nonNull(ownerId)) {
                final CrmUser owner = users.get(ownerId);

                if (Objects.nonNull(owner)) {
                    invoice.setOwner(owner);
                }
            }
        }

        if (Objects.nonNull(contactId)) {
            final Contact contact = new Contact();

            contact.setId(contactId);
            invoice.setContact(contact);
        }

        if (Objects.nonNull(companyId)) {
            final Company company = new Company();

            company.setId(companyId);
            invoice.setCompany(company);
        }

        if (Objects.nonNull(dealId)) {
            final Deal deal = new Deal();

            deal.setId(dealId);
            invoice.setDeal(deal);
        }

        if (Objects.nonNull(dataContainer)) {
            final Table productInvoiceTable = getInvoiceLineItemsTable();
            final Collection<InvoiceLineItem> lineItems = dataContainer.getRows(productInvoiceTable,
                            new WhereClause(Criteria.eq(productInvoiceTable.getColumn(OMIVINVOICELINEITEMS.INVOICE_ID), invoiceId)))
                    .map(productRow -> {
                        final InvoiceLineItem invoiceLineItem = new InvoiceLineItem();

                        invoiceLineItem.setProductId(productRow.get(OMIVINVOICELINEITEMS.PRODUCT_ID));
                        invoiceLineItem.setAmount(productRow.get(OMIVINVOICELINEITEMS.AMOUNT));

                        return invoiceLineItem;
                    })
                    .toList();

            invoice.setInvoiceLineItems(lineItems);
        }

        return invoice;
    }

    /**
     * <p>
     * Loads the product-invoice join rows by diffing the provided product id
     * collection against what is already persisted.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param invoiceId       Represents the invoice id.
     * @param lineItems       Holds the current set of line items.
     * @param sourceContainer Holds the existing data from the DB, may be {@code null} on create.
     * @param targetContainer Holds the changes to stage for commit.
     */
    private void loadProductDetailsIntoContainer(final long orgId, final long invoiceId, final Collection<InvoiceLineItem> lineItems,
                                                 final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table productInvoiceTable = getInvoiceLineItemsTable();
        final Map<Long, InvoiceLineItem> itemsByProductId = new HashMap<>();
        final Set<Long> remainingProductIds = new HashSet<>();

        if (!Objects.isNull(lineItems)) {

            for (final InvoiceLineItem invoiceLineItem : lineItems) {
                final Long productid;

                if (Objects.nonNull(invoiceLineItem) && Objects.nonNull(productid = invoiceLineItem.getProductId())) {
                    itemsByProductId.put(productid, invoiceLineItem);
                    remainingProductIds.add(productid);
                }
            }
        }
        final Stream<Row> existingRows = Objects.isNull(sourceContainer) ? Stream.empty() : sourceContainer.getRows(productInvoiceTable,
                new WhereClause(Criteria.eq(productInvoiceTable.getColumn(OMIVINVOICELINEITEMS.INVOICE_ID), invoiceId)));

        existingRows.forEach(existingRow -> {
            final Long productId = existingRow.get(OMIVINVOICELINEITEMS.PRODUCT_ID);

            if (!remainingProductIds.remove(productId)) {
                targetContainer.deleteRow(existingRow);
            }
        });

        for (final Long productId : remainingProductIds) {
            final Row productInvoiceRow = new Row(productInvoiceTable);

            productInvoiceRow.setOrgId(orgId);
            productInvoiceRow.set(OMIVINVOICELINEITEMS.INVOICE_ID, invoiceId);
            productInvoiceRow.set(OMIVINVOICELINEITEMS.PRODUCT_ID, productId);
            final InvoiceLineItem invoiceLineItem = itemsByProductId.get(productId);

            if (Objects.nonNull(invoiceLineItem)) {
                productInvoiceRow.set(OMIVINVOICELINEITEMS.AMOUNT, invoiceLineItem.getAmount());
            }

            targetContainer.addNewRow(productInvoiceRow);
        }
    }

    /**
     * <p>
     * Converts the raw {@link Row} from the master invoice template table
     * into a fully populated {@link InvoiceTemplate} instance, including organization-specific
     * metadata when available.
     * </p>
     *
     * @param templateRow   Which contains the raw SQL row from the MIVInvoiceTemplate table.
     * @param dataContainer Contextual data container containing joined org-specific data.
     * @return {@link InvoiceTemplate} that holds the values of the template.
     */
    private InvoiceTemplate convertRowToInvoiceTemplate(final Row templateRow, final DataContainer dataContainer) {
        final InvoiceTemplate template = new InvoiceTemplate();
        final Long templateId = templateRow.getPKValue();
        final String name = templateRow.get(MIVINVOICETEMPLATE.NAME);
        final String schemaJson = templateRow.get(MIVINVOICETEMPLATE.SCHEMA_JSON);
        final Boolean isDefault = templateRow.get(MIVINVOICETEMPLATE.IS_DEFAULT);

        template.setId(templateId);
        template.setName(name);

        if (Objects.nonNull(schemaJson)) {
            template.setSchemaJsonAsString(schemaJson);
        }

        if (Objects.nonNull(isDefault)) {
            template.setDefault(isDefault);
        }

        if (Objects.nonNull(dataContainer)) {
            final Table orgTemplateTable = getOrgInvoiceTemplateTable();
            final Row orgTemplateRow = dataContainer.getRow(orgTemplateTable,
                    new WhereClause(Criteria.eq(orgTemplateTable.getColumn(OMIVINVOICETEMPLATE.TEMPLATE_ID), templateId)));

            if (Objects.nonNull(orgTemplateRow)) {
                final Long orgId = orgTemplateRow.getOrgId();
                final Long createdById = orgTemplateRow.get(OMIVINVOICETEMPLATE.CREATED_BY);
                final Long updatedById = orgTemplateRow.get(OMIVINVOICETEMPLATE.UPDATED_BY);
                final DateTime createdTime = orgTemplateRow.get(OMIVINVOICETEMPLATE.CREATED_TIME);
                final DateTime updatedTime = orgTemplateRow.get(OMIVINVOICETEMPLATE.UPDATED_TIME);
                final Collection<Long> userIds = new HashSet<>();

                template.setOrgId(orgId);
                template.setDefault(orgTemplateRow.get(OMIVINVOICETEMPLATE.IS_DEFAULT));

                if (Objects.nonNull(createdTime)) {
                    template.setCreatedTime(createdTime);
                }

                if (Objects.nonNull(updatedTime)) {
                    template.setUpdatedTime(updatedTime);
                }

                if (Objects.nonNull(createdById)) {
                    userIds.add(createdById);
                }

                if (Objects.nonNull(updatedById)) {
                    userIds.add(updatedById);
                }

                if (!userIds.isEmpty() && Objects.nonNull(orgId)) {
                    final Map<Long, CrmUser> users = userService.getUsers(orgId, userIds)
                            .stream()
                            .collect(Collectors.toMap(CrmUser::id, Function.identity()));

                    if (Objects.nonNull(createdById)) {
                        template.setCreatedBy(users.get(createdById));
                    }

                    if (Objects.nonNull(updatedById)) {
                        template.setUpdatedBy(users.get(updatedById));
                    }
                }
            }
        }

        return template;
    }
}