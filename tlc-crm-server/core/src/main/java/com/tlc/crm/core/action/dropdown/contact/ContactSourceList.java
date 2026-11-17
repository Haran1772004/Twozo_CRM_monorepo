package com.tlc.crm.core.action.dropdown.contact;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import java.util.Collection;
import java.util.Map;

@Dropdown(name = "contactSource", fieldAlias = "contactSource")
public class ContactSourceList extends AbstractDropdownAction {

    private final OptionService optionService;
    private final FieldService fieldService;

    public ContactSourceList() {
        optionService = OptionService.getInstance();
        fieldService = FieldService.getSecureInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final String contactSourceKey = ContactFields.CONTACT_SOURCE;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, contactSourceKey);
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final Collection<Option> contactSources = optionService.getOptions(dropdownRequest.getOrgId(),
                fields.get(contactSourceKey), dropdownRequest.getCriteria(), dropdownRequest.i18nResolver(), dropdownRequest.getOrder());
        final int start = dropdownLimit.start();
        final long totalRecords = contactSources.size();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);
        final CrmRequest.ClientType clientType = dropdownRequest.getClientType();

        if (totalRecords > 0 && start <= totalRecords) {

            if (CrmRequest.ClientType.ZAPIER == clientType || CrmRequest.ClientType.REST_API == clientType) {

                for (final Option contactSource : applyLimit(contactSources, start, dropdownLimit.limit())) {
                    final JsonObject jsonObject = Json.object();

                    jsonObject.put(OptionKeys.ID, contactSource.id());
                    jsonObject.put(OptionKeys.NAME, contactSource.getOption());

                    optionContainer.addOption(jsonObject);
                }
            } else {

                for (final Option contactSource : applyLimit(contactSources, start, dropdownLimit.limit())) {
                    optionContainer.addOption(contactSource.id(), contactSource.getOption(), contactSource.getOrder(),
                            contactSource.isEditable(), contactSource.isDefault());
                }
            }
        }

        return optionContainer;
    }
}
