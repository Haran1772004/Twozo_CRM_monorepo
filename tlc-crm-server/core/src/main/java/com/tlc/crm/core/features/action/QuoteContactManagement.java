package com.tlc.crm.core.features.action;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.action.contact.ContactManagement;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages the quote contact related web actions
 *
 * @author Kishore J
 * @version 1.0
 */
@WebAction(path = "/quote/contact/mgmt")
public class QuoteContactManagement extends ContactManagement {

    /**
     * <p>
     * Loads the response and invokes the post action
     * </p>
     *
     * @param action         The {@link Action} type
     * @param crmResponse    The {@link CrmResponse}
     * @param processOutcome The {@link ProcessOutcome}
     * @param contacts       The collection of {@link TlcModel}
     * @param eventData      The {@link EventData}
     */
    @Override
    protected void loadResponseAndInvokePostAction(final Action action, final CrmResponse crmResponse, final ProcessOutcome processOutcome,
                                                   final Collection<Contact> contacts, final EventData eventData) {
        final ErrorSummary errorSummary = processOutcome.getErrorSummary();
        final JsonArray jsonArray = Json.array();

        contacts.forEach(contact -> jsonArray.put(contactConverter.convertContactWithEmailAndPhone(contact)));
        crmResponse.put(ActionFields.CREATED_RECORDS, jsonArray);

        if (Objects.nonNull(errorSummary)) {
            crmResponse.setErrorCode(getActionErrorCode(action));
            crmResponse.setErrorSummary(errorSummary);
        }
        postAction(action, contacts.stream().filter(model -> processOutcome.getProcessedRecords().contains(model.id()))
                .collect(Collectors.toUnmodifiableSet()), eventData);
    }
}

