package com.tlc.crm.core.action.dropdown.marketinglist;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.action.dropdown.contact.ContactOptionBuilder;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.marketinglist.manager.MarketingListContactManager;
import com.tlc.crm.core.field.marketinglist.MarketingListFields;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import java.util.Collection;

/**
 * <p>
 * Manages the contact list dropdown for marketing list, displays the contacts that are not assigned to the specified marketing list
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@StaticDropdown(name = "marketingContactList")
public class MarketingContactListDropdown extends AbstractDropdownAction {

    private final MarketingListContactManager marketingListContactManager;
    private final ContactConverter contactConverter;

    public MarketingContactListDropdown() {
        marketingListContactManager = MarketingListContactManager.getInstance();
        contactConverter = ContactConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final Long marketingListId = dropdownRequest.data().getLong(MarketingListFields.MARKETING_LIST_ID);
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final long totalRecords = marketingListContactManager.getUnassignedContactCount(orgId, marketingListId, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Contact> contacts = marketingListContactManager.getUnassignedContacts(orgId, marketingListId, dropdownLimit, dropdownCriteria);

            for (final Contact contact : contacts) {
                final JsonObject option = ContactOptionBuilder.build(contact, contactConverter);

                optionContainer.addOption(option);
            }
        }

        return optionContainer;
    }
}