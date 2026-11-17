package com.tlc.crm.entity.currency.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.entity.internal.status.CurrencyUnitErrorCodes;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * <p>
 * Represents the {@link CurrencyUnit} that encapsulates the information about currency related data.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see TlcModel
 */
public class CurrencyUnit implements TlcModel, Serializable {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.currency.unit.id.invalid")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.currency.unit.orgId.invalid")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.currency.unit.currency.invalid")
    private Currency currency;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.currency.unit.symbol.invalid")
    private String symbol;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.currency.unit.conversion.type.invalid")
    private ConversionType conversionType;

    private boolean isEnabled;

    public CurrencyUnit() {

    }

    public CurrencyUnit(final Long orgId, final Currency currency, final boolean isEnabled) {
        this.orgId = orgId;
        this.currency = currency;
        this.isEnabled = isEnabled;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(final boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public ConversionType getConversionType() {
        return conversionType;
    }

    public void setConversionType(final ConversionType conversionType) {
        this.conversionType = conversionType;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * <p>
     * Represents the different types for a {@link ConversionType}.
     * </p>
     *
     * @author KowsalyaSP
     */
    public enum ConversionType {

        /**
         * Represents the auto conversion.
         */
        AUTO((short) 1),

        /**
         * Represents the manual conversion.
         */
        MANUAL((short) 2),
        ;

        private final short id;

        ConversionType(final short id) {
            this.id = id;
        }

        public short id() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link ConversionType} for the given id
         * </p>
         *
         * @param id Represents the conversion type id
         * @return {@link ConversionType}
         */
        public static ConversionType get(final short id) {
            return switch (id) {
                case 1 -> AUTO;
                case 2 -> MANUAL;
                default -> throw ErrorCode.get(CurrencyUnitErrorCodes.INVALID_CONVERSION_TYPE,
                        "i18n.currency.unit.invalid.conversion.type");
            };
        }
    }
}
