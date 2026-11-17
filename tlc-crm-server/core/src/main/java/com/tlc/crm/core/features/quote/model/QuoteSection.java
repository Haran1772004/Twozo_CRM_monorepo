package com.tlc.crm.core.features.quote.model;

import com.tlc.commons.template.model.Section;
import com.tlc.commons.util.StringUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the available sections and sub-sections of a quotation template.
 *
 * <p>This enum represents the logical sections present in a quotation document,
 * including header, parties, summary, line items, payment, and acceptance
 * sections. Each section is mapped with a unique identifier and name that can
 * be used during template processing and customization.
 *
 * <p>The section identifier returned by {@link #id()} is formatted to match the
 * HTML element ID convention used in the quotation template.
 *
 * @author Sathish
 * @version 1.0
 */
public enum QuoteSection implements Section {

    BASE_CARD_SECTION("quote-card", "baseCardSection"),
    TOP_BANNER("top-banner", "topBanner"),

    HEADER("1", "header"),
    PARTIES("2", "parties"),
    SUMMARY("3", "summary"),
    LINE_ITEMS("4", "lineItems"),
    PAYMENT("5", "payment"),
    ACCEPTANCE("6", "acceptance"),

    HEADER_TITLE("1.1", "headerTitle"),
    HEADER_QUOTE_REFERENCE("1.2", "headerQuoteReference"),
    HEADER_ISSUE_DATE("1.3", "headerIssueDate"),
    HEADER_EXPIRY_DATE("1.4", "headerExpiryDate"),
    HEADER_CURRENCY("1.5", "headerCurrency"),
    HEADER_COMPANY_NAME("1.6", "headerCompanyName"),
    HEADER_LOGO("1.7", "headerLogo"),

    PARTIES_SELLER_TITLE("2.1", "partiesSellerTitle"),
    PARTIES_SELLER_COMPANY("2.2", "partiesSellerCompany"),
    PARTIES_SELLER_CONTACT("2.3", "partiesSellerContact"),
    PARTIES_BUYER_TITLE("2.4", "partiesBuyerTitle"),
    PARTIES_BUYER_COMPANY("2.5", "partiesBuyerCompany"),
    PARTIES_BUYER_CONTACT("2.6", "partiesBuyerContact"),

    SUMMARY_EFFECTIVE_DATE("3.1", "summaryEffectiveDate"),
    SUMMARY_TERM_LENGTH("3.2", "summaryTermLength"),
    SUMMARY_DISCOUNT("3.3", "summaryDiscount"),
    SUMMARY_TOTAL_VALUE("3.4", "summaryTotalValue"),

    LINE_ITEMS_PRODUCTS("4.1", "lineItemsProducts"),
    LINE_ITEMS_TOTALS("4.2", "lineItemsTotals"),
    LINE_ITEMS_SUB_TOTAL("4.3", "lineItemsSubTotal"),
    LINE_ITEMS_DISCOUNT("4.4", "lineItemsDiscount"),
    LINE_ITEMS_TAX("4.5", "lineItemsTax"),
    LINE_ITEMS_DUE("4.6", "lineItemsDue"),
    LINE_ITEMS_TOTAL_VALUE("4.7", "lineItemsTotalValue"),

    PAYMENT_TITLE("5.1", "paymentTitle"),
    PAYMENT_BILLING("5.2", "paymentBilling"),

    ACCEPTANCE_TITLE("6.1", "acceptanceTitle"),
    ACCEPTANCE_METHOD("6.2", "acceptanceMethod"),
    ACCEPTANCE_BUYER_SUB_TITLE("6.3", "acceptanceBuyerSubTitle"),
    ACCEPTANCE_BUYER_CONTACTS("6.4", "acceptanceBuyerContacts"),
    ACCEPTANCE_SELLER_SUB_TITLE("6.5", "acceptanceSellerSubTitle"),
    ACCEPTANCE_SELLER_CONTACTS("6.6", "acceptanceSellerContacts"),
    ;

    private static final String SECTION = "section";
    private static final Map<String, QuoteSection> SECTION_MAP = new HashMap<>();

    static {
        for (final QuoteSection section : values()) {
            SECTION_MAP.put(section.id, section);
        }
    }

    private final String id;
    private final String name;

    /**
     * Creates a quotation section with the given identifier and name.
     *
     * @param id   Unique section identifier.
     * @param name Logical name of the section.
     */
    QuoteSection(final String id, final String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns the HTML element identifier for this quotation section.
     *
     * <p>The returned ID follows the template convention by prefixing the
     * section identifier with the constant section prefix.
     *
     * @return Formatted HTML section identifier.
     */
    @Override
    public String id() {
        return StringUtil.join(StringUtil.Delimiter.HYPHEN, SECTION, id);
    }

    /**
     * Returns the logical name of this quotation section.
     *
     * @return Section name used during template processing.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the quotation section matching the given identifier.
     *
     * @param id Section identifier.
     * @return Matching {@link QuoteSection}, or {@code null} if no section exists
     *         for the given identifier.
     */
    public static QuoteSection get(final String id) {
        return SECTION_MAP.get(id);
    }

    /**
     * Returns the sections related to the current quote section.
     *
     * <p>This method provides dependent sections that should be handled together
     * with the current section. For example, the line item totals section contains
     * related sections such as subtotal, discount, tax, due, and total value.</p>
     *
     * @return a collection of related {@link QuoteSection} values, or an empty
     * collection if no related sections are defined
     */
    public Collection<QuoteSection> getRelatedSections() {
        return switch (this) {
            case LINE_ITEMS_TOTALS -> List.of(LINE_ITEMS_SUB_TOTAL, LINE_ITEMS_DISCOUNT, LINE_ITEMS_TAX, LINE_ITEMS_DUE, LINE_ITEMS_TOTAL_VALUE);
            case LINE_ITEMS -> List.of(LINE_ITEMS_PRODUCTS);
            default -> Collections.emptyList();
        };
    }
}