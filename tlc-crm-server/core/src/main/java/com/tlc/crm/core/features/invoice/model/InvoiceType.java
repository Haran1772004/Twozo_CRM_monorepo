package com.tlc.crm.core.features.invoice.model;

/**
 * <p>
 * Represents the types of invoices.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum InvoiceType {

    ONE_TIME_INVOICE((short) 1, "i18n.invoice.type.one.time.invoice"),
    ;

    private final short id;
    private final String i18nKey;

    /**
     * <p>
     * Constructs the invoice type.
     * </p>
     *
     * @param id      Represents the invoice type id.
     * @param i18nKey Represents the i18n key.
     */
    InvoiceType(final short id, final String i18nKey) {
        this.id = id;
        this.i18nKey = i18nKey;
    }

    /**
     * <p>
     * Gets the invoice type id.
     * </p>
     *
     * @return Invoice type id.
     */
    public short getValue() {
        return id;
    }

    /**
     * <p>
     * Gets the i18n key.
     * </p>
     *
     * @return I18n key.
     */
    public String getI18nKey() {
        return i18nKey;
    }

    /**
     * <p>
     * Gets the {@link InvoiceType} by value.
     * </p>
     *
     * @param value Represents the invoice type id.
     * @return {@link InvoiceType}.
     */
    public static InvoiceType get(final short value) {

        for (final InvoiceType invoiceType : values()) {
            if (invoiceType.id == value) {
                return invoiceType;
            }
        }

        return null;
    }
}