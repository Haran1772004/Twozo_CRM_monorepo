package com.tlc.crm.core.features.invoice.action.transformer;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;
import com.tlc.crm.core.action.transformer.AbstractModelTransformer;
import com.tlc.crm.core.features.invoice.service.InvoiceService;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;
import com.tlc.crm.core.features.invoice.manager.InvoiceManager;
import com.tlc.crm.core.features.invoice.model.Invoice;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Transforms the invoice entities into required forms.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@Transformer(name = "invoice", type = Transformer.Type.ENTITY)
public class InvoiceTransformer extends AbstractModelTransformer {

    private final InvoiceService invoiceService;

    public InvoiceTransformer() {
        this.invoiceService = InvoiceService.getInstance();
    }

    /**
     * <p>
     * Transforms the invoice entities into the required forms.
     * </p>
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return {@link TransformerResponse}
     */
    @Override
    public TransformerResponse transform(final TransformerRequest transformerRequest) {
        final Collection<Long> userIds = new HashSet<>();
        final Collection<Long> invoiceIds = new HashSet<>();
        final Collection<Map<String, Object>> records = transformerRequest.records();
        final TransformerResponse transformerResponse = new TransformerResponse();
        final Map<Long, Map<String, Object>> invoices = new LinkedHashMap<>();
        final String ownerField = TransformerFields.OWNER.getName();
        final String createdBy = TransformerFields.CREATED_BY.getName();
        final String updatedBy = TransformerFields.UPDATED_BY.getName();
        final String userId = TransformerFields.USER_ID.getName();
        final String idKey = TransformerFields.ID.getName();
        final String issuedAt = InvoiceFields.INVOICE_DATE;
        final String dueAt = InvoiceFields.DUE_DATE;
        final String overDue = InvoiceFields.DAYS_OVER_DUE;

        for (final Map<String, Object> invoiceRow : records) {
            final Map<String, Object> users = new HashMap<>();
            final Long pkValue = (Long) invoiceRow.get(idKey);

            collectUserIds(ownerField, invoiceRow, userIds, users);
            collectUserIds(createdBy, invoiceRow, userIds, users);
            collectUserIds(updatedBy, invoiceRow, userIds, users);

            if (invoiceRow.containsKey(issuedAt) && invoiceRow.containsKey(dueAt) && !invoiceRow.containsKey(overDue)) {
                invoiceRow.put(overDue, NumericalConstant.ZERO);
            }

            if (!userIds.isEmpty()) {
                invoiceRow.put(userId, users);
            }

            if (Objects.nonNull(pkValue)) {
                invoiceIds.add(pkValue);
            }

            groupLineItems(pkValue, invoiceRow, invoices);
        }
        final long orgId = transformerRequest.orgId();
        final long currentUserId = LoggedInUserInfo.getUser().id();
        final long roleId = LoggedInUserInfo.getUser().getRoleId();
        final InvoiceManager invoiceManager = InvoiceManager.getInstance();
        final Map<Long, Map<Source, Collection<Long>>> entityIdsByInvoice = invoiceManager.getContactCompanyAndDealIds(orgId, invoiceIds);

        for (final Map.Entry<Long, Map<String, Object>> invoiceEntry : invoices.entrySet()) {
            final Long invoiceId = invoiceEntry.getKey();
            final Map<String, Object> invoiceRow = invoiceEntry.getValue();
            final boolean entityAccessRestricted = invoiceService.isEntityAccessRestricted(orgId, currentUserId, roleId,
                    entityIdsByInvoice.get(invoiceId));
            final Invoice.Status status = resolveStatus(invoiceRow);

            invoiceService.appendActionConstraints(invoiceRow, status, entityAccessRestricted);
        }

        transformerResponse.setResults(invoices.values());
        transformerResponse.setUserIds(userIds);

        return transformerResponse;
    }

    /**
     * <p>
     * Resolves the {@link Invoice.Status} from the status value stored in a list row map.
     * </p>
     *
     * @param row Represents the list row map containing the invoice field values.
     * @return The resolved {@link Invoice.Status}, or {@code null} when the status value is missing or not numeric.
     */
    private Invoice.Status resolveStatus(final Map<String, Object> row) {
        final Object statusValue = row.get(InvoiceFields.STATUS);

        if (statusValue instanceof final Number number) {
            return Invoice.Status.get(number.shortValue());
        }

        return null;
    }

    /**
     * <p>
     * Groups line item data from multiple rows into a single invoice entry.
     * </p>
     *
     * @param pkValue    The invoice primary key
     * @param invoiceRow The current row of data
     * @param invoices   Map tracking deduplicated invoice entries by PK
     */
    private void groupLineItems(final Long pkValue, final Map<String, Object> invoiceRow,
                                final Map<Long, Map<String, Object>> invoices) {
        final Object productId = invoiceRow.remove(InvoiceFields.PRODUCT_ID);
        final Object productName = invoiceRow.remove(InvoiceFields.PRODUCT_NAME);
        final Object amount = invoiceRow.remove(InvoiceFields.AMOUNT);

        if (Objects.isNull(productId) && Objects.isNull(amount)) {
            invoices.putIfAbsent(pkValue, invoiceRow);
            return;
        }
        final Map<String, Object> existingRow = invoices.computeIfAbsent(pkValue, k -> {
            invoiceRow.put(InvoiceFields.INVOICE_LINE_ITEMS, new ArrayList<>());

            return invoiceRow;
        });
        final Map<String, Object> item = new HashMap<>();

        item.put(InvoiceFields.PRODUCT_ID, productId);
        item.put(InvoiceFields.PRODUCT_NAME, productName);
        item.put(InvoiceFields.AMOUNT, amount);

        ((Collection<Map<String, Object>>) existingRow.computeIfAbsent(
                InvoiceFields.INVOICE_LINE_ITEMS, k -> new ArrayList<>())).add(item);
    }
}