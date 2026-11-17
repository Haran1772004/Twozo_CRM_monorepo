package com.tlc.crm.entity.currency.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.entity.internal.status.CurrencyUnitErrorCodes;

import com.tlc.exchange.rate.model.ReadableExchangeRateContainer;

import com.tlc.validator.TlcModel;

import jakarta.validation.Valid;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Represents the {@link BaseCurrency} encapsulates the information about base currency data.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see TlcModel
 */
public class BaseCurrency implements TlcModel, Serializable {

    private Long id;
    private Long orgId;
    private DecimalPoint decimalPoint;
    private Currency baseCurrency;
    private boolean isImmutable;
    @Valid
    private Collection<CurrencyUnit> currencyUnits;
    private ReadableExchangeRateContainer readableExchangeRateContainer;

    public Currency getDefaultCurrency() {
        return baseCurrency;
    }

    public void setDefaultCurrency(final Currency defaultCurrency) {
        this.baseCurrency = defaultCurrency;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public DecimalPoint getDecimalPoint() {
        return decimalPoint;
    }

    public void setDecimalPoint(final DecimalPoint decimalPoint) {
        this.decimalPoint = decimalPoint;
    }

    public Collection<CurrencyUnit> getCurrencyUnits() {
        return currencyUnits;
    }

    public void setCurrencyUnits(final Collection<CurrencyUnit> currencyUnits) {
        this.currencyUnits = currencyUnits;
    }

    public void addCurrencyUnits(final CurrencyUnit currencyUnit) {
        this.currencyUnits = Objects.isNull(this.currencyUnits) ? new ArrayList<>() : this.currencyUnits;
        this.currencyUnits.add(currencyUnit);
    }

    public ReadableExchangeRateContainer getReadableExchangeRateContainer() {
        return readableExchangeRateContainer;
    }

    public void setReadableExchangeRateContainer(final ReadableExchangeRateContainer readableExchangeRateContainer) {
        this.readableExchangeRateContainer = readableExchangeRateContainer;
    }

    public boolean isImmutable() {
        return isImmutable;
    }

    public void setImmutable(final boolean isImmutable) {
        this.isImmutable = isImmutable;
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
     * Represents different levels of precision for decimal points.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    public enum DecimalPoint {

        /**
         * Represents no precision decimal point.
         */
        ZERO((short) 1, (short) 0),

        /**
         * Represents one precision decimal point.
         */
        ONE((short) 2, (short) 1),

        /**
         * Represents two precision decimal point.
         */
        TWO((short) 3, (short) 2),

        /**
         * Represents three precision decimal point.
         */
        THREE((short) 4, (short) 3),

        /**
         * Represents four precision decimal point.
         */
        FOUR((short) 5, (short) 4),
        ;

        private final short id;
        private final short points;

        DecimalPoint(final short id, final short points) {
            this.id = id;
            this.points = points;
        }

        public short id() {
            return id;
        }

        public short getPoints() {
            return points;
        }

        /**
         * <p>
         * Gets the {@link DecimalPoint} for the given id.
         * </p>
         *
         * @param id Represents the id of decimal point
         * @return {@link DecimalPoint}
         */
        public static DecimalPoint get(final short id) {
            return switch (id) {
                case 1 -> ZERO;
                case 2 -> ONE;
                case 3 -> TWO;
                case 4 -> THREE;
                case 5 -> FOUR;
                default -> throw ErrorCode.get(CurrencyUnitErrorCodes.INVALID_DECIMAL_POINT,
                        "i18n.currency.unit.invalid.decimal.point");
            };
        }
    }

    /**
     * <p>
     * Represents the status of base currency, which it denotes whether the currency is active or not.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    public enum Status {

        ACTIVE,
        INACTIVE
    }
}
