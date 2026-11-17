package com.tlc.crm.core.features.action;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.action.deal.DealManagement;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.TlcModel;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages the quote deal related web actions
 *
 * @author Kishore J
 * @version 1.0
 */
@WebAction(path = "/quote/deal/mgmt")
public class QuoteDealManagement extends DealManagement {

    private final BaseCurrencyService baseCurrencyService;

    public QuoteDealManagement() {
        baseCurrencyService = BaseCurrencyService.getInstance();
    }

    /**
     * <p>
     * Loads the response and invokes the post action
     * </p>
     *
     * @param action         The {@link Action} type
     * @param crmResponse    The {@link CrmResponse}
     * @param processOutcome The {@link ProcessOutcome}
     * @param deals          The collection of {@link TlcModel}
     * @param eventData      The {@link EventData}
     */
    protected void loadResponseAndInvokePostAction(final Action action, final CrmResponse crmResponse, final ProcessOutcome processOutcome,
                                                   final Collection<Deal> deals, final EventData eventData) {
        final ErrorSummary errorSummary = processOutcome.getErrorSummary();
        final JsonArray jsonArray = Json.array();
        final String currencyKey = DealFields.CURRENCY;
        final String symbolKey = DealFields.SYMBOL;
        final String dealValueKey = DealFields.DEAL_VALUE;
        final String nameKey = DealFields.NAME;
        final String valueKey = DealFields.VALUE;

        deals.forEach(deal -> {
            final DealValue dealValue = deal.getDealValue();
            final Currency currency = dealValue.getCurrency();
            final JsonObject dealJson = Json.object();

            dealJson.put(currencyKey, currency.getCode());
            dealJson.put(symbolKey, currency.getSymbol());
            dealJson.put(dealValueKey, baseCurrencyService.applyPrecision(deal.orgId(), dealValue.getValue()));
            dealJson.put(nameKey, deal.getTitle());
            dealJson.put(valueKey, deal.id());

            jsonArray.put(dealJson);
        });
        crmResponse.put(ActionFields.CREATED_RECORDS, jsonArray);

        if (Objects.nonNull(errorSummary)) {
            crmResponse.setErrorCode(getActionErrorCode(action));
            crmResponse.setErrorSummary(errorSummary);
        }
        postAction(action, deals.stream().filter(model -> processOutcome.getProcessedRecords().contains(model.id()))
                .collect(Collectors.toUnmodifiableSet()), eventData);
    }
}