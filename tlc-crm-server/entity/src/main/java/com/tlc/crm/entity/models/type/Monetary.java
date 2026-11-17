package com.tlc.crm.entity.models.type;

import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * <p>
 * A model that represents a Monetary data
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public class Monetary implements TlcModel {

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = Group.Delete.class, message = "i18n.entity.field.type.monetaryId.required")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.entity.field.type.orgId.required")
    private Long orgId;

    @SearchField(name = "currency", readMethodName = "getSymbol")
    @NotNull(message = "i18n.entity.currency.invalid", groups = {Group.Create.class, Group.Update.class})
    private Currency currency;

    @SearchField(name = "value")
    @DecimalMin(value = "0", message = "i18n.entity.currency.invalid.value", groups = {Group.Create.class, Group.Update.class})
    private BigDecimal value;

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public void addValue(final BigDecimal value) {
        this.value = this.value.add(value);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
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
     * {@inheritDoc}
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(value, currency, orgId);
    }

    /**
     *{@inheritDoc}
     *
     * @param object Represents the object to be compared
     * @return true if given object is equals to the current object
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final Monetary monetary)) {
            return false;
        }
        return Objects.equals(orgId, monetary.orgId) && currency == monetary.currency
                && Objects.equals(value, monetary.value);
    }

    public String getSymbol() {
        return currency.getSymbol();
    }

    public String getCurrencyCode() {
        return currency.getCode();
    }

    /**
     * <p>
     * Gets the value with symbol.
     * </p>
     *
     * @param symbol Represents the symbol
     * @param value  Represents the value
     * @return the value along with symbol
     */
    public static String getValueWithSymbol(final String symbol, final Double value) {
        return symbol.concat(String.valueOf(value));
    }
}