package com.tlc.crm.core.action.dropdown.call;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.field.call.CallLogFields;
import com.tlc.crm.core.field.deal.DealFields;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;

import java.util.Collection;

/**
 * Manages the contact {@link Deal} related dropdown actions.
 *
 * @author Sathish
 * @version 1.0
 */
@Dropdown(name = "contactDealList", fieldAlias = "contactDeals")
public class ContactDealList extends AbstractDropdownAction {

    private final DealManager dealManager;

    public ContactDealList() {
        this.dealManager = DealManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request
     * @return {@link OptionContainer} that holds the requested dropdown values
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final Long contactId = dropdownRequest.data().getNullableValue(CallLogFields.CONTACT_ID.getName());
        final long totalRecords = dealManager.getTotalCountByContact(orgId, contactId, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Deal> deals = dealManager.getDealsByContact(orgId, contactId, dropdownLimit, dropdownCriteria);

            deals.forEach(deal -> optionContainer.addOption(convertDealIntoJson(deal)));
        }

        return optionContainer;
    }

    /**
     * <p>
     * Convert the {@link Deal} into {@link JsonObject}
     * </p>
     *
     * @param deal Represents the {@link Deal}
     * @return {@link JsonObject}
     */
    private JsonObject convertDealIntoJson(final Deal deal) {
        final JsonObject dealJson = Json.object();

        dealJson.put(DealFields.NAME, deal.getTitle());
        dealJson.put(DealFields.VALUE, deal.id());
        return dealJson;
    }
}
