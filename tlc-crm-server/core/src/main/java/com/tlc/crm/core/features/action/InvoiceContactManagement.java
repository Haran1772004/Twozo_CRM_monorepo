package com.tlc.crm.core.features.action;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.action.contact.ContactManagement;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.features.invoice.model.converter.InvoiceConverter;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the invoice contact related web actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@WebAction(path = "/invoice/contact/mgmt")
public class InvoiceContactManagement extends ContactManagement {

    private final InvoiceConverter invoiceConverter;
    private final CompanyManager companyManager;

    public InvoiceContactManagement() {
        this.invoiceConverter = InvoiceConverter.getInstance();
        this.companyManager = CompanyManager.getInstance();
    }

    /**
     * <p>
     * Loads the created contact records into the response and invokes the post action
     * for the successfully processed contacts.
     * </p>
     *
     * @param action         Represents the action being processed.
     * @param crmResponse    Represents the response to be populated.
     * @param processOutcome Represents the outcome of the action processing.
     * @param contacts       Represents the collection of contacts processed by the action.
     * @param eventData      Represents the event data associated with the action.
     */
    @Override
    protected void loadResponseAndInvokePostAction(final Action action, final CrmResponse crmResponse, final ProcessOutcome processOutcome,
                                                   final Collection<Contact> contacts, final EventData eventData) {
        final ErrorSummary errorSummary = processOutcome.getErrorSummary();
        final JsonArray jsonArray = Json.array();
        final Collection<Long> companyIds = contacts.stream().map(Contact::getCompany).map(Company::id).collect(Collectors.toSet());
        Map<Long, Company> companyMap;

        if (companyIds.isEmpty()) {
            companyMap = Collections.emptyMap();
        } else {
            companyMap = companyManager.getCompaniesWithAddress(eventData.getOrgId(), companyIds)
                    .stream()
                    .collect(Collectors.toMap(Company::id, company -> company));
        }

        contacts.forEach(contact -> jsonArray.put(invoiceConverter.convertInvoiceContactDataToJson(contact, companyMap)));
        crmResponse.put(ActionFields.CREATED_RECORDS, jsonArray);

        if (Objects.nonNull(errorSummary)) {
            crmResponse.setErrorCode(getActionErrorCode(action));
            crmResponse.setErrorSummary(errorSummary);
        }

        postAction(action, contacts.stream()
                .filter(contact -> processOutcome.getProcessedRecords().contains(contact.id()))
                .collect(Collectors.toUnmodifiableSet()), eventData);
    }
}