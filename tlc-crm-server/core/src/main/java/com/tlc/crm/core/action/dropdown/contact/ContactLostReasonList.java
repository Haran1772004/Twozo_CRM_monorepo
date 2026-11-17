package com.tlc.crm.core.action.dropdown.contact;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.contact.LifeCycleStatusFields;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages contact lost reason related dropdown actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Dropdown(name = "contactLostReasonList", fieldAlias = "contactLostReason")
public class ContactLostReasonList extends AbstractDropdownAction {

    private final OptionService optionService;
    private final FieldService fieldService;
    private final ContactManager contactManager;

    public ContactLostReasonList() {
        optionService = OptionService.getInstance();
        fieldService = FieldService.getSecureInstance();
        contactManager = ContactManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final FieldInfo field = dropdownRequest.getField();
        final Optional<FieldInfo> optionalField = Objects.isNull(field) ? fieldService.getDefaultFieldByAliasName(Source.CONTACT, ContactFields.CONTACT_LOST_REASON) : Optional.of(field);
        final OptionContainer optionContainer = new OptionContainer();

        optionalField.ifPresent(lostReasonField -> {
            final long contactId = dropdownRequest.data().optLong(LifeCycleStatusFields.CONTACT_ID.getName(), 0L);
            final Long contactLostReasonId = Objects.equals(contactId, 0) ? null : contactManager.getContactLostReason(dropdownRequest.getOrgId(), contactId).map(Option::id).orElseGet(() -> null);
            final Collection<Option> options = optionService.getOptions(dropdownRequest.getOrgId(), lostReasonField,
                    dropdownRequest.getCriteria(), dropdownRequest.i18nResolver(), dropdownRequest.getOrder());
            final int start = dropdownLimit.start();
            final long totalRecords = options.size();

            if (totalRecords > 0 && start <= totalRecords) {
                for (final Option option : applyLimit(options, start, dropdownLimit.limit())) {
                    final JsonObject optionJson = Json.object();
                    final Long optionId = option.id();

                    optionJson.put(OptionKeys.VALUE, optionId);
                    optionJson.put(OptionKeys.NAME, option.getOption());
                    optionJson.put(OptionKeys.ORDER, option.getOrder());
                    optionJson.put(OptionKeys.IS_EDITABLE_OPTION, option.isEditable());

                    if (option.isDefault()) {
                        optionJson.put(OptionKeys.IS_DEFAULT, true);
                    }

                    if (Objects.equals(contactLostReasonId, optionId)) {
                        optionJson.put(OptionKeys.IS_ASSOCIATED, true);
                    }
                    optionContainer.addOption(optionJson);
                }
            }
            optionContainer.setTotalRecords(totalRecords);
        });

        return optionContainer;
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest The request object containing parameters for filtering or context.
     * @return An {@link OptionContainer} with the relevant dropdown options.
     */
    @Override
    public OptionContainer fetchZapierData(final DropdownRequest dropdownRequest) {
        return getOptionContainer(dropdownRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest The request object containing parameters for filtering or context.
     * @return An {@link OptionContainer} with the relevant dropdown options.
     */
    @Override
    public OptionContainer fetchRestAPIData(final DropdownRequest dropdownRequest) {
        return getOptionContainer(dropdownRequest);
    }

    /**
     * Retrieves an {@link OptionContainer} containing contact lost reason options based on the provided {@link DropdownRequest}.
     *
     * @param dropdownRequest The request object containing parameters for filtering or context.
     * @return An {@link OptionContainer} with the relevant dropdown options.
     */
    private OptionContainer getOptionContainer(final DropdownRequest dropdownRequest) {
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final FieldInfo field = dropdownRequest.getField();
        final Optional<FieldInfo> optionalField = Objects.isNull(field) ? fieldService.getDefaultFieldByAliasName(Source.CONTACT, ContactFields.CONTACT_LOST_REASON) : Optional.of(field);
        final OptionContainer optionContainer = new OptionContainer(OptionKeys.ID, OptionKeys.NAME);

        optionalField.ifPresent(lostReasonField -> {
            final Collection<Option> options = optionService.getOptions(dropdownRequest.getOrgId(), lostReasonField,
                    dropdownRequest.getCriteria(), dropdownRequest.i18nResolver(), dropdownRequest.getOrder());
            final int start = dropdownLimit.start();
            final long totalRecords = options.size();

            if (totalRecords > 0 && start <= totalRecords) {

                for (final Option option : applyLimit(options, start, dropdownLimit.limit())) {
                    optionContainer.addOption(option.id(), option.getOption());
                }
            }
            optionContainer.setTotalRecords(totalRecords);
        });

        return optionContainer;
    }
}
