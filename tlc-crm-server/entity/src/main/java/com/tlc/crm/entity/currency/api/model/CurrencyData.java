package com.tlc.crm.entity.currency.api.model;

import com.tlc.l10n.model.currency.Currency;

/**
 * <p>
 * Represents the {@link CurrencyData} which gives the information about the {@link Currency}.
 * </p>
 *
 * @param isBaseCurrency A flag which represents the currency is base or not
 * @param isActive       A flag which represents the currency is active or not
 * @param currency       Represents the {@link Currency}
 */
public record CurrencyData(boolean isBaseCurrency, boolean isActive, Currency currency) {

}

