package com.tlc.crm.core.features.invoice.manager;

import com.tlc.crm.core.features.invoice.model.Invoice;
import com.tlc.crm.core.features.invoice.model.InvoiceTemplate;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.core.features.invoice.model.InvoiceLineItem;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Provides the database operations for {@link Invoice}, covering full CRUD,
 * product association, email-conversation linking, clone operations, status
 * management, invoice numbering, and template management.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface InvoiceManager {

    /**
     * Returns the singleton instance of {@link InvoiceManager}.
     *
     * @return singleton instance
     */
    static InvoiceManager getInstance() {
        return InvoiceManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the invoice table.
     * </p>
     *
     * @return {@link Table} representing the invoice table.
     */
    Table getTable();

    /**
     * <p>
     * Gets the product-invoice association table.
     * </p>
     *
     * @return {@link Table} representing the product-invoice table.
     */
    Table getInvoiceLineItemsTable();

    /**
     * <p>
     * Creates an invoice.
     * </p>
     *
     * @param invoice Holds the {@link Invoice} attributes.
     */
    void create(final Invoice invoice);

    /**
     * <p>
     * Creates multiple invoices in bulk.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param invoices Holds the collection of {@link Invoice} attributes to create.
     */
    void bulkCreate(final long orgId, final Collection<Invoice> invoices);

    /**
     * <p>
     * Gets an invoice by its id.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param id    Represents the invoice id.
     * @return {@link Invoice} that holds the retrieved data.
     */
    Invoice get(final long orgId, final long id);

    /**
     * <p>
     * Gets invoices by their ids.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param ids   Contains the collection of invoice ids.
     * @return Collection of {@link Invoice} that hold the retrieved data.
     */
    Collection<Invoice> get(final long orgId, final Collection<Long> ids);

    /**
     * <p>
     * Updates an invoice.
     * </p>
     *
     * @param invoice Holds the {@link Invoice} attributes.
     */
    void update(final Invoice invoice);

    /**
     * <p>
     * Updates multiple invoices in bulk.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param invoices Holds the collection of {@link Invoice} attributes to update.
     */
    void bulkUpdate(final long orgId, final Collection<Invoice> invoices);

    /**
     * <p>
     * Updates the status of an invoice.
     * </p>
     *
     * @param invoice        Holds the {@link Invoice} attributes.
     * @param userId         Represents the user id performing the status update.
     * @param statusToUpdate Represents the {@link Invoice.Status} to set.
     */
    void updateStatus(final Invoice invoice, final long userId, final Invoice.Status statusToUpdate);

    /**
     * <p>
     * Deletes an invoice.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param id    Represents the invoice id.
     */
    void delete(final long orgId, final long id);

    /**
     * <p>
     * Clones an existing invoice.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param id    Represents the invoice id to clone.
     * @return {@link Invoice} that holds the newly cloned invoice data.
     */
    Invoice clone(final long orgId, final long id);

    /**
     * <p>
     * Creates the default invoice template mappings for an organization.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param userId      Represents the user id performing the mapping.
     * @param templateIds Which contains the collection of template ids to map as defaults.
     */
    void createDefaultTemplateMapping(final long orgId, final long userId, final Collection<Long> templateIds);

    /**
     * <p>
     * Gets all default invoice templates.
     * </p>
     *
     * @return Collection of {@link InvoiceTemplate} that hold the default template data.
     */
    Collection<InvoiceTemplate> getDefaultTemplates();

    /**
     * <p>
     * Gets the default invoice template for an organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return {@link Optional} of {@link InvoiceTemplate} that holds the default template.
     */
    Optional<InvoiceTemplate> getDefaultTemplate(final long orgId);

    /**
     * <p>
     * Loads an invoice status update into the given data container and records the timeline
     * entry using the provided actor user id.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param actorUserId     Represents the user who performed the status change.
     * @param id              Represents the invoice id.
     * @param status          Represents the {@link Invoice.Status} to set.
     * @param targetContainer Holds the updated information of the invoice status.
     */
    void loadStatusUpdateIntoContainer(final long orgId, final long actorUserId, final long id, final Invoice.Status status,
                                       final DataContainer targetContainer);

    /**
     * <p>
     * Gets the current status of an invoice.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param invoiceId Represents the invoice id.
     * @return {@link Optional} of {@link Invoice.Status} that holds the current status.
     */
    Optional<Invoice.Status> getStatus(final long orgId, final long invoiceId);

    /**
     * <p>
     * Retrieves the invoice file ids (HTML/PDF) associated with the given
     * organization and invoice id.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param id    Represents the invoice id.
     * @return Collection of the non-null file ids, otherwise empty.
     */
    Collection<Long> getInvoiceFileIds(final long orgId, final long id);

    /**
     * <p>
     * Retrieves the associated product ids for each of the given invoices.
     * The returned map is keyed by invoice id, with each value being the
     * collection of product ids linked to that invoice.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param invoiceIds Represents the collection of invoice ids.
     * @return Map of invoice id to collection of associated product ids.
     */
    Map<Long, Collection<Long>> getProductIds(final long orgId, final Collection<Long> invoiceIds);

    /**
     * <p>
     * Retrieves the invoice line items for each of the given invoices. The map is keyed by
     * invoice id and the value is the collection of {@link InvoiceLineItem} containing
     * productId and amount information.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param invoiceIds Represents the collection of invoice ids.
     * @return Map of invoice id to collection of {@link InvoiceLineItem}.
     */
    Map<Long, Collection<InvoiceLineItem>> getLineItems(final long orgId, final Collection<Long> invoiceIds);

    /**
     * <p>
     * Retrieves contact, company, and deal ids linked on each invoice row ({@code OMIVInvoice} FK columns).
     * Used by list transformers so entity access checks do not depend on list column selection.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param invoiceIds Represents the collection of invoice ids.
     * @return Map of invoice id → (source → collection of linked entity ids); never {@code null}, may be empty.
     */
    Map<Long, Map<Source, Collection<Long>>> getContactCompanyAndDealIds(final long orgId, final Collection<Long> invoiceIds);

    /**
     * <p>
     * Fetches invoice preview data, including buyer contact email metadata, in a batched fashion.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param invoiceIds Represents the collection of invoice ids.
     * @return Data container containing the preview data.
     */
    DataContainer getPreviewDetails(final long orgId, final Collection<Long> invoiceIds);

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
    void addEmailConversation(final long orgId, final long userId, final long invoiceId, final long conversationId);

    /**
     * <p>
     * Retrieves the conversation ids linked to a given invoice.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param invoiceId Represents the invoice id.
     * @return Collection of linked conversation ids; never {@code null}, may be empty.
     */
    Collection<Long> getEmailConversationIds(final long orgId, final long invoiceId);

    /**
     * <p>
     * Generates the next sequential invoice number for the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return The next invoice number.
     */
    int generateInvoiceNumber(final long orgId);

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
    void loadInvoiceEmailDataIntoContainer(final long orgId, final long userId, final long invoiceId, final long conversationId,
                                           final Invoice.Status status, final DataContainer targetContainer);
}