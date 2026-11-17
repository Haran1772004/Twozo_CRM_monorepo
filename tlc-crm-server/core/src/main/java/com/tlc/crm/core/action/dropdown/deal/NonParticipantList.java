package com.tlc.crm.core.action.dropdown.deal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.action.dropdown.contact.ContactOptionBuilder;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;

import java.util.Collection;

/**
 * Manages the non {@link Participant} related dropdown actions.
 *
 * @author pandiaraj
 */
@Dropdown(name = "nonParticipantList", fieldAlias = "participant")
public class NonParticipantList extends AbstractDropdownAction {

    private final DealManager dealManager;
    private final ContactConverter contactConverter;

    public NonParticipantList() {
        dealManager = DealManager.getInstance();
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
        final Long dealId = dropdownRequest.data().getLong(DealFields.DEAL_ID);

        if (!dealManager.exists(orgId, dealId)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final long totalRecords = dealManager.getNonParticipantsCountByDealId(orgId, dealId, dropdownCriteria);
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Participant> participants = dealManager.getNonParticipantsByDealId(orgId, dealId, dropdownLimit, dropdownCriteria);

            for (final Participant participant : participants) {
                final Contact contact = participant.getContact();
                final JsonObject option = ContactOptionBuilder.build(contact, contactConverter);

                optionContainer.addOption(option);
            }
        }

        return optionContainer;
    }
}