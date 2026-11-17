package com.tlc.crm.core.features.action;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.action.contact.CompanyManagement;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.features.invoice.model.converter.InvoiceConverter;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the invoice company related web actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@WebAction(path = "/invoice/company/mgmt")
public class InvoiceCompanyManagement extends CompanyManagement {

    private final InvoiceConverter invoiceConverter;

    public InvoiceCompanyManagement() {
        this.invoiceConverter = InvoiceConverter.getInstance();
    }

    /**
     * <p>
     * Loads the created company records into the response and invokes the post action
     * for the successfully processed companies.
     * </p>
     *
     * @param action         Represents the action being processed.
     * @param crmResponse    Represents the response to be populated.
     * @param processOutcome Represents the outcome of the action processing.
     * @param companies      Represents the collection of companies processed by the action.
     * @param eventData      Represents the event data associated with the action.
     */
    @Override
    protected void loadResponseAndInvokePostAction(final Action action, final CrmResponse crmResponse, final ProcessOutcome processOutcome,
                                                   final Collection<Company> companies, final EventData eventData) {
        final ErrorSummary errorSummary = processOutcome.getErrorSummary();
        final JsonArray jsonArray = Json.array();

        companies.forEach(company -> jsonArray.put(invoiceConverter.convertInvoiceCompanyDataToJson(company)));
        crmResponse.put(ActionFields.CREATED_RECORDS, jsonArray);

        if (Objects.nonNull(errorSummary)) {
            crmResponse.setErrorCode(getActionErrorCode(action));
            crmResponse.setErrorSummary(errorSummary);
        }

        postAction(action, companies.stream()
                .filter(company -> processOutcome.getProcessedRecords().contains(company.id()))
                .collect(Collectors.toUnmodifiableSet()), eventData);
    }
}