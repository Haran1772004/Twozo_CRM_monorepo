package com.tlc.crm.core.api.deal.model;

import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.l10n.model.currency.Currency;

import java.math.BigDecimal;

/**
 * <p>
 * Model which represents the {@link DealValue}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class DealValue extends Monetary {

    public DealValue() {}

    public DealValue(final Long orgId, final Long monetaryId, final Currency currency, final BigDecimal value) {
        super.setId(monetaryId);
        super.setOrgId(orgId);
        super.setValue(value);
        super.setCurrency(currency);
    }

    public DealValue(final Monetary monetary) {
        super.setId(monetary.id());
        super.setOrgId(monetary.orgId());
        super.setValue(monetary.getValue());
        super.setCurrency(monetary.getCurrency());
    }
}
