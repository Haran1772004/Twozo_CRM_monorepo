package com.tlc.crm.core.features.invoice.model;

import com.tlc.commons.template.model.Section;
import com.tlc.commons.util.StringUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Defines the sections available in invoice templates and provides the metadata
 * required to resolve, order, and process invoice sections during template
 * rendering.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum InvoiceSection implements Section {

    BASE_CARD_SECTION("invoice-card", "baseCardSection"),

    HEADER("1", "header"),
    PARTIES("2", "parties"),
    LINE_ITEMS("3", "lineItems"),

    HEADER_TITLE("1.1", "headerTitle"),
    HEADER_INVOICE_DATE("1.2", "headerInvoiceDate"),
    HEADER_DUE_DATE("1.3", "headerDueDate"),
    HEADER_PO_NUMBER("1.4", "headerPoNumber"),
    HEADER_COMPANY_NAME("1.5", "headerCompanyName"),
    HEADER_LOGO("1.6", "headerLogo"),

    PARTIES_BUYER_TITLE("2.1", "partiesBuyerTitle"),
    PARTIES_BUYER_CONTACT("2.2", "partiesBuyerContact"),
    PARTIES_BUYER_COMPANY("2.3", "partiesBuyerCompany"),
    PARTIES_SHIPPING_ADDRESS("2.4", "partiesShippingAddress"),
    PARTIES_BILLING_ADDRESS("2.5", "partiesBillingAddress"),

    LINE_ITEMS_PRODUCTS("3.1", "lineItemsProducts"),
    LINE_ITEMS_TOTALS("3.2", "lineItemsTotals"),
    LINE_ITEMS_SUB_TOTAL("3.3", "lineItemsSubTotal"),
    LINE_ITEMS_DISCOUNT("3.4", "lineItemsDiscount"),
    LINE_ITEMS_TAX("3.5", "lineItemsTax"),
    LINE_ITEMS_DUE("3.6", "lineItemsDue"),
    LINE_ITEMS_TOTAL_VALUE("3.7", "lineItemsTotalValue");

    private static final String SECTION = "section";
    private static final Map<String, InvoiceSection> SECTION_MAP = new HashMap<>();

    static {
        for (final InvoiceSection section : values()) {
            SECTION_MAP.put(section.id, section);
        }
    }

    private final String id;
    private final String name;

    /**
     * <p>
     * Creates an invoice section with the given identifier and template name.
     * </p>
     *
     * @param id   Represents the section identifier.
     * @param name Represents the section name used by the invoice template.
     */
    InvoiceSection(final String id, final String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * <p>
     * Returns the template section identifier prefixed with the common
     * {@code section} identifier.
     * </p>
     *
     * @return The formatted section identifier.
     */
    @Override
    public String id() {
        return StringUtil.join(StringUtil.Delimiter.HYPHEN, SECTION, id);
    }

    /**
     * <p>
     * Gets the name associated with the invoice section.
     * </p>
     *
     * @return The invoice section name.
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Resolves an invoice section using its form section identifier.
     * </p>
     *
     * @param id Represents the form section identifier.
     * @return The corresponding {@link InvoiceSection}, or {@code null} if no matching section exists.
     */
    public static InvoiceSection get(final String id) {
        return SECTION_MAP.get(id);
    }

    /**
     * <p>
     * Gets the sections related to the current invoice section. Related
     * sections are used by the template processor to process dependent
     * sections together.
     * </p>
     *
     * @return Collection of sections related to the current section.
     */
    @Override
    public Collection<? extends Section> getRelatedSections() {
        return switch (this) {
            case LINE_ITEMS_TOTALS ->
                    List.of(LINE_ITEMS_SUB_TOTAL, LINE_ITEMS_DISCOUNT, LINE_ITEMS_TAX, LINE_ITEMS_DUE, LINE_ITEMS_TOTAL_VALUE);
            case LINE_ITEMS -> List.of(LINE_ITEMS_PRODUCTS);
            default -> Collections.emptyList();
        };
    }
}