package com.tlc.crm.entity.currency.action.dropdown;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.i18n.I18nResolver;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.entity.currency.action.config.CurrencyConverterConfig;
import com.tlc.crm.entity.currency.api.manager.CurrencyUnitManager;
import com.tlc.crm.entity.currency.api.model.converter.CurrencyDataConverter;
import com.tlc.crm.entity.currency.api.manager.BaseCurrencyManager;
import com.tlc.crm.entity.currency.api.model.CurrencyData;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles the dropdown actions related to only permitted {@link Currency} list.
 * </p>
 *
 * @author kowsalyaSP
 * @version 1.0
 * @see DropdownAction
 */
@Dropdown(name = "permittedCurrencies", fieldAlias = "currency")
public class PermittedCurrencies extends AbstractDropdownAction {

    private final BaseCurrencyService baseCurrencyService;
    private final CurrencyDataConverter currencyDataConverter;
    private final BaseCurrencyManager baseCurrencyManager;
    private final CurrencyUnitManager currencyUnitManager;

    public PermittedCurrencies() {
        this.baseCurrencyService = BaseCurrencyService.getInstance();
        this.currencyDataConverter = CurrencyDataConverter.getInstance();
        this.baseCurrencyManager = BaseCurrencyManager.getInstance();
        this.currencyUnitManager = CurrencyUnitManager.getInstance();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final I18nResolver i18nResolver = dropdownRequest.i18nResolver();
        final Currency baseCurrency = baseCurrencyService.getDefaultCurrency(orgId);
        final Collection<CurrencyData> currencyData = new HashSet<>(currencyUnitManager.getCurrencies(orgId));

        currencyData.add(new CurrencyData(true, true, baseCurrency));
        final Collection<CurrencyData> currencies = new ArrayList<>(applyCriteria(currencyData, dropdownRequest.getCriteria(),
                List.of(currency -> i18nResolver.get(currency.currency().getI18nKey()), currency -> currency.currency().getCode()),
                Optional.empty()));
        final OptionContainer optionContainer = new OptionContainer(currencies.size());
        final Collection<Currency> currencySet = currencies.stream().map(CurrencyData::currency).collect(Collectors.toUnmodifiableSet());
        final Collection<Currency> inactiveBaseCurrencies = baseCurrencyManager.getInactiveBaseCurrencies(orgId);

        for (final Currency currency : inactiveBaseCurrencies) {

            if (!currencySet.contains(currency)) {
                currencies.add(new CurrencyData(false, false, currency));
            }
        }

        if (!currencies.isEmpty()) {
            final Collection<CurrencyData> currencyDataList = currencies.stream().sorted(Comparator.comparing(CurrencyData::isActive).reversed()
                    .thenComparing(data -> data.currency().getCode())).toList();

            for (final CurrencyData currency : currencyDataList) {
                optionContainer.addOption(currencyDataConverter.convert(new CurrencyConverterConfig(orgId,
                        dropdownRequest.getUserId(), dropdownRequest.locale(), currency.isBaseCurrency()), currency));
            }
        }

        return optionContainer;
    }
}
